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

import com.cloud.api.commands.AddVpnUserCmd;
import com.cloud.api.commands.AssignToLoadBalancerRuleCmd;
import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.api.commands.CreatePortForwardingRuleCmd;
import com.cloud.api.commands.CreateRemoteAccessVpnCmd;
import com.cloud.api.commands.DeleteLoadBalancerRuleCmd;
import com.cloud.api.commands.DeleteRemoteAccessVpnCmd;
import com.cloud.api.commands.DisassociateIPAddrCmd;
import com.cloud.api.commands.ListPortForwardingRulesCmd;
import com.cloud.api.commands.RemoveFromLoadBalancerRuleCmd;
import com.cloud.api.commands.RemoveVpnUserCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;


public interface NetworkService {
    List<? extends NetworkOffering> listNetworkOfferings();
    /**
     * Associates a public IP address for a router.
     * @param cmd - the command specifying ipAddress
     * @return ip address object
     * @throws ResourceAllocationException, InsufficientCapacityException 
     */
    IpAddress associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException;    
    /**
     * Assign a virtual machine, or list of virtual machines, to a load balancer.
     */
    boolean assignToLoadBalancer(AssignToLoadBalancerRuleCmd cmd)  throws NetworkRuleConflictException;

    public boolean removeFromLoadBalancer(RemoveFromLoadBalancerRuleCmd cmd);
    
    public boolean deleteLoadBalancerRule(DeleteLoadBalancerRuleCmd cmd);
    public LoadBalancer updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd);
    public boolean disassociateIpAddress(DisassociateIPAddrCmd cmd);

    /**
     * Create a remote access vpn from the given public ip address and client ip range
     * @param cmd the command specifying the ip address, ip range
     * @return the newly created RemoteAccessVpnVO if successful, null otherwise
     * @throws InvalidParameterValueException
     * @throws PermissionDeniedException
     * @throws ConcurrentOperationException 
     */
    public RemoteAccessVpn createRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, InvalidParameterValueException, PermissionDeniedException;
    
    /**
     * Start a remote access vpn for the given public ip address and client ip range
     * @param cmd the command specifying the ip address, ip range
     * @return the RemoteAccessVpnVO if successful, null otherwise
     * @throws ConcurrentOperationException 
     * @throws ResourceUnavailableException 
     */
    public RemoteAccessVpn startRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException;
    
    /**
     * Destroy a previously created remote access VPN
     * @param cmd the command specifying the account and zone
     * @return success if successful, false otherwise
     * @throws ConcurrentOperationException 
     */
    public boolean destroyRemoteAccessVpn(DeleteRemoteAccessVpnCmd cmd) throws ConcurrentOperationException;

    VpnUser addVpnUser(AddVpnUserCmd cmd) throws ConcurrentOperationException, AccountLimitException;

    boolean removeVpnUser(RemoveVpnUserCmd cmd) throws ConcurrentOperationException;
    
    /**
     * Create a port forwarding rule from the given ipAddress/port to the given virtual machine/port.
     * @param cmd the command specifying the ip address, public port, protocol, private port, and virtual machine id.
     * @return the newly created FirewallRuleVO if successful, null otherwise.
     */
    public FirewallRule createPortForwardingRule(CreatePortForwardingRuleCmd cmd) throws NetworkRuleConflictException;

    /**
     * List port forwarding rules assigned to an ip address
     * @param cmd the command object holding the criteria for listing port forwarding rules (the ipAddress)
     * @return list of port forwarding rules on the given address, empty list if no rules exist
     */
    public List<? extends FirewallRule> listPortForwardingRules(ListPortForwardingRulesCmd cmd);

    /**
     * Create a load balancer rule from the given ipAddress/port to the given private port
     * @param cmd the command specifying the ip address, public port, protocol, private port, and algorithm
     * @return the newly created LoadBalancerVO if successful, null otherwise
     */
    public LoadBalancer createLoadBalancerRule(CreateLoadBalancerRuleCmd cmd);

    FirewallRule createIpForwardingRuleInDb(String ipAddr, long virtualMachineId);
    
    FirewallRule createIpForwardingRuleOnDomr(long ruleId);

    boolean deleteIpForwardingRule(Long id);
    boolean deletePortForwardingRule(Long id, boolean sysContext);

}
