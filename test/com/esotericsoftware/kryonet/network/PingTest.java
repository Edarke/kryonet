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

import com.esotericsoftware.kryonet.adapters.ConnectionAdapter;
import com.esotericsoftware.kryonet.network.impl.Client;
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.kryonet.network.messages.BidirectionalMessage;
import com.esotericsoftware.kryonet.utils.StringMessage;
import com.esotericsoftware.minlog.Log;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PingTest extends KryoNetTestCase {

	public static class Ping2 implements BidirectionalMessage {
		boolean isReply;
	}





	public void testPingToServer() throws IOException, TimeoutException {

		AtomicInteger countClient = new AtomicInteger(0);
		AtomicBoolean isReplyClient = new AtomicBoolean(false);
		AtomicBoolean isEarlyDisconnect = new AtomicBoolean(false);



		AtomicInteger countServer = new AtomicInteger(0);

		final Server server = new Server();
		server.addListener(new ConnectionAdapter<ClientConnection>(){
			@Override
			public void received (ClientConnection connection, Object object) {
				Log.debug("Handling " + object);
				countServer.incrementAndGet();
				test.assertTrue(object instanceof Ping2);



				Ping2 ping = (Ping2)object;
				test.assertFalse(ping.isReply);

				Log.debug("Got " + object);
				if (ping.isReply) {
					System.out.println("Ping2: " + connection.getReturnTripTime());
				} else {
					ping.isReply = true;
					connection.send(ping);
				}

			}
		});


		// ----

		Client client = new Client();
		reg(server.getKryo(), client.getKryo(), Ping2.class);

		client.addListener(new ConnectionAdapter<ServerConnection>() {
			public void onConnected(ServerConnection server) {
				try {
					test.assertTrue(countClient.get() == 0);
					server.send(new Ping2());
				} catch (Exception e){
					test.fail(e);
				}
			}

			@Override
			public void onDisconnected(ServerConnection con){
				test.assertEquals(1, countClient.get());
			}

			@Override
			public void received (ServerConnection server, Object object) {
				try {
					Log.info("Rec:" + object + "");
					countClient.incrementAndGet();

					test.assertThat(object, Instance.Of(Ping2.class));


					Ping2 ping = (Ping2) object;
					isReplyClient.set(ping.isReply);

					if (!ping.isReply) {
						ping.isReply = true;
						server.send(ping);
						System.out.println("Ping2: " + server.getReturnTripTime());
					} else {
						Log.info("Got reply!");
						test.resume();
					}
				} catch (Exception e) {
					test.fail(e);
				}
			}
		});

		Log.TRACE();
		reg(server.getKryo(), client.getKryo(), Ping2.class, StringMessage.class);
		new Thread(client).start();
		new Thread(server).start();
		server.bind(tcpPort);
		client.connect(2000, host, tcpPort);


		test.await(5000);
		assertFalse(isEarlyDisconnect.get());
		assertTrue(countClient.get() == 1);
		assertTrue(isReplyClient.get());
	}
}
