package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.adapters.RegisteredClientListener;
import com.esotericsoftware.kryonet.adapters.RegisteredServerListener;
import com.esotericsoftware.kryonet.network.messages.QueryToServer;
import com.esotericsoftware.minlog.Log;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Before;
import org.junit.Test;

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


        RegisteredServerListener serverListener = new RegisteredServerListener(){
            @Override
            public void onConnected(ClientConnection c) {
                System.err.println("Got connection!");
            }
        };
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
        while(clientRef == null) {
            sleep(20);
        }
    }


    @Test
    public void testQueryTimeout_NoResponse(){
        assertNotNull(clientRef);
        Optional<Integer> result = client.getConnection().sendAndWait(new ShortQuery());
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void testQueryTimeout_Response(){
        assertNotNull(clientRef);
        ShortQuery query = new ShortQuery();
        query.timeout = Duration.ofDays(1);
        Optional<Integer> result = client.getConnection().sendAndWait(query);
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    public void testQueryFutureTimeoutDefault(){
        assertNotNull(clientRef);
        ShortQuery query = new ShortQuery();
        query.timeout = Duration.ofMillis(1);
        CompletableFuture<Integer> result = client.getConnection().sendAsync(query);
        assertNotNull(result);
        sleep(100);
        assertTrue(result.isCompletedExceptionally());
    }


    @Test
    public void testQueryFutureTimeout(){
        assertNotNull(clientRef);
        ShortQuery query = new ShortQuery();
        CompletableFuture<Integer> result = client.getConnection().sendAsync(query, Duration.ofMillis(1));
        assertNotNull(result);
        sleep(100);
        assertTrue(result.isCompletedExceptionally());
    }


    @Test
    public void testQueryFutureCompletion() throws ExecutionException, InterruptedException {
        System.err.println(clientRef);
        assertNotNull(clientRef);
        ShortQuery query = new ShortQuery();
        query.timeout = Duration.ofMinutes(10);
        CompletableFuture<Integer> result = client.getConnection().sendAsync(query);
        assertNotNull(result);
        sleep(100);
        assertTrue(result.isDone());
        assertEquals(54321, result.get().intValue());
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
