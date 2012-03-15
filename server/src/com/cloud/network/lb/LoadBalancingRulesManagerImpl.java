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
package com.cloud.network.lb;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.commands.CreateLBStickinessPolicyCmd;
import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.api.commands.ListLBStickinessPoliciesCmd;
import com.cloud.api.commands.ListLoadBalancerRuleInstancesCmd;
import com.cloud.api.commands.ListLoadBalancerRulesCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.api.response.ServiceResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.ExternalLoadBalancerUsageManager;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.LBStickinessPolicyVO;
import com.cloud.network.LoadBalancerVMMapVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LBStickinessPolicyDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LbStickinessMethod.LbStickinessMethodParam;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.offering.NetworkOffering;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainService;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.AnnotationHelper;
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Local(value = { LoadBalancingRulesManager.class, LoadBalancingRulesService.class })
public class LoadBalancingRulesManagerImpl<Type> implements LoadBalancingRulesManager, LoadBalancingRulesService, Manager {
    private static final Logger s_logger = Logger.getLogger(LoadBalancingRulesManagerImpl.class);

    String _name;

    @Inject
    NetworkManager _networkMgr;
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
    ElasticLoadBalancerManager _elbMgr;
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

    private String getLBStickinessCapability(long networkid) {
        Map<Service, Map<Capability, String>> serviceCapabilitiesMap = _networkMgr.getNetworkCapabilities(networkid);
        if (serviceCapabilitiesMap != null) {
            for (Service service : serviceCapabilitiesMap.keySet()) {
                ServiceResponse serviceResponse = new ServiceResponse();
                serviceResponse.setName(service.getName());
                if ("Lb".equalsIgnoreCase(service.getName())) {
                    Map<Capability, String> serviceCapabilities = serviceCapabilitiesMap
                            .get(service);
                    if (serviceCapabilities != null) {
                        for (Capability capability : serviceCapabilities
                                .keySet()) {
                            if (Capability.SupportedStickinessMethods.getName()
                                    .equals(capability.getName())) {
                                return serviceCapabilities.get(capability);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean genericValidator(CreateLBStickinessPolicyCmd cmd) throws InvalidParameterValueException {
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        /* Validation : check for valid Method name and params */
        List<LbStickinessMethod> stickinessMethodList = getStickinessMethods(loadBalancer
                .getNetworkId());
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
                        HashMap<String, String> paramKVpair = (HashMap) iter.next();
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
        if (stickinessPolicies.size() > 0) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Already policy attached " + cmd.getLbRuleId());
        }
        return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_STICKINESSPOLICY_CREATE, eventDescription = "create lb stickinesspolicy to load balancer", create = true)
    public StickinessPolicy createLBStickinessPolicy(CreateLBStickinessPolicyCmd cmd) throws NetworkRuleConflictException {
        UserContext caller = UserContext.current();

        /* Validation : check corresponding load balancer rule exist */
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed: LB rule id: " + cmd.getLbRuleId() + " not present ");
        }

        _accountMgr.checkAccess(caller.getCaller(), null, true, loadBalancer);
        if (loadBalancer.getState() == FirewallRule.State.Revoke) {
            throw new InvalidParameterValueException("Failed:  LB rule id: " + cmd.getLbRuleId() + " is in deleting state: ");
        }

        /* Generic validations */
        if (!genericValidator(cmd)) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Validation Failed " + cmd.getLbRuleId());
        }

        /* Specific validations using network element validator for specific validations */
        LBStickinessPolicyVO lbpolicy = new LBStickinessPolicyVO(loadBalancer.getId(), cmd.getLBStickinessPolicyName(), cmd.getStickinessMethodName(), cmd.getparamList(), cmd.getDescription());
        List<LbStickinessPolicy> policyList = new ArrayList<LbStickinessPolicy>();
        policyList.add(new LbStickinessPolicy(cmd.getStickinessMethodName(), lbpolicy.getParams()));
        LoadBalancingRule lbRule = new LoadBalancingRule(loadBalancer, getExistingDestinations(lbpolicy.getId()), policyList);
        if (!_networkMgr.validateRule(lbRule)) {
            throw new InvalidParameterValueException("Failed to create Stickiness policy: Validation Failed " + cmd.getLbRuleId());
        }

        /* Finally Insert into DB */
        LBStickinessPolicyVO policy = new LBStickinessPolicyVO(loadBalancer.getId(), cmd.getLBStickinessPolicyName(), cmd.getStickinessMethodName(), cmd.getparamList(), cmd.getDescription());
        policy = _lb2stickinesspoliciesDao.persist(policy);

        return policy;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_LB_STICKINESSPOLICY_CREATE, eventDescription = "Apply Stickinesspolicy to load balancer ", async = true)
    public boolean applyLBStickinessPolicy(CreateLBStickinessPolicyCmd cmd) {
        boolean success = true;
        
        LoadBalancerVO loadBalancer = _lbDao.findById(cmd.getLbRuleId());
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer Id:"  + cmd.getLbRuleId());
        }
        FirewallRule.State backupState = loadBalancer.getState();
        loadBalancer.setState(FirewallRule.State.Add);
        _lbDao.persist(loadBalancer);
        try {
            applyLoadBalancerConfig(cmd.getLbRuleId());
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply Stickiness policy to the lb rule: " + cmd.getLbRuleId() + " because resource is unavaliable:", e);
            if (isRollBackAllowedForProvider(loadBalancer)) {
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
                s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + " lb state rolback while creating sticky policy" );
            }
            deleteLBStickinessPolicy(cmd.getEntityId(), false);
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
            throw new InvalidParameterException("Invalid Load balancer : " + stickinessPolicy.getLoadBalancerId() + " for Stickiness policy id: " + stickinessPolicyId);
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
                s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
                success = false;
            }
        }else{
            _lb2stickinesspoliciesDao.remove(stickinessPolicy.getLoadBalancerId());
        }

        return success;
    }  

    private boolean isRollBackAllowedForProvider(LoadBalancerVO loadBalancer) {
        Network network = _networkDao.findById(loadBalancer.getNetworkId());
        Provider provider = Network.Provider.Netscaler;
        return _ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(), Service.Lb, provider);
    }
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ASSIGN_TO_LOAD_BALANCER_RULE, eventDescription = "assigning to load balancer", async = true)
    public boolean assignToLoadBalancer(long loadBalancerId, List<Long> instanceIds) {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + ", the load balancer was not found.");
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

            // Let's check to make sure the vm has a nic in the same network as the load balancing rule.
            List<? extends Nic> nics = _networkMgr.getNics(vm.getId());
            Nic nicInSameNetwork = null;
            for (Nic nic : nics) {
                if (nic.getNetworkId() == loadBalancer.getNetworkId()) {
                    nicInSameNetwork = nic;
                    break;
                }
            }

            if (nicInSameNetwork == null) {
            	InvalidParameterValueException ex = new InvalidParameterValueException("VM " + instanceId + " cannot be added because it doesn't belong in the same network.");
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
                    s_logger.debug("LB Rollback rule id: " + loadBalancer.getId() + "  while attaching VM: " + vmInstanceIds);
                }
                loadBalancer.setState(backupState);
                _lbDao.persist(loadBalancer);
            }
            s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
        }
        
        if(!success){
        	CloudRuntimeException ex = new CloudRuntimeException("Failed to add specified loadbalancerruleid for vms " + instanceIds);
        	ex.addProxyObject(loadBalancer, loadBalancerId, "loadBalancerId");           
            // TBD: Also pack in the instanceIds in the exception using the right VO object or table name.            
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

            if (!applyLoadBalancerConfig(loadBalancerId)) {
                s_logger.warn("Failed to remove load balancer rule id " + loadBalancerId + " for vms " + instanceIds);
                CloudRuntimeException ex = new CloudRuntimeException("Failed to remove specified load balancer rule id for vms " + instanceIds);
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
        if(!success){
        	CloudRuntimeException ex = new CloudRuntimeException("Failed to remove specified load balancer rule id for vms " + instanceIds);
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
            s_logger.debug("Set load balancer rule for revoke: rule id " + map.getLoadBalancerId() + ", vmId " + instanceId);
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
    public boolean deleteLoadBalancerRule(long loadBalancerId, boolean apply, Account caller, long callerUserId, boolean rollBack) {
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
                s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", vmId " + map.getInstanceId());
            }
        }

        if (generateUsageEvent) {
            // Generate usage event right after all rules were marked for revoke
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_LOAD_BALANCER_DELETE, lb.getAccountId(), 0, lb.getId(), null);
            _usageEventDao.persist(usageEvent);
        }

        txn.commit();

        // gather external network usage stats for this lb rule
        NetworkVO network = _networkDao.findById(lb.getNetworkId());
        if (network != null) {
            if (_networkMgr.networkIsConfiguredForExternalNetworking(network.getDataCenterId(), network.getId())) {
                _externalLBUsageMgr.updateExternalLoadBalancerNetworkUsageStats(loadBalancerId);
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
                    s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
                }
                return false;
            }
        }

        FirewallRuleVO relatedRule = _firewallDao.findByRelatedId(lb.getId());
        if (relatedRule != null) {
            s_logger.warn("Unable to remove firewall rule id=" + lb.getId() + " as it has related firewall rule id=" + relatedRule.getId() + "; leaving it in Revoke state");
            success = false;
        } else {
            _firewallDao.remove(lb.getId());
        }

        _elbMgr.handleDeleteLoadBalancerRule(lb, callerUserId, caller);

        if (success) {
            s_logger.debug("Load balancer with id " + lb.getId() + " is removed successfully");
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_CREATE, eventDescription = "creating load balancer")
    public LoadBalancer createLoadBalancerRule(CreateLoadBalancerRuleCmd lb, boolean openFirewall) throws NetworkRuleConflictException, InsufficientAddressCapacityException {
        Account lbOwner = _accountMgr.getAccount(lb.getEntityOwnerId());

        int defPortStart = lb.getDefaultPortStart();
        int defPortEnd = lb.getDefaultPortEnd();

        if (!NetUtils.isValidPort(defPortEnd)) {
            throw new InvalidParameterValueException("privatePort is an invalid value: " + defPortEnd);
        }
        if (defPortStart > defPortEnd) {
            throw new InvalidParameterValueException("private port range is invalid: " + defPortStart + "-" + defPortEnd);
        }
        if ((lb.getAlgorithm() == null) || !NetUtils.isValidAlgorithm(lb.getAlgorithm())) {
            throw new InvalidParameterValueException("Invalid algorithm: " + lb.getAlgorithm());
        }

        Long ipAddrId = lb.getSourceIpAddressId();
        IPAddressVO ipAddressVo = null;
        if (ipAddrId != null) {
            ipAddressVo = _ipAddressDao.findById(ipAddrId);

            // Validate ip address
            if (ipAddressVo == null) {
                throw new InvalidParameterValueException("Unable to create load balance rule; ip id=" + ipAddrId + " doesn't exist in the system");
            } else if (ipAddressVo.isOneToOneNat()) {
                throw new NetworkRuleConflictException("Can't do load balance on ip address: " + ipAddressVo.getAddress());
            }

            _networkMgr.checkIpForService(ipAddressVo, Service.Lb);
        }

        LoadBalancer result = _elbMgr.handleCreateLoadBalancerRule(lb, lbOwner, lb.getNetworkId());
        if (result == null) {
            IpAddress ip = null;
            Network guestNetwork = _networkMgr.getNetwork(lb.getNetworkId());
            NetworkOffering off = _configMgr.getNetworkOffering(guestNetwork.getNetworkOfferingId());
            if (off.getElasticLb() && ipAddressVo == null) {
                ip = _networkMgr.assignSystemIp(lb.getNetworkId(), lbOwner, true, false);
                lb.setSourceIpAddressId(ip.getId());
            }
            try {
                result = createLoadBalancer(lb, openFirewall);
            } catch (Exception ex) {
                s_logger.warn("Failed to create load balancer due to ", ex);
                if (ex instanceof NetworkRuleConflictException) {
                    throw (NetworkRuleConflictException) ex;
                }
            } finally {
                if (result == null && ip != null) {
                    s_logger.debug("Releasing system IP address " + ip + " as corresponding lb rule failed to create");
                    _networkMgr.handleSystemIpRelease(ip);
                }
            }
        }

        if (result == null) {
            throw new CloudRuntimeException("Failed to create load balancer rule: " + lb.getName());
        }

        return result;
    }

    @DB
    public LoadBalancer createLoadBalancer(CreateLoadBalancerRuleCmd lb, boolean openFirewall) throws NetworkRuleConflictException {
        UserContext caller = UserContext.current();
        int srcPortStart = lb.getSourcePortStart();
        int defPortStart = lb.getDefaultPortStart();
        int srcPortEnd = lb.getSourcePortEnd();
        long sourceIpId = lb.getSourceIpAddressId();

        IPAddressVO ipAddr = _ipAddressDao.findById(sourceIpId);
        Long networkId = ipAddr.getSourceNetworkId();
        // make sure ip address exists
        if (ipAddr == null || !ipAddr.readyToUse()) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address id specified");
        	ex.addProxyObject(ipAddr, sourceIpId, "sourceIpId");            
            throw ex;
        } else if (ipAddr.isOneToOneNat()) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Unable to create load balancer rule; specified sourceip id has static nat enabled");
        	ex.addProxyObject(ipAddr, sourceIpId, "sourceIpId");            
            throw ex;
        }

        _firewallMgr.validateFirewallRule(caller.getCaller(), ipAddr, srcPortStart, srcPortEnd, lb.getProtocol(), Purpose.LoadBalancing, FirewallRuleType.User);

        networkId = ipAddr.getAssociatedWithNetworkId();
        if (networkId == null) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Unable to create load balancer rule ; specified sourceip id is not associated with any network");
        	ex.addProxyObject(ipAddr, sourceIpId, "sourceIpId");            
            throw ex;

        }
        NetworkVO network = _networkDao.findById(networkId);

        _accountMgr.checkAccess(caller.getCaller(), null, true, ipAddr);

        // verify that lb service is supported by the network
        if (!_networkMgr.areServicesSupportedInNetwork(network.getId(), Service.Lb)) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("LB service is not supported in specified network id");
        	ex.addProxyObject(network, networkId, "networkId");        	
            throw ex;
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        LoadBalancerVO newRule = new LoadBalancerVO(lb.getXid(), lb.getName(), lb.getDescription(), lb.getSourceIpAddressId(), lb.getSourcePortEnd(), lb.getDefaultPortStart(),
                lb.getAlgorithm(), network.getId(), ipAddr.getAllocatedToAccountId(), ipAddr.getAllocatedInDomainId());

        newRule = _lbDao.persist(newRule);

        if (openFirewall) {
            _firewallMgr.createRuleForAllCidrs(sourceIpId, caller.getCaller(), lb.getSourcePortStart(), lb.getSourcePortEnd(), lb.getProtocol(), null, null, newRule.getId());
        }

        boolean success = true;

        try {
            _firewallMgr.detectRulesConflict(newRule, ipAddr);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            s_logger.debug("Load balancer " + newRule.getId() + " for Ip address id=" + sourceIpId + ", public port " + srcPortStart + ", private port " + defPortStart + " is added successfully.");
            UserContext.current().setEventDetails("Load balancer Id: " + newRule.getId());
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_LOAD_BALANCER_CREATE, ipAddr.getAllocatedToAccountId(), ipAddr.getDataCenterId(), newRule.getId(), null);
            _usageEventDao.persist(usageEvent);
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
                _lbDao.remove(newRule.getId());

                txn.commit();
            }
        }
    }

    @Override
    public boolean applyLoadBalancerConfig(long lbRuleId) throws ResourceUnavailableException {
        LoadBalancerVO lb = _lbDao.findById(lbRuleId);
        List<LoadBalancerVO> lbs;
        if (isRollBackAllowedForProvider(lb)) { 
            // this is for Netscalar type of devices. if their is failure the db entries will be rollbacked.
            lbs = new ArrayList<LoadBalancerVO>(1);
            lbs.add(_lbDao.findById(lbRuleId));
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

    @DB
    protected boolean applyLoadBalancerRules(List<LoadBalancerVO> lbs, boolean updateRulesInDB) throws ResourceUnavailableException {
        Transaction txn = Transaction.currentTxn();
        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        for (LoadBalancerVO lb : lbs) {
            List<LbDestination> dstList = getExistingDestinations(lb.getId());
            List<LbStickinessPolicy> policyList = getStickinessPolicies(lb.getId());

            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList);
            rules.add(loadBalancing);
        }

        if (!_networkMgr.applyRules(rules, false)) {
            s_logger.debug("LB rules are not completely applied");
            return false;
        }

        if (updateRulesInDB) {
            for (LoadBalancerVO lb : lbs) {
                boolean checkForReleaseElasticIp = false;
                txn.start();
                if (lb.getState() == FirewallRule.State.Revoke) {
                    _lbDao.remove(lb.getId());
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
                    s_logger.debug("LB rule " + lb.getId() + " state is set to Add as there are no more active LB-VM mappings");
                }

                // remove LB-Stickiness policy mapping that were state to revoke
                List<LBStickinessPolicyVO> stickinesspolicies = _lb2stickinesspoliciesDao.listByLoadBalancerId(lb.getId(), true);
                if (!stickinesspolicies.isEmpty()) {
                    _lb2stickinesspoliciesDao.remove(lb.getId(), true);
                    s_logger.debug("Load balancer rule id " + lb.getId() + " is removed stickiness policies");
                }

                txn.commit();

                if (checkForReleaseElasticIp) {
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
            }
        }

        return true;
    }

    protected boolean handleSystemLBIpRelease(LoadBalancerVO lb) {
        IpAddress ip = _ipAddressDao.findById(lb.getSourceIpAddressId());
        boolean success = true;
        if (ip.getSystem()) {
            s_logger.debug("Releasing system ip address " + lb.getSourceIpAddressId() + " as a part of delete lb rule");
            if (!_networkMgr.releasePublicIpAddress(lb.getSourceIpAddressId(), UserContext.current().getCallerUserId(), UserContext.current().getCaller())) {
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
        List<FirewallRuleVO> rules = _firewallDao.listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.LoadBalancing);
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
            LbStickinessPolicy sPolicy = new LbStickinessPolicy(sDbPolicy.getMethodName(), sDbPolicy.getParams(), sDbPolicy.isRevoke());
            stickinessPolicies.add(sPolicy);
        }
        return stickinessPolicies;
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
            LbDestination lbDst = new LbDestination(lb.getDefaultPortStart(), lb.getDefaultPortEnd(), dstIp, lbVmMap.isRevoke());
            dstList.add(lbDst);
        }
        return dstList;
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
                    /* NOTE : We use lb object to update db instead of lbBackup object since db layer will fail to update if there is no change in the object.  
                     */
                    if (lbBackup.getName() != null) {
                        lb.setName(lbBackup.getName()); 
                    }
                    if (lbBackup.getDescription() != null) {
                        lb.setDescription(lbBackup.getDescription());
                    }
                    if (lbBackup.getAlgorithm() != null){
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
    public List<UserVmVO> listLoadBalancerInstances(ListLoadBalancerRuleInstancesCmd cmd) throws PermissionDeniedException {
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
        List<UserVmVO> userVms = _vmDao.listVirtualNetworkInstancesByAcctAndZone(loadBalancer.getAccountId(), addr.getDataCenterId(), loadBalancer.getNetworkId());

        for (UserVmVO userVm : userVms) {
            // if the VM is destroyed, being expunged, in an error state, or in an unknown state, skip it
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
    public List<LbStickinessMethod> getStickinessMethods(long networkid)
    {
        String capability = getLBStickinessCapability(networkid);
        if (capability == null)
            return null;
        Gson gson = new Gson();
        java.lang.reflect.Type listType = new TypeToken<List<LbStickinessMethod>>() {
        }.getType();
        List<LbStickinessMethod> result = gson.fromJson(capability, listType);
        return result;
    }

    @Override
    public List<LBStickinessPolicyVO> searchForLBStickinessPolicies(ListLBStickinessPoliciesCmd cmd) throws PermissionDeniedException {
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
    public List<LoadBalancerVO> searchForLoadBalancers(ListLoadBalancerRulesCmd cmd) {
        Long ipId = cmd.getPublicIpId();
        Long zoneId = cmd.getZoneId();
        Long id = cmd.getId();
        String name = cmd.getLoadBalancerRuleName();
        String keyword = cmd.getKeyword();
        Long instanceId = cmd.getVirtualMachineId();

        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
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
            sb.join("lbVMSearch", lbVMSearch, sb.entity().getId(), lbVMSearch.entity().getLoadBalancerId(), JoinBuilder.JoinType.INNER);
        }

        if (zoneId != null) {
            SearchBuilder<IPAddressVO> ipSearch = _ipAddressDao.createSearchBuilder();
            ipSearch.and("zoneId", ipSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
            sb.join("ipSearch", ipSearch, sb.entity().getSourceIpAddressId(), ipSearch.entity().getId(), JoinBuilder.JoinType.INNER);
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

        return _lbDao.search(sc, searchFilter);
    }

    @Override
    public List<LoadBalancingRule> listByNetworkId(long networkId) {
        List<LoadBalancerVO> lbs = _lbDao.listByNetworkId(networkId);
        List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
        for (LoadBalancerVO lb : lbs) {
            List<LbDestination> dstList = getExistingDestinations(lb.getId());
            List<LbStickinessPolicy> policyList = this.getStickinessPolicies(lb.getId());
            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList);
            lbRules.add(loadBalancing);
        }
        return lbRules;
    }

    @Override
    public LoadBalancerVO findById(long lbId) {
        return _lbDao.findById(lbId);
    }

}
