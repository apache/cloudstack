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

package org.apache.cloudstack.ha;

import com.cloud.api.response.SimulatorHAStateResponse;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HAFenceException;
import org.apache.cloudstack.ha.provider.HAProvider;
import org.apache.cloudstack.ha.provider.HARecoveryException;
import org.apache.cloudstack.ha.provider.host.HAAbstractHostProvider;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimulatorHAProvider extends HAAbstractHostProvider implements HAProvider<Host>, StateListener<HAConfig.HAState, HAConfig.Event, HAConfig> {

    @Inject
    private HAManager haManager;

    private final Map<Long, SimulatorHAState> hostHAStateMap = new ConcurrentHashMap<>();

    public SimulatorHAProvider() {
        HAConfig.HAState.getStateMachine().registerListener(this);
    }

    public void setHAStateForHost(final Long hostId, final SimulatorHAState state) {
        hostHAStateMap.put(hostId, state);
        haManager.purgeHACounter(hostId, HAResource.ResourceType.Host);
    }

    public List<SimulatorHAStateResponse> listHAStateTransitions(final Long hostId) {
        final SimulatorHAState haState = hostHAStateMap.get(hostId);
        if (haState == null) {
            return Collections.emptyList();
        }
        return haState.listRecentStateTransitions();
    }

    @Override
    public HAResource.ResourceType resourceType() {
        return HAResource.ResourceType.Host;
    }

    @Override
    public HAResource.ResourceSubType resourceSubType() {
        return HAResource.ResourceSubType.Simulator;
    }

    @Override
    public boolean isEligible(final Host host) {
        if (host == null) {
            return false;
        }
        final SimulatorHAState haState = hostHAStateMap.get(host.getId());
        return !isInMaintenanceMode(host) && !isDisabled(host) && haState != null
                && Hypervisor.HypervisorType.Simulator.equals(host.getHypervisorType());
    }

    @Override
    public boolean isHealthy(final Host host) throws HACheckerException {
        final SimulatorHAState haState = hostHAStateMap.get(host.getId());
        return haState != null && haState.isHealthy();
    }

    @Override
    public boolean hasActivity(final Host host, final DateTime afterThis) throws HACheckerException {
        final SimulatorHAState haState = hostHAStateMap.get(host.getId());
        return haState != null && haState.hasActivity();
    }

    @Override
    public boolean recover(final Host host) throws HARecoveryException {
        final SimulatorHAState haState = hostHAStateMap.get(host.getId());
        return haState != null && haState.canRecover();
    }

    @Override
    public boolean fence(final Host host) throws HAFenceException {
        final SimulatorHAState haState = hostHAStateMap.get(host.getId());
        return haState != null && haState.canFenced();
    }

    @Override
    public Object getConfigValue(final HAProvider.HAProviderConfig name, final Host host) {
        switch (name) {
            case HealthCheckTimeout:
                return 5L;
            case ActivityCheckTimeout:
                return 5L;
            case RecoveryTimeout:
                return 5L;
            case FenceTimeout:
                return 5L;
            case MaxActivityCheckInterval:
                return 1L;
            case MaxActivityChecks:
                return 3L;
            case ActivityCheckFailureRatio:
                final SimulatorHAState haState = hostHAStateMap.get(host.getId());
                return (haState != null && haState.hasActivity()) ? 1.0 : 0.0;
            case MaxDegradedWaitTimeout:
                return 1L;
            case MaxRecoveryAttempts:
                return 2L;
            case RecoveryWaitTimeout:
                return 1L;
            default:
                throw new InvalidParameterException("Unknown HAProviderConfig " + name.toString());
        }
    }

    private boolean addStateTransition(final HAConfig vo, final boolean status,
                                       final HAConfig.HAState oldState, final HAConfig.HAState newState, final HAConfig.Event event) {
        if (vo.getResourceType() != HAResource.ResourceType.Host) {
            return false;
        }
        final SimulatorHAState haState = hostHAStateMap.get(vo.getResourceId());
        if (haState == null || !status) {
            return false;
        }
        final HAResourceCounter counter = haManager.getHACounter(vo.getResourceId(), vo.getResourceType());
        return haState.addStateTransition(newState, oldState, event, counter);
    }

    @Override
    public boolean preStateTransitionEvent(final HAConfig.HAState oldState, final HAConfig.Event event,
                                           final HAConfig.HAState newState, final HAConfig vo, final boolean status, final Object opaque) {
        return addStateTransition(vo, status, oldState, newState, event);
    }

    @Override
    public boolean postStateTransitionEvent(final StateMachine2.Transition<HAConfig.HAState, HAConfig.Event> transition,
                                            final HAConfig vo, final boolean status, final Object opaque) {
        return addStateTransition(vo, status, transition.getCurrentState(), transition.getToState(), transition.getEvent());
    }
}
