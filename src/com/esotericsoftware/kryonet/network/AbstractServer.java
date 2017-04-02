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
import com.esotericsoftware.kryo.util.IntMap;
import com.esotericsoftware.kryonet.adapters.Listener;
import com.esotericsoftware.kryonet.network.messages.FrameworkMessage;
import com.esotericsoftware.kryonet.network.messages.FrameworkMessage.RegisterTCP;
import com.esotericsoftware.kryonet.network.messages.MessageToClient;
import com.esotericsoftware.kryonet.network.messages.QueryToServer;
import com.esotericsoftware.kryonet.serializers.KryoSerialization;
import com.esotericsoftware.kryonet.serializers.Serialization;
import com.esotericsoftware.kryonet.util.KryoNetException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.esotericsoftware.minlog.Log.DEBUG;
import static com.esotericsoftware.minlog.Log.ERROR;
import static com.esotericsoftware.minlog.Log.INFO;
import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.WARN;
import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
import static com.esotericsoftware.minlog.Log.info;
import static com.esotericsoftware.minlog.Log.trace;
import static com.esotericsoftware.minlog.Log.warn;

/**
 * Manages TCP and optionally UDP connections from many {@link AbstractClient Clients}.
 *
 * @author Nathan Sweet <misc@n4te.com>
 * @author Evan Darke <evancdarke@gmail.com>
 */
public abstract class AbstractServer<T extends ClientConnection> extends EndPoint<MessageToClient, T> {

    private final IntMap<T> pendingConnections = new IntMap<>();
    private final List<T> connections = new CopyOnWriteArrayList<>();
    private final Class<T> classTag;
    private final Listener<Connection> dispatchListener = new Listener<Connection>() {

        public void onConnected(Connection conn) {
            System.err.println("Dispatch listener got connection!");
            conn.notifyConnected();

            final T connection = classTag.cast(conn);
            final List<Listener<? super T>> listeners = AbstractServer.this.listeners;

            for (Listener<? super T> listener : listeners)
                listener.onConnected(connection);
        }

        public void onDisconnected(Connection conn) {
            final T connection = classTag.cast(conn);
            removeConnection(connection);

            final List<Listener<? super T>> listeners = AbstractServer.this.listeners;

            for (Listener<? super T> listener : listeners)
                listener.onDisconnected(connection);
        }

        public void onIdle(Connection conn) {
            final T connection = classTag.cast(conn);
            final List<Listener<? super T>> listeners = AbstractServer.this.listeners;


            for (Listener<? super T> listener : listeners) {
                listener.onIdle(connection);
                if (!connection.isIdle()) break;
            }
        }

        public void received(Connection conn, Object object) {
            throw new UnsupportedOperationException();
        }
    };
    private ServerSocketChannel serverChannel;
    private UdpConnection udp;
    private int nextConnectionID = 1;
    private volatile boolean shutdown;
    private ServerDiscoveryHandler discoveryHandler;

    /**
     * Creates a Server with a write buffer size of 16384 and an object buffer size of 2048.
     */
    public AbstractServer(Class<T> tag) {
        this(tag, DEFAULT_WRITE_BUFFER, DEFAULT_OBJ_BUFFER);
    }

    /**
     * @param writeBufferSize  One buffer of this size is allocated for each onConnected client. Objects are serialized to the write
     *                         buffer where the bytes are queued until they can be written to the TCP socket.
     *                         <p>
     *                         Normally the socket is writable and the bytes are written immediately. If the socket cannot be written to and
     *                         enough serialized objects are queued to overflow the buffer, then the connection will be closed.
     *                         <p>
     *                         The write buffer should be sized at least as large as the largest object that will be sent, plus some head room to
     *                         allow for some serialized objects to be queued in case the buffer is temporarily not writable. The amount of head
     *                         room needed is dependent upon the size of objects being sent and how often they are sent.
     * @param objectBufferSize One (using only TCP) or three (using both TCP and UDP) buffers of this size are allocated. These
     *                         buffers are used to hold the bytes for a single object graph until it can be sent over the network or
     *                         deserialized.
     *                         <p>
     *                         The object buffers should be sized at least as large as the largest object that will be sent or received.
     */
    public AbstractServer(Class<T> tag, int writeBufferSize, int objectBufferSize) {
        this(tag, writeBufferSize, objectBufferSize, new KryoSerialization());
    }

    public AbstractServer(Class<T> tag, int writeBufferSize, int objectBufferSize, Serialization serialization) {
        super(serialization, writeBufferSize, objectBufferSize);
        this.discoveryHandler = ServerDiscoveryHandler.DEFAULT;
        this.classTag = tag;

        try {
            selector = Selector.open();
        } catch (IOException ex) {
            throw new RuntimeException("Error opening selector.", ex);
        }
    }

    public void setDiscoveryHandler(ServerDiscoveryHandler newDiscoveryHandler) {
        discoveryHandler = newDiscoveryHandler;
    }

    /**
     * Opens a TCP only server.
     *
     * @throws IOException if the server could not be opened.
     */
    public void bind(int tcpPort) throws IOException {
        bind(new InetSocketAddress(tcpPort), null);
    }

    /**
     * @param udpPort May be null.
     */
    public void bind(InetSocketAddress tcpPort, InetSocketAddress udpPort) throws IOException {
        close();
        synchronized (updateLock) {
            selector.wakeup();
            try {
                serverChannel = selector.provider().openServerSocketChannel();
                serverChannel.socket().bind(tcpPort);
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                if (DEBUG) debug(TAG, "Accepting connections on port: " + tcpPort + "/TCP");

                if (udpPort != null) {
                    udp = new UdpConnection(serializer, objectBufferSize);
                    udp.bind(selector, udpPort);
                    if (DEBUG) debug(TAG, "Accepting connections on port: " + udpPort + "/UDP");
                }
            } catch (IOException ex) {
                close();
                throw ex;
            }
        }
        if (INFO) info(TAG, "Server opened.");
    }

    /**
     * Opens a TCP and UDP server.
     *
     * @throws IOException if the server could not be opened.
     */
    public void bind(int tcpPort, int udpPort) throws IOException {
        bind(new InetSocketAddress(tcpPort), new InetSocketAddress(udpPort));
    }

    void removeConnection(T connection) {
        connections.remove(connection);
        pendingConnections.remove(connection.id);
    }

    public void sendToAll(MessageToClient object, Iterable<T> targets) {
        if (object.isReliable()) {
            sendToAllTCP(object, targets);
        } else {
            sendToAllUDP(object, targets);
        }
    }

    public void sendToAllTCP(MessageToClient object, Iterable<T> targets) {
        final CachedMessage<MessageToClient> raw = cachedMessageFactory.createTemp(object);
        sendToAllTCP(raw, targets);
    }

    public void sendToAllUDP(MessageToClient object, Iterable<T> targets) {
        sendToAllUDP(cachedMessageFactory.createTemp(object), targets);
    }

    public void sendToAll(CachedMessage<? extends MessageToClient> msg) {
        sendToAll(msg, this.getConnections());
    }

    public void sendToAll(CachedMessage<? extends MessageToClient> msg, Iterable<T> targets) {
        if (msg.isReliable) {
            sendToAllTCP(msg, targets);
        } else {
            sendToAllUDP(msg, targets);
        }
    }

    public void sendToAllTCP(CachedMessage<? extends MessageToClient> msg, Iterable<T> targets) {
        final byte[] raw = msg.cached;
        final int offset = msg.start;
        final int length = msg.length;
        ByteBuffer buffer = ByteBuffer.wrap(raw, offset, length);

        for (T target : targets) {
            target.sendBytesTCP(buffer, length);
            buffer.position(offset);
        }
    }

    public void sendToAllUDP(CachedMessage<? extends MessageToClient> msg, Iterable<T> targets) {
        final byte[] raw = msg.cached;
        final int length = msg.length;
        final int offset = msg.start;
        final ByteBuffer buffer = ByteBuffer.wrap(raw, offset, length);

        for (T target : targets) {
            target.sendBytesUDP(buffer);
            buffer.position(offset);
        }
    }

    public void sendToAllTCP(MessageToClient msg) {
        sendToAllTCP(msg, this.connections);
    }

    public void sendToAllTCP(CachedMessage<? extends MessageToClient> msg) {
        sendToAllTCP(msg, this.connections);
    }

    public void sendToAllUDP(MessageToClient msg) {
        sendToAllUDP(msg, this.connections);
    }

    public void sendToAllUDP(CachedMessage<? extends MessageToClient> msg) {
        sendToAllUDP(msg, this.connections);
    }

    public void sendToAllOthers(int connectionID, MessageToClient msg) {
        if (msg.isReliable()) {
            sendToAllOthersTCP(connectionID, msg);
        } else {
            sendToAllOthersUDP(connectionID, msg);
        }
    }

    public void sendToAllOthersTCP(int connectionID, MessageToClient object) {
        sendToAllOthersTCP(connectionID, cachedMessageFactory.createTemp(object));
    }

    public void sendToAllOthersUDP(int connectionID, MessageToClient object) {
        sendToAllOthersUDP(connectionID, cachedMessageFactory.createTemp(object));
    }

    public void sendToAllOthers(int connectionID, CachedMessage<? extends MessageToClient> msg) {
        if (msg.isReliable) {
            sendToAllOthersTCP(connectionID, msg);
        } else {
            sendToAllOthersUDP(connectionID, msg);
        }
    }

    public void sendToAllOthersTCP(int connectionID, CachedMessage<? extends MessageToClient> msg) {
        final byte[] buffer = msg.cached;
        final int length = msg.length;
        final int offset = msg.start;
        ByteBuffer raw = ByteBuffer.wrap(buffer, offset, length);

        final List<T> connections = this.connections;
        for (T target : connections) {
            if (target.getID() != connectionID) {
                target.sendBytesTCP(raw, length);
                raw.position(offset);
            }
        }
    }

    public void sendToAllOthersUDP(int connectionID, CachedMessage<? extends MessageToClient> msg) {
        final byte[] buffer = msg.cached;
        final int offset = msg.start;
        final int length = msg.length;
        ByteBuffer raw = ByteBuffer.wrap(buffer, offset, length);


        final List<T> connections = this.connections;
        for (T target : connections) {
            if (target.getID() != connectionID) {
                target.sendBytesUDP(raw);
                raw.position(offset);
            }
        }
    }

    /**
     * Returns an unmodifiable view of active connections. As clients connect and
     * disconnect, the view will be updated, but attempting to modify it directly will
     * result in an exception.
     * <p>
     * Its safe to call this method once the server is create and maintain a reference to it indefinitely
     * No guarantee is made about the order in which the clients are maintained
     */
    public List<T> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    protected String getTag() {
        return "KryoServer";
    }

    public void run() {
        if (TRACE) trace(TAG, "Server thread started.");
        shutdown = false;
        while (!shutdown) {
            try {
                update(250);
            } catch (IOException ex) {
                if (ERROR) error(TAG, "Error updating server connections.", ex);
                close();
            }
        }
        if (TRACE) trace(TAG, "Server thread stopped.");
    }

    private void keepAlive() {
        long time = System.currentTimeMillis();
        final List<T> connections = this.connections;
        for (T connection : connections) {
            if (connection.tcp.needsKeepAlive(time)) connection.sendObjectTCP(FrameworkMessage.keepAlive);
        }
    }

    private void handleTCP(Object object, T fromConnection) {
        if (object instanceof FrameworkMessage) {
            if (TRACE) {
                trace(TAG, fromConnection + " received: " + object.getClass().getSimpleName());
            }

            if (object instanceof FrameworkMessage.Ping) {
                fromConnection.acceptPing((FrameworkMessage.Ping) object);
            }

            return;  // Don't expose framework objects to user.
        }


        if (DEBUG) {
            String objectString = object == null ? "null" : object.getClass().getSimpleName();
            debug(TAG, fromConnection + " received: " + objectString);
        }


        if (object instanceof Response) {
            fromConnection.accept((Response) object);
            return;
        } else if (object instanceof QueryToServer) {
            ((Query) object).setOrigin(fromConnection);
        }

        final List<Listener<? super T>> listeners = AbstractServer.this.listeners;

        for (Listener<? super T> listener : listeners)
            listener.received(fromConnection, object);

    }

    private void acceptOperation(SocketChannel socketChannel) {
        T connection = newConnection();
        connection.initialize(serializer, dispatchListener, writeBufferSize, objectBufferSize);
        connection.endPoint = this;
        UdpConnection udp = this.udp;
        if (udp != null) connection.udp = udp;
        try {
            SelectionKey selectionKey = connection.tcp.accept(selector, socketChannel);
            selectionKey.attach(connection);

            int id = ++nextConnectionID;
            if (nextConnectionID == -1) nextConnectionID = 1;
            connection.id = id;
            connection.setConnected(true);


            if (udp == null)
                addConnection(connection);
            else
                pendingConnections.put(id, connection);

            RegisterTCP registerConnection = new RegisterTCP();
            registerConnection.connectionID = id;
            connection.sendObjectTCP(registerConnection);

            if (udp == null) connection.notifyConnected();
        } catch (IOException ex) {
            connection.close();
            if (DEBUG) debug(TAG, "Unable to accept TCP connection.", ex);
        }
    }

    private void handleUDP(Object object, T fromConnection, InetSocketAddress fromAddress) {
        if (object instanceof FrameworkMessage) {
            if (object instanceof FrameworkMessage.RegisterUDP) {
                // Store the fromAddress on the connection and reply over TCP with a RegisterUDP to indicate success.
                int fromConnectionID = ((FrameworkMessage.RegisterUDP) object).connectionID;
                T connection = pendingConnections.remove(fromConnectionID);
                if (connection != null) {
                    if (connection.udpRemoteAddress == null) {
                        connection.udpRemoteAddress = fromAddress;
                        addConnection(connection);
                        connection.sendObjectTCP(new FrameworkMessage.RegisterUDP());
                        if (DEBUG)
                            debug(TAG, "Port " + udp.datagramChannel.socket().getLocalPort() + "/UDP connected to: " + fromAddress);
                        dispatchListener.onConnected(connection);
                    }
                } else if (DEBUG)
                    debug(TAG, "Ignoring incoming RegisterUDP with invalid connection ID: " + fromConnectionID);
            } else if (object instanceof FrameworkMessage.DiscoverHost) {
                try {
                    boolean responseSent = discoveryHandler.onDiscoverHost(udp.datagramChannel, fromAddress, serializer);
                    if (DEBUG && responseSent)
                        debug(TAG, "Responded to host discovery from: " + fromAddress);
                } catch (IOException ex) {
                    if (WARN) warn(TAG, "Error replying to host discovery from: " + fromAddress, ex);
                }
            }
        } else if (fromConnection != null) {
            final List<Listener<? super T>> listeners = AbstractServer.this.listeners;

            for (Listener<? super T> listener : listeners)
                listener.received(fromConnection, object);
        } else {
            if (DEBUG) debug(TAG, "Ignoring UDP from unregistered address: " + fromAddress);
        }
    }

    /**
     * Allows the connections used by the server to be subclassed.
     * This can be useful for storing per connection data.
     */
    protected abstract T newConnection();

    private void addConnection(T connection) {
        connections.add(connection);
    }

    public void start() {
        new Thread(this, "Server").start();
    }

    public void stop() {
        if (shutdown) return;
        close();
        if (TRACE) trace(TAG, "Server thread stopping.");
        shutdown = true;
    }

    /**
     * Closes all open connections and the server port(s).
     */
    public void close() {
        List<T> connections = this.connections;
        if (INFO && connections.size() > 0) info(TAG, "Closing server connections...");
        for (T t : connections)
            t.close();
        connections.clear();

        ServerSocketChannel serverChannel = this.serverChannel;
        if (serverChannel != null) {
            try {
                serverChannel.close();
                if (INFO) info(TAG, "Server closed.");
            } catch (IOException ex) {
                if (DEBUG) debug(TAG, "Unable to close server.", ex);
            }
            this.serverChannel = null;
        }

        UdpConnection udp = this.udp;
        if (udp != null) {
            udp.close();
            this.udp = null;
        }

        synchronized (updateLock) { // Blocks to avoid a select while the selector is used to bind the server connection.
        }
        // Select one last time to complete closing the socket.
        selector.wakeup();
        try {
            selector.selectNow();
        } catch (IOException ignored) {
        }
    }

    /**
     * Accepts any new connections and reads or writes any pending data for the current connections.
     *
     * @param timeout Wait for up to the specified milliseconds for a connection to be ready to process. May be zero to return
     *                immediately if there are no connections to process.
     */
    public void update(int timeout) throws IOException {
        if (isSelectReady(timeout)) {
            emptySelects = 0;
            Set<SelectionKey> keys = selector.selectedKeys();
            synchronized (keys) {
                UdpConnection udp = this.udp;
                for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext(); ) {
                    keepAlive();
                    SelectionKey selectionKey = iter.next();
                    iter.remove();
                    T fromConnection = classTag.cast(selectionKey.attachment());
                    try {
                        final int ops = selectionKey.readyOps();

                        if (fromConnection != null) { // Must be a TCP read or write operation.
                            if (udp != null && fromConnection.udpRemoteAddress == null) {
                                fromConnection.close();
                                continue;
                            }
                            if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                                try {
                                    while (true) {
                                        Object object = fromConnection.tcp.readObject();
                                        if (object == null) break;
                                        handleTCP(object, fromConnection);
                                    }
                                } catch (IOException ex) {
                                    if (TRACE) {
                                        trace(TAG, "Unable to read TCP from: " + fromConnection, ex);
                                    } else if (DEBUG) {
                                        debug(TAG, fromConnection + " update: " + ex.getMessage());
                                    }
                                    fromConnection.close();
                                } catch (KryoNetException ex) {
                                    if (ERROR) error(TAG, "Error reading TCP from connection: " + fromConnection, ex);
                                    fromConnection.close();
                                }
                            }
                            if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                                try {
                                    fromConnection.tcp.writeOperation();
                                } catch (IOException ex) {
                                    if (TRACE) {
                                        trace(TAG, "Unable to write TCP to connection: " + fromConnection, ex);
                                    } else if (DEBUG) {
                                        debug(TAG, fromConnection + " update: " + ex.getMessage());
                                    }
                                    fromConnection.close();
                                }
                            }
                            continue;
                        }

                        if ((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                            ServerSocketChannel serverChannel = this.serverChannel;
                            if (serverChannel == null) continue;
                            try {
                                SocketChannel socketChannel = serverChannel.accept();
                                if (socketChannel != null) acceptOperation(socketChannel);
                            } catch (IOException ex) {
                                if (DEBUG) debug(TAG, "Unable to accept new connection.", ex);
                            }
                            continue;
                        }

                        // Must be a UDP read operation.
                        if (udp == null) {
                            selectionKey.channel().close();
                            continue;
                        }
                        InetSocketAddress fromAddress;
                        try {
                            fromAddress = udp.readFromAddress();
                        } catch (IOException ex) {
                            if (WARN) warn(TAG, "Error reading UDP data.", ex);
                            continue;
                        }
                        if (fromAddress == null) continue;

                        List<T> connections = this.connections;
                        for (T connection : connections) {
                            if (fromAddress.equals(connection.udpRemoteAddress)) {
                                fromConnection = connection;
                                break;
                            }
                        }

                        Object object;
                        try {
                            object = udp.readObject();
                        } catch (KryoNetException ex) {
                            if (WARN) {
                                if (fromConnection != null) {
                                    if (ERROR) error(TAG, "Error reading UDP from connection: " + fromConnection, ex);
                                } else
                                    warn(TAG, "Error reading UDP from unregistered address: " + fromAddress, ex);
                            }
                            continue;
                        }
                        handleUDP(object, fromConnection, fromAddress);
                    } catch (CancelledKeyException ex) {
                        if (fromConnection != null)
                            fromConnection.close();
                        else
                            selectionKey.channel().close();
                    }
                }
            }
        }
        long time = System.currentTimeMillis();
        List<T> connections = this.connections;
        for (Connection connection : connections) {
            if (connection.tcp.isTimedOut(time)) {
                if (DEBUG)
                    debug(TAG, connection + " timed out.");
                connection.close();
            } else {
                if (connection.tcp.needsKeepAlive(time)) connection.sendObjectTCP(FrameworkMessage.keepAlive);
            }
            if (connection.isIdle()) dispatchListener.onIdle(connection);
        }
    }

    public Kryo getKryo() {
        return ((KryoSerialization) serializer).getKryo();
    }


    public void sendToOrigUDP(MessageToClient msg) {
        for (ClientConnection c : this.connections)
            c.sendUDP(msg);
    }

    public void sendToAll(MessageToClient msg) {
        sendToAll(msg, this.connections);
    }
}
