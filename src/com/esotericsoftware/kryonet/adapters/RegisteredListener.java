package com.esotericsoftware.kryonet.adapters;

import com.esotericsoftware.kryonet.network.Connection;
import com.esotericsoftware.kryonet.network.messages.Message;
import com.esotericsoftware.minlog.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Listeners of this type allow you to register callbacks for each method type allowing O(1) message handling.
 * The library provides two implementations of this class: RegisteredServerListener and RegisteredClientListener.
 * If a message is received for which there is no registered callback, the class's default handler is invoked.
 * If an exception is throw by a callback, the class's errorHandler is invoked.
 *
 * Created by Evan on 6/25/16.
 */
public abstract class RegisteredListener<C extends Connection> implements Listener<C> {

    /**This is the default handler for received messages. It logs the name of the class received */
    public static final BiConsumer<Message, Connection> DEFAULT_HANDLE =
            (m, b) -> Log.warn("Kryonet", "No handler for " + m.getClass() + " has been registered");

    /**This is a nop handler that does nothing when a message is received. No message is logged.*/
    public static final BiConsumer<Message, Connection> NO_OP = (m, b) -> {};

    protected final Map<Class<?>, BiConsumer> map = new HashMap<>();

    protected BiConsumer<? super Message, ? super C> defaultCallback = DEFAULT_HANDLE;

    protected ErrorHandler errorHandler = new DefaultErrorHandler();

    protected RegisteredListener() { }


    /**This call back is invoked when no other callback has been registered for a class.
     * This will apply to both Messages and Queries. */
    public void setDefaultHandler(BiConsumer<? super Message, ? super C> defaultHandler){
        defaultCallback = defaultHandler;
    }

    /**This is called whenever there is an uncaught exception that arises from any callback.*/
    public void setErrorHandler(ErrorHandler handler){
        errorHandler = handler;
    }


    @Override
    public void onConnected(C connection) {
        if (Log.DEBUG) Log.debug("kryonet", "Connected with " + connection);
    }

    @Override
    public void onDisconnected(C connection) {
        if (Log.DEBUG) Log.debug("kryonet", "Disconnected with " + connection);
    }

    @Override
    public void onIdle(C connection) {
        if (Log.DEBUG) Log.debug("kryonet", "Connection idle " + connection);
    }

    @Override
    @SuppressWarnings("unchecked")
    /** Invokes the callback registered for msg, or invokes the error handler if none exists.*/
    public void received(C connection, Object msg){
        try {
             map.getOrDefault(msg.getClass(), DEFAULT_HANDLE).accept(msg, connection);
        } catch (Exception e){
            errorHandler.onError((Message)msg, e);
        }
    }


    private static class DefaultErrorHandler implements ErrorHandler {
        @Override
        public void onError(Message message, Exception e) {
            Log.error("KryoNet", "An error occurred when handling message " + message, e);
        }
    }
}
