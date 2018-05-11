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
package org.apache.cloudstack.api.command.admin.br.provider;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.NetworkRuleConflictException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.br.BRManager;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.api.response.BRProviderResponse;
import org.apache.cloudstack.framework.br.BRProvider;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = AddBRProviderCmd.APINAME,
        description = "Adds a Backup and Recovery provider on a zone",
        responseObject = BRProviderResponse.class, since = "4.12.0",
        authorized = {RoleType.Admin})
public class AddBRProviderCmd extends BaseCmd {

    public static final String APINAME = "addBRProvider";

    private static final Logger s_logger = Logger.getLogger(AddBRProviderCmd.class);

    @Inject
    BRManager brManager;

    @Parameter(name=ApiConstants.ZONE_ID, type=BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            required=true, description="the ID of the zone you wish to register the Backup and Recovery provider to.")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the Backup and Recovery provider")
    private String name;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, length = 2048, description = "the URL of the Backup and Recovery provider portal")
    private String url;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "the username for the Backup and Recovery provider")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "the password for the Backup and Recovery provider")
    private String password;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, required = true, description = "the Backup and Recovery provider type")
    private String provider;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        BRProvider providerVO = brManager.addBRProvider(name, url, username, password, zoneId, provider);
        if (providerVO != null) {
            BRProviderResponse response = brManager.createBRProviderResponse(providerVO);
            response.setObjectName("brprovider");
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add a Backup and Recovery Provider");
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
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

    public String getProvider() {
        return provider;
    }
}
