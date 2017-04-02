/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.adapters.Listener;
import com.esotericsoftware.kryonet.futures.TimedFuture;
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.kryonet.network.messages.FrameworkMessage;
import com.esotericsoftware.kryonet.network.messages.FrameworkMessage.Ping;
import com.esotericsoftware.kryonet.network.messages.Message;
import com.esotericsoftware.kryonet.serializers.Serialization;
import com.esotericsoftware.kryonet.util.KryoNetException;
import com.esotericsoftware.minlog.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;

import static com.esotericsoftware.minlog.Log.DEBUG;
import static com.esotericsoftware.minlog.Log.INFO;
import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.info;
import static com.esotericsoftware.minlog.Log.trace;

// BOZO - Layer to handle handshake state.

/**
 * Represents a TCP and optionally a UDP connection between a {@link AbstractClient} and a {@link Server}. If either underlying connection
 * is closed or errors, both connections are closed.
 *
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Connection<MSG extends Message> {
    protected static final ConcurrentHashMap<Integer, CompletableFuture<?>> queries = new ConcurrentHashMap<>();

    int id = -1;
    EndPoint endPoint;
    TcpConnection tcp;
    UdpConnection udp;
    InetSocketAddress udpRemoteAddress;
    volatile boolean isConnected;
    volatile KryoNetException lastProtocolError;
    private String name;
    private int lastPingID;
    private long lastPingSendTime;
    private int returnTripTime;
    private Listener<Connection> listener;

    protected Connection() {
    }

    void initialize(Serialization serialization, Listener<Connection> handler, int writeBufferSize, int objectBufferSize) {
        tcp = new TcpConnection(serialization, writeBufferSize, objectBufferSize);
        listener = handler;
    }


    @SuppressWarnings("unchecked")
    <Q> void accept(Response<Q> response) {
        final CompletableFuture<Q> future = (CompletableFuture<Q>) queries.remove(response.id);
        if (future != null) {
            future.complete(response.result);
        } else {
            Log.warn("Received query response, but could not find matching request: " + response);
        }
    }

    /**
     * Returns the server assigned ID. Will return -1 if this connection has never been onConnected or the last assigned ID if this
     * connection has been onDisconnected.
     */
    public int getID() {
        return id;
    }

    /**
     * Returns true if this connection is onConnected to the remote end. Note that a connection can become onDisconnected at any time.
     */
    public boolean isConnected() {
        return isConnected;
    }

    void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected && name == null) name = "Connection " + id;
    }

    /**
     * Returns the last protocol error that occurred on the connection.
     *
     * @return The last protocol error or null if none error occurred.
     */
    public KryoNetException getLastProtocolError() {
        return lastProtocolError;
    }

    public int send(CachedMessage<? extends MSG> msg) {
        final int length = msg.length;
        ByteBuffer buffer = ByteBuffer.wrap(msg.cached, msg.start, length);
        if (msg.isReliable) {
            sendBytesTCP(buffer, length);
        } else {
            sendBytesUDP(buffer);
        }
        return length;
    }

    public int sendTCP(CachedMessage<? extends MSG> msg) {
        final int length = msg.length;
        ByteBuffer raw = ByteBuffer.wrap(msg.cached, msg.start, msg.length);
        sendBytesTCP(raw, length);
        return length;
    }

    int sendBytesTCP(ByteBuffer buffer, int length) {
        try {
            return tcp.sendRaw(buffer, length);
        } catch (IOException e) {
            if (DEBUG) debug("kryonet", "Unable to sendRaw TCP with connection: " + this, e);
            close();
            return 0;
        }
    }

    public void close() {
        boolean wasConnected = isConnected;
        isConnected = false;
        tcp.close();
        if (udp != null && udp.connectedAddress != null) udp.close();
        setConnected(false);

        if (wasConnected) {
            if (INFO) info("kryonet", this + " onDisconnected.");
            notifyDisconnected();
        }
    }

    private void notifyDisconnected() {
        listener.onDisconnected(this);
    }

    public int sendUDP(CachedMessage<? extends MSG> msg) {
        sendBytesUDP(ByteBuffer.wrap(msg.cached, msg.start, msg.length));
        return msg.length;
    }

    void sendBytesUDP(ByteBuffer raw) {
        SocketAddress address = udpRemoteAddress;
        if (address == null && udp != null) address = udp.connectedAddress;
        if (address == null && isConnected) throw new IllegalStateException("Connection is not onConnected via UDP.");

        try {
            if (address == null)
                throw new SocketException("Connection is closed.");
            udp.sendRaw(raw, address);
        } catch (IOException ex) {
            if (DEBUG) debug("kryonet", "Unable to sendRaw UDP with connection: " + this, ex);
            close();
        }
    }

    /**
     * Sends a Message via TCP or UDP depending on the return value of
     * msg.isReliable(). If isReliable() returns true, the message
     * is sent over TCP, otherwise it's sent over UDP.
     * <p>
     * This is the preferred way to send a message to an endpoint.
     * To send a particular instance of a message over TCP or UDP
     * regardless of its implementation of isReliable see
     * {@link #sendTCP(MSG) } and {@link #sendUDP(MSG) }
     *
     * @return The number of bytes sent
     */
    public int send(MSG msg) {
        if (msg.isReliable())
            return sendObjectTCP(msg);
        else
            return sendObjectUDP(msg);
    }

    /**
     * Sends the object over the network using TCP.
     *
     * @return The number of bytes sent.
     * @see Kryo#register(Class, com.esotericsoftware.kryo.Serializer)
     */
    public int sendTCP(MSG msg) {
        return sendObjectTCP(msg);
    }

    /**
     * Sends the object over the network using UDP.
     *
     * @return The number of bytes sent.
     * @throws IllegalStateException if this connection was not opened with both TCP and UDP.
     * @see Kryo#register(Class, com.esotericsoftware.kryo.Serializer)
     */
    public int sendUDP(MSG msg) {
        return sendObjectUDP(msg);
    }

    int sendObjectUDP(Object object) {
        Objects.requireNonNull(object, "Cannot send null object.");
        SocketAddress address = udpRemoteAddress;
        if (address == null && udp != null) address = udp.connectedAddress;
        if (address == null && isConnected) throw new IllegalStateException("Connection is not onConnected via UDP.");

        try {
            if (address == null) throw new SocketException("Connection is closed.");

            int length = udp.send(object, address);
            if (length == 0) {
                if (TRACE) trace("kryonet", this + " UDP had nothing to sendRaw.");
            } else if (DEBUG) {
                if (length != -1) {
                    String objectString = object.getClass().getSimpleName();
                    if (!(object instanceof FrameworkMessage)) {
                        debug("kryonet", this + " sent UDP: " + objectString + " (" + length + ")");
                    } else if (TRACE) {
                        trace("kryonet", this + " sent UDP: " + objectString + " (" + length + ")");
                    }
                } else
                    debug("kryonet", this + " was unable to sendRaw, UDP socket buffer full.");
            }
            return length;
        } catch (IOException | KryoNetException ex) {
            if (DEBUG) debug("kryonet", "Unable to sendRaw UDP with connection: " + this, ex);
            close();
            return 0;
        }
    }

    /**
     * Requests the connection to communicate with the remote computer to determine a new value for the
     * {@link #getReturnTripTime() return trip time}. When the connection receives a {@link Ping} object with
     * {@link Ping#isReply isReply} set to true, the new return trip time is available.
     */
    public void updateReturnTripTime() {
        Ping ping = new Ping();
        ping.id = lastPingID++;
        lastPingSendTime = System.currentTimeMillis();
        sendObjectTCP(ping);
    }

    protected int sendObjectTCP(Object object) {
        if (object == null) throw new IllegalArgumentException("object cannot be null.");

        try {
            final int length = tcp.send(object);
            if (length == 0) {
                if (TRACE) trace("kryonet", this + " TCP had nothing to send.");
            } else if (DEBUG) {
                String objectString = object.getClass().getSimpleName();
                if (!(object instanceof FrameworkMessage)) {
                    debug("kryonet", this + " sent TCP: " + objectString + " (" + length + ")");
                } else if (TRACE) {
                    trace("kryonet", this + " sent TCP: " + objectString + " (" + length + ")");
                }
            }
            return length;
        } catch (IOException | KryoNetException ex) {
            if (DEBUG) debug("kryonet", "Unable to sendRaw TCP with connection: " + this, ex);
            close();
            return 0;
        }
    }

    /**
     * Returns the last calculated TCP return trip time, or -1 if {@link #updateReturnTripTime()} has never been called or the
     * {@link Ping} response has not yet been received.
     */
    public int getReturnTripTime() {
        return returnTripTime;
    }

    void setReturnTripTime(int returnTripTime) {
        this.returnTripTime = returnTripTime;
    }

    /**
     * An empty object will be sent if the TCP connection has not sent an object within the specified milliseconds. Periodically
     * sending a keep alive ensures that an abnormal close is detected in a reasonable amount of time (see {@link #setTimeout(int)}
     * ). Also, some network hardware will close a TCP connection that ceases to transmit for a period of time (typically 1+
     * minutes). Set to zero to disable. Defaults to 8000.
     */
    public void setKeepAliveTCP(int keepAliveMillis) {
        tcp.keepAliveMillis = keepAliveMillis;
    }

    /**
     * If the specified amount of time passes without receiving an object over TCP, the connection is considered closed. When a TCP
     * socket is closed normally, the remote end is notified immediately and this timeout is not needed. However, if a socket is
     * closed abnormally (eg, power loss), KryoNet uses this timeout to detect the problem. The timeout should be set higher than
     * the {@link #setKeepAliveTCP(int) TCP keep alive} for the remote end of the connection. The keep alive ensures that the remote
     * end of the connection will be constantly sending objects, and setting the timeout higher than the keep alive allows for
     * network latency. Set to zero to disable. Defaults to 12000.
     */
    public void setTimeout(int timeoutMillis) {
        tcp.timeoutMillis = timeoutMillis;
    }

    void notifyConnected() {
        if (INFO) {
            SocketChannel socketChannel = tcp.socketChannel;
            if (socketChannel != null) {
                Socket socket = tcp.socketChannel.socket();
                if (socket != null) {
                    InetSocketAddress remoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
                    if (remoteSocketAddress != null)
                        info("kryonet", this + " onConnected: " + remoteSocketAddress.getAddress());
                }
            }
        }
    }

    /**
     * Returns the local {@link AbstractClient} or {@link Server} to which this connection belongs.
     */
    public EndPoint getEndPoint() {
        return endPoint;
    }

    /**
     * Returns the IP address and port of the remote end of the TCP connection, or null if this connection is not onConnected.
     */
    public InetSocketAddress getRemoteAddressTCP() {
        SocketChannel socketChannel = tcp.socketChannel;
        if (socketChannel != null) {
            Socket socket = tcp.socketChannel.socket();
            if (socket != null) {
                return (InetSocketAddress) socket.getRemoteSocketAddress();
            }
        }
        return null;
    }

    /**
     * Returns the IP address and port of the remote end of the UDP connection, or null if this connection is not onConnected.
     */
    public InetSocketAddress getRemoteAddressUDP() {
        InetSocketAddress connectedAddress = udp.connectedAddress;
        if (connectedAddress != null) return connectedAddress;
        return udpRemoteAddress;
    }

    /**
     * Workaround for broken NIO networking on Android 1.6. If true, the underlying NIO buffer is always copied to the beginning of
     * the buffer before being given to the SocketChannel for sending. The Harmony SocketChannel implementation in Android 1.6
     * ignores the buffer position, always copying from the beginning of the buffer. This is fixed in Android 2.0+.
     */
    public void setBufferPositionFix(boolean bufferPositionFix) {
        tcp.bufferPositionFix = bufferPositionFix;
    }

    /**
     * Sets the friendly name of this connection. This is returned by {@link #toString()} and is useful for providing application
     * specific identifying information in the logging. May be null for the default name of "Connection X", where X is the
     * connection ID.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the number of bytes that are waiting to be written to the TCP socket, if any.
     */
    public int getTcpWriteBufferSize() {
        return tcp.writeBuffer.position();
    }

    /**
     * @see #setIdleThreshold(float)
     */
    public boolean isIdle() {
        return tcp.writeBuffer.position() / (float) tcp.writeBuffer.capacity() < tcp.idleThreshold;
    }

    /**
     * If the percent of the TCP write buffer that is filled is less than the specified threshold,
     * {@link Listener#onIdle(Connection)} will be called for each network thread update. Default is 0.1.
     */
    public void setIdleThreshold(float idleThreshold) {
        tcp.idleThreshold = idleThreshold;
    }

    @Override
    public String toString() {
        if (name != null)
            return "Connection(" + name + ")";
        return "Connection(" + id + ")";
    }

    public void acceptPing(Ping ping) {
        if (ping.isReply) {
            if (ping.id == lastPingID - 1) {
                returnTripTime = ((int) (System.currentTimeMillis() - lastPingSendTime));
                if (TRACE) trace("kryonet", this + " return trip time: " + returnTripTime);
            }
        } else {
            ping.isReply = true;
            this.sendObjectTCP(ping);
        }
    }

    int getLastPingID() {
        return lastPingID;
    }

    long getLastPingSendTime() {
        return lastPingSendTime;
    }




   /*
    * These methods are not type-safe and are therefore not public.
    * The implementation should expose a public type-safe wrapper for these methods.
    */

    protected <Q> Optional<Q> sendAndWait(Query<Q, ?> query) {
        return sendAndWait(query, query.getTimeout());
    }


    protected <Q> Optional<Q> sendAndWait(Query<Q, ?> query, @Nullable Duration timeout) {
        try {
            return Optional.of(sendAsync(query, timeout).get());
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            return Optional.empty();
        }
    }


    protected <T> CompletableFuture<T> sendAsync(Query<T, ?> query, @Nullable Duration timeout) {
        CompletableFuture<T> future = new CompletableFuture<>();
        queries.put(query.id, future);
        sendObjectTCP(query);
        if (timeout != null) {
            TimedFuture.addTimeout(future, timeout);
            future.exceptionally(error -> {
                queries.remove(query.id);
                return null;
            });
        }
        return future;
    }


    protected <T> CompletableFuture<T> sendAsync(Query<T, ?> query) {
        return sendAsync(query, query.getTimeout());
    }
}
