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

package org.apache.cloudstack.api.command.admin.resource;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.RollingMaintenanceResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.resource.RollingMaintenanceManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;

@APICommand(name = StartRollingMaintenanceCmd.APINAME, description = "Start rolling maintenance",
        responseObject = RollingMaintenanceResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class StartRollingMaintenanceCmd extends BaseAsyncCmd {

    @Inject
    RollingMaintenanceManager manager;

    public static final Logger s_logger = Logger.getLogger(StartRollingMaintenanceCmd.class.getName());

    public static final String APINAME = "startRollingMaintenance";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.POD_IDS, type = CommandType.LIST, collectionType = CommandType.UUID,
            entityType = PodResponse.class, description = "the IDs of the pods to start maintenance on")
    private List<Long> podIds;

    @Parameter(name = ApiConstants.CLUSTER_IDS, type = CommandType.LIST, collectionType = CommandType.UUID,
            entityType = ClusterResponse.class, description = "the IDs of the clusters to start maintenance on")
    private List<Long> clusterIds;

    @Parameter(name = ApiConstants.ZONE_ID_LIST, type = CommandType.LIST, collectionType = CommandType.UUID,
            entityType = ZoneResponse.class, description = "the IDs of the zones to start maintenance on")
    private List<Long> zoneIds;

    @Parameter(name = ApiConstants.HOST_IDS, type = CommandType.LIST, collectionType = CommandType.UUID,
            entityType = HostResponse.class, description = "the IDs of the hosts to start maintenance on")
    private List<Long> hostIds;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN,
            description = "if rolling mechanism should continue in case of an error")
    private Boolean forced;

    @Parameter(name = ApiConstants.PAYLOAD, type = CommandType.STRING,
            description = "the command to execute while hosts are on maintenance")
    private String payload;

    @Parameter(name = ApiConstants.TIMEOUT, type = CommandType.INTEGER,
            description = "optional operation timeout (in seconds) that overrides the global timeout setting")
    private Integer timeout;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<Long> getPodIds() {
        return podIds;
    }

    public List<Long> getClusterIds() {
        return clusterIds;
    }

    public List<Long> getZoneIds() {
        return zoneIds;
    }

    public List<Long> getHostIds() {
        return hostIds;
    }

    public Boolean getForced() {
        return forced != null && forced;
    }

    public String getPayload() {
        return payload;
    }

    public Integer getTimeout() {
        return timeout;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        Ternary<Boolean, String, Pair<List<RollingMaintenanceManager.HostUpdated>, List<RollingMaintenanceManager.HostSkipped>>>
                result = manager.startRollingMaintenance(this);
        Boolean success = result.first();
        String details = result.second();
        Pair<List<RollingMaintenanceManager.HostUpdated>, List<RollingMaintenanceManager.HostSkipped>> pair = result.third();
        List<RollingMaintenanceManager.HostUpdated> hostsUpdated = pair.first();
        List<RollingMaintenanceManager.HostSkipped> hostsSkipped = pair.second();

        RollingMaintenanceResponse response = _responseGenerator.createRollingMaintenanceResponse(success, details, hostsUpdated, hostsSkipped);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public String getEventType() {
        Pair<RollingMaintenanceManager.ResourceType, List<Long>> pair = manager.getResourceTypeIdPair(this);
        RollingMaintenanceManager.ResourceType type = pair.first();
        String eventType = "";
        switch (type) {
            case Zone:
                eventType =  EventTypes.EVENT_ZONE_ROLLING_MAINTENANCE;
                break;
            case Pod:
                eventType = EventTypes.EVENT_POD_ROLLING_MAINTENANCE;
                break;
            case Cluster:
                eventType = EventTypes.EVENT_CLUSTER_ROLLING_MAINTENANCE;
                break;
            case Host:
                eventType = EventTypes.EVENT_HOST_ROLLING_MAINTENANCE;
        }
        return eventType;
    }

    @Override
    public String getEventDescription() {
        Pair<RollingMaintenanceManager.ResourceType, List<Long>> pair = manager.getResourceTypeIdPair(this);
        return "Starting rolling maintenance on entity: " + pair.first() + " with IDs: " + pair.second();
    }

    @Override
    public Long getApiResourceId() {
        String eventType = getEventType();
        switch (eventType) {
            case EventTypes.EVENT_ZONE_ROLLING_MAINTENANCE:
                return getZoneIds().get(0);
            case EventTypes.EVENT_POD_ROLLING_MAINTENANCE:
                return getPodIds().get(0);
            case EventTypes.EVENT_CLUSTER_ROLLING_MAINTENANCE:
                return getClusterIds().get(0);
            case EventTypes.EVENT_HOST_ROLLING_MAINTENANCE:
                return getHostIds().get(0);
        }
        return null;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        String eventType = getEventType();
        switch (eventType) {
            case EventTypes.EVENT_ZONE_ROLLING_MAINTENANCE:
                return ApiCommandResourceType.Zone;
            case EventTypes.EVENT_POD_ROLLING_MAINTENANCE:
                return ApiCommandResourceType.Pod;
            case EventTypes.EVENT_CLUSTER_ROLLING_MAINTENANCE:
                return ApiCommandResourceType.Cluster;
            case EventTypes.EVENT_HOST_ROLLING_MAINTENANCE:
                return ApiCommandResourceType.Host;
        }
        return null;
    }
}