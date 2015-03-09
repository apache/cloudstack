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

public interface StateListener<S, E, V> {
    /**
     * Event is triggered before state machine transition finished.
     * If you want to get the state of vm before state machine changed, you need to listen on this event
     * @param oldState VM's old state
     * @param event that triggered this VM state change
     * @param newState VM's new state
     * @param vo the VM instance
     * @param opaque host id
     * @return
     */
    public boolean preStateTransitionEvent(S oldState, E event, S newState, V vo, boolean status, Object opaque);

    /**
    * Event is triggered after state machine transition finished
    * @param transition The Transition fo the Event
    * @param vo the VM instance
    * @param status the state transition is allowed or not
    * @return
    */
    public boolean postStateTransitionEvent(StateMachine2.Transition<S, E> transition, V vo, boolean status, Object opaque);
}
