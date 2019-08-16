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
package com.cloud.vpc;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.api.command.admin.config.UpdateCfgCmd;
import org.apache.cloudstack.api.command.admin.network.CreateManagementNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteManagementNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.CreateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.CreateServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.DeleteDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.DeleteServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.pod.DeletePodCmd;
import org.apache.cloudstack.api.command.admin.pod.UpdatePodCmd;
import org.apache.cloudstack.api.command.admin.region.CreatePortableIpRangeCmd;
import org.apache.cloudstack.api.command.admin.region.DeletePortableIpRangeCmd;
import org.apache.cloudstack.api.command.admin.region.ListPortableIpRangesCmd;
import org.apache.cloudstack.api.command.admin.vlan.CreateVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DedicatePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DeleteVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.ReleasePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.UpdateVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.zone.CreateZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.DeleteZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.UpdateZoneCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkOfferingsCmd;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.region.PortableIp;
import org.apache.cloudstack.region.PortableIpRange;
import org.springframework.stereotype.Component;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationService;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDaoImpl;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

@Component
public class MockConfigurationManagerImpl extends ManagerBase implements ConfigurationManager, ConfigurationService {
    @Inject
    NetworkOfferingDaoImpl _ntwkOffDao;

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#updateConfiguration(org.apache.cloudstack.api.commands.UpdateCfgCmd)
     */
    @Override
    public Configuration updateConfiguration(UpdateCfgCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#createServiceOffering(org.apache.cloudstack.api.commands.CreateServiceOfferingCmd)
     */
    @Override
    public ServiceOffering createServiceOffering(CreateServiceOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#updateServiceOffering(org.apache.cloudstack.api.commands.UpdateServiceOfferingCmd)
     */
    @Override
    public ServiceOffering updateServiceOffering(UpdateServiceOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#deleteServiceOffering(org.apache.cloudstack.api.commands.DeleteServiceOfferingCmd)
     */
    @Override
    public boolean deleteServiceOffering(DeleteServiceOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Long> getServiceOfferingDomains(Long serviceOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> getServiceOfferingZones(Long serviceOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#updateDiskOffering(org.apache.cloudstack.api.commands.UpdateDiskOfferingCmd)
     */
    @Override
    public DiskOffering updateDiskOffering(UpdateDiskOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#deleteDiskOffering(org.apache.cloudstack.api.commands.DeleteDiskOfferingCmd)
     */
    @Override
    public boolean deleteDiskOffering(DeleteDiskOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#createDiskOffering(org.apache.cloudstack.api.commands.CreateDiskOfferingCmd)
     */
    @Override
    public DiskOffering createDiskOffering(CreateDiskOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> getDiskOfferingDomains(Long diskOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> getDiskOfferingZones(Long diskOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#createPod(long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Pod createPod(long zoneId, String name, String startIp, String endIp, String gateway, String netmask, String allocationState) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#createPodIpRange(org.apache.cloudstack.api.command.admin.network.CreateManagementNetworkIpRangeCmd)
     */
    @Override
    public Pod createPodIpRange(CreateManagementNetworkIpRangeCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#deletePodIpRange(org.apache.cloudstack.api.command.admin.network.DeleteManagementNetworkIpRangeCmd)
     */
    @Override
    public void deletePodIpRange(DeleteManagementNetworkIpRangeCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#editPod(org.apache.cloudstack.api.commands.UpdatePodCmd)
     */
    @Override
    public Pod editPod(UpdatePodCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#deletePod(org.apache.cloudstack.api.commands.DeletePodCmd)
     */
    @Override
    public boolean deletePod(DeletePodCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#createZone(org.apache.cloudstack.api.commands.CreateZoneCmd)
     */
    @Override
    public DataCenter createZone(CreateZoneCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#editZone(org.apache.cloudstack.api.commands.UpdateZoneCmd)
     */
    @Override
    public DataCenter editZone(UpdateZoneCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#deleteZone(org.apache.cloudstack.api.commands.DeleteZoneCmd)
     */
    @Override
    public boolean deleteZone(DeleteZoneCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#createVlanAndPublicIpRange(org.apache.cloudstack.api.commands.CreateVlanIpRangeCmd)
     */
    @Override
    public Vlan createVlanAndPublicIpRange(CreateVlanIpRangeCmd cmd) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException, ResourceAllocationException{
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#updateVlanAndPublicIpRange(org.apache.cloudstack.api
     * .commands.UpdateVlanIpRangeCmd)
     */
    @Override
    public Vlan updateVlanAndPublicIpRange(UpdateVlanIpRangeCmd cmd) throws ConcurrentOperationException,
            ResourceUnavailableException, ResourceAllocationException{
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#markDefaultZone(java.lang.String, long, long)
     */
    @Override
    public Account markDefaultZone(String accountName, long domainId, long defaultZoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#deleteVlanIpRange(org.apache.cloudstack.api.commands.DeleteVlanIpRangeCmd)
     */
    @Override
    public boolean deleteVlanIpRange(DeleteVlanIpRangeCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#createNetworkOffering(org.apache.cloudstack.api.commands.CreateNetworkOfferingCmd)
     */
    @Override
    public NetworkOffering createNetworkOffering(CreateNetworkOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#updateNetworkOffering(org.apache.cloudstack.api.commands.UpdateNetworkOfferingCmd)
     */
    @Override
    public NetworkOffering updateNetworkOffering(UpdateNetworkOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#searchForNetworkOfferings(org.apache.cloudstack.api.commands.ListNetworkOfferingsCmd)
     */
    @Override
    public Pair<List<? extends NetworkOffering>, Integer> searchForNetworkOfferings(ListNetworkOfferingsCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#deleteNetworkOffering(org.apache.cloudstack.api.commands.DeleteNetworkOfferingCmd)
     */
    @Override
    public boolean deleteNetworkOffering(DeleteNetworkOfferingCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Long> getNetworkOfferingDomains(Long networkOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> getNetworkOfferingZones(Long networkOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#getNetworkOfferingNetworkRate(long)
     */
    @Override
    public Integer getNetworkOfferingNetworkRate(long networkOfferingId, Long dataCenterId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#getVlanAccount(long)
     */
    @Override
    public Account getVlanAccount(long vlanId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#listNetworkOfferings(com.cloud.network.Networks.TrafficType, boolean)
     */
    @Override
    public List<? extends NetworkOffering> listNetworkOfferings(TrafficType trafficType, boolean systemOnly) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#getDefaultPageSize()
     */
    @Override
    public Long getDefaultPageSize() {
        return 500L;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#getServiceOfferingNetworkRate(long)
     */
    @Override
    public Integer getServiceOfferingNetworkRate(long serviceOfferingId, Long dataCenterId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationService#isOfferingForVpc(com.cloud.offering.NetworkOffering)
     */
    @Override
    public boolean isOfferingForVpc(NetworkOffering offering) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PortableIpRange createPortableIpRange(CreatePortableIpRangeCmd cmd) throws ConcurrentOperationException {
        return null;// TODO Auto-generated method stub
    }

    @Override
    public boolean deletePortableIpRange(DeletePortableIpRangeCmd cmd) {
        return false;// TODO Auto-generated method stub
    }

    @Override
    public List<? extends PortableIpRange> listPortableIpRanges(ListPortableIpRangesCmd cmd) {
        return null;// TODO Auto-generated method stub
    }

    @Override
    public List<? extends PortableIp> listPortableIps(long id) {
        return null;// TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#updateConfiguration(long, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String updateConfiguration(long userId, String name, String category, String value, String scope, Long resourceId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#createPod(long, java.lang.String, long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp, String allocationState,
        boolean skipGatewayOverlapCheck) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#deleteVlanAndPublicIpRange(long, long, com.cloud.user.Account)
     */
    @Override
    public boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#checkZoneAccess(com.cloud.user.Account, com.cloud.dc.DataCenter)
     */
    @Override
    public void checkZoneAccess(Account caller, DataCenter zone) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#checkDiskOfferingAccess(com.cloud.user.Account, com.cloud.offering.DiskOffering)
     */
    @Override
    public void checkDiskOfferingAccess(Account caller, DiskOffering dof, DataCenter zone) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#createNetworkOffering(java.lang.String, java.lang.String, com.cloud.network.Networks.TrafficType, java.lang.String, boolean, com.cloud.offering.NetworkOffering.Availability, java.lang.Integer, java.util.Map, boolean, com.cloud.network.Network.GuestType, boolean, java.lang.Long, boolean, java.util.Map, boolean)
     */
    @Override
    public NetworkOfferingVO createNetworkOffering(String name, String displayText, TrafficType trafficType, String tags, boolean specifyVlan, Availability availability,
                                                   Integer networkRate, Map<Service, Set<Provider>> serviceProviderMap, boolean isDefault, GuestType type, boolean systemOnly, Long serviceOfferingId,
                                                   boolean conserveMode, Map<Service, Map<Capability, String>> serviceCapabilityMap, boolean specifyIpRanges, boolean isPersistent,
                                                   Map<NetworkOffering.Detail, String> details, boolean egressDefaultPolicy, Integer maxconn, boolean enableKeepAlive, Boolean forVpc, List<Long> domainIds, List<Long> zoneIds) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#createVlanAndPublicIpRange(long, long, long, boolean, java.lang.Long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.cloud.user.Account)
     */
    @Override
    public Vlan createVlanAndPublicIpRange(long zoneId, long networkId, long physicalNetworkId, boolean forVirtualNetwork, boolean forSystemVms, Long podId, String startIP, String endIP,
        String vlanGateway, String vlanNetmask, String vlanId, boolean bypassVlanOverlapCheck, Domain domain, Account vlanOwner, String startIPv6, String endIPv6, String vlanGatewayv6, String vlanCidrv6)
        throws InsufficientCapacityException, ConcurrentOperationException, InvalidParameterValueException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#createDefaultSystemNetworks(long)
     */
    @Override
    public void createDefaultSystemNetworks(long zoneId) throws ConcurrentOperationException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#releaseDomainSpecificVirtualRanges(long)
     */
    @Override
    public boolean releaseDomainSpecificVirtualRanges(long domainId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#releaseAccountSpecificVirtualRanges(long)
     */
    @Override
    public boolean releaseAccountSpecificVirtualRanges(long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#editPod(long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Pod editPod(long id, String name, String startIp, String endIp, String gateway, String netmask, String allocationStateStr) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#checkPodCidrSubnets(long, java.lang.Long, java.lang.String)
     */
    @Override
    public void checkPodCidrSubnets(long zoneId, Long podIdToBeSkipped, String cidr) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#findPodAllocationState(com.cloud.dc.HostPodVO)
     */
    @Override
    public AllocationState findPodAllocationState(HostPodVO pod) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#findClusterAllocationState(com.cloud.dc.ClusterVO)
     */
    @Override
    public AllocationState findClusterAllocationState(ClusterVO cluster) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.configuration.ConfigurationManager#createZone(long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Long, com.cloud.dc.DataCenter.NetworkType, java.lang.String, java.lang.String, boolean, boolean)
     */
    @Override
    public DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String guestCidr, String domain,
        Long domainId, NetworkType zoneType, String allocationState, String networkDomain, boolean isSecurityGroupEnabled, boolean isLocalStorageEnabled, String ip6Dns1,
        String ip6Dns2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vlan dedicatePublicIpRange(DedicatePublicIpRangeCmd cmd) throws ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releasePublicIpRange(ReleasePublicIpRangeCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Domain getVlanDomain(long vlanId) {
        // TODO Auto-generated method stub
        return null;
    }

}
