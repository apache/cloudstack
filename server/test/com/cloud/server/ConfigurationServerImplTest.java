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

import java.io.File;
import java.io.IOException;

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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
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

    final static String TEST = "the quick brown fox jumped over the lazy dog";

    @Test(expected = IOException.class)
    public void testGetBase64KeystoreNoSuchFile() throws IOException {
        ConfigurationServerImpl.getBase64Keystore("notexisting" + System.currentTimeMillis());
    }

    @Test(expected = IOException.class)
    public void testGetBase64KeystoreTooBigFile() throws IOException {
        File temp = File.createTempFile("keystore", "");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            builder.append("way too long...\n");
        }
        FileUtils.writeStringToFile(temp, builder.toString());
        try {
            ConfigurationServerImpl.getBase64Keystore(temp.getPath());
        } finally {
            temp.delete();
        }
    }

    @Test
    public void testGetBase64Keystore() throws IOException {
        File temp = File.createTempFile("keystore", "");
        try {
            FileUtils.writeStringToFile(temp, Base64.encodeBase64String(TEST.getBytes()));
            final String keystore = ConfigurationServerImpl.getBase64Keystore(temp.getPath());
            // let's decode it to make sure it makes sense
            Base64.decodeBase64(keystore);
        } finally {
            temp.delete();
        }
    }

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
