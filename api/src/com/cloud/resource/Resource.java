/**
 * 
 */
package com.cloud.resource;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.cloud.utils.fsm.FiniteState;
import com.cloud.utils.fsm.StateMachine;

/**
 * Indicates a resource in CloudStack.  
 * Any resource that requires an reservation and release system
 * must implement this interface.
 *
 */
public interface Resource {
    enum Event {
        ReservationRequested,
        ReleaseRequested,
        CancelRequested,
        OperationCompleted,
        OperationFailed,
    }
    
    enum State implements FiniteState<State, Event> {
        Allocated("Resource is allocated but not reserved"),
        Reserving("Resource is being reserved right now"), 
        Reserved("Resource has been reserved."),  
        Releasing("Resource is being released"), 
        Ready("Resource is ready which means it doesn't need to go through resservation");

        String _description;
        
        @Override
        public StateMachine<State, Event> getStateMachine() {
            
            // TODO Auto-generated method stub
            return null;
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
        
        private State(String description) {
            _description = description;
        }
        
        @Override
        public String getDescription() {
            return _description;
        }
        
        final static private StateMachine<State, Event> s_fsm = new StateMachine<State, Event>();
        static {
            s_fsm.addTransition(State.Allocated, Event.ReservationRequested, State.Reserving);
            s_fsm.addTransition(State.Reserving, Event.CancelRequested, State.Allocated);
            s_fsm.addTransition(State.Reserving, Event.OperationCompleted, State.Reserved);
            s_fsm.addTransition(State.Reserving, Event.OperationFailed, State.Allocated);
            s_fsm.addTransition(State.Reserved, Event.ReleaseRequested, State.Releasing);
            s_fsm.addTransition(State.Releasing, Event.OperationCompleted, State.Allocated);
            s_fsm.addTransition(State.Releasing, Event.OperationFailed, State.Reserved);
        }
    }
    
    enum ReservationStrategy {
        UserSpecified,
        Create,
        Start
    }
    
    /**
     * @return id in the CloudStack database
     */
    long getId();
    
    /**
     * @return reservation id returned by the allocation source.  This can be the
     * String version of the database id if the allocation source does not need it's 
     * own implementation of the reservation id.  This is passed back to the 
     * allocation source to release the resource.
     */
    String getReservationId();
    
    /**
     * @return unique name for the allocation source.
     */
    String getReserver();
    
    /**
     * @return the time a reservation request was made to the allocation source.
     */
    Date getUpdateTime();
    
    /**
     * @return the expected reservation interval.  -1 indicates 
     */
    int getExpectedReservationInterval();
    
    /**
     * @return the expected release interval.
     */
    int getExpectedReleaseInterval();
    
    /**
     * @return the reservation state of the resource.
     */
    State getState();
    
    ReservationStrategy getReservationStrategy();
}
