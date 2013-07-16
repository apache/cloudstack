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

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.user.loadbalancer.ListApplicationLoadBalancersCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDao;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
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
    @Inject NetworkManager _ntwkMgr;
    
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LOAD_BALANCER_CREATE, eventDescription = "creating load balancer")
    public ApplicationLoadBalancerRule createApplicationLoadBalancer(String name, String description, Scheme scheme, long sourceIpNetworkId, String sourceIp,
            int sourcePort, int instancePort, String algorithm, long networkId, long lbOwnerId) throws InsufficientAddressCapacityException,
            NetworkRuleConflictException, InsufficientVirtualNetworkCapcityException {
        
        //Validate LB rule guest network
        Network guestNtwk = _networkModel.getNetwork(networkId);
        if (guestNtwk == null || guestNtwk.getTrafficType() != TrafficType.Guest) {
            throw new InvalidParameterValueException("Can't find guest network by id");
        }
        
        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, AccessType.UseNetwork, false, guestNtwk);
        
        Network sourceIpNtwk = _networkModel.getNetwork(sourceIpNetworkId);
        if (sourceIpNtwk == null) {
            throw new InvalidParameterValueException("Can't find source ip network by id");
        }
        
        Account lbOwner = _accountMgr.getAccount(lbOwnerId);
        if (lbOwner == null) {
            throw new InvalidParameterValueException("Can't find the lb owner account");
        }
        
        return createApplicationLoadBalancer(name, description, scheme, sourceIpNtwk, sourceIp, sourcePort, instancePort, algorithm, lbOwner, guestNtwk);
    }

    
    protected ApplicationLoadBalancerRule createApplicationLoadBalancer(String name, String description, Scheme scheme, Network sourceIpNtwk, String sourceIp, int sourcePort, int instancePort, String algorithm,
            Account lbOwner, Network guestNtwk) throws NetworkRuleConflictException, InsufficientVirtualNetworkCapcityException {
        
        //Only Internal scheme is supported in this release
        if (scheme != Scheme.Internal) {
            throw new UnsupportedServiceException("Only scheme of type " + Scheme.Internal + " is supported");
        }
        
        //1) Validate LB rule's parameters
        validateLbRule(sourcePort, instancePort, algorithm, guestNtwk, scheme);
        
        //2) Validate source network
        validateSourceIpNtwkForLbRule(sourceIpNtwk, scheme);
        
        //3) Get source ip address
        Ip sourceIpAddr = getSourceIp(scheme, sourceIpNtwk, sourceIp);
               
        ApplicationLoadBalancerRuleVO newRule = new ApplicationLoadBalancerRuleVO(name, description, sourcePort, instancePort, algorithm, guestNtwk.getId(),
                lbOwner.getId(), lbOwner.getDomainId(), sourceIpAddr, sourceIpNtwk.getId(), scheme);
        
        //4) Validate Load Balancing rule on the providers
        LoadBalancingRule loadBalancing = new LoadBalancingRule(newRule, new ArrayList<LbDestination>(),
                new ArrayList<LbStickinessPolicy>(), new ArrayList<LbHealthCheckPolicy>(), sourceIpAddr);
        if (!_lbMgr.validateLbRule(loadBalancing)) {
            throw new InvalidParameterValueException("LB service provider cannot support this rule");
        }

        //5) Persist Load Balancer rule
        return persistLbRule(newRule);
    }

    
    @DB
    protected ApplicationLoadBalancerRule persistLbRule(ApplicationLoadBalancerRuleVO newRule) throws NetworkRuleConflictException {
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        //1) Persist the rule
        newRule = _lbDao.persist(newRule);
        boolean success = true;

        try {
            //2) Detect conflicts
            detectLbRulesConflicts(newRule);
            if (!_firewallDao.setStateToAdd(newRule)) {
                throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
            }
            s_logger.debug("Load balancer " + newRule.getId() + " for Ip address " + newRule.getSourceIp().addr() + ", source port "
                    + newRule.getSourcePortStart() + ", instance port " + newRule.getDefaultPortStart() + " is added successfully.");
            CallContext.current().setEventDetails("Load balancer Id: " + newRule.getId());
            Network ntwk = _networkModel.getNetwork(newRule.getNetworkId());
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_LOAD_BALANCER_CREATE, newRule.getAccountId(),
                    ntwk.getDataCenterId(), newRule.getId(), null, LoadBalancingRule.class.getName(),
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
            }
        }
    }

    /**
     * Validates Lb rule parameters
     * @param sourcePort
     * @param instancePort
     * @param algorithm
     * @param network
     * @param scheme TODO
     * @param networkId
     */
    protected void validateLbRule(int sourcePort, int instancePort, String algorithm, Network network, Scheme scheme) {
        //1) verify that lb service is supported by the network
        if (!_networkModel.areServicesSupportedInNetwork(network.getId(), Service.Lb)) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "LB service is not supported in specified network id");
            ex.addProxyObject(network.getUuid(), "networkId");
            throw ex;
        }
        
        //2) verify that lb service is supported by the network
        _lbMgr.isLbServiceSupportedInNetwork(network.getId(), scheme);
        
        Map<Network.Capability, String> caps = _networkModel.getNetworkServiceCapabilities(network.getId(), Service.Lb);
        String supportedProtocols = caps.get(Capability.SupportedProtocols).toLowerCase();
        if (!supportedProtocols.contains(NetUtils.TCP_PROTO.toLowerCase())) {
            throw new InvalidParameterValueException("Protocol " + NetUtils.TCP_PROTO.toLowerCase() + " is not supported in zone " + network.getDataCenterId());
        }
        
        //3) Validate rule parameters
        if (!NetUtils.isValidPort(instancePort)) {
            throw new InvalidParameterValueException("Invalid value for instance port: " + instancePort);
        }
        
        if (!NetUtils.isValidPort(sourcePort)) {
            throw new InvalidParameterValueException("Invalid value for source port: " + sourcePort);
        }
       
        if ((algorithm == null) || !NetUtils.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterValueException("Invalid algorithm: " + algorithm);
        }
    }
    

    /**
     * Gets source ip address based on the LB rule scheme/source IP network/requested IP address
     * @param scheme
     * @param sourceIpNtwk
     * @param requestedIp
     * @return
     * @throws InsufficientVirtualNetworkCapcityException
     */
    protected Ip getSourceIp(Scheme scheme, Network sourceIpNtwk, String requestedIp) throws InsufficientVirtualNetworkCapcityException {       
        
        if (requestedIp != null) {
            if (_lbDao.countBySourceIp(new Ip(requestedIp), sourceIpNtwk.getId()) > 0)  {
                s_logger.debug("IP address " + requestedIp + " is already used by existing LB rule, returning it");
                return new Ip(requestedIp);
            }
            
            validateRequestedSourceIpForLbRule(sourceIpNtwk, new Ip(requestedIp), scheme);
        }
        
        requestedIp = allocateSourceIpForLbRule(scheme, sourceIpNtwk, requestedIp);
        
        if (requestedIp == null) {
            throw new InsufficientVirtualNetworkCapcityException("Unable to acquire IP address for network " + sourceIpNtwk, Network.class, sourceIpNtwk.getId());
        }
        return new Ip(requestedIp);
    }


    /**
     * Allocates new Source IP address for the Load Balancer rule based on LB rule scheme/sourceNetwork
     * @param scheme
     * @param sourceIpNtwk
     * @param requestedIp TODO
     * @param sourceIp
     * @return
     */
    protected String allocateSourceIpForLbRule(Scheme scheme, Network sourceIpNtwk, String requestedIp) {
        String sourceIp = null;
        if (scheme != Scheme.Internal) {
            throw new InvalidParameterValueException("Only scheme " + Scheme.Internal + " is supported");
        } else {
            sourceIp = allocateSourceIpForInternalLbRule(sourceIpNtwk, requestedIp);
        }
        return sourceIp;
    }
    

    /**
     * Allocates sourceIp for the Internal LB rule
     * @param sourceIpNtwk
     * @param requestedIp TODO
     * @return
     */
    protected String allocateSourceIpForInternalLbRule(Network sourceIpNtwk, String requestedIp) {
        return _ntwkMgr.acquireGuestIpAddress(sourceIpNtwk, requestedIp);
    }

    
    /**
     * Validates requested source ip address of the LB rule based on Lb rule scheme/sourceNetwork
     * @param sourceIpNtwk
     * @param requestedSourceIp
     * @param scheme
     */
    void validateRequestedSourceIpForLbRule(Network sourceIpNtwk, Ip requestedSourceIp, Scheme scheme) {
        //only Internal scheme is supported in this release
        if (scheme != Scheme.Internal) {
            throw new UnsupportedServiceException("Only scheme of type " + Scheme.Internal + " is supported");
        } else {
            //validate guest source ip
            validateRequestedSourceIpForInternalLbRule(sourceIpNtwk, requestedSourceIp);
        }
    }

    
    /**
     * Validates requested source IP address of Internal Lb rule against sourceNetworkId
     * @param sourceIpNtwk
     * @param requestedSourceIp
     */
    protected void validateRequestedSourceIpForInternalLbRule(Network sourceIpNtwk, Ip requestedSourceIp) {
        //Check if the IP is within the network cidr
        Pair<String, Integer> cidr = NetUtils.getCidr(sourceIpNtwk.getCidr());
        if (!NetUtils.getCidrSubNet(requestedSourceIp.addr(), cidr.second()).equalsIgnoreCase(NetUtils.getCidrSubNet(cidr.first(), cidr.second()))) {
            throw new InvalidParameterValueException("The requested IP is not in the network's CIDR subnet.");
        }
    }

    
    /**
     * Validates source IP network for the LB rule
     * @param sourceNtwk
     * @param scheme
     * @return
     */
    protected Network validateSourceIpNtwkForLbRule(Network sourceNtwk, Scheme scheme) {
        //only Internal scheme is supported in this release
        if (scheme != Scheme.Internal) {
            throw new UnsupportedServiceException("Only scheme of type " + Scheme.Internal + " is supported");
        } else {
            //validate source ip network
            return validateSourceIpNtwkForInternalLbRule(sourceNtwk);
        }
        
    }

    /**
     * Validates source IP network for the Internal LB rule
     * @param sourceIpNtwk
     * @return
     */
    protected Network validateSourceIpNtwkForInternalLbRule(Network sourceIpNtwk) {
        if (sourceIpNtwk.getTrafficType() != TrafficType.Guest) {
            throw new InvalidParameterValueException("Only traffic type " + TrafficType.Guest + " is supported");
        } 
        
        //Can't create the LB rule if the network's cidr is NULL
        String ntwkCidr = sourceIpNtwk.getCidr();
        if (ntwkCidr == null) {
            throw new InvalidParameterValueException("Can't create the application load balancer rule for the network having NULL cidr");
        }
        
        //check if the requested ip address is within the cidr
        return sourceIpNtwk;
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

        Account caller = CallContext.current().getCallingAccount();
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
    public ApplicationLoadBalancerRule getApplicationLoadBalancer(long ruleId) {
        ApplicationLoadBalancerRule lbRule = _lbDao.findById(ruleId);
        if (lbRule == null) {
            throw new InvalidParameterValueException("Can't find the load balancer by id");
        }
        return lbRule;
    }
   
    
    /**
     * Detects lb rule conflicts against other rules
     * @param newLbRule
     * @throws NetworkRuleConflictException
     */
    protected void detectLbRulesConflicts(ApplicationLoadBalancerRule newLbRule) throws NetworkRuleConflictException {
        if (newLbRule.getScheme() != Scheme.Internal) {
            throw new UnsupportedServiceException("Only scheme of type " + Scheme.Internal + " is supported");
        } else {
            detectInternalLbRulesConflict(newLbRule);
        }
    }
    
    
    /**
     * Detects Internal Lb Rules conflicts
     * @param newLbRule
     * @throws NetworkRuleConflictException
     */
    protected void detectInternalLbRulesConflict(ApplicationLoadBalancerRule newLbRule) throws NetworkRuleConflictException {
        List<ApplicationLoadBalancerRuleVO> lbRules = _lbDao.listBySourceIpAndNotRevoked(newLbRule.getSourceIp(), newLbRule.getSourceIpNetworkId());

        for (ApplicationLoadBalancerRuleVO lbRule : lbRules) {
            if (lbRule.getId() == newLbRule.getId()) {
                continue; // Skips my own rule.
            }

            if (lbRule.getNetworkId() != newLbRule.getNetworkId() && lbRule.getState() != State.Revoke) {
                throw new NetworkRuleConflictException("New rule is for a different network than what's specified in rule "
                        + lbRule.getXid());
            }

          if ((lbRule.getSourcePortStart().intValue() <= newLbRule.getSourcePortStart().intValue() 
                  && lbRule.getSourcePortEnd().intValue() >= newLbRule.getSourcePortStart().intValue())
                  || (lbRule.getSourcePortStart().intValue() <= newLbRule.getSourcePortEnd().intValue() 
                  && lbRule.getSourcePortEnd().intValue() >= newLbRule.getSourcePortEnd().intValue())
                  || (newLbRule.getSourcePortStart().intValue() <= lbRule.getSourcePortStart().intValue() 
                  && newLbRule.getSourcePortEnd().intValue() >= lbRule.getSourcePortStart().intValue())
                  || (newLbRule.getSourcePortStart().intValue() <= lbRule.getSourcePortEnd().intValue() 
                  && newLbRule.getSourcePortEnd().intValue() >= lbRule.getSourcePortEnd().intValue())) {


                    throw new NetworkRuleConflictException("The range specified, " + newLbRule.getSourcePortStart() + "-" + newLbRule.getSourcePortEnd() + ", conflicts with rule " + lbRule.getId()
                            + " which has " + lbRule.getSourcePortStart() + "-" + lbRule.getSourcePortEnd());
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("No network rule conflicts detected for " + newLbRule + " against " + (lbRules.size() - 1) + " existing rules");
        }
    }
}
