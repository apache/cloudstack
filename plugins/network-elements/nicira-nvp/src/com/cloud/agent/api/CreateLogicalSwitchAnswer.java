package com.cloud.agent.api;

public class CreateLogicalSwitchAnswer extends Answer {
    private String _logicalSwitchUuid;

    public CreateLogicalSwitchAnswer(Command command, boolean success,
            String details, String logicalSwitchUuid) {
        super(command, success, details);
        this._logicalSwitchUuid = logicalSwitchUuid;
    }
    
    public CreateLogicalSwitchAnswer(Command command, Exception e) {
        super(command, e);
    }

    public String getLogicalSwitchUuid() {
        return _logicalSwitchUuid;
    }

}
