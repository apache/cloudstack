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
package org.apache.cloudstack.storage.volume;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotApiService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link VolumeServiceImpl#deletePrimaryOnlySnapshotsBeforeRbdVolumeDelete(VolumeVO)}, invoked from
 * {@link VolumeServiceImpl#expungeVolumeAsync} before a Primary role volume is deleted.
 */
@RunWith(MockitoJUnitRunner.class)
public class VolumeServiceImplRbdSnapshotCleanupTest {

    private VolumeServiceImpl volumeServiceImplSpy;

    @Mock
    private VolumeDao volumeDaoMock;

    @Mock
    private PrimaryDataStoreDao primaryDataStoreDaoMock;

    @Mock
    private SnapshotDataStoreDao snapshotDataStoreDaoMock;

    @Mock
    private SnapshotApiService snapshotApiServiceMock;

    @Mock
    private VolumeVO volumeVoMock;

    private static final long VOLUME_ID = 83L;

    @Before
    public void setup() {
        volumeServiceImplSpy = Mockito.spy(new VolumeServiceImpl());
        volumeServiceImplSpy.volDao = volumeDaoMock;
        volumeServiceImplSpy.storagePoolDao = primaryDataStoreDaoMock;
        volumeServiceImplSpy._snapshotStoreDao = snapshotDataStoreDaoMock;
        ReflectionTestUtils.setField(volumeServiceImplSpy, "snapshotApiService", snapshotApiServiceMock);

        Mockito.doReturn(VOLUME_ID).when(volumeVoMock).getId();
    }

    private void invoke() {
        ReflectionTestUtils.invokeMethod(volumeServiceImplSpy, "deletePrimaryOnlySnapshotsBeforeRbdVolumeDelete", volumeVoMock);
    }

    @Test
    public void skipsWhenPoolIdIsNull() {
        Mockito.doReturn(HypervisorType.KVM).when(volumeDaoMock).getHypervisorType(VOLUME_ID);
        Mockito.doReturn(null).when(volumeVoMock).getPoolId();

        invoke();

        Mockito.verify(primaryDataStoreDaoMock, Mockito.never()).findById(Mockito.anyLong());
        Mockito.verify(snapshotDataStoreDaoMock, Mockito.never()).listAllByVolumeAndDataStore(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void skipsWhenHypervisorIsNotKvm() {
        Mockito.doReturn(HypervisorType.VMware).when(volumeDaoMock).getHypervisorType(VOLUME_ID);

        invoke();

        Mockito.verify(primaryDataStoreDaoMock, Mockito.never()).findById(Mockito.anyLong());
        Mockito.verify(snapshotDataStoreDaoMock, Mockito.never()).listAllByVolumeAndDataStore(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void skipsWhenPoolIsNotRbd() {
        long poolId = 5L;
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(HypervisorType.KVM).when(volumeDaoMock).getHypervisorType(VOLUME_ID);
        Mockito.doReturn(poolId).when(volumeVoMock).getPoolId();
        Mockito.doReturn(pool).when(primaryDataStoreDaoMock).findById(poolId);
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(pool).getPoolType();

        invoke();

        Mockito.verify(snapshotDataStoreDaoMock, Mockito.never()).listAllByVolumeAndDataStore(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void skipsWhenNoSnapshotsOnPrimary() {
        long poolId = 5L;
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(HypervisorType.KVM).when(volumeDaoMock).getHypervisorType(VOLUME_ID);
        Mockito.doReturn(poolId).when(volumeVoMock).getPoolId();
        Mockito.doReturn(pool).when(primaryDataStoreDaoMock).findById(poolId);
        Mockito.doReturn(Storage.StoragePoolType.RBD).when(pool).getPoolType();
        Mockito.doReturn(Collections.emptyList()).when(snapshotDataStoreDaoMock).listAllByVolumeAndDataStore(VOLUME_ID, DataStoreRole.Primary);

        invoke();

        Mockito.verifyNoInteractions(snapshotApiServiceMock);
    }

    @Test
    public void deletesEachPrimaryOnlySnapshotBeforeVolumeIsDeleted() {
        long poolId = 5L;
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(HypervisorType.KVM).when(volumeDaoMock).getHypervisorType(VOLUME_ID);
        Mockito.doReturn(poolId).when(volumeVoMock).getPoolId();
        Mockito.doReturn(pool).when(primaryDataStoreDaoMock).findById(poolId);
        Mockito.doReturn(Storage.StoragePoolType.RBD).when(pool).getPoolType();

        SnapshotDataStoreVO snap1 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.doReturn(11L).when(snap1).getSnapshotId();
        SnapshotDataStoreVO snap2 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.doReturn(22L).when(snap2).getSnapshotId();
        List<SnapshotDataStoreVO> snapStoreVOs = Arrays.asList(snap1, snap2);
        Mockito.doReturn(snapStoreVOs).when(snapshotDataStoreDaoMock).listAllByVolumeAndDataStore(VOLUME_ID, DataStoreRole.Primary);

        // Neither snapshot has an Image role sibling, so both should be deleted through the normal workflow.
        Mockito.doReturn(Collections.singletonList(snap1)).when(snapshotDataStoreDaoMock).findBySnapshotId(11L);
        Mockito.doReturn(Collections.singletonList(snap2)).when(snapshotDataStoreDaoMock).findBySnapshotId(22L);

        invoke();

        Mockito.verify(snapshotApiServiceMock).deleteSnapshot(11L, null);
        Mockito.verify(snapshotApiServiceMock).deleteSnapshot(22L, null);
    }

    @Test
    public void keepsGoingWhenOneSnapshotDeleteFails() {
        long poolId = 5L;
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(HypervisorType.KVM).when(volumeDaoMock).getHypervisorType(VOLUME_ID);
        Mockito.doReturn(poolId).when(volumeVoMock).getPoolId();
        Mockito.doReturn(pool).when(primaryDataStoreDaoMock).findById(poolId);
        Mockito.doReturn(Storage.StoragePoolType.RBD).when(pool).getPoolType();

        SnapshotDataStoreVO snap1 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.doReturn(11L).when(snap1).getSnapshotId();
        SnapshotDataStoreVO snap2 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.doReturn(22L).when(snap2).getSnapshotId();
        Mockito.doReturn(Arrays.asList(snap1, snap2)).when(snapshotDataStoreDaoMock).listAllByVolumeAndDataStore(VOLUME_ID, DataStoreRole.Primary);
        Mockito.doReturn(Collections.singletonList(snap1)).when(snapshotDataStoreDaoMock).findBySnapshotId(11L);
        Mockito.doReturn(Collections.singletonList(snap2)).when(snapshotDataStoreDaoMock).findBySnapshotId(22L);
        Mockito.doThrow(new RuntimeException("boom")).when(snapshotApiServiceMock).deleteSnapshot(11L, null);

        invoke();

        Mockito.verify(snapshotApiServiceMock).deleteSnapshot(11L, null);
        Mockito.verify(snapshotApiServiceMock).deleteSnapshot(22L, null);
    }

    @Test
    public void removesOnlyTheReferenceWhenSnapshotHasAnImageCopy() {
        long poolId = 5L;
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(HypervisorType.KVM).when(volumeDaoMock).getHypervisorType(VOLUME_ID);
        Mockito.doReturn(poolId).when(volumeVoMock).getPoolId();
        Mockito.doReturn(pool).when(primaryDataStoreDaoMock).findById(poolId);
        Mockito.doReturn(Storage.StoragePoolType.RBD).when(pool).getPoolType();

        SnapshotDataStoreVO primaryRef = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.doReturn(11L).when(primaryRef).getSnapshotId();
        Mockito.doReturn(99L).when(primaryRef).getId();
        SnapshotDataStoreVO imageRef = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.doReturn(DataStoreRole.Image).when(imageRef).getRole();

        Mockito.doReturn(Collections.singletonList(primaryRef)).when(snapshotDataStoreDaoMock).listAllByVolumeAndDataStore(VOLUME_ID, DataStoreRole.Primary);
        Mockito.doReturn(Arrays.asList(primaryRef, imageRef)).when(snapshotDataStoreDaoMock).findBySnapshotId(11L);

        invoke();

        Mockito.verify(snapshotDataStoreDaoMock).remove(99L);
        Mockito.verify(snapshotApiServiceMock, Mockito.never()).deleteSnapshot(Mockito.anyLong(), Mockito.any());
    }
}
