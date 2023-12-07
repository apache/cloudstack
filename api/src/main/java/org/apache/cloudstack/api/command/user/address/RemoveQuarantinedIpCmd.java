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
package org.apache.cloudstack.api.command.user.address;

import com.cloud.network.PublicIpQuarantine;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.IpQuarantineResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

@APICommand(name = "removeQuarantinedIp", responseObject = IpQuarantineResponse.class, description = "Removes a public IP address from quarantine. Only IPs in active " +
        "quarantine can be removed.",
        since = "4.19", entityType = {PublicIpQuarantine.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.DomainAdmin})
public class RemoveQuarantinedIpCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = IpQuarantineResponse.class, description = "The ID of the public IP address in active quarantine. " +
            "Either the IP address is informed, or the ID of the IP address in quarantine.")
    private Long id;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, description = "The public IP address in active quarantine. Either the IP address is informed, or the ID" +
            " of the IP address in quarantine.")
    private String ipAddress;

    @Parameter(name = ApiConstants.REMOVAL_REASON, type = CommandType.STRING, required = true, description = "The reason for removing the public IP address from quarantine " +
            "prematurely.")
    private String removalReason;

    public Long getId() {
        return id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    @Override
    public void execute() {
        _networkService.removePublicIpAddressFromQuarantine(this);
        final SuccessResponse response = new SuccessResponse();
        response.setResponseName(getCommandName());
        response.setSuccess(true);
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
