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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
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

@Local(value = { RulesManager.class, RulesService.class })
public class RulesManagerImpl implements RulesManager, RulesService, Manager {
    private static final Logger s_logger = Logger.getLogger(RulesManagerImpl.class);
    String _name;

    @Inject
    PortForwardingRulesDao _forwardingDao;
    @Inject
    FirewallRulesCidrsDao _firewallCidrsDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    UserVmDao _vmDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    EventDao _eventDao;
    @Inject
    UsageEventDao _usageEventDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    FirewallManager _firewallMgr;
    @Inject
    DomainManager _domainMgr;

    @Override
    public void checkIpAndUserVm(IpAddress ipAddress, UserVm userVm, Account caller) {
        if (ipAddress == null || ipAddress.getAllocatedTime() == null || ipAddress.getAllocatedToAccountId() == null) {
            throw new InvalidParameterValueException("Unable to create ip forwarding rule on address " + ipAddress + ", invalid IP address specified.");
        }

        if (userVm == null) {
            return;
        }

        if (userVm.getState() == VirtualMachine.State.Destroyed || userVm.getState() == VirtualMachine.State.Expunging) {
            throw new InvalidParameterValueException("Invalid user vm: " + userVm.getId());
        }

        _accountMgr.checkAccess(caller, null, ipAddress);
        _accountMgr.checkAccess(caller, null, userVm);

        // validate that IP address and userVM belong to the same account
        if (ipAddress.getAllocatedToAccountId().longValue() != userVm.getAccountId()) {
            throw new InvalidParameterValueException("Unable to create ip forwarding rule, IP address " + ipAddress + " owner is not the same as owner of virtual machine " + userVm.toString());
        }

        // validate that userVM is in the same availability zone as the IP address
        if (ipAddress.getDataCenterId() != userVm.getDataCenterIdToDeployIn()) {
            throw new InvalidParameterValueException("Unable to create ip forwarding rule, IP address " + ipAddress + " is not in the same availability zone as virtual machine " + userVm.toString());
        }

    }

    @Override
    public void checkRuleAndUserVm(FirewallRule rule, UserVm userVm, Account caller) {
        if (userVm == null || rule == null) {
            return;
        }

        _accountMgr.checkAccess(caller, null, rule);
        _accountMgr.checkAccess(caller, null, userVm);

        if (userVm.getState() == VirtualMachine.State.Destroyed || userVm.getState() == VirtualMachine.State.Expunging) {
            throw new InvalidParameterValueException("Invalid user vm: " + userVm.getId());
        }

        if (rule.getAccountId() != userVm.getAccountId()) {
            throw new InvalidParameterValueException("New rule " + rule + " and vm id=" + userVm.getId() + " belong to different accounts");
        }
    }

    @Override @DB
    @ActionEvent(eventType = EventTypes.EVENT_NET_RULE_ADD, eventDescription = "creating forwarding rule", create = true)
    public PortForwardingRule createPortForwardingRule(PortForwardingRule rule, Long vmId, boolean openFirewall) throws NetworkRuleConflictException {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        Long ipAddrId = rule.getSourceIpAddressId();

        IPAddressVO ipAddress = _ipAddressDao.findById(ipAddrId);
        
        // Validate ip address
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule; ip id=" + ipAddrId + " doesn't exist in the system");
        } else if (ipAddress.isOneToOneNat()) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule; ip id=" + ipAddrId + " has static nat enabled");
        } 
        
        _firewallMgr.validateFirewallRule(caller, ipAddress, rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol(), Purpose.PortForwarding);
        
        Long networkId = ipAddress.getAssociatedWithNetworkId();
        Long accountId = ipAddress.getAccountId();
        Long domainId = ipAddress.getDomainId();
        
        // start port can't be bigger than end port
        if (rule.getDestinationPortStart() > rule.getDestinationPortEnd()) {
            throw new InvalidParameterValueException("Start port can't be bigger than end port");
        }
        
        // check that the port ranges are of equal size
        if ((rule.getDestinationPortEnd() - rule.getDestinationPortStart()) != (rule.getSourcePortEnd() - rule.getSourcePortStart())) {
            throw new InvalidParameterValueException("Source port and destination port ranges should be of equal sizes.");
        }
        
        // validate user VM exists
        UserVm vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule on address " + ipAddress + ", invalid virtual machine id specified (" + vmId + ").");
        } else {
            checkRuleAndUserVm(rule, vm, caller);
        }

        // Verify that vm has nic in the network
        Ip dstIp = rule.getDestinationIpAddress();
        Nic guestNic = _networkMgr.getNicInNetwork(vmId, networkId);
        if (guestNic == null || guestNic.getIp4Address() == null) {
            throw new InvalidParameterValueException("Vm doesn't belong to network associated with ipAddress");
        } else {
            dstIp = new Ip(guestNic.getIp4Address());
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        PortForwardingRuleVO newRule = new PortForwardingRuleVO(rule.getXid(), rule.getSourceIpAddressId(), rule.getSourcePortStart(), rule.getSourcePortEnd(), dstIp, rule.getDestinationPortStart(),
                rule.getDestinationPortEnd(), rule.getProtocol().toLowerCase(), networkId, accountId, domainId, vmId);
        newRule = _forwardingDao.persist(newRule);
        
        //create firewallRule for 0.0.0.0/0 cidr
        if (openFirewall) {
            _firewallMgr.createRuleForAllCidrs(ipAddrId, caller, rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol(), null, null, newRule.getId());
        }

        try {
            _firewallMgr.detectRulesConflict(newRule, ipAddress);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            UserContext.current().setEventDetails("Rule Id: " + newRule.getId());
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_ADD, newRule.getAccountId(), ipAddress.getDataCenterId(), newRule.getId(), null);
            _usageEventDao.persist(usageEvent);
            txn.commit();
            return newRule;
        } catch (Exception e) {
            
           
            if (newRule != null) {
                
                txn.start();
                //no need to apply the rule as it wasn't programmed on the backend yet
                _firewallMgr.revokeRelatedFirewallRule(newRule.getId(), false);
                _forwardingDao.remove(newRule.getId());
                
                txn.commit();
            }
            
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            }
            throw new CloudRuntimeException("Unable to add rule for the ip id=" + ipAddrId, e);
        }
    }

    @Override @DB
    @ActionEvent(eventType = EventTypes.EVENT_NET_RULE_ADD, eventDescription = "creating static nat rule", create = true)
    public StaticNatRule createStaticNatRule(StaticNatRule rule, boolean openFirewall) throws NetworkRuleConflictException {
        Account caller = UserContext.current().getCaller();

        Long ipAddrId = rule.getSourceIpAddressId();

        IPAddressVO ipAddress = _ipAddressDao.findById(ipAddrId);
        
        // Validate ip address
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to create static nat rule; ip id=" + ipAddrId + " doesn't exist in the system");
        } else if (ipAddress.isSourceNat() || !ipAddress.isOneToOneNat() || ipAddress.getAssociatedWithVmId() == null) {
            throw new NetworkRuleConflictException("Can't do static nat on ip address: " + ipAddress.getAddress());
        } 
        
        _firewallMgr.validateFirewallRule(caller, ipAddress, rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol(), Purpose.StaticNat);
        
        Long networkId = ipAddress.getAssociatedWithNetworkId();
        Long accountId = ipAddress.getAccountId();
        Long domainId = ipAddress.getDomainId();


        String dstIp = _networkMgr.getIpInNetwork(ipAddress.getAssociatedWithVmId(), networkId);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        

        FirewallRuleVO newRule = new FirewallRuleVO(rule.getXid(), rule.getSourceIpAddressId(), rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol().toLowerCase(), 
                networkId, accountId, domainId, rule.getPurpose(), null, null, null, null);

        newRule = _firewallDao.persist(newRule);
        
        //create firewallRule for 0.0.0.0/0 cidr
        if (openFirewall) {
            _firewallMgr.createRuleForAllCidrs(ipAddrId, caller, rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol(), null, null, newRule.getId());
        }
        

        try {
            _firewallMgr.detectRulesConflict(newRule, ipAddress);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            UserContext.current().setEventDetails("Rule Id: " + newRule.getId());
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_ADD, newRule.getAccountId(), 0, newRule.getId(), null);
            _usageEventDao.persist(usageEvent);

            txn.commit();
            StaticNatRule staticNatRule = new StaticNatRuleImpl(newRule, dstIp);

            return staticNatRule;
        } catch (Exception e) {
            
            if (newRule != null) {
                txn.start();
                //no need to apply the rule as it wasn't programmed on the backend yet
                _firewallMgr.revokeRelatedFirewallRule(newRule.getId(), false);
                _forwardingDao.remove(newRule.getId()); 
                txn.commit();
            }
           
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            }
            throw new CloudRuntimeException("Unable to add static nat rule for the ip id=" + newRule.getSourceIpAddressId(), e);
        }
    }

    @Override
    public boolean enableStaticNat(long ipId, long vmId) throws NetworkRuleConflictException {

        Account caller = UserContext.current().getCaller();

        // Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Can't enable static nat for the address id=" + ipId + ", invalid virtual machine id specified (" + vmId + ").");
        }

        IPAddressVO ipAddress = _ipAddressDao.findById(ipId);
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id " + ipId);
        }

        // Check permissions
        checkIpAndUserVm(ipAddress, vm, caller);

        // Verify that the ip is associated with the network and firewallService is supported for the network
        Long networkId = ipAddress.getAssociatedWithNetworkId();
        if (networkId == null) {
            throw new InvalidParameterValueException("Unable to enable static nat for the ipAddress id=" + ipId + " as ip is not associated with any network");
        }

        // Check that vm has a nic in the network
        Nic guestNic = _networkMgr.getNicInNetwork(vmId, networkId);
        if (guestNic == null) {
            throw new InvalidParameterValueException("Vm doesn't belong to the network " + networkId);
        }

        Network network = _networkMgr.getNetwork(networkId);
        if (!_networkMgr.isServiceSupported(network.getNetworkOfferingId(), Service.Firewall)) {
            throw new InvalidParameterValueException("Unable to create static nat rule; Firewall service is not supported in network id=" + networkId);
        }

        // Verify ip address parameter
        if (ipAddress.isSourceNat()) {
            throw new InvalidParameterValueException("Can't enable static, ip address id=" + ipId + " is a sourceNat ip address");
        }

        if (!ipAddress.isOneToOneNat()) {
            List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.PortForwarding);
            if (rules != null && !rules.isEmpty()) {
                throw new NetworkRuleConflictException("Failed to enable static nat for the ip address id=" + ipId + " as it already has firewall rules assigned");
            }
        } else {
            if (ipAddress.getAssociatedWithVmId() != null && ipAddress.getAssociatedWithVmId().longValue() != vmId) {
                throw new NetworkRuleConflictException("Failed to enable static for the ip address id=" + ipId + " and vm id=" + vmId + " as it's already assigned to antoher vm");
            }
        }

        // If there is public ip address already associated with the vm, throw an exception
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(vmId);

        if (ip != null) {
            throw new InvalidParameterValueException("Failed to enable static nat for the ip address id=" + ipId + " as vm id=" + " is already associated with ip id=" + ip.getId());
        }

        ipAddress.setOneToOneNat(true);
        ipAddress.setAssociatedWithVmId(vmId);

        if (_ipAddressDao.update(ipAddress.getId(), ipAddress)) {
            //enable static nat on the backend
            s_logger.trace("Enabling static nat for ip address " + ipAddress + " and vm id=" + vmId + " on the backend");
            if (applyStaticNatForIp(ipId, false, caller, false)) {
                return true;
            } else {
                ipAddress.setOneToOneNat(false);
                ipAddress.setAssociatedWithVmId(null);
                _ipAddressDao.update(ipAddress.getId(), ipAddress);
                s_logger.warn("Failed to enable static nat rule for ip address " + ipId + " on the backend");
                return false;
            }
        } else {
            s_logger.warn("Failed to update ip address " + ipAddress + " in the DB as a part of enableStaticNat");
            return false;
        }
    }


    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_RULE_DELETE, eventDescription = "revoking forwarding rule", async = true)
    public boolean revokePortForwardingRule(long ruleId, boolean apply) {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        PortForwardingRuleVO rule = _forwardingDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find " + ruleId);
        }

        _accountMgr.checkAccess(caller, null, rule);

        return revokePortForwardingRuleInternal(ruleId, caller, ctx.getCallerUserId(), apply);
    }

    private boolean revokePortForwardingRuleInternal(long ruleId, Account caller, long userId, boolean apply) {
        PortForwardingRuleVO rule = _forwardingDao.findById(ruleId);

        _firewallMgr.revokeRule(rule, caller, userId, true);

        boolean success = false;

        if (apply) {
            success = applyPortForwardingRules(rule.getSourceIpAddressId(), true, caller);
        } else {
            success = true;
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_RULE_DELETE, eventDescription = "revoking forwarding rule", async = true)
    public boolean revokeStaticNatRule(long ruleId, boolean apply) {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        FirewallRuleVO rule = _firewallDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find " + ruleId);
        }

        _accountMgr.checkAccess(caller, null, rule);

        return revokeStaticNatRuleInternal(ruleId, caller, ctx.getCallerUserId(), apply);
    }

    private boolean revokeStaticNatRuleInternal(long ruleId, Account caller, long userId, boolean apply) {
        FirewallRuleVO rule = _firewallDao.findById(ruleId);

        _firewallMgr.revokeRule(rule, caller, userId, true);

        boolean success = false;

        if (apply) {
            success = applyStaticNatRules(rule.getSourceIpAddressId(), true, caller);
        } else {
            success = true;
        }

        return success;
    }

    @Override
    public boolean revokePortForwardingRulesForVm(long vmId) {
        boolean success = true;
        UserVmVO vm = _vmDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            return false;
        }

        List<PortForwardingRuleVO> rules = _forwardingDao.listByVm(vmId);
        Set<Long> ipsToReprogram = new HashSet<Long>();

        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No port forwarding rules are found for vm id=" + vmId);
            return true;
        }

        for (PortForwardingRuleVO rule : rules) {
            // Mark port forwarding rule as Revoked, but don't revoke it yet (apply=false)
            revokePortForwardingRuleInternal(rule.getId(), _accountMgr.getSystemAccount(), Account.ACCOUNT_ID_SYSTEM, false);
            ipsToReprogram.add(rule.getSourceIpAddressId());
        }

        // apply rules for all ip addresses
        for (Long ipId : ipsToReprogram) {
            s_logger.debug("Applying port forwarding rules for ip address id=" + ipId + " as a part of vm expunge");
            if (!applyPortForwardingRules(ipId, true, _accountMgr.getSystemAccount())) {
                s_logger.warn("Failed to apply port forwarding rules for ip id=" + ipId);
                success = false;
            }
        }

        return success;
    }
    
    @Override
    public boolean revokeStaticNatRulesForVm(long vmId) {
        boolean success = true;

        UserVmVO vm = _vmDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            return false;
        }

        List<FirewallRuleVO> rules = _firewallDao.listStaticNatByVmId(vm.getId());
        Set<Long> ipsToReprogram = new HashSet<Long>();

        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No static nat rules are found for vm id=" + vmId);
            return true;
        }

        for (FirewallRuleVO rule : rules) {
            // mark static nat as Revoked, but don't revoke it yet (apply = false)
            revokeStaticNatRuleInternal(rule.getId(), _accountMgr.getSystemAccount(), Account.ACCOUNT_ID_SYSTEM, false);
            ipsToReprogram.add(rule.getSourceIpAddressId());
        }

        // apply rules for all ip addresses
        for (Long ipId : ipsToReprogram) {
            s_logger.debug("Applying static nat rules for ip address id=" + ipId + " as a part of vm expunge");
            if (!applyStaticNatRules(ipId, true, _accountMgr.getSystemAccount())) {
                success = false;
                s_logger.warn("Failed to apply static nat rules for ip id=" + ipId);
            }
        }

        return success;
    }

    @Override
    public List<? extends PortForwardingRule> listPortForwardingRulesForApplication(long ipId) {
        return _forwardingDao.listForApplication(ipId);
    }

    @Override
    public List<? extends PortForwardingRule> listPortForwardingRules(ListPortForwardingRulesCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long ipId = cmd.getIpAddressId();
        Long id = cmd.getId();
        String path = null;

        Pair<List<Long>, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        List<Long> permittedAccounts = accountDomainPair.first();
        Long domainId = accountDomainPair.second();

        if (ipId != null) {
            IPAddressVO ipAddressVO = _ipAddressDao.findById(ipId);
            if (ipAddressVO == null || !ipAddressVO.readyToUse()) {
                throw new InvalidParameterValueException("Ip address id=" + ipId + " not ready for port forwarding rules yet");
            }
            _accountMgr.checkAccess(caller, null, ipAddressVO);
        }

        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            Domain domain = _domainMgr.getDomain(caller.getDomainId());
            path = domain.getPath();
        }

        Filter filter = new Filter(PortForwardingRuleVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<PortForwardingRuleVO> sb = _forwardingDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("ip", sb.entity().getSourceIpAddressId(), Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), Op.EQ);

        if (path != null) {
            // for domain admin we should show only subdomains information
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<PortForwardingRuleVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (ipId != null) {
            sc.setParameters("ip", ipId);
        }

        if (domainId != null) {
            sc.setParameters("domainId", domainId);
        }
        
        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountId", permittedAccounts.toArray());
        }

        sc.setParameters("purpose", Purpose.PortForwarding);

        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
        }

        return _forwardingDao.search(sc, filter);
    }
    
    @Override
    public List<String> getSourceCidrs(long ruleId){
        return _firewallCidrsDao.getSourceCidrs(ruleId);
    }

    @Override
    public boolean applyPortForwardingRules(long ipId, boolean continueOnError, Account caller) {
        List<PortForwardingRuleVO> rules = _forwardingDao.listForApplication(ipId);
        
        if (rules.size() == 0) {
            s_logger.debug("There are no firwall rules to apply for ip id=" + ipId);
            return true;
        }
        

        if (caller != null) {
            _accountMgr.checkAccess(caller, null, rules.toArray(new PortForwardingRuleVO[rules.size()]));
        }

        try {
            if (!_firewallMgr.applyRules(rules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to ", ex);
            return false;
        }

        return true;
    }

    @Override
    public boolean applyStaticNatRules(long sourceIpId, boolean continueOnError, Account caller) {
        List<? extends FirewallRule> rules = _firewallDao.listByIpAndPurpose(sourceIpId, Purpose.StaticNat);
        List<StaticNatRule> staticNatRules = new ArrayList<StaticNatRule>();

        if (rules.size() == 0) {
            s_logger.debug("There are no firwall rules to apply for ip id=" + sourceIpId);
            return true;
        }

        for (FirewallRule rule : rules) {
            IpAddress sourceIp = _ipAddressDao.findById(rule.getSourceIpAddressId());

            UserVmVO vm = _vmDao.findById(sourceIp.getAssociatedWithVmId());

            Long networkId = sourceIp.getAssociatedWithNetworkId();
            if (networkId == null) {
                throw new CloudRuntimeException("Ip address is not associated with any network");
            }

            Network network = _networkMgr.getNetwork(networkId);

            if (network == null) {
                throw new CloudRuntimeException("Unable to find ip address to map to in vm id=" + vm.getId());
            }

            Nic guestNic = _networkMgr.getNicInNetworkIncludingRemoved(vm.getId(), networkId);
            FirewallRuleVO ruleVO = _firewallDao.findById(rule.getId());

            staticNatRules.add(new StaticNatRuleImpl(ruleVO, guestNic.getIp4Address()));
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, null, staticNatRules.toArray(new StaticNatRule[staticNatRules.size()]));
        }

        try {
            if (!_firewallMgr.applyRules(staticNatRules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply static nat rules due to ", ex);
            return false;
        }

        return true;
    }

    @Override
    public boolean applyPortForwardingRulesForNetwork(long networkId, boolean continueOnError, Account caller) {
        List<PortForwardingRuleVO> rules = listByNetworkId(networkId);
        if (rules.size() == 0) {
            s_logger.debug("There are no port forwarding rules to apply for network id=" + networkId);
            return true;
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, null, rules.toArray(new PortForwardingRuleVO[rules.size()]));
        }

        try {
            if (!_firewallMgr.applyRules(rules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to ", ex);
            return false;
        }

        return true;
    }

    @Override
    public boolean applyStaticNatRulesForNetwork(long networkId, boolean continueOnError, Account caller) {
        List<FirewallRuleVO> rules = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.StaticNat);
        List<StaticNatRule> staticNatRules = new ArrayList<StaticNatRule>();

        if (rules.size() == 0) {
            s_logger.debug("There are no static nat rules to apply for network id=" + networkId);
            return true;
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, null, rules.toArray(new FirewallRule[rules.size()]));
        }

        for (FirewallRuleVO rule : rules) {
            staticNatRules.add(buildStaticNatRule(rule));
        }

        try {
            if (!_firewallMgr.applyRules(staticNatRules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to ", ex);
            return false;
        }

        return true;
    }


    @Override
    public boolean applyStaticNatsForNetwork(long networkId, boolean continueOnError, Account caller) {
        List<IPAddressVO> ips = _ipAddressDao.listStaticNatPublicIps(networkId);
        if (ips.isEmpty()) {
            s_logger.debug("There are no static nat to apply for network id=" + networkId);
            return true;
        }
        
        if (caller != null) {
            _accountMgr.checkAccess(caller, null, ips.toArray(new IPAddressVO[ips.size()]));
        }
        
        List<StaticNat> staticNats = new ArrayList<StaticNat>();
        for (IPAddressVO ip : ips) {
            // Get nic IP4 address
            String dstIp = _networkMgr.getIpInNetwork(ip.getAssociatedWithVmId(), networkId);
            StaticNatImpl staticNat = new StaticNatImpl(ip.getAccountId(), ip.getDomainId(), networkId, ip.getId(), dstIp, false);
            staticNats.add(staticNat);
        }
        
        try {
            if (!_networkMgr.applyStaticNats(staticNats, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to create static nat rule due to ", ex);
            return false;
        }

        return true;
    }

    @Override
    public List<? extends FirewallRule> searchStaticNatRules(Long ipId, Long id, Long vmId, Long start, Long size, String accountName, Long domainId, Long projectId) {
        Account caller = UserContext.current().getCaller();
        String path = null;

        Pair<List<Long>, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, accountName, domainId, projectId);
        List<Long> permittedAccounts = accountDomainPair.first();
        domainId = accountDomainPair.second();

        if (ipId != null) {
            IPAddressVO ipAddressVO = _ipAddressDao.findById(ipId);
            if (ipAddressVO == null || !ipAddressVO.readyToUse()) {
                throw new InvalidParameterValueException("Ip address id=" + ipId + " not ready for port forwarding rules yet");
            }
            _accountMgr.checkAccess(caller, null, ipAddressVO);
        }

        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            Domain domain = _domainMgr.getDomain(caller.getDomainId());
            path = domain.getPath();
        }

        Filter filter = new Filter(PortForwardingRuleVO.class, "id", false, start, size);
        SearchBuilder<FirewallRuleVO> sb = _firewallDao.createSearchBuilder();
        sb.and("ip", sb.entity().getSourceIpAddressId(), Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), Op.EQ);
        sb.and("id", sb.entity().getId(), Op.EQ);

        if (path != null) {
            // for domain admin we should show only subdomains information
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
        }
        
        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountId", permittedAccounts.toArray());
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

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_RULE_ADD, eventDescription = "applying port forwarding rule", async = true)
    public boolean applyPortForwardingRules(long ipId, Account caller) throws ResourceUnavailableException {
        return applyPortForwardingRules(ipId, false, caller);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_RULE_ADD, eventDescription = "applying static nat rule", async = true)
    public boolean applyStaticNatRules(long ipId, Account caller) throws ResourceUnavailableException {
        return applyStaticNatRules(ipId, false, caller);
    }

    @Override
    public boolean revokeAllPFAndStaticNatRulesForIp(long ipId, long userId, Account caller) throws ResourceUnavailableException {
        List<FirewallRule> rules = new ArrayList<FirewallRule>();

        List<PortForwardingRuleVO> pfRules = _forwardingDao.listByIpAndNotRevoked(ipId);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + pfRules.size() + " port forwarding rules for ip id=" + ipId);
        }

        for (PortForwardingRuleVO rule : pfRules) {
            // Mark all PF rules as Revoke, but don't revoke them yet
            revokePortForwardingRuleInternal(rule.getId(), caller, userId, false);
        }

        List<FirewallRuleVO> staticNatRules = _firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.StaticNat);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + staticNatRules.size() + " static nat rules for ip id=" + ipId);
        }

        
        for (FirewallRuleVO rule : staticNatRules) {
            // Mark all static nat rules as Revoke, but don't revoke them yet
            revokeStaticNatRuleInternal(rule.getId(), caller, userId, false);
        }

        //revoke static nat for the ip address
        boolean success = applyStaticNatForIp(ipId, false, caller, true);

        // revoke all port forwarding rules
        success = success && applyPortForwardingRules(ipId, true, caller);

        // revoke all all static nat rules
        success  = success && applyStaticNatRules(ipId, true, caller);

        // Now we check again in case more rules have been inserted.
        rules.addAll(_forwardingDao.listByIpAndNotRevoked(ipId));
        rules.addAll(_firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.StaticNat));

        if (s_logger.isDebugEnabled() && success) {
            s_logger.debug("Successfully released rules for ip id=" + ipId + " and # of rules now = " + rules.size());
        }

        return (rules.size() == 0 && success);
    }

    @Override
    public boolean revokeAllRulesForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException {
        List<FirewallRule> rules = new ArrayList<FirewallRule>();

        List<PortForwardingRuleVO> pfRules = _forwardingDao.listByNetwork(networkId);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + pfRules.size() + " port forwarding rules for network id=" + networkId);
        }

        List<FirewallRuleVO> staticNatRules = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.StaticNat);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + staticNatRules.size() + " static nat rules for network id=" + networkId);
        }

        // Mark all pf rules (Active and non-Active) to be revoked, but don't revoke it yet - pass apply=false
        for (PortForwardingRuleVO rule : pfRules) {
            revokePortForwardingRuleInternal(rule.getId(), caller, userId, false);
        }

        // Mark all static nat rules (Active and non-Active) to be revoked, but don't revoke it yet - pass apply=false
        for (FirewallRuleVO rule : staticNatRules) {
            revokeStaticNatRuleInternal(rule.getId(), caller, userId, false);
        }

        // revoke all PF rules for the network
        applyPortForwardingRulesForNetwork(networkId, true, caller);

        // revoke all all static nat rules for the network
        applyStaticNatRulesForNetwork(networkId, true, caller);

        // Now we check again in case more rules have been inserted.
        rules.addAll(_forwardingDao.listByNetworkAndNotRevoked(networkId));
        rules.addAll(_firewallDao.listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.StaticNat));

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released rules for network id=" + networkId + " and # of rules now = " + rules.size());
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

    @Override
    @DB
    public FirewallRuleVO[] reservePorts(IpAddress ip, String protocol, FirewallRule.Purpose purpose, boolean openFirewall, Account caller, int... ports) throws NetworkRuleConflictException {
        FirewallRuleVO[] rules = new FirewallRuleVO[ports.length];

        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (int i = 0; i < ports.length; i++) {
            
            rules[i] = new FirewallRuleVO(null, ip.getId(), ports[i], protocol, ip.getAssociatedWithNetworkId(), ip.getAllocatedToAccountId(), ip.getAllocatedInDomainId(), purpose, null, null, null, null);
            rules[i] = _firewallDao.persist(rules[i]);
            
            if (openFirewall) {
                _firewallMgr.createRuleForAllCidrs(ip.getId(), caller, ports[i], ports[i], protocol, null, null, rules[i].getId());
            }
        }
        txn.commit();

        boolean success = false;
        try {
            for (FirewallRuleVO newRule : rules) {
                _firewallMgr.detectRulesConflict(newRule, ip);
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
        return _forwardingDao.listByNetwork(networkId);
    }

    @Override
    public boolean disableStaticNat(long ipId) throws ResourceUnavailableException {
        boolean success = true;

        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        IPAddressVO ipAddress = _ipAddressDao.findById(ipId);
        checkIpAndUserVm(ipAddress, null, caller);

        if (!ipAddress.isOneToOneNat()) {
            throw new InvalidParameterValueException("One to one nat is not enabled for the ip id=" + ipId);
        }
        
        //Revoke all firewall rules for the ip
        try {
            s_logger.debug("Revoking all " + Purpose.Firewall + "rules as a part of public IP id=" + ipId + " release...");
            if (!_firewallMgr.revokeFirewallRulesForIp(ipId, ctx.getCallerUserId(), caller)) {
                s_logger.warn("Unable to revoke all the firewall rules for ip id=" + ipId + " as a part of disable statis nat");
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to revoke all firewall rules for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }

        if (!revokeAllPFAndStaticNatRulesForIp(ipId, UserContext.current().getCallerUserId(), caller)) {
            s_logger.warn("Unable to revoke all static nat rules for ip " + ipAddress);
            success = false;
        }
        
        if (success) {
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

    @Override
    public StaticNatRule buildStaticNatRule(FirewallRule rule) {
        IpAddress ip = _ipAddressDao.findById(rule.getSourceIpAddressId());
        FirewallRuleVO ruleVO = _firewallDao.findById(rule.getId());

        if (ip == null || !ip.isOneToOneNat() || ip.getAssociatedWithVmId() == null) {
            throw new InvalidParameterValueException("Source ip address of the rule id=" + rule.getId() + " is not static nat enabled");
        }

        String dstIp = _networkMgr.getIpInNetwork(ip.getAssociatedWithVmId(), rule.getNetworkId());

        return new StaticNatRuleImpl(ruleVO, dstIp);
    }
    
    @Override
    public boolean applyStaticNatForIp(long sourceIpId, boolean continueOnError, Account caller, boolean forRevoke) {
        
        List<StaticNat> staticNats = new ArrayList<StaticNat>();
        IpAddress sourceIp = _ipAddressDao.findById(sourceIpId);
        
        if (!sourceIp.isOneToOneNat()) {
            s_logger.debug("Source ip id=" + sourceIpId + " is not one to one nat");
            return true;
        }

        Long networkId = sourceIp.getAssociatedWithNetworkId();
        if (networkId == null) {
            throw new CloudRuntimeException("Ip address is not associated with any network");
        }
        
        UserVmVO vm = _vmDao.findById(sourceIp.getAssociatedWithVmId());
        Network network = _networkMgr.getNetwork(networkId);
        if (network == null) {
            throw new CloudRuntimeException("Unable to find ip address to map to in vm id=" + vm.getId());
        }
        
        if (caller != null) {
            _accountMgr.checkAccess(caller, null, sourceIp);
        }

        //create new static nat rule
        // Get nic IP4 address
        
        String dstIp;
        if (forRevoke) {
            dstIp = _networkMgr.getIpInNetworkIncludingRemoved(sourceIp.getAssociatedWithVmId(), networkId);
        } else {
            dstIp = _networkMgr.getIpInNetwork(sourceIp.getAssociatedWithVmId(), networkId);
        }
        
        StaticNatImpl staticNat = new StaticNatImpl(sourceIp.getAccountId(), sourceIp.getDomainId(), networkId, sourceIpId, dstIp, forRevoke);
        staticNats.add(staticNat);
        
        try {
            if (!_networkMgr.applyStaticNats(staticNats, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to create static nat rule due to ", ex);
            return false;
        }

        return true;
    }
}
