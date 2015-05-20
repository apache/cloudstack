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

import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.fsm.StateDao;

public interface NetworkDao extends GenericDao<NetworkVO, Long>, StateDao<State, Network.Event, Network> {

    List<NetworkVO> listByOwner(long ownerId);

    List<NetworkVO> listByGuestType(GuestType type);

    List<NetworkVO> listBy(long accountId, long offeringId, long dataCenterId);

    List<NetworkVO> listBy(long accountId, long dataCenterId, String cidr, boolean skipVpc);

    List<NetworkVO> listByZoneAndGuestType(long accountId, long dataCenterId, Network.GuestType type, Boolean isSystem);

    NetworkVO persist(NetworkVO network, boolean gc, Map<String, String> serviceProviderMap);

    SearchBuilder<NetworkAccountVO> createSearchBuilderForAccount();

    List<NetworkVO> getNetworksForOffering(long offeringId, long dataCenterId, long accountId);

    @Override
    @Deprecated
    NetworkVO persist(NetworkVO vo);

    /**
     * Retrieves the next available mac address in this network configuration.
     *
     * @param networkConfigId
     *            id
     * @return mac address if there is one. null if not.
     */
    String getNextAvailableMacAddress(long networkConfigId);

    List<NetworkVO> listBy(long accountId, long networkId);

    long countByZoneAndUri(long zoneId, String broadcastUri);

    long countByZoneUriAndGuestType(long zoneId, String broadcastUri, GuestType guestType);

    List<NetworkVO> listByZone(long zoneId);

    void changeActiveNicsBy(long networkId, int nicsCount);

    int getActiveNicsIn(long networkId);

    List<Long> findNetworksToGarbageCollect();

    void clearCheckForGc(long networkId);

    List<NetworkVO> listByZoneSecurityGroup(Long zoneId);

    void addDomainToNetwork(long networkId, long domainId, Boolean subdomainAccess);

    List<NetworkVO> listByPhysicalNetwork(long physicalNetworkId);

    List<NetworkVO> listSecurityGroupEnabledNetworks();

    List<NetworkVO> listByPhysicalNetworkTrafficType(long physicalNetworkId, TrafficType trafficType);

    List<NetworkVO> listBy(long accountId, long dataCenterId, Network.GuestType type, TrafficType trafficType);

    List<NetworkVO> listByPhysicalNetworkAndProvider(long physicalNetworkId, String providerName);

    void persistNetworkServiceProviders(long networkId, Map<String, String> serviceProviderMap);

    boolean update(Long networkId, NetworkVO network, Map<String, String> serviceProviderMap);

    List<NetworkVO> listByZoneAndTrafficType(long zoneId, TrafficType trafficType);

    void setCheckForGc(long networkId);

    int getNetworkCountByNetworkOffId(long networkOfferingId);

    long countNetworksUserCanCreate(long ownerId);

    List<NetworkVO> listSourceNATEnabledNetworks(long accountId, long dataCenterId, GuestType type);

    int getNetworkCountByVpcId(long vpcId);

    List<NetworkVO> listByVpc(long vpcId);

    NetworkVO getPrivateNetwork(String broadcastUri, String cidr, long accountId, long zoneId, Long networkOfferingId);

    long countVpcNetworks(long vpcId);

    List<NetworkVO> listNetworksByAccount(long accountId, long zoneId, Network.GuestType type, boolean isSystem);

    List<NetworkVO> listRedundantNetworks();

    List<NetworkVO> listVpcNetworks();

    List<NetworkVO> listByAclId(long aclId);

    int getNonSystemNetworkCountByVpcId(long vpcId);
}
