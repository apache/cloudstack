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
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterVmMapCheckWorkerTest {

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;

    @Test
    public void parseNodeNamesKeepsNodesOnly() {
        Assert.assertEquals(List.of("k8s-control-1a", "k8s-node-2b"), KubernetesClusterVmMapCheckWorker.parseNodeNames(
                "node/k8s-control-1a\r\nWarning: something\n  node/K8S-Node-2B  \nnode/\n"));
        Assert.assertTrue(KubernetesClusterVmMapCheckWorker.parseNodeNames("").isEmpty());
        Assert.assertTrue(KubernetesClusterVmMapCheckWorker.parseNodeNames(null).isEmpty());
    }

    @Test
    public void clusterVmHostNamePatternMatchesTheNamesOfTheClusterVmsOnly() {
        Mockito.when(kubernetesCluster.getName()).thenReturn("Prod");
        Pattern pattern = new KubernetesClusterVmMapCheckWorker(kubernetesCluster, clusterManager).getClusterVmHostNamePattern();
        for (String name : List.of("prod-control-19a8f2c3d4e", "prod-node-19a8f2c3d4e", "PROD-NODE-19A8F2C3D4E",
                "Prod-etcd-0-19a8f2c3d4e")) {
            Assert.assertTrue(name, pattern.matcher(name).matches());
        }
        // e.g. the nodes of a cluster named prod-node or prod2, or a VM named by hand
        for (String name : List.of("prod-node-node-19a8f2c3d4e", "prod2-node-19a8f2c3d4e", "prod-node-x1",
                "prod-control", "prod-etcd-19a8f2c3d4e", "other-prod-node-1a",
                "prod-node-1a", "prod-node-02", "prod-master", "prod-control-beef")) {
            Assert.assertFalse(name, pattern.matcher(name).matches());
        }
    }

    @Test
    public void clusterVmHostNamePatternMatchesNamesWithThePrefixTruncatedTheOldWay() {
        String name = "a234567890b234567890c234567890d234567890e2345";
        Mockito.when(kubernetesCluster.getName()).thenReturn(name);
        Pattern pattern = new KubernetesClusterVmMapCheckWorker(kubernetesCluster, clusterManager).getClusterVmHostNamePattern();
        Assert.assertTrue(pattern.matcher(name.substring(0, 43) + "-node-19a8f2c3d4e").matches());
        Assert.assertTrue(pattern.matcher(name.substring(0, 40) + "-node-19a8f2c3d4e").matches());
        Assert.assertFalse(pattern.matcher(name.substring(0, 41) + "-node-19a8f2c3d4e").matches());
    }

    @Test
    public void listKubernetesNodeNamesReturnsNullWithoutAnAddress() {
        KubernetesClusterVmMapCheckWorker worker = Mockito.spy(new KubernetesClusterVmMapCheckWorker(kubernetesCluster, clusterManager));
        Mockito.doReturn(new Pair<>(null, 22)).when(worker).getKubernetesClusterServerIpSshPort(null);
        Assert.assertNull(worker.listKubernetesNodeNames());
    }

    @Test
    public void listKubernetesNodeNamesReturnsNullOnFailure() {
        KubernetesClusterVmMapCheckWorker worker = Mockito.spy(new KubernetesClusterVmMapCheckWorker(kubernetesCluster, clusterManager));
        Mockito.doThrow(new RuntimeException("test")).when(worker).getKubernetesClusterServerIpSshPort(null);
        Assert.assertNull(worker.listKubernetesNodeNames());
    }

    private KubernetesClusterVmMapCheckWorker mockSshWorker() {
        KubernetesClusterVmMapCheckWorker worker = Mockito.spy(new KubernetesClusterVmMapCheckWorker(kubernetesCluster, clusterManager));
        Mockito.doReturn(new Pair<>("192.0.2.1", 2222)).when(worker).getKubernetesClusterServerIpSshPort(null);
        Mockito.doReturn(null).when(worker).getManagementServerSshPublicKeyFile();
        return worker;
    }

    private MockedStatic<SshHelper> mockSsh(boolean success, String output) {
        MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class);
        sshHelper.when(() -> SshHelper.sshExecute(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(KubernetesClusterVmMapCheckWorker.LIST_NODES_COMMAND),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(new Pair<>(success, output));
        return sshHelper;
    }

    @Test
    public void listKubernetesNodeNamesListsTheNodes() {
        KubernetesClusterVmMapCheckWorker worker = mockSshWorker();
        Mockito.doReturn("core").when(worker).getControlNodeLoginUser();
        try (MockedStatic<SshHelper> ignored = mockSsh(true, "node/k8s-control-1a\nnode/k8s-node-2b\n")) {
            Assert.assertEquals(List.of("k8s-control-1a", "k8s-node-2b"), worker.listKubernetesNodeNames());
        }
    }

    @Test
    public void listKubernetesNodeNamesReturnsNullOnlyWhenKubectlFails() {
        KubernetesClusterVmMapCheckWorker worker = mockSshWorker();
        Mockito.doReturn("core").when(worker).getControlNodeLoginUser();
        try (MockedStatic<SshHelper> ignored = mockSsh(false, "connection refused")) {
            Assert.assertNull(worker.listKubernetesNodeNames());
        }
        try (MockedStatic<SshHelper> ignored = mockSsh(true, "")) {
            Assert.assertEquals(List.of(), worker.listKubernetesNodeNames());
        }
    }

    @Test
    public void listKubernetesNodeNamesUsesTheDefaultUserWhenTheFirstVmIsGone() {
        KubernetesClusterVmMapCheckWorker worker = mockSshWorker();
        Mockito.doThrow(new CloudRuntimeException("gone")).when(worker).getControlNodeLoginUser();
        try (MockedStatic<SshHelper> sshHelper = mockSsh(true, "node/k8s-control-1a")) {
            Assert.assertEquals(List.of("k8s-control-1a"), worker.listKubernetesNodeNames());
            sshHelper.verify(() -> SshHelper.sshExecute(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(KubernetesClusterActionWorker.CLUSTER_NODE_VM_USER), ArgumentMatchers.any(), ArgumentMatchers.any(),
                    ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()));
        }
    }
}
