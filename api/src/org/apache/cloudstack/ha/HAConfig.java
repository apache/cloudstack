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

package org.apache.cloudstack.ha;

import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

public interface HAConfig extends StateObject<HAConfig.HAState>, InternalIdentity {

    long getResourceId();
    HAResource.ResourceType getResourceType();
    boolean isEnabled();
    HAState getState();
    String getHaProvider();
    Long getManagementServerId();

    enum Event {
        Eligible,
        Ineligible,
        Disabled,
        Enabled,
        HealthCheckPassed,
        HealthCheckFailed,
        PerformActivityCheck,
        TooFewActivityCheckSamples,
        PeriodicRecheckResourceActivity,
        ActivityCheckFailureOverThresholdRatio,
        ActivityCheckFailureUnderThresholdRatio,
        PowerCycle,
        Recovered,
        RetryRecovery,
        RecoveryWaitPeriodTimeout,
        RecoveryOperationThresholdExceeded,
        RetryFencing,
        Fenced;

        public Long getServerId() {
            // TODO: change in future if we've better claim & ownership
            // Right now the first one to update the db wins
            // and mgmt server id would eventually become consistent
            return ManagementServerNode.getManagementServerId();
        }
    }

    enum HAState {
        Disabled("HA Operations disabled"),
        Available("The resource is healthy"),
        Ineligible("The current state does not support HA/recovery"),
        Suspect("Most recent health check failed"),
        Degraded("The resource cannot be managed, but services end user requests"),
        Checking("The activity checks are currently being performed"),
        Recovering("The resource is undergoing recovery operation"),
        Recovered("The resource is recovered"),
        Fencing("The resource is undergoing fence operation"),
        Fenced("The resource is fenced");

        String description;

        HAState(String description) {
            this.description = description;
        }

        public static StateMachine2<HAState, Event, HAConfig> getStateMachine() {
            return FSM;
        }

        public String getDescription() {
            return description;
        }

        private static final StateMachine2<HAState, Event, HAConfig> FSM = new StateMachine2<>();

        static {
            FSM.addInitialTransition(Event.Disabled, Disabled);
            FSM.addInitialTransition(Event.Enabled, Available);
            FSM.addInitialTransition(Event.Ineligible, Ineligible);

            FSM.addTransition(Disabled, Event.Enabled, Available);

            FSM.addTransition(Ineligible, Event.Disabled, Disabled);
            FSM.addTransition(Ineligible, Event.Ineligible, Ineligible);
            FSM.addTransition(Ineligible, Event.Eligible, Available);

            FSM.addTransition(Available, Event.Disabled, Disabled);
            FSM.addTransition(Available, Event.Ineligible, Ineligible);
            FSM.addTransition(Available, Event.HealthCheckPassed, Available);
            FSM.addTransition(Available, Event.HealthCheckFailed, Suspect);

            FSM.addTransition(Suspect, Event.Disabled, Disabled);
            FSM.addTransition(Suspect, Event.Ineligible, Ineligible);
            FSM.addTransition(Suspect, Event.HealthCheckFailed, Suspect);
            FSM.addTransition(Suspect, Event.PerformActivityCheck, Checking);
            FSM.addTransition(Suspect, Event.HealthCheckPassed, Available);

            FSM.addTransition(Checking, Event.Disabled, Disabled);
            FSM.addTransition(Checking, Event.Ineligible, Ineligible);
            FSM.addTransition(Checking, Event.TooFewActivityCheckSamples, Suspect);
            FSM.addTransition(Checking, Event.ActivityCheckFailureUnderThresholdRatio, Degraded);
            FSM.addTransition(Checking, Event.ActivityCheckFailureOverThresholdRatio, Recovering);

            FSM.addTransition(Degraded, Event.Disabled, Disabled);
            FSM.addTransition(Degraded, Event.Ineligible, Ineligible);
            FSM.addTransition(Degraded, Event.HealthCheckFailed, Degraded);
            FSM.addTransition(Degraded, Event.HealthCheckPassed, Available);
            FSM.addTransition(Degraded, Event.PeriodicRecheckResourceActivity, Suspect);

            FSM.addTransition(Recovering, Event.Disabled, Disabled);
            FSM.addTransition(Recovering, Event.Ineligible, Ineligible);
            FSM.addTransition(Recovering, Event.RetryRecovery, Recovering);
            FSM.addTransition(Recovering, Event.Recovered, Recovered);
            FSM.addTransition(Recovering, Event.RecoveryOperationThresholdExceeded, Fencing);

            FSM.addTransition(Recovered, Event.Disabled, Disabled);
            FSM.addTransition(Recovered, Event.Ineligible, Ineligible);
            FSM.addTransition(Recovered, Event.RecoveryWaitPeriodTimeout, Available);

            FSM.addTransition(Fencing, Event.Disabled, Disabled);
            FSM.addTransition(Fencing, Event.Ineligible, Ineligible);
            FSM.addTransition(Fencing, Event.RetryFencing, Fencing);
            FSM.addTransition(Fencing, Event.Fenced, Fenced);

            FSM.addTransition(Fenced, Event.Disabled, Disabled);
            FSM.addTransition(Fenced, Event.HealthCheckPassed, Ineligible);
            FSM.addTransition(Fenced, Event.HealthCheckFailed, Fenced);
        }
    }
}
