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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
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
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterAffinityGroupMapDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.uservm.UserVm;
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

    @Mock
    KubernetesClusterAffinityGroupMapDao kubernetesClusterAffinityGroupMapDao;

    @Mock
    AffinityGroupDao affinityGroupDao;

    KubernetesClusterActionWorker actionWorker = null;

    KubernetesCluster kubernetesCluster = null;

    final static Long DEFAULT_ID = 1L;

    @Before
    public void setUp() throws Exception {
        kubernetesClusterManager.kubernetesClusterDao = kubernetesClusterDao;
        kubernetesClusterManager.kubernetesSupportedVersionDao = kubernetesSupportedVersionDao;
        kubernetesClusterManager.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        kubernetesClusterManager.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        kubernetesClusterManager.kubernetesClusterAffinityGroupMapDao = kubernetesClusterAffinityGroupMapDao;
        kubernetesCluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(kubernetesCluster.getId()).thenReturn(DEFAULT_ID);
        actionWorker = new KubernetesClusterActionWorker(kubernetesCluster, kubernetesClusterManager);
        actionWorker.ipAddressDao = ipAddressDao;
        actionWorker.affinityGroupDao = affinityGroupDao;
        actionWorker.userVmDao = userVmDao;
    }

    private KubernetesClusterVmMapVO vmMap(long vmId) {
        KubernetesClusterVmMapVO vmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vmMap.getVmId()).thenReturn(vmId);
        return vmMap;
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

    @Test
    public void testGetAffinityGroupIdsForNodeTypeReturnsIds() {
        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(DEFAULT_ID, "CONTROL"))
            .thenReturn(Arrays.asList(1L, 2L));

        List<Long> result = actionWorker.getAffinityGroupIdsForNodeType(KubernetesClusterNodeType.CONTROL);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.containsAll(Arrays.asList(1L, 2L)));
    }

    @Test
    public void testGetAffinityGroupIdsForNodeTypeReturnsEmptyList() {
        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(DEFAULT_ID, "WORKER"))
            .thenReturn(Collections.emptyList());

        List<Long> result = actionWorker.getAffinityGroupIdsForNodeType(KubernetesClusterNodeType.WORKER);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetMergedAffinityGroupIdsWithExplicitDedication() {
        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(DEFAULT_ID, "CONTROL"))
            .thenReturn(new ArrayList<>(Arrays.asList(1L)));

        AffinityGroupVO explicitGroup = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(explicitGroup.getId()).thenReturn(99L);
        Mockito.when(affinityGroupDao.findByAccountAndType(Mockito.anyLong(), Mockito.eq("ExplicitDedication")))
            .thenReturn(explicitGroup);

        List<Long> result = actionWorker.getMergedAffinityGroupIds(KubernetesClusterNodeType.CONTROL, 1L, 1L);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(1L));
        Assert.assertTrue(result.contains(99L));
    }

    @Test
    public void testGetMergedAffinityGroupIdsNoExplicitDedication() {
        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(DEFAULT_ID, "WORKER"))
            .thenReturn(new ArrayList<>(Arrays.asList(1L, 2L)));
        Mockito.when(affinityGroupDao.findByAccountAndType(Mockito.anyLong(), Mockito.eq("ExplicitDedication")))
            .thenReturn(null);
        Mockito.when(affinityGroupDao.findDomainLevelGroupByType(Mockito.anyLong(), Mockito.eq("ExplicitDedication")))
            .thenReturn(null);

        List<Long> result = actionWorker.getMergedAffinityGroupIds(KubernetesClusterNodeType.WORKER, 1L, 1L);

        Assert.assertEquals(2, result.size());
    }

    @Test
    public void testGetMergedAffinityGroupIdsReturnsNullWhenEmpty() {
        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(DEFAULT_ID, "ETCD"))
            .thenReturn(new ArrayList<>());
        Mockito.when(affinityGroupDao.findByAccountAndType(Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(null);
        Mockito.when(affinityGroupDao.findDomainLevelGroupByType(Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(null);

        List<Long> result = actionWorker.getMergedAffinityGroupIds(KubernetesClusterNodeType.ETCD, 1L, 1L);

        Assert.assertNull(result);
    }

    @Test
    public void testGetMergedAffinityGroupIdsExplicitDedicationAlreadyInList() {
        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(DEFAULT_ID, "CONTROL"))
            .thenReturn(new ArrayList<>(Arrays.asList(99L, 2L)));

        AffinityGroupVO explicitGroup = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(explicitGroup.getId()).thenReturn(99L);
        Mockito.when(affinityGroupDao.findByAccountAndType(Mockito.anyLong(), Mockito.eq("ExplicitDedication")))
            .thenReturn(explicitGroup);

        List<Long> result = actionWorker.getMergedAffinityGroupIds(KubernetesClusterNodeType.CONTROL, 1L, 1L);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(99L));
        Assert.assertTrue(result.contains(2L));
    }

    @Test
    public void testGetControlNodeVMsUsesControlMapsSortedByVmId() {
        UserVmVO vm10 = Mockito.mock(UserVmVO.class);
        UserVmVO vm20 = Mockito.mock(UserVmVO.class);
        KubernetesClusterVmMapVO map10 = vmMap(10L);
        KubernetesClusterVmMapVO map20 = vmMap(20L);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(DEFAULT_ID, KubernetesClusterNodeType.CONTROL))
            .thenReturn(Arrays.asList(map20, map10));
        Mockito.when(userVmDao.findById(10L)).thenReturn(vm10);
        Mockito.when(userVmDao.findById(20L)).thenReturn(vm20);

        List<UserVm> controlVms = actionWorker.getControlNodeVMs();

        Assert.assertEquals(Arrays.asList(vm10, vm20), controlVms);
        Mockito.verify(kubernetesClusterVmMapDao, Mockito.never()).listByClusterId(DEFAULT_ID);
    }

    @Test
    public void testGetControlNodeVMsFallsBackForClustersWithoutControlFlag() {
        UserVmVO vm3 = Mockito.mock(UserVmVO.class);
        UserVmVO vm4 = Mockito.mock(UserVmVO.class);
        Mockito.when(kubernetesCluster.getControlNodeCount()).thenReturn(2L);
        KubernetesClusterVmMapVO etcd1 = vmMap(1L);
        KubernetesClusterVmMapVO etcd2 = vmMap(2L);
        Mockito.when(etcd1.isEtcdNode()).thenReturn(true);
        Mockito.when(etcd2.isEtcdNode()).thenReturn(true);
        KubernetesClusterVmMapVO control1 = vmMap(3L);
        KubernetesClusterVmMapVO control2 = vmMap(4L);
        KubernetesClusterVmMapVO worker = vmMap(5L);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(DEFAULT_ID, KubernetesClusterNodeType.CONTROL))
            .thenReturn(Collections.emptyList());
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID))
            .thenReturn(Arrays.asList(etcd1, etcd2, control1, control2, worker));
        Mockito.when(userVmDao.findById(3L)).thenReturn(vm3);
        Mockito.when(userVmDao.findById(4L)).thenReturn(vm4);

        List<UserVm> controlVms = actionWorker.getControlNodeVMs();

        Assert.assertEquals(Arrays.asList(vm3, vm4), controlVms);
    }

    @Test
    public void testGetControlNodeVMsFallbackIgnoresExternalNodes() {
        UserVmVO vm2 = Mockito.mock(UserVmVO.class);
        UserVmVO vm3 = Mockito.mock(UserVmVO.class);
        Mockito.when(kubernetesCluster.getControlNodeCount()).thenReturn(2L);
        KubernetesClusterVmMapVO external = vmMap(1L);
        Mockito.when(external.isExternalNode()).thenReturn(true);
        KubernetesClusterVmMapVO control2 = vmMap(2L);
        KubernetesClusterVmMapVO control3 = vmMap(3L);
        KubernetesClusterVmMapVO worker = vmMap(4L);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(DEFAULT_ID, KubernetesClusterNodeType.CONTROL))
            .thenReturn(Collections.emptyList());
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID))
            .thenReturn(Arrays.asList(external, control2, control3, worker));
        Mockito.when(userVmDao.findById(2L)).thenReturn(vm2);
        Mockito.when(userVmDao.findById(3L)).thenReturn(vm3);

        List<UserVm> controlVms = actionWorker.getControlNodeVMs();

        Assert.assertEquals(Arrays.asList(vm2, vm3), controlVms);
    }

    @Test
    public void testGetControlNodeVMsReturnsEmptyWithoutMaps() {
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(DEFAULT_ID, KubernetesClusterNodeType.CONTROL))
            .thenReturn(Collections.emptyList());
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(Collections.emptyList());

        Assert.assertTrue(actionWorker.getControlNodeVMs().isEmpty());
    }

    @Test
    public void testGetControlNodeVMsSkipsMissingUserVms() {
        UserVmVO vm20 = Mockito.mock(UserVmVO.class);
        KubernetesClusterVmMapVO map10 = vmMap(10L);
        KubernetesClusterVmMapVO map20 = vmMap(20L);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(DEFAULT_ID, KubernetesClusterNodeType.CONTROL))
            .thenReturn(Arrays.asList(map10, map20));
        Mockito.when(userVmDao.findById(20L)).thenReturn(vm20);

        List<UserVm> controlVms = actionWorker.getControlNodeVMs();

        Assert.assertEquals(Collections.singletonList(vm20), controlVms);
    }
}
