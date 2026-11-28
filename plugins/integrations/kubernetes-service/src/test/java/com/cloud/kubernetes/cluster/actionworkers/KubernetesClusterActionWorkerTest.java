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
package com.cloud.kubernetes.cluster.actionworkers;

import java.io.File;
import java.util.UUID;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterActionWorkerTest {

    @Mock
    KubernetesClusterDao kubernetesClusterDao;

    @Mock
    KubernetesClusterVmMapDao kubernetesClusterVmMapDao;

    @Mock
    KubernetesClusterDetailsDao kubernetesClusterDetailsDao;

    @Mock
    KubernetesSupportedVersionDao kubernetesSupportedVersionDao;

    @Mock
    KubernetesClusterManagerImpl kubernetesClusterManager;

    @Mock
    IPAddressDao ipAddressDao;

    KubernetesClusterActionWorker actionWorker = null;

    final static Long DEFAULT_ID = 1L;

    @Before
    public void setUp() throws Exception {
        kubernetesClusterManager.kubernetesClusterDao = kubernetesClusterDao;
        kubernetesClusterManager.kubernetesSupportedVersionDao = kubernetesSupportedVersionDao;
        kubernetesClusterManager.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        kubernetesClusterManager.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        KubernetesCluster kubernetesCluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(kubernetesCluster.getId()).thenReturn(DEFAULT_ID);
        actionWorker = new KubernetesClusterActionWorker(kubernetesCluster, kubernetesClusterManager);
        actionWorker.ipAddressDao = ipAddressDao;
    }

    private KubernetesClusterActionWorker joinIpWorker(String printedIp) {
        KubernetesClusterActionWorker worker = Mockito.spy(actionWorker);
        Mockito.doReturn(new Pair<>("203.0.113.5", 2222)).when(worker).getKubernetesClusterServerIpSshPort(Mockito.nullable(UserVm.class));
        Mockito.doReturn("cloud").when(worker).getControlNodeLoginUser();
        Mockito.doReturn(new File("id_rsa")).when(worker).getManagementServerSshPublicKeyFile();
        Mockito.doReturn("10.1.1.10").when(worker).getControlVmPrivateIp();
        worker.joinEndpointIp = printedIp;
        return worker;
    }

    private String getNodeJoinIp(KubernetesClusterActionWorker worker, Pair<Boolean, String> checkResult) {
        try (MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class)) {
            sshHelper.when(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
                    Mockito.any(), Mockito.contains("-checkip 10.1.1.10"), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                    .thenReturn(checkResult);
            String joinIp = worker.getNodeJoinIp();
            sshHelper.verify(() -> SshHelper.sshExecute(Mockito.eq("203.0.113.5"), Mockito.eq(2222), Mockito.eq("cloud"), Mockito.any(),
                    Mockito.any(), Mockito.contains("-checkip 10.1.1.10"), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()));
            return joinIp;
        }
    }

    @Test
    public void testNodeJoinIpIsTheControlNodeIpWhenTheCertificateCoversIt() {
        Assert.assertEquals("10.1.1.10", getNodeJoinIp(joinIpWorker("203.0.113.5"), new Pair<>(true, "IP 10.1.1.10 does match certificate\n")));
    }

    @Test
    public void testNodeJoinIpIsThePrintedEndpointWhenTheCertificateDoesNotCoverTheControlNodeIp() {
        Assert.assertEquals("203.0.113.5", getNodeJoinIp(joinIpWorker("203.0.113.5"), new Pair<>(true, "IP 10.1.1.10 does NOT match certificate\n")));
    }

    @Test
    public void testNodeJoinIpIsTheControlNodeIpWhenTheCertificateCanNotBeChecked() {
        Assert.assertEquals("10.1.1.10", getNodeJoinIp(joinIpWorker("203.0.113.5"), new Pair<>(true, "Could not read certificate from <stdin>\nUnable to load certificate\n")));
    }

    @Test
    public void testNodeJoinIpIsTheControlNodeIpWithoutAPrintedEndpoint() {
        KubernetesClusterActionWorker worker = joinIpWorker(null);
        Assert.assertEquals("10.1.1.10", worker.getNodeJoinIp());
        Mockito.verify(worker, Mockito.never()).isApiServerCertificateValidFor(Mockito.anyString());
    }

    @Test
    public void testDeleteJoinTokenOnlyOnce() {
        KubernetesClusterActionWorker worker = Mockito.spy(actionWorker);
        Mockito.doReturn(new Pair<>("10.1.1.1", 2222)).when(worker).getKubernetesClusterServerIpSshPort(Mockito.nullable(UserVm.class));
        Mockito.doReturn("cloud").when(worker).getControlNodeLoginUser();
        Mockito.doReturn(new File("id_rsa")).when(worker).getManagementServerSshPublicKeyFile();
        worker.joinToken = "abcdef.0123456789abcdef";
        worker.joinCaCertHash = "sha256:9826a516d7c2f8974dd0a09a64048c5cba6ff67780b01568b13cd3acf92a409f";
        try (MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class)) {
            sshHelper.when(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
                    Mockito.any(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                    .thenReturn(new Pair<>(true, ""));
            worker.deleteJoinToken();
            worker.deleteJoinToken();
            sshHelper.verify(() -> SshHelper.sshExecute(Mockito.eq("10.1.1.1"), Mockito.eq(2222), Mockito.eq("cloud"), Mockito.any(),
                    Mockito.any(), Mockito.eq("sudo /opt/bin/kubeadm token delete abcdef"), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()));
            sshHelper.verifyNoMoreInteractions();
        }
        Assert.assertNull(worker.joinToken);
        Assert.assertNull(worker.joinCaCertHash);
    }

    @Test
    public void testDeleteJoinTokenControlNodeUnreachable() {
        KubernetesClusterActionWorker worker = Mockito.spy(actionWorker);
        Mockito.doThrow(new CloudRuntimeException("no control node")).when(worker).getKubernetesClusterServerIpSshPort(Mockito.nullable(UserVm.class));
        worker.joinToken = "abcdef.0123456789abcdef";
        worker.deleteJoinToken();
        Assert.assertNull(worker.joinToken);
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullDetail() {
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(Mockito.mock(Network.class));
        Assert.assertNull(result);
    }

    private String mockClusterPublicIpDetail(boolean isNull) {
        String uuid = isNull ? null : UUID.randomUUID().toString();
        KubernetesClusterDetailsVO detailsVO = new KubernetesClusterDetailsVO(DEFAULT_ID, ApiConstants.PUBLIC_IP_ID, uuid, false);
        Mockito.when(kubernetesClusterDetailsDao.findDetail(DEFAULT_ID, ApiConstants.PUBLIC_IP_ID)).thenReturn(detailsVO);
        return uuid;
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullDetailValue() {
        mockClusterPublicIpDetail(true);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(Mockito.mock(Network.class));
        Assert.assertNull(result);
    }

    private Network mockNetworkForGetVpcTierKubernetesPublicIpTest() {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getVpcId()).thenReturn(DEFAULT_ID);
        return network;
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullVpc() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNull(result);
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpDifferentVpc() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(address.getVpcId()).thenReturn(2L);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNull(result);
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpValid() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(address.getVpcId()).thenReturn(DEFAULT_ID);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNotNull(result);
    }
}
