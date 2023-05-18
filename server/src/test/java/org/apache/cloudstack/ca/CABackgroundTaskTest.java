//
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
//

package org.apache.cloudstack.ca;

import static org.apache.cloudstack.ca.CAManager.AutomaticCertRenewal;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;

import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.security.CertUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class CABackgroundTaskTest {

    @Mock
    private CAManager caManager;
    @Mock
    private HostDao hostDao;

    private String hostIp = "1.2.3.4";
    private HostVO host = new HostVO(1L, "some.host",Host.Type.Routing, hostIp, "255.255.255.0", null, null, null, null, null, null, null, null, null, null,
                       UUID.randomUUID().toString(), Status.Up, "1.0", null, null, 1L, null, 0, 0, "aa", 0, Storage.StoragePoolType.NetworkFilesystem);

    private X509Certificate expiredCertificate;
    private Map<String, X509Certificate> certMap = new HashMap<>();
    private CAManagerImpl.CABackgroundTask task;

    @Before
    public void setUp() throws Exception {
        host.setManagementServerId(ManagementServerNode.getManagementServerId());
        task = new CAManagerImpl.CABackgroundTask(caManager, hostDao);
        final KeyPair keypair = CertUtils.generateRandomKeyPair(1024);
        expiredCertificate = CertUtils.generateV3Certificate(null, keypair, keypair.getPublic(), "CN=ca", "SHA256withRSA", 0, null, null);

        Mockito.when(hostDao.findByIp(Mockito.anyString())).thenReturn(host);
        Mockito.when(caManager.getActiveCertificatesMap()).thenReturn(certMap);
    }

    @After
    public void tearDown() throws Exception {
        certMap.clear();
        Mockito.reset(caManager);
        Mockito.reset(hostDao);
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    @Test
    public void testNullCert() throws Exception {
        certMap.put(hostIp, null);
        Assume.assumeThat(certMap.size() == 1, is(true));
        task.runInContext();
        Assert.assertTrue(certMap.size() == 0);
    }

    @Test
    public void testNullHost() throws Exception {
        Mockito.when(hostDao.findByIp(Mockito.anyString())).thenReturn(null);
        certMap.put(hostIp, expiredCertificate);
        Assume.assumeThat(certMap.size() == 1, is(true));
        task.runInContext();
        Assert.assertTrue(certMap.size() == 0);
    }

    @Test
    public void testAutoRenewalEnabledWithNoExceptionsOnProvisioning() throws Exception {
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "true");
        host.setManagementServerId(ManagementServerNode.getManagementServerId());
        certMap.put(hostIp, expiredCertificate);
        Assume.assumeThat(certMap.size() == 1, is(true));
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(1)).provisionCertificate(host, false, null);
        Mockito.verify(caManager, Mockito.times(0)).sendAlert(Mockito.any(Host.class), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testAutoRenewalEnabledWithExceptionsOnProvisioning() throws Exception {
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "true");
        Mockito.when(caManager.provisionCertificate(any(Host.class), anyBoolean(), nullable(String.class))).thenThrow(new CloudRuntimeException("some error"));
        host.setManagementServerId(ManagementServerNode.getManagementServerId());
        certMap.put(hostIp, expiredCertificate);
        Assume.assumeThat(certMap.size() == 1, is(true));
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(1)).provisionCertificate(host, false, null);
        Mockito.verify(caManager, Mockito.times(1)).sendAlert(Mockito.any(Host.class), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testAutoRenewalDisabled() throws Exception {
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "false");
        certMap.put(hostIp, expiredCertificate);
        Assume.assumeThat(certMap.size() == 1, is(true));
        // First round
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(0)).provisionCertificate(Mockito.any(Host.class), anyBoolean(), Mockito.anyString());
        Mockito.verify(caManager, Mockito.times(1)).sendAlert(Mockito.any(Host.class), Mockito.anyString(), Mockito.anyString());
        Mockito.reset(caManager);
        // Second round
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(0)).provisionCertificate(Mockito.any(Host.class), anyBoolean(), Mockito.anyString());
        Mockito.verify(caManager, Mockito.times(0)).sendAlert(Mockito.any(Host.class), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testGetDelay() throws Exception {
        Assert.assertTrue(task.getDelay() == CAManager.CABackgroundJobDelay.value() * 1000L);
    }

}
