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
public class StateMachine<S, E> {
    private final HashMap<S, StateEntry> _states = new HashMap<S, StateEntry>();
    private final StateEntry _initialStateEntry;

    public StateMachine() {
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

    public S getNextState(S s, E e) {
        StateEntry entry = null;
        if (s == null) {
            entry = _initialStateEntry;
        } else {
            entry = _states.get(s);
            assert entry != null : "Cannot retrieve transitions for state " + s.toString();
        }

        return entry.nextStates.get(e);
    }

    public List<S> getFromStates(S s, E e) {
        StateEntry entry = _states.get(s);
        if (entry == null) {
            return new ArrayList<S>();
        }

        return entry.prevStates.get(e);
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
            assert !nextStates.containsKey(e) : "State " + getStateStr() + " already contains a transition to state " + nextStates.get(e).toString() + " via event " +
                e.toString() + ".  Please revisit the rule you're adding to state " + s.toString();
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
