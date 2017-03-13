package com.esotericsoftware.kryonet.network;

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

    final ByteBuffer cached;

    private final boolean isReliable;

    CachedMessage(ByteBuffer src, boolean isReliable){
        cached = src;
        this.isReliable = isReliable;
    }

    /** Returns true if the original message was to be sent over TCP. */
    public final boolean isReliable(){
        return isReliable;
    }
}
