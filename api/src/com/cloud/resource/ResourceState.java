package com.cloud.resource;

import java.util.List;
import java.util.Set;

import com.cloud.utils.fsm.StateMachine;

public enum ResourceState {
    Enabled,
    Disabled,
    Unmanaged,
    PrepareForMaintenance,
    ErrorInMaintenance,
    Maintenance;
    
    public enum Event {
        InternalCreating("Resource is creating"),
        AdminEnable("Admin enables"),
        AdminDisable("Admin disables"),
        ClusterUnmanage("Cluster is unmanaged"),
        ClusterManage("Cluster is managed"),
        AdminAskMaintenace("Admin asks to enter maintenance"),
        AdminCancelMaintenance("Admin asks to cancel maintenance"),
        InternalEnterMaintenance("Resource enters maintenance"),
        Unmanaged("Admin turns a host into umanaged state"),
        AdminAskReconnect("Admin triggers a reconnect"),
        UpdatePassword("Admin updates password of host"),
        UnableToMigrate("Management server migrates VM failed"),
        DeleteHost("Admin delete a host");
        
        private final String comment;
        private Event(String comment) {
            this.comment = comment;
        }
        
        public String getDescription() {
            return this.comment;
        }
    }
    
    public ResourceState getNextState(Event a) {
        return s_fsm.getNextState(this, a);
    }
    
    public ResourceState[] getFromStates(Event a) {
        List<ResourceState> from = s_fsm.getFromStates(this, a);
        return from.toArray(new ResourceState[from.size()]);
    }
    
    public Set<Event> getPossibleEvents() {
        return s_fsm.getPossibleEvents(this);
    }
    
    public static String[] toString(ResourceState... states) {
        String[] strs = new String[states.length];
        for (int i=0; i<states.length; i++) {
            strs[i] = states[i].toString();
        }
        return strs;
    }
    
    protected static final StateMachine<ResourceState, Event> s_fsm = new StateMachine<ResourceState, Event>();
    static {
        s_fsm.addTransition(null, Event.InternalCreating, ResourceState.Enabled);
        s_fsm.addTransition(ResourceState.Enabled, Event.AdminEnable, ResourceState.Enabled);
        s_fsm.addTransition(ResourceState.Enabled, Event.AdminDisable, ResourceState.Disabled);
        s_fsm.addTransition(ResourceState.Enabled, Event.ClusterUnmanage, ResourceState.Unmanaged);
        s_fsm.addTransition(ResourceState.Enabled, Event.AdminAskMaintenace, ResourceState.PrepareForMaintenace);
        s_fsm.addTransition(ResourceState.Disabled, Event.AdminEnable, ResourceState.Enabled);
        s_fsm.addTransition(ResourceState.Disabled, Event.AdminDisable, ResourceState.Disabled);
        s_fsm.addTransition(ResourceState.Disabled, Event.ClusterUnmanage, ResourceState.Unmanaged);
        s_fsm.addTransition(ResourceState.Unmanaged, Event.ClusterUnmanage, ResourceState.Unmanaged);
        s_fsm.addTransition(ResourceState.Unmanaged, Event.ClusterManage, ResourceState.Disabled);
        s_fsm.addTransition(ResourceState.PrepareForMaintenace, Event.InternalEnterMaintenance, ResourceState.Maintenance);
        s_fsm.addTransition(ResourceState.PrepareForMaintenace, Event.AdminCancelMaintenance, ResourceState.Enabled);
        s_fsm.addTransition(ResourceState.Maintenance, Event.AdminCancelMaintenance, ResourceState.Enabled);
    }
}
