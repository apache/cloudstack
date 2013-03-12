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
package com.cloud.network.vpn;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;

public class RemoteAccessVpnTest {
    private final static Logger s_logger = Logger.getLogger(RemoteAccessVpnTest.class);


//	private static void addDaos(MockComponentLocator locator) {
//		locator.addDao("AccountDao", AccountDaoImpl.class);
//		locator.addDao("VpnUserDao", VpnUserDaoImpl.class);
//		locator.addDao("FirewallRulesDao", FirewallRulesDaoImpl.class);
//		locator.addDao("IPAddressDao", IPAddressDaoImpl.class);
//		locator.addDao("DomainDao", DomainDaoImpl.class);
//		locator.addDao("UsageEventDao", UsageEventDaoImpl.class);
//		locator.addDao("RemoteAccessVpnDao", RemoteAccessVpnDaoImpl.class);
//		locator.addDao("ConfigurationDao", ConfigurationDaoImpl.class);
//
//	}
//	
//	private static void addManagers(MockComponentLocator locator) {
//		locator.addManager("AccountManager", MockAccountManagerImpl.class);
//		locator.addManager("DomainManager", MockDomainManagerImpl.class);
//		locator.addManager("NetworkManager", MockNetworkManagerImpl.class);
//	    locator.addManager("NetworkModel", MockNetworkModelImpl.class);
//		locator.addManager("RulesManager", MockRulesManagerImpl.class);
//		locator.addManager("FirewallManager", MockFirewallManagerImpl.class);
//	}

//	@Before
//	public void setUp() {
//		locator = new MockComponentLocator("management-server");
//		addDaos(locator);
//		addManagers(locator);
//		s_logger.info("Finished setUp");
//	}

    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void testInjected() throws Exception  {
//		List<Pair<String, Class<? extends RemoteAccessVPNServiceProvider>>> list = 
//				new ArrayList<Pair<String, Class<? extends RemoteAccessVPNServiceProvider>>>();
//		list.add(new Pair<String, Class<? extends RemoteAccessVPNServiceProvider>>("RemoteAccessVPNServiceProvider", MockRemoteAccessVPNServiceProvider.class));
//		locator.addAdapterChain(RemoteAccessVPNServiceProvider.class, list);
//		s_logger.info("Finished add adapter");
//		locator.makeActive(new DefaultInterceptorLibrary());
//		s_logger.info("Finished make active");
//		RemoteAccessVpnManagerImpl vpnMgr = ComponentLocator.inject(RemoteAccessVpnManagerImpl.class);
//		s_logger.info("Finished inject");
//		Assert.assertTrue(vpnMgr.configure("RemoteAccessVpnMgr",new HashMap<String, Object>()) );
//		Assert.assertTrue(vpnMgr.start());
//		int numProviders = vpnMgr.getRemoteAccessVPNServiceProviders().size();
//		Assert.assertTrue(numProviders > 0);
    }


}
