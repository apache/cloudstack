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


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkRuleApplier;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.utils.component.Adapter;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.ComponentLocator.ComponentInfo;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.testcase.ComponentSetup;
import com.cloud.utils.testcase.ComponentTestCase;

@Ignore("Requires database to be set up")
@ComponentSetup(managerName="management-server", setupXml="network-mgr-component.xml")
public class FirewallManagerTest extends ComponentTestCase {
    private static final Logger s_logger = Logger.getLogger(FirewallManagerTest.class);
    
    @Before
    public void setUp() {
        Logger componentlogger = Logger.getLogger(ComponentLocator.class);
        Logger daoLogger = Logger.getLogger(GenericDaoBase.class);
        Logger cloudLogger = Logger.getLogger("com.cloud");

        componentlogger.setLevel(Level.WARN);
        daoLogger.setLevel(Level.ERROR);
        cloudLogger.setLevel(Level.ERROR);
        s_logger.setLevel(Level.INFO);
        super.setUp();
    }
    

    @Test
    public void testInjected() {
        
        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)ComponentLocator.getCurrentLocator().getManager(FirewallManager.class);
        Assert.assertTrue(firewallMgr._firewallElements.enumeration().hasMoreElements());
        Assert.assertTrue(firewallMgr._pfElements.enumeration().hasMoreElements());
        Assert.assertTrue(firewallMgr._staticNatElements.enumeration().hasMoreElements());
        Assert.assertTrue(firewallMgr._networkAclElements.enumeration().hasMoreElements());
        Assert.assertNotNull(firewallMgr._networkModel);
        
        Assert.assertNotNull(firewallMgr._firewallElements.get("VirtualRouter"));
        Assert.assertNotNull(firewallMgr._firewallElements.get("VpcVirtualRouter"));
        Assert.assertNotNull(firewallMgr._pfElements.get("VirtualRouter"));
        Assert.assertNotNull(firewallMgr._pfElements.get("VpcVirtualRouter"));
        Assert.assertNotNull(firewallMgr._staticNatElements.get("VirtualRouter"));
        Assert.assertNotNull(firewallMgr._staticNatElements.get("VpcVirtualRouter"));
        Assert.assertNotNull(firewallMgr._networkAclElements.get("VpcVirtualRouter"));
        Assert.assertNull(firewallMgr._networkAclElements.get("VirtualRouter"));

        
        Assert.assertTrue(firewallMgr._firewallElements.get("VirtualRouter") instanceof FirewallServiceProvider);
        Assert.assertTrue(firewallMgr._pfElements.get("VirtualRouter") instanceof PortForwardingServiceProvider);
        Assert.assertTrue(firewallMgr._staticNatElements.get("VirtualRouter") instanceof StaticNatServiceProvider);
        Assert.assertTrue(firewallMgr._networkAclElements.get("VpcVirtualRouter") instanceof NetworkACLServiceProvider);
        
        s_logger.info("Done testing injection of service elements into firewall manager");

    }
    
    @Test
    public void testApplyRules() {
        List<FirewallRuleVO> ruleList = new ArrayList<FirewallRuleVO>();
        FirewallRuleVO rule = 
                new FirewallRuleVO("rule1", 1, 80, "TCP", 1, 2, 1, 
                        FirewallRule.Purpose.Firewall, null, null, null, null);
        ruleList.add(rule);
        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)ComponentLocator.getCurrentLocator().getManager(FirewallManager.class);
        
        NetworkManager netMgr = mock(NetworkManager.class);
        firewallMgr._networkMgr = netMgr;
        
        try {
            firewallMgr.applyRules(ruleList, false, false);
            verify(netMgr)
                   .applyRules(any(List.class), 
                         any(FirewallRule.Purpose.class), 
                         any(NetworkRuleApplier.class), 
                         anyBoolean());
            
        } catch (ResourceUnavailableException e) {
            Assert.fail("Unreachable code");
        }
    }
    
    @Test
    public void testApplyFWRules() {
        List<FirewallRuleVO> ruleList = new ArrayList<FirewallRuleVO>();
        FirewallRuleVO rule = 
                new FirewallRuleVO("rule1", 1, 80, "TCP", 1, 2, 1, 
                        FirewallRule.Purpose.Firewall, null, null, null, null);
        ruleList.add(rule);
        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)ComponentLocator.getCurrentLocator().getManager(FirewallManager.class);
        VirtualRouterElement virtualRouter =  
                mock(VirtualRouterElement.class);
        VpcVirtualRouterElement vpcVirtualRouter =                  
                mock(VpcVirtualRouterElement.class);
        ComponentInfo<Adapter> c1 = 
                new ComponentInfo<Adapter>("VirtualRouter", 
                        VirtualRouterElement.class, virtualRouter);
        ComponentInfo<Adapter> c2 = 
                new ComponentInfo<Adapter>("VpcVirtualRouter", 
                        VpcVirtualRouterElement.class, vpcVirtualRouter);
        List<ComponentInfo<Adapter>> adapters = 
                new ArrayList<ComponentLocator.ComponentInfo<Adapter>>();
        adapters.add(c1);
        adapters.add(c2);
        Adapters<FirewallServiceProvider> fwElements = 
                new Adapters<FirewallServiceProvider>("firewalElements", adapters);
        firewallMgr._firewallElements = fwElements;
        
        try {
            when(
                  virtualRouter.applyFWRules(any(Network.class), any(List.class))
                ).thenReturn(false);
            when(
                    vpcVirtualRouter.applyFWRules(any(Network.class), any(List.class))
                  ).thenReturn(true);
            //Network network, Purpose purpose, List<? extends FirewallRule> rules
            firewallMgr.applyRules(mock(Network.class), Purpose.Firewall, ruleList);
            verify(vpcVirtualRouter).applyFWRules(any(Network.class), any(List.class));
            verify(virtualRouter).applyFWRules(any(Network.class), any(List.class));

            
        } catch (ResourceUnavailableException e) {
            Assert.fail("Unreachable code");
        }
    }

}
