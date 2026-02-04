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

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.DnsProviderManager;
import org.apache.cloudstack.dns.DnsServer;

@APICommand(name = "addDnsServer", description = "Adds a new external DNS server",
        responseObject = DnsServerResponse.class, requestHasSensitiveInfo = true)
public class AddDnsServerCmd extends BaseCmd {

    @Inject
    DnsProviderManager dnsProviderManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    ///
    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of the DNS server")
    private String name;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, description = "API URL of the provider")
    private String url;

    @Parameter(name = "provider", type = CommandType.STRING, required = true, description = "Provider type (e.g., PowerDNS)")
    private String provider;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "API Username")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, description = "API Password or Token")
    private String password;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getProvider() { return provider; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    @Override
    public void execute() {
        DnsServer server = dnsProviderManager.addDnsServer(this);
        DnsServerResponse response = dnsProviderManager.createDnsServerResponse(server);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
