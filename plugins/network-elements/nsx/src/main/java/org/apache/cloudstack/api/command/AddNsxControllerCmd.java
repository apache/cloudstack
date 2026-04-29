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

import com.cloud.network.nsx.NsxProvider;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.response.NsxControllerResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.service.NsxProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


@APICommand(name = AddNsxControllerCmd.APINAME, description = "Add NSX Controller to CloudStack",
        responseObject = NsxControllerResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false, since = "4.19.0")
public class AddNsxControllerCmd extends BaseCmd {
    public static final String APINAME = "addNsxController";
    public static final Logger LOGGER = LoggerFactory.getLogger(AddNsxControllerCmd.class.getName());

    @Inject
    NsxProviderService nsxProviderService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true,
            description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "NSX controller / provider name")
    private String name;

    @Parameter(name = ApiConstants.NSX_PROVIDER_HOSTNAME, type = CommandType.STRING, required = true, description = "NSX controller hostname / IP address")
    private String hostname;

    @Parameter(name = ApiConstants.NSX_PROVIDER_PORT, type = CommandType.STRING, description = "NSX controller port")
    private String port;
    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Username to log into NSX controller")
    private String username;
    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Password to login into NSX controller")
    private String password;

    @Parameter(name = ApiConstants.TIER0_GATEWAY, type = CommandType.STRING, required = true, description = "Tier-0 Gateway address")
    private String tier0Gateway;

    @Parameter(name = ApiConstants.EDGE_CLUSTER, type = CommandType.STRING, required = true, description = "Edge Cluster name")
    private String edgeCluster;

    @Parameter(name = ApiConstants.TRANSPORT_ZONE, type = CommandType.STRING, required = true, description = "Transport Zone controls to which hosts a logical switch can reach")
    private String transportZone;

    public NsxProviderService getNsxProviderService() {
        return nsxProviderService;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getTier0Gateway() {
        return tier0Gateway;
    }

    public String getEdgeCluster() {
        return edgeCluster;
    }

    public String getTransportZone() {
        return transportZone;
    }

    @Override
    public void execute() throws ServerApiException {
        NsxProvider nsxProvider = nsxProviderService.addProvider(this);
        NsxControllerResponse nsxControllerResponse =
                nsxProviderService.createNsxControllerResponse(
                        nsxProvider);
        if (nsxControllerResponse == null)
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add NSX controller");
        else {
            nsxControllerResponse.setResponseName(getCommandName());
            setResponseObject(nsxControllerResponse);
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
