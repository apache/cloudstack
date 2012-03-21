/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.dao;

import java.util.List;
import java.util.Map;

import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.NetworkAccountVO;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchBuilder;

public interface NetworkDao extends GenericDao<NetworkVO, Long> {

    List<NetworkVO> listByOwner(long ownerId);

    List<NetworkVO> listBy(long accountId, long offeringId, long dataCenterId);

    List<NetworkVO> listBy(long accountId, long dataCenterId, String cidr);

    List<NetworkVO> listBy(long accountId, long dataCenterId, Network.GuestType type);

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

    List<NetworkVO> listBy(long zoneId, String broadcastUri);

    List<NetworkVO> listByZone(long zoneId);

    void changeActiveNicsBy(long networkId, int nicsCount);

    int getActiveNicsIn(long networkId);

    List<Long> findNetworksToGarbageCollect();

    void clearCheckForGc(long networkId);

    List<NetworkVO> listByZoneSecurityGroup(Long zoneId);

    void addDomainToNetwork(long networkId, long domainId, Boolean subdomainAccess);

    Long getNetworkCountByOfferingId(long offeringId);

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

}
