package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.adapters.ConnectionAdapter;
import com.esotericsoftware.kryonet.adapters.RegisteredClientListener;
import com.esotericsoftware.kryonet.adapters.RegisteredServerListener;
import com.esotericsoftware.kryonet.network.impl.Client;
import com.esotericsoftware.kryonet.utils.StringMessage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Evan on 6/28/16.
 */
public class MultiClientTest extends KryoNetTestCase {

    private final ConcurrentHashMap<ClientConnection, Boolean> clients = new ConcurrentHashMap<>();



    public static void append(StringBuffer buf, StringMessage data, CountDownLatch count){
        buf.append(data.msg);
        count.countDown();
    }

    public void testChat() throws InterruptedException {
        RegisteredServerListener serverSide = new RegisteredServerListener();
        RegisteredClientListener clientSide1 = new RegisteredClientListener();
        RegisteredClientListener clientSide2 = new RegisteredClientListener();


        server.addListener(serverSide);
        client.addListener(clientSide1);

        Client client2 = new Client();
        client2.addListener(clientSide2);

        super.reg(server.getKryo(), StringMessage.class);
        super.reg(client.getKryo(), client2.getKryo(), StringMessage.class);

        server.addListener(new ConnectionAdapter<ClientConnection>(){
            @Override
            public void onConnected(ClientConnection client){
                clients.put(client, true);
            }
        });


        List<String> toClient2 = Arrays.asList("Hello, World!", "  ", "baz",  " kryo ", "");
        List<String> toClient1 = Arrays.asList("Foo", "bar",  "fubar",  "",  " net ");

        CountDownLatch count = new CountDownLatch(toClient1.size() + toClient2.size());

        StringBuffer buffer1 = new StringBuffer();
        StringBuffer buffer2 = new StringBuffer();
        clientSide1.addHandler(StringMessage.class, (msg, server) -> append(buffer1, msg, count));
        clientSide2.addHandler(StringMessage.class, (msg, server) -> append(buffer2, msg, count));


        serverSide.addHandler(StringMessage.class, (msg, sender) -> {
            for(ClientConnection c : clients.keySet()){
                if(c != sender)
                    c.send(msg);
            }
        });

        start(server, client, client2);
        sleep(250);



        for(int i = 0; i < toClient1.size(); ++i){
            client.send(new StringMessage(toClient2.get(i)));
            client2.send(new StringMessage(toClient1.get(i)));
        }

        String client1Ref = String.join("", toClient1);
        String client2Ref = String.join("", toClient2);

        // Wait for all messages to be received
        count.await(5, TimeUnit.SECONDS);


        test.assertEquals(client1Ref, buffer1.toString());
        test.assertEquals(client2Ref, buffer2.toString());

    }


}
