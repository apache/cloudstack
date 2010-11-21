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
package com.cloud.network.rules;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

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
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouterVO;

@Local(value=RulesManager.class)
public class RulesManagerImpl implements RulesManager, Manager {
    String _name;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public List<FirewallRuleVO> updatePortForwardingRules(List<FirewallRuleVO> fwRules, DomainRouterVO router, Long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean updateLoadBalancerRules(List<FirewallRuleVO> fwRules, DomainRouterVO router, Long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<FirewallRuleVO> updateFirewallRules(String publicIpAddress, List<FirewallRuleVO> fwRules, DomainRouterVO router) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FirewallRuleVO createPortForwardingRule(CreatePortForwardingRuleCmd cmd) throws NetworkRuleConflictException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<FirewallRuleVO> listPortForwardingRules(ListPortForwardingRulesCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LoadBalancerVO createLoadBalancerRule(CreateLoadBalancerRuleCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean updateFirewallRule(FirewallRuleVO fwRule, String oldPrivateIP, String oldPrivatePort) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean assignToLoadBalancer(AssignToLoadBalancerRuleCmd cmd) throws NetworkRuleConflictException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeFromLoadBalancer(RemoveFromLoadBalancerRuleCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteLoadBalancerRule(DeleteLoadBalancerRuleCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public LoadBalancerVO updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RemoteAccessVpnVO createRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, InvalidParameterValueException,
            PermissionDeniedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RemoteAccessVpnVO startRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean destroyRemoteAccessVpn(DeleteRemoteAccessVpnCmd cmd) throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public VpnUserVO addVpnUser(AddVpnUserCmd cmd) throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeVpnUser(RemoveVpnUserCmd cmd) throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FirewallRuleVO createIpForwardingRuleInDb(String ipAddr, Long virtualMachineId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deletePortForwardingRule(Long id, boolean sysContext) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteIpForwardingRule(Long id) {
        // TODO Auto-generated method stub
        return false;
    }
}
