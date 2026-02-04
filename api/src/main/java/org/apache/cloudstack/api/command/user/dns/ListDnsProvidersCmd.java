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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.response.DnsProviderResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.dns.DnsProviderManager;

@APICommand(name = "listDnsProviders", description = "Lists available DNS plugin providers",
        responseObject = DnsProviderResponse.class, requestHasSensitiveInfo = false)
public class ListDnsProvidersCmd extends BaseListCmd {

    @Inject
    DnsProviderManager dnsManager;

    @Override
    public void execute() {
        List<String> providers = dnsManager.listProviderNames();
        ListResponse<DnsProviderResponse> response = new ListResponse<>();
        List<DnsProviderResponse> responses = new ArrayList<>();
        for (String name : providers) {
            DnsProviderResponse resp = new DnsProviderResponse();
            resp.setName(name);
            resp.setObjectName("dnsprovider");
            responses.add(resp);
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
