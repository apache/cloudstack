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
import com.cloud.utils.Pair;
import com.cloud.utils.ssh.SshHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterUtilTest {

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

    private String waitForEtcd(MockedStatic<SshHelper> sshHelper, long timeoutMs) {
        KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
        return KubernetesClusterUtil.waitForEtcdOnEtcdNode(cluster, "etcd-1", "10.1.1.10", 50000, "cloud", null,
                System.currentTimeMillis() + timeoutMs, 1);
    }

    private void mockEtcdNodeStatus(MockedStatic<SshHelper> sshHelper, Pair<Boolean, String> first, Pair<Boolean, String>... next) {
        sshHelper.when(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.eq(KubernetesClusterUtil.ETCD_NODE_STATUS_COMMAND), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(first, next);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWaitForEtcdOnEtcdNodeRunning() {
        try (MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class)) {
            mockEtcdNodeStatus(sshHelper, new Pair<>(true, "WAITING\n"), new Pair<>(true, "ACTIVE\n"), new Pair<>(true, "ACTIVE\n"));
            Assert.assertNull(waitForEtcd(sshHelper, 10000));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWaitForEtcdOnEtcdNodeSetupFailed() {
        try (MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class)) {
            String reason = "The Kubernetes ISO does not include the etcd binaries";
            mockEtcdNodeStatus(sshHelper, new Pair<>(true, "WAITING\n"), new Pair<>(true, "FAILED: " + reason + "\n"));
            Assert.assertEquals(reason, waitForEtcd(sshHelper, 10000));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWaitForEtcdOnEtcdNodeNeverStarts() {
        try (MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class)) {
            mockEtcdNodeStatus(sshHelper, new Pair<>(true, "WAITING\n"));
            String failure = waitForEtcd(sshHelper, 100);
            Assert.assertNotNull(failure);
            Assert.assertTrue(failure, failure.contains("the etcd service was not running"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWaitForEtcdOnEtcdNodeKeepsRestarting() {
        try (MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class)) {
            // Running on one check, down on the next: never running twice in a row
            mockEtcdNodeStatus(sshHelper, new Pair<>(true, "ACTIVE\n"), new Pair<>(true, "WAITING\n"), new Pair<>(true, "ACTIVE\n"),
                    new Pair<>(true, "WAITING\n"));
            Assert.assertNotNull(waitForEtcd(sshHelper, 100));
        }
    }

    @Test
    public void testWaitForEtcdOnEtcdNodeNotReachable() {
        try (MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class)) {
            sshHelper.when(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(), Mockito.any(),
                    Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenThrow(new java.io.IOException("Connection refused"));
            String failure = waitForEtcd(sshHelper, 100);
            Assert.assertNotNull(failure);
            Assert.assertTrue(failure, failure.contains("not reachable over SSH"));
        }
    }
}
