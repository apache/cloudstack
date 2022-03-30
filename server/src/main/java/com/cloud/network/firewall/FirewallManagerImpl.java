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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.firewall.IListFirewallRulesCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Config;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkRuleApplier;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.FirewallRulesDcidrsDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
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
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.VpcManager;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

@Component
public class FirewallManagerImpl extends ManagerBase implements FirewallService, FirewallManager, NetworkRuleApplier {
    private static final Logger s_logger = Logger.getLogger(FirewallManagerImpl.class);

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
    FirewallRulesDcidrsDao _firewallDcidrsDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkOrchestrationService _networkMgr;
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
    NetworkDao _networkDao;
    @Inject
    VpcManager _vpcMgr;
    List<FirewallServiceProvider> _firewallElements;

    List<PortForwardingServiceProvider> _pfElements;

    List<StaticNatServiceProvider> _staticNatElements;

    List<NetworkACLServiceProvider> _networkAclElements;
    @Inject
    IpAddressManager _ipAddrMgr;

    private boolean _elbEnabled = false;
    static Boolean rulesContinueOnErrFlag = true;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        String elbEnabledString = _configDao.getValue(Config.ElasticLoadBalancerEnabled.key());
        _elbEnabled = Boolean.parseBoolean(elbEnabledString);
        if (_ipAddrMgr.RulesContinueOnError.value() != null) {
            rulesContinueOnErrFlag = _ipAddrMgr.RulesContinueOnError.value();
        }
        return true;
    }

    @Override
    public boolean start() {
        s_logger.info("Firewall provider list is " + _firewallElements.iterator().next());
        return super.start();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_EGRESS_OPEN, eventDescription = "creating egress firewall rule for network", create = true)
    public FirewallRule createEgressFirewallRule(FirewallRule rule) throws NetworkRuleConflictException {
        Account caller = CallContext.current().getCallingAccount();

        Network network = _networkDao.findById(rule.getNetworkId());
        if (network.getGuestType() == Network.GuestType.Shared) {
            throw new InvalidParameterValueException("Egress firewall rules are not supported for " + network.getGuestType() + "  networks");
        }

        List<String> sourceCidrs = rule.getSourceCidrList();
        if (sourceCidrs != null && !sourceCidrs.isEmpty())
        Collections.replaceAll(sourceCidrs, "0.0.0.0/0", network.getCidr());

        return createFirewallRule(null, caller, rule.getXid(), rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol(), sourceCidrs, rule.getDestinationCidrList(),
                rule.getIcmpCode(), rule.getIcmpType(), null, rule.getType(), rule.getNetworkId(), rule.getTrafficType(), rule.isDisplay());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating firewall rule", create = true)
    public FirewallRule createIngressFirewallRule(FirewallRule rule) throws NetworkRuleConflictException {
         Account caller = CallContext.current().getCallingAccount();
        Long sourceIpAddressId = rule.getSourceIpAddressId();

        return createFirewallRule(sourceIpAddressId, caller, rule.getXid(), rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol(),
            rule.getSourceCidrList(), null, rule.getIcmpCode(), rule.getIcmpType(), null, rule.getType(), rule.getNetworkId(), rule.getTrafficType(), rule.isDisplay());
    }
    //Destination CIDR capability is currently implemented for egress rules only. For others, the field is passed as null.
    @DB
    protected FirewallRule createFirewallRule(final Long ipAddrId, Account caller, final String xId, final Integer portStart, final Integer portEnd,
        final String protocol, final List<String> sourceCidrList, final List<String> destCidrList, final Integer icmpCode, final Integer icmpType, final Long relatedRuleId,
 final FirewallRule.FirewallRuleType type,
            final Long networkId, final FirewallRule.TrafficType trafficType, final Boolean forDisplay) throws NetworkRuleConflictException {

        IPAddressVO ipAddress = null;
        if (ipAddrId != null) {
            // this for ingress firewall rule, for egress id is null
             ipAddress = _ipAddressDao.findById(ipAddrId);
        // Validate ip address
        if (ipAddress == null && type == FirewallRule.FirewallRuleType.User) {
                throw new InvalidParameterValueException("Unable to create firewall rule; " + "couldn't locate IP address by id in the system");
        }
        _networkModel.checkIpForService(ipAddress, Service.Firewall, null);
        }

        validateFirewallRule(caller, ipAddress, portStart, portEnd, protocol, Purpose.Firewall, type, networkId, trafficType);

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
            //Ingress firewall rule
            accountId = ipAddress.getAllocatedToAccountId();
            domainId = ipAddress.getAllocatedInDomainId();
        } else if (networkId != null) {
            //egress firewall rule
                Network network = _networkModel.getNetwork(networkId);
                accountId = network.getAccountId();
                domainId = network.getDomainId();
        }

        final Long accountIdFinal = accountId;
        final Long domainIdFinal = domainId;
        return Transaction.execute(new TransactionCallbackWithException<FirewallRuleVO, NetworkRuleConflictException>() {
            @Override
            public FirewallRuleVO doInTransaction(TransactionStatus status) throws NetworkRuleConflictException {
                FirewallRuleVO newRule =
                        new FirewallRuleVO(xId, ipAddrId, portStart, portEnd, protocol.toLowerCase(), networkId, accountIdFinal, domainIdFinal, Purpose.Firewall,
                                sourceCidrList, destCidrList, icmpCode, icmpType, relatedRuleId, trafficType);
                newRule.setType(type);
                if (forDisplay != null) {
                    newRule.setDisplay(forDisplay);
                }
                newRule = _firewallDao.persist(newRule);

                if (type == FirewallRuleType.User)
                    detectRulesConflict(newRule);

                if (!_firewallDao.setStateToAdd(newRule)) {
                    throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
                }
                CallContext.current().setEventDetails("Rule Id: " + newRule.getId());
                CallContext.current().putContextParameter(FirewallRule.class, newRule.getId());

                return newRule;
            }
        });
    }

    @Override
    public Pair<List<? extends FirewallRule>, Integer> listFirewallRules(IListFirewallRulesCmd cmd) {
        Long ipId = cmd.getIpAddressId();
        Long id = cmd.getId();
        Long networkId = cmd.getNetworkId();
        Map<String, String> tags = cmd.getTags();
        FirewallRule.TrafficType trafficType = cmd.getTrafficType();
        Boolean display = cmd.getDisplay();

        Account caller = CallContext.current().getCallingAccount();
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
        sb.and("trafficType", sb.entity().getTrafficType(), Op.EQ);
            sb.and("networkId", sb.entity().getNetworkId(), Op.EQ);
        sb.and("ip", sb.entity().getSourceIpAddressId(), Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), Op.EQ);
        sb.and("display", sb.entity().isDisplay(), Op.EQ);

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
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
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.FirewallRule.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (display != null) {
            sc.setParameters("display", display);
        }

        if (ipId != null) {
            sc.setParameters("ip", ipId);
        }

            if (networkId != null) {
                sc.setParameters("networkId", networkId);
            }

        sc.setParameters("purpose", Purpose.Firewall);
        sc.setParameters("trafficType", trafficType);

        Pair<List<FirewallRuleVO>, Integer> result = _firewallDao.searchAndCount(sc, filter);
        return new Pair<List<? extends FirewallRule>, Integer>(result.first(), result.second());
    }

    //Intermediate function used in detectRulesConflict to check for the conflicting cidrs in the rules already applied and the newRule being applied.
    boolean detectConflictingCidrs(List<String> cidrList1, List<String> cidrList2){
        if(cidrList1.isEmpty() && cidrList2.isEmpty()){
            return true;
        }

        Collection<String> similar = new HashSet<String>(cidrList1);
        similar.retainAll(cidrList2);
        return (similar.size()>0);
    }

    @Override
    public void detectRulesConflict(FirewallRule newRule) throws NetworkRuleConflictException {
        List<FirewallRuleVO> rules;
        if (newRule.getSourceIpAddressId() != null) {
             rules = _firewallDao.listByIpAndPurposeAndNotRevoked(newRule.getSourceIpAddressId(), null);
            assert (rules.size() >= 1) : "For network rules, we now always first persist the rule and then check for "
                + "network conflicts so we should at least have one rule at this point.";
        } else {
            // fetches only firewall egress rules.
            rules = _firewallDao.listByNetworkPurposeTrafficTypeAndNotRevoked(newRule.getNetworkId(), Purpose.Firewall, newRule.getTrafficType());
            assert (rules.size() >= 1);
        }

        for (FirewallRuleVO rule : rules) {
            if (rule.getId() == newRule.getId()) {
                continue; // Skips my own rule.
            }

            boolean oneOfRulesIsFirewall =
                ((rule.getPurpose() == Purpose.Firewall || newRule.getPurpose() == Purpose.Firewall) && ((newRule.getPurpose() != rule.getPurpose()) || (!newRule.getProtocol()
                            .equalsIgnoreCase(rule.getProtocol()))));

            // if both rules are firewall and their cidrs are different, we can skip port ranges verification
            boolean bothRulesFirewall = (rule.getPurpose() == newRule.getPurpose() && rule.getPurpose() == Purpose.Firewall);
            boolean duplicatedCidrs = false;
            if (bothRulesFirewall) {
                _firewallDao.loadSourceCidrs(rule);
                _firewallDao.loadSourceCidrs((FirewallRuleVO)newRule);

                _firewallDao.loadDestinationCidrs(rule);
                _firewallDao.loadDestinationCidrs((FirewallRuleVO) newRule);

                if (rule.getSourceCidrList() == null || newRule.getSourceCidrList() == null) {
                    continue;
                }
                duplicatedCidrs = (detectConflictingCidrs(rule.getSourceCidrList(), newRule.getSourceCidrList()) && detectConflictingCidrs(rule.getDestinationCidrList(), newRule.getDestinationCidrList()));
            }

            if (!oneOfRulesIsFirewall) {
                if (rule.getPurpose() == Purpose.StaticNat && newRule.getPurpose() != Purpose.StaticNat) {
                    throw new NetworkRuleConflictException("There is 1 to 1 Nat rule specified for the ip address id=" + newRule.getSourceIpAddressId());
                } else if (rule.getPurpose() != Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat) {
                    throw new NetworkRuleConflictException("There is already firewall rule specified for the ip address id=" + newRule.getSourceIpAddressId());
                }
            }

            // Checking if the rule applied is to the same network that is passed in the rule.
            if (rule.getNetworkId() != newRule.getNetworkId() && rule.getState() != State.Revoke) {
                throw new NetworkRuleConflictException("New rule is for a different network than what's specified in rule " + rule.getXid());
            }

            //Check for the ICMP protocol. This has to be done separately from other protocols as we need to check the ICMP codes and ICMP type also.
            if (newRule.getProtocol().equalsIgnoreCase(NetUtils.ICMP_PROTO) && newRule.getProtocol().equalsIgnoreCase(rule.getProtocol())) {
                if (newRule.getIcmpCode().longValue() == rule.getIcmpCode().longValue() && newRule.getIcmpType().longValue() == rule.getIcmpType().longValue() &&
                    newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()) && duplicatedCidrs) {
                    throw new InvalidParameterValueException("New rule conflicts with existing rule id=" + rule.getId());
                }
            }

            boolean notNullPorts =
                (newRule.getSourcePortStart() != null && newRule.getSourcePortEnd() != null && rule.getSourcePortStart() != null && rule.getSourcePortEnd() != null);
            boolean nullPorts =
                (newRule.getSourcePortStart() == null && newRule.getSourcePortEnd() == null && rule.getSourcePortStart() == null && rule.getSourcePortEnd() == null);

            // If ports are not specified and cidrs are same and protocol is also same(NOT ICMP as it is separately checked above)
            if(nullPorts && duplicatedCidrs && (rule.getProtocol().equalsIgnoreCase(newRule.getProtocol())) && !newRule.getProtocol().equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
                throw new NetworkRuleConflictException("There is already a firewall rule specified with protocol = " +newRule.getProtocol()+ " and no ports");
            }

            if (!notNullPorts) {
                continue;
            } else if (!oneOfRulesIsFirewall &&
                !(bothRulesFirewall && !duplicatedCidrs) &&
                ((rule.getSourcePortStart().intValue() <= newRule.getSourcePortStart().intValue() &&
                    rule.getSourcePortEnd().intValue() >= newRule.getSourcePortStart().intValue()) ||
                    (rule.getSourcePortStart().intValue() <= newRule.getSourcePortEnd().intValue() &&
                    rule.getSourcePortEnd().intValue() >= newRule.getSourcePortEnd().intValue()) ||
                    (newRule.getSourcePortStart().intValue() <= rule.getSourcePortStart().intValue() &&
                    newRule.getSourcePortEnd().intValue() >= rule.getSourcePortStart().intValue()) ||
                (newRule.getSourcePortStart().intValue() <= rule.getSourcePortEnd().intValue() &&
                newRule.getSourcePortEnd().intValue() >= rule.getSourcePortEnd().intValue()))) {
                //Above else if conditions checks for the conflicting port ranges.

                // we allow port forwarding rules with the same parameters but different protocols
                boolean allowPf =
                    (rule.getPurpose() == Purpose.PortForwarding && newRule.getPurpose() == Purpose.PortForwarding && !newRule.getProtocol().equalsIgnoreCase(
                        rule.getProtocol())) || (rule.getPurpose() == Purpose.Vpn && newRule.getPurpose() == Purpose.PortForwarding && !newRule.getProtocol().equalsIgnoreCase(
                            rule.getProtocol()));
                boolean allowStaticNat =
                    (rule.getPurpose() == Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));

                boolean allowVpnPf =
                        (rule.getPurpose() == Purpose.PortForwarding && newRule.getPurpose() == Purpose.Vpn && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));

                boolean allowVpnLb =
                        (rule.getPurpose() == Purpose.LoadBalancing && newRule.getPurpose() == Purpose.Vpn && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));

                if (!(allowPf || allowStaticNat || oneOfRulesIsFirewall || allowVpnPf || allowVpnLb)) {
                    throw new NetworkRuleConflictException("The range specified, " + newRule.getSourcePortStart() + "-" + newRule.getSourcePortEnd() +
                        ", conflicts with rule " + rule.getId() + " which has " + rule.getSourcePortStart() + "-" + rule.getSourcePortEnd());
                }
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("No network rule conflicts detected for " + newRule + " against " + (rules.size() - 1) + " existing rules");
        }
    }

    @Override
    public void validateFirewallRule(Account caller, IPAddressVO ipAddress, Integer portStart, Integer portEnd, String proto, Purpose purpose, FirewallRuleType type,
        Long networkId, FirewallRule.TrafficType trafficType) {
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

        if (ipAddress != null) {
            if (ipAddress.getAssociatedWithNetworkId() == null) {
                    throw new InvalidParameterValueException("Unable to create firewall rule ; ip with specified id is not associated with any network");
            } else {
                networkId = ipAddress.getAssociatedWithNetworkId();
            }

            // Validate ip address
            _accountMgr.checkAccess(caller, null, true, ipAddress);
        }

        //network id either has to be passed explicitly, or implicitly as a part of ipAddress object
        if (networkId == null) {
            throw new InvalidParameterValueException("Unable to retrieve network id to validate the rule");
        }

        Network network = _networkModel.getNetwork(networkId);
        assert network != null : "Can't create rule as network associated with public ip address is null?";

        if (trafficType == FirewallRule.TrafficType.Egress) {
            _accountMgr.checkAccess(caller, null, true, network);
        }

        // Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> caps = null;

        if (purpose == Purpose.LoadBalancing) {
            if (!_elbEnabled) {
                caps = _networkModel.getNetworkServiceCapabilities(network.getId(), Service.Lb);
            }
        } else if (purpose == Purpose.PortForwarding) {
            caps = _networkModel.getNetworkServiceCapabilities(network.getId(), Service.PortForwarding);
        } else if (purpose == Purpose.Firewall) {
            caps = _networkModel.getNetworkServiceCapabilities(network.getId(), Service.Firewall);
        }

        if (caps != null) {
            String supportedProtocols;
            String supportedTrafficTypes = null;
            if (purpose == FirewallRule.Purpose.Firewall) {
                supportedTrafficTypes = caps.get(Capability.SupportedTrafficDirection).toLowerCase();
            }

            if (purpose == FirewallRule.Purpose.Firewall && trafficType == FirewallRule.TrafficType.Egress) {
                supportedProtocols = caps.get(Capability.SupportedEgressProtocols).toLowerCase();
            } else {
                supportedProtocols = caps.get(Capability.SupportedProtocols).toLowerCase();
            }

            if (!supportedProtocols.contains(proto.toLowerCase())) {
                throw new InvalidParameterValueException("Protocol " + proto + " is not supported in zone " + network.getDataCenterId());
            } else if (proto.equalsIgnoreCase(NetUtils.ICMP_PROTO) && purpose != Purpose.Firewall) {
                throw new InvalidParameterValueException("Protocol " + proto + " is currently supported only for rules with purpose " + Purpose.Firewall);
            } else if (purpose == Purpose.Firewall && !supportedTrafficTypes.contains(trafficType.toString().toLowerCase())) {
                throw new InvalidParameterValueException("Traffic Type " + trafficType + " is currently supported by Firewall in network " + networkId);
            }
        }

    }

    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError, boolean updateRulesInDB) throws ResourceUnavailableException {
        boolean success = true;
        if (rules == null || rules.size() == 0) {
            s_logger.debug("There are no rules to forward to the network elements");
            return true;
        }
        Purpose purpose = rules.get(0).getPurpose();
        if (!_ipAddrMgr.applyRules(rules, purpose, this, continueOnError)) {
            s_logger.warn("Rules are not completely applied");
            return false;
        } else {
            if (updateRulesInDB) {
                for (FirewallRule rule : rules) {
                    if (rule.getState() == FirewallRule.State.Revoke) {
                        FirewallRuleVO relatedRule = _firewallDao.findByRelatedId(rule.getId());
                        if (relatedRule != null) {
                            s_logger.warn("Can't remove the firewall rule id=" + rule.getId() + " as it has related firewall rule id=" + relatedRule.getId() +
                                "; leaving it in Revoke state");
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
    public boolean applyRules(Network network, Purpose purpose, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        boolean handled = false;
        switch (purpose) {
        /* StaticNatRule would be applied by Firewall provider, since the incompatible of two object */
        case StaticNat:
        case Firewall:
                for (FirewallServiceProvider fwElement : _firewallElements) {
                Network.Provider provider = fwElement.getProvider();
                boolean  isFwProvider = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Firewall, provider);
                if (!isFwProvider) {
                    continue;
                }
                handled = fwElement.applyFWRules(network, rules);
                if (handled)
                    break;
            }
            break;
        case PortForwarding:
                for (PortForwardingServiceProvider element : _pfElements) {
                Network.Provider provider = element.getProvider();
                boolean  isPfProvider = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.PortForwarding, provider);
                if (!isPfProvider) {
                    continue;
                }
                    handled = element.applyPFRules(network, (List<PortForwardingRule>)rules);
                if (handled)
                    break;
            }
            break;
            /*        case NetworkACL:
            for (NetworkACLServiceProvider element: _networkAclElements) {
                Network.Provider provider = element.getProvider();
                boolean  isAclProvider = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.NetworkACL, provider);
                if (!isAclProvider) {
                    continue;
                }
                handled = element.applyNetworkACLs(network, rules);
                if (handled)
                    break;
            }
            break;*/
        default:
                assert (false) : "Unexpected fall through in applying rules to the network elements";
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
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating firewall rule", async = true)
    public boolean applyIngressFwRules(long ipId, Account caller) throws ResourceUnavailableException {
        return applyIngressFirewallRules(ipId, caller);
    }

    @Override
    public boolean applyIngressFirewallRules(long ipId, Account caller) throws ResourceUnavailableException {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurpose(ipId, Purpose.Firewall);
        return applyFirewallRules(rules, false, caller);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_EGRESS_OPEN, eventDescription = "creating egress firewall rule", async = true)
    public boolean applyEgressFirewallRules(FirewallRule rule, Account caller) throws ResourceUnavailableException {
                List<FirewallRuleVO> rules = _firewallDao.listByNetworkPurposeTrafficType(rule.getNetworkId(), Purpose.Firewall, FirewallRule.TrafficType.Egress);
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
            rule.setDestinationCidrsList(_firewallDcidrsDao.getDestCidrs(rule.getId()));
        }

        if (caller != null) {
            _accountMgr.checkAccess(caller, null, true, rules.toArray(new FirewallRuleVO[rules.size()]));
        }

        try {
            if (!applyRules(rules, continueOnError, true)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to : "+ ex.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean applyDefaultEgressFirewallRule(Long networkId, boolean defaultPolicy, boolean add) throws ResourceUnavailableException {

        s_logger.debug("applying default firewall egress rules ");

        NetworkVO network = _networkDao.findById(networkId);
        List<String> sourceCidr = new ArrayList<String>();
        List<String> destCidr = new ArrayList<String>();


        sourceCidr.add(network.getCidr());
        destCidr.add(NetUtils.ALL_IP4_CIDRS);

        FirewallRuleVO ruleVO =
            new FirewallRuleVO(null, null, null, null, "all", networkId, network.getAccountId(), network.getDomainId(), Purpose.Firewall, sourceCidr, destCidr, null, null, null,
                FirewallRule.TrafficType.Egress, FirewallRuleType.System);
        ruleVO.setState(add ? State.Add : State.Revoke);
        List<FirewallRuleVO> rules = new ArrayList<FirewallRuleVO>();
        rules.add(ruleVO);

        try {
            //this is not required to store in db because we don't to add this rule along with the normal rules
            if (!applyRules(rules, false, false)) {
                return  false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply default egress rules for guest network due to ", ex);
            return false;
        }
        return true;
    }

    protected boolean revokeFirewallRule(long ruleId, boolean apply, Account caller, long userId) {

        FirewallRuleVO rule = _firewallDao.findById(ruleId);
        if (rule == null || rule.getPurpose() != Purpose.Firewall) {
            throw new InvalidParameterValueException("Unable to find " + ruleId + " having purpose " + Purpose.Firewall);
        }

        if (rule.getType() == FirewallRuleType.System && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new InvalidParameterValueException("Only root admin can delete the system wide firewall rule");
        }

        _accountMgr.checkAccess(caller, null, true, rule);

        revokeRule(rule, caller, userId, false);

        boolean success = false;
        Long networkId = rule.getNetworkId();

        if (apply) {
            // ingress firewall rule
            if (rule.getSourceIpAddressId() != null) {
                //feteches ingress firewall, ingress firewall rules associated with the ip
            List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurpose(rule.getSourceIpAddressId(), Purpose.Firewall);
            return applyFirewallRules(rules, false, caller);
                //egress firewall rule
            } else if (networkId != null) {
                List<FirewallRuleVO> rules = _firewallDao.listByNetworkPurposeTrafficType(rule.getNetworkId(), Purpose.Firewall, FirewallRule.TrafficType.Egress);
                return applyFirewallRules(rules, false, caller);
            }
        } else {
            success = true;
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_CLOSE, eventDescription = "revoking firewall rule", async = true)
    public boolean revokeIngressFwRule(long ruleId, boolean apply) {
        return revokeIngressFirewallRule(ruleId, apply);
    }


    @Override
    public boolean revokeIngressFirewallRule(long ruleId, boolean apply) {
        Account caller = CallContext.current().getCallingAccount();
        long userId = CallContext.current().getCallingUserId();
        return revokeFirewallRule(ruleId, apply, caller, userId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_EGRESS_CLOSE, eventDescription = "revoking egress firewall rule", async = true)
    public boolean revokeEgressFirewallRule(long ruleId, boolean apply) {
        Account caller = CallContext.current().getCallingAccount();
        long userId = CallContext.current().getCallingUserId();
        return revokeFirewallRule(ruleId, apply, caller, userId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_UPDATE, eventDescription = "updating firewall rule", async = true)
    public FirewallRule updateIngressFirewallRule(long ruleId, String customId, Boolean forDisplay) {
        Account caller = CallContext.current().getCallingAccount();
        return updateFirewallRule(ruleId, customId, caller, forDisplay);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_EGRESS_UPDATE, eventDescription = "updating egress firewall rule", async = true)
    public FirewallRule updateEgressFirewallRule(long ruleId, String customId, Boolean forDisplay) {
        Account caller = CallContext.current().getCallingAccount();
        return updateFirewallRule(ruleId, customId, caller, forDisplay);
    }

    protected FirewallRule updateFirewallRule(long ruleId, String customId, Account caller, Boolean forDisplay) {
        FirewallRuleVO rule = _firewallDao.findById(ruleId);
        if (rule == null || rule.getPurpose() != Purpose.Firewall) {
            throw new InvalidParameterValueException("Unable to find " + ruleId + " having purpose " + Purpose.Firewall);
        }

        if (rule.getType() == FirewallRuleType.System && caller.getType() != Account.Type.ADMIN) {
            throw new InvalidParameterValueException("Only root admin can update the system wide firewall rule");
        }

        _accountMgr.checkAccess(caller, null, true, rule);

        if (customId != null) {
            rule.setUuid(customId);
        }

        if (forDisplay != null) {
            rule.setDisplay(forDisplay);
        }

        _firewallDao.update(ruleId, rule);

        return _firewallDao.findById(ruleId);
    }

    @Override
    @DB
    public void revokeRule(final FirewallRuleVO rule, Account caller, long userId, final boolean needUsageEvent) {
        if (caller != null) {
            _accountMgr.checkAccess(caller, null, true, rule);
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
        boolean generateUsageEvent = false;

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
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_RULE_DELETE, rule.getAccountId(), 0, rule.getId(), null, rule.getClass().getName(),
                        rule.getUuid());
        }
            }
        });
    }

    @Override
    public FirewallRule getFirewallRule(long ruleId) {
        return _firewallDao.findById(ruleId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_CLOSE, eventDescription = "revoking firewall rule", async = true)
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
        //apply rules
        if (!applyFirewallRules(rulesToApply, rulesContinueOnErrFlag, caller)) {
            if (!rulesContinueOnErrFlag) {
                return false;
            }
        }
        // Now we check again in case more rules have been inserted.
        rules.addAll(_firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.Firewall));

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released firewall rules for ip id=" + ipId + " and # of rules now = " + rules.size());
        }

        return rules.size() == 0;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating firewall rule", create = true)
    public FirewallRule createRuleForAllCidrs(long ipAddrId, Account caller, Integer startPort, Integer endPort, String protocol, Integer icmpCode, Integer icmpType,
        Long relatedRuleId, long networkId) throws NetworkRuleConflictException {

        // If firwallRule for this port range already exists, return it
        List<FirewallRuleVO> rules = _firewallDao.listByIpPurposeAndProtocolAndNotRevoked(ipAddrId, startPort, endPort, protocol, Purpose.Firewall);
        if (!rules.isEmpty()) {
            return rules.get(0);
        }

        List<String> oneCidr = new ArrayList<String>();
        oneCidr.add(NetUtils.ALL_IP4_CIDRS);
        return createFirewallRule(ipAddrId, caller, null, startPort, endPort, protocol, oneCidr, null, icmpCode, icmpType, relatedRuleId, FirewallRule.FirewallRuleType.User,
            networkId, FirewallRule.TrafficType.Ingress, true);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_CLOSE, eventDescription = "revoking firewall rule", async = true)
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
        return revokeIngressFirewallRule(fwRule.getId(), apply);

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_CLOSE, eventDescription = "revoking firewall rule", async = true)
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
                success = success && applyIngressFirewallRules(ipId, _accountMgr.getSystemAccount());
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Failed to apply firewall rules for ip id=" + ipId);
                success = false;
            }
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating firewall rule", create = true)
    public boolean addSystemFirewallRules(IPAddressVO ip, Account acct) {
        List<FirewallRuleVO> systemRules = _firewallDao.listSystemRules();
        for (FirewallRuleVO rule : systemRules) {
            try {
                if (rule.getSourceCidrList() == null && (rule.getPurpose() == Purpose.Firewall || rule.getPurpose() == Purpose.NetworkACL)) {
                    _firewallDao.loadSourceCidrs(rule);
                }
                createFirewallRule(ip.getId(), acct, rule.getXid(), rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getProtocol(), rule.getSourceCidrList(),null,
                        rule.getIcmpCode(), rule.getIcmpType(), rule.getRelated(), FirewallRuleType.System, rule.getNetworkId(), rule.getTrafficType(), true);
            } catch (Exception e) {
                s_logger.debug("Failed to add system wide firewall rule, due to:" + e.toString());
            }
        }
        return true;
    }

    public List<FirewallServiceProvider> getFirewallElements() {
        return _firewallElements;
    }

    @Inject
    public void setFirewallElements(List<FirewallServiceProvider> firewallElements) {
        _firewallElements = firewallElements;
    }

    public List<PortForwardingServiceProvider> getPfElements() {
        return _pfElements;
    }

    @Inject
    public void setPfElements(List<PortForwardingServiceProvider> pfElements) {
        _pfElements = pfElements;
    }

    public List<StaticNatServiceProvider> getStaticNatElements() {
        return _staticNatElements;
    }

    @Inject
    public void setStaticNatElements(List<StaticNatServiceProvider> staticNatElements) {
        _staticNatElements = staticNatElements;
    }

    public List<NetworkACLServiceProvider> getNetworkAclElements() {
        return _networkAclElements;
    }

    @Inject
    public void setNetworkAclElements(List<NetworkACLServiceProvider> networkAclElements) {
        _networkAclElements = networkAclElements;
    }

}
