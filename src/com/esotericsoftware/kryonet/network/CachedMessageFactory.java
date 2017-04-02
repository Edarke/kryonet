package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.network.messages.Message;
import com.esotericsoftware.kryonet.serializers.Serialization;
import java.nio.ByteBuffer;

/**
 * Created by Evan on 7/14/16.
 */
public class CachedMessageFactory {

    byte[] buffer;
    private int free;
    private final Serialization serializer;
    private final int maxBufferSize;


    CachedMessageFactory(Serialization serializer, int maxBufferSize){
        this.serializer = serializer;
        this.maxBufferSize = maxBufferSize;

        // This buffer can hold at least 8 pre-serialized messages, but it likely can hold many, many more.
        this.buffer = new byte[maxBufferSize * 8];
    }

    <T extends Message> CachedMessage<T> createTemp(T msg){
        synchronized (this) {
            final byte[] localBuffer = buffer;
            final int offset = free;

            ByteBuffer temp = ByteBuffer.wrap(localBuffer, offset, maxBufferSize);
            serializer.write(temp, msg);
            final int end = temp.position();

            if (end + maxBufferSize >= localBuffer.length) {
                buffer = new byte[localBuffer.length];
                free = 0;
            } else {
                free = end;
            }
            return new CachedMessage<>(localBuffer, offset, end, msg.isReliable());
        }
    }


    /**
     * Create a pre-serialized form of a message in a type safe wrapper.
     * Any time you want to send a message of type T, you can send a CachedMessage<T>
     * without the overhead of serialization. This method can be used to optimize messages
     * that are sent frequently.
     */
    public <T extends Message> CachedMessage<T> create(T msg){
        ByteBuffer buffer = ByteBuffer.allocate(maxBufferSize);
        serializer.write(buffer, msg);
        buffer.flip();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes, 0, bytes.length);

        return new CachedMessage<T>(bytes, 0, bytes.length, msg.isReliable());
    }
}
