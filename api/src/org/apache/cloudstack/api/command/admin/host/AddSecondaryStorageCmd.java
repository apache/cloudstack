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

import org.apache.cloudstack.api.*;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;
import com.cloud.user.Account;

@APICommand(name = "addSecondaryStorage", description="Adds secondary storage.", responseObject=HostResponse.class)
public class AddSecondaryStorageCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddSecondaryStorageCmd.class.getName());
    private static final String s_name = "addsecondarystorageresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL for the secondary storage")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType=ZoneResponse.class,
            description="the Zone ID for the secondary storage")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        try {
            List<? extends Host> result = _resourceService.discoverHosts(this);
            HostResponse hostResponse = null;
            if (result != null && result.size() > 0) {
                for (Host host : result) {
                    // There should only be one secondary storage host per add
                    hostResponse = _responseGenerator.createHostResponse(host);
                    hostResponse.setResponseName(getCommandName());
                    hostResponse.setObjectName("secondarystorage");
                    this.setResponseObject(hostResponse);
                }
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add secondary storage");
            }
        } catch (DiscoveryException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
