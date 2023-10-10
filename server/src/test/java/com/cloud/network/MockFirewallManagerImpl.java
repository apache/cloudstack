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

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.TrafficType;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import org.apache.cloudstack.api.command.user.firewall.IListFirewallRulesCmd;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

public class MockFirewallManagerImpl extends ManagerBase implements FirewallManager, FirewallService {

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<? extends FirewallRule>, Integer> listFirewallRules(IListFirewallRulesCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean revokeIngressFirewallRule(long ruleId, boolean apply) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean revokeEgressFirewallRule(long ruleId, boolean apply) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FirewallRule getFirewallRule(long ruleId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean revokeRelatedFirewallRule(long ruleId, boolean apply) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FirewallRule updateIngressFirewallRule(long ruleId, String customId, Boolean forDisplay) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FirewallRule updateEgressFirewallRule(long ruleId, String customId, Boolean forDisplay) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean applyIngressFwRules(long ipId, Account caller) throws ResourceUnavailableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean revokeIngressFwRule(long ruleId, boolean apply) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void detectRulesConflict(FirewallRule newRule) throws NetworkRuleConflictException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError, boolean updateRulesInDB) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean applyFirewallRules(List<FirewallRuleVO> rules, boolean continueOnError, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void revokeRule(FirewallRuleVO rule, Account caller, long userId, boolean needUsageEvent) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean revokeFirewallRulesForIp(long ipId, long userId, Account caller) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FirewallRule createRuleForAllCidrs(long ipAddrId, Account caller, Integer startPort, Integer endPort, String protocol, Integer icmpCode, Integer icmpType,
        Long relatedRuleId, long networkId) throws NetworkRuleConflictException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean revokeAllFirewallRulesForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean revokeFirewallRulesForVm(long vmId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addSystemFirewallRules(IPAddressVO ip, Account acct) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeRule(FirewallRule rule) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean applyDefaultEgressFirewallRule(Long networkId, boolean defaultPolicy, boolean add) throws ResourceUnavailableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void validateFirewallRule(Account caller, IPAddressVO ipAddress, Integer portStart, Integer portEnd, String proto, Purpose purpose, FirewallRuleType type,
        Long networkid, TrafficType trafficType) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean applyEgressFirewallRules(FirewallRule rule, Account caller) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean applyIngressFirewallRules(long ipId, Account caller) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FirewallRule createEgressFirewallRule(FirewallRule rule) throws NetworkRuleConflictException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FirewallRule createIngressFirewallRule(FirewallRule rule) throws NetworkRuleConflictException {
        // TODO Auto-generated method stub
        return null;
    }


}
