/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils.fsm;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StateMachine is a partial implementation of a finite state machine.
 * Specifically, it implements the Moore machine.
 * It's main purpose is to keep the state diagram in one place in code
 * so someone else can add/modify states easily without regression.
 * It doesn't implement any actions because that's generally in the
 * business logic anyways.
 *
 * @param <S> state
 * @param <E> event
 */
public class StateMachine2<S, E, V extends StateObject<S>> {
    private final HashMap<S, StateEntry> _states = new HashMap<S, StateEntry>();
    private final StateEntry _initialStateEntry;

    private List<StateListener<S,E,V>> _listeners = new ArrayList<StateListener<S,E,V>>();
    
    public StateMachine2() {
        _initialStateEntry = new StateEntry(null);

    }
    
    public void addTransition(S currentState, E event, S toState) {
        StateEntry entry = null;
        if (currentState == null) {
            entry = _initialStateEntry;
        } else {
            entry = _states.get(currentState);
            if (entry == null) {
                entry = new StateEntry(currentState);
                _states.put(currentState, entry);
            }
        }
        
        entry.addTransition(event, toState);
        
        entry = _states.get(toState);
        if (entry == null) {
            entry = new StateEntry(toState);
            _states.put(toState, entry);
        }
        entry.addFromTransition(event, currentState);
    }
    
    public Set<E> getPossibleEvents(S s) {
        StateEntry entry = _states.get(s);
        return entry.nextStates.keySet();
    }
    
    public S getNextState(S s, E e) throws NoTransitionException {
        StateEntry entry = null;
        if (s == null) {
            entry = _initialStateEntry;
        } else {
            entry = _states.get(s);
            assert entry != null : "Cannot retrieve transitions for state " + s;
        }
        
        S ns = entry.nextStates.get(e);
        if (ns == null) {
            throw new NoTransitionException("Unable to transition to a new state from " + s + " via " + e);
        }
        return ns;
    }
    
    public List<S> getFromStates(S s, E e) {
        StateEntry entry = _states.get(s);
        if (entry == null) {
            return new ArrayList<S>();
        }
        
        return entry.prevStates.get(e);
    }
    
 
    public boolean transitTo(V vo, E e, Object opaque, StateDao<S,E,V> dao) throws NoTransitionException {
    	S currentState = vo.getState();
    	S nextState = getNextState(currentState, e);

    	boolean transitionStatus = true;
    	if (nextState == null) {
    		transitionStatus = false;
    	}

    	for (StateListener<S,E, V> listener : _listeners) {
    		listener.preStateTransitionEvent(currentState, e, nextState, vo, transitionStatus, opaque);
    	}
    	
    	transitionStatus = dao.updateState(currentState, e, nextState, vo, opaque);
    	if (!transitionStatus) {
    	    return false;
    	}
    	
    	for (StateListener<S,E, V> listener : _listeners) {
            listener.postStateTransitionEvent(currentState, e, nextState, vo, transitionStatus, opaque);
        }
    	
    	return true;
    }

    public boolean registerListener(StateListener<S,E,V> listener) {
        synchronized (_listeners) {
            return _listeners.add(listener);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(1024);
        _initialStateEntry.buildString(str);
        for (StateEntry entry : _states.values()) {
            entry.buildString(str);
        }
        return str.toString();
    }
    
    private class StateEntry {
        public S state;
        public HashMap<E, S> nextStates;
        public HashMap<E, List<S>> prevStates;
        
        public StateEntry(S state) {
            this.state = state;
            nextStates = new HashMap<E, S>();
            prevStates = new HashMap<E, List<S>>();
        }
        
        public void addTransition(E e, S s) {
            assert !nextStates.containsKey(e) : "State " + getStateStr() + " already contains a transition to state " + nextStates.get(e).toString() +  " via event " + e.toString() + ".  Please revisit the rule you're adding to state " + s.toString();
            nextStates.put(e, s);
        }
        
        public void addFromTransition(E e, S s) {
            List<S> l = prevStates.get(e);
            if (l == null) {
                l = new ArrayList<S>();
                prevStates.put(e, l);
            }
            
            assert !l.contains(s) : "Already contains the from transition " + e.toString() + " from state " + s.toString() + " to " + getStateStr();
            l.add(s);
        }
        
        protected String getStateStr() {
            return state == null ? "Initial" : state.toString();
        }
        
        public void buildString(StringBuilder str) {
            str.append("State: ").append(getStateStr()).append("\n");
            for (Map.Entry<E, S> nextState : nextStates.entrySet()) {
                str.append("  --> Event: ");
                Formatter format = new Formatter();
                str.append(format.format("%-30s", nextState.getKey().toString()));
                str.append("----> State: ");
                str.append(nextState.getValue().toString());
                str.append("\n");
            }
        }
    }
}
