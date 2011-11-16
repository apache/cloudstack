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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.api.commands.ListLoadBalancerRuleInstancesCmd;
import com.cloud.api.commands.ListLoadBalancerRulesCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
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
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVMMapVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.RulesManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainService;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
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
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

@Local(value = { LoadBalancingRulesManager.class, LoadBalancingRulesService.class })
public class LoadBalancingRulesManagerImpl implements LoadBalancingRulesManager, LoadBalancingRulesService, Manager {
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
                throw new InvalidParameterValueException("Invalid instance id: " + instanceId);
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
                throw new InvalidParameterValueException("VM " + instanceId + " cannot be added because it doesn't belong in the same network.");
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
        try {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
            applyLoadBalancerConfig(loadBalancerId);
            success = true;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
        }
        
        if(!success){
        	throw new CloudRuntimeException("Failed to add load balancer rule id " + loadBalancerId + " for vms " + instanceIds);
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_REMOVE_FROM_LOAD_BALANCER_RULE, eventDescription = "removing from load balancer", async = true)
    public boolean removeFromLoadBalancer(long loadBalancerId, List<Long> instanceIds) {
        return removeFromLoadBalancerInternal(loadBalancerId, instanceIds);
    }

    private boolean removeFromLoadBalancerInternal(long loadBalancerId, List<Long> instanceIds) {
        UserContext caller = UserContext.current();

        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(loadBalancerId));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid load balancer value: " + loadBalancerId);
        }

        _accountMgr.checkAccess(caller.getCaller(), null, loadBalancer);

        boolean success = false;
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
                throw new CloudRuntimeException("Failed to remove load balancer rule id " + loadBalancerId + " for vms " + instanceIds);
            }
            success = true;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
        }
        if(!success){
        	throw new CloudRuntimeException("Failed to remove load balancer rule id " + loadBalancerId + " for vms " + instanceIds);
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
                if (!removeFromLoadBalancerInternal(lb.getKey(), lb.getValue())) {
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

        _accountMgr.checkAccess(caller, null, rule);


        boolean result = deleteLoadBalancerRule(loadBalancerId, apply, caller, ctx.getCallerUserId());
        if(!result){
    		throw new CloudRuntimeException("Unable to remove load balancer rule " + loadBalancerId);
    	}
        return result;
    }

    @DB
    public boolean deleteLoadBalancerRule(long loadBalancerId, boolean apply, Account caller, long callerUserId) {
        LoadBalancerVO lb = _lbDao.findById(loadBalancerId);
        Transaction txn = Transaction.currentTxn();
        boolean generateUsageEvent = false;
        boolean success = true;

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
        
        if (apply) {
            try {
                if (!applyLoadBalancerConfig(loadBalancerId)) {
                    s_logger.warn("Unable to apply the load balancer config");
                    return false;
                }
            } catch (ResourceUnavailableException e) {
                s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
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

    @Override @DB
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_CREATE, eventDescription = "creating load balancer")
    public LoadBalancer createLoadBalancerRule(CreateLoadBalancerRuleCmd lb,  boolean openFirewall) throws NetworkRuleConflictException, InsufficientAddressCapacityException {
        UserContext caller = UserContext.current();
    
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
        
        LoadBalancer result = _elbMgr.handleCreateLoadBalancerRule(lb, caller.getCaller());
        if (result == null){
            result =  createLoadBalancer(lb, openFirewall);
        } 
    	if(result == null){
    		throw new CloudRuntimeException("Failed to create load balancer rule: "+lb.getName());
    	}
        return result;
    }
    
    @DB
    public LoadBalancer createLoadBalancer(CreateLoadBalancerRuleCmd lb, boolean openFirewall) throws NetworkRuleConflictException {
        long ipId = lb.getSourceIpAddressId();
        UserContext caller = UserContext.current();
        int srcPortStart = lb.getSourcePortStart();
        int defPortStart = lb.getDefaultPortStart();
        int srcPortEnd = lb.getSourcePortEnd();
        
        IPAddressVO ipAddr = _ipAddressDao.findById(ipId);
        Long networkId = ipAddr.getSourceNetworkId();
        // make sure ip address exists
        if (ipAddr == null || !ipAddr.readyToUse()) {
            throw new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address id" + ipId);
        } else if (ipAddr.isOneToOneNat()) {
            throw new InvalidParameterValueException("Unable to create load balancer rule; ip id=" + ipId + " has static nat enabled");
        }                 
        
        _firewallMgr.validateFirewallRule(caller.getCaller(), ipAddr, srcPortStart, srcPortEnd, lb.getProtocol(), Purpose.LoadBalancing, FirewallRuleType.User);

        
         networkId = ipAddr.getAssociatedWithNetworkId();
        if (networkId == null) {
            throw new InvalidParameterValueException("Unable to create load balancer rule ; ip id=" + ipId + " is not associated with any network");

        }
        NetworkVO network = _networkDao.findById(networkId);

        _accountMgr.checkAccess(caller.getCaller(), null, ipAddr);

        // verify that lb service is supported by the network
        if (!_networkMgr.areServicesSupportedInNetwork(network.getId(), Service.Lb)) {
            throw new InvalidParameterValueException("LB service is not supported in network id= " + networkId);

        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        LoadBalancerVO newRule = new LoadBalancerVO(lb.getXid(), lb.getName(), lb.getDescription(), lb.getSourceIpAddressId(), lb.getSourcePortEnd(), lb.getDefaultPortStart(), 
                lb.getAlgorithm(), network.getId(), ipAddr.getAccountId(), ipAddr.getDomainId());

        newRule = _lbDao.persist(newRule);
        
        if (openFirewall) {
            _firewallMgr.createRuleForAllCidrs(ipId, caller.getCaller(), lb.getSourcePortStart(), lb.getSourcePortEnd(), lb.getProtocol(), null, null, newRule.getId());
        }

        boolean success = true;

        try {
            _firewallMgr.detectRulesConflict(newRule, ipAddr);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            s_logger.debug("Load balancer " + newRule.getId() + " for Ip address id=" + ipId + ", public port " + srcPortStart + ", private port " + defPortStart + " is added successfully.");
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
        List<LoadBalancerVO> lbs = new ArrayList<LoadBalancerVO>(1);
        lbs.add(_lbDao.findById(lbRuleId));
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

            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList);
            rules.add(loadBalancing);
        }

        if (!_networkMgr.applyRules(rules, false)) {
            s_logger.debug("LB rules are not completely applied");
            return false;
        }

        if (updateRulesInDB) {
            for (LoadBalancerVO lb : lbs) {
                txn.start();
                if (lb.getState() == FirewallRule.State.Revoke) {
                    _lbDao.remove(lb.getId());
                    s_logger.warn("LB " + lb.getId() + " is successfully removed");
                } else if (lb.getState() == FirewallRule.State.Add) {
                    lb.setState(FirewallRule.State.Active);
                    s_logger.warn("LB rule " + lb.getId() + " state is set to Active");
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

                txn.commit();
            }
        }
        
        return true;
    }

    @Override
    public boolean removeAllLoadBalanacersForIp(long ipId, Account caller, long callerUserId) {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.LoadBalancing);
        if (rules != null)
            s_logger.debug("Found " + rules.size() + " lb rules to cleanup");
        for (FirewallRule rule : rules) {
            boolean result = deleteLoadBalancerRule(rule.getId(), true, caller, callerUserId);
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
            boolean result = deleteLoadBalancerRule(rule.getId(), true, caller, callerUserId);
            if (result == false) {
                s_logger.warn("Unable to remove load balancer rule " + rule.getId());
                return false;
            }
        }
        return true;
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
        
        if (lb == null) {
            throw new InvalidParameterValueException("Unable to find lb rule by id=" + lbRuleId);
        }
        
        //check permissions
        _accountMgr.checkAccess(caller, null, lb);

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
                s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
                success = false;
            }
        }

        if(!success){
    		throw new CloudRuntimeException("Failed to update load balancer rule: "+lbRuleId);
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

        _accountMgr.checkAccess(caller, null, loadBalancer);

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
    public List<LoadBalancerVO> searchForLoadBalancers(ListLoadBalancerRulesCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long ipId = cmd.getPublicIpId();
        Long zoneId = cmd.getZoneId();
        String path = null;

        Pair<List<Long>, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        List<Long> permittedAccounts = accountDomainPair.first();
        Long domainId = accountDomainPair.second();

        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            Domain domain = _domainMgr.getDomain(caller.getDomainId());
            path = domain.getPath();
        }

        Filter searchFilter = new Filter(LoadBalancerVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object name = cmd.getLoadBalancerRuleName();
        Object keyword = cmd.getKeyword();
        Object instanceId = cmd.getVirtualMachineId();

        SearchBuilder<LoadBalancerVO> sb = _lbDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("sourceIpAddress", sb.entity().getSourceIpAddressId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);

        if (instanceId != null) {
            SearchBuilder<LoadBalancerVMMapVO> lbVMSearch = _lb2VmMapDao.createSearchBuilder();
            lbVMSearch.and("instanceId", lbVMSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("lbVMSearch", lbVMSearch, sb.entity().getId(), lbVMSearch.entity().getLoadBalancerId(), JoinBuilder.JoinType.INNER);
        }

        if (path != null) {
            // for domain admin we should show only subdomains information
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (zoneId != null) {
            SearchBuilder<IPAddressVO> ipSearch = _ipAddressDao.createSearchBuilder();
            ipSearch.and("zoneId", ipSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
            sb.join("ipSearch", ipSearch, sb.entity().getSourceIpAddressId(), ipSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<LoadBalancerVO> sc = sb.create();
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

        if (domainId != null) {
            sc.setParameters("domainId", domainId);
        }
        
        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountId", permittedAccounts.toArray());
        }

        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
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
            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList);
            lbRules.add(loadBalancing);
        }
        return lbRules;
    }

    @Override
    public LoadBalancerVO findById(long lbId) {
        return _lbDao.findById(lbId);
    }

}
