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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.DnsProviderManager;
import org.apache.cloudstack.dns.DnsZone;

@APICommand(name = "updateDnsZone", description = "Updates a DNS Zone's metadata", responseObject = DnsZoneResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.23.0")
public class UpdateDnsZoneCmd extends BaseCmd {

    @Inject
    DnsProviderManager dnsProviderManager;

    /////////////////////////////////////////////////////
    //////////////// API Parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            required = true, description = "The ID of the DNS zone")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Display text for the zone")
    private String description;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// Implementation //////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            DnsZone result = dnsProviderManager.updateDnsZone(this);
            if (result != null) {
                DnsZoneResponse response = dnsProviderManager.createDnsZoneResponse(result);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update DNS Zone on external provider");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update DNS Zone: " + e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
