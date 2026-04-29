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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap;

@APICommand(name = "createIpv4SubnetForGuestNetwork",
        description = "Creates a IPv4 subnet for guest networks.",
        responseObject = Ipv4SubnetForGuestNetworkResponse.class,
        since = "4.20.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class CreateIpv4SubnetForGuestNetworkCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.PARENT_ID,
            type = CommandType.UUID,
            entityType = DataCenterIpv4SubnetResponse.class,
            required = true,
            description = "The zone Ipv4 subnet which the IPv4 subnet belongs to.")
    private Long parentId;

    @Parameter(name = ApiConstants.SUBNET,
            type = CommandType.STRING,
            description = "The CIDR of this Ipv4 subnet.")
    private String subnet;

    @Parameter(name = ApiConstants.CIDR_SIZE,
            type = CommandType.INTEGER,
            description = "the CIDR size of IPv4 network. This is mutually exclusive with subnet.")
    private Integer cidrSize;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getParentId() {
        return parentId;
    }

    public String getSubnet() {
        return subnet;
    }

    public Integer getCidrSize() {
        return cidrSize;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IP4_GUEST_SUBNET_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating guest IPv4 subnet " + getSubnet() + " in zone subnet=" + getParentId();
    }

    @Override
    public void execute() {
        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.createIpv4SubnetForGuestNetwork(this);
        if (result != null) {
            Ipv4SubnetForGuestNetworkResponse response = routedIpv4Manager.createIpv4SubnetForGuestNetworkResponse(result);
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
