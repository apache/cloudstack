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
package com.cloud.network;

import java.util.List;
import java.util.Map;

import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
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
     * @param requestedIp TODO
     * @return
     * @throws InsufficientAddressCapacityException
     */
    PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId, String requestedIp) throws InsufficientAddressCapacityException;

    /**
     * assigns a source nat ip address to an account within a network.
     * 
     * @param owner
     * @param network
     * @param callerId
     * @return
     * @throws ConcurrentOperationException
     * @throws InsufficientAddressCapacityException
     */
    PublicIp assignSourceNatIpAddress(Account owner, Network network, long callerId) throws ConcurrentOperationException, InsufficientAddressCapacityException;

    /**
     * Do all of the work of releasing public ip addresses. Note that if this method fails, there can be side effects.
     * 
     * @param userId
     * @param caller
     *            TODO
     * @param ipAddress
     * @return true if it did; false if it didn't
     */
    public boolean releasePublicIpAddress(long id, long userId, Account caller);

    /**
     * Lists IP addresses that belong to VirtualNetwork VLANs
     * 
     * @param accountId
     *            - account that the IP address should belong to
     * @param dcId
     *            - zone that the IP address should belong to
     * @param sourceNat
     *            - (optional) true if the IP address should be a source NAT address
     * @param associatedNetworkId
     *            TODO
     * @return - list of IP addresses
     */
    List<IPAddressVO> listPublicIpAddressesInVirtualNetwork(long accountId, long dcId, Boolean sourceNat, Long associatedNetworkId);

    List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isDefault, boolean isShared)
            throws ConcurrentOperationException;

    List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean isDefault, boolean errorIfAlreadySetup,
            Long domainId, boolean isShared) throws ConcurrentOperationException;

    List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames);

    void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException, ConcurrentOperationException;

    void prepare(VirtualMachineProfile<? extends VMInstanceVO> profile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException,
            ResourceUnavailableException;

    void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced);

    void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm);

    void expungeNics(VirtualMachineProfile<? extends VMInstanceVO> vm);

    List<? extends Nic> getNics(long vmId);

    List<NicProfile> getNicProfiles(VirtualMachine vm);

    String getNextAvailableMacAddressInNetwork(long networkConfigurationId) throws InsufficientAddressCapacityException;

    boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException;

    List<? extends RemoteAccessVPNServiceProvider> getRemoteAccessVpnElements();

    PublicIpAddress getPublicIpAddress(long ipAddressId);

    List<? extends Vlan> listPodVlans(long podId);

    Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException;

    List<NetworkVO> listNetworksUsedByVm(long vmId, boolean isSystem);

    <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest);

    void shutdownNetwork(long networkId, ReservationContext context, boolean cleanupElements);

    boolean destroyNetwork(long networkId, ReservationContext context);

    Network createNetwork(long networkOfferingId, String name, String displayText, Boolean isDefault, String gateway, String cidr, String vlanId, String networkDomain, Account owner, boolean isSecurityGroupEnabled,
            Long domainId, Boolean isShared, PhysicalNetwork physicalNetwork, long zoneId) throws ConcurrentOperationException, InsufficientCapacityException;

    /**
     * @throws InsufficientCapacityException
     *             Associates an ip address list to an account. The list of ip addresses are all addresses associated with the
     *             given vlan id.
     * @param userId
     * @param accountId
     * @param zoneId
     * @param vlanId
     * @throws InsufficientAddressCapacityException
     * @throws
     */
    boolean associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId, Network networkToAssociateWith) throws InsufficientCapacityException, ConcurrentOperationException,
            ResourceUnavailableException;

    Nic getNicInNetwork(long vmId, long networkId);

    Nic getNicInNetworkIncludingRemoved(long vmId, long networkId);

    List<? extends Nic> getNicsForTraffic(long vmId, TrafficType type);

    Network getDefaultNetworkForVm(long vmId);

    Nic getDefaultNic(long vmId);

    List<? extends UserDataServiceProvider> getPasswordResetElements();
    
    @Deprecated
    boolean zoneIsConfiguredForExternalNetworking(long zoneId);
    
    boolean networkIsConfiguredForExternalNetworking(long zoneId, long networkOfferingId);

    Map<Capability, String> getServiceCapabilities(Long networkOfferingId, Service service);

    boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException;

    boolean isServiceSupportedByNetworkOffering(long networkOfferingId, Network.Service service);

    NetworkVO getNetworkWithSecurityGroupEnabled(Long zoneId);

    boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    String getIpOfNetworkElementInVirtualNetwork(long accountId, long dataCenterId);

    List<NetworkVO> listNetworksForAccount(long accountId, long zoneId, Network.GuestType type, Boolean isDefault);

    IPAddressVO markIpAsUnavailable(long addrId);
    
    public String acquireGuestIpAddress(Network network, String requestedIp);

    String getGlobalGuestDomainSuffix();
    
    String getStartIpAddress(long networkId);

    boolean applyStaticNats(List<? extends StaticNat> staticNats, boolean continueOnError) throws ResourceUnavailableException;
    
    String getIpInNetwork(long vmId, long networkId);

    String getIpInNetworkIncludingRemoved(long vmId, long networkId);
    
    Long getPodIdForVlan(long vlanDbId);
    
    boolean isProviderSupported(long networkOfferingId, Service service, Provider provider);
    
    List<Long> listNetworkOfferingsForUpgrade(long networkId);

    PhysicalNetwork translateZoneIdToPhysicalNetwork(long zoneId);

    boolean isSecurityGroupSupportedInNetwork(Network network);
    
    boolean isProviderEnabled(PhysicalNetworkServiceProvider provider);
    
    boolean isProviderAvailable(long physicalNetowrkId, String providerName);
    
    boolean isServiceEnabled(Long physicalNetworkId, long networkOfferingId, Service service);

    List<String> getNetworkTags(HypervisorType hType, Network network);

    List<Service> getElementServices(Provider provider);

    boolean canElementEnableIndividualServices(Provider provider);

    NetworkOfferingVO getExclusiveGuestNetworkOffering();
}
