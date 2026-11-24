/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.motion;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.driver.OntapPrimaryDatastoreDriver;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo;
import com.cloud.agent.api.ModifyTargetsAnswer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OntapDataMotionStrategyTest {

    private static final long SRC_HOST_ID = 100L;
    private static final int LIVE_MIGRATION_CPU_SHARES = 256;
    private static final long DEST_HOST_ID = 200L;

    @InjectMocks
    private OntapDataMotionStrategy strategy;

    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private VolumeDao volumeDao;
    @Mock
    private VolumeDetailsDao volumeDetailsDao;
    @Mock
    private VolumeDataFactory volumeDataFactory;
    @Mock
    private VolumeService volumeService;
    @Mock
    private DataStoreManager dataStoreManager;
    @Mock
    private AgentManager agentManager;
    @Mock
    private HostDao hostDao;
    @Mock
    private ResourceManager resourceManager;
    @Mock
    private VMInstanceDao vmDao;
    @Mock
    private GuestOSDao guestOsDao;
    @Mock
    private GuestOSCategoryDao guestOsCategoryDao;

    @Test
    public void canHandleMigratingVolumeOnOntapPrimary() {
        VolumeInfo srcVolume = Mockito.mock(VolumeInfo.class);
        VolumeInfo destVolume = Mockito.mock(VolumeInfo.class);
        PrimaryDataStore srcStore = Mockito.mock(PrimaryDataStore.class);
        PrimaryDataStore destStore = Mockito.mock(PrimaryDataStore.class);
        StoragePoolVO srcPool = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(srcStore).when(srcVolume).getDataStore();
        Mockito.doReturn(destStore).when(destVolume).getDataStore();
        Mockito.doReturn(DataStoreRole.Primary).when(srcStore).getRole();
        Mockito.doReturn(DataStoreRole.Primary).when(destStore).getRole();
        Mockito.doReturn(1L).when(srcStore).getId();
        Mockito.doReturn(2L).when(destStore).getId();
        Mockito.doReturn(Volume.State.Migrating).when(srcVolume).getState();
        Mockito.doReturn(HypervisorType.KVM).when(srcVolume).getHypervisorType();
        Mockito.doReturn(DataStoreProvider.ONTAP_PLUGIN_NAME).when(srcPool).getStorageProviderName();
        Mockito.doReturn(srcPool).when(primaryDataStoreDao).findById(1L);

        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(srcVolume, destVolume));

        Mockito.doReturn(Volume.State.Ready).when(srcVolume).getState();
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(srcVolume, destVolume));
    }

    @Test
    public void canHandleLiveMigrationWithOntapPoolOnKvm() {
        VolumeInfo srcVolume = Mockito.mock(VolumeInfo.class);
        DataStore destStore = Mockito.mock(DataStore.class);
        StoragePoolVO srcPool = Mockito.mock(StoragePoolVO.class);
        Host srcHost = Mockito.mock(Host.class);
        Mockito.doReturn(1L).when(srcVolume).getPoolId();
        Mockito.doReturn(DataStoreProvider.ONTAP_PLUGIN_NAME).when(srcPool).getStorageProviderName();
        Mockito.doReturn(srcPool).when(primaryDataStoreDao).findById(1L);
        Mockito.doReturn(HypervisorType.KVM).when(srcHost).getHypervisorType();

        Map<VolumeInfo, DataStore> volumeMap = Map.of(srcVolume, destStore);
        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(volumeMap, srcHost, Mockito.mock(Host.class)));

        Mockito.doReturn(HypervisorType.VMware).when(srcHost).getHypervisorType();
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(volumeMap, srcHost, Mockito.mock(Host.class)));
    }

    @Test
    public void offlineMigrationBetweenSupportedOntapPoolsUsesCopyCommand() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "NFS3", "svm1", null);
        VolumeObjectTO copiedVolume = new VolumeObjectTO();
        copiedVolume.setPath("copied-volume-path");
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class)))
                .thenReturn(new CopyCmdAnswer(copiedVolume));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        Mockito.verify(agentManager).send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class));
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.eq(context.host.getId()), Mockito.any(MigrateVolumeCommand.class));
        Mockito.verify(context.destVolumeVO).setPath("copied-volume-path");
    }

    @Test
    public void offlineMigrationMatchesSvmByUuidBeforeName() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "NFS3", "svm2", null);
        Mockito.doReturn(Map.of("storageIP", "storage-ip", "svmUUID", "svm-uuid", "svmName", "svm1", "protocol", "NFS3"))
                .when(primaryDataStoreDao).getDetails(1L);
        Mockito.doReturn(Map.of("storageIP", "storage-ip", "svmUUID", "svm-uuid", "svmName", "svm2", "protocol", "NFS3"))
                .when(primaryDataStoreDao).getDetails(2L);
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class)))
                .thenReturn(new CopyCmdAnswer(new VolumeObjectTO()));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        Mockito.verify(agentManager).send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class));
    }

    @Test
    public void offlineMigrationFromNonOntapPoolUsesMigrateVolumeCommand() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(false, "NFS3", "svm1", null);
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(MigrateVolumeCommand.class)))
                .thenReturn(new MigrateVolumeAnswer(null, true, null, "migrated-volume-path"));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        Mockito.verify(agentManager).send(Mockito.eq(context.host.getId()), Mockito.any(MigrateVolumeCommand.class));
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class));
        Mockito.verify(context.destVolumeVO).setPath("migrated-volume-path");
    }

    @Test
    // Fallback only. Same-SVM iSCSI is intercepted by OntapPrimaryDatastoreDriver.canCopy before this strategy runs.
    public void offlineMigrationBetweenSupportedOntapIscsiPoolsUsesMigrateVolumeCommand() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "ISCSI", "ISCSI", "svm1", null,
                StoragePoolType.OntapiSCSI, StoragePoolType.OntapiSCSI);
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(MigrateVolumeCommand.class)))
                .thenReturn(new MigrateVolumeAnswer(null, true, null, "migrated-volume-path"));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        Mockito.verify(agentManager).send(Mockito.eq(context.host.getId()), Mockito.any(MigrateVolumeCommand.class));
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class));
    }

    @Test
    public void offlineIscsiMigrationReadsSourceIqnAfterGrantAccess() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "ISCSI", "ISCSI", "svm1", null,
                StoragePoolType.OntapiSCSI, StoragePoolType.OntapiSCSI);
        VolumeVO srcVolumeVO = volumeDao.findById(10L);
        AtomicBoolean srcGranted = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> srcGranted.get() ? "/iqn/2" : "/iqn/0").when(srcVolumeVO).get_iScsiName();
        Mockito.doAnswer(invocation -> {
            srcGranted.set(true);
            return true;
        }).when(volumeService).grantAccess(Mockito.eq(context.srcVolume), Mockito.any(), Mockito.any());
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(MigrateVolumeCommand.class)))
                .thenReturn(new MigrateVolumeAnswer(null, true, null, "migrated-volume-path"));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        ArgumentCaptor<MigrateVolumeCommand> commandCaptor = ArgumentCaptor.forClass(MigrateVolumeCommand.class);
        Mockito.verify(agentManager).send(Mockito.eq(context.host.getId()), commandCaptor.capture());
        assertEquals("/iqn/2", commandCaptor.getValue().getSrcDetails().get(DiskTO.IQN));
    }

    @Test
    public void offlineMigrationTagsDestinationBeforeGrantAndClearsItAfter() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "NFS3", "svm1", null);
        VolumeObjectTO copiedVolume = new VolumeObjectTO();
        copiedVolume.setPath("copied-volume-path");
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class)))
                .thenReturn(new CopyCmdAnswer(copiedVolume));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        InOrder inOrder = Mockito.inOrder(volumeDetailsDao, volumeService, context.callback);
        inOrder.verify(volumeDetailsDao).addDetail(20L, OntapStorageConstants.MIGRATION_DESTINATION,
                OntapStorageConstants.TRUE, false);
        inOrder.verify(volumeService).grantAccess(Mockito.eq(context.destVolume), Mockito.any(Host.class), Mockito.any());
        // Revoked once after the copy command and once more by the outer finally.
        inOrder.verify(volumeService, Mockito.times(2)).revokeAccess(Mockito.eq(context.destVolume), Mockito.any(Host.class), Mockito.any());
        inOrder.verify(volumeDetailsDao).removeDetail(20L, OntapStorageConstants.MIGRATION_DESTINATION);
        inOrder.verify(context.callback).complete(Mockito.any());
    }

    @Test
    public void offlineMigrationClearsDestinationTagWhenMigrationFails() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "NFS3", "svm1", null);
        Answer failedAnswer = Mockito.mock(Answer.class);
        Mockito.when(failedAnswer.getResult()).thenReturn(false);
        Mockito.when(failedAnswer.getDetails()).thenReturn("copy failed");
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class)))
                .thenReturn(failedAnswer);

        assertThrows(CloudRuntimeException.class, () ->
                strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback));

        Mockito.verify(volumeDetailsDao).addDetail(20L, OntapStorageConstants.MIGRATION_DESTINATION,
                OntapStorageConstants.TRUE, false);
        Mockito.verify(volumeDetailsDao).removeDetail(20L, OntapStorageConstants.MIGRATION_DESTINATION);
        Mockito.verify(context.callback).complete(Mockito.argThat(CopyCommandResult::isFailed));
    }

    @Test
    public void liveMigrationDoesNotTagDestination() throws Exception {
        LiveMigrationTestContext context = configureLiveMigration("NFS3", StoragePoolType.NetworkFilesystem);

        strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback);

        Mockito.verify(volumeDetailsDao, Mockito.never()).addDetail(Mockito.anyLong(),
                Mockito.eq(OntapStorageConstants.MIGRATION_DESTINATION), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void offlineMigrationBetweenOntapPoolsWithDifferentProtocolIsRejected() {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "ISCSI", "svm1", null);
        assertThrows(CloudRuntimeException.class, () ->
                strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback));
        Mockito.verify(context.callback).complete(Mockito.argThat(result ->
                result != null && result.isFailed()
                        && result.getResult() != null
                        && result.getResult().contains("Cross-protocol")));
        Mockito.verify(context.destVolume.getDataStore().getDriver(), Mockito.never())
                .createAsync(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void offlineMigrationFromOntapIscsiToNfsIsRejected() {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "ISCSI", "NFS3", "svm1", null,
                StoragePoolType.OntapiSCSI, StoragePoolType.NetworkFilesystem);
        assertThrows(CloudRuntimeException.class, () ->
                strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback));
        Mockito.verify(context.destVolume.getDataStore().getDriver(), Mockito.never())
                .createAsync(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void offlineMigrationBetweenOntapPoolsWithDifferentSvmUsesMigrateVolumeCommand() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "NFS3", "svm2", null);
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(MigrateVolumeCommand.class)))
                .thenReturn(new MigrateVolumeAnswer(null, true, null, "migrated-volume-path"));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        Mockito.verify(agentManager).send(Mockito.eq(context.host.getId()), Mockito.any(MigrateVolumeCommand.class));
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class));
    }

    @Test
    public void offlineMigrationAttachedToStoppedVmUsesCopyCommand() throws Exception {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.doReturn(VirtualMachine.State.Stopped).when(vm).getState();
        OfflineMigrationTestContext context = configureOfflineMigration(true, "NFS3", "svm1", vm);
        VolumeObjectTO copiedVolume = new VolumeObjectTO();
        copiedVolume.setPath("copied-volume-path");
        Mockito.when(agentManager.send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class)))
                .thenReturn(new CopyCmdAnswer(copiedVolume));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        Mockito.verify(agentManager).send(Mockito.eq(context.host.getId()), Mockito.any(CopyCommand.class));
    }

    private VolumeVO captureDuplicatedVolume() {
        ArgumentCaptor<VolumeVO> captor = ArgumentCaptor.forClass(VolumeVO.class);
        Mockito.verify(volumeDao).persist(captor.capture());
        return captor.getValue();
    }

    @Test
    public void liveMigrationToOntapNfsUsesFileDiskAndUuidPath() throws Exception {
        LiveMigrationTestContext context = configureLiveMigration("NFS3", StoragePoolType.NetworkFilesystem);

        strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback);

        MigrateCommand migrateCommand = captureMigrateCommand();
        assertEquals(LIVE_MIGRATION_CPU_SHARES, migrateCommand.getNewVmCpuShares());
        assertTrue(migrateCommand.getMigrateDiskInfoList().isEmpty());
        MigrateDiskInfo diskInfo = migrateCommand.getMigrateStorage().get("src-path");
        assertEquals(MigrateDiskInfo.DiskType.FILE, diskInfo.getDiskType());
        assertEquals(MigrateDiskInfo.DriverType.QCOW2, diskInfo.getDriverType());
        assertEquals("dest-connected", diskInfo.getSourceText());
        Mockito.verify(context.destVolumeVO).setPath("dest-uuid");
        Mockito.verify(context.destVolumeVO).setFormat(Storage.ImageFormat.QCOW2);
        assertEquals(Storage.ImageFormat.QCOW2, captureDuplicatedVolume().getFormat());
        assertSame(context.destVolumeTO, context.vmDisk.getData());
        Mockito.verify(volumeService).copyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigration(Mockito.any(), Mockito.isNull(),
                Mockito.eq(context.srcVolume), Mockito.eq(context.destVolume), Mockito.eq(false));
        Mockito.verify(context.callback).complete(Mockito.argThat(result -> result != null && !result.isFailed()));
    }

    @Test
    public void liveMigrationToOntapIscsiUsesBlockDiskAndLunPath() throws Exception {
        LiveMigrationTestContext context = configureLiveMigration("ISCSI", StoragePoolType.OntapiSCSI);

        strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback);

        MigrateCommand migrateCommand = captureMigrateCommand();
        assertEquals(1, migrateCommand.getMigrateDiskInfoList().size());
        MigrateDiskInfo diskInfo = migrateCommand.getMigrateDiskInfoList().get(0);
        assertEquals(MigrateDiskInfo.DiskType.BLOCK, diskInfo.getDiskType());
        assertEquals(MigrateDiskInfo.DriverType.RAW, diskInfo.getDriverType());
        assertEquals("src-connected", diskInfo.getSerialNumber());
        assertEquals("dest-connected", diskInfo.getSourceText());
        assertSame(diskInfo, migrateCommand.getMigrateStorage().get("src-path"));
        assertSame(diskInfo, migrateCommand.getMigrateStorage().get("src-connected"));
        Mockito.verify(context.destVolumeVO).setPath("/iqn/dest/1");
        Mockito.verify(context.destVolumeVO).setFormat(Storage.ImageFormat.RAW);
        assertEquals(Storage.ImageFormat.RAW, captureDuplicatedVolume().getFormat());
        Mockito.verify(volumeService).grantAccess(context.srcVolume, context.srcHost, context.srcVolume.getDataStore());
        ArgumentCaptor<ModifyTargetsCommand> targetsCaptor = ArgumentCaptor.forClass(ModifyTargetsCommand.class);
        Mockito.verify(agentManager, Mockito.atLeastOnce()).easySend(Mockito.eq(SRC_HOST_ID), targetsCaptor.capture());
        assertTrue(targetsCaptor.getAllValues().stream().anyMatch(command -> !command.getAdd()
                && command.getTargets().stream().anyMatch(target -> "/iqn/dest/1".equals(target.get(ModifyTargetsCommand.IQN)))));
        Mockito.verify(context.callback).complete(Mockito.argThat(result -> result != null && !result.isFailed()));
    }

    @Test
    public void liveMigrationFromManagedPoolToDifferentSvmIsRejected() {
        LiveMigrationTestContext context = configureLiveMigration("ISCSI", StoragePoolType.OntapiSCSI);
        Mockito.doReturn(Map.of("storageIP", "storage-ip", "svmName", "svm2", "protocol", "ISCSI")).when(primaryDataStoreDao).getDetails(2L);

        assertThrows(CloudRuntimeException.class, () ->
                strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback));
        Mockito.verify(volumeDao, Mockito.never()).persist(Mockito.any(VolumeVO.class));
        Mockito.verify(context.callback).complete(Mockito.argThat(CopyCommandResult::isFailed));
    }

    @Test
    public void liveMigrationExpungesDestinationWhenGrantAccessFails() {
        LiveMigrationTestContext context = configureLiveMigration("NFS3", StoragePoolType.NetworkFilesystem);
        Mockito.doThrow(new CloudRuntimeException("grant failed")).when(volumeService)
                .grantAccess(Mockito.eq(context.destVolume), Mockito.eq(context.destHost), Mockito.any());

        assertThrows(CloudRuntimeException.class, () ->
                strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback));

        Mockito.verify(volumeService).expungeVolumeAsync(context.destVolume);
        Mockito.verify(context.callback).complete(Mockito.argThat(CopyCommandResult::isFailed));
    }

    @Test
    public void liveMigrationUnexpectedExceptionReportsFailureAndRollsBack() {
        LiveMigrationTestContext context = configureLiveMigration("NFS3", StoragePoolType.NetworkFilesystem);
        IllegalStateException unexpected = new IllegalStateException("unexpected state");
        Mockito.doThrow(unexpected).when(volumeService)
                .grantAccess(Mockito.eq(context.destVolume), Mockito.eq(context.destHost), Mockito.any());

        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () ->
                strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback));

        assertSame(unexpected, thrown.getCause());
        assertTrue(thrown.getMessage().contains("Unexpected error while migrating"));
        assertTrue(thrown.getMessage().contains("unexpected state"));
        Mockito.verify(volumeService).expungeVolumeAsync(context.destVolume);
        Mockito.verify(context.callback).complete(Mockito.argThat(result -> result.isFailed() && result.getResult() != null
                && result.getResult().contains("unexpected state")));
    }

    @Test
    public void liveMigrationUnexpectedExceptionWithoutMessageStillReportsFailure() {
        LiveMigrationTestContext context = configureLiveMigration("NFS3", StoragePoolType.NetworkFilesystem);
        Mockito.doThrow(new NullPointerException()).when(volumeService)
                .grantAccess(Mockito.eq(context.destVolume), Mockito.eq(context.destHost), Mockito.any());

        assertThrows(CloudRuntimeException.class, () ->
                strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback));

        Mockito.verify(context.callback).complete(Mockito.argThat(result -> result.isFailed()
                && result.getResult().contains("NullPointerException")));
    }

    @Test
    public void liveMigrationTimeoutKeepsDestinationWhenVmIsRunningThere() throws Exception {
        LiveMigrationTestContext context = configureLiveMigration("NFS3", StoragePoolType.NetworkFilesystem);
        VMInstanceVO vm = vmDao.findById(5L);
        Mockito.doReturn("i-5-VM").when(vm).getInstanceName();
        Mockito.doThrow(new OperationTimedoutException(null, SRC_HOST_ID, 0, 0, false)).when(agentManager)
                .send(Mockito.eq(SRC_HOST_ID), Mockito.any(MigrateCommand.class));
        CheckVirtualMachineCommand checkCommand = new CheckVirtualMachineCommand("i-5-VM");
        Mockito.doReturn(new CheckVirtualMachineAnswer(checkCommand, VirtualMachine.PowerState.PowerOn, null)).when(agentManager)
                .send(Mockito.eq(DEST_HOST_ID), Mockito.any(CheckVirtualMachineCommand.class));

        strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback);

        Mockito.verify(volumeService, Mockito.never()).expungeVolumeAsync(Mockito.any());
        Mockito.verify(volumeService).copyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigration(Mockito.any(), Mockito.isNull(),
                Mockito.eq(context.srcVolume), Mockito.eq(context.destVolume), Mockito.eq(false));
        Mockito.verify(context.callback).complete(Mockito.argThat(result -> result != null && !result.isFailed()));
    }

    @Test
    public void liveMigrationContinuesWhenQosReapplyFails() throws Exception {
        LiveMigrationTestContext context = configureLiveMigration("NFS3", StoragePoolType.NetworkFilesystem);
        OntapPrimaryDatastoreDriver ontapDriver = Mockito.mock(OntapPrimaryDatastoreDriver.class);
        DataStore destStore = context.destVolume.getDataStore();
        Mockito.doReturn(ontapDriver).when(destStore).getDriver();
        Mockito.doThrow(new CloudRuntimeException("qos patch failed")).when(ontapDriver)
                .applyQosPolicyToVolume(context.destVolume);

        strategy.copyAsync(context.volumeMap, context.vmTO, context.srcHost, context.destHost, context.callback);

        Mockito.verify(ontapDriver, Mockito.times(3)).applyQosPolicyToVolume(context.destVolume);
        Mockito.verify(volumeService, Mockito.never()).expungeVolumeAsync(Mockito.any());
        Mockito.verify(volumeService).copyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigration(Mockito.any(), Mockito.isNull(),
                Mockito.eq(context.srcVolume), Mockito.eq(context.destVolume), Mockito.eq(false));
        Mockito.verify(context.callback).complete(Mockito.argThat(result -> result != null && !result.isFailed()));
    }

    @Test
    public void offlineMigrationUsesSourceClusterHost() throws Exception {
        OfflineMigrationTestContext context = configureOfflineMigration(true, "NFS3", "svm1", null);
        Scope srcScope = context.srcVolume.getDataStore().getScope();
        Mockito.doReturn(ScopeType.CLUSTER).when(srcScope).getScopeType();
        DataStore destStore = context.destVolume.getDataStore();
        Scope destScope = Mockito.mock(Scope.class);
        Mockito.doReturn(destScope).when(destStore).getScope();
        Mockito.doReturn(ScopeType.CLUSTER).when(destScope).getScopeType();

        StoragePoolVO srcPool = primaryDataStoreDao.findById(1L);
        StoragePoolVO destPool = primaryDataStoreDao.findById(2L);
        Mockito.doReturn(11L).when(srcPool).getClusterId();
        Mockito.doReturn(22L).when(destPool).getClusterId();
        PrimaryDataStore srcStoreInfo = Mockito.mock(PrimaryDataStore.class);
        Mockito.doReturn(srcStoreInfo).when(dataStoreManager).getPrimaryDataStore(1L);

        HostVO sourceHost = Mockito.mock(HostVO.class);
        Mockito.doReturn(8L).when(sourceHost).getId();
        Mockito.doReturn(ResourceState.Enabled).when(sourceHost).getResourceState();
        Mockito.doReturn(List.of(sourceHost)).when(resourceManager)
                .getEligibleUpAndEnabledHostsInClusterForStorageConnection(srcStoreInfo);
        VolumeObjectTO copiedVolume = new VolumeObjectTO();
        copiedVolume.setPath("copied-volume-path");
        Mockito.when(agentManager.send(Mockito.eq(8L), Mockito.any(CopyCommand.class))).thenReturn(new CopyCmdAnswer(copiedVolume));

        strategy.copyAsync(context.srcVolume, context.destVolume, (Host) null, context.callback);

        Mockito.verify(agentManager).send(Mockito.eq(8L), Mockito.any(CopyCommand.class));
        Mockito.verify(dataStoreManager, Mockito.never()).getPrimaryDataStore(2L);
    }

    private MigrateCommand captureMigrateCommand() throws Exception {
        ArgumentCaptor<MigrateCommand> captor = ArgumentCaptor.forClass(MigrateCommand.class);
        Mockito.verify(agentManager).send(Mockito.eq(SRC_HOST_ID), captor.capture());
        assertFalse(captor.getAllValues().isEmpty());
        return captor.getValue();
    }

    private LiveMigrationTestContext configureLiveMigration(String protocol, StoragePoolType poolType) {
        VolumeInfo srcVolume = Mockito.mock(VolumeInfo.class);
        VolumeInfo destVolume = Mockito.mock(VolumeInfo.class);
        PrimaryDataStore srcStore = Mockito.mock(PrimaryDataStore.class);
        PrimaryDataStore destStore = Mockito.mock(PrimaryDataStore.class);
        PrimaryDataStoreDriver destDriver = Mockito.mock(PrimaryDataStoreDriver.class);
        StoragePoolVO srcPool = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO destPool = Mockito.mock(StoragePoolVO.class);
        VolumeVO destVolumeVO = Mockito.mock(VolumeVO.class);
        VolumeObjectTO destVolumeTO = new VolumeObjectTO();
        HostVO srcHost = Mockito.mock(HostVO.class);
        HostVO destHost = Mockito.mock(HostVO.class);
        VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);
        AsyncCompletionCallback<CopyCommandResult> callback = Mockito.mock(AsyncCompletionCallback.class);

        Mockito.doReturn(SRC_HOST_ID).when(srcHost).getId();
        Mockito.doReturn(DEST_HOST_ID).when(destHost).getId();
        Mockito.doReturn(HypervisorType.KVM).when(srcHost).getHypervisorType();

        VolumeObjectTO srcVolumeTO = new VolumeObjectTO();
        srcVolumeTO.setId(10L);
        DiskTO vmDisk = new DiskTO();
        vmDisk.setData(srcVolumeTO);
        Mockito.doReturn(5L).when(vmTO).getId();
        Mockito.doReturn("vm-name").when(vmTO).getName();
        Mockito.doReturn(new DiskTO[] {vmDisk}).when(vmTO).getDisks();

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        GuestOSVO guestOs = Mockito.mock(GuestOSVO.class);
        GuestOSCategoryVO guestOsCategory = Mockito.mock(GuestOSCategoryVO.class);
        Mockito.doReturn(VirtualMachine.State.Running).when(vm).getState();
        Mockito.doReturn(7L).when(vm).getGuestOSId();
        Mockito.doReturn(vm).when(vmDao).findById(5L);
        Mockito.doReturn(8L).when(guestOs).getCategoryId();
        Mockito.doReturn(guestOs).when(guestOsDao).findById(7L);
        Mockito.doReturn("Linux").when(guestOsCategory).getName();
        Mockito.doReturn(guestOsCategory).when(guestOsCategoryDao).findById(8L);

        Mockito.doReturn(10L).when(srcVolume).getId();
        Mockito.doReturn(1L).when(srcVolume).getPoolId();
        Mockito.doReturn("src-path").when(srcVolume).getPath();
        Mockito.doReturn("/iqn/src/0").when(srcVolume).get_iScsiName();
        Mockito.doReturn(srcStore).when(srcVolume).getDataStore();
        Mockito.doReturn(srcVolume).when(volumeDataFactory).getVolume(10L, srcStore);

        Mockito.doReturn(20L).when(destVolume).getId();
        Mockito.doReturn(2L).when(destVolume).getPoolId();
        Mockito.doReturn("dest-uuid").when(destVolume).getUuid();
        Mockito.doReturn("/iqn/dest/1").when(destVolume).get_iScsiName();
        Mockito.doReturn(poolType).when(destVolume).getStoragePoolType();
        Mockito.doReturn(destVolumeTO).when(destVolume).getTO();
        Mockito.doReturn(destStore).when(destVolume).getDataStore();
        Mockito.doReturn(2L).when(destStore).getId();
        Mockito.doReturn(destDriver).when(destStore).getDriver();

        configureOntapPool(srcPool, 1L, poolType);
        configureOntapPool(destPool, 2L, poolType);
        Mockito.doReturn(Map.of("storageIP", "storage-ip", "svmName", "svm1", "protocol", protocol)).when(primaryDataStoreDao).getDetails(1L);
        Mockito.doReturn(Map.of("storageIP", "storage-ip", "svmName", "svm1", "protocol", protocol)).when(primaryDataStoreDao).getDetails(2L);

        VolumeVO srcVolumeVO = new VolumeVO("name", 0L, 0L, 0L, 0L, 0L, "folder", "src-path",
                Storage.ProvisioningType.THIN, 0L, Volume.Type.ROOT);
        Mockito.doReturn(srcVolumeVO).when(volumeDao).findById(10L);
        Mockito.doReturn(20L).when(destVolumeVO).getId();
        Mockito.doReturn(destVolumeVO).when(volumeDao).persist(Mockito.any(VolumeVO.class));
        Mockito.doReturn(destVolumeVO).when(volumeDao).findById(20L);
        Mockito.doReturn(destVolume).when(volumeDataFactory).getVolume(20L, destStore);

        configureModifyTargets(SRC_HOST_ID, "src-connected");
        configureModifyTargets(DEST_HOST_ID, "dest-connected");
        try {
            PrepareForMigrationAnswer prepareAnswer = new PrepareForMigrationAnswer(new PrepareForMigrationCommand(vmTO));
            prepareAnswer.setNewVmCpuShares(LIVE_MIGRATION_CPU_SHARES);
            Mockito.doReturn(prepareAnswer).when(agentManager)
                    .send(Mockito.eq(DEST_HOST_ID), Mockito.any(PrepareForMigrationCommand.class));
            Mockito.doReturn(new MigrateAnswer(null, true, null, null)).when(agentManager)
                    .send(Mockito.eq(SRC_HOST_ID), Mockito.any(MigrateCommand.class));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return new LiveMigrationTestContext(Map.of(srcVolume, destStore), vmTO, vmDisk, srcHost, destHost, srcVolume, destVolume,
                destVolumeVO, destVolumeTO, callback);
    }

    private void configureOntapPool(StoragePoolVO pool, long id, StoragePoolType poolType) {
        Mockito.doReturn(id).when(pool).getId();
        Mockito.doReturn(true).when(pool).isManaged();
        Mockito.doReturn(poolType).when(pool).getPoolType();
        Mockito.doReturn("pool-uuid-" + id).when(pool).getUuid();
        Mockito.doReturn(DataStoreProvider.ONTAP_PLUGIN_NAME).when(pool).getStorageProviderName();
        Mockito.doReturn(pool).when(primaryDataStoreDao).findById(id);
    }

    private void configureModifyTargets(long hostId, String connectedPath) {
        ModifyTargetsAnswer answer = Mockito.mock(ModifyTargetsAnswer.class);
        Mockito.doReturn(true).when(answer).getResult();
        Mockito.doReturn(List.of(connectedPath)).when(answer).getConnectedPaths();
        Mockito.doReturn(answer).when(agentManager).easySend(Mockito.eq(hostId), Mockito.any(ModifyTargetsCommand.class));
    }

    private OfflineMigrationTestContext configureOfflineMigration(boolean isSourceOntap, String destProtocol,
            String destSvmName, VirtualMachine vm) {
        return configureOfflineMigration(isSourceOntap, "NFS3", destProtocol, destSvmName, vm,
                StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem);
    }

    private OfflineMigrationTestContext configureOfflineMigration(boolean isSourceOntap, String srcProtocol,
            String destProtocol, String destSvmName, VirtualMachine vm, StoragePoolType srcPoolType,
            StoragePoolType destPoolType) {
        VolumeInfo srcVolume = Mockito.mock(VolumeInfo.class);
        VolumeInfo destVolume = Mockito.mock(VolumeInfo.class);
        PrimaryDataStore srcStore = Mockito.mock(PrimaryDataStore.class);
        PrimaryDataStore destStore = Mockito.mock(PrimaryDataStore.class);
        PrimaryDataStoreDriver destDriver = Mockito.mock(PrimaryDataStoreDriver.class);
        Scope srcScope = Mockito.mock(Scope.class);
        HostVO host = Mockito.mock(HostVO.class);
        StoragePoolVO srcPool = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO destPool = Mockito.mock(StoragePoolVO.class);
        VolumeVO srcVolumeVO = Mockito.mock(VolumeVO.class);
        VolumeVO destVolumeVO = Mockito.mock(VolumeVO.class);
        AsyncCompletionCallback<CopyCommandResult> callback = Mockito.mock(AsyncCompletionCallback.class);

        Mockito.doReturn(3L).when(host).getId();
        Mockito.doReturn(Volume.State.Migrating).when(srcVolume).getState();
        Mockito.doReturn(HypervisorType.KVM).when(srcVolume).getHypervisorType();
        Mockito.doReturn(vm).when(srcVolume).getAttachedVM();
        Mockito.doReturn(10L).when(srcVolume).getId();
        Mockito.doReturn(20L).when(destVolume).getId();
        Mockito.doReturn(1L).when(srcVolume).getPoolId();
        Mockito.doReturn(2L).when(destVolume).getPoolId();
        Mockito.doReturn(srcStore).when(srcVolume).getDataStore();
        Mockito.doReturn(destStore).when(destVolume).getDataStore();
        Mockito.doReturn(new VolumeObjectTO()).when(srcVolume).getTO();
        Mockito.doReturn(new VolumeObjectTO()).when(destVolume).getTO();

        Mockito.doReturn(DataStoreRole.Primary).when(srcStore).getRole();
        Mockito.doReturn(DataStoreRole.Primary).when(destStore).getRole();
        Mockito.doReturn(1L).when(srcStore).getId();
        Mockito.doReturn(2L).when(destStore).getId();
        Mockito.doReturn(destDriver).when(destStore).getDriver();
        Mockito.doReturn(srcScope).when(srcStore).getScope();
        Mockito.doReturn(ScopeType.HOST).when(srcScope).getScopeType();
        Mockito.doReturn(host.getId()).when(srcScope).getScopeId();

        Mockito.doReturn(isSourceOntap).when(srcPool).isManaged();
        Mockito.doReturn(true).when(destPool).isManaged();
        Mockito.doReturn(1L).when(srcPool).getId();
        Mockito.doReturn(2L).when(destPool).getId();
        Mockito.doReturn(srcPoolType).when(srcPool).getPoolType();
        Mockito.doReturn(destPoolType).when(destPool).getPoolType();
        Mockito.doReturn(isSourceOntap ? DataStoreProvider.ONTAP_PLUGIN_NAME : DataStoreProvider.DEFAULT_PRIMARY)
                .when(srcPool).getStorageProviderName();
        Mockito.doReturn(DataStoreProvider.ONTAP_PLUGIN_NAME).when(destPool).getStorageProviderName();
        Mockito.doReturn(Map.of("storageIP", "storage-ip", "svmName", "svm1", "protocol", srcProtocol))
                .when(primaryDataStoreDao).getDetails(1L);
        Mockito.doReturn(Map.of("storageIP", "storage-ip", "svmName", destSvmName, "protocol", destProtocol))
                .when(primaryDataStoreDao).getDetails(2L);
        Mockito.doReturn(srcPool).when(primaryDataStoreDao).findById(1L);
        Mockito.doReturn(destPool).when(primaryDataStoreDao).findById(2L);

        if (isSourceOntap) {
            Mockito.doReturn(srcPoolType).when(srcVolumeVO).getPoolType();
            Mockito.doReturn(srcVolumeVO).when(volumeDao).findById(10L);
        }
        Mockito.doReturn(destPoolType).when(destVolumeVO).getPoolType();
        Mockito.doReturn(Storage.ImageFormat.QCOW2).when(destVolumeVO).getFormat();
        Mockito.doReturn(destVolumeVO).when(volumeDao).findById(20L);
        Mockito.doReturn(destVolume).when(volumeDataFactory).getVolume(20L, destStore);
        Mockito.doReturn(host).when(hostDao).findById(3L);

        return new OfflineMigrationTestContext(srcVolume, destVolume, destVolumeVO, host, callback);
    }

    private static class OfflineMigrationTestContext {
        private final VolumeInfo srcVolume;
        private final VolumeInfo destVolume;
        private final VolumeVO destVolumeVO;
        private final HostVO host;
        private final AsyncCompletionCallback<CopyCommandResult> callback;

        OfflineMigrationTestContext(VolumeInfo srcVolume, VolumeInfo destVolume, VolumeVO destVolumeVO, HostVO host,
                AsyncCompletionCallback<CopyCommandResult> callback) {
            this.srcVolume = srcVolume;
            this.destVolume = destVolume;
            this.destVolumeVO = destVolumeVO;
            this.host = host;
            this.callback = callback;
        }
    }

    private static class LiveMigrationTestContext {
        private final Map<VolumeInfo, DataStore> volumeMap;
        private final VirtualMachineTO vmTO;
        private final DiskTO vmDisk;
        private final HostVO srcHost;
        private final HostVO destHost;
        private final VolumeInfo srcVolume;
        private final VolumeInfo destVolume;
        private final VolumeVO destVolumeVO;
        private final VolumeObjectTO destVolumeTO;
        private final AsyncCompletionCallback<CopyCommandResult> callback;

        LiveMigrationTestContext(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTO, DiskTO vmDisk, HostVO srcHost, HostVO destHost,
                VolumeInfo srcVolume, VolumeInfo destVolume, VolumeVO destVolumeVO, VolumeObjectTO destVolumeTO,
                AsyncCompletionCallback<CopyCommandResult> callback) {
            this.volumeMap = volumeMap;
            this.vmTO = vmTO;
            this.vmDisk = vmDisk;
            this.srcHost = srcHost;
            this.destHost = destHost;
            this.srcVolume = srcVolume;
            this.destVolume = destVolume;
            this.destVolumeVO = destVolumeVO;
            this.destVolumeTO = destVolumeTO;
            this.callback = callback;
        }
    }
}
