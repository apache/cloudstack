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
//

package org.apache.cloudstack.storage.datastore.driver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.datastore.api.Volume;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOPrimaryDataStoreDriverVolumeFromSnapshotTest {

    private static final long POOL_ID = 7L;
    private static final long SNAPSHOT_ID = 42L;
    private static final long NEW_VOLUME_ID = 100L;
    private static final long BASE_VOLUME_ID = 99L;
    private static final long HOST_ID = 5L;
    private static final String SDC_ID = "c70c930300000001";
    private static final String SNAPSHOT_PATH = "e44cfc4c00000002:snap-42-abcd-sim";
    private static final String NEW_VOLUME_PATH = "e44cfc4c00000003:vol-100-abcd-sim";

    @Spy
    @InjectMocks
    ScaleIOPrimaryDataStoreDriver driver = new ScaleIOPrimaryDataStoreDriver();

    @Mock
    VolumeDetailsDao volumeDetailsDao;
    @Mock
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Mock
    SnapshotDao snapshotDao;
    @Mock
    VolumeDao volumeDao;
    @Mock
    PrimaryDataStoreDao storagePoolDao;
    @Mock
    DataStoreManager dataStoreMgr;
    @Mock
    VolumeDataFactory volumeDataFactory;
    @Mock
    EndPointSelector selector;
    @Mock
    HostDao hostDao;
    @Mock
    ConfigurationDao configDao;

    @Mock
    ScaleIOGatewayClient client;
    @Mock
    StoragePoolVO storagePool;
    @Mock
    VolumeVO newVolume;
    @Mock
    SnapshotDataStoreVO snapshotStore;
    @Mock
    PrimaryDataStore dataStore;
    @Mock
    VolumeInfo newVolumeInfo;
    @Mock
    EndPoint endPoint;
    @Mock
    HostVO host;

    @Before
    public void setUp() {
        lenient().when(storagePool.getId()).thenReturn(POOL_ID);
        lenient().when(newVolume.getId()).thenReturn(NEW_VOLUME_ID);
        lenient().when(newVolume.getPath()).thenReturn(NEW_VOLUME_PATH);
        lenient().when(newVolume.getSize()).thenReturn(16L << 30);
        lenient().when(snapshotStore.getInstallPath()).thenReturn(SNAPSHOT_PATH);
        lenient().when(snapshotStore.getDataStoreId()).thenReturn(POOL_ID);
        lenient().when(snapshotStore.getSize()).thenReturn(16L << 30);
        lenient().when(snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(SNAPSHOT_ID, DataStoreRole.Primary)).thenReturn(snapshotStore);

        SnapshotVO snapshot = mock(SnapshotVO.class);
        lenient().when(snapshot.getVolumeId()).thenReturn(BASE_VOLUME_ID);
        lenient().when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(snapshot);
        VolumeVO baseVolume = mock(VolumeVO.class);
        lenient().when(baseVolume.getFormat()).thenReturn(Storage.ImageFormat.RAW);
        lenient().when(volumeDao.findByIdIncludingRemoved(BASE_VOLUME_ID)).thenReturn(baseVolume);

        lenient().when(dataStoreMgr.getDataStore(POOL_ID, DataStoreRole.Primary)).thenReturn(dataStore);
        lenient().when(volumeDataFactory.getVolume(NEW_VOLUME_ID, dataStore)).thenReturn(newVolumeInfo);
        lenient().when(newVolumeInfo.getTO()).thenReturn(new VolumeObjectTO());
        lenient().when(endPoint.getId()).thenReturn(HOST_ID);
        lenient().when(hostDao.findById(HOST_ID)).thenReturn(host);
    }

    private void stubCopyPath() {
        when(selector.select(newVolumeInfo)).thenReturn(endPoint);
        doReturn(SDC_ID).when(driver).prepareSdcOnHost(host, dataStore);
        doReturn(mock(PrimaryDataStoreTO.class)).when(driver).getPrimaryDataStoreTOForManagedVolume(any(), anyString(), any(), anyLong());
        doNothing().when(driver).revokeVolumeAccess(anyString(), any(), any());
        when(client.mapVolumeToSdc(anyString(), eq(SDC_ID))).thenReturn(true);
    }

    @Test
    public void getSnapshotIdToCreateVolumeFromWithoutDetail() {
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(NEW_VOLUME_ID);
        Assert.assertNull(driver.getSnapshotIdToCreateVolumeFrom(volumeInfo));
    }

    @Test
    public void getSnapshotIdToCreateVolumeFromWithCloneOfSnapshotDetail() {
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(NEW_VOLUME_ID);
        VolumeDetailVO detail = mock(VolumeDetailVO.class);
        when(detail.getValue()).thenReturn(String.valueOf(SNAPSHOT_ID));
        when(volumeDetailsDao.findDetail(NEW_VOLUME_ID, ScaleIOPrimaryDataStoreDriver.CLONE_OF_SNAPSHOT)).thenReturn(detail);
        Assert.assertEquals(Long.valueOf(SNAPSHOT_ID), driver.getSnapshotIdToCreateVolumeFrom(volumeInfo));
    }

    @Test
    public void copySnapshotToNewVolumeMapsCopiesAndUnmaps() {
        stubCopyPath();
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, true, null));

        driver.copySnapshotToNewVolume(SNAPSHOT_ID, newVolume, storagePool, client);

        ArgumentCaptor<CopyCommand> cmdCaptor = ArgumentCaptor.forClass(CopyCommand.class);
        InOrder order = inOrder(client, endPoint, driver);
        order.verify(client).mapVolumeToSdc("e44cfc4c00000002", SDC_ID);
        order.verify(client).mapVolumeToSdc("e44cfc4c00000003", SDC_ID);
        order.verify(endPoint).sendMessage(cmdCaptor.capture());
        order.verify(driver).revokeVolumeAccess(SNAPSHOT_PATH, host, dataStore);
        order.verify(driver).revokeVolumeAccess(NEW_VOLUME_PATH, host, dataStore);

        CopyCommand cmd = cmdCaptor.getValue();
        Assert.assertTrue(cmd.getSrcTO() instanceof VolumeObjectTO);
        Assert.assertEquals(SNAPSHOT_PATH, cmd.getSrcTO().getPath());
        Assert.assertEquals(Storage.ImageFormat.RAW, ((VolumeObjectTO) cmd.getSrcTO()).getFormat());
        Assert.assertEquals(NEW_VOLUME_PATH, cmd.getDestTO().getPath());
        Assert.assertEquals(Storage.ImageFormat.RAW, ((VolumeObjectTO) cmd.getDestTO()).getFormat());
        verify(client, never()).deleteVolume(anyString());
    }

    @Test
    public void copySnapshotToNewVolumeFailedCopyRemovesNewVolumeAndUnmaps() {
        stubCopyPath();
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, false, "qemu-img convert failed"));

        try {
            driver.copySnapshotToNewVolume(SNAPSHOT_ID, newVolume, storagePool, client);
            Assert.fail("expected the copy failure to be raised");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("qemu-img convert failed"));
        }
        verify(driver).revokeVolumeAccess(SNAPSHOT_PATH, host, dataStore);
        verify(driver).revokeVolumeAccess(NEW_VOLUME_PATH, host, dataStore);
        verify(client).deleteVolume("e44cfc4c00000003");
    }

    @Test
    public void copySnapshotToNewVolumeMapFailureRemovesNewVolume() {
        stubCopyPath();
        when(client.mapVolumeToSdc("e44cfc4c00000003", SDC_ID)).thenReturn(false);

        Assert.assertThrows(CloudRuntimeException.class, () -> driver.copySnapshotToNewVolume(SNAPSHOT_ID, newVolume, storagePool, client));
        verify(endPoint, never()).sendMessage(any());
        verify(driver).revokeVolumeAccess(SNAPSHOT_PATH, host, dataStore);
        verify(client).deleteVolume("e44cfc4c00000003");
    }

    @Test
    public void copySnapshotToNewVolumeNoSdcOnHost() {
        stubCopyPath();
        doReturn(null).when(driver).prepareSdcOnHost(host, dataStore);

        Assert.assertThrows(CloudRuntimeException.class, () -> driver.copySnapshotToNewVolume(SNAPSHOT_ID, newVolume, storagePool, client));
        verify(client, never()).mapVolumeToSdc(anyString(), anyString());
        verify(client).deleteVolume("e44cfc4c00000003");
    }

    @Test
    public void copySnapshotToNewVolumeNoEndpoint() {
        when(selector.select(newVolumeInfo)).thenReturn(null);

        Assert.assertThrows(CloudRuntimeException.class, () -> driver.copySnapshotToNewVolume(SNAPSHOT_ID, newVolume, storagePool, client));
        verify(client).deleteVolume("e44cfc4c00000003");
        verify(client, never()).mapVolumeToSdc(anyString(), anyString());
    }

    @Test
    public void copySnapshotToNewVolumeSnapshotOnAnotherPool() {
        when(snapshotStore.getDataStoreId()).thenReturn(POOL_ID + 1);

        Assert.assertThrows(CloudRuntimeException.class, () -> driver.copySnapshotToNewVolume(SNAPSHOT_ID, newVolume, storagePool, client));
        verify(client, never()).mapVolumeToSdc(anyString(), anyString());
        verify(selector, never()).select(any(VolumeInfo.class));
    }

    @Test
    public void copySnapshotToNewVolumeSnapshotNotOnPrimary() {
        when(snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(SNAPSHOT_ID, DataStoreRole.Primary)).thenReturn(null);

        Assert.assertThrows(CloudRuntimeException.class, () -> driver.copySnapshotToNewVolume(SNAPSHOT_ID, newVolume, storagePool, client));
        verify(client, never()).mapVolumeToSdc(anyString(), anyString());
    }

    @Test
    public void primaryDataStoreTOForManagedVolumeTargetsTheVolume() {
        when(storagePool.getHostAddress()).thenReturn("192.0.2.10");
        when(storagePool.getPort()).thenReturn(443);
        driver.getPrimaryDataStoreTOForManagedVolume(storagePool, SNAPSHOT_PATH, "snap", 16L << 30);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> details = ArgumentCaptor.forClass(Map.class);
        verify(dataStore).setDetails(details.capture());
        Assert.assertEquals(SNAPSHOT_PATH, details.getValue().get(PrimaryDataStore.MANAGED_STORE_TARGET));
        Assert.assertEquals("true", details.getValue().get(PrimaryDataStore.MANAGED));
        Assert.assertEquals("192.0.2.10", details.getValue().get(PrimaryDataStore.STORAGE_HOST));
        Assert.assertEquals(String.valueOf(16L << 30), details.getValue().get(PrimaryDataStore.VOLUME_SIZE));
    }

    @Test
    public void createVolumeFromSnapshotRefusesEncryptionBeforeCreatingOnPowerFlex() {
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(NEW_VOLUME_ID);
        VolumeDetailVO detail = mock(VolumeDetailVO.class);
        when(detail.getValue()).thenReturn(String.valueOf(SNAPSHOT_ID));
        when(volumeDetailsDao.findDetail(NEW_VOLUME_ID, ScaleIOPrimaryDataStoreDriver.CLONE_OF_SNAPSHOT)).thenReturn(detail);
        when(storagePool.getHostAddress()).thenReturn("192.0.2.10");
        when(storagePoolDao.findById(POOL_ID)).thenReturn(storagePool);
        doReturn(true).when(driver).anyVolumeRequiresEncryption(volumeInfo);

        Assert.assertThrows(CloudRuntimeException.class, () -> driver.createVolume(volumeInfo, POOL_ID, false, null));
        verify(client, never()).createVolume(anyString(), anyString(), anyInt(), any());
        verify(driver, never()).copySnapshotToNewVolume(anyLong(), any(), any(), any());
    }

    private VolumeInfo stubCreateVolume(Long cloneOfSnapshot) {
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(NEW_VOLUME_ID);
        when(volumeInfo.getSize()).thenReturn(10L << 30);
        if (cloneOfSnapshot != null) {
            VolumeDetailVO detail = mock(VolumeDetailVO.class);
            when(detail.getValue()).thenReturn(String.valueOf(cloneOfSnapshot));
            when(volumeDetailsDao.findDetail(NEW_VOLUME_ID, ScaleIOPrimaryDataStoreDriver.CLONE_OF_SNAPSHOT)).thenReturn(detail);
        }
        when(storagePool.getHostAddress()).thenReturn("192.0.2.10");
        when(storagePool.getUuid()).thenReturn("a1b2c3d4-0000-0000-0000-000000000000");
        when(storagePool.getPath()).thenReturn("pool-id-1");
        when(storagePool.getCapacityBytes()).thenReturn(1L << 40);
        when(storagePoolDao.findById(POOL_ID)).thenReturn(storagePool);
        doReturn(client).when(driver).getScaleIOClient(storagePool);
        doReturn(false).when(driver).anyVolumeRequiresEncryption(volumeInfo);
        Volume created = mock(Volume.class);
        when(created.getId()).thenReturn("e44cfc4c00000003");
        when(created.getSizeInKb()).thenReturn(16L * 1024 * 1024);
        when(client.createVolume(anyString(), eq("pool-id-1"), eq(10), any())).thenReturn(created);
        when(volumeDao.findById(NEW_VOLUME_ID)).thenReturn(newVolume);
        return volumeInfo;
    }

    @Test
    public void createVolumeFromSnapshotCopiesTheSnapshotIntoTheNewVolume() {
        VolumeInfo volumeInfo = stubCreateVolume(SNAPSHOT_ID);
        doNothing().when(driver).copySnapshotToNewVolume(anyLong(), any(), any(), any());
        try (MockedStatic<VolumeObject> volumeObjectMock = mockStatic(VolumeObject.class)) {
            VolumeObject createdObject = mock(VolumeObject.class);
            volumeObjectMock.when(() -> VolumeObject.getVolumeObject(any(), any())).thenReturn(createdObject);

            driver.createVolume(volumeInfo, POOL_ID, false, null);

            InOrder order = inOrder(client, createdObject, driver);
            order.verify(client).createVolume(anyString(), eq("pool-id-1"), eq(10), any());
            order.verify(createdObject).update();
            order.verify(driver).copySnapshotToNewVolume(SNAPSHOT_ID, newVolume, storagePool, client);
        }
    }

    @Test
    public void createVolumeWithoutSnapshotDoesNotCopy() {
        VolumeInfo volumeInfo = stubCreateVolume(null);
        try (MockedStatic<VolumeObject> volumeObjectMock = mockStatic(VolumeObject.class)) {
            volumeObjectMock.when(() -> VolumeObject.getVolumeObject(any(), any())).thenReturn(mock(VolumeObject.class));

            driver.createVolume(volumeInfo, POOL_ID, false, null);

            verify(client).createVolume(anyString(), eq("pool-id-1"), eq(10), any());
            verify(driver, never()).copySnapshotToNewVolume(anyLong(), any(), any(), any());
        }
    }
}
