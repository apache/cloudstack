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
package org.apache.cloudstack.api.command.user.vpn;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.Site2SiteCustomerGatewayResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.user.Account;

@APICommand(name = "deleteVpnCustomerGateway", description = "Delete site to site vpn customer gateway", responseObject = SuccessResponse.class, entityType = {Site2SiteCustomerGateway.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteVpnCustomerGatewayCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteVpnCustomerGatewayCmd.class.getName());

    private static final String s_name = "deletevpncustomergatewayresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = Site2SiteCustomerGatewayResponse.class,
               required = true,
               description = "id of customer gateway")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Site2SiteCustomerGateway gw = _entityMgr.findById(Site2SiteCustomerGateway.class, getId());
        if (gw != null) {
            return gw.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventDescription() {
        return "Delete site-to-site VPN customer gateway for account " + getEntityOwnerId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_S2S_VPN_CUSTOMER_GATEWAY_DELETE;
    }

    @Override
    public void execute() {
        boolean result = _s2sVpnService.deleteCustomerGateway(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete customer VPN gateway");
        }
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VpnCustomerGateway;
    }
}
