package com.esotericsoftware.kryonet.network.serializers;

import com.esotericsoftware.kryonet.serializers.Serialization;
import java.nio.ByteBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Evan on 4/2/17.
 */
public abstract class SerializerUnitTest {

    protected SerializerUnitTest(){

    }

    public abstract Serialization getSerializer();


    @Test
    public void serializedObject_should_be_deserializable() {
        Serialization s = getSerializer();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        final EqualityMessage original = new EqualityMessage();
        s.write(buffer, original);
        buffer.flip();
        final Object deserialized = s.read(buffer);
        assertEquals(original, deserialized);
    }




    public static class EqualityMessage {
        public int integer = 1;
        public float floater = 2f;
        public double doubler = 3d;
        public String msg = "Hello";
        public NestedObject obj = new NestedObject();
        public NestedObject nullable = null;
        public String[] args = {"arg0", "arg1", "arg2"};

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EqualityMessage that = (EqualityMessage) o;

            if (integer != that.integer) return false;
            if (Float.compare(that.floater, floater) != 0) return false;
            if (Double.compare(that.doubler, doubler) != 0) return false;
            if (msg != null ? !msg.equals(that.msg) : that.msg != null) return false;
            return obj != null ? obj.equals(that.obj) : that.obj == null;

        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = integer;
            result = 31 * result + (floater != +0.0f ? Float.floatToIntBits(floater) : 0);
            temp = Double.doubleToLongBits(doubler);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + (msg != null ? msg.hashCode() : 0);
            result = 31 * result + (obj != null ? obj.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "EqualityMessage{" +
                    "integer=" + integer +
                    ", floater=" + floater +
                    ", doubler=" + doubler +
                    ", msg='" + msg + '\'' +
                    ", obj=" + obj +
                    ", nullable=" + nullable +
                    '}';
        }

        public static class NestedObject {
            public long bigint = 5L;
            public short smallint = 6;
            public byte nibble = -7;
            public boolean james = true;


            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                NestedObject that = (NestedObject) o;

                if (bigint != that.bigint) return false;
                if (smallint != that.smallint) return false;
                if (nibble != that.nibble) return false;
                return james == that.james;

            }

            @Override
            public int hashCode() {
                int result = (int) (bigint ^ (bigint >>> 32));
                result = 31 * result + (int) smallint;
                result = 31 * result + (int) nibble;
                result = 31 * result + (james ? 1 : 0);
                return result;
            }


            @Override
            public String toString() {
                return "NestedObject{" +
                        "bigint=" + bigint +
                        ", smallint=" + smallint +
                        ", nibble=" + nibble +
                        ", james=" + james +
                        '}';
            }
        }


    }


}
