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

import java.util.Arrays;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.DnsZone;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.utils.EnumUtils;

@APICommand(name = "createDnsZone",
        description = "Creates a new DNS Zone on a specific server",
        responseObject = DnsZoneResponse.class,
        entityType = {DnsZone.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateDnsZoneCmd extends BaseAsyncCreateCmd {

    /////////////////////////////////////////////////////
    //////////////// API Parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "The name of the DNS zone (e.g. example.com)")
    private String name;

    @ACL
    @Parameter(name = ApiConstants.DNS_SERVER_ID, type = CommandType.UUID, entityType = DnsServerResponse.class,
            required = true, description = "The ID of the DNS server to host this zone")
    private Long dnsServerId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING,
            description = "The type of zone (Public, Private). Defaults to Public.")
    private String type;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Display text for the zone")
    private String description;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public Long getDnsServerId() {
        return dnsServerId;
    }

    public DnsZone.ZoneType getType() {
        if (StringUtils.isBlank(type)) {
            return DnsZone.ZoneType.Public;
        }
        DnsZone.ZoneType zoneType = EnumUtils.getEnumIgnoreCase(DnsZone.ZoneType.class, type);
        if (type == null) {
            throw new IllegalArgumentException("Invalid type value, supported values are: " + Arrays.toString(DnsZone.ZoneType.values()));
        }
        return zoneType;
    }

    public String getDescription() {
        return description;
    }

    /////////////////////////////////////////////////////
    /////////////// Implementation //////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void create() throws ResourceAllocationException {
        try {
            DnsZone zone = dnsProviderManager.allocateDnsZone(this);
            if (zone != null) {
                setEntityId(zone.getId());
                setEntityUuid(zone.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create DNS Zone entity");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to allocate DNS Zone: " + e.getMessage());
        }
    }

    @Override
    public void execute() {
        try {
            DnsZone result = dnsProviderManager.provisionDnsZone(getEntityId());
            if (result != null) {
                DnsZoneResponse response = dnsProviderManager.createDnsZoneResponse(result);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to provision DNS Zone on external provider");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to provision DNS Zone: " + e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DNS_ZONE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating DNS zone: " + getName();
    }
}
