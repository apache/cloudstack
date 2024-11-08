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

package org.apache.cloudstack.api.command.admin.network;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "createIpv4SubnetForZone",
        description = "Creates a IPv4 subnet for a zone.",
        responseObject = DataCenterIpv4SubnetResponse.class,
        since = "4.20.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class CreateIpv4SubnetForZoneCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = true,
            description = "UUID of the zone which the IPv4 subnet belongs to.",
            validations = {ApiArgValidator.PositiveNumber})
    private Long zoneId;

    @Parameter(name = ApiConstants.SUBNET,
            type = CommandType.STRING,
            required = true,
            description = "The CIDR of the IPv4 subnet.")
    private String subnet;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "account who will own the IPv4 subnet")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "project who will own the IPv4 subnet")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "domain ID of the account owning the IPv4 subnet")
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getZoneId() {
        return zoneId;
    }

    public String getSubnet() {
        return subnet;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getDomainId() {
        return domainId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ZONE_IP4_SUBNET_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating guest IPv4 subnet " + getSubnet() + " for zone=" + getZoneId();
    }

    @Override
    public void execute() {
        DataCenterIpv4GuestSubnet result = routedIpv4Manager.createDataCenterIpv4GuestSubnet(this);
        if (result != null) {
            DataCenterIpv4SubnetResponse response = routedIpv4Manager.createDataCenterIpv4SubnetResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create zone guest IPv4 subnet.");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
