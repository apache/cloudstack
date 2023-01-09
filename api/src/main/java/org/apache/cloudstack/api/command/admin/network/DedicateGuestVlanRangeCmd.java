/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.admin.network;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.GuestVlanRangeResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.GuestVlanRange;
import com.cloud.user.Account;

@APICommand(name = "dedicateGuestVlanRange", description = "Dedicates a guest vlan range to an account", responseObject = GuestVlanRangeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DedicateGuestVlanRangeCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DedicateGuestVlanRangeCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VLAN_RANGE, type = CommandType.STRING, required = true, description = "guest vlan range to be dedicated")
    private String vlan;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "account who will own the VLAN")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "project who will own the VLAN")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "domain ID of the account owning a VLAN")
    private Long domainId;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               required = true,
               description = "physical network ID of the vlan")
    private Long physicalNetworkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getVlan() {
        return vlan;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException {
        GuestVlanRange result = _networkService.dedicateGuestVlanRange(this);
        if (result != null) {
            GuestVlanRangeResponse response = _responseGenerator.createDedicatedGuestVlanRangeResponse(result);
            response.setResponseName(getCommandName());
            response.setObjectName("dedicatedguestvlanrange");
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to dedicate guest vlan range");
        }
    }

}
