package com.esotericsoftware.kryonet.network.util;

import com.esotericsoftware.kryonet.util.ProtocolUtils;
import java.nio.ByteBuffer;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by Evan on 4/2/17.
 */
public class ProtocolTest {


    @Test
    public void writeIntByte(){
        int value = Byte.MAX_VALUE;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        ProtocolUtils.writeInt(buffer, value, 1);
        buffer.flip();

        assertEquals(value, ProtocolUtils.readInt(buffer, 1));
    }

    @Test
    public void writeIntShort(){
        int value = Short.MAX_VALUE;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        ProtocolUtils.writeInt(buffer, value, 2);
        buffer.flip();

        assertEquals(value, ProtocolUtils.readInt(buffer, 2));
    }


    @Test
    public void writeIntTriple(){
        int value = Short.MAX_VALUE + 1;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        ProtocolUtils.writeInt(buffer, value, 3);
        buffer.flip();

        assertEquals(value, ProtocolUtils.readInt(buffer, 3));
    }

    @Test
    public void writeIntFull(){
        int value = Integer.MAX_VALUE;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        ProtocolUtils.writeInt(buffer, value, 4);
        buffer.flip();

        assertEquals(value, ProtocolUtils.readInt(buffer, 4));
    }





    //////////
    @Test
    public void writeIntByteAbs(){
        int value = Byte.MAX_VALUE;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        ProtocolUtils.writeInt(buffer, value, 1, 32);
        buffer.position(32 + 1);
        buffer.flip();
        buffer.position(32);

        assertEquals(value, ProtocolUtils.readInt(buffer, 1));
    }

    @Test
    public void writeIntShortAbs(){
        int value = Short.MAX_VALUE;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        ProtocolUtils.writeInt(buffer, value, 2, 32);
        buffer.position(32 + 2);
        buffer.flip();
        buffer.position(32);

        assertEquals(value, ProtocolUtils.readInt(buffer, 2));
    }


    @Test
    public void writeIntTripleAbs(){
        int value = Short.MAX_VALUE + 1;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        ProtocolUtils.writeInt(buffer, value, 3, 32);
        buffer.position(32 + 3);
        buffer.flip();
        buffer.position(32);

        assertEquals(value, ProtocolUtils.readInt(buffer, 3));
    }

    @Test
    public void writeIntFullAbs(){
        int value = Integer.MAX_VALUE;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        ProtocolUtils.writeInt(buffer, value, 4, 32);
        buffer.position(32 + 4);
        buffer.flip();
        buffer.position(32);

        assertEquals(value, ProtocolUtils.readInt(buffer, 4));
    }

}
