package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.network.messages.Message;
import com.esotericsoftware.kryonet.serializers.Serialization;

import java.nio.ByteBuffer;

/**
 * Created by Evan on 7/14/16.
 */
public class CachedMessageFactory {

    private final Serialization serializer;
    private final int maxBufferSize;


    CachedMessageFactory(Serialization serializer, int maxBufferSize){
        this.serializer = serializer;
        this.maxBufferSize = maxBufferSize;
    }

    /** Create a pre-serialized form of a message in a type safe wrapper.
     * Any time you want to send a message of type T, you can send a CachedMessage<T>
     * without the overhead of serialization.
     *
     * This method is similar to {@link #create(Message)} but creates a minimally sized buffer at the cost of
     * extra computational cost. This method may be more appropriate than {@link #create(Message)} for long lived objects
     * created when the server starts.*/
    public <T extends Message> CachedMessage<T> createMinimal(T msg){
        ByteBuffer buffer = ByteBuffer.allocate(maxBufferSize);
        buffer.clear();
        serializer.write(buffer, msg);
        ByteBuffer cache = ByteBuffer.allocate(buffer.position()+1);

        buffer.flip();
        cache.put(buffer);
        cache.flip();
        return new CachedMessage<>(cache, msg.isReliable());
    }

    /** Create a pre-serialized form of a message in a type safe wrapper.
     * Any time you want to send a message of type T, you can send a CachedMessage<T>
     * without the overhead of serialization. */
    public <T extends Message> CachedMessage<T> create(T msg){
        ByteBuffer cache = ByteBuffer.allocate(maxBufferSize);
        serializer.write(cache, msg);
        cache.flip();
        return new CachedMessage<>(cache, msg.isReliable());
    }
}
