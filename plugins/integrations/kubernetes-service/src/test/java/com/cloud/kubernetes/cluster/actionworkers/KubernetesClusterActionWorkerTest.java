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

import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

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

    @Mock
    UserVmDao userVmDao;

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
        actionWorker.userVmDao = userVmDao;
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

    private KubernetesClusterVmMapVO mockClusterVm(long vmId, boolean control, boolean etcd, boolean external, String ip) {
        KubernetesClusterVmMapVO vmMap = new KubernetesClusterVmMapVO(DEFAULT_ID, vmId, control);
        vmMap.setEtcdNode(etcd);
        vmMap.setExternalNode(external);
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.lenient().when(vm.getPrivateIpAddress()).thenReturn(ip);
        Mockito.lenient().when(userVmDao.findById(vmId)).thenReturn(vm);
        return vmMap;
    }

    @Test
    public void testGetControlVmPrivateIpIgnoresExternalNodeWithLowerVmId() {
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(7L, true, false, false, "10.0.0.7"),
                mockClusterVm(8L, false, false, false, "10.0.0.8"),
                mockClusterVm(4L, false, false, true, "10.0.0.4"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertEquals("10.0.0.7", actionWorker.getControlVmPrivateIp());
    }

    @Test
    public void testGetControlVmPrivateIpPicksLowestControlVmIdOverEtcdNodes() {
        Mockito.lenient().when(actionWorker.kubernetesCluster.getEtcdNodeCount()).thenReturn(2L);
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(2L, false, true, false, "10.0.0.2"),
                mockClusterVm(3L, false, true, false, "10.0.0.3"),
                mockClusterVm(6L, true, false, false, "10.0.0.6"),
                mockClusterVm(4L, true, false, false, "10.0.0.4"),
                mockClusterVm(5L, false, false, false, "10.0.0.5"),
                mockClusterVm(1L, false, false, true, "10.0.0.1"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertEquals("10.0.0.4", actionWorker.getControlVmPrivateIp());
    }

    @Test
    public void testGetControlVmPrivateIpSkipsRemovedControlVm() {
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(3L, true, false, false, "10.0.0.3"),
                mockClusterVm(5L, true, false, false, "10.0.0.5"));
        Mockito.when(userVmDao.findById(3L)).thenReturn(null);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertEquals("10.0.0.5", actionWorker.getControlVmPrivateIp());
    }

    @Test
    public void testGetControlVmPrivateIpLegacyClusterWithoutControlFlag() {
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(9L, false, false, false, "10.0.0.9"),
                mockClusterVm(7L, false, false, false, "10.0.0.7"),
                mockClusterVm(4L, false, false, true, "10.0.0.4"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertEquals("10.0.0.7", actionWorker.getControlVmPrivateIp());
    }

    @Test
    public void testGetControlVmPrivateIpUnflaggedFallbackSkipsEtcdNodes() {
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(1L, false, true, false, "10.0.0.1"),
                mockClusterVm(6L, false, false, false, "10.0.0.6"),
                mockClusterVm(3L, false, false, false, "10.0.0.3"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertEquals("10.0.0.3", actionWorker.getControlVmPrivateIp());
    }

    @Test
    public void testGetControlVmPrivateIpUnflaggedFallbackDoesNotPickNextVm() {
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(3L, false, false, false, "10.0.0.3"),
                mockClusterVm(6L, false, false, false, "10.0.0.6"));
        Mockito.when(userVmDao.findById(3L)).thenReturn(null);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertNull(actionWorker.getControlVmPrivateIp());
    }

    @Test
    public void testGetControlVmPrivateIpFlaggedControlGoneDoesNotFallBack() {
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(5L, true, false, false, "10.0.0.5"),
                mockClusterVm(3L, false, false, false, "10.0.0.3"));
        Mockito.when(userVmDao.findById(5L)).thenReturn(null);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertNull(actionWorker.getControlVmPrivateIp());
    }

    @Test
    public void testGetKubernetesClusterServerIpSshPortForSharedNetworkUsesControlVm() {
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(7L, true, false, false, "10.0.0.7"),
                mockClusterVm(4L, false, false, true, "10.0.0.4"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertEquals("10.0.0.7", actionWorker.getKubernetesClusterServerIpSshPortForSharedNetwork(null).first());
    }

    @Test
    public void testGetControlVmPrivateIpNoClusterVms() {
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(List.of());
        Assert.assertNull(actionWorker.getControlVmPrivateIp());
    }

    @Test
    public void testGetControlVmPrivateIpOnlyExternalNodes() {
        List<KubernetesClusterVmMapVO> vmMaps = List.of(
                mockClusterVm(1L, false, false, true, "10.0.0.1"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(vmMaps);
        Assert.assertNull(actionWorker.getControlVmPrivateIp());
    }
}
