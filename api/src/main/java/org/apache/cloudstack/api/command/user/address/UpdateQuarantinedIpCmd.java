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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.IpQuarantineResponse;

import java.util.Date;

@APICommand(name = "updateQuarantinedIp", responseObject = IpQuarantineResponse.class, description = "Updates the quarantine end date for the given public IP address.",
        since = "4.19", entityType = {PublicIpQuarantine.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.DomainAdmin})
public class UpdateQuarantinedIpCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = IpQuarantineResponse.class, description = "The ID of the public IP address in " +
            "active quarantine.")
    private Long id;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, description = "The public IP address in active quarantine. Either the IP address is informed, or the ID" +
            " of the IP address in quarantine.")
    private String ipAddress;

    @Parameter(name = ApiConstants.END_DATE, type = BaseCmd.CommandType.DATE, required = true, description = "The date when the quarantine will no longer be active.")
    private Date endDate;

    public Long getId() {
        return id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public void execute() {
        PublicIpQuarantine publicIpQuarantine = _networkService.updatePublicIpAddressInQuarantine(this);
        if (publicIpQuarantine == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update public IP quarantine.");
        }
        IpQuarantineResponse response = _responseGenerator.createQuarantinedIpsResponse(publicIpQuarantine);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
