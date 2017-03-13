package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.network.messages.MessageToServer;
import com.esotericsoftware.kryonet.network.messages.QueryToServer;
import com.esotericsoftware.kryonet.util.SameThreadListener;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

    /** Send a query to the server and invokes the given callback when the response received.
     * If no response is received or the client disconnects, no error is propagated.
     * @see #sendAsync(QueryToServer) */
    public <T> void sendAsync(QueryToServer<T> query, Consumer<T> callback) {
        queries.put(query.id, callback);
        sendObjectTCP(query);
    }

    /** Sends a query in a new thread which blocks until a response is received.
     * If the client's connection is removed no error is propagated.
     * If the client does not respond, no error is propagated.
     * see {@link CompletableFuture#get(long, TimeUnit)} to detect such cases. */
    public <T> CompletableFuture<T> sendAsync(QueryToServer<T> query) {
        CompletableFuture<T> future = new CompletableFuture<>();
        sendAsync(query, future::complete);
        return future;
    }
}
