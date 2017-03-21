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
package org.apache.cloudstack.network.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.cloudstack.api.commands.ConfigureVpcInlineLoadBalancerElementCmd;
import org.apache.cloudstack.api.commands.CreateVpcInlineLoadBalancerElementCmd;
import org.apache.cloudstack.api.commands.ListVpcInlineLoadBalancerElementsCmd;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.network.lb.VpcInlineLoadBalancerVmManager;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;

public class VpcInlineLoadBalancerElement extends AdapterBase implements LoadBalancingServiceProvider, VpcInlineLoadBalancerElementService, IpDeployer {
    private static final Logger s_logger = Logger.getLogger(VpcInlineLoadBalancerElement.class);
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    NetworkModel _ntwkModel;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNtwkSvcProviderDao;
    @Inject
    VpcInlineLoadBalancerVmManager _vpcInlineLbMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    LoadBalancerDao _loadBalancerDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOrchestrationService _ntwkMgr;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    EntityManager _entityMgr;
    @Autowired
    @Qualifier("networkHelper")
    protected NetworkHelper _networkHelper;

    private static class RulesToApply {
        List<LoadBalancingRule> rules;
        Set<Ip> activeIps = new HashSet<Ip>();
        Set<Ip> inactiveIps = new HashSet<Ip>();
        boolean rulesInRevoke = false;

        private RulesToApply(List<LoadBalancingRule> rules) {
            for (LoadBalancingRule rule : rules) {
                Ip ip = rule.getSourceIp();
                if (rule.getState() == FirewallRule.State.Active || rule.getState().equals(FirewallRule.State.Add)) {
                    activeIps.add(ip);
                    inactiveIps.remove(ip);
                } else if (!activeIps.contains(ip)) {
                    if (rule.getState() == FirewallRule.State.Revoke) {
                        rulesInRevoke = true;
                    }
                    inactiveIps.add(ip);
                }
            }

            this.rules = rules;
        }

        public static RulesToApply getLbRulesToApply(List<LoadBalancingRule> rules) {
            return new RulesToApply(rules);
        }

        public boolean hasActiveRules() {
            return !activeIps.isEmpty();
        }

        public boolean hasRulesInRevoke() {
            return rulesInRevoke;
        }
    }

    private boolean canHandle(Network config, Scheme lbScheme) {
        //works in Advance zone only
        DataCenter dc = _entityMgr.findById(DataCenter.class, config.getDataCenterId());
        if (dc.getNetworkType() != NetworkType.Advanced) {
            s_logger.trace("Not handling zone of network type " + dc.getNetworkType());
            return false;
        }
        if (config.getGuestType() != Network.GuestType.Isolated || config.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("Not handling network with Type  " + config.getGuestType() + " and traffic type " + config.getTrafficType());
            return false;
        }

        Map<Capability, String> lbCaps = getCapabilities().get(Service.Lb);
        if (!lbCaps.isEmpty()) {
            String schemeCaps = lbCaps.get(Capability.LbSchemes);
            if (schemeCaps != null && lbScheme != null) {
                if (!schemeCaps.contains(lbScheme.toString())) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Scheme " + lbScheme.toString() + " is not supported by the provider " + getName());
                    }
                    return false;
                }
            }
        }

        if (!_ntwkModel.isProviderSupportServiceInNetwork(config.getId(), Service.Lb, getProvider())) {
            s_logger.trace("Element " + getProvider().getName() + " doesn't support service " + Service.Lb
                    + " in the network " + config);
            return false;
        }
        return true;
    }

    @Override
    public boolean handlesOnlyRulesInTransitionState() {
        return false;
    }


    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.VpcInlineLbVm;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {

        if (!canHandle(network, null)) {
            s_logger.trace("No need to implement " + getName());
            return true;
        }

        return implementVpcInlineLbVms(network, dest);
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {

        if (!canHandle(network, null)) {
            s_logger.trace("No need to prepare " + getName());
            return true;
        }

        if (vm.getType() == VirtualMachine.Type.User) {
            return implementVpcInlineLbVms(network, dest);
        }
        return true;
    }

    private boolean implementVpcInlineLbVms(Network network, DeployDestination dest) throws ResourceUnavailableException {
        //1) Get all the Ips from the network having LB rules assigned
        final List<IPAddressVO> ips = _ipAddressDao.listByAssociatedNetwork(network.getId(), false);
        final List<IPAddressVO> lbIps = new LinkedList<IPAddressVO>();

        //2) Start vpcInline lb vms for the ips having active rules
        for (IPAddressVO ip : ips) {
            int active = _loadBalancerDao.listByIpAddress(ip.getId()).size();
            if (active > 0) {
                lbIps.add(ip);
            }
        }

        if (CollectionUtils.isNotEmpty(lbIps)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Have to implement vpcInline lb vm as a part of network " + network
                        + " implement as there are vpcInline lb rules for this network");
            }

            try {
                List<DomainRouterVO> vpcInlineLbVms = _vpcInlineLbMgr.deployVpcInlineLbVm(network, dest, _accountMgr.getAccount(network.getAccountId()), null, lbIps);

                if (CollectionUtils.isEmpty(vpcInlineLbVms)) {
                    throw new ResourceUnavailableException("Can't deploy " + getName() + " to handle LB rules",
                            DataCenter.class, network.getDataCenterId());
                }

            } catch (InsufficientCapacityException e) {
                s_logger.warn("Failed to deploy element " + getName() + " for due to:", e);
                return false;
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Failed to deploy element " + getName() + " for due to:", e);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        List<? extends VirtualRouter> vpcInlineLbVms = _routerDao.listByNetworkAndRole(network.getId(), Role.LB);
        if (CollectionUtils.isEmpty(vpcInlineLbVms)) {
            return true;
        }

        boolean result = true;
        for (VirtualRouter vpcInlineLbVm : vpcInlineLbVms) {
            result = result && _vpcInlineLbMgr.destroyVpcInlineLbVm(vpcInlineLbVm.getId(), context.getAccount(), context.getCaller().getId());
            if (cleanup) {
                if (!result) {
                    s_logger.warn("Failed to stop vpcInline lb element " + vpcInlineLbVm + ", but trying clean up anyway.");
                }
                result = (_vpcInlineLbMgr.destroyVpcInlineLbVm(vpcInlineLbVm.getId(), context.getAccount(), context.getCaller().getId()));
                if (!result) {
                    s_logger.warn("Failed to clean up vpcInline lb element " + vpcInlineLbVm);
                }
            }
        }
        return result;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        List<? extends VirtualRouter> vpcInlineLbVms = _routerDao.listByNetworkAndRole(network.getId(), Role.LB);
        boolean result = true;
        if (vpcInlineLbVms != null) {
            for (VirtualRouter vpcInlineLbVm : vpcInlineLbVms) {
                result = result && (_vpcInlineLbMgr.destroyVpcInlineLbVm(vpcInlineLbVm.getId(), context.getAccount(), context.getCaller().getId()));
            }
        }

        if (!network.equals(network)) {
            return result && _ntwkMgr.destroyNetwork(network.getId(), context, false);
        }
        return result;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        try {
            VirtualRouterProvider element = _vrProviderDao.findByNspIdAndType(provider.getId(), Type.VpcInlineLbVm);
            if (element == null) {
                element = configureVpcInlineLoadBalancerElementByNspId(provider.getId(), true);
            }
            return element.isEnabled();
        } catch (InvalidParameterValueException e) {
            return false;
        }
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(provider.getId(), Type.VpcInlineLbVm);
        if (element == null) {
            return true;
        }

        long elementId = element.getId();
        List<DomainRouterVO> vpcInlineLbVms = _routerDao.listByElementId(elementId);
        boolean result = true;
        for (DomainRouterVO vpcInlineLbVm : vpcInlineLbVms) {
            result = result && (_vpcInlineLbMgr.destroyVpcInlineLbVm(vpcInlineLbVm.getId(), context.getAccount(), context.getCaller().getId()));
        }
        _vrProviderDao.remove(elementId);

        return result;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    private void destroyVpcInlineLbVmsIfNoActiveRules(Network guestNetwork, RulesToApply rulesToApply) throws ResourceUnavailableException {
        if (!hasActiveLbRules(guestNetwork, rulesToApply)) {
            final Account owner = _accountMgr.getAccount(guestNetwork.getAccountId());

            //2.1 Destroy vpcInline lb vm
            List<? extends VirtualRouter> vms = _vpcInlineLbMgr.findVpcInlineLbVms(guestNetwork, null);
            for (VirtualRouter vm : vms) {
                for (Ip inactiveIp : rulesToApply.inactiveIps) {
                    _vpcInlineLbMgr.removeSecondaryIpMapping(guestNetwork, owner, inactiveIp, vm);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Destroying vpcInline lb vm in network " + guestNetwork.getId() + " as all the rules for this vm are in Revoke state");
                }
                _vpcInlineLbMgr.destroyVpcInlineLbVm(vm.getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM),
                        _accountMgr.getUserIncludingRemoved(User.UID_SYSTEM).getId());
            }
        }
    }

    @Override
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        Network networkLock = _networkDao.acquireInLockTable(network.getId(), NetworkOrchestrationService.NetworkLockTimeout.value());
        if (networkLock == null) {
            throw new ConcurrentOperationException("Unable to lock network " + network.getId() + " for applying LB rules");
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Lock is acquired for network " + network.getId() + " as part of applying LB rules");
        }

        try {
            List<LoadBalancingRule> dbRules = Lists.newArrayListWithExpectedSize(rules.size());

            if (allRulesSameState(rules, FirewallRule.State.Active)
                    || network.getState() == Network.State.Allocated /* CLOUD-926: */) {
                return true;
            } else if (allRulesSameState(rules, FirewallRule.State.Revoke)
                    || network.getState() == Network.State.Destroy) {
                dbRules.addAll(rules);
            } else if (!rules.isEmpty()) {
                dbRules = refreshLoadBalancingRules(rules);
            }

            RulesToApply rulesToApply = RulesToApply.getLbRulesToApply(dbRules);
            try {
                destroyVpcInlineLbVmsIfNoActiveRules(network, rulesToApply);

                if (!rulesToApply.hasActiveRules()) {
                    return !rulesToApply.hasRulesInRevoke(); // || _vpcInlineLbMgr.applyLoadBalancingRulesToAclList(network, dbRules);
                }

            } catch (ConcurrentOperationException e) {
                s_logger.warn("Failed to apply lb rule(s) for network " + network.getId() + " on the element " + getName() + " due to:", e);
                return false;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Applying " + rulesToApply.activeIps.size() + " on element " + getName());
            }

            final DeployDestination dest = new DeployDestination(_entityMgr.findById(DataCenter.class, network.getDataCenterId()), null, null, null);
            Map<Ip, String> publicIpGuestIpMapping = Maps.newHashMap();
            final Account owner = _accountMgr.getAccount(network.getAccountId());

            DomainRouterVO vpcInlineLbVm = null;
            DomainRouterVO lock = null;
            try {
                List<DomainRouterVO> vpcInlineLbVms = _vpcInlineLbMgr.deployVpcInlineLbVm(network, dest, owner, null);
                if (CollectionUtils.isEmpty(vpcInlineLbVms)) {
                    throw new ResourceUnavailableException("Can't find/deploy vpcInline lb vm to handle LB rules",
                            DataCenter.class, network.getDataCenterId());
                }

                vpcInlineLbVm = Iterables.getFirst(vpcInlineLbVms, null);
                lock = _routerDao.acquireInLockTable(vpcInlineLbVm.getId());
                if (lock == null) {
                    throw new ConcurrentOperationException("Unable to lock domain router " + vpcInlineLbVm.getId());
                }

                for (Ip sourceIp : rulesToApply.activeIps) {
                    String secondaryIp = _vpcInlineLbMgr.createSecondaryIpMapping(network, dest, owner, sourceIp, vpcInlineLbVm);
                    publicIpGuestIpMapping.put(sourceIp, secondaryIp);
                }

                for (Ip sourceIp : rulesToApply.inactiveIps) {
                    _vpcInlineLbMgr.removeSecondaryIpMapping(network, owner, sourceIp, vpcInlineLbVm);
                }

                //2.3 Apply Internal LB rules on the VM
                if (!_vpcInlineLbMgr.applyLoadBalancingRules(network, dbRules, vpcInlineLbVm, publicIpGuestIpMapping)) {
                    throw new CloudRuntimeException("Failed to apply load balancing rules" +
                            " in network " + network.getId() + " on element " + getName());
                }
            } catch (InsufficientCapacityException e) {
                s_logger.warn("Failed to apply lb rule(s) on the element " + getName() + " due to:", e);

                for (Ip sourceIp : publicIpGuestIpMapping.keySet()) {
                    _vpcInlineLbMgr.removeSecondaryIpMapping(network, owner, sourceIp, vpcInlineLbVm);
                }
                return false;
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Failed to apply lb rule(s) on the element " + getName() + " due to:", e);
                return false;
            } finally {
                if (lock != null) {
                    _routerDao.releaseFromLockTable(lock.getId());
                }
            }

            // Cleanup unused VM's
            try {
                _vpcInlineLbMgr.cleanupUnusedVpcInlineLbVms(network.getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM),
                        _accountMgr.getUserIncludingRemoved(User.UID_SYSTEM).getId());
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Failed to cleanup unused VPC inline LB VMs due to:", e);
            }

            return true;

        } finally {
            _networkDao.releaseFromLockTable(networkLock.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Lock is released for network " + networkLock.getId() + " as part of applying LB rules");
            }
        }
    }

    private List<LoadBalancingRule> refreshLoadBalancingRules(List<LoadBalancingRule> rules) {
        LoadBalancingRule lbRule = Iterables.getFirst(rules, null);
        if (lbRule == null) {
            return Lists.newLinkedList();
        }

        List<LoadBalancingRule> refreshedLbRules = Lists.newArrayListWithExpectedSize(rules.size());
        List<LoadBalancerVO> lbs = _loadBalancerDao.listByNetworkIdAndScheme(lbRule.getNetworkId(), lbRule.getScheme());
        for (LoadBalancerVO lb : lbs) {
            IPAddressVO ip = _ipAddressDao.findById(lb.getSourceIpAddressId());
            try {
                List<LoadBalancingRule.LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                List<LoadBalancingRule.LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                List<LoadBalancingRule.LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
                LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, ip.getAddress());
                refreshedLbRules.add(loadBalancing);
            } catch (NoSuchElementException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("LB rule wasn't found in the DB anymore");
                }
            }
        }
        return refreshedLbRules;
    }

    private boolean allRulesSameState(List<LoadBalancingRule> rules, FirewallRule.State expectedState) {
        for (LoadBalancingRule rule : rules) {
            if (rule.getState() != expectedState) {
                return false;
            }
        }
        return true;
    }

    private boolean hasActiveLbRules(Network network, RulesToApply rules) {
        Set<Ip> lbPublicIps = new HashSet<Ip>();
        lbPublicIps.addAll(rules.activeIps);
        lbPublicIps.addAll(rules.inactiveIps);

        long ruleCount = 0;
        for (Ip sourceIp : lbPublicIps) {
            //2) Check if there are non revoked rules for the source ip address
            long ipId = _ntwkModel.getPublicIpAddress(sourceIp.addr(), network.getDataCenterId()).getId();
            ruleCount += _loadBalancerDao.countActiveByIpAddress(ipId);
        }

        return ruleCount > 0;
    }

    @Override
    public boolean validateLBRule(Network network, LoadBalancingRule rule) {
        if (canHandle(network, rule.getScheme())) {
            List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.LB);
            if (CollectionUtils.isEmpty(routers)) {
                return true;
            }
            return _networkHelper.validateHAProxyLBRule(rule);
        }
        return true;
    }

    @Override
    public List<LoadBalancerTO> updateHealthChecks(Network network, List<LoadBalancingRule> lbrules) {
        return null;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // Set capabilities for LB service
        Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin,leastconn,source");
        lbCapabilities.put(Capability.SupportedLBIsolation, "dedicated");
        lbCapabilities.put(Capability.SupportedProtocols, "tcp, udp");
        lbCapabilities.put(Capability.SupportedStickinessMethods, VirtualRouterElement.getHAProxyStickinessCapability());
        lbCapabilities.put(Capability.HealthCheckPolicy, "true");
        lbCapabilities.put(Capability.LbSchemes, Scheme.Public.toString());

        capabilities.put(Service.Lb, lbCapabilities);
        return capabilities;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateVpcInlineLoadBalancerElementCmd.class);
        cmdList.add(ConfigureVpcInlineLoadBalancerElementCmd.class);
        cmdList.add(ListVpcInlineLoadBalancerElementsCmd.class);
        return cmdList;
    }

    @Override
    public VirtualRouterProvider configureVpcInlineLoadBalancerElement(long id, boolean enable) {
        VirtualRouterProviderVO element = _vrProviderDao.findById(id);
        if (element == null || element.getType() != Type.VpcInlineLbVm) {
            throw new InvalidParameterValueException("Can't find " + getName() + " element with network service provider id " + id +
                    " to be used as a provider for " + getName());
        }

        element.setEnabled(enable);
        element = _vrProviderDao.persist(element);

        return element;
    }

    @Override
    public VirtualRouterProvider configureVpcInlineLoadBalancerElementByNspId(long ntwkSvcProviderId, boolean enable) {
        VirtualRouterProviderVO element = _vrProviderDao.findByNspId(ntwkSvcProviderId);
        if (element == null) {
            element = (VirtualRouterProviderVO) addVpcInlineLoadBalancerElement(ntwkSvcProviderId);
        } else if (element.getType() != Type.VpcInlineLbVm) {
            throw new InvalidParameterValueException("Can't find " + getName() + " element with network service provider id " + ntwkSvcProviderId +
                    " to be used as a provider for " + getName());
        }

        element.setEnabled(enable);
        element = _vrProviderDao.persist(element);

        return element;
    }

    @Override
    public VirtualRouterProvider addVpcInlineLoadBalancerElement(long ntwkSvcProviderId) {
        VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(ntwkSvcProviderId, Type.VpcInlineLbVm);
        if (element != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("There is already an " + getName() + " with service provider id " + ntwkSvcProviderId);
            }
            return null;
        }

        PhysicalNetworkServiceProvider provider = _pNtwkSvcProviderDao.findById(ntwkSvcProviderId);
        if (provider == null || !provider.getProviderName().equalsIgnoreCase(getName())) {
            throw new InvalidParameterValueException("Invalid network service provider is specified");
        }

        element = new VirtualRouterProviderVO(ntwkSvcProviderId, Type.VpcInlineLbVm);
        element = _vrProviderDao.persist(element);
        return element;
    }

    @Override
    public VirtualRouterProvider getVpcInlineLoadBalancerElement(long id) {
        VirtualRouterProvider provider = _vrProviderDao.findById(id);
        if (provider == null || provider.getType() != Type.VpcInlineLbVm) {
            throw new InvalidParameterValueException("Unable to find " + getName() + " by id");
        }
        return provider;
    }

    @Override
    public List<? extends VirtualRouterProvider> searchForVpcInlineLoadBalancerElements(Long id, Long ntwkSvsProviderId, Boolean enabled) {
        QueryBuilder<VirtualRouterProviderVO> sc = QueryBuilder.create(VirtualRouterProviderVO.class);
        if (id != null) {
            sc.and(sc.entity().getId(), Op.EQ, id);
        }
        if (ntwkSvsProviderId != null) {
            sc.and(sc.entity().getNspId(), Op.EQ, ntwkSvsProviderId);
        }
        if (enabled != null) {
            sc.and(sc.entity().isEnabled(), Op.EQ, enabled);
        }

        //return only VPC Inline LB elements
        sc.and(sc.entity().getType(), Op.EQ, Type.VpcInlineLbVm);
        return sc.list();
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> services) throws ResourceUnavailableException {
        //do nothing here; this element just has to extend the ip deployer
        //as the LB service implements IPDeployerRequester
        return true;
    }
}
