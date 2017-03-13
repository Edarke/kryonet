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

package com.esotericsoftware.kryonet.adapters;

import com.esotericsoftware.kryonet.network.AbstractClient;
import com.esotericsoftware.kryonet.network.Connection;
import com.esotericsoftware.kryonet.network.impl.Server;

/** Used to be notified about connection events. */
public interface Listener<T extends Connection> {
	/** Called when the remote end has been onConnected. This will be invoked before any objects are received.
	 * This will be invoked on the same thread as {@link AbstractClient#update(int)} and
	 * {@link Server#update(int)}. This method should not block for long periods as other network activity will not be processed
	 * until it returns. */
	void onConnected(T connection);

	/** Called when the remote end is no longer onConnected. There is no guarantee as to what thread will invoke this method. */
	void onDisconnected(T connection);

	/** Called when the connection is below the {@link Connection#setIdleThreshold(float) onIdle threshold}. */
	void onIdle(T connection);

	void received(T connection, Object msg);

}
