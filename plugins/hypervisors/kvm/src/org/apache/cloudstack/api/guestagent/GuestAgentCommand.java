package org.apache.cloudstack.api.guestagent;

import java.util.HashMap;

public class GuestAgentCommand {
    String execute;
    HashMap arguments;

    public GuestAgentCommand(String execute, HashMap arguments) {
        this.execute = execute;
        this.arguments = arguments;
    }

    public String getCommand() {
        return execute;
    }

    public HashMap getArguments() {
        return arguments;
    }
}
