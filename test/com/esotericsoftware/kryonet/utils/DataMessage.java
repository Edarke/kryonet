package com.esotericsoftware.kryonet.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryonet.network.messages.BidirectionalMessage;
import com.esotericsoftware.kryonet.network.PingTest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Evan on 6/19/16.
 */
public class DataMessage implements BidirectionalMessage {


    public String string;
    public String[] strings;
    public int[] ints;
    public short[] shorts;
    public float[] floats;
    public byte[] bytes;
    public boolean[] booleans;
    public Integer[] Ints;
    public Short[] Shorts;
    public Float[] Floats;
    public Byte[] Bytes;
    public Boolean[] Booleans;
    public boolean isTCP;

    public long[] longs;
    public Long[] Longs;

    public double[] doubles;
    public Double[] Doubles;
    public char[] chars;





    private static String randString(int length){
        byte[] rstring = new byte[length];
        ThreadLocalRandom.current().nextBytes(rstring);
        return new String(rstring, StandardCharsets.UTF_8);
    }


    public static DataMessage random(){
        DataMessage msg = new DataMessage();
        msg.bytes = new byte[ThreadLocalRandom.current().nextInt(0, 8)];
        ThreadLocalRandom.current().nextBytes(msg.bytes);

        msg.ints = ThreadLocalRandom.current().ints().limit(ThreadLocalRandom.current().nextInt(0, 4)).toArray();

        msg.string = randString(12);
        msg.strings = new String[]{randString(0), randString(1), randString(8)};


        msg.booleans = new boolean[]{ThreadLocalRandom.current().nextBoolean(), ThreadLocalRandom.current().nextBoolean(), ThreadLocalRandom.current().nextBoolean()};
        msg.Booleans = new Boolean[]{ThreadLocalRandom.current().nextBoolean(), ThreadLocalRandom.current().nextBoolean(), ThreadLocalRandom.current().nextBoolean()};
        msg.shorts = new short[] { (short) ThreadLocalRandom.current().nextInt(), (short) ThreadLocalRandom.current().nextInt()};
        msg.Shorts = new Short[] { (short) ThreadLocalRandom.current().nextInt(), (short) ThreadLocalRandom.current().nextInt()};

        msg.doubles = ThreadLocalRandom.current().doubles().limit(ThreadLocalRandom.current().nextInt(1,10)).toArray();
        msg.Doubles = ThreadLocalRandom.current().doubles().limit(ThreadLocalRandom.current().nextInt(1,10)).boxed().toArray(Double[]::new);
        msg.chars = "Hello Word, Random String".toCharArray();

        msg.longs = ThreadLocalRandom.current().longs().limit(ThreadLocalRandom.current().nextInt(1, 10)).toArray();
        msg.Longs = ThreadLocalRandom.current().longs().limit(ThreadLocalRandom.current().nextInt(1, 10)).boxed().toArray(Long[]::new);

        msg.floats = new float[] { (float) ThreadLocalRandom.current().nextDouble(), (float) ThreadLocalRandom.current().nextDouble()};
        msg.Floats = new Float[] { (float) ThreadLocalRandom.current().nextDouble(), (float) ThreadLocalRandom.current().nextDouble()};

        msg.Bytes = new Byte[] { (byte) ThreadLocalRandom.current().nextInt(), (byte) ThreadLocalRandom.current().nextInt()};

        return msg;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataMessage that = (DataMessage) o;

        if (isTCP != that.isTCP) return false;
        if (string != null ? !string.equals(that.string) : that.string != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(strings, that.strings)) return false;
        if (!Arrays.equals(ints, that.ints)) return false;
        if (!Arrays.equals(shorts, that.shorts)) return false;
        if (!Arrays.equals(floats, that.floats)) return false;
        if (!Arrays.equals(bytes, that.bytes)) return false;
        if (!Arrays.equals(booleans, that.booleans)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(Ints, that.Ints)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(Shorts, that.Shorts)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(Floats, that.Floats)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(Bytes, that.Bytes)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(Booleans, that.Booleans);

    }

    @Override
    public int hashCode() {
        int result = string != null ? string.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(strings);
        result = 31 * result + Arrays.hashCode(ints);
        result = 31 * result + Arrays.hashCode(shorts);
        result = 31 * result + Arrays.hashCode(floats);
        result = 31 * result + Arrays.hashCode(bytes);
        result = 31 * result + Arrays.hashCode(booleans);
        result = 31 * result + Arrays.hashCode(Ints);
        result = 31 * result + Arrays.hashCode(Shorts);
        result = 31 * result + Arrays.hashCode(Floats);
        result = 31 * result + Arrays.hashCode(Bytes);
        result = 31 * result + Arrays.hashCode(Booleans);
        result = 31 * result + (isTCP ? 1 : 0);
        return result;
    }

    public String toString() {
        return "Data";
    }



    private static <T> void registerArray(Kryo kryo, Class<T> tag){
        kryo.register(tag, new DefaultArraySerializers.ObjectArraySerializer(kryo, tag));
    }


    public static void reg(Kryo... kryos) {
        for(Kryo kryo : kryos){
            kryo.register(Integer.class);
            kryo.register(Byte.class);
            kryo.register(Short.class);
            kryo.register(Float.class);
            kryo.register(Double.class);
            kryo.register(Boolean.class);
            kryo.register(Long.class);
            kryo.register(Double.class);

            registerArray(kryo, Integer[].class);
            registerArray(kryo, Byte[].class);
            registerArray(kryo, Short[].class);
            registerArray(kryo, Float[].class);
            registerArray(kryo, Double[].class);
            registerArray(kryo, Boolean[].class);
            registerArray(kryo, Long[].class);
            registerArray(kryo, Double[].class);

            kryo.register(boolean[].class, new DefaultArraySerializers.BooleanArraySerializer());
            kryo.register(int[].class, new DefaultArraySerializers.IntArraySerializer());
            kryo.register(byte[].class, new DefaultArraySerializers.ByteArraySerializer());
            kryo.register(float[].class, new DefaultArraySerializers.FloatArraySerializer());
            kryo.register(double[].class, new DefaultArraySerializers.DoubleArraySerializer());
            kryo.register(short[].class, new DefaultArraySerializers.ShortArraySerializer());
            kryo.register(char[].class, new DefaultArraySerializers.CharArraySerializer());
            kryo.register(long[].class, new DefaultArraySerializers.LongArraySerializer());
            kryo.register(double[].class, new DefaultArraySerializers.DoubleArraySerializer());

            kryo.register(PingTest.Ping2.class);

            kryo.register(String.class, new DefaultSerializers.StringSerializer());
            kryo.register(String[].class, new DefaultArraySerializers.StringArraySerializer());

            kryo.register(DataMessage.class);
        }

    }
}
