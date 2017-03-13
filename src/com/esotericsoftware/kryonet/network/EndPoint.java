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
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.kryonet.network.messages.Message;
import com.esotericsoftware.kryonet.serializers.KryoSerialization;
import com.esotericsoftware.kryonet.serializers.Serialization;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Selector;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;


/** Represents the local end point of a connection.
 * @author Nathan Sweet <misc@n4te.com> */
public abstract class EndPoint<FM extends Message, C extends Connection<FM>> implements Runnable, Closeable {
	public static final int DEFAULT_WRITE_BUFFER = 16384, DEFAULT_OBJ_BUFFER = 2048;

	protected Thread updateThread;
	protected int emptySelects;
	protected final Object updateLock = new Object();
	protected Selector selector;
	protected CachedMessageFactory cachedMessageFactory;

	protected final List<Listener<? super C>> listeners = new CopyOnWriteArrayList<>();


	protected final String TAG = getTag();

	protected final Serialization serializer;

	protected final int writeBufferSize;



	protected EndPoint(Serialization serializer, int writeBufferSize){
		this.serializer = serializer;
		this.writeBufferSize = writeBufferSize;
		this.cachedMessageFactory = new CachedMessageFactory(serializer, writeBufferSize);
	}



	/**This is the tag messages from this class are logged with.*/
	protected abstract String getTag();



	protected boolean isSelectReady(int timeout) throws IOException {
		updateThread = Thread.currentThread();
		synchronized (updateLock) { // Blocks to avoid a select while the selector is used to bind the server connection.
		}

		long startTime = System.currentTimeMillis();
		int select = timeout > 0 ? selector.select(timeout) : selector.selectNow();


		if (select == 0) {
			++emptySelects;
			if (emptySelects == 100) {
				emptySelects = 0;
				// NIO freaks and returns immediately with 0 sometimes, so try to keep from hogging the CPU.
				long elapsedTime = System.currentTimeMillis() - startTime;
				try {
					if (elapsedTime < 25)
						Thread.sleep(25 - elapsedTime);
				} catch (InterruptedException ex) {
				}
			}
			return false;
		} else {
			return true;
		}
	}


	/** Releases the resources used by this client, which may no longer be used. */
	public void dispose () throws IOException {
		close();
		selector.close();
	}


	public void addListener (Listener<? super C> listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		listeners.add(listener);
		if (TRACE) trace(TAG, "Added Listener " + listener.getClass().getName());
	}

	public void removeListener (Listener<? super C> listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		listeners.remove(listener);
		if (TRACE) trace(TAG, "Removed Listener: " + listener.getClass().getName());
	}


	/** Gets the serialization instance that will be used to serialize and deserialize objects. */
	public Serialization getSerialization() {
		return serializer;
	}

	/** Continually updates this end point until {@link #stop()} is called. */
	public abstract void run();

	/** Starts a new thread that calls {@link #run()}. */
	public abstract void start();

	/** Closes this end point and causes {@link #run()} to return. */
	public abstract void stop();

	/** @see AbstractClient
	 * @see Server */
	public abstract void close();

	/** @see AbstractClient#update(int)
	 * @see Server#update(int) */
	public abstract void update(int timeout) throws IOException;

	/** Returns the last thread that called {@link #update(int)} for this end point. This can be useful to detect when long running
	 * code will be run on the update thread. */
	public Thread getUpdateThread () {
		return updateThread;
	}

	/** Gets the Kryo instance that will be used to serialize and deserialize objects. This is only valid if
	 * {@link KryoSerialization} is being used, which is the default. */
	public abstract Kryo getKryo();


	/** Creates a new Factory capable of creating CachedMessages.
	 * CachedMessages contain pre-serialized forms of Message objects so they can be sent multiple times
	 * without redundant serialization. */
	public CachedMessageFactory getCachedMessageFactory(){
		return cachedMessageFactory;
	}

}
