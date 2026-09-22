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

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.offering.ServiceOffering;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class KubernetesClusterNodeCapacityReconcilerImplTest {
    private final KubernetesClusterNodeCapacityReconciler reconciler = new KubernetesClusterNodeCapacityReconcilerImpl();

    @Test
    public void testParseKubernetesQuantities() {
        Assert.assertEquals(3900L, KubernetesClusterNodeCapacityReconcilerImpl.parseCpuMillis("3900m"));
        Assert.assertEquals(4000L, KubernetesClusterNodeCapacityReconcilerImpl.parseCpuMillis("4"));
        Assert.assertEquals(4037048L * 1024L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("4037048Ki"));
        Assert.assertEquals(4L * 1024L * 1024L * 1024L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("4Gi"));
    }

    @Test
    public void testOnlyRunningKubernetesNodesWithChangedCpuOrRamRequireRefresh() {
        ServiceOffering oldOffering = offering(2, 2048);
        ServiceOffering cpuOffering = offering(4, 2048);
        ServiceOffering ramOffering = offering(2, 4096);
        ServiceOffering capOnlyOffering = offering(2, 2048);

        Assert.assertTrue(reconciler.requiresKubeletRefresh(oldOffering, cpuOffering, KubernetesClusterNodeType.WORKER, KubernetesCluster.State.Running));
        Assert.assertTrue(reconciler.requiresKubeletRefresh(oldOffering, ramOffering, KubernetesClusterNodeType.CONTROL, KubernetesCluster.State.Running));
        Assert.assertFalse(reconciler.requiresKubeletRefresh(oldOffering, capOnlyOffering, KubernetesClusterNodeType.WORKER, KubernetesCluster.State.Running));
        Assert.assertFalse(reconciler.requiresKubeletRefresh(oldOffering, cpuOffering, KubernetesClusterNodeType.ETCD, KubernetesCluster.State.Running));
        Assert.assertFalse(reconciler.requiresKubeletRefresh(oldOffering, cpuOffering, KubernetesClusterNodeType.WORKER, KubernetesCluster.State.Stopped));
    }

    @Test
    public void testCurrentKubernetesResourcesDoNotRequireASecondKubeletRestart() {
        long memoryKiB = 4L * 1024L * 1024L;
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot current =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(false, false, 4, memoryKiB,
                        4000, memoryKiB * 1024L, 3900, memoryKiB * 1024L - 128L * 1024L * 1024L, true);
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot stale =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(false, false, 4, memoryKiB,
                        2000, memoryKiB * 1024L, 1900, memoryKiB * 1024L - 128L * 1024L * 1024L, true);

        Assert.assertTrue(reconciler.isKubernetesResourcesCurrent(current, offering(4, 4096)));
        Assert.assertFalse(reconciler.isKubernetesResourcesCurrent(stale, offering(4, 4096)));
    }

    private ServiceOffering offering(int cpu, int memory) {
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        Mockito.when(offering.getCpu()).thenReturn(cpu);
        Mockito.when(offering.getRamSize()).thenReturn(memory);
        return offering;
    }
}
