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
import com.cloud.network.IpAddress;
import com.cloud.utils.Pair;
import com.cloud.utils.net.Ip;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.uservm.UserVm;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterAddWorkerTest {

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private UserVm userVm;
    @Mock
    private IpAddress publicIp;

    private final File pkFile = new File("/dev/null");
    private KubernetesClusterAddWorker worker;

    @Before
    public void setUp() {
        worker = Mockito.spy(new KubernetesClusterAddWorker(kubernetesCluster, clusterManager));
        Mockito.doReturn("cloud").when(worker).getControlNodeLoginUser();
        Mockito.when(publicIp.getAddress()).thenReturn(new Ip("10.1.1.1"));
        Mockito.when(userVm.getUuid()).thenReturn("vm-uuid");
    }

    @Test
    public void getNodeProvisioningResetCommandClearsMarkerAndKubeadmState() {
        Assert.assertEquals("sudo rm -rf /var/lib/cloud/instances/vm-uuid/sem/* && sudo rm -f /home/cloud/success && " +
                "{ sudo systemctl stop deploy-kube-system 2>/dev/null; " +
                "sudo umount /mnt/k8sdisk 2>/dev/null; mountpoint -q /mnt/k8sdisk || sudo rm -rf /mnt/k8sdisk; sudo rm -rf /aux-dwnld; " +
                "sudo systemctl disable --now kubelet 2>/dev/null; " +
                "if PATH=$PATH:/opt/bin command -v kubeadm >/dev/null; then " +
                "sudo env PATH=$PATH:/opt/bin kubeadm reset -f; fi; }",
                worker.getNodeProvisioningResetCommand(userVm));
    }

    private MockedStatic<SshHelper> mockSsh() {
        return Mockito.mockStatic(SshHelper.class);
    }

    private void verifyResetCommandRun(MockedStatic<SshHelper> sshHelper) {
        String command = worker.getNodeProvisioningResetCommand(userVm);
        sshHelper.verify(() -> SshHelper.sshExecute(ArgumentMatchers.eq("10.1.1.1"), ArgumentMatchers.eq(2222),
                ArgumentMatchers.eq("cloud"), ArgumentMatchers.eq(pkFile), ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(command),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()));
    }

    @Test
    public void resetNodeProvisioningStateRunsResetCommand() {
        try (MockedStatic<SshHelper> sshHelper = mockSsh()) {
            sshHelper.when(() -> SshHelper.sshExecute(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(new Pair<>(true, ""));
            worker.resetNodeProvisioningState(userVm, publicIp, pkFile, 2222);
            verifyResetCommandRun(sshHelper);
        }
    }

    @Test
    public void resetNodeProvisioningStateToleratesUnreachableNode() {
        try (MockedStatic<SshHelper> sshHelper = mockSsh()) {
            sshHelper.when(() -> SshHelper.sshExecute(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenThrow(new RuntimeException("unreachable"));
            worker.resetNodeProvisioningState(userVm, publicIp, pkFile, 2222);
            verifyResetCommandRun(sshHelper);
        }
    }

    @Test
    public void resetNodeProvisioningStateToleratesCommandFailure() {
        try (MockedStatic<SshHelper> sshHelper = mockSsh()) {
            sshHelper.when(() -> SshHelper.sshExecute(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(new Pair<>(false, "kubeadm failed"));
            worker.resetNodeProvisioningState(userVm, publicIp, pkFile, 2222);
            verifyResetCommandRun(sshHelper);
        }
    }
}
