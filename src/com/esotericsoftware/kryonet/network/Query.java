package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryonet.network.messages.Message;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used to sendRaw messages that require a response.
 *
 * When an instance is initialized the result field is initially null,
 * when sent to an endpoint, the handler of the Query message should
 * generate a response of type T and call reply.
 *
 * Created by Evan on 6/16/16.
 */
public abstract class Query<T, C extends Connection> implements Message {
    private static final AtomicInteger counter = new AtomicInteger(0);

    public final int id;

    private transient C origin;


    protected Query(){
        id = counter.incrementAndGet();
    }


    /** Call this method on a received query to send back a result.
     * This method should be called exactly once per received query
     * and the response must be non-null*/
    public void reply(T response){
        Objects.requireNonNull(response, "Cannot reply to query with null response.");
        origin.sendObjectTCP(new Response<>(id, response));
    }


    /** This method determines how long the client will wait to get a response from a query before timing out.
     * If a query times out, an error condition is returned instead of a result.
     * The default implementation is 10 minutes (subject to change)
     *
     * Each user-defined query can potentially override with method to define a timeout per query type.
     */
    public Duration getTimeout(){
        return Duration.ofMinutes(10);
    }



    void setOrigin(C sender){
        if(origin != null)
            throw new KryoException("Origin is already set");
        origin = sender;
    }


    /** Returns the endpoint that sent this query.
     *
     * For incoming queries, this returns the client/server that sent the request
     * For outgoing queries this returns null;
     * */
    public C getSender(){
        return origin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Query<?, ?> query = (Query<?, ?>) o;

        return id == query.id;

    }

    @Override
    public int hashCode() {
        return id;
    }


    @Override
    public String toString(){
        return getClass().getSimpleName() + "(" + id + ")";
    }
}
