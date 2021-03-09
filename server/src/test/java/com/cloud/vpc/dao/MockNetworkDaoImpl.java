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
package com.cloud.vpc.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkAccountVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;

@DB()
public class MockNetworkDaoImpl extends GenericDaoBase<NetworkVO, Long> implements NetworkDao {

    @Override
    public List<NetworkVO> listByOwner(final long ownerId) {
        return null;
    }

    @Override
    public List<NetworkVO> listByGuestType(GuestType type) {
        return null;
    }

    @Override
    public List<NetworkVO> listBy(final long accountId, final long offeringId, final long dataCenterId) {
        return null;
    }

    @Override
    public List<NetworkVO> listBy(final long accountId, final long dataCenterId, final String cidr, final boolean skipVpc) {
        return null;
    }

    @Override
    public List<NetworkVO> listByZoneAndGuestType(final long accountId, final long dataCenterId, final GuestType type, final Boolean isSystem) {
        return null;
    }

    @Override
    public NetworkVO persist(final NetworkVO network, final boolean gc, final Map<String, String> serviceProviderMap) {
        return null;
    }

    @Override
    public SearchBuilder<NetworkAccountVO> createSearchBuilderForAccount() {
        return null;
    }

    @Override
    public List<NetworkVO> getNetworksForOffering(final long offeringId, final long dataCenterId, final long accountId) {
        return null;
    }

    @Override
    public int getOtherPersistentNetworksCount(long id, String broadcastURI, boolean isPersistent) {
        return 0;
    }

    @Override
    public String getNextAvailableMacAddress(final long networkConfigId, Integer zoneMacIdentifier) {
        return null;
    }

    @Override
    public List<NetworkVO> listBy(final long accountId, final long networkId) {
        return null;
    }

    @Override
    public List<NetworkVO> listByZoneAndUriAndGuestType(long zoneId, String broadcastUri, GuestType guestType) {
        return null;
    }

    @Override
    public List<NetworkVO> listByZone(final long zoneId) {
        return null;
    }

    @Override
    public void changeActiveNicsBy(final long networkId, final int nicsCount) {
    }

    @Override
    public int getActiveNicsIn(final long networkId) {
        return 0;
    }

    @Override
    public List<Long> findNetworksToGarbageCollect() {
        return null;
    }

    @Override
    public void clearCheckForGc(final long networkId) {
    }

    @Override
    public List<NetworkVO> listByZoneSecurityGroup(final Long zoneId) {
        return null;
    }

    @Override
    public void addDomainToNetwork(final long networkId, final long domainId, final Boolean subdomainAccess) {
    }

    @Override
    public List<NetworkVO> listByPhysicalNetwork(final long physicalNetworkId) {
        return null;
    }

    @Override
    public List<NetworkVO> listSecurityGroupEnabledNetworks() {
        return null;
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkTrafficType(final long physicalNetworkId, final TrafficType trafficType) {
        return null;
    }

    @Override
    public List<NetworkVO> listBy(final long accountId, final long dataCenterId, final GuestType type, final TrafficType trafficType) {
        return null;
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkAndProvider(final long physicalNetworkId, final String providerName) {
        return null;
    }

    @Override
    public void persistNetworkServiceProviders(final long networkId, final Map<String, String> serviceProviderMap) {
    }

    @Override
    public boolean update(final Long networkId, final NetworkVO network, final Map<String, String> serviceProviderMap) {
        return false;
    }

    @Override
    public List<NetworkVO> listByZoneAndTrafficType(final long zoneId, final TrafficType trafficType) {
        return null;
    }

    @Override
    public void setCheckForGc(final long networkId) {
    }

    @Override
    public int getNetworkCountByNetworkOffId(final long networkOfferingId) {
        return 0;
    }

    @Override
    public long countNetworksUserCanCreate(final long ownerId) {
        return 0;
    }

    @Override
    public List<NetworkVO> listSourceNATEnabledNetworks(final long accountId, final long dataCenterId, final GuestType type) {
        return null;
    }

    @Override
    public int getNetworkCountByVpcId(final long vpcId) {
        return 0;
    }

    @Override
    public List<NetworkVO> listByVpc(final long vpcId) {
        final List<NetworkVO> networks = new ArrayList<NetworkVO>();
        networks.add(new NetworkVO());
        return networks;
    }

    @Override
    public NetworkVO getPrivateNetwork(final String broadcastUri, final String cidr, final long accountId, final long zoneId, final Long netofferid, final Long vpcId) {
        return null;
    }

    @Override
    public long countVpcNetworks(final long vpcId) {
        return 0;
    }

    @Override
    public boolean updateState(final Network.State currentState, final Network.Event event, final Network.State nextState, final Network vo, final Object data) {
        return true;
    }

    @Override
    public List<NetworkVO> listNetworksByAccount(final long accountId, final long zoneId, final GuestType type, final boolean isSystem) {
        return null;
    }

    @Override
    public List<NetworkVO> listRedundantNetworks() {
        return null;
    }

    @Override
    public List<NetworkVO> listVpcNetworks() {
        return null;
    }

    @Override
    public List<NetworkVO> listByAclId(final long aclId) {
        return null;
    }

    @Override
    public int getNonSystemNetworkCountByVpcId(final long vpcId) {
        return 0;
    }

    @Override
    public List<NetworkVO> listNetworkVO(List<Long> idset) {
        return null;
    }

    @Override
    public NetworkVO findByVlan(String vlan) {
        return null;
    }

    @Override
    public List<NetworkVO> listByAccountIdNetworkName(final long accountId, final String name) {
        return null;
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkPvlan(long physicalNetworkId, String broadcastUri, Network.PVlanType pVlanType) {
        return null;
    }

    @Override
    public List<NetworkVO> listByPhysicalNetworkPvlan(long physicalNetworkId, String broadcastUri) {
        return null;
    }
}
