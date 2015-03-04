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
package org.apache.cloudstack.api.command.admin.host;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GuestOSCategoryResponse;
import org.apache.cloudstack.api.response.HostResponse;

import com.cloud.host.Host;
import com.cloud.user.Account;

@APICommand(name = "updateHost", description = "Updates a host.", responseObject = HostResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateHostCmd.class.getName());
    private static final String s_name = "updatehostresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HostResponse.class, required = true, description = "the ID of the host to update")
    private Long id;

    @Parameter(name = ApiConstants.OS_CATEGORY_ID,
               type = CommandType.UUID,
               entityType = GuestOSCategoryResponse.class,
               description = "the id of Os category to update the host with")
    private Long osCategoryId;

    @Parameter(name = ApiConstants.ALLOCATION_STATE,
               type = CommandType.STRING,
               description = "Change resource state of host, valid values are [Enable, Disable]. Operation may failed if host in states not allowing Enable/Disable")
    private String allocationState;

    @Parameter(name = ApiConstants.HOST_TAGS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "list of tags to be added to the host")
    private List<String> hostTags;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, description = "the new uri for the secondary storage: nfs://host/path")
    private String url;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getOsCategoryId() {
        return osCategoryId;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public List<String> getHostTags() {
        return hostTags;
    }

    public String getUrl() {
        return url;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "updatehost";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        Host result;
        try {
            result = _resourceService.updateHost(this);
            HostResponse hostResponse = _responseGenerator.createHostResponse(result);
            hostResponse.setResponseName(getCommandName());
            this.setResponseObject(hostResponse);
        } catch (Exception e) {
            s_logger.debug("Failed to update host:" + getId(), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update host:" + getId() + "," + e.getMessage());
        }
    }
}
