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
package com.cloud.network.router;

import java.util.List;
import java.util.Map;

import com.cloud.api.commands.UpgradeRouterCmd;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.network.VpnUserVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

/**
 * NetworkManager manages the network for the different end users.
 *
 */
public interface DomainRouterManager extends Manager {
    public static final int DEFAULT_ROUTER_VM_RAMSIZE = 128;            // 128M
    public static final boolean USE_POD_VLAN = false;
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
    
    boolean destroyRouter(long routerId);
    
    boolean stopRouter(long routerId, long eventId);
    
    boolean getRouterStatistics(long vmId, Map<String, long[]> netStats, Map<String, long[]> diskStats);

    boolean rebootRouter(long routerId, long eventId);
    
    /**
     * @param hostId get all of the virtual machine routers on a host.
     * @return collection of VirtualMachineRouter
     */
    List<? extends VirtualRouter> getRouters(long hostId);
    
    /**
     * @param routerId id of the router
     * @return VirtualMachineRouter
     */
    DomainRouterVO getRouter(long routerId);
    
    VirtualRouter upgradeRouter(UpgradeRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
	
	DomainRouterVO getRouter(long accountId, long zoneId);
	DomainRouterVO getRouter(String publicIpAddress);
	
	DomainRouterVO deployVirtualRouter(Network guestConfig, DeployDestination dest, Account owner) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException;
	
	DomainRouterVO deployDhcp(Network guestConfig, DeployDestination dest, Account owner) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException;
	
	RemoteAccessVpnVO startRemoteAccessVpn(RemoteAccessVpnVO vpnVO) throws ResourceUnavailableException;
	
	boolean addRemoveVpnUsers(RemoteAccessVpnVO vpnVO, List<VpnUserVO> addUsers, List<VpnUserVO> removeUsers);

	boolean deleteRemoteAccessVpn(RemoteAccessVpnVO vpnVO);
	
	DomainRouterVO addVirtualMachineIntoNetwork(Network config, NicProfile nic, VirtualMachineProfile<UserVm> vm, DeployDestination dest, ReservationContext context, Boolean startDhcp) throws ConcurrentOperationException, InsufficientNetworkCapacityException, ResourceUnavailableException;
    
    boolean associateIP (Network network, List<? extends PublicIpAddress> ipAddress);
    
    boolean applyLBRules(Network network, List<? extends FirewallRule> rules);
    boolean applyPortForwardingRules(Network network, List<? extends FirewallRule> rules);
    
}
