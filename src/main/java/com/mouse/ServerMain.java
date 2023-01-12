package com.mouse;

import com.jme3.app.SimpleApplication;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.system.JmeContext;
import org.dyn4j.dynamics.Force;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerMain extends SimpleApplication {
    private static Map<String, Force> forces = new HashMap<>();

    private double[][] forceArray = new double[6][2];
    private int blueScore = 0;
    private int redScore = 0;
    private int turns = 0;
    static Server myServer;

    boolean[] shotCheck = {false, false};
    boolean[] stopCheck = {false, false};
    boolean start = false;

    public static void main(String[] args) {
        ServerMain app = new ServerMain();
        app.start(JmeContext.Type.Headless); // headless type for servers!
    }

    @Override
    public void simpleInitApp() {
        try {
            myServer = Network.createServer(4234);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Serializer.registerClass(PlayerMessage.class);
        myServer.start();
        myServer.addMessageListener(new ServerListener(), PlayerMessage.class);
        myServer.addConnectionListener(new PlayerComeListener());


    }

    @Override
    public void simpleUpdate(float tpf) {
        if (!start && (myServer.getConnections().size() == 2)) {
            myServer.broadcast(new PlayerMessage("Start"));
            start = true;
        }
        if (shotCheck[0] && shotCheck[1]) {
            shotCheck[0] = false;
            shotCheck[1] = false;
            myServer.broadcast(new PlayerMessage("Run", forceArray));
        }

        if (stopCheck[0] && stopCheck[1]) {
            stopCheck[0] = false;
            stopCheck[1] = false;
            myServer.broadcast(new PlayerMessage("Round"));
        }
    }


    public class ServerListener implements MessageListener<HostedConnection> {
        public void messageReceived(HostedConnection source, Message message) {
            if (message instanceof PlayerMessage) {
                // do something with the message
                PlayerMessage Message = (PlayerMessage) message;
                String command = Message.getCommand();
                System.out.println("Server received '" + command + "' from client #" + source.getId());
                if (command.equals("Shot") && !shotCheck[source.getId()]) {
                    double[][] fa = Message.getForcearray();
                    for (int i = 0; i < 6; i++) {
                        for (int j = 0; j < 2; j++) {
                            forceArray[i][j] += fa[i][j];
                        }
                    }
                    shotCheck[source.getId()] = true;
                }

                if (command.equals("Stop")) {
                    stopCheck[source.getId()] = true;
                }
            } // else....
        }
    }

    public class PlayerComeListener implements ConnectionListener {


        @Override
        public void connectionAdded(Server server, HostedConnection conn) {
            if (myServer.getConnections().size() > 2) {
                conn.close("There is two players");
            }
            // Blue
            if (conn.getId() == 0) {
                Message message = new PlayerMessage("Blue");
                message.setReliable(true);
                System.out.println("Server send " + conn.getId() + " Blue");
                myServer.broadcast(Filters.in(conn), message);
            }
            // Red
            else if (conn.getId() == 1) {

                Message message = new PlayerMessage("Red");
                message.setReliable(true);
                System.out.println("Server send " + conn.getId() + " Red");
                myServer.broadcast(Filters.in(conn), message);
            }
        }

        @Override
        public void connectionRemoved(Server server, HostedConnection conn) {

        }
    }
}


