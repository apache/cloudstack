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


import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.utils.component.AdapterBase;


@Ignore("Requires database to be set up")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/testContext.xml")
//@ComponentSetup(managerName="management-server", setupXml="network-mgr-component.xml")
public class NetworkManagerTest {
    private static final Logger s_logger = Logger.getLogger(NetworkManagerTest.class);
    @Inject NetworkManager _networkMgr;

    @Test
    public void testInjected() {
        NetworkManagerImpl networkMgr = (NetworkManagerImpl)_networkMgr;
        Assert.assertTrue(networkMgr._ipDeployers.iterator().hasNext());
        Assert.assertTrue(networkMgr._networkElements.iterator().hasNext());
        Assert.assertTrue(networkMgr._dhcpProviders.iterator().hasNext());
        Assert.assertNotNull(networkMgr._networkModel);

        Assert.assertNotNull(AdapterBase.getAdapterByName(networkMgr._ipDeployers, "VirtualRouter"));
        Assert.assertNotNull(AdapterBase.getAdapterByName(networkMgr._ipDeployers, "VpcVirtualRouter"));

        Assert.assertNotNull(AdapterBase.getAdapterByName(networkMgr._dhcpProviders, "VirtualRouter"));
        Assert.assertNotNull(AdapterBase.getAdapterByName(networkMgr._dhcpProviders, "VpcVirtualRouter"));


        Assert.assertTrue(AdapterBase.getAdapterByName(networkMgr._ipDeployers, "VirtualRouter") instanceof IpDeployer);
        Assert.assertTrue(AdapterBase.getAdapterByName(networkMgr._dhcpProviders, "VirtualRouter") instanceof DhcpServiceProvider);

        s_logger.info("Done testing injection of network manager's network elements");

    }

}
