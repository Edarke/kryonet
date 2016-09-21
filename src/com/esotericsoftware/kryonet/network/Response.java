package com.esotericsoftware.kryonet.network;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * This class represents a response to a query.
 * It contains a result field and the id of the query associated with it.
 *
 * It should not be accessed outside kryonet
 * Created by Evan on 8/21/16.
 */
public final class Response<T> {
    @JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT )
    public T result;
    public int id;


    /**no-arg constructor for use by serialization libraries*/
    Response() {

    }

    Response(int id, T result) {
        this.id = id;
        this.result = result;
    }

    @Override
    public String toString() {
        return "Response(" + result + ')';
    }
}
