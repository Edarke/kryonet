package com.esotericsoftware.kryonet.network.cache;

import com.esotericsoftware.kryonet.network.EndPoint;

import java.nio.ByteBuffer;
/**
 * This class wraps a pre-serialized form a message.
 * Messages that are sent often can be explicitly cached before the server/client starts through a
 * CachedMessageFactory created by the server/client
 *
 * <p>See {@link EndPoint#getCachedMessageFactory()} for creating CachedMessages
 * Created by Evan on 7/14/16.
 */
public class CachedMessage<T> {


    public final ByteBuffer cached;

    private final boolean isReliable;

    CachedMessage(ByteBuffer src, boolean isReliable){
        cached = src;
        this.isReliable = isReliable;
    }


    public final boolean isReliable(){
        return isReliable;
    }
}
