package com.cloud.agent.api;

import com.cloud.resource.ResourceState;

public class PropagateResourceEventCommand extends Command {
    long hostId;
    ResourceState.Event event;
    
    protected PropagateResourceEventCommand() {
        
    }
    
    public PropagateResourceEventCommand(long hostId, ResourceState.Event event) {
        this.hostId = hostId;
        this.event = event;
    }
    
    public long getHostId() {
        return hostId;
    }
    
    public ResourceState.Event getEvent() {
        return event;
    }
    
    @Override
    public boolean executeInSequence() {
        // TODO Auto-generated method stub
        return false;
    }

}
