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
package com.cloud.kubernetes.cluster;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.UpdateKubernetesClusterCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterRollingUpdateWorker;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterAffinityGroupMapDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersionVO;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

/** Tests of updateKubernetesCluster: validation, the stored settings, cancel, and the Updating state */
@RunWith(MockitoJUnitRunner.Silent.class)
public class KubernetesClusterManagerImplUpdateTest {

    private static final long CLUSTER_ID = 1L;
    private static final long CLUSTER_OFFERING = 3L;

    @Mock
    KubernetesClusterDao kubernetesClusterDao;
    @Mock
    KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    @Mock
    KubernetesClusterAffinityGroupMapDao kubernetesClusterAffinityGroupMapDao;
    @Mock
    KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Mock
    ServiceOfferingDao serviceOfferingDao;
    @Mock
    VolumeDao volumeDao;
    @Mock
    DiskOfferingDao diskOfferingDao;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    AffinityGroupDao affinityGroupDao;
    @Mock
    AffinityGroupVMMapDao affinityGroupVMMapDao;
    @Mock
    AsyncJobDao asyncJobDao;
    @Mock
    AccountManager accountManager;
    @Mock
    AccountDao accountDao;

    @Spy
    @InjectMocks
    KubernetesClusterManagerImpl manager;

    MockedStatic<Transaction> transactionMock;
    KubernetesClusterVO cluster;
    final List<KubernetesClusterVmMapVO> vmMaps = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(KubernetesClusterService.KubernetesServiceEnabled, "true");
        transactionMock = Mockito.mockStatic(Transaction.class);
        transactionMock.when(() -> Transaction.execute(Mockito.any(TransactionCallback.class)))
                .thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        cluster = Mockito.spy(new KubernetesClusterVO());
        Mockito.doReturn(CLUSTER_ID).when(cluster).getId();
        Mockito.doReturn("cluster").when(cluster).getName();
        cluster.setState(KubernetesCluster.State.Running);
        cluster.setClusterType(KubernetesCluster.ClusterType.CloudManaged);
        cluster.setServiceOfferingId(CLUSTER_OFFERING);
        cluster.setTemplateId(4L);
        cluster.setKubernetesVersionId(5L);
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(cluster);
        Mockito.when(kubernetesClusterDao.lockRow(CLUSTER_ID, true)).thenReturn(cluster);
        Mockito.when(kubernetesSupportedVersionDao.findById(5L)).thenReturn(Mockito.mock(KubernetesSupportedVersionVO.class));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(vmMaps);
        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(Mockito.anyLong(), Mockito.anyString())).thenReturn(List.of());
        Mockito.when(affinityGroupVMMapDao.listAffinityGroupIdsByVmId(Mockito.anyLong())).thenReturn(List.of());
        Mockito.when(volumeDao.findByInstanceAndType(Mockito.anyLong(), Mockito.eq(Volume.Type.ROOT))).thenReturn(List.of());
    }

    @After
    public void tearDown() {
        transactionMock.close();
        CallContext.unregister();
    }

    private UpdateKubernetesClusterCmd cmd(boolean rollout, Integer maxUnavailable, Map<String, Long> offerings, Map<String, Long> rootDiskSizes,
                                           Map<String, List<Long>> affinityGroups) {
        UpdateKubernetesClusterCmd cmd = Mockito.mock(UpdateKubernetesClusterCmd.class);
        Mockito.when(cmd.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(cmd.getActualCommandName()).thenReturn(BaseCmd.getCommandNameByClass(UpdateKubernetesClusterCmd.class));
        Mockito.when(cmd.isRollout()).thenReturn(rollout);
        Mockito.when(cmd.getMaxUnavailable()).thenReturn(maxUnavailable);
        Mockito.when(cmd.getServiceOfferingNodeTypeMap()).thenReturn(offerings);
        Mockito.when(cmd.getRootDiskSizeNodeTypeMap()).thenReturn(rootDiskSizes);
        Mockito.when(cmd.getAffinityGroupNodeTypeMap()).thenReturn(affinityGroups);
        return cmd;
    }

    private UpdateKubernetesClusterCmd cancelCmd() {
        UpdateKubernetesClusterCmd cmd = cmd(false, null, null, null, null);
        Mockito.when(cmd.isCancelRollout()).thenReturn(true);
        return cmd;
    }

    private void mockOffering(long id) {
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.getId()).thenReturn(id);
        Mockito.when(serviceOfferingDao.findById(id)).thenReturn(offering);
        Mockito.doNothing().when(manager).checkServiceOfferingForNodesScale(Mockito.eq(offering), Mockito.any(), Mockito.any());
    }

    private VMInstanceVO addVm(long vmId, KubernetesServiceHelper.KubernetesClusterNodeType type, long offeringId, Long rootSizeGb) {
        KubernetesClusterVmMapVO vmMap = new KubernetesClusterVmMapVO(CLUSTER_ID, vmId, type == CONTROL);
        vmMap.setEtcdNode(type == ETCD);
        vmMaps.add(vmMap);
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getHostName()).thenReturn("vm" + vmId);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(offeringId);
        Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        if (rootSizeGb != null) {
            VolumeVO root = Mockito.mock(VolumeVO.class);
            Mockito.when(root.getSize()).thenReturn(rootSizeGb << 30);
            Mockito.when(root.getDiskOfferingId()).thenReturn(50L);
            Mockito.when(volumeDao.findByInstanceAndType(vmId, Volume.Type.ROOT)).thenReturn(List.of(root));
        }
        return vm;
    }

    private KubernetesClusterRollingUpdateWorker mockWorker() {
        KubernetesClusterRollingUpdateWorker worker = Mockito.mock(KubernetesClusterRollingUpdateWorker.class);
        Mockito.doReturn(worker).when(manager).createRollingUpdateWorker(Mockito.any(), Mockito.anyInt());
        Mockito.when(worker.rollout()).thenReturn(true);
        return worker;
    }

    // rollout=false

    @Test
    public void updateWithoutRolloutOnlyStoresTheSettings() {
        cluster.setState(KubernetesCluster.State.Stopped);
        mockOffering(7L);
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(com.cloud.user.AccountVO.class));
        Mockito.when(affinityGroupDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(AffinityGroupVO.class));
        KubernetesClusterRollingUpdateWorker worker = mockWorker();

        Assert.assertTrue(manager.updateKubernetesCluster(cmd(false, null, Map.of(WORKER.name(), 7L), Map.of(CONTROL.name(), 40L),
                Map.of(WORKER.name(), List.of(8L, 8L, 9L)))));

        Assert.assertEquals(Long.valueOf(7L), cluster.getWorkerNodeServiceOfferingId());
        // gen 1 to gen 2: the other node types keep the offering of the cluster
        Assert.assertEquals(Long.valueOf(CLUSTER_OFFERING), cluster.getControlNodeServiceOfferingId());
        Assert.assertNull(cluster.getEtcdNodeServiceOfferingId());
        Assert.assertEquals(Long.valueOf(40L), cluster.getControlNodeRootDiskSize());
        Assert.assertNull(cluster.getWorkerNodeRootDiskSize());
        Mockito.verify(kubernetesClusterAffinityGroupMapDao).removeByClusterIdAndNodeType(CLUSTER_ID, WORKER.name());
        Mockito.verify(kubernetesClusterAffinityGroupMapDao, Mockito.times(2)).persist(Mockito.any());
        Mockito.verify(worker, Mockito.never()).rollout();
    }

    @Test
    public void updateRefusesWhileAnotherOperationRuns() {
        for (KubernetesCluster.State state : List.of(KubernetesCluster.State.Updating, KubernetesCluster.State.Scaling, KubernetesCluster.State.Destroying)) {
            cluster.setState(state);
            Assert.assertThrows(state.name(), InvalidParameterValueException.class,
                    () -> manager.updateKubernetesCluster(cmd(false, null, null, Map.of(WORKER.name(), 40L), null)));
        }
        Assert.assertNull(cluster.getWorkerNodeRootDiskSize());
    }

    @Test
    public void rolloutNeedsRunningOrAlert() {
        KubernetesClusterRollingUpdateWorker worker = mockWorker();
        cluster.setState(KubernetesCluster.State.Stopped);
        Assert.assertThrows(InvalidParameterValueException.class, () -> manager.updateKubernetesCluster(cmd(true, null, null, null, null)));

        cluster.setState(KubernetesCluster.State.Alert);
        Assert.assertTrue(manager.updateKubernetesCluster(cmd(true, null, null, null, null)));
        Mockito.verify(manager).createRollingUpdateWorker(cluster, 1);
        Mockito.verify(worker).rollout();
    }

    @Test
    public void rolloutTakesTheClusterToUpdatingBeforeStoringTheSettings() {
        KubernetesClusterRollingUpdateWorker worker = mockWorker();
        mockOffering(7L);
        Mockito.doAnswer(i -> {
            Assert.assertNull("stored before the cluster was Updating", cluster.getWorkerNodeServiceOfferingId());
            return null;
        }).when(worker).begin();

        Assert.assertTrue(manager.updateKubernetesCluster(cmd(true, null, Map.of(WORKER.name(), 7L), null, null)));

        InOrder order = Mockito.inOrder(worker);
        order.verify(worker).begin();
        order.verify(worker).rollout();
        Assert.assertEquals(Long.valueOf(7L), cluster.getWorkerNodeServiceOfferingId());
    }

    @Test
    public void rolloutReleasesTheClusterIfTheSettingsCanNotBeStored() {
        KubernetesClusterRollingUpdateWorker worker = mockWorker();
        Mockito.doThrow(new CloudRuntimeException("db down")).when(manager).convertToNodeTypeSettings(Mockito.any());

        Assert.assertThrows(CloudRuntimeException.class, () -> manager.updateKubernetesCluster(cmd(true, null, null, null, null)));

        // the last error, of a rollout that did node work, stays
        Mockito.verify(worker).abort();
        Mockito.verify(worker, Mockito.never()).release(Mockito.any());
        Mockito.verify(worker, Mockito.never()).rollout();
    }

    @Test
    public void rolloutWithoutSettingsResumes() {
        KubernetesClusterRollingUpdateWorker worker = mockWorker();

        Assert.assertTrue(manager.updateKubernetesCluster(cmd(true, null, Map.of(), Map.of(), Map.of())));

        Mockito.verify(worker).rollout();
    }

    // gen 1 to gen 2

    @Test
    public void convertCopiesTheClusterOfferingAndCommonAffinityGroupsButNotTemplates() {
        cluster.setEtcdNodeCount(3L);
        cluster.setWorkerNodeServiceOfferingId(9L);
        addVm(10L, WORKER, 9L, null);
        addVm(11L, WORKER, 9L, null);
        addVm(12L, CONTROL, CLUSTER_OFFERING, null);
        addVm(13L, CONTROL, CLUSTER_OFFERING, null);
        Mockito.when(affinityGroupVMMapDao.listAffinityGroupIdsByVmId(10L)).thenReturn(List.of(20L, 30L));
        Mockito.when(affinityGroupVMMapDao.listAffinityGroupIdsByVmId(11L)).thenReturn(List.of(20L, 30L));
        Mockito.when(affinityGroupVMMapDao.listAffinityGroupIdsByVmId(12L)).thenReturn(List.of(20L));
        Mockito.when(affinityGroupVMMapDao.listAffinityGroupIdsByVmId(13L)).thenReturn(List.of(21L));
        AffinityGroupVO antiAffinity = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(antiAffinity.getType()).thenReturn("host anti-affinity");
        AffinityGroupVO dedication = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(dedication.getType()).thenReturn("ExplicitDedication");
        Mockito.when(affinityGroupDao.findById(20L)).thenReturn(antiAffinity);
        Mockito.when(affinityGroupDao.findById(21L)).thenReturn(antiAffinity);
        Mockito.when(affinityGroupDao.findById(30L)).thenReturn(dedication);

        manager.convertToNodeTypeSettings(cluster);

        Assert.assertEquals(Long.valueOf(CLUSTER_OFFERING), cluster.getControlNodeServiceOfferingId());
        Assert.assertEquals(Long.valueOf(9L), cluster.getWorkerNodeServiceOfferingId());
        Assert.assertEquals(Long.valueOf(CLUSTER_OFFERING), cluster.getEtcdNodeServiceOfferingId());
        Assert.assertNull(cluster.getControlNodeTemplateId());
        Assert.assertNull(cluster.getWorkerNodeTemplateId());
        // the workers share group 20, the explicit dedication group left out; the control nodes share none
        Mockito.verify(kubernetesClusterAffinityGroupMapDao).persist(Mockito.argThat(map -> WORKER.name().equals(map.getNodeType()) && map.getAffinityGroupId() == 20L));
        Mockito.verify(kubernetesClusterAffinityGroupMapDao, Mockito.times(1)).persist(Mockito.any());
    }

    // validation

    @Test
    public void validationRefusesBadParameters() {
        addVm(10L, WORKER, CLUSTER_OFFERING, 20L);
        addVm(11L, WORKER, CLUSTER_OFFERING, 20L);
        addVm(12L, WORKER, CLUSTER_OFFERING, 20L);
        List<UpdateKubernetesClusterCmd> refused = List.of(
                cmd(false, null, null, null, null),
                cmd(false, 1, null, Map.of(WORKER.name(), 30L), null),
                cmd(true, 0, null, null, null),
                cmd(true, 3, null, null, null),
                cmd(false, null, null, Map.of(ETCD.name(), 30L), null),
                cmd(false, null, null, Map.of(WORKER.name(), 0L), null),
                // a rollout only grows disks
                cmd(true, null, null, Map.of(WORKER.name(), 10L), null));
        for (UpdateKubernetesClusterCmd cmd : refused) {
            Assert.assertThrows(InvalidParameterValueException.class, () -> manager.validateKubernetesClusterUpdateParameters(cmd, cluster));
        }
        manager.validateKubernetesClusterUpdateParameters(cmd(true, 2, null, Map.of(WORKER.name(), 30L), null), cluster);
        manager.validateKubernetesClusterUpdateParameters(cmd(true, 1, null, null, null), cluster);
        // stored for new nodes only
        manager.validateKubernetesClusterUpdateParameters(cmd(false, null, null, Map.of(WORKER.name(), 10L), null), cluster);
    }

    @Test
    public void rolloutRefusesSettingsOfAControlNodeThatCanNotBeRestarted() {
        addVm(10L, CONTROL, CLUSTER_OFFERING, null);
        addVm(11L, WORKER, CLUSTER_OFFERING, null);
        mockOffering(7L);
        KubernetesClusterRollingUpdateWorker worker = mockWorker();

        InvalidParameterValueException e = Assert.assertThrows(InvalidParameterValueException.class,
                () -> manager.updateKubernetesCluster(cmd(true, null, Map.of(CONTROL.name(), 7L), null, null)));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("The 1 control node(s)"));
        Mockito.verify(worker).abort();
        Mockito.verify(worker, Mockito.never()).rollout();
    }

    @Test
    public void rolloutRefusesSettingsOfTwoEtcdNodes() {
        cluster.setEtcdNodeCount(2L);
        addVm(10L, CONTROL, CLUSTER_OFFERING, null);
        addVm(11L, CONTROL, CLUSTER_OFFERING, null);
        addVm(12L, ETCD, CLUSTER_OFFERING, null);
        addVm(13L, ETCD, CLUSTER_OFFERING, null);
        mockOffering(7L);
        KubernetesClusterRollingUpdateWorker worker = mockWorker();

        InvalidParameterValueException e = Assert.assertThrows(InvalidParameterValueException.class,
                () -> manager.updateKubernetesCluster(cmd(true, null, Map.of(ETCD.name(), 7L), null, null)));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("The 2 etcd node(s)"));
        // two control nodes with external etcd: one serves the API while the other restarts
        Assert.assertTrue(manager.updateKubernetesCluster(cmd(true, null, Map.of(CONTROL.name(), 7L), null, null)));
        Mockito.verify(worker).rollout();
    }

    @Test
    public void rolloutLeavesAnUnrollableNodeThatDifferedAlreadyOrIsStoppedToTheRollout() {
        VMInstanceVO control = addVm(10L, CONTROL, 9L, null);
        addVm(11L, WORKER, CLUSTER_OFFERING, null);
        mockOffering(7L);
        KubernetesClusterRollingUpdateWorker worker = mockWorker();

        // it differed from the settings before the call: the rollout skips and reports it
        Assert.assertTrue(manager.updateKubernetesCluster(cmd(true, null, Map.of(CONTROL.name(), 7L), null, null)));

        Mockito.when(control.getServiceOfferingId()).thenReturn(7L);
        Mockito.when(control.getState()).thenReturn(VirtualMachine.State.Stopped);
        mockOffering(8L);
        Assert.assertTrue(manager.updateKubernetesCluster(cmd(true, null, Map.of(CONTROL.name(), 8L), null, null)));
        Mockito.verify(worker, Mockito.times(2)).rollout();
    }

    @Test
    public void rolloutAcceptsSettingsThatRestartNoUnrollableNode() {
        addVm(10L, CONTROL, CLUSTER_OFFERING, null);
        addVm(11L, WORKER, CLUSTER_OFFERING, null);
        mockOffering(7L);
        mockOffering(CLUSTER_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = mockWorker();

        // the offering the control node has already, with the template size of its disk: nothing to restart it for
        Assert.assertTrue(manager.updateKubernetesCluster(cmd(true, null, Map.of(CONTROL.name(), CLUSTER_OFFERING, WORKER.name(), 7L), null, null)));
        Mockito.verify(worker).rollout();
    }

    @Test
    public void explicitDedicationGroupIsRefusedForANodeType() {
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(com.cloud.user.AccountVO.class));
        AffinityGroupVO dedication = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(dedication.getType()).thenReturn("ExplicitDedication");
        Mockito.when(affinityGroupDao.findById(30L)).thenReturn(dedication);

        InvalidParameterValueException e = Assert.assertThrows(InvalidParameterValueException.class,
                () -> manager.validateKubernetesClusterUpdateParameters(cmd(false, null, null, null, Map.of(WORKER.name(), List.of(30L))), cluster));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("explicit dedication"));
    }

    @Test
    public void rolloutRefusesToGrowADiskOfAStrictOffering() {
        addVm(10L, WORKER, CLUSTER_OFFERING, 20L);
        DiskOfferingVO strict = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(strict.getDiskSizeStrictness()).thenReturn(true);
        Mockito.when(diskOfferingDao.findById(50L)).thenReturn(strict);

        Assert.assertThrows(InvalidParameterValueException.class,
                () -> manager.validateKubernetesClusterUpdateParameters(cmd(true, null, null, Map.of(WORKER.name(), 30L), null), cluster));
    }

    // cancel

    @Test
    public void cancelMarksTheRolloutInProgress() {
        cluster.setState(KubernetesCluster.State.Updating);

        Assert.assertTrue(manager.updateKubernetesCluster(cancelCmd()));

        Mockito.verify(kubernetesClusterDetailsDao).addDetail(CLUSTER_ID, KubernetesClusterManagerImpl.ROLLOUT_CANCEL_DETAIL, "true", false);
    }

    @Test
    public void cancelRefusesWithoutARolloutOrWithOtherParameters() {
        Assert.assertThrows(InvalidParameterValueException.class, () -> manager.updateKubernetesCluster(cancelCmd()));
        cluster.setState(KubernetesCluster.State.Updating);
        UpdateKubernetesClusterCmd cmd = cancelCmd();
        Mockito.when(cmd.isRollout()).thenReturn(true);
        Assert.assertThrows(InvalidParameterValueException.class, () -> manager.updateKubernetesCluster(cmd));
        Mockito.verify(kubernetesClusterDetailsDao, Mockito.never()).addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    // the Updating state

    @Test
    public void noOtherOperationCanStartWhileUpdating() {
        for (KubernetesCluster.Event event : List.of(KubernetesCluster.Event.StartRequested, KubernetesCluster.Event.StopRequested,
                KubernetesCluster.Event.DestroyRequested, KubernetesCluster.Event.RecoveryRequested, KubernetesCluster.Event.AutoscaleRequested,
                KubernetesCluster.Event.ScaleUpRequested, KubernetesCluster.Event.ScaleDownRequested, KubernetesCluster.Event.AddNodeRequested,
                KubernetesCluster.Event.RemoveNodeRequested, KubernetesCluster.Event.UpgradeRequested, KubernetesCluster.Event.UpdateRequested,
                KubernetesCluster.Event.FaultsDetected)) {
            Assert.assertThrows(event.name(), NoTransitionException.class,
                    () -> KubernetesCluster.State.getStateMachine().getNextState(KubernetesCluster.State.Updating, event));
        }
    }

    @Test
    public void updatingEndsInRunningOrAlert() throws NoTransitionException {
        Assert.assertEquals(KubernetesCluster.State.Updating, KubernetesCluster.State.getStateMachine().getNextState(KubernetesCluster.State.Running,
                KubernetesCluster.Event.UpdateRequested));
        Assert.assertEquals(KubernetesCluster.State.Updating, KubernetesCluster.State.getStateMachine().getNextState(KubernetesCluster.State.Alert,
                KubernetesCluster.Event.UpdateRequested));
        Assert.assertEquals(KubernetesCluster.State.Running, KubernetesCluster.State.getStateMachine().getNextState(KubernetesCluster.State.Updating,
                KubernetesCluster.Event.OperationSucceeded));
        Assert.assertEquals(KubernetesCluster.State.Alert, KubernetesCluster.State.getStateMachine().getNextState(KubernetesCluster.State.Updating,
                KubernetesCluster.Event.OperationFailed));
        Assert.assertTrue(KubernetesClusterManagerImpl.OPERATION_STATES.contains(KubernetesCluster.State.Updating));
    }

    @Test
    public void orphanedRolloutGoesToAlertAndRecountsCapacity() throws Exception {
        cluster.setState(KubernetesCluster.State.Updating);
        Mockito.when(asyncJobDao.findInstancePendingAsyncJob(ApiCommandResourceType.KubernetesCluster.toString(), CLUSTER_ID)).thenReturn(null);
        Mockito.when(kubernetesClusterDao.updateState(KubernetesCluster.State.Updating, KubernetesCluster.Event.OperationFailed,
                KubernetesCluster.State.Alert, cluster, null)).thenReturn(true);
        Mockito.doNothing().when(manager).updateKubernetesClusterCapacity(CLUSTER_ID);

        manager.recoverOrphanedOperationState(CLUSTER_ID);

        Mockito.verify(manager).updateKubernetesClusterCapacity(CLUSTER_ID);
    }

    // out of date nodes and capacity

    @Test
    public void nodeOutOfDateReason() {
        cluster.setWorkerNodeServiceOfferingId(7L);
        cluster.setWorkerNodeRootDiskSize(30L);
        VMInstanceVO upToDate = addVm(10L, WORKER, 7L, 30L);
        VMInstanceVO oldOffering = addVm(11L, WORKER, CLUSTER_OFFERING, 30L);
        VMInstanceVO smallDisk = addVm(12L, WORKER, 7L, 20L);
        VMInstanceVO largeDisk = addVm(13L, WORKER, 7L, 50L);
        VMInstanceVO otherGroups = addVm(14L, WORKER, 7L, 30L);
        Mockito.when(affinityGroupVMMapDao.listAffinityGroupIdsByVmId(14L)).thenReturn(List.of(20L));
        Mockito.when(affinityGroupDao.findById(20L)).thenReturn(Mockito.mock(AffinityGroupVO.class));

        Assert.assertNull(manager.getNodeOutOfDateReason(cluster, vmMaps.get(0), upToDate));
        Assert.assertEquals("its service offering differs", manager.getNodeOutOfDateReason(cluster, vmMaps.get(1), oldOffering));
        Assert.assertEquals("its root disk is smaller than 30 GB", manager.getNodeOutOfDateReason(cluster, vmMaps.get(2), smallDisk));
        Assert.assertNull(manager.getNodeOutOfDateReason(cluster, vmMaps.get(3), largeDisk));
        // with no groups recorded for the type, the nodes keep theirs
        Assert.assertNull(manager.getNodeOutOfDateReason(cluster, vmMaps.get(4), otherGroups));
        Assert.assertEquals(Map.of(CONTROL, 0, WORKER, 2), manager.countOutOfDateNodes(cluster));

        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(CLUSTER_ID, WORKER.name())).thenReturn(List.of(21L));
        Mockito.when(affinityGroupDao.findById(21L)).thenReturn(Mockito.mock(AffinityGroupVO.class));
        Assert.assertEquals("its affinity groups differ", manager.getNodeOutOfDateReason(cluster, vmMaps.get(4), otherGroups));
        Assert.assertEquals("its affinity groups differ", manager.getNodeOutOfDateReason(cluster, vmMaps.get(0), upToDate));
    }

    @Test
    public void explicitDedicationGroupOfTheTypeIsNotADifference() {
        VMInstanceVO vm = addVm(10L, WORKER, CLUSTER_OFFERING, null);
        AffinityGroupVO dedication = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(dedication.getType()).thenReturn("ExplicitDedication");
        Mockito.when(affinityGroupDao.findById(30L)).thenReturn(dedication);
        Mockito.when(kubernetesClusterAffinityGroupMapDao.listAffinityGroupIdsByClusterIdAndNodeType(CLUSTER_ID, WORKER.name())).thenReturn(List.of(30L));

        // only a dedication group recorded: the nodes keep their groups
        Assert.assertNull(manager.getNodeTypeWantedAffinityGroupIds(CLUSTER_ID, WORKER));
        Assert.assertNull(manager.getNodeOutOfDateReason(cluster, vmMaps.get(0), vm));
    }

    @Test
    public void explicitDedicationGroupIsNotADifference() {
        VMInstanceVO vm = addVm(10L, WORKER, CLUSTER_OFFERING, null);
        AffinityGroupVO dedication = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(dedication.getType()).thenReturn("ExplicitDedication");
        Mockito.when(affinityGroupDao.findById(30L)).thenReturn(dedication);
        Mockito.when(affinityGroupVMMapDao.listAffinityGroupIdsByVmId(10L)).thenReturn(List.of(30L));

        Assert.assertNull(manager.getNodeOutOfDateReason(cluster, vmMaps.get(0), vm));
        Assert.assertEquals(Set.of(30L), manager.getVmAffinityGroupIds(10L, true));
    }

    @Test
    public void capacityIsCountedFromTheOfferingsOfAllVms() {
        addVm(10L, CONTROL, 1L, null);
        addVm(11L, WORKER, 2L, null);
        addVm(12L, WORKER, 2L, null);
        vmMaps.get(2).setExternalNode(true);
        Map<Long, ServiceOfferingVO> offerings = new HashMap<>();
        for (long id : List.of(1L, 2L)) {
            ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
            Mockito.when(offering.getCpu()).thenReturn((int) id * 2);
            Mockito.when(offering.getRamSize()).thenReturn((int) id * 1024);
            offerings.put(id, offering);
        }
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenAnswer(i -> offerings.get((Long) i.getArgument(1)));
        KubernetesClusterVO update = new KubernetesClusterVO();
        Mockito.when(kubernetesClusterDao.createForUpdate(CLUSTER_ID)).thenReturn(update);

        manager.updateKubernetesClusterCapacity(CLUSTER_ID);

        Assert.assertEquals(10L, update.getCores());
        Assert.assertEquals(5120L, update.getMemory());
        Mockito.verify(kubernetesClusterDao).update(CLUSTER_ID, update);
    }
}
