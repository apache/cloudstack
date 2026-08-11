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

package org.apache.cloudstack.dns;

import java.util.List;

import org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd;
import org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd;
import org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd;
import org.apache.cloudstack.api.command.user.dns.CreateDnsZoneCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsServerCmd;
import org.apache.cloudstack.api.command.user.dns.DisassociateDnsZoneFromNetworkCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsRecordsCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsServersCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsZonesCmd;
import org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd;
import org.apache.cloudstack.api.command.user.dns.UpdateDnsZoneCmd;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneNetworkMapResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.user.Account;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;

public interface DnsProviderManager extends Manager, PluggableService {

    DnsServer addDnsServer(AddDnsServerCmd cmd);
    ListResponse<DnsServerResponse> listDnsServers(ListDnsServersCmd cmd);
    DnsServer updateDnsServer(UpdateDnsServerCmd cmd);
    boolean deleteDnsServer(DeleteDnsServerCmd cmd);
    DnsServerResponse createDnsServerResponse(DnsServer server);

    // Allocates the DB row (State: Inactive)
    DnsZone allocateDnsZone(CreateDnsZoneCmd cmd);
    // Calls the Plugin (State: Inactive -> Active)
    DnsZone provisionDnsZone(long zoneId, boolean isImport);

    DnsZone updateDnsZone(UpdateDnsZoneCmd cmd);
    boolean deleteDnsZone(Long id, boolean isUnmanage);
    ListResponse<DnsZoneResponse> listDnsZones(ListDnsZonesCmd cmd);

    DnsRecordResponse createDnsRecord(CreateDnsRecordCmd cmd);
    boolean deleteDnsRecord(DeleteDnsRecordCmd cmd);
    ListResponse<DnsRecordResponse> listDnsRecords(ListDnsRecordsCmd cmd);

    List<String> listProviderNames();

    // Helper to create the response object
    DnsZoneResponse createDnsZoneResponse(DnsZone zone);
    DnsRecordResponse createDnsRecordResponse(DnsRecord record);

    DnsZoneNetworkMapResponse associateZoneToNetwork(AssociateDnsZoneToNetworkCmd cmd);

    boolean disassociateZoneFromNetwork(DisassociateDnsZoneFromNetworkCmd cmd);

    void checkDnsServerPermission(Account caller, DnsServer dnsServer);

    void checkDnsZonePermission(Account caller, DnsZone dnsZone);
}
