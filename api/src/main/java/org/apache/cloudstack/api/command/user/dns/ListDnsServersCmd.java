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
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsServer;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.EnumUtils;

@APICommand(name = "listDnsServers",
        description = "Lists DNS servers owned by the account.",
        responseObject = DnsServerResponse.class,
        entityType = {DnsServer.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListDnsServersCmd  extends BaseListAccountResourcesCmd {

    /////////////////////////////////////////////////////
    //////////////// API Parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsServerResponse.class,
            description = "the ID of the DNS server")
    private Long id;

    @Parameter(name = ApiConstants.PROVIDER_TYPE, type = CommandType.STRING,
            description = "filter by provider type (e.g. PowerDNS, Cloudflare)")
    private String providerType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public DnsProviderType getProviderType() {
        DnsProviderType dnsProviderType = EnumUtils.getEnumIgnoreCase(DnsProviderType.class, providerType, DnsProviderType.PowerDNS);
        if (dnsProviderType == null) {
            throw new InvalidParameterValueException(String.format("Invalid value passed for provider type, valid values are: %s",
                    EnumUtils.listValues(DnsProviderType.values())));
        }
        return dnsProviderType;
    }

    /////////////////////////////////////////////////////
    /////////////// Implementation //////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<DnsServerResponse> response = dnsProviderManager.listDnsServers(this);
        response.setResponseName(getCommandName());
        response.setObjectName("dnsserver");
        setResponseObject(response);
    }
}
