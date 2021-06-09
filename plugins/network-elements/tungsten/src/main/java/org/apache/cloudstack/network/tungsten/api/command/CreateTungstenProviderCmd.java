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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.TungstenProvider;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenProviderService;

import javax.inject.Inject;

@APICommand(name = "createTungstenProvider", description = "Create Tungsten-Fabric provider in cloudstack", responseObject =
    TungstenProviderResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateTungstenProviderCmd extends BaseCmd {

    private static final String s_name = "createtungstenproviderresponse";

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true
        , description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Tungsten Fabric provider"
        + " name")
    private String name;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_HOSTNAME, type = CommandType.STRING, required = true,
        description = "Tungsten Fabric provider hostname")
    private String hostname;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_PORT, type = CommandType.STRING, required = false, description =
        "Tungsten Fabric provider port")
    private String port;

    @Parameter(name = ApiConstants.TUNGSTEN_GATEWAY, type = CommandType.STRING, required = true,
        description = "Tungsten gateway")
    private String gateway;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_VROUTER_PORT, type = CommandType.STRING, required = false,
        description = "Tungsten Fabric provider vrouter port")
    private String vrouterPort;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_INTROSPECT_PORT, type = CommandType.STRING, required = false,
        description = "Tungsten Fabric provider introspect port")
    private String introspectPort;

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPort() {
        return port;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(final String gateway) {
        this.gateway = gateway;
    }

    public String getIntrospectPort() {
        return introspectPort;
    }

    public void setIntrospectPort(final String introspectPort) {
        this.introspectPort = introspectPort;
    }

    public String getVrouterPort() {
        return vrouterPort;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final Long zoneId) {
        this.zoneId = zoneId;
    }

    @Inject
    private TungstenProviderService tungstenProviderService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenProvider tungstenProvider = tungstenProviderService.addProvider(this);
        TungstenProviderResponse tungstenProviderResponse = tungstenProviderService.getTungstenProvider(zoneId);
        if (tungstenProviderResponse == null)
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Tungsten-Fabric provider");
        else {
            tungstenProviderResponse.setResponseName(getCommandName());
            setResponseObject(tungstenProviderResponse);
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
