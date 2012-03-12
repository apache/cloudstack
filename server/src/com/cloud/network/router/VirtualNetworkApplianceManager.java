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
import com.cloud.network.Network.Provider;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
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
     * @param routers TODO
     * 
     */
    boolean savePasswordToRouter(Network network, NicProfile nic, VirtualMachineProfile<UserVm> profile, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;
    
    boolean getRouterStatistics(long vmId, Map<String, long[]> netStats, Map<String, long[]> diskStats);
    
    List<DomainRouterVO> getRouters(long accountId, long zoneId);
	
	List<DomainRouterVO> deployVirtualRouter(Network guestNetwork, DeployDestination dest, Account owner, Map<VirtualMachineProfile.Param, Object> params, boolean isRedundant) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException;
	
    boolean startRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;
	
	boolean deleteRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;
    
    boolean associateIP (Network network, final List<? extends PublicIpAddress> ipAddress, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;
    
    boolean applyFirewallRules(Network network, final List<? extends FirewallRule> rules, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;
    
    List<VirtualRouter> getRoutersForNetwork(long networkId);

    String[] applyVpnUsers(Network network, List<? extends VpnUser> users, List<DomainRouterVO> routers) throws ResourceUnavailableException;
    
    VirtualRouter stop(VirtualRouter router, boolean forced, User callingUser, Account callingAccount) throws ConcurrentOperationException, ResourceUnavailableException;

    String getDnsBasicZoneUpdate();
    
    boolean applyStaticNats(Network network, final List<? extends StaticNat> rules, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;
    
	boolean applyDhcpEntry(Network config, NicProfile nic, VirtualMachineProfile<UserVm> vm, DeployDestination dest, List<DomainRouterVO> routers) throws ResourceUnavailableException;
	
	boolean applyUserData(Network config, NicProfile nic, VirtualMachineProfile<UserVm> vm, DeployDestination dest, List<DomainRouterVO> routers) throws ResourceUnavailableException;
    
    long getDefaultVirtualRouterServiceOfferingId();
}
