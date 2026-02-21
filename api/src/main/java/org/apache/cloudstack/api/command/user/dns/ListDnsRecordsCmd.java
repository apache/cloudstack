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
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.dns.DnsRecord;

@APICommand(name = "listDnsRecords",
        description = "Lists DNS records from the external provider",
        responseObject = DnsRecordResponse.class,
        entityType = {DnsRecord.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListDnsRecordsCmd extends BaseListCmd {

    @ACL
    @Parameter(name = ApiConstants.DNS_ZONE_ID, type = CommandType.UUID, entityType = DnsZoneResponse.class, required = true,
            description = "ID of the DNS zone to list records from")
    private Long dnsZoneId;

    public Long getDnsZoneId() {
        return dnsZoneId;
    }

    @Override
    public void execute() {
        // The manager will fetch live data from the plugin
        ListResponse<DnsRecordResponse> response = dnsProviderManager.listDnsRecords(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}