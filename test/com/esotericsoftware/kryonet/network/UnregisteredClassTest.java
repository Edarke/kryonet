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
import com.esotericsoftware.kryonet.utils.DataMessage;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class UnregisteredClassTest extends KryoNetTestCase {
	public void testUnregisteredClasses () throws IOException, TimeoutException {
		final DataMessage dataTCP = DataMessage.random();
		populateData(dataTCP, true);
		final DataMessage dataUDP = DataMessage.random();
		populateData(dataUDP, false);

		final AtomicInteger receivedTCP = new AtomicInteger();
		final AtomicInteger receivedUDP = new AtomicInteger();

		final Server server = new Server(1024 * 32, 1024 * 16);
		server.getKryo().setRegistrationRequired(false);
		startEndPoint(server);
		server.bind(tcpPort, udpPort);
		server.addListener(new ConnectionAdapter<ClientConnection>() {
			public void onConnected(ClientConnection connection) {
				try {
					connection.sendTCP(dataTCP);
					connection.sendUDP(dataUDP);
				} catch (Exception e){
					test.fail(e);
				}
			}

			public void received (ClientConnection connection, Object object) {
				if (object instanceof DataMessage) {
					DataMessage data = (DataMessage)object;
					if (data.isTCP) {
						test.assertEquals(data, dataTCP);
						receivedTCP.incrementAndGet();
					} else {
						test.assertEquals(data, dataUDP);
						receivedUDP.incrementAndGet();
					}

					if(receivedTCP.get() == 2 && receivedUDP.get() == 2){
						test.resume();
					}
				} else {
					test.fail();
				}
			}
		});

		// ----
		Client client = new Client(1024 * 32, 1024 * 16);
		client.getKryo().setRegistrationRequired(false);
		startEndPoint(client);
		client.addListener(new ConnectionAdapter<ServerConnection>() {
			public void received (ServerConnection connection, Object object) {
				if (object instanceof DataMessage) {
					DataMessage data = (DataMessage)object;
					if (data.isTCP) {
						test.assertEquals(data, dataTCP);
						receivedTCP.incrementAndGet();
						connection.sendTCP(data);
					} else {
						test.assertEquals(data, dataUDP);
						receivedUDP.incrementAndGet();
						connection.sendUDP(data);
					}
				} else {
					test.fail();
				}
			}
		});

		client.connect(5000, host, tcpPort, udpPort);

		test.await(5000);

		assertEquals(2, receivedTCP.intValue());
		assertEquals(2, receivedUDP.intValue());
	}

	private void populateData (DataMessage data, boolean isTCP) {
		data.isTCP = isTCP;

		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < 3000; i++)
			buffer.append('a');
		data.string = buffer.toString();

		data.strings = new String[] {"abcdefghijklmnopqrstuvwxyz0123456789", "", null, "!@#$", "�����"};
		data.ints = new int[] {-1234567, 1234567, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE};
		data.shorts = new short[] {-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE};
		data.floats = new float[] {0, -0, 1, -1, 123456, -123456, 0.1f, 0.2f, -0.3f, (float)Math.PI, Float.MAX_VALUE,
			Float.MIN_VALUE};
		data.doubles = new double[] {0, -0, 1, -1, 123456, -123456, 0.1d, 0.2d, -0.3d, Math.PI, Double.MAX_VALUE, Double.MIN_VALUE};
		data.longs = new long[] {0, -0, 1, -1, 123456, -123456, 99999999999L, -99999999999L, Long.MAX_VALUE, Long.MIN_VALUE};
		data.bytes = new byte[] {-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE};
		data.chars = new char[] {32345, 12345, 0, 1, 63, Character.MAX_VALUE, Character.MIN_VALUE};
		data.booleans = new boolean[] {true, false};
		data.Ints = new Integer[] {-1234567, 1234567, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE};
		data.Shorts = new Short[] {-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE};
		data.Floats = new Float[] {0f, -0f, 1f, -1f, 123456f, -123456f, 0.1f, 0.2f, -0.3f, (float)Math.PI, Float.MAX_VALUE,
			Float.MIN_VALUE};
		data.Doubles = new Double[] {0d, -0d, 1d, -1d, 123456d, -123456d, 0.1d, 0.2d, -0.3d, Math.PI, Double.MAX_VALUE,
			Double.MIN_VALUE};
		data.Longs = new Long[] {0L, -0L, 1L, -1L, 123456L, -123456L, 99999999999L, -99999999999L, Long.MAX_VALUE, Long.MIN_VALUE};
		data.Bytes = new Byte[] {-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE};
		data.Booleans = new Boolean[] {true, false};
	}
}
