package com.cloud.agent.api;

public class CreateLogicalSwitchPortAnswer extends Answer {
    private String _logicalSwitchPortUuid;

    public CreateLogicalSwitchPortAnswer(Command command, boolean success,
            String details, String localSwitchPortUuid) {
        super(command, success, details);
        this._logicalSwitchPortUuid = localSwitchPortUuid;
    }
    
    public String getLogicalSwitchPortUuid() {
        return _logicalSwitchPortUuid;
    }

    public CreateLogicalSwitchPortAnswer(Command command, Exception e) {
        super(command, e);
    }
    
}
