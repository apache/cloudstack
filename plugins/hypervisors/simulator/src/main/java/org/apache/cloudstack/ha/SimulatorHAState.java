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
import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class SimulatorHAState {
    private boolean healthy;
    private boolean activity;
    private boolean recover;
    private boolean fence;
    private Queue<SimulatorHAStateResponse> stateTransitions = EvictingQueue.create(100);

    public SimulatorHAState(boolean healthy, boolean activity, boolean recover, boolean fence) {
        this.healthy = healthy;
        this.activity = activity;
        this.recover = recover;
        this.fence = fence;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean hasActivity() {
        return activity;
    }

    public void setActivity(boolean activity) {
        this.activity = activity;
    }

    public boolean canRecover() {
        return recover;
    }

    public void setRecover(boolean recover) {
        this.recover = recover;
    }

    public boolean canFenced() {
        return fence;
    }

    public void setFence(boolean fence) {
        this.fence = fence;
    }

    public boolean addStateTransition(final HAConfig.HAState newHaState, final HAConfig.HAState oldHaState, final HAConfig.Event event, final HAResourceCounter counter) {
        final SimulatorHAStateResponse stateResponse = new SimulatorHAStateResponse();
        stateResponse.setHaState(newHaState);
        stateResponse.setPreviousHaState(oldHaState);
        stateResponse.setHaEvent(event);
        if (counter != null) {
            stateResponse.setActivityCounter(counter.getActivityCheckCounter());
            stateResponse.setRecoveryCounter(counter.getRecoveryCounter());
        }
        stateResponse.setObjectName("hastatetransition");
        return stateTransitions.add(stateResponse);
    }

    public List<SimulatorHAStateResponse> listRecentStateTransitions() {
        return new ArrayList<>(stateTransitions);
    }
}
