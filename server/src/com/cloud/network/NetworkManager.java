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

import com.cloud.agent.api.to.NicTO;
import com.cloud.api.commands.AssignToLoadBalancerRuleCmd;
import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateIPForwardingRuleCmd;
import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.api.commands.DeleteIPForwardingRuleCmd;
import com.cloud.api.commands.DeleteLoadBalancerRuleCmd;
import com.cloud.api.commands.DeletePortForwardingServiceRuleCmd;
import com.cloud.api.commands.DisassociateIPAddrCmd;
import com.cloud.api.commands.ListPortForwardingRulesCmd;
import com.cloud.api.commands.RebootRouterCmd;
import com.cloud.api.commands.RemoveFromLoadBalancerRuleCmd;
import com.cloud.api.commands.StartRouterCmd;
import com.cloud.api.commands.StopRouterCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.api.commands.UpgradeRouterCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;

/**
 * NetworkManager manages the network for the different end users.
 *
 */
public interface NetworkManager extends Manager {
    public static final int DEFAULT_ROUTER_VM_RAMSIZE = 128;            // 128M
    public static final boolean USE_POD_VLAN = false;
    /**
     * create the router.
     * 
     * @param accountId account Id the router belongs to.
     * @param ipAddress public ip address the router should use to access the internet.
     * @param dcId data center id the router should live in.
     * @param domain domain name of this network.
     * @param offering service offering associated with this request
     * @return DomainRouterVO if created.  null if not.
     */
    DomainRouterVO createRouter(long accountId, String ipAddress, long dcId, String domain, ServiceOfferingVO offering, long startEventId) throws ConcurrentOperationException;

    /**
     * create a DHCP server/user data server for directly connected VMs
     * @param userId the user id of the user creating the router.
     * @param accountId the account id of the user creating the router.
     * @param dcId data center id the router should live in.
     * @param domain domain name of this network.
     * @return DomainRouterVO if created.  null if not.
     */
	DomainRouterVO createDhcpServerForDirectlyAttachedGuests(long userId, long accountId, DataCenterVO dc, HostPodVO pod, Long candidateHost, VlanVO vlan) throws ConcurrentOperationException;
    
    /**
    /*
     * Send ssh public/private key pair to specified host
     * @param hostId
     * @param pubKey
     * @param prvKey
     */
    boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey);

    /**
     * save a vm password on the router.
     * 
	 * @param routerId the ID of the router to save the password to
	 * @param vmIpAddress the IP address of the User VM that will use the password
	 * @param password the password to save to the router
     */
    boolean savePasswordToRouter(long routerId, String vmIpAddress, String password);
    
    DomainRouterVO startRouter(long routerId, long eventId);
    
    /**
     * Starts domain router
     * @param cmd the command specifying router's id
     * @return DomainRouter object
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    DomainRouterVO startRouter(StartRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
    boolean releaseRouter(long routerId);
    
    boolean destroyRouter(long routerId);
    
    boolean stopRouter(long routerId, long eventId);
    
    /**
     * Stops domain router
     * @param cmd the command specifying router's id
     * @return router if successful, null otherwise
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    DomainRouterVO stopRouter(StopRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
    boolean getRouterStatistics(long vmId, Map<String, long[]> netStats, Map<String, long[]> diskStats);

    boolean rebootRouter(long routerId, long eventId);
    
    /**
     * Reboots domain router
     * @param cmd the command specifying router's id
     * @return success or failure
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    boolean rebootRouter(RebootRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    /**
     * @param hostId get all of the virtual machine routers on a host.
     * @return collection of VirtualMachineRouter
     */
    List<? extends DomainRouter> getRouters(long hostId);
    
    /**
     * @param routerId id of the router
     * @return VirtualMachineRouter
     */
    DomainRouterVO getRouter(long routerId);
    
    /**
     * Do all of the work of releasing public ip addresses.  Note that
     * if this method fails, there can be side effects.
     * @param userId
     * @param ipAddress
     * @return true if it did; false if it didn't
     */
    public boolean releasePublicIpAddress(long userId, String ipAddress);
    
    /**
     * Find or create the source nat ip address a user uses within the
     * data center.
     * 
     * @param account account
     * @param dc data center
     * @param domain domain used for user's network.
     * @param so service offering associated with this request
     * @return public ip address.
     */
    public String assignSourceNatIpAddress(Account account, DataCenterVO dc, String domain, ServiceOfferingVO so, long startEventId, HypervisorType hyperType) throws ResourceAllocationException;
    
    /**
     * @param fwRules list of rules to be updated
     * @param router  router where the rules have to be updated
     * @return list of rules successfully updated
     */
    public List<FirewallRuleVO> updatePortForwardingRules(List<FirewallRuleVO> fwRules, DomainRouterVO router, Long hostId);

    /**
     * @param fwRules list of rules to be updated
     * @param router  router where the rules have to be updated
     * @return success
     */
    public boolean updateLoadBalancerRules(List<FirewallRuleVO> fwRules, DomainRouterVO router, Long hostId);
    
    /**
     * @param publicIpAddress public ip address associated with the fwRules
     * @param fwRules list of rules to be updated
     * @param router router where the rules have to be updated
     * @return list of rules successfully updated
     */
    public List<FirewallRuleVO> updateFirewallRules(String publicIpAddress, List<FirewallRuleVO> fwRules, DomainRouterVO router);

    /**
     * Create a port forwarding rule from the given ipAddress/port to the given virtual machine/port.
     * @param cmd the command specifying the ip address, public port, protocol, private port, and virtual machine id.
     * @return the newly created FirewallRuleVO if successful, null otherwise.
     */
    public FirewallRuleVO createPortForwardingRule(CreateIPForwardingRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, NetworkRuleConflictException;

    /**
     * List port forwarding rules assigned to an ip address
     * @param cmd the command object holding the criteria for listing port forwarding rules (the ipAddress)
     * @return list of port forwarding rules on the given address, empty list if no rules exist
     */
    public List<FirewallRuleVO> listPortForwardingRules(ListPortForwardingRulesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

    /**
     * Create a load balancer rule from the given ipAddress/port to the given private port
     * @param cmd the command specifying the ip address, public port, protocol, private port, and algorithm
     * @return the newly created LoadBalancerVO if successful, null otherwise
     */
    public LoadBalancerVO createLoadBalancerRule(CreateLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

    /**
     * Associates or disassociates a list of public IP address for a router.
     * @param router router object to send the association to
     * @param ipAddrList list of public IP addresses
     * @param add true if associate, false if disassociate
     * @param vmId
     * @return
     */
    boolean associateIP(DomainRouterVO router, List<String> ipAddrList, boolean add, long vmId);
    
    /**
     * Associates a public IP address for a router.
     * @param cmd - the command specifying ipAddress
     * @return ip address object
     * @throws ResourceAllocationException, InsufficientCapacityException, InternalErrorException, InvalidParameterValueException, PermissionDeniedException
     */
    IPAddressVO associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, InternalErrorException, InvalidParameterValueException, PermissionDeniedException;    
    
    boolean updateFirewallRule(FirewallRuleVO fwRule, String oldPrivateIP, String oldPrivatePort);

    /**
     * Assign a virtual machine, or list of virtual machines, to a load balancer.
     */
    void assignToLoadBalancer(AssignToLoadBalancerRuleCmd cmd)  throws NetworkRuleConflictException,
                                                                       InternalErrorException,
                                                                       PermissionDeniedException,
                                                                       InvalidParameterValueException;

    public boolean removeFromLoadBalancer(RemoveFromLoadBalancerRuleCmd cmd) throws InvalidParameterValueException;
    
    public boolean deleteLoadBalancerRule(DeleteLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    public LoadBalancerVO updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
    /**
     * Add a DHCP entry on the domr dhcp server
     * @param routerHostId - the host id of the domr
     * @param routerIp - the private ip address of the domr
     * @param vmName - the name of the VM (e.g., i-10-TEST)
     * @param vmMac  - the mac address of the eth0 interface of the VM
     * @param vmIp   - the ip address to hand out.
     * @return success or failure
     */
    public boolean addDhcpEntry(long routerHostId, String routerIp, String vmName, String vmMac, String vmIp);
    
    /**
     * Adds a virtual machine into the guest network.
     *   1. Starts the domR
     *   2. Sets the dhcp Entry on the domR
     *   3. Sets the domR
     * 
     * @param vm user vm to add to the guest network
     * @param password password for this vm.  Can be null
     * @return DomainRouterVO if everything is successful.  null if not.
     * 
     * @throws ConcurrentOperationException if multiple starts are being attempted.
     */
	public DomainRouterVO addVirtualMachineToGuestNetwork(UserVmVO vm, String password, long startEventId) throws ConcurrentOperationException;	

    String createZoneVlan(DomainRouterVO router);
    
    /**
     * Lists IP addresses that belong to VirtualNetwork VLANs
     * @param accountId - account that the IP address should belong to
     * @param dcId - zone that the IP address should belong to
     * @param sourceNat - (optional) true if the IP address should be a source NAT address
     * @return - list of IP addresses
     */
    List<IPAddressVO> listPublicIpAddressesInVirtualNetwork(long accountId, long dcId, Boolean sourceNat);	
    
    public boolean deleteNetworkRuleConfig(DeletePortForwardingServiceRuleCmd cmd) throws PermissionDeniedException;
	
    public boolean disassociateIpAddress(DisassociateIPAddrCmd cmd) throws PermissionDeniedException;
    
    public boolean deleteIpForwardingRule(DeleteIPForwardingRuleCmd cmd) throws PermissionDeniedException, InvalidParameterValueException;

    List<NetworkConfigurationVO> setupNetworkConfiguration(Account owner, NetworkOfferingVO offering, DeploymentPlan plan);
    List<NetworkConfigurationVO> setupNetworkConfiguration(Account owner, NetworkOfferingVO offering, NetworkConfiguration predefined, DeploymentPlan plan);
    
    List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames);
    
    List<NicProfile> allocate(VirtualMachineProfile vm, List<Pair<NetworkConfigurationVO, NicProfile>> networks) throws InsufficientCapacityException;

    NicTO[] prepare(VirtualMachineProfile profile, DeployDestination dest, Account user) throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapcityException;
    void release(VirtualMachineProfile vmProfile);
    
    <K extends VMInstanceVO> List<NicVO> getNics(K vm);
	boolean upgradeRouter(UpgradeRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
	
    List<AccountVO> getAccountsUsingNetworkConfiguration(long configurationId);    
    AccountVO getNetworkConfigurationOwner(long configurationId);
    
    List<NetworkConfigurationVO> getNetworkConfigurationsforOffering(long offeringId, long dataCenterId, long accountId);
}
