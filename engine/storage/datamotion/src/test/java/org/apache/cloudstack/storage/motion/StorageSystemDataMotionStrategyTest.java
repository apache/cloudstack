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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifyTargetsAnswer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ImageStore;
import com.cloud.storage.MigrationOptions;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;

@RunWith(MockitoJUnitRunner.class)
public class StorageSystemDataMotionStrategyTest {

    @Spy
    @InjectMocks
    private StorageSystemDataMotionStrategy strategy;

    @Mock
    private VolumeObject volumeObjectSource;
    @Mock
    private DataObject dataObjectDestination;
    @Mock
    private PrimaryDataStore sourceStore;
    @Mock
    private ImageStore destinationStore;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private AgentManager agentManager;
    @Mock
    private VMTemplatePoolDao templatePoolDao;
    @Mock
    private VMTemplateDao vmTemplateDao;

    @Mock
    StoragePoolVO sourceStoragePoolVoMock, destinationStoragePoolVoMock;

    @Mock
    Map<String, Storage.StoragePoolType> mapStringStoragePoolTypeMock;

    List<ScopeType> scopeTypes = Arrays.asList(ScopeType.CLUSTER, ScopeType.ZONE);

    @Before
    public void setUp() throws Exception {
        sourceStore = mock(PrimaryDataStoreImpl.class);
        destinationStore = mock(ImageStoreImpl.class);
        volumeObjectSource = mock(VolumeObject.class);
        dataObjectDestination = mock(VolumeObject.class);

        initMocks(strategy);
    }

    @Test
    public void cantHandleSecondary() {
        lenient().doReturn(sourceStore).when(volumeObjectSource).getDataStore();
        doReturn(DataStoreRole.Primary).when(sourceStore).getRole();
        lenient().doReturn(destinationStore).when(dataObjectDestination).getDataStore();
        doReturn(DataStoreRole.Image).when((DataStore)destinationStore).getRole();
        doReturn(sourceStore).when(volumeObjectSource).getDataStore();
        doReturn(destinationStore).when(dataObjectDestination).getDataStore();
        StoragePoolVO storeVO = new StoragePoolVO();
        doReturn(storeVO).when(primaryDataStoreDao).findById(0l);

        assertTrue(strategy.canHandle(volumeObjectSource, dataObjectDestination) == StrategyPriority.CANT_HANDLE);
    }

    @Test
    public void internalCanHandleTestAllStoragePoolsAreManaged() {
        configureAndTestInternalCanHandle(true, true, StrategyPriority.HIGHEST);
    }

    @Test
    public void internalCanHandleTestFirstStoragePoolsIsManaged() {
        configureAndTestInternalCanHandle(false, true, StrategyPriority.HIGHEST);
    }

    @Test
    public void internalCanHandleTestSecondStoragePoolsIsManaged() {
        configureAndTestInternalCanHandle(true, false, StrategyPriority.HIGHEST);
    }

    @Test
    public void internalCanHandleTestNoStoragePoolsIsManaged() {
        configureAndTestInternalCanHandle(false, false, StrategyPriority.CANT_HANDLE);
    }

    private void configureAndTestInternalCanHandle(boolean sPool0IsManaged, boolean sPool1IsManaged, StrategyPriority expectedStrategyPriority) {
        VolumeObject volumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(0l).when(volumeInfo).getPoolId();

        DataStore ds = Mockito.spy(new PrimaryDataStoreImpl());
        Mockito.doReturn(1l).when(ds).getId();

        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();
        volumeMap.put(volumeInfo, ds);

        StoragePoolVO storagePool0 = Mockito.spy(new StoragePoolVO());
        Mockito.doReturn(sPool0IsManaged).when(storagePool0).isManaged();
        StoragePoolVO storagePool1 = Mockito.spy(new StoragePoolVO());
        Mockito.doReturn(sPool1IsManaged).when(storagePool1).isManaged();

        Mockito.doReturn(storagePool0).when(primaryDataStoreDao).findById(0l);
        Mockito.doReturn(storagePool1).when(primaryDataStoreDao).findById(1l);

        StrategyPriority strategyPriority = strategy.internalCanHandle(volumeMap, new HostVO("srcHostUuid"), new HostVO("destHostUuid"));

        Assert.assertEquals(expectedStrategyPriority, strategyPriority);
    }

    @Test
    public void isStoragePoolTypeOfFileTest() {
        StoragePoolVO sourceStoragePool = Mockito.spy(new StoragePoolVO());
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length; i++) {
            Mockito.doReturn(storagePoolTypeArray[i]).when(sourceStoragePool).getPoolType();
            boolean result = strategy.isStoragePoolTypeOfFile(sourceStoragePool);
            if (sourceStoragePool.getPoolType() == StoragePoolType.Filesystem) {
                Assert.assertTrue(result);
            } else {
                Assert.assertFalse(result);
            }
        }
    }

    @Test
    public void generateDestPathTest() {
        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        HostVO destHost = new HostVO("guid");
        Mockito.doReturn("iScsiName").when(destVolumeInfo).get_iScsiName();
        Mockito.doReturn(0l).when(destVolumeInfo).getPoolId();
        Mockito.doReturn("expected").when(strategy).connectHostToVolume(destHost, 0l, "iScsiName");

        String expected = strategy.generateDestPath(destHost, Mockito.mock(StoragePoolVO.class), destVolumeInfo);

        Assert.assertEquals(expected, "expected");
        Mockito.verify(strategy).connectHostToVolume(destHost, 0l, "iScsiName");
    }

    @Test
    public void connectHostToVolumeSurfacesOriginalAgentErrorWhenAnswerTypeIsGeneric() {
        Host host = mock(Host.class);
        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        Answer answer = new Answer(mock(ModifyTargetsCommand.class), false, "Can't find volume:volume-1");

        Mockito.when(host.getId()).thenReturn(42L);
        Mockito.doReturn(storagePool).when(primaryDataStoreDao).findById(0L);
        Mockito.when(storagePool.getPoolType()).thenReturn(StoragePoolType.NetworkFilesystem);
        Mockito.when(storagePool.getUuid()).thenReturn("pool-uuid");
        Mockito.when(storagePool.getHostAddress()).thenReturn("10.0.0.10");
        Mockito.when(storagePool.getPort()).thenReturn(2049);
        Mockito.doReturn(answer).when(agentManager).easySend(Mockito.eq(42L), Mockito.any(ModifyTargetsCommand.class));

        try {
            strategy.connectHostToVolume(host, 0L, "volume-1");
            Assert.fail("Expected CloudRuntimeException to be thrown");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Can't find volume:volume-1"));
            Assert.assertFalse(e.getMessage().contains("ClassCastException"));
        }
    }

    @Test
    public void connectHostToVolumeThrowsWhenConnectedPathIsMissing() {
        Host host = mock(Host.class);
        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        ModifyTargetsAnswer answer = new ModifyTargetsAnswer();
        answer.setConnectedPaths(Collections.emptyList());

        Mockito.when(host.getId()).thenReturn(42L);
        Mockito.doReturn(storagePool).when(primaryDataStoreDao).findById(0L);
        Mockito.when(storagePool.getPoolType()).thenReturn(StoragePoolType.NetworkFilesystem);
        Mockito.when(storagePool.getUuid()).thenReturn("pool-uuid");
        Mockito.when(storagePool.getHostAddress()).thenReturn("10.0.0.10");
        Mockito.when(storagePool.getPort()).thenReturn(2049);
        Mockito.doReturn(answer).when(agentManager).easySend(Mockito.eq(42L), Mockito.any(ModifyTargetsCommand.class));

        try {
            strategy.connectHostToVolume(host, 0L, "volume-1");
            Assert.fail("Expected CloudRuntimeException to be thrown");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("no connected path was returned"));
            Assert.assertTrue(e.getMessage().contains("volume-1"));
        }
    }

    @Test
    public void configureMigrateDiskInfoTest() {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn("volume path").when(srcVolumeInfo).getPath();
        MigrateCommand.MigrateDiskInfo migrateDiskInfo = strategy.configureMigrateDiskInfo(srcVolumeInfo, "destPath", null);
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, migrateDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.RAW, migrateDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, migrateDiskInfo.getSource());
        Assert.assertEquals("destPath", migrateDiskInfo.getSourceText());
        Assert.assertEquals("volume path", migrateDiskInfo.getSerialNumber());
    }

    @Test
    public void configureMigrateDiskInfoWithBackingTest() {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn("volume path").when(srcVolumeInfo).getPath();
        MigrateCommand.MigrateDiskInfo migrateDiskInfo = strategy.configureMigrateDiskInfo(srcVolumeInfo, "destPath", "backingPath");
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, migrateDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.RAW, migrateDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, migrateDiskInfo.getSource());
        Assert.assertEquals("destPath", migrateDiskInfo.getSourceText());
        Assert.assertEquals("volume path", migrateDiskInfo.getSerialNumber());
        Assert.assertEquals("backingPath", migrateDiskInfo.getBackingStoreText());
    }

    @Test
    public void createLinkedCloneMigrationOptionsUsesSourceBackingFileWhenDestinationReferencePathDiffers() {
        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeInfo.class);
        VolumeInfo destVolumeInfo = Mockito.mock(VolumeInfo.class);
        StoragePoolVO srcPool = Mockito.mock(StoragePoolVO.class);
        DataStore srcDataStore = Mockito.mock(DataStore.class);
        Scope scope = Mockito.mock(Scope.class);
        VMTemplateStoragePoolVO ref = Mockito.mock(VMTemplateStoragePoolVO.class);

        Mockito.when(srcPool.getUuid()).thenReturn("src-pool");
        Mockito.when(srcPool.getPoolType()).thenReturn(StoragePoolType.NetworkFilesystem);
        Mockito.when(srcPool.getClusterId()).thenReturn(1L);
        Mockito.when(srcVolumeInfo.getTemplateId()).thenReturn(13L);
        Mockito.when(srcVolumeInfo.getDataStore()).thenReturn(srcDataStore);
        Mockito.when(srcDataStore.getScope()).thenReturn(scope);
        Mockito.when(scope.getScopeType()).thenReturn(ScopeType.CLUSTER);
        Mockito.when(destVolumeInfo.getPoolId()).thenReturn(4L);
        Mockito.when(templatePoolDao.findByPoolTemplate(4L, 13L, null)).thenReturn(ref);
        Mockito.when(ref.getInstallPath()).thenReturn("target-backing.qcow2");

        MigrationOptions options = strategy.createLinkedCloneMigrationOptions(srcVolumeInfo, destVolumeInfo, "source-backing.qcow2", srcPool);

        Assert.assertTrue(options.isCopySrcTemplate());
        Assert.assertEquals("source-backing.qcow2", options.getSrcBackingFilePath());
    }

    @Test
    public void updateCopiedTemplateReferenceUpdatesExistingDestinationReference() {
        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeInfo.class);
        VolumeInfo destVolumeInfo = Mockito.mock(VolumeInfo.class);
        VMTemplateStoragePoolVO srcRef = Mockito.mock(VMTemplateStoragePoolVO.class);
        VMTemplateStoragePoolVO destRef = Mockito.mock(VMTemplateStoragePoolVO.class);

        Mockito.when(srcVolumeInfo.getPoolId()).thenReturn(5L);
        Mockito.when(srcVolumeInfo.getTemplateId()).thenReturn(13L);
        Mockito.when(destVolumeInfo.getPoolId()).thenReturn(4L);
        Mockito.when(srcRef.getTemplateId()).thenReturn(13L);
        Mockito.when(srcRef.getTemplateSize()).thenReturn(1851129856L);
        Mockito.when(srcRef.getLocalDownloadPath()).thenReturn("d06b4640-d7d3-45d7-92c1-8cd3c8eb1eb7");
        Mockito.when(srcRef.getInstallPath()).thenReturn("d06b4640-d7d3-45d7-92c1-8cd3c8eb1eb7");
        Mockito.when(destRef.getId()).thenReturn(19L);
        Mockito.when(templatePoolDao.findByPoolTemplate(5L, 13L, null)).thenReturn(srcRef);
        Mockito.when(templatePoolDao.findByPoolTemplate(4L, 13L, null)).thenReturn(destRef);

        strategy.updateCopiedTemplateReference(srcVolumeInfo, destVolumeInfo);

        Mockito.verify(destRef).setDownloadPercent(100);
        Mockito.verify(destRef).setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        Mockito.verify(destRef).setState(ObjectInDataStoreStateMachine.State.Ready);
        Mockito.verify(destRef).setTemplateSize(1851129856L);
        Mockito.verify(destRef).setLocalDownloadPath("d06b4640-d7d3-45d7-92c1-8cd3c8eb1eb7");
        Mockito.verify(destRef).setInstallPath("d06b4640-d7d3-45d7-92c1-8cd3c8eb1eb7");
        Mockito.verify(templatePoolDao).update(19L, destRef);
        Mockito.verify(templatePoolDao, Mockito.never()).persist(Mockito.any(VMTemplateStoragePoolVO.class));
    }

    @Test
    public void updateCopiedTemplateReferencePersistsDestinationReferenceWhenMissing() {
        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeInfo.class);
        VolumeInfo destVolumeInfo = Mockito.mock(VolumeInfo.class);
        VMTemplateStoragePoolVO srcRef = Mockito.mock(VMTemplateStoragePoolVO.class);
        ArgumentCaptor<VMTemplateStoragePoolVO> captor = ArgumentCaptor.forClass(VMTemplateStoragePoolVO.class);

        Mockito.when(srcVolumeInfo.getPoolId()).thenReturn(5L);
        Mockito.when(srcVolumeInfo.getTemplateId()).thenReturn(13L);
        Mockito.when(destVolumeInfo.getPoolId()).thenReturn(4L);
        Mockito.when(srcRef.getTemplateId()).thenReturn(13L);
        Mockito.when(srcRef.getTemplateSize()).thenReturn(1851129856L);
        Mockito.when(srcRef.getLocalDownloadPath()).thenReturn("d06b4640-d7d3-45d7-92c1-8cd3c8eb1eb7");
        Mockito.when(srcRef.getInstallPath()).thenReturn("d06b4640-d7d3-45d7-92c1-8cd3c8eb1eb7");
        Mockito.when(templatePoolDao.findByPoolTemplate(5L, 13L, null)).thenReturn(srcRef);
        Mockito.when(templatePoolDao.findByPoolTemplate(4L, 13L, null)).thenReturn(null);

        strategy.updateCopiedTemplateReference(srcVolumeInfo, destVolumeInfo);

        Mockito.verify(templatePoolDao).persist(captor.capture());
        VMTemplateStoragePoolVO persistedRef = captor.getValue();
        Assert.assertEquals(4L, persistedRef.getPoolId());
        Assert.assertEquals(13L, persistedRef.getTemplateId());
        Assert.assertEquals(100, persistedRef.getDownloadPercent());
        Assert.assertEquals(VMTemplateStorageResourceAssoc.Status.DOWNLOADED, persistedRef.getDownloadState());
        Assert.assertEquals(ObjectInDataStoreStateMachine.State.Ready, persistedRef.getState());
        Assert.assertEquals(1851129856L, persistedRef.getTemplateSize());
        Assert.assertEquals("d06b4640-d7d3-45d7-92c1-8cd3c8eb1eb7", persistedRef.getLocalDownloadPath());
        Assert.assertEquals("d06b4640-d7d3-45d7-92c1-8cd3c8eb1eb7", persistedRef.getInstallPath());
        Mockito.verify(templatePoolDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any(VMTemplateStoragePoolVO.class));
    }

    @Test
    public void updateCopiedTemplateReferenceThrowsWhenSourceReferenceMissing() {
        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeInfo.class);
        VolumeInfo destVolumeInfo = Mockito.mock(VolumeInfo.class);

        Mockito.when(srcVolumeInfo.getPoolId()).thenReturn(5L);
        Mockito.when(srcVolumeInfo.getTemplateId()).thenReturn(13L);
        Mockito.when(templatePoolDao.findByPoolTemplate(5L, 13L, null)).thenReturn(null);

        try {
            strategy.updateCopiedTemplateReference(srcVolumeInfo, destVolumeInfo);
            Assert.fail("Expected CloudRuntimeException to be thrown");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("source template reference was not found"));
            Assert.assertTrue(e.getMessage().contains("pool [5]"));
            Assert.assertTrue(e.getMessage().contains("template [13]"));
        }
    }

    @Test
    public void shouldForceFullCloneMigrationReturnsTrueForMixedDirectDownloadVolumes() {
        VolumeInfo directDownloadVolume = Mockito.mock(VolumeInfo.class);
        VolumeInfo regularVolume = Mockito.mock(VolumeInfo.class);
        DataStore destPrimary = Mockito.mock(DataStore.class);
        DataStore otherDestPrimary = Mockito.mock(DataStore.class);
        Host destHost = Mockito.mock(Host.class);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();

        configurePoolLookup(directDownloadVolume, destPrimary, 1L, 2L, StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem);
        configurePoolLookup(regularVolume, otherDestPrimary, 3L, 4L, StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem);
        Mockito.when(directDownloadVolume.isDirectDownload()).thenReturn(true);
        Mockito.when(regularVolume.isDirectDownload()).thenReturn(false);

        volumeMap.put(directDownloadVolume, destPrimary);
        volumeMap.put(regularVolume, otherDestPrimary);

        Assert.assertTrue(strategy.shouldForceFullCloneMigration(volumeMap, destHost));
    }

    @Test
    public void shouldForceFullCloneMigrationIgnoresSkippedDirectDownloadVolumes() {
        VolumeInfo skippedDirectDownloadVolume = Mockito.mock(VolumeInfo.class);
        VolumeInfo regularVolume = Mockito.mock(VolumeInfo.class);
        DataStore samePowerFlexStore = Mockito.mock(DataStore.class);
        DataStore destPrimary = Mockito.mock(DataStore.class);
        Host destHost = Mockito.mock(Host.class);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();

        configurePoolLookup(skippedDirectDownloadVolume, samePowerFlexStore, 1L, 1L, StoragePoolType.PowerFlex, StoragePoolType.PowerFlex);
        configurePoolLookup(regularVolume, destPrimary, 3L, 4L, StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem);
        Mockito.when(regularVolume.isDirectDownload()).thenReturn(false);

        volumeMap.put(skippedDirectDownloadVolume, samePowerFlexStore);
        volumeMap.put(regularVolume, destPrimary);

        Assert.assertFalse(strategy.shouldForceFullCloneMigration(volumeMap, destHost));
    }

    @Test
    public void shouldForceFullCloneMigrationReturnsFalseWhenNoVolumeIsDirectDownload() {
        VolumeInfo regularVolume = Mockito.mock(VolumeInfo.class);
        DataStore destPrimary = Mockito.mock(DataStore.class);
        Host destHost = Mockito.mock(Host.class);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();

        configurePoolLookup(regularVolume, destPrimary, 3L, 4L, StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem);
        Mockito.when(regularVolume.isDirectDownload()).thenReturn(false);
        volumeMap.put(regularVolume, destPrimary);

        Assert.assertFalse(strategy.shouldForceFullCloneMigration(volumeMap, destHost));
    }

    private void configurePoolLookup(VolumeInfo volume, DataStore destStore, long sourcePoolId, long destPoolId, StoragePoolType sourcePoolType, StoragePoolType destPoolType) {
        StoragePoolVO sourcePool = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO destPool = sourcePoolId == destPoolId ? sourcePool : Mockito.mock(StoragePoolVO.class);

        Mockito.when(volume.getPoolId()).thenReturn(sourcePoolId);
        Mockito.when(destStore.getId()).thenReturn(destPoolId);
        Mockito.when(sourcePool.getId()).thenReturn(sourcePoolId);
        Mockito.lenient().when(destPool.getId()).thenReturn(destPoolId);
        Mockito.when(sourcePool.getPoolType()).thenReturn(sourcePoolType);
        Mockito.lenient().when(destPool.getPoolType()).thenReturn(destPoolType);
        Mockito.when(primaryDataStoreDao.findById(sourcePoolId)).thenReturn(sourcePool);
        Mockito.when(primaryDataStoreDao.findById(destPoolId)).thenReturn(destPool);
    }

    @Test
    public void decideMigrationTypeAndCopyTemplateIfNeededUsesFullCloneWhenForced() {
        Host destHost = Mockito.mock(Host.class);
        VMInstanceVO vmInstance = Mockito.mock(VMInstanceVO.class);
        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeInfo.class);
        StoragePoolVO sourceStoragePool = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO destStoragePool = Mockito.mock(StoragePoolVO.class);
        DataStore destDataStore = Mockito.mock(DataStore.class);

        Mockito.when(srcVolumeInfo.getId()).thenReturn(61L);

        MigrationOptions.Type migrationType = strategy.decideMigrationTypeAndCopyTemplateIfNeeded(destHost, vmInstance, srcVolumeInfo, sourceStoragePool,
                destStoragePool, destDataStore, true);

        Assert.assertEquals(MigrationOptions.Type.FullClone, migrationType);
        Mockito.verify(strategy, Mockito.never()).copyTemplateToTargetFilesystemStorageIfNeeded(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void decideMigrationTypeAndCopyTemplateIfNeededUsesLinkedCloneWhenForcedFlagIsFalse() {
        Host destHost = Mockito.mock(Host.class);
        VMInstanceVO vmInstance = Mockito.mock(VMInstanceVO.class);
        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeInfo.class);
        StoragePoolVO sourceStoragePool = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO destStoragePool = Mockito.mock(StoragePoolVO.class);
        DataStore destDataStore = Mockito.mock(DataStore.class);
        VMTemplateVO vmTemplate = Mockito.mock(VMTemplateVO.class);

        Mockito.when(vmInstance.getTemplateId()).thenReturn(101L);
        Mockito.when(vmTemplateDao.findById(101L)).thenReturn(vmTemplate);
        Mockito.when(vmTemplate.getName()).thenReturn("rocky-template");
        Mockito.when(srcVolumeInfo.getId()).thenReturn(61L);
        Mockito.when(srcVolumeInfo.getTemplateId()).thenReturn(13L);
        Mockito.when(destStoragePool.getPoolType()).thenReturn(StoragePoolType.Filesystem);
        Mockito.doReturn("source-backing.qcow2").when(strategy).getVolumeBackingFile(srcVolumeInfo);

        MigrationOptions.Type migrationType = strategy.decideMigrationTypeAndCopyTemplateIfNeeded(destHost, vmInstance, srcVolumeInfo, sourceStoragePool,
                destStoragePool, destDataStore, false);

        Assert.assertEquals(MigrationOptions.Type.LinkedClone, migrationType);
        Mockito.verify(strategy).copyTemplateToTargetFilesystemStorageIfNeeded(srcVolumeInfo, sourceStoragePool, destDataStore, destStoragePool, destHost);
    }

    @Test
    public void setVolumePathTest() {
        VolumeVO volume = new VolumeVO("name", 0l, 0l, 0l, 0l, 0l, "folder", "path", Storage.ProvisioningType.THIN, 0l, Volume.Type.ROOT);
        String volumePath = "iScsiName";
        volume.set_iScsiName(volumePath);

        strategy.setVolumePath(volume);

        Assert.assertEquals(volumePath, volume.getPath());
    }

    @Test
    public void shouldMigrateVolumeTest() {
        StoragePoolVO sourceStoragePool = Mockito.spy(new StoragePoolVO());
        HostVO destHost = new HostVO("guid");
        StoragePoolVO destStoragePool = new StoragePoolVO();
        StoragePoolType[] storagePoolTypes = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypes.length; i++) {
            Mockito.lenient().doReturn(storagePoolTypes[i]).when(sourceStoragePool).getPoolType();
            boolean result = strategy.shouldMigrateVolume(sourceStoragePool, destHost, destStoragePool);
            Assert.assertTrue(result);
        }
    }

    @Test
    public void isSourceAndDestinationPoolTypeOfNfsTestNfsNfs() {
        configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem, true);
    }

    @Test
    public void isSourceAndDestinationPoolTypeOfNfsTestNfsAny() {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length - 1; i++) {
            if (storagePoolTypeArray[i] != StoragePoolType.NetworkFilesystem) {
                configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(StoragePoolType.NetworkFilesystem, storagePoolTypeArray[i], false);
            }
        }
    }

    @Test
    public void isSourceAndDestinationPoolTypeOfNfsTestAnyNfs() {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length - 1; i++) {
            if (storagePoolTypeArray[i] != StoragePoolType.NetworkFilesystem) {
                configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(storagePoolTypeArray[i], StoragePoolType.NetworkFilesystem, false);
            }
        }
    }

    @Test
    public void isSourceAndDestinationPoolTypeOfNfsTestAnyAny() {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length - 1; i++) {
            for (int j = 0; j < storagePoolTypeArray.length - 1; j++) {
                if (storagePoolTypeArray[i] != StoragePoolType.NetworkFilesystem || storagePoolTypeArray[j] != StoragePoolType.NetworkFilesystem) {
                    configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(storagePoolTypeArray[i], storagePoolTypeArray[j], false);
                }
            }
        }
    }

    private void configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(StoragePoolType destStoragePoolType, StoragePoolType sourceStoragePoolType, boolean expected) {
        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeObject.class);
        Mockito.lenient().when(srcVolumeInfo.getId()).thenReturn(0l);

        DataStore destDataStore = Mockito.mock(PrimaryDataStoreImpl.class);
        Mockito.when(destDataStore.getId()).thenReturn(1l);

        StoragePoolVO destStoragePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(destStoragePool.getPoolType()).thenReturn(destStoragePoolType);

        StoragePoolVO sourceStoragePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(sourceStoragePool.getPoolType()).thenReturn(sourceStoragePoolType);

        Map<VolumeInfo, DataStore> volumeDataStoreMap = new HashMap<>();
        volumeDataStoreMap.put(srcVolumeInfo, destDataStore);

        Mockito.doReturn(sourceStoragePool).when(primaryDataStoreDao).findById(0l);
        Mockito.doReturn(destStoragePool).when(primaryDataStoreDao).findById(1l);

        boolean result = strategy.isSourceAndDestinationPoolTypeOfNfs(volumeDataStoreMap);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void formatMigrationElementsAsJsonToDisplayOnLogValidateFormat(){
        String objectName = "test";
        Long object = 1L, from = 2L, to = 3L;

        Assert.assertEquals(String.format("{%s: \"%s\", from: \"%s\", to:\"%s\"}", objectName, object, from, to), strategy.formatMigrationElementsAsJsonToDisplayOnLog(objectName,
                object, from, to));
    }

    @Test
    public void formatEntryOfVolumesAndStoragesAsJsonToDisplayOnLogValidateFormat(){
        Long volume = 1L, from = 2L, to = 3L;
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        DataStore dataStore = Mockito.mock(DataStore.class);

        Mockito.when(volumeInfo.getId()).thenReturn(volume);
        Mockito.when(volumeInfo.getPoolId()).thenReturn(from);
        Mockito.when(dataStore.getId()).thenReturn(to);

        Assert.assertEquals(String.format("{volume: \"%s\", from: \"%s\", to:\"%s\"}", volume, from, to), strategy.formatEntryOfVolumesAndStoragesAsJsonToDisplayOnLog(new AbstractMap.SimpleEntry<>(volumeInfo, dataStore)));
    }

    @Test
    public void validateSupportStoragePoolTypeDefaultValues() {
        Set<StoragePoolType> supportedTypes = new HashSet<>();
        supportedTypes.add(StoragePoolType.NetworkFilesystem);
        supportedTypes.add(StoragePoolType.SharedMountPoint);

        for (StoragePoolType poolType : StoragePoolType.values()) {
            boolean isSupported = strategy.supportStoragePoolType(poolType);
            if (supportedTypes.contains(poolType)) {
                assertTrue(isSupported);
            } else {
                assertFalse(isSupported);
            }
        }
    }

    @Test
    public void validateSupportStoragePoolTypeExtraValues() {
        Set<StoragePoolType> supportedTypes = new HashSet<>();
        supportedTypes.add(StoragePoolType.NetworkFilesystem);
        supportedTypes.add(StoragePoolType.SharedMountPoint);
        supportedTypes.add(StoragePoolType.Iscsi);
        supportedTypes.add(StoragePoolType.CLVM);

        for (StoragePoolType poolType : StoragePoolType.values()) {
            boolean isSupported = strategy.supportStoragePoolType(poolType, StoragePoolType.Iscsi, StoragePoolType.CLVM);
            if (supportedTypes.contains(poolType)) {
                assertTrue(isSupported);
            } else {
                assertFalse(isSupported);
            }
        }
    }

    @Test
    public void validateIsStoragePoolTypeInListReturnsTrue() {
        StoragePoolType[] listTypes = new StoragePoolType[3];
        listTypes[0] = StoragePoolType.LVM;
        listTypes[1] = StoragePoolType.NetworkFilesystem;
        listTypes[2] = StoragePoolType.SharedMountPoint;

        assertTrue(strategy.isStoragePoolTypeInList(StoragePoolType.SharedMountPoint, listTypes));
    }

    @Test
    public void validateIsStoragePoolTypeInListReturnsFalse() {
        StoragePoolType[] listTypes = new StoragePoolType[3];
        listTypes[0] = StoragePoolType.LVM;
        listTypes[1] = StoragePoolType.NetworkFilesystem;
        listTypes[2] = StoragePoolType.RBD;

        assertFalse(strategy.isStoragePoolTypeInList(StoragePoolType.SharedMountPoint, listTypes));
    }
}
