/*
 * Copyright 2016 ShapeBlue Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cloudstack.api.command.user.applicationcluster;

import org.apache.cloudstack.applicationcluster.ApplicationClusterEventTypes;
import org.apache.cloudstack.applicationcluster.ApplicationCluster;
import org.apache.cloudstack.applicationcluster.ApplicationClusterService;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ApplicationClusterResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = StopApplicationClusterCmd.APINAME, description = "Stops a running container cluster",
        responseObject = SuccessResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {ApplicationCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class StopApplicationClusterCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(StopApplicationClusterCmd.class.getName());

    public static final String APINAME = "stopApplicationCluster";

    @Inject
    public ApplicationClusterService applicationClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ApplicationClusterResponse.class,
            description = "the ID of the container cluster")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    @Override
    public String getEventType() {
        return ApplicationClusterEventTypes.EVENT_CONTAINER_CLUSTER_STOP;
    }

    @Override
    public String getEventDescription() {
        return "Stopping container cluster id: " + getId();
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + "response";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public ApplicationCluster validateRequest() {
        if (getId() == null || getId() < 1L) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid container cluster ID provided");
        }
        final ApplicationCluster applicationCluster = applicationClusterService.findById(getId());
        if (applicationCluster == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Given container cluster was not found");
        }
        return applicationCluster;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final ApplicationCluster applicationCluster = validateRequest();
        try {
            final boolean result = applicationClusterService.stopContainerCluster(getId());
            final SuccessResponse response = new SuccessResponse(getCommandName());
            response.setSuccess(result);
            setResponseObject(response);
        } catch (ManagementServerException ex) {
            s_logger.warn("Failed to stop container cluster:" + applicationCluster.getUuid() + " due to " + ex.getMessage());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to stop container cluster:" + applicationCluster.getUuid(), ex);
        }
    }

}
