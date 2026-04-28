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

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.user.Account;
import com.cloud.utils.EnumUtils;

@APICommand(name = "updateDnsServer",
        description = "Update DNS server",
        responseObject = DnsServerResponse.class,
        entityType = {DnsServer.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateDnsServerCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsServerResponse.class,
            required = true, description = "The ID of the DNS server to update")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Name of the DNS server")
    private String name;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, description = "API URL of the provider")
    private String url;

    @Parameter(name = ApiConstants.DNS_API_KEY, type = CommandType.STRING, description = "API Key or Credentials for the external provider")
    private String dnsApiKey;

    @Parameter(name = ApiConstants.PORT, type = CommandType.INTEGER, description = "Port number of the external DNS server")
    private Integer port;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN,
            description = "Whether this DNS server can be used by accounts other than the owner to create and manage DNS zones")
    private Boolean isPublic;

    @Parameter(name = ApiConstants.PUBLIC_DOMAIN_SUFFIX, type = CommandType.STRING,
            description = "Domain suffix that restricts DNS zones created by non-owner accounts to subdomains of this " +
                    "suffix (for example, sub.example.com under example.com)")
    private String publicDomainSuffix;

    @Parameter(name = ApiConstants.NAME_SERVERS, type = CommandType.LIST, collectionType = CommandType.STRING,
            required = true,
            description = "Comma separated list of name servers; used to create NS records for the DNS Zone (for example, ns1.example.com, ns2.example.com)")
    private List<String> nameServers;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "Update state for the DNS server (Enabled, Disabled)")
    private String state;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getDnsApiKey() {
        return dnsApiKey;
    }
    public Integer getPort() {
        return port;
    }
    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }
    public String getPublicDomainSuffix() {
        return publicDomainSuffix;
    }
    public String getNameServers() { return String.join(",", nameServers); }

    @Override
    public long getEntityOwnerId() {
        DnsServer server = _entityMgr.findById(DnsServer.class, id);
        if (server != null) {
            return server.getAccountId();
        }
        // If server not found, return System to fail safely (or let manager handle 404)
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        try {
            DnsServer server = dnsProviderManager.updateDnsServer(this);
            if (server != null) {
                DnsServerResponse response = dnsProviderManager.createDnsServerResponse(server);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update DNS server");
            }
        } catch (Exception ex) {
            logger.error("Failed to add update server", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    public DnsServer.State getState() {
        if (StringUtils.isBlank(state)) {
            return null;
        }
        DnsServer.State dnsState = EnumUtils.getEnumIgnoreCase(DnsServer.State.class, state);
        if (dnsState == null) {
            throw new IllegalArgumentException("Invalid state value: " + state);
        }
        return dnsState;
    }
}
