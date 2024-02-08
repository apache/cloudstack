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


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = "replaceNetworkACLList", description = "Replaces ACL associated with a network or private gateway", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ReplaceNetworkACLListCmd extends BaseAsyncCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACL_ID, type = CommandType.UUID, entityType = NetworkACLResponse.class, required = true, description = "the ID of the network ACL")
    private long aclId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "the ID of the network")
    private Long networkId;

    @Parameter(name = ApiConstants.GATEWAY_ID, type = CommandType.UUID, entityType = PrivateGatewayResponse.class, description = "the ID of the private gateway")
    private Long privateGatewayId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public long getAclId() {
        return aclId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getPrivateGatewayId() {
        return privateGatewayId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ACL_REPLACE;
    }

    @Override
    public String getEventDescription() {
        return ("Associating network ACL ID=" + aclId + " with network ID=" + networkId);
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();
        return caller.getAccountId();
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        if (getNetworkId() == null && getPrivateGatewayId() == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Network ID and private gateway can't be null at the same time");
        }

        if (getNetworkId() != null && getPrivateGatewayId() != null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Network ID and private gateway can't be passed at the same time");
        }

        CallContext.current().setEventDetails("Network ACL ID: " + aclId);
        boolean result = false;
        if (getPrivateGatewayId() != null) {
            result = _networkACLService.replaceNetworkACLonPrivateGw(aclId, privateGatewayId);
        } else {
            result = _networkACLService.replaceNetworkACL(aclId, networkId);
        }

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to replace network ACL");
        }
    }
}
