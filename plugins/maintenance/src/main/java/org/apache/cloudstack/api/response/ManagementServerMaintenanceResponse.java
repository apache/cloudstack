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
package org.apache.cloudstack.api.response;

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.management.ManagementServerHost.State;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ManagementServerMaintenanceResponse extends BaseResponse {
    @SerializedName(ApiConstants.MANAGEMENT_SERVER_ID)
    @Param(description = "The id of the management server")
    private String managementServerId;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the management server")
    private State state;

    @SerializedName(ApiConstants.MAINTENANCE_INITIATED)
    @Param(description = "Indicates whether maintenance has been initiated")
    private Boolean maintenanceInitiated;

    @SerializedName(ApiConstants.SHUTDOWN_TRIGGERED)
    @Param(description = "Indicates whether a shutdown has been triggered")
    private Boolean shutdownTriggered;

    @SerializedName(ApiConstants.READY_FOR_SHUTDOWN)
    @Param(description = "Indicates whether CloudStack is ready to shutdown")
    private Boolean readyForShutdown;

    @SerializedName(ApiConstants.PENDING_JOBS_COUNT)
    @Param(description = "The number of jobs in progress")
    private Long pendingJobsCount;

    @SerializedName(ApiConstants.AGENTS_COUNT)
    @Param(description = "The number of host agents this management server is responsible for")
    private Long agentsCount;

    @SerializedName(ApiConstants.AGENTS)
    @Param(description = "The host agents this management server is responsible for")
    private List<String> agents;

    public ManagementServerMaintenanceResponse(String managementServerId, State state, Boolean maintenanceInitiated, Boolean shutdownTriggered, Boolean readyForShutdown, long pendingJobsCount, long agentsCount, List<String> agents) {
        this.managementServerId = managementServerId;
        this.state = state;
        this.maintenanceInitiated = maintenanceInitiated;
        this.shutdownTriggered = shutdownTriggered;
        this.readyForShutdown = readyForShutdown;
        this.pendingJobsCount = pendingJobsCount;
        this.agentsCount = agentsCount;
        this.agents = agents;
    }

    public String getManagementServerId() {
        return managementServerId;
    }

    public void setManagementServerId(String managementServerId) {
        this.managementServerId = managementServerId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Boolean getMaintenanceInitiated() {
        return this.maintenanceInitiated;
    }

    public void setMaintenanceInitiated(Boolean maintenanceInitiated) {
        this.maintenanceInitiated = maintenanceInitiated;
    }

    public Boolean getShutdownTriggered() {
        return this.shutdownTriggered;
    }

    public void setShutdownTriggered(Boolean shutdownTriggered) {
        this.shutdownTriggered = shutdownTriggered;
    }

    public Boolean getReadyForShutdown() {
        return this.readyForShutdown;
    }

    public void setReadyForShutdown(Boolean readyForShutdown) {
        this.readyForShutdown = readyForShutdown;
    }

    public Long getPendingJobsCount() {
        return this.pendingJobsCount;
    }

    public void setPendingJobsCount(Long pendingJobsCount) {
        this.pendingJobsCount = pendingJobsCount;
    }

    public Long getAgentsCount() {
        return this.agentsCount;
    }

    public void setAgentsCount(Long agentsCount) {
        this.agentsCount = agentsCount;
    }

    public List<String> getAgents() {
        return agents;
    }

    public void setAgents(List<String> agents) {
        this.agents = agents;
    }
}
