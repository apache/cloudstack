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

package org.apache.cloudstack.vm.bootgroup;

import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRuleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;

/**
 * Invalidates a VM's cached boot-group readiness the moment it actually comes up from a cold start
 * (Starting -&gt; Running only — not a live-migration landing in Running, nor a same-state
 * confirmation self-transition), so a VM started outside boot group orchestration can't keep
 * reporting a stale Ready left over from before it stopped. Deliberately does nothing on the way
 * down to Stopped — {@code InstanceBootGroupReadinessRuleManagerImpl}'s own state-aware reads already
 * make a stopped VM report NotReady regardless of cache, and leaving that cache row untouched is what
 * lets {@code ignoreinstancestate} keep showing the last real check result for diagnosis.
 */
public class InstanceBootGroupVmStateListener implements StateListener<State, Event, VirtualMachine> {

    protected Logger logger = LogManager.getLogger(getClass());

    private final InstanceBootGroupReadinessRuleService instanceBootGroupReadinessRuleService;

    public InstanceBootGroupVmStateListener(InstanceBootGroupReadinessRuleService instanceBootGroupReadinessRuleService) {
        this.instanceBootGroupReadinessRuleService = instanceBootGroupReadinessRuleService;
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<State, Event> transition, VirtualMachine vo, boolean status, Object opaque) {
        if (!status || transition.getCurrentState() != State.Starting || transition.getToState() != State.Running) {
            return true;
        }
        try {
            instanceBootGroupReadinessRuleService.invalidateCachedReadinessOnRestart(vo.getId());
        } catch (Exception e) {
            logger.warn("Failed to invalidate instance boot group readiness cache for {} after it started", vo, e);
        }
        return true;
    }
}
