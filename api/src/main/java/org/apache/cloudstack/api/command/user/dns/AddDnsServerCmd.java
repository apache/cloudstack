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
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.commons.lang3.BooleanUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.EnumUtils;

@APICommand(name = "addDnsServer",
        description = "Adds a new external DNS server",
        responseObject = DnsServerResponse.class,
        entityType = {DnsServer.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class AddDnsServerCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    ///
    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of the DNS server")
    private String name;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, description = "API URL of the provider")
    private String url;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, required = true, description = "Provider type (e.g., PowerDNS)")
    private String provider;

    @Parameter(name = ApiConstants.DNS_USER_NAME, type = CommandType.STRING,
            description = "Username or email associated with the external DNS provider account (used for authentication)")
    private String dnsUserName;

    @Parameter(name = ApiConstants.CREDENTIALS, required = true, type = CommandType.STRING, description = "API key or credentials for the external provider")
    private String credentials;

    @Parameter(name = ApiConstants.PORT, type = CommandType.INTEGER, description = "Port number of the external DNS server")
    private Integer port;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "Whether the DNS server is publicly accessible by other accounts")
    private Boolean isPublic;

    @Parameter(name = ApiConstants.PUBLIC_DOMAIN_SUFFIX, type = CommandType.STRING, description = "The domain suffix used for public access (e.g. public.example.com)")
    private String publicDomainSuffix;

    @Parameter(name = ApiConstants.NAME_SERVERS, type = CommandType.LIST, collectionType = CommandType.STRING,
            required = true, description = "Comma separated list of name servers")
    private List<String> nameServers;

    @Parameter(name = "externalserverid", type = CommandType.STRING, description = "External server id or hostname for the DNS server, e.g., 'localhost' for PowerDNS")
    private String externalServerId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() { return name; }

    public String getUrl() { return url; }

    public String getCredentials() {
        return credentials;
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

    public List<String> getNameServers() {
        return nameServers;
    }

    public DnsProviderType getProvider() {
        DnsProviderType dnsProviderType = EnumUtils.getEnumIgnoreCase(DnsProviderType.class, provider, DnsProviderType.PowerDNS);
        if (dnsProviderType == null) {
            throw new InvalidParameterValueException(String.format("Invalid value passed for provider type, valid values are: %s",
                    EnumUtils.listValues(DnsProviderType.values())));
        }
        return dnsProviderType;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public void execute() {
        try {
            DnsServer server = dnsProviderManager.addDnsServer(this);
            if (server != null) {
                DnsServerResponse response = dnsProviderManager.createDnsServerResponse(server);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add DNS server");
            }
        } catch (Exception ex) {
            logger.error("Failed to add DNS server", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    public String getExternalServerId() {
        return externalServerId;
    }

    public String getDnsUserName() {
        return dnsUserName;
    }
}
