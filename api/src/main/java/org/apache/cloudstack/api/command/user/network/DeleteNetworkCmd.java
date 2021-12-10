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
package org.apache.cloudstack.api.command.user.network;


import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;

@APICommand(name = "deleteNetwork", description = "Deletes a network", responseObject = SuccessResponse.class, entityType = {Network.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteNetworkCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteNetworkCmd.class.getName());
    private static final String s_name = "deletenetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = NetworkResponse.class, required = true, description = "the ID of the network")
    private Long id;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN, required = false, description = "Force delete a network." +
            " Network will be marked as 'Destroy' even when commands to shutdown and cleanup to the backend fails.")
    private Boolean forced;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public boolean isForced() {
        return (forced != null) ? forced : false;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Network Id: " + id);
        boolean result = _networkService.deleteNetwork(id, isForced());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete network");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return id;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting network: " + id;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Network;
    }
    @Override
    public long getEntityOwnerId() {
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Network ID=" + id + " doesn't exist");
        } else {
            return _networkService.getNetwork(id).getAccountId();
        }
    }
}
