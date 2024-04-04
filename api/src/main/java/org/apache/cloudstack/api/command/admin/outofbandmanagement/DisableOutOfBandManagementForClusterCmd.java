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

package org.apache.cloudstack.api.command.admin.outofbandmanagement;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.org.Cluster;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.OutOfBandManagementResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;

import javax.inject.Inject;

@APICommand(name = "disableOutOfBandManagementForCluster", description = "Disables out-of-band management for a cluster",
        responseObject = OutOfBandManagementResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.9.0", authorized = {RoleType.Admin})
public class DisableOutOfBandManagementForClusterCmd extends BaseAsyncCmd {

    @Inject
    private OutOfBandManagementService outOfBandManagementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CLUSTER_ID, type = BaseCmd.CommandType.UUID, required = true, entityType = ClusterResponse.class,
            validations = {ApiArgValidator.PositiveNumber}, description = "the ID of the cluster")
    private Long clusterId;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    final public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final Cluster cluster = _resourceService.getCluster(getClusterId());
        if (cluster == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find cluster by ID: " + getClusterId());
        }

        OutOfBandManagementResponse response = outOfBandManagementService.disableOutOfBandManagement(cluster);

        CallContext.current().setEventDetails("Cluster Id:" + cluster.getId() + " out-of-band management enabled: false");
        CallContext.current().putContextParameter(Cluster.class, cluster.getUuid());

        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    final public Long getClusterId() {
        return clusterId;
    }

    @Override
    final public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_HOST_OUTOFBAND_MANAGEMENT_DISABLE;
    }

    @Override
    public String getEventDescription() {
        return "disable out-of-band management password for cluster: " + getClusterId();
    }

    @Override
    public Long getApiResourceId() {
        return getClusterId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Cluster;
    }
}
