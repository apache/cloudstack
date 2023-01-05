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
package org.apache.cloudstack.api.commands;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DedicateHostResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.dedicated.DedicatedService;

import com.cloud.dc.DedicatedResources;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "dedicateHost", description = "Dedicates a host.", responseObject = DedicateHostResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DedicateHostCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DedicateHostCmd.class.getName());
    @Inject
    DedicatedService dedicatedService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, required = true, description = "the ID of the host to update")
    private Long hostId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               required = true,
               description = "the ID of the containing domain")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the name of the account which needs dedication. Must be used with domainId.")
    private String accountName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAccountName() {
        return accountName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        List<? extends DedicatedResources> result = dedicatedService.dedicateHost(getHostId(), getDomainId(), getAccountName());
        ListResponse<DedicateHostResponse> response = new ListResponse<DedicateHostResponse>();
        List<DedicateHostResponse> hostResponseList = new ArrayList<DedicateHostResponse>();
        if (result != null) {
            for (DedicatedResources resource : result) {
                DedicateHostResponse hostResponse = dedicatedService.createDedicateHostResponse(resource);
                hostResponseList.add(hostResponse);
            }
            response.setResponses(hostResponseList);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to dedicate host");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DEDICATE_RESOURCE;
    }

    @Override
    public String getEventDescription() {
        return "dedicating a host";
    }
}
