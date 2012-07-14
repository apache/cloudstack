package com.cloud.agent.api;

public class DeleteLogicalSwitchPortAnswer extends Answer {

    public DeleteLogicalSwitchPortAnswer(Command command, boolean success,
            String details) {
        super(command, success, details);
    }

    public DeleteLogicalSwitchPortAnswer(Command command, Exception e) {
        super(command, e);
    }

}
