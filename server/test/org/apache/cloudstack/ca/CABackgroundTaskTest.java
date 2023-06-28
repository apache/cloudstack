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
import static org.apache.cloudstack.ca.CAManager.CertExpiryAlertPeriod;
import static org.apache.cloudstack.ca.CAManager.CertExpiryWarningPeriod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.security.CertUtils;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.After;
import org.junit.Assert;
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
        generateCertificate(0);
        // This is because it is using a static variable in the class. So each test contaminates the next.
        CAManagerImpl.getAlertMap().clear();

        Mockito.when(hostDao.findByIp(Mockito.anyString())).thenReturn(host);
        Mockito.when(caManager.getActiveCertificatesMap()).thenReturn(certMap);
    }

    private void generateCertificate(int validityDays) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, CertificateException, InvalidKeyException, SignatureException, OperatorCreationException {
        final KeyPair keypair = CertUtils.generateRandomKeyPair(1024);
        expiredCertificate = CertUtils.generateV3Certificate(null, keypair, keypair.getPublic(), "CN=ca", "SHA256withRSA", validityDays, null, null);
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
        Assert.assertTrue(certMap.size() == 1);
        task.runInContext();
        Assert.assertTrue(certMap.size() == 0);
    }

    @Test
    public void testNullHost() throws Exception {
        Mockito.when(hostDao.findByIp(Mockito.anyString())).thenReturn(null);
        certMap.put(hostIp, expiredCertificate);
        Assert.assertTrue(certMap.size() == 1);
        task.runInContext();
        Assert.assertTrue(certMap.size() == 0);
    }

    @Test
    public void testAutoRenewalEnabledWithNoExceptionsOnProvisioning() throws Exception {
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "true");
        Mockito.when(caManager.provisionCertificate(Mockito.any(Host.class), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(true);
        certMap.put(hostIp, expiredCertificate);
        Assert.assertTrue(certMap.size() == 1);
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(1)).provisionCertificate(host, false, null);
        verifyAlerts("Certificate auto-renewal succeeded for host.*", "Certificate auto-renew succeeded for.*");
    }

    @Test
    public void testAutoRenewalEnabledWithExceptionsOnProvisioning() throws Exception {
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "true");
        Mockito.when(caManager.provisionCertificate(Mockito.any(Host.class), Mockito.anyBoolean(), Mockito.anyString())).thenThrow(new CloudRuntimeException("some error"));
        certMap.put(hostIp, expiredCertificate);
        Assert.assertTrue(certMap.size() == 1);
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(1)).provisionCertificate(host, false, null);
        verifyAlerts("Certificate auto-renewal failed for host.*", String.format("Certificate is going to expire for.* Error in auto-renewal, failed to renew the certificate, please renew it manually. It is not valid after %s.", expiredCertificate.getNotAfter()));
    }

    @Test
    public void testFailedAutoRenew() throws Exception {
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "true");
        Mockito.when(caManager.provisionCertificate(Mockito.any(Host.class), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(false);
        certMap.put(hostIp, expiredCertificate);
        Assert.assertTrue(certMap.size() == 1);
        task.runInContext();
        verifyAlerts("Certificate auto-renewal failed for host.*", String.format("Certificate is going to expire for.* Auto-renewal failed to renew the certificate, please renew it manually. It is not valid after %s.", expiredCertificate.getNotAfter()));
    }

    @Test
    public void testAutoRenewalDisabled() throws Exception {
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "false");
        certMap.put(hostIp, expiredCertificate);
        Assert.assertTrue(certMap.size() == 1);
        // First round
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(0)).provisionCertificate(Mockito.any(Host.class), Mockito.anyBoolean(), Mockito.anyString());
        Mockito.verify(caManager, Mockito.times(1)).sendAlert(Mockito.any(Host.class), Mockito.anyString(), Mockito.anyString());
        Mockito.reset(caManager);
        // Second round
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(0)).provisionCertificate(Mockito.any(Host.class), Mockito.anyBoolean(), Mockito.anyString());
        Mockito.verify(caManager, Mockito.times(0)).sendAlert(Mockito.any(Host.class), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testAutoRenewWarning() throws Exception {
        generateCertificate(4);
        overrideDefaultConfigValue(CertExpiryAlertPeriod, "_defaultValue", "2");
        overrideDefaultConfigValue(CertExpiryWarningPeriod, "_defaultValue", "3");
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "true");
        Mockito.when(caManager.provisionCertificate(Mockito.any(Host.class), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(true);
        certMap.put(hostIp, expiredCertificate);
        Assert.assertTrue(certMap.size() == 1);
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(0)).provisionCertificate(host, false, null);
        verifyAlerts("Certificate expiring soon for.*", "Certificate is going to expire for.*It will auto renew on.*");
    }

    @Test
    public void testExpirationWarning() throws Exception {
        generateCertificate(4);
        overrideDefaultConfigValue(CertExpiryAlertPeriod, "_defaultValue", "2");
        overrideDefaultConfigValue(CertExpiryWarningPeriod, "_defaultValue", "3");
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "false");
        Mockito.when(caManager.provisionCertificate(Mockito.any(Host.class), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(true);
        certMap.put(hostIp, expiredCertificate);
        Assert.assertTrue(certMap.size() == 1);
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(0)).provisionCertificate(host, false, null);
        verifyAlerts("Certificate expiring soon for.*", "Certificate is going to expire for.*Auto renewing is not enabled.");
    }

    @Test
    public void testExpirationAlert() throws Exception {
        generateCertificate(4);
        overrideDefaultConfigValue(CertExpiryAlertPeriod, "_defaultValue", "5");
        overrideDefaultConfigValue(CertExpiryWarningPeriod, "_defaultValue", "3");
        overrideDefaultConfigValue(AutomaticCertRenewal, "_defaultValue", "false");
        Mockito.when(caManager.provisionCertificate(Mockito.any(Host.class), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(true);
        certMap.put(hostIp, expiredCertificate);
        Assert.assertTrue(certMap.size() == 1);
        task.runInContext();
        Mockito.verify(caManager, Mockito.times(0)).provisionCertificate(host, false, null);
        verifyAlerts("Certificate expiring soon for.*", String.format("Certificate is going to expire for.*Please manually renew it since auto-renew is disabled. It is not valid after %s.", expiredCertificate.getNotAfter()));
    }

    @Test
    public void testGetDelay() throws Exception {
        Assert.assertTrue(task.getDelay() == CAManager.CABackgroundJobDelay.value() * 1000L);
    }

    private void verifyAlerts(String subjectRegex, String messageRegex) {
        Mockito.verify(caManager, Mockito.times(1)).sendAlert(Mockito.any(Host.class),
                Mockito.matches(subjectRegex),
                Mockito.matches(messageRegex));
    }

}