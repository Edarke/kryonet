package com.esotericsoftware.kryonet.v2;

import com.esotericsoftware.kryonet.adapters.RegisteredClientListener;
import com.esotericsoftware.kryonet.adapters.RegisteredServerListener;
import com.esotericsoftware.kryonet.network.messages.QueryToServer;
import com.esotericsoftware.minlog.Log;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Evan on 8/21/16.
 */
public class QueryTimeOut extends KryoNetTestCase {

    public static class ShortQuery extends QueryToServer<Integer> {
        public transient Duration timeout = Duration.ofMillis(1);
        @Override
        public Duration getTimeout(){
            return timeout;
        }
    }



    @Before
    public void setUp() throws Exception {
        super.setUp();

        final int tcp = ThreadLocalRandom.current().nextInt(5000, 6000);
        final int udp = ThreadLocalRandom.current().nextInt(6000, 7000);
        server.bind(tcp, udp);


        RegisteredServerListener serverListener = new RegisteredServerListener();
        serverListener.addQueryHandle(ShortQuery.class, (query, con) -> {
            Log.info("Server Received " + query);
            sleep(10);
            query.reply(54321);
        });
        server.addListener(serverListener);



        RegisteredClientListener listener = new RegisteredClientListener();



        super.reg(server.getKryo(), client.getKryo(), ShortQuery.class);

        startEndPoint(server);


        client.addListener(listener);
        startEndPoint(client);
        client.connect(2000, "localhost", tcp, udp);
    }


    @Test
    public void testQueryTimeout(){
        assertNotNull(clientRef);

        Optional<Integer> result = client.getConnection().sendAndWait(new ShortQuery());
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void testQueryOk(){
        assertNotNull(clientRef);

        Optional<Integer> result = client.getConnection().sendAndWait(new ShortQuery(), Duration.ofMinutes(10));
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(54321, result.get().intValue());
    }

    @Test
    public void testTimeOutAtLeast(){
        assertNotNull(clientRef);
        NotShortQuery subclass = new NotShortQuery();
        subclass.timeout = Duration.ofSeconds(5);
        super.reg(client.getKryo(), server.getKryo(), subclass.getClass());

        final long startTime = System.currentTimeMillis();
        Optional<Integer> result = client.getConnection().sendAndWait(subclass);
        assertNotNull(result);
        assertFalse(result.isPresent());
        System.out.println(System.currentTimeMillis() - startTime);
        assertTrue(System.currentTimeMillis() - startTime >= 5_000);
    }


    public static class NotShortQuery extends ShortQuery {}
}
