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
package org.apache.cloudstack.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.network.netris.NetrisProvider;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetrisProviderResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.service.NetrisProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@APICommand(name = AddNetrisProviderCmd.APINAME, description = "Add Netris Provider to CloudStack",
        responseObject = NetrisProviderResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.21.0")
public class AddNetrisProviderCmd extends BaseCmd {
    public static final String APINAME = "addNetrisProvider";
    public static final Logger LOGGER = LoggerFactory.getLogger(AddNetrisProviderCmd.class.getName());

    @Inject
    NetrisProviderService netrisProviderService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true,
            description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Netris provider name")
    private String name;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, description = "Netris provider URL")
    private String url;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Username to login into Netris")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Password to login into Netris")
    private String password;

    @Parameter(name = ApiConstants.SITE_NAME, type = CommandType.STRING, required = true, description = "Netris Site name")
    private String siteName;

    @Parameter(name = ApiConstants.TENANT_NAME, type = CommandType.STRING, required = true, description = "Netris Tenant name")
    private String tenantName;

    @Parameter(name = ApiConstants.NETRIS_TAG, type = CommandType.STRING, required = true, description = "Netris tag for vNets")
    private String netrisTag;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

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

    public String getSiteName() {
        return siteName;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getNetrisTag() {
        return netrisTag;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        NetrisProvider provider = netrisProviderService.addProvider(this);
        NetrisProviderResponse response = netrisProviderService.createNetrisProviderResponse(provider);
        if (response == null)
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Netris provider");
        else {
            response.setResponseName(getCommandName());
            setResponseObject(response);
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
