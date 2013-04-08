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

package org.apache.cloudstack.network.lb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.command.user.loadbalancer.ListApplicationLoadBalancersCmd;
import org.apache.cloudstack.network.lb.dao.ApplicationLoadBalancerRuleDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

@Component
@Local(value = { ApplicationLoadBalancerService.class })
public class ApplicationLoadBalancerManagerImpl extends ManagerBase implements ApplicationLoadBalancerService {
    private static final Logger s_logger = Logger.getLogger(ApplicationLoadBalancerManagerImpl.class);
    
    @Inject NetworkModel _networkModel;
    @Inject ApplicationLoadBalancerRuleDao _lbDao;
    @Inject AccountManager _accountMgr;
    @Inject LoadBalancingRulesManager _lbMgr;
    @Inject FirewallRulesDao _firewallDao;
    @Inject ResourceTagDao _resourceTagDao;
    
    
    //TODO - add action even annotation here; test it as well
    @Override
    public ApplicationLoadBalancerRule createApplicationLoadBalancer(String name, String description, Scheme scheme, long sourceIpNetworkId, String sourceIp,
            int sourcePort, int instancePort, String algorithm, long networkId, long lbOwnerId) throws InsufficientAddressCapacityException,
            NetworkRuleConflictException {
        
        if (!NetUtils.isValidPort(instancePort)) {
            throw new InvalidParameterValueException("Invalid value for instance port: " + instancePort);
        }
        
        
        if (!NetUtils.isValidPort(instancePort)) {
            throw new InvalidParameterValueException("Invalid value for source port: " + sourcePort);
        }
       
        if ((algorithm == null) || !NetUtils.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterValueException("Invalid algorithm: " + algorithm);
        }
        
        Network network = _networkModel.getNetwork(networkId);
        // verify that lb service is supported by the network
        if (!_networkModel.areServicesSupportedInNetwork(network.getId(), Service.Lb)) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "LB service is not supported in specified network id");
            ex.addProxyObject(network, networkId, "networkId");
            throw ex;
        }
        
        Account lbOwner = _accountMgr.getAccount(lbOwnerId);
        if (lbOwner == null) {
            throw new InvalidParameterValueException("Can't find the lb owner account");
        }

        //TODO - assign guest ip address here//add validation for the source ip address/network id
        ApplicationLoadBalancerRuleVO newRule = new ApplicationLoadBalancerRuleVO(name, description, sourcePort, instancePort, algorithm, networkId,
                lbOwner.getId(), lbOwner.getDomainId(), new Ip(sourceIp), sourceIpNetworkId, scheme);
        

        // verify rule is supported by Lb provider of the network
        LoadBalancingRule loadBalancing = new LoadBalancingRule(newRule, new ArrayList<LbDestination>(),
                new ArrayList<LbStickinessPolicy>(), new ArrayList<LbHealthCheckPolicy>(), new Ip(sourceIp));
        
        if (!_lbMgr.validateLbRule(loadBalancing)) {
            throw new InvalidParameterValueException("LB service provider cannot support this rule");
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        newRule = _lbDao.persist(newRule);
        boolean success = true;

        try {
            //TODO - add validation for the LB rule against other lb rules
            
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            s_logger.debug("Load balancer " + newRule.getId() + " for Ip address " + sourceIp + ", source port "
                    + sourcePort + ", instance port " + instancePort + " is added successfully.");
            UserContext.current().setEventDetails("Load balancer Id: " + newRule.getId());
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_LOAD_BALANCER_CREATE, lbOwnerId,
                    network.getDataCenterId(), newRule.getId(), null, LoadBalancingRule.class.getName(),
                    newRule.getUuid());
            txn.commit();

            return newRule;
        } catch (Exception e) {
            success = false;
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            }
            throw new CloudRuntimeException("Unable to add lb rule for ip address " + newRule.getSourceIpAddressId(), e);
        } finally {
            if (!success && newRule != null) {
                _lbMgr.removeLBRule(newRule);
                //TODO - unassign the guest ip address here
            }
        }
    }

    @Override
    public boolean deleteApplicationLoadBalancer(long id) {
        return _lbMgr.deleteLoadBalancerRule(id, true);
    }

    @Override
    public Pair<List<? extends ApplicationLoadBalancerRule>, Integer> listApplicationLoadBalancers(ListApplicationLoadBalancersCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getLoadBalancerRuleName();
        String ip = cmd.getSourceIp();
        Long ipNtwkId = cmd.getSourceIpNetworkId();
        String keyword = cmd.getKeyword();
        Scheme scheme = cmd.getScheme();
        Long networkId = cmd.getNetworkId();
        
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

        Filter searchFilter = new Filter(ApplicationLoadBalancerRuleVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<ApplicationLoadBalancerRuleVO> sb = _lbDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("sourceIpAddress", sb.entity().getSourceIp(), SearchCriteria.Op.EQ);
        sb.and("sourceIpAddressNetworkId", sb.entity().getSourceIpNetworkId(), SearchCriteria.Op.EQ);
        sb.and("scheme", sb.entity().getScheme(), SearchCriteria.Op.EQ);
        sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        
        //list only load balancers having not null sourceIp/sourceIpNtwkId
        sb.and("sourceIpAddress", sb.entity().getSourceIp(), SearchCriteria.Op.NNULL);
        sb.and("sourceIpAddressNetworkId", sb.entity().getSourceIpNetworkId(), SearchCriteria.Op.NNULL);

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

        SearchCriteria<ApplicationLoadBalancerRuleVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<ApplicationLoadBalancerRuleVO> ssc = _lbDao.createSearchCriteria();
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
        
        if (ip != null) {
            sc.setParameters("sourceIpAddress", ip);
        }

        if (ipNtwkId != null) {
            sc.setParameters("sourceIpAddressNetworkId", ipNtwkId);
        }
        
        if (scheme != null) {
            sc.setParameters("scheme", scheme);
        }
        
        if (networkId != null) {
            sc.setParameters("networkId", networkId);    
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
        
        Pair<List<ApplicationLoadBalancerRuleVO>, Integer> result = _lbDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends ApplicationLoadBalancerRule>, Integer>(result.first(), result.second());
    }

    @Override
    public ApplicationLoadBalancerRule findById(long ruleId) {
        return _lbDao.findById(ruleId);
    }

}
