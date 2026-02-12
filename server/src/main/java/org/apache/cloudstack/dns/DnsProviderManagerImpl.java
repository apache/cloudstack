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

import javax.inject.Inject;

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
import org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.dao.DnsServerDao;
import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DnsProviderManagerImpl extends ManagerBase implements DnsProviderManager, PluggableService {
    List<DnsProvider> dnsProviders;
    @Inject
    AccountManager accountMgr;
    @Inject
    DnsServerDao dnsServerDao;

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
        Account caller = CallContext.current().getCallingAccount();
        DnsServer existing = dnsServerDao.findByUrlAndAccount(cmd.getUrl(), caller.getId());
        if (existing != null) {
            throw new InvalidParameterValueException(
                    "This Account already has a DNS Server integration for URL: " + cmd.getUrl());
        }

        DnsProviderType type = DnsProviderType.fromString(cmd.getProvider());
        DnsProvider provider = getProvider(type);
        DnsServerVO server = new DnsServerVO(cmd.getName(), cmd.getUrl(), type, cmd.getCredentials(), cmd.getPort(),
                cmd.isPublic(), cmd.getPublicDomainSuffix(), cmd.getNameServers(), caller.getId());
        try {
            provider.validate(server);
        } catch (Exception ex) {
            logger.error("Failed to validate DNS server", ex);
            throw new CloudRuntimeException("Failed to validate DNS server");
        }
        return dnsServerDao.persist(server);
    }

    @Override
    public ListResponse<DnsServerResponse> listDnsServers(ListDnsServersCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long accountIdFilter = null;
        if (accountMgr.isRootAdmin(caller.getId())) {
            // Root Admin: Can see all, unless they specifically ask for an account
            if (cmd.getAccountName() != null) {
                Account target = accountMgr.getActiveAccountByName(cmd.getAccountName(), cmd.getDomainId());
                if (target == null) {
                    return new ListResponse<>(); // Account not found
                }
                accountIdFilter = target.getId();
            }
        } else {
            // Regular User / Domain Admin: STRICTLY restricted to their own account
            accountIdFilter = caller.getId();
        }

        Filter searchFilter = new Filter(DnsServerVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<DnsServerVO>, Integer> result = dnsServerDao.searchDnsServers(cmd.getId(), cmd.getKeyword(),
                cmd.getProvider(), accountIdFilter, searchFilter);

        ListResponse<DnsServerResponse> response = new ListResponse<>();
        List<DnsServerResponse> serverResponses = new ArrayList<>();
        for (DnsServerVO server : result.first()) {
            serverResponses.add(createDnsServerResponse(server));
        }
        response.setResponses(serverResponses, result.second());
        return response;
    }

    @Override
    public DnsServer updateDnsServer(UpdateDnsServerCmd cmd) {
        Long dnsServerId = cmd.getId();
        DnsServerVO dnsServer = dnsServerDao.findById(dnsServerId);
        if (dnsServer == null) {
            throw new InvalidParameterValueException(String.format("DNS Server with ID: %s not found.", dnsServerId));
        }

        Account caller = CallContext.current().getCallingAccount();
        if (!accountMgr.isRootAdmin(caller.getId()) && dnsServer.getAccountId() != caller.getId()) {
            throw new PermissionDeniedException("You do not have permission to update this DNS server.");
        }

        boolean validationRequired = false;
        String originalUrl = dnsServer.getUrl();
        String originalKey = dnsServer.getApiKey();

        if (cmd.getName() != null) {
            dnsServer.setName(cmd.getName());
        }

        if (cmd.getUrl() != null) {
            if (!cmd.getUrl().equals(originalUrl)) {
                DnsServer duplicate = dnsServerDao.findByUrlAndAccount(cmd.getUrl(), dnsServer.getAccountId());
                if (duplicate != null && duplicate.getId() != dnsServer.getId()) {
                    throw new InvalidParameterValueException("Another DNS Server with this URL already exists.");
                }
                dnsServer.setUrl(cmd.getUrl());
                validationRequired = true;
            }
        }

        if (cmd.getCredentials() != null && !cmd.getCredentials().equals(originalKey)) {
            dnsServer.setApiKey(cmd.getCredentials());
            validationRequired = true;
        }

        if (cmd.getPort() != null) {
            dnsServer.setPort(cmd.getPort());
        }
        if (cmd.isPublic() != null) {
            dnsServer.setIsPublic(cmd.isPublic());
        }

        if (cmd.getPublicDomainSuffix() != null) {
            dnsServer.setPublicDomainSuffix(cmd.getPublicDomainSuffix());
        }

        if (cmd.getNameServers() != null) {
            dnsServer.setNameServers(cmd.getNameServers());
        }
        if (validationRequired) {
            DnsProvider provider = getProvider(dnsServer.getProviderType());
            try {
                provider.validate(dnsServer);
            } catch (Exception ex) {
                logger.error("Validation failed for DNS server", ex);
                throw new InvalidParameterValueException("Validation failed for DNS server");
            }
        }
        boolean updateStatus = dnsServerDao.update(dnsServerId, dnsServer);

        if (updateStatus) {
            return dnsServerDao.findById(dnsServerId);
        } else {
            throw new CloudRuntimeException(String.format("Unable to update DNS server: %s", dnsServer.getName()));
        }
    }

    @Override
    public boolean deleteDnsServer(DeleteDnsServerCmd cmd) {
        Long dnsServerId = cmd.getId();
        DnsServerVO dnsServer = dnsServerDao.findById(dnsServerId);
        if (dnsServer == null) {
            throw new InvalidParameterValueException(String.format("DNS Server with ID: %s not found.", dnsServerId));
        }
        Account caller = CallContext.current().getCallingAccount();
        if (!accountMgr.isRootAdmin(caller.getId()) && dnsServer.getAccountId() != caller.getId()) {
            throw new PermissionDeniedException("You do not have permission to delete this DNS server.");
        }
        return dnsServerDao.remove(dnsServerId);
    }

    @Override
    public DnsServerResponse createDnsServerResponse(DnsServer server) {
        DnsServerResponse response = new DnsServerResponse();
        response.setId(server.getUuid());
        response.setName(server.getName());
        response.setUrl(server.getUrl());
        response.setProvider(server.getProviderType());
        response.setPublic(server.isPublic());
        response.setObjectName("dnsserver");
        return response;
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

        cmdList.add(ListDnsProvidersCmd.class);
        // DNS Server Commands
        cmdList.add(AddDnsServerCmd.class);
        cmdList.add(ListDnsServersCmd.class);
        cmdList.add(DeleteDnsServerCmd.class);
        cmdList.add(UpdateDnsServerCmd.class);

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

    public List<DnsProvider> getDnsProviders() {
        return dnsProviders;
    }

    public void setDnsProviders(List<DnsProvider> dnsProviders) {
        this.dnsProviders = dnsProviders;
    }
}
