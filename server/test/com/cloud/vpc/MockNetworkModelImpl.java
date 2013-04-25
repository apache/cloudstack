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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.dc.Vlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;

@Local(value = {NetworkModel.class})
public class MockNetworkModelImpl extends ManagerBase implements NetworkModel {

    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        return "MockNetworkModelImpl";
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listPublicIpsAssignedToGuestNtwk(long, long, java.lang.Boolean)
     */
    @Override
    public List<IPAddressVO> listPublicIpsAssignedToGuestNtwk(long accountId, long associatedNetworkId,
            Boolean sourceNat) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getSystemAccountNetworkOfferings(java.lang.String[])
     */
    @Override
    public List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNics(long)
     */
    @Override
    public List<? extends Nic> getNics(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNextAvailableMacAddressInNetwork(long)
     */
    @Override
    public String getNextAvailableMacAddressInNetwork(long networkConfigurationId)
            throws InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getPublicIpAddress(long)
     */
    @Override
    public PublicIpAddress getPublicIpAddress(long ipAddressId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listPodVlans(long)
     */
    @Override
    public List<? extends Vlan> listPodVlans(long podId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listNetworksUsedByVm(long, boolean)
     */
    @Override
    public List<NetworkVO> listNetworksUsedByVm(long vmId, boolean isSystem) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNicInNetwork(long, long)
     */
    @Override
    public Nic getNicInNetwork(long vmId, long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNicsForTraffic(long, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public List<? extends Nic> getNicsForTraffic(long vmId, TrafficType type) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultNetworkForVm(long)
     */
    @Override
    public Network getDefaultNetworkForVm(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultNic(long)
     */
    @Override
    public Nic getDefaultNic(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getUserDataUpdateProvider(com.cloud.network.Network)
     */
    @Override
    public UserDataServiceProvider getUserDataUpdateProvider(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#networkIsConfiguredForExternalNetworking(long, long)
     */
    @Override
    public boolean networkIsConfiguredForExternalNetworking(long zoneId, long networkId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNetworkServiceCapabilities(long, com.cloud.network.Network.Service)
     */
    @Override
    public Map<Capability, String> getNetworkServiceCapabilities(long networkId, Service service) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#areServicesSupportedByNetworkOffering(long, com.cloud.network.Network.Service[])
     */
    @Override
    public boolean areServicesSupportedByNetworkOffering(long networkOfferingId, Service... services) {
        return (_ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(networkOfferingId, services));
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNetworkWithSecurityGroupEnabled(java.lang.Long)
     */
    @Override
    public NetworkVO getNetworkWithSecurityGroupEnabled(Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getIpOfNetworkElementInVirtualNetwork(long, long)
     */
    @Override
    public String getIpOfNetworkElementInVirtualNetwork(long accountId, long dataCenterId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listNetworksForAccount(long, long, com.cloud.network.Network.GuestType)
     */
    @Override
    public List<NetworkVO> listNetworksForAccount(long accountId, long zoneId, GuestType type) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listAllNetworksInAllZonesByType(com.cloud.network.Network.GuestType)
     */
    @Override
    public List<NetworkVO> listAllNetworksInAllZonesByType(GuestType type) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getGlobalGuestDomainSuffix()
     */
    @Override
    public String getGlobalGuestDomainSuffix() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getStartIpAddress(long)
     */
    @Override
    public String getStartIpAddress(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getIpInNetwork(long, long)
     */
    @Override
    public String getIpInNetwork(long vmId, long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getIpInNetworkIncludingRemoved(long, long)
     */
    @Override
    public String getIpInNetworkIncludingRemoved(long vmId, long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getPodIdForVlan(long)
     */
    @Override
    public Long getPodIdForVlan(long vlanDbId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listNetworkOfferingsForUpgrade(long)
     */
    @Override
    public List<Long> listNetworkOfferingsForUpgrade(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isSecurityGroupSupportedInNetwork(com.cloud.network.Network)
     */
    @Override
    public boolean isSecurityGroupSupportedInNetwork(Network network) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isProviderSupportServiceInNetwork(long, com.cloud.network.Network.Service, com.cloud.network.Network.Provider)
     */
    @Override
    public boolean isProviderSupportServiceInNetwork(long networkId, Service service, Provider provider) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isProviderEnabledInPhysicalNetwork(long, java.lang.String)
     */
    @Override
    public boolean isProviderEnabledInPhysicalNetwork(long physicalNetowrkId, String providerName) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNetworkTag(com.cloud.hypervisor.Hypervisor.HypervisorType, com.cloud.network.Network)
     */
    @Override
    public String getNetworkTag(HypervisorType hType, Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getElementServices(com.cloud.network.Network.Provider)
     */
    @Override
    public List<Service> getElementServices(Provider provider) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#canElementEnableIndividualServices(com.cloud.network.Network.Provider)
     */
    @Override
    public boolean canElementEnableIndividualServices(Provider provider) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#areServicesSupportedInNetwork(long, com.cloud.network.Network.Service[])
     */
    @Override
    public boolean areServicesSupportedInNetwork(long networkId, Service... services) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isNetworkSystem(com.cloud.network.Network)
     */
    @Override
    public boolean isNetworkSystem(Network network) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNetworkOfferingServiceCapabilities(com.cloud.offering.NetworkOffering, com.cloud.network.Network.Service)
     */
    @Override
    public Map<Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getPhysicalNetworkId(com.cloud.network.Network)
     */
    @Override
    public Long getPhysicalNetworkId(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getAllowSubdomainAccessGlobal()
     */
    @Override
    public boolean getAllowSubdomainAccessGlobal() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isProviderForNetwork(com.cloud.network.Network.Provider, long)
     */
    @Override
    public boolean isProviderForNetwork(Provider provider, long networkId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isProviderForNetworkOffering(com.cloud.network.Network.Provider, long)
     */
    @Override
    public boolean isProviderForNetworkOffering(Provider provider, long networkOfferingId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#canProviderSupportServices(java.util.Map)
     */
    @Override
    public void canProviderSupportServices(Map<Provider, Set<Service>> providersMap) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getPhysicalNetworkInfo(long, com.cloud.hypervisor.Hypervisor.HypervisorType)
     */
    @Override
    public List<PhysicalNetworkSetupInfo> getPhysicalNetworkInfo(long dcId, HypervisorType hypervisorType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#canAddDefaultSecurityGroup()
     */
    @Override
    public boolean canAddDefaultSecurityGroup() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listNetworkOfferingServices(long)
     */
    @Override
    public List<Service> listNetworkOfferingServices(long networkOfferingId) {
        if (networkOfferingId == 2) {
            return new ArrayList<Service>();
        }

        List<Service> services = new ArrayList<Service>();
        services.add(Service.SourceNat);
        return services;

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#areServicesEnabledInZone(long, com.cloud.offering.NetworkOffering, java.util.List)
     */
    @Override
    public boolean areServicesEnabledInZone(long zoneId, NetworkOffering offering, List<Service> services) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#checkIpForService(com.cloud.network.IPAddressVO, com.cloud.network.Network.Service, java.lang.Long)
     */
    @Override
    public boolean checkIpForService(IpAddress ip, Service service, Long networkId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#checkCapabilityForProvider(java.util.Set, com.cloud.network.Network.Service, com.cloud.network.Network.Capability, java.lang.String)
     */
    @Override
    public void checkCapabilityForProvider(Set<Provider> providers, Service service, Capability cap, String capValue) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultUniqueProviderForService(java.lang.String)
     */
    @Override
    public Provider getDefaultUniqueProviderForService(String serviceName) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#checkNetworkPermissions(com.cloud.user.Account, com.cloud.network.Network)
     */
    @Override
    public void checkNetworkPermissions(Account owner, Network network) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultManagementTrafficLabel(long, com.cloud.hypervisor.Hypervisor.HypervisorType)
     */
    @Override
    public String getDefaultManagementTrafficLabel(long zoneId, HypervisorType hypervisorType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultStorageTrafficLabel(long, com.cloud.hypervisor.Hypervisor.HypervisorType)
     */
    @Override
    public String getDefaultStorageTrafficLabel(long zoneId, HypervisorType hypervisorType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultPublicTrafficLabel(long, com.cloud.hypervisor.Hypervisor.HypervisorType)
     */
    @Override
    public String getDefaultPublicTrafficLabel(long dcId, HypervisorType vmware) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultGuestTrafficLabel(long, com.cloud.hypervisor.Hypervisor.HypervisorType)
     */
    @Override
    public String getDefaultGuestTrafficLabel(long dcId, HypervisorType vmware) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getElementImplementingProvider(java.lang.String)
     */
    @Override
    public NetworkElement getElementImplementingProvider(String providerName) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getAccountNetworkDomain(long, long)
     */
    @Override
    public String getAccountNetworkDomain(long accountId, long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultNetworkDomain()
     */
    @Override
    public String getDefaultNetworkDomain() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNtwkOffDistinctProviders(long)
     */
    @Override
    public List<Provider> getNtwkOffDistinctProviders(long ntwkOffId) {
        return new ArrayList<Provider>();
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listPublicIpsAssignedToAccount(long, long, java.lang.Boolean)
     */
    @Override
    public List<IPAddressVO> listPublicIpsAssignedToAccount(long accountId, long dcId, Boolean sourceNat) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getPhysicalNtwksSupportingTrafficType(long, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public List<? extends PhysicalNetwork> getPhysicalNtwksSupportingTrafficType(long zoneId, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isPrivateGateway(com.cloud.vm.Nic)
     */
    @Override
    public boolean isPrivateGateway(Nic guestNic) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNetworkCapabilities(long)
     */
    @Override
    public Map<Service, Map<Capability, String>> getNetworkCapabilities(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getSystemNetworkByZoneAndTrafficType(long, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public Network getSystemNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDedicatedNetworkDomain(long)
     */
    @Override
    public Long getDedicatedNetworkDomain(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNetworkOfferingServiceProvidersMap(long)
     */
    @Override
    public Map<Service, Set<Provider>> getNetworkOfferingServiceProvidersMap(long networkOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listSupportedNetworkServiceProviders(java.lang.String)
     */
    @Override
    public List<? extends Provider> listSupportedNetworkServiceProviders(String serviceName) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#listNetworksByVpc(long)
     */
    @Override
    public List<? extends Network> listNetworksByVpc(long vpcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#canUseForDeploy(com.cloud.network.Network)
     */
    @Override
    public boolean canUseForDeploy(Network network) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getExclusiveGuestNetwork(long)
     */
    @Override
    public Network getExclusiveGuestNetwork(long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#findPhysicalNetworkId(long, java.lang.String, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public long findPhysicalNetworkId(long zoneId, String tag, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNetworkRate(long, java.lang.Long)
     */
    @Override
    public Integer getNetworkRate(long networkId, Long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isVmPartOfNetwork(long, long)
     */
    @Override
    public boolean isVmPartOfNetwork(long vmId, long ntwkId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDefaultPhysicalNetworkByZoneAndTrafficType(long, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public PhysicalNetwork getDefaultPhysicalNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNetwork(long)
     */
    @Override
    public Network getNetwork(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getIp(long)
     */
    @Override
    public IpAddress getIp(long sourceIpAddressId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isNetworkAvailableInDomain(long, long)
     */
    @Override
    public boolean isNetworkAvailableInDomain(long networkId, long domainId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getNicProfile(com.cloud.vm.VirtualMachine, long, java.lang.String)
     */
    @Override
    public NicProfile getNicProfile(VirtualMachine vm, long networkId, String broadcastUri) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getAvailableIps(com.cloud.network.Network, java.lang.String)
     */
    @Override
    public Set<Long> getAvailableIps(Network network, String requestedIp) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getDomainNetworkDomain(long, long)
     */
    @Override
    public String getDomainNetworkDomain(long domainId, long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getIpToServices(java.util.List, boolean, boolean)
     */
    @Override
    public Map<PublicIpAddress, Set<Service>> getIpToServices(List<? extends PublicIpAddress> publicIps, boolean rulesRevoked,
            boolean includingFirewall) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getProviderToIpList(com.cloud.network.Network, java.util.Map)
     */
    @Override
    public Map<Provider, ArrayList<PublicIpAddress>> getProviderToIpList(Network network,
            Map<PublicIpAddress, Set<Service>> ipToServices) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#getSourceNatIpAddressForGuestNetwork(com.cloud.user.Account, com.cloud.network.Network)
     */
    @Override
    public PublicIpAddress getSourceNatIpAddressForGuestNetwork(Account owner, Network guestNetwork) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkModel#isNetworkInlineMode(com.cloud.network.Network)
     */
    @Override
    public boolean isNetworkInlineMode(Network network) {
        // TODO Auto-generated method stub
        return false;
    }

	@Override
	public boolean isIP6AddressAvailableInNetwork(long networkId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIP6AddressAvailableInVlan(long vlanId) {
		// TODO Auto-generated method stub
		return false;
	}

        @Override
        public void checkIp6Parameters(String startIPv6, String endIPv6, String ip6Gateway, String ip6Cidr)
                  throws InvalidParameterValueException {
            // TODO Auto-generated method stub
        }

	@Override
	public void checkRequestedIpAddresses(long networkId, String ip4, String ip6)
			throws InvalidParameterValueException {
		// TODO Auto-generated method stub
	}

	@Override
	public String getStartIpv6Address(long id) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public boolean isProviderEnabledInZone(long zoneId, String provider) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Nic getPlaceholderNicForRouter(Network network, Long podId) {
        // TODO Auto-generated method stub
        return null;
    }

}
