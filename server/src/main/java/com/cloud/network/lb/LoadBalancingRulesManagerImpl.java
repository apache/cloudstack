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
package com.cloud.network.lb;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBHealthCheckPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBStickinessPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLBHealthCheckPoliciesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLBStickinessPoliciesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRuleInstancesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRulesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.response.ServiceResponse;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.ExternalDeviceUsageManager;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.LBHealthCheckPolicyVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScalePolicyConditionMapVO;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmGroupPolicyMapVO;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.network.as.Condition;
import com.cloud.network.as.Counter;
import com.cloud.network.as.dao.AutoScalePolicyConditionMapDao;
import com.cloud.network.as.dao.AutoScalePolicyDao;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.as.dao.AutoScaleVmProfileDao;
import com.cloud.network.as.dao.ConditionDao;
import com.cloud.network.as.dao.CounterDao;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LBHealthCheckPolicyDao;
import com.cloud.network.dao.LBStickinessPolicyDao;
import com.cloud.network.dao.LBStickinessPolicyVO;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.SslCertVO;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScalePolicy;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScaleVmGroup;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScaleVmProfile;
import com.cloud.network.lb.LoadBalancingRule.LbCondition;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LbStickinessMethod.LbStickinessMethodParam;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.network.vpc.VpcManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class LoadBalancingRulesManagerImpl<Type> extends ManagerBase implements LoadBalancingRulesManager, LoadBalancingRulesService {
    private static final Logger s_logger = Logger.getLogger(LoadBalancingRulesManagerImpl.class);

    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    EventDao _eventDao;
    @Inject
    LoadBalancerVMMapDao _lb2VmMapDao;
    @Inject
    LBStickinessPolicyDao _lb2stickinesspoliciesDao;
    @Inject
    LBHealthCheckPolicyDao _lb2healthcheckDao;
    @Inject
    UserVmDao _vmDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    NicDao _nicDao;
    @Inject
    UsageEventDao _usageEventDao;
    @Inject
    FirewallRulesCidrsDao _firewallCidrsDao;
    @Inject
    FirewallManager _firewallMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOfferingServiceMapDao _networkOfferingServiceDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    DomainService _domainMgr;
    @Inject
    ConfigurationManager _configMgr;

    @Inject
    ExternalDeviceUsageManager _externalDeviceUsageMgr;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    ServiceOfferingDao _offeringsDao;
    @Inject
    CounterDao _counterDao;
    @Inject
    ConditionDao _conditionDao;
    @Inject
    AutoScaleVmProfileDao _autoScaleVmProfileDao;
    @Inject
    AutoScalePolicyDao _autoScalePolicyDao;
    @Inject
    AutoScalePolicyConditionMapDao _autoScalePolicyConditionMapDao;
    @Inject
    AutoScaleVmGroupDao _autoScaleVmGroupDao;
    @Inject
    AutoScaleVmGroupPolicyMapDao _autoScaleVmGroupPolicyMapDao;
    @Inject
    AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;
    @Inject
    AutoScaleManager autoScaleManager;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    UserDao _userDao;
    List<LoadBalancingServiceProvider> _lbProviders;
    @Inject
    ApplicationLoadBalancerRuleDao _appLbRuleDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    EntityManager _entityMgr;
    @Inject
    LoadBalancerCertMapDao _lbCertMapDao;

    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;

    private static final int DNS_PORT = 53;
    // Will return a string. For LB Stickiness this will be a json, for
    // autoscale this will be "," separated values
    @Override
    public String getLBCapability(long networkid, String capabilityName) {
        Map<Service, Map<Capability, String>> serviceCapabilitiesMap = _networkModel.getNetworkCapabilities(networkid);
        if (serviceCapabilitiesMap != null) {
            for (Service service : serviceCapabilitiesMap.keySet()) {
                ServiceResponse serviceResponse = new ServiceResponse();
                serviceResponse.setName(service.getName());
                if ("Lb".equalsIgnoreCase(service.getName())) {
                    Map<Capability, String> serviceCapabilities = serviceCapabilitiesMap.get(service);
                    if (serviceCapabilities != null) {
                        for (Capability capability : serviceCapabilities.keySet()) {
                            if (capabilityName.equals(capability.getName())) {
                                return serviceCapabilities.get(capability);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private LbAutoScaleVmGroup getLbAutoScaleVmGroup(AutoScaleVmGroupVO vmGroup, AutoScaleVmGroup.State currentState, LoadBalancerVO lb) {
        long lbNetworkId = lb.getNetworkId();
        String lbName = lb.getName();
        List<AutoScaleVmGroupPolicyMapVO> vmGroupPolicyMapList = _autoScaleVmGroupPolicyMapDao.listByVmGroupId(vmGroup.getId());
        List<LbAutoScalePolicy> autoScalePolicies = new ArrayList<LbAutoScalePolicy>();
        for (AutoScaleVmGroupPolicyMapVO vmGroupPolicyMap : vmGroupPolicyMapList) {
            AutoScalePolicy autoScalePolicy = _autoScalePolicyDao.findById(vmGroupPolicyMap.getPolicyId());
            List<AutoScalePolicyConditionMapVO> autoScalePolicyConditionMapList = _autoScalePolicyConditionMapDao.listByAll(autoScalePolicy.getId(), null);
            List<LbCondition> lbConditions = new ArrayList<LbCondition>();
            for (AutoScalePolicyConditionMapVO autoScalePolicyConditionMap : autoScalePolicyConditionMapList) {
                Condition condition = _conditionDao.findById(autoScalePolicyConditionMap.getConditionId());
                Counter counter = _counterDao.findById(condition.getCounterId());
                lbConditions.add(new LbCondition(counter, condition));
            }
            autoScalePolicies.add(new LbAutoScalePolicy(autoScalePolicy, lbConditions));
        }
        AutoScaleVmProfile autoScaleVmProfile = _autoScaleVmProfileDao.findById(vmGroup.getProfileId());
        String zoneId = _dcDao.findById(autoScaleVmProfile.getZoneId()).getUuid();
        String domainId = _domainDao.findById(autoScaleVmProfile.getDomainId()).getUuid();
        String serviceOfferingId = _offeringsDao.findById(autoScaleVmProfile.getServiceOfferingId()).getUuid();
        String templateId = _templateDao.findById(autoScaleVmProfile.getTemplateId()).getUuid();
        String vmName = "AutoScale-LB-" + lbName;
        String lbNetworkUuid = null;

        DataCenter zone = _entityMgr.findById(DataCenter.class, vmGroup.getZoneId());
        if (zone == null) {
            // This should never happen, but still a cautious check
            s_logger.warn("Unable to find zone while packaging AutoScale Vm Group, zoneid: " + vmGroup.getZoneId());
            throw new InvalidParameterValueException("Unable to find zone");
        } else {
            if (zone.getNetworkType() == NetworkType.Advanced) {
                NetworkVO lbNetwork = _networkDao.findById(lbNetworkId);
                lbNetworkUuid = lbNetwork.getUuid();
            }
        }

        String apiKey = null;
        String secretKey = null;
        String csUrl = ApiServiceConfiguration.ApiServletPath.value();
        Network.Provider provider = getLoadBalancerServiceProvider(lb);
        if (Network.Provider.Netscaler.equals(provider)) {
            Long autoscaleUserId = autoScaleVmProfile.getAutoScaleUserId();
            if (autoscaleUserId == null) {
                throw new InvalidParameterValueException("autoscaleUserId is required but not specified");
            }
            User user = _userDao.findById(autoscaleUserId);
            if (user == null) {
                throw new InvalidParameterValueException("Unable to find user by id " + autoscaleUserId);
            }
            apiKey = user.getApiKey();
            secretKey = user.getSecretKey();
            if (apiKey == null) {
                throw new InvalidParameterValueException("apiKey for user: " + user.getUsername() + " is empty. Please generate it");
            }

            if (secretKey == null) {
                throw new InvalidParameterValueException("secretKey for user: " + user.getUsername() + " is empty. Please generate it");
            }

            if (csUrl == null || csUrl.contains("localhost")) {
                throw new InvalidParameterValueException(String.format("Global setting %s has to be set to the Management Server's API end point", ApiServiceConfiguration.ApiServletPath.key()));
            }
        }

        LbAutoScaleVmProfile lbAutoScaleVmProfile =
            new LbAutoScaleVmProfile(autoScaleVmProfile, apiKey, secretKey, csUrl, zoneId, domainId, serviceOfferingId, templateId, vmName, lbNetworkUuid);
        return new LbAutoScaleVmGroup(vmGroup, autoScalePolicies, lbAutoScaleVmProfile, currentState);
    }

    @Override
    public LoadBalancerTO.AutoScaleVmGroupTO toAutoScaleVmGroupTO(LbAutoScaleVmGroup lbAutoScaleVmGroup) {
        List<LbAutoScalePolicy> lbAutoScalePolicies = lbAutoScaleVmGroup.getPolicies();
        List<LoadBalancerTO.AutoScalePolicyTO> autoScalePolicyTOs = new ArrayList<>(lbAutoScalePolicies.size());
        for (LbAutoScalePolicy lbAutoScalePolicy : lbAutoScalePolicies) {
            List<LbCondition> lbConditions = lbAutoScalePolicy.getConditions();
            List<LoadBalancerTO.ConditionTO> conditionTOs = new ArrayList<>(lbConditions.size());
            for (LbCondition lbCondition : lbConditions) {
                Counter counter = lbCondition.getCounter();
                LoadBalancerTO.CounterTO counterTO = new LoadBalancerTO.CounterTO(counter.getId(), counter.getName(), counter.getSource(), "" + counter.getValue(), counter.getProvider());
                Condition condition = lbCondition.getCondition();
                LoadBalancerTO.ConditionTO conditionTO = new LoadBalancerTO.ConditionTO(condition.getId(), condition.getThreshold(), condition.getRelationalOperator(), counterTO);
                conditionTOs.add(conditionTO);
            }
            AutoScalePolicy autoScalePolicy = lbAutoScalePolicy.getPolicy();
            autoScalePolicyTOs.add(new LoadBalancerTO.AutoScalePolicyTO(autoScalePolicy.getId(), autoScalePolicy.getDuration(), autoScalePolicy.getQuietTime(), autoScalePolicy.getLastQuietTime(),
                    autoScalePolicy.getAction(), conditionTOs, lbAutoScalePolicy.isRevoked()));
        }
        LbAutoScaleVmProfile lbAutoScaleVmProfile = lbAutoScaleVmGroup.getProfile();
        AutoScaleVmProfile autoScaleVmProfile = lbAutoScaleVmProfile.getProfile();

        LoadBalancerTO.AutoScaleVmProfileTO autoScaleVmProfileTO =
                new LoadBalancerTO.AutoScaleVmProfileTO(lbAutoScaleVmProfile.getZoneId(), lbAutoScaleVmProfile.getDomainId(), lbAutoScaleVmProfile.getCsUrl(),
                        lbAutoScaleVmProfile.getAutoScaleUserApiKey(), lbAutoScaleVmProfile.getAutoScaleUserSecretKey(), lbAutoScaleVmProfile.getServiceOfferingId(),
                        lbAutoScaleVmProfile.getTemplateId(), lbAutoScaleVmProfile.getVmName(), lbAutoScaleVmProfile.getNetworkId(), autoScaleVmProfile.getOtherDeployParams(),
                        autoScaleVmProfile.getCounterParams(), autoScaleVmProfile.getExpungeVmGracePeriod());

        AutoScaleVmGroup autoScaleVmGroup = lbAutoScaleVmGroup.getVmGroup();
        return
                new LoadBalancerTO.AutoScaleVmGroupTO(autoScaleVmGroup.getId(), autoScaleVmGroup.getUuid(), autoScaleVmGroup.getMinMembers(), autoScaleVmGroup.getMaxMembers(), autoScaleVmGroup.getMemberPort(),
                        autoScaleVmGroup.getInterval(), autoScalePolicyTOs, autoScaleVmProfileTO, autoScaleVmGroup.getState(), lbAutoScaleVmGroup.getCurrentState(), autoScaleVmGroup.getLoadBalancerId());
    }

    @Override
    public LoadBalancerTO.AutoScaleVmGroupTO toAutoScaleVmGroupTO(AutoScaleVmGroupVO vmGroup) {
        final LoadBalancerVO loadBalancer = _lbDao.findById(vmGroup.getLoadBalancerId());
        if (loadBalancer == null) {
            throw new CloudRuntimeException("Unable to find load balancer with id: " + vmGroup.getLoadBalancerId());
        }
        LbAutoScaleVmGroup lbAutoScaleVmGroup = getLbAutoScaleVmGroup(vmGroup, vmGroup.getState(), loadBalancer);
        return toAutoScaleVmGroupTO(lbAutoScaleVmGroup);
    }

    @Override
    public Network.Provider getLoadBalancerServiceProvider(LoadBalancerVO loadBalancer) {
        Network network = _networkDao.findById(loadBalancer.getNetworkId());
        List<Network.Provider> providers = _networkMgr.getProvidersForServiceInNetwork(network, Network.Service.Lb);
        if (CollectionUtils.isEmpty(providers)) {
            throw new CloudRuntimeException(String.format("Unable to find LB provider for network with id: %s ", network.getId()));
        }
        return providers.get(0);
    }

    private boolean applyAutoScaleConfig(LoadBalancerVO lb, AutoScaleVmGroupVO vmGroup, AutoScaleVmGroup.State currentState) throws ResourceUnavailableException {
        LbAutoScaleVmGroup lbAutoScaleVmGroup = getLbAutoScaleVmGroup(vmGroup, currentState, lb);
        /*
         * Regular config like destinations need not be packed for applying
         * autoscale config as of today.
         */
        List<LbStickinessPolicy> policyList = getStickinessPolicies(lb.getId());
        Ip sourceIp = getSourceIp(lb);
        LoadBalancingRule rule = new LoadBalancingRule(lb, null, policyList, null, sourceIp, null, lb.getLbProtocol());
        rule.setAutoScaleVmGroup(lbAutoScaleVmGroup);

        if (!isRollBackAllowedForProvider(lb)) {
            // this is for Netscaler type of devices. if their is failure the db
            // entries will be rollbacked.
            return false;
        }

        List<LoadBalancingRule> rules = Arrays.asList(rule);

        if (!applyLbRules(new ArrayList<>(rules), false)) {
            s_logger.debug("LB rules' autoscale config are not completely applied");
            return false;
        }

        return true;
    }

    private Ip getSourceIp(LoadBalancer lb) {
        Ip sourceIp = null;
        if (lb.getScheme() == Scheme.Public) {
            sourceIp = _networkModel.getPublicIpAddress(lb.getSourceIpAddressId()).getAddress();
        } else if (lb.getScheme() == Scheme.Internal) {
            ApplicationLoadBalancerRuleVO appLbRule = _appLbRuleDao.findById(lb.getId());
            sourceIp = appLbRule.getSourceIp();
        }
        return sourceIp;
    }

    @Override
    @DB
    public boolean configureLbAutoScaleVmGroup(final long vmGroupid, AutoScaleVmGroup.State currentState) throws ResourceUnavailableException {
        final AutoScaleVmGroupVO vmGroup = _autoScaleVmGroupDao.findById(vmGroupid);
        boolean success = false;

        final LoadBalancerVO loadBalancer = _lbDao.findById(vmGroup.getLoadBalancerId());

        FirewallRule.State backupState = loadBalancer.getState();

        if (vmGroup.getState().equals(AutoScaleVmGroup.State.NEW)
                || (loadBalancer.getState() == FirewallRule.State.Active && vmGroup.getState().equals(AutoScaleVmGroup.State.REVOKE))) {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
        }

        try {
            success = applyAutoScaleConfig(loadBalancer, vmGroup, currentState);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to configure AutoScaleVmGroup to the lb rule: " + loadBalancer.getId() + " because resource is unavailable:", e);
            if (isRollBackAllowedForProvider(loadBalancer)) {
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + " lb state rolback while creating AutoscaleVmGroup");
            }
            throw e;
        } finally {
            if (!success) {
                s_logger.warn("Failed to configure LB Auto Scale Vm Group with Id:" + vmGroupid);
            }
        }

        if (success) {
            if (vmGroup.getState().equals(AutoScaleVmGroup.State.NEW)) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        loadBalancer.setState(FirewallRule.State.Active);
                        s_logger.debug("LB rule " + loadBalancer.getId() + " state is set to Active");
                        _lbDao.persist(loadBalancer);
                        vmGroup.setState(AutoScaleVmGroup.State.ENABLED);
                        _autoScaleVmGroupDao.persist(vmGroup);
                        s_logger.debug("LB Auto Scale Vm Group with Id: " + vmGroupid + " is set to Enabled state.");
                    }
                });
            }
            s_logger.info("Successfully configured LB Autoscale Vm Group with Id: " + vmGroupid);
        }
        return success;
    }

    private boolean validateHealthCheck(CreateLBHealthCheckPolicyCmd cmd) {
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        String capability = getLBCapability(loadBalancer.getNetworkId(), Capability.HealthCheckPolicy.getName());
        if (capability != null) {
            return true;
        }
        return false;
    }

    private boolean genericValidator(CreateLBStickinessPolicyCmd cmd) throws InvalidParameterValueException {
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        /* Validation : check for valid Method name and params */
        List<LbStickinessMethod> stickinessMethodList = getStickinessMethods(loadBalancer.getNetworkId());
        boolean methodMatch = false;

        if (stickinessMethodList == null) {
            throw new InvalidParameterValueException("Failed:  No Stickiness method available for LB rule:" + cmd.getLbRuleId());
        }
        for (LbStickinessMethod method : stickinessMethodList) {
            if (method.getMethodName().equalsIgnoreCase(cmd.getStickinessMethodName())) {
                methodMatch = true;
                Map apiParamList = cmd.getparamList();
                List<LbStickinessMethodParam> methodParamList = method.getParamList();
                Map<String, String> tempParamList = new HashMap<String, String>();

                /*
                 * validation-1: check for any extra params that are not
                 * required by the policymethod(capability), FIXME: make the
                 * below loop simple without using raw data type
                 */
                if (apiParamList != null) {
                    Collection userGroupCollection = apiParamList.values();
                    Iterator iter = userGroupCollection.iterator();
                    while (iter.hasNext()) {
                        HashMap<String, String> paramKVpair = (HashMap)iter.next();
                        String paramName = paramKVpair.get("name");
                        String paramValue = paramKVpair.get("value");

                        tempParamList.put(paramName, paramValue);
                        Boolean found = false;
                        for (LbStickinessMethodParam param : methodParamList) {
                            if (param.getParamName().equalsIgnoreCase(paramName)) {
                                if ((param.getIsflag() == false) && (paramValue == null)) {
                                    throw new InvalidParameterValueException("Failed : Value expected for the Param :" + param.getParamName());
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            throw new InvalidParameterValueException("Failed : Stickiness policy does not support param name :" + paramName);
                        }
                    }
                }

                /* validation-2: check for mandatory params */
                for (LbStickinessMethodParam param : methodParamList) {
                    if (param.getRequired()) {
                        if (tempParamList.get(param.getParamName()) == null) {
                            throw new InvalidParameterValueException("Failed : Missing Manadatory Param :" + param.getParamName());
                        }
                    }
                }
                /* Successfully completed the Validation */
                break;
            }
        }
        if (methodMatch == false) {
            throw new InvalidParameterValueException("Failed to match Stickiness method name for LB rule:" + cmd.getLbRuleId());
        }

        /* Validation : check for the multiple policies to the rule id */
        List<LBStickinessPolicyVO> stickinessPolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(cmd.getLbRuleId(), false);
        if (stickinessPolicies.size() > 1) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Already two policies attached " + cmd.getLbRuleId());
        }
        return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_STICKINESSPOLICY_CREATE, eventDescription = "create lb stickinesspolicy to load balancer", create = true)
    public StickinessPolicy createLBStickinessPolicy(CreateLBStickinessPolicyCmd cmd) throws NetworkRuleConflictException {
        CallContext caller = CallContext.current();

        /* Validation : check corresponding load balancer rule exist */
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed: LB rule id: " + cmd.getLbRuleId() + " not present ");
        }

        _accountMgr.checkAccess(caller.getCallingAccount(), null, true, loadBalancer);
        if (loadBalancer.getState() == FirewallRule.State.Revoke) {
            throw new InvalidParameterValueException("Failed:  LB rule id: " + cmd.getLbRuleId() + " is in deleting state: ");
        }

        /* Generic validations */
        if (!genericValidator(cmd)) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Validation Failed " + cmd.getLbRuleId());
        }

        /*
         * Specific validations using network element validator for specific
         * validations
         */
        LBStickinessPolicyVO lbpolicy =
            new LBStickinessPolicyVO(loadBalancer.getId(), cmd.getLBStickinessPolicyName(), cmd.getStickinessMethodName(), cmd.getparamList(), cmd.getDescription());
        List<LbStickinessPolicy> policyList = new ArrayList<LbStickinessPolicy>();
        policyList.add(new LbStickinessPolicy(cmd.getStickinessMethodName(), lbpolicy.getParams()));
        Ip sourceIp = getSourceIp(loadBalancer);
        LoadBalancingRule lbRule =
            new LoadBalancingRule(loadBalancer, getExistingDestinations(lbpolicy.getId()), policyList, null, sourceIp, null, loadBalancer.getLbProtocol());
        if (!validateLbRule(lbRule)) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Validation Failed " + cmd.getLbRuleId());
        }

        /* Finally Insert into DB */
        LBStickinessPolicyVO policy =
            new LBStickinessPolicyVO(loadBalancer.getId(), cmd.getLBStickinessPolicyName(), cmd.getStickinessMethodName(), cmd.getparamList(), cmd.getDescription());
        Boolean forDisplay = cmd.getDisplay();
        if (forDisplay != null) {
            policy.setDisplay(forDisplay);
        }
        policy = _lb2stickinesspoliciesDao.persist(policy);

        return policy;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_HEALTHCHECKPOLICY_CREATE, eventDescription = "create load balancer health check to load balancer", create = true)
    public HealthCheckPolicy createLBHealthCheckPolicy(CreateLBHealthCheckPolicyCmd cmd) {
        CallContext caller = CallContext.current();

        /*
         * Validation of cmd Monitor interval must be greater than response
         * timeout
         */
        Map<String, String> paramMap = cmd.getFullUrlParams();

        if (paramMap.containsKey(ApiConstants.HEALTHCHECK_RESPONSE_TIMEOUT) && paramMap.containsKey(ApiConstants.HEALTHCHECK_INTERVAL_TIME)) {
            if (cmd.getResponsTimeOut() > cmd.getHealthCheckInterval())
                throw new InvalidParameterValueException("Failed to create HealthCheck policy : Monitor interval must be greater than response timeout");
        }
        /* Validation : check corresponding load balancer rule exist */
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed: LB rule id: " + cmd.getLbRuleId() + " not present ");
        }

        _accountMgr.checkAccess(caller.getCallingAccount(), null, true, loadBalancer);

        if (loadBalancer.getState() == FirewallRule.State.Revoke) {
            throw new InvalidParameterValueException("Failed:  LB rule id: " + cmd.getLbRuleId() + " is in deleting state: ");
        }

        /*
         * Validate Whether LB Provider has the capabilities to support Health
         * Checks
         */
        if (!validateHealthCheck(cmd)) {
            throw new InvalidParameterValueException(
                "Failed to create HealthCheck policy: Validation Failed (HealthCheck Policy is not supported by LB Provider for the LB rule id :" + cmd.getLbRuleId() + ")");
        }

        /* Validation : check for the multiple hc policies to the rule id */
        List<LBHealthCheckPolicyVO> hcPolicies = _lb2healthcheckDao.listByLoadBalancerId(cmd.getLbRuleId(), false);
        if (hcPolicies.size() > 0) {
            throw new InvalidParameterValueException("Failed to create HealthCheck policy: Already policy attached  for the LB Rule id :" + cmd.getLbRuleId());
        }
        /*
         * Specific validations using network element validator for specific
         * validations
         */
        LBHealthCheckPolicyVO hcpolicy =
            new LBHealthCheckPolicyVO(loadBalancer.getId(), cmd.getPingPath(), cmd.getDescription(), cmd.getResponsTimeOut(), cmd.getHealthCheckInterval(),
                cmd.getHealthyThreshold(), cmd.getUnhealthyThreshold());

        List<LbHealthCheckPolicy> hcPolicyList = new ArrayList<LbHealthCheckPolicy>();
        hcPolicyList.add(new LbHealthCheckPolicy(hcpolicy.getpingpath(), hcpolicy.getDescription(), hcpolicy.getResponseTime(), hcpolicy.getHealthcheckInterval(),
            hcpolicy.getHealthcheckThresshold(), hcpolicy.getUnhealthThresshold()));

        // Finally Insert into DB
        LBHealthCheckPolicyVO policy =
            new LBHealthCheckPolicyVO(loadBalancer.getId(), cmd.getPingPath(), cmd.getDescription(), cmd.getResponsTimeOut(), cmd.getHealthCheckInterval(),
                cmd.getHealthyThreshold(), cmd.getUnhealthyThreshold());

        Boolean forDisplay = cmd.getDisplay();
        if (forDisplay != null) {
            policy.setDisplay(forDisplay);
        }

        policy = _lb2healthcheckDao.persist(policy);
        return policy;
    }

    @Override
    public boolean validateLbRule(LoadBalancingRule lbRule) {
        Network network = _networkDao.findById(lbRule.getNetworkId());
        Purpose purpose = lbRule.getPurpose();
        if (purpose != Purpose.LoadBalancing) {
            s_logger.debug("Unable to validate network rules for purpose: " + purpose.toString());
            return false;
        }
        for (LoadBalancingServiceProvider ne : _lbProviders) {
            boolean validated = ne.validateLBRule(network, lbRule);
            if (!validated)
                return false;
        }
        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_STICKINESSPOLICY_CREATE, eventDescription = "Apply Stickinesspolicy to load balancer ", async = true)
    public boolean applyLBStickinessPolicy(CreateLBStickinessPolicyCmd cmd) {
        boolean success = true;
        FirewallRule.State backupState = null;
        long oldStickinessPolicyId = 0;

        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer Id:" + cmd.getLbRuleId());
        }

        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, loadBalancer);

        List<LBStickinessPolicyVO> stickinessPolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(cmd.getLbRuleId(), false);
        for (LBStickinessPolicyVO stickinessPolicy : stickinessPolicies) {
            if (stickinessPolicy.getId() == cmd.getEntityId()) {
                backupState = loadBalancer.getState();
                loadBalancer.setState(FirewallRule.State.Add);
                _lbDao.persist(loadBalancer);
            } else {
                oldStickinessPolicyId = stickinessPolicy.getId();
                stickinessPolicy.setRevoke(true);
                _lb2stickinesspoliciesDao.persist(stickinessPolicy);
            }
        }
        try {
            applyLoadBalancerConfig(cmd.getLbRuleId());
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply Stickiness policy to the lb rule: " + cmd.getLbRuleId() + " because resource is unavailable:", e);
            if (isRollBackAllowedForProvider(loadBalancer)) {
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                deleteLBStickinessPolicy(cmd.getEntityId(), false);
                s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + " lb state rolback while creating sticky policy");
            } else {
                deleteLBStickinessPolicy(cmd.getEntityId(), false);
                if (oldStickinessPolicyId != 0) {
                    LBStickinessPolicyVO stickinessPolicy = _lb2stickinesspoliciesDao.findById(oldStickinessPolicyId);
                    stickinessPolicy.setRevoke(false);
                    _lb2stickinesspoliciesDao.persist(stickinessPolicy);
                    try {
                        if (backupState.equals(FirewallRule.State.Active))
                            applyLoadBalancerConfig(cmd.getLbRuleId());
                    } catch (ResourceUnavailableException e1) {
                        s_logger.info("[ignored] applying load balancer config.", e1);
                    } finally {
                        loadBalancer.setState(backupState);
                        _lbDao.persist(loadBalancer);
                    }
                }
            }
            success = false;
        }

        return success;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_HEALTHCHECKPOLICY_CREATE, eventDescription = "Apply HealthCheckPolicy to load balancer ", async = true)
    public boolean applyLBHealthCheckPolicy(CreateLBHealthCheckPolicyCmd cmd) {
        boolean success = true;

        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer Id:" + cmd.getLbRuleId());
        }
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, loadBalancer);
        FirewallRule.State backupState = loadBalancer.getState();
        loadBalancer.setState(FirewallRule.State.Add);
        _lbDao.persist(loadBalancer);
        try {
            applyLoadBalancerConfig(cmd.getLbRuleId());
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply healthcheck policy to the lb rule: " + cmd.getLbRuleId() + " because resource is unavailable:", e);
            if (isRollBackAllowedForProvider(loadBalancer)) {
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + " lb state rolback while creating healthcheck policy");
            }
            deleteLBHealthCheckPolicy(cmd.getEntityId(), false);
            success = false;
        }
        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_STICKINESSPOLICY_DELETE, eventDescription = "revoking LB Stickiness policy ", async = true)
    public boolean deleteLBStickinessPolicy(long stickinessPolicyId, boolean apply) {
        boolean success = true;

        CallContext caller = CallContext.current();
        LBStickinessPolicyVO stickinessPolicy = _lb2stickinesspoliciesDao.findById(stickinessPolicyId);

        if (stickinessPolicy == null) {
            throw new InvalidParameterException("Invalid Stickiness policy id value: " + stickinessPolicyId);
        }
        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(stickinessPolicy.getLoadBalancerId()));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer : " + stickinessPolicy.getLoadBalancerId() + " for Stickiness policy id: " + stickinessPolicyId);
        }
        long loadBalancerId = loadBalancer.getId();
        FirewallRule.State backupState = loadBalancer.getState();
        _accountMgr.checkAccess(caller.getCallingAccount(), null, true, loadBalancer);

        if (apply) {
            if (loadBalancer.getState() == FirewallRule.State.Active) {
                loadBalancer.setState(FirewallRule.State.Add);
                _lbDao.persist(loadBalancer);
            }

            boolean backupStickyState = stickinessPolicy.isRevoke();
            stickinessPolicy.setRevoke(true);
            _lb2stickinesspoliciesDao.persist(stickinessPolicy);
            s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", stickinesspolicyID " + stickinessPolicyId);

            try {
                if (!applyLoadBalancerConfig(loadBalancerId)) {
                    s_logger.warn("Failed to remove load balancer rule id " + loadBalancerId + " for stickinesspolicyID " + stickinessPolicyId);
                    throw new CloudRuntimeException("Failed to remove load balancer rule id " + loadBalancerId + " for stickinesspolicyID " + stickinessPolicyId);
                }
            } catch (ResourceUnavailableException e) {
                if (isRollBackAllowedForProvider(loadBalancer)) {
                    stickinessPolicy.setRevoke(backupStickyState);
                    _lb2stickinesspoliciesDao.persist(stickinessPolicy);
                    loadBalancer.setState(backupState);
                    _lbDao.persist(loadBalancer);
                    s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + "  while deleting sticky policy: " + stickinessPolicyId);
                }
                s_logger.warn("Unable to apply the load balancer config because resource is unavailable.", e);
                success = false;
            }
        } else {
            _lb2stickinesspoliciesDao.expunge(stickinessPolicyId);
        }
        return success;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_HEALTHCHECKPOLICY_DELETE, eventDescription = "revoking LB HealthCheck policy ", async = true)
    public boolean deleteLBHealthCheckPolicy(long healthCheckPolicyId, boolean apply) {
        boolean success = true;

        CallContext caller = CallContext.current();
        LBHealthCheckPolicyVO healthCheckPolicy = _lb2healthcheckDao.findById(healthCheckPolicyId);

        if (healthCheckPolicy == null) {
            throw new InvalidParameterException("Invalid HealthCheck policy id value: " + healthCheckPolicyId);
        }
        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(healthCheckPolicy.getLoadBalancerId()));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer : " + healthCheckPolicy.getLoadBalancerId() + " for HealthCheck policy id: " + healthCheckPolicyId);
        }
        final long loadBalancerId = loadBalancer.getId();
        FirewallRule.State backupState = loadBalancer.getState();
        _accountMgr.checkAccess(caller.getCallingAccount(), null, true, loadBalancer);

        if (apply) {
            if (loadBalancer.getState() == FirewallRule.State.Active) {
                loadBalancer.setState(FirewallRule.State.Add);
                _lbDao.persist(loadBalancer);
            }

            boolean backupStickyState = healthCheckPolicy.isRevoke();
            healthCheckPolicy.setRevoke(true);
            _lb2healthcheckDao.persist(healthCheckPolicy);
            s_logger.debug("Set health check policy to revoke for loadbalancing rule id : " + loadBalancerId + ", healthCheckpolicyID " + healthCheckPolicyId);

            // removing the state of services set by the monitor.
            final List<LoadBalancerVMMapVO> maps = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId);
            if (maps != null) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        s_logger.debug("Resetting health state policy for services in loadbalancing rule id : " + loadBalancerId);
                        for (LoadBalancerVMMapVO map : maps) {
                            map.setState(null);
                            _lb2VmMapDao.persist(map);
                        }
                    }
                });
            }

            try {
                if (!applyLoadBalancerConfig(loadBalancerId)) {
                    s_logger.warn("Failed to remove load balancer rule id " + loadBalancerId + " for healthCheckpolicyID " + healthCheckPolicyId);
                    throw new CloudRuntimeException("Failed to remove load balancer rule id " + loadBalancerId + " for healthCheckpolicyID " + healthCheckPolicyId);
                }
            } catch (ResourceUnavailableException e) {
                if (isRollBackAllowedForProvider(loadBalancer)) {
                    healthCheckPolicy.setRevoke(backupStickyState);
                    _lb2healthcheckDao.persist(healthCheckPolicy);
                    loadBalancer.setState(backupState);
                    _lbDao.persist(loadBalancer);
                    s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + "  while deleting healthcheck policy: " + healthCheckPolicyId);
                }
                s_logger.warn("Unable to apply the load balancer config because resource is unavailable.", e);
                success = false;
            }
        } else {
            _lb2healthcheckDao.remove(healthCheckPolicy.getLoadBalancerId());
        }
        return success;
    }

    // This method will check the status of services which has monitors created
    // by CloudStack and update them in lbvmmap table
    @DB
    @Override
    public void updateLBHealthChecks(Scheme scheme) throws ResourceUnavailableException {
        List<LoadBalancerVO> rules = _lbDao.listAll();
        List<NetworkVO> networks = _networkDao.listAll();
        List<LoadBalancerTO> stateRules = null;
        boolean isHandled = false;
        for (NetworkVO ntwk : networks) {
            Network network = _networkDao.findById(ntwk.getId());
            String capability = getLBCapability(network.getId(), Capability.HealthCheckPolicy.getName());

            if (capability != null && capability.equalsIgnoreCase("true")) {
                /*
                 * s_logger.debug(
                 * "HealthCheck Manager :: LB Provider in the Network has the Healthcheck policy capability :: "
                 * + provider.get(0).getName());
                 */
                rules = _lbDao.listByNetworkIdAndScheme(network.getId(), scheme);
                if (rules != null && rules.size() > 0) {
                    List<LoadBalancingRule> lbrules = new ArrayList<LoadBalancingRule>();
                    for (LoadBalancerVO lb : rules) {
                        List<LbDestination> dstList = getExistingDestinations(lb.getId());
                        List<LbHealthCheckPolicy> hcPolicyList = getHealthCheckPolicies(lb.getId());
                        // Now retrive the status of services from NS even there are no policies. because there is default monitor
                        Ip sourceIp = getSourceIp(lb);
                        LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, null, hcPolicyList, sourceIp, null, lb.getLbProtocol());
                        lbrules.add(loadBalancing);
                    }
                    if (lbrules.size() > 0) {
                        isHandled = false;
                        for (LoadBalancingServiceProvider lbElement : _lbProviders) {
                            stateRules = lbElement.updateHealthChecks(network, lbrules);
                            if (stateRules != null && stateRules.size() > 0) {
                                for (LoadBalancerTO lbto : stateRules) {
                                    LoadBalancerVO ulb = _lbDao.findByUuid(lbto.getUuid());
                                    List<LoadBalancerVMMapVO> lbVmMaps = _lb2VmMapDao.listByLoadBalancerId(ulb.getId());
                                    for (LoadBalancerVMMapVO lbVmMap : lbVmMaps) {
                                        UserVm vm = _vmDao.findById(lbVmMap.getInstanceId());
                                        Nic nic = _nicDao.findByInstanceIdAndNetworkIdIncludingRemoved(ulb.getNetworkId(), vm.getId());
                                        String dstIp = lbVmMap.getInstanceIp() == null ? nic.getIPv4Address(): lbVmMap.getInstanceIp();

                                        for (int i = 0; i < lbto.getDestinations().length; i++) {
                                            LoadBalancerTO.DestinationTO des = lbto.getDestinations()[i];
                                            if (dstIp.equalsIgnoreCase(lbto.getDestinations()[i].getDestIp())) {
                                                lbVmMap.setState(des.getMonitorState());
                                                _lb2VmMapDao.persist(lbVmMap);
                                                s_logger.debug("Updating the LB VM Map table with the service state");
                                            }
                                        }
                                    }
                                }
                                isHandled = true;
                            }
                            if (isHandled) {
                                break;
                            }
                        }
                    }
                }
            } else {
                // s_logger.debug("HealthCheck Manager :: LB Provider in the Network DNOT the Healthcheck policy capability ");
            }
        }
    }

    private boolean isRollBackAllowedForProvider(LoadBalancerVO loadBalancer) {
        Network network = _networkDao.findById(loadBalancer.getNetworkId());
        List<Provider> provider = _networkMgr.getProvidersForServiceInNetwork(network, Service.Lb);
        if (provider == null || provider.size() == 0) {
            return false;
        }
        if (provider.get(0) == Provider.Netscaler || provider.get(0) == Provider.F5BigIp ||
            provider.get(0) == Provider.VirtualRouter || provider.get(0) == Provider.VPCVirtualRouter) {
            return true;
        }
        return false;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ASSIGN_TO_LOAD_BALANCER_RULE, eventDescription = "assigning to load balancer", async = true)
    public boolean assignToLoadBalancer(long loadBalancerId, List<Long> instanceIds, Map<Long, List<String>> vmIdIpMap, boolean isAutoScaleVM) {
        CallContext ctx = CallContext.current();
        Account caller = ctx.getCallingAccount();

        final LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + ", the load balancer was not found.");
        }

        if (!isAutoScaleVM && _autoScaleVmGroupDao.isAutoScaleLoadBalancer(loadBalancerId)) {
            throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + " because it is being used by an Autoscale VM group.");
        }

        if (instanceIds == null && vmIdIpMap.isEmpty()) {
            throw new InvalidParameterValueException("Both instanceids and vmidipmap  can't be null");
        }

        // instanceIds and vmIdipmap is passed
        if (instanceIds != null && !vmIdIpMap.isEmpty()) {
            for(long instanceId: instanceIds) {
                if (!vmIdIpMap.containsKey(instanceId)) {
                    vmIdIpMap.put(instanceId, null);
                }
            }
        }

        //only instanceids list passed
        if (instanceIds != null && vmIdIpMap.isEmpty()){
            vmIdIpMap = new HashMap<Long, List<String>>();
            for (long instanceId: instanceIds){
                vmIdIpMap.put(instanceId, null);
            }
        }

        List<LoadBalancerVMMapVO> mappedInstances = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId, false);
        Set<Long> mappedInstanceIds = new HashSet<Long>();
        for (LoadBalancerVMMapVO mappedInstance : mappedInstances) {
            mappedInstanceIds.add(Long.valueOf(mappedInstance.getInstanceId()));
        }

        Map<Long, List<String>> existingVmIdIps = new HashMap<Long, List<String>>();
        // now get the ips of vm and add it to map
        for (LoadBalancerVMMapVO mappedInstance : mappedInstances) {

            List<String> ipsList = null;
            if (existingVmIdIps.containsKey(mappedInstance.getInstanceId())) {
                ipsList = existingVmIdIps.get(mappedInstance.getInstanceId());
            } else {
                ipsList = new ArrayList<String>();
            }
            ipsList.add(mappedInstance.getInstanceIp());
            existingVmIdIps.put(mappedInstance.getInstanceId(), ipsList);
        }

        final List<UserVm> vmsToAdd = new ArrayList<UserVm>();

        // check for conflict
        Set<Long> passedInstanceIds = vmIdIpMap.keySet();
        for (Long instanceId : passedInstanceIds) {
            UserVm vm = _vmDao.findById(instanceId);
            if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Invalid instance id specified");
                if (vm == null) {
                    ex.addProxyObject(instanceId.toString(), "instanceId");
                } else {
                    ex.addProxyObject(vm.getUuid(), "instanceId");
                }
                throw ex;
            }

            _rulesMgr.checkRuleAndUserVm(loadBalancer, vm, caller);

            Account vmOwner = _accountDao.findById(vm.getAccountId());
            Network network = _networkDao.findById(loadBalancer.getNetworkId());
            _accountMgr.checkAccess(vmOwner, SecurityChecker.AccessType.UseEntry, false, network);

            // Let's check to make sure the vm has a nic in the same network as
            // the load balancing rule.
            List<? extends Nic> nics = _networkModel.getNics(vm.getId());
            Nic nicInSameNetwork = null;
            for (Nic nic : nics) {
                if (nic.getNetworkId() == loadBalancer.getNetworkId()) {
                    nicInSameNetwork = nic;
                    break;
                }
            }

            if (nicInSameNetwork == null) {
                InvalidParameterValueException ex =
                        new InvalidParameterValueException("VM with id specified cannot be added because it doesn't belong in the same network.");
                ex.addProxyObject(vm.getUuid(), "instanceId");
                throw ex;
            }

            String priIp = nicInSameNetwork.getIPv4Address();

            if (existingVmIdIps.containsKey(instanceId)) {
                // now check for ip address
                List<String> mappedIps = existingVmIdIps.get(instanceId);
                List<String> newIps = vmIdIpMap.get(instanceId);

                if (newIps == null) {
                    newIps = new ArrayList<String>();
                    newIps.add(priIp);
                }

                for (String newIp: newIps) {
                    if (mappedIps.contains(newIp)) {
                        throw new InvalidParameterValueException("VM " + instanceId + " with " + newIp +" is already mapped to load balancer.");
                    }
                }
            }

            List<String> vmIpsList = vmIdIpMap.get(instanceId);
            String vmLbIp = null;

            if (vmIpsList != null) {

                //check if the ips belongs to nic secondary ip
                for (String ip: vmIpsList) {
                    // skip the primary ip from vm secondary ip comparisions
                    if (ip.equals(priIp)) {
                        continue;
                    }
                    if(_nicSecondaryIpDao.findByIp4AddressAndNicId(ip,nicInSameNetwork.getId()) == null) {
                        throw new InvalidParameterValueException("VM ip "+ ip + " specified does not belong to " +
                                "nic in network " + nicInSameNetwork.getNetworkId());
                    }
                }
            } else {
                vmIpsList = new ArrayList<String>();
                vmIpsList.add(priIp);
            }

            // when vm id is passed in instance ids and in vmidipmap
            // assign for primary ip and ip passed in vmidipmap
            if (instanceIds != null ) {
                if (instanceIds.contains(instanceId)) {
                    vmIpsList.add(priIp);
                }
            }

            vmIdIpMap.put(instanceId, vmIpsList);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Adding " + vm + " to the load balancer pool");
            }
            vmsToAdd.add(vm);
        }

        final Set<Long> vmIds = vmIdIpMap.keySet();
        final Map<Long, List<String>> newMap = vmIdIpMap;

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {

                for (Long vmId : vmIds) {
                    final Set<String> lbVmIps = new HashSet<String>(newMap.get(vmId));
                    for (String vmIp: lbVmIps) {
                        LoadBalancerVMMapVO map = new LoadBalancerVMMapVO(loadBalancer.getId(), vmId, vmIp, false);
                        map = _lb2VmMapDao.persist(map);
                    }
                }
            }
        });

        if (_autoScaleVmGroupDao.isAutoScaleLoadBalancer(loadBalancerId)) {
            Network.Provider provider = getLoadBalancerServiceProvider(loadBalancer);
            if (Network.Provider.Netscaler.equals(provider)) {
                // For netscaler autoscale loadbalancer, the rules need not be applied,
                // meaning the call need not reach the resource layer.
                // We can consider the job done.
                return true;
            }
        }
        boolean success = false;
        FirewallRule.State backupState = loadBalancer.getState();
        try {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
            applyLoadBalancerConfig(loadBalancerId);
            success = true;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply the load balancer config because resource is unavailable.", e);
            success = false;
        } finally {
            if (!success) {
                final List<Long> vmInstanceIds = new ArrayList<Long>();
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        for (Long vmId : vmIds) {
                            vmInstanceIds.add(vmId);
                        }
                    }
                });
                if (!vmInstanceIds.isEmpty()) {
                    _lb2VmMapDao.remove(loadBalancer.getId(), vmInstanceIds, null);
                    s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + "  while attaching VM: " + vmInstanceIds);
                }
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                CloudRuntimeException ex = new CloudRuntimeException("Failed to add specified loadbalancerruleid for vms "
                    + vmInstanceIds);
                ex.addProxyObject(loadBalancer.getUuid(), "loadBalancerId");
                // TBD: Also pack in the instanceIds in the exception using the
                // right VO object or table name.
                throw ex;
            }

        }

        return success;
    }

    @Override
    public boolean assignSSLCertToLoadBalancerRule(Long lbId, String certName, String publicCert, String privateKey) {
        s_logger.error("Calling the manager for LB");
        LoadBalancerVO loadBalancer = _lbDao.findById(lbId);

        return false;  //TODO
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_REMOVE_FROM_LOAD_BALANCER_RULE, eventDescription = "removing from load balancer", async = true)
    public boolean removeFromLoadBalancer(long loadBalancerId, List<Long> instanceIds, Map<Long, List<String>> vmIdIpsMap, boolean isAutoScaleVM) {
        return removeFromLoadBalancerInternal(loadBalancerId, instanceIds, true, vmIdIpsMap, isAutoScaleVM);
    }

    @Override
    public LbSslCert getLbSslCert(long lbRuleId) {
        LoadBalancerCertMapVO lbCertMap = _lbCertMapDao.findByLbRuleId(lbRuleId);

        if (lbCertMap == null)
            return null;

        SslCertVO certVO = _entityMgr.findById(SslCertVO.class, lbCertMap.getCertId());
        if (certVO == null) {
            s_logger.warn("Cert rule with cert ID " + lbCertMap.getCertId() + " but Cert is not found");
            return null;
        }

        return new LbSslCert(certVO.getCertificate(), certVO.getKey(), certVO.getPassword(), certVO.getChain(), certVO.getFingerPrint(), lbCertMap.isRevoke());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_CERT_ASSIGN, eventDescription = "assigning certificate to load balancer", async = true)
    public boolean assignCertToLoadBalancer(long lbRuleId, Long certId, boolean forced) {
        CallContext caller = CallContext.current();

        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(lbRuleId));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid load balancer id: " + lbRuleId);
        }

        SslCertVO certVO = _entityMgr.findById(SslCertVO.class, certId);
        if (certVO == null) {
            throw new InvalidParameterException("Invalid certificate id: " + certId);
        }

        _accountMgr.checkAccess(caller.getCallingAccount(), null, true, loadBalancer);

        // check if LB and Cert belong to the same account
        if (loadBalancer.getAccountId() != certVO.getAccountId()) {
            throw new InvalidParameterValueException("Access denied for account " + certVO.getAccountId());
        }

        String capability = getLBCapability(loadBalancer.getNetworkId(), Capability.SslTermination.getName());
        if (capability == null) {
            throw new InvalidParameterValueException("Ssl termination not supported by the loadbalancer");
        }

        //check if the lb is already bound
        LoadBalancerCertMapVO certMapRule = _lbCertMapDao.findByLbRuleId(loadBalancer.getId());
        if (certMapRule != null) {
            if (! forced) {
                throw new InvalidParameterValueException("Another certificate is already bound to the LB");
            }
            s_logger.debug("Another certificate is already bound to the LB, removing it");
            removeCertFromLoadBalancer(lbRuleId);
        }

        //check for correct port
        if (loadBalancer.getLbProtocol() == null || !(loadBalancer.getLbProtocol().equals(NetUtils.SSL_PROTO)))
            throw new InvalidParameterValueException("Bad LB protocol: Expected ssl got " + loadBalancer.getLbProtocol());

        boolean success = false;
        FirewallRule.State backupState = loadBalancer.getState();

        try {

            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
            LoadBalancerCertMapVO certMap = new LoadBalancerCertMapVO(lbRuleId, certId, false);
            _lbCertMapDao.persist(certMap);
            applyLoadBalancerConfig(loadBalancer.getId());
            success = true;
        } catch (ResourceUnavailableException e) {
            if (isRollBackAllowedForProvider(loadBalancer)) {

                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                LoadBalancerCertMapVO certMap = _lbCertMapDao.findByLbRuleId(lbRuleId);
                _lbCertMapDao.remove(certMap.getId());
                s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + " while adding cert");
            }
            s_logger.warn("Unable to apply the load balancer config because resource is unavailable.", e);
        }
        return success;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_CERT_REMOVE, eventDescription = "removing certificate from load balancer", async = true)
    public boolean removeCertFromLoadBalancer(long lbRuleId) {
        CallContext caller = CallContext.current();

        LoadBalancerVO loadBalancer = _lbDao.findById(lbRuleId);
        LoadBalancerCertMapVO lbCertMap = _lbCertMapDao.findByLbRuleId(lbRuleId);

        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid load balancer value: " + lbRuleId);
        }

        if (lbCertMap == null) {
            throw new InvalidParameterException("No certificate is bound to lb with id: " + lbRuleId);
        }

        _accountMgr.checkAccess(caller.getCallingAccount(), null, true, loadBalancer);

        boolean success = false;
        FirewallRule.State backupState = loadBalancer.getState();
        try {

            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
            lbCertMap.setRevoke(true);
            _lbCertMapDao.persist(lbCertMap);

            if (!applyLoadBalancerConfig(lbRuleId)) {
                s_logger.warn("Failed to remove cert from load balancer rule id " + lbRuleId);
                CloudRuntimeException ex = new CloudRuntimeException("Failed to remove certificate load balancer rule id " + lbRuleId);
                ex.addProxyObject(loadBalancer.getUuid(), "loadBalancerId");
                throw ex;
            }
            success = true;
        } catch (ResourceUnavailableException e) {
            if (isRollBackAllowedForProvider(loadBalancer)) {
                lbCertMap.setRevoke(false);
                _lbCertMapDao.persist(lbCertMap);
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("Rolled back certificate removal lb id " + lbRuleId);
            }
            s_logger.warn("Unable to apply the load balancer config because resource is unavailable.", e);
            if (!success) {
                CloudRuntimeException ex = new CloudRuntimeException("Failed to remove certificate from load balancer rule id " + lbRuleId);
                ex.addProxyObject(loadBalancer.getUuid(), "loadBalancerId");
                throw ex;
            }
        }
        return success;
    }

    private boolean removeFromLoadBalancerInternal(long loadBalancerId, List<Long> instanceIds, boolean rollBack, Map<Long, List<String>> vmIdIpMap, boolean isAutoScaleVM) {
        CallContext caller = CallContext.current();

        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(loadBalancerId));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid load balancer value: " + loadBalancerId);
        }

        _accountMgr.checkAccess(caller.getCallingAccount(), null, true, loadBalancer);

        if (instanceIds == null && vmIdIpMap.isEmpty()) {
            throw new InvalidParameterValueException("Both instanceids and vmidipmap  can't be null");
        }

        // instanceIds and vmIdipmap is passed
        if (instanceIds != null && !vmIdIpMap.isEmpty()) {
            for(long instanceId: instanceIds) {
                if (!vmIdIpMap.containsKey(instanceId)) {
                    vmIdIpMap.put(instanceId, null);
                }
            }
        }

        //only instanceids list passed
        if (instanceIds != null && vmIdIpMap.isEmpty()){
            vmIdIpMap = new HashMap<Long, List<String>>();
            for (long instanceId: instanceIds){
                vmIdIpMap.put(instanceId, null);
            }
        }



        boolean success = false;
        FirewallRule.State backupState = loadBalancer.getState();
        Set<Long> vmIds = vmIdIpMap.keySet();

        if (!isAutoScaleVM) {
            for (long instanceId : vmIds) {
                autoScaleManager.checkIfVmActionAllowed(instanceId);
            }
        }

        try {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);

            for (long instanceId : vmIds) {
                List<String> lbVmIps = vmIdIpMap.get(instanceId);

                if (lbVmIps == null || lbVmIps.isEmpty()) {
                    List<LoadBalancerVMMapVO> lbVms = _lb2VmMapDao.listByLoadBalancerIdAndVmId(loadBalancerId, instanceId);
                    if (lbVms == null) {
                        throw new InvalidParameterValueException("The instance id: "+ instanceId +" is not configured "
                                + " for LB rule id " + loadBalancerId);
                    }

                    for (LoadBalancerVMMapVO lbvm: lbVms) {
                        lbvm.setRevoke(true);
                        _lb2VmMapDao.persist(lbvm);
                    }
                    s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", vmId " + instanceId);

                } else {
                    for (String vmIp: lbVmIps) {
                        LoadBalancerVMMapVO map = _lb2VmMapDao.findByLoadBalancerIdAndVmIdVmIp (loadBalancerId, instanceId, vmIp);
                        if (map == null) {
                            throw new InvalidParameterValueException("The instance id: "+ instanceId +" is not configured "
                                    + " for LB rule id " + loadBalancerId);
                        }
                        map.setRevoke(true);
                        _lb2VmMapDao.persist(map);
                        s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", vmId " +
                                instanceId + ", vmip " + vmIp);
                    }
                }
            }

            if (!applyLoadBalancerConfig(loadBalancerId)) {
                s_logger.warn("Failed to remove load balancer rule id " + loadBalancerId + " for vms " + instanceIds);
                CloudRuntimeException ex = new CloudRuntimeException("Failed to remove specified load balancer rule id for vms " + instanceIds);
                ex.addProxyObject(loadBalancer.getUuid(), "loadBalancerId");
                throw ex;
            }

            if (_autoScaleVmGroupDao.isAutoScaleLoadBalancer(loadBalancerId)) {
                List<Long> vmIdsList = new ArrayList<>(vmIdIpMap.keySet());
                _lb2VmMapDao.remove(loadBalancer.getId(), vmIdsList, null);
                for (Long instanceId: vmIdsList){
                    autoScaleVmGroupVmMapDao.removeByVm(instanceId);
                }
            }

            success = true;
        } catch (ResourceUnavailableException e) {
            if (rollBack && isRollBackAllowedForProvider(loadBalancer)) {

                for (long instanceId : vmIds) {
                    List<String> lbVmIps = vmIdIpMap.get(instanceId);

                    if (lbVmIps == null || lbVmIps.isEmpty()) {
                        LoadBalancerVMMapVO map = _lb2VmMapDao.findByLoadBalancerIdAndVmId(loadBalancerId, instanceId);
                        map.setRevoke(false);
                        _lb2VmMapDao.persist(map);
                        s_logger.debug("LB Rollback rule id: " + loadBalancerId + ",while removing vmId " + instanceId);
                    }else {
                        for (String vmIp: lbVmIps) {
                            LoadBalancerVMMapVO map = _lb2VmMapDao.findByLoadBalancerIdAndVmIdVmIp (loadBalancerId, instanceId, vmIp);
                            map.setRevoke(true);
                            _lb2VmMapDao.persist(map);
                            s_logger.debug("LB Rollback rule id: " + loadBalancerId + ",while removing vmId " +
                                    instanceId + ", vmip " + vmIp);
                        }
                    }
                }

                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("LB Rollback rule id: " + loadBalancerId + " while removing vm instances");
            }
            s_logger.warn("Unable to apply the load balancer config because resource is unavailable.", e);
        }
        if (!success) {
            CloudRuntimeException ex = new CloudRuntimeException("Failed to remove specified load balancer rule id for vms " + vmIds);
            ex.addProxyObject(loadBalancer.getUuid(), "loadBalancerId");
            throw ex;
        }
        return success;
    }

    @Override
    public boolean removeVmFromLoadBalancers(long instanceId) {
        boolean success = true;
        List<LoadBalancerVMMapVO> maps = _lb2VmMapDao.listByInstanceId(instanceId);
        if (maps == null || maps.isEmpty()) {
            return true;
        }

        Map<Long, List<Long>> lbsToReconfigure = new HashMap<Long, List<Long>>();

        // first set all existing lb mappings with Revoke state
        for (LoadBalancerVMMapVO map : maps) {
            long lbId = map.getLoadBalancerId();
            List<Long> instances = lbsToReconfigure.get(lbId);
            if (instances == null) {
                instances = new ArrayList<Long>();
            }
            instances.add(map.getInstanceId());
            lbsToReconfigure.put(lbId, instances);

            map.setRevoke(true);
            _lb2VmMapDao.persist(map);
            s_logger.debug("Set load balancer rule for revoke: rule id " + map.getLoadBalancerId() + ", vmId " + instanceId);
        }

        // Reapply all lbs that had the vm assigned
        if (lbsToReconfigure != null) {
            for (Map.Entry<Long, List<Long>> lb : lbsToReconfigure.entrySet()) {
                if (!removeFromLoadBalancerInternal(lb.getKey(), lb.getValue(), false, new HashMap<>(), false)) {
                    success = false;
                }
            }
        }
        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_DELETE, eventDescription = "deleting load balancer", async = true)
    public boolean deleteLoadBalancerRule(long loadBalancerId, boolean apply) {
        CallContext ctx = CallContext.current();
        Account caller = ctx.getCallingAccount();

        LoadBalancerVO rule = _lbDao.findById(loadBalancerId);

        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find load balancer rule " + loadBalancerId);
        }
        _accountMgr.checkAccess(caller, null, true, rule);

        boolean result = deleteLoadBalancerRule(loadBalancerId, apply, caller, ctx.getCallingUserId(), true);
        if (!result) {
            throw new CloudRuntimeException("Unable to remove load balancer rule " + loadBalancerId);
        }
        return result;
    }

    @DB
    public boolean deleteLoadBalancerRule(final long loadBalancerId, boolean apply, Account caller, long callerUserId, boolean rollBack) {
        List<AutoScaleVmGroupVO> vmGroups = _autoScaleVmGroupDao.listByLoadBalancer(loadBalancerId);
        if (CollectionUtils.isNotEmpty(vmGroups)) {
            throw new CloudRuntimeException(String.format("Cannot delete load balancer rule %d because it is being used by %s Autoscale VM group.", loadBalancerId, vmGroups.size()));
        }

        final LoadBalancerVO lb = _lbDao.findById(loadBalancerId);
        FirewallRule.State backupState = lb.getState();

        // remove any ssl certs associated with this LB rule before trying to delete it.
        LoadBalancerCertMapVO lbCertMap = _lbCertMapDao.findByLbRuleId(loadBalancerId);
        if (lbCertMap != null) {
            boolean removeResult = removeCertFromLoadBalancer(loadBalancerId);
            if (!removeResult) {
                throw new CloudRuntimeException("Unable to remove certificate from load balancer rule " + loadBalancerId);
            }
        }

        List<LoadBalancerVMMapVO> backupMaps = Transaction.execute(new TransactionCallback<List<LoadBalancerVMMapVO>>() {
            @Override
            public List<LoadBalancerVMMapVO> doInTransaction(TransactionStatus status) {
                boolean generateUsageEvent = false;

                if (lb.getState() == FirewallRule.State.Staged) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found a rule that is still in stage state so just removing it: " + lb);
                    }
                    generateUsageEvent = true;
                } else if (lb.getState() == FirewallRule.State.Add || lb.getState() == FirewallRule.State.Active) {
                    lb.setState(FirewallRule.State.Revoke);
                    _lbDao.persist(lb);
                    generateUsageEvent = true;
                }
                List<LoadBalancerVMMapVO> backupMaps = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId);
                List<LoadBalancerVMMapVO> maps = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId);
                if (maps != null) {
                    for (LoadBalancerVMMapVO map : maps) {
                        map.setRevoke(true);
                        _lb2VmMapDao.persist(map);
                        s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", vmId " + map.getInstanceId());
                    }
                }

                List<LBHealthCheckPolicyVO> hcPolicies = _lb2healthcheckDao.listByLoadBalancerIdAndDisplayFlag(loadBalancerId, null);
                for (LBHealthCheckPolicyVO lbHealthCheck : hcPolicies) {
                    lbHealthCheck.setRevoke(true);
                    _lb2healthcheckDao.persist(lbHealthCheck);
                }

                if (generateUsageEvent) {
                    // Generate usage event right after all rules were marked for revoke
                    Network network = _networkModel.getNetwork(lb.getNetworkId());
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_LOAD_BALANCER_DELETE, lb.getAccountId(), network.getDataCenterId(), lb.getId(),
                            null, LoadBalancingRule.class.getName(), lb.getUuid());
                }

                return backupMaps;
            }
        });

        // gather external network usage stats for this lb rule
        NetworkVO network = _networkDao.findById(lb.getNetworkId());
        if (network != null) {
            if (_networkModel.networkIsConfiguredForExternalNetworking(network.getDataCenterId(), network.getId())) {
                _externalDeviceUsageMgr.updateExternalLoadBalancerNetworkUsageStats(loadBalancerId);
            }
        }

        if (apply) {
            try {
                if (!applyLoadBalancerConfig(loadBalancerId)) {
                    s_logger.warn("Unable to apply the load balancer config");
                    return false;
                }
            } catch (ResourceUnavailableException e) {
                if (rollBack && isRollBackAllowedForProvider(lb)) {
                    if (backupMaps != null) {
                        for (LoadBalancerVMMapVO map : backupMaps) {
                            _lb2VmMapDao.persist(map);
                            s_logger.debug("LB Rollback rule id: " + loadBalancerId + ", vmId " + map.getInstanceId());
                        }
                    }
                    lb.setState(backupState);
                    _lbDao.persist(lb);
                    s_logger.debug("LB Rollback rule id: " + loadBalancerId + " while deleting LB rule.");
                } else {
                    s_logger.warn("Unable to apply the load balancer config because resource is unavailable.", e);
                }
                return false;
            }
        }

        FirewallRuleVO relatedRule = _firewallDao.findByRelatedId(lb.getId());
        if (relatedRule != null) {
            s_logger.warn("Unable to remove firewall rule id=" + lb.getId() + " as it has related firewall rule id=" + relatedRule.getId() +
                "; leaving it in Revoke state");
            return false;
        } else {
            _firewallMgr.removeRule(lb);
        }

        // FIXME: breaking the dependency on ELB manager. This breaks
        // functionality of ELB using virtual router
        // Bug CS-15411 opened to document this
        // _elbMgr.handleDeleteLoadBalancerRule(lb, callerUserId, caller);

        s_logger.debug("Load balancer with id " + lb.getId() + " is removed successfully");

        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_CREATE, eventDescription = "creating load balancer")
    public LoadBalancer createPublicLoadBalancerRule(String xId, String name, String description, int srcPortStart, int srcPortEnd, int defPortStart, int defPortEnd,
        Long ipAddrId, String protocol, String algorithm, long networkId, long lbOwnerId, boolean openFirewall, String lbProtocol, Boolean forDisplay) throws NetworkRuleConflictException,
        InsufficientAddressCapacityException {
            return createPublicLoadBalancerRule(xId, name, description, srcPortStart, srcPortEnd, defPortStart, defPortEnd, ipAddrId, protocol, algorithm, networkId, lbOwnerId, openFirewall, lbProtocol, forDisplay, null);
        }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_CREATE, eventDescription = "creating load balancer")
    public LoadBalancer createPublicLoadBalancerRule(String xId, String name, String description, int srcPortStart, int srcPortEnd, int defPortStart, int defPortEnd,
            Long ipAddrId, String protocol, String algorithm, long networkId, long lbOwnerId, boolean openFirewall, String lbProtocol, Boolean forDisplay, List<String> cidrList) throws NetworkRuleConflictException,
            InsufficientAddressCapacityException {
        Account lbOwner = _accountMgr.getAccount(lbOwnerId);

        if (srcPortStart != srcPortEnd) {
            throw new InvalidParameterValueException("Port ranges are not supported by the load balancer");
        }

        IPAddressVO ipVO = null;
        if (ipAddrId != null) {
            ipVO = _ipAddressDao.findById(ipAddrId);
        }

        Network network = _networkModel.getNetwork(networkId);

        // FIXME: breaking the dependency on ELB manager. This breaks
        // functionality of ELB using virtual router
        // Bug CS-15411 opened to document this
        // LoadBalancer result = _elbMgr.handleCreateLoadBalancerRule(lb,
        // lbOwner, lb.getNetworkId());
        LoadBalancer result = null;
        IpAddress systemIp = null;
        NetworkOffering off = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());

        if (srcPortStart == DNS_PORT && ipVO.isSourceNat()) {
            List<NetworkOfferingServiceMapVO> offeringServices = _networkOfferingServiceDao.listByNetworkOfferingId(network.getNetworkOfferingId());
            for (NetworkOfferingServiceMapVO serviceMapVo: offeringServices) {
                if (serviceMapVo.getService().equals(Service.Dns.getName())) {
                    throw new InvalidParameterValueException("Error adding load balancer rule, cannot add port 53 with network service offering having DNS service and Source NAT.");
                }
            }
        }

        if (off.isElasticLb() && ipVO == null && network.getVpcId() == null) {
            systemIp = _ipAddrMgr.assignSystemIp(networkId, lbOwner, true, false);
            if (systemIp != null) {
                ipVO = _ipAddressDao.findById(systemIp.getId());
            }
        }

        // Validate ip address
        if (ipVO == null) {
            throw new InvalidParameterValueException("Unable to create load balance rule; can't find/allocate source IP");
        } else if (ipVO.isOneToOneNat()) {
            throw new NetworkRuleConflictException("Can't do load balance on ip address: " + ipVO.getAddress());
        }

        String cidrString = generateCidrString(cidrList);

        boolean performedIpAssoc = false;
        try {
            if (ipVO.getAssociatedWithNetworkId() == null) {
                boolean assignToVpcNtwk = network.getVpcId() != null && ipVO.getVpcId() != null && ipVO.getVpcId().longValue() == network.getVpcId();
                if (assignToVpcNtwk) {
                    // set networkId just for verification purposes
                    _networkModel.checkIpForService(ipVO, Service.Lb, networkId);

                    s_logger.debug("The ip is not associated with the VPC network id=" + networkId + " so assigning");
                    ipVO = _ipAddrMgr.associateIPToGuestNetwork(ipAddrId, networkId, false);
                    performedIpAssoc = true;
                }
            } else {
                _networkModel.checkIpForService(ipVO, Service.Lb, null);
            }

            if (ipVO.getAssociatedWithNetworkId() == null) {
                throw new InvalidParameterValueException("Ip address " + ipVO + " is not assigned to the network " + network);
            }

            result = createPublicLoadBalancer(xId, name, description, srcPortStart, defPortStart, ipVO.getId(), protocol, algorithm, openFirewall, CallContext.current(),
                    lbProtocol, forDisplay, cidrString);
        } catch (Exception ex) {
            s_logger.warn("Failed to create load balancer due to ", ex);
            if (ex instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException)ex;
            }

            if (ex instanceof InvalidParameterValueException) {
                throw (InvalidParameterValueException)ex;
            }

        } finally {
            if (result == null && systemIp != null) {
                s_logger.debug("Releasing system IP address " + systemIp + " as corresponding lb rule failed to create");
                _ipAddrMgr.handleSystemIpRelease(systemIp);
            }
            // release ip address if ipassoc was perfored
            if (performedIpAssoc) {
                ipVO = _ipAddressDao.findById(ipVO.getId());
                _vpcMgr.unassignIPFromVpcNetwork(ipVO.getId(), networkId);
            }
        }

        if (result == null) {
            throw new CloudRuntimeException("Failed to create load balancer rule: " + name);
        }

        return result;
    }
   /**
    * Transforms the cidrList from a List of Strings to a String which contains all the CIDRs from cidrList separated by whitespaces. This is used to facilitate both the persistence
    * in the DB and also later when building the configuration String in the getRulesForPool method of the HAProxyConfigurator class.
   */
    protected String generateCidrString(List<String> cidrList) {
        if (cidrList == null) {
            s_logger.trace("The given CIDR list is null, therefore we will return null.");
            return null;
        }
        String cidrString;
        StringBuilder sb = new StringBuilder();
        for (String cidr: cidrList) {
            cidr = validateCidr(cidr);
            sb.append(cidr).append(' ');
        }
        cidrString = sb.toString();
        s_logger.trace(String.format("From the cidrList [%s] we generated the following CIDR String [%s].", cidrList, cidrString));
        return StringUtils.trim(cidrString);
    }

    private String validateCidr(String cidr) {
        cidr = StringUtils.trim(cidr);
        boolean validCidr = NetUtils.isValidIp4Cidr(cidr) || NetUtils.isValidIp6Cidr(cidr);
        boolean validIp = NetUtils.isValidIp4(cidr) || NetUtils.isValidIp6(cidr);
        if (!validCidr && !validIp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("CIDR [%s] is invalid.", cidr));
        }
        return cidr;
    }
    @DB
    @Override
    public LoadBalancer createPublicLoadBalancer(final String xId, final String name, final String description, final int srcPort, final int destPort,
 final long sourceIpId,
            final String protocol, final String algorithm, final boolean openFirewall, final CallContext caller, final String lbProtocol, final Boolean forDisplay, String cidrList)
            throws NetworkRuleConflictException {

        if (!NetUtils.isValidPort(destPort)) {
            throw new InvalidParameterValueException("privatePort is an invalid value: " + destPort);
        }

        if ((algorithm == null) || !NetUtils.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterValueException("Invalid algorithm: " + algorithm);
        }

        final IPAddressVO ipAddr = _ipAddressDao.findById(sourceIpId);
        // make sure ip address exists
        if (ipAddr == null || !ipAddr.readyToUse()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address id specified");
            if (ipAddr == null) {
                ex.addProxyObject(String.valueOf(sourceIpId), "sourceIpId");
            } else {
                ex.addProxyObject(ipAddr.getUuid(), "sourceIpId");
            }
            throw ex;
        } else if (ipAddr.isOneToOneNat()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to create load balancer rule; specified sourceip id has static nat enabled");
            ex.addProxyObject(ipAddr.getUuid(), "sourceIpId");
            throw ex;
        }

        _accountMgr.checkAccess(caller.getCallingAccount(), null, true, ipAddr);

        final Long networkId = ipAddr.getAssociatedWithNetworkId();
        if (networkId == null) {
            InvalidParameterValueException ex =
                new InvalidParameterValueException("Unable to create load balancer rule ; specified sourceip id is not associated with any network");
            ex.addProxyObject(ipAddr.getUuid(), "sourceIpId");
            throw ex;
        }

        // verify that lb service is supported by the network
        isLbServiceSupportedInNetwork(networkId, Scheme.Public);

        _firewallMgr.validateFirewallRule(caller.getCallingAccount(), ipAddr, srcPort, srcPort, protocol, Purpose.LoadBalancing, FirewallRuleType.User, networkId, null);

        LoadBalancerVO newRule =
            new LoadBalancerVO(xId, name, description, sourceIpId, srcPort, destPort, algorithm, networkId, ipAddr.getAllocatedToAccountId(),
                ipAddr.getAllocatedInDomainId(), lbProtocol, cidrList);

        // verify rule is supported by Lb provider of the network
        Ip sourceIp = getSourceIp(newRule);
        LoadBalancingRule loadBalancing =
            new LoadBalancingRule(newRule, new ArrayList<LbDestination>(), new ArrayList<LbStickinessPolicy>(), new ArrayList<LbHealthCheckPolicy>(), sourceIp, null,
                lbProtocol);
        if (!validateLbRule(loadBalancing)) {
            throw new InvalidParameterValueException("LB service provider cannot support this rule");
        }

        return Transaction.execute(new TransactionCallbackWithException<LoadBalancerVO, NetworkRuleConflictException>() {
            @Override
            public LoadBalancerVO doInTransaction(TransactionStatus status) throws NetworkRuleConflictException {
                LoadBalancerVO newRule =
                    new LoadBalancerVO(xId, name, description, sourceIpId, srcPort, destPort, algorithm, networkId, ipAddr.getAllocatedToAccountId(),
                        ipAddr.getAllocatedInDomainId(), lbProtocol, cidrList);

                if (forDisplay != null) {
                    newRule.setDisplay(forDisplay);
                }

                // verify rule is supported by Lb provider of the network
                Ip sourceIp = getSourceIp(newRule);
                LoadBalancingRule loadBalancing =
                    new LoadBalancingRule(newRule, new ArrayList<LbDestination>(), new ArrayList<LbStickinessPolicy>(), new ArrayList<LbHealthCheckPolicy>(), sourceIp,
                        null, lbProtocol);
                if (!validateLbRule(loadBalancing)) {
                    throw new InvalidParameterValueException("LB service provider cannot support this rule");
                }

                newRule = _lbDao.persist(newRule);

                //create rule for all CIDRs
                if (openFirewall) {
                    _firewallMgr.createRuleForAllCidrs(sourceIpId, caller.getCallingAccount(), srcPort, srcPort, protocol, null, null, newRule.getId(), networkId);
                }

                boolean success = true;

                try {
                    _firewallMgr.detectRulesConflict(newRule);
                    if (!_firewallDao.setStateToAdd(newRule)) {
                        throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
                    }
                    s_logger.debug("Load balancer " + newRule.getId() + " for Ip address id=" + sourceIpId + ", public port " + srcPort + ", private port " + destPort +
                        " is added successfully.");
                    CallContext.current().setEventDetails("Load balancer Id: " + newRule.getId());
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_LOAD_BALANCER_CREATE, ipAddr.getAllocatedToAccountId(), ipAddr.getDataCenterId(), newRule.getId(),
                        null, LoadBalancingRule.class.getName(), newRule.getUuid());

                    return newRule;
                } catch (Exception e) {
                    success = false;
                    if (e instanceof NetworkRuleConflictException) {
                        throw (NetworkRuleConflictException)e;
                    }
                    throw new CloudRuntimeException("Unable to add rule for ip address id=" + newRule.getSourceIpAddressId(), e);
                } finally {
                    if (!success && newRule != null) {
                        _firewallMgr.revokeRelatedFirewallRule(newRule.getId(), false);
                        removeLBRule(newRule);
                    }
                }
            }
        });

    }

    @Override
    public boolean applyLoadBalancerConfig(long lbRuleId) throws ResourceUnavailableException {
        LoadBalancerVO lb = _lbDao.findById(lbRuleId);
        List<LoadBalancerVO> lbs;
        if (isRollBackAllowedForProvider(lb)) {
            // this is for Netscalar type of devices. if their is failure the db
            // entries will be rollbacked.
            lbs = Arrays.asList(lb);
        } else {
            boolean onlyRulesInTransitionState = true;
            for (LoadBalancingServiceProvider lbElement : _lbProviders) {
                Provider provider = lbElement.getProvider();
                boolean isLbProvider = _networkModel.isProviderSupportServiceInNetwork(lb.getNetworkId(), Service.Lb, provider);
                if (!isLbProvider) {
                    continue;
                }
                onlyRulesInTransitionState = lbElement.handlesOnlyRulesInTransitionState();
                break;
            }

            // get all rules in transition state
            if (onlyRulesInTransitionState) {
                lbs = _lbDao.listInTransitionStateByNetworkIdAndScheme(lb.getNetworkId(), lb.getScheme());
            } else {
                lbs = _lbDao.listByNetworkIdAndScheme(lb.getNetworkId(), lb.getScheme());
            }

        }
        return applyLoadBalancerRules(lbs, true);
    }

    @Override
    public boolean revokeLoadBalancersForNetwork(long networkId, Scheme scheme) throws ResourceUnavailableException {
        List<LoadBalancerVO> lbs = _lbDao.listByNetworkIdAndScheme(networkId, scheme);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Revoking " + lbs.size() + " " + scheme + " load balancing rules for network id=" + networkId);
        }
        if (lbs != null) {
            for (LoadBalancerVO lb : lbs) { // called during restart, not persisting state in db
                lb.setState(FirewallRule.State.Revoke);
            }
            return applyLoadBalancerRules(lbs, false); // called during restart, not persisting state in db
        } else {
            s_logger.info("Network id=" + networkId + " doesn't have load balancer rules, nothing to revoke");
            return true;
        }
    }

    @Override
    public boolean applyLoadBalancersForNetwork(long networkId, Scheme scheme) throws ResourceUnavailableException {
        List<LoadBalancerVO> lbs = _lbDao.listByNetworkIdAndScheme(networkId, scheme);
        if (lbs != null) {
            s_logger.debug("Applying load balancer rules of scheme " + scheme + " in network id=" + networkId);
            return applyLoadBalancerRules(lbs, true);
        } else {
            s_logger.info("Network id=" + networkId + " doesn't have load balancer rules of scheme " + scheme + ", nothing to apply");
            return true;
        }
    }

    protected boolean applyLbRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        boolean handled = false;
        for (LoadBalancingServiceProvider lbElement : _lbProviders) {
            Provider provider = lbElement.getProvider();
            boolean isLbProvider = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Lb, provider);
            if (!isLbProvider) {
                continue;
            }
            handled = lbElement.applyLBRules(network, rules);
            if (handled)
                break;
        }
        return handled;
    }

    private LoadBalancingRule getLoadBalancerRuleToApply(LoadBalancerVO lb) {

        List<LbStickinessPolicy> policyList = getStickinessPolicies(lb.getId());
        Ip sourceIp = getSourceIp(lb);
        LbSslCert sslCert = getLbSslCert(lb.getId());
        LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, null, policyList, null, sourceIp, sslCert, lb.getLbProtocol());

        if (_autoScaleVmGroupDao.isAutoScaleLoadBalancer(lb.getId())) {
            // Get the associated VmGroup
            AutoScaleVmGroupVO vmGroup = _autoScaleVmGroupDao.listByAll(lb.getId(), null).get(0);
            LbAutoScaleVmGroup lbAutoScaleVmGroup = getLbAutoScaleVmGroup(vmGroup, vmGroup.getState(), lb);
            loadBalancing.setAutoScaleVmGroup(lbAutoScaleVmGroup);
        } else {
            List<LbDestination> dstList = getExistingDestinations(lb.getId());
            loadBalancing.setDestinations(dstList);
            List<LbHealthCheckPolicy> hcPolicyList = getHealthCheckPolicies(lb.getId());
            loadBalancing.setHealthCheckPolicies(hcPolicyList);
        }

        return loadBalancing;
    }

    @DB
    protected boolean applyLoadBalancerRules(List<LoadBalancerVO> lbs, boolean updateRulesInDB) throws ResourceUnavailableException {
        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        for (LoadBalancerVO lb : lbs) {
            rules.add(getLoadBalancerRuleToApply(lb));
        }

        if (!applyLbRules(rules, false)) {
            s_logger.debug("LB rules are not completely applied");
            return false;
        }

        if (updateRulesInDB) {
            for (final LoadBalancerVO lb : lbs) {
                boolean checkForReleaseElasticIp = Transaction.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        boolean checkForReleaseElasticIp = false;

                        if (lb.getState() == FirewallRule.State.Revoke) {
                            removeLBRule(lb);
                            s_logger.debug("LB " + lb.getId() + " is successfully removed");
                            checkForReleaseElasticIp = true;
                        } else if (lb.getState() == FirewallRule.State.Add) {
                            lb.setState(FirewallRule.State.Active);
                            s_logger.debug("LB rule " + lb.getId() + " state is set to Active");
                            _lbDao.persist(lb);
                        }

                        // remove LB-Vm mappings that were state to revoke
                        List<LoadBalancerVMMapVO> lbVmMaps = _lb2VmMapDao.listByLoadBalancerId(lb.getId(), true);
                        List<Long> instanceIds = new ArrayList<Long>();

                        for (LoadBalancerVMMapVO lbVmMap : lbVmMaps) {
                            instanceIds.add(lbVmMap.getInstanceId());
                            _lb2VmMapDao.remove(lb.getId(), lbVmMap.getInstanceId(), lbVmMap.getInstanceIp(), null);
                            s_logger.debug("Load balancer rule id " + lb.getId() + " is removed for vm " +
                                    lbVmMap.getInstanceId() + " instance ip " + lbVmMap.getInstanceIp());
                        }


                        if (_lb2VmMapDao.listByLoadBalancerId(lb.getId()).isEmpty()) {
                            lb.setState(FirewallRule.State.Add);
                            _lbDao.persist(lb);
                            s_logger.debug("LB rule " + lb.getId() + " state is set to Add as there are no more active LB-VM mappings");
                        }

                        // remove LB-Stickiness policy mapping that were state to revoke
                        List<LBStickinessPolicyVO> stickinesspolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(lb.getId(), true);
                        if (!stickinesspolicies.isEmpty()) {
                            _lb2stickinesspoliciesDao.remove(lb.getId(), true);
                            s_logger.debug("Load balancer rule id " + lb.getId() + " is removed stickiness policies");
                        }

                        // remove LB-HealthCheck policy mapping that were state to
                        // revoke
                        List<LBHealthCheckPolicyVO> healthCheckpolicies = _lb2healthcheckDao.listByLoadBalancerId(lb.getId(), true);
                        if (!healthCheckpolicies.isEmpty()) {
                            _lb2healthcheckDao.remove(lb.getId(), true);
                            s_logger.debug("Load balancer rule id " + lb.getId() + " is removed health check monitors policies");
                        }

                        LoadBalancerCertMapVO lbCertMap = _lbCertMapDao.findByLbRuleId(lb.getId());
                        if (lbCertMap != null && lbCertMap.isRevoke()) {
                            _lbCertMapDao.remove(lbCertMap.getId());
                            s_logger.debug("Load balancer rule id " + lb.getId() + " removed certificate mapping");
                        }

                        return checkForReleaseElasticIp;
                    }
                });

                if (checkForReleaseElasticIp && lb.getSourceIpAddressId() != null) {
                    boolean success = true;
                    long count = _firewallDao.countRulesByIpId(lb.getSourceIpAddressId());
                    if (count == 0) {
                        try {
                            success = handleSystemLBIpRelease(lb);
                        } catch (Exception ex) {
                            s_logger.warn("Failed to release system ip as a part of lb rule " + lb + " deletion due to exception ", ex);
                            success = false;
                        } finally {
                            if (!success) {
                                s_logger.warn("Failed to release system ip as a part of lb rule " + lb + " deletion");
                            }
                        }
                    }
                }
                // if the rule is the last one for the ip address assigned to
                // VPC, unassign it from the network
                if (lb.getSourceIpAddressId() != null) {
                    IpAddress ip = _ipAddressDao.findById(lb.getSourceIpAddressId());
                    _vpcMgr.unassignIPFromVpcNetwork(ip.getId(), lb.getNetworkId());
                }
            }
        }

        return true;
    }

    protected boolean handleSystemLBIpRelease(LoadBalancerVO lb) {
        IpAddress ip = _ipAddressDao.findById(lb.getSourceIpAddressId());
        boolean success = true;
        if (ip.getSystem()) {
            s_logger.debug("Releasing system ip address " + lb.getSourceIpAddressId() + " as a part of delete lb rule");
            if (!_ipAddrMgr.disassociatePublicIpAddress(lb.getSourceIpAddressId(), CallContext.current().getCallingUserId(), CallContext.current().getCallingAccount())) {
                s_logger.warn("Unable to release system ip address id=" + lb.getSourceIpAddressId() + " as a part of delete lb rule");
                success = false;
            } else {
                s_logger.warn("Successfully released system ip address id=" + lb.getSourceIpAddressId() + " as a part of delete lb rule");
            }
        }
        return success;
    }

    @Override
    public boolean removeAllLoadBalanacersForIp(long ipId, Account caller, long callerUserId) {

        //Included revoked rules to remove the rules of ips which are in revoke state
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurpose(ipId, Purpose.LoadBalancing);

        if (rules != null) {
            s_logger.debug("Found " + rules.size() + " lb rules to cleanup");
            for (FirewallRule rule : rules) {
                boolean result = deleteLoadBalancerRule(rule.getId(), true, caller, callerUserId, false);
                if (result == false) {
                    s_logger.warn("Unable to remove load balancer rule " + rule.getId());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean removeAllLoadBalanacersForNetwork(long networkId, Account caller, long callerUserId) {
        List<FirewallRuleVO> rules = _firewallDao.listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.LoadBalancing);
        if (rules != null) {
            s_logger.debug("Found " + rules.size() + " lb rules to cleanup");
            for (FirewallRule rule : rules) {
                boolean result = deleteLoadBalancerRule(rule.getId(), true, caller, callerUserId, false);
                if (result == false) {
                    s_logger.warn("Unable to remove load balancer rule " + rule.getId());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public List<LbStickinessPolicy> getStickinessPolicies(long lbId) {
        List<LbStickinessPolicy> stickinessPolicies = new ArrayList<LbStickinessPolicy>();
        List<LBStickinessPolicyVO> sDbpolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(lbId, false);

        for (LBStickinessPolicyVO sDbPolicy : sDbpolicies) {
            LbStickinessPolicy sPolicy = new LbStickinessPolicy(sDbPolicy.getMethodName(), sDbPolicy.getParams(), sDbPolicy.isRevoke());
            stickinessPolicies.add(sPolicy);
        }
        return stickinessPolicies;
    }

    @Override
    public List<LbHealthCheckPolicy> getHealthCheckPolicies(long lbId) {
        List<LbHealthCheckPolicy> healthCheckPolicies = new ArrayList<LbHealthCheckPolicy>();
        List<LBHealthCheckPolicyVO> hcDbpolicies = _lb2healthcheckDao.listByLoadBalancerIdAndDisplayFlag(lbId, null);

        for (LBHealthCheckPolicyVO policy : hcDbpolicies) {
            String pingpath = policy.getpingpath();
            LbHealthCheckPolicy hDbPolicy =
                new LbHealthCheckPolicy(pingpath, policy.getDescription(), policy.getResponseTime(), policy.getHealthcheckInterval(), policy.getHealthcheckThresshold(),
                    policy.getUnhealthThresshold(), policy.isRevoke());
            healthCheckPolicies.add(hDbPolicy);
        }
        return healthCheckPolicies;
    }

    @Override
    public List<LbDestination> getExistingDestinations(long lbId) {
        List<LbDestination> dstList = new ArrayList<LbDestination>();
        List<LoadBalancerVMMapVO> lbVmMaps = _lb2VmMapDao.listByLoadBalancerId(lbId);
        LoadBalancerVO lb = _lbDao.findById(lbId);

        String dstIp = null;
        for (LoadBalancerVMMapVO lbVmMap : lbVmMaps) {
            UserVm vm = _vmDao.findById(lbVmMap.getInstanceId());
            Nic nic = _nicDao.findByInstanceIdAndNetworkIdIncludingRemoved(lb.getNetworkId(), vm.getId());
            dstIp = lbVmMap.getInstanceIp() == null ? nic.getIPv4Address(): lbVmMap.getInstanceIp();
            LbDestination lbDst = new LbDestination(lb.getDefaultPortStart(), lb.getDefaultPortEnd(), dstIp, lbVmMap.isRevoke());
            dstList.add(lbDst);
        }
        return dstList;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_UPDATE, eventDescription = "updating load balancer", async = true)
    public LoadBalancer updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long lbRuleId = cmd.getId();
        String name = cmd.getLoadBalancerName();
        String description = cmd.getDescription();
        String algorithm = cmd.getAlgorithm();
        LoadBalancerVO lb = _lbDao.findById(lbRuleId);
        LoadBalancerVO lbBackup = _lbDao.findById(lbRuleId);
        String customId = cmd.getCustomId();
        Boolean forDisplay = cmd.getDisplay();
        String lbProtocol = cmd.getLbProtocol();

        if (lb == null) {
            throw new InvalidParameterValueException("Unable to find lb rule by id=" + lbRuleId);
        }

        // check permissions
        _accountMgr.checkAccess(caller, null, true, lb);

        if (name != null) {
            lb.setName(name);
        }

        if (description != null) {
            lb.setDescription(description);
        }

        if (algorithm != null) {
            lb.setAlgorithm(algorithm);
        }

        if (customId != null) {
            lb.setUuid(customId);
        }

        if (forDisplay != null) {
            lb.setDisplay(forDisplay);
        }

        if (StringUtils.isNotBlank(lbProtocol)) {
            lb.setLbProtocol(lbProtocol);
        }

        // Validate rule in LB provider
        LoadBalancingRule rule = getLoadBalancerRuleToApply(lb);
        if (!validateLbRule(rule)) {
            throw new InvalidParameterValueException("Modifications in lb rule " + lbRuleId + " are not supported.");
        }

        LoadBalancerVO tmplbVo = _lbDao.findById(lbRuleId);
        boolean success = _lbDao.update(lbRuleId, lb);

        // If algorithm is changed, have to reapply the lb config
        if ((algorithm != null && tmplbVo.getAlgorithm().compareTo(algorithm) != 0)
                || (lbProtocol != null && ! tmplbVo.getLbProtocol().equalsIgnoreCase(lbProtocol))) {
            try {
                lb.setState(FirewallRule.State.Add);
                _lbDao.persist(lb);
                applyLoadBalancerConfig(lbRuleId);
            } catch (ResourceUnavailableException e) {
                if (isRollBackAllowedForProvider(lb)) {
                    /*
                     * NOTE : We use lb object to update db instead of lbBackup
                     * object since db layer will fail to update if there is no
                     * change in the object.
                     */
                    if (lbBackup.getName() != null) {
                        lb.setName(lbBackup.getName());
                    }
                    if (lbBackup.getDescription() != null) {
                        lb.setDescription(lbBackup.getDescription());
                    }
                    if (lbBackup.getAlgorithm() != null) {
                        lb.setAlgorithm(lbBackup.getAlgorithm());
                    }
                    if (lbBackup.getLbProtocol() != null) {
                        lb.setLbProtocol(lbBackup.getLbProtocol());
                    }
                    lb.setState(lbBackup.getState());
                    _lbDao.update(lb.getId(), lb);
                    _lbDao.persist(lb);

                    s_logger.debug("LB Rollback rule id: " + lbRuleId + " while updating LB rule.");
                }
                s_logger.warn("Unable to apply the load balancer config because resource is unavailable.", e);
                success = false;
            }
        }

        if (!success) {
            throw new CloudRuntimeException("Failed to update load balancer rule: " + lbRuleId);
        }

        return lb;
    }

    @Override
    public Pair<List<? extends UserVm>, List<String>> listLoadBalancerInstances(ListLoadBalancerRuleInstancesCmd cmd) throws PermissionDeniedException {
        Account caller = CallContext.current().getCallingAccount();
        Long loadBalancerId = cmd.getId();
        Boolean applied = cmd.isApplied();

        if (applied == null) {
            applied = Boolean.TRUE;
        }

        LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            return null;
        }

        _accountMgr.checkAccess(caller, null, true, loadBalancer);

        List<UserVmVO> loadBalancerInstances = new ArrayList<UserVmVO>();
        List<String> serviceStates = new ArrayList<String>();
        List<LoadBalancerVMMapVO> vmLoadBalancerMappings = null;
        vmLoadBalancerMappings = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId);
        if(vmLoadBalancerMappings == null) {
            String msg = "no VM Loadbalancer Mapping found";
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        Map<Long, String> vmServiceState = new HashMap<Long, String>(vmLoadBalancerMappings.size());
        List<Long> appliedInstanceIdList = new ArrayList<Long>();

        if ((vmLoadBalancerMappings != null) && !vmLoadBalancerMappings.isEmpty()) {
            for (LoadBalancerVMMapVO vmLoadBalancerMapping : vmLoadBalancerMappings) {
                appliedInstanceIdList.add(vmLoadBalancerMapping.getInstanceId());
                vmServiceState.put(vmLoadBalancerMapping.getInstanceId(), vmLoadBalancerMapping.getState());
            }
        }

        List<UserVmVO> userVms = _vmDao.listVirtualNetworkInstancesByAcctAndNetwork(loadBalancer.getAccountId(), loadBalancer.getNetworkId());

        for (UserVmVO userVm : userVms) {
            // if the VM is destroyed, being expunged, in an error state, or in
            // an unknown state, skip it
            switch (userVm.getState()) {
            case Destroyed:
            case Expunging:
            case Error:
            case Unknown:
                continue;
            }

            boolean isApplied = appliedInstanceIdList.contains(userVm.getId());
            if ((isApplied && applied) || (!isApplied && !applied)) {
                loadBalancerInstances.add(userVm);
                serviceStates.add(vmServiceState.get(userVm.getId()));
            }
        }
        return new Pair<List<? extends UserVm>, List<String>>(loadBalancerInstances, serviceStates);
    }

    @Override
    public List<String> listLbVmIpAddress (long id, long vmId) {

        List <LoadBalancerVMMapVO> listLbvmMapVo = _lb2VmMapDao.listByLoadBalancerIdAndVmId(id, vmId);

        List <String> vmIps = new ArrayList<String>();
        for (LoadBalancerVMMapVO lbVmVo : listLbvmMapVo) {
            vmIps.add(lbVmVo.getInstanceIp());
        }
        return vmIps;
    }

    @Override
    public List<LbStickinessMethod> getStickinessMethods(long networkid) {
        String capability = getLBCapability(networkid, Capability.SupportedStickinessMethods.getName());
        if (capability == null) {
            return null;
        }
        Gson gson = new Gson();
        java.lang.reflect.Type listType = new TypeToken<List<LbStickinessMethod>>() {
        }.getType();
        List<LbStickinessMethod> result = gson.fromJson(capability, listType);
        return result;
    }

    @Override
    public List<LBStickinessPolicyVO> searchForLBStickinessPolicies(ListLBStickinessPoliciesCmd cmd) throws PermissionDeniedException {
        Account caller = CallContext.current().getCallingAccount();
        Long loadBalancerId = cmd.getLbRuleId();
        Long stickinessId = cmd.getId();

        boolean forDisplay = cmd.getDisplay();
        LoadBalancerVO loadBalancer = null;

        if (loadBalancerId == null) {
            loadBalancer = findLbByStickinessId(stickinessId);
        } else {
            loadBalancer = _lbDao.findById(loadBalancerId);
        }

        if (loadBalancer == null) {
            return null;
        }

        _accountMgr.checkAccess(caller, null, true, loadBalancer);

        List<LBStickinessPolicyVO> sDbpolicies = _lb2stickinesspoliciesDao.listByLoadBalancerIdAndDisplayFlag(loadBalancer.getId(), forDisplay);

        return sDbpolicies;
    }

    @Override
    public List<LBHealthCheckPolicyVO> searchForLBHealthCheckPolicies(ListLBHealthCheckPoliciesCmd cmd) throws PermissionDeniedException {
        Account caller = CallContext.current().getCallingAccount();
        Long loadBalancerId = cmd.getLbRuleId();
        Long policyId = cmd.getId();
        boolean forDisplay = cmd.getDisplay();
        if(loadBalancerId == null) {
            loadBalancerId = findLBIdByHealtCheckPolicyId(policyId);
        }
        LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            return null;
        }

        _accountMgr.checkAccess(caller, null, true, loadBalancer);
        List<LBHealthCheckPolicyVO> hcDbpolicies = _lb2healthcheckDao.listByLoadBalancerIdAndDisplayFlag(loadBalancerId, forDisplay);

        return hcDbpolicies;
    }

    @Override
    public Pair<List<? extends LoadBalancer>, Integer> searchForLoadBalancers(ListLoadBalancerRulesCmd cmd) {
        Long ipId = cmd.getPublicIpId();
        Long zoneId = cmd.getZoneId();
        Long id = cmd.getId();
        String name = cmd.getLoadBalancerRuleName();
        String keyword = cmd.getKeyword();
        Long instanceId = cmd.getVirtualMachineId();
        Long networkId = cmd.getNetworkId();
        Map<String, String> tags = cmd.getTags();
        Boolean forDisplay = cmd.getDisplay();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(LoadBalancerVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<LoadBalancerVO> sb = _lbDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("sourceIpAddress", sb.entity().getSourceIpAddressId(), SearchCriteria.Op.EQ);
        sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        sb.and("scheme", sb.entity().getScheme(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplay(), SearchCriteria.Op.EQ);

        if (instanceId != null) {
            SearchBuilder<LoadBalancerVMMapVO> lbVMSearch = _lb2VmMapDao.createSearchBuilder();
            lbVMSearch.and("instanceId", lbVMSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("lbVMSearch", lbVMSearch, sb.entity().getId(), lbVMSearch.entity().getLoadBalancerId(), JoinBuilder.JoinType.INNER);
        }

        if (zoneId != null) {
            SearchBuilder<IPAddressVO> ipSearch = _ipAddressDao.createSearchBuilder();
            ipSearch.and("zoneId", ipSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
            sb.join("ipSearch", ipSearch, sb.entity().getSourceIpAddressId(), ipSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

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

        SearchCriteria<LoadBalancerVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<LoadBalancerVO> ssc = _lbDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (ipId != null) {
            sc.setParameters("sourceIpAddress", ipId);
        }

        if (instanceId != null) {
            sc.setJoinParameters("lbVMSearch", "instanceId", instanceId);
        }

        if (zoneId != null) {
            sc.setJoinParameters("ipSearch", "zoneId", zoneId);
        }

        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.LoadBalancer.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (forDisplay != null) {
            sc.setParameters("display", forDisplay);
        }

        //list only Public load balancers using this command
        sc.setParameters("scheme", Scheme.Public);

        Pair<List<LoadBalancerVO>, Integer> result = _lbDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends LoadBalancer>, Integer>(result.first(), result.second());
    }

    @Override
    public LoadBalancerVO findById(long lbId) {
        return _lbDao.findById(lbId);
    }

    @Override
    public LoadBalancerVO findLbByStickinessId(long stickinessPolicyId) {
        LBStickinessPolicyVO stickinessPolicy = _lb2stickinesspoliciesDao.findById(stickinessPolicyId);

        if (stickinessPolicy == null) {
            return null;
        }
        return _lbDao.findById(stickinessPolicy.getLoadBalancerId());
    }

    @Override
    public void removeLBRule(LoadBalancer rule) {
        // remove the rule
        _lbDao.remove(rule.getId());
    }

    public boolean applyLbRules(List<LoadBalancingRule> rules, boolean continueOnError) throws ResourceUnavailableException {
        if (rules == null || rules.size() == 0) {
            s_logger.debug("There are no Load Balancing Rules to forward to the network elements");
            return true;
        }

        boolean success = true;
        Network network = _networkModel.getNetwork(rules.get(0).getNetworkId());
        List<PublicIp> publicIps = new ArrayList<PublicIp>();

        // get the list of public ip's owned by the network
        List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                publicIps.add(publicIp);
            }
        }

        // rules can not programmed unless IP is associated with network
        // service provider, so run IP assoication for
        // the network so as to ensure IP is associated before applying
        // rules (in add state)
        _ipAddrMgr.applyIpAssociations(network, false, continueOnError, publicIps);

        try {
            applyLbRules(network, rules);
        } catch (ResourceUnavailableException e) {
            if (!continueOnError) {
                throw e;
            }
            s_logger.warn("Problems with applying load balancing rules but pushing on", e);
            success = false;
        }

        // if all the rules configured on public IP are revoked then
        // dis-associate IP with network service provider
        _ipAddrMgr.applyIpAssociations(network, true, continueOnError, publicIps);

        return success;
    }

    @Override
    public Map<Ip, UserVm> getLbInstances(long lbId) {
        Map<Ip, UserVm> dstList = new HashMap<Ip, UserVm>();
        List<LoadBalancerVMMapVO> lbVmMaps = _lb2VmMapDao.listByLoadBalancerId(lbId);
        LoadBalancerVO lb = _lbDao.findById(lbId);

        for (LoadBalancerVMMapVO lbVmMap : lbVmMaps) {
            UserVm vm = _vmDao.findById(lbVmMap.getInstanceId());
            Nic nic = _nicDao.findByInstanceIdAndNetworkIdIncludingRemoved(lb.getNetworkId(), vm.getId());
            Ip ip = new Ip(nic.getIPv4Address());
            dstList.put(ip, vm);
        }
        return dstList;
    }

    @Override
    public boolean isLbRuleMappedToVmGuestIp(String vmSecondaryIp) {
        List<LoadBalancerVMMapVO> lbVmMap = _lb2VmMapDao.listByInstanceIp(vmSecondaryIp);
        if (lbVmMap == null || lbVmMap.isEmpty()) {
            return  false;
        }
        return true;
    }

    @Override
    public void isLbServiceSupportedInNetwork(long networkId, Scheme scheme) {
        Network network = _networkDao.findById(networkId);

        //1) Check if the LB service is supported
        if (!_networkModel.areServicesSupportedInNetwork(network.getId(), Service.Lb)) {
            InvalidParameterValueException ex = new InvalidParameterValueException("LB service is not supported in specified network id");
            ex.addProxyObject(network.getUuid(), "networkId");
            throw ex;
        }

        //2) Check if the Scheme is supported\
        NetworkOffering off = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
        if (scheme == Scheme.Public) {
            if (!off.isPublicLb()) {
                throw new InvalidParameterValueException("Scheme " + scheme + " is not supported by the network offering " + off);
            }
        } else {
            if (!off.isInternalLb()) {
                throw new InvalidParameterValueException("Scheme " + scheme + " is not supported by the network offering " + off);
            }
        }

        //3) Check if the provider supports the scheme
        LoadBalancingServiceProvider lbProvider = _networkMgr.getLoadBalancingProviderForNetwork(network, scheme);
        if (lbProvider == null) {
            throw new InvalidParameterValueException("Lb rule with scheme " + scheme.toString() + " is not supported by lb providers in network " + network);
        }
    }

    public List<LoadBalancingServiceProvider> getLbProviders() {
        return _lbProviders;
    }

    @Inject
    public void setLbProviders(List<LoadBalancingServiceProvider> lbProviders) {
        this._lbProviders = lbProviders;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_STICKINESSPOLICY_UPDATE, eventDescription = "updating lb stickiness policy", async = true)
    public StickinessPolicy updateLBStickinessPolicy(long id, String customId, Boolean forDisplay) {
        LBStickinessPolicyVO policy = _lb2stickinesspoliciesDao.findById(id);
        if (policy == null) {
            throw new InvalidParameterValueException("Fail to find stickiness policy with " + id);
        }

        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(policy.getLoadBalancerId()));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer : " + policy.getLoadBalancerId() + " for Stickiness policy id: " + id);
        }

        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, loadBalancer);

        if (customId != null) {
            policy.setUuid(customId);
        }

        if (forDisplay != null) {
            policy.setDisplay(forDisplay);
        }

        _lb2stickinesspoliciesDao.update(id, policy);
        return _lb2stickinesspoliciesDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_HEALTHCHECKPOLICY_UPDATE, eventDescription = "updating lb healthcheck policy", async = true)
    public HealthCheckPolicy updateLBHealthCheckPolicy(long id, String customId, Boolean forDisplay) {
        LBHealthCheckPolicyVO policy = _lb2healthcheckDao.findById(id);
        if (policy == null) {
            throw new InvalidParameterValueException("Fail to find stickiness policy with " + id);
        }

        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(policy.getLoadBalancerId()));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer : " + policy.getLoadBalancerId() + " for Stickiness policy id: " + id);
        }

        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, loadBalancer);

        if (customId != null) {
            policy.setUuid(customId);
        }

        if (forDisplay != null) {
            policy.setDisplay(forDisplay);
        }

        _lb2healthcheckDao.update(id, policy);
        return _lb2healthcheckDao.findById(id);
    }

    @Override
    public Long findLBIdByHealtCheckPolicyId(long lbHealthCheckPolicy) {
        LBHealthCheckPolicyVO policy= _lb2healthcheckDao.findById(lbHealthCheckPolicy);
        if(policy != null) {
            return policy.getLoadBalancerId();
        }
        return null;
    }

}
