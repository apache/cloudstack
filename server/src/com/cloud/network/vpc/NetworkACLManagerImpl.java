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
package com.cloud.network.vpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.firewall.NetworkACLService;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.TrafficType;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
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


@Local(value = { NetworkACLService.class, NetworkACLManager.class})
public class NetworkACLManagerImpl implements Manager,NetworkACLManager{
    String _name;
    private static final Logger s_logger = Logger.getLogger(NetworkACLManagerImpl.class);

    @Inject
    AccountManager _accountMgr;
    @Inject
    FirewallManager _firewallMgr;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    ResourceTagDao _resourceTagDao;

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
    public boolean applyNetworkACLs(long networkId, Account caller) throws ResourceUnavailableException {
        List<FirewallRuleVO> rules = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.NetworkACL);
        return _firewallMgr.applyFirewallRules(rules, false, caller);
    }

    @Override
    public FirewallRule createNetworkACL(FirewallRule acl) throws NetworkRuleConflictException {
        return createNetworkACL(UserContext.current().getCaller(), acl.getXid(), acl.getSourcePortStart(), 
                acl.getSourcePortEnd(), acl.getProtocol(), acl.getSourceCidrList(), acl.getIcmpCode(),
                acl.getIcmpType(), null, acl.getType(), acl.getNetworkId(), acl.getTrafficType());
    }

    @DB
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating firewall rule", create = true)
    protected FirewallRule createNetworkACL(Account caller, String xId, Integer portStart, 
            Integer portEnd, String protocol, List<String> sourceCidrList, Integer icmpCode, Integer icmpType,
            Long relatedRuleId, FirewallRule.FirewallRuleType type, long networkId, TrafficType trafficType) throws NetworkRuleConflictException {
        
        Network network = _networkMgr.getNetwork(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Can't find network by id");
        }
        
        if (network.getVpcId() == null) {
            throw new UnsupportedOperationException("Network ACL rules are supported just for VPC networks");
        }
        
        Vpc vpc = _vpcMgr.getVpc(network.getVpcId());
        Account aclOwner = _accountMgr.getAccount(vpc.getAccountId());
        
        //check if the caller can access vpc
        _accountMgr.checkAccess(caller, null, false, vpc);

        //check if the acl can be created for this network
        _accountMgr.checkAccess(aclOwner, AccessType.UseNetwork, false, network);
        
        if (!_networkMgr.areServicesSupportedInNetwork(networkId, Service.NetworkACL)) {
            throw new InvalidParameterValueException("Service " + Service.NetworkACL + " is not supported in network " + network);
        }
        
        // icmp code and icmp type can't be passed in for any other protocol rather than icmp
        if (!protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (icmpCode != null || icmpType != null)) {
            throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
        }

        if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (portStart != null || portEnd != null)) {
            throw new InvalidParameterValueException("Can't specify start/end port when protocol is ICMP");
        } 
        
        //validate icmp code and type
        if (icmpType != null) {
            if (icmpType.longValue() != -1 && !NetUtils.validateIcmpType(icmpType.longValue())) {
                throw new InvalidParameterValueException("Invalid icmp type; should belong to [0-255] range");
            }
            if (icmpCode != null) {
                if (icmpCode.longValue() != -1 && !NetUtils.validateIcmpCode(icmpCode.longValue())) {
                    throw new InvalidParameterValueException("Invalid icmp code; should belong to [0-15] range and can" +
                            " be defined when icmpType belongs to [0-40] range");
                }
            }
        }

        validateNetworkACL(caller, network, portStart, portEnd, protocol);

        Transaction txn = Transaction.currentTxn();
        txn.start();

        FirewallRuleVO newRule = new FirewallRuleVO(xId, null, portStart, portEnd, protocol.toLowerCase(), networkId,
                aclOwner.getAccountId(), aclOwner.getDomainId(), Purpose.NetworkACL, sourceCidrList, icmpCode, icmpType, 
                relatedRuleId, trafficType);
        newRule.setType(type);
        newRule = _firewallDao.persist(newRule);

        if (type == FirewallRule.FirewallRuleType.User) {
            detectNetworkACLConflict(newRule);
        }

        if (!_firewallDao.setStateToAdd(newRule)) {
            throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
        }
        UserContext.current().setEventDetails("Rule Id: " + newRule.getId());

        txn.commit();

        return getNetworkACL(newRule.getId());
    }
    
    
    protected void validateNetworkACL(Account caller, Network network, Integer portStart, Integer portEnd, 
            String proto) {
        
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
        
        if (network.getTrafficType() != Networks.TrafficType.Guest) {
            throw new InvalidParameterValueException("Network ACL can be created just for networks of type " + Networks.TrafficType.Guest);
        }

        // Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> caps = _networkMgr.getNetworkServiceCapabilities(network.getId(), Service.NetworkACL);
        

        if (caps != null) {
            String supportedProtocols = caps.get(Capability.SupportedProtocols).toLowerCase();
            if (!supportedProtocols.contains(proto.toLowerCase())) {
                throw new InvalidParameterValueException("Protocol " + proto + " is not supported by the network " + network);
            }
        } else {
            throw new InvalidParameterValueException("No capabilities are found for network " + network);
        }
    }
    
    protected void detectNetworkACLConflict(FirewallRuleVO newRule) throws NetworkRuleConflictException {
        if (newRule.getPurpose() != Purpose.NetworkACL) {
            return;
        }
        
        List<FirewallRuleVO> rules = _firewallDao.listByNetworkPurposeTrafficTypeAndNotRevoked(newRule.getNetworkId(),
                Purpose.NetworkACL, newRule.getTrafficType());
        assert (rules.size() >= 1) : "For network ACLs, we now always first persist the rule and then check for " +
                "network conflicts so we should at least have one rule at this point.";

        for (FirewallRuleVO rule : rules) {
            if (rule.getId() == newRule.getId() || !rule.getProtocol().equalsIgnoreCase(newRule.getProtocol())) {
                continue; // Skips my own rule and skip the rule if the protocol is different
            }

            // if one cidr overlaps another, do port veirficatino
            boolean duplicatedCidrs = false;
            // Verify that the rules have different cidrs
            List<String> ruleCidrList = rule.getSourceCidrList();
            List<String> newRuleCidrList = newRule.getSourceCidrList();

            if (ruleCidrList == null || newRuleCidrList == null) {
                continue;
            }
            
            for (String newCidr : newRuleCidrList) {
                for (String ruleCidr : ruleCidrList) {
                    if (NetUtils.isNetworksOverlap(newCidr, ruleCidr)) {
                        duplicatedCidrs = true;
                        break;
                    }
                    if (duplicatedCidrs) {
                        break;
                    }
                }
            }

            if (newRule.getProtocol().equalsIgnoreCase(NetUtils.ICMP_PROTO) 
                    && newRule.getProtocol().equalsIgnoreCase(rule.getProtocol())) {
                if ((newRule.getIcmpCode().longValue() == rule.getIcmpCode().longValue() 
                        || rule.getIcmpCode().longValue() == -1 || newRule.getIcmpCode().longValue() == -1)
                        && (newRule.getIcmpType().longValue() == rule.getIcmpType().longValue() 
                        || rule.getIcmpType().longValue() == -1 || newRule.getIcmpType().longValue() == -1)
                        && newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()) && duplicatedCidrs) {
                    throw new InvalidParameterValueException("New network ACL conflicts with existing network ACL id=" + rule.getId());
                }
            }

            boolean notNullPorts = (newRule.getSourcePortStart() != null && newRule.getSourcePortEnd() != null && 
                    rule.getSourcePortStart() != null && rule.getSourcePortEnd() != null);
            if (!notNullPorts) {
                continue;
            } else if (duplicatedCidrs
                    && ((rule.getSourcePortStart().intValue() <= newRule.getSourcePortStart().intValue() 
                    && rule.getSourcePortEnd().intValue() >= newRule.getSourcePortStart().intValue())
                            || (rule.getSourcePortStart().intValue() <= newRule.getSourcePortEnd().intValue() 
                            && rule.getSourcePortEnd().intValue() >= newRule.getSourcePortEnd().intValue())
                            || (newRule.getSourcePortStart().intValue() <= rule.getSourcePortStart().intValue() 
                            && newRule.getSourcePortEnd().intValue() >= rule.getSourcePortStart().intValue())
                            || (newRule.getSourcePortStart().intValue() <= rule.getSourcePortEnd().intValue() 
                            && newRule.getSourcePortEnd().intValue() >= rule.getSourcePortEnd().intValue()))) {

                throw new NetworkRuleConflictException("The range specified, " + newRule.getSourcePortStart() + "-" 
                            + newRule.getSourcePortEnd() + ", conflicts with rule " + rule.getId()
                            + " which has " + rule.getSourcePortStart() + "-" + rule.getSourcePortEnd());
                
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("No network rule conflicts detected for " + newRule + " against " + (rules.size() - 1) 
                    + " existing network ACLs");
        }
    }
    
    @Override
    public boolean revokeNetworkACL(long ruleId, boolean apply) {
        Account caller = UserContext.current().getCaller();
        long userId = UserContext.current().getCallerUserId();
        return revokeNetworkACL(ruleId, apply, caller, userId);
    }
    
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_CLOSE, eventDescription = "revoking firewall rule", async = true)
    protected boolean revokeNetworkACL(long ruleId, boolean apply, Account caller, long userId) {

        FirewallRuleVO rule = _firewallDao.findById(ruleId);
        if (rule == null || rule.getPurpose() != Purpose.NetworkACL) {
            throw new InvalidParameterValueException("Unable to find " + ruleId + " having purpose " + Purpose.NetworkACL);
        }
        
        _accountMgr.checkAccess(caller, null, true, rule);

        _firewallMgr.revokeRule(rule, caller, userId, false);

        boolean success = false;

        if (apply) {
            List<FirewallRuleVO> rules = _firewallDao.listByNetworkAndPurpose(rule.getNetworkId(), Purpose.NetworkACL);
            success = _firewallMgr.applyFirewallRules(rules, false, caller);
        } else {
            success = true;
        }

        return success;
    }

    
    @Override
    public FirewallRule getNetworkACL(long ACLId) {
        FirewallRule rule = _firewallDao.findById(ACLId);
        if (rule != null && rule.getPurpose() == Purpose.NetworkACL) {
            return rule;
        }
        return null;
    }

    
    @Override
    public Pair<List<? extends FirewallRule>,Integer> listNetworkACLs(ListNetworkACLsCmd cmd) {
        Long networkId = cmd.getNetworkId();
        Long id = cmd.getId();
        String trafficType = cmd.getTrafficType();
        Map<String, String> tags = cmd.getTags();

        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = 
                new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter filter = new Filter(FirewallRuleVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<FirewallRuleVO> sb = _firewallDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("networkId", sb.entity().getNetworkId(), Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), Op.EQ);
        sb.and("trafficType", sb.entity().getTrafficType(), Op.EQ);
        
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

        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }
        
        if (trafficType != null) {
            sc.setParameters("trafficType", trafficType);
        }
        
        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.NetworkACL.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }   
        }

        sc.setParameters("purpose", Purpose.NetworkACL);

        Pair<List<FirewallRuleVO>, Integer> result = _firewallDao.searchAndCount(sc, filter);
        return new Pair<List<? extends FirewallRule>, Integer>(result.first(), result.second());
    }


    @Override
    public List<? extends FirewallRule> listNetworkACLs(long guestNtwkId) {
        return _firewallDao.listByNetworkAndPurpose(guestNtwkId, Purpose.NetworkACL);
    }

    
    @Override
    public boolean revokeAllNetworkACLsForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException {

        List<FirewallRuleVO> ACLs = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.NetworkACL);
        
        if (ACLs.isEmpty()) {
            s_logger.debug("Found no network ACLs for network id=" + networkId);
            return true;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + ACLs.size() + " Network ACLs for network id=" + networkId);
        }

        for (FirewallRuleVO ACL : ACLs) {
            // Mark all Network ACLs rules as Revoke, but don't revoke them yet - we have to revoke all rules for ip, no
            // need to send them one by one
            revokeNetworkACL(ACL.getId(), false, caller, Account.ACCOUNT_ID_SYSTEM);
        }
        
        List<FirewallRuleVO> ACLsToRevoke = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.NetworkACL);

        // now send everything to the backend
        boolean success = _firewallMgr.applyFirewallRules(ACLsToRevoke, false, caller);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released Network ACLs for network id=" + networkId + " and # of rules now = " 
                                + ACLs.size());
        }

        return success;
    }
    
}
