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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.commands.ListPortForwardingRulesCmd;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

@Local(value={RulesManager.class, RulesService.class})
public class RulesManagerImpl implements RulesManager, RulesService, Manager {
    private static final Logger s_logger = Logger.getLogger(RulesManagerImpl.class);
    String _name;
    
    @Inject PortForwardingRulesDao _forwardingDao;
    @Inject FirewallRulesDao _firewallDao;
    @Inject IPAddressDao _ipAddressDao;
    @Inject UserVmDao _vmDao;
    @Inject AccountManager _accountMgr;
    @Inject NetworkManager _networkMgr;
    @Inject EventDao _eventDao;
    @Inject UsageEventDao _usageEventDao;

    @Override
    public void detectRulesConflict(FirewallRule newRule, IpAddress ipAddress) throws NetworkRuleConflictException {
        assert newRule.getSourceIpAddress().equals(ipAddress.getAddress()) : "You passed in an ip address that doesn't match the address in the new rule";
        
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndNotRevoked(newRule.getSourceIpAddress(), null);
        assert (rules.size() >= 1) : "For network rules, we now always first persist the rule and then check for network conflicts so we should at least have one rule at this point.";
        
        for (FirewallRuleVO rule : rules) {
            if (rule.getId() == newRule.getId()) {
                continue;  // Skips my own rule.
            }
            
            if (rule.isOneToOneNat() && !newRule.isOneToOneNat()) {
                throw new NetworkRuleConflictException("There is 1 to 1 Nat rule specified for the " + newRule.getSourceIpAddress());
            } else if (!rule.isOneToOneNat() && newRule.isOneToOneNat()) {
                throw new NetworkRuleConflictException("There is already firewall rule specified for the " + newRule.getSourceIpAddress());
            }
            
            if (rule.getNetworkId() != newRule.getNetworkId() && rule.getState() != State.Revoke) {
                throw new NetworkRuleConflictException("New rule is for a different network than what's specified in rule " + rule.getXid());
            }
           
            if ((rule.getSourcePortStart() <= newRule.getSourcePortStart() && rule.getSourcePortEnd() >= newRule.getSourcePortStart()) || 
                (rule.getSourcePortStart() <= newRule.getSourcePortEnd() && rule.getSourcePortEnd() >= newRule.getSourcePortEnd()) ||
                (newRule.getSourcePortStart() <= rule.getSourcePortStart() && newRule.getSourcePortEnd() >= rule.getSourcePortStart()) ||
                (newRule.getSourcePortStart() <= rule.getSourcePortEnd() && newRule.getSourcePortEnd() >= rule.getSourcePortEnd())) {
                
                //we allow port forwarding rules with the same parameters but different protocols
                if (!(rule.getPurpose() == Purpose.PortForwarding && newRule.getPurpose() == Purpose.PortForwarding && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()))) {
                    throw new NetworkRuleConflictException("The range specified, " + newRule.getSourcePortStart() + "-" + newRule.getSourcePortEnd() + ", conflicts with rule " + rule.getId() + " which has " + rule.getSourcePortStart() + "-" + rule.getSourcePortEnd());
                }
            }
        }
        
        if (s_logger.isDebugEnabled()) { 
            s_logger.debug("No network rule conflicts detected for " + newRule + " against " + (rules.size() - 1) + " existing rules");
        }
        
    }

    @Override
    public void checkIpAndUserVm(IpAddress ipAddress, UserVm userVm, Account caller) throws InvalidParameterValueException, PermissionDeniedException {
        if (ipAddress == null || ipAddress.getAllocatedTime() == null || ipAddress.getAllocatedToAccountId() == null) {
            throw new InvalidParameterValueException("Unable to create ip forwarding rule on address " + ipAddress + ", invalid IP address specified.");
        }
        
        if (userVm == null) {
            return;
        }
        
        if (userVm.getState() == VirtualMachine.State.Destroyed || userVm.getState() == VirtualMachine.State.Expunging) {
            throw new InvalidParameterValueException("Invalid user vm: " + userVm.getId());
        }
        
        _accountMgr.checkAccess(caller, ipAddress);
        _accountMgr.checkAccess(caller, userVm);
        
        // validate that IP address and userVM belong to the same account
        if (ipAddress.getAllocatedToAccountId().longValue() != userVm.getAccountId()) {
            throw new InvalidParameterValueException("Unable to create ip forwarding rule, IP address " + ipAddress + " owner is not the same as owner of virtual machine " + userVm.toString()); 
        }

        // validate that userVM is in the same availability zone as the IP address
        if (ipAddress.getDataCenterId() != userVm.getDataCenterId()) {
            throw new InvalidParameterValueException("Unable to create ip forwarding rule, IP address " + ipAddress + " is not in the same availability zone as virtual machine " + userVm.toString());
        }
        
    }

    @Override @DB
    public PortForwardingRule createPortForwardingRule(PortForwardingRule rule, Long vmId, boolean isNat) throws NetworkRuleConflictException {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();
        
        Ip ipAddr = rule.getSourceIpAddress();
        
        IPAddressVO ipAddress = _ipAddressDao.findById(ipAddr);
        
        Ip dstIp = rule.getDestinationIpAddress();
        long networkId;
        UserVmVO vm = null;
        Network network = null;
        if (vmId != null) {
            // validate user VM exists
            vm = _vmDao.findById(vmId);
            if (vm == null) {
                throw new InvalidParameterValueException("Unable to create ip forwarding rule on address " + ipAddress + ", invalid virtual machine id specified (" + vmId + ").");
            }
            dstIp = null;
            List<? extends Nic> nics = _networkMgr.getNics(vm);
            for (Nic nic : nics) {
                Network ntwk = _networkMgr.getNetwork(nic.getNetworkId());
                if (ntwk.getGuestType() == GuestIpType.Virtual && nic.getIp4Address() != null) {
                    network = ntwk;
                    dstIp = new Ip(nic.getIp4Address());
                    break;
                }
            }
            
            if (network == null) {
                throw new CloudRuntimeException("Unable to find ip address to map to in vm id=" + vmId);
            }
        } else {
            network = _networkMgr.getNetwork(rule.getNetworkId());
            if (network == null) {
                throw new InvalidParameterValueException("Unable to get the network " + rule.getNetworkId());
            }
        }

        _accountMgr.checkAccess(caller, network);
        
        networkId = network.getId();
        long accountId = network.getAccountId();
        long domainId = network.getDomainId();
        
        if (isNat && (ipAddress.isSourceNat() || !ipAddress.isOneToOneNat() || ipAddress.getVmId() == null)) {
            throw new NetworkRuleConflictException("Can't do one to one NAT on ip address: " + ipAddress.getAddress());
        }
        
        PortForwardingRuleVO newRule = 
            new PortForwardingRuleVO(rule.getXid(), 
                    rule.getSourceIpAddress(), 
                    rule.getSourcePortStart(), 
                    rule.getSourcePortEnd(),
                    dstIp,
                    rule.getDestinationPortStart(), 
                    rule.getDestinationPortEnd(), 
                    rule.getProtocol(), 
                    networkId,
                    accountId,
                    domainId, vmId, isNat);
        newRule = _forwardingDao.persist(newRule);

        try {
            detectRulesConflict(newRule, ipAddress);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_ADD, newRule.getAccountId(), 0, newRule.getId(), null);
            _usageEventDao.persist(usageEvent);
            return newRule;
        } catch (Exception e) {
            _forwardingDao.remove(newRule.getId());
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException)e;
            }
            throw new CloudRuntimeException("Unable to add rule for " + newRule.getSourceIpAddress(), e);
        }
    }
    
    @Override
    public boolean enableOneToOneNat(Ip ip, long vmId) throws NetworkRuleConflictException{
        IPAddressVO ipAddress = _ipAddressDao.findById(ip);
        Account caller = UserContext.current().getCaller();
        
        UserVmVO vm = null;
        vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Can't enable one-to-one nat for the address " + ipAddress + ", invalid virtual machine id specified (" + vmId + ").");
        }
        
        checkIpAndUserVm(ipAddress, vm, caller);
        
        if (ipAddress.isSourceNat()) {
            throw new InvalidParameterValueException("Can't enable one to one nat, ip address: " + ip.addr() + " is a sourceNat ip address");
        }
       
        if (!ipAddress.isOneToOneNat()) {
            List<FirewallRuleVO> rules = _firewallDao.listByIpAndNotRevoked(ip, false);
            if (rules != null && !rules.isEmpty()) {
                throw new NetworkRuleConflictException("Failed to enable one to one nat for the ip address " + ipAddress.getAddress() + " as it already has firewall rules assigned");
            }
        } else {
            if (ipAddress.getVmId() != null && ipAddress.getVmId().longValue() != vmId) {
                throw new NetworkRuleConflictException("Failed to enable one to one nat for the ip address " + ipAddress.getAddress() + " and vm id=" + vmId + " as it's already assigned to antoher vm");
            }
        } 
        
        ipAddress.setOneToOneNat(true);
        ipAddress.setAssociatedWithVmId(vmId);
        return _ipAddressDao.update(ipAddress.getAddress(), ipAddress);
       
    }
    
    protected Pair<Network, Ip> getUserVmGuestIpAddress(UserVm vm) {
        Ip dstIp = null;
        List<? extends Nic> nics = _networkMgr.getNics(vm);
        for (Nic nic : nics) {
            Network ntwk = _networkMgr.getNetwork(nic.getNetworkId());
            if (ntwk.getGuestType() == GuestIpType.Virtual) {
                dstIp = new Ip(nic.getIp4Address());
                return new Pair<Network, Ip>(ntwk, dstIp);
            }
        }
        
        throw new CloudRuntimeException("Unable to find ip address to map to in " + vm.getId());
    }
    
    @DB
    protected void revokeRule(FirewallRuleVO rule, Account caller, long userId) {
        if (caller != null) {
            _accountMgr.checkAccess(caller, rule);
        }
       
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (rule.getState() == State.Staged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a rule that is still in stage state so just removing it: " + rule);
            }
            _firewallDao.remove(rule.getId());
        } else if (rule.getState() == State.Add || rule.getState() == State.Active) {
            rule.setState(State.Revoke);
            _firewallDao.update(rule.getId(), rule);
        }
        
        // Save and create the event
        String ruleName = rule.getPurpose() == Purpose.Firewall ? "Firewall" : (rule.isOneToOneNat() ? "ip forwarding" : "port forwarding");
        StringBuilder description = new StringBuilder("deleted ").append(ruleName).append(" rule [").append(rule.getSourceIpAddress()).append(":").append(rule.getSourcePortStart()).append("-").append(rule.getSourcePortEnd()).append("]");
        if (rule.getPurpose() == Purpose.PortForwarding) {
            PortForwardingRuleVO pfRule = (PortForwardingRuleVO)rule;
            description.append("->[").append(pfRule.getDestinationIpAddress()).append(":").append(pfRule.getDestinationPortStart()).append("-").append(pfRule.getDestinationPortEnd()).append("]");
        }
        description.append(" ").append(rule.getProtocol());

        txn.commit();
    }
    
    @Override
    public boolean revokePortForwardingRule(long ruleId, boolean apply) {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();
        
        PortForwardingRuleVO rule = _forwardingDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find " + ruleId);
        }
        
        _accountMgr.checkAccess(caller, rule);
        revokeRule(rule, caller, ctx.getCallerUserId());
        
        boolean success = false;
        
        if (apply) {
            success = applyPortForwardingRules(rule.getSourceIpAddress(), true);
        } else {
            success = true;
        }
        if(success){
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_DELETE, rule.getAccountId(), 0, rule.getId(), null);
            _usageEventDao.persist(usageEvent);
        }
        return success;
    }
    
    @Override
    public boolean revokePortForwardingRule(long vmId) {
    	UserVmVO vm = _vmDao.findById(vmId);
    	if (vm == null) {
    		return false;
    	}
    	
    	List<PortForwardingRuleVO> rules = _forwardingDao.listByVm(vmId);
    	for (PortForwardingRuleVO rule : rules) {
    		revokePortForwardingRule(rule.getId(), true);
    	}
        return true;
    }
    
    public List<? extends FirewallRule> listFirewallRules(Ip ip) {
        return _firewallDao.listByIpAndNotRevoked(ip, null);
    }

    @Override
    public List<? extends PortForwardingRule> listPortForwardingRulesForApplication(Ip ip) {
        return _forwardingDao.listForApplication(ip);
    }
    
    @Override
    public List<? extends PortForwardingRule> listPortForwardingRules(ListPortForwardingRulesCmd cmd) {
       Account caller = UserContext.current().getCaller();
       String ip = cmd.getIpAddress();
        
       Pair<String, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, cmd.getAccountName(), cmd.getDomainId());
       String accountName = accountDomainPair.first();
       Long domainId = accountDomainPair.second();
        
        if(cmd.getIpAddress() != null){
            Ip ipAddress = new Ip(cmd.getIpAddress());
            IPAddressVO ipAddressVO = _ipAddressDao.findById(ipAddress);
            if (ipAddressVO == null || !ipAddressVO.readyToUse()) {
                throw new InvalidParameterValueException("Ip address not ready for port forwarding rules yet: " + ipAddress);
            }
            _accountMgr.checkAccess(caller, ipAddressVO);
        }
        
        Filter filter = new Filter(PortForwardingRuleVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal()); 
        SearchBuilder<PortForwardingRuleVO> sb = _forwardingDao.createSearchBuilder();
        sb.and("ip", sb.entity().getSourceIpAddress(), Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), Op.EQ);
        sb.and("oneToOneNat", sb.entity().isOneToOneNat(), Op.EQ);
        
        SearchCriteria<PortForwardingRuleVO> sc = sb.create();
        
        if (ip != null) {
            sc.setParameters("ip", ip);
        }
        
        if (domainId != null) {
            sc.setParameters("domainId", domainId);
            if (accountName != null) {
                Account account = _accountMgr.getActiveAccount(accountName, domainId);
                sc.setParameters("accountId", account.getId());
            }
        }
        
        sc.setParameters("oneToOneNat", false);
       
        return _forwardingDao.search(sc, filter);
    }

    @Override 
    public boolean applyPortForwardingRules(Ip ip, boolean continueOnError) {
        try {
            return applyPortForwardingRules(ip, continueOnError, null);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to reapply port forwarding rules for " + ip);
            return false;
        }
    }
    
    protected boolean applyPortForwardingRules(Ip ip, boolean continueOnError, Account caller) throws ResourceUnavailableException {
        List<PortForwardingRuleVO> rules = _forwardingDao.listForApplication(ip);
        if (rules.size() == 0) {
            s_logger.debug("There are no rules to apply for " + ip);
            return true;
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, rules.toArray(new PortForwardingRuleVO[rules.size()]));
        }
        
        if (!_networkMgr.applyRules(rules, continueOnError)) {
            s_logger.debug("Rules are not completely applied");
            return false;
        }
        
        for (PortForwardingRuleVO rule : rules) {
            if (rule.getState() == FirewallRule.State.Revoke) {
                _forwardingDao.remove(rule.getId());
            } else if (rule.getState() == FirewallRule.State.Add) {
                rule.setState(FirewallRule.State.Active);
                _forwardingDao.update(rule.getId(), rule);
            }
        }
        
        return true;
    }
    
    @Override
    public List<PortForwardingRuleVO> searchForIpForwardingRules(Ip ip, Long id, Long vmId, Long start, Long size) {
        return _forwardingDao.searchNatRules(ip, id, vmId, start, size);
    }
    
    @Override
    public boolean applyPortForwardingRules(Ip ip, Account caller) throws ResourceUnavailableException {
        return applyPortForwardingRules(ip, false, caller);
    }
    
    @Override
    public boolean revokeAllRules(Ip ip, long userId) throws ResourceUnavailableException {
        List<PortForwardingRuleVO> rules = _forwardingDao.listByIpAndNotRevoked(ip);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + rules.size() + " rules for " + ip);
        }

        for (PortForwardingRuleVO rule : rules) {
            revokeRule(rule, null, userId);
        }
      
        applyPortForwardingRules(ip, true, null);
        
        // Now we check again in case more rules have been inserted.
        rules = _forwardingDao.listByIpAndNotRevoked(ip);
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released rules for " + ip + " and # of rules now = " + rules.size());
        }
        
        return rules.size() == 0;
    }
    
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
    public List<? extends FirewallRule> listFirewallRulesByIp(Ip ip) {
        return null;
    }
    
    @Override
    public boolean releasePorts(Ip ip, String protocol, FirewallRule.Purpose purpose, int... ports) {
        return _firewallDao.releasePorts(ip, protocol, purpose, ports); 
    }
    
    @Override @DB
    public FirewallRuleVO[] reservePorts(IpAddress ip, String protocol, FirewallRule.Purpose purpose, int... ports) throws NetworkRuleConflictException {
        FirewallRuleVO[] rules = new FirewallRuleVO[ports.length];
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (int i = 0; i < ports.length; i++) {
            rules[i] = 
                new FirewallRuleVO(null,
                        ip.getAddress(),
                        ports[i],
                        protocol,
                        ip.getAssociatedWithNetworkId(),
                        ip.getAllocatedToAccountId(),
                        ip.getAllocatedInDomainId(),
                        purpose, ip.isOneToOneNat());
            rules[i] = _firewallDao.persist(rules[i]);
        }
        txn.commit();

        boolean success = false;
        try {
            for (FirewallRuleVO newRule : rules) {
                detectRulesConflict(newRule, ip);
            }
            success = true;
            return rules;
        } finally {
            if (!success) {
                txn.start();
                
                for (FirewallRuleVO newRule : rules) {
                    _forwardingDao.remove(newRule.getId());
                }
                txn.commit();
            }
        } 
    }
    
    @Override
    public List<? extends PortForwardingRule> gatherPortForwardingRulesForApplication(List<? extends IpAddress> addrs) {
        List<PortForwardingRuleVO> allRules = new ArrayList<PortForwardingRuleVO>();
        
        for (IpAddress addr : addrs) {
            if (!addr.readyToUse()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Skipping " + addr + " because it is not ready for propation yet.");
                }
                continue;
            }
            allRules.addAll(_forwardingDao.listForApplication(addr.getAddress()));
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Found " + allRules.size() + " rules to apply for the addresses.");
        }
        
        return allRules;
    }
    
    @Override
    public List<? extends PortForwardingRule> listByNetworkId(long networkId) {
        return _forwardingDao.listByNetworkId(networkId);
    }
    
    public boolean isLastOneToOneNatRule(FirewallRule ruleToCheck) {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndNotRevoked(ruleToCheck.getSourceIpAddress(), false);
        if (rules != null && !rules.isEmpty()) {
            for (FirewallRuleVO rule : rules) {
                if (ruleToCheck.getId() == rule.getId()) {
                    continue;
                }
                if (rule.isOneToOneNat()) {
                    return false;
                }
            }
        } else {
            return true;
        }
        
        return true;
    }
    
    @Override
    public boolean disableOneToOneNat(Ip ip){
        Account caller = UserContext.current().getCaller();
        
        IPAddressVO ipAddress = _ipAddressDao.findById(ip);
        checkIpAndUserVm(ipAddress, null, caller);
        
        if (!ipAddress.isOneToOneNat()) {
            throw new InvalidParameterValueException("One to one nat is not enabled for the ip: " + ip.addr());
        }
       
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndNotRevoked(ip, true);
        if (rules != null) {
            for (FirewallRuleVO rule : rules) {
                rule.setState(State.Revoke);
                _firewallDao.update(rule.getId(), rule);
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_DELETE, rule.getAccountId(), 0, rule.getId(), null);
                _usageEventDao.persist(usageEvent);
            }
        }
        
        if (applyPortForwardingRules(ip, true)) {
            ipAddress.setOneToOneNat(false);
            ipAddress.setAssociatedWithVmId(null);
            _ipAddressDao.update(ipAddress.getAddress(), ipAddress);
            return true;
        } else {
            s_logger.warn("Failed to disable one to one nat for the ip address " + ip.addr());
            return false;
        }
    }

}
