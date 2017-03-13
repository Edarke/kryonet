package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryonet.serializers.KryoSerialization;
import com.esotericsoftware.kryonet.utils.StringMessage;
import com.esotericsoftware.minlog.Log;
import java.nio.ByteBuffer;
import junit.framework.TestCase;

/**
 * Created by Evan on 7/16/16.
 */
public class CachedMessageTest extends TestCase {

    public void testFactory(){
        StringMessage hello = new StringMessage("Hello, World! The red fox jumped over the lazy, brown dog.");

        KryoSerialization kryo = new KryoSerialization();
        kryo.getKryo().register(String.class, new DefaultSerializers.StringSerializer());
        kryo.getKryo().register(StringMessage.class);


        CachedMessageFactory factory = new CachedMessageFactory(kryo, 8192);
        final CachedMessage<StringMessage> msg = factory.create(hello);

        Object o = kryo.getKryo().readClassAndObject(new Input(new ByteBufferInputStream(msg.cached)));
        assertNotNull(o);
        assertTrue(o instanceof StringMessage);
        assertEquals(hello.msg, ((StringMessage)o).msg);

        ByteBuffer test = ByteBuffer.allocateDirect(8192*32);

        // Warm up
        final int warmup = 1000;
        for(int i = 0; i < warmup; ++i){
            kryo.write(test, hello);
        }
        for(int i = 0; i < warmup; ++i){
            test.put(msg.cached);
        }
        System.gc();



        // Benchmark
        final int count = 1000;
        final long t1 = System.nanoTime();
        for(int i = 0; i < count; ++i){
            kryo.write(test, hello);
        }
        final long t2 = System.nanoTime();
        for(int i = 0; i < count; ++i){
            test.put(msg.cached);
        }
        final long t3 = System.nanoTime();

        Log.info("Benchmark", "String was " + hello.msg.length() + " chars. Serialized object was " + msg.cached.capacity() + " bytes.");
        Log.info("Benchmark", String.format("Kryo serialization took %,dns. Cached serialization took %,dns", t2 - t1, t3 - t2));
        Log.info("Benchmark", String.format("Approximate speedup was: %.2fx", ((double)(t2 - t1)) / (t3 - t2)));
    }

}
