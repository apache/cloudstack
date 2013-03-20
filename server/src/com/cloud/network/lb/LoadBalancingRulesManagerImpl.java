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

import javax.ejb.Local;
import javax.inject.Inject;

import com.cloud.event.UsageEventUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBHealthCheckPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBStickinessPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLBHealthCheckPoliciesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLBStickinessPoliciesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRuleInstancesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRulesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.response.ServiceResponse;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.ExternalLoadBalancerUsageManager;
import com.cloud.network.IpAddress;
import com.cloud.network.LBHealthCheckPolicyVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkRuleApplier;
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
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScalePolicy;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScaleVmGroup;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScaleVmProfile;
import com.cloud.network.lb.LoadBalancingRule.LbCondition;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
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
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.network.vpc.VpcManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Component
@Local(value = { LoadBalancingRulesManager.class, LoadBalancingRulesService.class })
public class LoadBalancingRulesManagerImpl<Type> extends ManagerBase implements LoadBalancingRulesManager,
        LoadBalancingRulesService, NetworkRuleApplier {
    private static final Logger s_logger = Logger.getLogger(LoadBalancingRulesManagerImpl.class);

    @Inject
    NetworkManager _networkMgr;
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
    FirewallRulesDao _firewallDao;
    @Inject
    DomainService _domainMgr;
    @Inject
    ConfigurationManager _configMgr;

    @Inject
    ExternalLoadBalancerUsageManager _externalLBUsageMgr;
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
    ConfigurationDao _configDao;
    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    UserDao _userDao;
    @Inject
    List<LoadBalancingServiceProvider> _lbProviders;

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

    private LbAutoScaleVmGroup getLbAutoScaleVmGroup(AutoScaleVmGroupVO vmGroup, String currentState, LoadBalancerVO lb) {
        long lbNetworkId = lb.getNetworkId();
        String lbName = lb.getName();
        List<AutoScaleVmGroupPolicyMapVO> vmGroupPolicyMapList = _autoScaleVmGroupPolicyMapDao.listByVmGroupId(vmGroup
                .getId());
        List<LbAutoScalePolicy> autoScalePolicies = new ArrayList<LbAutoScalePolicy>();
        for (AutoScaleVmGroupPolicyMapVO vmGroupPolicyMap : vmGroupPolicyMapList) {
            AutoScalePolicy autoScalePolicy = _autoScalePolicyDao.findById(vmGroupPolicyMap.getPolicyId());
            List<AutoScalePolicyConditionMapVO> autoScalePolicyConditionMapList = _autoScalePolicyConditionMapDao
                    .listByAll(autoScalePolicy.getId(), null);
            List<LbCondition> lbConditions = new ArrayList<LbCondition>();
            for (AutoScalePolicyConditionMapVO autoScalePolicyConditionMap : autoScalePolicyConditionMapList) {
                Condition condition = _conditionDao.findById(autoScalePolicyConditionMap.getConditionId());
                Counter counter = _counterDao.findById(condition.getCounterid());
                lbConditions.add(new LbCondition(counter, condition));
            }
            autoScalePolicies.add(new LbAutoScalePolicy(autoScalePolicy, lbConditions));
        }
        AutoScaleVmProfile autoScaleVmProfile = _autoScaleVmProfileDao.findById(vmGroup.getProfileId());
        Long autoscaleUserId = autoScaleVmProfile.getAutoScaleUserId();
        User user = _userDao.findByIdIncludingRemoved(autoscaleUserId);
        String apiKey = user.getApiKey();
        String secretKey = user.getSecretKey();
        String csUrl = _configDao.getValue(Config.EndpointeUrl.key());
        String zoneId = _dcDao.findById(autoScaleVmProfile.getZoneId()).getUuid();
        String domainId = _domainDao.findById(autoScaleVmProfile.getDomainId()).getUuid();
        String serviceOfferingId = _offeringsDao.findById(autoScaleVmProfile.getServiceOfferingId()).getUuid();
        String templateId = _templateDao.findById(autoScaleVmProfile.getTemplateId()).getUuid();
        String vmName = "AutoScale-LB-" + lbName;
        String lbNetworkUuid = null;

        DataCenter zone = _configMgr.getZone(vmGroup.getZoneId());
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

        if (apiKey == null) {
            throw new InvalidParameterValueException("apiKey for user: " + user.getUsername()
                    + " is empty. Please generate it");
        }

        if (secretKey == null) {
            throw new InvalidParameterValueException("secretKey for user: " + user.getUsername()
                    + " is empty. Please generate it");
        }

        if (csUrl == null || csUrl.contains("localhost")) {
            throw new InvalidParameterValueException(
                    "Global setting endpointe.url has to be set to the Management Server's API end point");
        }

        LbAutoScaleVmProfile lbAutoScaleVmProfile = new LbAutoScaleVmProfile(autoScaleVmProfile, apiKey, secretKey,
                csUrl, zoneId, domainId, serviceOfferingId, templateId, vmName, lbNetworkUuid);
        return new LbAutoScaleVmGroup(vmGroup, autoScalePolicies, lbAutoScaleVmProfile, currentState);
    }

    private boolean applyAutoScaleConfig(LoadBalancerVO lb, AutoScaleVmGroupVO vmGroup, String currentState)
            throws ResourceUnavailableException {
        LbAutoScaleVmGroup lbAutoScaleVmGroup = getLbAutoScaleVmGroup(vmGroup, currentState, lb);
        /*
         * Regular config like destinations need not be packed for applying
         * autoscale config as of today.
         */
        LoadBalancingRule rule = new LoadBalancingRule(lb, null, null, null);
        rule.setAutoScaleVmGroup(lbAutoScaleVmGroup);

        if (!isRollBackAllowedForProvider(lb)) {
            // this is for Netscaler type of devices. if their is failure the db
            // entries will be rollbacked.
            return false;
        }

        List<LoadBalancingRule> rules = Arrays.asList(rule);

        if (!_networkMgr.applyRules(rules, FirewallRule.Purpose.LoadBalancing, this, false)) {
            s_logger.debug("LB rules' autoscale config are not completely applied");
            return false;
        }

        return true;
    }

    @Override
    @DB
    public boolean configureLbAutoScaleVmGroup(long vmGroupid, String currentState) throws ResourceUnavailableException {
        AutoScaleVmGroupVO vmGroup = _autoScaleVmGroupDao.findById(vmGroupid);
        boolean success = false;

        LoadBalancerVO loadBalancer = _lbDao.findById(vmGroup.getLoadBalancerId());

        FirewallRule.State backupState = loadBalancer.getState();

        if (vmGroup.getState().equals(AutoScaleVmGroup.State_New)) {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
        } else if (loadBalancer.getState() == FirewallRule.State.Active
                && vmGroup.getState().equals(AutoScaleVmGroup.State_Revoke)) {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
        }

        try {
            success = applyAutoScaleConfig(loadBalancer, vmGroup, currentState);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to configure AutoScaleVmGroup to the lb rule: " + loadBalancer.getId()
                    + " because resource is unavaliable:", e);
            if (isRollBackAllowedForProvider(loadBalancer)) {
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("LB Rollback rule id: " + loadBalancer.getId()
                        + " lb state rolback while creating AutoscaleVmGroup");
            }
            throw e;
        } finally {
            if (!success) {
                s_logger.warn("Failed to configure LB Auto Scale Vm Group with Id:" + vmGroupid);
            }
        }

        if (success) {
            if (vmGroup.getState().equals(AutoScaleVmGroup.State_New)) {
                Transaction.currentTxn().start();
                loadBalancer.setState(FirewallRule.State.Active);
                s_logger.debug("LB rule " + loadBalancer.getId() + " state is set to Active");
                _lbDao.persist(loadBalancer);
                vmGroup.setState(AutoScaleVmGroup.State_Enabled);
                _autoScaleVmGroupDao.persist(vmGroup);
                s_logger.debug("LB Auto Scale Vm Group with Id: " + vmGroupid + " is set to Enabled state.");
                Transaction.currentTxn().commit();
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
            throw new InvalidParameterValueException("Failed:  No Stickiness method available for LB rule:"
                    + cmd.getLbRuleId());
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
                        HashMap<String, String> paramKVpair = (HashMap) iter.next();
                        String paramName = paramKVpair.get("name");
                        String paramValue = paramKVpair.get("value");

                        tempParamList.put(paramName, paramValue);
                        Boolean found = false;
                        for (LbStickinessMethodParam param : methodParamList) {
                            if (param.getParamName().equalsIgnoreCase(paramName)) {
                                if ((param.getIsflag() == false) && (paramValue == null)) {
                                    throw new InvalidParameterValueException("Failed : Value expected for the Param :"
                                            + param.getParamName());
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            throw new InvalidParameterValueException(
                                    "Failed : Stickiness policy does not support param name :" + paramName);
                        }
                    }
                }

                /* validation-2: check for mandatory params */
                for (LbStickinessMethodParam param : methodParamList) {
                    if (param.getRequired()) {
                        if (tempParamList.get(param.getParamName()) == null) {
                            throw new InvalidParameterValueException("Failed : Missing Manadatory Param :"
                                    + param.getParamName());
                        }
                    }
                }
                /* Successfully completed the Validation */
                break;
            }
        }
        if (methodMatch == false) {
            throw new InvalidParameterValueException("Failed to match Stickiness method name for LB rule:"
                    + cmd.getLbRuleId());
        }

        /* Validation : check for the multiple policies to the rule id */
        List<LBStickinessPolicyVO> stickinessPolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(
                cmd.getLbRuleId(), false);
        if (stickinessPolicies.size() > 0) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Already policy attached "
                    + cmd.getLbRuleId());
        }
        return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_STICKINESSPOLICY_CREATE, eventDescription = "create lb stickinesspolicy to load balancer", create = true)
    public StickinessPolicy createLBStickinessPolicy(CreateLBStickinessPolicyCmd cmd)
            throws NetworkRuleConflictException {
        UserContext caller = UserContext.current();

        /* Validation : check corresponding load balancer rule exist */
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed: LB rule id: " + cmd.getLbRuleId() + " not present ");
        }

        _accountMgr.checkAccess(caller.getCaller(), null, true, loadBalancer);
        if (loadBalancer.getState() == FirewallRule.State.Revoke) {
            throw new InvalidParameterValueException("Failed:  LB rule id: " + cmd.getLbRuleId()
                    + " is in deleting state: ");
        }

        /* Generic validations */
        if (!genericValidator(cmd)) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Validation Failed "
                    + cmd.getLbRuleId());
        }

        /*
         * Specific validations using network element validator for specific
         * validations
         */
        LBStickinessPolicyVO lbpolicy = new LBStickinessPolicyVO(loadBalancer.getId(), cmd.getLBStickinessPolicyName(),
                cmd.getStickinessMethodName(), cmd.getparamList(), cmd.getDescription());
        List<LbStickinessPolicy> policyList = new ArrayList<LbStickinessPolicy>();
        policyList.add(new LbStickinessPolicy(cmd.getStickinessMethodName(), lbpolicy.getParams()));
        LoadBalancingRule lbRule = new LoadBalancingRule(loadBalancer, getExistingDestinations(lbpolicy.getId()),
                policyList, null);
        if (!validateRule(lbRule)) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Validation Failed "
                    + cmd.getLbRuleId());
        }

        /* Finally Insert into DB */
        LBStickinessPolicyVO policy = new LBStickinessPolicyVO(loadBalancer.getId(), cmd.getLBStickinessPolicyName(),
                cmd.getStickinessMethodName(), cmd.getparamList(), cmd.getDescription());
        policy = _lb2stickinesspoliciesDao.persist(policy);

        return policy;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_HEALTHCHECKPOLICY_CREATE, eventDescription = "create load balancer health check to load balancer", create = true)
    public HealthCheckPolicy createLBHealthCheckPolicy(CreateLBHealthCheckPolicyCmd cmd) {
        UserContext caller = UserContext.current();

        /*
         * Validation of cmd Monitor interval must be greater than response
         * timeout
         */
        Map<String, String> paramMap = cmd.getFullUrlParams();

        if (paramMap.containsKey(ApiConstants.HEALTHCHECK_RESPONSE_TIMEOUT)
                && paramMap.containsKey(ApiConstants.HEALTHCHECK_INTERVAL_TIME)) {
            if (cmd.getResponsTimeOut() > cmd.getHealthCheckInterval())
                throw new InvalidParameterValueException(
                        "Failed to create HealthCheck policy : Monitor interval must be greater than response timeout");
        }
        /* Validation : check corresponding load balancer rule exist */
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed: LB rule id: " + cmd.getLbRuleId() + " not present ");
        }

        _accountMgr.checkAccess(caller.getCaller(), null, true, loadBalancer);

        if (loadBalancer.getState() == FirewallRule.State.Revoke) {
            throw new InvalidParameterValueException("Failed:  LB rule id: " + cmd.getLbRuleId()
                    + " is in deleting state: ");
        }

        /*
         * Validate Whether LB Provider has the capabilities to support Health
         * Checks
         */
        if (!validateHealthCheck(cmd)) {
            throw new InvalidParameterValueException(
                    "Failed to create HealthCheck policy: Validation Failed (HealthCheck Policy is not supported by LB Provider for the LB rule id :)"
                            + cmd.getLbRuleId());
        }

        /* Validation : check for the multiple hc policies to the rule id */
        List<LBHealthCheckPolicyVO> hcPolicies = _lb2healthcheckDao.listByLoadBalancerId(cmd.getLbRuleId(), false);
        if (hcPolicies.size() > 0) {
            throw new InvalidParameterValueException(
                    "Failed to create HealthCheck policy: Already policy attached  for the LB Rule id :"
                            + cmd.getLbRuleId());
        }
        /*
         * Specific validations using network element validator for specific
         * validations
         */
        LBHealthCheckPolicyVO hcpolicy = new LBHealthCheckPolicyVO(loadBalancer.getId(), cmd.getPingPath(),
                cmd.getDescription(), cmd.getResponsTimeOut(), cmd.getHealthCheckInterval(), cmd.getHealthyThreshold(),
                cmd.getUnhealthyThreshold());

        List<LbHealthCheckPolicy> hcPolicyList = new ArrayList<LbHealthCheckPolicy>();
        hcPolicyList.add(new LbHealthCheckPolicy(hcpolicy.getpingpath(), hcpolicy.getDescription(), hcpolicy
                .getResponseTime(), hcpolicy.getHealthcheckInterval(), hcpolicy.getHealthcheckThresshold(), hcpolicy
                .getUnhealthThresshold()));

        // Finally Insert into DB
        LBHealthCheckPolicyVO policy = new LBHealthCheckPolicyVO(loadBalancer.getId(), cmd.getPingPath(),
                cmd.getDescription(), cmd.getResponsTimeOut(), cmd.getHealthCheckInterval(), cmd.getHealthyThreshold(),
                cmd.getUnhealthyThreshold());

        policy = _lb2healthcheckDao.persist(policy);
        return policy;
    }

    private boolean validateRule(LoadBalancingRule lbRule) {
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

        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer Id:" + cmd.getLbRuleId());
        }
        FirewallRule.State backupState = loadBalancer.getState();
        loadBalancer.setState(FirewallRule.State.Add);
        _lbDao.persist(loadBalancer);
        try {
            applyLoadBalancerConfig(cmd.getLbRuleId());
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply Stickiness policy to the lb rule: " + cmd.getLbRuleId()
                    + " because resource is unavaliable:", e);
            if (isRollBackAllowedForProvider(loadBalancer)) {
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("LB Rollback rule id: " + loadBalancer.getId()
                        + " lb state rolback while creating sticky policy");
            }
            deleteLBStickinessPolicy(cmd.getEntityId(), false);
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
        FirewallRule.State backupState = loadBalancer.getState();
        loadBalancer.setState(FirewallRule.State.Add);
        _lbDao.persist(loadBalancer);
        try {
            applyLoadBalancerConfig(cmd.getLbRuleId());
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply healthcheck policy to the lb rule: " + cmd.getLbRuleId()
                    + " because resource is unavaliable:", e);
            if (isRollBackAllowedForProvider(loadBalancer)) {
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("LB Rollback rule id: " + loadBalancer.getId()
                        + " lb state rolback while creating healthcheck policy");
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

        UserContext caller = UserContext.current();
        LBStickinessPolicyVO stickinessPolicy = _lb2stickinesspoliciesDao.findById(stickinessPolicyId);

        if (stickinessPolicy == null) {
            throw new InvalidParameterException("Invalid Stickiness policy id value: " + stickinessPolicyId);
        }
        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(stickinessPolicy.getLoadBalancerId()));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer : " + stickinessPolicy.getLoadBalancerId()
                    + " for Stickiness policy id: " + stickinessPolicyId);
        }
        long loadBalancerId = loadBalancer.getId();
        FirewallRule.State backupState = loadBalancer.getState();
        _accountMgr.checkAccess(caller.getCaller(), null, true, loadBalancer);

        if (apply) {
            if (loadBalancer.getState() == FirewallRule.State.Active) {
                loadBalancer.setState(FirewallRule.State.Add);
                _lbDao.persist(loadBalancer);
            }

            boolean backupStickyState = stickinessPolicy.isRevoke();
            stickinessPolicy.setRevoke(true);
            _lb2stickinesspoliciesDao.persist(stickinessPolicy);
            s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", stickinesspolicyID "
                    + stickinessPolicyId);

            try {
                if (!applyLoadBalancerConfig(loadBalancerId)) {
                    s_logger.warn("Failed to remove load balancer rule id " + loadBalancerId
                            + " for stickinesspolicyID " + stickinessPolicyId);
                    throw new CloudRuntimeException("Failed to remove load balancer rule id " + loadBalancerId
                            + " for stickinesspolicyID " + stickinessPolicyId);
                }
            } catch (ResourceUnavailableException e) {
                if (isRollBackAllowedForProvider(loadBalancer)) {
                    stickinessPolicy.setRevoke(backupStickyState);
                    _lb2stickinesspoliciesDao.persist(stickinessPolicy);
                    loadBalancer.setState(backupState);
                    _lbDao.persist(loadBalancer);
                    s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + "  while deleting sticky policy: "
                            + stickinessPolicyId);
                }
                s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
                success = false;
            }
        } else {
            _lb2stickinesspoliciesDao.remove(stickinessPolicy.getLoadBalancerId());
        }
        return success;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_HEALTHCHECKPOLICY_DELETE, eventDescription = "revoking LB HealthCheck policy ", async = true)
    public boolean deleteLBHealthCheckPolicy(long healthCheckPolicyId, boolean apply) {
        boolean success = true;

        UserContext caller = UserContext.current();
        LBHealthCheckPolicyVO healthCheckPolicy = _lb2healthcheckDao.findById(healthCheckPolicyId);

        if (healthCheckPolicy == null) {
            throw new InvalidParameterException("Invalid HealthCheck policy id value: " + healthCheckPolicyId);
        }
        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(healthCheckPolicy.getLoadBalancerId()));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer : " + healthCheckPolicy.getLoadBalancerId()
                    + " for HealthCheck policy id: " + healthCheckPolicyId);
        }
        long loadBalancerId = loadBalancer.getId();
        FirewallRule.State backupState = loadBalancer.getState();
        _accountMgr.checkAccess(caller.getCaller(), null, true, loadBalancer);

        if (apply) {
            if (loadBalancer.getState() == FirewallRule.State.Active) {
                loadBalancer.setState(FirewallRule.State.Add);
                _lbDao.persist(loadBalancer);
            }

            boolean backupStickyState = healthCheckPolicy.isRevoke();
            healthCheckPolicy.setRevoke(true);
            _lb2healthcheckDao.persist(healthCheckPolicy);
            s_logger.debug("Set health check policy to revoke for loadbalancing rule id : " + loadBalancerId
                    + ", healthCheckpolicyID " + healthCheckPolicyId);

            // removing the state of services set by the monitor.
            List<LoadBalancerVMMapVO> maps = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId);
            if (maps != null) {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                s_logger.debug("Resetting health state policy for services in loadbalancing rule id : "
                        + loadBalancerId);
                for (LoadBalancerVMMapVO map : maps) {
                    map.setState(null);
                    _lb2VmMapDao.persist(map);
                }
                txn.commit();
            }

            try {
                if (!applyLoadBalancerConfig(loadBalancerId)) {
                    s_logger.warn("Failed to remove load balancer rule id " + loadBalancerId
                            + " for healthCheckpolicyID " + healthCheckPolicyId);
                    throw new CloudRuntimeException("Failed to remove load balancer rule id " + loadBalancerId
                            + " for healthCheckpolicyID " + healthCheckPolicyId);
                }
            } catch (ResourceUnavailableException e) {
                if (isRollBackAllowedForProvider(loadBalancer)) {
                    healthCheckPolicy.setRevoke(backupStickyState);
                    _lb2healthcheckDao.persist(healthCheckPolicy);
                    loadBalancer.setState(backupState);
                    _lbDao.persist(loadBalancer);
                    s_logger.debug("LB Rollback rule id: " + loadBalancer.getId()
                            + "  while deleting healthcheck policy: " + healthCheckPolicyId);
                }
                s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
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
    public void updateLBHealthChecks() throws ResourceUnavailableException {
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
                rules = _lbDao.listByNetworkId(network.getId());
                if (rules != null && rules.size() > 0) {
                    List<LoadBalancingRule> lbrules = new ArrayList<LoadBalancingRule>();
                    for (LoadBalancerVO lb : rules) {
                        List<LbDestination> dstList = getExistingDestinations(lb.getId());
                        List<LbHealthCheckPolicy> hcPolicyList = getHealthCheckPolicies(lb.getId());
                        // adding to lbrules list only if the LB rule
                        // hashealtChecks
                        if (hcPolicyList != null && hcPolicyList.size() > 0) {
                            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, null, hcPolicyList);
                            lbrules.add(loadBalancing);
                        }
                    }
                    if (lbrules.size() > 0) {
                        isHandled = false;
                        for (LoadBalancingServiceProvider lbElement : _lbProviders) {
                            stateRules = lbElement.updateHealthChecks(network, (List<LoadBalancingRule>) lbrules);
                            if (stateRules != null && stateRules.size() > 0) {
                                for (LoadBalancerTO lbto : stateRules) {
                                    LoadBalancerVO ulb = _lbDao.findByUuid(lbto.getUuid());
                                    List<LoadBalancerVMMapVO> lbVmMaps = _lb2VmMapDao.listByLoadBalancerId(ulb.getId());
                                    for (LoadBalancerVMMapVO lbVmMap : lbVmMaps) {
                                        UserVm vm = _vmDao.findById(lbVmMap.getInstanceId());
                                        Nic nic = _nicDao.findByInstanceIdAndNetworkIdIncludingRemoved(
                                                ulb.getNetworkId(), vm.getId());
                                        String dstIp = nic.getIp4Address();
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
        if (provider.get(0) == Provider.Netscaler || provider.get(0) == Provider.F5BigIp) {
            return true;
        }
        return false;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ASSIGN_TO_LOAD_BALANCER_RULE, eventDescription = "assigning to load balancer", async = true)
    public boolean assignToLoadBalancer(long loadBalancerId, List<Long> instanceIds) {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId
                    + ", the load balancer was not found.");
        }

        List<LoadBalancerVMMapVO> mappedInstances = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId, false);
        Set<Long> mappedInstanceIds = new HashSet<Long>();
        for (LoadBalancerVMMapVO mappedInstance : mappedInstances) {
            mappedInstanceIds.add(Long.valueOf(mappedInstance.getInstanceId()));
        }

        List<UserVm> vmsToAdd = new ArrayList<UserVm>();

        for (Long instanceId : instanceIds) {
            if (mappedInstanceIds.contains(instanceId)) {
                throw new InvalidParameterValueException("VM " + instanceId + " is already mapped to load balancer.");
            }

            UserVm vm = _vmDao.findById(instanceId);
            if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Invalid instance id specified");
                ex.addProxyObject(vm, instanceId, "instanceId");
                throw ex;
            }

            _rulesMgr.checkRuleAndUserVm(loadBalancer, vm, caller);

            if (vm.getAccountId() != loadBalancer.getAccountId()) {
                throw new PermissionDeniedException("Cannot add virtual machines that do not belong to the same owner.");
            }

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
                InvalidParameterValueException ex = new InvalidParameterValueException("VM " + instanceId
                        + " cannot be added because it doesn't belong in the same network.");
                ex.addProxyObject(vm, instanceId, "instanceId");
                throw ex;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Adding " + vm + " to the load balancer pool");
            }
            vmsToAdd.add(vm);
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (UserVm vm : vmsToAdd) {
            LoadBalancerVMMapVO map = new LoadBalancerVMMapVO(loadBalancer.getId(), vm.getId(), false);
            map = _lb2VmMapDao.persist(map);
        }
        txn.commit();
        if (_autoScaleVmGroupDao.isAutoScaleLoadBalancer(loadBalancerId)) {
            // For autoscaled loadbalancer, the rules need not be applied,
            // meaning the call need not reach the resource layer.
            // We can consider the job done.
            return true;
        }
        boolean success = false;
        FirewallRule.State backupState = loadBalancer.getState();
        try {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
            applyLoadBalancerConfig(loadBalancerId);
            success = true;
        } catch (ResourceUnavailableException e) {
            if (isRollBackAllowedForProvider(loadBalancer)) {
                List<Long> vmInstanceIds = new ArrayList<Long>();
                txn = Transaction.currentTxn();
                txn.start();
                for (UserVm vm : vmsToAdd) {
                    vmInstanceIds.add(vm.getId());
                }
                txn.commit();
                if (!vmInstanceIds.isEmpty()) {
                    _lb2VmMapDao.remove(loadBalancer.getId(), vmInstanceIds, null);
                    s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + "  while attaching VM: "
                            + vmInstanceIds);
                }
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
            }
            s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
        }

        if (!success) {
            CloudRuntimeException ex = new CloudRuntimeException("Failed to add specified loadbalancerruleid for vms "
                    + instanceIds);
            ex.addProxyObject(loadBalancer, loadBalancerId, "loadBalancerId");
            // TBD: Also pack in the instanceIds in the exception using the
            // right VO object or table name.
            throw ex;
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_REMOVE_FROM_LOAD_BALANCER_RULE, eventDescription = "removing from load balancer", async = true)
    public boolean removeFromLoadBalancer(long loadBalancerId, List<Long> instanceIds) {
        return removeFromLoadBalancerInternal(loadBalancerId, instanceIds, true);
    }

    private boolean removeFromLoadBalancerInternal(long loadBalancerId, List<Long> instanceIds, boolean rollBack) {
        UserContext caller = UserContext.current();

        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(loadBalancerId));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid load balancer value: " + loadBalancerId);
        }

        _accountMgr.checkAccess(caller.getCaller(), null, true, loadBalancer);

        boolean success = false;
        FirewallRule.State backupState = loadBalancer.getState();
        try {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);

            for (long instanceId : instanceIds) {
                LoadBalancerVMMapVO map = _lb2VmMapDao.findByLoadBalancerIdAndVmId(loadBalancerId, instanceId);
                map.setRevoke(true);
                _lb2VmMapDao.persist(map);
                s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", vmId " + instanceId);
            }

            if (_autoScaleVmGroupDao.isAutoScaleLoadBalancer(loadBalancerId)) {
                // For autoscaled loadbalancer, the rules need not be applied,
                // meaning the call need not reach the resource layer.
                // We can consider the job done and only need to remove the
                // rules in DB
                _lb2VmMapDao.remove(loadBalancer.getId(), instanceIds, null);
                return true;
            }

            if (!applyLoadBalancerConfig(loadBalancerId)) {
                s_logger.warn("Failed to remove load balancer rule id " + loadBalancerId + " for vms " + instanceIds);
                CloudRuntimeException ex = new CloudRuntimeException(
                        "Failed to remove specified load balancer rule id for vms " + instanceIds);
                ex.addProxyObject(loadBalancer, loadBalancerId, "loadBalancerId");
                throw ex;
            }
            success = true;
        } catch (ResourceUnavailableException e) {
            if (rollBack && isRollBackAllowedForProvider(loadBalancer)) {

                for (long instanceId : instanceIds) {
                    LoadBalancerVMMapVO map = _lb2VmMapDao.findByLoadBalancerIdAndVmId(loadBalancerId, instanceId);
                    map.setRevoke(false);
                    _lb2VmMapDao.persist(map);
                    s_logger.debug("LB Rollback rule id: " + loadBalancerId + ",while removing vmId " + instanceId);
                }

                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("LB Rollback rule id: " + loadBalancerId + " while removing vm instances");
            }
            s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
        }
        if (!success) {
            CloudRuntimeException ex = new CloudRuntimeException(
                    "Failed to remove specified load balancer rule id for vms " + instanceIds);
            ex.addProxyObject(loadBalancer, loadBalancerId, "loadBalancerId");
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
            s_logger.debug("Set load balancer rule for revoke: rule id " + map.getLoadBalancerId() + ", vmId "
                    + instanceId);
        }

        // Reapply all lbs that had the vm assigned
        if (lbsToReconfigure != null) {
            for (Map.Entry<Long, List<Long>> lb : lbsToReconfigure.entrySet()) {
                if (!removeFromLoadBalancerInternal(lb.getKey(), lb.getValue(), false)) {
                    success = false;
                }
            }
        }
        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_DELETE, eventDescription = "deleting load balancer", async = true)
    public boolean deleteLoadBalancerRule(long loadBalancerId, boolean apply) {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        LoadBalancerVO rule = _lbDao.findById(loadBalancerId);

        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find load balancer rule " + loadBalancerId);
        }
        _accountMgr.checkAccess(caller, null, true, rule);

        boolean result = deleteLoadBalancerRule(loadBalancerId, apply, caller, ctx.getCallerUserId(), true);
        if (!result) {
            throw new CloudRuntimeException("Unable to remove load balancer rule " + loadBalancerId);
        }
        return result;
    }

    @DB
    public boolean deleteLoadBalancerRule(long loadBalancerId, boolean apply, Account caller, long callerUserId,
            boolean rollBack) {
        LoadBalancerVO lb = _lbDao.findById(loadBalancerId);
        Transaction txn = Transaction.currentTxn();
        boolean generateUsageEvent = false;
        boolean success = true;
        FirewallRule.State backupState = lb.getState();

        txn.start();
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
                s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", vmId "
                        + map.getInstanceId());
            }
        }

        List<LBHealthCheckPolicyVO> hcPolicies = _lb2healthcheckDao.listByLoadBalancerId(loadBalancerId);
        for (LBHealthCheckPolicyVO lbHealthCheck : hcPolicies) {
            lbHealthCheck.setRevoke(true);
            _lb2healthcheckDao.persist(lbHealthCheck);
        }

        if (generateUsageEvent) {
            // Generate usage event right after all rules were marked for revoke
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_LOAD_BALANCER_DELETE, lb.getAccountId(), 0, lb.getId(),
                    null, LoadBalancingRule.class.getName(), lb.getUuid());
        }

        txn.commit();

        // gather external network usage stats for this lb rule
        NetworkVO network = _networkDao.findById(lb.getNetworkId());
        if (network != null) {
            if (_networkModel.networkIsConfiguredForExternalNetworking(network.getDataCenterId(), network.getId())) {
                _externalLBUsageMgr.updateExternalLoadBalancerNetworkUsageStats(loadBalancerId);
            }
        }

        if (apply) {
            try {
                if (_autoScaleVmGroupDao.isAutoScaleLoadBalancer(loadBalancerId)) {
                    // Get the associated VmGroup
                    AutoScaleVmGroupVO vmGroup = _autoScaleVmGroupDao.listByAll(loadBalancerId, null).get(0);
                    if (!applyAutoScaleConfig(lb, vmGroup, vmGroup.getState())) {
                        s_logger.warn("Unable to apply the autoscale config");
                        return false;
                    }
                } else {
                    if (!applyLoadBalancerConfig(loadBalancerId)) {
                        s_logger.warn("Unable to apply the load balancer config");
                        return false;
                    }
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
                    s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
                }
                return false;
            }
        }

        FirewallRuleVO relatedRule = _firewallDao.findByRelatedId(lb.getId());
        if (relatedRule != null) {
            s_logger.warn("Unable to remove firewall rule id=" + lb.getId() + " as it has related firewall rule id="
                    + relatedRule.getId() + "; leaving it in Revoke state");
            success = false;
        } else {
            _firewallMgr.removeRule(lb);
        }

        // FIXME: breaking the dependency on ELB manager. This breaks
        // functionality of ELB using virtual router
        // Bug CS-15411 opened to document this
        // _elbMgr.handleDeleteLoadBalancerRule(lb, callerUserId, caller);

        if (success) {
            s_logger.debug("Load balancer with id " + lb.getId() + " is removed successfully");
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_CREATE, eventDescription = "creating load balancer")
    public LoadBalancer createLoadBalancerRule(CreateLoadBalancerRuleCmd lb, boolean openFirewall)
            throws NetworkRuleConflictException, InsufficientAddressCapacityException {
        Account lbOwner = _accountMgr.getAccount(lb.getEntityOwnerId());

        int defPortStart = lb.getDefaultPortStart();
        int defPortEnd = lb.getDefaultPortEnd();

        if (!NetUtils.isValidPort(defPortEnd)) {
            throw new InvalidParameterValueException("privatePort is an invalid value: " + defPortEnd);
        }
        if (defPortStart > defPortEnd) {
            throw new InvalidParameterValueException("private port range is invalid: " + defPortStart + "-"
                    + defPortEnd);
        }
        if ((lb.getAlgorithm() == null) || !NetUtils.isValidAlgorithm(lb.getAlgorithm())) {
            throw new InvalidParameterValueException("Invalid algorithm: " + lb.getAlgorithm());
        }

        Long ipAddrId = lb.getSourceIpAddressId();
        IPAddressVO ipVO = null;
        if (ipAddrId != null) {
            ipVO = _ipAddressDao.findById(ipAddrId);
        }

        Network network = _networkModel.getNetwork(lb.getNetworkId());

        // FIXME: breaking the dependency on ELB manager. This breaks
        // functionality of ELB using virtual router
        // Bug CS-15411 opened to document this
        // LoadBalancer result = _elbMgr.handleCreateLoadBalancerRule(lb,
        // lbOwner, lb.getNetworkId());
        LoadBalancer result = null;
        if (result == null) {
            IpAddress systemIp = null;
            NetworkOffering off = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
            if (off.getElasticLb() && ipVO == null && network.getVpcId() == null) {
                systemIp = _networkMgr.assignSystemIp(lb.getNetworkId(), lbOwner, true, false);
                lb.setSourceIpAddressId(systemIp.getId());
                ipVO = _ipAddressDao.findById(systemIp.getId());
            }

            // Validate ip address
            if (ipVO == null) {
                throw new InvalidParameterValueException(
                        "Unable to create load balance rule; can't find/allocate source IP");
            } else if (ipVO.isOneToOneNat()) {
                throw new NetworkRuleConflictException("Can't do load balance on ip address: " + ipVO.getAddress());
            }

            boolean performedIpAssoc = false;
            try {
                if (ipVO.getAssociatedWithNetworkId() == null) {
                    boolean assignToVpcNtwk = network.getVpcId() != null && ipVO.getVpcId() != null
                            && ipVO.getVpcId().longValue() == network.getVpcId();
                    if (assignToVpcNtwk) {
                        // set networkId just for verification purposes
                        _networkModel.checkIpForService(ipVO, Service.Lb, lb.getNetworkId());

                        s_logger.debug("The ip is not associated with the VPC network id=" + lb.getNetworkId()
                                + " so assigning");
                        ipVO = _networkMgr.associateIPToGuestNetwork(ipAddrId, lb.getNetworkId(), false);
                        performedIpAssoc = true;
                    }
                } else {
                    _networkModel.checkIpForService(ipVO, Service.Lb, null);
                }

                if (ipVO.getAssociatedWithNetworkId() == null) {
                    throw new InvalidParameterValueException("Ip address " + ipVO + " is not assigned to the network "
                            + network);
                }

                if (lb.getSourceIpAddressId() == null) {
                    throw new CloudRuntimeException("No ip address is defined to assign the LB to");
                }
                result = createLoadBalancer(lb, openFirewall);
            } catch (Exception ex) {
                s_logger.warn("Failed to create load balancer due to ", ex);
                if (ex instanceof NetworkRuleConflictException) {
                    throw (NetworkRuleConflictException) ex;
                }
            } finally {
                if (result == null && systemIp != null) {
                    s_logger.debug("Releasing system IP address " + systemIp
                            + " as corresponding lb rule failed to create");
                    _networkMgr.handleSystemIpRelease(systemIp);
                }
                // release ip address if ipassoc was perfored
                if (performedIpAssoc) {
                    ipVO = _ipAddressDao.findById(ipVO.getId());
                    _vpcMgr.unassignIPFromVpcNetwork(ipVO.getId(), lb.getNetworkId());
                }
            }
        }

        if (result == null) {
            throw new CloudRuntimeException("Failed to create load balancer rule: " + lb.getName());
        }

        return result;
    }

    @Override
    @DB
    public LoadBalancer createLoadBalancer(CreateLoadBalancerRuleCmd lb, boolean openFirewall)
            throws NetworkRuleConflictException {
        UserContext caller = UserContext.current();
        int srcPortStart = lb.getSourcePortStart();
        int defPortStart = lb.getDefaultPortStart();
        int srcPortEnd = lb.getSourcePortEnd();
        long sourceIpId = lb.getSourceIpAddressId();

        IPAddressVO ipAddr = _ipAddressDao.findById(sourceIpId);
        // make sure ip address exists
        if (ipAddr == null || !ipAddr.readyToUse()) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "Unable to create load balancer rule, invalid IP address id specified");
            ex.addProxyObject(ipAddr, sourceIpId, "sourceIpId");
            throw ex;
        } else if (ipAddr.isOneToOneNat()) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "Unable to create load balancer rule; specified sourceip id has static nat enabled");
            ex.addProxyObject(ipAddr, sourceIpId, "sourceIpId");
            throw ex;
        }

        Long networkId = ipAddr.getAssociatedWithNetworkId();
        if (networkId == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "Unable to create load balancer rule ; specified sourceip id is not associated with any network");
            ex.addProxyObject(ipAddr, sourceIpId, "sourceIpId");
            throw ex;
        }

        _firewallMgr.validateFirewallRule(caller.getCaller(), ipAddr, srcPortStart, srcPortEnd, lb.getProtocol(),
                Purpose.LoadBalancing, FirewallRuleType.User, networkId, null);
        NetworkVO network = _networkDao.findById(networkId);
        _accountMgr.checkAccess(caller.getCaller(), null, true, ipAddr);

        // verify that lb service is supported by the network
        if (!_networkModel.areServicesSupportedInNetwork(network.getId(), Service.Lb)) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "LB service is not supported in specified network id");
            ex.addProxyObject(network, networkId, "networkId");
            throw ex;
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        LoadBalancerVO newRule = new LoadBalancerVO(lb.getXid(), lb.getName(), lb.getDescription(),
                lb.getSourceIpAddressId(), lb.getSourcePortEnd(), lb.getDefaultPortStart(), lb.getAlgorithm(),
                network.getId(), ipAddr.getAllocatedToAccountId(), ipAddr.getAllocatedInDomainId());

        // verify rule is supported by Lb provider of the network
        LoadBalancingRule loadBalancing = new LoadBalancingRule(newRule, new ArrayList<LbDestination>(),
                new ArrayList<LbStickinessPolicy>(), new ArrayList<LbHealthCheckPolicy>());
        if (!validateRule(loadBalancing)) {
            throw new InvalidParameterValueException("LB service provider cannot support this rule");
        }

        newRule = _lbDao.persist(newRule);

        if (openFirewall) {
            _firewallMgr.createRuleForAllCidrs(sourceIpId, caller.getCaller(), lb.getSourcePortStart(),
                    lb.getSourcePortEnd(), lb.getProtocol(), null, null, newRule.getId(), networkId);
        }

        boolean success = true;

        try {
            _firewallMgr.detectRulesConflict(newRule);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            s_logger.debug("Load balancer " + newRule.getId() + " for Ip address id=" + sourceIpId + ", public port "
                    + srcPortStart + ", private port " + defPortStart + " is added successfully.");
            UserContext.current().setEventDetails("Load balancer Id: " + newRule.getId());
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_LOAD_BALANCER_CREATE, ipAddr.getAllocatedToAccountId(),
                    ipAddr.getDataCenterId(), newRule.getId(), null, LoadBalancingRule.class.getName(),
                    newRule.getUuid());
            txn.commit();

            return newRule;
        } catch (Exception e) {
            success = false;
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            }
            throw new CloudRuntimeException("Unable to add rule for ip address id=" + newRule.getSourceIpAddressId(), e);
        } finally {
            if (!success && newRule != null) {

                txn.start();
                _firewallMgr.revokeRelatedFirewallRule(newRule.getId(), false);
                removeLBRule(newRule);

                txn.commit();
            }
        }
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
            // get all rules in transition state
            lbs = _lbDao.listInTransitionStateByNetworkId(lb.getNetworkId());
        }
        return applyLoadBalancerRules(lbs, true);
    }

    @Override
    public boolean applyLoadBalancersForNetwork(long networkId) throws ResourceUnavailableException {
        List<LoadBalancerVO> lbs = _lbDao.listByNetworkId(networkId);
        if (lbs != null) {
            return applyLoadBalancerRules(lbs, true);
        } else {
            s_logger.info("Network id=" + networkId + " doesn't have load balancer rules, nothing to apply");
            return true;
        }
    }

    @Override
    public boolean applyRules(Network network, Purpose purpose, List<? extends FirewallRule> rules)
            throws ResourceUnavailableException {
        assert (purpose == Purpose.LoadBalancing) : "LB Manager asked to handle non-LB rules";
        boolean handled = false;
        for (LoadBalancingServiceProvider lbElement : _lbProviders) {
            Provider provider = lbElement.getProvider();
            boolean isLbProvider = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Lb,
                    provider);
            if (!isLbProvider) {
                continue;
            }
            handled = lbElement.applyLBRules(network, (List<LoadBalancingRule>) rules);
            if (handled)
                break;
        }
        return handled;
    }

    @DB
    protected boolean applyLoadBalancerRules(List<LoadBalancerVO> lbs, boolean updateRulesInDB)
            throws ResourceUnavailableException {
        Transaction txn = Transaction.currentTxn();
        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        for (LoadBalancerVO lb : lbs) {
            List<LbDestination> dstList = getExistingDestinations(lb.getId());
            List<LbStickinessPolicy> policyList = getStickinessPolicies(lb.getId());
            List<LbHealthCheckPolicy> hcPolicyList = getHealthCheckPolicies(lb.getId());

            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList);
            rules.add(loadBalancing);
        }

        if (!_networkMgr.applyRules(rules, FirewallRule.Purpose.LoadBalancing, this, false)) {
            s_logger.debug("LB rules are not completely applied");
            return false;
        }

        if (updateRulesInDB) {
            for (LoadBalancerVO lb : lbs) {
                boolean checkForReleaseElasticIp = false;
                txn.start();
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
                }

                if (!instanceIds.isEmpty()) {
                    _lb2VmMapDao.remove(lb.getId(), instanceIds, null);
                    s_logger.debug("Load balancer rule id " + lb.getId() + " is removed for vms " + instanceIds);
                }

                if (_lb2VmMapDao.listByLoadBalancerId(lb.getId()).isEmpty()) {
                    lb.setState(FirewallRule.State.Add);
                    _lbDao.persist(lb);
                    s_logger.debug("LB rule " + lb.getId()
                            + " state is set to Add as there are no more active LB-VM mappings");
                }

                // remove LB-Stickiness policy mapping that were state to revoke
                List<LBStickinessPolicyVO> stickinesspolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(
                        lb.getId(), true);
                if (!stickinesspolicies.isEmpty()) {
                    _lb2stickinesspoliciesDao.remove(lb.getId(), true);
                    s_logger.debug("Load balancer rule id " + lb.getId() + " is removed stickiness policies");
                }

                // remove LB-HealthCheck policy mapping that were state to
                // revoke
                List<LBHealthCheckPolicyVO> healthCheckpolicies = _lb2healthcheckDao.listByLoadBalancerId(lb.getId(),
                        true);
                if (!healthCheckpolicies.isEmpty()) {
                    _lb2healthcheckDao.remove(lb.getId(), true);
                    s_logger.debug("Load balancer rule id " + lb.getId() + " is removed health check monitors policies");
                }

                txn.commit();
                if (checkForReleaseElasticIp) {
                    boolean success = true;
                    long count = _firewallDao.countRulesByIpId(lb.getSourceIpAddressId());
                    if (count == 0) {
                        try {
                            success = handleSystemLBIpRelease(lb);
                        } catch (Exception ex) {
                            s_logger.warn("Failed to release system ip as a part of lb rule " + lb
                                    + " deletion due to exception ", ex);
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
                IpAddress ip = _ipAddressDao.findById(lb.getSourceIpAddressId());
                _vpcMgr.unassignIPFromVpcNetwork(ip.getId(), lb.getNetworkId());
            }
        }

        return true;
    }

    protected boolean handleSystemLBIpRelease(LoadBalancerVO lb) {
        IpAddress ip = _ipAddressDao.findById(lb.getSourceIpAddressId());
        boolean success = true;
        if (ip.getSystem()) {
            s_logger.debug("Releasing system ip address " + lb.getSourceIpAddressId() + " as a part of delete lb rule");
            if (!_networkMgr.disassociatePublicIpAddress(lb.getSourceIpAddressId(), UserContext.current()
                    .getCallerUserId(), UserContext.current().getCaller())) {
                s_logger.warn("Unable to release system ip address id=" + lb.getSourceIpAddressId()
                        + " as a part of delete lb rule");
                success = false;
            } else {
                s_logger.warn("Successfully released system ip address id=" + lb.getSourceIpAddressId()
                        + " as a part of delete lb rule");
            }
        }
        return success;
    }

    @Override
    public boolean removeAllLoadBalanacersForIp(long ipId, Account caller, long callerUserId) {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.LoadBalancing);
        if (rules != null)
            s_logger.debug("Found " + rules.size() + " lb rules to cleanup");
        for (FirewallRule rule : rules) {
            boolean result = deleteLoadBalancerRule(rule.getId(), true, caller, callerUserId, false);
            if (result == false) {
                s_logger.warn("Unable to remove load balancer rule " + rule.getId());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeAllLoadBalanacersForNetwork(long networkId, Account caller, long callerUserId) {
        List<FirewallRuleVO> rules = _firewallDao
                .listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.LoadBalancing);
        if (rules != null)
            s_logger.debug("Found " + rules.size() + " lb rules to cleanup");
        for (FirewallRule rule : rules) {
            boolean result = deleteLoadBalancerRule(rule.getId(), true, caller, callerUserId, false);
            if (result == false) {
                s_logger.warn("Unable to remove load balancer rule " + rule.getId());
                return false;
            }
        }
        return true;
    }

    @Override
    public List<LbStickinessPolicy> getStickinessPolicies(long lbId) {
        List<LbStickinessPolicy> stickinessPolicies = new ArrayList<LbStickinessPolicy>();
        List<LBStickinessPolicyVO> sDbpolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(lbId);

        for (LBStickinessPolicyVO sDbPolicy : sDbpolicies) {
            LbStickinessPolicy sPolicy = new LbStickinessPolicy(sDbPolicy.getMethodName(), sDbPolicy.getParams(),
                    sDbPolicy.isRevoke());
            stickinessPolicies.add(sPolicy);
        }
        return stickinessPolicies;
    }

    @Override
    public List<LbHealthCheckPolicy> getHealthCheckPolicies(long lbId) {
        List<LbHealthCheckPolicy> healthCheckPolicies = new ArrayList<LbHealthCheckPolicy>();
        List<LBHealthCheckPolicyVO> hcDbpolicies = _lb2healthcheckDao.listByLoadBalancerId(lbId);

        for (LBHealthCheckPolicyVO policy : hcDbpolicies) {
            String pingpath = policy.getpingpath();
            LbHealthCheckPolicy hDbPolicy = new LbHealthCheckPolicy(pingpath, policy.getDescription(),
                    policy.getResponseTime(), policy.getHealthcheckInterval(), policy.getHealthcheckThresshold(),
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
            dstIp = nic.getIp4Address();
            LbDestination lbDst = new LbDestination(lb.getDefaultPortStart(), lb.getDefaultPortEnd(), dstIp,
                    lbVmMap.isRevoke());
            dstList.add(lbDst);
        }
        return dstList;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_UPDATE, eventDescription = "updating load balancer", async = true)
    public LoadBalancer updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long lbRuleId = cmd.getId();
        String name = cmd.getLoadBalancerName();
        String description = cmd.getDescription();
        String algorithm = cmd.getAlgorithm();
        LoadBalancerVO lb = _lbDao.findById(lbRuleId);
        LoadBalancerVO lbBackup = _lbDao.findById(lbRuleId);

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

        boolean success = _lbDao.update(lbRuleId, lb);

        // If algorithm is changed, have to reapply the lb config
        if (algorithm != null) {
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
                    lb.setState(lbBackup.getState());
                    _lbDao.update(lb.getId(), lb);
                    _lbDao.persist(lb);

                    s_logger.debug("LB Rollback rule id: " + lbRuleId + " while updating LB rule.");
                }
                s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
                success = false;
            }
        }

        if (!success) {
            throw new CloudRuntimeException("Failed to update load balancer rule: " + lbRuleId);
        }

        return lb;
    }

    @Override
    public List<UserVmVO> listLoadBalancerInstances(ListLoadBalancerRuleInstancesCmd cmd)
            throws PermissionDeniedException {
        Account caller = UserContext.current().getCaller();
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
        List<LoadBalancerVMMapVO> vmLoadBalancerMappings = null;

        vmLoadBalancerMappings = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId);

        List<Long> appliedInstanceIdList = new ArrayList<Long>();
        if ((vmLoadBalancerMappings != null) && !vmLoadBalancerMappings.isEmpty()) {
            for (LoadBalancerVMMapVO vmLoadBalancerMapping : vmLoadBalancerMappings) {
                appliedInstanceIdList.add(vmLoadBalancerMapping.getInstanceId());
            }
        }

        IPAddressVO addr = _ipAddressDao.findById(loadBalancer.getSourceIpAddressId());
        List<UserVmVO> userVms = _vmDao.listVirtualNetworkInstancesByAcctAndZone(loadBalancer.getAccountId(),
                addr.getDataCenterId(), loadBalancer.getNetworkId());

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
            }
        }

        return loadBalancerInstances;
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
    public List<LBStickinessPolicyVO> searchForLBStickinessPolicies(ListLBStickinessPoliciesCmd cmd)
            throws PermissionDeniedException {
        Account caller = UserContext.current().getCaller();
        Long loadBalancerId = cmd.getLbRuleId();
        LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            return null;
        }

        _accountMgr.checkAccess(caller, null, true, loadBalancer);

        List<LBStickinessPolicyVO> sDbpolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(cmd.getLbRuleId());

        return sDbpolicies;
    }

    @Override
    public List<LBHealthCheckPolicyVO> searchForLBHealthCheckPolicies(ListLBHealthCheckPoliciesCmd cmd)
            throws PermissionDeniedException {
        Account caller = UserContext.current().getCaller();
        Long loadBalancerId = cmd.getLbRuleId();
        LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            return null;
        }
        _accountMgr.checkAccess(caller, null, true, loadBalancer);
        List<LBHealthCheckPolicyVO> hcDbpolicies = _lb2healthcheckDao.listByLoadBalancerId(cmd.getLbRuleId());
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
        Map<String, String> tags = cmd.getTags();

        Account caller = UserContext.current().getCaller();
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
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("sourceIpAddress", sb.entity().getSourceIpAddressId(), SearchCriteria.Op.EQ);

        if (instanceId != null) {
            SearchBuilder<LoadBalancerVMMapVO> lbVMSearch = _lb2VmMapDao.createSearchBuilder();
            lbVMSearch.and("instanceId", lbVMSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("lbVMSearch", lbVMSearch, sb.entity().getId(), lbVMSearch.entity().getLoadBalancerId(),
                    JoinBuilder.JoinType.INNER);
        }

        if (zoneId != null) {
            SearchBuilder<IPAddressVO> ipSearch = _ipAddressDao.createSearchBuilder();
            ipSearch.and("zoneId", ipSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
            sb.join("ipSearch", ipSearch, sb.entity().getSourceIpAddressId(), ipSearch.entity().getId(),
                    JoinBuilder.JoinType.INNER);
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
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(),
                    JoinBuilder.JoinType.INNER);
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
            sc.setParameters("name", "%" + name + "%");
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

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.LoadBalancer.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        Pair<List<LoadBalancerVO>, Integer> result = _lbDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends LoadBalancer>, Integer>(result.first(), result.second());
    }

    @Override
    public List<LoadBalancingRule> listByNetworkId(long networkId) {
        List<LoadBalancerVO> lbs = _lbDao.listByNetworkId(networkId);
        List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
        for (LoadBalancerVO lb : lbs) {
            List<LbDestination> dstList = getExistingDestinations(lb.getId());
            List<LbStickinessPolicy> policyList = this.getStickinessPolicies(lb.getId());
            List<LbHealthCheckPolicy> hcPolicyList = this.getHealthCheckPolicies(lb.getId());
            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList);
            lbRules.add(loadBalancing);
        }
        return lbRules;
    }

    @Override
    public LoadBalancerVO findById(long lbId) {
        return _lbDao.findById(lbId);
    }

    protected void removeLBRule(LoadBalancerVO rule) {
        // remove the rule
        _lbDao.remove(rule.getId());
    }
}
