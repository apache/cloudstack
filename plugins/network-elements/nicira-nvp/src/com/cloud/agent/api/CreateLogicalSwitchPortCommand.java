package com.cloud.agent.api;

public class CreateLogicalSwitchPortCommand extends Command {
    private String _logicalSwitchUuid;
    private String _attachmentUuid;
    private String _ownerName;
    private String _nicName;
    
    public CreateLogicalSwitchPortCommand(String logicalSwitchUuid, String attachmentUuid, String ownerName, String nicName) {
        this._logicalSwitchUuid = logicalSwitchUuid;
        this._attachmentUuid = attachmentUuid;
        this._ownerName = ownerName;
        this._nicName = nicName;
    }
    
    
    public String getLogicalSwitchUuid() {
        return _logicalSwitchUuid;
    }


    public String getAttachmentUuid() {
        return _attachmentUuid;
    }


    public String getOwnerName() {
        return _ownerName;
    }


    public String getNicName() {
        return _nicName;
    }


    @Override
    public boolean executeInSequence() {
        return false;
    }

}
