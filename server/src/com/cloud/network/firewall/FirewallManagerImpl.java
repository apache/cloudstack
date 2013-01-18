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
package com.cloud.network.firewall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.firewall.ListFirewallRulesCmd;
import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
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
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkRuleApplier;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.VpcManager;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Adapters;
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

@Local(value = { FirewallService.class, FirewallManager.class})
public class FirewallManagerImpl implements FirewallService, FirewallManager, NetworkRuleApplier, Manager {
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
    NetworkModel _networkModel;
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
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject(adapter = FirewallServiceProvider.class)
    Adapters<FirewallServiceProvider> _firewallElements;

    @Inject(adapter = PortForwardingServiceProvider.class)
    Adapters<PortForwardingServiceProvider> _pfElements;
    
    @Inject(adapter = StaticNatServiceProvider.class)
    Adapters<StaticNatServiceProvider> _staticNatElements;
    
    @Inject(adapter = NetworkACLServiceProvider.class)
    Adapters<NetworkACLServiceProvider> _networkAclElements;

    private boolean _elbEnabled = false;

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
        s_logger.info("Firewall provider list is " + _firewallElements.iterator().next());
        return true;
    }

    @Override
    public FirewallRule createFirewallRule(FirewallRule rule) throws NetworkRuleConflictException {
        Account caller = UserContext.current().getCaller();

        return createFirewallRule(rule.getSourceIpAddressId(), caller, rule.getXid(), rule.getSourcePortStart(), 
                rule.getSourcePortEnd(), rule.getProtocol(), rule.getSourceCidrList(), rule.getIcmpCode(),
                rule.getIcmpType(), null, rule.getType(), rule.getNetworkId());
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating firewall rule", create = true)
    public FirewallRule createFirewallRule(long ipAddrId, Account caller, String xId, Integer portStart, 
            Integer portEnd, String protocol, List<String> sourceCidrList, Integer icmpCode, Integer icmpType,
            Long relatedRuleId, FirewallRule.FirewallRuleType type, long networkId) throws NetworkRuleConflictException {
        
        IPAddressVO ipAddress = _ipAddressDao.findById(ipAddrId);
        // Validate ip address
        if (ipAddress == null && type == FirewallRule.FirewallRuleType.User) {
            throw new InvalidParameterValueException("Unable to create firewall rule; ip id=" + ipAddrId + 
                    " doesn't exist in the system");
        }

        _networkModel.checkIpForService(ipAddress, Service.Firewall, null);  

        validateFirewallRule(caller, ipAddress, portStart, portEnd, protocol, Purpose.Firewall, type);

        // icmp code and icmp type can't be passed in for any other protocol rather than icmp
        if (!protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (icmpCode != null || icmpType != null)) {
            throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
        }

        if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (portStart != null || portEnd != null)) {
            throw new InvalidParameterValueException("Can't specify start/end port when protocol is ICMP");
        }

        Long accountId = null;
        Long domainId = null;

        if (ipAddress != null) {
            accountId = ipAddress.getAllocatedToAccountId();
            domainId = ipAddress.getAllocatedInDomainId();
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        FirewallRuleVO newRule = new FirewallRuleVO(xId, ipAddrId, portStart, portEnd, protocol.toLowerCase(), networkId,
                accountId, domainId, Purpose.Firewall, sourceCidrList, icmpCode, icmpType, relatedRuleId, null);
        newRule.setType(type);
        newRule = _firewallDao.persist(newRule);

        if (type == FirewallRuleType.User)
            detectRulesConflict(newRule);

        if (!_firewallDao.setStateToAdd(newRule)) {
            throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
        }
        UserContext.current().setEventDetails("Rule Id: " + newRule.getId());

        txn.commit();

        return newRule;
    }

    @Override
    public Pair<List<? extends FirewallRule>, Integer> listFirewallRules(ListFirewallRulesCmd cmd) {
        Long ipId = cmd.getIpAddressId();
        Long id = cmd.getId();
        Map<String, String> tags = cmd.getTags();

        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        if (ipId != null) {
            IPAddressVO ipAddressVO = _ipAddressDao.findById(ipId);
            if (ipAddressVO == null || !ipAddressVO.readyToUse()) {
                throw new InvalidParameterValueException("Ip address id=" + ipId + " not ready for firewall rules yet");
            }
            _accountMgr.checkAccess(caller, null, true, ipAddressVO);
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter filter = new Filter(FirewallRuleVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<FirewallRuleVO> sb = _firewallDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("ip", sb.entity().getSourceIpAddressId(), Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), Op.EQ);


        if (tags != null && !tags.isEmpty()) {
        SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
        for (int count=0; count < tags.size(); count++) {
            tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
            tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
            tagSearch.cp();
        }
        tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        sb.groupBy(sb.entity().getId());
        sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
    }

        SearchCriteria<FirewallRuleVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.FirewallRule.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (ipId != null) {
            sc.setParameters("ip", ipId);
        }

        sc.setParameters("purpose", Purpose.Firewall);

        Pair<List<FirewallRuleVO>, Integer> result = _firewallDao.searchAndCount(sc, filter);
        return new Pair<List<? extends FirewallRule>, Integer>(result.first(), result.second());
    }

    @Override
    public void detectRulesConflict(FirewallRule newRule) throws NetworkRuleConflictException {

        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurposeAndNotRevoked(newRule.getSourceIpAddressId(), null);
        assert (rules.size() >= 1) : "For network rules, we now always first persist the rule and then check for " +
        		"network conflicts so we should at least have one rule at this point.";

        for (FirewallRuleVO rule : rules) {
            if (rule.getId() == newRule.getId()) {
                continue; // Skips my own rule.
            }

            boolean oneOfRulesIsFirewall = ((rule.getPurpose() == Purpose.Firewall || newRule.getPurpose() == Purpose.Firewall)
                    && ((newRule.getPurpose() != rule.getPurpose()) || (!newRule.getProtocol()
                    .equalsIgnoreCase(rule.getProtocol()))));

            // if both rules are firewall and their cidrs are different, we can skip port ranges verification
            boolean bothRulesFirewall = (rule.getPurpose() == newRule.getPurpose() && rule.getPurpose() == Purpose.Firewall);
            boolean duplicatedCidrs = false;
            if (bothRulesFirewall) {
                // Verify that the rules have different cidrs
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
                    throw new NetworkRuleConflictException("There is 1 to 1 Nat rule specified for the ip address id=" 
                            + newRule.getSourceIpAddressId());
                } else if (rule.getPurpose() != Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat) {
                    throw new NetworkRuleConflictException("There is already firewall rule specified for the ip address id="
                            + newRule.getSourceIpAddressId());
                }
            }

            if (rule.getNetworkId() != newRule.getNetworkId() && rule.getState() != State.Revoke) {
                throw new NetworkRuleConflictException("New rule is for a different network than what's specified in rule "
                        + rule.getXid());
            }

            if (newRule.getProtocol().equalsIgnoreCase(NetUtils.ICMP_PROTO) && newRule.getProtocol().equalsIgnoreCase(rule.getProtocol())) {
                if (newRule.getIcmpCode().longValue() == rule.getIcmpCode().longValue() 
                        && newRule.getIcmpType().longValue() == rule.getIcmpType().longValue()
                        && newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()) && duplicatedCidrs) {
                    throw new InvalidParameterValueException("New rule conflicts with existing rule id=" + rule.getId());
                }
            }

            boolean notNullPorts = (newRule.getSourcePortStart() != null && newRule.getSourcePortEnd() != null && 
                    rule.getSourcePortStart() != null && rule.getSourcePortEnd() != null);
            if (!notNullPorts) {
                continue;
            } else if (!oneOfRulesIsFirewall && !(bothRulesFirewall && !duplicatedCidrs)
                    && ((rule.getSourcePortStart().intValue() <= newRule.getSourcePortStart().intValue() 
                    && rule.getSourcePortEnd().intValue() >= newRule.getSourcePortStart().intValue())
                            || (rule.getSourcePortStart().intValue() <= newRule.getSourcePortEnd().intValue() 
                            && rule.getSourcePortEnd().intValue() >= newRule.getSourcePortEnd().intValue())
                            || (newRule.getSourcePortStart().intValue() <= rule.getSourcePortStart().intValue() 
                            && newRule.getSourcePortEnd().intValue() >= rule.getSourcePortStart().intValue())
                            || (newRule.getSourcePortStart().intValue() <= rule.getSourcePortEnd().intValue() 
                            && newRule.getSourcePortEnd().intValue() >= rule.getSourcePortEnd().intValue()))) {

                // we allow port forwarding rules with the same parameters but different protocols
                boolean allowPf = (rule.getPurpose() == Purpose.PortForwarding && newRule.getPurpose() == Purpose.PortForwarding
                        && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));
                boolean allowStaticNat = (rule.getPurpose() == Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat
                        && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));

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
    public void validateFirewallRule(Account caller, IPAddressVO ipAddress, Integer portStart, Integer portEnd, 
            String proto, Purpose purpose, FirewallRuleType type) {
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
        _accountMgr.checkAccess(caller, null, true, ipAddress);

        Long networkId = null;

        if (ipAddress.getAssociatedWithNetworkId() == null) {
            throw new InvalidParameterValueException("Unable to create firewall rule ; ip id=" + 
                    ipAddress.getId() + " is not associated with any network");
        } else {
            networkId = ipAddress.getAssociatedWithNetworkId();
        }

        Network network = _networkModel.getNetwork(networkId);
        assert network != null : "Can't create port forwarding rule as network associated with public ip address is null?";

        // Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> caps = null;

        if (purpose == Purpose.LoadBalancing) {
            if (!_elbEnabled) {
                caps = _networkModel.getNetworkServiceCapabilities(network.getId(), Service.Lb);
            }
        } else if (purpose == Purpose.PortForwarding) {
            caps = _networkModel.getNetworkServiceCapabilities(network.getId(), Service.PortForwarding);
        }

        if (caps != null) {
            String supportedProtocols = caps.get(Capability.SupportedProtocols).toLowerCase();
            if (!supportedProtocols.contains(proto.toLowerCase())) {
                throw new InvalidParameterValueException("Protocol " + proto + " is not supported in zone " + network.getDataCenterId());
            } else if (proto.equalsIgnoreCase(NetUtils.ICMP_PROTO) && purpose != Purpose.Firewall) {
                throw new InvalidParameterValueException("Protocol " + proto + " is currently supported only for rules with purpose " + Purpose.Firewall);
            }
        }
    }

    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError, boolean updateRulesInDB) 
            throws ResourceUnavailableException {
        boolean success = true;
        if (rules == null || rules.size() == 0) {
            s_logger.debug("There are no rules to forward to the network elements");
            return true;
        }
        Purpose purpose = rules.get(0).getPurpose();
        if (!_networkMgr.applyRules(rules, purpose, this, continueOnError)) {
            s_logger.warn("Rules are not completely applied");
            return false;
        } else {
            if (updateRulesInDB) {
                for (FirewallRule rule : rules) {
                    if (rule.getState() == FirewallRule.State.Revoke) {
                        FirewallRuleVO relatedRule = _firewallDao.findByRelatedId(rule.getId());
                        if (relatedRule != null) {
                            s_logger.warn("Can't remove the firewall rule id=" + rule.getId() + 
                                    " as it has related firewall rule id=" + relatedRule.getId() + "; leaving it in Revoke state");
                            success = false;
                        } else {
                            removeRule(rule);
                            if (rule.getSourceIpAddressId() != null) {
                                //if the rule is the last one for the ip address assigned to VPC, unassign it from the network
                                IpAddress ip = _ipAddressDao.findById(rule.getSourceIpAddressId());
                                _vpcMgr.unassignIPFromVpcNetwork(ip.getId(), rule.getNetworkId());
                             }
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
    public  boolean applyRules(Network network, Purpose purpose, List<? extends FirewallRule> rules) 
            throws ResourceUnavailableException {
    	boolean handled = false;
    	switch (purpose){
    	case Firewall:
    	    for (FirewallServiceProvider fwElement: _firewallElements) {
    	        handled = fwElement.applyFWRules(network, rules);
    	        if (handled)
    	            break;
    	    }
    	case PortForwarding:
    	    for (PortForwardingServiceProvider element: _pfElements) {
                handled = element.applyPFRules(network, (List<PortForwardingRule>) rules);
                if (handled)
                    break;
            }
    	    break;
    	case StaticNat:
            for (StaticNatServiceProvider element: _staticNatElements) {
                handled = element.applyStaticNats(network, (List<? extends StaticNat>) rules);
                if (handled)
                    break;
            }
            break;
    	case NetworkACL:
            for (NetworkACLServiceProvider element: _networkAclElements) {
                handled = element.applyNetworkACLs(network, (List<? extends FirewallRule>) rules);
                if (handled)
                    break;
            }
            break;
    	default:
    	    assert(false): "Unexpected fall through in applying rules to the network elements";
    	    s_logger.error("FirewallManager cannot process rules of type " + purpose);
    	    throw new CloudRuntimeException("FirewallManager cannot process rules of type " + purpose);
    	}
    	return handled;
    }
    
    @Override
    public void removeRule(FirewallRule rule) {

        //remove the rule
        _firewallDao.remove(rule.getId());
    }

    @Override
    public boolean applyFirewallRules(long ipId, Account caller) throws ResourceUnavailableException {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurpose(ipId, Purpose.Firewall);
        return applyFirewallRules(rules, false, caller);
    }

    @Override
    public boolean applyFirewallRules(List<FirewallRuleVO> rules, boolean continueOnError, Account caller) {

        if (rules.size() == 0) {
            s_logger.debug("There are no firewall rules to apply");
            return true;
        }

        for (FirewallRuleVO rule : rules) {
            // load cidrs if any
            rule.setSourceCidrList(_firewallCidrsDao.getSourceCidrs(rule.getId()));
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, null, true, rules.toArray(new FirewallRuleVO[rules.size()]));
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

        _accountMgr.checkAccess(caller, null, true, rule);

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
            _accountMgr.checkAccess(caller, null, true, rule);
        }

        Transaction txn = Transaction.currentTxn();
        boolean generateUsageEvent = false;

        txn.start();
        if (rule.getState() == State.Staged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a rule that is still in stage state so just removing it: " + rule);
            }
            removeRule(rule);
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
            // Mark all Firewall rules as Revoke, but don't revoke them yet - we have to revoke all rules for ip, no
            // need to send them one by one
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
    public FirewallRule createRuleForAllCidrs(long ipAddrId, Account caller,
            Integer startPort, Integer endPort, String protocol, Integer icmpCode, Integer icmpType, Long relatedRuleId, long networkId)
            throws NetworkRuleConflictException {

        // If firwallRule for this port range already exists, return it
        List<FirewallRuleVO> rules = _firewallDao.listByIpPurposeAndProtocolAndNotRevoked(ipAddrId, startPort, endPort,
                protocol, Purpose.Firewall);
        if (!rules.isEmpty()) {
            return rules.get(0);
        }

        List<String> oneCidr = new ArrayList<String>();
        oneCidr.add(NetUtils.ALL_CIDRS);
        return createFirewallRule(ipAddrId, caller, null, startPort, endPort, protocol, oneCidr, icmpCode, icmpType,
                relatedRuleId, FirewallRule.FirewallRuleType.User, networkId);
    }

    @Override
    public boolean revokeAllFirewallRulesForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException {
        List<FirewallRule> rules = new ArrayList<FirewallRule>();

        List<FirewallRuleVO> fwRules = _firewallDao.listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.Firewall);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + fwRules.size() + " firewall rules for network id=" + networkId);
        }

        for (FirewallRuleVO rule : fwRules) {
            // Mark all Firewall rules as Revoke, but don't revoke them yet - we have to revoke all rules for ip, no
            // need to send them one by one
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

        // Make a list of firewall rules to reprogram
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
                success = success && applyFirewallRules(ipId, _accountMgr.getSystemAccount());
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
                        rule.getSourceCidrList(), rule.getIcmpCode(), rule.getIcmpType(), rule.getRelated(), FirewallRuleType.System, rule.getNetworkId());
            } catch (Exception e) {
                s_logger.debug("Failed to add system wide firewall rule, due to:" + e.toString());
            }
        }
        return true;
    }

}
