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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd;
import org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd;
import org.apache.cloudstack.api.command.user.dns.CreateDnsZoneCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsServerCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsZoneCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsProvidersCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsRecordsCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsServersCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsZonesCmd;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DnsProviderManagerImpl extends ManagerBase implements DnsProviderManager, PluggableService {
    @Autowired(required = false)
    List<DnsProvider> dnsProviders;

    private DnsProvider getProvider(DnsProviderType type) {
        if (type == null) {
            throw new CloudRuntimeException("Provider type cannot be null");
        }
        for (DnsProvider provider : dnsProviders) {
            if (provider.getProviderType() == type) {
                return provider;
            }
        }
        throw new CloudRuntimeException("No plugin found for DNS Provider type: " + type);
    }

    @Override
    public DnsServer addDnsServer(AddDnsServerCmd cmd) {
        return null;
    }

    @Override
    public ListResponse<DnsServerResponse> listDnsServers(ListDnsServersCmd cmd) {
        return null;
    }

    @Override
    public boolean deleteDnsServer(DeleteDnsServerCmd cmd) {
        return false;
    }

    @Override
    public DnsServerResponse createDnsServerResponse(DnsServer server) {
        return null;
    }

    @Override
    public DnsServer getDnsServer(Long id) {
        return null;
    }

    @Override
    public DnsZone createDnsZone(CreateDnsZoneCmd cmd) {
        return null;
    }

    @Override
    public boolean deleteDnsZone(DeleteDnsZoneCmd cmd) {
        return false;
    }

    @Override
    public ListResponse<DnsZoneResponse> listDnsZones(ListDnsZonesCmd cmd) {
        return null;
    }

    @Override
    public DnsZone getDnsZone(long id) {
        return null;
    }

    @Override
    public DnsRecordResponse createDnsRecord(CreateDnsRecordCmd cmd) {
        return null;
    }

    @Override
    public boolean deleteDnsRecord(DeleteDnsRecordCmd cmd) {
        return false;
    }

    @Override
    public ListResponse<DnsRecordResponse> listDnsRecords(ListDnsRecordsCmd cmd) {
        return null;
    }

    @Override
    public List<String> listProviderNames() {
        List<String> providerNames = new ArrayList<>();
        if (dnsProviders != null) {
            for (DnsProvider provider : dnsProviders) {
                providerNames.add(provider.getProviderType().toString());
            }
        }
        return providerNames;
    }

    @Override
    public DnsZone allocDnsZone(CreateDnsZoneCmd cmd) {
        return null;
    }

    @Override
    public DnsZone provisionDnsZone(long zoneId) {
        return null;
    }

    @Override
    public DnsZoneResponse createDnsZoneResponse(DnsZone zone) {
        return null;
    }

    @Override
    public boolean start() {
        if (dnsProviders == null || dnsProviders.isEmpty()) {
            logger.warn("DNS Framework started but no provider plugins were found!");
        } else {
            logger.info("DNS Framework started with: {} providers.",  dnsProviders.size());
        }
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        // DNS Server Commands
        cmdList.add(AddDnsServerCmd.class);
        cmdList.add(ListDnsServersCmd.class);
        cmdList.add(DeleteDnsServerCmd.class);
        cmdList.add(ListDnsProvidersCmd.class);

        // DNS Zone Commands
        cmdList.add(CreateDnsZoneCmd.class);
        cmdList.add(ListDnsZonesCmd.class);
        cmdList.add(DeleteDnsZoneCmd.class);

        // DNS Record Commands
        cmdList.add(CreateDnsRecordCmd.class);
        cmdList.add(ListDnsRecordsCmd.class);
        cmdList.add(DeleteDnsRecordCmd.class);
        return cmdList;
    }
}
