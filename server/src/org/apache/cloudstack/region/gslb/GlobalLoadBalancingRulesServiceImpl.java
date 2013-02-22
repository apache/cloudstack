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

import com.cloud.agent.api.routing.GlobalLoadBalancerConfigCommand;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
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
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.List;

@Component
@Local(value = {GlobalLoadBalancingRulesService.class})
public class GlobalLoadBalancingRulesServiceImpl implements GlobalLoadBalancingRulesService {

    @Inject
    AccountManager _accountMgr;
    @Inject
    GlobalLoadBalancerDao _globalLoadBalancerDao;
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

        Account gslbOwner = _accountMgr.getAccount(newGslb.getEntityOwnerId());

        if (!GlobalLoadBalancerRule.Algorithm.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterSpecException("Invalid Algorithm: " + algorithm);
        }

        if (!GlobalLoadBalancerRule.Persistence.isValidPersistence(stickyMethod)) {
            throw new InvalidParameterSpecException("Invalid persistence: " + stickyMethod);
        }

        List<GlobalLoadBalancerRuleVO> gslbRules = _globalLoadBalancerDao.listByDomainName(domainName);
        if (gslbRules != null && !gslbRules.isEmpty()) {
            throw new InvalidParameterSpecException("There exist a GSLB rule with that conflicts with domain name : "
                    + domainName + " provided");
        }

        if (!NetUtils.verifyDomainName(domainName)){
            throw new InvalidParameterSpecException("Invalid domain name : " + domainName);
        }

        Region region = _regionDao.findById(regionId);
        if (region == null) {
            throw new InvalidParameterSpecException("Invalid region ID: " + region.getName());
        }

        if (!region.checkIfServiceEnabled(Region.Service.Gslb)) {
            throw new InvalidParameterValueException("GSLB service is not enabled in region : " + region.getName());
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        GlobalLoadBalancerRuleVO newGslbRule = new GlobalLoadBalancerRuleVO(name, description, domainName, algorithm,
                stickyMethod, regionId, gslbOwner.getId(), gslbOwner.getDomainId(), GlobalLoadBalancerRule.State.Staged);
        _globalLoadBalancerDao.persist(newGslbRule);
        txn.commit();
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
        List<Long> lbRuleIds = assignToGslbCmd.getLoadBalancerRulesIds();

        GlobalLoadBalancerRuleVO gslbRule = _globalLoadBalancerDao.findById(gslbRuleId);
        if (gslbRule == null) {
            throw new InvalidParameterValueException("Invalid GSLB rule id: " + gslbRuleId);
        }

        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ModifyEntry, true, gslbRule);

        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Revoke) {
            throw new InvalidParameterValueException("GSLB rule id: " + gslbRuleId + " is in revoked state");
        }

        /* check each of the load balance rule id is
         * valid ID
         * caller has access to the rule
         * check rule is not revoked
         * no two rules are in same zone
         */
        if (lbRuleIds != null) {
            List<Long> zones = new ArrayList<Long>();
            for (Long lbRuleId : lbRuleIds) {
                LoadBalancerVO loadBalancer = _lbDao.findById(lbRuleId);
                if (loadBalancer == null) {
                    throw new InvalidParameterValueException("Invalid load balancer ID " + lbRuleId);
                }

                _accountMgr.checkAccess(caller, null, true, loadBalancer);

                if (loadBalancer.getState() == LoadBalancer.State.Revoke) {
                    throw new InvalidParameterValueException("Load balancer ID " + lbRuleId + " is in revoke state");
                }
                Network network = _networkDao.findById(loadBalancer.getNetworkId());
                if (zones.contains(network.getDataCenterId())) {
                    throw new InvalidParameterValueException("Each load balancer rule specified should be in unique zone");
                }
                zones.add(network.getDataCenterId());
            }
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        // get the persisted list of LB rules that are assigned to GSLB rule
        List<GlobalLoadBalancerLbRuleMapVO> gslbMapVos = _gslbLbMapDao.listByGslbRuleId(gslbRuleId);

        // compare intended list of lb rules with earlier assigned lb rules, to figure mapping that needs to go
        if (gslbMapVos != null) {
            for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbMapVos) {
                if (lbRuleIds == null || !lbRuleIds.contains(gslbLbMapVo.getLoadBalancerId())) {
                    _gslbLbMapDao.remove(gslbLbMapVo.getId());
                }
            }
        }

        // persist the mapping of new Lb rule that needs to assigned to a gslb rule
        if (lbRuleIds != null) {
            for (Long lbRuleId : lbRuleIds) {
                boolean  mappingExists = false;
                for (GlobalLoadBalancerLbRuleMapVO gslbLbMapVo : gslbMapVos) {
                    if (gslbLbMapVo.getGslbLoadBalancerId() == lbRuleId) {
                        mappingExists = true;
                        break;
                    }
                }
                if (!mappingExists) {
                    GlobalLoadBalancerLbRuleMapVO newGslbLbMap = new GlobalLoadBalancerLbRuleMapVO();
                    newGslbLbMap.setGslbLoadBalancerId(gslbRuleId);
                    newGslbLbMap.setLoadBalancerId(lbRuleId);
                    _gslbLbMapDao.persist(newGslbLbMap);
                }
            }
        }

        // mark the rule state as add
        if (gslbRule.getState() == GlobalLoadBalancerRule.State.Staged) {
            gslbRule.setState(GlobalLoadBalancerRule.State.Add);
            _globalLoadBalancerDao.update(gslbRule.getId(), gslbRule);
        }

        txn.commit();

        // apply rules on each of the GSLB providers at each site participating in the global load balancing rule

        // on success set state to Active

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

    @Override
    public boolean removeFromGlobalLoadBalancerRule(RemoveFromGlobalLoadBalancerRuleCmd removeFromGslbCmd) {
        return false;
    }

    private void deployGlobalLoadBalancerRule(GlobalLoadBalancerConfigCommand cmd) {

    }

    private void configureZoneGslbProvider(long zoneId, GlobalLoadBalancerConfigCommand cmd) {

    }
}