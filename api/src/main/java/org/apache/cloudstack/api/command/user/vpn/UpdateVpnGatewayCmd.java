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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.Site2SiteVpnGatewayResponse;

import com.cloud.event.EventTypes;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.user.Account;

@APICommand(name = "updateVpnGateway", description = "Updates site to site vpn local gateway", responseObject = Site2SiteVpnGatewayResponse.class, since = "4.4",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateVpnGatewayCmd extends BaseAsyncCustomIdCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = Site2SiteVpnGatewayResponse.class, required = true, description = "id of customer gateway")
    private Long id;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the vpn to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public Boolean getDisplay() {
        return display;
    }

    @Override
    public long getEntityOwnerId() {
        Site2SiteVpnGateway gw = _entityMgr.findById(Site2SiteVpnGateway.class, getId());
        if (gw != null) {
            return gw.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventDescription() {
        return "Update site-to-site VPN gateway id= " + id;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_S2S_VPN_GATEWAY_UPDATE;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {
        Site2SiteVpnGateway result = _s2sVpnService.updateVpnGateway(id, this.getCustomId(), getDisplay());
        if (result != null) {
            Site2SiteVpnGatewayResponse response = _responseGenerator.createSite2SiteVpnGatewayResponse(result);
            response.setResponseName(getCommandName());
        }
    }

    @Override
    public void checkUuid() {
        if (this.getCustomId() != null) {
            _uuidMgr.checkUuid(this.getCustomId(), Site2SiteVpnGateway.class);
        }
    }
}
