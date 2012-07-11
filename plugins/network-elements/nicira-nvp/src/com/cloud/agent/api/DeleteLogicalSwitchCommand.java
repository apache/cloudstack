package com.cloud.agent.api;

public class DeleteLogicalSwitchCommand extends Command {
    
    private String _logicalSwitchUuid;
    
    public DeleteLogicalSwitchCommand(String logicalSwitchUuid) {
        this._logicalSwitchUuid = logicalSwitchUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getLogicalSwitchUuid() {
        return _logicalSwitchUuid;
    }
}
