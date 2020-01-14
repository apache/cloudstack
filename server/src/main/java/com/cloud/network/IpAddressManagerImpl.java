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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.response.AcquirePodIpCmdResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.region.PortableIp;
import org.apache.cloudstack.region.PortableIpDao;
import org.apache.cloudstack.region.PortableIpVO;
import org.apache.cloudstack.region.Region;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DomainVlanMapVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.DataCenterVnetDao;
import com.cloud.dc.dao.DomainVlanMapDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddress.State;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkAccountDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.IpDeployingRequester;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Journal;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class IpAddressManagerImpl extends ManagerBase implements IpAddressManager, Configurable {
    private static final Logger s_logger = Logger.getLogger(IpAddressManagerImpl.class);

    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    EntityManager _entityMgr;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    UserDao _userDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    AlertManager _alertMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    AccountVlanMapDao _accountVlanMapDao;
    @Inject
    DomainVlanMapDao _domainVlanMapDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NetworkDao _networksDao;
    @Inject
    NicDao _nicDao;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    RemoteAccessVpnService _vpnMgr;
    @Inject
    PodVlanMapDao _podVlanMapDao;
    @Inject
    NetworkOfferingDetailsDao _ntwkOffDetailsDao;
    @Inject
    AccountGuestVlanMapDao _accountGuestVlanMapDao;
    @Inject
    DataCenterVnetDao _datacenterVnetDao;
    @Inject
    NetworkAccountDao _networkAccountDao;
    @Inject
    protected NicIpAliasDao _nicIpAliasDao;
    @Inject
    protected IPAddressDao _publicIpAddressDao;
    @Inject
    NetworkDomainDao _networkDomainDao;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    FirewallManager _firewallMgr;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;

    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNSPDao;
    @Inject
    PortForwardingRulesDao _portForwardingRulesDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _pNTrafficTypeDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    HostDao _hostDao;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    StorageNetworkManager _stnwMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    NetworkACLManager _networkACLMgr;
    @Inject
    UsageEventDao _usageEventDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    Ipv6AddressManager _ipv6Mgr;
    @Inject
    PortableIpDao _portableIpDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    DataCenterIpAddressDao _privateIPAddressDao;
    @Inject
    HostPodDao _hpDao;

    SearchBuilder<IPAddressVO> AssignIpAddressSearch;
    SearchBuilder<IPAddressVO> AssignIpAddressFromPodVlanSearch;
    private static final Object allocatedLock = new Object();

    static Boolean rulesContinueOnErrFlag = true;

    private static final ConfigKey<Boolean> SystemVmPublicIpReservationModeStrictness = new ConfigKey<Boolean>("Advanced",
            Boolean.class, "system.vm.public.ip.reservation.mode.strictness", "false",
            "If enabled, the use of System VMs public IP reservation is strict, preferred if not.", false, ConfigKey.Scope.Global);

    private Random rand = new Random(System.currentTimeMillis());

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        // populate providers
        Map<Network.Service, Set<Network.Provider>> defaultSharedNetworkOfferingProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        Set<Network.Provider> defaultProviders = new HashSet<Network.Provider>();

        defaultProviders.add(Network.Provider.VirtualRouter);
        defaultSharedNetworkOfferingProviders.put(Service.Dhcp, defaultProviders);
        defaultSharedNetworkOfferingProviders.put(Service.Dns, defaultProviders);
        defaultSharedNetworkOfferingProviders.put(Service.UserData, defaultProviders);

        Map<Network.Service, Set<Network.Provider>> defaultIsolatedNetworkOfferingProviders = defaultSharedNetworkOfferingProviders;
        defaultIsolatedNetworkOfferingProviders.put(Service.Dhcp, defaultProviders);
        defaultIsolatedNetworkOfferingProviders.put(Service.Dns, defaultProviders);
        defaultIsolatedNetworkOfferingProviders.put(Service.UserData, defaultProviders);
        defaultIsolatedNetworkOfferingProviders.put(Service.Firewall, defaultProviders);
        defaultIsolatedNetworkOfferingProviders.put(Service.Gateway, defaultProviders);
        defaultIsolatedNetworkOfferingProviders.put(Service.Lb, defaultProviders);
        defaultIsolatedNetworkOfferingProviders.put(Service.StaticNat, defaultProviders);
        defaultIsolatedNetworkOfferingProviders.put(Service.PortForwarding, defaultProviders);
        defaultIsolatedNetworkOfferingProviders.put(Service.Vpn, defaultProviders);

        Map<Network.Service, Set<Network.Provider>> defaultSharedSGEnabledNetworkOfferingProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        defaultSharedSGEnabledNetworkOfferingProviders.put(Service.Dhcp, defaultProviders);
        defaultSharedSGEnabledNetworkOfferingProviders.put(Service.Dns, defaultProviders);
        defaultSharedSGEnabledNetworkOfferingProviders.put(Service.UserData, defaultProviders);
        Set<Provider> sgProviders = new HashSet<Provider>();
        sgProviders.add(Provider.SecurityGroupProvider);
        defaultSharedSGEnabledNetworkOfferingProviders.put(Service.SecurityGroup, sgProviders);

        Map<Network.Service, Set<Network.Provider>> defaultIsolatedSourceNatEnabledNetworkOfferingProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        defaultProviders.clear();
        defaultProviders.add(Network.Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Dhcp, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Dns, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.UserData, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Firewall, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Gateway, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Lb, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.SourceNat, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.StaticNat, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.PortForwarding, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Vpn, defaultProviders);

        Map<Network.Service, Set<Network.Provider>> defaultVPCOffProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        defaultProviders.clear();
        defaultProviders.add(Network.Provider.VirtualRouter);
        defaultVPCOffProviders.put(Service.Dhcp, defaultProviders);
        defaultVPCOffProviders.put(Service.Dns, defaultProviders);
        defaultVPCOffProviders.put(Service.UserData, defaultProviders);
        defaultVPCOffProviders.put(Service.NetworkACL, defaultProviders);
        defaultVPCOffProviders.put(Service.Gateway, defaultProviders);
        defaultVPCOffProviders.put(Service.Lb, defaultProviders);
        defaultVPCOffProviders.put(Service.SourceNat, defaultProviders);
        defaultVPCOffProviders.put(Service.StaticNat, defaultProviders);
        defaultVPCOffProviders.put(Service.PortForwarding, defaultProviders);
        defaultVPCOffProviders.put(Service.Vpn, defaultProviders);

        //#8 - network offering with internal lb service
        Map<Network.Service, Set<Network.Provider>> internalLbOffProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        Set<Network.Provider> defaultVpcProvider = new HashSet<Network.Provider>();
        defaultVpcProvider.add(Network.Provider.VPCVirtualRouter);

        Set<Network.Provider> defaultInternalLbProvider = new HashSet<Network.Provider>();
        defaultInternalLbProvider.add(Network.Provider.InternalLbVm);

        internalLbOffProviders.put(Service.Dhcp, defaultVpcProvider);
        internalLbOffProviders.put(Service.Dns, defaultVpcProvider);
        internalLbOffProviders.put(Service.UserData, defaultVpcProvider);
        internalLbOffProviders.put(Service.NetworkACL, defaultVpcProvider);
        internalLbOffProviders.put(Service.Gateway, defaultVpcProvider);
        internalLbOffProviders.put(Service.Lb, defaultInternalLbProvider);
        internalLbOffProviders.put(Service.SourceNat, defaultVpcProvider);

        Map<Network.Service, Set<Network.Provider>> netscalerServiceProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        Set<Network.Provider> vrProvider = new HashSet<Network.Provider>();
        vrProvider.add(Provider.VirtualRouter);
        Set<Network.Provider> sgProvider = new HashSet<Network.Provider>();
        sgProvider.add(Provider.SecurityGroupProvider);
        Set<Network.Provider> nsProvider = new HashSet<Network.Provider>();
        nsProvider.add(Provider.Netscaler);
        netscalerServiceProviders.put(Service.Dhcp, vrProvider);
        netscalerServiceProviders.put(Service.Dns, vrProvider);
        netscalerServiceProviders.put(Service.UserData, vrProvider);
        netscalerServiceProviders.put(Service.SecurityGroup, sgProvider);
        netscalerServiceProviders.put(Service.StaticNat, nsProvider);
        netscalerServiceProviders.put(Service.Lb, nsProvider);

        Map<Service, Map<Capability, String>> serviceCapabilityMap = new HashMap<Service, Map<Capability, String>>();
        Map<Capability, String> elb = new HashMap<Capability, String>();
        elb.put(Capability.ElasticLb, "true");
        Map<Capability, String> eip = new HashMap<Capability, String>();
        eip.put(Capability.ElasticIp, "true");
        serviceCapabilityMap.put(Service.Lb, elb);
        serviceCapabilityMap.put(Service.StaticNat, eip);

        AssignIpAddressSearch = _ipAddressDao.createSearchBuilder();
        AssignIpAddressSearch.and("dc", AssignIpAddressSearch.entity().getDataCenterId(), Op.EQ);
        AssignIpAddressSearch.and("allocated", AssignIpAddressSearch.entity().getAllocatedTime(), Op.NULL);
        AssignIpAddressSearch.and("vlanId", AssignIpAddressSearch.entity().getVlanId(), Op.IN);
        if (SystemVmPublicIpReservationModeStrictness.value()) {
            AssignIpAddressSearch.and("forSystemVms", AssignIpAddressSearch.entity().isForSystemVms(), Op.EQ);
        }
        SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        vlanSearch.and("type", vlanSearch.entity().getVlanType(), Op.EQ);
        vlanSearch.and("networkId", vlanSearch.entity().getNetworkId(), Op.EQ);
        AssignIpAddressSearch.join("vlan", vlanSearch, vlanSearch.entity().getId(), AssignIpAddressSearch.entity().getVlanId(), JoinType.INNER);
        AssignIpAddressSearch.done();

        AssignIpAddressFromPodVlanSearch = _ipAddressDao.createSearchBuilder();
        AssignIpAddressFromPodVlanSearch.and("dc", AssignIpAddressFromPodVlanSearch.entity().getDataCenterId(), Op.EQ);
        AssignIpAddressFromPodVlanSearch.and("allocated", AssignIpAddressFromPodVlanSearch.entity().getAllocatedTime(), Op.NULL);
        AssignIpAddressFromPodVlanSearch.and("vlanId", AssignIpAddressFromPodVlanSearch.entity().getVlanId(), Op.IN);

        SearchBuilder<VlanVO> podVlanSearch = _vlanDao.createSearchBuilder();
        podVlanSearch.and("type", podVlanSearch.entity().getVlanType(), Op.EQ);
        podVlanSearch.and("networkId", podVlanSearch.entity().getNetworkId(), Op.EQ);
        SearchBuilder<PodVlanMapVO> podVlanMapSB = _podVlanMapDao.createSearchBuilder();
        podVlanMapSB.and("podId", podVlanMapSB.entity().getPodId(), Op.EQ);
        AssignIpAddressFromPodVlanSearch.join("podVlanMapSB", podVlanMapSB, podVlanMapSB.entity().getVlanDbId(), AssignIpAddressFromPodVlanSearch.entity().getVlanId(),
                JoinType.INNER);
        AssignIpAddressFromPodVlanSearch.join("vlan", podVlanSearch, podVlanSearch.entity().getId(), AssignIpAddressFromPodVlanSearch.entity().getVlanId(), JoinType.INNER);
        AssignIpAddressFromPodVlanSearch.done();

        Network.State.getStateMachine().registerListener(new NetworkStateListener(_configDao));

        if (RulesContinueOnError.value() != null) {
            rulesContinueOnErrFlag = RulesContinueOnError.value();
        }

        s_logger.info("IPAddress Manager is configured.");

        return true;
    }

    private IpAddress allocateIP(Account ipOwner, boolean isSystem, long zoneId) throws ResourceAllocationException, InsufficientAddressCapacityException,
            ConcurrentOperationException {
        Account caller = CallContext.current().getCallingAccount();
        long callerUserId = CallContext.current().getCallingUserId();
        // check permissions
        _accountMgr.checkAccess(caller, null, false, ipOwner);

        DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);

        return allocateIp(ipOwner, isSystem, caller, callerUserId, zone, null, null);
    }

    // An IP association is required in below cases
    //  1.there is at least one public IP associated with the network on which first rule (PF/static NAT/LB) is being applied.
    //  2.last rule (PF/static NAT/LB) on the public IP has been revoked. So the public IP should not be associated with any provider
    boolean checkIfIpAssocRequired(Network network, boolean postApplyRules, List<PublicIp> publicIps) {

        if (network.getState() == Network.State.Implementing) {
            return true;
        }

        for (PublicIp ip : publicIps) {
            if (ip.isSourceNat()) {
                continue;
            } else if (ip.isOneToOneNat()) {
                continue;
            } else {
                Long totalCount = null;
                Long revokeCount = null;
                Long activeCount = null;
                Long addCount = null;

                totalCount = _firewallDao.countRulesByIpId(ip.getId());
                if (postApplyRules) {
                    revokeCount = _firewallDao.countRulesByIpIdAndState(ip.getId(), FirewallRule.State.Revoke);
                } else {
                    activeCount = _firewallDao.countRulesByIpIdAndState(ip.getId(), FirewallRule.State.Active);
                    addCount = _firewallDao.countRulesByIpIdAndState(ip.getId(), FirewallRule.State.Add);
                }

                if (totalCount == null || totalCount.longValue() == 0L) {
                    continue;
                }

                if (postApplyRules) {

                    if (revokeCount != null && revokeCount.longValue() == totalCount.longValue()) {
                        s_logger.trace("All rules are in Revoke state, have to dis-assiciate IP from the backend");
                        return true;
                    }
                } else {
                    if (activeCount != null && activeCount > 0) {
                        if (network.getVpcId() != null) {
                            // If there are more than one ip in the vpc tier network and services configured on it.
                            // restart network with cleanup case, on network reprogramming this needs to be return true
                            // because on the VR ips has removed. In VPC case restart tier network with cleanup will not
                            // reboot the VR. So ipassoc is needed.
                            return true;
                        }
                        continue;
                    } else if (addCount != null && addCount.longValue() == totalCount.longValue()) {
                        s_logger.trace("All rules are in Add state, have to assiciate IP with the backend");
                        return true;
                    } else {
                        continue;
                    }
                }
            }
        }

        // there are no IP's corresponding to this network that need to be associated with provider
        return false;
    }

    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, FirewallRule.Purpose purpose, NetworkRuleApplier applier, boolean continueOnError)
            throws ResourceUnavailableException {
        if (rules == null || rules.size() == 0) {
            s_logger.debug("There are no rules to forward to the network elements");
            return true;
        }

        boolean success = true;
        Network network = _networksDao.findById(rules.get(0).getNetworkId());
        FirewallRuleVO.TrafficType trafficType = rules.get(0).getTrafficType();
        List<PublicIp> publicIps = new ArrayList<PublicIp>();

        if (!(rules.get(0).getPurpose() == FirewallRule.Purpose.Firewall && trafficType == FirewallRule.TrafficType.Egress)) {
            // get the list of public ip's owned by the network
            List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
            if (userIps != null && !userIps.isEmpty()) {
                for (IPAddressVO userIp : userIps) {
                    PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                    publicIps.add(publicIp);
                }
            }
        }
        // rules can not programmed unless IP is associated with network service provider, so run IP assoication for
        // the network so as to ensure IP is associated before applying rules (in add state)
        if (checkIfIpAssocRequired(network, false, publicIps)) {
            applyIpAssociations(network, false, continueOnError, publicIps);
        }

        try {
            applier.applyRules(network, purpose, rules);
        } catch (ResourceUnavailableException e) {
            if (!continueOnError) {
                throw e;
            }
            s_logger.warn("Problems with applying " + purpose + " rules but pushing on", e);
            success = false;
        }

        // if there are no active rules associated with a public IP, then public IP need not be associated with a provider.
        // This IPAssoc ensures, public IP is dis-associated after last active rule is revoked.
        if (checkIfIpAssocRequired(network, true, publicIps)) {
            applyIpAssociations(network, true, continueOnError, publicIps);
        }

        return success;
    }

    protected boolean cleanupIpResources(long ipId, long userId, Account caller) {
        boolean success = true;

        // Revoke all firewall rules for the ip
        try {
            s_logger.debug("Revoking all " + Purpose.Firewall + "rules as a part of public IP id=" + ipId + " release...");
            if (!_firewallMgr.revokeFirewallRulesForIp(ipId, userId, caller)) {
                s_logger.warn("Unable to revoke all the firewall rules for ip id=" + ipId + " as a part of ip release");
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to revoke all firewall rules for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }

        // Revoke all PF/Static nat rules for the ip
        try {
            s_logger.debug("Revoking all " + Purpose.PortForwarding + "/" + Purpose.StaticNat + " rules as a part of public IP id=" + ipId + " release...");
            if (!_rulesMgr.revokeAllPFAndStaticNatRulesForIp(ipId, userId, caller)) {
                s_logger.warn("Unable to revoke all the port forwarding rules for ip id=" + ipId + " as a part of ip release");
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to revoke all the port forwarding rules for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }

        s_logger.debug("Revoking all " + Purpose.LoadBalancing + " rules as a part of public IP id=" + ipId + " release...");
        if (!_lbMgr.removeAllLoadBalanacersForIp(ipId, caller, userId)) {
            s_logger.warn("Unable to revoke all the load balancer rules for ip id=" + ipId + " as a part of ip release");
            success = false;
        }

        // remote access vpn can be enabled only for static nat ip, so this part should never be executed under normal
        // conditions
        // only when ip address failed to be cleaned up as a part of account destroy and was marked as Releasing, this part of
        // the code would be triggered
        s_logger.debug("Cleaning up remote access vpns as a part of public IP id=" + ipId + " release...");
        try {
            _vpnMgr.destroyRemoteAccessVpnForIp(ipId, caller,false);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to destroy remote access vpn for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }

        return success;
    }

    @Override
    @DB
    public boolean disassociatePublicIpAddress(long addrId, long userId, Account caller) {

        boolean success = true;
        // Cleanup all ip address resources - PF/LB/Static nat rules
        if (!cleanupIpResources(addrId, userId, caller)) {
            success = false;
            s_logger.warn("Failed to release resources for ip address id=" + addrId);
        }

        IPAddressVO ip = markIpAsUnavailable(addrId);
        if (ip == null) {
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip id=" + addrId + "; sourceNat = " + ip.isSourceNat());
        }

        if (ip.getAssociatedWithNetworkId() != null) {
            Network network = _networksDao.findById(ip.getAssociatedWithNetworkId());
            try {
                if (!applyIpAssociations(network, rulesContinueOnErrFlag)) {
                    s_logger.warn("Unable to apply ip address associations for " + network);
                    success = false;
                }
            } catch (ResourceUnavailableException e) {
                throw new CloudRuntimeException("We should never get to here because we used true when applyIpAssociations", e);
            }
        } else {
            if (ip.getState() == IpAddress.State.Releasing) {
                _ipAddressDao.unassignIpAddress(ip.getId());
            }
        }

        if (success) {
            if (ip.isPortable()) {
                releasePortableIpAddress(addrId);
            }
            s_logger.debug("Released a public ip id=" + addrId);
        }

        return success;
    }

    @DB
    @Override
    public boolean releasePortableIpAddress(final long addrId) {
        final GlobalLock portableIpLock = GlobalLock.getInternLock("PortablePublicIpRange");

        try {
            return Transaction.execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction(TransactionStatus status) {
                    portableIpLock.lock(5);
                    IPAddressVO ip = _ipAddressDao.findById(addrId);

                    // unassign portable IP
                    PortableIpVO portableIp = _portableIpDao.findByIpAddress(ip.getAddress().addr());
                    _portableIpDao.unassignIpAddress(portableIp.getId());

                    // removed the provisioned vlan
                    VlanVO vlan = _vlanDao.findById(ip.getVlanId());
                    _vlanDao.remove(vlan.getId());

                    // remove the provisioned public ip address
                    _ipAddressDao.remove(ip.getId());

                    return true;
                }
            });
        } finally {
            portableIpLock.releaseRef();
        }
    }

    @Override
    public PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId, String requestedIp, boolean isSystem, boolean forSystemVms)
            throws InsufficientAddressCapacityException {
        return fetchNewPublicIp(dcId, podId, null, owner, type, networkId, false, true, requestedIp, isSystem, null, null, forSystemVms);
    }

    @Override
    public PublicIp assignPublicIpAddressFromVlans(long dcId, Long podId, Account owner, VlanType type, List<Long> vlanDbIds, Long networkId, String requestedIp, boolean isSystem)
            throws InsufficientAddressCapacityException {
        return fetchNewPublicIp(dcId, podId, vlanDbIds, owner, type, networkId, false, true, requestedIp, isSystem, null, null, false);
    }

    @DB
    public PublicIp fetchNewPublicIp(final long dcId, final Long podId, final List<Long> vlanDbIds, final Account owner, final VlanType vlanUse, final Long guestNetworkId,
            final boolean sourceNat, final boolean assign, final String requestedIp, final boolean isSystem, final Long vpcId, final Boolean displayIp, final boolean forSystemVms)
                    throws InsufficientAddressCapacityException {
        IPAddressVO addr = Transaction.execute(new TransactionCallbackWithException<IPAddressVO, InsufficientAddressCapacityException>() {
            @Override
            public IPAddressVO doInTransaction(TransactionStatus status) throws InsufficientAddressCapacityException {
                StringBuilder errorMessage = new StringBuilder("Unable to get ip address in ");
                boolean fetchFromDedicatedRange = false;
                List<Long> dedicatedVlanDbIds = new ArrayList<Long>();
                List<Long> nonDedicatedVlanDbIds = new ArrayList<Long>();
                DataCenter zone = _entityMgr.findById(DataCenter.class, dcId);

                SearchCriteria<IPAddressVO> sc = null;
                if (podId != null) {
                    sc = AssignIpAddressFromPodVlanSearch.create();
                    sc.setJoinParameters("podVlanMapSB", "podId", podId);
                    errorMessage.append(" pod id=" + podId);
                } else {
                    sc = AssignIpAddressSearch.create();
                    errorMessage.append(" zone id=" + dcId);
                }

                // If owner has dedicated Public IP ranges, fetch IP from the dedicated range
                // Otherwise fetch IP from the system pool
                Network network = _networksDao.findById(guestNetworkId);
                //Checking if network is null in the case of system VM's. At the time of allocation of IP address to systemVm, no network is present.
                if(network == null || !(network.getGuestType() == GuestType.Shared && zone.getNetworkType() == NetworkType.Advanced)) {
                    List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByAccount(owner.getId());
                    for (AccountVlanMapVO map : maps) {
                        if (vlanDbIds == null || vlanDbIds.contains(map.getVlanDbId()))
                            dedicatedVlanDbIds.add(map.getVlanDbId());
                    }
                }
                List<DomainVlanMapVO> domainMaps = _domainVlanMapDao.listDomainVlanMapsByDomain(owner.getDomainId());
                for (DomainVlanMapVO map : domainMaps) {
                    if (vlanDbIds == null || vlanDbIds.contains(map.getVlanDbId()))
                        dedicatedVlanDbIds.add(map.getVlanDbId());
                }
                List<VlanVO> nonDedicatedVlans = _vlanDao.listZoneWideNonDedicatedVlans(dcId);
                for (VlanVO nonDedicatedVlan : nonDedicatedVlans) {
                    if (vlanDbIds == null || vlanDbIds.contains(nonDedicatedVlan.getId()))
                        nonDedicatedVlanDbIds.add(nonDedicatedVlan.getId());
                }
                if (dedicatedVlanDbIds != null && !dedicatedVlanDbIds.isEmpty()) {
                    fetchFromDedicatedRange = true;
                    sc.setParameters("vlanId", dedicatedVlanDbIds.toArray());
                    errorMessage.append(", vlanId id=" + Arrays.toString(dedicatedVlanDbIds.toArray()));
                } else if (nonDedicatedVlanDbIds != null && !nonDedicatedVlanDbIds.isEmpty()) {
                    sc.setParameters("vlanId", nonDedicatedVlanDbIds.toArray());
                    errorMessage.append(", vlanId id=" + Arrays.toString(nonDedicatedVlanDbIds.toArray()));
                } else {
                    if (podId != null) {
                        InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Insufficient address capacity", Pod.class, podId);
                        ex.addProxyObject(ApiDBUtils.findPodById(podId).getUuid());
                        throw ex;
                    }
                    s_logger.warn(errorMessage.toString());
                    InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Insufficient address capacity", DataCenter.class, dcId);
                    ex.addProxyObject(ApiDBUtils.findZoneById(dcId).getUuid());
                    throw ex;
                }

                sc.setParameters("dc", dcId);

                // for direct network take ip addresses only from the vlans belonging to the network
                if (vlanUse == VlanType.DirectAttached) {
                    sc.setJoinParameters("vlan", "networkId", guestNetworkId);
                    errorMessage.append(", network id=" + guestNetworkId);
                }
                sc.setJoinParameters("vlan", "type", vlanUse);

                if (requestedIp != null) {
                    sc.addAnd("address", SearchCriteria.Op.EQ, requestedIp);
                    errorMessage.append(": requested ip " + requestedIp + " is not available");
                }

                boolean ascOrder = ! forSystemVms;
                Filter filter = new Filter(IPAddressVO.class, "forSystemVms", ascOrder, 0l, 1l);
                if (SystemVmPublicIpReservationModeStrictness.value()) {
                    sc.setParameters("forSystemVms", forSystemVms);
                }

                filter.addOrderBy(IPAddressVO.class,"vlanId", true);

                List<IPAddressVO> addrs = _ipAddressDao.search(sc, filter, false);

                // If all the dedicated IPs of the owner are in use fetch an IP from the system pool
                if (addrs.size() == 0 && fetchFromDedicatedRange) {
                    // Verify if account is allowed to acquire IPs from the system
                    boolean useSystemIps = UseSystemPublicIps.valueIn(owner.getId());
                    if (useSystemIps && nonDedicatedVlanDbIds != null && !nonDedicatedVlanDbIds.isEmpty()) {
                        fetchFromDedicatedRange = false;
                        sc.setParameters("vlanId", nonDedicatedVlanDbIds.toArray());
                        errorMessage.append(", vlanId id=" + Arrays.toString(nonDedicatedVlanDbIds.toArray()));
                        addrs = _ipAddressDao.search(sc, filter, false);
                    }
                }

                if (addrs.size() == 0) {
                    if (podId != null) {
                        InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Insufficient address capacity", Pod.class, podId);
                        // for now, we hardcode the table names, but we should ideally do a lookup for the tablename from the VO object.
                        ex.addProxyObject(ApiDBUtils.findPodById(podId).getUuid());
                        throw ex;
                    }
                    s_logger.warn(errorMessage.toString());
                    InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Insufficient address capacity", DataCenter.class, dcId);
                    ex.addProxyObject(ApiDBUtils.findZoneById(dcId).getUuid());
                    throw ex;
                }

                assert(addrs.size() == 1) : "Return size is incorrect: " + addrs.size();

                if (!fetchFromDedicatedRange && VlanType.VirtualNetwork.equals(vlanUse)) {
                    // Check that the maximum number of public IPs for the given accountId will not be exceeded
                    try {
                        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.public_ip);
                    } catch (ResourceAllocationException ex) {
                        s_logger.warn("Failed to allocate resource of type " + ex.getResourceType() + " for account " + owner);
                        throw new AccountLimitException("Maximum number of public IP addresses for account: " + owner.getAccountName() + " has been exceeded.");
                    }
                }

                IPAddressVO finalAddr = null;
                for (final IPAddressVO possibleAddr: addrs) {
                    if (possibleAddr.getState() != IpAddress.State.Free) {
                        continue;
                    }
                    final IPAddressVO addr = possibleAddr;
                    addr.setSourceNat(sourceNat);
                    addr.setAllocatedTime(new Date());
                    addr.setAllocatedInDomainId(owner.getDomainId());
                    addr.setAllocatedToAccountId(owner.getId());
                    addr.setSystem(isSystem);

                    if (displayIp != null) {
                        addr.setDisplay(displayIp);
                    }

                    if (vlanUse != VlanType.DirectAttached) {
                        addr.setAssociatedWithNetworkId(guestNetworkId);
                        addr.setVpcId(vpcId);
                    }
                    if (_ipAddressDao.lockRow(possibleAddr.getId(), true) != null) {
                        final IPAddressVO userIp = _ipAddressDao.findById(addr.getId());
                        if (userIp.getState() == IpAddress.State.Free) {
                            addr.setState(IpAddress.State.Allocating);
                            if (_ipAddressDao.update(addr.getId(), addr)) {
                                finalAddr = addr;
                                break;
                            }
                        }
                    }
                }

                if (finalAddr == null) {
                    s_logger.error("Failed to fetch any free public IP address");
                    throw new CloudRuntimeException("Failed to fetch any free public IP address");
                }

                if (assign) {
                    markPublicIpAsAllocated(finalAddr);
                }

                final State expectedAddressState = assign ? State.Allocated : State.Allocating;
                if (finalAddr.getState() != expectedAddressState) {
                    s_logger.error("Failed to fetch new public IP and get in expected state=" + expectedAddressState);
                    throw new CloudRuntimeException("Failed to fetch new public IP with expected state " + expectedAddressState);
                }

                return finalAddr;
            }
        });

        if (vlanUse == VlanType.VirtualNetwork) {
            _firewallMgr.addSystemFirewallRules(addr, owner);
        }

        return PublicIp.createFromAddrAndVlan(addr, _vlanDao.findById(addr.getVlanId()));
    }

    @DB
    @Override
    public void markPublicIpAsAllocated(final IPAddressVO addr) {
        synchronized (allocatedLock) {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    Account owner = _accountMgr.getAccount(addr.getAllocatedToAccountId());
                    if (_ipAddressDao.lockRow(addr.getId(), true) != null) {
                        final IPAddressVO userIp = _ipAddressDao.findById(addr.getId());
                        if (userIp.getState() == IpAddress.State.Allocating || addr.getState() == IpAddress.State.Free) {
                            addr.setState(IpAddress.State.Allocated);
                            if (_ipAddressDao.update(addr.getId(), addr)) {
                                // Save usage event
                                if (owner.getAccountId() != Account.ACCOUNT_ID_SYSTEM) {
                                    VlanVO vlan = _vlanDao.findById(addr.getVlanId());
                                    String guestType = vlan.getVlanType().toString();
                                    if (!isIpDedicated(addr)) {
                                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_ASSIGN, owner.getId(), addr.getDataCenterId(), addr.getId(),
                                                addr.getAddress().toString(),
                                                addr.isSourceNat(), guestType, addr.getSystem(), addr.getClass().getName(), addr.getUuid());
                                    }
                                    if (updateIpResourceCount(addr)) {
                                        _resourceLimitMgr.incrementResourceCount(owner.getId(), ResourceType.public_ip);
                                    }
                                }
                            } else {
                                s_logger.error("Failed to mark public IP as allocated with id=" + addr.getId() + " address=" + addr.getAddress());
                            }
                        }
                    } else {
                        s_logger.error("Failed to acquire row lock to mark public IP as allocated with id=" + addr.getId() + " address=" + addr.getAddress());
                    }
                }
            });
        }
    }

    private boolean isIpDedicated(IPAddressVO addr) {
        List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByVlan(addr.getVlanId());
        if (maps != null && !maps.isEmpty())
            return true;
        return false;
    }

    @Override
    public PublicIp assignSourceNatIpAddressToGuestNetwork(Account owner, Network guestNetwork) throws InsufficientAddressCapacityException, ConcurrentOperationException {
        assert(guestNetwork.getTrafficType() != null) : "You're asking for a source nat but your network "
                + "can't participate in source nat.  What do you have to say for yourself?";
        long dcId = guestNetwork.getDataCenterId();

        IPAddressVO sourceNatIp = getExistingSourceNatInNetwork(owner.getId(), guestNetwork.getId());

        PublicIp ipToReturn = null;
        if (sourceNatIp != null) {
            ipToReturn = PublicIp.createFromAddrAndVlan(sourceNatIp, _vlanDao.findById(sourceNatIp.getVlanId()));
        } else {
            ipToReturn = assignDedicateIpAddress(owner, guestNetwork.getId(), null, dcId, true);
        }

        return ipToReturn;
    }

    @DB
    @Override
    public PublicIp assignDedicateIpAddress(Account owner, final Long guestNtwkId, final Long vpcId, final long dcId, final boolean isSourceNat)
            throws ConcurrentOperationException, InsufficientAddressCapacityException {

        final long ownerId = owner.getId();

        PublicIp ip = null;
        try {
            ip = Transaction.execute(new TransactionCallbackWithException<PublicIp, InsufficientAddressCapacityException>() {
                @Override
                public PublicIp doInTransaction(TransactionStatus status) throws InsufficientAddressCapacityException {
                    Account owner = _accountDao.acquireInLockTable(ownerId);

                    if (owner == null) {
                        // this ownerId comes from owner or type Account. See the class "AccountVO" and the annotations in that class
                        // to get the table name and field name that is queried to fill this ownerid.
                        ConcurrentOperationException ex = new ConcurrentOperationException("Unable to lock account");
                        throw ex;
                    }
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("lock account " + ownerId + " is acquired");
                    }
                    boolean displayIp = true;
                    if (guestNtwkId != null) {
                        Network ntwk = _networksDao.findById(guestNtwkId);
                        displayIp = ntwk.getDisplayNetwork();
                    } else if (vpcId != null) {
                        VpcVO vpc = _vpcDao.findById(vpcId);
                        displayIp = vpc.isDisplay();
                    }
                    return fetchNewPublicIp(dcId, null, null, owner, VlanType.VirtualNetwork, guestNtwkId, isSourceNat, true, null, false, vpcId, displayIp, false);
                }
            });
            if (ip.getState() != State.Allocated) {
                s_logger.error("Failed to fetch new IP and allocate it for ip with id=" + ip.getId() + ", address=" + ip.getAddress());
            }
            return ip;
        } finally {
            if (owner != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock account " + ownerId);
                }

                _accountDao.releaseFromLockTable(ownerId);
            }
            if (ip == null) {
                s_logger.error("Unable to get source nat ip address for account " + ownerId);
            }
        }
    }

    @Override
    public boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException {
        List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
        boolean success = true;
        // CloudStack will take a lazy approach to associate an acquired public IP to a network service provider as
        // it will not know what service an acquired IP will be used for. An IP is actually associated with a provider when first
        // rule is applied. Similarly when last rule on the acquired IP is revoked, IP is not associated with any provider
        // but still be associated with the account. At this point just mark IP as allocated or released.
        for (IPAddressVO addr : userIps) {
            if (addr.getState() == IpAddress.State.Allocating) {
                addr.setAssociatedWithNetworkId(network.getId());
                markPublicIpAsAllocated(addr);
            } else if (addr.getState() == IpAddress.State.Releasing) {
                // Cleanup all the resources for ip address if there are any, and only then un-assign ip in the system
                if (cleanupIpResources(addr.getId(), Account.ACCOUNT_ID_SYSTEM, _accountMgr.getSystemAccount())) {
                    _ipAddressDao.unassignIpAddress(addr.getId());
                } else {
                    success = false;
                    s_logger.warn("Failed to release resources for ip address id=" + addr.getId());
                }
            }
        }
        return success;
    }

    // CloudStack will take a lazy approach to associate an acquired public IP to a network service provider as
    // it will not know what a acquired IP will be used for. An IP is actually associated with a provider when first
    // rule is applied. Similarly when last rule on the acquired IP is revoked, IP is not associated with any provider
    // but still be associated with the account. Its up to caller of this function to decide when to invoke IPAssociation
    @Override
    public boolean applyIpAssociations(Network network, boolean postApplyRules, boolean continueOnError, List<? extends PublicIpAddress> publicIps)
            throws ResourceUnavailableException {
        boolean success = true;

        Map<PublicIpAddress, Set<Service>> ipToServices = _networkModel.getIpToServices(publicIps, postApplyRules, true);
        Map<Provider, ArrayList<PublicIpAddress>> providerToIpList = _networkModel.getProviderToIpList(network, ipToServices);

        for (Provider provider : providerToIpList.keySet()) {
            try {
                ArrayList<PublicIpAddress> ips = providerToIpList.get(provider);
                if (ips == null || ips.isEmpty()) {
                    continue;
                }
                IpDeployer deployer = null;
                NetworkElement element = _networkModel.getElementImplementingProvider(provider.getName());
                if (!(element instanceof IpDeployingRequester)) {
                    throw new CloudRuntimeException("Element " + element + " is not a IpDeployingRequester!");
                }
                deployer = ((IpDeployingRequester)element).getIpDeployer(network);
                if (deployer == null) {
                    throw new CloudRuntimeException("Fail to get ip deployer for element: " + element);
                }
                Set<Service> services = new HashSet<Service>();
                for (PublicIpAddress ip : ips) {
                    if (!ipToServices.containsKey(ip)) {
                        continue;
                    }
                    services.addAll(ipToServices.get(ip));
                }
                deployer.applyIps(network, ips, services);
            } catch (ResourceUnavailableException e) {
                success = false;
                if (!continueOnError) {
                    throw e;
                } else {
                    s_logger.debug("Resource is not available: " + provider.getName(), e);
                }
            }
        }

        return success;
    }

    @DB
    @Override
    public AcquirePodIpCmdResponse allocatePodIp(String zoneId, String podId) throws ConcurrentOperationException, ResourceAllocationException {

        DataCenter zone = _entityMgr.findByUuid(DataCenter.class, zoneId);
        Account caller = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            ResourceAllocationException ex = new ResourceAllocationException("Cannot perform this operation, " + "Zone is currently disabled" + "zoneId=" + zone.getUuid(),
                    ResourceType.network);
            throw ex;
        }

        DataCenterIpAddressVO vo = null;
        if (podId == null)
            throw new ResourceAllocationException("Please do not provide NULL podId", ResourceType.network);
        HostPodVO podvo = null;
        podvo = _hpDao.findByUuid(podId);
        if (podvo == null)
            throw new ResourceAllocationException("No sush pod exists", ResourceType.network);

        vo = _privateIPAddressDao.takeIpAddress(zone.getId(), podvo.getId(), 0, caller.getId() + "", false);
        if(vo == null)
            throw new ResourceAllocationException("Unable to allocate IP from this Pod", ResourceType.network);
        if (vo.getIpAddress() == null)
            throw new ResourceAllocationException("Unable to allocate IP from this Pod", ResourceType.network);

        HostPodVO pod_vo = _hpDao.findById(vo.getPodId());
        AcquirePodIpCmdResponse ret = new AcquirePodIpCmdResponse();
        ret.setCidrAddress(pod_vo.getCidrAddress());
        ret.setGateway(pod_vo.getGateway());
        ret.setInstanceId(vo.getInstanceId());
        ret.setIpAddress(vo.getIpAddress());
        ret.setMacAddress(vo.getMacAddress());
        ret.setPodId(vo.getPodId());
        ret.setId(vo.getId());

        return ret;
    }

    @DB
    @Override
    public void releasePodIp(Long id) throws CloudRuntimeException {

        // Verify input parameters
        DataCenterIpAddressVO ipVO = _privateIPAddressDao.findById(id);
        if (ipVO == null) {
            throw new CloudRuntimeException("Unable to find ip address by id:" + id);
        }

        if (ipVO.getTakenAt() == null) {
            s_logger.debug("Ip Address with id= " + id + " is not allocated, so do nothing.");
            throw new CloudRuntimeException("Ip Address  with id= " + id + " is not allocated, so do nothing.");
        }
        // Verify permission
        DataCenter zone = _entityMgr.findById(DataCenter.class, ipVO.getDataCenterId());
        Account caller = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new CloudRuntimeException("Cannot perform this operation, " + "Zone is currently disabled" + "zoneId=" + ipVO.getDataCenterId());
        }
        try {
            _privateIPAddressDao.releasePodIpAddress(id);
        } catch (Exception e) {
            new CloudRuntimeException(e.getMessage());
        }
    }

    @DB
    @Override
    public IpAddress allocateIp(final Account ipOwner, final boolean isSystem, Account caller, long callerUserId, final DataCenter zone, final Boolean displayIp, final String ipaddress)
            throws ConcurrentOperationException,
            ResourceAllocationException, InsufficientAddressCapacityException {

        final VlanType vlanType = VlanType.VirtualNetwork;
        final boolean assign = false;

        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            // zone is of type DataCenter. See DataCenterVO.java.
            PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation, " + "Zone is currently disabled");
            ex.addProxyObject(zone.getUuid(), "zoneId");
            throw ex;
        }

        PublicIp ip = null;

        Account accountToLock = null;
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address called by the user " + callerUserId + " account " + ipOwner.getId());
            }
            accountToLock = _accountDao.acquireInLockTable(ipOwner.getId());
            if (accountToLock == null) {
                s_logger.warn("Unable to lock account: " + ipOwner.getId());
                throw new ConcurrentOperationException("Unable to acquire account lock");
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address lock acquired");
            }

            ip = Transaction.execute(new TransactionCallbackWithException<PublicIp, InsufficientAddressCapacityException>() {
                @Override
                public PublicIp doInTransaction(TransactionStatus status) throws InsufficientAddressCapacityException {
                    PublicIp ip = fetchNewPublicIp(zone.getId(), null, null, ipOwner, vlanType, null, false, assign, ipaddress, isSystem, null, displayIp, false);

                    if (ip == null) {
                        InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Unable to find available public IP addresses", DataCenter.class, zone
                                .getId());
                        ex.addProxyObject(ApiDBUtils.findZoneById(zone.getId()).getUuid());
                        throw ex;

                    }
                    CallContext.current().setEventDetails("Ip Id: " + ip.getId());
                    Ip ipAddress = ip.getAddress();

                    s_logger.debug("Got " + ipAddress + " to assign for account " + ipOwner.getId() + " in zone " + zone.getId());

                    return ip;
                }
            });

        } finally {
            if (accountToLock != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock account " + ipOwner);
                }
                _accountDao.releaseFromLockTable(ipOwner.getId());
                s_logger.debug("Associate IP address lock released");
            }
        }
        return ip;
    }

    @Override
    @DB
    public IpAddress allocatePortableIp(final Account ipOwner, Account caller, final long dcId, final Long networkId, final Long vpcID)
            throws ConcurrentOperationException,
            ResourceAllocationException, InsufficientAddressCapacityException {

        GlobalLock portableIpLock = GlobalLock.getInternLock("PortablePublicIpRange");
        IPAddressVO ipaddr;

        try {
            portableIpLock.lock(5);

            ipaddr = Transaction.execute(new TransactionCallbackWithException<IPAddressVO, InsufficientAddressCapacityException>() {
                @Override
                public IPAddressVO doInTransaction(TransactionStatus status) throws InsufficientAddressCapacityException {
                    PortableIpVO allocatedPortableIp;

                    List<PortableIpVO> portableIpVOs = _portableIpDao.listByRegionIdAndState(1, PortableIp.State.Free);
                    if (portableIpVOs == null || portableIpVOs.isEmpty()) {
                        InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Unable to find available portable IP addresses", Region.class,
                                new Long(1));
                        throw ex;
                    }

                    // allocate first portable IP to the user
                    allocatedPortableIp = portableIpVOs.get(0);
                    allocatedPortableIp.setAllocatedTime(new Date());
                    allocatedPortableIp.setAllocatedToAccountId(ipOwner.getAccountId());
                    allocatedPortableIp.setAllocatedInDomainId(ipOwner.getDomainId());
                    allocatedPortableIp.setState(PortableIp.State.Allocated);
                    _portableIpDao.update(allocatedPortableIp.getId(), allocatedPortableIp);

                    // To make portable IP available as a zone level resource we need to emulate portable IP's (which are
                    // provisioned at region level) as public IP provisioned in a zone. user_ip_address and vlan combo give the
                    // identity of a public IP in zone. Create entry for portable ip in these tables.

                    // provision portable IP range VLAN into the zone
                    long physicalNetworkId = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(dcId, TrafficType.Public).getId();
                    Network network = _networkModel.getSystemNetworkByZoneAndTrafficType(dcId, TrafficType.Public);
                    String range = allocatedPortableIp.getAddress() + "-" + allocatedPortableIp.getAddress();
                    VlanVO vlan = new VlanVO(VlanType.VirtualNetwork, allocatedPortableIp.getVlan(), allocatedPortableIp.getGateway(), allocatedPortableIp.getNetmask(), dcId,
                            range, network.getId(), physicalNetworkId, null, null, null);
                    vlan = _vlanDao.persist(vlan);

                    // provision the portable IP in to user_ip_address table
                    IPAddressVO ipaddr = new IPAddressVO(new Ip(allocatedPortableIp.getAddress()), dcId, networkId, vpcID, physicalNetworkId, network.getId(), vlan.getId(), true);
                    ipaddr.setState(State.Allocated);
                    ipaddr.setAllocatedTime(new Date());
                    ipaddr.setAllocatedInDomainId(ipOwner.getDomainId());
                    ipaddr.setAllocatedToAccountId(ipOwner.getId());
                    ipaddr = _ipAddressDao.persist(ipaddr);

                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_PORTABLE_IP_ASSIGN, ipaddr.getId(), ipaddr.getDataCenterId(), ipaddr.getId(),
                            ipaddr.getAddress().toString(), ipaddr.isSourceNat(), null, ipaddr.getSystem(), ipaddr.getClass().getName(), ipaddr.getUuid());

                    return ipaddr;
                }
            });
        } finally {
            portableIpLock.unlock();
        }

        return ipaddr;
    }

    protected IPAddressVO getExistingSourceNatInNetwork(long ownerId, Long networkId) {
        List<? extends IpAddress> addrs;
        Network guestNetwork = _networksDao.findById(networkId);
        if (guestNetwork.getGuestType() == GuestType.Shared) {
            // ignore the account id for the shared network
            addrs = _networkModel.listPublicIpsAssignedToGuestNtwk(networkId, true);
        } else {
            addrs = _networkModel.listPublicIpsAssignedToGuestNtwk(ownerId, networkId, true);
        }

        IPAddressVO sourceNatIp = null;
        if (addrs.isEmpty()) {
            return null;
        } else {
            // Account already has ip addresses
            for (IpAddress addr : addrs) {
                if (addr.isSourceNat()) {
                    sourceNatIp = _ipAddressDao.findById(addr.getId());
                    return sourceNatIp;
                }
            }

            assert(sourceNatIp != null) : "How do we get a bunch of ip addresses but none of them are source nat? " + "account=" + ownerId + "; networkId=" + networkId;
        }

        return sourceNatIp;
    }

    @DB
    @Override
    public IPAddressVO associateIPToGuestNetwork(long ipId, long networkId, boolean releaseOnFailure) throws ResourceAllocationException, ResourceUnavailableException,
            InsufficientAddressCapacityException, ConcurrentOperationException {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = null;

        IPAddressVO ipToAssoc = _ipAddressDao.findById(ipId);
        if (ipToAssoc != null) {
            Network network = _networksDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Invalid network id is given");
            }

            DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
            if (zone.getNetworkType() == NetworkType.Advanced) {
                if (network.getGuestType() == Network.GuestType.Shared) {
                    if (isSharedNetworkOfferingWithServices(network.getNetworkOfferingId())) {
                        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), AccessType.UseEntry, false,
                                network);
                    } else {
                        throw new InvalidParameterValueException("IP can be associated with guest network of 'shared' type only if "
                                + "network services Source Nat, Static Nat, Port Forwarding, Load balancing, firewall are enabled in the network");
                    }
                }
            } else {
                _accountMgr.checkAccess(caller, null, true, ipToAssoc);
            }
            owner = _accountMgr.getAccount(ipToAssoc.getAllocatedToAccountId());
        } else {
            s_logger.debug("Unable to find ip address by id: " + ipId);
            return null;
        }

        if (ipToAssoc.getAssociatedWithNetworkId() != null) {
            s_logger.debug("IP " + ipToAssoc + " is already assocaited with network id" + networkId);
            return ipToAssoc;
        }

        Network network = _networksDao.findById(networkId);
        if (network != null) {
            _accountMgr.checkAccess(owner, AccessType.UseEntry, false, network);
        } else {
            s_logger.debug("Unable to find ip address by id: " + ipId);
            return null;
        }

        DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());

        // allow associating IP addresses to guest network only
        if (network.getTrafficType() != TrafficType.Guest) {
            throw new InvalidParameterValueException("Ip address can be associated to the network with trafficType " + TrafficType.Guest);
        }

        // Check that network belongs to IP owner - skip this check
        //     - if zone is basic zone as there is just one guest network,
        //     - if shared network in Advanced zone
        //     - and it belongs to the system
        if (network.getAccountId() != owner.getId()) {
            if (zone.getNetworkType() != NetworkType.Basic && !(zone.getNetworkType() == NetworkType.Advanced && network.getGuestType() == Network.GuestType.Shared)) {
                throw new InvalidParameterValueException("The owner of the network is not the same as owner of the IP");
            }
        }

        if (zone.getNetworkType() == NetworkType.Advanced) {
            // In Advance zone allow to do IP assoc only for Isolated networks with source nat service enabled
            if (network.getGuestType() == GuestType.Isolated && !(_networkModel.areServicesSupportedInNetwork(network.getId(), Service.SourceNat))) {
                if (releaseOnFailure && ipToAssoc != null) {
                    s_logger.warn("Failed to associate ip address, so unassigning ip from the database " + ipToAssoc);
                    _ipAddressDao.unassignIpAddress(ipToAssoc.getId());
                }
                throw new InvalidParameterValueException("In zone of type " + NetworkType.Advanced + " ip address can be associated only to the network of guest type "
                        + GuestType.Isolated + " with the " + Service.SourceNat.getName() + " enabled");
            }

            // In Advance zone allow to do IP assoc only for shared networks with source nat/static nat/lb/pf services enabled
            if (network.getGuestType() == GuestType.Shared && !isSharedNetworkOfferingWithServices(network.getNetworkOfferingId())) {
                if (releaseOnFailure && ipToAssoc != null) {
                    s_logger.warn("Failed to associate ip address, so unassigning ip from the database " + ipToAssoc);
                    _ipAddressDao.unassignIpAddress(ipToAssoc.getId());
                }
                throw new InvalidParameterValueException("In zone of type " + NetworkType.Advanced + " ip address can be associated with network of guest type " + GuestType.Shared
                        + "only if at " + "least one of the services " + Service.SourceNat.getName() + "/" + Service.StaticNat.getName() + "/" + Service.Lb.getName() + "/"
                        + Service.PortForwarding.getName() + " is enabled");
            }
        }

        boolean isSourceNat = isSourceNatAvailableForNetwork(owner, ipToAssoc, network);

        s_logger.debug("Associating ip " + ipToAssoc + " to network " + network);

        IPAddressVO ip = _ipAddressDao.findById(ipId);
        //update ip address with networkId
        ip.setAssociatedWithNetworkId(networkId);
        ip.setSourceNat(isSourceNat);
        _ipAddressDao.update(ipId, ip);

        boolean success = false;
        try {
            success = applyIpAssociations(network, false);
            if (success) {
                s_logger.debug("Successfully associated ip address " + ip.getAddress().addr() + " to network " + network);
            } else {
                s_logger.warn("Failed to associate ip address " + ip.getAddress().addr() + " to network " + network);
            }
            return ip;
        } finally {
            if (!success && releaseOnFailure) {
                if (ip != null) {
                    try {
                        s_logger.warn("Failed to associate ip address, so releasing ip from the database " + ip);
                        _ipAddressDao.markAsUnavailable(ip.getId());
                        if (!applyIpAssociations(network, true)) {
                            // if fail to apply ip assciations again, unassign ip address without updating resource
                            // count and generating usage event as there is no need to keep it in the db
                            _ipAddressDao.unassignIpAddress(ip.getId());
                        }
                    } catch (Exception e) {
                        s_logger.warn("Unable to disassociate ip address for recovery", e);
                    }
                }
            }
        }
    }

    /**
     * Prevents associating an IP address to an allocated (unimplemented network) network, throws an Exception otherwise
     * @param owner Used to check if the user belongs to the Network
     * @param ipToAssoc IP address to be associated to a Network, can only be associated to an implemented network for Source NAT
     * @param network Network to which IP address is to be associated with, must not be in allocated state for Source NAT Network/IP association
     * @return true if IP address can be successfully associated with Source NAT network
     */
    protected boolean isSourceNatAvailableForNetwork(Account owner, IPAddressVO ipToAssoc, Network network) {
        NetworkOffering offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        boolean sharedSourceNat = offering.isSharedSourceNat();
        boolean isSourceNat = false;
        if (!sharedSourceNat) {
            if (getExistingSourceNatInNetwork(owner.getId(), network.getId()) == null) {
                if (network.getGuestType() == GuestType.Isolated && network.getVpcId() == null && !ipToAssoc.isPortable()) {
                    isSourceNat = true;
                }
            }
        }
        return isSourceNat;
    }

    protected boolean isSharedNetworkOfferingWithServices(long networkOfferingId) {
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if ((networkOffering.getGuestType() == Network.GuestType.Shared)
                && (_networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.SourceNat)
                        || _networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.StaticNat)
                        || _networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.Firewall)
                        || _networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.PortForwarding) || _networkModel.areServicesSupportedByNetworkOffering(
                                networkOfferingId, Service.Lb))) {
            return true;
        }
        return false;
    }

    @Override
    public IPAddressVO associatePortableIPToGuestNetwork(long ipAddrId, long networkId, boolean releaseOnFailure) throws ResourceAllocationException, ResourceUnavailableException,
            InsufficientAddressCapacityException, ConcurrentOperationException {
        return associateIPToGuestNetwork(ipAddrId, networkId, releaseOnFailure);
    }

    @DB
    @Override
    public IPAddressVO disassociatePortableIPToGuestNetwork(long ipId, long networkId) throws ResourceAllocationException, ResourceUnavailableException,
            InsufficientAddressCapacityException, ConcurrentOperationException {

        Account caller = CallContext.current().getCallingAccount();
        Account owner = null;

        Network network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Invalid network id is given");
        }

        IPAddressVO ipToAssoc = _ipAddressDao.findById(ipId);
        if (ipToAssoc != null) {

            if (ipToAssoc.getAssociatedWithNetworkId() == null) {
                throw new InvalidParameterValueException("IP " + ipToAssoc + " is not associated with any network");
            }

            if (ipToAssoc.getAssociatedWithNetworkId() != network.getId()) {
                throw new InvalidParameterValueException("IP " + ipToAssoc + " is not associated with network id" + networkId);
            }

            DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
            if (zone.getNetworkType() == NetworkType.Advanced) {
                if (network.getGuestType() == Network.GuestType.Shared) {
                    assert(isSharedNetworkOfferingWithServices(network.getNetworkOfferingId()));
                    _accountMgr.checkAccess(CallContext.current().getCallingAccount(), AccessType.UseEntry, false,
                            network);
                }
            } else {
                _accountMgr.checkAccess(caller, null, true, ipToAssoc);
            }
            owner = _accountMgr.getAccount(ipToAssoc.getAllocatedToAccountId());
        } else {
            s_logger.debug("Unable to find ip address by id: " + ipId);
            return null;
        }

        DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());

        // Check that network belongs to IP owner - skip this check
        //     - if zone is basic zone as there is just one guest network,
        //     - if shared network in Advanced zone
        //     - and it belongs to the system
        if (network.getAccountId() != owner.getId()) {
            if (zone.getNetworkType() != NetworkType.Basic && !(zone.getNetworkType() == NetworkType.Advanced && network.getGuestType() == Network.GuestType.Shared)) {
                throw new InvalidParameterValueException("The owner of the network is not the same as owner of the IP");
            }
        }

        // Check if IP has any services (rules) associated in the network
        List<PublicIpAddress> ipList = new ArrayList<PublicIpAddress>();
        PublicIp publicIp = PublicIp.createFromAddrAndVlan(ipToAssoc, _vlanDao.findById(ipToAssoc.getVlanId()));
        ipList.add(publicIp);
        Map<PublicIpAddress, Set<Service>> ipToServices = _networkModel.getIpToServices(ipList, false, true);
        if (!ipToServices.isEmpty()) {
            Set<Service> services = ipToServices.get(publicIp);
            if (services != null && !services.isEmpty()) {
                throw new InvalidParameterValueException("IP " + ipToAssoc + " has services and rules associated in the network " + networkId);
            }
        }

        IPAddressVO ip = _ipAddressDao.findById(ipId);
        ip.setAssociatedWithNetworkId(null);
        _ipAddressDao.update(ipId, ip);

        try {
            boolean success = applyIpAssociations(network, false);
            if (success) {
                s_logger.debug("Successfully associated ip address " + ip.getAddress().addr() + " to network " + network);
            } else {
                s_logger.warn("Failed to associate ip address " + ip.getAddress().addr() + " to network " + network);
            }
            return ip;
        } finally {

        }
    }

    @Override
    public boolean isPortableIpTransferableFromNetwork(long ipAddrId, long networkId) {
        Network network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Invalid network id is given");
        }

        IPAddressVO ip = _ipAddressDao.findById(ipAddrId);
        if (ip == null) {
            throw new InvalidParameterValueException("Invalid network id is given");
        }

        // Check if IP has any services (rules) associated in the network
        List<PublicIpAddress> ipList = new ArrayList<PublicIpAddress>();
        PublicIp publicIp = PublicIp.createFromAddrAndVlan(ip, _vlanDao.findById(ip.getVlanId()));
        ipList.add(publicIp);
        Map<PublicIpAddress, Set<Service>> ipToServices = _networkModel.getIpToServices(ipList, false, true);
        if (!ipToServices.isEmpty()) {
            Set<Service> ipServices = ipToServices.get(publicIp);
            if (ipServices != null && !ipServices.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @DB
    @Override
    public void transferPortableIP(final long ipAddrId, long currentNetworkId, long newNetworkId) throws ResourceAllocationException, ResourceUnavailableException,
            InsufficientAddressCapacityException, ConcurrentOperationException {

        Network srcNetwork = _networksDao.findById(currentNetworkId);
        if (srcNetwork == null) {
            throw new InvalidParameterValueException("Invalid source network id " + currentNetworkId + " is given");
        }

        final Network dstNetwork = _networksDao.findById(newNetworkId);
        if (dstNetwork == null) {
            throw new InvalidParameterValueException("Invalid source network id " + newNetworkId + " is given");
        }

        final IPAddressVO ip = _ipAddressDao.findById(ipAddrId);
        if (ip == null) {
            throw new InvalidParameterValueException("Invalid portable ip address id is given");
        }

        assert(isPortableIpTransferableFromNetwork(ipAddrId, currentNetworkId));

        // disassociate portable IP with current network/VPC network
        if (srcNetwork.getVpcId() != null) {
            _vpcMgr.unassignIPFromVpcNetwork(ipAddrId, currentNetworkId);
        } else {
            disassociatePortableIPToGuestNetwork(ipAddrId, currentNetworkId);
        }

        // If portable IP need to be transferred across the zones, then mark the entry corresponding to portable ip
        // in user_ip_address and vlan tables so as to emulate portable IP as provisioned in destination data center
        if (srcNetwork.getDataCenterId() != dstNetwork.getDataCenterId()) {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    long physicalNetworkId = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(dstNetwork.getDataCenterId(), TrafficType.Public).getId();
                    long publicNetworkId = _networkModel.getSystemNetworkByZoneAndTrafficType(dstNetwork.getDataCenterId(), TrafficType.Public).getId();

                    ip.setDataCenterId(dstNetwork.getDataCenterId());
                    ip.setPhysicalNetworkId(physicalNetworkId);
                    ip.setSourceNetworkId(publicNetworkId);
                    _ipAddressDao.update(ipAddrId, ip);

                    VlanVO vlan = _vlanDao.findById(ip.getVlanId());
                    vlan.setPhysicalNetworkId(physicalNetworkId);
                    vlan.setNetworkId(publicNetworkId);
                    vlan.setDataCenterId(dstNetwork.getDataCenterId());
                    _vlanDao.update(ip.getVlanId(), vlan);
                }
            });
        }

        // associate portable IP with new network/VPC network
        associatePortableIPToGuestNetwork(ipAddrId, newNetworkId, false);

        Transaction.execute(new TransactionCallbackNoReturn() {

            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                if (dstNetwork.getVpcId() != null) {
                    ip.setVpcId(dstNetwork.getVpcId());
                } else {
                    ip.setVpcId(null);
                }

                _ipAddressDao.update(ipAddrId, ip);
            }

        });

        // trigger an action event for the transfer of portable IP across the networks, so that external entities
        // monitoring for this event can initiate the route advertisement for the availability of IP from the zoe
        ActionEventUtils.onActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, Domain.ROOT_DOMAIN, EventTypes.EVENT_PORTABLE_IP_TRANSFER,
                "Portable IP associated is transferred from network " + currentNetworkId + " to " + newNetworkId);
    }

    protected List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner) {

        return _networksDao.listSourceNATEnabledNetworks(owner.getId(), zoneId, Network.GuestType.Isolated);
    }

    @Override
    @DB
    public boolean associateIpAddressListToAccount(long userId, final long accountId, final long zoneId, final Long vlanId, final Network guestNetworkFinal)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, ResourceAllocationException {
        final Account owner = _accountMgr.getActiveAccountById(accountId);

        if (guestNetworkFinal != null && guestNetworkFinal.getTrafficType() != TrafficType.Guest) {
            throw new InvalidParameterValueException("Network " + guestNetworkFinal + " is not of a type " + TrafficType.Guest);
        }

        Ternary<Boolean, List<NetworkOfferingVO>, Network> pair = null;
        try {
            pair = Transaction.execute(new TransactionCallbackWithException<Ternary<Boolean, List<NetworkOfferingVO>, Network>, Exception>() {
                @Override
                public Ternary<Boolean, List<NetworkOfferingVO>, Network> doInTransaction(TransactionStatus status) throws InsufficientCapacityException,
                        ResourceAllocationException {
                    boolean createNetwork = false;
                    Network guestNetwork = guestNetworkFinal;

                    if (guestNetwork == null) {
                        List<? extends Network> networks = getIsolatedNetworksWithSourceNATOwnedByAccountInZone(zoneId, owner);
                        if (networks.size() == 0) {
                            createNetwork = true;
                        } else if (networks.size() == 1) {
                            guestNetwork = networks.get(0);
                        } else {
                            throw new InvalidParameterValueException("Error, more than 1 Guest Isolated Networks with SourceNAT "
                                    + "service enabled found for this account, cannot assosiate the IP range, please provide the network ID");
                        }
                    }

                    // create new Virtual network (Isolated with SourceNAT) for the user if it doesn't exist
                    List<NetworkOfferingVO> requiredOfferings = _networkOfferingDao.listByAvailability(Availability.Required, false);
                    if (requiredOfferings.size() < 1) {
                        throw new CloudRuntimeException("Unable to find network offering with availability=" + Availability.Required
                                + " to automatically create the network as part of createVlanIpRange");
                    }
                    if (createNetwork) {
                        if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
                            long physicalNetworkId = _networkModel.findPhysicalNetworkId(zoneId, requiredOfferings.get(0).getTags(), requiredOfferings.get(0).getTrafficType());
                            // Validate physical network
                            PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
                            if (physicalNetwork == null) {
                                throw new InvalidParameterValueException("Unable to find physical network with id: " + physicalNetworkId + " and tag: "
                                        + requiredOfferings.get(0).getTags());
                            }

                            s_logger.debug("Creating network for account " + owner + " from the network offering id=" + requiredOfferings.get(0).getId()
                                    + " as a part of createVlanIpRange process");
                            guestNetwork = _networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(), owner.getAccountName() + "-network", owner.getAccountName()
                                    + "-network", null, null, null, false, null, owner, null, physicalNetwork, zoneId, ACLType.Account, null, null, null, null, true, null, null);
                            if (guestNetwork == null) {
                                s_logger.warn("Failed to create default Virtual network for the account " + accountId + "in zone " + zoneId);
                                throw new CloudRuntimeException("Failed to create a Guest Isolated Networks with SourceNAT "
                                        + "service enabled as a part of createVlanIpRange, for the account " + accountId + "in zone " + zoneId);
                            }
                        } else {
                            throw new CloudRuntimeException("Required network offering id=" + requiredOfferings.get(0).getId() + " is not in " + NetworkOffering.State.Enabled);
                        }
                    }

                    // Check if there is a source nat ip address for this account; if not - we have to allocate one
                    boolean allocateSourceNat = false;
                    List<IPAddressVO> sourceNat = _ipAddressDao.listByAssociatedNetwork(guestNetwork.getId(), true);
                    if (sourceNat.isEmpty()) {
                        allocateSourceNat = true;
                    }

                    // update all ips with a network id, mark them as allocated and update resourceCount/usage
                    List<IPAddressVO> ips = _ipAddressDao.listByVlanId(vlanId);
                    boolean isSourceNatAllocated = false;
                    for (IPAddressVO addr : ips) {
                        if (addr.getState() != State.Allocated) {
                            if (!isSourceNatAllocated && allocateSourceNat) {
                                addr.setSourceNat(true);
                                isSourceNatAllocated = true;
                            } else {
                                addr.setSourceNat(false);
                            }
                            addr.setAssociatedWithNetworkId(guestNetwork.getId());
                            addr.setVpcId(guestNetwork.getVpcId());
                            addr.setAllocatedTime(new Date());
                            addr.setAllocatedInDomainId(owner.getDomainId());
                            addr.setAllocatedToAccountId(owner.getId());
                            addr.setSystem(false);
                            addr.setState(IpAddress.State.Allocating);
                            markPublicIpAsAllocated(addr);
                        }
                    }
                    return new Ternary<Boolean, List<NetworkOfferingVO>, Network>(createNetwork, requiredOfferings, guestNetwork);
                }
            });
        } catch (Exception e1) {
            ExceptionUtil.rethrowRuntime(e1);
            ExceptionUtil.rethrow(e1, InsufficientCapacityException.class);
            ExceptionUtil.rethrow(e1, ResourceAllocationException.class);
            throw new IllegalStateException(e1);
        }

        boolean createNetwork = pair.first();
        List<NetworkOfferingVO> requiredOfferings = pair.second();
        Network guestNetwork = pair.third();

        // if the network offering has persistent set to true, implement the network
        if (createNetwork && requiredOfferings.get(0).isPersistent()) {
            DataCenter zone = _dcDao.findById(zoneId);
            DeployDestination dest = new DeployDestination(zone, null, null, null);
            Account callerAccount = CallContext.current().getCallingAccount();
            UserVO callerUser = _userDao.findById(CallContext.current().getCallingUserId());
            Journal journal = new Journal.LogJournal("Implementing " + guestNetwork, s_logger);
            ReservationContext context = new ReservationContextImpl(UUID.randomUUID().toString(), journal, callerUser, callerAccount);
            s_logger.debug("Implementing network " + guestNetwork + " as a part of network provision for persistent network");
            try {
                Pair<? extends NetworkGuru, ? extends Network> implementedNetwork = _networkMgr.implementNetwork(guestNetwork.getId(), dest, context);
                if (implementedNetwork == null || implementedNetwork.first() == null) {
                    s_logger.warn("Failed to implement the network " + guestNetwork);
                }
                if (implementedNetwork != null) {
                    guestNetwork = implementedNetwork.second();
                }
            } catch (Exception ex) {
                s_logger.warn("Failed to implement network " + guestNetwork + " elements and resources as a part of" + " network provision due to ", ex);
                CloudRuntimeException e = new CloudRuntimeException("Failed to implement network (with specified id)"
                        + " elements and resources as a part of network provision for persistent network");
                e.addProxyObject(guestNetwork.getUuid(), "networkId");
                throw e;
            }
        }
        return true;
    }

    @DB
    @Override
    public IPAddressVO markIpAsUnavailable(final long addrId) {
        final IPAddressVO ip = _ipAddressDao.findById(addrId);

        if (ip.getAllocatedToAccountId() == null && ip.getAllocatedTime() == null) {
            s_logger.trace("Ip address id=" + addrId + " is already released");
            return ip;
        }

        if (ip.getState() != State.Releasing) {
            return Transaction.execute(new TransactionCallback<IPAddressVO>() {
                @Override
                public IPAddressVO doInTransaction(TransactionStatus status) {
                    if (updateIpResourceCount(ip)) {
                        _resourceLimitMgr.decrementResourceCount(_ipAddressDao.findById(addrId).getAllocatedToAccountId(), ResourceType.public_ip);
                    }

                    // Save usage event
                    if (ip.getAllocatedToAccountId() != null && ip.getAllocatedToAccountId() != Account.ACCOUNT_ID_SYSTEM) {
                        VlanVO vlan = _vlanDao.findById(ip.getVlanId());

                        String guestType = vlan.getVlanType().toString();
                        if (!isIpDedicated(ip)) {
                            String eventType = ip.isPortable() ? EventTypes.EVENT_PORTABLE_IP_RELEASE : EventTypes.EVENT_NET_IP_RELEASE;
                            UsageEventUtils.publishUsageEvent(eventType, ip.getAllocatedToAccountId(), ip.getDataCenterId(), addrId, ip.getAddress().addr(), ip.isSourceNat(),
                                    guestType, ip.getSystem(), ip.getClass().getName(), ip.getUuid());
                        }
                    }

                    return _ipAddressDao.markAsUnavailable(addrId);
                }
            });
        }

        return ip;
    }

    protected boolean updateIpResourceCount(IPAddressVO ip) {
        // don't increment resource count for direct and dedicated ip addresses
        return (ip.getAssociatedWithNetworkId() != null || ip.getVpcId() != null) && !isIpDedicated(ip);
    }

    @Override
    @DB
    public String acquireGuestIpAddress(Network network, String requestedIp) {
        if (requestedIp != null && requestedIp.equals(network.getGateway())) {
            s_logger.warn("Requested ip address " + requestedIp + " is used as a gateway address in network " + network);
            return null;
        }

        if (_networkModel.listNetworkOfferingServices(network.getNetworkOfferingId()).isEmpty() && network.getCidr() == null) {
            return null;
        }

        Set<Long> availableIps = _networkModel.getAvailableIps(network, requestedIp);

        if (availableIps == null || availableIps.isEmpty()) {
            s_logger.debug("There are no free ips in the  network " + network);
            return null;
        }

        Long[] array = availableIps.toArray(new Long[availableIps.size()]);

        if (requestedIp != null) {
            // check that requested ip has the same cidr
            String[] cidr = network.getCidr().split("/");
            boolean isSameCidr = NetUtils.sameSubnetCIDR(requestedIp, NetUtils.long2Ip(array[0]), Integer.parseInt(cidr[1]));
            if (!isSameCidr) {
                s_logger.warn("Requested ip address " + requestedIp + " doesn't belong to the network " + network + " cidr");
                return null;
            } else if (NetUtils.IsIpEqualToNetworkOrBroadCastIp(requestedIp, cidr[0], Integer.parseInt(cidr[1]))) {
                s_logger.warn("Requested ip address " + requestedIp + " is equal to the to the network/broadcast ip of the network" + network);
                return null;
            }
            return requestedIp;
        }

        return NetUtils.long2Ip(array[rand.nextInt(array.length)]);
    }

    /**
     * Get the list of public IPs that need to be applied for a static NAT enable/disable operation.
     * Manipulating only these ips prevents concurrency issues when disabling static nat at the same time.
     * @param staticNats
     * @return The list of IPs that need to be applied for the static NAT to work.
     */
    public List<IPAddressVO> getStaticNatSourceIps(List<? extends StaticNat> staticNats) {
        List<IPAddressVO> userIps = new ArrayList<>();

        for (StaticNat snat : staticNats) {
            userIps.add(_ipAddressDao.findById(snat.getSourceIpAddressId()));
        }

        return userIps;
    }

    @Override
    public boolean applyStaticNats(List<? extends StaticNat> staticNats, boolean continueOnError, boolean forRevoke) throws ResourceUnavailableException {
        if (staticNats == null || staticNats.size() == 0) {
            s_logger.debug("There are no static nat rules for the network elements");
            return true;
        }

        Network network = _networksDao.findById(staticNats.get(0).getNetworkId());
        boolean success = true;

        // Check if the StaticNat service is supported
        if (!_networkModel.areServicesSupportedInNetwork(network.getId(), Service.StaticNat)) {
            s_logger.debug("StaticNat service is not supported in specified network id");
            return true;
        }

        List<IPAddressVO> userIps = getStaticNatSourceIps(staticNats);

        List<PublicIp> publicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                publicIps.add(publicIp);
            }
        }

        // static NAT rules can not programmed unless IP is associated with source NAT service provider, so run IP
        // association for the network so as to ensure IP is associated before applying rules
        if (checkStaticNatIPAssocRequired(network, false, forRevoke, publicIps)) {
            applyIpAssociations(network, false, continueOnError, publicIps);
        }

        // get provider
        StaticNatServiceProvider element = _networkMgr.getStaticNatProviderForNetwork(network);
        try {
            success = element.applyStaticNats(network, staticNats);
        } catch (ResourceUnavailableException e) {
            if (!continueOnError) {
                throw e;
            }
            s_logger.warn("Problems with " + element.getName() + " but pushing on", e);
            success = false;
        }

        // For revoked static nat IP, set the vm_id to null, indicate it should be revoked
        for (StaticNat staticNat : staticNats) {
            if (staticNat.isForRevoke()) {
                for (PublicIp publicIp : publicIps) {
                    if (publicIp.getId() == staticNat.getSourceIpAddressId()) {
                        publicIps.remove(publicIp);
                        IPAddressVO ip = _ipAddressDao.findByIdIncludingRemoved(staticNat.getSourceIpAddressId());
                        // ip can't be null, otherwise something wrong happened
                        ip.setAssociatedWithVmId(null);
                        publicIp = PublicIp.createFromAddrAndVlan(ip, _vlanDao.findById(ip.getVlanId()));
                        publicIps.add(publicIp);
                        break;
                    }
                }
            }
        }

        // if the static NAT rules configured on public IP is revoked then, dis-associate IP with static NAT service provider
        if (checkStaticNatIPAssocRequired(network, true, forRevoke, publicIps)) {
            applyIpAssociations(network, true, continueOnError, publicIps);
        }

        return success;
    }

    // checks if there are any public IP assigned to network, that are marked for one-to-one NAT that
    // needs to be associated/dis-associated with static-nat provider
    boolean checkStaticNatIPAssocRequired(Network network, boolean postApplyRules, boolean forRevoke, List<PublicIp> publicIps) {
        for (PublicIp ip : publicIps) {
            if (ip.isOneToOneNat()) {
                Long activeFwCount = null;
                activeFwCount = _firewallDao.countRulesByIpIdAndState(ip.getId(), FirewallRule.State.Active);

                if (!postApplyRules && !forRevoke) {
                    if (activeFwCount > 0) {
                        continue;
                    } else {
                        return true;
                    }
                } else if (postApplyRules && forRevoke) {
                    return true;
                }
            } else {
                continue;
            }
        }
        return false;
    }

    @Override
    public IpAddress assignSystemIp(long networkId, Account owner, boolean forElasticLb, boolean forElasticIp) throws InsufficientAddressCapacityException {
        Network guestNetwork = _networksDao.findById(networkId);
        NetworkOffering off = _entityMgr.findById(NetworkOffering.class, guestNetwork.getNetworkOfferingId());
        IpAddress ip = null;
        if ((off.isElasticLb() && forElasticLb) || (off.isElasticIp() && forElasticIp)) {

            try {
                s_logger.debug("Allocating system IP address for load balancer rule...");
                // allocate ip
                ip = allocateIP(owner, true, guestNetwork.getDataCenterId());
                // apply ip associations
                ip = associateIPToGuestNetwork(ip.getId(), networkId, true);
                ;
            } catch (ResourceAllocationException ex) {
                throw new CloudRuntimeException("Failed to allocate system ip due to ", ex);
            } catch (ConcurrentOperationException ex) {
                throw new CloudRuntimeException("Failed to allocate system lb ip due to ", ex);
            } catch (ResourceUnavailableException ex) {
                throw new CloudRuntimeException("Failed to allocate system lb ip due to ", ex);
            }

            if (ip == null) {
                throw new CloudRuntimeException("Failed to allocate system ip");
            }
        }

        return ip;
    }

    @Override
    public boolean handleSystemIpRelease(IpAddress ip) {
        boolean success = true;
        Long networkId = ip.getAssociatedWithNetworkId();
        if (networkId != null) {
            if (ip.getSystem()) {
                CallContext ctx = CallContext.current();
                if (!disassociatePublicIpAddress(ip.getId(), ctx.getCallingUserId(), ctx.getCallingAccount())) {
                    s_logger.warn("Unable to release system ip address id=" + ip.getId());
                    success = false;
                } else {
                    s_logger.warn("Successfully released system ip address id=" + ip.getId());
                }
            }
        }
        return success;
    }

    @Override
    @DB
    public void allocateDirectIp(final NicProfile nic, final DataCenter dc, final VirtualMachineProfile vm, final Network network, final String requestedIpv4,
            final String requestedIpv6) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<InsufficientAddressCapacityException>() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws InsufficientAddressCapacityException {
                //This method allocates direct ip for the Shared network in Advance zones
                boolean ipv4 = false;

                if (network.getGateway() != null) {
                    if (nic.getIPv4Address() == null) {
                        PublicIp ip = null;

                        //Get ip address from the placeholder and don't allocate a new one
                        if (requestedIpv4 != null && vm.getType() == VirtualMachine.Type.DomainRouter) {
                            Nic placeholderNic = _networkModel.getPlaceholderNicForRouter(network, null);
                            if (placeholderNic != null) {
                                IPAddressVO userIp = _ipAddressDao.findByIpAndSourceNetworkId(network.getId(), placeholderNic.getIPv4Address());
                                ip = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                                s_logger.debug("Nic got an ip address " + placeholderNic.getIPv4Address() + " stored in placeholder nic for the network " + network);
                            }
                        }

                        if (ip == null) {
                            ip = assignPublicIpAddress(dc.getId(), null, vm.getOwner(), VlanType.DirectAttached, network.getId(), requestedIpv4, false, false);
                        }

                        nic.setIPv4Address(ip.getAddress().toString());
                        nic.setIPv4Gateway(ip.getGateway());
                        nic.setIPv4Netmask(ip.getNetmask());
                        nic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
                        nic.setBroadcastType(network.getBroadcastDomainType());
                        if (network.getBroadcastUri() != null)
                            nic.setBroadcastUri(network.getBroadcastUri());
                        else
                            nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(ip.getVlanTag()));
                        nic.setFormat(AddressFormat.Ip4);
                        nic.setReservationId(String.valueOf(ip.getVlanTag()));
                        if(nic.getMacAddress() == null) {
                            nic.setMacAddress(ip.getMacAddress());
                        }
                    }
                    nic.setIPv4Dns1(dc.getDns1());
                    nic.setIPv4Dns2(dc.getDns2());
                }

                _ipv6Mgr.setNicIp6Address(nic, dc, network);
            }
        });
    }

    @Override
    @DB
    public void allocateNicValues(final NicProfile nic, final DataCenter dc, final VirtualMachineProfile vm, final Network network, final String requestedIpv4,
            final String requestedIpv6) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<InsufficientAddressCapacityException>() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws InsufficientAddressCapacityException {
                //This method allocates direct ip for the Shared network in Advance zones
                boolean ipv4 = false;

                if (network.getGateway() != null) {
                    if (nic.getIPv4Address() == null) {
                        ipv4 = true;
                        // PublicIp ip = null;

                        //Get ip address from the placeholder and don't allocate a new one
                        if (requestedIpv4 != null && vm.getType() == VirtualMachine.Type.DomainRouter) {
                            s_logger.debug("There won't be nic assignment for VR id " + vm.getId() + "  in this network " + network);

                        }

                        // nic ip address is not set here. Because the DHCP is external to cloudstack
                        nic.setIPv4Gateway(network.getGateway());
                        nic.setIPv4Netmask(NetUtils.getCidrNetmask(network.getCidr()));

                        List<VlanVO> vlan = _vlanDao.listVlansByNetworkId(network.getId());

                        //TODO: get vlan tag for the ntwork
                        if (vlan != null && !vlan.isEmpty()) {
                            nic.setIsolationUri(IsolationType.Vlan.toUri(vlan.get(0).getVlanTag()));
                        }

                        nic.setBroadcastType(BroadcastDomainType.Vlan);
                        nic.setBroadcastType(network.getBroadcastDomainType());

                        nic.setBroadcastUri(network.getBroadcastUri());
                        nic.setFormat(AddressFormat.Ip4);
                        if(nic.getMacAddress() == null) {
                            nic.setMacAddress(_networkModel.getNextAvailableMacAddressInNetwork(network.getId()));
                        }
                    }
                    nic.setIPv4Dns1(dc.getDns1());
                    nic.setIPv4Dns2(dc.getDns2());
                }

                _ipv6Mgr.setNicIp6Address(nic, dc, network);
            }
        });
    }

    @Override
    public int getRuleCountForIp(Long addressId, FirewallRule.Purpose purpose, FirewallRule.State state) {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurposeWithState(addressId, purpose, state);
        if (rules == null) {
            return 0;
        }
        return rules.size();
    }

    @Override
    public String allocatePublicIpForGuestNic(Network network, Long podId, Account owner, String requestedIp) throws InsufficientAddressCapacityException {
        PublicIp ip = assignPublicIpAddress(network.getDataCenterId(), podId, owner, VlanType.DirectAttached, network.getId(), requestedIp, false, false);
        if (ip == null) {
            s_logger.debug("There is no free public ip address");
            return null;
        }
        Ip ipAddr = ip.getAddress();
        return ipAddr.addr();
    }

    @Override
    public String allocateGuestIP(Network network, String requestedIp) throws InsufficientAddressCapacityException {
        return acquireGuestIpAddress(network, requestedIp);
    }

    @Override
    public String getConfigComponentName() {
        return IpAddressManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {UseSystemPublicIps, RulesContinueOnError, SystemVmPublicIpReservationModeStrictness};
    }

    /**
     * Returns true if the given IP address is equals the gateway or there is no network offerrings for the given network
     */
    @Override
    public boolean isIpEqualsGatewayOrNetworkOfferingsEmpty(Network network, String requestedIp) {
        if (requestedIp.equals(network.getGateway()) || requestedIp.equals(network.getIp6Gateway())) {
            return true;
        }
        if (_networkModel.listNetworkOfferingServices(network.getNetworkOfferingId()).isEmpty() && network.getCidr() == null) {
            return true;
        }
        return false;
    }
}
