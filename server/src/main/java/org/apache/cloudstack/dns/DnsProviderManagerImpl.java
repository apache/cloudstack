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

import static com.cloud.event.EventTypes.EVENT_DNS_RECORD_CREATE;
import static com.cloud.event.EventTypes.EVENT_DNS_RECORD_DELETE;
import static com.cloud.event.EventTypes.EVENT_NIC_CREATE;
import static com.cloud.event.EventTypes.EVENT_NIC_DELETE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

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
import org.apache.cloudstack.dns.dao.DnsNicJoinDao;
import org.apache.cloudstack.dns.dao.DnsServerDao;
import org.apache.cloudstack.dns.dao.DnsServerJoinDao;
import org.apache.cloudstack.dns.dao.DnsZoneDao;
import org.apache.cloudstack.dns.dao.DnsZoneJoinDao;
import org.apache.cloudstack.dns.dao.DnsZoneNetworkMapDao;
import org.apache.cloudstack.dns.exception.DnsConflictException;
import org.apache.cloudstack.dns.exception.DnsNotFoundException;
import org.apache.cloudstack.dns.exception.DnsProviderException;
import org.apache.cloudstack.dns.exception.DnsTransportException;
import org.apache.cloudstack.dns.vo.DnsNicJoinVO;
import org.apache.cloudstack.dns.vo.DnsServerJoinVO;
import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.apache.cloudstack.dns.vo.DnsZoneJoinVO;
import org.apache.cloudstack.dns.vo.DnsZoneNetworkMapVO;
import org.apache.cloudstack.dns.vo.DnsZoneVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.NicDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

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
    @Inject
    DomainDao domainDao;
    @Inject
    DnsZoneJoinDao dnsZoneJoinDao;
    @Inject
    DnsServerJoinDao dnsServerJoinDao;
    @Inject
    AccountDao accountDao;
    @Inject
    NicDetailsDao nicDetailsDao;
    @Inject
    MessageBus messageBus;
    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    DnsNicJoinDao dnsNicJoinDao;

    private DnsProvider getProviderByType(DnsProviderType type) {
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
    @ActionEvent(eventType = EventTypes.EVENT_DNS_SERVER_ADD, eventDescription = "Adding a DNS Server")
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
            publicDomainSuffix = DnsProviderUtil.normalizeDomain(publicDomainSuffix);
        }

        DnsProviderType type = cmd.getProvider();
        DnsServerVO server = new DnsServerVO(cmd.getName(), cmd.getUrl(), cmd.getPort(), cmd.getExternalServerId(), type,
                cmd.getDnsUserName(), cmd.getCredentials(), isDnsPublic, publicDomainSuffix, cmd.getNameServers(),
                caller.getAccountId(), caller.getDomainId());
        try {
            DnsProvider provider = getProviderByType(type);
            String dnsServerId = provider.validateAndResolveServer(server); // returns localhost for PowerDNS
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
        Pair<List<DnsServerVO>, Integer> result = searchForDnsServerInternal(cmd);
        ListResponse<DnsServerResponse> response = new ListResponse<>();
        if (result == null) {
            return response;
        }
        List<String> serverIds = new ArrayList<>();
        for (DnsServer server : result.first()) {
            serverIds.add(server.getUuid());
        }
        List<DnsServerJoinVO> joinResult = dnsServerJoinDao.listByUuids(serverIds);
        List<DnsServerResponse> serverResponses = new ArrayList<>();
        for (DnsServerJoinVO server : joinResult) {
            serverResponses.add(createDnsServerResponse(server));
        }
        response.setResponses(serverResponses, result.second());
        return response;
    }

    private Pair<List<DnsServerVO>, Integer> searchForDnsServerInternal(ListDnsServersCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Set<Long> parentDomainIds = domainDao.getDomainParentIds(caller.getDomainId());
        Filter searchFilter = new Filter(DnsServerVO.class, ApiConstants.ID, true, cmd.getStartIndex(), cmd.getPageSizeVal());
        return dnsServerDao.searchDnsServer(cmd.getId(), caller.getAccountId(), parentDomainIds, cmd.getProviderType(), cmd.getKeyword(), searchFilter);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DNS_SERVER_UPDATE, eventDescription = "Updating DNS Server")
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
            dnsServer.setPublicServer(cmd.isPublic());
        }

        if (cmd.getPublicDomainSuffix() != null) {
            dnsServer.setPublicDomainSuffix(DnsProviderUtil.normalizeDomain(cmd.getPublicDomainSuffix()));
        }

        if (cmd.getNameServers() != null) {
            dnsServer.setNameServers(cmd.getNameServers());
        }
        if (cmd.getState() != null) {
            dnsServer.setState(cmd.getState());
        }

        if (validationRequired) {
            DnsProvider provider = getProviderByType(dnsServer.getProviderType());
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
    @ActionEvent(eventType = EventTypes.EVENT_DNS_SERVER_DELETE, eventDescription = "Deleting DNS Server")
    public boolean deleteDnsServer(DeleteDnsServerCmd cmd) {
        Long dnsServerId = cmd.getId();
        DnsServerVO dnsServer = dnsServerDao.findById(dnsServerId);
        if (dnsServer == null) {
            throw new InvalidParameterValueException(String.format("DNS server with ID: %s not found.", dnsServerId));
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, dnsServer);
        return Transaction.execute((TransactionCallback<Boolean>) status -> {
            if (cmd.getCleanup()) {
                List<DnsZoneVO> dnsZones = dnsZoneDao.findDnsZonesByServerId(dnsServerId);
                for (DnsZoneVO dnsZone : dnsZones) {
                    try {
                        deleteDnsZone(dnsZone.getId());
                    } catch (Exception ex) {
                        logger.error("Error cleaning up DNS zone: {} during DNS server: {} deletion", dnsZone.getName(), dnsServer.getName());
                    }
                }
            }
            return dnsServerDao.remove(dnsServerId);
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DNS_ZONE_DELETE, eventDescription = "Deleting DNS Zone")
    public boolean deleteDnsZone(Long zoneId) {
        DnsZoneVO dnsZone = dnsZoneDao.findById(zoneId);
        if (dnsZone == null) {
            throw new InvalidParameterValueException("DNS zone not found for the given ID.");
        }
        String dnsZoneName = dnsZone.getName();
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, dnsZone);
        DnsServerVO server = dnsServerDao.findById(dnsZone.getDnsServerId());
        if (server == null) {
            throw new CloudRuntimeException(String.format("The DNS server not found for DNS zone: %s", dnsZoneName));
        }

        boolean dbResult = Transaction.execute((TransactionCallback<Boolean>) status -> {
            DnsZoneNetworkMapVO networkMapVO = dnsZoneNetworkMapDao.findByZoneId(zoneId);
            DnsProvider provider = getProviderByType(server.getProviderType());
            // Remove DNS records from nic_details if there are any
            if (networkMapVO != null) {
                try {
                    List<DnsRecord> records = provider.listRecords(server, dnsZone);
                    if (CollectionUtils.isNotEmpty(records)) {
                        List<String> dnsRecordNames = records.stream().map(DnsRecord::getName).filter(Objects::nonNull)
                                .map(name -> name.replaceAll("\\.+$", ""))
                                .collect(Collectors.toList());
                        nicDetailsDao.removeDetailsForValuesIn(ApiConstants.NIC_DNS_RECORD, dnsRecordNames);
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to fetch DNS records for dnsZone: {}, perform manual cleanup.", dnsZoneName, ex);
                }
                dnsZoneNetworkMapDao.removeNetworkMappingByZoneId(zoneId);
            }

            // Remove DNS zone from provider and cleanup DB
            try {
                provider.deleteZone(server, dnsZone);
                logger.debug("Deleted DNS zone: {} from provider", dnsZoneName);
            } catch (DnsNotFoundException ex) {
                logger.warn("DNS zone: {} is not present in the provider, proceeding with cleanup", dnsZoneName);
            } catch (Exception ex) {
                logger.error("Failed to delete DNS zone from provider", ex);
                throw new CloudRuntimeException(String.format("Failed to delete DNS zone: %s.", dnsZoneName));
            }
            return dnsZoneDao.remove(zoneId);
        });

        if (!dbResult) {
            logger.error("Failed to remove DNS zone {} from DB after provider deletion", dnsZoneName);
        }
        return dbResult;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DNS_ZONE_UPDATE, eventDescription = "Updating DNS Zone")
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
                DnsProvider provider = getProviderByType(server.getProviderType());
                provider.updateZone(server, dnsZone);
                dnsZoneDao.update(dnsZone.getId(), dnsZone);
            } catch (Exception ex) {
                logger.error("Failed to update DNS zone: {} on DNS server: {}", dnsZone.getName(), server.getName(), ex);
                throw new CloudRuntimeException("Failed to update DNS zone: " + dnsZone.getName());
            }
        }
        return dnsZone;
    }

    @Override
    public ListResponse<DnsZoneResponse> listDnsZones(ListDnsZonesCmd cmd) {
        Pair<List<DnsZoneVO>, Integer> result = searchForDnsZonesInternal(cmd);
        List<String> zoneIds = new ArrayList<>();
        for (DnsZoneVO zone : result.first()) {
            zoneIds.add(zone.getUuid());
        }
        List<DnsZoneJoinVO> zoneJoinVos = dnsZoneJoinDao.listByUuids(zoneIds);
        List<DnsZoneResponse> zoneResponses = new ArrayList<>();
        for (DnsZoneJoinVO zoneJoin: zoneJoinVos) {
            zoneResponses.add(createDnsZoneResponse(zoneJoin));
        }
        ListResponse<DnsZoneResponse> response = new ListResponse<>();
        response.setResponses(zoneResponses, result.second());
        return response;
    }

    private Pair<List<DnsZoneVO>, Integer> searchForDnsZonesInternal(ListDnsZonesCmd cmd) {
        if (cmd.getId() != null) {
            DnsZone dnsZone = dnsZoneDao.findById(cmd.getId());
            if (dnsZone == null) {
                throw new InvalidParameterValueException("DNS zone not found for the given ID");
            }
        }
        Account caller = CallContext.current().getCallingAccount();
        if (cmd.getDnsServerId() != null) {
            DnsServer dnsServer = dnsServerDao.findById(cmd.getDnsServerId());
            accountMgr.checkAccess(caller, null, true, dnsServer);
        }
        List<Long> ownDnsServerIds = dnsServerDao.listDnsServerIdsByAccountId(caller.getAccountId());
        String keyword = cmd.getKeyword();
        if (cmd.getName() != null) {
            keyword = cmd.getName();
        }
        Filter searchFilter = new Filter(DnsZoneVO.class, ApiConstants.ID, true, cmd.getStartIndex(), cmd.getPageSizeVal());
        return dnsZoneDao.searchZones(cmd.getId(), caller.getAccountId(), ownDnsServerIds, cmd.getDnsServerId(), keyword, searchFilter);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DNS_RECORD_CREATE, eventDescription = "Creating DNS Record")
    public DnsRecordResponse createDnsRecord(CreateDnsRecordCmd cmd) {
        String recordName = StringUtils.trimToEmpty(cmd.getName()).toLowerCase();
        if (StringUtils.isBlank(recordName)) {
            throw new InvalidParameterValueException("Empty DNS record name is not allowed");
        }
        DnsZoneVO dnsZone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (dnsZone == null) {
            throw new InvalidParameterValueException("DNS zone not found.");
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, dnsZone);
        DnsServerVO server = dnsServerDao.findById(dnsZone.getDnsServerId());
        if (server == null) {
            throw new CloudRuntimeException("The underlying DNS server for this DNS zone is missing.");
        }
        try {
            DnsRecord.RecordType type = cmd.getType();
            List<String> normalizedContents = cmd.getContents().stream()
                    .map(value -> DnsProviderUtil.normalizeDnsRecordValue(value, type)).collect(Collectors.toList());
            DnsRecord record = new DnsRecord(recordName, type, normalizedContents, cmd.getTtl());
            DnsProvider provider = getProviderByType(server.getProviderType());
            String normalizedRecordName = provider.addRecord(server, dnsZone, record);
            record.setName(normalizedRecordName);
            publishDnsRecordEventMessageBus(recordName, type, caller.getAccountId(), EVENT_DNS_RECORD_CREATE, normalizedContents);
            return createDnsRecordResponse(record);
        } catch (Exception ex) {
            logger.error("Failed to add DNS record via provider", ex);
            throw new CloudRuntimeException(String.format("Failed to add DNS record: %s", recordName));
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DNS_RECORD_DELETE, eventDescription = "Deleting DNS Record")
    public boolean deleteDnsRecord(DeleteDnsRecordCmd cmd) {
        DnsZoneVO zone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("DNS zone not found.");
        }
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, true, zone);
        DnsServerVO server = dnsServerDao.findById(zone.getDnsServerId());
        DnsRecord.RecordType recordType = cmd.getType();
        try {
            DnsRecord record = new DnsRecord();
            record.setName(cmd.getName());
            record.setType(recordType);
            DnsProvider provider = getProviderByType(server.getProviderType());
            String deletedDnsRecord = provider.deleteRecord(server, zone, record);
            publishDnsRecordEventMessageBus(deletedDnsRecord, recordType, caller.getAccountId(), EVENT_DNS_RECORD_DELETE, null);
            return deletedDnsRecord != null;
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
            DnsProvider provider = getProviderByType(server.getProviderType());
            List<DnsRecord> records = provider.listRecords(server, zone);
            List<DnsRecordResponse> responses = new ArrayList<>();
            for (DnsRecord record : records) {
                responses.add(createDnsRecordResponse(record));
            }

            ListResponse<DnsRecordResponse> listResponse = new ListResponse<>();
            listResponse.setResponses(responses, responses.size());
            return listResponse;
        } catch (DnsNotFoundException ex) {
            logger.error("DNS zone is not found", ex);
            throw new CloudRuntimeException("DNS zone is not found, please register it first");
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

        String dnsZoneName = DnsProviderUtil.normalizeDomain(cmd.getName());
        DnsServerVO server = dnsServerDao.findById(cmd.getDnsServerId());
        if (server == null) {
            throw new InvalidParameterValueException(String.format("DNS server not found for the given ID: %s", cmd.getDnsServerId()));
        }
        Account caller = CallContext.current().getCallingAccount();
        boolean isOwner = (server.getAccountId() == caller.getId());
        if (!isOwner) {
            if (!server.getPublicServer()) {
                throw new PermissionDeniedException("You do not have permission to use this DNS server.");
            }
            dnsZoneName = DnsProviderUtil.appendPublicSuffixToZone(dnsZoneName, server.getPublicDomainSuffix());
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
            DnsProvider provider = getProviderByType(server.getProviderType());
            String externalReferenceId = provider.provisionZone(server, dnsZone);
            dnsZone.setExternalReference(externalReferenceId);
            dnsZone.setState(DnsZone.State.Active);
            dnsZoneDao.update(dnsZone.getId(), dnsZone);
            logger.debug("DNS zone: {} created successfully on DNS server: {} with ID: {}", dnsZone.getName(), server.getName(), dnsZoneId);
        } catch (Exception ex) {
            dnsZoneDao.remove(dnsZoneId);
            logger.error("Failed to provision DNS zone: {} on DNS server: {}", dnsZone.getName(), server.getName(), ex);
            String errorMsg = "";
            if ( ex instanceof DnsConflictException) {
                errorMsg = String.format("DNS zone: %s already exists", dnsZone.getName());
            } else if (ex instanceof DnsTransportException){
                errorMsg = String.format("DNS server: %s not reachable", server.getName());
            }
            throw new CloudRuntimeException(errorMsg);
        }
        return dnsZone;
    }


    public DnsServerResponse createDnsServerResponse(DnsServer dnsServer) {
        DnsServerJoinVO serverJoin = dnsServerJoinDao.findById(dnsServer.getId());
        return createDnsServerResponse(serverJoin);
    }

    DnsServerResponse createDnsServerResponse(DnsServerJoinVO server) {
        DnsServerResponse response = new DnsServerResponse();
        response.setId(server.getUuid());
        response.setName(server.getName());
        response.setUrl(server.getUrl());
        response.setPort(server.getPort());
        response.setProvider(server.getProviderType());
        response.setPublic(server.isPublicServer());
        response.setNameServers(server.getNameServers());
        response.setPublicDomainSuffix(server.getPublicDomainSuffix());
        response.setAccountName(server.getAccountName());
        response.setDomainId(server.getDomainUuid()); // Note: APIs always return UUIDs, not internal DB IDs!
        response.setDomainName(server.getDomainName());
        response.setState(server.getState().name());
        response.setObjectName("dnsserver");
        return response;
    }

    @Override
    public DnsZoneResponse createDnsZoneResponse(DnsZone dnsZone) {
        DnsZoneJoinVO zoneJoinVO = dnsZoneJoinDao.findById(dnsZone.getId());
        return createDnsZoneResponse(zoneJoinVO);
    }

    DnsZoneResponse createDnsZoneResponse(DnsZoneJoinVO zone) {
        DnsZoneResponse response = new DnsZoneResponse();
        response.setId(zone.getUuid());
        response.setName(zone.getName());
        response.setDnsServerId(zone.getDnsServerUuid());
        response.setAccountName(zone.getAccountName());
        response.setDomainId(zone.getDomainUuid());
        response.setDomainName(zone.getDomainName());
        response.setDnsServerName(zone.getDnsServerName());
        response.setDnsServerAccountName(zone.getDnsServerAccountName());
        response.setState(zone.getState());
        response.setDescription(zone.getDescription());
        return response;
    }

    @Override
    public DnsRecordResponse createDnsRecordResponse(DnsRecord record) {
        DnsRecordResponse res = new DnsRecordResponse();
        res.setName(record.getName());
        res.setType(record.getType());
        res.setContent(record.getContents());
        res.setTtl(record.getTtl());
        return res;
    }

    @Override
    public DnsZoneNetworkMapResponse associateZoneToNetwork(AssociateDnsZoneToNetworkCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        DnsZoneVO dnsZone = dnsZoneDao.findById(cmd.getDnsZoneId());
        if (dnsZone == null) {
            throw new InvalidParameterValueException("DNS zone not found.");
        }
        accountMgr.checkAccess(caller, null, true, dnsZone);
        DnsServerVO dnsServer = dnsServerDao.findById(dnsZone.getDnsServerId());
        if (dnsServer == null) {
            throw new CloudRuntimeException("The underlying DNS server for this DNS zone is missing.");
        }
        NetworkVO network = networkDao.findById(cmd.getNetworkId());
        if (network == null) {
            throw new InvalidParameterValueException("Network not found.");
        }
        if (!NetworkVO.GuestType.Shared.equals(network.getGuestType())) {
            throw new CloudRuntimeException(String.format("Operation is not permitted for network type: %s", network.getGuestType()));
        }
        accountMgr.checkAccess(caller, null, true, network);
        DnsZoneNetworkMapVO existing = dnsZoneNetworkMapDao.findByNetworkId(network.getId());
        if (existing != null) {
            throw new InvalidParameterValueException("Network has existing DNS zone associated to it.");
        }

        DnsZoneNetworkMapVO mapping = new DnsZoneNetworkMapVO(dnsZone.getId(), network.getId(), cmd.getSubDomain());
        dnsZoneNetworkMapDao.persist(mapping);
        DnsZoneNetworkMapResponse response = new DnsZoneNetworkMapResponse();
        response.setId(mapping.getUuid());
        response.setDnsZoneId(dnsZone.getUuid());
        response.setNetworkId(network.getUuid());
        response.setSubDomain(mapping.getSubDomain());
        return response;
    }

    @Override
    public boolean disassociateZoneFromNetwork(DisassociateDnsZoneFromNetworkCmd cmd) {
        DnsZoneNetworkMapVO mapping = dnsZoneNetworkMapDao.findByNetworkId(cmd.getNetworkId());
        if (mapping == null) {
            throw new InvalidParameterValueException("No DNS zone is associated to specified network.");
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
    public void deleteDnsRecordForVM(VirtualMachine instance, Network network, Nic nic) {
        String instanceName = instance.getInstanceName();
        NicDetailVO nicDetailVO = nicDetailsDao.findDetail(nic.getId(), ApiConstants.NIC_DNS_RECORD);
        if (nicDetailVO == null || Strings.isBlank(nicDetailVO.getValue())) {
            logger.debug("No DNS record found for Instance: {}", instance.getInstanceName());
            return;
        }
        String dnsRecord = nicDetailVO.getValue();
        try {
            DnsZoneNetworkMapVO dnsZoneNetworkMap = dnsZoneNetworkMapDao.findByNetworkId(network.getId());
            DnsZoneVO dnsZone = null;
            DnsServerVO dnsServer = null;
            if (dnsZoneNetworkMap != null) {
                dnsZone = dnsZoneDao.findById(dnsZoneNetworkMap.getDnsZoneId());
            }
            if (dnsZone != null) {
                dnsServer = dnsServerDao.findById(dnsZone.getDnsServerId());
            }
            if (dnsServer != null) {
                processDnsRecordInProvider(dnsRecord, instance, dnsServer, dnsZone, nic, false);
            } else {
                logger.warn("Skipping deletion of DNS record: {} from provider for Instance: {}.", dnsRecord, instanceName);
            }
        } catch (Exception ex) {
            logger.error("Failed deleting DNS record: {} for Instance: {}, proceeding with DB cleanup.", dnsRecord, instanceName);
        } finally {
            nicDetailsDao.removeDetail(nic.getId(), ApiConstants.NIC_DNS_RECORD);
            logger.debug("Removed DNS record from DB for Instance: {}, NIC ID: {}", instanceName, nic.getUuid());
        }
    }

    private String processDnsRecordInProvider(String recordName, VirtualMachine instance, DnsServer server, DnsZone dnsZone,
                                              Nic nic, boolean isAdd) {

        try {
            DnsProvider provider = getProviderByType(server.getProviderType());
            // Handle IPv4 (A Record)
            String ipv4DnsRecord = null;
            if (nic.getIPv4Address() != null) {
                DnsRecord recordA = new DnsRecord(recordName, DnsRecord.RecordType.A, Collections.singletonList(nic.getIPv4Address()), 3600);
                if (isAdd) {
                    ipv4DnsRecord = provider.addRecord(server, dnsZone, recordA);
                } else {
                    ipv4DnsRecord = provider.deleteRecord(server, dnsZone, recordA);
                }
            }

            // Handle IPv6 (AAAA Record) if it exists
            String ipv6DnsRecord = null;
            if (nic.getIPv6Address() != null) {
                DnsRecord recordAAAA = new DnsRecord(recordName, DnsRecord.RecordType.AAAA, Collections.singletonList(nic.getIPv6Address()), 3600);
                if (isAdd) {
                    ipv6DnsRecord = provider.addRecord(server, dnsZone, recordAAAA);
                } else {
                    ipv6DnsRecord = provider.deleteRecord(server, dnsZone, recordAAAA);
                }
            }
            return ipv4DnsRecord != null ? ipv4DnsRecord : ipv6DnsRecord;
        } catch (Exception ex) {
            logger.error(
                    "Failed to {} DNS record for Instance {} in zone {}",
                    isAdd ? "register" : "remove",
                    instance.getInstanceName(),
                    dnsZone.getName(),
                    ex
            );
        }
        return null;
    }

    @Override
    public void checkDnsServerPermission(Account caller, DnsServer dnsServer) throws PermissionDeniedException {
        if (caller.getId() == dnsServer.getAccountId()) {
            return;
        }
        if (!dnsServer.getPublicServer()) {
            throw new PermissionDeniedException(caller + "is not allowed to access the DNS server " + dnsServer.getName());
        }
        Account owner = getAccount(dnsServer.getAccountId());
        if (!domainDao.isChildDomain(owner.getDomainId(), caller.getDomainId())) {
            throw new PermissionDeniedException(caller + "is not allowed to access the DNS server " + dnsServer.getName());
        }
    }

    @Override
    public void checkDnsZonePermission(Account caller, DnsZone zone) {
        if (caller.getId() != zone.getAccountId()) {
            throw new PermissionDeniedException(caller + "is not allowed to access the DNS Zone " + zone.getName());
        }
    }

    public Account getAccount(long accountId) {
        return accountDao.findByIdIncludingRemoved(accountId);
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
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        messageBus.subscribe(VirtualMachineManager.Topics.VM_LIFECYCLE, new VmLifecycleSubscriber());
        messageBus.subscribe(Nic.Topics.NIC_LIFECYCLE, new NicLifecycleSubscriber());
        messageBus.subscribe(DnsProvider.Topics.DNS_RECORD_LIFECYCLE, new DnsRecordLifecycleSubscriber());
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
        return cmdList;
    }

    public List<DnsProvider> getDnsProviders() {
        return dnsProviders;
    }

    public void setDnsProviders(List<DnsProvider> dnsProviders) {
        this.dnsProviders = dnsProviders;
    }

    class VmLifecycleSubscriber implements MessageSubscriber {

        @Override
        public void onPublishMessage(String senderAddress, String subject, Object args) {
            try {
                logger.trace("VM lifecycle event: {},  {}, {}", senderAddress, subject, args);

                @SuppressWarnings("unchecked")
                Map<String, Object> event = (Map<String, Object>) args;
                VirtualMachine.State oldState = (VirtualMachine.State) event.get(ApiConstants.OLD_STATE);
                VirtualMachine.State newState = (VirtualMachine.State) event.get(ApiConstants.NEW_STATE);
                if (oldState == newState) {
                    logger.debug("Instance state is unchanged, skip event processing");
                    return;
                }
                long instanceId = (long) event.get(ApiConstants.INSTANCE_ID);
                switch (newState) {
                    case Running:
                        handleVmRunningState(instanceId);
                        break;
                    case Stopped:
                    case Destroyed:
                        handleVmStopAndDestroy(instanceId);
                        break;
                    default:
                        logger.warn("Ignoring lifecycle event for Instance ID: {}, unsupported state={}", instanceId, newState);
                        break;
                }
            } catch (Exception ex) {
                logger.error("Failed to process Instance lifecycle event", ex);
            }
        }
    }

    class NicLifecycleSubscriber implements MessageSubscriber {
        @Override
        public void onPublishMessage(String senderAddress, String subject, Object args) {
            try {
                logger.trace("NIC lifecycle event: {},  {}, {}", senderAddress, subject, args);

                @SuppressWarnings("unchecked")
                Map<String, Object> event = (Map<String, Object>) args;
                String eventType = (String) event.get(ApiConstants.EVENT_TYPE);
                Long nicId = (Long) event.get(ApiConstants.NIC_ID);
                Long instanceId = (Long) event.get(ApiConstants.INSTANCE_ID);
                if (nicId == null || instanceId == null) {
                    logger.warn("Insufficient data to process NIC lifecycle event: {}", args);
                    return;
                }
                if (EVENT_NIC_CREATE.equals(eventType)) {
                    handleNicPlug(instanceId, nicId);
                } else if (EVENT_NIC_DELETE.equals(eventType)) {
                    handleNicUnplug(instanceId, nicId);
                } else {
                    logger.warn("Ignoring lifecycle event for NIC with ID: {}, unsupported eventType={}", nicId, eventType);
                }
            } catch (Exception ex) {
                logger.error("Failed to process NIC lifecycle event", ex);
            }

        }
    }

    class DnsRecordLifecycleSubscriber implements MessageSubscriber {
        @Override
        public void onPublishMessage(String senderAddress, String subject, Object args) {
            try {
                logger.trace("DNS record lifecycle event: {},  {}, {}", senderAddress, subject, args);

                @SuppressWarnings("unchecked")
                Map<String, Object> event = (Map<String, Object>) args;
                String eventType = (String) event.get(ApiConstants.EVENT_TYPE);
                String dnsRecord = (String) event.get(ApiConstants.DNS_RECORD);
                if (EVENT_DNS_RECORD_CREATE.equalsIgnoreCase(eventType)) {
                    @SuppressWarnings("unchecked")
                    List<String> contents = (List<String>) event.get(ApiConstants.CONTENTS);
                    if (CollectionUtils.isNotEmpty(contents)) {
                        for (String ipAddress : contents) {
                            Nic nic = nicDao.findByIpAddressAndVmType(ipAddress, VirtualMachine.Type.User);
                            if (nic != null) {
                                nicDetailsDao.addDetail(nic.getId(), ApiConstants.NIC_DNS_RECORD, dnsRecord, true);
                            }
                        }
                    }
                } else if (EVENT_DNS_RECORD_DELETE.equalsIgnoreCase(eventType)) {
                    nicDetailsDao.removeDetailsForValuesIn(ApiConstants.NIC_DNS_RECORD, Collections.singletonList(dnsRecord));
                }
            } catch (Exception ex) {
                logger.error("Failed to process DNS record lifecycle event", ex);
            }
        }
    }

    void publishDnsRecordEventMessageBus(String dnsRecord, DnsRecord.RecordType recordType,  Long accountId,
                                         String eventType, List<String> contents) {

        // Only publish for A or AAAA records and non-null record name
        if ((recordType != DnsRecord.RecordType.A && recordType != DnsRecord.RecordType.AAAA) || dnsRecord == null) {
            return;
        }
        try {
            Map<String, Object> event = new HashMap<>();
            event.put(ApiConstants.EVENT_ID, UUID.randomUUID().toString());
            event.put(ApiConstants.DNS_RECORD, dnsRecord);
            event.put(ApiConstants.ACCOUNT_ID, accountId);
            event.put(ApiConstants.EVENT_TYPE, eventType);
            event.put(ApiConstants.CONTENTS, contents != null ? contents : Collections.emptyList());
            event.put(ApiConstants.TIME_STAMP, System.currentTimeMillis());
            messageBus.publish(_name, DnsProvider.Topics.DNS_RECORD_LIFECYCLE, PublishScope.GLOBAL, event);
        } catch (Exception ex) {
            logger.error("Failed to publish {} event for DNS record: {}", eventType, dnsRecord, ex);
        }
    }

    void handleVmRunningState(long instanceId) {
        VirtualMachine instance = vmInstanceDao.findById(instanceId);
        if (instance == null) {
            logger.debug("Instance is not found for the given ID: {}", instanceId);
            return;
        }
        List<DnsNicJoinVO> mappedNics = dnsNicJoinDao.listActiveByVmId(instanceId);
        if (CollectionUtils.isEmpty(mappedNics)) {
            logger.debug("No active DNS zone associated to NICs");
            return;
        }
        Map<Long, Map<String, List<DnsNicJoinVO>>> dnsZoneRecordNicMap = new HashMap<>();
        for (DnsNicJoinVO nic : mappedNics) {
            DnsZoneVO targetZone = dnsZoneDao.findById(nic.getDnsZoneId());
            if (targetZone == null) {
                continue;
            }
            String targetDnsRecordUrl = prepareDnsRecordUrl(instance.getHostName(), nic.getSubDomain(), targetZone.getName());
            dnsZoneRecordNicMap.computeIfAbsent(targetZone.getId(), k -> new HashMap<>())
                    .computeIfAbsent(targetDnsRecordUrl, k -> new ArrayList<>())
                    .add(nic);
        }

        for (Map.Entry<Long, Map<String, List<DnsNicJoinVO>>> zoneEntry : dnsZoneRecordNicMap.entrySet()) {
            long targetZoneId = zoneEntry.getKey();
            for (Map.Entry<String, List<DnsNicJoinVO>> dnsUrlEntry : zoneEntry.getValue().entrySet()) {
                String dnsRecordUrl = dnsUrlEntry.getKey();
                List<DnsNicJoinVO> nicsForThisFqdn = dnsUrlEntry.getValue();
                try {
                    Transaction.execute(new TransactionCallbackWithExceptionNoReturn<DnsProviderException>() {
                        @Override
                        public void doInTransactionWithoutResult(TransactionStatus status) throws DnsProviderException{
                            DnsNicJoinVO existing = dnsNicJoinDao.findActiveByDnsRecordAndZone(dnsRecordUrl, targetZoneId);
                            if (existing != null && existing.getInstanceId() != instanceId) {
                                logger.error("DNS Collision: Cannot register DNS record: {}. Already owned by Instance: {}.",
                                        dnsRecordUrl, existing.getInstanceId());
                                return;
                            }
                            for (DnsNicJoinVO nic : nicsForThisFqdn) {
                                nicDetailsDao.addDetail(nic.getId(), ApiConstants.NIC_DNS_RECORD, dnsRecordUrl, true);
                            }
                            syncDnsState(instanceId, dnsRecordUrl, targetZoneId);
                        }
                    });
                } catch (DnsProviderException ex) {
                    logger.error("Failed to register DNS record: {} in provider for Instance: {}", dnsRecordUrl, instance.getUuid(), ex);
                } catch (Exception ex) {
                    logger.warn("Failed to register DNS record: {} for Instance: {}", dnsRecordUrl, instance.getUuid(), ex);
                }
            }
        }
    }

    void handleVmStopAndDestroy(long instanceId) {
        List<DnsNicJoinVO> historicalNics = dnsNicJoinDao.listIncludingRemovedByVmId(instanceId);
        if (CollectionUtils.isEmpty(historicalNics)) {
            return;
        }
        Map<Long, Map<String, List<DnsNicJoinVO>>> groupByDnsZone = new HashMap<>();
        for (DnsNicJoinVO nic : historicalNics) {
            // If the DNS record url is null, it means this NIC was never registered in nic_details
            if (nic.getNicDnsUrl() == null) {
                continue;
            }
            groupByDnsZone
                    .computeIfAbsent(nic.getDnsZoneId(), k -> new HashMap<>())
                    .computeIfAbsent(nic.getNicDnsUrl(), k -> new ArrayList<>())
                    .add(nic);
        }

        for (Map.Entry<Long, Map<String, List<DnsNicJoinVO>>> zoneEntry : groupByDnsZone.entrySet()) {
            long targetZoneId = zoneEntry.getKey();
            for (Map.Entry<String, List<DnsNicJoinVO>> dnsRecordEntry : zoneEntry.getValue().entrySet()) {
                String dnsRecordUrl = dnsRecordEntry.getKey();
                List<DnsNicJoinVO> nicsForDnsUrl = dnsRecordEntry.getValue();

                try {
                    Transaction.execute(new TransactionCallbackWithExceptionNoReturn<DnsProviderException>() {
                        @Override
                        public void doInTransactionWithoutResult(TransactionStatus status) throws DnsProviderException {
                            for (DnsNicJoinVO nic : nicsForDnsUrl) {
                                nicDetailsDao.removeDetail(nic.getId(), ApiConstants.NIC_DNS_RECORD);
                            }
                            // Because we just deleted the nic_details, the sync method will naturally
                            // find 0 active IPs for this VM/FQDN combo and issue a clean DELETE to PowerDNS.
                            syncDnsState(instanceId, dnsRecordUrl, targetZoneId);
                        }
                    });
                    logger.debug("Successfully cleaned up DNS record: {} for Instance with ID: {}", dnsRecordUrl, instanceId);
                } catch (DnsProviderException ex) {
                    logger.error("Failed to cleanup DNS record: {} in provider", dnsRecordUrl, ex);
                } catch (Exception ex) {
                    logger.error("Failed DNS record: {} cleanup for Instance", dnsRecordUrl, ex);
                }
            }
        }
    }

    void handleNicPlug(long instanceId, long nicId) {
        VirtualMachine instance = vmInstanceDao.findById(instanceId);
        if (instance == null || instance.getState() != VirtualMachine.State.Running) {
            return;
        }
        DnsNicJoinVO nic = dnsNicJoinDao.findById(nicId);
        if (nic == null) {
            logger.debug("NIC with ID: {} doesn't have DNS zone associated", nicId);
            return;
        }

        DnsZoneVO targetZone = dnsZoneDao.findById(nic.getDnsZoneId());
        if (targetZone == null) {
            return;
        }

        String dnsRecordUrl = prepareDnsRecordUrl(instance.getHostName(), nic.getSubDomain(), targetZone.getName());
        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<DnsProviderException>() {

                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws DnsProviderException {
                    DnsNicJoinVO existing = dnsNicJoinDao.findActiveByDnsRecordAndZone(dnsRecordUrl, targetZone.getId());
                    if (existing != null && existing.getInstanceId() != instanceId) {
                        logger.error("DNS Collision: Cannot register DNS record: {}. Already owned by Instance: {}.",
                                dnsRecordUrl, existing.getInstanceId());
                        return;
                    }
                    nicDetailsDao.addDetail(nicId, ApiConstants.NIC_DNS_RECORD, dnsRecordUrl, true);
                    syncDnsState(instanceId, dnsRecordUrl, targetZone.getId());
                    logger.debug("Successfully synced DNS on NIC Plug for: {}", dnsRecordUrl);
                }
            });
        } catch (DnsProviderException ex) {
            logger.error("Failed to register DNS record: {} in provider for Instance: {}", dnsRecordUrl, instance.getUuid(), ex);
        } catch (Exception ex) {
            logger.warn("Failed to register DNS record: {} for Instance: {}", dnsRecordUrl, instance.getUuid(), ex);
        }
    }

    void handleNicUnplug(long instanceId, long nicId) {
        DnsNicJoinVO nic = dnsNicJoinDao.findByIdIncludingRemoved(nicId);
        if (nic == null || nic.getNicDnsUrl() == null) {
            return;
        }
        String dnsRecordUrl = nic.getNicDnsUrl();
        long dnsZoneId = nic.getDnsZoneId();

        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<DnsProviderException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws DnsProviderException {
                    nicDetailsDao.removeDetail(nicId, ApiConstants.NIC_DNS_RECORD);
                    syncDnsState(instanceId, dnsRecordUrl, dnsZoneId);
                    logger.debug("Successfully synced DNS record: {} on NIC unplug", dnsRecordUrl);
                }
            });
        } catch (DnsProviderException ex) {
            logger.error("Failed to sync DNS record: {} on NIC unplug in provider", dnsRecordUrl);
        } catch (Exception ex) {
            logger.error("Failed to sync DNS record: {} on NIC unplug ", dnsRecordUrl);
        }
    }

    String prepareDnsRecordUrl(String hostName, String subDomain, String dnsZoneName) {
        List<String> parts = new ArrayList<>();
        parts.add(hostName);
        if (StringUtils.isNotBlank(subDomain)) {
            parts.add(subDomain.trim());
        }
        parts.add(dnsZoneName);
        return String.join(".", parts);
    }

    public void syncDnsState(Long instanceId, String dnsRecordUrl, long dnsZoneId) throws DnsProviderException {
        DnsZone dnsZone = dnsZoneDao.findById(dnsZoneId);
        if (dnsZone == null) {
            logger.error("DNS zone not found for the provided ID: {}", dnsZoneId);
            return;
        }
        DnsServerVO dnsServer = dnsServerDao.findById(dnsZone.getDnsServerId());
        List<DnsNicJoinVO> activeNics = dnsNicJoinDao.listActiveByVmIdZoneAndDnsRecord(instanceId, dnsZoneId, dnsRecordUrl);

        List<String> ipv4s = new ArrayList<>();
        List<String> ipv6s = new ArrayList<>();
        for (DnsNicJoinVO nic : activeNics) {
            if (nic.getIp4Address() != null && !nic.getIp4Address().isEmpty()) {
                ipv4s.add(nic.getIp4Address());
            }
            if (nic.getIp6Address() != null && !nic.getIp6Address().isEmpty()) {
                ipv6s.add(nic.getIp6Address());
            }
        }
        logger.debug("Syncing DNS for: {}. Found: {} IPv4s and {} IPv6s.", dnsRecordUrl, ipv4s.size(), ipv6s.size());
        DnsProvider provider = getProviderByType(dnsServer.getProviderType());
        DnsRecord recordIpv4 = new DnsRecord(dnsRecordUrl, DnsRecord.RecordType.A, null, 3600);
        // push A records into Provider
        if (ipv4s.isEmpty()) {
            provider.deleteRecord(dnsServer, dnsZone, recordIpv4);
        } else {
            recordIpv4.setContents(ipv4s);
            provider.addRecord(dnsServer, dnsZone, recordIpv4);
        }
        // push AAAA records into Provider
        DnsRecord recordIpv6 = new DnsRecord(dnsRecordUrl, DnsRecord.RecordType.AAAA, null, 3600);
        if (ipv6s.isEmpty()) {
            provider.deleteRecord(dnsServer, dnsZone, recordIpv6);
        } else {
            recordIpv6.setContents(ipv6s);
            provider.addRecord(dnsServer, dnsZone, recordIpv6);
        }
    }
}
