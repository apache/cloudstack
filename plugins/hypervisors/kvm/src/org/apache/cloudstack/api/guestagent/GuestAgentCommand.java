package org.apache.cloudstack.api.guestagent;

import java.util.HashMap;

public class GuestAgentCommand {
    String execute;
    @SuppressWarnings("rawtypes")
    HashMap arguments;

    @SuppressWarnings("rawtypes")
    public GuestAgentCommand(String execute, HashMap arguments) {
        this.execute = execute;
        this.arguments = arguments;
    }

    public String getCommand() {
        return execute;
    }

    @SuppressWarnings("rawtypes")
    public HashMap getArguments() {
        return arguments;
    }
}
