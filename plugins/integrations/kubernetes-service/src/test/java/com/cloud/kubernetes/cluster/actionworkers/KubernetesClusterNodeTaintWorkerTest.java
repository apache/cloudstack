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

import static com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterNodeTaintWorker.OUT_OF_SERVICE_TAINTED_VMS;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersionVO;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.utils.Pair;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.Silent.class)
public class KubernetesClusterNodeTaintWorkerTest {

    private static final long CLUSTER_ID = 1L;

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private KubernetesClusterDao kubernetesClusterDao;
    @Mock
    private KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    private UserVmDao userVmDao;
    @Mock
    private KubernetesClusterVO clusterVO;
    @Mock
    private KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Mock
    private KubernetesSupportedVersionVO kubernetesVersion;

    private KubernetesClusterNodeTaintWorker worker;

    @Before
    public void setUp() {
        worker = Mockito.spy(new KubernetesClusterNodeTaintWorker(kubernetesCluster, clusterManager));
        worker.kubernetesClusterDao = kubernetesClusterDao;
        worker.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        worker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        worker.userVmDao = userVmDao;
        worker.kubernetesSupportedVersionDao = kubernetesSupportedVersionDao;
        KubernetesClusterNodeTaintWorker.CONFIRMED_TAINTED_VMS.clear();
        Mockito.when(kubernetesCluster.getKubernetesVersionId()).thenReturn(5L);
        Mockito.when(kubernetesSupportedVersionDao.findById(5L)).thenReturn(kubernetesVersion);
        Mockito.when(kubernetesVersion.getSemanticVersion()).thenReturn("1.28.4");
        Mockito.when(kubernetesCluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(clusterVO);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Alert);
    }

    private KubernetesClusterVmMapVO addNode(long vmId, boolean control, boolean etcd, VirtualMachine.State state, String hostName, String ip) {
        KubernetesClusterVmMapVO map = new KubernetesClusterVmMapVO(CLUSTER_ID, vmId, control);
        map.setEtcdNode(etcd);
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getState()).thenReturn(state);
        Mockito.when(vm.getHostName()).thenReturn(hostName);
        Mockito.when(vm.getPrivateIpAddress()).thenReturn(ip);
        Mockito.when(userVmDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        return map;
    }

    private void setTainted(String value) {
        KubernetesClusterDetailsVO detail = new KubernetesClusterDetailsVO(CLUSTER_ID, OUT_OF_SERVICE_TAINTED_VMS, value, false);
        Mockito.when(kubernetesClusterDetailsDao.findDetail(CLUSTER_ID, OUT_OF_SERVICE_TAINTED_VMS)).thenReturn(detail);
    }

    @Test
    public void taintsStoppedNodeAndRecordsIt() {
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Stopped, "K8S-Node-1", "10.0.0.2"),
                addNode(12L, false, false, VirtualMachine.State.Running, "k8s-node-2", "10.0.0.3"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);
        Mockito.doReturn(true).when(worker).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());

        worker.syncOutOfServiceTaints(true);

        Mockito.verify(worker).runOnControlNode(Mockito.anyList(), Mockito.anyList(),
                Mockito.eq("taint nodes k8s-node-1 node.kubernetes.io/out-of-service=nodeshutdown:NoExecute --overwrite"));
        Mockito.verify(worker, Mockito.times(1)).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());
        Mockito.verify(kubernetesClusterDetailsDao).addDetail(CLUSTER_ID, OUT_OF_SERVICE_TAINTED_VMS, "11", false);
    }

    @Test
    public void doesNotResendConfirmedTaint() {
        setTainted("11");
        KubernetesClusterNodeTaintWorker.CONFIRMED_TAINTED_VMS.put(11L, System.currentTimeMillis());
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Stopped, "k8s-node-1", "10.0.0.2"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);

        worker.syncOutOfServiceTaints(true);

        Mockito.verify(worker, Mockito.never()).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());
    }

    @Test
    public void resendsConfirmedTaintAfterTtl() {
        setTainted("11");
        KubernetesClusterNodeTaintWorker.CONFIRMED_TAINTED_VMS.put(11L,
                System.currentTimeMillis() - KubernetesClusterNodeTaintWorker.CONFIRMED_TAINT_TTL_MS - 1);
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Stopped, "k8s-node-1", "10.0.0.2"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);
        Mockito.doReturn(true).when(worker).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());

        worker.syncOutOfServiceTaints(true);

        Mockito.verify(worker, Mockito.times(1)).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());
    }

    @Test
    public void doesNotTaintBeforeKubernetes126() {
        Mockito.when(kubernetesVersion.getSemanticVersion()).thenReturn("1.25.9");
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Stopped, "k8s-node-1", "10.0.0.2"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);

        worker.syncOutOfServiceTaints(true);

        Mockito.verify(worker, Mockito.never()).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());
    }

    @Test
    public void doesNotTaintTransitionalOrUnknownStates() {
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Stopping, "k8s-node-1", "10.0.0.2"),
                addNode(12L, false, false, VirtualMachine.State.Unknown, "k8s-node-2", "10.0.0.3"),
                addNode(13L, false, false, VirtualMachine.State.Migrating, "k8s-node-3", "10.0.0.4"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);

        worker.syncOutOfServiceTaints(true);

        Mockito.verify(worker, Mockito.never()).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());
        Mockito.verify(kubernetesClusterDetailsDao, Mockito.never()).addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void removesTaintWhenVmRunsAgainEvenIfDisabled() {
        setTainted("11");
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Running, "k8s-node-1", "10.0.0.2"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);
        Mockito.doReturn(true).when(worker).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());

        worker.syncOutOfServiceTaints(false);

        Mockito.verify(worker).runOnControlNode(Mockito.anyList(), Mockito.anyList(),
                Mockito.eq("taint nodes k8s-node-1 node.kubernetes.io/out-of-service:NoExecute-"));
        Mockito.verify(kubernetesClusterDetailsDao).removeDetail(CLUSTER_ID, OUT_OF_SERVICE_TAINTED_VMS);
    }

    @Test
    public void recordsBeforeTaintingAndKeepsRecordWhenControlNodeUnreachable() {
        setTainted("11");
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Running, "k8s-node-1", "10.0.0.2"),
                addNode(12L, false, false, VirtualMachine.State.Stopped, "k8s-node-2", "10.0.0.3"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);
        Mockito.doReturn(false).when(worker).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());

        worker.syncOutOfServiceTaints(true);

        Mockito.verify(kubernetesClusterDetailsDao, Mockito.times(1)).addDetail(CLUSTER_ID, OUT_OF_SERVICE_TAINTED_VMS, "11,12", false);
        Mockito.verify(kubernetesClusterDetailsDao, Mockito.never()).removeDetail(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void dropsRecordOfVmNoLongerInCluster() {
        setTainted("11,99");
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Stopped, "k8s-node-1", "10.0.0.2"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);
        Mockito.doReturn(true).when(worker).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());

        worker.syncOutOfServiceTaints(true);

        Mockito.verify(worker, Mockito.times(1)).runOnControlNode(Mockito.anyList(), Mockito.anyList(),
                Mockito.eq("taint nodes k8s-node-1 node.kubernetes.io/out-of-service=nodeshutdown:NoExecute --overwrite"));
        Mockito.verify(kubernetesClusterDetailsDao).addDetail(CLUSTER_ID, OUT_OF_SERVICE_TAINTED_VMS, "11", false);
    }

    @Test
    public void doesNotTaintOnceClusterOperationStarted() {
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control", "10.0.0.1"),
                addNode(11L, false, false, VirtualMachine.State.Stopped, "k8s-node-1", "10.0.0.2"));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Stopping);

        worker.syncOutOfServiceTaints(true);

        Mockito.verify(worker, Mockito.never()).runOnControlNode(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());
    }

    @Test
    public void skipsWhenDisabledAndNothingTainted() {
        worker.syncOutOfServiceTaints(false);

        Mockito.verify(kubernetesClusterVmMapDao, Mockito.never()).listByClusterId(Mockito.anyLong());
    }

    @Test
    public void controlEndpointsUsePerNodePortAndSkipDownControlNodes() {
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Stopped, "k8s-control-1", "10.0.0.1"),
                addNode(11L, true, false, VirtualMachine.State.Running, "k8s-control-2", "10.0.0.2"),
                addNode(12L, false, false, VirtualMachine.State.Running, "k8s-node-1", "10.0.0.3"));
        List<UserVmVO> vms = Arrays.asList(userVmDao.findByIdIncludingRemoved(10L), userVmDao.findByIdIncludingRemoved(11L),
                userVmDao.findByIdIncludingRemoved(12L));
        Mockito.doReturn(new Pair<>("192.0.2.10", KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT))
                .when(worker).getKubernetesClusterServerIpSshPort(null);

        List<Pair<String, Integer>> endpoints = worker.getRunningControlNodeEndpoints(maps, vms);

        Assert.assertEquals(1, endpoints.size());
        Assert.assertEquals("192.0.2.10", endpoints.get(0).first());
        Assert.assertEquals(KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT + 1, endpoints.get(0).second().intValue());
    }

    @Test
    public void runOnControlNodeFallsBackToNextControlNode() throws Exception {
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control-1", "10.0.0.1"),
                addNode(11L, true, false, VirtualMachine.State.Running, "k8s-control-2", "10.0.0.2"),
                addNode(12L, true, false, VirtualMachine.State.Running, "k8s-control-3", "10.0.0.3"));
        List<UserVmVO> vms = Arrays.asList(userVmDao.findByIdIncludingRemoved(10L), userVmDao.findByIdIncludingRemoved(11L),
                userVmDao.findByIdIncludingRemoved(12L));
        Mockito.doReturn(new Pair<>("192.0.2.10", 2222)).when(worker).getKubernetesClusterServerIpSshPort(null);
        Mockito.doReturn(null).when(worker).getManagementServerSshPublicKeyFile();
        Mockito.doReturn("cloud").when(worker).getControlNodeLoginUser();
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.eq(2222), Mockito.anyString(), Mockito.any(), Mockito.any(),
                    Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenThrow(new Exception("unreachable"));
            ssh.when(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.eq(2223), Mockito.anyString(), Mockito.any(), Mockito.any(),
                    Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new Pair<>(false, "connection refused"));
            ssh.when(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.eq(2224), Mockito.anyString(), Mockito.any(), Mockito.any(),
                    Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new Pair<>(true, ""));

            Assert.assertTrue(worker.runOnControlNode(maps, vms, "taint nodes x key:NoExecute-"));
            Assert.assertTrue(worker.runOnControlNode(maps, vms, "taint nodes y key:NoExecute-"));

            ssh.verify(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.eq(2222), Mockito.anyString(), Mockito.any(), Mockito.any(),
                    Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()), Mockito.times(1));
            ssh.verify(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.eq(2223), Mockito.anyString(), Mockito.any(), Mockito.any(),
                    Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()), Mockito.times(2));
            ssh.verify(() -> SshHelper.sshExecute(Mockito.anyString(), Mockito.eq(2224), Mockito.anyString(), Mockito.any(), Mockito.any(),
                    Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()), Mockito.times(2));
        }
    }

    @Test
    public void notFoundIsOnlyAcceptedForMissingTaintOrNode() {
        Pattern p = Pattern.compile(KubernetesClusterNodeTaintWorker.NOT_FOUND_PATTERN);
        Assert.assertTrue(p.matcher("error: taint \"node.kubernetes.io/out-of-service:NoExecute\" not found").find());
        Assert.assertTrue(p.matcher("Error from server (NotFound): nodes \"k8s-node-1\" not found").find());
        Assert.assertFalse(p.matcher("sudo: /opt/bin/kubectl: command not found").find());
    }

    @Test
    public void controlEndpointsUsePrivateIpOnSharedNetwork() {
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(
                addNode(10L, true, false, VirtualMachine.State.Running, "k8s-control-1", "10.0.0.1"));
        List<UserVmVO> vms = Arrays.asList(userVmDao.findByIdIncludingRemoved(10L));
        Mockito.doReturn(new Pair<>(null, KubernetesClusterActionWorker.DEFAULT_SSH_PORT))
                .when(worker).getKubernetesClusterServerIpSshPort(null);

        List<Pair<String, Integer>> endpoints = worker.getRunningControlNodeEndpoints(maps, vms);

        Assert.assertEquals(1, endpoints.size());
        Assert.assertEquals("10.0.0.1", endpoints.get(0).first());
        Assert.assertEquals(KubernetesClusterActionWorker.DEFAULT_SSH_PORT, endpoints.get(0).second().intValue());
    }
}
