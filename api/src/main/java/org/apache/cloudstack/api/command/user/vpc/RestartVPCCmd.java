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
package org.apache.cloudstack.api.command.user.vpc;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;

@APICommand(name = "restartVPC", description = "Restarts a VPC", responseObject = SuccessResponse.class, entityType = {Vpc.class},
requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RestartVPCCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RestartVPCCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VpcResponse.class, required = true, description = "the id of the VPC")
    private Long id;

    @Parameter(name = ApiConstants.CLEANUP, type = CommandType.BOOLEAN, required = false, description = "If cleanup old network elements")
    private Boolean cleanup = false;

    @Parameter(name = ApiConstants.MAKEREDUNDANT, type = CommandType.BOOLEAN, required = false, description = "Turn a single VPC into a redundant one.")
    private Boolean makeredundant = false;

    @Parameter(name = ApiConstants.LIVE_PATCH, type = CommandType.BOOLEAN, required = false,
            description = "Live patches the router software before restarting it. This parameter will only work when 'cleanup' is false.",
            since = "4.17.0")
    private Boolean livePatch = false;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean getCleanup() {
        return cleanup;
    }

    public Boolean getMakeredundant() {
        return makeredundant;
    }

    public Boolean getLivePatch() { return livePatch; }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        final Vpc vpc = _entityMgr.findById(Vpc.class, getId());
        if (vpc != null) {
            return vpc.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        try {
            final boolean result = _vpcService.restartVpc(this);
            if (result) {
                final SuccessResponse response = new SuccessResponse(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to restart VPC");
            }
        } catch (final ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (final ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (final InsufficientCapacityException ex) {
            s_logger.info(ex);
            s_logger.trace(ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VPC_RESTART;
    }

    @Override
    public String getEventDescription() {
        return "restarting VPC id=" + getId();
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.vpcSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getId();
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Vpc;
    }
}
