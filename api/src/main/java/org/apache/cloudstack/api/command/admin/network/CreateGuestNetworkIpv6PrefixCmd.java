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
import org.apache.cloudstack.api.response.DataCenterGuestIpv6PrefixResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenterGuestIpv6Prefix;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = "createGuestNetworkIpv6Prefix",
        description = "Creates a guest network IPv6 prefix.",
        responseObject = DataCenterGuestIpv6PrefixResponse.class,
        since = "4.17.0.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class CreateGuestNetworkIpv6PrefixCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CreateGuestNetworkIpv6PrefixCmd.class);


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = true,
            description = "UUID of zone to which the IPv6 prefix belongs to.",
            validations = {ApiArgValidator.PositiveNumber})
    private Long zoneId;

    @Parameter(name = ApiConstants.PREFIX,
            type = CommandType.STRING,
            required = true,
            description = "The /56 or higher IPv6 CIDR for network prefix.")
    private String prefix;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getZoneId() {
        return zoneId;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GUEST_IP6_PREFIX_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating guest IPv6 prefix " + getPrefix() + " for zone=" + getZoneId();
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException {
        DataCenterGuestIpv6Prefix result = _configService.createDataCenterGuestIpv6Prefix(this);
        if (result != null) {
            DataCenterGuestIpv6PrefixResponse response = _responseGenerator.createDataCenterGuestIpv6PrefixResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create zone guest IPv6 prefix.");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
