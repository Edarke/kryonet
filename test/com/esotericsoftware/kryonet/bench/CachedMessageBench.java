package com.esotericsoftware.kryonet.bench;

import com.esotericsoftware.kryonet.adapters.RegisteredClientListener;
import com.esotericsoftware.kryonet.network.CachedMessage;
import com.esotericsoftware.kryonet.network.impl.Client;
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.kryonet.network.messages.BidirectionalMessage;
import com.esotericsoftware.kryonet.serializers.KryoSerialization;
import com.esotericsoftware.kryonet.utils.DataMessage;
import com.esotericsoftware.kryonet.utils.StringMessage;
import com.esotericsoftware.minlog.Log;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Created by Evan on 3/12/17.
 */
@State(Scope.Thread)
public class CachedMessageBench {

    private Server server;
    private Client client;
    private TestMessage msg;
    private CachedMessage<TestMessage> cached;

    @Setup
    public void init() throws IOException {
        final int tcp = ThreadLocalRandom.current().nextInt(10_000, 20_000);
        final int udp = ThreadLocalRandom.current().nextInt(10_000, 20_000);

        server = new Server(Server.DEFAULT_WRITE_BUFFER, Server.DEFAULT_OBJ_BUFFER, new KryoSerialization());

        Client[] clients = new Client[8];
        for (int i = 0; i < clients.length; ++i) {
            clients[i] = new Client(Server.DEFAULT_WRITE_BUFFER, Server.DEFAULT_OBJ_BUFFER, new KryoSerialization());
            clients[i].start();
            clients[i].getKryo().register(TestMessage.class);
            clients[i].getKryo().register(DataMessage.class);
            clients[i].getKryo().register(StringMessage.class);

        }
        client = clients[0];

        server.start();


        RegisteredClientListener listener = new RegisteredClientListener();
        listener.addHandler(TestMessage.class, RegisteredClientListener.NO_OP);
        // listener.addHandler(TestMessage.class, (msg, server) -> System.err.println(msg));

        client.addListener(listener);


        server.bind(tcp, udp);
        for (Client c: clients)
            c.connect(5_000, "localhost", tcp, udp);
        server.getKryo().register(TestMessage.class);
        server.getKryo().register(DataMessage.class);
        server.getKryo().register(StringMessage.class);
        msg = TestMessage.create();
        cached = client.getCachedMessageFactory().create(TestMessage.create());

        Log.ERROR();
    }

    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public int tcp_cached_saved() {
        return client.send(cached);
    }

    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public int tcp_create_orig() {
        TestMessage msg = TestMessage.create();
        return client.send(msg);
    }

    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public int tcp_create_cached(){
        TestMessage msg = TestMessage.create();
        CachedMessage<TestMessage> cmsg = this.client.getCachedMessageFactory().create(msg);
        return client.send(cmsg);
    }

    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public int tcp_orig_saved() {
        return client.send(msg);
    }


    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public void tcp_sendAll_orig() {
        server.sendToAll(msg);
    }


    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public void udp_sendAll_cached() {
        server.sendToAllUDP(cached);
    }

    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public void udp_sendAll_base() {
        server.sendToOrigUDP(msg);
    }


    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public int udp_send_cached() {
        return client.sendUDP(cached);
    }



    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public int udp_send_orig() {
        return client.sendUDP(msg);
    }


    public static class TestMessage implements BidirectionalMessage {
        public String text;
        public int num;
        public double pi;
        public boolean truth;

        public static TestMessage create() {
            TestMessage msg = new TestMessage();
            byte[] chars = new byte[16];
            ThreadLocalRandom.current().nextBytes(chars);
            msg.text = new String(chars);
            msg.num = ThreadLocalRandom.current().nextInt();
            msg.pi = ThreadLocalRandom.current().nextDouble();
            msg.truth = ThreadLocalRandom.current().nextBoolean();
            return msg;
        }

        @Override
        public String toString() {
            return String.format("TestMessage(%s, %s, %s, %s)", text, num, pi, truth);
        }
    }

}
