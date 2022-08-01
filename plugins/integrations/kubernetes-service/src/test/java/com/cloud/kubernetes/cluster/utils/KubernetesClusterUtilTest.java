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

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.utils.Pair;
import com.cloud.utils.ssh.SshHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SshHelper.class)
public class KubernetesClusterUtilTest {
    String ipAddress = "10.1.1.1";
    int port = 2222;
    String user = "user";
    File sshKeyFile = Mockito.mock(File.class);
    String hostName = "host";

    private void mockSshHelperExecuteThrowAndTestVersionMatch() {
        try {
            Mockito.when(SshHelper.sshExecute(ipAddress, port, user, sshKeyFile, null, KubernetesClusterUtil.CLUSTER_NODE_VERSION_COMMAND, 10000, 10000, 20000)).thenThrow(Exception.class);
        } catch (Exception e) {
            Assert.fail(String.format("Exception: %s", e.getMessage()));
        }
        Assert.assertFalse(KubernetesClusterUtil.clusterNodeVersionMatches("1.24.0", false, ipAddress, port, user, sshKeyFile, hostName));
    }

    private void mockSshHelperExecuteAndTestVersionMatch(boolean status, String response, boolean isControlNode, boolean expectedResult) {
        try {
            Mockito.when(SshHelper.sshExecute(ipAddress, port, user, sshKeyFile, null, KubernetesClusterUtil.CLUSTER_NODE_VERSION_COMMAND, 10000, 10000, 20000)).thenReturn(new Pair<>(status, response));
        } catch (Exception e) {
            Assert.fail(String.format("Exception: %s", e.getMessage()));
        }
        boolean result = KubernetesClusterUtil.clusterNodeVersionMatches("1.24.0", isControlNode, ipAddress, port, user, sshKeyFile, hostName);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testClusterNodeVersionMatches() {
        PowerMockito.mockStatic(SshHelper.class);
        String v1233WorkerNodeOutput = "Client Version: v1.23.3\n" +
                "The connection to the server localhost:8080 was refused - did you specify the right host or port?";
        String v1240WorkerNodeOutput = "Client Version: v1.24.0\n" +
                "Kustomize Version: v4.5.4\n" +
                "The connection to the server localhost:8080 was refused - did you specify the right host or port?";
        String v1240ControlNodeOutput = "Client Version: v1.24.0\n" +
                "Kustomize Version: v4.5.4\n" +
                "Server Version: v1.24.0";
        mockSshHelperExecuteAndTestVersionMatch(true, v1240WorkerNodeOutput, false, true);

        mockSshHelperExecuteAndTestVersionMatch(true, v1240ControlNodeOutput, true, true);

        mockSshHelperExecuteAndTestVersionMatch(true, v1240WorkerNodeOutput, true, false);

        mockSshHelperExecuteAndTestVersionMatch(false, v1240WorkerNodeOutput, false, false);

        mockSshHelperExecuteAndTestVersionMatch(true, v1233WorkerNodeOutput, false, false);

        mockSshHelperExecuteAndTestVersionMatch(true, "Client Version: v1.24.0\n" +
                "Kustomize Version: v4.5.4\n" +
                "Server Version: v1.23.0", true, false);

        mockSshHelperExecuteAndTestVersionMatch(true, null, false, false);

        mockSshHelperExecuteAndTestVersionMatch(false, "-\n-", false, false);

        mockSshHelperExecuteAndTestVersionMatch(false, "1.24.0", false, false);

        mockSshHelperExecuteThrowAndTestVersionMatch();
    }
}
