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

import com.cloud.agent.AgentManager;
import com.cloud.certificate.CrlVO;
import com.cloud.certificate.dao.CrlDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.framework.ca.CAProvider;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.utils.security.CertUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.host.HostVO;
import com.cloud.vm.VMInstanceVO;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.trilead.ssh2.Connection;
import com.cloud.agent.api.routing.NetworkElementCommand;
import org.apache.cloudstack.api.ApiConstants;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;

@RunWith(MockitoJUnitRunner.class)
public class CAManagerImplTest {

    @Mock
    private HostDao hostDao;
    @Mock
    private CrlDao crlDao;
    @Mock
    private AgentManager agentManager;
    @Mock
    private CAProvider caProvider;
    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private NetworkOrchestrationService networkOrchestrationService;
    @Mock
    private ConfigurationDao configDao;
    @InjectMocks
    @Spy
    private CAManagerImpl caManager = new CAManagerImpl();

    private void addField(final CAManagerImpl provider, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = CAManagerImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(provider, o);
    }

    @Before
    public void setUp() throws Exception {
        addField(caManager, "configuredCaProvider", caProvider);

        Mockito.when(caProvider.getProviderName()).thenReturn("root");
        caManager.setCaProviders(Collections.singletonList(caProvider));
    }

    @After
    public void tearDown() throws Exception {
        Mockito.reset(crlDao);
        Mockito.reset(agentManager);
        Mockito.reset(caProvider);
    }

    @Test(expected = ServerApiException.class)
    public void testIssueCertificateThrowsException() {
        caManager.issueCertificate(null, null, null, 1, null);
    }

    @Test
    public void testIssueCertificate() {
        caManager.issueCertificate(null, Collections.singletonList("domain.example"), null, 1, null);
        Mockito.verify(caProvider, Mockito.times(1)).issueCertificate(anyList(), nullable(List.class), anyInt());
        Mockito.verify(caProvider, Mockito.times(0)).issueCertificate(anyString(), anyList(), anyList(), anyInt());
    }

    @Test
    public void testRevokeCertificate() {
        final CrlVO crl = new CrlVO(CertUtils.generateRandomBigInt(), "some.domain", "some-uuid");
        Mockito.when(crlDao.revokeCertificate(Mockito.any(BigInteger.class), anyString())).thenReturn(crl);
        Mockito.when(caProvider.revokeCertificate(Mockito.any(BigInteger.class), anyString())).thenReturn(true);
        Assert.assertTrue(caManager.revokeCertificate(crl.getCertSerial(), crl.getCertCn(), null));
        Mockito.verify(caProvider, Mockito.times(1)).revokeCertificate(Mockito.any(BigInteger.class), anyString());
    }

    @Test
    public void testProvisionCertificate() throws Exception {
        final Host host = Mockito.mock(Host.class);
        Mockito.when(host.getPrivateIpAddress()).thenReturn("1.2.3.4");
        final KeyPair keyPair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate certificate = CertUtils.generateV3Certificate(null, keyPair, keyPair.getPublic(), "CN=ca", "SHA256withRSA", 365, null, null);
        Mockito.when(caProvider.issueCertificate(anyString(), anyList(), anyList(), anyInt())).thenReturn(new Certificate(certificate, null, Collections.singletonList(certificate)));
        Mockito.when(agentManager.send(anyLong(), any(SetupCertificateCommand.class))).thenReturn(new SetupCertificateAnswer(true));
        Mockito.when(agentManager.send(anyLong(), any(SetupKeyStoreCommand.class))).thenReturn(new SetupKeystoreAnswer("someCsr"));
        Mockito.doNothing().when(agentManager).reconnect(Mockito.anyLong());
        Assert.assertTrue(caManager.provisionCertificate(host, true, null, false));
        Mockito.verify(agentManager, Mockito.times(1)).send(Mockito.anyLong(), any(SetupKeyStoreCommand.class));
        Mockito.verify(agentManager, Mockito.times(1)).send(Mockito.anyLong(), any(SetupCertificateCommand.class));
        Mockito.verify(agentManager, Mockito.times(1)).reconnect(Mockito.anyLong());
    }


    @Test
    public void testProvisionCertificateForced() throws Exception {
        final Host host = Mockito.mock(Host.class);
        Mockito.doReturn(true).when(caManager).provisionCertificateForced(host, true, null);
        Assert.assertTrue(caManager.provisionCertificate(host, true, null, true));
        Mockito.verify(caManager, Mockito.times(1)).provisionCertificateForced(host, true, null);
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.anyLong(), any(SetupKeyStoreCommand.class));
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.anyLong(), any(SetupCertificateCommand.class));
    }

    @Test
    public void testIssueCertificateWithCsr() throws Exception {
        final KeyPair keyPair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate x509 = CertUtils.generateV3Certificate(null, keyPair, keyPair.getPublic(), "CN=ca", "SHA256withRSA", 365, null, null);
        Mockito.when(caProvider.issueCertificate(anyString(), anyList(), anyList(), anyInt()))
                .thenReturn(new Certificate(x509, null, Collections.singletonList(x509)));
        final Certificate result = caManager.issueCertificate("someCsr", Collections.singletonList("domain.example"), Collections.singletonList("1.2.3.4"), 365, null);
        Assert.assertNotNull(result);
        Mockito.verify(caProvider, Mockito.times(1)).issueCertificate(anyString(), anyList(), anyList(), anyInt());
        Mockito.verify(caProvider, Mockito.never()).issueCertificate(anyList(), nullable(List.class), anyInt());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testProvisionCertificateNullHost() {
        caManager.provisionCertificate(null, true, null, false);
    }

    @Test
    public void testProvisionCertificateForSystemVm() throws Exception {
        final Host host = Mockito.mock(Host.class);
        Mockito.when(host.getType()).thenReturn(Host.Type.ConsoleProxy);
        Mockito.when(host.getPrivateIpAddress()).thenReturn("1.2.3.4");
        final KeyPair keyPair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate x509 = CertUtils.generateV3Certificate(null, keyPair, keyPair.getPublic(), "CN=ca", "SHA256withRSA", 365, null, null);
        Mockito.when(caProvider.issueCertificate(anyList(), anyList(), anyInt()))
                .thenReturn(new Certificate(x509, null, Collections.singletonList(x509)));
        Mockito.when(agentManager.send(anyLong(), any(SetupCertificateCommand.class))).thenReturn(new SetupCertificateAnswer(true));
        Assert.assertTrue(caManager.provisionCertificate(host, false, null, false));
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.anyLong(), any(SetupKeyStoreCommand.class));
        Mockito.verify(agentManager, Mockito.times(1)).send(Mockito.anyLong(), any(SetupCertificateCommand.class));
        Mockito.verify(agentManager, Mockito.never()).reconnect(Mockito.anyLong());
    }

    @Test
    public void testProvisionCertificateWithoutReconnect() throws Exception {
        final Host host = Mockito.mock(Host.class);
        Mockito.when(host.getPrivateIpAddress()).thenReturn("1.2.3.4");
        final KeyPair keyPair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate x509 = CertUtils.generateV3Certificate(null, keyPair, keyPair.getPublic(), "CN=ca", "SHA256withRSA", 365, null, null);
        Mockito.when(caProvider.issueCertificate(anyString(), anyList(), anyList(), anyInt()))
                .thenReturn(new Certificate(x509, null, Collections.singletonList(x509)));
        Mockito.when(agentManager.send(anyLong(), any(SetupCertificateCommand.class))).thenReturn(new SetupCertificateAnswer(true));
        Mockito.when(agentManager.send(anyLong(), any(SetupKeyStoreCommand.class))).thenReturn(new SetupKeystoreAnswer("someCsr"));
        Assert.assertTrue(caManager.provisionCertificate(host, false, null, false));
        Mockito.verify(agentManager, Mockito.never()).reconnect(Mockito.anyLong());
    }

    @Test
    public void testRevokeCertificateReturnsFalseWhenCrlIsNull() {
        Mockito.when(crlDao.revokeCertificate(Mockito.any(BigInteger.class), anyString())).thenReturn(null);
        Assert.assertFalse(caManager.revokeCertificate(BigInteger.ONE, "some.domain", null));
        Mockito.verify(caProvider, Mockito.never()).revokeCertificate(Mockito.any(BigInteger.class), anyString());
    }

    @Test
    public void testRevokeCertificateReturnsFalseWhenSerialMismatch() {
        final CrlVO crl = new CrlVO(BigInteger.ONE, "some.domain", "some-uuid");
        Mockito.when(crlDao.revokeCertificate(Mockito.any(BigInteger.class), anyString())).thenReturn(crl);
        Assert.assertFalse(caManager.revokeCertificate(BigInteger.TWO, "some.domain", null));
        Mockito.verify(caProvider, Mockito.never()).revokeCertificate(Mockito.any(BigInteger.class), anyString());
    }

    @Test
    public void testPurgeHostCertificate() throws Exception {
        final Host host = Mockito.mock(Host.class);
        Mockito.when(host.getPrivateIpAddress()).thenReturn("10.0.0.1");
        Mockito.when(host.getPublicIpAddress()).thenReturn("192.168.0.1");
        final KeyPair keyPair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate x509 = CertUtils.generateV3Certificate(null, keyPair,
                keyPair.getPublic(), "CN=ca", "SHA256withRSA",
                365, null, null);
        caManager.getActiveCertificatesMap().put("10.0.0.1", x509);
        caManager.getActiveCertificatesMap().put("192.168.0.1", x509);
        caManager.purgeHostCertificate(host);
        Assert.assertFalse(caManager.getActiveCertificatesMap().containsKey("10.0.0.1"));
        Assert.assertFalse(caManager.getActiveCertificatesMap().containsKey("192.168.0.1"));
    }
    @Test
    public void testProvisionCertificateViaSsh() throws Exception {
        Connection sshConnection = Mockito.mock(Connection.class);
        final String agentIp = "192.168.1.1";
        final String agentHostname = "host1";
        final String caProviderStr = "root";

        try (MockedStatic<SSHCmdHelper> sshCmdHelperMock = Mockito.mockStatic(SSHCmdHelper.class)) {
            SSHCmdHelper.SSHCmdResult successResult = new SSHCmdHelper.SSHCmdResult(0, "someCsr", "");
            sshCmdHelperMock.when(() -> SSHCmdHelper.sshExecuteCmdWithResult(Mockito.eq(sshConnection), Mockito.anyString()))
                    .thenReturn(successResult);

            final KeyPair keyPair = CertUtils.generateRandomKeyPair(1024);
            final X509Certificate x509 = CertUtils.generateV3Certificate(null, keyPair, keyPair.getPublic(), "CN=ca", "SHA256withRSA", 365, null, null);
            Mockito.doReturn(new Certificate(x509, null, Collections.singletonList(x509)))
                   .when(caManager).issueCertificate(Mockito.anyString(), Mockito.anyList(), Mockito.anyList(), Mockito.nullable(Integer.class), Mockito.anyString());

            caManager.provisionCertificateViaSsh(sshConnection, agentIp, agentHostname, caProviderStr);

            sshCmdHelperMock.verify(() -> SSHCmdHelper.sshExecuteCmdWithResult(Mockito.eq(sshConnection), Mockito.contains("keystore-setup")), Mockito.times(1));
            sshCmdHelperMock.verify(() -> SSHCmdHelper.sshExecuteCmdWithResult(Mockito.eq(sshConnection), Mockito.contains("keystore-cert-import")), Mockito.times(1));
        }
    }

    @Test
    public void testProvisionKvmHostViaSsh() throws Exception {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getPrivateIpAddress()).thenReturn("192.168.1.1");
        Mockito.when(host.getName()).thenReturn("host1");
        Mockito.when(host.getClusterId()).thenReturn(1L);

        Mockito.doNothing().when(hostDao).loadDetails(host);
        Mockito.when(host.getDetail(ApiConstants.USERNAME)).thenReturn("root");
        Mockito.when(host.getDetail(ApiConstants.PASSWORD)).thenReturn("password");

        Mockito.when(configDao.getValue("ssh.privatekey")).thenReturn("privatekey");

        try (MockedConstruction<Connection> ignored = Mockito.mockConstruction(Connection.class,
                (mock, context) -> {
                    // Do nothing on connect
                });
             MockedStatic<SSHCmdHelper> sshCmdHelperMock = Mockito.mockStatic(SSHCmdHelper.class)) {
            sshCmdHelperMock.when(() -> SSHCmdHelper.acquireAuthorizedConnectionWithPublicKey(Mockito.any(Connection.class), Mockito.anyString(), Mockito.anyString()))
                    .thenReturn(true);

            Mockito.doNothing().when(caManager).provisionCertificateViaSsh(Mockito.any(Connection.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            Method method = CAManagerImpl.class.getDeclaredMethod("provisionKvmHostViaSsh", Host.class, String.class);
            method.setAccessible(true);
            boolean result = (Boolean) method.invoke(caManager, host, "root");

            Assert.assertTrue(result);
            Mockito.verify(caManager, Mockito.times(1)).provisionCertificateViaSsh(Mockito.any(Connection.class), Mockito.eq("192.168.1.1"), Mockito.eq("host1"), Mockito.eq("root"));
            sshCmdHelperMock.verify(() -> SSHCmdHelper.sshExecuteCmd(Mockito.any(Connection.class), Mockito.eq("sudo service cloudstack-agent restart")), Mockito.times(1));
        }
    }

    @Test
    public void testProvisionSystemVmViaSsh() throws Exception {
        Host host = Mockito.mock(Host.class);
        Mockito.when(host.getName()).thenReturn("v-1-VM");

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getHostId()).thenReturn(1L);
        Mockito.when(vm.getHostName()).thenReturn("host1");
        Mockito.when(vm.getInstanceName()).thenReturn("v-1-VM");
        Mockito.when(vmInstanceDao.findVMByInstanceName("v-1-VM")).thenReturn(vm);

        Map<String, String> accessDetails = new HashMap<>();
        accessDetails.put(NetworkElementCommand.ROUTER_IP, "192.168.1.2");
        Mockito.when(networkOrchestrationService.getSystemVMAccessDetails(vm)).thenReturn(accessDetails);

        HostVO hypervisorHost = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.findById(1L)).thenReturn(hypervisorHost);

        final KeyPair keyPair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate x509 = CertUtils.generateV3Certificate(null, keyPair, keyPair.getPublic(), "CN=ca", "SHA256withRSA", 365, null, null);
        Certificate cert = new Certificate(x509, null, Collections.singletonList(x509));
        Mockito.doReturn(cert)
               .when(caManager).issueCertificate(Mockito.nullable(String.class), Mockito.anyList(), Mockito.anyList(), Mockito.nullable(Integer.class), Mockito.anyString());

        Mockito.doReturn(true)
               .when(caManager).deployCertificate(Mockito.eq(hypervisorHost), Mockito.eq(cert), Mockito.anyBoolean(), Mockito.eq(accessDetails));

        Method method = CAManagerImpl.class.getDeclaredMethod("provisionSystemVmViaSsh", Host.class, Boolean.class, String.class);
        method.setAccessible(true);
        boolean result = (Boolean) method.invoke(caManager, host, true, "root");

        Assert.assertTrue(result);
        Mockito.verify(caManager, Mockito.times(1)).deployCertificate(Mockito.eq(hypervisorHost), Mockito.eq(cert), Mockito.eq(true), Mockito.eq(accessDetails));
    }
}
