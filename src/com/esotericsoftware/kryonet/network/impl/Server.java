package com.esotericsoftware.kryonet.network.impl;

import com.esotericsoftware.kryonet.network.AbstractServer;
import com.esotericsoftware.kryonet.network.ClientConnection;
import com.esotericsoftware.kryonet.serializers.Serialization;

/**
 * Created by Evan on 6/9/16.
 */
public class Server extends AbstractServer<ClientConnection> {


    public Server() {
        super(ClientConnection.class);
    }


    public Server(int writeBufferSize, int objectBufferSize) {
        super(ClientConnection.class, writeBufferSize, objectBufferSize);
    }


    public Server(int writeBufferSize, int objectBufferSize, Serialization format) {
        super(ClientConnection.class, writeBufferSize, objectBufferSize, format);
    }


    @Override
    protected ClientConnection newConnection() {
        return new ClientConnection();
    }


}
