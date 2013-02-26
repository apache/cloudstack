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

package org.apache.cloudstack.region.gslb;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.GlobalLoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SiteLoadBalancerConfig;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.dao.*;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.RulesManager;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.command.user.region.ha.gslb.*;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.region.dao.RegionDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Local(value = {GlobalLoadBalancingRulesService.class})
public class GlobalLoadBalancingRulesServiceImpl implements GlobalLoadBalancingRulesService {

    private static final Logger s_logger = Logger.getLogger(GlobalLoadBalancingRulesServiceImpl.class);

    @Inject
    AccountManager _accountMgr;
    @Inject
    GlobalLoadBalancerRuleDao _gslbRuleDao;
    @Inject
    GlobalLoadBalancerLbRuleMapDao _gslbLbMapDao;
    @Inject
    RegionDao _regionDao;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    ConfigurationDao _globalConfigDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AgentManager _agentMgr;

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GLOBAL_LOAD_BALANCER_CREATE, eventDescription = "creating global load balancer")
    public GlobalLoadBalancerRule createGlobalLoadBalancerRule(CreateGlobalLoadBalancerRuleCmd newGslb)
            throws InvalidParameterSpecException {

        Integer regionId = newGslb.getRegionId();
        String algorithm = newGslb.getAlgorithm();
        String stickyMethod = newGslb.getStickyMethod();
        String name = newGslb.getName();
        String description = newGslb.getDescription();
        String domainName = newGslb.getServiceDomainName();
        String serviceType = newGslb.getServiceType();

        Account gslbOwner = _accountMgr.getAccount(newGslb.getEntityOwnerId());

        if (!GlobalLoadBalancerRule.Algorithm.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterSpecException("Invalid Algorithm: " + algorithm);
        }

        if (!GlobalLoadBalancerRule.Persistence.isValidPersistence(stickyMethod)) {
            throw new InvalidParameterSpecException("Invalid persistence: " + stickyMethod);
        }

        if (!GlobalLoadBalancerRule.ServiceType.isValidServiceType(serviceType)) {
            throw new InvalidParameterSpecException("Invalid service type: " + serviceType);
        }

        List<GlobalLoadBalancerRuleVO> gslbRules = _gslbRuleDao.listByDomainName(domainName);
        if (gslbRules != null && !gslbRules.isEmpty()) {
            throw new InvalidParameterSpecException("There exist a GSLB rule with that conflicts with domain name : "
                    + domainName + " provided");
        }

        if (!NetUtils.verifyDomainName(domainName)){
            throw new InvalidParameterSpecException("Invalid domain name : " + domainName);
        }

        Region region = _regionDao.findById(regionId);
        if (region == null) {
            throw new InvalidParameterSpecException("Invalid region ID: " + regionId);
        }

        if (!region.checkIfServiceEnabled(Region.Service.Gslb)) {
            throw new InvalidParameterValueException("GSLB service is not enabled in region : " + region.getName());
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        GlobalLoadBalancerRuleVO newGslbRule = new GlobalLoadBalancerRuleVO(name, description, domainName, algorithm,
                stickyMethod, serviceType, regionId, gslbOwner.getId(), gslbOwner.getDomainId(),
                GlobalLoadBalancerRule.State.Staged);
        _gslbRuleDao.persist(newGslbRule);
        txn.commit();

        s_logger.debug("successfully create new global load balancer rule for the account " + gslbOwner.getId());
        return newGslbRule;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ASSIGN_TO_GLOBAL_LOAD_BALANCER_RULE, eventDescription =
            "Assign a load balancer rule to global load balancer rule")
    public boolean assignToGlobalLoadBalancerRule(AssignToGlobalLoadBalancerRuleCmd assignToGslbCmd) {

        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        long gslbRuleId =  assignToGslbCmd.getGlobalLoadBalancerRuleId();
        GlobalLoadBalancerRuleVO gslbRule = _gslbRuleDao.findById(gslbRuleId);
        if (gslbRule == null) {
            throw new InvalidParameterValueException("Invalid global load balancer rule id: " + gslbRuleId);
        }

        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ModifyEntry, true, gslbRule);

        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Revoke) {
            throw new InvalidParameterValueException("global load balancer rule id: " + gslbRuleId + " is in revoked state");
        }

        List<Long> newLbRuleIds = assignToGslbCmd.getLoadBalancerRulesIds();
        if (newLbRuleIds == null || newLbRuleIds.isEmpty()) {
            throw new InvalidParameterValueException("empty list of load balancer rule Ids specified to be assigned"
            + " global load balancer rule");
        }

        // get the active list of LB rules id's that are assigned currently to GSLB rule and corresponding zones
        List<Long> oldLbRuleIds = new ArrayList<Long>();
        List<Long> oldZones = new ArrayList<Long>();
        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);
        if (gslbLbMapVos != null) {
            for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbLbMapVos) {
                LoadBalancerVO loadBalancer = _lbDao.findById(gslbLbMapVo.getLoadBalancerId());
                Network network = _networkDao.findById(loadBalancer.getNetworkId());
                oldZones.add(network.getDataCenterId());
                oldLbRuleIds.add(gslbLbMapVo.getLoadBalancerId());
            }
        }

        List<Long> newZones = new ArrayList<Long>(oldZones);
        /* check each of the load balancer rule id is
         *     valid ID
         *     caller has access to the rule
         *     check rule is not revoked
         *     no two rules are in same zone
         *     rule is already assigned to gslb rule
         */
        for (Long lbRuleId : newLbRuleIds) {
            LoadBalancerVO loadBalancer = _lbDao.findById(lbRuleId);
            if (loadBalancer == null) {
                throw new InvalidParameterValueException("Specified load balancer rule ID does not exist.");
            }

            _accountMgr.checkAccess(caller, null, true, loadBalancer);

            if (loadBalancer.getState() == LoadBalancer.State.Revoke) {
                throw new InvalidParameterValueException("Load balancer ID " + loadBalancer.getUuid()  + " is in revoke state");
            }

            if (oldLbRuleIds != null && oldLbRuleIds.contains(loadBalancer.getId())) {
                throw new InvalidParameterValueException("Load balancer ID " + loadBalancer.getUuid() + " is already assigned");
            }

            Network network = _networkDao.findById(loadBalancer.getNetworkId());
            if (oldZones.contains(network.getDataCenterId())) {
                throw new InvalidParameterValueException("Each load balancer rule specified should be in unique zone");
            }
            newZones.add(network.getDataCenterId());
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        // persist the mapping for the new Lb rule that needs to assigned to a gslb rule
        for (Long lbRuleId : newLbRuleIds) {
            GlobalLoadBalancerLbRuleMapVO newGslbLbMap = new GlobalLoadBalancerLbRuleMapVO();
            newGslbLbMap.setGslbLoadBalancerId(gslbRuleId);
            newGslbLbMap.setLoadBalancerId(lbRuleId);
            _gslbLbMapDao.persist(newGslbLbMap);
        }

        // mark the gslb rule state as add
        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Staged) {
            gslbRule.setState(GlobalLoadBalancerRule.State.Add);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);
        }

        txn.commit();

        s_logger.debug("Updated the global load balancer rule: " + gslbRuleId + " in database");

        try {
            s_logger.debug("Attempting to configure global load balancer rule configuration on the gslb service providers ");
            // apply the gslb rule on to the back end gslb service providers
            applyGlobalLoadBalancerRuleConfig(gslbRuleId, false);

            // on success set state to Active
            gslbRule.setState(GlobalLoadBalancerRule.State.Active);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);
            return true;
        } catch (Exception e) {

        }

        return false;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_REMOVE_FROM_GLOBAL_LOAD_BALANCER_RULE, eventDescription =
            "Removing a load balancer rule to be part of global load balancer rule")
    public boolean removeFromGlobalLoadBalancerRule(RemoveFromGlobalLoadBalancerRuleCmd removeFromGslbCmd) {

        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        long gslbRuleId =  removeFromGslbCmd.getGlobalLoadBalancerRuleId();
        GlobalLoadBalancerRuleVO gslbRule = _gslbRuleDao.findById(gslbRuleId);
        if (gslbRule == null) {
            throw new InvalidParameterValueException("Invalid global load balancer rule id: " + gslbRuleId);
        }

        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ModifyEntry, true, gslbRule);

        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Revoke) {
            throw new InvalidParameterValueException("global load balancer rule id: " + gslbRuleId + " is already in revoked state");
        }

        List<Long> lbRuleIdsToremove = removeFromGslbCmd.getLoadBalancerRulesId();
        if (lbRuleIdsToremove == null || lbRuleIdsToremove.isEmpty()) {
            throw new InvalidParameterValueException("empty list of load balancer rule Ids specified to be un-assigned"
                    + " to global load balancer rule");
        }

        // get the active list of LB rules id's that are assigned currently to GSLB rule and corresponding zones
        List<Long> oldLbRuleIds = new ArrayList<Long>();
        List<Long> oldZones = new ArrayList<Long>();
        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);
        if (gslbLbMapVos == null) {
            throw new InvalidParameterValueException(" There are no load balancer rules that are assigned to global " +
                    " load balancer rule id: " + gslbRule.getUuid() + " that are available for deletion");
        }

        for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbLbMapVos) {
            LoadBalancerVO loadBalancer = _lbDao.findById(gslbLbMapVo.getLoadBalancerId());
            Network network = _networkDao.findById(loadBalancer.getNetworkId());
            oldZones.add(network.getDataCenterId());
            oldLbRuleIds.add(gslbLbMapVo.getLoadBalancerId());
        }

        for (Long lbRuleId : lbRuleIdsToremove) {
            LoadBalancerVO loadBalancer = _lbDao.findById(lbRuleId);
            if (loadBalancer == null) {
                throw new InvalidParameterValueException("Specified load balancer rule ID does not exist.");
            }

            _accountMgr.checkAccess(caller, null, true, loadBalancer);

            if (loadBalancer.getState() == LoadBalancer.State.Revoke) {
                throw new InvalidParameterValueException("Load balancer ID " + loadBalancer.getUuid()  + " is in revoke state");
            }

            if (oldLbRuleIds != null && !oldLbRuleIds.contains(loadBalancer.getId())) {
                throw new InvalidParameterValueException("Load balancer ID " + loadBalancer.getUuid() + " is not assigned"
                        + " to global load balancer rule: " + gslbRule.getUuid());
            }
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        // update the mapping of  gslb rule to Lb rule, to revoke state
        for (Long lbRuleId : lbRuleIdsToremove) {
            GlobalLoadBalancerLbRuleMapVO removeGslbLbMap = _gslbLbMapDao.findByGslbRuleIdAndLbRuleId(gslbRuleId, lbRuleId);
            removeGslbLbMap.setRevoke(true);
            _gslbLbMapDao.update(removeGslbLbMap.getId(), removeGslbLbMap);
        }

        // mark the gslb rule state as add
        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Staged) {
            gslbRule.setState(GlobalLoadBalancerRule.State.Active);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);
        }

        txn.commit();

        s_logger.debug("Updated the global load balancer rule: " + gslbRuleId + " in database");

        try {
            s_logger.debug("Attempting to configure global load balancer rule configuration on the gslb service providers ");

            // apply the gslb rule on to the back end gslb service providers
            applyGlobalLoadBalancerRuleConfig(gslbRuleId, false);

            // on success set state to Active
            gslbRule.setState(GlobalLoadBalancerRule.State.Add);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);

            return true;
        } catch (Exception e) {

        }

        return false;
    }

    @Override
    public boolean deleteGlobalLoadBalancerRule(DeleteGlobalLoadBalancerRuleCmd deleteGslbCmd) {

        return false;
    }

    @Override
    public GlobalLoadBalancerRule updateGlobalLoadBalancerRule(UpdateGlobalLoadBalancerRuleCmd updateGslbCmd) {
        return null;
    }

    @Override
    public List<GlobalLoadBalancerRule> listGlobalLoadBalancerRule(ListGlobalLoadBalancerRuleCmd listGslbCmd) {
        return null;
    }

    private void applyGlobalLoadBalancerRuleConfig(long gslbRuleId, boolean revoke) {

        GlobalLoadBalancerRuleVO gslbRule = _gslbRuleDao.findById(gslbRuleId);

        String domainName = gslbRule.getGslbDomain();
        String lbMethod = gslbRule.getAlgorithm();
        String persistenceMethod = gslbRule.getUuid();
        String serviceType = gslbRule.getServiceType();

        String providerDnsName = _globalConfigDao.getValue(Config.CloudDnsName.name());
        String gslbFqdn = domainName + providerDnsName;

        GlobalLoadBalancerConfigCommand gslbConfigCmd = new GlobalLoadBalancerConfigCommand(gslbFqdn,
                lbMethod, persistenceMethod, serviceType, revoke);

        List<Long> gslbSiteIds = new ArrayList<Long>();

        Map<Long, SiteLoadBalancerConfig> zoneSiteLoadbalancerMap = new HashMap<Long, SiteLoadBalancerConfig>();

        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);
        for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbLbMapVos) {
            LoadBalancerVO loadBalancer = _lbDao.findById(gslbLbMapVo.getLoadBalancerId());
            Network network = _networkDao.findById(loadBalancer.getNetworkId());

            gslbSiteIds.add(network.getDataCenterId());

            IPAddressVO ip = _ipAddressDao.findById(loadBalancer.getSourceIpAddressId());
            SiteLoadBalancerConfig siteLb = new SiteLoadBalancerConfig(gslbLbMapVo.isRevoke(), serviceType,
                    ip.getAddress().addr(), Integer.toString(loadBalancer.getDefaultPortStart()));

            zoneSiteLoadbalancerMap.put(network.getDataCenterId(), siteLb);
        }

        for (long zoneId: gslbSiteIds) {

            List<SiteLoadBalancerConfig> slbs = new ArrayList<SiteLoadBalancerConfig>();

            for (long innerLoopZoneId: gslbSiteIds) {
                SiteLoadBalancerConfig siteLb = zoneSiteLoadbalancerMap.get(innerLoopZoneId);
                siteLb.setLocal(zoneId == innerLoopZoneId);
                slbs.add(siteLb);
            }

            long zoneGslbProviderHosId = 0;

            gslbConfigCmd.setSiteLoadBalancers(slbs);
            Answer answer = _agentMgr.easySend(zoneGslbProviderHosId, gslbConfigCmd);
            if (answer == null) {

            }
        }
    }
}