package com.cloud.agent.api;

public class DeleteLogicalSwitchPortCommand extends Command {
    private String _logicalSwitchUuid;
    private String _logicalSwithPortUuid;
    
    public DeleteLogicalSwitchPortCommand(String logicalSwitchUuid, String logicalSwitchPortUuid) {
        this._logicalSwitchUuid = logicalSwitchUuid;
        this._logicalSwithPortUuid = logicalSwitchPortUuid;
    }
    
    public String getLogicalSwitchUuid() {
        return _logicalSwitchUuid;
    }
    
    public String getLogicalSwitchPortUuid() {
        return _logicalSwithPortUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
