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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroupVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.logging.log4j.Level;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterNodeGroupWorkerTest {

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;

    private KubernetesClusterNodeGroupVO nodeGroup;
    private KubernetesClusterNodeGroupWorker worker;

    @Before
    public void setUp() {
        nodeGroup = new KubernetesClusterNodeGroupVO(1L, "gpu", 10L, 20L, "disktype=ssd", "gpu=true:NoSchedule");
        worker = new KubernetesClusterNodeGroupWorker(kubernetesCluster, nodeGroup, clusterManager);
        worker.serviceOfferingDao = serviceOfferingDao;
    }

    @Test
    public void testGetWorkerNodeHostName() {
        worker.kubernetesClusterNodeNamePrefix = "mycluster";
        Assert.assertEquals("mycluster-gpu-18f2a3b4c5d", worker.getWorkerNodeHostName("18f2a3b4c5d"));
    }

    @Test
    public void testGetWorkerNodeHostNameTruncatesPrefix() {
        worker.kubernetesClusterNodeNamePrefix = "a-very-long-kubernetes-cluster-name-prefix-of-43-chars";
        String hostName = worker.getWorkerNodeHostName("18f2a3b4c5d");
        Assert.assertTrue(hostName.length() <= 63);
        Assert.assertTrue(hostName.endsWith("-gpu-18f2a3b4c5d"));
        Assert.assertFalse(hostName.contains("--"));
    }

    @Test
    public void testGetWorkerNodeKubeletExtraArgs() {
        Assert.assertEquals("--node-labels=cloudstack.apache.org/nodegroup=gpu,disktype=ssd --register-with-taints=gpu=true:NoSchedule",
                worker.getWorkerNodeKubeletExtraArgs());
    }

    @Test
    public void testGetServiceOfferingForWorkerNodeIsNodeGroupOffering() {
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(10L)).thenReturn(offering);
        ServiceOffering result = worker.getServiceOfferingForNodeTypeOnCluster(WORKER, kubernetesCluster);
        Assert.assertSame(offering, result);
    }
    @Test
    public void testGetWorkerNodeHostNameDefaultNodeGroup() {
        nodeGroup.setDefault(true);
        worker.kubernetesClusterNodeNamePrefix = "mycluster";
        Assert.assertEquals("mycluster-node-18f2a3b4c5d", worker.getWorkerNodeHostName("18f2a3b4c5d"));
    }

    @Test
    public void testGetWorkerNodeGroupId() {
        Assert.assertSame(nodeGroup, worker.getWorkerNodeGroup());
    }

    @Test
    public void testFailureDoesNotChangeClusterState() {
        KubernetesClusterNodeGroupWorker spyWorker = Mockito.spy(worker);
        try {
            spyWorker.logTransitStateAndThrow(Level.ERROR, "failure", 1L, KubernetesCluster.Event.OperationFailed, null);
            Assert.fail("Expected an exception");
        } catch (CloudRuntimeException e) {
            Assert.assertEquals("failure", e.getMessage());
        }
        Mockito.verify(spyWorker, Mockito.never()).stateTransitTo(Mockito.anyLong(), Mockito.any());
    }
}
