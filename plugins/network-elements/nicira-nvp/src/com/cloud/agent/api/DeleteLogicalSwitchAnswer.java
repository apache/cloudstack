package com.cloud.agent.api;

public class DeleteLogicalSwitchAnswer extends Answer {

    public DeleteLogicalSwitchAnswer(Command command, boolean success,
            String details) {
        super(command, success, details);
    }

    public DeleteLogicalSwitchAnswer(Command command, Exception e) {
        super(command, e);
    }

}
