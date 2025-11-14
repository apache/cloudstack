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

package org.apache.cloudstack.api.command.admin.network.bgp;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.BgpPeer;
import org.apache.commons.collections.MapUtils;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

import java.util.Collection;
import java.util.Map;

@APICommand(name = "createBgpPeer",
        description = "Creates a Bgp Peer for a zone.",
        responseObject = BgpPeerResponse.class,
        since = "4.20.0",
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class CreateBgpPeerCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = true,
            description = "UUID of the zone which the Bgp Peer belongs to.",
            validations = {ApiArgValidator.PositiveNumber})
    private Long zoneId;

    @Parameter(name = ApiConstants.IP_ADDRESS,
            type = CommandType.STRING,
            description = "The IPv4 address of the Bgp Peer.")
    private String ip4Address;

    @Parameter(name = ApiConstants.IP6_ADDRESS,
            type = CommandType.STRING,
            description = "The IPv6 address of the Bgp Peer.")
    private String ip6Address;

    @Parameter(name = ApiConstants.AS_NUMBER,
            type = CommandType.LONG,
            required = true,
            description = "The AS number of the Bgp Peer.")
    private Long asNumber;

    @Parameter(name = ApiConstants.PASSWORD,
            type = CommandType.STRING,
            description = "The password of the Bgp Peer.")
    private String password;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "account who will own the Bgp Peer")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "project who will own the Bgp Peer")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "domain ID of the account owning the Bgp Peer")
    private Long domainId;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
            description = "BGP peer details in key/value pairs.")
    protected Map details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getZoneId() {
        return zoneId;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public String getPassword() {
        return password;
    }

    public Long getAsNumber() {
        return asNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Map<String, String> getDetails() {
        if (MapUtils.isEmpty(details)) {
            return null;
        }
        Collection<String> paramsCollection = this.details.values();
        return (Map<String, String>) (paramsCollection.toArray())[0];
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_BGP_PEER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating Bgp Peer " + getAsNumber() + " for zone=" + getZoneId();
    }

    @Override
    public void execute() {
        BgpPeer result = routedIpv4Manager.createBgpPeer(this);
        if (result != null) {
            BgpPeerResponse response = routedIpv4Manager.createBgpPeerResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Bgp Peer.");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
