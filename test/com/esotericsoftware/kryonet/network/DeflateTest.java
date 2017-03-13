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
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DeflateSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryonet.adapters.ConnectionAdapter;
import com.esotericsoftware.kryonet.network.impl.Client;
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.kryonet.network.messages.BidirectionalMessage;
import com.esotericsoftware.kryonet.utils.ObjectMessage;
import com.esotericsoftware.minlog.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class DeflateTest extends KryoNetTestCase {
	public void testDeflate () throws IOException, TimeoutException {
		final Server server = new Server();
		register(server.getKryo());

		final SomeData data = new SomeData();
		data.text = "some text here aaaaaaaaaabbbbbbbbbbbcccccccccc";
		data.stuff = new short[] {1, 2, 3, 4, 5, 6, 7, 8};

		final ArrayList<Integer> a = new ArrayList<>();
		a.add(12);
		a.add(null);
		a.add(34);

		startEndPoint(server);
		server.bind(tcpPort, udpPort);
		server.addListener(new ConnectionAdapter<ClientConnection>() {
			public void onConnected(ClientConnection connection) {
				server.sendToAllTCP(data);
				connection.sendTCP(data);
				connection.sendTCP(new ObjectMessage(a));
			}
		});

		// ----


		final Client client = new Client();
		register(client.getKryo());
		startEndPoint(client);
		client.addListener(new ConnectionAdapter<ServerConnection>() {

			public void received (ServerConnection connection, Object object) {
				Log.info("AbstractClient received " + object);
				if (object instanceof SomeData) {
					SomeData data = (SomeData)object;
					System.out.println(data.stuff[3]);
				} else if (object instanceof ObjectMessage) {
					test.resume();
				}
			}
		});
		client.connect(5000, host, tcpPort, udpPort);

		test.await(5000);
	}

	static public void register (Kryo kryo) {
		kryo.register(short[].class);
		kryo.register(SomeData.class, new DeflateSerializer(new FieldSerializer(kryo, SomeData.class)));
		kryo.register(ArrayList.class, new CollectionSerializer());
		kryo.register(ObjectMessage.class);
	}

	static public class SomeData implements BidirectionalMessage {
		public String text;
		public short[] stuff;
	}
}
