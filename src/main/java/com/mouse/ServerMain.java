package com.mouse;

import com.jme3.app.SimpleApplication;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.system.JmeContext;

import java.io.IOException;

public class ServerMain extends SimpleApplication {

    private final double[][] forceArray = new double[6][2];

    static Server myServer;

    boolean[] shotCheck = {false, false};
    boolean[] stopCheck = {false, false};
    boolean[] goalCheck = {false, false};
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
            Message message = new PlayerMessage("Start");
            message.setReliable(true);
            myServer.broadcast(message);
            start = true;
        }
        if (shotCheck[0] && shotCheck[1]) {
            shotCheck[0] = false;
            shotCheck[1] = false;
            Message message = new PlayerMessage("Run", forceArray);
            message.setReliable(true);
            myServer.broadcast(message);
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 2; j++) {
                    forceArray[i][j] = 0;
                }
            }
        }

        if (stopCheck[0] && stopCheck[1]) {
            stopCheck[0] = false;
            stopCheck[1] = false;
            Message message = new PlayerMessage("Round");
            message.setReliable(true);
            myServer.broadcast(message);
        }

        if (goalCheck[0] && goalCheck[1]) {
            goalCheck[0] = false;
            goalCheck[1] = false;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Message message = new PlayerMessage("Reset");
            message.setReliable(true);
            myServer.broadcast(message);
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
                if (command.startsWith("Score")) {
                    goalCheck[source.getId()] = true;
                }
            } // else....
        }
    }

    public static class PlayerComeListener implements ConnectionListener {


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
            myServer.close();
        }
    }
}


