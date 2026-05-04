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
package org.apache.cloudstack.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.InternalBackupJoinDao;
import org.apache.cloudstack.backup.dao.InternalBackupStoragePoolDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.RevertSnapshotCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreObjectDownloadDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreObjectDownloadVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.Upload;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.snapshot.VMSnapshot;

@RunWith(MockitoJUnitRunner.class)
public class InternalBackupServiceImplTest {

    @Mock
    private InternalBackupStoragePoolDao internalBackupStoragePoolDaoMock;

    @Mock
    private VolumeObjectTO volumeObjectToMock;

    @Mock
    private SnapshotObjectTO snapshotObjectToMock;

    @Mock
    private InternalBackupStoragePoolVO internalBackupStoragePoolVoMock;

    @Mock
    private InternalBackupJoinDao internalBackupJoinDaoMock;

    @Mock
    private BackupDetailsDao backupDetailDaoMock;

    @Mock
    private BackupDetailVO backupDetailVoMock;

    @Mock
    private BackupDetailVO backupDetailVoMock2;

    @Mock
    private InternalBackupJoinVO internalBackupJoinVoMock;

    @Mock
    private Volume volumeMock;

    @Mock
    private VolumeVO volumeVoMock;

    @Mock
    private VirtualMachine virtualMachineMock;

    @Mock
    private InternalBackupProvider internalBackupProviderMock;

    @Mock
    private VirtualMachineManager virtualMachineManagerMock;

    @Mock
    private VolumeDao volumeDaoMock;

    @Mock
    private ImageStoreObjectDownloadDao imageStoreObjectDownloadDaoMock;

    @Mock
    private DataStoreManager dataStoreMgrMock;

    @Mock
    private ImageStoreEntity imageStoreEntityMock;

    @Mock
    private ImageStoreObjectDownloadVO imageStoreObjectDownloadVoMock;

    @Mock
    private VMSnapshot vmSnapshotMock;

    @Spy
    @InjectMocks
    private InternalBackupServiceImpl internalBackupServiceImplSpy;

    private static final long IMAGE_STORE_ID = 7L;
    private static final String SCREENSHOT_PATH = "/tmp/screenshot.png";
    private static final long VOLUME_ID = 42L;
    private static final long BACKUP_ID = 100L;
    private static final long ZONE_ID = 1L;
    private static final long OLD_VOLUME_ID = 5L;
    private static final long NEW_VOLUME_ID = 6L;
    private static final long INSTANCE_ID = 10L;

    @Test
    public void configureChainInfoTestNonVolumeObjectReturnsImmediately() {
        DataTO dataToMock = mock(DataTO.class);
        Command cmdMock = mock(Command.class);

        internalBackupServiceImplSpy.configureChainInfo(dataToMock, cmdMock);

        verify(internalBackupStoragePoolDaoMock, never()).findOneByVolumeId(anyLong());
    }

    @Test
    public void configureChainInfoTestVolumeWithoutBackupDeltaReturnsImmediately() {
        doReturn(VOLUME_ID).when(volumeObjectToMock).getVolumeId();
        doReturn(null).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);

        internalBackupServiceImplSpy.configureChainInfo(volumeObjectToMock, mock(Command.class));

        verify(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        verify(volumeObjectToMock, never()).setChainInfo(anyString());
    }

    @Test
    public void configureChainInfoTestSetsChainInfoForGenericCommand() {
        doReturn(VOLUME_ID).when(volumeObjectToMock).getVolumeId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        doReturn("/path/to/parent").when(internalBackupStoragePoolVoMock).getBackupDeltaParentPath();

        Command cmdMock = mock(Command.class);

        internalBackupServiceImplSpy.configureChainInfo(volumeObjectToMock, cmdMock);

        verify(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        verify(volumeObjectToMock).setChainInfo("/path/to/parent");
    }

    @Test
    public void configureChainInfoTestSetsDeleteChainForDeleteCommand() {
        doReturn(VOLUME_ID).when(volumeObjectToMock).getVolumeId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        doReturn("/path/to/parent").when(internalBackupStoragePoolVoMock).getBackupDeltaParentPath();

        DeleteCommand deleteCommand = new DeleteCommand(volumeObjectToMock);

        internalBackupServiceImplSpy.configureChainInfo(volumeObjectToMock, deleteCommand);

        verify(volumeObjectToMock).setChainInfo("/path/to/parent");
        assertTrue(deleteCommand.isDeleteChain());
    }

    @Test
    public void configureChainInfoTestSetsDeleteChainForRevertSnapshotCommand() {
        doReturn(VOLUME_ID).when(volumeObjectToMock).getVolumeId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        doReturn("/path/to/parent").when(internalBackupStoragePoolVoMock).getBackupDeltaParentPath();

        RevertSnapshotCommand revertSnapshotCommand = new RevertSnapshotCommand(snapshotObjectToMock, snapshotObjectToMock);

        internalBackupServiceImplSpy.configureChainInfo(volumeObjectToMock, revertSnapshotCommand);

        verify(volumeObjectToMock).setChainInfo("/path/to/parent");
        assertTrue(revertSnapshotCommand.isDeleteChain());
    }

    @Test
    public void cleanupBackupMetadataTestNoDeltaReturnsImmediately() {
        doReturn(null).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);

        internalBackupServiceImplSpy.cleanupBackupMetadata(VOLUME_ID);

        verify(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        verify(internalBackupStoragePoolDaoMock, never()).expungeByVolumeId(VOLUME_ID);
        verify(internalBackupJoinDaoMock, never()).findById(anyLong());
    }

    @Test
    public void cleanupBackupMetadataTestDeltaExistsButOtherDeltasRemainReturnsImmediately() {
        doReturn(BACKUP_ID).when(internalBackupStoragePoolVoMock).getBackupId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        doReturn(List.of(internalBackupStoragePoolVoMock, mock(InternalBackupStoragePoolVO.class)))
                .when(internalBackupStoragePoolDaoMock).listByBackupId(BACKUP_ID);

        internalBackupServiceImplSpy.cleanupBackupMetadata(VOLUME_ID);

        verify(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        verify(internalBackupStoragePoolDaoMock).expungeByVolumeId(VOLUME_ID);
        verify(internalBackupStoragePoolDaoMock).listByBackupId(BACKUP_ID);
        verify(internalBackupJoinDaoMock, never()).findById(anyLong());
    }

    @Test
    public void cleanupBackupMetadataTestLastDeltaAndEndOfChainTrue() {
        doReturn(BACKUP_ID).when(internalBackupStoragePoolVoMock).getBackupId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        doReturn(List.of()).when(internalBackupStoragePoolDaoMock).listByBackupId(BACKUP_ID);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(BACKUP_ID);
        doReturn(BACKUP_ID).when(internalBackupJoinVoMock).getId();
        doReturn(true).when(internalBackupJoinVoMock).getEndOfChain();

        internalBackupServiceImplSpy.cleanupBackupMetadata(VOLUME_ID);

        verify(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        verify(internalBackupStoragePoolDaoMock).expungeByVolumeId(VOLUME_ID);
        verify(internalBackupStoragePoolDaoMock).listByBackupId(BACKUP_ID);
        verify(internalBackupJoinDaoMock).findById(BACKUP_ID);
        verify(backupDetailDaoMock).removeDetail(BACKUP_ID, BackupDetailsDao.CURRENT);
        verify(backupDetailDaoMock, never()).persist(any());
    }

    @Test
    public void cleanupBackupMetadataTestLastDeltaAndEndOfChainFalse() {
        doReturn(BACKUP_ID).when(internalBackupStoragePoolVoMock).getBackupId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        doReturn(List.of()).when(internalBackupStoragePoolDaoMock).listByBackupId(BACKUP_ID);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(BACKUP_ID);
        doReturn(BACKUP_ID).when(internalBackupJoinVoMock).getId();
        doReturn(false).when(internalBackupJoinVoMock).getEndOfChain();

        internalBackupServiceImplSpy.cleanupBackupMetadata(VOLUME_ID);

        verify(internalBackupStoragePoolDaoMock).findOneByVolumeId(VOLUME_ID);
        verify(internalBackupStoragePoolDaoMock).expungeByVolumeId(VOLUME_ID);
        verify(internalBackupStoragePoolDaoMock).listByBackupId(BACKUP_ID);
        verify(internalBackupJoinDaoMock).findById(BACKUP_ID);
        verify(backupDetailDaoMock).removeDetail(BACKUP_ID, BackupDetailsDao.CURRENT);
        verify(backupDetailDaoMock).persist(any(BackupDetailVO.class));
    }

    @Test
    public void prepareVolumeForDetachTestBackupFrameworkDisabledReturnsImmediately() {
        doReturn(true).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);

        internalBackupServiceImplSpy.prepareVolumeForDetach(volumeMock, virtualMachineMock);

        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy, never()).getInternalBackupProviderForZone(anyLong());
    }

    @Test
    public void prepareVolumeForDetachTestProviderIsNullReturnsImmediately() {
        doReturn(false).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        doReturn(ZONE_ID).when(virtualMachineMock).getDataCenterId();
        doReturn(null).when(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);

        internalBackupServiceImplSpy.prepareVolumeForDetach(volumeMock, virtualMachineMock);

        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        verify(internalBackupProviderMock, never()).prepareVolumeForDetach(any(), any());
    }

    @Test
    public void prepareVolumeForDetachTestProviderCallsPrepareVolumeForDetach() {
        doReturn(false).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        doReturn(ZONE_ID).when(virtualMachineMock).getDataCenterId();
        doReturn(internalBackupProviderMock).when(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        doNothing().when(internalBackupProviderMock).prepareVolumeForDetach(volumeMock, virtualMachineMock);

        internalBackupServiceImplSpy.prepareVolumeForDetach(volumeMock, virtualMachineMock);

        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        verify(internalBackupProviderMock).prepareVolumeForDetach(volumeMock, virtualMachineMock);
    }

    @Test
    public void prepareVolumeForMigrationTestVolumeInstanceIdIsNullReturnsImmediately() {
        doReturn(null).when(volumeMock).getInstanceId();

        internalBackupServiceImplSpy.prepareVolumeForMigration(volumeMock);

        verify(volumeMock).getInstanceId();
        verify(virtualMachineManagerMock, never()).findById(anyLong());
    }

    @Test
    public void prepareVolumeForMigrationTestBackupFrameworkDisabledReturnsImmediately() {
        doReturn(INSTANCE_ID).when(volumeMock).getInstanceId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(true).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);

        internalBackupServiceImplSpy.prepareVolumeForMigration(volumeMock);

        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy, never()).getInternalBackupProviderForZone(anyLong());
    }

    @Test
    public void prepareVolumeForMigrationTestProviderIsNullReturnsImmediately() {
        doReturn(INSTANCE_ID).when(volumeMock).getInstanceId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(false).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        doReturn(ZONE_ID).when(volumeMock).getDataCenterId();
        doReturn(null).when(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);

        internalBackupServiceImplSpy.prepareVolumeForMigration(volumeMock);

        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        verify(internalBackupProviderMock, never()).prepareVolumeForMigration(any(), any());
    }

    @Test
    public void prepareVolumeForMigrationTestProviderCallsPrepareVolumeForMigration() {
        doReturn(INSTANCE_ID).when(volumeMock).getInstanceId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(false).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        doReturn(ZONE_ID).when(volumeMock).getDataCenterId();
        doReturn(internalBackupProviderMock).when(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        doNothing().when(internalBackupProviderMock).prepareVolumeForMigration(volumeMock, virtualMachineMock);

        internalBackupServiceImplSpy.prepareVolumeForMigration(volumeMock);

        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        verify(internalBackupProviderMock).prepareVolumeForMigration(volumeMock, virtualMachineMock);
    }

    @Test
    public void updateVolumeIdTestVolumeInstanceIdIsNullReturnsImmediately() {
        doReturn(volumeVoMock).when(volumeDaoMock).findById(NEW_VOLUME_ID);
        doReturn(null).when(volumeVoMock).getInstanceId();

        internalBackupServiceImplSpy.updateVolumeId(OLD_VOLUME_ID, NEW_VOLUME_ID);

        verify(volumeDaoMock).findById(NEW_VOLUME_ID);
        verify(volumeVoMock).getInstanceId();
        verify(virtualMachineManagerMock, never()).findById(anyLong());
    }

    @Test
    public void updateVolumeIdTestBackupFrameworkDisabledReturnsImmediately() {
        doReturn(volumeVoMock).when(volumeDaoMock).findById(NEW_VOLUME_ID);
        doReturn(INSTANCE_ID).when(volumeVoMock).getInstanceId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(true).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);

        internalBackupServiceImplSpy.updateVolumeId(OLD_VOLUME_ID, NEW_VOLUME_ID);

        verify(volumeDaoMock).findById(NEW_VOLUME_ID);
        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy, never()).getInternalBackupProviderForZone(anyLong());
    }

    @Test
    public void updateVolumeIdTestProviderIsNullReturnsImmediately() {
        doReturn(volumeVoMock).when(volumeDaoMock).findById(NEW_VOLUME_ID);
        doReturn(INSTANCE_ID).when(volumeVoMock).getInstanceId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(false).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        doReturn(ZONE_ID).when(virtualMachineMock).getDataCenterId();
        doReturn(null).when(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);

        internalBackupServiceImplSpy.updateVolumeId(OLD_VOLUME_ID, NEW_VOLUME_ID);

        verify(volumeDaoMock).findById(NEW_VOLUME_ID);
        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        verify(internalBackupProviderMock, never()).updateVolumeId(any(), anyLong(), anyLong());
    }

    @Test
    public void updateVolumeIdTestProviderCallsUpdateVolumeId() {
        doReturn(volumeVoMock).when(volumeDaoMock).findById(NEW_VOLUME_ID);
        doReturn(INSTANCE_ID).when(volumeVoMock).getInstanceId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(false).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        doReturn(ZONE_ID).when(virtualMachineMock).getDataCenterId();
        doReturn(internalBackupProviderMock).when(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        doNothing().when(internalBackupProviderMock).updateVolumeId(virtualMachineMock, OLD_VOLUME_ID, NEW_VOLUME_ID);

        internalBackupServiceImplSpy.updateVolumeId(OLD_VOLUME_ID, NEW_VOLUME_ID);

        verify(volumeDaoMock).findById(NEW_VOLUME_ID);
        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        verify(internalBackupProviderMock).updateVolumeId(virtualMachineMock, OLD_VOLUME_ID, NEW_VOLUME_ID);
    }

    @Test
    public void downloadScreenshotTestScreenshotPathDetailMissingReturnsNotCreated() {
        doReturn(null).when(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.SCREENSHOT_PATH);

        ExtractResponse result = internalBackupServiceImplSpy.downloadScreenshot(BACKUP_ID);

        verify(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.SCREENSHOT_PATH);
        assertEquals(Upload.Status.DOWNLOAD_URL_NOT_CREATED.toString(), result.getState());
    }

    @Test
    public void downloadScreenshotTestImageStoreObjectExistsReturnsCreatedResponse() {
        doReturn(backupDetailVoMock).when(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.SCREENSHOT_PATH);
        doReturn(backupDetailVoMock2).when(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.IMAGE_STORE_ID);
        doReturn(String.valueOf(IMAGE_STORE_ID)).when(backupDetailVoMock2).getValue();
        doReturn(SCREENSHOT_PATH).when(backupDetailVoMock).getValue();
        doReturn(imageStoreEntityMock).when(dataStoreMgrMock).getDataStore(IMAGE_STORE_ID, DataStoreRole.Image);
        doReturn(imageStoreObjectDownloadVoMock).when(imageStoreObjectDownloadDaoMock)
                .findByStoreIdAndPath(IMAGE_STORE_ID, SCREENSHOT_PATH);
        doReturn("http://download/url").when(imageStoreObjectDownloadVoMock).getDownloadUrl();

        ExtractResponse result = internalBackupServiceImplSpy.downloadScreenshot(BACKUP_ID);

        verify(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.SCREENSHOT_PATH);
        verify(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.IMAGE_STORE_ID);
        verify(imageStoreObjectDownloadDaoMock).findByStoreIdAndPath(IMAGE_STORE_ID, SCREENSHOT_PATH);
        verify(imageStoreObjectDownloadDaoMock, never()).persist(any());
        assertEquals("http://download/url", result.getUrl());
        assertEquals("screenshot.png", result.getName());
        assertEquals(Upload.Status.DOWNLOAD_URL_CREATED.toString(), result.getState());
    }

    @Test
    public void downloadScreenshotTestImageStoreObjectMissingButPersistSucceedsReturnsCreatedResponse() {
        doReturn(backupDetailVoMock).when(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.SCREENSHOT_PATH);
        doReturn(backupDetailVoMock2).when(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.IMAGE_STORE_ID);
        doReturn(String.valueOf(IMAGE_STORE_ID)).when(backupDetailVoMock2).getValue();
        doReturn(SCREENSHOT_PATH).when(backupDetailVoMock).getValue();
        doReturn(imageStoreEntityMock).when(dataStoreMgrMock).getDataStore(IMAGE_STORE_ID, DataStoreRole.Image);
        doReturn(null).when(imageStoreObjectDownloadDaoMock).findByStoreIdAndPath(IMAGE_STORE_ID, SCREENSHOT_PATH);
        doReturn(123L).when(imageStoreEntityMock).getId();
        doReturn("http://download/url").when(imageStoreEntityMock).createEntityExtractUrl(SCREENSHOT_PATH, Storage.ImageFormat.PNG, null);
        doReturn(imageStoreObjectDownloadVoMock).when(imageStoreObjectDownloadDaoMock).persist(any(ImageStoreObjectDownloadVO.class));
        doReturn("http://download/url").when(imageStoreObjectDownloadVoMock).getDownloadUrl();

        ExtractResponse result = internalBackupServiceImplSpy.downloadScreenshot(BACKUP_ID);

        verify(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.SCREENSHOT_PATH);
        verify(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.IMAGE_STORE_ID);
        verify(imageStoreObjectDownloadDaoMock).findByStoreIdAndPath(IMAGE_STORE_ID, SCREENSHOT_PATH);
        verify(imageStoreEntityMock).createEntityExtractUrl(SCREENSHOT_PATH, Storage.ImageFormat.PNG, null);
        verify(imageStoreObjectDownloadDaoMock).persist(any(ImageStoreObjectDownloadVO.class));
        assertEquals("http://download/url", result.getUrl());
        assertEquals("screenshot.png", result.getName());
        assertEquals(Upload.Status.DOWNLOAD_URL_CREATED.toString(), result.getState());
    }

    @Test
    public void downloadScreenshotTestImageStoreObjectMissingAndPersistReturnsNullReturnsNotCreated() {
        doReturn(backupDetailVoMock).when(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.SCREENSHOT_PATH);
        doReturn(backupDetailVoMock2).when(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.IMAGE_STORE_ID);
        doReturn(String.valueOf(IMAGE_STORE_ID)).when(backupDetailVoMock2).getValue();
        doReturn(SCREENSHOT_PATH).when(backupDetailVoMock).getValue();
        doReturn(imageStoreEntityMock).when(dataStoreMgrMock).getDataStore(IMAGE_STORE_ID, DataStoreRole.Image);
        doReturn(null).when(imageStoreObjectDownloadDaoMock).findByStoreIdAndPath(IMAGE_STORE_ID, SCREENSHOT_PATH);
        doReturn(123L).when(imageStoreEntityMock).getId();
        doReturn("http://download/url").when(imageStoreEntityMock).createEntityExtractUrl(SCREENSHOT_PATH, Storage.ImageFormat.PNG, null);
        doReturn(null).when(imageStoreObjectDownloadDaoMock).persist(any(ImageStoreObjectDownloadVO.class));

        ExtractResponse result = internalBackupServiceImplSpy.downloadScreenshot(BACKUP_ID);

        verify(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.SCREENSHOT_PATH);
        verify(backupDetailDaoMock).findDetail(BACKUP_ID, BackupDetailsDao.IMAGE_STORE_ID);
        verify(imageStoreObjectDownloadDaoMock).findByStoreIdAndPath(IMAGE_STORE_ID, SCREENSHOT_PATH);
        verify(imageStoreEntityMock).createEntityExtractUrl(SCREENSHOT_PATH, Storage.ImageFormat.PNG, null);
        verify(imageStoreObjectDownloadDaoMock).persist(any(ImageStoreObjectDownloadVO.class));
        org.junit.Assert.assertNull(result.getUrl());
        org.junit.Assert.assertNull(result.getName());
        org.junit.Assert.assertEquals(Upload.Status.DOWNLOAD_URL_NOT_CREATED.toString(), result.getState());
    }

    @Test
    public void prepareVmForSnapshotRevertTestBackupFrameworkDisabledReturnsImmediately() {
        doReturn(INSTANCE_ID).when(vmSnapshotMock).getVmId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(true).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);

        internalBackupServiceImplSpy.prepareVmForSnapshotRevert(vmSnapshotMock);

        verify(vmSnapshotMock).getVmId();
        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy, never()).getInternalBackupProviderForZone(anyLong());
    }

    @Test
    public void prepareVmForSnapshotRevertTestProviderIsNullReturnsImmediately() {
        doReturn(INSTANCE_ID).when(vmSnapshotMock).getVmId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(false).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        doReturn(ZONE_ID).when(virtualMachineMock).getDataCenterId();
        doReturn(null).when(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);

        internalBackupServiceImplSpy.prepareVmForSnapshotRevert(vmSnapshotMock);

        verify(vmSnapshotMock).getVmId();
        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        verify(internalBackupProviderMock, never()).prepareVmForSnapshotRevert(any(), any());
    }

    @Test
    public void prepareVmForSnapshotRevertTestProviderCallsPrepareVmForSnapshotRevert() {
        doReturn(INSTANCE_ID).when(vmSnapshotMock).getVmId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(INSTANCE_ID);
        doReturn(false).when(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        doReturn(ZONE_ID).when(virtualMachineMock).getDataCenterId();
        doReturn(internalBackupProviderMock).when(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        doNothing().when(internalBackupProviderMock).prepareVmForSnapshotRevert(vmSnapshotMock, virtualMachineMock);

        internalBackupServiceImplSpy.prepareVmForSnapshotRevert(vmSnapshotMock);

        verify(vmSnapshotMock).getVmId();
        verify(virtualMachineManagerMock).findById(INSTANCE_ID);
        verify(internalBackupServiceImplSpy).isBackupFrameworkDisabled(virtualMachineMock);
        verify(internalBackupServiceImplSpy).getInternalBackupProviderForZone(ZONE_ID);
        verify(internalBackupProviderMock).prepareVmForSnapshotRevert(vmSnapshotMock, virtualMachineMock);
    }
}
