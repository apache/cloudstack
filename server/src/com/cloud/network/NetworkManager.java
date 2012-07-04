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
package com.cloud.network;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.acl.ControlledEntity.ACLType;
import com.cloud.agent.api.to.NicTO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

/**
 * NetworkManager manages the network for the different end users.
 * 
 */
public interface NetworkManager extends NetworkService {
    /**
     * Assigns a new public ip address.
     * 
     * @param dcId
     * @param podId
     *            TODO
     * @param owner
     * @param type
     * @param networkId
     * @param requestedIp
     *            TODO
     * @param allocatedBy
     *            TODO
     * @return
     * @throws InsufficientAddressCapacityException
     */

    PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId, String requestedIp, 
            boolean isSystem) throws InsufficientAddressCapacityException;


    /**
     * Do all of the work of releasing public ip addresses. Note that if this method fails, there can be side effects.
     * 
     * @param userId
     * @param caller
     *            TODO
     * @param ipAddress
     * @return true if it did; false if it didn't
     */
    public boolean disassociatePublicIpAddress(long id, long userId, Account caller);

    /**
     * Lists IP addresses that belong to VirtualNetwork VLANs
     * 
     * @param accountId
     *            - account that the IP address should belong to
     * @param associatedNetworkId
     *            TODO
     * @param sourceNat
     *            - (optional) true if the IP address should be a source NAT address
     * @return - list of IP addresses
     */
    List<IPAddressVO> listPublicIpsAssignedToGuestNtwk(long accountId, long associatedNetworkId, Boolean sourceNat);

    List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isDefault)
            throws ConcurrentOperationException;

    List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean errorIfAlreadySetup, Long domainId,
            ACLType aclType, Boolean subdomainAccess, Long vpcId) throws ConcurrentOperationException;

    List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames);

    void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException, ConcurrentOperationException;

    void prepare(VirtualMachineProfile<? extends VMInstanceVO> profile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException,
            ResourceUnavailableException;

    void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced) throws
			ConcurrentOperationException, ResourceUnavailableException;

    void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm);

    void expungeNics(VirtualMachineProfile<? extends VMInstanceVO> vm);

    List<? extends Nic> getNics(long vmId);

    List<NicProfile> getNicProfiles(VirtualMachine vm);

    String getNextAvailableMacAddressInNetwork(long networkConfigurationId) throws InsufficientAddressCapacityException;

    boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException;

    public boolean validateRule(FirewallRule rule);

    List<? extends RemoteAccessVPNServiceProvider> getRemoteAccessVpnElements();
    
    List<? extends Site2SiteVpnServiceProvider> getSite2SiteVpnElements();

    PublicIpAddress getPublicIpAddress(long ipAddressId);

    List<? extends Vlan> listPodVlans(long podId);

    Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException;

    List<NetworkVO> listNetworksUsedByVm(long vmId, boolean isSystem);

    <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest);

    boolean shutdownNetwork(long networkId, ReservationContext context, boolean cleanupElements);

    boolean destroyNetwork(long networkId, ReservationContext context);

    Network createGuestNetwork(long networkOfferingId, String name, String displayText, String gateway, String cidr,
            String vlanId, String networkDomain, Account owner, Long domainId, PhysicalNetwork physicalNetwork,
            long zoneId, ACLType aclType, Boolean subdomainAccess, Long vpcId) 
                    throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException;

    /**
     * @throws ResourceAllocationException TODO
     * @throws InsufficientCapacityException
     *             Associates an ip address list to an account. The list of ip addresses are all addresses associated
     *             with the
     *             given vlan id.
     * @param userId
     * @param accountId
     * @param zoneId
     * @param vlanId
     * @throws InsufficientAddressCapacityException
     * @throws
     */
    boolean associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId, Network guestNetwork) throws InsufficientCapacityException, ConcurrentOperationException,
            ResourceUnavailableException, ResourceAllocationException;

    Nic getNicInNetwork(long vmId, long networkId);

    List<? extends Nic> getNicsForTraffic(long vmId, TrafficType type);

    Network getDefaultNetworkForVm(long vmId);

    Nic getDefaultNic(long vmId);

    List<? extends UserDataServiceProvider> getPasswordResetElements();

    boolean networkIsConfiguredForExternalNetworking(long zoneId, long networkId);

    Map<Capability, String> getNetworkServiceCapabilities(long networkId, Service service);

    boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException;

    boolean areServicesSupportedByNetworkOffering(long networkOfferingId, Service... services);

    NetworkVO getNetworkWithSecurityGroupEnabled(Long zoneId);

    boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    String getIpOfNetworkElementInVirtualNetwork(long accountId, long dataCenterId);

    List<NetworkVO> listNetworksForAccount(long accountId, long zoneId, Network.GuestType type);
    
    List<NetworkVO> listAllNetworksInAllZonesByType(Network.GuestType type);

    IPAddressVO markIpAsUnavailable(long addrId);

    public String acquireGuestIpAddress(Network network, String requestedIp);

    String getGlobalGuestDomainSuffix();

    String getStartIpAddress(long networkId);

    boolean applyStaticNats(List<? extends StaticNat> staticNats, boolean continueOnError) throws ResourceUnavailableException;

    String getIpInNetwork(long vmId, long networkId);

    String getIpInNetworkIncludingRemoved(long vmId, long networkId);

    Long getPodIdForVlan(long vlanDbId);

    List<Long> listNetworkOfferingsForUpgrade(long networkId);

    PhysicalNetwork translateZoneIdToPhysicalNetwork(long zoneId);

    boolean isSecurityGroupSupportedInNetwork(Network network);

    boolean isProviderSupportServiceInNetwork(long networkId, Service service, Provider provider);

    boolean isProviderEnabledInPhysicalNetwork(long physicalNetowrkId, String providerName);

    String getNetworkTag(HypervisorType hType, Network network);

    List<Service> getElementServices(Provider provider);

    boolean canElementEnableIndividualServices(Provider provider);

    boolean areServicesSupportedInNetwork(long networkId, Service... services);

    boolean isNetworkSystem(Network network);

    boolean reallocate(VirtualMachineProfile<? extends VMInstanceVO> vm,
            DataCenterDeployment dest) throws InsufficientCapacityException, ConcurrentOperationException;

    Map<Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service);

    Long getPhysicalNetworkId(Network network);

    boolean getAllowSubdomainAccessGlobal();

    boolean isProviderForNetwork(Provider provider, long networkId);

    boolean isProviderForNetworkOffering(Provider provider, long networkOfferingId);

    void canProviderSupportServices(Map<Provider, Set<Service>> providersMap);

    List<PhysicalNetworkSetupInfo> getPhysicalNetworkInfo(long dcId,
            HypervisorType hypervisorType);

    boolean canAddDefaultSecurityGroup();

    List<Service> listNetworkOfferingServices(long networkOfferingId);

    boolean areServicesEnabledInZone(long zoneId, NetworkOffering offering, List<Service> services);

    public Map<PublicIp, Set<Service>> getIpToServices(List<PublicIp> publicIps, boolean rulesRevoked, boolean includingFirewall);

    public Map<Provider, ArrayList<PublicIp>> getProviderToIpList(Network network, Map<PublicIp, Set<Service>> ipToServices);

    public boolean checkIpForService(IPAddressVO ip, Service service);

    void checkVirtualNetworkCidrOverlap(Long zoneId, String cidr);

    void checkCapabilityForProvider(Set<Provider> providers, Service service,
            Capability cap, String capValue);

    Provider getDefaultUniqueProviderForService(String serviceName);

    IpAddress assignSystemIp(long networkId, Account owner,
            boolean forElasticLb, boolean forElasticIp)
            throws InsufficientAddressCapacityException;

    boolean handleSystemIpRelease(IpAddress ip);

    void checkNetworkPermissions(Account owner, Network network);

    void allocateDirectIp(NicProfile nic, DataCenter dc,
            VirtualMachineProfile<? extends VirtualMachine> vm,
            Network network, String requestedIp)
            throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException;

    String getDefaultManagementTrafficLabel(long zoneId, HypervisorType hypervisorType);

    String getDefaultStorageTrafficLabel(long zoneId, HypervisorType hypervisorType);

    String getDefaultPublicTrafficLabel(long dcId, HypervisorType vmware);

    String getDefaultGuestTrafficLabel(long dcId, HypervisorType vmware);

    /**
     * @param providerName
     * @return
     */
    NetworkElement getElementImplementingProvider(String providerName);

    /**
     * @param owner
     * @param guestNetwork
     * @return
     * @throws ConcurrentOperationException 
     * @throws InsufficientAddressCapacityException 
     */
    PublicIp assignSourceNatIpAddressToGuestNetwork(Account owner, Network guestNetwork) throws InsufficientAddressCapacityException, ConcurrentOperationException;


    /**
     * @param owner
     * @param vpc
     * @return
     * @throws ConcurrentOperationException 
     * @throws InsufficientAddressCapacityException 
     */
    PublicIp assignSourceNatIpAddressToVpc(Account owner, Vpc vpc) throws InsufficientAddressCapacityException, ConcurrentOperationException;


    /**
     * @param accountId
     * @param zoneId
     * @return
     */
    String getAccountNetworkDomain(long accountId, long zoneId);


    /**
     * @return
     */
    String getDefaultNetworkDomain();


    /**
     * @param networkId
     * @return
     */
    List<Provider> getNtwkOffDistinctProviders(long networkId);


    /**
     * @param requested
     * @param network
     * @param isDefaultNic
     * @param deviceId
     * @param vm
     * @return
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     */
    Pair<NicProfile,Integer> allocateNic(NicProfile requested, Network network, Boolean isDefaultNic, int deviceId, 
            VirtualMachineProfile<? extends VMInstanceVO> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException;


    /**
     * @param vmProfile
     * @param dest
     * @param context
     * @param nicId
     * @param network
     * @return
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     * @throws InsufficientCapacityException
     * @throws ResourceUnavailableException
     */
    NicProfile prepareNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest, 
            ReservationContext context, long nicId, NetworkVO network) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException;


    /**
     * @param vmProfile
     * @param network
     * @return TODO
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    NicProfile releaseNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, NetworkVO network) 
            throws ConcurrentOperationException, ResourceUnavailableException;


    /**
     * @param vm
     * @param network
     */
    void removeNic(VirtualMachineProfile<? extends VMInstanceVO> vm, Network network);


    /**
     * @param accountId
     * @param dcId
     * @param sourceNat
     * @return
     */
    List<IPAddressVO> listPublicIpsAssignedToAccount(long accountId, long dcId, Boolean sourceNat);


    /**
     * @param ipAddrId
     * @param networkId
     */
    IPAddressVO associateIPToGuestNetwork(long ipAddrId, long networkId) throws ResourceAllocationException, ResourceUnavailableException, 
        InsufficientAddressCapacityException, ConcurrentOperationException;


    /**
     * @param ipId
     */
    void unassignIPFromVpcNetwork(long ipId);


    /**
     * @param vm
     * @param networkId
     * @return
     */
    NicProfile getNicProfile(VirtualMachine vm, long networkId);


    /**
     * @param network
     * @param provider
     * @return
     */
    boolean setupDns(Network network, Provider provider);


    /**
     * @param vmProfile
     * @param network
     * @param broadcastUri
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    NicProfile releaseNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, NetworkVO network, URI broadcastUri) 
            throws ConcurrentOperationException, ResourceUnavailableException;


    /**
     * @param zoneId
     * @param trafficType
     * @return
     */
    List<? extends PhysicalNetwork> getPhysicalNtwksSupportingTrafficType(long zoneId, TrafficType trafficType);


    /**
     * @param guestNic
     * @return
     */
    boolean isPrivateGateway(Nic guestNic);
    

}
