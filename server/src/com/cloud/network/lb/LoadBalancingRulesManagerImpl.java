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

import com.cloud.api.commands.ListLoadBalancerRuleInstancesCmd;
import com.cloud.api.commands.ListLoadBalancerRulesCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVMMapVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.RulesManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
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
    FirewallRulesDao _rulesDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    EventDao _eventDao;
    @Inject
    LoadBalancerVMMapDao _lb2VmMapDao;
    @Inject UserVmDao _vmDao;
    @Inject AccountDao _accountDao;
    @Inject DomainDao _domainDao;
    @Inject NicDao _nicDao;
    @Inject UsageEventDao _usageEventDao;

    @Override @DB
    public boolean assignToLoadBalancer(long loadBalancerId, List<Long> instanceIds) {
        UserContext caller = UserContext.current();

        LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + ", the load balancer was not found.");
        }
        
        _accountMgr.checkAccess(caller.getCaller(), loadBalancer);

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
            
            _accountMgr.checkAccess(caller.getCaller(), vm);
            
            if (vm.getAccountId() != loadBalancer.getAccountId()) {
                throw new PermissionDeniedException("Cannot add virtual machines that do not belong to the same owner.");
            }
            
            // Let's check to make sure the vm has a nic in the same network as the load balancing rule.
            List<? extends Nic> nics = _networkMgr.getNics(vm);
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
        
        try {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
            applyLoadBalancerConfig(loadBalancerId);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
            return false;
        }
       
        return true;
    }
    

    @Override
    public boolean removeFromLoadBalancer(long loadBalancerId, List<Long> instanceIds) {
        UserContext caller = UserContext.current();

        LoadBalancerVO loadBalancer = _lbDao.findById(Long.valueOf(loadBalancerId));
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid load balancer value: " + loadBalancerId);
        } 
        
        _accountMgr.checkAccess(caller.getCaller(), loadBalancer);
       
        try {
            loadBalancer.setState(FirewallRule.State.Add);
            _lbDao.persist(loadBalancer);
            
            for (long instanceId : instanceIds) {
                LoadBalancerVMMapVO map = _lb2VmMapDao.findByLoadBalancerIdAndVmId(loadBalancerId, instanceId);
                map.setRevoke(true);
                _lb2VmMapDao.persist(map);
                s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", vmId " + instanceId);
            }

            applyLoadBalancerConfig(loadBalancerId);
            _lb2VmMapDao.remove(loadBalancerId, instanceIds, null);
            s_logger.debug("Load balancer rule id " + loadBalancerId + " is removed for vms " + instanceIds);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
            return false;
        }
       
        return true;
    }
    
    
    @Override
    public boolean removeVmFromLoadBalancers(long instanceId) {
        boolean success = true;
        List<LoadBalancerVMMapVO> maps = _lb2VmMapDao.listByInstanceId(instanceId);
        if (maps == null || maps.isEmpty()) {
            return true;
        }
        
        Map<Long, List<Long>> lbsToReconfigure = new HashMap<Long, List<Long>>();
        
        //first set all existing lb mappings with Revoke state
        for (LoadBalancerVMMapVO map: maps) {
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
        
        //Reapply all lbs that had the vm assigned
        if (lbsToReconfigure != null) {
            for (Map.Entry<Long, List<Long>> lb : lbsToReconfigure.entrySet()) {
                if (!removeFromLoadBalancer(lb.getKey(), lb.getValue())) {
                    success = false;
                }
            }
        }
        return success;
    }
    

    @Override
    public boolean deleteLoadBalancerRule(long loadBalancerId, boolean apply) {
        UserContext caller = UserContext.current();
        
        LoadBalancerVO lb = _lbDao.findById(loadBalancerId);
        if (lb == null) {
            throw new InvalidParameterException("Invalid load balancer value: " + loadBalancerId);
        }
        
        _accountMgr.checkAccess(caller.getCaller(), lb);
        
        lb.setState(FirewallRule.State.Revoke);
        _lbDao.persist(lb);
        
        List<LoadBalancerVMMapVO> maps = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId);
        if (maps != null) {
            for (LoadBalancerVMMapVO map : maps) {
                map.setRevoke(true);
                _lb2VmMapDao.persist(map);
                s_logger.debug("Set load balancer rule for revoke: rule id " + loadBalancerId + ", vmId " + map.getInstanceId());
            }  
        }
        
        if (apply) {
            try {
                applyLoadBalancerConfig(loadBalancerId);
            } catch (ResourceUnavailableException e) {
                s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
                return false;
            }
        }
        
        _rulesDao.remove(lb.getId());
        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_LOAD_BALANCER_DELETE, lb.getAccountId(), 0 , lb.getId(), null);
        _usageEventDao.persist(usageEvent);
        s_logger.debug("Load balancer with id " + lb.getId() + " is removed successfully");
        return true;
    }

    @Override
    public LoadBalancer createLoadBalancerRule(LoadBalancer lb) throws NetworkRuleConflictException {
        UserContext caller = UserContext.current();

        Ip srcIp = lb.getSourceIpAddress();

        // make sure ip address exists
        IPAddressVO ipAddr = _ipAddressDao.findById(srcIp);
        if (ipAddr == null || !ipAddr.readyToUse()) {
            throw new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address " + srcIp);
        }

        int srcPortStart = lb.getSourcePortStart();
        int srcPortEnd = lb.getSourcePortEnd();
        int defPortStart = lb.getDefaultPortStart();
        int defPortEnd = lb.getDefaultPortEnd();

        if (!NetUtils.isValidPort(srcPortStart)) {
            throw new InvalidParameterValueException("publicPort is an invalid value: " + srcPortStart);
        }
        if (!NetUtils.isValidPort(srcPortEnd)) {
            throw new InvalidParameterValueException("Public port range is an invalid value: " + srcPortEnd);
        }
        if (srcPortStart > srcPortEnd) {
            throw new InvalidParameterValueException("Public port range is an invalid value: " + srcPortStart + "-" + srcPortEnd);
        }
        if (!NetUtils.isValidPort(defPortStart)) {
            throw new InvalidParameterValueException("privatePort is an invalid value: " + defPortStart);
        }
        if (!NetUtils.isValidPort(defPortEnd)) {
            throw new InvalidParameterValueException("privatePort is an invalid value: " + defPortEnd);
        }
        if (defPortStart > defPortEnd) {
            throw new InvalidParameterValueException("private port range is invalid: " + defPortStart + "-" + defPortEnd);
        }
        if ((lb.getAlgorithm() == null) || !NetUtils.isValidAlgorithm(lb.getAlgorithm())) {
            throw new InvalidParameterValueException("Invalid algorithm: " + lb.getAlgorithm());
        }
        
        Long networkId = lb.getNetworkId();
        if (networkId == -1 ) {
            networkId = ipAddr.getAssociatedWithNetworkId();
        }
        _accountMgr.checkAccess(caller.getCaller(), ipAddr);
        LoadBalancerVO newRule = new LoadBalancerVO(lb.getXid(), lb.getName(), lb.getDescription(), lb.getSourceIpAddress(), lb.getSourcePortEnd(),
                lb.getDefaultPortStart(), lb.getAlgorithm(), networkId, ipAddr.getAccountId(), ipAddr.getDomainId());

        newRule = _lbDao.persist(newRule);

        boolean success = false;
        try {
            _rulesMgr.detectRulesConflict(newRule, ipAddr);
            if (!_rulesDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            s_logger.debug("Load balancer " + newRule.getId() + " for Ip address " +  srcIp + ", public port " + srcPortStart + ", private port " + defPortStart+ " is added successfully.");
            success = true;
            return newRule;
        } catch (Exception e) {
            _lbDao.remove(newRule.getId());
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            }
            throw new CloudRuntimeException("Unable to add rule for " + newRule.getSourceIpAddress(), e);
        } finally {
            long userId = caller.getCallerUserId();

            EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(ipAddr.getAllocatedToAccountId());
            event.setType(EventTypes.EVENT_LOAD_BALANCER_CREATE);

            if (!success) {
                event.setDescription("Failed to create load balancer " + lb.getName() + " on ip address " + srcIp + "[" + srcPortStart + "->"
                        + defPortStart + "]");
                event.setLevel(EventVO.LEVEL_ERROR);
            } else {
                event.setDescription("Successfully created load balancer " + lb.getName() + " on ip address " + srcIp + "[" + srcPortStart + "->"
                        + defPortStart + "]");
                event.setLevel(EventVO.LEVEL_INFO);
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_LOAD_BALANCER_CREATE, ipAddr.getAllocatedToAccountId(), ipAddr.getDataCenterId(), newRule.getId(), null);
                _usageEventDao.persist(usageEvent);
            }
            _eventDao.persist(event);
        }
    }

    @Override
    public boolean applyLoadBalancerConfig(long lbRuleId) throws ResourceUnavailableException {
        
        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        LoadBalancerVO lb = _lbDao.findById(lbRuleId);
        List<LbDestination> dstList = getExistingDestinations(lb.getId());
        
        if (dstList != null && !dstList.isEmpty()) {
            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList);
            rules.add(loadBalancing);

            if (!_networkMgr.applyRules(rules, false)) {
                s_logger.debug("LB rules are not completely applied");
                return false;
            } 
            
            if (lb.getState() == FirewallRule.State.Revoke) {
                _lbDao.remove(lb.getId());
                s_logger.debug("LB " + lb.getId() + " is successfully removed");
            } else if (lb.getState() == FirewallRule.State.Add) {
                lb.setState(FirewallRule.State.Active);
                s_logger.debug("LB rule " + lbRuleId + " state is set to Active");
                _lbDao.persist(lb);
            }
        }
        return true;
    }

    @Override
    public boolean removeAllLoadBalanacers(Ip ip) {   
        List<FirewallRuleVO> rules = _rulesDao.listByIpAndNotRevoked(ip);
        if (rules != null)
        s_logger.debug("Found " + rules.size() + " lb rules to cleanup");
        for (FirewallRule rule : rules) {
            if (rule.getPurpose() == Purpose.LoadBalancing) {
                boolean result = deleteLoadBalancerRule(rule.getId(), true);
                if (result == false) {
                    s_logger.warn("Unable to remove load balancer rule " + rule.getId());
                    return false;
                }
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
            Nic nic = _nicDao.findByInstanceIdAndNetworkId(lb.getNetworkId(), vm.getId());
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
    public LoadBalancer updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd) {
        Long lbRuleId = cmd.getId();
        String name = cmd.getLoadBalancerName();
        String description = cmd.getDescription();
        String algorithm = cmd.getAlgorithm();
        LoadBalancerVO lb = _lbDao.findById(lbRuleId);
        
        
        if (name != null) {
            lb.setName(name);
        }
         
        if (description != null) {
            lb.setDescription(description);
        }
        
        if (algorithm != null) {
            lb.setAlgorithm(algorithm);
        }
        
        _lbDao.update(lbRuleId, lb);
        
        //If algorithm is changed, have to reapply the lb config
        if (algorithm != null) {
            try {
                lb.setState(FirewallRule.State.Add);
                _lbDao.persist(lb);
                applyLoadBalancerConfig(lbRuleId);
            } catch (ResourceUnavailableException e) {
                s_logger.warn("Unable to apply the load balancer config because resource is unavaliable.", e);
            }
        }
        
        return lb;
    }
    
//  @Override @DB
//  public boolean removeFromLoadBalancer(RemoveFromLoadBalancerRuleCmd cmd) throws InvalidParameterValueException {
//
//      Long userId = UserContext.current().getUserId();
//      Account account = UserContext.current().getAccount();
//      Long loadBalancerId = cmd.getId();
//      Long vmInstanceId = cmd.getVirtualMachineId();
//      List<Long> instanceIds = cmd.getVirtualMachineIds();
//
//      if ((vmInstanceId == null) && (instanceIds == null)) {
//          throw new ServerApiException(BaseCmd.PARAM_ERROR, "No virtual machine id specified.");
//      }
//
//      // if a single instanceId was given, add it to the list so we can always just process the list if instanceIds
//      if (instanceIds == null) {
//          instanceIds = new ArrayList<Long>();
//          instanceIds.add(vmInstanceId);
//      }
//
//      if (userId == null) {
//          userId = Long.valueOf(1);
//      }
//
//      LoadBalancerVO loadBalancer = _loadBalancerDao.findById(Long.valueOf(loadBalancerId));
//
//      if (loadBalancer == null) {
//          throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find load balancer rule with id " + loadBalancerId);
//      } else if (account != null) {
//          if (!isAdmin(account.getType()) && (loadBalancer.getAccountId() != account.getId())) {
//              throw new ServerApiException(BaseCmd.PARAM_ERROR, "Account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName() +
//                      " (id:" + loadBalancer.getId() + ")");
//          } else if (!_domainDao.isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
//              throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid load balancer rule id (" + loadBalancer.getId() + ") given, unable to remove virtual machine instances.");
//          }
//      }
//
//      Transaction txn = Transaction.currentTxn();
//      LoadBalancerVO loadBalancerLock = null;
//      boolean success = true;
//      try {
//
//          IPAddressVO ipAddress = _ipAddressDao.findById(loadBalancer.getIpAddress());
//          if (ipAddress == null) {
//              return false;
//          }
//
//          DomainRouterVO router = _routerMgr.getRouter(ipAddress.getAccountId(), ipAddress.getDataCenterId());
//          if (router == null) {
//              return false;
//          }
//
//          txn.start();
//          for (Long instanceId : instanceIds) {
//              UserVm userVm = _userVmDao.findById(instanceId);
//              if (userVm == null) {
//                  s_logger.warn("Unable to find virtual machine with id " + instanceId);
//                  throw new InvalidParameterValueException("Unable to find virtual machine with id " + instanceId);
//              }
//              PortForwardingRuleVO fwRule = _rulesDao.findByGroupAndPrivateIp(loadBalancerId, userVm.getGuestIpAddress(), false);
//              if (fwRule != null) {
//                  fwRule.setEnabled(false);
//                  _rulesDao.update(fwRule.getId(), fwRule);
//              }
//          }
//
//          List<PortForwardingRuleVO> allLbRules = new ArrayList<PortForwardingRuleVO>();
//          IPAddressVO ipAddr = _ipAddressDao.findById(loadBalancer.getIpAddress());
//          List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddr.getDataCenterId(), null);
//          for (IPAddressVO ipv : ipAddrs) {
//              List<PortForwardingRuleVO> rules = _rulesDao.listIPForwarding(ipv.getAddress(), false);
//              allLbRules.addAll(rules);
//          }
//
//          updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);
//
//          // firewall rules are updated, lock the load balancer as mappings are updated
//          loadBalancerLock = _loadBalancerDao.acquireInLockTable(loadBalancerId);
//          if (loadBalancerLock == null) {
//              s_logger.warn("removeFromLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
//          }
//
//          // remove all the loadBalancer->VM mappings
//          _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, Boolean.FALSE);
//
//          // Save and create the event
//          String description;
//          String type = EventTypes.EVENT_NET_RULE_DELETE;
//          String level = EventVO.LEVEL_INFO;
//
//          for (PortForwardingRuleVO updatedRule : allLbRules) {
//              if (!updatedRule.isEnabled()) {
//                  _rulesDao.remove(updatedRule.getId());
//
//                  description = "deleted load balancer rule [" + updatedRule.getSourceIpAddress() + ":" + updatedRule.getSourcePort() + "]->["
//                  + updatedRule.getDestinationIpAddress() + ":" + updatedRule.getDestinationPort() + "]" + " " + updatedRule.getProtocol();
//
//                  EventUtils.saveEvent(userId, loadBalancer.getAccountId(), level, type, description);
//              }
//          }
//          txn.commit();
//      } catch (Exception ex) {
//          s_logger.warn("Failed to delete load balancing rule with exception: ", ex);
//          success = false;
//          txn.rollback();
//      } finally {
//          if (loadBalancerLock != null) {
//              _loadBalancerDao.releaseFromLockTable(loadBalancerId);
//          }
//      }
//      return success;
//  }
//
//  @Override @DB
//  public boolean deleteLoadBalancerRule(DeleteLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
//      Long loadBalancerId = cmd.getId();
//      Long userId = UserContext.current().getUserId();
//      Account account = UserContext.current().getAccount();
//
//      ///verify input parameters
//      LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
//      if (loadBalancer == null) {
//          throw new InvalidParameterValueException ("Unable to find load balancer rule with id " + loadBalancerId);
//      }
//
//      if (account != null) {
//          if (!isAdmin(account.getType())) {
//              if (loadBalancer.getAccountId() != account.getId()) {
//                  throw new PermissionDeniedException("Account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName() + " (id:" + loadBalancerId + "), permission denied");
//              }
//          } else if (!_domainDao.isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
//              throw new PermissionDeniedException("Unable to delete load balancer rule " + loadBalancer.getName() + " (id:" + loadBalancerId + "), permission denied.");
//          }
//      }
//
//      if (userId == null) {
//          userId = Long.valueOf(1);
//      }
//
//      Transaction txn = Transaction.currentTxn();
//      LoadBalancerVO loadBalancerLock = null;
//      try {
//
//          IPAddressVO ipAddress = _ipAddressDao.findById(loadBalancer.getIpAddress());
//          if (ipAddress == null) {
//              return false;
//          }
//
//          DomainRouterVO router = _routerMgr.getRouter(ipAddress.getAccountId(), ipAddress.getDataCenterId());
//          List<PortForwardingRuleVO> fwRules = _firewallRulesDao.listByLoadBalancerId(loadBalancerId);
//
//          txn.start();
//
//          if ((fwRules != null) && !fwRules.isEmpty()) {
//              for (PortForwardingRuleVO fwRule : fwRules) {
//                  fwRule.setEnabled(false);
//                  _firewallRulesDao.update(fwRule.getId(), fwRule);
//              }
//
//              List<PortForwardingRuleVO> allLbRules = new ArrayList<PortForwardingRuleVO>();
//              List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddress.getDataCenterId(), null);
//              for (IPAddressVO ipv : ipAddrs) {
//                  List<PortForwardingRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
//                  allLbRules.addAll(rules);
//              }
//
//              updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);
//
//              // firewall rules are updated, lock the load balancer as the mappings are updated
//              loadBalancerLock = _loadBalancerDao.acquireInLockTable(loadBalancerId);
//              if (loadBalancerLock == null) {
//                  s_logger.warn("deleteLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
//              }
//
//              // remove all loadBalancer->VM mappings
//              List<LoadBalancerVMMapVO> lbVmMap = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId);
//              if (lbVmMap != null && !lbVmMap.isEmpty()) {
//                  for (LoadBalancerVMMapVO lb : lbVmMap) {
//                      _loadBalancerVMMapDao.remove(lb.getId());
//                  }
//              }
//
//              // Save and create the event
//              String description;
//              String type = EventTypes.EVENT_NET_RULE_DELETE;
//              String ruleName = "load balancer";
//              String level = EventVO.LEVEL_INFO;
//              Account accountOwner = _accountDao.findById(loadBalancer.getAccountId());
//
//              for (PortForwardingRuleVO updatedRule : fwRules) {
//                  _firewallRulesDao.remove(updatedRule.getId());
//
//                  description = "deleted " + ruleName + " rule [" + updatedRule.getSourceIpAddress() + ":" + updatedRule.getSourcePort() + "]->["
//                  + updatedRule.getDestinationIpAddress() + ":" + updatedRule.getDestinationPort() + "]" + " " + updatedRule.getProtocol();
//
//                  EventUtils.saveEvent(userId, accountOwner.getId(), level, type, description);
//              }
//          }
//
//          txn.commit();
//      } catch (Exception ex) {
//          txn.rollback();
//          s_logger.error("Unexpected exception deleting load balancer " + loadBalancerId, ex);
//          return false;
//      } finally {
//          if (loadBalancerLock != null) {
//              _loadBalancerDao.releaseFromLockTable(loadBalancerId);
//          }
//      }
//
//      boolean success = _loadBalancerDao.remove(loadBalancerId);
//
//      // save off an event for removing the load balancer
//      EventVO event = new EventVO();
//      event.setUserId(userId);
//      event.setAccountId(loadBalancer.getAccountId());
//      event.setType(EventTypes.EVENT_LOAD_BALANCER_DELETE);
//      if (success) {
//          event.setLevel(EventVO.LEVEL_INFO);
//          String params = "id="+loadBalancer.getId();
//          event.setParameters(params);
//          event.setDescription("Successfully deleted load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ")");
//      } else {
//          event.setLevel(EventVO.LEVEL_ERROR);
//          event.setDescription("Failed to delete load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ")");
//      }
//      _eventDao.persist(event);
//      return success;
//  }
//  @Override @DB
//  public boolean assignToLoadBalancer(AssignToLoadBalancerRuleCmd cmd)  throws NetworkRuleConflictException {
//      Long loadBalancerId = cmd.getLoadBalancerId();
//      Long instanceIdParam = cmd.getVirtualMachineId();
//      List<Long> instanceIds = cmd.getVirtualMachineIds();
//
//      if ((instanceIdParam == null) && (instanceIds == null)) {
//          throw new InvalidParameterValueException("Unable to assign to load balancer " + loadBalancerId + ", no instance id is specified.");
//      }
//
//      if ((instanceIds == null) && (instanceIdParam != null)) {
//          instanceIds = new ArrayList<Long>();
//          instanceIds.add(instanceIdParam);
//      }
//
//      // FIXME:  We should probably lock the load balancer here to prevent multiple updates...
//      LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
//      if (loadBalancer == null) {
//          throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + ", the load balancer was not found.");
//      }
//
//
//      // Permission check...
//      Account account = UserContext.current().getAccount();
//      if (account != null) {
//          if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
//              if (!_domainDao.isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
//                  throw new PermissionDeniedException("Failed to assign to load balancer " + loadBalancerId + ", permission denied.");
//              }
//          } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN && account.getId() != loadBalancer.getAccountId()) {
//              throw new PermissionDeniedException("Failed to assign to load balancer " + loadBalancerId + ", permission denied.");
//          }
//      }
//
//      Transaction txn = Transaction.currentTxn();
//      List<PortForwardingRuleVO> firewallRulesToApply = new ArrayList<PortForwardingRuleVO>();
//      long accountId = 0;
//      DomainRouterVO router = null;
//
//      List<LoadBalancerVMMapVO> mappedInstances = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId, false);
//      Set<Long> mappedInstanceIds = new HashSet<Long>();
//      if (mappedInstances != null) {
//          for (LoadBalancerVMMapVO mappedInstance : mappedInstances) {
//              mappedInstanceIds.add(Long.valueOf(mappedInstance.getInstanceId()));
//          }
//      }
//
//      List<Long> finalInstanceIds = new ArrayList<Long>();
//      for (Long instanceId : instanceIds) {
//          if (mappedInstanceIds.contains(instanceId)) {
//              continue;
//          } else {
//              finalInstanceIds.add(instanceId);
//          }
//
//          UserVmVO userVm = _vmDao.findById(instanceId);
//          if (userVm == null) {
//              s_logger.warn("Unable to find virtual machine with id " + instanceId);
//              throw new InvalidParameterValueException("Unable to find virtual machine with id " + instanceId);
//          } else {
//              // sanity check that the vm can be applied to the load balancer
//              ServiceOfferingVO offering = _serviceOfferingDao.findById(userVm.getServiceOfferingId());
//              if ((offering == null) || !GuestIpType.Virtualized.equals(offering.getGuestIpType())) {
//                  // we previously added these instanceIds to the loadBalancerVMMap, so remove them here as we are rejecting the API request
//                  // without actually modifying the load balancer
//                  _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, Boolean.TRUE);
//
//                  if (s_logger.isDebugEnabled()) {
//                      s_logger.debug("Unable to add virtual machine " + userVm.toString() + " to load balancer " + loadBalancerId + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
//                  }
//
//                  throw new InvalidParameterValueException("Unable to add virtual machine " + userVm.toString() + " to load balancer " + loadBalancerId + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
//              }
//          }
//
//          if (accountId == 0) {
//              accountId = userVm.getAccountId();
//          } else if (accountId != userVm.getAccountId()) {
//              s_logger.warn("guest vm " + userVm.getHostName() + " (id:" + userVm.getId() + ") belongs to account " + userVm.getAccountId()
//                      + ", previous vm in list belongs to account " + accountId);
//              throw new InvalidParameterValueException("guest vm " + userVm.getHostName() + " (id:" + userVm.getId() + ") belongs to account " + userVm.getAccountId()
//                      + ", previous vm in list belongs to account " + accountId);
//          }
//
//          DomainRouterVO nextRouter = null;
//          if (userVm.getDomainRouterId() != null) {
//              nextRouter = _routerMgr.getRouter(userVm.getDomainRouterId());
//          }
//          if (nextRouter == null) {
//              s_logger.warn("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + instanceId);
//              throw new InvalidParameterValueException("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + instanceId);
//          }
//
//          if (router == null) {
//              router = nextRouter;
//
//              // Make sure owner of router is owner of load balancer.  Since we are already checking that all VMs belong to the same router, by checking router
//              // ownership once we'll make sure all VMs belong to the owner of the load balancer.
//              if (router.getAccountId() != loadBalancer.getAccountId()) {
//                  throw new InvalidParameterValueException("guest vm " + userVm.getHostName() + " (id:" + userVm.getId() + ") does not belong to the owner of load balancer " +
//                          loadBalancer.getName() + " (owner is account id " + loadBalancer.getAccountId() + ")");
//              }
//          } else if (router.getId() != nextRouter.getId()) {
//              throw new InvalidParameterValueException("guest vm " + userVm.getHostName() + " (id:" + userVm.getId() + ") belongs to router " + nextRouter.getHostName()
//                      + ", previous vm in list belongs to router " + router.getHostName());
//          }
//
//          // check for ip address/port conflicts by checking exising forwarding and loadbalancing rules
//          String ipAddress = loadBalancer.getIpAddress();
//          String privateIpAddress = userVm.getGuestIpAddress();
//          List<PortForwardingRuleVO> existingRulesOnPubIp = _rulesDao.listIPForwarding(ipAddress);
//
//          if (existingRulesOnPubIp != null) {
//              for (PortForwardingRuleVO fwRule : existingRulesOnPubIp) {
//                  if (!(  (fwRule.isForwarding() == false) &&
//                          (fwRule.getGroupId() != null) &&
//                          (fwRule.getGroupId() == loadBalancer.getId())  )) {
//                      // if the rule is not for the current load balancer, check to see if the private IP is our target IP,
//                      // in which case we have a conflict
//                      if (fwRule.getSourcePort().equals(loadBalancer.getPublicPort())) {
//                          throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + loadBalancer.getPublicPort()
//                                  + " exists, found while trying to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ") to instance "
//                                  + userVm.getHostName() + ".");
//                      }
//                  } else if (fwRule.getDestinationIpAddress().equals(privateIpAddress) && fwRule.getDestinationPort().equals(loadBalancer.getPrivatePort()) && fwRule.isEnabled()) {
//                      // for the current load balancer, don't add the same instance to the load balancer more than once
//                      continue;
//                  }
//              }
//          }
//
//          PortForwardingRuleVO newFwRule = new PortForwardingRuleVO();
//          newFwRule.setAlgorithm(loadBalancer.getAlgorithm());
//          newFwRule.setEnabled(true);
//          newFwRule.setForwarding(false);
//          newFwRule.setPrivatePort(loadBalancer.getPrivatePort());
//          newFwRule.setPublicPort(loadBalancer.getPublicPort());
//          newFwRule.setPublicIpAddress(loadBalancer.getIpAddress());
//          newFwRule.setPrivateIpAddress(userVm.getGuestIpAddress());
//          newFwRule.setGroupId(loadBalancer.getId());
//
//          firewallRulesToApply.add(newFwRule);
//      }
//
//      // if there's no work to do, bail out early rather than reconfiguring the proxy with the existing rules
//      if (firewallRulesToApply.isEmpty()) {
//          return true;
//      }
//
//      //Sync on domR
//      if(router == null){
//          throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + ", the domain router was not found at " + loadBalancer.getIpAddress());
//      }
//      else{
//          cmd.synchronizeCommand("Router", router.getId());
//      }
//
//      IPAddressVO ipAddr = _ipAddressDao.findById(loadBalancer.getIpAddress());
//      List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(accountId, ipAddr.getDataCenterId(), null);
//      for (IPAddressVO ipv : ipAddrs) {
//          List<PortForwardingRuleVO> rules = _rulesDao.listIpForwardingRulesForLoadBalancers(ipv.getAddress());
//          firewallRulesToApply.addAll(rules);
//      }
//
//      txn.start();
//
//      List<PortForwardingRuleVO> updatedRules = null;
//      if (router.getState().equals(State.Starting)) {
//          // Starting is a special case...if the router is starting that means the IP address hasn't yet been assigned to the domR and the update firewall rules script will fail.
//          // In this case, just store the rules and they will be applied when the router state is resent (after the router is started).
//          updatedRules = firewallRulesToApply;
//      } else {
//          updatedRules = updateFirewallRules(loadBalancer.getIpAddress(), firewallRulesToApply, router);
//      }
//
//      // Save and create the event
//      String description;
//      String type = EventTypes.EVENT_NET_RULE_ADD;
//      String ruleName = "load balancer";
//      String level = EventVO.LEVEL_INFO;
//
//      LoadBalancerVO loadBalancerLock = null;
//      try {
//          loadBalancerLock = _loadBalancerDao.acquireInLockTable(loadBalancerId);
//          if (loadBalancerLock == null) {
//              s_logger.warn("assignToLoadBalancer: Failed to lock load balancer " + loadBalancerId + ", proceeding with updating loadBalancerVMMappings...");
//          }
//          if ((updatedRules != null) && (updatedRules.size() == firewallRulesToApply.size())) {
//              // flag the instances as mapped to the load balancer
//              for (Long addedInstanceId : finalInstanceIds) {
//                  LoadBalancerVMMapVO mappedVM = new LoadBalancerVMMapVO(loadBalancerId, addedInstanceId);
//                  _loadBalancerVMMapDao.persist(mappedVM);
//              }
//
//              /* We used to add these instances as pending when the API command is received on the server, and once they were applied,
//               * the pending status was removed.  In the 2.2 API framework, this is no longer done and instead the new mappings just
//               * need to be persisted
//              List<LoadBalancerVMMapVO> pendingMappedVMs = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId, true);
//              for (LoadBalancerVMMapVO pendingMappedVM : pendingMappedVMs) {
//                  if (instanceIds.contains(pendingMappedVM.getInstanceId())) {
//                      LoadBalancerVMMapVO pendingMappedVMForUpdate = _loadBalancerVMMapDao.createForUpdate();
//                      pendingMappedVMForUpdate.setPending(false);
//                      _loadBalancerVMMapDao.update(pendingMappedVM.getId(), pendingMappedVMForUpdate);
//                  }
//              }
//               */
//
//              for (PortForwardingRuleVO updatedRule : updatedRules) {
//                  _rulesDao.persist(updatedRule);
//
//                  description = "created new " + ruleName + " rule [" + updatedRule.getSourceIpAddress() + ":"
//                  + updatedRule.getSourcePort() + "]->[" + updatedRule.getDestinationIpAddress() + ":"
//                  + updatedRule.getDestinationPort() + "]" + " " + updatedRule.getProtocol();
//
//                  EventUtils.saveEvent(UserContext.current().getUserId(), loadBalancer.getAccountId(), level, type, description);
//              }
//              txn.commit();
//              return true;
//          } else {
//              // Remove the instanceIds from the load balancer since there was a failure.  Make sure to commit the
//              // transaction here, otherwise the act of throwing the internal error exception will cause this
//              // remove operation to be rolled back.
//              _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, null);
//              txn.commit();
//
//              s_logger.warn("Failed to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancerId + ") to guest virtual machines " + StringUtils.join(instanceIds, ","));
//              throw new CloudRuntimeException("Failed to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancerId + ") to guest virtual machine " + StringUtils.join(instanceIds, ","));
//          }
//      } finally {
//          if (loadBalancerLock != null) {
//              _loadBalancerDao.releaseFromLockTable(loadBalancerId);
//          }
//      }
//  }


//    @Override @DB
//    public LoadBalancer createLoadBalancerRule(CreateLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
//        String publicIp = cmd.getPublicIp();
//
//        // make sure ip address exists
//        IPAddressVO ipAddr = _ipAddressDao.findById(cmd.getPublicIp());
//        if (ipAddr == null) {
//            throw new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address " + publicIp);
//        }
//
//        VlanVO vlan = _vlanDao.findById(ipAddr.getVlanDbId());
//        if (vlan != null) {
//            if (!VlanType.VirtualNetwork.equals(vlan.getVlanType())) {
//                throw new InvalidParameterValueException("Unable to create load balancer rule for IP address " + publicIp + ", only VirtualNetwork type IP addresses can be used for load balancers.");
//            }
//        } // else ERROR?
//
//        // Verify input parameters
//        if ((ipAddr.getAccountId() == null) || (ipAddr.getAllocated() == null)) {
//            throw new InvalidParameterValueException("Unable to create load balancer rule, cannot find account owner for ip " + publicIp);
//        }
//
//        Account account = UserContext.current().getAccount();
//        if (account != null) {
//            if ((account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
//                if (!_domainDao.isChildDomain(account.getDomainId(), ipAddr.getDomainId())) {
//                    throw new PermissionDeniedException("Unable to create load balancer rule on IP address " + publicIp + ", permission denied.");
//                }
//            } else if (account.getId() != ipAddr.getAccountId().longValue()) {
//                throw new PermissionDeniedException("Unable to create load balancer rule, account " + account.getAccountName() + " doesn't own ip address " + publicIp);
//            }
//        }
//
//        String loadBalancerName = cmd.getLoadBalancerRuleName();
//        LoadBalancerVO existingLB = _loadBalancerDao.findByAccountAndName(ipAddr.getAccountId(), loadBalancerName);
//        if (existingLB != null) {
//            throw new InvalidParameterValueException("Unable to create load balancer rule, an existing load balancer rule with name " + loadBalancerName + " already exists.");
//        }
//
//        // validate params
//        String publicPort = cmd.getPublicPort();
//        String privatePort = cmd.getPrivatePort();
//        String algorithm = cmd.getAlgorithm();
//
//        if (!NetUtils.isValidPort(publicPort)) {
//            throw new InvalidParameterValueException("publicPort is an invalid value");
//        }
//        if (!NetUtils.isValidPort(privatePort)) {
//            throw new InvalidParameterValueException("privatePort is an invalid value");
//        }
//        if ((algorithm == null) || !NetUtils.isValidAlgorithm(algorithm)) {
//            throw new InvalidParameterValueException("Invalid algorithm");
//        }
//
//        boolean locked = false;
//        try {
//            LoadBalancerVO exitingLB = _loadBalancerDao.findByIpAddressAndPublicPort(publicIp, publicPort);
//            if (exitingLB != null) {
//                throw new InvalidParameterValueException("IP Address/public port already load balanced by an existing load balancer rule");
//            }
//
//            List<PortForwardingRuleVO> existingFwRules = _rulesDao.listIPForwarding(publicIp, publicPort, true);
//            if ((existingFwRules != null) && !existingFwRules.isEmpty()) {
//                throw new InvalidParameterValueException("IP Address (" + publicIp + ") and port (" + publicPort + ") already in use");
//            }
//
//            ipAddr = _ipAddressDao.acquireInLockTable(publicIp);
//            if (ipAddr == null) {
//                throw new PermissionDeniedException("User does not own ip address " + publicIp);
//            }
//
//            locked = true;
//
//            LoadBalancerVO loadBalancer = new LoadBalancerVO(loadBalancerName, cmd.getDescription(), ipAddr.getAccountId(), publicIp, publicPort, privatePort, algorithm);
//            loadBalancer = _loadBalancerDao.persist(loadBalancer);
//            Long id = loadBalancer.getId();
//
//            // Save off information for the event that the security group was applied
//            Long userId = UserContext.current().getUserId();
//            if (userId == null) {
//                userId = Long.valueOf(User.UID_SYSTEM);
//            }
//
//            EventVO event = new EventVO();
//            event.setUserId(userId);
//            event.setAccountId(ipAddr.getAccountId());
//            event.setType(EventTypes.EVENT_LOAD_BALANCER_CREATE);
//
//            if (id == null) {
//                event.setDescription("Failed to create load balancer " + loadBalancer.getName() + " on ip address " + publicIp + "[" + publicPort + "->" + privatePort + "]");
//                event.setLevel(EventVO.LEVEL_ERROR);
//            } else {
//                event.setDescription("Successfully created load balancer " + loadBalancer.getName() + " on ip address " + publicIp + "[" + publicPort + "->" + privatePort + "]");
//                String params = "id="+loadBalancer.getId()+"\ndcId="+ipAddr.getDataCenterId();
//                event.setParameters(params);
//                event.setLevel(EventVO.LEVEL_INFO);
//            }
//            _eventDao.persist(event);
//
//            return _loadBalancerDao.findById(id);
//        } finally {
//            if (locked) {
//                _ipAddressDao.releaseFromLockTable(publicIp);
//            }
//        }
//    }

//  @Override
//  public boolean updateLoadBalancerRules(final List<PortForwardingRuleVO> fwRules, final DomainRouterVO router, Long hostId) {
//
//      for (PortForwardingRuleVO rule : fwRules) {
//          // Determine the the VLAN ID and netmask of the rule's public IP address
//          IPAddressVO ip = _ipAddressDao.findById(rule.getSourceIpAddress());
//          VlanVO vlan = _vlanDao.findById(new Long(ip.getVlanDbId()));
//          String vlanNetmask = vlan.getVlanNetmask();
//
//          rule.setVlanNetmask(vlanNetmask);
//      }
//
//      final LoadBalancerConfigurator cfgrtr = new HAProxyConfigurator();
//      final String [] cfg = cfgrtr.generateConfiguration(fwRules);
//      final String [][] addRemoveRules = cfgrtr.generateFwRules(fwRules);
//      final LoadBalancerCfgCommand cmd = new LoadBalancerCfgCommand(cfg, addRemoveRules, router.getInstanceName(), router.getPrivateIpAddress());
//      final Answer ans = _agentMgr.easySend(hostId, cmd);
//      if (ans == null) {
//          return false;
//      } else {
//          return ans.getResult();
//      }
//  }
//    @Override @DB
//    public LoadBalancerVO updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
//        Long loadBalancerId = cmd.getId();
//        String privatePort = cmd.getPrivatePort();
//        String algorithm = cmd.getAlgorithm();
//        String name = cmd.getLoadBalancerName();
//        String description = cmd.getDescription();
//        Account account = UserContext.current().getAccount();
//
//        //Verify input parameters
//        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
//        if (loadBalancer == null) {
//            throw new InvalidParameterValueException("Unable to find load balancer rule " + loadBalancerId + " for update.");
//        }
//
//        // make sure the name's not already in use
//        if (name != null) {
//            LoadBalancerVO existingLB = _loadBalancerDao.findByAccountAndName(loadBalancer.getAccountId(), name);
//            if ((existingLB != null) && (existingLB.getId() != loadBalancer.getId())) {
//                throw new InvalidParameterValueException("Unable to update load balancer " + loadBalancer.getName() + " with new name " + name + ", the name is already in use.");
//            }
//        }
//
//        Account lbOwner = _accountDao.findById(loadBalancer.getAccountId());
//        if (lbOwner == null) {
//            throw new InvalidParameterValueException("Unable to update load balancer rule, cannot find owning account");
//        }
//
//        Long accountId = lbOwner.getId();
//        if (account != null) {
//            if (!isAdmin(account.getType())) {
//                if (account.getId() != accountId.longValue()) {
//                    throw new PermissionDeniedException("Unable to update load balancer rule, permission denied");
//                }
//            } else if (!_domainDao.isChildDomain(account.getDomainId(), lbOwner.getDomainId())) {
//                throw new PermissionDeniedException("Unable to update load balancer rule, permission denied.");
//            }
//        }
//
//        String updatedPrivatePort = ((privatePort == null) ? loadBalancer.getPrivatePort() : privatePort);
//        String updatedAlgorithm = ((algorithm == null) ? loadBalancer.getAlgorithm() : algorithm);
//        String updatedName = ((name == null) ? loadBalancer.getName() : name);
//        String updatedDescription = ((description == null) ? loadBalancer.getDescription() : description);
//
//        Transaction txn = Transaction.currentTxn();
//        try {
//            txn.start();
//            loadBalancer.setPrivatePort(updatedPrivatePort);
//            loadBalancer.setAlgorithm(updatedAlgorithm);
//            loadBalancer.setName(updatedName);
//            loadBalancer.setDescription(updatedDescription);
//            _loadBalancerDao.update(loadBalancer.getId(), loadBalancer);
//
//            List<PortForwardingRuleVO> fwRules = _firewallRulesDao.listByLoadBalancerId(loadBalancer.getId());
//            if ((fwRules != null) && !fwRules.isEmpty()) {
//                for (PortForwardingRuleVO fwRule : fwRules) {
//                    fwRule.setPrivatePort(updatedPrivatePort);
//                    fwRule.setAlgorithm(updatedAlgorithm);
//                    _firewallRulesDao.update(fwRule.getId(), fwRule);
//                }
//            }
//            txn.commit();
//        } catch (RuntimeException ex) {
//            s_logger.warn("Unhandled exception trying to update load balancer rule", ex);
//            txn.rollback();
//            throw ex;
//        } finally {
//            txn.close();
//        }
//
//        // now that the load balancer has been updated, reconfigure the HA Proxy on the router with all the LB rules 
//        List<PortForwardingRuleVO> allLbRules = new ArrayList<PortForwardingRuleVO>();
//        IPAddressVO ipAddress = _ipAddressDao.findById(loadBalancer.getIpAddress());
//        List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddress.getDataCenterId(), null);
//        for (IPAddressVO ipv : ipAddrs) {
//            List<PortForwardingRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
//            allLbRules.addAll(rules);
//        }
//
//        IPAddressVO ip = _ipAddressDao.findById(loadBalancer.getIpAddress());
//        DomainRouterVO router = _routerMgr.getRouter(ip.getAccountId(), ip.getDataCenterId());
//        updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);
//        return _loadBalancerDao.findById(loadBalancer.getId());
//    }

    @Override
    public List<UserVmVO> listLoadBalancerInstances(ListLoadBalancerRuleInstancesCmd cmd) throws PermissionDeniedException {
        Account account = UserContext.current().getCaller();
        Long loadBalancerId = cmd.getId();
        Boolean applied = cmd.isApplied();

        if (applied == null) {
            applied = Boolean.TRUE;
        }

        LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            return null;
        }

        long lbAcctId = loadBalancer.getAccountId();
        if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            Account userAccount = _accountDao.findById(lbAcctId);
            if (!_domainDao.isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
                throw new PermissionDeniedException("Invalid load balancer rule id (" + loadBalancerId + ") given, unable to list load balancer instances.");
            }
        } else if (account.getType() == Account.ACCOUNT_TYPE_NORMAL && account.getId() != lbAcctId) {
            throw new PermissionDeniedException("Unable to list load balancer instances, account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName());
        }

        List<UserVmVO> loadBalancerInstances = new ArrayList<UserVmVO>();
        List<LoadBalancerVMMapVO> vmLoadBalancerMappings = null;
        if (applied) {
            // List only the instances that have actually been applied to the load balancer (pending is false).
            vmLoadBalancerMappings = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId, false);
        } else {
            // List all instances applied, even pending ones that are currently being assigned, so that the semantics
            // of "what instances can I apply to this load balancer" are maintained.
            vmLoadBalancerMappings = _lb2VmMapDao.listByLoadBalancerId(loadBalancerId);
        }
        List<Long> appliedInstanceIdList = new ArrayList<Long>();
        if ((vmLoadBalancerMappings != null) && !vmLoadBalancerMappings.isEmpty()) {
            for (LoadBalancerVMMapVO vmLoadBalancerMapping : vmLoadBalancerMappings) {
                appliedInstanceIdList.add(vmLoadBalancerMapping.getInstanceId());
            }
        }

        IPAddressVO addr = _ipAddressDao.findById(loadBalancer.getSourceIpAddress());
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
            if (!applied && !isApplied) {
                loadBalancerInstances.add(userVm);
            } else if (applied && isApplied) {
                loadBalancerInstances.add(userVm);
            }
        }

        return loadBalancerInstances;
    }

    @Override
    public List<LoadBalancerVO> searchForLoadBalancers(ListLoadBalancerRulesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account caller = UserContext.current().getCaller();
        Account owner = null;
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        String ipString = cmd.getPublicIp();
        Ip ipAddress = null;
        if (ipString != null) {
            ipAddress = new Ip(cmd.getPublicIp());
        }
        
        if (accountName != null && domainId != null) {
            owner = _accountDao.findActiveAccount(accountName, domainId);
            if (owner == null) {
               accountId = -1L;
            }
        }
        
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
            accountId = caller.getAccountId();
        } else if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN && owner != null) {
            accountId = owner.getId();
        } else if (owner != null && caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN){
            _accountMgr.checkAccess(caller, owner);
        }

        Filter searchFilter = new Filter(LoadBalancerVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object name = cmd.getLoadBalancerRuleName();
        Object keyword = cmd.getKeyword();
        Object instanceId = cmd.getVirtualMachineId();

        SearchBuilder<LoadBalancerVO> sb = _lbDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("sourceIpAddress", sb.entity().getSourceIpAddress(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);

        if (instanceId != null) {
            SearchBuilder<LoadBalancerVMMapVO> lbVMSearch = _lb2VmMapDao.createSearchBuilder();
            lbVMSearch.and("instanceId", lbVMSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("lbVMSearch", lbVMSearch, sb.entity().getId(), lbVMSearch.entity().getLoadBalancerId(), JoinBuilder.JoinType.INNER);
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

        if (ipAddress != null) {
            sc.setParameters("sourceIpAddress", ipAddress);
        }

        if (instanceId != null) {
            sc.setJoinParameters("lbVMSearch", "instanceId", instanceId);
        }
        
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            sc.setParameters("domainId", domainId);
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

}
