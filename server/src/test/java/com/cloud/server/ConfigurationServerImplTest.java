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
package com.cloud.server;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigDepotAdmin;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationServerImplTest {

    @Mock
    private ConfigurationDao _configDao;
    @Mock
    private DataCenterDao _zoneDao;
    @Mock
    private HostPodDao _podDao;
    @Mock
    private DiskOfferingDao _diskOfferingDao;
    @Mock
    private ServiceOfferingDao _serviceOfferingDao;
    @Mock
    private NetworkOfferingDao _networkOfferingDao;
    @Mock
    private DataCenterDao _dataCenterDao;
    @Mock
    private NetworkDao _networkDao;
    @Mock
    private VlanDao _vlanDao;
    @Mock
    private DomainDao _domainDao;
    @Mock
    private AccountDao _accountDao;
    @Mock
    private ResourceCountDao _resourceCountDao;
    @Mock
    private NetworkOfferingServiceMapDao _ntwkOfferingServiceMapDao;
    @Mock
    private ConfigDepotAdmin _configDepotAdmin;
    @Mock
    private ConfigDepot _configDepot;
    @Mock
    private ConfigurationManager _configMgr;
    @Mock
    private ManagementService _mgrService;

    @InjectMocks
    private ConfigurationServerImpl configurationServer;

    @Spy
    ConfigurationServerImpl windowsImpl = new ConfigurationServerImpl() {
      protected boolean isOnWindows() {
        return true;
      }
    };

    @Spy
    ConfigurationServerImpl linuxImpl = new ConfigurationServerImpl() {
      protected boolean isOnWindows() {
        return false;
      }
    };

    @Test
    public void testWindowsScript() {
      Assert.assertTrue(windowsImpl.isOnWindows());
      Assert.assertEquals("scripts/vm/systemvm/injectkeys.py", windowsImpl.getInjectScript());

      Assert.assertFalse(linuxImpl.isOnWindows());
      Assert.assertEquals("scripts/vm/systemvm/injectkeys.sh", linuxImpl.getInjectScript());
    }

    @Test
    public void testUpdateSystemvmPassword() {
        //setup
        String realusername = System.getProperty("user.name");
        System.setProperty("user.name", "cloud");
        Mockito.when(_configDao.getValue("system.vm.random.password")).thenReturn(String.valueOf(true));
        TransactionLegacy.open("cloud");
        Mockito.when(_mgrService.generateRandomPassword()).thenReturn("randomPassword");

        //call the method to test
        configurationServer.updateSystemvmPassword();

        //verify that generateRandomPassword() is called
        Mockito.verify(_mgrService, Mockito.times(1)).generateRandomPassword();
        //teardown
        System.setProperty("user.name", realusername);
    }
}
