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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import org.apache.cloudstack.api.command.user.dns.RegisterDnsRecordForVmCmd;
import org.apache.cloudstack.api.command.user.dns.RemoveDnsRecordForVmCmd;
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
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

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
    @Inject
    UserVmDao userVmDao;
    @Inject
    NicDao nicDao;

    private DnsProvider getProvider(DnsProviderType type) {
        if (type == null) {
            throw new CloudRuntimeException("Provider type cannot be null");
        }
        for (DnsProvider provider : dnsProviders) {
            if (provider.getProviderType() == type) {
                return provider;
            }
        }
        throw new CloudRuntimeException("No plugin found for DNS provider type: " + type);
    }

    @Override
    public DnsServer addDnsServer(AddDnsServerCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        DnsServer existing = dnsServerDao.findByUrlAndAccount(cmd.getUrl(), caller.getId());
        if (existing != null) {
            throw new InvalidParameterValueException(
                    "This Account already has a DNS server integration for URL: " + cmd.getUrl());
        }

        boolean isDnsPublic = cmd.isPublic();
        String publicDomainSuffix = cmd.getPublicDomainSuffix();
        if (caller.getType().equals(Account.Type.NORMAL)) {
            logger.info("Only admin and domain admin users are allowed to configure a public DNS server");
            isDnsPublic = false;
            publicDomainSuffix = null;
        }

        if (StringUtils.isNotBlank(publicDomainSuffix)) {
            publicDomainSuffix = DnsUtil.normalizeDomain(publicDomainSuffix);
        }

        DnsProviderType type = DnsProviderType.fromString(cmd.getProvider());
        DnsServerVO server = new DnsServerVO(cmd.getName(), cmd.getUrl(), cmd.getPort(), cmd.getExternalServerId(), type,
                cmd.getDnsUserName(), cmd.getCredentials(), isDnsPublic, publicDomainSuffix, cmd.getNameServers(),
                caller.getAccountId(), caller.getDomainId());
        try {
            DnsProvider provider = getProvider(type);
            String dnsServerId = provider.validateAndResolveServer(server); // localhost for PowerDNS
            if (StringUtils.isNotBlank(dnsServerId)) {
                server.setExternalServerId(dnsServerId);
            }
            return dnsServerDao.persist(server);
        } catch (Exception ex) {
            logger.error("Failed to validate DNS server", ex);
            throw new CloudRuntimeException("Failed to validate DNS server");
        }
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
            throw new InvalidParameterValueException(String.format("DNS server with ID: %s not found.", dnsServerId));
        }

        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, dnsServer);

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
                    throw new InvalidParameterValueException("Another DNS server with this URL already exists.");
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
            dnsServer.setPublicDomainSuffix(DnsUtil.normalizeDomain(cmd.getPublicDomainSuffix()));
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
            throw new InvalidParameterValueException(String.format("DNS server with ID: %s not found.", dnsServerId));
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, dnsServer);
        return dnsServerDao.remove(dnsServerId);
    }

    @Override
    public DnsServerResponse createDnsServerResponse(DnsServer server) {
        DnsServerResponse response = new DnsServerResponse();
        response.setId(server.getUuid());
        response.setName(server.getName());
        response.setUrl(server.getUrl());
        response.setPort(server.getPort());
        response.setProvider(server.getProviderType());
        response.setPublic(server.isPublic());
        response.setNameServers(server.getNameServers());
        response.setPublicDomainSuffix(server.getPublicDomainSuffix());
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
            throw new InvalidParameterValueException("DNS zone not found for the given ID.");
        }

        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);
        DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());
        if (server != null && zone.getState() == DnsZone.State.Active) {
            try {
                DnsProvider provider = getProvider(server.getProviderType());
                provider.deleteZone(server, zone);
                logger.debug("Deleted DNS zone: {}", zone.getName());
            } catch (Exception ex) {
                logger.error("Failed to delete DNS zone from provider", ex);
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
        DnsZoneVO dnsZone = dnsZoneDao.findById(cmd.getId());
        if (dnsZone == null) {
            throw new InvalidParameterValueException("DNS zone not found.");
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, dnsZone);
        boolean updated = false;
        if (cmd.getDescription() != null) {
            dnsZone.setDescription(cmd.getDescription());
            updated = true;
        }

        if (updated) {
            DnsServerVO server = dnsServerDao.findById(dnsZone.getDnsServerId());
            if (server == null) {
                throw new CloudRuntimeException("The underlying DNS server for this DNS zone is missing.");
            }
            try {
                DnsProvider provider = getProvider(server.getProviderType());
                provider.updateZone(server, dnsZone);
            } catch (Exception ex) {
                logger.error("Failed to update DNS zone: {} on DNS server: {}", dnsZone.getName(), server.getName(), ex);
                throw new CloudRuntimeException("Failed to update DNS zone: " + dnsZone.getName());
            }
        }
        return dnsZone;
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
    public DnsRecordResponse createDnsRecord(CreateDnsRecordCmd cmd) {
        String recordName = StringUtils.trimToEmpty(cmd.getName()).toLowerCase();
        if (StringUtils.isBlank(recordName)) {
            throw new InvalidParameterValueException("Empty DNS record name is not allowed");
        }
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("DNS zone not found.");
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);
        DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());
        try {
            DnsRecord.RecordType type = cmd.getType();
            List<String> normalizedContents = cmd.getContents().stream()
                    .map(value -> DnsUtil.normalizeDnsRecordValue(value, type)).collect(Collectors.toList());
            DnsRecord record = new DnsRecord(recordName, type, normalizedContents, cmd.getTtl());
            DnsProvider provider = getProvider(server.getProviderType());
            String normalizedRecordName = provider.addRecord(server, zone, record);
            record.setName(normalizedRecordName);
            return createDnsRecordResponse(record);
        } catch (Exception ex) {
            logger.error("Failed to add DNS record via provider", ex);
            throw new CloudRuntimeException(String.format("Failed to add DNS record: %s", recordName));
        }
    }

    @Override
    public boolean deleteDnsRecord(DeleteDnsRecordCmd cmd) {
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("DNS zone not found.");
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
            throw new CloudRuntimeException(String.format("Failed to delete DNS record: %s", cmd.getName()));
        }
    }

    @Override
    public ListResponse<DnsRecordResponse> listDnsRecords(ListDnsRecordsCmd cmd) {
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("DNS zone not found for the given ID.");
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);
        DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());
        if (server == null) {
            throw new CloudRuntimeException("The underlying DNS server for this DNS zone is missing.");
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
        if (StringUtils.isBlank(cmd.getName())) {
            throw new InvalidParameterValueException("DNS zone name cannot be empty");
        }

        String dnsZoneName = DnsUtil.normalizeDomain(cmd.getName());
        DnsServerVO server = dnsServerDao.findById(cmd.getDnsServerId());
        if (server == null) {
            throw new InvalidParameterValueException(String.format("DNS server not found for the given ID: %s", cmd.getDnsServerId()));
        }

        Account caller = CallContext.current().getCallingAccount();
        boolean isOwner = (server.getAccountId() == caller.getId());
        if (!isOwner) {
            if (!server.isPublic()) {
                throw new PermissionDeniedException("You do not have permission to use this DNS server.");
            }
            dnsZoneName = DnsUtil.appendPublicSuffixToZone(dnsZoneName, DnsUtil.normalizeDomain(server.getPublicDomainSuffix()));
        }
        DnsZone.ZoneType type = cmd.getType();
        DnsZoneVO existing = dnsZoneDao.findByNameServerAndType(dnsZoneName, server.getId(), type);
        if (existing != null) {
            throw new InvalidParameterValueException("DNS zone already exists on this server.");
        }
        DnsZoneVO dnsZoneVO = new DnsZoneVO(dnsZoneName, type, server.getId(), caller.getId(), caller.getDomainId(), cmd.getDescription());
        return dnsZoneDao.persist(dnsZoneVO);
    }

    @Override
    public DnsZone provisionDnsZone(long dnsZoneId) {
        DnsZoneVO dnsZone = dnsZoneDao.findById(dnsZoneId);
        if (dnsZone == null) {
            throw new CloudRuntimeException("DNS zone not found during provisioning");
        }
        DnsServerVO server = dnsServerDao.findById(dnsZone.getDnsServerId());
        try {
            DnsProvider provider = getProvider(server.getProviderType());
            String externalReferenceId = provider.provisionZone(server, dnsZone);
            dnsZone.setExternalReference(externalReferenceId);
            dnsZone.setState(DnsZone.State.Active);
            dnsZoneDao.update(dnsZone.getId(), dnsZone);
            logger.debug("DNS zone: {} created successfully on DNS server: {} with ID: {}", dnsZone.getName(), server.getName(), dnsZoneId);
        } catch (Exception ex) {
            dnsZoneDao.remove(dnsZoneId);
            logger.error("Failed to provision DNS zone: {} on DNS server: {}", dnsZone.getName(), server.getName(), ex);
            throw new CloudRuntimeException("Failed to provision DNS zone: " + dnsZone.getName());
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
            throw new InvalidParameterValueException("DNS zone not found.");
        }
        accountMgr.checkAccess(caller, null, true, zone);

        NetworkVO network = networkDao.findById(cmd.getNetworkId());
        if (network == null) {
            throw new InvalidParameterValueException("Network not found.");
        }

        if (!NetworkVO.GuestType.Shared.equals(network.getGuestType())) {
            throw new CloudRuntimeException(String.format("Operation is not permitted for network type: %s", network.getGuestType()));
        }
        accountMgr.checkAccess(caller, null, true, network);
        DnsZoneNetworkMapVO existing = dnsZoneNetworkMapDao.findByZoneAndNetwork(zone.getId(), network.getId());
        if (existing != null) {
            throw new InvalidParameterValueException("This DNS zone is already associated with this Network.");
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
    public boolean registerDnsRecordForVm(RegisterDnsRecordForVmCmd cmd) {
        return processDnsRecordForInstance(cmd.getVmId(), cmd.getNetworkId(), true);
    }

    @Override
    public boolean removeDnsRecordForVm(RemoveDnsRecordForVmCmd cmd) {
        return processDnsRecordForInstance(cmd.getVmId(), cmd.getNetworkId(), false);
    }

    /**
     * Helper method to handle both Register and Remove logic for Instance
     */
    private boolean processDnsRecordForInstance(Long instanceId, Long networkId, boolean isAdd) {
        // 1. Fetch VM and verify access
        UserVmVO instance = userVmDao.findById(instanceId);
        if (instance == null) {
            throw new InvalidParameterValueException("Provided Instance not found.");
        }
        accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, instance);

        // 2. Resolve the NIC and Network
        NicVO nic;
        if (networkId != null) {
            nic = nicDao.findByNtwkIdAndInstanceId(networkId, instance.getId());
        } else {
            nic = nicDao.findDefaultNicForVM(instance.getId());
            networkId = nic != null ? nic.getNetworkId() : null;
        }

        // networkId may not be of Shared network type
        // there might be multiple shared networks
        // possible to have dns record for secondary ip

        if (nic == null) {
            throw new CloudRuntimeException("No valid NIC found for this Instance on the specified Network.");
        }

        // 3. Find if this network is linked to any DNS Zones
        List<DnsZoneNetworkMapVO> mappings = dnsZoneNetworkMapDao.listByNetworkId(networkId);
        if (mappings == null || mappings.isEmpty()) {
            throw new CloudRuntimeException("No DNS zones are mapped to this network. Please associate a zone first.");
        }

        boolean atLeastOneSuccess = false;
        // 4. Iterate over mapped zones and push the record
        for (DnsZoneNetworkMapVO map : mappings) {
            DnsZoneVO zone = dnsZoneDao.findById(map.getDnsZoneId());
            if (zone == null || zone.getState() != DnsZone.State.Active) {
                continue;
            }

            DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());

            // Construct FQDN Prefix (e.g., "instance-id" or "instance-id.subdomain")
            String recordName = String.valueOf(instance.getInstanceName());
            if (StringUtils.isNotBlank(map.getSubDomain() )) {
                recordName = recordName + "." + map.getSubDomain();
            }

            try {
                DnsProvider provider = getProvider(server.getProviderType());
                // Handle IPv4 (A Record)
                if (nic.getIPv4Address() != null) {
                    DnsRecord recordA = new DnsRecord(recordName, DnsRecord.RecordType.A, Collections.singletonList(nic.getIPv4Address()), 3600);
                    if (isAdd) {
                        provider.addRecord(server, zone, recordA);
                    } else {
                        provider.deleteRecord(server, zone, recordA);
                    }
                    atLeastOneSuccess = true;
                }

                // Handle IPv6 (AAAA Record) if it exists
                if (nic.getIPv6Address() != null) {
                    DnsRecord recordAAAA = new DnsRecord(recordName, DnsRecord.RecordType.AAAA, Collections.singletonList(nic.getIPv6Address()), 3600);
                    if (isAdd) {
                        provider.addRecord(server, zone, recordAAAA);
                    } else {
                        provider.deleteRecord(server, zone, recordAAAA);
                    }
                    atLeastOneSuccess = true;
                }

            } catch (Exception ex) {
                logger.error(
                        "Failed to {} DNS record for Instance {} in zone {}",
                        isAdd ? "register" : "remove",
                        instance.getHostName(),
                        zone.getName(),
                        ex
                );
            }
        }

        if (!atLeastOneSuccess) {
            throw new CloudRuntimeException("Failed to process DNS records. Ensure the Instance has a valid IP address.");
        }
        return true;
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
        cmdList.add(DisassociateDnsZoneFromNetworkCmd.class);

        // DNS Record Commands
        cmdList.add(CreateDnsRecordCmd.class);
        cmdList.add(ListDnsRecordsCmd.class);
        cmdList.add(DeleteDnsRecordCmd.class);
        cmdList.add(RegisterDnsRecordForVmCmd.class);
        cmdList.add(RemoveDnsRecordForVmCmd.class);
        return cmdList;
    }

    public List<DnsProvider> getDnsProviders() {
        return dnsProviders;
    }

    public void setDnsProviders(List<DnsProvider> dnsProviders) {
        this.dnsProviders = dnsProviders;
    }
}
