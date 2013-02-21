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
import com.cloud.region.ha.GlobalLoadBalancer;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.api.command.user.region.ha.gslb.*;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.region.dao.RegionDao;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import java.security.spec.InvalidParameterSpecException;
import java.util.List;

@Component
@Local(value = {GlobalLoadBalancingRulesService.class})
public class GlobalLoadBalancingRulesServiceImpl implements GlobalLoadBalancingRulesService {

    @Inject
    AccountManager _accountMgr;
    @Inject
    GlobalLoadBalancerDao _globalLoadBalancerDao;
    @Inject
    RegionDao _regionDao;

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GLOBAL_LOAD_BALANCER_CREATE, eventDescription = "creating global load balancer")
    public GlobalLoadBalancer createGlobalLoadBalancerRule(CreateGlobalLoadBalancerRuleCmd newGslb)
            throws InvalidParameterSpecException {

        Integer regionId = newGslb.getRegionId();
        String algorithm = newGslb.getAlgorithm();
        String stickyMethod = newGslb.getStickyMethod();
        String name = newGslb.getName();
        String description = newGslb.getDescription();
        String domainName = newGslb.getServiceDomainName();

        Account gslbOwner = _accountMgr.getAccount(newGslb.getEntityOwnerId());

        if (!GlobalLoadBalancer.Algorithm.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterSpecException("Invalid Algorithm: " + algorithm);
        }

        if (!GlobalLoadBalancer.Persistence.isValidPersistence(stickyMethod)) {
            throw new InvalidParameterSpecException("Invalid persistence: " + stickyMethod);
        }

        List<GlobalLoadBalancerVO> gslbRules = _globalLoadBalancerDao.listByDomainName(domainName);
        if (gslbRules != null && !gslbRules.isEmpty()) {
            throw new InvalidParameterSpecException("There exist a GSLB rule with same domain name : " + domainName);
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

        // persist in the DB
        Transaction txn = Transaction.currentTxn();
        txn.start();
        GlobalLoadBalancerVO newGslbRule = new GlobalLoadBalancerVO(name, description, domainName, algorithm,
                stickyMethod, regionId, gslbOwner.getId());
        _globalLoadBalancerDao.persist(newGslbRule);
        txn.commit();
        return newGslbRule;
    }

    @Override
    public boolean deleteGlobalLoadBalancerRule(DeleteGlobalLoadBalancerRuleCmd deleteGslbCmd) {
        return false;
    }

    @Override
    public GlobalLoadBalancer updateGlobalLoadBalancerRule(UpdateGlobalLoadBalancerRuleCmd updateGslbCmd) {
        return null;
    }

    @Override
    public List<GlobalLoadBalancer> listGlobalLoadBalancerRule(ListGlobalLoadBalancerRuleCmd listGslbCmd) {
        return null;
    }

    @Override
    public boolean assignToGlobalLoadBalancerRule(AssignToGlobalLoadBalancerRuleCmd assignToGslbCmd) {
        return false;
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
