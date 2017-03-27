package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.adapters.ConnectionAdapter;
import com.esotericsoftware.kryonet.adapters.RegisteredClientListener;
import com.esotericsoftware.kryonet.adapters.RegisteredListener;
import com.esotericsoftware.kryonet.network.messages.QueryToClient;
import com.esotericsoftware.minlog.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by Evan on 6/29/16.
 */
public class PingQueryTest extends KryoNetTestCase {

    public static class PingQuery extends QueryToClient<Long> {}

    public static final int NUM_MSG = 5000;

    public void testPingQuery() throws TimeoutException {
        RegisteredListener<ClientConnection> listener
                = new RegisteredListener<ClientConnection>(){
            @Override
            public void onConnected(ClientConnection con){
                for(int i = 0; i < NUM_MSG; ++i) {
                    long start = System.nanoTime();
                    con.sendAsync(new PingQuery()).thenAccept(time -> logPing(time, start));
                }
            }
        };
        server.addListener(listener);

        RegisteredClientListener responder = new RegisteredClientListener();
        responder.addQueryHandle(PingQuery.class, (ping, con) -> ping.reply(System.nanoTime()));
        client.addListener(new ConnectionAdapter.ThreadedListener<>(responder, Executors.newFixedThreadPool(4)));

        reg(server.getKryo(), client.getKryo(), Long.class, PingQuery.class);
        start(server, client);

        sleep(1000);
        test.await(3000);

        Log.error(String.format("Average latency was: %,d", + sum.longValue() / (NUM_MSG - 100)));
    }

    AtomicInteger count = new AtomicInteger(0);

    LongAdder sum = new LongAdder();
    AtomicInteger n = new AtomicInteger(0);
    public void logPing(Long end, long start){
        final long delta = end - start;

        Log.error(String.format("Ping completed in %,dns", delta));
        if(count.incrementAndGet() == NUM_MSG)
            test.resume();
        else if (count.get() >= 100){
            sum.add(delta);
            n.incrementAndGet();
        }
    }

}
