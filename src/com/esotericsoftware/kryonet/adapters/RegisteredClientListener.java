package com.esotericsoftware.kryonet.adapters;

import com.esotericsoftware.kryonet.network.ServerConnection;
import com.esotericsoftware.kryonet.network.messages.MessageToClient;
import com.esotericsoftware.kryonet.network.messages.QueryToClient;
import org.eclipse.jdt.annotation.Nullable;

import java.util.function.BiConsumer;

/**
 * Created by Evan on 6/30/16.
 */
public class RegisteredClientListener extends RegisteredListener<ServerConnection> {



    /** Register a handler for a message type. When of message of type clazz is received,
     * the callback is invoked with the message and the connection it came from passed
     * as arguments.
     *
     * A call to this method removes any previously existing handlers for the given class type.
     *
     * Example:
     * <code>
     *     // Add a handler that prints out the content of a StringMessage to stdout.
     *     endpoint.addHandler(StringMessage.class, (msg, conn) -> System.out.println(msg.data));
     *     // Add a handler that always responds 'YES' to a Yes/No prompt
     *     endpoint.addHandler(YesNoQuery.class, (query, conn) -> query.reply(YesNoQuery.YES));
     * </code>
     *
     * @return The previously registered callback for this message type, or null if none existed.
     */
    @SuppressWarnings("unchecked")
    public <K extends MessageToClient> @Nullable BiConsumer<? super K, ? super ServerConnection>
        addHandler(Class<K> clazz, BiConsumer<? super K, ? super ServerConnection> callback){
        return map.put(clazz, callback);
    }

    /** Register a handler for a query type. When query of type clazz are received,
     * the callback is invoked with the query and the connection it came from passed
     * as arguments.
     *
     * A call to this method removes any previously existing handlers for the given query type.
     *
     * Example:
     * <code>
     *     // Add a handler that prints out the content of a StringMessage to stdout.
     *     endpoint.addHandler(StringMessage.class, (msg, conn) -> System.out.println(msg.data));
     *     // Add a handler that always responds 'YES' to a Yes/No prompt
     *     endpoint.addHandler(YesNoQuery.class, (query, conn) -> query.reply(YesNoQuery.YES));
     * </code>
     *
     * @return The previously registered callback for this message type, or null if none existed.
     */
    @SuppressWarnings("unchecked")
    public <Q extends QueryToClient<?>> @Nullable BiConsumer<? super Q, ? super ServerConnection>
        addQueryHandle(Class<Q> clazz, BiConsumer<? super Q, ? super ServerConnection> callback){
        return map.put(clazz, callback);
    }

}
