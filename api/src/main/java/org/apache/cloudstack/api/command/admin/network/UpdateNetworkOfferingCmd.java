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
package org.apache.cloudstack.api.command.admin.network;

import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.offering.DomainAndZoneIdResolver;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;


import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;

@APICommand(name = "updateNetworkOffering", description = "Updates a network offering.", responseObject = NetworkOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateNetworkOfferingCmd extends BaseCmd implements DomainAndZoneIdResolver {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = NetworkOfferingResponse.class, description = "The ID of the network offering")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the network offering")
    private String networkOfferingName;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "The display text of the network offering")
    private String displayText;

    @Parameter(name = ApiConstants.AVAILABILITY, type = CommandType.STRING, description = "The availability of network offering."
            + " The value is Required makes this network offering default for Guest Virtual Networks. Only one network offering can have the value Required ")
    private String availability;

    @Parameter(name = ApiConstants.SORT_KEY, type = CommandType.INTEGER, description = "Sort key of the network offering, integer")
    private Integer sortKey;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "Update state for the network offering")
    private String state;

    @Parameter(name = ApiConstants.KEEPALIVE_ENABLED,
            type = CommandType.BOOLEAN,
            required = false,
            description = "If true keepalive will be turned on in the loadbalancer. At the time of writing this has only an effect on haproxy; the mode http and httpclose options are unset in the haproxy conf file.")
    private Boolean keepAliveEnabled;

    @Parameter(name = ApiConstants.MAX_CONNECTIONS,
            type = CommandType.INTEGER,
            description = "Maximum number of concurrent connections supported by the network offering")
    private Integer maxConnections;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.STRING, description = "The tags for the network offering.", length = 4096)
    private String tags;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.STRING,
            length = 4096,
            description = "The ID of the containing domain(s) as comma separated string, public for public offerings")
    private String domainIds;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.STRING,
            description = "The ID of the containing zone(s) as comma separated string, all for all zones offerings",
            since = "4.13",
            length = 4096)
    private String zoneIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getNetworkOfferingName() {
        return networkOfferingName;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public String getAvailability() {
        return availability;
    }

    public String getState() {
        return state;
    }

    public Integer getSortKey() {
        return sortKey;
    }

    public Integer getMaxconnections() {
        return maxConnections;
    }

    public Boolean getKeepAliveEnabled() {
        return keepAliveEnabled;
    }

    public String getTags() {
        return tags;
    }

    public List<Long> getDomainIds() {
        return resolveDomainIds(domainIds, id, _configService::getNetworkOfferingDomains, "network offering");
    }

    public List<Long> getZoneIds() {
        return resolveZoneIds(zoneIds, id, _configService::getNetworkOfferingZones, "network offering");
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.NetworkOffering;
    }

    @Override
    public void execute() {
        NetworkOffering result = _configService.updateNetworkOffering(this);
        if (result != null) {
            NetworkOfferingResponse response = _responseGenerator.createNetworkOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update network offering");
        }
    }
}
