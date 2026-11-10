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

import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
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

    @Test
    public void testClusterNodeVersionMatchesRecordsVersion() {
        KubernetesClusterVmMapDao vmMapDao = Mockito.mock(KubernetesClusterVmMapDao.class);
        KubernetesClusterVmMapVO vmMapVO = new KubernetesClusterVmMapVO(1L, 2L, false);
        vmMapVO.setNodeVersion("1.24.0");
        Mockito.when(vmMapDao.getClusterMapFromVmId(2L)).thenReturn(vmMapVO);
        try (MockedStatic<SshHelper> sshHelper = Mockito.mockStatic(SshHelper.class)) {
            sshHelper.when(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
                    Mockito.any(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                    .thenReturn(new Pair<>(true, "v1.25.1\n"));

            Assert.assertTrue(KubernetesClusterUtil.clusterNodeVersionMatches("1.25.1", "10.1.1.1", 2222, "cloud",
                    new File("id_rsa"), "node-1", System.currentTimeMillis() + 60000, 1, 2L, vmMapDao));
        }
        // in the same form as for a new node, not the kubectl output
        Assert.assertEquals("1.25.1", vmMapVO.getNodeVersion());
        Mockito.verify(vmMapDao).update(vmMapVO.getId(), vmMapVO);
    }
}
