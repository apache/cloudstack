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

import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;
import javax.persistence.TableGenerator;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.springframework.stereotype.Component;

import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.Network.Event;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDaoImpl;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import com.cloud.utils.db.*;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.net.NetUtils;

@Component
@Local(value = NetworkDao.class)
@DB(txn = false)
public class NetworkDaoImpl extends GenericDaoBase<NetworkVO, Long> implements NetworkDao {
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
    GenericSearchBuilder<NetworkVO, Long>  CountByZoneAndURI;
    GenericSearchBuilder<NetworkVO, Long> VpcNetworksCount;
    SearchBuilder<NetworkVO> OfferingAccountNetworkSearch;

    GenericSearchBuilder<NetworkVO, Long> GarbageCollectedSearch;
    
    
    
    @Inject ResourceTagDao _tagsDao;
    @Inject NetworkAccountDao _accountsDao;
    @Inject NetworkDomainDao _domainsDao;
    @Inject NetworkOpDao _opDao;
    @Inject NetworkServiceMapDao _ntwkSvcMap;
    @Inject NetworkOfferingDao _ntwkOffDao;
    @Inject NetworkOpDao _ntwkOpDao;

    TableGenerator _tgMacAddress;

    Random _rand = new Random(System.currentTimeMillis());
    long _prefix = 0x2;

    public NetworkDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        AllFieldsSearch = createSearchBuilder();
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
        SearchBuilder<NetworkOfferingVO> join1 = _ntwkOffDao.createSearchBuilder();
        join1.and("isSystem", join1.entity().isSystemOnly(), Op.EQ);
        join1.and("isRedundant", join1.entity().getRedundantRouter(), Op.EQ);
        AllFieldsSearch.join("offerings", join1, AllFieldsSearch.entity().getNetworkOfferingId(), join1.entity().getId(), JoinBuilder.JoinType.INNER);
        AllFieldsSearch.done();

        AccountSearch = createSearchBuilder();
        AccountSearch.and("offering", AccountSearch.entity().getNetworkOfferingId(), Op.EQ);
        SearchBuilder<NetworkAccountVO> join = _accountsDao.createSearchBuilder();
        join.and("account", join.entity().getAccountId(), Op.EQ);
        AccountSearch.join("accounts", join, AccountSearch.entity().getId(), join.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        AccountSearch.and("datacenter", AccountSearch.entity().getDataCenterId(), Op.EQ);
        AccountSearch.and("cidr", AccountSearch.entity().getCidr(), Op.EQ);
        AccountSearch.and("vpcId", AccountSearch.entity().getVpcId(), Op.EQ);
        AccountSearch.done();

        RelatedConfigSearch = createSearchBuilder();
        RelatedConfigSearch.and("offering", RelatedConfigSearch.entity().getNetworkOfferingId(), Op.EQ);
        RelatedConfigSearch.and("datacenter", RelatedConfigSearch.entity().getDataCenterId(), Op.EQ);
        SearchBuilder<NetworkAccountVO> join2 = _accountsDao.createSearchBuilder();
        join2.and("account", join2.entity().getAccountId(), Op.EQ);
        RelatedConfigSearch.join("account", join2, join2.entity().getNetworkId(), RelatedConfigSearch.entity().getId(), JoinType.INNER);
        RelatedConfigSearch.done();

        AccountNetworkSearch = createSearchBuilder();
        AccountNetworkSearch.and("networkId", AccountNetworkSearch.entity().getId(), Op.EQ);
        SearchBuilder<NetworkAccountVO> mapJoin = _accountsDao.createSearchBuilder();
        mapJoin.and("accountId", mapJoin.entity().getAccountId(), Op.EQ);
        AccountNetworkSearch.join("networkSearch", mapJoin, AccountNetworkSearch.entity().getId(), mapJoin.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        AccountNetworkSearch.done();

        ZoneBroadcastUriSearch = createSearchBuilder();
        ZoneBroadcastUriSearch.and("dataCenterId", ZoneBroadcastUriSearch.entity().getDataCenterId(), Op.EQ);
        ZoneBroadcastUriSearch.and("broadcastUri", ZoneBroadcastUriSearch.entity().getBroadcastUri(), Op.EQ);
        ZoneBroadcastUriSearch.and("guestType", ZoneBroadcastUriSearch.entity().getGuestType(), Op.EQ);
        ZoneBroadcastUriSearch.done();

        CountByZoneAndURI = createSearchBuilder(Long.class);
        CountByZoneAndURI.select(null, Func.COUNT, null);
        CountByZoneAndURI.and("dataCenterId", CountByZoneAndURI.entity().getDataCenterId(), Op.EQ);
        CountByZoneAndURI.and("broadcastUri", CountByZoneAndURI.entity().getBroadcastUri(), Op.EQ);
        CountByZoneAndURI.and("guestType", CountByZoneAndURI.entity().getGuestType(), Op.EQ);

        CountByZoneAndURI.done();
        
        ZoneSecurityGroupSearch = createSearchBuilder();
        ZoneSecurityGroupSearch.and("dataCenterId", ZoneSecurityGroupSearch.entity().getDataCenterId(), Op.EQ);
        SearchBuilder<NetworkServiceMapVO> offJoin = _ntwkSvcMap.createSearchBuilder();
        offJoin.and("service", offJoin.entity().getService(), Op.EQ);
        ZoneSecurityGroupSearch.join("services", offJoin, ZoneSecurityGroupSearch.entity().getId(), offJoin.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        ZoneSecurityGroupSearch.done();

        CountBy = createSearchBuilder(Integer.class);
        CountBy.select(null, Func.COUNT, CountBy.entity().getId());
        CountBy.and("offeringId", CountBy.entity().getNetworkOfferingId(), Op.EQ);
        CountBy.and("vpcId", CountBy.entity().getVpcId(), Op.EQ);
        CountBy.and("removed", CountBy.entity().getRemoved(), Op.NULL);
        CountBy.done();

        PhysicalNetworkSearch = createSearchBuilder();
        PhysicalNetworkSearch.and("physicalNetworkId", PhysicalNetworkSearch.entity().getPhysicalNetworkId(), Op.EQ);
        PhysicalNetworkSearch.done();

        SecurityGroupSearch = createSearchBuilder();
        SearchBuilder<NetworkServiceMapVO> join3 = _ntwkSvcMap.createSearchBuilder();
        join3.and("service", join3.entity().getService(), Op.EQ);
        SecurityGroupSearch.join("services", join3, SecurityGroupSearch.entity().getId(), join3.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        SecurityGroupSearch.done();
        
        NetworksCount = createSearchBuilder(Integer.class);
        NetworksCount.select(null, Func.COUNT, NetworksCount.entity().getId());
        NetworksCount.and("networkOfferingId", NetworksCount.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        NetworksCount.done();
        
        NetworksRegularUserCanCreateSearch = createSearchBuilder(Long.class);
        NetworksRegularUserCanCreateSearch.and("aclType", NetworksRegularUserCanCreateSearch.entity().getAclType(), Op.EQ);
        NetworksRegularUserCanCreateSearch.select(null, Func.COUNT, NetworksRegularUserCanCreateSearch.entity().getId());
        SearchBuilder<NetworkAccountVO> join4 = _accountsDao.createSearchBuilder();
        join4.and("account", join4.entity().getAccountId(), Op.EQ);
        join4.and("isOwner", join4.entity().isOwner(), Op.EQ);
        NetworksRegularUserCanCreateSearch.join("accounts", join4, NetworksRegularUserCanCreateSearch.entity().getId(), join4.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        SearchBuilder<NetworkOfferingVO> join5 = _ntwkOffDao.createSearchBuilder();
        join5.and("specifyVlan", join5.entity().getSpecifyVlan(), Op.EQ);
        NetworksRegularUserCanCreateSearch.join("ntwkOff", join5, NetworksRegularUserCanCreateSearch.entity().getNetworkOfferingId(), join5.entity().getId(), JoinBuilder.JoinType.INNER);    
        NetworksRegularUserCanCreateSearch.done();

        _tgMacAddress = _tgs.get("macAddress");
        
        SourceNATSearch = createSearchBuilder();
        SourceNATSearch.and("account", SourceNATSearch.entity().getAccountId(), Op.EQ);
        SourceNATSearch.and("datacenter", SourceNATSearch.entity().getDataCenterId(), Op.EQ);
        SourceNATSearch.and("guestType", SourceNATSearch.entity().getGuestType(), Op.EQ);
        SearchBuilder<NetworkServiceMapVO> join6 = _ntwkSvcMap.createSearchBuilder();
        join6.and("service", join6.entity().getService(), Op.EQ);
        SourceNATSearch.join("services", join6, SourceNATSearch.entity().getId(), join6.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        SourceNATSearch.done();
        
        VpcNetworksCount = createSearchBuilder(Long.class);
        VpcNetworksCount.and("vpcId", VpcNetworksCount.entity().getVpcId(), Op.EQ);
        VpcNetworksCount.select(null, Func.COUNT, VpcNetworksCount.entity().getId());
        VpcNetworksCount.done();

        OfferingAccountNetworkSearch = createSearchBuilder();
        OfferingAccountNetworkSearch.select(null, Func.DISTINCT, OfferingAccountNetworkSearch.entity().getId());
        SearchBuilder<NetworkOfferingVO> ntwkOfferingJoin = _ntwkOffDao.createSearchBuilder();
        ntwkOfferingJoin.and("isSystem", ntwkOfferingJoin.entity().isSystemOnly(), Op.EQ);
        OfferingAccountNetworkSearch.join("ntwkOfferingSearch", ntwkOfferingJoin, OfferingAccountNetworkSearch.entity().getNetworkOfferingId(), ntwkOfferingJoin.entity().getId(), JoinBuilder.JoinType.LEFT);
        SearchBuilder<NetworkAccountVO> ntwkAccountJoin = _accountsDao.createSearchBuilder();
        ntwkAccountJoin.and("accountId", ntwkAccountJoin.entity().getAccountId(), Op.EQ);
        OfferingAccountNetworkSearch.join("ntwkAccountSearch", ntwkAccountJoin, OfferingAccountNetworkSearch.entity().getId(), ntwkAccountJoin.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        OfferingAccountNetworkSearch.and("zoneId", OfferingAccountNetworkSearch.entity().getDataCenterId(), Op.EQ);
        OfferingAccountNetworkSearch.and("type", OfferingAccountNetworkSearch.entity().getGuestType(), Op.EQ);
        OfferingAccountNetworkSearch.done();

        GarbageCollectedSearch = createSearchBuilder(Long.class);
        GarbageCollectedSearch.selectField(GarbageCollectedSearch.entity().getId());
        SearchBuilder<NetworkOpVO> join7 = _ntwkOpDao.createSearchBuilder();
        join7.and("activenics", join7.entity().getActiveNicsCount(), Op.EQ);
        join7.and("gc", join7.entity().isGarbageCollected(), Op.EQ);
        join7.and("check", join7.entity().isCheckForGc(), Op.EQ);
        GarbageCollectedSearch.join("ntwkOpGC", join7, GarbageCollectedSearch.entity().getId(), join7.entity().getId(), JoinBuilder.JoinType.INNER);
        SearchBuilder<NetworkOfferingVO> join8 = _ntwkOffDao.createSearchBuilder();
        join8.and("isPersistent", join8.entity().getIsPersistent(), Op.EQ);
        GarbageCollectedSearch.join("ntwkOffGC", join8, GarbageCollectedSearch.entity().getNetworkOfferingId(), join8.entity().getId(), JoinBuilder.JoinType.INNER);
        GarbageCollectedSearch.done();

    }

    @Override
    public List<NetworkVO> listByZoneAndGuestType(long accountId, long dataCenterId, Network.GuestType type, Boolean isSystem) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
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

    public List<NetworkVO> findBy(TrafficType trafficType, Mode mode, BroadcastDomainType broadcastType, long networkOfferingId, long dataCenterId) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("broadcastType", broadcastType);
        sc.setParameters("offering", networkOfferingId);
        sc.setParameters("datacenter", dataCenterId);

        return search(sc, null);
    }

    @Override
    public List<NetworkVO> listBy(long accountId, long offeringId, long dataCenterId) {
        SearchCriteria<NetworkVO> sc = AccountSearch.create();
        sc.setParameters("offering", offeringId);
        sc.setJoinParameters("accounts", "account", accountId);
        sc.setParameters("datacenter", dataCenterId);

        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listBy(long accountId, long dataCenterId, String cidr, boolean skipVpc) {
        SearchCriteria<NetworkVO> sc = AccountSearch.create();
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
    public NetworkVO persist(NetworkVO network, boolean gc, Map<String, String> serviceProviderMap) {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        // 1) create network
        NetworkVO newNetwork = super.persist(network);
        // 2) add account to the network
        addAccountToNetwork(network.getId(), network.getAccountId(), true);
        // 3) add network to gc monitor table
        NetworkOpVO op = new NetworkOpVO(network.getId(), gc);
        _opDao.persist(op);
        // 4) add services/providers for the network
        persistNetworkServiceProviders(newNetwork.getId(), serviceProviderMap);

        txn.commit();
        return newNetwork;
    }

    @Override
    @DB
    public boolean update(Long networkId, NetworkVO network, Map<String, String> serviceProviderMap) {
        Transaction txn = Transaction.currentTxn();
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
    public void persistNetworkServiceProviders(long networkId, Map<String, String> serviceProviderMap) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (String service : serviceProviderMap.keySet()) {
            NetworkServiceMapVO serviceMap = new NetworkServiceMapVO(networkId, Service.getService(service), Provider.getProvider(serviceProviderMap.get(service)));
            _ntwkSvcMap.persist(serviceMap);
        }
        txn.commit();
    }

    protected void addAccountToNetwork(long networkId, long accountId, boolean isOwner) {
        NetworkAccountVO account = new NetworkAccountVO(networkId, accountId, isOwner);
        _accountsDao.persist(account);
    }

    @Override
    public SearchBuilder<NetworkAccountVO> createSearchBuilderForAccount() {
        return _accountsDao.createSearchBuilder();
    }

    @Override
    public List<NetworkVO> getNetworksForOffering(long offeringId, long dataCenterId, long accountId) {
        SearchCriteria<NetworkVO> sc = RelatedConfigSearch.create();
        sc.setParameters("offering", offeringId);
        sc.setParameters("dc", dataCenterId);
        sc.setJoinParameters("account", "account", accountId);
        return search(sc, null);
    }

    @Override
    public String getNextAvailableMacAddress(long networkConfigId) {
        SequenceFetcher fetch = SequenceFetcher.getInstance();

        long seq = fetch.getNextSequence(Long.class, _tgMacAddress, networkConfigId);
        seq = seq | _prefix << 40 | ((_rand.nextInt(Short.MAX_VALUE) << 16) & 0x00000000ffff0000l);
        return NetUtils.long2Mac(seq);
    }

    @Override
    public List<NetworkVO> listBy(long accountId, long networkId) {
        SearchCriteria<NetworkVO> sc = AccountNetworkSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setJoinParameters("networkSearch", "accountId", accountId);
        return listBy(sc);
    }

    @Override
    public long countByZoneAndUri(long zoneId, String broadcastUri) {

        SearchCriteria<Long> sc = CountByZoneAndURI.create();
        sc.setParameters("dataCenterId", zoneId);
        sc.setParameters("broadcastUri", broadcastUri);
        
        return customSearch(sc, null).get(0);
    }

    @Override
    public List<NetworkVO> listByZone(long zoneId) {
        SearchCriteria<NetworkVO> sc = ZoneBroadcastUriSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        return search(sc, null);
    }

    @Override
    public long countByZoneUriAndGuestType(long zoneId, String broadcastUri, GuestType guestType) {
        SearchCriteria<Long> sc = CountByZoneAndURI.create();
        sc.setParameters("dataCenterId", zoneId);
        sc.setParameters("broadcastUri", broadcastUri);
        sc.setParameters("guestType", guestType);
        return customSearch(sc, null).get(0);
    }

    @Override
    public List<NetworkVO> listByZoneSecurityGroup(Long zoneId) {
        SearchCriteria<NetworkVO> sc = ZoneSecurityGroupSearch.create();
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        sc.setJoinParameters("services", "service", Service.SecurityGroup.getName());
        return search(sc, null);
    }

    @Override
    public void changeActiveNicsBy(long networkId, int count) {
        _opDao.changeActiveNicsBy(networkId, count);
    }

    @Override
    public int getActiveNicsIn(long networkId) {
        return _opDao.getActiveNics(networkId);
    }

    @Override
    public List<Long> findNetworksToGarbageCollect() {
        SearchCriteria<Long> sc = GarbageCollectedSearch.create();
        sc.setJoinParameters("ntwkOffGC", "isPersistent", false);
        sc.setJoinParameters("ntwkOpGC", "activenics", 0);
        sc.setJoinParameters("ntwkOpGC", "gc", true);
        sc.setJoinParameters("ntwkOpGC", "check", true);
        return customSearch(sc, null);
    }

    @Override
    public void clearCheckForGc(long networkId) {
        _opDao.clearCheckForGc(networkId);
    }
    
    @Override
    public void setCheckForGc(long networkId) {
        _opDao.setCheckForGc(networkId);
    }

    @Override
    public List<NetworkVO> listByOwner(long ownerId) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", ownerId);
        return listBy(sc);
    }

    @Override
    public void addDomainToNetwork(long networkId, long domainId, Boolean subdomainAccess) {
        addDomainToNetworknetwork(networkId, domainId, subdomainAccess);
    }

    protected void addDomainToNetworknetwork(long networkId, long domainId, Boolean subdomainAccess) {
        NetworkDomainVO domain = new NetworkDomainVO(networkId, domainId, subdomainAccess);
        _domainsDao.persist(domain);
    }
    
    
    @Override
    public int getNetworkCountByVpcId(long vpcId) {
        SearchCriteria<Integer> sc = CountBy.create();
        sc.setParameters("vpcId", vpcId);
        List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }
    

    @Override
    public List<NetworkVO> listSecurityGroupEnabledNetworks() {
        SearchCriteria<NetworkVO> sc = SecurityGroupSearch.create();
        sc.setJoinParameters("services", "service", Service.SecurityGroup.getName());
        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<NetworkVO> sc = PhysicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkTrafficType(long physicalNetworkId, TrafficType trafficType) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkAndProvider(long physicalNetworkId, String providerName) {
        SearchBuilder<NetworkServiceMapVO> svcProviderMapSearch = _ntwkSvcMap.createSearchBuilder();
        NetworkServiceMapVO svcProviderEntry = svcProviderMapSearch.entity();
        svcProviderMapSearch.and("Provider", svcProviderMapSearch.entity().getProvider(), SearchCriteria.Op.EQ);

        SearchBuilder<NetworkVO> networksSearch = createSearchBuilder();
        networksSearch.and("physicalNetworkId", networksSearch.entity().getPhysicalNetworkId(), Op.EQ);
        networksSearch.join("svcProviderMapSearch", svcProviderMapSearch, networksSearch.entity().getId(), svcProviderEntry.getNetworkId(), JoinBuilder.JoinType.INNER);

        SearchCriteria<NetworkVO> sc = networksSearch.create();
        sc.setJoinParameters("svcProviderMapSearch", "Provider", providerName);
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        return listBy(sc);
    }

    @Override
    public List<NetworkVO> listBy(long accountId, long dataCenterId, Network.GuestType type, TrafficType trafficType) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("datacenter", dataCenterId);
        sc.setParameters("account", accountId);
        sc.setParameters("guestType", type);
        sc.setParameters("trafficType", trafficType);

        return listBy(sc, null);
    }

    @Override
    public List<NetworkVO> listByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("datacenter", zoneId);
        sc.setParameters("trafficType", trafficType);

        return listBy(sc, null);
    }
    
    @Override
    public int getNetworkCountByNetworkOffId(long networkOfferingId) {
        SearchCriteria<Integer> sc = NetworksCount.create();
        sc.setParameters("networkOfferingId", networkOfferingId);
        List<Integer> count = customSearch(sc, null);
        return count.get(0);
    }
    
    @Override
    public long countNetworksUserCanCreate(long ownerId) {
        SearchCriteria<Long> sc = NetworksRegularUserCanCreateSearch.create();
        sc.setParameters("aclType", ACLType.Account);
        sc.setJoinParameters("accounts", "account", ownerId);
        sc.setJoinParameters("ntwkOff", "specifyVlan", false);
        return customSearch(sc, null).get(0);
    }
    
    
    @Override
    public List<NetworkVO> listSourceNATEnabledNetworks(long accountId, long dataCenterId, Network.GuestType type) {
        SearchCriteria<NetworkVO> sc = SourceNATSearch.create();
        sc.setParameters("datacenter", dataCenterId);
        sc.setParameters("account", accountId);
        sc.setParameters("guestType", type);
        sc.setJoinParameters("services", "service", Service.SourceNat.getName());
        return listBy(sc);
    }
    
    @Override
    public List<NetworkVO> listByVpc(long vpcId) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);

        return listBy(sc, null);
    }


    @Override
    public NetworkVO getPrivateNetwork(String broadcastUri, String cidr, long accountId, long zoneId) {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setParameters("datacenter", zoneId);
        sc.setParameters("broadcastUri", broadcastUri);
        sc.setParameters("cidr", cidr);
        sc.setParameters("account", accountId);
        sc.setParameters("offering", _ntwkOffDao.findByUniqueName(NetworkOffering.SystemPrivateGatewayNetworkOffering).getId());
        return findOneBy(sc);
    }

    @Override
    @DB
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        NetworkVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, TaggedResourceType.Network);
        }
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public long countVpcNetworks(long vpcId) {
        SearchCriteria<Long> sc = VpcNetworksCount.create();
        sc.setParameters("vpcId", vpcId);
        return customSearch(sc, null).get(0);
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, Network vo, Object data) {
       // TODO: ensure this update is correct
       Transaction txn = Transaction.currentTxn();
       txn.start();

       NetworkVO networkVo = (NetworkVO) vo;
       networkVo.setState(nextState);
       super.update(networkVo.getId(), networkVo);

       txn.commit();
       return true;
    }

    @Override
    public List<NetworkVO> listNetworksByAccount(long accountId, long zoneId, Network.GuestType type, boolean isSystem) {
        SearchCriteria<NetworkVO> sc = OfferingAccountNetworkSearch.create();
        sc.setJoinParameters("ntwkOfferingSearch", "isSystem", isSystem);
        sc.setJoinParameters("ntwkAccountSearch", "accountId", accountId);
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("type", type);

        List<NetworkVO> networks = search(sc, null);
        return networks;
    }

    @Override
    public List<NetworkVO> listRedundantNetworks() {
        SearchCriteria<NetworkVO> sc = AllFieldsSearch.create();
        sc.setJoinParameters("offerings", "isRedundant", true);
        return listBy(sc, null);
    }
}
