package com.cloud.resource;

import java.util.List;
import java.util.Set;

import com.cloud.utils.fsm.StateMachine;

public enum ResourceState {
    Enabled,
    Disabled,
    Unmanaged,
    PrepareForMaintenace,
    ErrorInMaintenance,
    Maintenance;
    
    public enum Action {
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
        private Action(String comment) {
            this.comment = comment;
        }
        
        public String getDescription() {
            return this.comment;
        }
    }
    
    public ResourceState getNextState(Action a) {
        return s_fsm.getNextState(this, a);
    }
    
    public ResourceState[] getFromStates(Action a) {
        List<ResourceState> from = s_fsm.getFromStates(this, a);
        return from.toArray(new ResourceState[from.size()]);
    }
    
    public Set<Action> getPossibleActions() {
        return s_fsm.getPossibleEvents(this);
    }
    
    public static String[] toString(ResourceState... states) {
        String[] strs = new String[states.length];
        for (int i=0; i<states.length; i++) {
            strs[i] = states[i].toString();
        }
        return strs;
    }
    
    protected static final StateMachine<ResourceState, Action> s_fsm = new StateMachine<ResourceState, Action>();
    static {
        s_fsm.addTransition(null, Action.InternalCreating, ResourceState.Enabled);
        s_fsm.addTransition(ResourceState.Enabled, Action.AdminEnable, ResourceState.Enabled);
        s_fsm.addTransition(ResourceState.Enabled, Action.AdminDisable, ResourceState.Disabled);
        s_fsm.addTransition(ResourceState.Enabled, Action.ClusterUnmanage, ResourceState.Unmanaged);
        s_fsm.addTransition(ResourceState.Enabled, Action.AdminAskMaintenace, ResourceState.PrepareForMaintenace);
        s_fsm.addTransition(ResourceState.Disabled, Action.AdminEnable, ResourceState.Enabled);
        s_fsm.addTransition(ResourceState.Disabled, Action.AdminDisable, ResourceState.Disabled);
        s_fsm.addTransition(ResourceState.Disabled, Action.ClusterUnmanage, ResourceState.Unmanaged);
        s_fsm.addTransition(ResourceState.Unmanaged, Action.ClusterUnmanage, ResourceState.Unmanaged);
        s_fsm.addTransition(ResourceState.Unmanaged, Action.ClusterManage, ResourceState.Disabled);
        s_fsm.addTransition(ResourceState.PrepareForMaintenace, Action.InternalEnterMaintenance, ResourceState.Maintenance);
        s_fsm.addTransition(ResourceState.PrepareForMaintenace, Action.AdminCancelMaintenance, ResourceState.Enabled);
        s_fsm.addTransition(ResourceState.Maintenance, Action.AdminCancelMaintenance, ResourceState.Enabled);
    }
}
