package com.esotericsoftware.kryonet.util;


import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import org.eclipse.jdt.annotation.Nullable;

import static com.esotericsoftware.minlog.Log.DEBUG;
import static com.esotericsoftware.minlog.Log.debug;

/**
 * Created by Evan on 6/18/16.
 */
public class ProtocolUtils {

    /**Closes the closable argument if its non-null and wakes up the selection key, if its non-null
     *
     * @return true if the closeable was not null, false otherwise.*/
    public static boolean close(@Nullable Closeable toBeClosed, @Nullable SelectionKey toBeWoken) {
        if (toBeClosed != null) {
            try {
                toBeClosed.close();
                if (toBeWoken != null)
                    toBeWoken.selector().wakeup();
            }catch (IOException ex) {
                    if (DEBUG) debug("kryonet", "Unable to close connection: " + toBeClosed, ex);
            }
            return true;
        }
        return false;
    }


    public static void writeInt(ByteBuffer buffer, int value, int numBytes) {
        switch (numBytes) {
            case 1:
                buffer.put((byte) value);
                return;
            case 2:
                buffer.putShort((short)value);
                return;
            case 3:
                buffer.put((byte)(value & 0xFF));

                value >>>= 8;
                buffer.put((byte)(value & 0xFF));

                value >>>= 8;
                buffer.put((byte)(value & 0xFF));
                return;
            case 4:
                buffer.putInt(value);
        }
    }


    public static int readInt(ByteBuffer buffer, int bytes) {
        switch (bytes) {
            case 1:
                return buffer.get();
            case 2:
                return buffer.getShort();
            case 3:
                return (buffer.get() & 0xFF) | (buffer.get() & 0xFF) << 8 | (buffer.get() & 0xFF) << 16;
            default:
                return buffer.getInt();
        }
    }

    public static void writeInt(ByteBuffer writeBuffer, int value, int numByte, int position) {
        switch (numByte) {
            case 1:
                writeBuffer.put(position, (byte) value);
                return;
            case 2:
                writeBuffer.putShort(position, (short)value);
                return;
            case 3:
                writeBuffer.put(position, (byte)(value & 0xFF));

                value >>>= 8;
                writeBuffer.put(position + 1, (byte)(value & 0xFF));

                value >>>= 8;
                writeBuffer.put(position + 2, (byte)(value & 0xFF));
                return;
            case 4:
                writeBuffer.putInt(position, value);
        }
    }
}
