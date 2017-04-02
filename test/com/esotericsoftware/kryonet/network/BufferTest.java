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
import com.esotericsoftware.kryonet.adapters.ConnectionAdapter;
import com.esotericsoftware.kryonet.network.impl.Client;
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.kryonet.network.messages.BidirectionalMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class BufferTest extends KryoNetTestCase {

	public void testManyLargeMessages () throws IOException, TimeoutException {
		final int messageCount = 1024;
		int objectBufferSize = 10250;
		int writeBufferSize = 10250 * messageCount;

		Server server = new Server(writeBufferSize, objectBufferSize);
		startEndPoint(server);
		register(server.getKryo());
		server.bind(tcpPort);


		AtomicInteger sreceived = new AtomicInteger();
		AtomicInteger sreceivedBytes = new AtomicInteger();

		server.addListener(new ConnectionAdapter<ClientConnection>() {
			@Override
			public void received (ClientConnection connection, Object object) {
				try{
					if (object instanceof LargeMessage) {
						System.out.println("Server sending message: " + sreceived.get());
						connection.sendTCP((LargeMessage)object);

						sreceivedBytes.addAndGet(((LargeMessage)object).bytes.length);

						int count = sreceived.incrementAndGet();
						System.out.println("Server received " + count + " messages.");
						if (count == messageCount) {
							System.out.println("Server received all " + messageCount + " messages!");
							System.out.println("Server received and sent " + sreceivedBytes.get() + " bytes.");
						}
					} else {
						test.fail("Unexpected message " + object);
					}
				} catch (Exception e) {
					test.fail();
				}
			}
		});
		Client client = new Client(writeBufferSize, writeBufferSize);
		startEndPoint(client);
		register(client.getKryo());
		client.connect(2000, host, tcpPort);


		AtomicInteger creceived = new AtomicInteger();
		AtomicInteger creceivedBytes = new AtomicInteger();

		client.addListener(new ConnectionAdapter<ServerConnection>() {
			@Override
			public void received (ServerConnection connection, Object object) {
				try {
					if (object instanceof LargeMessage) {
						int count = creceived.incrementAndGet();
						System.out.println("Client received " + count + " messages.");
						if (count == messageCount) {
							System.out.println("Client received all " + messageCount + " messages!");
							System.out.println("Client received and sent " + creceivedBytes.get() + " bytes.");
							test.resume();
						}
					} else {
						test.fail("Unexpected message " + object);
					}
				} catch (Exception e){
					test.fail();
				}
			}
		});

		byte[] b = new byte[1024 * 10];
		for (int i = 0; i < messageCount; i++) {
			System.out.println("Client sending: " + i);
			client.sendTCP(new LargeMessage(b));
		}
		System.out.println("Client has queued " + messageCount + " messages.");


		test.await(5000);

	}

	private void register (Kryo kryo) {
		kryo.register(byte[].class);
		kryo.register(LargeMessage.class);
	}

	public static class LargeMessage implements BidirectionalMessage {
		public byte[] bytes;

		public LargeMessage () {
		}

		public LargeMessage(int size){
			bytes = new byte[size];
			ThreadLocalRandom.current().nextBytes(bytes);
		}

		public LargeMessage (byte[] bytes) {
			this.bytes = bytes;
		}


		@Override
		public boolean equals(Object o){
			if(o == null) return false;
			if(!(o instanceof LargeMessage)) return false;
			return Arrays.equals(bytes, ((LargeMessage)o).bytes);
		}
	}
}
