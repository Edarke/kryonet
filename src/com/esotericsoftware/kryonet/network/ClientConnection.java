package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.network.messages.MessageToClient;
import com.esotericsoftware.kryonet.network.messages.QueryToClient;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Created by Evan on 6/17/16.
 */
public class ClientConnection extends Connection<MessageToClient> {


    /**
     * Send a query message to this connection and block until a reply is received.
     * The request will timeout after the duration {@link Query#getTimeout()}.
     * If no reply is received within the timeout window, Optional.empty() is returned.
     *
     * <p>To override the default timeout for a specific request, use {@link #sendAndWait(QueryToClient, Duration)}</p>
     *
     * @return The reply sent by this connection
     */
    public <Q> Optional<Q> sendAndWait(QueryToClient<Q> query) {
        return super.sendAndWait(query);
    }


    /**
     * Send a query message to this connection and block until a reply is received.
     * If no reply is received within the timeout window, Optional.empty() is returned. If the timeout duration is null,
     * this method will block indefinitely until a reply is received.
     *
     * @return The reply sent by the client, or Optional.empty if no reply was received.
     */
    public <Q> Optional<Q> sendAndWait(QueryToClient<Q> query, @Nullable Duration timeout){
        return super.sendAndWait(query, timeout);
    }

    /**
     * Sends a query message to a client and returns a future containing the response.
     * If a response is not received within the timeout duration, the future is completed with an exception
     *
     * @param query   The request to send to the client. This query should be used in no more than one request. Reusing
     *                queries may lead to unexpected behavior.
     * @param timeout After this duration passes, the returned future will be canceled. If timeout is null,
     *                The request is kept alive indefinitely.
     */
    public <T> CompletableFuture<T> sendAsync(QueryToClient<T> query, @Nullable Duration timeout) {
        return super.sendAsync(query, timeout);
    }

    /**
     * Sends a query message to a client and returns a future containing the response.
     * The query will timeout after the duration {@link Query#getTimeout()} and the returned future will be canceled
     * if no response has been received. If {@link Query#getTimeout()} returns null, the request will never timeout.
     *
     * <p>
     * The returned future will be completed exceptionally with a {@link java.util.concurrent.CancellationException} if a timeout occurs.
     * To override the default timeout for a specific request use {@link ClientConnection#sendAsync(QueryToClient, Duration)}
     * to specify a timeout duration
     *
     * @param query The request to send to the client. This query should be used in no more than one request. Reusing
     *              queries may lead to unexpected behavior.
     */
    public <T> CompletableFuture<T> sendAsync(QueryToClient<T> query) {
        return super.sendAsync(query);
    }
}
