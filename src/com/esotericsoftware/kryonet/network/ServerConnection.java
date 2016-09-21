package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.network.messages.MessageToServer;
import com.esotericsoftware.kryonet.network.messages.QueryToServer;
import com.esotericsoftware.kryonet.util.SameThreadListener;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Created by Evan on 6/18/16.
 */
public class ServerConnection extends Connection<MessageToServer> {

    /**Send a query message to this connection and block until a reply is received.
     *
     * @return The reply sent by this connection*/
    public <Q> Optional<Q> sendAndWait(QueryToServer<Q> query) {
        return sendAndWait(query, query.getTimeout());
    }

    /**Send a query message to this connection and block until a reply is received.
     *
     * @return The reply sent by this connection*/
    public <Q> Optional<Q> sendAndWait(QueryToServer<Q> query, Duration timeout) {
        final SameThreadListener<Q> callback = new SameThreadListener<>();
        sendAsync(query, callback);

        try {
            return Optional.of(callback.waitForResult(timeout));
        } catch (TimeoutException e) {
            queries.remove(query.id);
            return Optional.empty();
        }
    }

    public <T> void sendAsync(QueryToServer<T> query, Consumer<T> callback) {
        queries.put(query.id, callback);
        sendObjectTCP(query);
    }

}
