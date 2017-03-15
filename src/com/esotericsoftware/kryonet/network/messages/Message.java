package com.esotericsoftware.kryonet.network.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by Evan on 6/16/16.
 */
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY)
public interface Message {

    /**This method returns true if the packet should be sent over tcp,
     * and false if it should be sent over udp. The default implementation
     * is to return true so that messages are sent over tcp by default.
     *
     *
     * Usually, this would be overridden to return a true or false constant
     * to indicate whether messages of this type should be sent over tpc.
     */
    default boolean isReliable(){
        return true;
    }

}
