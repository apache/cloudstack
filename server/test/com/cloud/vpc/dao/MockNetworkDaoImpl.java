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

import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkAccountVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;

import javax.ejb.Local;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Local(value = NetworkDao.class)
@DB()
public class MockNetworkDaoImpl extends GenericDaoBase<NetworkVO, Long> implements NetworkDao{

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByOwner(long)
     */
    @Override
    public List<NetworkVO> listByOwner(long ownerId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listBy(long, long, long)
     */
    @Override
    public List<NetworkVO> listBy(long accountId, long offeringId, long dataCenterId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listBy(long, long, java.lang.String, boolean)
     */
    @Override
    public List<NetworkVO> listBy(long accountId, long dataCenterId, String cidr, boolean skipVpc) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByZoneAndGuestType(long, long, com.cloud.network.Network.GuestType, java.lang.Boolean)
     */
    @Override
    public List<NetworkVO> listByZoneAndGuestType(long accountId, long dataCenterId, GuestType type, Boolean isSystem) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#persist(com.cloud.network.NetworkVO, boolean, java.util.Map)
     */
    @Override
    public NetworkVO persist(NetworkVO network, boolean gc, Map<String, String> serviceProviderMap) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#createSearchBuilderForAccount()
     */
    @Override
    public SearchBuilder<NetworkAccountVO> createSearchBuilderForAccount() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getNetworksForOffering(long, long, long)
     */
    @Override
    public List<NetworkVO> getNetworksForOffering(long offeringId, long dataCenterId, long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getNextAvailableMacAddress(long)
     */
    @Override
    public String getNextAvailableMacAddress(long networkConfigId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listBy(long, long)
     */
    @Override
    public List<NetworkVO> listBy(long accountId, long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#countByZoneAndUri(long, java.lang.String)
     */
    @Override
    public long countByZoneAndUri(long zoneId, String broadcastUri) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#countByZoneUriAndGuestType(long, java.lang.String, com.cloud.network.Network.GuestType)
     */
    @Override
    public long countByZoneUriAndGuestType(long zoneId, String broadcastUri, GuestType guestType) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByZone(long)
     */
    @Override
    public List<NetworkVO> listByZone(long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#changeActiveNicsBy(long, int)
     */
    @Override
    public void changeActiveNicsBy(long networkId, int nicsCount) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getActiveNicsIn(long)
     */
    @Override
    public int getActiveNicsIn(long networkId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#findNetworksToGarbageCollect()
     */
    @Override
    public List<Long> findNetworksToGarbageCollect() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#clearCheckForGc(long)
     */
    @Override
    public void clearCheckForGc(long networkId) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByZoneSecurityGroup(java.lang.Long)
     */
    @Override
    public List<NetworkVO> listByZoneSecurityGroup(Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#addDomainToNetwork(long, long, java.lang.Boolean)
     */
    @Override
    public void addDomainToNetwork(long networkId, long domainId, Boolean subdomainAccess) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByPhysicalNetwork(long)
     */
    @Override
    public List<NetworkVO> listByPhysicalNetwork(long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listSecurityGroupEnabledNetworks()
     */
    @Override
    public List<NetworkVO> listSecurityGroupEnabledNetworks() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByPhysicalNetworkTrafficType(long, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public List<NetworkVO> listByPhysicalNetworkTrafficType(long physicalNetworkId, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listBy(long, long, com.cloud.network.Network.GuestType, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public List<NetworkVO> listBy(long accountId, long dataCenterId, GuestType type, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByPhysicalNetworkAndProvider(long, java.lang.String)
     */
    @Override
    public List<NetworkVO> listByPhysicalNetworkAndProvider(long physicalNetworkId, String providerName) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#persistNetworkServiceProviders(long, java.util.Map)
     */
    @Override
    public void persistNetworkServiceProviders(long networkId, Map<String, String> serviceProviderMap) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#update(java.lang.Long, com.cloud.network.NetworkVO, java.util.Map)
     */
    @Override
    public boolean update(Long networkId, NetworkVO network, Map<String, String> serviceProviderMap) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByZoneAndTrafficType(long, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public List<NetworkVO> listByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#setCheckForGc(long)
     */
    @Override
    public void setCheckForGc(long networkId) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getNetworkCountByNetworkOffId(long)
     */
    @Override
    public int getNetworkCountByNetworkOffId(long networkOfferingId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#countNetworksUserCanCreate(long)
     */
    @Override
    public long countNetworksUserCanCreate(long ownerId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listSourceNATEnabledNetworks(long, long, com.cloud.network.Network.GuestType)
     */
    @Override
    public List<NetworkVO> listSourceNATEnabledNetworks(long accountId, long dataCenterId, GuestType type) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getNetworkCountByVpcId(long)
     */
    @Override
    public int getNetworkCountByVpcId(long vpcId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByVpc(long)
     */
    @Override
    public List<NetworkVO> listByVpc(long vpcId) {
        List<NetworkVO> networks = new ArrayList<NetworkVO>();
        networks.add(new NetworkVO());
        return networks;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getPrivateNetwork(java.lang.String, java.lang.String, long, long)
     */
    @Override
    public NetworkVO getPrivateNetwork(String broadcastUri, String cidr, long accountId, long zoneId, Long netofferid) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#countVpcNetworks(long)
     */
    @Override
    public long countVpcNetworks(long vpcId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean updateState(Network.State currentState, Network.Event event, Network.State nextState, Network vo, Object data) {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listNetworksByAccount(long, long, com.cloud.network.Network.GuestType, boolean)
     */
    @Override
    public List<NetworkVO> listNetworksByAccount(long accountId, long zoneId, GuestType type, boolean isSystem) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> listRedundantNetworks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> listByAclId(long aclId) {
        return null;
    }

    
    @Override
    public int getNonSystemNetworkCountByVpcId(long vpcId) {
        return 0;
    }
}
