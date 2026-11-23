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
package com.cloud.kubernetes.cluster.utils;

import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterUtilTest {

    private KubernetesClusterVO createCluster(Long clusterOfferingId, Long controlOfferingId, Long workerOfferingId,
                                              Long etcdNodeCount, Long etcdOfferingId) {
        KubernetesClusterVO cluster = new KubernetesClusterVO();
        if (clusterOfferingId != null) {
            cluster.setServiceOfferingId(clusterOfferingId);
        }
        cluster.setControlNodeCount(1);
        cluster.setNodeCount(3);
        cluster.setEtcdNodeCount(etcdNodeCount);
        cluster.setControlNodeServiceOfferingId(controlOfferingId);
        cluster.setWorkerNodeServiceOfferingId(workerOfferingId);
        cluster.setEtcdNodeServiceOfferingId(etcdOfferingId);
        return cluster;
    }

    @Test
    public void getCommonNodeServiceOfferingIdAllNodeTypesOnSameOffering() {
        Assert.assertEquals(Long.valueOf(2L), KubernetesClusterUtil.getCommonNodeServiceOfferingId(createCluster(1L, 2L, 2L, 3L, 2L)));
    }

    @Test
    public void getCommonNodeServiceOfferingIdNodeTypesOnDifferentOfferings() {
        Assert.assertNull(KubernetesClusterUtil.getCommonNodeServiceOfferingId(createCluster(1L, 2L, 3L, null, null)));
    }

    @Test
    public void getCommonNodeServiceOfferingIdNodeTypeWithoutOfferingUsesClusterOffering() {
        Assert.assertNull(KubernetesClusterUtil.getCommonNodeServiceOfferingId(createCluster(1L, null, 2L, null, null)));
        Assert.assertEquals(Long.valueOf(1L), KubernetesClusterUtil.getCommonNodeServiceOfferingId(createCluster(1L, null, 1L, null, null)));
    }

    @Test
    public void getCommonNodeServiceOfferingIdIgnoresNodeTypeWithoutNodes() {
        Assert.assertEquals(Long.valueOf(2L), KubernetesClusterUtil.getCommonNodeServiceOfferingId(createCluster(1L, 2L, 2L, 0L, 3L)));
        Assert.assertEquals(Long.valueOf(2L), KubernetesClusterUtil.getCommonNodeServiceOfferingId(createCluster(1L, 2L, 2L, null, null)));
    }

    @Test
    public void getCommonNodeServiceOfferingIdEtcdNodesOnDifferentOffering() {
        Assert.assertNull(KubernetesClusterUtil.getCommonNodeServiceOfferingId(createCluster(1L, 2L, 2L, 3L, 3L)));
    }

    @Test
    public void getCommonNodeServiceOfferingIdNodeTypeWithoutAnyOffering() {
        Assert.assertNull(KubernetesClusterUtil.getCommonNodeServiceOfferingId(createCluster(null, null, 2L, null, null)));
    }

    private void executeThrowAndTestVersionMatch() {
        Pair<Boolean, String> resultPair = null;
        Pair<Boolean, String> result = KubernetesClusterUtil.clusterNodeVersionMatches(resultPair, "1.24.0");
        Assert.assertFalse(result.first());
    }

    private void executeAndTestVersionMatch(boolean status, String response, boolean expectedResult) {
        Pair<Boolean, String> resultPair = new Pair<>(status, response);
        Pair<Boolean, String> result = KubernetesClusterUtil.clusterNodeVersionMatches(resultPair, "1.24.0");
        Assert.assertEquals(expectedResult, result.first());
    }

    @Test
    public void testClusterNodeVersionMatches() {
        String v1233WorkerNodeOutput = "v1.23.3";
        String v1240WorkerNodeOutput = "v1.24.0";

        executeAndTestVersionMatch(true, v1240WorkerNodeOutput, true);

        executeAndTestVersionMatch(true, v1233WorkerNodeOutput, false);

        executeAndTestVersionMatch(false, v1240WorkerNodeOutput, false);

        executeAndTestVersionMatch(false, v1233WorkerNodeOutput, false);

        executeThrowAndTestVersionMatch();
    }
}
