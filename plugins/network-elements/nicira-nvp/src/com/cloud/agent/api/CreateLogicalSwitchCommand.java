package com.cloud.agent.api;

public class CreateLogicalSwitchCommand extends Command {
    
    private String _transportUuid;
    private String _transportType;
    private String _name;
    private String _ownerName;

    public CreateLogicalSwitchCommand(String transportUuid, String transportType, String name, String ownerName) {
        this._transportUuid = transportUuid;
        this._transportType = transportType;
        this._name = name;
        this._ownerName = ownerName;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getTransportUuid() {
        return _transportUuid;
    }

    public String getTransportType() {
        return _transportType;
    }

    public String getName() {
        return _name;
    }

    public String getOwnerName() {
        return _ownerName;
    }

}
