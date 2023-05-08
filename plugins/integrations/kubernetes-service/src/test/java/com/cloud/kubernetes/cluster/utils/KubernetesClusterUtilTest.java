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
        Pair<Boolean, String> resultPair = null;
        boolean result = KubernetesClusterUtil.clusterNodeVersionMatches(resultPair, "1.24.0");
        Assert.assertFalse(result);
    }

    private void mockSshHelperExecuteAndTestVersionMatch(boolean status, String response, boolean expectedResult) {
        Pair<Boolean, String> resultPair = new Pair<>(status, response);
        boolean result = KubernetesClusterUtil.clusterNodeVersionMatches(resultPair, "1.24.0");
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testClusterNodeVersionMatches() {
        PowerMockito.mockStatic(SshHelper.class);
        String v1233WorkerNodeOutput = "v1.23.3";
        String v1240WorkerNodeOutput = "v1.24.0";
        mockSshHelperExecuteAndTestVersionMatch(true, v1240WorkerNodeOutput, true);

        mockSshHelperExecuteAndTestVersionMatch(true, v1233WorkerNodeOutput, false);

        mockSshHelperExecuteAndTestVersionMatch(false, v1240WorkerNodeOutput, false);

        mockSshHelperExecuteAndTestVersionMatch(false, v1233WorkerNodeOutput, false);

        mockSshHelperExecuteThrowAndTestVersionMatch();
    }
}
