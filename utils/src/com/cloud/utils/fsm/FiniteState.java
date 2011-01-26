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

import java.util.List;
import java.util.Set;

/**
 * Interface for a state in the finite state machine.
 *
 * @param <S> State
 * @param <E> Event
 */
public interface FiniteState<S, E> {
    /**
     * @return the state machine being used.
     */
    StateMachine<S, E> getStateMachine();
    
    /**
     * get next state based on the event.
     * @param event
     * @return next State
     */
    S getNextState(E event);
    
    /**
     * Get the states that could have traveled to the current state
     * via this event.
     * @param event
     * @return array of states
     */
    List<S> getFromStates(E event);
    
    /**
     * Get the possible events that can happen from the current state.
     * @return array of events.
     */
    Set<E> getPossibleEvents();
    String getDescription();
}
