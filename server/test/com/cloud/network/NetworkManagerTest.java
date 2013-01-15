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

package com.cloud.network;


import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.testcase.ComponentSetup;
import com.cloud.utils.testcase.ComponentTestCase;

@Ignore("Requires database to be set up")
@ComponentSetup(managerName="management-server", setupXml="network-mgr-component.xml")
public class NetworkManagerTest extends ComponentTestCase {
    private static final Logger s_logger = Logger.getLogger(NetworkManagerTest.class);
    @Before
    @Override
    protected void setUp() {
        super.setUp();
    }
    
    @Test
    public void testInjected() {
        NetworkManagerImpl networkMgr = (NetworkManagerImpl)ComponentLocator.getCurrentLocator().getManager(NetworkManager.class);
        Assert.assertTrue(networkMgr._ipDeployers.enumeration().hasMoreElements());
        Assert.assertTrue(networkMgr._networkElements.enumeration().hasMoreElements());
        Assert.assertTrue(networkMgr._dhcpProviders.enumeration().hasMoreElements());
        Assert.assertNotNull(networkMgr._networkModel);
        
        Assert.assertNotNull(networkMgr._ipDeployers.get("VirtualRouter"));
        Assert.assertNotNull(networkMgr._ipDeployers.get("VpcVirtualRouter"));

        Assert.assertNotNull(networkMgr._dhcpProviders.get("VirtualRouter"));
        Assert.assertNotNull(networkMgr._dhcpProviders.get("VpcVirtualRouter"));

        
        Assert.assertTrue(networkMgr._ipDeployers.get("VirtualRouter") instanceof IpDeployer);
        Assert.assertTrue(networkMgr._dhcpProviders.get("VirtualRouter") instanceof DhcpServiceProvider);
       
        s_logger.info("Done testing injection of network manager's network elements");

    }

}
