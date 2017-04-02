package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.adapters.RegisteredClientListener;
import com.esotericsoftware.kryonet.adapters.RegisteredServerListener;
import com.esotericsoftware.kryonet.utils.YesNoQuery;
import com.esotericsoftware.kryonet.utils.YesNoServerQuery;
import com.esotericsoftware.minlog.Log;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Evan on 6/25/16.
 */
public class QueryTest extends KryoNetTestCase {


    @Before
    public void setUp() throws Exception {
        super.setUp();
        Log.debug("QueryTest");

        final int tcp = ThreadLocalRandom.current().nextInt(5000, 6000);
        final int udp = ThreadLocalRandom.current().nextInt(6000, 7000);
        server.bind(tcp, udp);


        RegisteredServerListener serverListener = new RegisteredServerListener();
        serverListener.addQueryHandle(YesNoServerQuery.class, (query, con) -> {
            Log.info("Server Received " + query);
            query.reply(false);
        });
        server.addListener(serverListener);



        RegisteredClientListener listener = new RegisteredClientListener();
        listener.addQueryHandle(YesNoQuery.class, (query, con) -> {
            Log.info("AbstractClient Received " + query);
            query.reply(true);
        });




        register(server.getKryo());
        register(client.getKryo());


        startEndPoint(server);


        client.addListener(listener);
        startEndPoint(client);
        client.connect(2000, "localhost", tcp, udp);

        while(clientRef == null){
            sleep(15);
        }
    }


    private static void register(Kryo kryo){
        kryo.register(Boolean.class);
        kryo.register(YesNoQuery.class);
        kryo.register(YesNoServerQuery.class);
    }



    @Test
    public void testQueryToClient(){
        assertNotNull(clientRef);

        Optional<Boolean> result = clientRef.sendAndWait(new YesNoQuery(), Duration.ofMinutes(1));
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertTrue(result.get());
    }




    @Test
    public void testQueryToServer(){
        Optional<Boolean> result = client.getConnection().sendAndWait(new YesNoServerQuery(), Duration.ofMinutes(1));
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertFalse(result.get());
    }
}
