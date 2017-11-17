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

package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.ha.HAConfig;

public class SimulatorHAStateResponse extends BaseResponse {
    @SerializedName(ApiConstants.HA_STATE) @Param(description="the ha state")
    private String haState;

    @SerializedName("prevhastate") @Param(description="the previous ha state")
    private String previousHaState;

    @SerializedName("event") @Param(description="the event that caused state transition")
    private String haEvent;

    @SerializedName("activitycounter") @Param(description="the activity counter")
    private Long activityCounter;

    @SerializedName("recoverycounter") @Param(description="the recovery counter")
    private Long recoveryCounter;

    public void setHaState(final HAConfig.HAState haState) {
        if (haState != null) {
            this.haState = haState.toString().toLowerCase();
        }
    }

    public void setPreviousHaState(final HAConfig.HAState previousHaState) {
        if (previousHaState != null) {
            this.previousHaState = previousHaState.toString().toLowerCase();
        }
    }

    public void setHaEvent(final HAConfig.Event haEvent) {
        this.haEvent = haEvent.toString().toLowerCase();
    }

    public void setActivityCounter(Long activityCounter) {
        this.activityCounter = activityCounter;
    }

    public void setRecoveryCounter(Long recoveryCounter) {
        this.recoveryCounter = recoveryCounter;
    }
}
