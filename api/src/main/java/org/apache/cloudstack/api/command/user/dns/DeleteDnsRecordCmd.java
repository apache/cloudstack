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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.DnsRecord;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.EnumUtils;

@APICommand(name = "deleteDnsRecord", description = "Deletes a DNS record from the external provider",
        responseObject = SuccessResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false, since = "4.23.0")
public class DeleteDnsRecordCmd extends BaseAsyncCmd {

    @Parameter(name = ApiConstants.DNS_ZONE_ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            required = true, description = "The ID of the DNS zone")
    private Long dnsZoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true)
    private String name;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true)
    private String type;

    // Getters
    public DnsRecord.RecordType getType() {
        DnsRecord.RecordType dnsRecordType = EnumUtils.getEnumIgnoreCase(DnsRecord.RecordType.class, type);
        if (dnsRecordType == null) {
            throw new InvalidParameterValueException("Invalid value passed for record type, valid values are: " + EnumUtils.listValues(DnsRecord.RecordType.values()));
        }
        return dnsRecordType;
    }
    public Long getDnsZoneId() { return dnsZoneId; }
    public String getName() { return name; }

    @Override
    public void execute() {
        try {
            boolean result = dnsProviderManager.deleteDnsRecord(this);
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete DNS Record");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Error deleting DNS Record: " + e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() { return CallContext.current().getCallingAccount().getId(); }

    @Override
    public String getEventType() { return EventTypes.EVENT_DNS_RECORD_DELETE; }

    @Override
    public String getEventDescription() { return "Deleting DNS Record: " + getName(); }
}
