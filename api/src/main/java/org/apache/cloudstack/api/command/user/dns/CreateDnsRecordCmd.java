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
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.DnsRecord;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.EnumUtils;

@APICommand(name = "createDnsRecord",
        description = "Creates a DNS record directly on the provider",
        responseObject = DnsRecordResponse.class,
        entityType = {DnsRecord.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateDnsRecordCmd extends BaseAsyncCmd {

    @ACL
    @Parameter(name = ApiConstants.DNS_ZONE_ID, type = CommandType.UUID, entityType = DnsZoneResponse.class, required = true,
            description = "ID of the DNS zone")
    private Long dnsZoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Record name")
    private String name;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true, description = "Record type (A, CNAME)")
    private String type;

    @Parameter(name = ApiConstants.CONTENTS, type = CommandType.LIST, collectionType = CommandType.STRING, required = true,
            description = "The content of the record (IP address for A/AAAA, FQDN for CNAME/NS, quoted string for TXT, etc.)")
    private List<String> contents;

    @Parameter(name = "ttl", type = CommandType.INTEGER, description = "Time to live")
    private Integer ttl;

    // Getters
    public Long getDnsZoneId() { return dnsZoneId; }
    public String getName() { return name; }

    public List<String> getContents() { return contents; }
    public Integer getTtl() { return (ttl == null) ? 3600 : ttl; }

    public DnsRecord.RecordType getType() {
        DnsRecord.RecordType dnsRecordType = EnumUtils.getEnumIgnoreCase(DnsRecord.RecordType.class, type);
        if (dnsRecordType == null) {
            throw new InvalidParameterValueException("Invalid value passed for record type, valid values are: " + EnumUtils.listValues(DnsRecord.RecordType.values()));
        }
        return dnsRecordType;
    }

    @Override
    public void execute() {
        try {
            DnsRecordResponse response = dnsProviderManager.createDnsRecord(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create DNS Record: " + e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() { return CallContext.current().getCallingAccount().getId(); }

    @Override
    public String getEventType() { return EventTypes.EVENT_DNS_RECORD_CREATE; }

    @Override
    public String getEventDescription() { return "Creating DNS Record: " + getName(); }
}
