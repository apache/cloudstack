/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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

package com.cloud.network.firewall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.commands.ListFirewallRulesCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
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
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.UserContext;
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
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

@Local(value = { FirewallService.class, FirewallManager.class })
public class FirewallManagerImpl implements FirewallService, FirewallManager, Manager{
    private static final Logger s_logger = Logger.getLogger(FirewallManagerImpl.class);
    String _name;

    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    EventDao _eventDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    FirewallRulesCidrsDao _firewallCidrsDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkManager _networkMgr;
    @Inject 
    UsageEventDao _usageEventDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    DomainManager _domainMgr;
    @Inject
    PortForwardingRulesDao _pfRulesDao;
    @Inject
    UserVmDao _vmDao;
    
    private boolean _elbEnabled=false;
    
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
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        String elbEnabledString = _configDao.getValue(Config.ElasticLoadBalancerEnabled.key());
        _elbEnabled = Boolean.parseBoolean(elbEnabledString);
        return true;
    }
    
    @Override
    public FirewallRule createFirewallRule(FirewallRule rule) throws NetworkRuleConflictException {
        Account caller = UserContext.current().getCaller();

        return createFirewallRule(rule.getSourceIpAddressId(), caller, rule.getXid(), rule.getSourcePortStart() ,rule.getSourcePortEnd(), rule.getProtocol(), rule.getSourceCidrList(), rule.getIcmpCode(), rule.getIcmpType(), null, rule.getType());    
    }
    
    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating firewll rule", create = true)
    public FirewallRule createFirewallRule(long ipAddrId, Account caller, String xId, Integer portStart,Integer portEnd, String protocol, List<String> sourceCidrList, Integer icmpCode, Integer icmpType, Long relatedRuleId, FirewallRule.FirewallRuleType type) throws NetworkRuleConflictException{
        IPAddressVO ipAddress = _ipAddressDao.findById(ipAddrId);
        
        // Validate ip address
        if (ipAddress == null && type == FirewallRule.FirewallRuleType.User) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule; ip id=" + ipAddrId + " doesn't exist in the system");
        } 
        
        validateFirewallRule(caller, ipAddress, portStart, portEnd, protocol, Purpose.Firewall, type);

        //icmp code and icmp type can't be passed in for any other protocol rather than icmp
        if (!protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (icmpCode != null || icmpType != null)) {
            throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
        }
        
        if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (portStart != null || portEnd != null)) {
            throw new InvalidParameterValueException("Can't specify start/end port when protocol is ICMP");
        }
        
        Long networkId = null;
        Long accountId = null;
        Long domainId = null;
        
        if (ipAddress != null) {
        	networkId = ipAddress.getAssociatedWithNetworkId();
        	accountId = ipAddress.getAccountId();
        	domainId = ipAddress.getDomainId();
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        FirewallRuleVO newRule = new FirewallRuleVO (xId, ipAddrId, portStart, portEnd, protocol.toLowerCase(), networkId, accountId, domainId, Purpose.Firewall, sourceCidrList, icmpCode, icmpType, relatedRuleId);
        newRule.setType(type);
        newRule = _firewallDao.persist(newRule);

        if (type == FirewallRuleType.User)
        	detectRulesConflict(newRule, ipAddress);
        
        if (!_firewallDao.setStateToAdd(newRule)) {
            throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
        }
        UserContext.current().setEventDetails("Rule Id: " + newRule.getId());
        
        txn.commit();

        return newRule;
    }
    
    @Override
    public List<? extends FirewallRule> listFirewallRules(ListFirewallRulesCmd cmd) {
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
                throw new InvalidParameterValueException("Ip address id=" + ipId + " not ready for firewall rules yet");
            }
            _accountMgr.checkAccess(caller, null, ipAddressVO);
        }

        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            Domain domain = _domainMgr.getDomain(caller.getDomainId());
            path = domain.getPath();
        }

        Filter filter = new Filter(FirewallRuleVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<FirewallRuleVO> sb = _firewallDao.createSearchBuilder();
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

        sc.setParameters("purpose", Purpose.Firewall);

        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
        }

        return _firewallDao.search(sc, filter);
    }
    
    @Override
    public void detectRulesConflict(FirewallRule newRule, IpAddress ipAddress) throws NetworkRuleConflictException {
        assert newRule.getSourceIpAddressId() == ipAddress.getId() : "You passed in an ip address that doesn't match the address in the new rule";

        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurposeAndNotRevoked(newRule.getSourceIpAddressId(), null);
        assert (rules.size() >= 1) : "For network rules, we now always first persist the rule and then check for network conflicts so we should at least have one rule at this point.";

        for (FirewallRuleVO rule : rules) {
            if (rule.getId() == newRule.getId()) {
                continue; // Skips my own rule.
            }
            
            boolean oneOfRulesIsFirewall = ((rule.getPurpose() == Purpose.Firewall || newRule.getPurpose() == Purpose.Firewall) && ((newRule.getPurpose() != rule.getPurpose()) || (!newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()))));
            
            //if both rules are firewall and their cidrs are different, we can skip port ranges verification
            boolean bothRulesFirewall = (rule.getPurpose() == newRule.getPurpose() && rule.getPurpose() == Purpose.Firewall);
            boolean duplicatedCidrs = false;
            if (bothRulesFirewall) {
                //Verify that the rules have different cidrs
                List<String> ruleCidrList = rule.getSourceCidrList();
                List<String> newRuleCidrList = newRule.getSourceCidrList();
                
                if (ruleCidrList == null || newRuleCidrList == null) {
                    continue;
                }
                
                Collection<String> similar = new HashSet<String>(ruleCidrList);
                similar.retainAll(newRuleCidrList);

                if (similar.size() > 0) {
                    duplicatedCidrs = true;
                }
            }
            
            if (!oneOfRulesIsFirewall) {
                if (rule.getPurpose() == Purpose.StaticNat && newRule.getPurpose() != Purpose.StaticNat) {
                    throw new NetworkRuleConflictException("There is 1 to 1 Nat rule specified for the ip address id=" + newRule.getSourceIpAddressId());
                } else if (rule.getPurpose() != Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat) {
                    throw new NetworkRuleConflictException("There is already firewall rule specified for the ip address id=" + newRule.getSourceIpAddressId());
                }
            }

            if (rule.getNetworkId() != newRule.getNetworkId() && rule.getState() != State.Revoke) {
                throw new NetworkRuleConflictException("New rule is for a different network than what's specified in rule " + rule.getXid());
            }  
            
            if (newRule.getProtocol().equalsIgnoreCase(NetUtils.ICMP_PROTO) && newRule.getProtocol().equalsIgnoreCase(rule.getProtocol())) {
                if (newRule.getIcmpCode().longValue() == rule.getIcmpCode().longValue() && newRule.getIcmpType().longValue() == rule.getIcmpType().longValue() && newRule.getProtocol().equalsIgnoreCase(rule.getProtocol())) {
                    throw new InvalidParameterValueException("New rule conflicts with existing rule id=" + rule.getId());
                }
            }

            boolean notNullPorts = (newRule.getSourcePortStart() != null && newRule.getSourcePortEnd() != null && rule.getSourcePortStart() != null && rule.getSourcePortEnd() != null) ;
            if (!notNullPorts) {
                continue;
            } else if (!oneOfRulesIsFirewall && !(bothRulesFirewall && !duplicatedCidrs) && ((rule.getSourcePortStart().intValue() <= newRule.getSourcePortStart().intValue() && rule.getSourcePortEnd().intValue() >= newRule.getSourcePortStart().intValue())
                    || (rule.getSourcePortStart().intValue() <= newRule.getSourcePortEnd().intValue() && rule.getSourcePortEnd().intValue() >= newRule.getSourcePortEnd().intValue())
                    || (newRule.getSourcePortStart().intValue() <= rule.getSourcePortStart().intValue() && newRule.getSourcePortEnd().intValue() >= rule.getSourcePortStart().intValue())
                    || (newRule.getSourcePortStart().intValue() <= rule.getSourcePortEnd().intValue() && newRule.getSourcePortEnd().intValue() >= rule.getSourcePortEnd().intValue()))) {

                // we allow port forwarding rules with the same parameters but different protocols
                boolean allowPf = (rule.getPurpose() == Purpose.PortForwarding && newRule.getPurpose() == Purpose.PortForwarding && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));
                boolean allowStaticNat = (rule.getPurpose() == Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));
                
                if (!(allowPf || allowStaticNat || oneOfRulesIsFirewall)) {
                    throw new NetworkRuleConflictException("The range specified, " + newRule.getSourcePortStart() + "-" + newRule.getSourcePortEnd() + ", conflicts with rule " + rule.getId()
                            + " which has " + rule.getSourcePortStart() + "-" + rule.getSourcePortEnd());
                }
            } 
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("No network rule conflicts detected for " + newRule + " against " + (rules.size() - 1) + " existing rules");
        }
    }

    
    @Override
    public void validateFirewallRule(Account caller, IPAddressVO ipAddress, Integer portStart, Integer portEnd, String proto, Purpose purpose, FirewallRuleType type) {
    	 if (portStart != null && !NetUtils.isValidPort(portStart)) {
             throw new InvalidParameterValueException("publicPort is an invalid value: " + portStart);
         }
         if (portEnd != null && !NetUtils.isValidPort(portEnd)) {
             throw new InvalidParameterValueException("Public port range is an invalid value: " + portEnd);
         }
         
         // start port can't be bigger than end port
         if (portStart != null && portEnd != null && portStart > portEnd) {
             throw new InvalidParameterValueException("Start port can't be bigger than end port");
         }
         
         if (ipAddress == null && type == FirewallRuleType.System) {
        	 return;
         }
         
        // Validate ip address
        _accountMgr.checkAccess(caller, null, ipAddress);

        Long networkId = ipAddress.getAssociatedWithNetworkId();
        if (networkId == null) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule ; ip id=" + ipAddress.getId() + " is not associated with any network");

        }
        
        Network network = _networkMgr.getNetwork(networkId);
        assert network != null : "Can't create port forwarding rule as network associated with public ip address is null?";
        
       
        // Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> protocolCapabilities = null;
        
        if (purpose == Purpose.LoadBalancing) {
            if (!_elbEnabled) {
                protocolCapabilities = _networkMgr.getNetworkServiceCapabilities(network.getId(), Service.Lb);
            }
        } else {
            protocolCapabilities = _networkMgr.getNetworkServiceCapabilities(network.getId(), Service.Firewall);
        }

        if (protocolCapabilities != null) {
        	String supportedProtocols = protocolCapabilities.get(Capability.SupportedProtocols).toLowerCase();
        	if (!supportedProtocols.contains(proto.toLowerCase())) {
        		throw new InvalidParameterValueException("Protocol " + proto + " is not supported in zone " + network.getDataCenterId());
        	} else if (proto.equalsIgnoreCase(NetUtils.ICMP_PROTO) && purpose != Purpose.Firewall) {
        		throw new InvalidParameterValueException("Protocol " + proto + " is currently supported only for rules with purpose " + Purpose.Firewall);
        	}
        }
    }
    
    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError, boolean updateRulesInDB) throws ResourceUnavailableException {
        boolean success = true;
        if (!_networkMgr.applyRules(rules, continueOnError)) {
            s_logger.warn("Rules are not completely applied");
            return false;
        } else {
            if (updateRulesInDB) {
                for (FirewallRule rule : rules) {
                    if (rule.getState() == FirewallRule.State.Revoke) {
                        FirewallRuleVO relatedRule = _firewallDao.findByRelatedId(rule.getId());
                        if (relatedRule != null) {
                            s_logger.warn("Can't remove the firewall rule id=" + rule.getId() + " as it has related firewall rule id=" + relatedRule.getId() + "; leaving it in Revoke state");
                            success = false;
                        } else {
                            _firewallDao.remove(rule.getId());
                        }
                    } else if (rule.getState() == FirewallRule.State.Add) {
                        FirewallRuleVO ruleVO = _firewallDao.findById(rule.getId());
                        ruleVO.setState(FirewallRule.State.Active);
                        _firewallDao.update(ruleVO.getId(), ruleVO);
                    }
                }
            }
        }
        
        return success;
    }
    
    @Override
    public boolean applyFirewallRules(long ipId, Account caller) throws ResourceUnavailableException {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurpose(ipId, Purpose.Firewall);
        return applyFirewallRules(rules, false, caller);
    }
    
    
    @Override
    public boolean applyFirewallRules(List<FirewallRuleVO> rules, boolean continueOnError, Account caller) {
        
        if (rules.size() == 0) {
            s_logger.debug("There are no firewall rules to apply for ip id=" + rules);
            return true;
        }
        
        for (FirewallRuleVO rule: rules){
            // load cidrs if any
            rule.setSourceCidrList(_firewallCidrsDao.getSourceCidrs(rule.getId()));  
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, null, rules.toArray(new FirewallRuleVO[rules.size()]));
        }

        try {
            if (!applyRules(rules, continueOnError, true)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to ", ex);
            return false;
        }

        return true;
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_CLOSE, eventDescription = "revoking firewall rule", async = true)
    public boolean revokeFirewallRule(long ruleId, boolean apply, Account caller, long userId) {

        FirewallRuleVO rule = _firewallDao.findById(ruleId);
        if (rule == null || rule.getPurpose() != Purpose.Firewall) {
            throw new InvalidParameterValueException("Unable to find " + ruleId + " having purpose " + Purpose.Firewall);
        }
        
        if (rule.getType() == FirewallRuleType.System && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
        	throw new InvalidParameterValueException("Only root admin can delete the system wide firewall rule");
        }

        _accountMgr.checkAccess(caller, null, rule);
        
        revokeRule(rule, caller, userId, false);

        boolean success = false;

        if (apply) {
            List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurpose(rule.getSourceIpAddressId(), Purpose.Firewall);
            return applyFirewallRules(rules, false, caller);
        } else {
            success = true;
        }

        return success;

    }
    
    @Override
    public boolean revokeFirewallRule(long ruleId, boolean apply) {
        Account caller = UserContext.current().getCaller();
        long userId = UserContext.current().getCallerUserId();
        return revokeFirewallRule(ruleId, apply, caller, userId);
    }
    
    @Override 
    @DB
    public void revokeRule(FirewallRuleVO rule, Account caller, long userId, boolean needUsageEvent) {
        if (caller != null) {
            _accountMgr.checkAccess(caller, null, rule);
        }

        Transaction txn = Transaction.currentTxn();
        boolean generateUsageEvent = false;

        txn.start();
        if (rule.getState() == State.Staged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a rule that is still in stage state so just removing it: " + rule);
            }
            _firewallDao.remove(rule.getId());
            generateUsageEvent = true;
        } else if (rule.getState() == State.Add || rule.getState() == State.Active) {
            rule.setState(State.Revoke);
            _firewallDao.update(rule.getId(), rule);
            generateUsageEvent = true;
        }

        if (generateUsageEvent && needUsageEvent) {
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_DELETE, rule.getAccountId(), 0, rule.getId(), null);
            _usageEventDao.persist(usageEvent);
        }
        
        txn.commit();
    }

    
    @Override
    public FirewallRule getFirewallRule(long ruleId) {
        return _firewallDao.findById(ruleId);
    }
    
    @Override
    public boolean revokeFirewallRulesForIp(long ipId, long userId, Account caller) throws ResourceUnavailableException {
        List<FirewallRule> rules = new ArrayList<FirewallRule>();

        List<FirewallRuleVO> fwRules = _firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.Firewall);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + fwRules.size() + " firewall rules for ip id=" + ipId);
        }

        for (FirewallRuleVO rule : fwRules) {
            // Mark all Firewall rules as Revoke, but don't revoke them yet - we have to revoke all rules for ip, no need to send them one by one
            revokeFirewallRule(rule.getId(), false, caller, Account.ACCOUNT_ID_SYSTEM);
        }

        // now send everything to the backend
        List<FirewallRuleVO> rulesToApply = _firewallDao.listByIpAndPurpose(ipId, Purpose.Firewall);
        applyFirewallRules(rulesToApply, true, caller);

        // Now we check again in case more rules have been inserted.
        rules.addAll(_firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.Firewall));

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released firewall rules for ip id=" + ipId + " and # of rules now = " + rules.size());
        }

        return rules.size() == 0;
    }
    
    @Override
    public FirewallRule createRuleForAllCidrs(long ipAddrId, Account caller, Integer startPort, Integer endPort, String protocol, Integer icmpCode, Integer icmpType, Long relatedRuleId) throws NetworkRuleConflictException{
        
        //If firwallRule for this port range already exists, return it
        List<FirewallRuleVO> rules = _firewallDao.listByIpPurposeAndProtocolAndNotRevoked(ipAddrId, startPort, endPort, protocol, Purpose.Firewall);
        if (!rules.isEmpty()) {
            return rules.get(0);
        }
        
        List<String> oneCidr = new ArrayList<String>();
        oneCidr.add(NetUtils.ALL_CIDRS);
        return createFirewallRule(ipAddrId, caller, null, startPort, endPort, protocol, oneCidr, icmpCode, icmpType, relatedRuleId, FirewallRule.FirewallRuleType.User);
    }
    
    @Override
    public boolean revokeAllFirewallRulesForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException {
        List<FirewallRule> rules = new ArrayList<FirewallRule>();

        List<FirewallRuleVO> fwRules = _firewallDao.listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.Firewall);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + fwRules.size() + " firewall rules for network id=" + networkId);
        }

        for (FirewallRuleVO rule : fwRules) {
            // Mark all Firewall rules as Revoke, but don't revoke them yet - we have to revoke all rules for ip, no need to send them one by one
            revokeFirewallRule(rule.getId(), false, caller, Account.ACCOUNT_ID_SYSTEM);
        }

        // now send everything to the backend
        List<FirewallRuleVO> rulesToApply = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.Firewall);
        boolean success = applyFirewallRules(rulesToApply, true, caller);

        // Now we check again in case more rules have been inserted.
        rules.addAll(_firewallDao.listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.Firewall));

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released firewall rules for network id=" + networkId + " and # of rules now = " + rules.size());
        }

        return success && rules.size() == 0;
    }
    
    @Override
    public boolean revokeRelatedFirewallRule(long ruleId, boolean apply) {
        FirewallRule fwRule = _firewallDao.findByRelatedId(ruleId);
        
        if (fwRule == null) {
            s_logger.trace("No related firewall rule exists for rule id=" + ruleId + " so returning true here");
            return true;
        }
        
        s_logger.debug("Revoking Firewall rule id=" + fwRule.getId() + " as a part of rule delete id=" + ruleId + " with apply=" + apply);
        return revokeFirewallRule(fwRule.getId(), apply);
        
    }
    
    
    @Override
    public boolean revokeFirewallRulesForVm(long vmId) {
        boolean success = true;
        UserVmVO vm = _vmDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            return false;
        }

        List<PortForwardingRuleVO> pfRules = _pfRulesDao.listByVm(vmId);
        List<FirewallRuleVO> staticNatRules = _firewallDao.listStaticNatByVmId(vm.getId());
        List<FirewallRuleVO> firewallRules = new ArrayList<FirewallRuleVO>();
        
        //Make a list of firewall rules to reprogram
        for (PortForwardingRuleVO pfRule : pfRules) {
            FirewallRuleVO relatedRule = _firewallDao.findByRelatedId(pfRule.getId());
            if (relatedRule != null) {
                firewallRules.add(relatedRule);
            }
        }
        
        for (FirewallRuleVO staticNatRule : staticNatRules) {
            FirewallRuleVO relatedRule = _firewallDao.findByRelatedId(staticNatRule.getId());
            if (relatedRule != null) {
                firewallRules.add(relatedRule);
            }
        }
        
        
        Set<Long> ipsToReprogram = new HashSet<Long>();

        if (firewallRules.isEmpty()) {
            s_logger.debug("No firewall rules are found for vm id=" + vmId);
            return true;
        } else {
            s_logger.debug("Found " + firewallRules.size() + " to cleanup for vm id=" + vmId);
        }

        for (FirewallRuleVO rule : firewallRules) {
            // Mark firewall rules as Revoked, but don't revoke it yet (apply=false)
            revokeFirewallRule(rule.getId(), false, _accountMgr.getSystemAccount(), Account.ACCOUNT_ID_SYSTEM);
            ipsToReprogram.add(rule.getSourceIpAddressId());
        }

        // apply rules for all ip addresses
        for (Long ipId : ipsToReprogram) {
            s_logger.debug("Applying firewall rules for ip address id=" + ipId + " as a part of vm expunge");
            try {
                success = success && applyFirewallRules(ipId,_accountMgr.getSystemAccount());
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Failed to apply port forwarding rules for ip id=" + ipId);
                success = false;
            }
        }

        return success;
    }

	@Override
	public boolean addSystemFirewallRules(IPAddressVO ip, Account acct) {
		List<FirewallRuleVO> systemRules = _firewallDao.listSystemRules();
		for (FirewallRuleVO rule : systemRules) {
			try {
				this.createFirewallRule(ip.getId(), acct, rule.getXid(), rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol(), 
										rule.getSourceCidrList(), rule.getIcmpCode(), rule.getIcmpType(), rule.getRelated(), FirewallRuleType.System);
			} catch (Exception e) {
				s_logger.debug("Failed to add system wide firewall rule, due to:" + e.toString());
			}
		}
		return true;
	}
}
