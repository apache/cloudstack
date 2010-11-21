/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 * 
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.rules;

import java.util.List;

import com.cloud.api.commands.AddVpnUserCmd;
import com.cloud.api.commands.AssignToLoadBalancerRuleCmd;
import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.api.commands.CreatePortForwardingRuleCmd;
import com.cloud.api.commands.CreateRemoteAccessVpnCmd;
import com.cloud.api.commands.DeleteLoadBalancerRuleCmd;
import com.cloud.api.commands.DeleteRemoteAccessVpnCmd;
import com.cloud.api.commands.ListPortForwardingRulesCmd;
import com.cloud.api.commands.RemoveFromLoadBalancerRuleCmd;
import com.cloud.api.commands.RemoveVpnUserCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.network.VpnUserVO;
import com.cloud.vm.DomainRouterVO;

public interface RulesManager {
    
    /**
     * @param fwRules list of rules to be updated
     * @param router  router where the rules have to be updated
     * @return list of rules successfully updated
     */
    List<FirewallRuleVO> updatePortForwardingRules(List<FirewallRuleVO> fwRules, DomainRouterVO router, Long hostId);

    /**
     * @param fwRules list of rules to be updated
     * @param router  router where the rules have to be updated
     * @return success
     */
    boolean updateLoadBalancerRules(List<FirewallRuleVO> fwRules, DomainRouterVO router, Long hostId);
    
    /**
     * @param publicIpAddress ip address associated with the fwRules
     * @param fwRules list of rules to be updated
     * @param router router where the rules have to be updated
     * @return list of rules successfully updated
     */
    List<FirewallRuleVO> updateFirewallRules(String publicIpAddress, List<FirewallRuleVO> fwRules, DomainRouterVO router);

    /**
     * Create a port forwarding rule from the given ipAddress/port to the given virtual machine/port.
     * @param cmd the command specifying the ip address, port, protocol, private port, and virtual machine id.
     * @return the newly created FirewallRuleVO if successful, null otherwise.
     */
    FirewallRuleVO createPortForwardingRule(CreatePortForwardingRuleCmd cmd) throws NetworkRuleConflictException;

    /**
     * List port forwarding rules assigned to an ip address
     * @param cmd the command object holding the criteria for listing port forwarding rules (the ipAddress)
     * @return list of port forwarding rules on the given address, empty list if no rules exist
     */
    List<FirewallRuleVO> listPortForwardingRules(ListPortForwardingRulesCmd cmd);

    /**
     * Create a load balancer rule from the given ipAddress/port to the given private port
     * @param cmd the command specifying the ip address, port, protocol, private port, and algorithm
     * @return the newly created LoadBalancerVO if successful, null otherwise
     */
    LoadBalancerVO createLoadBalancerRule(CreateLoadBalancerRuleCmd cmd);

    boolean updateFirewallRule(FirewallRuleVO fwRule, String oldPrivateIP, String oldPrivatePort);

    /**
     * Assign a virtual machine, or list of virtual machines, to a load balancer.
     */
    boolean assignToLoadBalancer(AssignToLoadBalancerRuleCmd cmd)  throws NetworkRuleConflictException;

    boolean removeFromLoadBalancer(RemoveFromLoadBalancerRuleCmd cmd);
    
    boolean deleteLoadBalancerRule(DeleteLoadBalancerRuleCmd cmd);
    LoadBalancerVO updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd);
    
    RemoteAccessVpnVO createRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, InvalidParameterValueException, PermissionDeniedException;
    
    /**
     * Start a remote access vpn for the given ip address and client ip range
     * @param cmd the command specifying the ip address, ip range
     * @return the RemoteAccessVpnVO if successful, null otherwise
     * @throws ConcurrentOperationException 
     * @throws ResourceUnavailableException 
     */
    RemoteAccessVpnVO startRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException;
    
    /**
     * Destroy a previously created remote access VPN
     * @param cmd the command specifying the account and zone
     * @return success if successful, false otherwise
     * @throws ConcurrentOperationException 
     */
    boolean destroyRemoteAccessVpn(DeleteRemoteAccessVpnCmd cmd) throws ConcurrentOperationException;

    VpnUserVO addVpnUser(AddVpnUserCmd cmd) throws ConcurrentOperationException;

    boolean removeVpnUser(RemoveVpnUserCmd cmd) throws ConcurrentOperationException;
    
    FirewallRuleVO createIpForwardingRuleInDb(String ipAddr, Long virtualMachineId);

    boolean deletePortForwardingRule(Long id, boolean sysContext);

    boolean deleteIpForwardingRule(Long id);
}
