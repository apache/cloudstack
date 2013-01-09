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


import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.rules.FirewallManager;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.testcase.ComponentSetup;
import com.cloud.utils.testcase.ComponentTestCase;

@ComponentSetup(managerName="management-server", setupXml="network-mgr-component.xml")
public class FirewallManagerTest extends ComponentTestCase {
    private static final Logger s_logger = Logger.getLogger(FirewallManagerTest.class);

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

}
