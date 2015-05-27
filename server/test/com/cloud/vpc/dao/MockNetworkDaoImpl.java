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

import javax.ejb.Local;

import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkAccountVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;

@Local(value = NetworkDao.class)
@DB()
public class MockNetworkDaoImpl extends GenericDaoBase<NetworkVO, Long> implements NetworkDao {

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByOwner(long)
     */
    @Override
    public List<NetworkVO> listByOwner(final long ownerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> listByGuestType(GuestType type) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listBy(long, long, long)
     */
    @Override
    public List<NetworkVO> listBy(final long accountId, final long offeringId, final long dataCenterId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listBy(long, long, java.lang.String, boolean)
     */
    @Override
    public List<NetworkVO> listBy(final long accountId, final long dataCenterId, final String cidr, final boolean skipVpc) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByZoneAndGuestType(long, long, com.cloud.network.Network.GuestType, java.lang.Boolean)
     */
    @Override
    public List<NetworkVO> listByZoneAndGuestType(final long accountId, final long dataCenterId, final GuestType type, final Boolean isSystem) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#persist(com.cloud.network.NetworkVO, boolean, java.util.Map)
     */
    @Override
    public NetworkVO persist(final NetworkVO network, final boolean gc, final Map<String, String> serviceProviderMap) {
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
    public List<NetworkVO> getNetworksForOffering(final long offeringId, final long dataCenterId, final long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getNextAvailableMacAddress(long)
     */
    @Override
    public String getNextAvailableMacAddress(final long networkConfigId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listBy(long, long)
     */
    @Override
    public List<NetworkVO> listBy(final long accountId, final long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#countByZoneAndUri(long, java.lang.String)
     */
    @Override
    public long countByZoneAndUri(final long zoneId, final String broadcastUri) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#countByZoneUriAndGuestType(long, java.lang.String, com.cloud.network.Network.GuestType)
     */
    @Override
    public long countByZoneUriAndGuestType(final long zoneId, final String broadcastUri, final GuestType guestType) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByZone(long)
     */
    @Override
    public List<NetworkVO> listByZone(final long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#changeActiveNicsBy(long, int)
     */
    @Override
    public void changeActiveNicsBy(final long networkId, final int nicsCount) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getActiveNicsIn(long)
     */
    @Override
    public int getActiveNicsIn(final long networkId) {
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
    public void clearCheckForGc(final long networkId) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByZoneSecurityGroup(java.lang.Long)
     */
    @Override
    public List<NetworkVO> listByZoneSecurityGroup(final Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#addDomainToNetwork(long, long, java.lang.Boolean)
     */
    @Override
    public void addDomainToNetwork(final long networkId, final long domainId, final Boolean subdomainAccess) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByPhysicalNetwork(long)
     */
    @Override
    public List<NetworkVO> listByPhysicalNetwork(final long physicalNetworkId) {
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
    public List<NetworkVO> listByPhysicalNetworkTrafficType(final long physicalNetworkId, final TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listBy(long, long, com.cloud.network.Network.GuestType, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public List<NetworkVO> listBy(final long accountId, final long dataCenterId, final GuestType type, final TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByPhysicalNetworkAndProvider(long, java.lang.String)
     */
    @Override
    public List<NetworkVO> listByPhysicalNetworkAndProvider(final long physicalNetworkId, final String providerName) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#persistNetworkServiceProviders(long, java.util.Map)
     */
    @Override
    public void persistNetworkServiceProviders(final long networkId, final Map<String, String> serviceProviderMap) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#update(java.lang.Long, com.cloud.network.NetworkVO, java.util.Map)
     */
    @Override
    public boolean update(final Long networkId, final NetworkVO network, final Map<String, String> serviceProviderMap) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByZoneAndTrafficType(long, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public List<NetworkVO> listByZoneAndTrafficType(final long zoneId, final TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#setCheckForGc(long)
     */
    @Override
    public void setCheckForGc(final long networkId) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getNetworkCountByNetworkOffId(long)
     */
    @Override
    public int getNetworkCountByNetworkOffId(final long networkOfferingId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#countNetworksUserCanCreate(long)
     */
    @Override
    public long countNetworksUserCanCreate(final long ownerId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listSourceNATEnabledNetworks(long, long, com.cloud.network.Network.GuestType)
     */
    @Override
    public List<NetworkVO> listSourceNATEnabledNetworks(final long accountId, final long dataCenterId, final GuestType type) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getNetworkCountByVpcId(long)
     */
    @Override
    public int getNetworkCountByVpcId(final long vpcId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listByVpc(long)
     */
    @Override
    public List<NetworkVO> listByVpc(final long vpcId) {
        final List<NetworkVO> networks = new ArrayList<NetworkVO>();
        networks.add(new NetworkVO());
        return networks;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#getPrivateNetwork(java.lang.String, java.lang.String, long, long)
     */
    @Override
    public NetworkVO getPrivateNetwork(final String broadcastUri, final String cidr, final long accountId, final long zoneId, final Long netofferid) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#countVpcNetworks(long)
     */
    @Override
    public long countVpcNetworks(final long vpcId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean updateState(final Network.State currentState, final Network.Event event, final Network.State nextState, final Network vo, final Object data) {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.dao.NetworkDao#listNetworksByAccount(long, long, com.cloud.network.Network.GuestType, boolean)
     */
    @Override
    public List<NetworkVO> listNetworksByAccount(final long accountId, final long zoneId, final GuestType type, final boolean isSystem) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> listRedundantNetworks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> listVpcNetworks() {
        // TODO Auto-generated method stub
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
}
