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
    KubernetesClusterAffinityGroupMapDao kubernetesClusterAffinityGroupMapDao;

    @Mock
    AffinityGroupDao affinityGroupDao;

    KubernetesClusterActionWorker actionWorker = null;

    final static Long DEFAULT_ID = 1L;

    @Before
    public void setUp() throws Exception {
        kubernetesClusterManager.kubernetesClusterDao = kubernetesClusterDao;
        kubernetesClusterManager.kubernetesSupportedVersionDao = kubernetesSupportedVersionDao;
        kubernetesClusterManager.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        kubernetesClusterManager.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        kubernetesClusterManager.kubernetesClusterAffinityGroupMapDao = kubernetesClusterAffinityGroupMapDao;
        KubernetesCluster kubernetesCluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(kubernetesCluster.getId()).thenReturn(DEFAULT_ID);
        actionWorker = new KubernetesClusterActionWorker(kubernetesCluster, kubernetesClusterManager);
        actionWorker.ipAddressDao = ipAddressDao;
        actionWorker.affinityGroupDao = affinityGroupDao;
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
    public void testCleanupScriptFilesDeletesAllTempFiles() throws Exception {
        actionWorker.deploySecretsScriptFile = File.createTempFile("deploy-cloudstack-secret", ".sh");
        actionWorker.deployProviderScriptFile = File.createTempFile("deploy-provider", ".sh");
        actionWorker.deployCsiDriverScriptFile = File.createTempFile("deploy-csi-driver", ".sh");
        actionWorker.deletePvScriptFile = File.createTempFile("delete-pv-reclaimpolicy-delete", ".sh");
        actionWorker.autoscaleScriptFile = File.createTempFile("autoscale-kube-cluster", ".sh");

        List<File> files = Arrays.asList(actionWorker.deploySecretsScriptFile, actionWorker.deployProviderScriptFile,
                actionWorker.deployCsiDriverScriptFile, actionWorker.deletePvScriptFile, actionWorker.autoscaleScriptFile);
        for (File f : files) {
            Assert.assertTrue(f.exists());
        }

        actionWorker.cleanupScriptFiles();

        for (File f : files) {
            Assert.assertFalse("Temporary script file should have been deleted: " + f.getAbsolutePath(), f.exists());
        }
    }

    @Test
    public void testDeleteScriptFileQuietlyHandlesNullAndMissingFiles() throws Exception {
        // null must not throw
        actionWorker.deleteScriptFileQuietly(null);

        // a File that does not exist must not throw
        File missing = new File(System.getProperty("java.io.tmpdir"), "cks-nonexistent-" + UUID.randomUUID() + ".sh");
        Assert.assertFalse(missing.exists());
        actionWorker.deleteScriptFileQuietly(missing);

        // an existing file is deleted
        File present = File.createTempFile("cks-present", ".sh");
        Assert.assertTrue(present.exists());
        actionWorker.deleteScriptFileQuietly(present);
        Assert.assertFalse(present.exists());
    }

    @Test
    public void testCleanupScriptFilesWithNullFieldsDoesNotThrow() {
        // No retrieveScriptFiles() was called, so all file fields are null.
        actionWorker.cleanupScriptFiles();
    }
}
