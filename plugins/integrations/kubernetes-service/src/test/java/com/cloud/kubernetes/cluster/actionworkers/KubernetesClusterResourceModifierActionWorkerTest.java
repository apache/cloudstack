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

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterResourceModifierActionWorkerTest {
    @Mock
    private KubernetesClusterDao kubernetesClusterDaoMock;

    @Mock
    private KubernetesClusterDetailsDao kubernetesClusterDetailsDaoMock;

    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDaoMock;

    @Mock
    private KubernetesSupportedVersionDao kubernetesSupportedVersionDaoMock;

    @Mock
    private KubernetesClusterManagerImpl kubernetesClusterManagerMock;

    @Mock
    private KubernetesCluster kubernetesClusterMock;

    private KubernetesClusterResourceModifierActionWorker kubernetesClusterResourceModifierActionWorker;

    @Before
    public void setUp() {
        kubernetesClusterManagerMock.kubernetesClusterDao = kubernetesClusterDaoMock;
        kubernetesClusterManagerMock.kubernetesSupportedVersionDao = kubernetesSupportedVersionDaoMock;
        kubernetesClusterManagerMock.kubernetesClusterDetailsDao = kubernetesClusterDetailsDaoMock;
        kubernetesClusterManagerMock.kubernetesClusterVmMapDao = kubernetesClusterVmMapDaoMock;

        kubernetesClusterResourceModifierActionWorker = new KubernetesClusterResourceModifierActionWorker(kubernetesClusterMock, kubernetesClusterManagerMock);
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestReturnOriginalPrefixWhenNamingAllRequirementsAreMet() {
        String originalPrefix = "k8s-cluster-01";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldOnlyContainLowerCaseCharacters() {
        String originalPrefix = "k8s-CLUSTER-01";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldBeTruncatedWhenRequired() {
        int maxPrefixLength = 43;

        String originalPrefix = "c".repeat(maxPrefixLength + 1);
        String expectedPrefix = "c".repeat(maxPrefixLength);

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        String normalizedPrefix = kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix();
        Assert.assertEquals(expectedPrefix, normalizedPrefix);
        Assert.assertEquals(maxPrefixLength, normalizedPrefix.length());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldBeTruncatedWhenRequiredAndWhenOriginalPrefixIsInvalid() {
        int maxPrefixLength = 43;

        String originalPrefix = "1!" + "c".repeat(maxPrefixLength);
        String expectedPrefix = "k8s-1" + "c".repeat(maxPrefixLength - 5);

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        String normalizedPrefix = kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix();
        Assert.assertEquals(expectedPrefix, normalizedPrefix);
        Assert.assertEquals(maxPrefixLength, normalizedPrefix.length());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldOnlyIncludeAlphanumericCharactersAndHyphen() {
        String originalPrefix = "Cluster!@#$%^&*()_+?.-01|<>";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldContainClusterUuidWhenAllCharactersAreInvalid() {
        String clusterUuid = "2699b547-cb56-4a59-a2c6-331cfb21d2e4";
        String originalPrefix = "!@#$%^&*()_+?.|<>";
        String expectedPrefix = "k8s-" + clusterUuid;

        Mockito.when(kubernetesClusterMock.getUuid()).thenReturn(clusterUuid);
        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldNotStartWithADigit() {
        String originalPrefix = "1 cluster";
        String expectedPrefix = "k8s-1cluster";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    private KubernetesClusterVO createClusterWithNodeOfferings(long clusterOfferingId, Long controlOfferingId, Long workerOfferingId) {
        KubernetesClusterVO cluster = new KubernetesClusterVO();
        cluster.setServiceOfferingId(clusterOfferingId);
        cluster.setControlNodeCount(1);
        cluster.setNodeCount(2);
        cluster.setControlNodeServiceOfferingId(controlOfferingId);
        cluster.setWorkerNodeServiceOfferingId(workerOfferingId);
        return cluster;
    }

    @Test
    public void syncClusterServiceOfferingWithNodeOfferingsSetsCommonOffering() {
        KubernetesClusterVO cluster = createClusterWithNodeOfferings(1L, 2L, 2L);
        kubernetesClusterResourceModifierActionWorker.syncClusterServiceOfferingWithNodeOfferings(cluster);
        Assert.assertEquals(Long.valueOf(2L), cluster.getServiceOfferingId());
        Mockito.verify(kubernetesClusterDaoMock).update(cluster.getId(), cluster);
    }

    @Test
    public void syncClusterServiceOfferingWithNodeOfferingsKeepsOfferingWhenNodeOfferingsDiffer() {
        KubernetesClusterVO cluster = createClusterWithNodeOfferings(1L, 2L, 3L);
        kubernetesClusterResourceModifierActionWorker.syncClusterServiceOfferingWithNodeOfferings(cluster);
        Assert.assertEquals(Long.valueOf(1L), cluster.getServiceOfferingId());
        Mockito.verify(kubernetesClusterDaoMock, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void syncClusterServiceOfferingWithNodeOfferingsKeepsOfferingWhenAlreadyInSync() {
        KubernetesClusterVO cluster = createClusterWithNodeOfferings(2L, null, 2L);
        kubernetesClusterResourceModifierActionWorker.syncClusterServiceOfferingWithNodeOfferings(cluster);
        Assert.assertEquals(Long.valueOf(2L), cluster.getServiceOfferingId());
        Mockito.verify(kubernetesClusterDaoMock, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void updateKubernetesClusterEntrySyncsClusterServiceOfferingAfterNodeOfferingScale() {
        KubernetesClusterVO forUpdate = new KubernetesClusterVO();
        KubernetesClusterVO stored = createClusterWithNodeOfferings(1L, 2L, null);
        Mockito.when(kubernetesClusterDaoMock.createForUpdate(Mockito.anyLong())).thenReturn(forUpdate);
        Mockito.when(kubernetesClusterDaoMock.persist(forUpdate)).thenAnswer(invocation -> {
            stored.setWorkerNodeServiceOfferingId(forUpdate.getWorkerNodeServiceOfferingId());
            return forUpdate;
        });
        Mockito.when(kubernetesClusterDaoMock.findById(Mockito.anyLong())).thenReturn(stored);
        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {
            transactionMock.when(() -> Transaction.execute(Mockito.any(TransactionCallback.class))).thenAnswer(invocation ->
                    ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
            KubernetesClusterVO result = kubernetesClusterResourceModifierActionWorker.updateKubernetesClusterEntry(null, null, null,
                    2L, false, null, null, WORKER, true, false);
            Assert.assertSame(stored, result);
        }
        Assert.assertEquals(Long.valueOf(2L), forUpdate.getWorkerNodeServiceOfferingId());
        Assert.assertNull(forUpdate.getServiceOfferingId());
        Assert.assertEquals(Long.valueOf(2L), stored.getServiceOfferingId());
        Mockito.verify(kubernetesClusterDaoMock).persist(forUpdate);
        Mockito.verify(kubernetesClusterDaoMock).update(stored.getId(), stored);
    }
}
