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
package org.apache.cloudstack.lb;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.apache.cloudstack.network.lb.ApplicationLoadBalancerManagerImpl;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerRule;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.network.lb.dao.ApplicationLoadBalancerRuleDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

/**
 * This class is responsible for unittesting the methods defined in ApplicationLoadBalancerService
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/appLoadBalancer.xml")
public class ApplicationLoadBalancerTest extends TestCase{
    //The interface to test
    @Inject ApplicationLoadBalancerManagerImpl _appLbSvc;
    
    //The interfaces below are mocked
    @Inject ApplicationLoadBalancerRuleDao _lbDao;
    @Inject LoadBalancingRulesManager _lbMgr;
    @Inject NetworkModel _ntwkModel;
    @Inject AccountManager _accountMgr;
    @Inject FirewallRulesDao _firewallDao;
    @Inject UsageEventDao _usageEventDao;

    
    public static long existingLbId = 1L;
    public static long nonExistingLbId = 2L;
    
    public static long validNetworkId = 1L;
    public static long invalidNetworkId = 2L;
    
    public static long validAccountId = 1L;
    public static long invalidAccountId = 2L;


    
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        //mockito for .getApplicationLoadBalancer tests
        Mockito.when(_lbDao.findById(1L)).thenReturn(new ApplicationLoadBalancerRuleVO());
        Mockito.when(_lbDao.findById(2L)).thenReturn(null);
        
        //mockito for .deleteApplicationLoadBalancer tests
        Mockito.when(_lbMgr.deleteLoadBalancerRule(existingLbId, true)).thenReturn(true);
        Mockito.when(_lbMgr.deleteLoadBalancerRule(nonExistingLbId, true)).thenReturn(false);
        
        //mockito for .createApplicationLoadBalancer tests
        NetworkVO network = new NetworkVO(TrafficType.Guest, null, null, 1,
                null, 1, 1L); 
   
        Mockito.when(_ntwkModel.getNetwork(validNetworkId)).thenReturn(network);
        Mockito.when(_ntwkModel.getNetwork(invalidNetworkId)).thenReturn(null);
        Mockito.when(_accountMgr.getAccount(validAccountId)).thenReturn(new AccountVO());
        Mockito.when(_accountMgr.getAccount(invalidAccountId)).thenReturn(null);
        Mockito.when(_ntwkModel.areServicesSupportedInNetwork(validNetworkId, Service.Lb)).thenReturn(true);
        Mockito.when(_ntwkModel.areServicesSupportedInNetwork(invalidNetworkId, Service.Lb)).thenReturn(false);
        
        String supportedProtocols = NetUtils.TCP_PROTO.toLowerCase();
        Map<Network.Capability, String> capsMap = new HashMap<Network.Capability, String>();
        capsMap.put(Capability.SupportedProtocols, supportedProtocols);
        Mockito.when(_ntwkModel.getNetworkServiceCapabilities(validNetworkId, Service.Lb)).thenReturn(capsMap);
        
        ApplicationLoadBalancerRuleVO lbRule = new ApplicationLoadBalancerRuleVO("new", "new", 22, 22, "roundrobin",
                validNetworkId, validAccountId, 1L, new Ip("10.1.1.1"), validNetworkId, Scheme.Internal);
        Mockito.when(_lbDao.persist(Mockito.any(ApplicationLoadBalancerRuleVO.class))).thenReturn(lbRule);
        
        Mockito.when(_lbMgr.validateLbRule(Mockito.any(LoadBalancingRule.class))).thenReturn(true);
        
        Mockito.when(_firewallDao.setStateToAdd(Mockito.any(FirewallRuleVO.class))).thenReturn(true);
        
        Mockito.when(_accountMgr.getSystemUser()).thenReturn(new UserVO(1));
        Mockito.when(_accountMgr.getSystemAccount()).thenReturn(new AccountVO(2));
        UserContext.registerContext(_accountMgr.getSystemUser().getId(), _accountMgr.getSystemAccount(), null, false);
        
    }
    
    /**
     * TESTS FOR .getApplicationLoadBalancer
     */
    
    @Test
    //Positive test - retrieve existing lb
    public void searchForExistingLoadBalancer() {
        ApplicationLoadBalancerRule rule = _appLbSvc.getApplicationLoadBalancer(existingLbId);
        assertNotNull("Couldn't find existing application load balancer", rule);
    }
    
    @Test
    //Negative test - try to retrieve non-existing lb
    public void searchForNonExistingLoadBalancer() {
        boolean notFound = false;
        ApplicationLoadBalancerRule rule = null;
        try {
            rule = _appLbSvc.getApplicationLoadBalancer(nonExistingLbId);
            if (rule != null) {
                notFound = false; 
            }
        } catch (InvalidParameterValueException ex) {
            notFound = true;
        }
        
        assertTrue("Found non-existing load balancer; no invalid parameter value exception was thrown", notFound);
    }
    
    /**
     * TESTS FOR .deleteApplicationLoadBalancer
     */
    
    
    @Test
    //Positive test - delete existing lb
    public void deleteExistingLoadBalancer() {
        boolean result = false; 
        
        try {
            result = _appLbSvc.deleteApplicationLoadBalancer(existingLbId);
        } finally {
            assertTrue("Couldn't delete existing application load balancer", result);   
        }
    }
    
    
    @Test
    //Negative test - try to delete non-existing lb
    public void deleteNonExistingLoadBalancer() {
        boolean result = true;
        try {
            result = _appLbSvc.deleteApplicationLoadBalancer(nonExistingLbId);
        } finally {
            assertFalse("Didn't fail when try to delete non-existing load balancer", result);
        }
    }
    
    /**
     * TESTS FOR .createApplicationLoadBalancer
     */
    //Positive test
    public void createValidLoadBalancer() {
        ApplicationLoadBalancerRule rule = null;
            
        try {
            rule = _appLbSvc.createApplicationLoadBalancer("alena", "alena", Scheme.Internal, validNetworkId, "10.1.1.1",
                            22, 22, "roundrobin", validNetworkId, validAccountId);
        } catch (InsufficientAddressCapacityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InsufficientVirtualNetworkCapcityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NetworkRuleConflictException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            //assertNotNull("Failed to create application load balancer rule", rule);
        }
    }
}
