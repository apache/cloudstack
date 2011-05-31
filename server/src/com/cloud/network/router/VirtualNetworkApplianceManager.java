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

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.network.VpnUser;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;
import com.cloud.user.User;
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
public interface VirtualNetworkApplianceManager extends Manager, VirtualNetworkApplianceService{
    public static final int DEFAULT_ROUTER_VM_RAMSIZE = 128;            // 128M
    public static final int DEFAULT_ROUTER_CPU_MHZ = 500;            	// 500 MHz
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
     */
    boolean savePasswordToRouter(Network network, NicProfile nic, VirtualMachineProfile<UserVm> profile) throws ResourceUnavailableException;
    
    boolean destroyRouter(long routerId) throws ResourceUnavailableException, ConcurrentOperationException;
    
    boolean getRouterStatistics(long vmId, Map<String, long[]> netStats, Map<String, long[]> diskStats);
    
    List<DomainRouterVO> getRouters(long accountId, long zoneId);
	
	List<DomainRouterVO> deployVirtualRouter(Network guestNetwork, DeployDestination dest, Account owner, Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException;
	
	List<DomainRouterVO> deployDhcp(Network guestNetwork, DeployDestination dest, Account owner, Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException;
	
	boolean startRemoteAccessVpn(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException;
	
	boolean deleteRemoteAccessVpn(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException;
	
	List<VirtualRouter> addVirtualMachineIntoNetwork(Network config, NicProfile nic, VirtualMachineProfile<UserVm> vm, DeployDestination dest, ReservationContext context, Boolean startDhcp) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException;
    
    boolean associateIP (Network network, List<? extends PublicIpAddress> ipAddress) throws ResourceUnavailableException;
    
    boolean applyFirewallRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException;
    
    String[] applyVpnUsers(Network network, List<? extends VpnUser> users) throws ResourceUnavailableException;
    
    List<VirtualRouter> getRoutersForNetwork(long networkId);
    
    VirtualRouter stop(VirtualRouter router, boolean forced, User callingUser, Account callingAccount) throws ConcurrentOperationException, ResourceUnavailableException;
}
