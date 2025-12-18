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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.BgpPeer;

@APICommand(name = "listBgpPeers",
        description = "Lists Bgp Peers.",
        responseObject = BgpPeerResponse.class,
        since = "4.20.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class ListBgpPeersCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = BgpPeerResponse.class,
            description = "UUID of the Bgp Peer.")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "UUID of zone to which the Bgp Peer belongs to.")
    private Long zoneId;

    @Parameter(name = ApiConstants.AS_NUMBER,
            type = CommandType.LONG,
            description = "AS number of the Bgp Peer.")
    private Long asNumber;

    @Parameter(name = ApiConstants.ACCOUNT,
            type = CommandType.STRING,
            description = "the account which the Bgp Peer is dedicated to. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "project who which the Bgp Peer is dedicated to")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the domain ID which the Bgp Peer is dedicated to.")
    private Long domainId;

    @Parameter(name = ApiConstants.IS_DEDICATED,
            type = CommandType.BOOLEAN,
            description = "Lists only dedicated or non-dedicated Bgp Peers. If not set, lists all dedicated and non-dedicated BGP peers the domain/account can access.")
    private Boolean isDedicated;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
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

    public Boolean getDedicated() {
        return isDedicated;
    }

    @Override
    public void execute() {
        List<? extends BgpPeer> subnets = routedIpv4Manager.listBgpPeers(this);
        ListResponse<BgpPeerResponse> response = new ListResponse<>();
        List<BgpPeerResponse> subnetResponses = new ArrayList<>();
        for (BgpPeer subnet : subnets) {
            BgpPeerResponse subnetResponse = routedIpv4Manager.createBgpPeerResponse(subnet);
            subnetResponse.setObjectName("bgppeer");
            subnetResponses.add(subnetResponse);
        }

        response.setResponses(subnetResponses, subnets.size());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
