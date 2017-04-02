package com.esotericsoftware.kryonet.network;

/**
 * This class wraps a pre-serialized form a message.
 * Messages that are sent often can be explicitly cached before the server/client starts through a
 * CachedMessageFactory created by the server/client
 *
 * <p>See {@link EndPoint#getCachedMessageFactory()} for creating CachedMessages
 * Created by Evan on 7/14/16.
 */
public final class CachedMessage<T> {

    final int start;
    public final byte[] cached;
    public final boolean isReliable;
    public final int length;

    CachedMessage(byte[] src, int start, int end, boolean isReliable){
        this.cached = src;
        this.start = start;
        this.length = end - start;
        this.isReliable = isReliable;
    }

}
