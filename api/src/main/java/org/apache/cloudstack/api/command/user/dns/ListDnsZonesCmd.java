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

package org.apache.cloudstack.api.command.user.dns;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.dns.DnsZone;

@APICommand(name = "listDnsZones",
        description = "Lists DNS zones.", responseObject = DnsZoneResponse.class,
        entityType = {DnsZone.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListDnsZonesCmd extends BaseListAccountResourcesCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            description = "List DNS zone by ID")
    private Long id;

    @Parameter(name = "dnsserverid", type = CommandType.UUID, entityType = DnsServerResponse.class,
            description = "List DNS zones belonging to a specific DNS server")
    private Long dnsServerId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "List by zone name")
    private String name;

    public Long getId() { return id; }
    public Long getDnsServerId() { return dnsServerId; }
    public String getName() { return name; }

    @Override
    public void execute() {
        ListResponse<DnsZoneResponse> response = dnsProviderManager.listDnsZones(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
