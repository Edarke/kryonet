package com.esotericsoftware.kryonet.bench;

import com.esotericsoftware.kryo.serializers.DefaultArraySerializers;
import com.esotericsoftware.kryonet.network.CachedMessage;
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.kryonet.network.messages.BidirectionalMessage;
import com.esotericsoftware.kryonet.serializers.KryoSerialization;
import java.io.IOException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Created by Evan on 4/1/17.
 */
@State(Scope.Thread)
public class CompressionBench {

    private KryoSerialization normal = new KryoSerialization();
    private KryoSerialization deflate = normal;

    private Server normalServer = new Server(Server.DEFAULT_WRITE_BUFFER, Server.DEFAULT_OBJ_BUFFER, normal);
    private Server deflateServer = new Server(Server.DEFAULT_WRITE_BUFFER, Server.DEFAULT_OBJ_BUFFER, deflate);


    private BigMessage msg = new BigMessage();

    @Setup
    public void init() throws IOException {
        normal.getKryo().register(BigMessage.class);
        normal.getKryo().register(int[].class, new DefaultArraySerializers.IntArraySerializer());
        normal.getKryo().register(String[].class, new DefaultArraySerializers.StringArraySerializer());

        deflate.getKryo().register(BigMessage.class);
        deflate.getKryo().register(int[].class, new DefaultArraySerializers.IntArraySerializer());
        deflate.getKryo().register(String[].class, new DefaultArraySerializers.StringArraySerializer());

        CachedMessage<BigMessage> output = normalServer.getCachedMessageFactory().create(msg);
        CachedMessage<BigMessage> deflated = deflateServer.getCachedMessageFactory().create(msg);

        System.err.println("Normal size is " + output.length);
        System.err.println("Deflate size is " + deflated.length);
        System.err.println("Normal = " + new String(output.cached) + " Deflated = " + new String(deflated.cached));
    }

//    @Benchmark
//    @Measurement(iterations = 50)
//    @Warmup(iterations = 50)
//    public void normal() {
//        normalServer.sendToAll(msg);
//    }


    @Benchmark
    @Measurement(iterations = 50)
    @Warmup(iterations = 50)
    public void deflate() {
        deflateServer.sendToAll(msg);
    }


    public static class BigMessage implements BidirectionalMessage {
        public int[] arr = {1, 0, 0, 4};
        public String[] types = {"this", "is", "just", "an", "example"};
        public String name = "John Doe";
        public boolean hasItem = true;
    }


}
