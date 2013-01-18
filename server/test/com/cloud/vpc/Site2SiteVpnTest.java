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
package com.cloud.vpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloud.configuration.DefaultInterceptorLibrary;
import com.cloud.network.dao.IPAddressDaoImpl;
import com.cloud.network.dao.Site2SiteCustomerGatewayDaoImpl;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnConnectionDaoImpl;
import com.cloud.network.dao.Site2SiteVpnGatewayDaoImpl;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.vpc.dao.VpcDaoImpl;
import com.cloud.network.vpn.Site2SiteVpnManagerImpl;
import com.cloud.user.MockAccountManagerImpl;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.MockComponentLocator;
import com.cloud.vpc.dao.MockConfigurationDaoImpl;

public class Site2SiteVpnTest {
	private MockComponentLocator locator;
    private final static Logger s_logger = Logger.getLogger(Site2SiteVpnTest.class);

	private static void addDaos(MockComponentLocator locator) {
		locator.addDao("AccountDao", AccountDaoImpl.class);
		locator.addDao("Site2SiteCustomerGatewayDao", Site2SiteCustomerGatewayDaoImpl.class);
		locator.addDao("Site2SiteVpnGatewayDao", Site2SiteVpnGatewayDaoImpl.class);
        locator.addDao("Site2SiteVpnConnectionDao", Site2SiteVpnConnectionDaoImpl.class);

		locator.addDao("IPAddressDao", IPAddressDaoImpl.class);
		locator.addDao("VpcDao", VpcDaoImpl.class);
		locator.addDao("ConfiguratioDao", MockConfigurationDaoImpl.class);

	}
	
	private static void addManagers(MockComponentLocator locator) {
		locator.addManager("AccountManager", MockAccountManagerImpl.class);
		locator.addManager("VpcManager", MockVpcManagerImpl.class);
	}
	
	@Before
	public void setUp() {
		locator = new MockComponentLocator("management-server");
		addDaos(locator);
		addManagers(locator);
		s_logger.info("Finished setUp");
	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void testInjected() throws Exception  {
		List<Pair<String, Class<? extends Site2SiteVpnServiceProvider>>> list = 
				new ArrayList<Pair<String, Class<? extends Site2SiteVpnServiceProvider>>>();
		list.add(new Pair<String, Class<? extends Site2SiteVpnServiceProvider>>("Site2SiteVpnServiceProvider", MockSite2SiteVpnServiceProvider.class));
		locator.addAdapterChain(Site2SiteVpnServiceProvider.class, list);
		s_logger.info("Finished add adapter");
		locator.makeActive(new DefaultInterceptorLibrary());
		s_logger.info("Finished make active");
		Site2SiteVpnManagerImpl vpnMgr = ComponentLocator.inject(Site2SiteVpnManagerImpl.class);
		s_logger.info("Finished inject");
		Assert.assertTrue(vpnMgr.configure("Site2SiteVpnMgr",new HashMap<String, Object>()) );
		Assert.assertTrue(vpnMgr.start());
		
	}
	

}
