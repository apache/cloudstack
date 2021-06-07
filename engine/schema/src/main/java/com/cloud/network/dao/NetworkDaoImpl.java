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
package com.cloud.network.dao;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.TableGenerator;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.api.ApiConstants;
import org.springframework.stereotype.Component;

import com.cloud.network.Network;
import com.cloud.network.Network.Event;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.vpc.VpcGatewayVO;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SequenceFetcher;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.net.NetUtils;

@Component
@DB()
public class NetworkDaoImpl extends GenericDaoBase<NetworkVO, Long>implements NetworkDao {
    SearchBuilder<NetworkVO> AllFieldsSearch;
    SearchBuilder<NetworkVO> AccountSearch;
    SearchBuilder<NetworkVO> RelatedConfigSearch;
    SearchBuilder<NetworkVO> AccountNetworkSearch;
    SearchBuilder<NetworkVO> ZoneBroadcastUriSearch;
    SearchBuilder<NetworkVO> ZoneSecurityGroupSearch;
    GenericSearchBuilder<NetworkVO, Integer> CountBy;
    SearchBuilder<NetworkVO> PhysicalNetworkSearch;
    SearchBuilder<NetworkVO> SecurityGroupSearch;
    GenericSearchBuilder<NetworkVO, Long> NetworksRegularUserCanCreateSearch;
    GenericSearchBuilder<NetworkVO, Integer> NetworksCount;
    SearchBuilder<NetworkVO> SourceNATSearch;
    GenericSearchBuilder<NetworkVO, Long> VpcNetworksCount;
    SearchBuilder<NetworkVO> OfferingAccountNetworkSearch;
    SearchBuilder<NetworkVO> PersistentNetworkSearch;

    GenericSearchBuilder<NetworkVO, Long> GarbageCollectedSearch;
    SearchBuilder<NetworkVO> PrivateNetworkSearch;

    @Inject
    ResourceTagDao _tagsDao;
    @Inject
    NetworkAccountDao _accountsDao;
    @Inject
    NetworkDomainDao _domainsDao;
    @Inject
    NetworkOpDao _opDao;
    @Inject
    NetworkServiceMapDao _ntwkSvcMap;
    @Inject
    NetworkOfferingDao _ntwkOffDao;
    @Inject
    NetworkOpDao _ntwkOpDao;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    VpcGatewayDao _vpcGatewayDao;

    TableGenerator _tgMacAddress;

    Random _rand = new Random(System.currentTimeMillis());
    long _prefix = 0x2;

    private static final Set<String> VLAN_OR_VXLAN = new HashSet<>(Arrays.asList(BroadcastDomainType.Vlan.toString().toLowerCase(),
                                                                BroadcastDomainType.Vxlan.toString().toLowerCase()));

    public NetworkDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), Op.EQ);
        AllFieldsSearch.and("trafficType", AllFieldsSearch.entity().getTrafficType(), Op.EQ);
        AllFieldsSearch.and("cidr", AllFieldsSearch.entity().getCidr(), Op.EQ);
        AllFieldsSearch.and("broadcastType", AllFieldsSearch.entity().getBroadcastDomainType(), Op.EQ);
        AllFieldsSearch.and("offering", AllFieldsSearch.entity().getNetworkOfferingId(), Op.EQ);
        AllFieldsSearch.and("datacenter", AllFieldsSearch.entity().getDataCenterId(), Op.EQ);
        AllFieldsSearch.and("account", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.and("related", AllFieldsSearch.entity().getRelated(), Op.EQ);
        AllFieldsSearch.and("guestType", AllFieldsSearch.entity().getGuestType(), Op.EQ);
        AllFieldsSearch.and("physicalNetwork", AllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        AllFieldsSearch.and("broadcastUri", AllFieldsSearch.entity().getBroadcastUri(), Op.EQ);
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), Op.EQ);
        AllFieldsSearch.and("aclId", AllFieldsSearch.entity().getNetworkACLId(), Op.EQ);
        AllFieldsSearch.and("redundant", AllFieldsSearch.entity().isRedundant(), Op.EQ);
        final SearchBuilder<NetworkOfferingVO> join1 = _ntwkOffDao.createSearchBuilder();
        join1.and("isSystem", join1.entity().isSystemOnly(), Op.EQ);
        join1.and("isRedundant", join1.entity().isRedundantRouter(), Op.EQ);
        AllFieldsSearch.join("offerings", join1, AllFieldsSearch.entity().getNetworkOfferingId(), join1.entity().getId(), JoinBuilder.JoinType.INNER);
        AllFieldsSearch.done();

        AccountSearch = createSearchBuilder();
        AccountSearch.and("offering", AccountSearch.entity().getNetworkOfferingId(), Op.EQ);
        final SearchBuilder<NetworkAccountVO> join = _accountsDao.createSearchBuilder();
        join.and("account", join.entity().getAccountId(), Op.EQ);
        AccountSearch.join("accounts", join, AccountSearch.entity().getId(), join.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        AccountSearch.and("datacenter", AccountSearch.entity().getDataCenterId(), Op.EQ);
        AccountSearch.and("cidr", AccountSearch.entity().getCidr(), Op.EQ);
        AccountSearch.and("vpcId", AccountSearch.entity().getVpcId(), Op.EQ);
        AccountSearch.done();

        RelatedConfigSearch = createSearchBuilder();
        RelatedConfigSearch.and("offering", RelatedConfigSearch.entity().getNetworkOfferingId(), Op.EQ);
        RelatedConfigSearch.and("datacenter", RelatedConfigSearch.entity().getDataCenterId(), Op.EQ);
        final SearchBuilder<NetworkAccountVO> join2 = _accountsDao.createSearchBuilder();
        join2.and("account", join2.entity().getAccountId(), Op.EQ);
        RelatedConfigSearch.join("account", join2, join2.entity().getNetworkId(), RelatedConfigSearch.entity().getId(), JoinType.INNER);
        RelatedConfigSearch.done();

        AccountNetworkSearch = createSearchBuilder();
        AccountNetworkSearch.and("networkId", AccountNetworkSearch.entity().getId(), Op.EQ);
        final SearchBuilder<NetworkAccountVO> mapJoin = _accountsDao.createSearchBuilder();
        mapJoin.and("accountId", mapJoin.entity().getAccountId(), Op.EQ);
        AccountNetworkSearch.join("networkSearch", mapJoin, AccountNetworkSearch.entity().getId(), mapJoin.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        AccountNetworkSearch.done();

        ZoneBroadcastUriSearch = createSearchBuilder();
        ZoneBroadcastUriSearch.and("dataCenterId", ZoneBroadcastUriSearch.entity().getDataCenterId(), Op.EQ);
        ZoneBroadcastUriSearch.and("broadcastUri", ZoneBroadcastUriSearch.entity().getBroadcastUri(), Op.EQ);
        ZoneBroadcastUriSearch.and("guestType", ZoneBroadcastUriSearch.entity().getGuestType(), Op.EQ);
        ZoneBroadcastUriSearch.done();

        ZoneSecurityGroupSearch = createSearchBuilder();
        ZoneSecurityGroupSearch.and("dataCenterId", ZoneSecurityGroupSearch.entity().getDataCenterId(), Op.EQ);
        final SearchBuilder<NetworkServiceMapVO> offJoin = _ntwkSvcMap.createSearchBuilder();
        offJoin.and("service", offJoin.entity().getService(), Op.EQ);
        ZoneSecurityGroupSearch.join("services", offJoin, ZoneSecurityGroupSearch.entity().getId(), offJoin.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        ZoneSecurityGroupSearch.done();

        CountBy = createSearchBuilder(Integer.class);
        CountBy.select(null, Func.COUNT, CountBy.entity().getId());
        CountBy.and("offeringId", CountBy.entity().getNetworkOfferingId(), Op.EQ);
        CountBy.and("vpcId", CountBy.entity().getVpcId(), Op.EQ);
        CountBy.and("removed", CountBy.entity().getRemoved(), Op.NULL);
        final SearchBuilder<NetworkOfferingVO> ntwkOffJoin = _ntwkOffDao.createSearchBuilder();
        ntwkOffJoin.and("isSystem", ntwkOffJoin.entity().isSystemOnly(), Op.EQ);
        CountBy.join("offerings", ntwkOffJoin, CountBy.entity().getNetworkOfferingId(), ntwkOffJoin.entity().getId(), JoinBuilder.JoinType.INNER);
        CountBy.done();

        PersistentNetworkSearch = createSearchBuilder();
        PersistentNetworkSearch.and("id", PersistentNetworkSearch.entity().getId(), Op.NEQ);
        PersistentNetworkSearch.and("guestType", PersistentNetworkSearch.entity().getGuestType(), Op.IN);
        PersistentNetworkSearch.and("broadcastUri", PersistentNetworkSearch.entity().getBroadcastUri(), Op.EQ);
        PersistentNetworkSearch.and("removed", PersistentNetworkSearch.entity().getRemoved(), Op.NULL);
        final SearchBuilder<NetworkOfferingVO> persistentNtwkOffJoin = _ntwkOffDao.createSearchBuilder();
        persistentNtwkOffJoin.and("persistent", persistentNtwkOffJoin.entity().isPersistent(), Op.EQ);
        PersistentNetworkSearch.join("persistent", persistentNtwkOffJoin, PersistentNetworkSearch.entity().getNetworkOfferingId(), persistentNtwkOffJoin.entity().getId(), JoinType.INNER);
        PersistentNetworkSearch.done();

        PhysicalNetworkSearch = createSearchBuilder();
        PhysicalNetworkSearch.and("physicalNetworkId", PhysicalNetworkSearch.entity().getPhysicalNetworkId(), Op.EQ);
        PhysicalNetworkSearch.done();

        SecurityGroupSearch = createSearchBuilder();
        final SearchBuilder<NetworkServiceMapVO> join3 = _ntwkSvcMap.createSearchBuilder();
        join3.and("service", join3.entity().getService(), Op.EQ);
        SecurityGroupSearch.join("services", join3, SecurityGroupSearch.entity().getId(), join3.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        SecurityGroupSearch.done();

        NetworksCount = createSearchBuilder(Integer.class);
        NetworksCount.select(null, Func.COUNT, NetworksCount.entity().getId());
        NetworksCount.and("networkOfferingId", NetworksCount.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        NetworksCount.done();

        NetworksRegularUserCanCreateSearch = createSearchBuilder(Long.class);
        NetworksRegularUserCanCreateSearch.and("aclType", NetworksRegularUserCanCreateSearch.entity().getAclType(), Op.EQ);
        NetworksRegularUserCanCreateSearch.and("displayNetwork", NetworksRegularUserCanCreateSearch.entity().getDisplayNetwork(), Op.EQ);
        NetworksRegularUserCanCreateSearch.select(null, Func.COUNT, NetworksRegularUserCanCreateSearch.entity().getId());
        final SearchBuilder<NetworkAccountVO> join4 = _accountsDao.createSearchBuilder();
        join4.and("account", join4.entity().getAccountId(), Op.EQ);
        join4.and("isOwner", join4.entity().isOwner(), Op.EQ);
        NetworksRegularUserCanCreateSearch.join("accounts", join4, NetworksRegularUserCanCreateSearch.entity().getId(), join4.entity().getNetworkId(),
                JoinBuilder.JoinType.INNER);
        final SearchBuilder<NetworkOfferingVO> join5 = _ntwkOffDao.createSearchBuilder();
        join5.and("specifyVlan", join5.entity().isSpecifyVlan(), Op.EQ);
        NetworksRegularUserCanCreateSearch.join("ntwkOff", join5, NetworksRegularUserCanCreateSearch.entity().getNetworkOfferingId(), join5.entity().getId(),
                JoinBuilder.JoinType.INNER);
        NetworksRegularUserCanCreateSearch.done();

        _tgMacAddress = _tgs.get("macAddress");

        SourceNATSearch = createSearchBuilder();
        SourceNATSearch.and("account", SourceNATSearch.entity().getAccountId(), Op.EQ);
        SourceNATSearch.and("datacenter", SourceNATSearch.entity().getDataCenterId(), Op.EQ);
        SourceNATSearch.and("guestType", SourceNATSearch.entity().getGuestType(), Op.EQ);
        final SearchBuilder<NetworkServiceMapVO> join6 = _ntwkSvcMap.createSearchBuilder();
        join6.and("service", join6.entity().getService(), Op.EQ);
        SourceNATSearch.join("services", join6, SourceNATSearch.entity().getId(), join6.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        SourceNATSearch.done();

        VpcNetworksCount = createSearchBuilder(Long.class);
        VpcNetworksCount.and("vpcId", VpcNetworksCount.entity().getVpcId(), Op.EQ);
        VpcNetworksCount.select(null, Func.COUNT, VpcNetworksCount.entity().getId());
        final SearchBuilder<NetworkOfferingVO> join9 = _ntwkOffDao.createSearchBuilder();
        join9.and("isSystem", join9.entity().isSystemOnly(), Op.EQ);
        VpcNetworksCount.join("offerings", join9, VpcNetworksCount.entity().getNetworkOfferingId(), join9.entity().getId(), JoinBuilder.JoinType.INNER);
        VpcNetworksCount.done();

        OfferingAccountNetworkSearch = createSearchBuilder();
        OfferingAccountNetworkSearch.select(null, Func.DISTINCT, OfferingAccountNetworkSearch.entity().getId());
        final SearchBuilder<NetworkOfferingVO> ntwkOfferingJoin = _ntwkOffDao.createSearchBuilder();
        ntwkOfferingJoin.and("isSystem", ntwkOfferingJoin.entity().isSystemOnly(), Op.EQ);
        OfferingAccountNetworkSearch.join("ntwkOfferingSearch", ntwkOfferingJoin, OfferingAccountNetworkSearch.entity().getNetworkOfferingId(), ntwkOfferingJoin.entity()
                .getId(), JoinBuilder.JoinType.LEFT);
        final SearchBuilder<NetworkAccountVO> ntwkAccountJoin = _accountsDao.createSearchBuilder();
        ntwkAccountJoin.and("accountId", ntwkAccountJoin.entity().getAccountId(), Op.EQ);
        OfferingAccountNetworkSearch.join("ntwkAccountSearch", ntwkAccountJoin, OfferingAccountNetworkSearch.entity().getId(), ntwkAccountJoin.entity().getNetworkId(),
                JoinBuilder.JoinType.INNER);
        OfferingAccountNetworkSearch.and("zoneId", OfferingAccountNetworkSearch.entity().getDataCenterId(), Op.EQ);
        OfferingAccountNetworkSearch.and("type", OfferingAccountNetworkSearch.entity().getGuestType(), Op.EQ);
        OfferingAccountNetworkSearch.done();

        GarbageCollectedSearch = createSearchBuilder(Long.class);
        GarbageCollectedSearch.selectFields(GarbageCollectedSearch.entity().getId());
        final SearchBuilder<NetworkOpVO> join7 = _ntwkOpDao.createSearchBuilder();
        join7.and("activenics", join7.entity().getActiveNicsCount(), Op.EQ);
        join7.and("gc", join7.entity().isGarbageCollected(), Op.EQ);
        join7.and("check", join7.entity().isCheckForGc(), Op.EQ);
        GarbageCollectedSearch.join("ntwkOpGC", join7, GarbageCollectedSearch.entity().getId(), join7.entity().getId(), JoinBuilder.JoinType.INNER);
        final SearchBuilder<NetworkOfferingVO> join8 = _ntwkOffDao.createSearchBuilder();
        join8.and("isPersistent", join8.entity().isPersistent(), Op.EQ);
        GarbageCollectedSearch.join("ntwkOffGC", join8, GarbageCollectedSearch.entity().getNetworkOfferingId(), join8.entity().getId(), JoinBuilder.JoinType.INNER);
        GarbageCollectedSearch.done();

        PrivateNetworkSearch = createSearchBuilder();
        PrivateNetworkSearch.and("cidr", PrivateNetworkSearch.entity().getCidr(), Op.EQ);
        PrivateNetworkSearch.and("offering", PrivateNetworkSearch.entity().getNetworkOfferingId(), Op.EQ);
        PrivateNetworkSearch.and("datacenter", PrivateNetworkSearch.entity().getDataCenterId(), Op.EQ);
        PrivateNetworkSearch.and("broadcastUri", PrivateNetworkSearch.entity().getBroadcastUri(), Op.EQ);
        final SearchBuilder<VpcGatewayVO> join10 = _vpcGatewayDao.createSearchBuilder();
        join10.and("vpc", join10.entity().getVpcId(), Op.EQ);
        PrivateNetworkSearch.join("vpcgateways", join10, PrivateNetworkSearch.entity().getId(), join10.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        PrivateNetworkSearch.done();
    }

    @Override
    public List<NetworkVO> listByZoneAndGuestType(final long accountId, final long dataCenterId, final Network.GuestType type, final Boolean isSystem) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("datacenter", dataCenterId);
        sc.setParameters("account", accountId);
        if (type != null) {
            sc.setParameters("guestType", type);
        }

        if (isSystem != null) {
            sc.setJoinParameters("offerings", "isSystem", isSystem);
        }

        return listBy(sc, null);
    }

    @Override
    public List<NetworkVO> listByGuestType(Network.GuestType type) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("guestType", type);
        return listBy(sc, null);
    }

    public List<NetworkVO> findBy(final TrafficType trafficType, final Mode mode, final BroadcastDomainType broadcastType, final long networkOfferingId, final long dataCenterId) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("broadcastType", broadcastType);
        sc.setParameters("offering", networkOfferingId);
        sc.setParameters("datacenter", dataCenterId);

        return search(sc, null);
    }

    @Override
    public List<NetworkVO> listBy(final long accountId, final long offeringId, final long dataCenterId) {
        final SearchCriteria<NetworkVO> sc = AccountSearch.create();
        sc.setParameters("offering", offeringId);
        sc.setJoinParameters("accounts", "account", accountId);
        sc.setParameters("datacenter", dataCenterId);

        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listBy(final long accountId, final long dataCenterId, final String cidr, final boolean skipVpc) {
        final SearchCriteria<NetworkVO> sc = AccountSearch.create();
        sc.setJoinParameters("accounts", "account", accountId);
        sc.setParameters("datacenter", dataCenterId);
        sc.setParameters("cidr", cidr);
        if (skipVpc) {
            sc.setParameters("vpcId", (Object)null);
        }

        return listBy(sc);
    }

    @Override
    @DB
    public NetworkVO persist(final NetworkVO network, final boolean gc, final Map<String, String> serviceProviderMap) {
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        // 1) create network
        final NetworkVO newNetwork = super.persist(network);
        // 2) add account to the network
        addAccountToNetwork(network.getId(), network.getAccountId(), true);
        // 3) add network to gc monitor table
        final NetworkOpVO op = new NetworkOpVO(network.getId(), gc);
        _opDao.persist(op);
        // 4) add services/providers for the network
        persistNetworkServiceProviders(newNetwork.getId(), serviceProviderMap);

        txn.commit();
        return newNetwork;
    }

    @Override
    @DB
    public boolean update(final Long networkId, final NetworkVO network, final Map<String, String> serviceProviderMap) {
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        super.update(networkId, network);
        if (serviceProviderMap != null) {
            _ntwkSvcMap.deleteByNetworkId(networkId);
            persistNetworkServiceProviders(networkId, serviceProviderMap);
        }

        txn.commit();
        return true;
    }

    @Override
    @DB
    public void persistNetworkServiceProviders(final long networkId, final Map<String, String> serviceProviderMap) {
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        for (final String service : serviceProviderMap.keySet()) {
            final NetworkServiceMapVO serviceMap = new NetworkServiceMapVO(networkId, Service.getService(service), Provider.getProvider(serviceProviderMap.get(service)));
            _ntwkSvcMap.persist(serviceMap);
        }
        txn.commit();
    }

    protected void addAccountToNetwork(final long networkId, final long accountId, final boolean isOwner) {
        final NetworkAccountVO account = new NetworkAccountVO(networkId, accountId, isOwner);
        _accountsDao.persist(account);
    }

    @Override
    public SearchBuilder<NetworkAccountVO> createSearchBuilderForAccount() {
        return _accountsDao.createSearchBuilder();
    }

    @Override
    public List<NetworkVO> getNetworksForOffering(final long offeringId, final long dataCenterId, final long accountId) {
        final SearchCriteria<NetworkVO> sc = RelatedConfigSearch.create();
        sc.setParameters("offering", offeringId);
        sc.setParameters("dc", dataCenterId);
        sc.setJoinParameters("account", "account", accountId);
        return search(sc, null);
    }

    @Override
    public int getOtherPersistentNetworksCount(long id, String broadcastURI, boolean isPersistent) {
        Object[] guestTypes = {"Isolated", "L2"};
        final SearchCriteria<NetworkVO> sc = PersistentNetworkSearch.create();
        sc.setParameters("id", id);
        sc.setParameters("broadcastUri", broadcastURI);
        sc.setParameters("guestType", guestTypes);
        sc.setJoinParameters("persistent", "persistent", isPersistent);
        List<NetworkVO> persistentNetworks = search(sc, null);
        return persistentNetworks.size();
    }

    @Override
    public String getNextAvailableMacAddress(final long networkConfigId, Integer zoneMacIdentifier) {
        final SequenceFetcher fetch = SequenceFetcher.getInstance();
        long seq = fetch.getNextSequence(Long.class, _tgMacAddress, networkConfigId);
        if(zoneMacIdentifier != null && zoneMacIdentifier.intValue() != 0 ){
            seq = seq | _prefix << 40 | (long)zoneMacIdentifier << 32 | networkConfigId << 16 & 0x00000000ffff0000l;
        }
        else {
            seq = seq | _prefix << 40 | _rand.nextInt(Short.MAX_VALUE) << 16 & 0x00000000ffff0000l;
        }
        return NetUtils.long2Mac(seq);
    }

    @Override
    public List<NetworkVO> listBy(final long accountId, final long networkId) {
        final SearchCriteria<NetworkVO> sc = AccountNetworkSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setJoinParameters("networkSearch", "accountId", accountId);
        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByZoneAndUriAndGuestType(long zoneId, String broadcastUri, GuestType guestType) {
        final URI searchUri = BroadcastDomainType.fromString(broadcastUri);
        final String searchRange = BroadcastDomainType.getValue(searchUri);
        final List<Integer> searchVlans = UriUtils.expandVlanUri(searchRange);
        final List<NetworkVO> overlappingNetworks = new ArrayList<>();

        final SearchCriteria<NetworkVO> sc = ZoneBroadcastUriSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        if (guestType != null) {
            sc.setParameters("guestType", guestType);
        }

        for (final NetworkVO network : listBy(sc)) {
            if (network.getBroadcastUri() == null || !network.getBroadcastUri().getScheme().equalsIgnoreCase(searchUri.getScheme())) {
                continue;
            }
            final String networkVlanRange = BroadcastDomainType.getValue(network.getBroadcastUri());
            if (networkVlanRange == null || networkVlanRange.isEmpty()) {
                continue;
            }
            for (final Integer networkVlan : UriUtils.expandVlanUri(networkVlanRange)) {
                if (searchVlans.contains(networkVlan)) {
                    overlappingNetworks.add(network);
                    break;
                }
            }
        }

        return overlappingNetworks;
    }

    @Override
    public List<NetworkVO> listByZone(final long zoneId) {
        final SearchCriteria<NetworkVO> sc = ZoneBroadcastUriSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        return search(sc, null);
    }

    @Override
    public List<NetworkVO> listByZoneSecurityGroup(final Long zoneId) {
        final SearchCriteria<NetworkVO> sc = ZoneSecurityGroupSearch.create();
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        sc.setJoinParameters("services", "service", Service.SecurityGroup.getName());
        return search(sc, null);
    }

    @Override
    public void changeActiveNicsBy(final long networkId, final int count) {
        _opDao.changeActiveNicsBy(networkId, count);
    }

    @Override
    public int getActiveNicsIn(final long networkId) {
        return _opDao.getActiveNics(networkId);
    }

    @Override
    public List<Long> findNetworksToGarbageCollect() {
        final SearchCriteria<Long> sc = GarbageCollectedSearch.create();
        sc.setJoinParameters("ntwkOffGC", "isPersistent", false);
        sc.setJoinParameters("ntwkOpGC", "activenics", 0);
        sc.setJoinParameters("ntwkOpGC", "gc", true);
        sc.setJoinParameters("ntwkOpGC", "check", true);
        return customSearch(sc, null);
    }

    @Override
    public void clearCheckForGc(final long networkId) {
        _opDao.clearCheckForGc(networkId);
    }

    @Override
    public void setCheckForGc(final long networkId) {
        _opDao.setCheckForGc(networkId);
    }

    @Override
    public List<NetworkVO> listByOwner(final long ownerId) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", ownerId);
        return listBy(sc);
    }

    @Override
    public void addDomainToNetwork(final long networkId, final long domainId, final Boolean subdomainAccess) {
        addDomainToNetworknetwork(networkId, domainId, subdomainAccess);
    }

    protected void addDomainToNetworknetwork(final long networkId, final long domainId, final Boolean subdomainAccess) {
        final NetworkDomainVO domain = new NetworkDomainVO(networkId, domainId, subdomainAccess);
        _domainsDao.persist(domain);
    }

    @Override
    public int getNetworkCountByVpcId(final long vpcId) {
        final SearchCriteria<Integer> sc = CountBy.create();
        sc.setParameters("vpcId", vpcId);
        final List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }

    @Override
    public List<NetworkVO> listSecurityGroupEnabledNetworks() {
        final SearchCriteria<NetworkVO> sc = SecurityGroupSearch.create();
        sc.setJoinParameters("services", "service", Service.SecurityGroup.getName());
        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByPhysicalNetwork(final long physicalNetworkId) {
        final SearchCriteria<NetworkVO> sc = PhysicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkTrafficType(final long physicalNetworkId, final TrafficType trafficType) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkAndProvider(final long physicalNetworkId, final String providerName) {
        final SearchBuilder<NetworkServiceMapVO> svcProviderMapSearch = _ntwkSvcMap.createSearchBuilder();
        final NetworkServiceMapVO svcProviderEntry = svcProviderMapSearch.entity();
        svcProviderMapSearch.and("Provider", svcProviderMapSearch.entity().getProvider(), SearchCriteria.Op.EQ);

        final SearchBuilder<NetworkVO> networksSearch = createSearchBuilder();
        networksSearch.and("physicalNetworkId", networksSearch.entity().getPhysicalNetworkId(), Op.EQ);
        networksSearch.join("svcProviderMapSearch", svcProviderMapSearch, networksSearch.entity().getId(), svcProviderEntry.getNetworkId(), JoinBuilder.JoinType.INNER);

        final SearchCriteria<NetworkVO> sc = networksSearch.create();
        sc.setJoinParameters("svcProviderMapSearch", "Provider", providerName);
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listBy(final long accountId, final long dataCenterId, final Network.GuestType type, final TrafficType trafficType) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("datacenter", dataCenterId);
        sc.setParameters("account", accountId);
        sc.setParameters("guestType", type);
        sc.setParameters("trafficType", trafficType);

        return listBy(sc, null);
    }

    @Override
    public List<NetworkVO> listByZoneAndTrafficType(final long zoneId, final TrafficType trafficType) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("datacenter", zoneId);
        sc.setParameters("trafficType", trafficType);

        return listBy(sc, null);
    }

    @Override
    public int getNetworkCountByNetworkOffId(final long networkOfferingId) {
        final SearchCriteria<Integer> sc = NetworksCount.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        final List<Integer> count = customSearch(sc, null);
        return count.get(0);
    }

    @Override
    public long countNetworksUserCanCreate(final long ownerId) {
        final SearchCriteria<Long> sc = NetworksRegularUserCanCreateSearch.create();
        sc.setParameters("aclType", ACLType.Account);
        sc.setParameters("displayNetwork", 1);
        sc.setJoinParameters("accounts", "account", ownerId);
        sc.setJoinParameters("ntwkOff", "specifyVlan", false);
        return customSearch(sc, null).get(0);
    }

    @Override
    public List<NetworkVO> listSourceNATEnabledNetworks(final long accountId, final long dataCenterId, final Network.GuestType type) {
        final SearchCriteria<NetworkVO> sc = SourceNATSearch.create();
        sc.setParameters("datacenter", dataCenterId);
        sc.setParameters("account", accountId);
        sc.setParameters("guestType", type);
        sc.setJoinParameters("services", "service", Service.SourceNat.getName());
        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByVpc(final long vpcId) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);

        return listBy(sc, null);
    }

    @Override
    public NetworkVO getPrivateNetwork(final String broadcastUri, final String cidr, final long accountId, final long zoneId, Long networkOfferingId, Long vpcId) {
        if (networkOfferingId == null) {
            networkOfferingId = _ntwkOffDao.findByUniqueName(NetworkOffering.SystemPrivateGatewayNetworkOffering).getId();
        }
        final SearchCriteria<NetworkVO> sc = PrivateNetworkSearch.create();
        sc.setParameters("datacenter", zoneId);
        sc.setParameters("broadcastUri", broadcastUri);
        sc.setParameters("cidr", cidr);
        sc.setParameters("account", accountId);
        sc.setParameters("offering", networkOfferingId);
        sc.setJoinParameters("vpcgateways", "vpc", vpcId);
        return findOneBy(sc);
    }

    @Override
    @DB
    public boolean remove(final Long id) {
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        final NetworkVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, ResourceObjectType.Network);
        }
        final boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public long countVpcNetworks(final long vpcId) {
        final SearchCriteria<Long> sc = VpcNetworksCount.create();
        sc.setParameters("vpcId", vpcId);
        //offering shouldn't be system (the one used by the private gateway)
        sc.setJoinParameters("offerings", "isSystem", false);
        return customSearch(sc, null).get(0);
    }

    @Override
    public boolean updateState(final State currentState, final Event event, final State nextState, final Network vo, final Object data) {
        // TODO: ensure this update is correct
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        final NetworkVO networkVo = (NetworkVO)vo;
        networkVo.setState(nextState);
        super.update(networkVo.getId(), networkVo);

        txn.commit();
        return true;
    }

    @Override
    public List<NetworkVO> listNetworksByAccount(final long accountId, final long zoneId, final Network.GuestType type, final boolean isSystem) {
        final SearchCriteria<NetworkVO> sc = OfferingAccountNetworkSearch.create();
        sc.setJoinParameters("ntwkOfferingSearch", "isSystem", isSystem);
        sc.setJoinParameters("ntwkAccountSearch", "accountId", accountId);
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("type", type);

        final List<NetworkVO> networks = search(sc, null);
        return networks;
    }

    @Override
    public List<NetworkVO> listRedundantNetworks() {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("redundant", true);
        return listBy(sc, null);
    }

    @Override
    public List<NetworkVO> listVpcNetworks() {
        final SearchBuilder<NetworkVO> sb = createSearchBuilder();
        sb.and("vpcId", sb.entity().getVpcId(), Op.NNULL);
        sb.done();

        final SearchCriteria<NetworkVO> sc = sb.create();

        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByAclId(final long aclId) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("aclId", aclId);

        return listBy(sc, null);
    }

    @Override
    public int getNonSystemNetworkCountByVpcId(final long vpcId) {
        final SearchCriteria<Integer> sc = CountBy.create();
        sc.setParameters("vpcId", vpcId);
        sc.setJoinParameters("offerings", "isSystem", false);
        final List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }

    @Override
    public List<NetworkVO> listNetworkVO(List<Long> idset) {
        final SearchCriteria<NetworkVO> sc_2 = createSearchCriteria();
        final Filter searchFilter_2 = new Filter(NetworkVO.class, "id", false, null, null);
        sc_2.addAnd("networkOfferingId", SearchCriteria.Op.IN, idset);
        sc_2.addAnd("removed", SearchCriteria.Op.EQ, null);
        return this.search(sc_2, searchFilter_2);
    }

    @Override
    public NetworkVO findByVlan(String vlan) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("broadcastType", BroadcastDomainType.Vlan);
        sc.setParameters("broadcastUri", BroadcastDomainType.Vlan.toUri(vlan));
        return findOneBy(sc);
    }

    @Override
    public List<NetworkVO> listByAccountIdNetworkName(final long accountId, final String name) {
        final SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("name", name);

        return listBy(sc, null);
    }

    /**
     * True when a requested PVLAN pair overlaps with any existing PVLAN pair within the same physical network, i.e when:
     *      - The requested exact PVLAN pair exists
     *      - The requested secondary VLAN ID is secondary VLAN ID of an existing PVLAN pair
     *      - The requested secondary VLAN ID is primary VLAN ID of an existing PVLAN pair
     *      - The requested primary VLAN ID is secondary VLAN ID of an existing PVLAN pair
     */
    protected boolean isNetworkOverlappingRequestedPvlan(Integer existingPrimaryVlan, Integer existingSecondaryVlan, Network.PVlanType existingPvlanType,
                                                         Integer requestedPrimaryVlan, Integer requestedSecondaryVlan, Network.PVlanType requestedPvlanType) {
        if (existingPrimaryVlan == null || existingSecondaryVlan == null || requestedPrimaryVlan == null || requestedSecondaryVlan == null) {
            throw new CloudRuntimeException(String.format("Missing VLAN ID while checking PVLAN pair (%s, %s)" +
                    " against existing pair (%s, %s)", existingPrimaryVlan, existingSecondaryVlan, requestedPrimaryVlan, requestedSecondaryVlan));
        }
        boolean exactMatch = existingPrimaryVlan.equals(requestedPrimaryVlan) && existingSecondaryVlan.equals(requestedSecondaryVlan);
        boolean secondaryVlanUsed = requestedPvlanType != Network.PVlanType.Promiscuous && requestedSecondaryVlan.equals(existingPrimaryVlan) || requestedSecondaryVlan.equals(existingSecondaryVlan);
        boolean primaryVlanUsed = existingPvlanType != Network.PVlanType.Promiscuous && requestedPrimaryVlan.equals(existingSecondaryVlan);
        boolean isolatedMax = false;
        boolean promiscuousMax = false;
        if (requestedPvlanType == Network.PVlanType.Isolated && existingPrimaryVlan.equals(requestedPrimaryVlan) && existingPvlanType.equals(Network.PVlanType.Isolated)) {
            isolatedMax = true;
        } else if (requestedPvlanType == Network.PVlanType.Promiscuous && existingPrimaryVlan.equals(requestedPrimaryVlan) && existingPvlanType == Network.PVlanType.Promiscuous) {
            promiscuousMax = true;
        }
        return exactMatch || secondaryVlanUsed || primaryVlanUsed || isolatedMax || promiscuousMax;
    }

    // True when a VLAN ID overlaps with an existing PVLAN primary or secondary ID
    protected boolean isNetworkOverlappingRequestedPvlan(Integer existingPrimaryVlan, Integer existingSecondaryVlan, Integer requestedVlan) {
        return requestedVlan.equals(existingPrimaryVlan) || requestedVlan.equals(existingSecondaryVlan);
    }

    protected Network.PVlanType getNetworkPvlanType(long networkId, List<Integer> existingPvlan) {
        Network.PVlanType existingPvlanType = null;
        NetworkDetailVO pvlanTypeDetail = networkDetailsDao.findDetail(networkId, ApiConstants.ISOLATED_PVLAN_TYPE);
        if (pvlanTypeDetail != null) {
            existingPvlanType = Network.PVlanType.valueOf(pvlanTypeDetail.getValue());
        } else {
            existingPvlanType = existingPvlan.get(0).equals(existingPvlan.get(1)) ? Network.PVlanType.Promiscuous : Network.PVlanType.Isolated;
        }
        return existingPvlanType;
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkPvlan(long physicalNetworkId, String broadcastUri) {
        final URI searchUri = BroadcastDomainType.fromString(broadcastUri);
        if (!VLAN_OR_VXLAN.contains(searchUri.getScheme().toLowerCase())) {
            throw new CloudRuntimeException(
                    String.format("Requested URI '%s' is not in the expected format. Expected URI Scheme as 'vlan://VID' or 'vxlan://VID'.", searchUri.toString()));
        }
        final String searchRange = BroadcastDomainType.getValue(searchUri);
        final List<Integer> searchVlans = UriUtils.expandVlanUri(searchRange);
        final List<NetworkVO> overlappingNetworks = new ArrayList<>();

        final SearchCriteria<NetworkVO> sc = PhysicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        for (final NetworkVO network : listBy(sc)) {
            if (network.getBroadcastUri() == null || !network.getBroadcastUri().getScheme().equalsIgnoreCase("pvlan")) {
                continue;
            }
            // Ensure existing and proposed VLAN don't overlap
            final String networkVlanRange = BroadcastDomainType.getValue(network.getBroadcastUri());
            if (networkVlanRange == null || networkVlanRange.isEmpty()) {
                continue;
            }
            List<Integer> existingPvlan = UriUtils.expandPvlanUri(networkVlanRange);
            if (isNetworkOverlappingRequestedPvlan(existingPvlan.get(0), existingPvlan.get(1), searchVlans.get(0))) {
                overlappingNetworks.add(network);
                break;
            }
        }

        return overlappingNetworks;
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkPvlan(long physicalNetworkId, String broadcastUri, Network.PVlanType pVlanType) {
        final URI searchUri = BroadcastDomainType.fromString(broadcastUri);
        if (!searchUri.getScheme().equalsIgnoreCase("pvlan")) {
            throw new CloudRuntimeException("PVLAN requested but URI is not in the expected format: " + searchUri.toString());
        }
        final String searchRange = BroadcastDomainType.getValue(searchUri);
        final List<Integer> searchVlans = UriUtils.expandPvlanUri(searchRange);
        final List<NetworkVO> overlappingNetworks = new ArrayList<>();

        final SearchCriteria<NetworkVO> sc = PhysicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        for (final NetworkVO network : listBy(sc)) {
            if (network.getBroadcastUri() == null || !network.getBroadcastUri().getScheme().equalsIgnoreCase("pvlan")) {
                continue;
            }
            final String networkVlanRange = BroadcastDomainType.getValue(network.getBroadcastUri());
            if (networkVlanRange == null || networkVlanRange.isEmpty()) {
                continue;
            }
            List<Integer> existingPvlan = UriUtils.expandPvlanUri(networkVlanRange);
            Network.PVlanType existingPvlanType = getNetworkPvlanType(network.getId(), existingPvlan);
            if (isNetworkOverlappingRequestedPvlan(existingPvlan.get(0), existingPvlan.get(1), existingPvlanType,
                    searchVlans.get(0), searchVlans.get(1), pVlanType)) {
                overlappingNetworks.add(network);
                break;
            }
        }

        return overlappingNetworks;
    }
}
