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
package com.cloud.api.commands;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExternalFirewallResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.network.element.JuniperSRXFirewallElementService;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "addExternalFirewall", description = "Adds an external firewall appliance", responseObject = ExternalFirewallResponse.class,
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class AddExternalFirewallCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddExternalFirewallCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = true,
               description = "Zone in which to add the external firewall appliance.")
    private Long zoneId;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, description = "URL of the external firewall appliance.")
    private String url;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Username of the external firewall appliance.")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Password of the external firewall appliance.")
    private String password;

    ///////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Inject
    JuniperSRXFirewallElementService _srxElementService;

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void execute() {
        try {
            Host externalFirewall = _srxElementService.addExternalFirewall(this);
            ExternalFirewallResponse response = _srxElementService.createExternalFirewallResponse(externalFirewall);
            response.setObjectName("externalfirewall");
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (InvalidParameterValueException ipve) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, ipve.getMessage());
        } catch (CloudRuntimeException cre) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, cre.getMessage());
        }
    }
}
