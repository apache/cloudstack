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
import com.cloud.agent.api.routing.GlobalLoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SiteLoadBalancerConfig;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.command.user.region.ha.gslb.*;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.region.dao.RegionDao;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected GslbServiceProvider _gslbProvider=null;
    public void setGslbServiceProvider(GslbServiceProvider provider) {
        this._gslbProvider = provider;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GLOBAL_LOAD_BALANCER_CREATE, eventDescription = "creating global load " +
            "balancer rule", create = true)
    public GlobalLoadBalancerRule createGlobalLoadBalancerRule(CreateGlobalLoadBalancerRuleCmd newRule) {

        Integer regionId = newRule.getRegionId();
        String algorithm = newRule.getAlgorithm();
        String stickyMethod = newRule.getStickyMethod();
        String name = newRule.getName();
        String description = newRule.getDescription();
        String domainName = newRule.getServiceDomainName();
        String serviceType = newRule.getServiceType();

        Account gslbOwner = _accountMgr.getAccount(newRule.getEntityOwnerId());

        if (!GlobalLoadBalancerRule.Algorithm.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterValueException("Invalid Algorithm: " + algorithm);
        }

        if (!GlobalLoadBalancerRule.Persistence.isValidPersistence(stickyMethod)) {
            throw new InvalidParameterValueException("Invalid persistence: " + stickyMethod);
        }

        if (!GlobalLoadBalancerRule.ServiceType.isValidServiceType(serviceType)) {
            throw new InvalidParameterValueException("Invalid service type: " + serviceType);
        }

        if (!NetUtils.verifyDomainName(domainName)){
            throw new InvalidParameterValueException("Invalid domain name : " + domainName);
        }

        GlobalLoadBalancerRuleVO gslbRuleWithDomainName = _gslbRuleDao.findByDomainName(domainName);
        if (gslbRuleWithDomainName != null) {
            throw new InvalidParameterValueException("Domain name " + domainName + "is in use");
        }

        Region region = _regionDao.findById(regionId);
        if (region == null) {
            throw new InvalidParameterValueException("Invalid region ID: " + regionId);
        }

        if (!region.checkIfServiceEnabled(Region.Service.Gslb)) {
            throw new CloudRuntimeException("GSLB service is not enabled in region : " + region.getName());
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        GlobalLoadBalancerRuleVO newGslbRule = new GlobalLoadBalancerRuleVO(name, description, domainName, algorithm,
                stickyMethod, serviceType, regionId, gslbOwner.getId(), gslbOwner.getDomainId(),
                GlobalLoadBalancerRule.State.Staged);
        _gslbRuleDao.persist(newGslbRule);
        txn.commit();

        s_logger.debug("successfully created new global load balancer rule for the account " + gslbOwner.getId());

        return newGslbRule;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ASSIGN_TO_GLOBAL_LOAD_BALANCER_RULE, eventDescription =
            "Assigning a load balancer rule to global load balancer rule", async=true)
    public boolean assignToGlobalLoadBalancerRule(AssignToGlobalLoadBalancerRuleCmd assignToGslbCmd) {

        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        long gslbRuleId =  assignToGslbCmd.getGlobalLoadBalancerRuleId();
        GlobalLoadBalancerRuleVO gslbRule = _gslbRuleDao.findById(gslbRuleId);
        if (gslbRule == null) {
            throw new InvalidParameterValueException("Invalid global load balancer rule id: " + gslbRule.getUuid());
        }

        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ModifyEntry, true, gslbRule);

        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Revoke) {
            throw new InvalidParameterValueException("global load balancer rule id: " + gslbRule.getUuid()
                    + " is in revoked state");
        }

        List<Long> newLbRuleIds = assignToGslbCmd.getLoadBalancerRulesIds();
        if (newLbRuleIds == null || newLbRuleIds.isEmpty()) {
            throw new InvalidParameterValueException("empty list of load balancer rule Ids specified to be assigned"
            + " global load balancer rule");
        }

        List<Long> oldLbRuleIds = new ArrayList<Long>();
        List<Long> oldZones = new ArrayList<Long>();
        List<Long> newZones = new ArrayList<Long>(oldZones);

        // get the list of load balancer rules id's that are assigned currently to GSLB rule and corresponding zone id's
        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);
        if (gslbLbMapVos != null) {
            for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbLbMapVos) {
                LoadBalancerVO loadBalancer = _lbDao.findById(gslbLbMapVo.getLoadBalancerId());
                Network network = _networkDao.findById(loadBalancer.getNetworkId());
                oldZones.add(network.getDataCenterId());
                oldLbRuleIds.add(gslbLbMapVo.getLoadBalancerId());
            }
        }

        /* check each of the load balancer rule id passed in the 'AssignToGlobalLoadBalancerRuleCmd' command is
         *     valid ID
         *     caller has access to the rule
         *     check rule is not revoked
         *     no two rules are in same zone
         *     rule is not already assigned to gslb rule
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

            if (oldZones != null && oldZones.contains(network.getDataCenterId()) ||
                    newZones != null && newZones.contains(network.getDataCenterId())) {
                throw new InvalidParameterValueException("Load balancer rule specified should be in unique zone");
            }

            newZones.add(network.getDataCenterId());
        }

        // check each of the zone has a GSLB service provider configured
        for (Long zoneId: newZones) {
            if (!checkGslbServiceEnabledInZone(zoneId)) {
                throw new InvalidParameterValueException("GSLB service is not enabled in the Zone");
            }
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
        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Staged || gslbRule.getState() ==
                GlobalLoadBalancerRule.State.Active ) {
            gslbRule.setState(GlobalLoadBalancerRule.State.Add);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);
        }

        txn.commit();

        boolean success = false;
        try {
            s_logger.debug("Configuring gslb rule configuration on the gslb service providers in the participating zones");

            // apply the gslb rule on to the back end gslb service providers on zones participating in gslb
            applyGlobalLoadBalancerRuleConfig(gslbRuleId, false);

            // on success set state to Active
            gslbRule.setState(GlobalLoadBalancerRule.State.Active);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);

            success = true;

        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Failed to apply gslb config");
        }

        return  success;
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
            throw new InvalidParameterValueException("Invalid global load balancer rule id: " + gslbRule.getUuid());
        }

        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ModifyEntry, true, gslbRule);

        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Revoke) {
            throw new InvalidParameterValueException("global load balancer rule id: " + gslbRuleId + " is already in revoked state");
        }

        List<Long> lbRuleIdsToremove = removeFromGslbCmd.getLoadBalancerRulesIds();
        if (lbRuleIdsToremove == null || lbRuleIdsToremove.isEmpty()) {
            throw new InvalidParameterValueException("empty list of load balancer rule Ids specified to be un-assigned"
                    + " to global load balancer rule");
        }

        // get the active list of LB rule id's that are assigned currently to GSLB rule and corresponding zone id's
        List<Long> oldLbRuleIds = new ArrayList<Long>();
        List<Long> oldZones = new ArrayList<Long>();

        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);
        if (gslbLbMapVos == null) {
            throw new InvalidParameterValueException(" There are no load balancer rules that are assigned to global " +
                    " load balancer rule id: " + gslbRule.getUuid() + " that are available for deletion");
        }

        for (Long lbRuleId : lbRuleIdsToremove) {
            LoadBalancerVO loadBalancer = _lbDao.findById(lbRuleId);
            if (loadBalancer == null) {
                throw new InvalidParameterValueException("Specified load balancer rule ID does not exist.");
            }

            _accountMgr.checkAccess(caller, null, true, loadBalancer);
        }

        for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbLbMapVos) {
            LoadBalancerVO loadBalancer = _lbDao.findById(gslbLbMapVo.getLoadBalancerId());
            Network network = _networkDao.findById(loadBalancer.getNetworkId());
            oldLbRuleIds.add(gslbLbMapVo.getLoadBalancerId());
            oldZones.add(network.getDataCenterId());
        }

        for (Long lbRuleId : lbRuleIdsToremove) {
            LoadBalancerVO loadBalancer = _lbDao.findById(lbRuleId);
            if (oldLbRuleIds != null && !oldLbRuleIds.contains(loadBalancer.getId())) {
                throw new InvalidParameterValueException("Load balancer ID " + loadBalancer.getUuid() + " is not assigned"
                        + " to global load balancer rule: " + gslbRule.getUuid());
            }
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        // update the mapping of gslb rule to Lb rule, to revoke state
        for (Long lbRuleId : lbRuleIdsToremove) {
            GlobalLoadBalancerLbRuleMapVO removeGslbLbMap = _gslbLbMapDao.findByGslbRuleIdAndLbRuleId(gslbRuleId, lbRuleId);
            removeGslbLbMap.setRevoke(true);
            _gslbLbMapDao.update(removeGslbLbMap.getId(), removeGslbLbMap);
        }

        // mark the gslb rule state as add
        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Staged) {
            gslbRule.setState(GlobalLoadBalancerRule.State.Add);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);
        }

        txn.commit();

        boolean success = false;
        try {
            s_logger.debug("Attempting to configure global load balancer rule configuration on the gslb service providers ");

            // apply the gslb rule on to the back end gslb service providers
            applyGlobalLoadBalancerRuleConfig(gslbRuleId, false);

            // on success set state to Active
            gslbRule.setState(GlobalLoadBalancerRule.State.Active);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);
            success = true;
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Failed to update removed load balancer details from gloabal load balancer");
        }

        return success;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GLOBAL_LOAD_BALANCER_DELETE, eventDescription =
            "Delete global load balancer rule")
    public boolean deleteGlobalLoadBalancerRule(DeleteGlobalLoadBalancerRuleCmd deleteGslbCmd) {

        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        long gslbRuleId =  deleteGslbCmd.getGlobalLoadBalancerId();
        GlobalLoadBalancerRuleVO gslbRule = _gslbRuleDao.findById(gslbRuleId);
        if (gslbRule == null) {
            throw new InvalidParameterValueException("Invalid global load balancer rule id: " + gslbRuleId);
        }

        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ModifyEntry, true, gslbRule);

        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Revoke) {
            throw new InvalidParameterValueException("global load balancer rule id: " + gslbRuleId + " is already in revoked state");
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);
        if (gslbLbMapVos != null) {
            //mark all the GSLB-LB mapping to be in revoke state
            for (GlobalLoadBalancerLbRuleMapVO gslbLbMap : gslbLbMapVos) {
                gslbLbMap.setRevoke(true);
                _gslbLbMapDao.update(gslbLbMap.getId(), gslbLbMap);
            }
        }

        //mark the GSlb rule to be in revoke state
        gslbRule.setState(GlobalLoadBalancerRule.State.Revoke);
        _gslbRuleDao.update(gslbRuleId, gslbRule);

        txn.commit();

        boolean success = false;
        try {
            if (gslbLbMapVos != null) {
                applyGlobalLoadBalancerRuleConfig(gslbRuleId, true);
            }
            success = true;
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Failed to update the gloabal load balancer");
        }

        txn.start();
        //remove all mappings between GSLB rule and load balancer rules
        if (gslbLbMapVos != null) {
            for (GlobalLoadBalancerLbRuleMapVO gslbLbMap : gslbLbMapVos) {
                _gslbLbMapDao.remove(gslbLbMap.getId());
            }
        }
        //remove the GSLB rule itself
        _gslbRuleDao.remove(gslbRuleId);
        txn.commit();
        return success;
    }

    @Override
    public GlobalLoadBalancerRule updateGlobalLoadBalancerRule(UpdateGlobalLoadBalancerRuleCmd updateGslbCmd) {

        String algorithm = updateGslbCmd.getAlgorithm();
        String stickyMethod = updateGslbCmd.getStickyMethod();
        String description = updateGslbCmd.getDescription();

        long gslbRuleId =  updateGslbCmd.getId();
        GlobalLoadBalancerRuleVO gslbRule = _gslbRuleDao.findById(gslbRuleId);
        if (gslbRule == null) {
            throw new InvalidParameterValueException("Invalid global load balancer rule id: " + gslbRuleId);
        }

        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ModifyEntry, true, gslbRule);


        if (!GlobalLoadBalancerRule.Algorithm.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterValueException("Invalid Algorithm: " + algorithm);
        }

        if (!GlobalLoadBalancerRule.Persistence.isValidPersistence(stickyMethod)) {
            throw new InvalidParameterValueException("Invalid persistence: " + stickyMethod);
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        gslbRule.setAlgorithm(algorithm);
        gslbRule.setPersistence(stickyMethod);
        gslbRule.setDescription(description);
        _gslbRuleDao.update(gslbRule.getId(), gslbRule);
        txn.commit();

        try {
            s_logger.debug("Updated global load balancer with id " + gslbRule.getUuid());

            // apply the gslb rule on to the back end gslb service providers on zones participating in gslb
            applyGlobalLoadBalancerRuleConfig(gslbRuleId, false);

            // on success set state to Active
            gslbRule.setState(GlobalLoadBalancerRule.State.Active);
            _gslbRuleDao.update(gslbRule.getId(), gslbRule);

            return gslbRule;
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Failed to configure gslb config due to " + e.getMessage());
        }
    }

    @Override
    public List<GlobalLoadBalancerRule> listGlobalLoadBalancerRule(ListGlobalLoadBalancerRuleCmd listGslbCmd) {
        Integer regionId =  listGslbCmd.getRegionId();
        Long ruleId = listGslbCmd.getId();
        List<GlobalLoadBalancerRule> response = new ArrayList<GlobalLoadBalancerRule>();
        if (regionId == null && ruleId == null) {
            throw new InvalidParameterValueException("Invalid arguments. At least one of region id, " +
                    "rule id must be specified");
        }

        if (regionId != null && ruleId != null) {
            throw new InvalidParameterValueException("Invalid arguments. Only one of region id, " +
                    "rule id must be specified");
        }

        if (ruleId != null) {
            GlobalLoadBalancerRule gslbRule = _gslbRuleDao.findById(ruleId);
            if (gslbRule == null) {
                throw new InvalidParameterValueException("Invalid gslb rule id specified");
            }
            response.add(gslbRule);
            return response;
        }

        if (regionId != null) {
            List<GlobalLoadBalancerRuleVO> gslbRules = _gslbRuleDao.listByRegionId(regionId);
            if (gslbRules != null) {
                response.addAll(gslbRules);
            }
            return response;
        }

        return null;
    }

    @Override
    public List<LoadBalancer> listSiteLoadBalancers(long gslbRuleId) {
        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);
        List<LoadBalancer> siteLoadBalancers = new ArrayList<LoadBalancer>();
        if (gslbLbMapVos != null) {
            for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbLbMapVos) {
                LoadBalancerVO loadBalancer = _lbDao.findById(gslbLbMapVo.getLoadBalancerId());
                siteLoadBalancers.add(loadBalancer);
            }
            return siteLoadBalancers;
        }
        return null;
    }

    private boolean applyGlobalLoadBalancerRuleConfig(long gslbRuleId, boolean revoke) throws ResourceUnavailableException {

        GlobalLoadBalancerRuleVO gslbRule = _gslbRuleDao.findById(gslbRuleId);
        assert(gslbRule != null);

        String lbMethod = gslbRule.getAlgorithm();
        String persistenceMethod = gslbRule.getPersistence();
        String serviceType = gslbRule.getServiceType();

        // each Gslb rule will have a FQDN, formed from the domain name associated with the gslb rule
        // and the deployment DNS name configured in global config parameter 'cloud.dns.name'
        String domainName = gslbRule.getGslbDomain();
        String providerDnsName = _globalConfigDao.getValue(Config.CloudDnsName.key());
        String gslbFqdn = domainName + "." + providerDnsName;

        GlobalLoadBalancerConfigCommand gslbConfigCmd = new GlobalLoadBalancerConfigCommand(gslbFqdn,
                lbMethod, persistenceMethod, serviceType, gslbRuleId, revoke);

        // list of the zones participating in global load balancing
        List<Long> gslbSiteIds = new ArrayList<Long>();

        // map of the zone and info corresponding to the load balancer configured in the zone
        Map<Long, SiteLoadBalancerConfig> zoneSiteLoadbalancerMap = new HashMap<Long, SiteLoadBalancerConfig>();

        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);

        assert (gslbLbMapVos != null && !gslbLbMapVos.isEmpty());

        for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbLbMapVos) {

            // get the zone in which load balancer rule is deployed
            LoadBalancerVO loadBalancer = _lbDao.findById(gslbLbMapVo.getLoadBalancerId());
            Network network = _networkDao.findById(loadBalancer.getNetworkId());
            long dataCenterId = network.getDataCenterId();

            gslbSiteIds.add(dataCenterId);

            IPAddressVO ip = _ipAddressDao.findById(loadBalancer.getSourceIpAddressId());
            SiteLoadBalancerConfig siteLb = new SiteLoadBalancerConfig(gslbLbMapVo.isRevoke(), serviceType,
                    ip.getAddress().addr(), Integer.toString(loadBalancer.getDefaultPortStart()),
                    dataCenterId);

            siteLb.setGslbProviderPublicIp(_gslbProvider.getZoneGslbProviderPublicIp(dataCenterId));
            siteLb.setGslbProviderPrivateIp(_gslbProvider.getZoneGslbProviderPrivateIp(dataCenterId));

            zoneSiteLoadbalancerMap.put(network.getDataCenterId(), siteLb);
        }

        // loop through all the zones, participating in GSLB, and send GSLB config command
        // to the corresponding GSLB service provider in that zone
        for (long zoneId: gslbSiteIds) {

            List<SiteLoadBalancerConfig> slbs = new ArrayList<SiteLoadBalancerConfig>();

            // set site as 'local' for the site in that zone
            for (long innerLoopZoneId: gslbSiteIds) {
                SiteLoadBalancerConfig siteLb = zoneSiteLoadbalancerMap.get(innerLoopZoneId);
                siteLb.setLocal(zoneId == innerLoopZoneId);
                slbs.add(siteLb);
            }

            gslbConfigCmd.setSiteLoadBalancers(slbs);

            try {
                _gslbProvider.applyGlobalLoadBalancerRule(zoneId, gslbConfigCmd);
            } catch (ResourceUnavailableException e) {
                s_logger.warn("Failed to configure GSLB rul in the zone " + zoneId + " due to " + e.getMessage());
                throw new CloudRuntimeException("Failed to configure GSLB rul in the zone");
            }
        }

        return true;
    }

    private boolean checkGslbServiceEnabledInZone(long zoneId) {

        if (_gslbProvider == null) {
            throw new CloudRuntimeException("No GSLB provider is available");
        }

        return _gslbProvider.isServiceEnabledInZone(zoneId);
    }

    @Override
    public GlobalLoadBalancerRule findById(long gslbRuleId) {
        return _gslbRuleDao.findById(gslbRuleId);
    }
}