package com.esotericsoftware.kryonet.bench;

import com.esotericsoftware.kryonet.network.CachedMessage;
import com.esotericsoftware.kryonet.utils.StringMessage;
import com.esotericsoftware.kryonet.network.KryoNetTestCase;
import org.junit.Test;

/**
 * Created by Evan on 3/12/17.
 */
public class BufferPool extends KryoNetTestCase {

    @Test
    public void test_orig() {
        reg(this.server.getKryo(), this.client.getKryo(), StringMessage.class);
        start(this.server, this.client);

        StringMessage msg = new StringMessage("This is the ith StringMessage test message being sent to all registered clients.");
        CachedMessage<StringMessage> cmsg = this.server.getCachedMessageFactory().create(msg);
        for (int i = 0; i < 4096; ++i) {
            this.server.sendToAll(cmsg, server.getConnections());
        }

        long start = System.currentTimeMillis();
        msg = new StringMessage("This is the ith StringMessage test message being sent to all registered clients.");
        cmsg = this.server.getCachedMessageFactory().create(msg);
        for (int i = 0; i < 40096; ++i) {
            this.server.sendToAll(cmsg, server.getConnections());
        }
        System.out.printf("Time taken: %,dms\n", System.currentTimeMillis() - start);
    }



}
