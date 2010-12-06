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

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
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
    public static final boolean USE_POD_VLAN = false;

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
     * Do all of the work of releasing public ip addresses.  Note that
     * if this method fails, there can be side effects.
     * @param userId
     * @param ipAddress
     * @return true if it did; false if it didn't
     */
    public boolean releasePublicIpAddress(String ipAddress, long ownerId, long userId);
    
    /**
     * Associates or disassociates a list of public IP address for a router.
     * @param router router object to send the association to
     * @param ipAddrList list of public IP addresses
     * @param add true if associate, false if disassociate
     * @param vmId
     * @return
     */
    boolean associateIP(DomainRouterVO router, List<String> ipAddrList, boolean add, long vmId) throws ConcurrentOperationException;
    
    /**
     * Associates or disassociates a single IP address for a router.
     * @param router router object to send the association to
     * @param ipAddress  public IP addresses
     * @param add true if associate, false if disassociate
     * @return
     */
    boolean associateIP(DomainRouterVO router, String ipAddress, boolean add, long vmId) throws ResourceAllocationException;
    
    
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
     * Lists IP addresses that belong to VirtualNetwork VLANs
     * @param accountId - account that the IP address should belong to
     * @param dcId - zone that the IP address should belong to
     * @param sourceNat - (optional) true if the IP address should be a source NAT address
     * @return - list of IP addresses
     */
    List<IPAddressVO> listPublicIpAddressesInVirtualNetwork(long accountId, long dcId, Boolean sourceNat);	
    
    List<NetworkVO> setupNetworkConfiguration(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isShared);
    List<NetworkVO> setupNetworkConfiguration(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean isShared);
    
    List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames);
    
    void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException, ConcurrentOperationException;

    void prepare(VirtualMachineProfile<? extends VMInstanceVO> profile, DeployDestination dest, ReservationContext context) throws InsufficientNetworkCapacityException, ConcurrentOperationException, ResourceUnavailableException;
    void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile);
    
    List<? extends Nic> getNics (VirtualMachine vm);
	
    List<AccountVO> getAccountsUsingNetworkConfiguration(long configurationId);    
    AccountVO getNetworkConfigurationOwner(long configurationId);
    
    List<NetworkVO> getNetworkConfigurationsforOffering(long offeringId, long dataCenterId, long accountId);

    List<NetworkVO> setupNetworkConfiguration(Account owner, ServiceOfferingVO offering, DeploymentPlan plan);
    
	Network getNetwork(long id);
	String getNextAvailableMacAddressInNetwork(long networkConfigurationId) throws InsufficientAddressCapacityException;

	boolean applyRules(Ip ip, List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException;
}
