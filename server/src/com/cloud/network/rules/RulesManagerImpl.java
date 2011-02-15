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
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
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
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.Service;
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
import com.cloud.utils.db.JoinBuilder;
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
    @Inject DomainDao _domainDao;

    @Override
    public void detectRulesConflict(FirewallRule newRule, IpAddress ipAddress) throws NetworkRuleConflictException {
        assert newRule.getSourceIpAddressId() == ipAddress.getId() : "You passed in an ip address that doesn't match the address in the new rule";
        
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndNotRevoked(newRule.getSourceIpAddressId(), null);
        assert (rules.size() >= 1) : "For network rules, we now always first persist the rule and then check for network conflicts so we should at least have one rule at this point.";
        
        for (FirewallRuleVO rule : rules) {
            if (rule.getId() == newRule.getId()) {
                continue;  // Skips my own rule.
            }
            
            if (rule.getPurpose() == Purpose.StaticNat && newRule.getPurpose() != Purpose.StaticNat) {
                throw new NetworkRuleConflictException("There is 1 to 1 Nat rule specified for the ip address id=" + newRule.getSourceIpAddressId());
            } else if (rule.getPurpose() != Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat) {
                throw new NetworkRuleConflictException("There is already firewall rule specified for the ip address id=" + newRule.getSourceIpAddressId());
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
    
    @Override
    public void checkRuleAndUserVm(FirewallRule rule, UserVm userVm, Account caller) throws InvalidParameterValueException, PermissionDeniedException {
        if (userVm == null || rule == null) {
            return;
        }
        
        _accountMgr.checkAccess(caller, rule);
        _accountMgr.checkAccess(caller, userVm);
       
        if (userVm.getState() == VirtualMachine.State.Destroyed || userVm.getState() == VirtualMachine.State.Expunging) {
            throw new InvalidParameterValueException("Invalid user vm: " + userVm.getId());
        }
        
        if (rule.getAccountId() != userVm.getAccountId()) {
            throw new InvalidParameterValueException("Rule id=" + rule.getId() + " and vm id=" + userVm.getId() + " belong to different accounts");
        }
    }


    @Override @ActionEvent (eventType=EventTypes.EVENT_NET_RULE_ADD, eventDescription="creating forwarding rule", create=true)
    public PortForwardingRule createPortForwardingRule(PortForwardingRule rule, Long vmId) throws NetworkRuleConflictException {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();
        
        Long ipAddrId = rule.getSourceIpAddressId();
        
        IPAddressVO ipAddress = _ipAddressDao.findById(ipAddrId);
        
        //Validate ip address
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule; ip id=" + ipAddrId + " doesn't exist in the system");
        } else if (ipAddress.isOneToOneNat()) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule; ip id=" + ipAddrId + " has static nat enabled");
        }else {
            _accountMgr.checkAccess(caller, ipAddress);
        }
        
        Ip dstIp = rule.getDestinationIpAddress();
        long networkId;
        UserVmVO vm = null;
        Network network = null;
        if (vmId != null) {
            // validate user VM exists
            vm = _vmDao.findById(vmId);
            if (vm == null) {
                throw new InvalidParameterValueException("Unable to create port forwarding rule on address " + ipAddress + ", invalid virtual machine id specified (" + vmId + ").");
            } else {
                checkRuleAndUserVm(rule, vm, caller);
            }
            
            Pair<Network, Ip> guestNic = getUserVmGuestIpAddress(vm, false);
            dstIp = guestNic.second();
            network = guestNic.first();
            
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
    
        //Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> firewallCapability = _networkMgr.getServiceCapability(network.getDataCenterId(), Service.Firewall);
        String supportedProtocols = firewallCapability.get(Capability.SupportedProtocols).toLowerCase();
        if (!supportedProtocols.contains(rule.getProtocol().toLowerCase())) {
            throw new InvalidParameterValueException("Protocol " + rule.getProtocol() + " is not supported in zone " + network.getDataCenterId());
        }
        
        //start port can't be bigger than end port
        if (rule.getDestinationPortStart() > rule.getDestinationPortEnd() || rule.getSourcePortStart() > rule.getSourcePortEnd()) {
            throw new InvalidParameterValueException("Start port can't be bigger than end port");
        }
        
        PortForwardingRuleVO newRule = 
            new PortForwardingRuleVO(rule.getXid(), 
                    rule.getSourceIpAddressId(), 
                    rule.getSourcePortStart(), 
                    rule.getSourcePortEnd(),
                    dstIp,
                    rule.getDestinationPortStart(), 
                    rule.getDestinationPortEnd(), 
                    rule.getProtocol().toLowerCase(), 
                    networkId,
                    accountId,
                    domainId, vmId);
        newRule = _forwardingDao.persist(newRule);

        try {
            detectRulesConflict(newRule, ipAddress);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            UserContext.current().setEventDetails("Rule Id: "+newRule.getId());
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_ADD, newRule.getAccountId(), 0, newRule.getId(), null);
            _usageEventDao.persist(usageEvent);
            return newRule;
        } catch (Exception e) {
            _forwardingDao.remove(newRule.getId());
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException)e;
            }
            throw new CloudRuntimeException("Unable to add rule for the ip id=" + newRule.getSourceIpAddressId(), e);
        }
    }
    
    
    
    @Override @ActionEvent (eventType=EventTypes.EVENT_NET_RULE_ADD, eventDescription="creating static nat rule", create=true)
    public StaticNatRule createStaticNatRule(StaticNatRule rule) throws NetworkRuleConflictException {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();
        
        Long ipAddrId = rule.getSourceIpAddressId();
        
        IPAddressVO ipAddress = _ipAddressDao.findById(ipAddrId);
        
        //Verify ip address existst and if 1-1 nat is enabled for it
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to create static nat rule; ip id=" + ipAddrId + " doesn't exist in the system");
        } else if (ipAddress.isSourceNat() || !ipAddress.isOneToOneNat() || ipAddress.getAssociatedWithVmId() == null) {
            throw new NetworkRuleConflictException("Can't do static nat on ip address: " + ipAddress.getAddress());
        }else {
            _accountMgr.checkAccess(caller, ipAddress);
        }
        
        //Get vm information and verify it
        long vmId = ipAddress.getAssociatedWithVmId();
         
        // validate user VM exists
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to create ip forwarding rule on address " + ipAddress + ", invalid virtual machine id specified (" + vmId + ").");
        } else {
            checkRuleAndUserVm(rule, vm, caller);
        }
        
        Pair<Network, Ip> guestNic = getUserVmGuestIpAddress(vm, false);
        Ip dstIp = guestNic.second();
        Network guestNetwork = guestNic.first();
        
        if (guestNetwork == null) {
            throw new CloudRuntimeException("Unable to find ip address to map to in vm id=" + vmId);
        }
       
        _accountMgr.checkAccess(caller, guestNetwork);
        
        long accountId = guestNetwork.getAccountId();
        long domainId = guestNetwork.getDomainId();
    
        //Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> firewallCapability = _networkMgr.getServiceCapability(guestNetwork.getDataCenterId(), Service.Firewall);
        String supportedProtocols = firewallCapability.get(Capability.SupportedProtocols).toLowerCase();
        if (!supportedProtocols.contains(rule.getProtocol().toLowerCase())) {
            throw new InvalidParameterValueException("Protocol " + rule.getProtocol() + " is not supported in zone " + guestNetwork.getDataCenterId());
        }
        
        //start port can't be bigger than end port
        if (rule.getSourcePortStart() > rule.getSourcePortEnd()) {
            throw new InvalidParameterValueException("Start port can't be bigger than end port");
        }
        
        FirewallRuleVO newRule = 
            new FirewallRuleVO(rule.getXid(),  
                    rule.getSourceIpAddressId(),
                    rule.getSourcePortStart(),
                    rule.getSourcePortEnd(),
                    rule.getProtocol().toLowerCase(),
                    guestNetwork.getId(),
                    accountId,
                    domainId, 
                    rule.getPurpose());
        newRule = _firewallDao.persist(newRule);

        try {
            detectRulesConflict(newRule, ipAddress);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            UserContext.current().setEventDetails("Rule Id: "+newRule.getId());
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_ADD, newRule.getAccountId(), 0, newRule.getId(), null);
            _usageEventDao.persist(usageEvent);
            
            StaticNatRule staticNatRule = new StaticNatRuleImpl(newRule, dstIp.addr());
            
            return staticNatRule;
        } catch (Exception e) {
            _forwardingDao.remove(newRule.getId());
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException)e;
            }
            throw new CloudRuntimeException("Unable to add static nat rule for the ip id=" + newRule.getSourceIpAddressId(), e);
        }
    }
    
    
    @Override
    public boolean enableOneToOneNat(long ipId, long vmId) throws NetworkRuleConflictException{
        IPAddressVO ipAddress = _ipAddressDao.findById(ipId);
        Account caller = UserContext.current().getCaller();
        
        UserVmVO vm = null;
        vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Can't enable one-to-one nat for the address " + ipAddress + ", invalid virtual machine id specified (" + vmId + ").");
        }
        
        checkIpAndUserVm(ipAddress, vm, caller);
        
        if (ipAddress.isSourceNat()) {
            throw new InvalidParameterValueException("Can't enable one to one nat, ip address id=" + ipId + " is a sourceNat ip address");
        }
       
        if (!ipAddress.isOneToOneNat()) {
            List<FirewallRuleVO> rules = _firewallDao.listByIpAndNotRevoked(ipId, Purpose.PortForwarding);
            if (rules != null && !rules.isEmpty()) {
                throw new NetworkRuleConflictException("Failed to enable one to one nat for the ip address id=" + ipAddress.getId() + " as it already has firewall rules assigned");
            }
        } else {
            if (ipAddress.getAssociatedWithVmId() != null && ipAddress.getAssociatedWithVmId().longValue() != vmId) {
                throw new NetworkRuleConflictException("Failed to enable one to one nat for the ip address id=" + ipAddress.getId() + " and vm id=" + vmId + " as it's already assigned to antoher vm");
            }
        } 
        
        ipAddress.setOneToOneNat(true);
        ipAddress.setAssociatedWithVmId(vmId);
        return _ipAddressDao.update(ipAddress.getId(), ipAddress);
       
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
        String ruleName = rule.getPurpose() == Purpose.Firewall ? "Firewall" : (rule.getPurpose() == FirewallRule.Purpose.StaticNat ? "ip forwarding" : "port forwarding");
        StringBuilder description = new StringBuilder("deleted ").append(ruleName).append(" rule [ipAddressId=").append(rule.getSourceIpAddressId()).append(":").append(rule.getSourcePortStart()).append("-").append(rule.getSourcePortEnd()).append("]");
        if (rule.getPurpose() == Purpose.PortForwarding) {
            PortForwardingRuleVO pfRule = (PortForwardingRuleVO)rule;
            description.append("->[").append(pfRule.getDestinationIpAddress()).append(":").append(pfRule.getDestinationPortStart()).append("-").append(pfRule.getDestinationPortEnd()).append("]");
        }
        description.append(" ").append(rule.getProtocol());

        txn.commit();
    }
    
    @Override @ActionEvent (eventType=EventTypes.EVENT_NET_RULE_DELETE, eventDescription="revoking forwarding rule", async=true)
    public boolean revokePortForwardingRule(long ruleId, boolean apply) {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();
        
        PortForwardingRuleVO rule = _forwardingDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find " + ruleId);
        }
        
        _accountMgr.checkAccess(caller, rule);
        
        return revokePortForwardingRuleInternal(ruleId, caller, ctx.getCallerUserId(), apply);
     }
    
    private boolean revokePortForwardingRuleInternal(long ruleId, Account caller, long userId, boolean apply) {  
        PortForwardingRuleVO rule = _forwardingDao.findById(ruleId);
        
        long ownerId = rule.getAccountId();
          
        revokeRule(rule, caller, userId);
        
        boolean success = false;
        
        if (apply) {
            success = applyPortForwardingRules(rule.getSourceIpAddressId(), true, caller);
        } else {
            success = true;
        }
        if(success){
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_DELETE, ownerId, 0, ruleId, null);
            _usageEventDao.persist(usageEvent);
        }
        return success;
    }
    
    
    @Override @ActionEvent (eventType=EventTypes.EVENT_NET_RULE_DELETE, eventDescription="revoking forwarding rule", async=true)
    public boolean revokeStaticNatRule(long ruleId, boolean apply) {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();
        
        FirewallRuleVO rule = _firewallDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find " + ruleId);
        }
        
        _accountMgr.checkAccess(caller, rule);
        
        return revokeStaticNatRuleInternal(ruleId, caller, ctx.getCallerUserId(), apply);
     }
    
    private boolean revokeStaticNatRuleInternal(long ruleId, Account caller, long userId, boolean apply) {  
        FirewallRuleVO rule = _firewallDao.findById(ruleId);
          
        revokeRule(rule, caller, userId);
        
        boolean success = false;
        
        if (apply) {
            success = applyStaticNatRules(rule.getSourceIpAddressId(), true, caller);
        } else {
            success = true;
        }
        if(success){
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_DELETE, rule.getAccountId(), 0, ruleId, null);
            _usageEventDao.persist(usageEvent);
        }
        return success;
    }
    
    
    @Override
    public boolean revokePortForwardingRulesForVm(long vmId) {
    	UserVmVO vm = _vmDao.findByIdIncludingRemoved(vmId);
    	if (vm == null) {
    		return false;
    	}
    	
    	List<PortForwardingRuleVO> rules = _forwardingDao.listByVm(vmId);
    	
    	if (rules == null || rules.isEmpty()) {
    	    s_logger.debug("No port forwarding rules are found for vm id=" + vmId);
            return true;
        }
    	
    	for (PortForwardingRuleVO rule : rules) {
    		revokePortForwardingRuleInternal(rule.getId(), _accountMgr.getSystemAccount(), Account.ACCOUNT_ID_SYSTEM, true);
    	}
        return true;
    }
    
    
    @Override
    public boolean revokeStaticNatRulesForVm(long vmId) {
        UserVmVO vm = _vmDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            return false;
        }
        
        List<FirewallRuleVO> rules = _firewallDao.listStaticNatByVmId(vm.getId());
        
        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No static nat rules are found for vm id=" + vmId);
            return true;
        }
        
        for (FirewallRuleVO rule : rules) {
            revokeStaticNatRuleInternal(rule.getId(), _accountMgr.getSystemAccount(), Account.ACCOUNT_ID_SYSTEM, true);
        }
        return true;
    }

    @Override
    public List<? extends PortForwardingRule> listPortForwardingRulesForApplication(long ipId) {
        return _forwardingDao.listForApplication(ipId);
    }
    
    @Override
    public List<? extends PortForwardingRule> listPortForwardingRules(ListPortForwardingRulesCmd cmd) {
       Account caller = UserContext.current().getCaller();
       Long ipId = cmd.getIpAddressId();
       String path = null;
        
       Pair<String, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, cmd.getAccountName(), cmd.getDomainId());
       String accountName = accountDomainPair.first();
       Long domainId = accountDomainPair.second();
        
        if(ipId != null){
            IPAddressVO ipAddressVO = _ipAddressDao.findById(ipId);
            if (ipAddressVO == null || !ipAddressVO.readyToUse()) {
                throw new InvalidParameterValueException("Ip address id=" + ipId + " not ready for port forwarding rules yet");
            }
            _accountMgr.checkAccess(caller, ipAddressVO);
        }
        
        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            Domain domain = _accountMgr.getDomain(caller.getDomainId());
            path = domain.getPath();
        }
        
        Filter filter = new Filter(PortForwardingRuleVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal()); 
        SearchBuilder<PortForwardingRuleVO> sb = _forwardingDao.createSearchBuilder();
        sb.and("ip", sb.entity().getSourceIpAddressId(), Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), Op.EQ);
        
        if (path != null) {
            //for domain admin we should show only subdomains information
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        SearchCriteria<PortForwardingRuleVO> sc = sb.create();
        
        if (ipId != null) {
            sc.setParameters("ip", ipId);
        }
        
        if (domainId != null) {
            sc.setParameters("domainId", domainId);
            if (accountName != null) {
                Account account = _accountMgr.getActiveAccount(accountName, domainId);
                sc.setParameters("accountId", account.getId());
            }
        }
        
        sc.setParameters("purpose", Purpose.PortForwarding);
        
        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
        }
       
        return _forwardingDao.search(sc, filter);
    }

   
    @Override
    public boolean applyPortForwardingRules(long ipId, boolean continueOnError, Account caller){
        List<PortForwardingRuleVO> rules = _forwardingDao.listForApplication(ipId);
        if (rules.size() == 0) {
            s_logger.debug("There are no firwall rules to apply for ip id=" + ipId);
            return true;
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, rules.toArray(new PortForwardingRuleVO[rules.size()]));
        }
        
        try {
            if (!applyRules(rules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to ", ex);
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean applyStaticNatRules(long sourceIpId, boolean continueOnError, Account caller){
        List<? extends FirewallRule> rules = _firewallDao.listByIpAndPurpose(sourceIpId, Purpose.StaticNat);
        List<StaticNatRule> staticNatRules = new ArrayList<StaticNatRule>();
        
        if (rules.size() == 0) {
            s_logger.debug("There are no firwall rules to apply for ip id=" + sourceIpId);
            return true;
        }
        
        for (FirewallRule rule : rules) {  
            IpAddress sourceIp = _ipAddressDao.findById(rule.getSourceIpAddressId());
            
            UserVmVO vm = _vmDao.findById(sourceIp.getAssociatedWithVmId());
            
            Pair<Network, Ip> guestNic = getUserVmGuestIpAddress(vm, true);
            Ip dstIp = guestNic.second();
            Network network = guestNic.first();
            
            if (network == null) {
                throw new CloudRuntimeException("Unable to find ip address to map to in vm id=" + vm.getId());
            }
            
            FirewallRuleVO ruleVO = _firewallDao.findById(rule.getId());
            
            staticNatRules.add(new StaticNatRuleImpl(ruleVO, dstIp.addr()));
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, staticNatRules.toArray(new StaticNatRule[staticNatRules.size()]));
        }
        
        try {
            if (!applyRules(staticNatRules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply static nat rules due to ", ex);
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean applyPortForwardingRulesForNetwork(long networkId, boolean continueOnError, Account caller){
        List<PortForwardingRuleVO> rules = listByNetworkId(networkId);
        if (rules.size() == 0) {
            s_logger.debug("There are no port forwarding rules to apply for network id=" + networkId);
            return true;
        }
        
        if (caller != null) {
            _accountMgr.checkAccess(caller, rules.toArray(new PortForwardingRuleVO[rules.size()]));
        }
        
        try {
            if (!applyRules(rules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to ", ex);
            return false;
        }
        
        return true;
    }
    
    
    @Override
    public boolean applyStaticNatRulesForNetwork(long networkId, boolean continueOnError, Account caller){
        List<FirewallRuleVO> rules = _firewallDao.listByNetworkIdAndPurpose(networkId, Purpose.StaticNat);
        List<StaticNatRule> staticNatRules = new ArrayList<StaticNatRule>();
        
        if (rules.size() == 0) {
            s_logger.debug("There are no static nat rules to apply for network id=" + networkId);
            return true;
        }
        
        if (caller != null) {
            _accountMgr.checkAccess(caller, rules.toArray(new FirewallRule[rules.size()]));
        }
        
        for (FirewallRuleVO rule : rules) {  
            IpAddress sourceIp = _ipAddressDao.findById(rule.getSourceIpAddressId());
            
            UserVmVO vm = _vmDao.findById(sourceIp.getAssociatedWithVmId());
            
            Pair<Network, Ip> guestNic = getUserVmGuestIpAddress(vm, true);
            Ip dstIp = guestNic.second();
            Network network = guestNic.first();
            
            if (network == null) {
                throw new CloudRuntimeException("Unable to find ip address to map to in vm id=" + vm.getId());
            }
            
            staticNatRules.add(new StaticNatRuleImpl(rule, dstIp.addr()));
        } 
        
        try {
            if (!applyRules(staticNatRules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to ", ex);
            return false;
        }
        
        return true;
    }
    
    
    
    private boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException{
        if (!_networkMgr.applyRules(rules, continueOnError)) {
            s_logger.warn("Rules are not completely applied");
            return false;
        } else {
            for (FirewallRule rule : rules) {
                if (rule.getState() == FirewallRule.State.Revoke) {
                    _firewallDao.remove(rule.getId());
                } else if (rule.getState() == FirewallRule.State.Add) {
                    FirewallRuleVO ruleVO = _firewallDao.findById(rule.getId());
                    ruleVO.setState(FirewallRule.State.Active);
                    _firewallDao.update(ruleVO.getId(), ruleVO);
                }
            }
            return true;
        }
    }
    
    @Override
    public List<? extends FirewallRule> searchStaticNatRules(Long ipId, Long id, Long vmId, Long start, Long size, String accountName, Long domainId) {
        Account caller = UserContext.current().getCaller();
        String path = null;
         
        Pair<String, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, accountName, domainId);
        accountName = accountDomainPair.first();
        domainId = accountDomainPair.second();
         
         if(ipId != null){
             IPAddressVO ipAddressVO = _ipAddressDao.findById(ipId);
             if (ipAddressVO == null || !ipAddressVO.readyToUse()) {
                 throw new InvalidParameterValueException("Ip address id=" + ipId + " not ready for port forwarding rules yet");
             }
             _accountMgr.checkAccess(caller, ipAddressVO);
         }
         
         if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
             Domain domain = _accountMgr.getDomain(caller.getDomainId());
             path = domain.getPath();
         }
         
         Filter filter = new Filter(PortForwardingRuleVO.class, "id", false, start, size); 
         SearchBuilder<FirewallRuleVO> sb = _firewallDao.createSearchBuilder();
         sb.and("ip", sb.entity().getSourceIpAddressId(), Op.EQ);
         sb.and("accountId", sb.entity().getAccountId(), Op.EQ);
         sb.and("domainId", sb.entity().getDomainId(), Op.EQ);
         sb.and("purpose", sb.entity().getPurpose(), Op.EQ);
         sb.and("id", sb.entity().getId(), Op.EQ);
         
         if (path != null) {
             //for domain admin we should show only subdomains information
             SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
             domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
             sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
         }
         
         if (vmId != null) {
             SearchBuilder<IPAddressVO> ipSearch = _ipAddressDao.createSearchBuilder();
             ipSearch.and("associatedWithVmId", ipSearch.entity().getAssociatedWithVmId(), Op.EQ);
             sb.join("ipSearch", ipSearch, sb.entity().getSourceIpAddressId(), ipSearch.entity().getId(), JoinBuilder.JoinType.INNER);
         }
         
         SearchCriteria<FirewallRuleVO> sc = sb.create();
         
         if (id != null) {
             sc.setParameters("id", id);
         }
         
         if (ipId != null) {
             sc.setParameters("ip", ipId);
         }
         
         if (domainId != null) {
             sc.setParameters("domainId", domainId);
             if (accountName != null) {
                 Account account = _accountMgr.getActiveAccount(accountName, domainId);
                 sc.setParameters("accountId", account.getId());
             }
         }
         
         sc.setParameters("purpose", Purpose.StaticNat);
         
         if (path != null) {
             sc.setJoinParameters("domainSearch", "path", path + "%");
         }
         
         if (vmId != null) {
             sc.setJoinParameters("ipSearch", "associatedWithVmId", vmId);
         }
         
         return _firewallDao.search(sc, filter);
    }
    
    @Override @ActionEvent (eventType=EventTypes.EVENT_NET_RULE_ADD, eventDescription="applying port forwarding rule", async=true)
    public boolean applyPortForwardingRules(long ipId, Account caller) throws ResourceUnavailableException {
        return applyPortForwardingRules(ipId, false, caller);
    }
    
    
    @Override @ActionEvent (eventType=EventTypes.EVENT_NET_RULE_ADD, eventDescription="applying static nat rule", async=true)
    public boolean applyStaticNatRules(long ipId, Account caller) throws ResourceUnavailableException {
        return applyStaticNatRules(ipId, false, caller);
    }
    
    @Override
    public boolean revokeAllRules(long ipId, long userId, Account caller) throws ResourceUnavailableException {
        List<FirewallRule> rules = new ArrayList<FirewallRule>();
        
        //revoke all port forwarding rules
        List<PortForwardingRuleVO> pfRules = _forwardingDao.listByIpAndNotRevoked(ipId);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + pfRules.size() + " port forwarding rules for ip id=" + ipId);
        }

        for (PortForwardingRuleVO rule : pfRules) {
            revokePortForwardingRuleInternal(rule.getId(), caller, userId, true);
        }
      
        applyPortForwardingRules(ipId, true, null);
        
        //revoke all all static nat rules
        List<FirewallRuleVO> staticNatRules = _firewallDao.listByIpAndNotRevoked(ipId, Purpose.StaticNat);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + pfRules.size() + " static nat rules for ip id=" + ipId);
        }

        for (FirewallRuleVO rule : staticNatRules) {
            revokeStaticNatRuleInternal(rule.getId(), caller, userId, true);
        }
      
        applyStaticNatRules(ipId, true, null);
        
        
        // Now we check again in case more rules have been inserted.
        rules.addAll(_forwardingDao.listByIpAndNotRevoked(ipId));
        rules.addAll(_firewallDao.listByIpAndPurpose(ipId, Purpose.StaticNat));
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released rules for ip id=" + ipId + " and # of rules now = " + rules.size());
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
    public List<? extends FirewallRule> listFirewallRulesByIp(long ipId) {
        return null;
    }
    
    @Override
    public boolean releasePorts(long ipId, String protocol, FirewallRule.Purpose purpose, int... ports) {
        return _firewallDao.releasePorts(ipId, protocol, purpose, ports); 
    }
    
    @Override @DB
    public FirewallRuleVO[] reservePorts(IpAddress ip, String protocol, FirewallRule.Purpose purpose, int... ports) throws NetworkRuleConflictException {
        FirewallRuleVO[] rules = new FirewallRuleVO[ports.length];
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (int i = 0; i < ports.length; i++) {
            rules[i] = 
                new FirewallRuleVO(null,
                        ip.getId(),
                        ports[i],
                        protocol,
                        ip.getAssociatedWithNetworkId(),
                        ip.getAllocatedToAccountId(),
                        ip.getAllocatedInDomainId(),
                        purpose);
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
            allRules.addAll(_forwardingDao.listForApplication(addr.getId()));
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Found " + allRules.size() + " rules to apply for the addresses.");
        }
        
        return allRules;
    }
    
    @Override
    public List<PortForwardingRuleVO> listByNetworkId(long networkId) {
        return _forwardingDao.listByNetworkId(networkId);
    }
    
    @Override
    public boolean disableOneToOneNat(long ipId){
        Account caller = UserContext.current().getCaller();
        
        IPAddressVO ipAddress = _ipAddressDao.findById(ipId);
        checkIpAndUserVm(ipAddress, null, caller);
        
        if (!ipAddress.isOneToOneNat()) {
            throw new InvalidParameterValueException("One to one nat is not enabled for the ip id=" + ipId);
        }
       
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndNotRevoked(ipId, Purpose.StaticNat);
        if (rules != null) {
            for (FirewallRuleVO rule : rules) {
                rule.setState(State.Revoke);
                _firewallDao.update(rule.getId(), rule);
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_DELETE, rule.getAccountId(), 0, rule.getId(), null);
                _usageEventDao.persist(usageEvent);
            }
        }
        
        if (applyStaticNatRules(ipId, true, caller)) {
            ipAddress.setOneToOneNat(false);
            ipAddress.setAssociatedWithVmId(null);
            _ipAddressDao.update(ipAddress.getId(), ipAddress);
            return true;
        } else {
            s_logger.warn("Failed to disable one to one nat for the ip address id" + ipId);
            return false;
        }
    }
    
    @Override
    public PortForwardingRule getPortForwardigRule(long ruleId) {
        return _forwardingDao.findById(ruleId);
    }
    
    
    @Override
    public FirewallRule getFirewallRule(long ruleId) {
        return _firewallDao.findById(ruleId);
    }
    
    protected Pair<Network, Ip> getUserVmGuestIpAddress(UserVm vm, boolean includeRemovedNics) {
        Ip dstIp = null;
        List<? extends Nic> nics;
        
        if (includeRemovedNics) {
           nics = _networkMgr.getNicsIncludingRemoved(vm);
        } else {
            nics = _networkMgr.getNics(vm);
        }
        
        for (Nic nic : nics) {
            Network ntwk = _networkMgr.getNetwork(nic.getNetworkId());
            if (ntwk.getGuestType() == GuestIpType.Virtual && nic.getIp4Address() != null) {
                dstIp = new Ip(nic.getIp4Address());
                return new Pair<Network, Ip>(ntwk, dstIp);
            }
        }
        
        throw new CloudRuntimeException("Unable to find ip address to map to in " + vm.getId());
    }
    
    @Override
    public StaticNatRule buildStaticNatRule(FirewallRule rule) {
        IpAddress ip = _ipAddressDao.findById(rule.getSourceIpAddressId());
        FirewallRuleVO ruleVO = _firewallDao.findById(rule.getId());
        
        if (ip == null || !ip.isOneToOneNat()) {
            throw new InvalidParameterValueException("Source ip address of the rule id=" + rule.getId() + " is not static nat enabled");
        }
        
        UserVm vm = _vmDao.findById(ip.getAssociatedWithVmId());
        
        if (vm == null) {
            throw new InvalidParameterValueException("Static nat rule id=" + rule.getId() + " doesn't have vm assocaited with it");
        }
       
        Pair<Network, Ip> guestNic = getUserVmGuestIpAddress(vm, false);
        Ip dstIp = guestNic.second();
        
        return new StaticNatRuleImpl(ruleVO, dstIp.addr());
    }
     
}
