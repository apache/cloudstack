/**
 * 
 */
package com.cloud.network;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.cloud.acl.ControlledEntity;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.utils.fsm.FiniteState;
import com.cloud.utils.fsm.StateMachine;

/**
 * A NetworkProfile defines the specifics of a network
 * owned by an account. 
 */
public interface Network extends ControlledEntity {
    
    public static class Service {
        
        public static final Service Vpn = new Service("Vpn", Capability.SupportedVpnTypes);
        public static final Service Dhcp = new Service("Dhcp");
        public static final Service Dns = new Service("Dns");
        public static final Service Gateway = new Service("Gateway");
        public static final Service Firewall = new Service("Firewall", Capability.PortForwarding, Capability.StaticNat, Capability.SupportedProtocols, Capability.MultipleIps, Capability.SupportedSourceNatTypes);
        public static final Service Lb = new Service("Lb", Capability.SupportedLBAlgorithms, Capability.SupportedProtocols);
        public static final Service UserData = new Service("UserData");
        
        private String name;
        private Capability[] caps;

        public Service(String name, Capability... caps) {
            this.name = name;
        }  
       
        public String getName() {
            return name;
        }
        
        public Capability[] getCapabilities() {
            return caps;
        }
    }
    
    public static class Provider {
        
        public static final Provider VirtualRouter = new Provider("VirtualRouter");
        public static final Provider DhcpServer = new Provider("DhcpServer");
        public static final Provider ExternalFirewall = new Provider("ExternalFirewall");
        public static final Provider ExternalLoadBalancer = new Provider("ExternalLoadBalancer");
        
        private String name;
        
        public Provider(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public static class Capability {
        
        public static final Capability PortForwarding = new Capability("PortForwarding");
        public static final Capability StaticNat = new Capability("StaticNat");
        public static final Capability SupportedProtocols = new Capability("SupportedProtocols");
        public static final Capability SupportedLBAlgorithms = new Capability("SupportedLbAlgorithms");
        public static final Capability MultipleIps = new Capability("MultipleIps");
        public static final Capability SupportedSourceNatTypes = new Capability("SupportedSourceNatTypes");
        public static final Capability SupportedVpnTypes = new Capability("SupportedVpnTypes");
        
        private String name;
        
        public Capability(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    enum Event {
        ImplementNetwork,
        DestroyNetwork,
        OperationSucceeded,
        OperationFailed;
    }
    
    enum State implements FiniteState<State, Event> {
        Allocated("Indicates the network configuration is in allocated but not setup"),
        Setup("Indicates the network configuration is setup"),
        Implementing("Indicates the network configuration is being implemented"),
        Implemented("Indicates the network configuration is in use"),
        Destroying("Indicates the network configuration is being destroyed");

        @Override
        public StateMachine<State, Event> getStateMachine() {
            return s_fsm;
        }

        @Override
        public State getNextState(Event event) {
            return s_fsm.getNextState(this, event);
        }

        @Override
        public List<State> getFromStates(Event event) {
            return s_fsm.getFromStates(this, event);
        }

        @Override
        public Set<Event> getPossibleEvents() {
            return s_fsm.getPossibleEvents(this);
        }
        
        String _description;
        
        @Override
        public String getDescription() {
            return _description;
        }
        
        private State(String description) {
            _description = description;
        }
        
        private static StateMachine<State, Event> s_fsm = new StateMachine<State, Event>();
        static {
            s_fsm.addTransition(State.Allocated, Event.ImplementNetwork, State.Implementing);
            s_fsm.addTransition(State.Implementing, Event.OperationSucceeded, State.Implemented);
            s_fsm.addTransition(State.Implementing, Event.OperationFailed, State.Destroying);
            s_fsm.addTransition(State.Implemented, Event.DestroyNetwork, State.Destroying);
            s_fsm.addTransition(State.Destroying, Event.OperationSucceeded, State.Allocated);
            s_fsm.addTransition(State.Destroying, Event.OperationFailed, State.Implemented);
        }
    }
    
    /**
     * @return id of the network profile.  Null means the network profile is not from the database.
     */
    long getId();
    
    String getName();

    Mode getMode();

    BroadcastDomainType getBroadcastDomainType();

    TrafficType getTrafficType();

    String getGateway();

    String getCidr();

    long getDataCenterId();
    
    long getNetworkOfferingId();
    
    State getState();
    
    long getRelated();
    
    URI getBroadcastUri();
    
    String getDns1();
    
    String getDns2();
    
    GuestIpType getGuestType();
    
    String getDisplayText();
    
    boolean isShared();
    
    String getReservationId();

}
