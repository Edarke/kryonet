package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.network.messages.MessageToClient;
import com.esotericsoftware.kryonet.network.messages.QueryToClient;
import com.esotericsoftware.kryonet.util.SameThreadListener;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Created by Evan on 6/17/16.
 */
public class ClientConnection extends Connection<MessageToClient> {



    /**Send a query message to this connection and block until a reply is received.
     * If no reply is received within the timeout window, Optional.empty() is returned.
     *
     * @return The reply sent by this connection*/
    public <Q> Optional<Q> sendAndWait(QueryToClient<Q> query) {
        return sendAndWait(query, query.getTimeout());
    }


    /**Send a query message to this connection and block until a reply is received.
     * If no reply is received within the timeout window, Optional.empty() is returned.
     *
     * @return The reply sent by this connection*/
    public <Q> Optional<Q> sendAndWait(QueryToClient<Q> query, Duration timeout) {
        final SameThreadListener<Q> callback = new SameThreadListener<>();
        sendAsync(query, callback);

        try {
            return Optional.of(callback.waitForResult(timeout));
        } catch (TimeoutException e) {
            queries.remove(query.id);
            return Optional.empty();
        }
    }

    public <T> void sendAsync(QueryToClient<T> query, Consumer<T> callback) {
        queries.put(query.id, callback);
        sendObjectTCP(query);
    }

}
