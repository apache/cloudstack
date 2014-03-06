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

import java.util.List;
import java.util.Set;

/**
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
