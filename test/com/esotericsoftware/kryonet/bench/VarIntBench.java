package com.esotericsoftware.kryonet.bench;

import com.esotericsoftware.kryonet.util.ProtocolUtils;
import java.nio.ByteBuffer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Created by Evan on 4/2/17.
 */
@State(Scope.Thread)
public class VarIntBench {

    private ByteBuffer buffer;

    @Setup
    public void init(){
        buffer = ByteBuffer.allocateDirect(1024);
    }


    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void regular() {
        buffer.putInt(1233);
        buffer.position(0);
    }


    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void onebyte1() {
        ProtocolUtils.writeInt(buffer, 127, 1);
        buffer.position(0);
    }

    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void onebyte2() {
        writeInt2(127, 1, buffer);
        buffer.position(0);
    }

    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void twobyte1() {
        ProtocolUtils.writeInt(buffer, 127, 2);
        buffer.position(0);
    }

    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void twobyte2() {
        writeInt2(127, 2, buffer);
        buffer.position(0);
    }

    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void threebyte1() {
        ProtocolUtils.writeInt(buffer, 127, 3);
        buffer.position(0);
    }

    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void threebyte2() {
        writeInt2(127, 3, buffer);
        buffer.position(0);
    }


    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void fourbyte1() {
        ProtocolUtils.writeInt(buffer, 127, 4);
        buffer.position(0);
    }

    @Benchmark
    @Measurement(iterations = 20)
    @Warmup(iterations = 20)
    public void fourbyte2() {
        writeInt2(127, 4, buffer);
        buffer.position(0);
    }



    public static void writeInt2(int data, int numByte, ByteBuffer buffer) {
        buffer.put((byte)(data & 0xFF));
        if (numByte > 1) {
            data >>>= 8;
            buffer.put((byte)(data & 0xFF));

            if (numByte > 2) {
                data >>>= 8;
                buffer.put((byte)(data & 0xFF));

                if (numByte > 3) {
                    data >>>= 8;
                    buffer.put((byte)(data & 0xFF));
                }
            }
        }
    }
}
