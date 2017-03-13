package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.network.impl.Client;
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.minlog.Log;

import java.util.List;

/**
 * Created by Evan on 6/28/16.
 */
public class ServerConnectionsView extends KryoNetTestCase {

    public void testServerConnectionsSize(){
        List<ClientConnection> list = server.getConnections();
        assertEquals(list.size(), 0);

        Client secondClient = new Client();
        start(server, client, secondClient);
        sleep(200);
        assertEquals(list.size(), 2);
        stopEndPoints();
    }

    public void testServerConnectionsUnmodifiable() {
        try {
            server.getConnections().clear();
        } catch (Exception e){
            Log.info("KryoTest", "Test correctly threw exception");
            return;
        }

        fail("Expected an exception to be thrown when attempting to modify connection list");
    }


    public void testServerConnectionsDisconnect(){
        Server s = new Server();
        Client one = new Client();
        Client two = new Client();
        Client three = new Client();

        List<ClientConnection> list = s.getConnections();

        start(s, one, two, three);
        sleep(200);
        assertEquals(list.size(), 3);
        one.close();
        sleep(200);
        assertEquals(list.size(), 2);
        two.close();
        sleep(200);
        assertEquals(list.size(), 1);
        three.close();
        sleep(200);
        assertEquals(list.size(), 0);
        stopEndPoints();
    }




    public void testServerConnectionsClose(){
        Server s = new Server();
        Client one = new Client();
        Client two = new Client();
        Client three = new Client();

        List<ClientConnection> list = s.getConnections();

        start(s, one, two, three);
        sleep(300);
        assertEquals(list.size(), 3);
        s.close();
        sleep(200);

        assertEquals(list.size(), 0);
        stopEndPoints();
    }

}
