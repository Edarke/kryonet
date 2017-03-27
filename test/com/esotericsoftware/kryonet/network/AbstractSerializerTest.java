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
import com.esotericsoftware.kryonet.serializers.Serialization;
import com.esotericsoftware.kryonet.utils.DataMessage;
import com.esotericsoftware.kryonet.utils.YesNoQuery;
import com.esotericsoftware.minlog.Log;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractSerializerTest<T extends Serialization> extends KryoNetTestCase {

    protected final T serializer;

    protected AbstractSerializerTest(T serial) {
        this.serializer = serial;
        register();
    }


    protected abstract void register();


    public void testSerializer() throws IOException, TimeoutException {
        final DataMessage dataTCP = DataMessage.random();
        populateData(dataTCP, true);
        final DataMessage dataUDP = DataMessage.random();
        populateData(dataUDP, false);

        final Server server = new Server(16384, 8192, serializer);

        startEndPoint(server);
        server.bind(tcpPort, udpPort);
        server.addListener(new ConnectionAdapter<ClientConnection>() {
            @Override
            public void onConnected(ClientConnection connection) {
                try {
                    int size = connection.sendTCP(dataTCP);
                    System.err.println("dataTCP size is " + size + " bytes");
                    connection.sendUDP(dataUDP); // Note UDP ping pong stops if a UDP packet is lost.
                } catch (Exception e) {
                    test.fail(e);
                }
            }

            @Override
            public void received(ClientConnection connection, Object object) {
                try {
                    if (object instanceof DataMessage) {
                        DataMessage data = (DataMessage) object;
                        if (data.isTCP) {
                            test.assertEquals(data, dataTCP);
                            connection.sendTCP(data);
                        } else {
                            test.assertEquals(data, dataUDP);
                            connection.sendUDP(data);
                        }
                    }
                } catch (Exception e) {
                    test.fail(e);
                }
            }
        });

        // ----

        AtomicInteger count = new AtomicInteger(0);
        final Client client = new Client(16384, 8192, serializer);
        startEndPoint(client);
        client.addListener(new ConnectionAdapter<ServerConnection>() {
            @Override
            public void received(ServerConnection connection, Object object) {
                try {
                    if (object instanceof DataMessage) {
                        DataMessage data = (DataMessage) object;
                        if (data.isTCP) {
                            count.incrementAndGet();
                            test.assertEquals(data, dataTCP);
                            connection.sendTCP(data);
                        } else {
                            count.incrementAndGet();
                            test.assertEquals(data, dataUDP);
                            connection.sendUDP(data);
                        }

                        if (count.get() == 2) {
                            test.resume();
                        }
                    }
                } catch (Exception e) {
                    test.fail(e);
                }
            }
        });

        client.connect(2000, host, tcpPort, udpPort);

        test.await(7500);
        stopEndPoints();
    }


    public void testQuery() throws IOException, TimeoutException {
        final Server server = new Server(16384 * 2, 8192 * 2, serializer);
        final Client client = new Client(16384 * 2, 8192 * 2, serializer);

        startEndPoint(server);
        server.bind(tcpPort, udpPort);
        server.addListener(new ConnectionAdapter<ClientConnection>() {
            @Override
            public void onConnected(ClientConnection connection) {
                try {
                    connection.sendAsync(new YesNoQuery()).thenAccept(result -> {
                        if (result)
                            test.resume();
                        Log.error("Server received reply " + result);
                    }).exceptionally(e -> {
                        test.fail(e);
                        return null;
                    });
                } catch (Exception e) {
                    test.fail(e);
                }
            }
        });

        startEndPoint(client);
        client.addListener(new ConnectionAdapter<ServerConnection>() {
            @Override
            public void received(ServerConnection connection, Object object) {
                try {
                    Log.error("Client received " + object);

                    if (object instanceof YesNoQuery) {
                        ((YesNoQuery) object).reply(true);
                        connection.send(new PingTest.Ping2());
                    }
                } catch (Exception e) {
                    test.fail(e);
                }
            }
        });

        client.connect(1000, host, tcpPort, udpPort);

        test.await(5000);
        stopEndPoints();
    }


    private void populateData(DataMessage data, boolean isTCP) {
        data.isTCP = isTCP;

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 3000; i++)
            buffer.append('a');
        data.string = buffer.toString();

        data.strings = new String[]{"abcdefghijklmnopqrstuvwxyz0123456789", "", null, "!@#$", "�����"};
        data.ints = new int[]{-1234567, 1234567, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        data.shorts = new short[]{-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE};
        data.floats = new float[]{0, 1, -1, 123456, -123456, 0.1f, 0.2f, -0.3f, (float) Math.PI, Float.MAX_VALUE, Float.MIN_VALUE};
        data.bytes = new byte[]{-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE};
        data.booleans = new boolean[]{true, false};
        data.Ints = new Integer[]{-1234567, 1234567, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        data.Shorts = new Short[]{-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE};
        data.Floats = new Float[]{0f, 1f, -1f, 123456f, -123456f, 0.1f, 0.2f, -0.3f, (float) Math.PI, Float.MAX_VALUE,
                Float.MIN_VALUE};
        data.Bytes = new Byte[]{-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE};
        data.Booleans = new Boolean[]{true, false};
    }


}
