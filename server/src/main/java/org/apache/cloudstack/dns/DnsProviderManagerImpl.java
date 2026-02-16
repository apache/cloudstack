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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd;
import org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd;
import org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd;
import org.apache.cloudstack.api.command.user.dns.CreateDnsZoneCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsServerCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsZoneCmd;
import org.apache.cloudstack.api.command.user.dns.DisassociateDnsZoneFromNetworkCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsProvidersCmd;
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
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.dao.DnsServerDao;
import org.apache.cloudstack.dns.dao.DnsZoneDao;
import org.apache.cloudstack.dns.dao.DnsZoneNetworkMapDao;
import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.apache.cloudstack.dns.vo.DnsZoneNetworkMapVO;
import org.apache.cloudstack.dns.vo.DnsZoneVO;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
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
    @Inject
    DnsZoneDao dnsZoneDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    DnsZoneNetworkMapDao dnsZoneNetworkMapDao;

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
        if (cmd.getState() != null) {
            dnsServer.setState(cmd.getState());
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
    public boolean deleteDnsZone(Long zoneId) {
        DnsZoneVO zone = dnsZoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("DNS Zone with ID " + zoneId + " not found.");
        }

        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);
        DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());
        if (server != null && zone.getState() == DnsZone.State.Active) {
            try {
                DnsProvider provider = getProvider(server.getProviderType());
                logger.debug("Deleting DNS zone {} from provider.", zone.getName());
                provider.deleteZone(server, zone);
            } catch (Exception ex) {
                logger.error("Failed to delete zone from provider", ex);
                throw new CloudRuntimeException("Failed to delete DNS zone.");
            }
        }
        return dnsZoneDao.remove(zoneId);
    }

    @Override
    public DnsZone getDnsZone(Long id) {
        return dnsZoneDao.findById(id);
    }

    @Override
    public DnsZone updateDnsZone(UpdateDnsZoneCmd cmd) {
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getId());
        if (zone == null) {
            throw new InvalidParameterValueException("DNS zone not found.");
        }

        // ACL Check
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);

        // Update fields
        boolean updated = false;
        if (cmd.getDescription() != null) {
            zone.setDescription(cmd.getDescription());
            updated = true;
        }

        if (updated) {
            dnsZoneDao.update(zone.getId(), zone);
        }
        return zone;
    }

    @Override
    public ListResponse<DnsZoneResponse> listDnsZones(ListDnsZonesCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Filter searchFilter = new Filter(DnsZoneVO.class, ApiConstants.ID, true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long accountIdFilter = caller.getAccountId();
        String keyword = cmd.getKeyword();
        if (cmd.getName() != null) {
            keyword = cmd.getName();
        }
        Pair<List<DnsZoneVO>, Integer> result = dnsZoneDao.searchZones(cmd.getId(), cmd.getDnsServerId(), keyword, accountIdFilter, searchFilter);
        List<DnsZoneResponse> zoneResponses = new ArrayList<>();
        for (DnsZoneVO zone : result.first()) {
            zoneResponses.add(createDnsZoneResponse(zone));
        }
        ListResponse<DnsZoneResponse> response = new ListResponse<>();
        response.setResponses(zoneResponses, result.second());
        return response;
    }

    @Override
    public DnsZone getDnsZone(long id) {
        return null;
    }

    @Override
    public DnsRecordResponse createDnsRecord(CreateDnsRecordCmd cmd) {
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("DNS Zone not found.");
        }

        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);
        DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());
        try {
            DnsRecord record = new DnsRecord(cmd.getName(), cmd.getType(), cmd.getContents(), cmd.getTtl());
            DnsProvider provider = getProvider(server.getProviderType());
            // Add Record via Provider
            provider.addRecord(server, zone, record);
            return createDnsRecordResponse(record);
        } catch (Exception ex) {
            logger.error("Failed to add DNS record via provider", ex);
            throw new CloudRuntimeException(String.format("Failed to add DNS record: %s", cmd.getName()));
        }
    }

    @Override
    public boolean deleteDnsRecord(DeleteDnsRecordCmd cmd) {
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("DNS Zone not found.");
        }

        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);

        DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());

        try {
            // Reconstruct the record DTO just for deletion criteria
            DnsRecord record = new DnsRecord();
            record.setName(cmd.getName());
            record.setType(cmd.getType());
            DnsProvider provider = getProvider(server.getProviderType());
            provider.deleteRecord(server, zone, record);
            return true;
        } catch (Exception ex) {
            logger.error("Failed to delete DNS record via provider", ex);
            throw new CloudRuntimeException(String.format("Failed to delete record: %s", cmd.getName()));
        }
    }

    @Override
    public ListResponse<DnsRecordResponse> listDnsRecords(ListDnsRecordsCmd cmd) {
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException(String.format("DNS Zone with ID %s not found.", cmd.getDnsZoneId()));
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);
        DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());
        if (server == null) {
            throw new CloudRuntimeException("The underlying DNS Server for this zone is missing.");
        }
        try {
            DnsProvider provider = getProvider(server.getProviderType());
            List<DnsRecord> records = provider.listRecords(server, zone);
            List<DnsRecordResponse> responses = new ArrayList<>();
            for (DnsRecord record : records) {
                responses.add(createDnsRecordResponse(record));
            }

            ListResponse<DnsRecordResponse> listResponse = new ListResponse<>();
            listResponse.setResponses(responses, responses.size());
            return listResponse;
        } catch (Exception ex) {
            logger.error("Failed to list DNS records from provider", ex);
            throw new CloudRuntimeException("Failed to fetch DNS records");
        }
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
    public DnsZone allocateDnsZone(CreateDnsZoneCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        DnsServerVO server = dnsServerDao.findById(cmd.getDnsServerId());
        if (server == null) {
            throw new InvalidParameterValueException("DNS Server not found");
        }
        boolean isOwner = (server.getAccountId() == caller.getId());
        if (!server.isPublic() && !isOwner) {
            throw new PermissionDeniedException("You do not have permission to use this DNS Server.");
        }
        DnsZone.ZoneType type = DnsZone.ZoneType.Public;
        if (cmd.getType() != null) {
            try {
                type = DnsZone.ZoneType.valueOf(cmd.getType());
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("Invalid Zone Type");
            }
        }
        DnsZoneVO existing = dnsZoneDao.findByNameServerAndType(cmd.getName(), server.getId(), type);
        if (existing != null) {
            throw new InvalidParameterValueException("Zone already exists on this server.");
        }
        DnsZoneVO dnsZoneVO = new DnsZoneVO(cmd.getName(), type, server.getId(), caller.getId(), caller.getDomainId(), cmd.getDescription());
        return dnsZoneDao.persist(dnsZoneVO);
    }

    @Override
    public DnsZone provisionDnsZone(long zoneId) {
        DnsZoneVO dnsZone = dnsZoneDao.findById(zoneId);
        if (dnsZone == null) {
            throw new CloudRuntimeException("DNS Zone not found during provisioning");
        }
        DnsServerVO server = dnsServerDao.findById(dnsZone.getDnsServerId());

        try {
            DnsProvider provider = getProvider(server.getProviderType());
            logger.debug("Provision DNS zone: {} on DNS server: {}", dnsZone.getName(), server.getName());
            provider.provisionZone(server, dnsZone);
            dnsZone.setState(DnsZone.State.Active);
            dnsZoneDao.update(dnsZone.getId(), dnsZone);
        } catch (Exception ex) {
            logger.error("Failed to provision zone: {} on server: {}", dnsZone.getName(), server.getName(), ex);
            dnsZoneDao.remove(zoneId);
            throw new CloudRuntimeException("Failed to provision zone: " + dnsZone.getName());
        }
        return dnsZone;
    }

    @Override
    public DnsZoneResponse createDnsZoneResponse(DnsZone zone) {
        DnsZoneResponse res = new DnsZoneResponse();
        res.setName(zone.getName());
        res.setDnsServerId(zone.getDnsServerId());
        res.setType(zone.getType());
        res.setState(zone.getState());
        res.setId(zone.getUuid());
        res.setDescription(zone.getDescription());
        return res;
    }

    @Override
    public DnsRecordResponse createDnsRecordResponse(DnsRecord record) {
        DnsRecordResponse res = new DnsRecordResponse();
        res.setName(record.getName());
        res.setType(record.getType());
        res.setContent(record.getContents());
        return res;
    }

    @Override
    public DnsZoneNetworkMapResponse associateZoneToNetwork(AssociateDnsZoneToNetworkCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("DNS Zone not found.");
        }
        accountMgr.checkAccess(caller, null, true, zone);

        NetworkVO network = networkDao.findById(cmd.getNetworkId());
        if (network == null) {
            throw new InvalidParameterValueException("Network not found.");
        }
        accountMgr.checkAccess(caller, null, true, network);
        DnsZoneNetworkMapVO existing = dnsZoneNetworkMapDao.findByZoneAndNetwork(zone.getId(), network.getId());
        if (existing != null) {
            throw new InvalidParameterValueException("This DNS Zone is already associated with this Network.");
        }
        DnsZoneNetworkMapVO mapping = new DnsZoneNetworkMapVO(zone.getId(), network.getId(), cmd.getSubDomain());
        dnsZoneNetworkMapDao.persist(mapping);
        DnsZoneNetworkMapResponse response = new DnsZoneNetworkMapResponse();
        response.setId(mapping.getUuid());
        response.setDnsZoneId(zone.getUuid());
        response.setNetworkId(network.getUuid());
        response.setSubDomain(mapping.getSubDomain());
        return response;
    }

    @Override
    public boolean disassociateZoneFromNetwork(DisassociateDnsZoneFromNetworkCmd cmd) {
        DnsZoneNetworkMapVO mapping = dnsZoneNetworkMapDao.findById(cmd.getId());
        if (mapping == null) {
            throw new InvalidParameterValueException("The specified DNS zone to network mapping does not exist.");
        }
        DnsZoneVO zone = dnsZoneDao.findById(mapping.getDnsZoneId());
        if (zone == null) {
            // If the zone is missing but the mapping exists (shouldn't happen due to CASCADE DELETE),
            // clean up the orphaned mapping.
            return dnsZoneNetworkMapDao.remove(mapping.getId());
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);
        return dnsZoneNetworkMapDao.remove(mapping.getId());
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
        cmdList.add(UpdateDnsZoneCmd.class);
        cmdList.add(AssociateDnsZoneToNetworkCmd.class);

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
