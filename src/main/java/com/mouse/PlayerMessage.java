package com.mouse;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import org.dyn4j.dynamics.Force;

import java.util.Map;

@Serializable
public class PlayerMessage extends AbstractMessage {
    private String command;
    private Map<String, Force> forces;


    public PlayerMessage() {
    }

    public PlayerMessage(String s) {
        command = s;
    }

    public PlayerMessage(String s, double[][] doubles) {
        command = s;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, Force> getForces() {
        return forces;
    }
}