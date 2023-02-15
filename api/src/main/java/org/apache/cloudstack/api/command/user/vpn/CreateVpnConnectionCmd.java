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

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.Site2SiteCustomerGatewayResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnConnectionResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnGatewayResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.vpc.Vpc;


@APICommand(name = "createVpnConnection", description = "Create site to site vpn connection", responseObject = Site2SiteVpnConnectionResponse.class, entityType = {Site2SiteVpnConnection.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVpnConnectionCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVpnConnectionCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.S2S_VPN_GATEWAY_ID,
               type = CommandType.UUID,
               entityType = Site2SiteVpnGatewayResponse.class,
               required = true,
               description = "id of the vpn gateway")
    private Long vpnGatewayId;

    @Parameter(name = ApiConstants.S2S_CUSTOMER_GATEWAY_ID,
               type = CommandType.UUID,
               entityType = Site2SiteCustomerGatewayResponse.class,
               required = true,
               description = "id of the customer gateway")
    private Long customerGatewayId;

    @Parameter(name = ApiConstants.PASSIVE, type = CommandType.BOOLEAN, required = false, description = "connection is passive or not")
    private Boolean passive;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the vpn to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVpnGatewayId() {
        return vpnGatewayId;
    }

    public Long getCustomerGatewayId() {
        return customerGatewayId;
    }

    public boolean isPassive() {
        if (passive == null) {
            return false;
        }
        return passive;
    }

    @Deprecated
    public Boolean getDisplay() {
        return display;
    }

    @Override
    public boolean isDisplay() {
        if (display != null) {
            return display;
        } else {
            return true;
        }
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Site2SiteVpnGateway  vpnGw = getVpnGateway();
        if (vpnGw != null) {
            Vpc vpc = _entityMgr.findById(Vpc.class, getVpnGateway().getVpcId());
            return vpc.getAccountId();
        }
        return -1;
    }

    @Override
    public String getEventDescription() {
        return "Create site-to-site VPN connection for account " + getEntityOwnerId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_S2S_VPN_CONNECTION_CREATE;
    }

    @Override
    public void create() {
        try {
            Site2SiteVpnConnection conn = _s2sVpnService.createVpnConnection(this);
            if (conn != null) {
                setEntityId(conn.getId());
                setEntityUuid(conn.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create site to site vpn connection");
            }
        } catch (NetworkRuleConflictException e) {
            s_logger.info("Network rule conflict: " + e.getMessage());
            s_logger.trace("Network Rule Conflict: ", e);
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
    }

    @Override
    public void execute() {
        try {
            Site2SiteVpnConnection result = _s2sVpnService.startVpnConnection(getEntityId());
            if (result != null) {
                Site2SiteVpnConnectionResponse response = _responseGenerator.createSite2SiteVpnConnectionResponse(result);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create site to site vpn connection");
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.vpcSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        Site2SiteVpnGateway vpnGw = getVpnGateway();
        if (vpnGw != null)
        {
          return vpnGw.getVpcId();
        }
        return null;
    }

    private Site2SiteVpnGateway getVpnGateway() {
        return _s2sVpnService.getVpnGateway(vpnGatewayId);
    }
}
