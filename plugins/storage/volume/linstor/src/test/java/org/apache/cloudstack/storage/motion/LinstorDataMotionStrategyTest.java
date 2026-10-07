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
package org.apache.cloudstack.storage.motion;

import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ResourceMakeAvailable;

import java.util.Collections;
import java.util.Map;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LinstorDataMotionStrategyTest {
    private static final long SRC_HOST_ID = 1L;
    private static final long DEST_HOST_ID = 3L;
    private static final String DEST_HOST_NAME = "kvm-dest";
    private static final long SRC_VOLUME_ID = 10L;
    private static final long DEST_VOLUME_ID = 20L;
    private static final long SRC_POOL_ID = 7L;
    private static final long DEST_POOL_ID = 8L;
    private static final String DEST_VOLUME_UUID = "8ed5dd1d-16f8-4169-8ed9-948178dbd44b";
    private static final String DEST_RSC_NAME = "cs-" + DEST_VOLUME_UUID;
    private static final String DEST_DEVICE_PATH = "/dev/drbd/by-res/" + DEST_RSC_NAME + "/0";

    @Mock
    private PrimaryDataStoreDao _storagePool;
    @Mock
    private PrimaryDataStoreDao _storagePoolDao;
    @Mock
    private VolumeDao _volumeDao;
    @Mock
    private VolumeDataFactory _volumeDataFactory;
    @Mock
    private VMInstanceDao _vmDao;
    @Mock
    private GuestOSDao _guestOsDao;
    @Mock
    private VolumeService _volumeService;
    @Mock
    private GuestOSCategoryDao _guestOsCategoryDao;
    @Mock
    private SnapshotDao _snapshotDao;
    @Mock
    private AgentManager _agentManager;

    @InjectMocks
    private LinstorDataMotionStrategy strategy;

    private MockedStatic<LinstorUtil> linstorUtil;
    private DevelopersApi api;
    private Host srcHost;
    private Host destHost;
    private VirtualMachineTO vmTO;
    private VolumeInfo srcVolumeInfo;
    private VolumeInfo destVolumeInfo;
    private DataStore destDataStore;
    private AsyncCompletionCallback<CopyCommandResult> callback;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        api = mock(DevelopersApi.class);
        linstorUtil = Mockito.mockStatic(LinstorUtil.class);
        linstorUtil.when(() -> LinstorUtil.getLinstorAPI(any(), any(), anyBoolean())).thenReturn(api);
        linstorUtil.when(() -> LinstorUtil.getDevicePath(api, DEST_RSC_NAME)).thenReturn(DEST_DEVICE_PATH);

        srcHost = mock(Host.class);
        when(srcHost.getId()).thenReturn(SRC_HOST_ID);
        when(srcHost.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        destHost = mock(Host.class);
        when(destHost.getId()).thenReturn(DEST_HOST_ID);
        when(destHost.getName()).thenReturn(DEST_HOST_NAME);
        when(destHost.getPrivateIpAddress()).thenReturn("192.168.0.3");

        vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getId()).thenReturn(100L);
        when(vmTO.getName()).thenReturn("i-2-100-VM");
        when(vmTO.getCpus()).thenReturn(2);
        when(vmTO.getSpeed()).thenReturn(5001);
        when(vmTO.getMinSpeed()).thenReturn(5001);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getState()).thenReturn(VirtualMachine.State.Migrating);
        when(vm.getGuestOSId()).thenReturn(5L);
        when(_vmDao.findById(100L)).thenReturn(vm);
        GuestOSVO guestOs = mock(GuestOSVO.class);
        when(guestOs.getCategoryId()).thenReturn(6L);
        when(_guestOsDao.findById(5L)).thenReturn(guestOs);
        GuestOSCategoryVO guestOsCategory = mock(GuestOSCategoryVO.class);
        when(guestOsCategory.getName()).thenReturn("Other");
        when(_guestOsCategoryDao.findById(6L)).thenReturn(guestOsCategory);

        // source volume on an NFS pool
        DataStore srcDataStore = mock(DataStore.class);
        when(srcDataStore.getId()).thenReturn(SRC_POOL_ID);
        srcVolumeInfo = mock(VolumeInfo.class);
        when(srcVolumeInfo.getId()).thenReturn(SRC_VOLUME_ID);
        when(srcVolumeInfo.getPath()).thenReturn("216fb792-c5ed-4ad7-98e6-ebdd25816e55");
        when(srcVolumeInfo.getDataStore()).thenReturn(srcDataStore);
        when(srcVolumeInfo.getPassphraseId()).thenReturn(null);
        StoragePoolVO srcPool = mock(StoragePoolVO.class);
        when(srcPool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(_storagePool.findById(SRC_POOL_ID)).thenReturn(srcPool);
        when(_volumeDao.findById(SRC_VOLUME_ID)).thenReturn(new VolumeVO(Volume.Type.ROOT, "ROOT-100", 1L, 1L, 2L, 3L,
                Storage.ProvisioningType.THIN, 1024L * 1024 * 1024, null, null, null));

        // destination volume on the Linstor pool
        destDataStore = mock(DataStore.class);
        when(destDataStore.getId()).thenReturn(DEST_POOL_ID);
        StoragePoolVO destPool = mock(StoragePoolVO.class);
        when(destPool.getId()).thenReturn(DEST_POOL_ID);
        when(destPool.getHostAddress()).thenReturn("http://linstor-controller");
        when(_storagePool.findById(DEST_POOL_ID)).thenReturn(destPool);
        VolumeVO destVolume = mock(VolumeVO.class);
        when(destVolume.getId()).thenReturn(DEST_VOLUME_ID);
        when(_volumeDao.persist(any(VolumeVO.class))).thenReturn(destVolume);
        when(_volumeDao.findById(DEST_VOLUME_ID)).thenReturn(destVolume);
        destVolumeInfo = mock(VolumeInfo.class);
        when(destVolumeInfo.getId()).thenReturn(DEST_VOLUME_ID);
        when(destVolumeInfo.getUuid()).thenReturn(DEST_VOLUME_UUID);
        when(destVolumeInfo.getDataStore()).thenReturn(destDataStore);
        when(_volumeDataFactory.getVolume(eq(DEST_VOLUME_ID), any(DataStore.class))).thenReturn(destVolumeInfo);
        when(_volumeDataFactory.getVolume(DEST_VOLUME_ID)).thenReturn(destVolumeInfo);
        when(_volumeDataFactory.getVolume(SRC_VOLUME_ID)).thenReturn(srcVolumeInfo);

        callback = mock(AsyncCompletionCallback.class);
    }

    @After
    public void tearDown() {
        linstorUtil.close();
    }

    private Map<VolumeInfo, DataStore> volumeMap() {
        return Collections.singletonMap(srcVolumeInfo, destDataStore);
    }

    private void prepareForMigrationAnswers(Integer cpuShares) throws Exception {
        when(_agentManager.send(eq(DEST_HOST_ID), any(PrepareForMigrationCommand.class))).thenAnswer(inv -> {
            PrepareForMigrationAnswer answer = new PrepareForMigrationAnswer(inv.getArgument(1));
            answer.setNewVmCpuShares(cpuShares);
            return answer;
        });
    }

    private void migrateSucceeds() throws Exception {
        when(_agentManager.send(eq(SRC_HOST_ID), any(MigrateCommand.class))).thenAnswer(
                inv -> new MigrateAnswer(inv.getArgument(1), true, null, null));
    }

    private MigrateCommand sentMigrateCommand() throws Exception {
        ArgumentCaptor<Command> captor = ArgumentCaptor.forClass(Command.class);
        verify(_agentManager, Mockito.atLeastOnce()).send(anyLong(), captor.capture());
        return captor.getAllValues().stream()
                .filter(MigrateCommand.class::isInstance)
                .map(MigrateCommand.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no MigrateCommand sent"));
    }

    private CopyCommandResult completedResult() {
        ArgumentCaptor<CopyCommandResult> captor = ArgumentCaptor.forClass(CopyCommandResult.class);
        verify(callback).complete(captor.capture());
        return captor.getValue();
    }

    @Test
    public void copyAsyncMakesNewResourceAvailableOnTargetHostBeforeMigrating() throws Exception {
        prepareForMigrationAnswers(8335);
        migrateSucceeds();

        strategy.copyAsync(volumeMap(), vmTO, srcHost, destHost, callback);

        InOrder order = inOrder(api, _agentManager);
        order.verify(api).resourceMakeAvailableOnNode(eq(DEST_RSC_NAME), eq(DEST_HOST_NAME), any(ResourceMakeAvailable.class));
        order.verify(_agentManager).send(eq(DEST_HOST_ID), any(PrepareForMigrationCommand.class));
        order.verify(_agentManager).send(eq(SRC_HOST_ID), any(MigrateCommand.class));

        MigrateCommand migrateCommand = sentMigrateCommand();
        Assert.assertEquals(1, migrateCommand.getMigrateDiskInfoList().size());
        Assert.assertEquals(DEST_DEVICE_PATH, migrateCommand.getMigrateDiskInfoList().get(0).getSourceText());
        Assert.assertTrue(completedResult().isSuccess());
    }

    @Test
    public void copyAsyncRemovesDestinationVolumeWhenCreateResourceFails() throws Exception {
        linstorUtil.when(() -> LinstorUtil.createResource(any(), any(), any(), anyBoolean()))
                .thenThrow(new CloudRuntimeException("Not enough available nodes"));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> strategy.copyAsync(volumeMap(), vmTO, srcHost, destHost, callback));

        verify(api, never()).resourceMakeAvailableOnNode(any(), any(), any());
        verify(_agentManager, never()).send(anyLong(), any(MigrateCommand.class));
        verify(_volumeService).destroyVolume(DEST_VOLUME_ID);
        verify(_volumeService).expungeVolumeAsync(destVolumeInfo);
        Assert.assertTrue(completedResult().isFailed());
    }

    @Test
    public void copyAsyncRemovesDestinationVolumeWhenMakeAvailableFails() throws Exception {
        when(api.resourceMakeAvailableOnNode(eq(DEST_RSC_NAME), eq(DEST_HOST_NAME), any(ResourceMakeAvailable.class)))
                .thenThrow(new ApiException("Autoplacer could not find diskless stor pool"));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> strategy.copyAsync(volumeMap(), vmTO, srcHost, destHost, callback));

        verify(_agentManager, never()).send(anyLong(), any(MigrateCommand.class));
        verify(_volumeService).destroyVolume(DEST_VOLUME_ID);
        verify(_volumeService).expungeVolumeAsync(destVolumeInfo);
        Assert.assertTrue(completedResult().isFailed());
    }

    @Test
    public void copyAsyncRemovesDestinationVolumeWhenPrepareForMigrationFails() throws Exception {
        when(_agentManager.send(eq(DEST_HOST_ID), any(PrepareForMigrationCommand.class))).thenAnswer(
                inv -> new PrepareForMigrationAnswer(inv.getArgument(1), "failed to prepare"));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> strategy.copyAsync(volumeMap(), vmTO, srcHost, destHost, callback));

        verify(_agentManager, never()).send(anyLong(), any(MigrateCommand.class));
        verify(_volumeService).destroyVolume(DEST_VOLUME_ID);
        verify(_volumeService).expungeVolumeAsync(destVolumeInfo);
        Assert.assertTrue(completedResult().isFailed());
    }

    @Test
    public void copyAsyncRemovesDestinationVolumeWhenMigrationFails() throws Exception {
        prepareForMigrationAnswers(8335);
        when(_agentManager.send(eq(SRC_HOST_ID), any(MigrateCommand.class))).thenAnswer(
                inv -> new MigrateAnswer(inv.getArgument(1), false, "blockdev-add failed", null));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> strategy.copyAsync(volumeMap(), vmTO, srcHost, destHost, callback));

        verify(_volumeService).destroyVolume(DEST_VOLUME_ID);
        verify(_volumeService).expungeVolumeAsync(destVolumeInfo);
        verify(_volumeService, never()).destroyVolume(SRC_VOLUME_ID);
        Assert.assertTrue(completedResult().isFailed());
    }

    private void migrateTimesOutWithVmOnTarget(VirtualMachine.PowerState powerState) throws Exception {
        prepareForMigrationAnswers(8335);
        when(_agentManager.send(eq(SRC_HOST_ID), any(MigrateCommand.class)))
                .thenThrow(new OperationTimedoutException(null, SRC_HOST_ID, 1L, 86400, false));
        when(_agentManager.send(eq(DEST_HOST_ID), any(CheckVirtualMachineCommand.class))).thenAnswer(
                inv -> new CheckVirtualMachineAnswer(inv.getArgument(1), powerState, 5900));
    }

    @Test
    public void copyAsyncSucceedsWhenMigrateTimesOutButVmRunsOnTargetHost() throws Exception {
        migrateTimesOutWithVmOnTarget(VirtualMachine.PowerState.PowerOn);

        strategy.copyAsync(volumeMap(), vmTO, srcHost, destHost, callback);

        verify(_volumeService, never()).destroyVolume(DEST_VOLUME_ID);
        verify(_volumeService).destroyVolume(SRC_VOLUME_ID);
        Assert.assertTrue(completedResult().isSuccess());
    }

    @Test
    public void copyAsyncKeepsDestinationVolumeWhenMigrateTimesOut() throws Exception {
        // a paused domain on the target: the incoming migration may still be running
        migrateTimesOutWithVmOnTarget(VirtualMachine.PowerState.PowerUnknown);

        Assert.assertThrows(CloudRuntimeException.class,
                () -> strategy.copyAsync(volumeMap(), vmTO, srcHost, destHost, callback));

        verify(_volumeService, never()).destroyVolume(DEST_VOLUME_ID);
        verify(_volumeService, never()).destroyVolume(SRC_VOLUME_ID);
        Assert.assertTrue(completedResult().isFailed());
    }
}
