package com.mouse;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class PlayerMessage extends AbstractMessage {
    private String command;
    private double[][] forcearray = new double[6][2];


    public PlayerMessage() {
    }

    public PlayerMessage(String s) {
        command = s;
    }

    public PlayerMessage(String s, double[][] doubles) {
        command = s;
        forcearray = doubles;
    }

    public String getCommand() {
        return command;
    }

    public double[][] getForcearray() {
        return forcearray;
    }
}