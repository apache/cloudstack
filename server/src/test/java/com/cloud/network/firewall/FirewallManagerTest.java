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

package com.cloud.network.firewall;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkRuleApplier;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.utils.component.ComponentContext;
import junit.framework.Assert;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirewallManagerTest {
    private Logger logger = LogManager.getLogger(FirewallManagerTest.class);
    private AutoCloseable closeable;


    @Ignore("Requires database to be set up")
    @Test
    public void testInjected() {

//        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)ComponentLocator.getCurrentLocator().getManager(FirewallManager.class);
//        Assert.assertTrue(firewallMgr._firewallElements.enumeration().hasMoreElements());
//        Assert.assertTrue(firewallMgr._pfElements.enumeration().hasMoreElements());
//        Assert.assertTrue(firewallMgr._staticNatElements.enumeration().hasMoreElements());
//        Assert.assertTrue(firewallMgr._networkAclElements.enumeration().hasMoreElements());
//        Assert.assertNotNull(firewallMgr._networkModel);
//
//        Assert.assertNotNull(firewallMgr._firewallElements.get("VirtualRouter"));
//        Assert.assertNotNull(firewallMgr._firewallElements.get("VpcVirtualRouter"));
//        Assert.assertNotNull(firewallMgr._pfElements.get("VirtualRouter"));
//        Assert.assertNotNull(firewallMgr._pfElements.get("VpcVirtualRouter"));
//        Assert.assertNotNull(firewallMgr._staticNatElements.get("VirtualRouter"));
//        Assert.assertNotNull(firewallMgr._staticNatElements.get("VpcVirtualRouter"));
//        Assert.assertNotNull(firewallMgr._networkAclElements.get("VpcVirtualRouter"));
//        Assert.assertNull(firewallMgr._networkAclElements.get("VirtualRouter"));
//
//
//        Assert.assertTrue(firewallMgr._firewallElements.get("VirtualRouter") instanceof FirewallServiceProvider);
//        Assert.assertTrue(firewallMgr._pfElements.get("VirtualRouter") instanceof PortForwardingServiceProvider);
//        Assert.assertTrue(firewallMgr._staticNatElements.get("VirtualRouter") instanceof StaticNatServiceProvider);
//        Assert.assertTrue(firewallMgr._networkAclElements.get("VpcVirtualRouter") instanceof NetworkACLServiceProvider);

        logger.info("Done testing injection of service elements into firewall manager");

    }

    @Mock
    AccountManager _accountMgr;
    @Mock
    NetworkOrchestrationService _networkMgr;
    @Mock
    NetworkModel _networkModel;
    @Mock
    DomainManager _domainMgr;
    @Mock
    VpcManager _vpcMgr;
    @Mock
    IpAddressManager _ipAddrMgr;
    @Mock
    FirewallRulesDao _firewallDao;

    @InjectMocks
    FirewallManager _firewallMgr = new FirewallManagerImpl();

    @Before
    public void initMocks() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Ignore("Requires database to be set up")
    @Test
    public void testApplyRules() {
        List<FirewallRuleVO> ruleList = new ArrayList<FirewallRuleVO>();
        FirewallRuleVO rule = new FirewallRuleVO("rule1", 1, 80, "TCP", 1, 2, 1, FirewallRule.Purpose.Firewall, null, null, null, null);
        ruleList.add(rule);
        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)_firewallMgr;

        NetworkOrchestrationService netMgr = mock(NetworkOrchestrationService.class);
        IpAddressManager addrMgr = mock(IpAddressManager.class);
        firewallMgr._networkMgr = netMgr;

        try {
            firewallMgr.applyRules(ruleList, false, false);
            verify(addrMgr).applyRules(any(List.class), any(FirewallRule.Purpose.class), any(NetworkRuleApplier.class), anyBoolean());

        } catch (ResourceUnavailableException e) {
            Assert.fail("Unreachable code");
        }
    }

    @Ignore("Requires database to be set up")
    @Test
    public void testApplyFWRules() {
        List<FirewallRuleVO> ruleList = new ArrayList<FirewallRuleVO>();
        FirewallRuleVO rule = new FirewallRuleVO("rule1", 1, 80, "TCP", 1, 2, 1, FirewallRule.Purpose.Firewall, null, null, null, null);
        ruleList.add(rule);
        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)_firewallMgr;
        VirtualRouterElement virtualRouter = mock(VirtualRouterElement.class);
        VpcVirtualRouterElement vpcVirtualRouter = mock(VpcVirtualRouterElement.class);

        List<FirewallServiceProvider> fwElements = new ArrayList<FirewallServiceProvider>();
        fwElements.add(ComponentContext.inject(VirtualRouterElement.class));
        fwElements.add(ComponentContext.inject(VpcVirtualRouterElement.class));

        firewallMgr._firewallElements = fwElements;

        try {
            when(virtualRouter.applyFWRules(any(Network.class), any(List.class))).thenReturn(false);
            when(vpcVirtualRouter.applyFWRules(any(Network.class), any(List.class))).thenReturn(true);
            //Network network, Purpose purpose, List<? extends FirewallRule> rules
            firewallMgr.applyRules(mock(Network.class), Purpose.Firewall, ruleList);
            verify(vpcVirtualRouter).applyFWRules(any(Network.class), any(List.class));
            verify(virtualRouter).applyFWRules(any(Network.class), any(List.class));

        } catch (ResourceUnavailableException e) {
            Assert.fail("Unreachable code");
        }
    }

    @Test
    public void testDetectRulesConflict() {
        List<FirewallRuleVO> ruleList = new ArrayList<FirewallRuleVO>();
        FirewallRuleVO rule1 = spy(new FirewallRuleVO("rule1", 3, 500, "UDP", 1, 2, 1, Purpose.Vpn, null, null, null, null));
        FirewallRuleVO rule2 = spy(new FirewallRuleVO("rule2", 3, 1701, "UDP", 1, 2, 1, Purpose.Vpn, null, null, null, null));
        FirewallRuleVO rule3 = spy(new FirewallRuleVO("rule3", 3, 4500, "UDP", 1, 2, 1, Purpose.Vpn, null, null, null, null));

        List<String> sString = Arrays.asList("10.1.1.1/24","192.168.1.1/24");
        List<String> dString1 = Arrays.asList("10.1.1.1/25");
        List<String> dString2 = Arrays.asList("10.1.1.128/25");

        FirewallRuleVO rule4 = spy(new FirewallRuleVO("rule4", 3L, 10, 20, "TCP", 1, 2, 1, Purpose.Firewall, sString, dString1, null, null,
                null, FirewallRule.TrafficType.Egress));

        ruleList.add(rule1);
        ruleList.add(rule2);
        ruleList.add(rule3);
        ruleList.add(rule4);

        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)_firewallMgr;

        when(firewallMgr._firewallDao.listByIpAndPurposeAndNotRevoked(3,null)).thenReturn(ruleList);
        when(rule1.getId()).thenReturn(1L);
        when(rule2.getId()).thenReturn(2L);
        when(rule3.getId()).thenReturn(3L);
        when(rule4.getId()).thenReturn(4L);

        FirewallRule newRule1 = new FirewallRuleVO("newRule1", 3, 500, "TCP", 1, 2, 1, Purpose.PortForwarding, null, null, null, null);
        FirewallRule newRule2 = new FirewallRuleVO("newRule2", 3, 1701, "TCP", 1, 2, 1, Purpose.PortForwarding, null, null, null, null);
        FirewallRule newRule3 = new FirewallRuleVO("newRule3", 3, 4500, "TCP", 1, 2, 1, Purpose.PortForwarding, null, null, null, null);
        FirewallRule newRule4 = new FirewallRuleVO("newRule4", 3L, 15, 25, "TCP", 1, 2, 1, Purpose.Firewall, sString, dString2, null, null,
                null, FirewallRule.TrafficType.Egress);

        try {
            firewallMgr.detectRulesConflict(newRule1);
            firewallMgr.detectRulesConflict(newRule2);
            firewallMgr.detectRulesConflict(newRule3);
            firewallMgr.detectRulesConflict(newRule4);
        }
        catch (NetworkRuleConflictException ex) {
            Assert.fail();
        }
    }



}
