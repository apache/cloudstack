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
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.network.Network;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterStartWorkerTest {

    private static final String PUBLIC_IP = "203.0.113.10";

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private Network network;

    private KubernetesClusterStartWorker worker;
    private int etcdNodeStartPort;

    @Before
    public void setUp() {
        worker = Mockito.spy(new KubernetesClusterStartWorker(kubernetesCluster, clusterManager));
        worker.publicIpAddress = PUBLIC_IP;
        etcdNodeStartPort = KubernetesClusterService.KubernetesEtcdNodeStartPort.value();
    }

    private long startTimeoutTime() {
        return System.currentTimeMillis() + 3600 * 1000L;
    }

    private List<UserVm> mockEtcdVms(int count) {
        List<UserVm> vms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            vms.add(Mockito.mock(UserVm.class));
        }
        return vms;
    }

    @Test
    public void testVerifyEtcdRunningOnEtcdNodesWithoutEtcdNodes() {
        worker.verifyEtcdRunningOnEtcdNodes(new ArrayList<>(), new ArrayList<>(), network, startTimeoutTime());

        Mockito.verify(worker, Mockito.never()).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void testVerifyEtcdRunningOnEtcdNodesUsesPortForwardingRules() {
        List<UserVm> etcdVms = mockEtcdVms(3);
        Mockito.when(clusterManager.isDirectAccess(network)).thenReturn(false);
        Mockito.doReturn(null).when(worker).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong());

        worker.verifyEtcdRunningOnEtcdNodes(etcdVms, new ArrayList<>(), network, startTimeoutTime());

        for (int i = 0; i < etcdVms.size(); i++) {
            Mockito.verify(worker).waitForEtcdOnEtcdNode(Mockito.eq(etcdVms.get(i)), Mockito.eq(PUBLIC_IP), Mockito.eq(etcdNodeStartPort + i), Mockito.anyLong());
        }
    }

    @Test
    public void testVerifyEtcdRunningOnEtcdNodesOnDirectAccessNetwork() {
        List<UserVm> etcdVms = mockEtcdVms(1);
        Mockito.when(etcdVms.get(0).getPrivateIpAddress()).thenReturn("10.1.1.20");
        Mockito.when(clusterManager.isDirectAccess(network)).thenReturn(true);
        Mockito.doReturn(null).when(worker).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong());

        worker.verifyEtcdRunningOnEtcdNodes(etcdVms, new ArrayList<>(), network, startTimeoutTime());

        Mockito.verify(worker).waitForEtcdOnEtcdNode(Mockito.eq(etcdVms.get(0)), Mockito.eq("10.1.1.20"),
                Mockito.eq(KubernetesClusterActionWorker.DEFAULT_SSH_PORT), Mockito.anyLong());
    }

    @Test
    public void testVerifyEtcdRunningOnEtcdNodesWaitIsBoundedBySetting() {
        List<UserVm> etcdVms = mockEtcdVms(1);
        Mockito.doReturn(null).when(worker).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong());
        long before = System.currentTimeMillis();

        worker.verifyEtcdRunningOnEtcdNodes(etcdVms, new ArrayList<>(), network, startTimeoutTime());

        ArgumentCaptor<Long> timeoutTime = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(worker).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), timeoutTime.capture());
        long setupTimeoutMs = KubernetesClusterService.KubernetesEtcdNodeSetupTimeout.value() * 1000;
        Assert.assertTrue(timeoutTime.getValue() <= System.currentTimeMillis() + setupTimeoutMs);
        Assert.assertTrue(timeoutTime.getValue() >= before + setupTimeoutMs);
    }

    @Test
    public void testVerifyEtcdRunningOnEtcdNodesWaitNeverExceedsClusterStartTimeout() {
        List<UserVm> etcdVms = mockEtcdVms(1);
        Mockito.doReturn(null).when(worker).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong());
        long startTimeoutTime = System.currentTimeMillis() + 5000;

        worker.verifyEtcdRunningOnEtcdNodes(etcdVms, new ArrayList<>(), network, startTimeoutTime);

        Mockito.verify(worker).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.eq(startTimeoutTime));
    }

    @Test
    public void testVerifyEtcdRunningOnEtcdNodesFailsClusterWithReason() {
        List<UserVm> etcdVms = mockEtcdVms(2);
        Mockito.when(etcdVms.get(0).getDisplayName()).thenReturn("cluster-etcd-1");
        List<UserVm> clusterVMs = mockEtcdVms(1);
        Mockito.when(kubernetesCluster.getName()).thenReturn("cluster");
        Mockito.when(kubernetesCluster.getId()).thenReturn(7L);
        String reason = "The Kubernetes ISO does not include the etcd binaries";
        Mockito.doReturn(reason).when(worker).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong());
        Mockito.doReturn(true).when(worker).stateTransitTo(7L, KubernetesCluster.Event.CreateFailed);
        Mockito.doNothing().when(worker).detachIsoKubernetesVMs(Mockito.anyList());

        try {
            worker.verifyEtcdRunningOnEtcdNodes(etcdVms, clusterVMs, network, startTimeoutTime());
            Assert.fail("The cluster start should fail when etcd is not running on an etcd node");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("cluster-etcd-1"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(reason));
        }

        // Fails on the first etcd node without waiting for the others, and the ISO is detached from every VM
        Mockito.verify(worker, Mockito.times(1)).waitForEtcdOnEtcdNode(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong());
        Mockito.verify(worker).stateTransitTo(7L, KubernetesCluster.Event.CreateFailed);
        ArgumentCaptor<List<UserVm>> detached = ArgumentCaptor.forClass(List.class);
        Mockito.verify(worker).detachIsoKubernetesVMs(detached.capture());
        Assert.assertEquals(3, detached.getValue().size());
    }
}
