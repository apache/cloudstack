//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils.fsm;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specifically, it implements the Moore machine.
 * so someone else can add/modify states easily without regression.
 * business logic anyways.
 *
 * @param <S> state
 * @param <E> event
 */
public class StateMachine2<S, E, V extends StateObject<S>> {
    private final HashMap<S, StateEntry> _states = new HashMap<S, StateEntry>();
    private final StateEntry _initialStateEntry;

    private List<StateListener<S, E, V>> _listeners = new ArrayList<StateListener<S, E, V>>();

    public StateMachine2() {
        _initialStateEntry = new StateEntry(null);

    }

    public void addTransition(S currentState, E event, S toState) {
      addTransition(new Transition<S, E>(currentState, event, toState, null));
    }


    public void addTransition(Transition<S, E> transition) {
      S currentState = transition.getCurrentState();
      E event = transition.getEvent();
      S toState = transition.getToState();
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

      entry.addTransition(event, toState, transition);

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
        return getTransition(s, e).getToState();
    }

    public Transition<S, E> getTransition(S s, E e) throws NoTransitionException {
      StateEntry entry = null;
      if (s == null) {
        entry = _initialStateEntry;
      } else {
        entry = _states.get(s);
        assert entry != null : "Cannot retrieve transitions for state " + s;
      }

      Transition<S, E> transition = entry.nextStates.get(e);
      if (transition == null) {
        throw new NoTransitionException("Unable to transition to a new state from " + s + " via " + e);
      }
      return transition;
    }

    public List<S> getFromStates(S s, E e) {
        StateEntry entry = _states.get(s);
        if (entry == null) {
            return new ArrayList<S>();
        }

        return entry.prevStates.get(e);
    }

    public boolean transitTo(V vo, E e, Object opaque, StateDao<S, E, V> dao) throws NoTransitionException {
        S currentState = vo.getState();
        S nextState = getNextState(currentState, e);
        Transition<S, E> transition = getTransition(currentState, e);

        boolean transitionStatus = true;
        if (nextState == null) {
            transitionStatus = false;
        }

        for (StateListener<S, E, V> listener : _listeners) {
            listener.preStateTransitionEvent(currentState, e, nextState, vo, transitionStatus, opaque);
        }

        transitionStatus = dao.updateState(currentState, e, nextState, vo, opaque);
        if (!transitionStatus) {
            return false;
        }

        for (StateListener<S, E, V> listener : _listeners) {
            listener.postStateTransitionEvent(transition, vo, transitionStatus, opaque);
        }

        return true;
    }

    public boolean registerListener(StateListener<S, E, V> listener) {
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

    public static class Transition<S, E> {

      private S currentState;

      private E event;

      private S toState;

      private List<Impact> impacts;

      public static enum Impact {
        USAGE
      }

      public Transition(S currentState, E event, S toState, List<Impact> impacts) {
        this.currentState = currentState;
        this.event = event;
        this.toState = toState;
        this.impacts = impacts;
      }

      public S getCurrentState() {
        return currentState;
      }

      public E getEvent() {
        return event;
      }

      public S getToState() {
        return toState;
      }

      public boolean isImpacted(Impact impact) {
        if (impacts == null || impacts.isEmpty()) {
          return false;
        }
        return impacts.contains(impact);
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transition that = (Transition) o;

        if (currentState != null ? !currentState.equals(that.currentState) : that.currentState != null) return false;
        if (event != null ? !event.equals(that.event) : that.event != null) return false;
        if (toState != null ? !toState.equals(that.toState) : that.toState != null) return false;

        return true;
      }

      @Override
      public int hashCode() {
        int result = currentState != null ? currentState.hashCode() : 0;
        result = 31 * result + (event != null ? event.hashCode() : 0);
        result = 31 * result + (toState != null ? toState.hashCode() : 0);
        return result;
      }
    }

    private class StateEntry {
        public S state;
        public HashMap<E, Transition<S, E>> nextStates;
        public HashMap<E, List<S>> prevStates;

        public StateEntry(S state) {
            this.state = state;
            prevStates = new HashMap<E, List<S>>();
            nextStates = new HashMap<E, Transition<S, E>>();
        }

        public void addTransition(E e, S s, Transition<S, E> transition) {
            assert !nextStates.containsKey(e) : "State " + getStateStr() + " already contains a transition to state " + nextStates.get(e).toString() + " via event " +
                e.toString() + ".  Please revisit the rule you're adding to state " + s.toString();
            nextStates.put(e, transition);
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
            for (Map.Entry<E, Transition<S, E>> nextState : nextStates.entrySet()) {
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
