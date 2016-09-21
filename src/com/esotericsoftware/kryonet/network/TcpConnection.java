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

import com.esotericsoftware.kryonet.serializers.Serialization;
import com.esotericsoftware.kryonet.util.KryoNetException;
import com.esotericsoftware.kryonet.util.ProtocolUtils;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static com.esotericsoftware.minlog.Log.*;

/** @author Nathan Sweet <misc@n4te.com> */
class TcpConnection {
	static private final int IPTOS_LOWDELAY = 0x10;
	private static final String TAG = "Kryonet";


	SocketChannel socketChannel;
	int keepAliveMillis = 8000;
	final ByteBuffer readBuffer, writeBuffer;
	boolean bufferPositionFix;
	int timeoutMillis = 12000;
	float idleThreshold = 0.1f;

	private final Serialization serialization;
	private SelectionKey selectionKey;
	private volatile long lastWriteTime, lastReadTime;
	private int currentObjectLength;
	private final Object writeLock = new Object();

	public TcpConnection (Serialization serialization, int writeBufferSize, int objectBufferSize) {
		this.serialization = serialization;
		writeBuffer = ByteBuffer.allocate(writeBufferSize);
		readBuffer = ByteBuffer.allocate(objectBufferSize);
		readBuffer.flip();
	}

	public SelectionKey accept (Selector selector, SocketChannel socketChannel) throws IOException {
		writeBuffer.clear();
		readBuffer.clear();
		readBuffer.flip();
		currentObjectLength = 0;
		try {
			this.socketChannel = socketChannel;
			socketChannel.configureBlocking(false);
			Socket socket = socketChannel.socket();
			socket.setTcpNoDelay(true);

			selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);

			if (DEBUG) {
				debug(TAG, "Port " + socketChannel.socket().getLocalPort() + "/TCP onConnected to: "
					+ socketChannel.socket().getRemoteSocketAddress());
			}

			lastReadTime = lastWriteTime = System.currentTimeMillis();

			return selectionKey;
		} catch (IOException ex) {
			close();
			throw ex;
		}
	}

	public void connect (Selector selector, SocketAddress remoteAddress, int timeout) throws IOException {
		close();
		writeBuffer.clear();
		readBuffer.clear();
		readBuffer.flip();
		currentObjectLength = 0;
		try {
			SocketChannel socketChannel = selector.provider().openSocketChannel();
			Socket socket = socketChannel.socket();
			socket.setTcpNoDelay(true);
			// socket.setTrafficClass(IPTOS_LOWDELAY);
			socket.connect(remoteAddress, timeout); // Connect using blocking mode for simplicity.
			socketChannel.configureBlocking(false);
			this.socketChannel = socketChannel;

			selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
			selectionKey.attach(this);

			if (DEBUG) {
				debug(TAG, "Port " + socketChannel.socket().getLocalPort() + "/TCP onConnected to: "
					+ socketChannel.socket().getRemoteSocketAddress());
			}

			lastReadTime = lastWriteTime = System.currentTimeMillis();
		} catch (IOException ex) {
			close();
			throw new IOException("Unable to connect to: " + remoteAddress, ex);
		}
	}




	private boolean fillReadBuffer(int length) throws IOException {
		final ByteBuffer buffer = this.readBuffer;

		if (buffer.remaining() < length) {
			buffer.compact();

			final int bytesRead = socketChannel.read(buffer);
			buffer.flip();

			if (bytesRead == -1) throw new SocketException("Connection is closed.");
			lastReadTime = System.currentTimeMillis();

			if (buffer.remaining() < length)
				return false;
		}
		return true;
	}




	public Object readObject() throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) throw new SocketException("Connection is closed.");

		if (currentObjectLength == 0) {

			// Read the length of the next object from the socket.
			if(!fillReadBuffer(serialization.getLengthLength())){
				return null;
			}
			currentObjectLength = serialization.readLength(readBuffer);

			if (currentObjectLength <= 0) throw new KryoNetException("Invalid object length: " + currentObjectLength);
			if (currentObjectLength > readBuffer.capacity())
				throw new KryoNetException("Unable to fillReadBuffer object larger than fillReadBuffer buffer: " + currentObjectLength);
		}


		final int length = currentObjectLength;
		if(!fillReadBuffer(length)){
			return null;
		}
		currentObjectLength = 0;


		final int startPosition = readBuffer.position();
		final int oldLimit = readBuffer.limit();
		readBuffer.limit(startPosition + length);

		final Object object;
		try {
			object = serialization.read(readBuffer);
		} catch (Exception ex) {
			throw new KryoNetException("Error during deserialization.", ex);
		}

		readBuffer.limit(oldLimit);
		if (readBuffer.position() - startPosition != length)
			throw new KryoNetException("Incorrect number of bytes (" + (startPosition + length - readBuffer.position())
				+ " remaining) used to deserialize object: " + object);

		return object;
	}

	public void writeOperation () throws IOException {
		synchronized (writeLock) {
			if (writeToSocket()) {
				// Write successful, clear OP_WRITE.
				selectionKey.interestOps(SelectionKey.OP_READ);
			}
			lastWriteTime = System.currentTimeMillis();
		}
	}

	private boolean writeToSocket () throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) throw new SocketException("Connection is closed.");

		ByteBuffer buffer = writeBuffer;
		buffer.flip();
		while (buffer.hasRemaining()) {
			if (bufferPositionFix) {
				buffer.compact();
				buffer.flip();
			}
			if (socketChannel.write(buffer) == 0) break;
		}
		buffer.compact();

		return buffer.position() == 0;
	}

	/** This method is thread safe. */
	public int send (Object object) throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) throw new SocketException("Connection is closed.");
		synchronized (writeLock) {
			// Leave room for length.
			int start = writeBuffer.position();
			int lengthLength = serialization.getLengthLength();
			writeBuffer.position(writeBuffer.position() + lengthLength);

			// Write data.
			try {
				serialization.write(writeBuffer, object);
			} catch (KryoNetException ex) {
				throw new KryoNetException("Error serializing object of type: " + object.getClass().getName(), ex);
			}
			int end = writeBuffer.position();

			// Write data length.
			writeBuffer.position(start);
			serialization.writeLength(writeBuffer, end - lengthLength - start);
			writeBuffer.position(end);

			// Write to socket if no data was queued.
			if (start == 0 && !writeToSocket()) {
				// A partial write, set OP_WRITE to be notified when more writing can occur.
				selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			} else {
				// Full write, wake up selector so onIdle event will be fired.
				selectionKey.selector().wakeup();
			}

			if (DEBUG || TRACE) {
				checkBufferCapacity();
			}

			lastWriteTime = System.currentTimeMillis();
			return end - start;
		}
	}








	/** This method is thread safe. */
	public int sendRaw (ByteBuffer raw) throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) throw new SocketException("Connection is closed.");
		synchronized (writeLock) {
			// Leave room for length.
			int start = writeBuffer.position();
			int lengthLength = serialization.getLengthLength();
			writeBuffer.position(writeBuffer.position() + lengthLength);

			// Write data.
			try {
				writeBuffer.put(raw);
				raw.rewind();
			} catch (KryoNetException ex) {
				throw new KryoNetException("Error serializing broadcast message", ex);
			}
			int end = writeBuffer.position();

			// Write data length.
			writeBuffer.position(start);
			serialization.writeLength(writeBuffer, end - lengthLength - start);
			writeBuffer.position(end);

			// Write to socket if no data was queued.
			if (start == 0 && !writeToSocket()) {
				// A partial write, set OP_WRITE to be notified when more writing can occur.
				selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			} else {
				// Full write, wake up selector so onIdle event will be fired.
				selectionKey.selector().wakeup();
			}

			if (DEBUG || TRACE) {
				checkBufferCapacity();
			}

			lastWriteTime = System.currentTimeMillis();
			return end - start;
		}


	}





	private void checkBufferCapacity() {
		final float percentage = writeBuffer.position() / (float) writeBuffer.capacity();
		if (percentage > 0.75f)
			debug(TAG, "TCP write buffer is approaching capacity: " + percentage + "%");
		else if (TRACE && percentage > 0.25f)
			trace(TAG, "TCP write buffer utilization: " + percentage + "%");
	}






	public void close () {
		if(ProtocolUtils.close(socketChannel, selectionKey)) {
			socketChannel = null;
		}
	}

	public boolean needsKeepAlive (long time) {
		return socketChannel != null && keepAliveMillis > 0 && time - lastWriteTime > keepAliveMillis;
	}

	public boolean isTimedOut (long time) {
		return socketChannel != null && timeoutMillis > 0 && time - lastReadTime > timeoutMillis;
	}


	@Override
	public String toString(){
		return "TcpConnection(" + socketChannel +")";
	}
}
