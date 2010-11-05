/**
 * 
 */
package com.cloud.network;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.cloud.acl.ControlledEntity;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.utils.fsm.FiniteState;
import com.cloud.utils.fsm.StateMachine;

/**
 * A NetworkProfile defines the specifics of a network
 * owned by an account. 
 */
public interface NetworkConfiguration extends ControlledEntity {
    enum Event {
        ImplementNetwork,
        DestroyNetwork;
    }
    
    enum State implements FiniteState<State, Event> {
        Allocated("Indicates the network configuration is in allocated but not setup"),
        Setup("Indicates the network configuration is setup"),
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
            s_fsm.addTransition(State.Allocated, Event.ImplementNetwork, State.Implemented);
            s_fsm.addTransition(State.Implemented, Event.DestroyNetwork, State.Destroying);
        }
        
    }
    
    
    /**
     * @return id of the network profile.  Null means the network profile is not from the database.
     */
    long getId();

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
}
