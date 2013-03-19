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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
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
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;

/**
 * NetworkManager manages the network for the different end users.
 * 
 */
public interface NetworkManager  {
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
     * @param IpAddress
     * @return true if it did; false if it didn't
     */
    public boolean disassociatePublicIpAddress(long id, long userId, Account caller);

    List<NetworkVO> setupNetwork(Account owner, NetworkOffering offering, DeploymentPlan plan, String name, String displayText, boolean isDefault)
            throws ConcurrentOperationException;

    List<NetworkVO> setupNetwork(Account owner, NetworkOffering offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean errorIfAlreadySetup, Long domainId,
            ACLType aclType, Boolean subdomainAccess, Long vpcId) throws ConcurrentOperationException;

    void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException, ConcurrentOperationException;

    void prepare(VirtualMachineProfile<? extends VMInstanceVO> profile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException,
            ResourceUnavailableException;

    void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced) throws
			ConcurrentOperationException, ResourceUnavailableException;

    void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm);

    void expungeNics(VirtualMachineProfile<? extends VMInstanceVO> vm);

    List<NicProfile> getNicProfiles(VirtualMachine vm);

    boolean applyRules(List<? extends FirewallRule> rules, FirewallRule.Purpose purpose, NetworkRuleApplier applier, boolean continueOnError) throws ResourceUnavailableException;

    Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException;

    <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest);

    boolean shutdownNetwork(long networkId, ReservationContext context, boolean cleanupElements);

    boolean destroyNetwork(long networkId, ReservationContext context);

    Network createGuestNetwork(long networkOfferingId, String name, String displayText, String gateway, String cidr,
            String vlanId, String networkDomain, Account owner, Long domainId, PhysicalNetwork physicalNetwork,
            long zoneId, ACLType aclType, Boolean subdomainAccess, Long vpcId, String ip6Gateway, String ip6Cidr) 
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

    UserDataServiceProvider getPasswordResetProvider(Network network);

    UserDataServiceProvider getSSHKeyResetProvider(Network network);

    boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException;
    
    boolean applyIpAssociations(Network network, boolean rulesRevoked, boolean continueOnError, List<? extends PublicIpAddress> publicIps) throws ResourceUnavailableException;

    boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    IPAddressVO markIpAsUnavailable(long addrId);

    public String acquireGuestIpAddress(Network network, String requestedIp);

    boolean applyStaticNats(List<? extends StaticNat> staticNats, boolean continueOnError) throws ResourceUnavailableException;

    boolean reallocate(VirtualMachineProfile<? extends VMInstanceVO> vm,
            DataCenterDeployment dest) throws InsufficientCapacityException, ConcurrentOperationException;

    IpAddress assignSystemIp(long networkId, Account owner,
            boolean forElasticLb, boolean forElasticIp)
            throws InsufficientAddressCapacityException;

    boolean handleSystemIpRelease(IpAddress ip);

    void allocateDirectIp(NicProfile nic, DataCenter dc,
            VirtualMachineProfile<? extends VirtualMachine> vm,
            Network network, String requestedIpv4, String requestedIpv6)
            throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException;

    /**
     * @param owner
     * @param guestNetwork
     * @return
     * @throws ConcurrentOperationException 
     * @throws InsufficientAddressCapacityException 
     */
    PublicIp assignSourceNatIpAddressToGuestNetwork(Account owner, Network guestNetwork) throws InsufficientAddressCapacityException, ConcurrentOperationException;


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
     * @param vm
     * @param nic TODO
     */
    void removeNic(VirtualMachineProfile<? extends VMInstanceVO> vm, Nic nic);


    /**
     * @param ipAddrId
     * @param networkId
     * @param releaseOnFailure TODO
     */
    IPAddressVO associateIPToGuestNetwork(long ipAddrId, long networkId, boolean releaseOnFailure) throws ResourceAllocationException, ResourceUnavailableException, 
        InsufficientAddressCapacityException, ConcurrentOperationException;


    /**
     * @param network
     * @param provider
     * @return
     */
    boolean setupDns(Network network, Provider provider);


    /**
     * @param vmProfile
     * @param nic TODO
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    void releaseNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, Nic nic) 
            throws ConcurrentOperationException, ResourceUnavailableException;


    /**
     * @param network
     * @param requested
     * @param context
     * @param vmProfile
     * @param prepare TODO
     * @return
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     * @throws InsufficientCapacityException
     * @throws ResourceUnavailableException
     */
    NicProfile createNicForVm(Network network, NicProfile requested, ReservationContext context, VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean prepare) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException;


    PublicIp assignVpnGatewayIpAddress(long dcId, Account owner, long vpcId) throws InsufficientAddressCapacityException, ConcurrentOperationException;


    /**
     * @param addr
     */
    void markPublicIpAsAllocated(IPAddressVO addr);


    /**
     * @param owner
     * @param guestNtwkId
     * @param vpcId
     * @param dcId
     * @param isSourceNat
     * @return
     * @throws ConcurrentOperationException
     * @throws InsufficientAddressCapacityException
     */
    PublicIp assignDedicateIpAddress(Account owner, Long guestNtwkId, Long vpcId, long dcId, boolean isSourceNat) throws ConcurrentOperationException, InsufficientAddressCapacityException;

    NetworkProfile convertNetworkToNetworkProfile(long networkId);

    /**
     * @return
     */
    int getNetworkLockTimeout();


    boolean cleanupIpResources(long addrId, long userId, Account caller);


    boolean restartNetwork(Long networkId, Account callerAccount,
            User callerUser, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;


    boolean shutdownNetworkElementsAndResources(ReservationContext context,
            boolean b, NetworkVO network);


	void implementNetworkElementsAndResources(DeployDestination dest,
			ReservationContext context, NetworkVO network,
			NetworkOfferingVO findById) throws ConcurrentOperationException, InsufficientAddressCapacityException, ResourceUnavailableException, InsufficientCapacityException;


	IpAddress allocateIp(Account ipOwner, boolean isSystem, Account caller, long callerId,
			DataCenter zone) throws ConcurrentOperationException, ResourceAllocationException, InsufficientAddressCapacityException;


	Map<String, String> finalizeServicesAndProvidersForNetwork(NetworkOffering offering,
			Long physicalNetworkId);


    List<Provider> getProvidersForServiceInNetwork(Network network, Service service);

    StaticNatServiceProvider getStaticNatProviderForNetwork(Network network);
    boolean isNetworkInlineMode(Network network);

    int getRuleCountForIp(Long addressId, FirewallRule.Purpose purpose, FirewallRule.State state);

    LoadBalancingServiceProvider getLoadBalancingProviderForNetwork(Network network);


    boolean isSecondaryIpSetForNic(long nicId);

     public String allocateGuestIP(Account ipOwner, boolean isSystem, long zoneId, Long networkId, String requestedIp)
     throws InsufficientAddressCapacityException;


    List<? extends Nic> listVmNics(Long vmId, Long nicId);
    String allocatePublicIpForGuestNic(Long networkId, DataCenter dc, Pod pod, Account caller, String requestedIp) throws InsufficientAddressCapacityException;
    boolean removeVmSecondaryIpsOfNic(long nicId);

}
