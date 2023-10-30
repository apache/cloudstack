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

package org.apache.cloudstack.snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotHelperTest {

    SnapshotHelper snapshotHelperSpy = Mockito.spy(SnapshotHelper.class);

    @Mock
    SnapshotService snapshotServiceMock;

    @Mock
    SnapshotDataStoreDao snapshotDataStoreDaoMock;

    @Mock
    SnapshotInfo snapshotInfoMock;

    @Mock
    SnapshotInfo snapshotInfoMock2;

    @Mock
    SnapshotDataFactory snapshotDataFactoryMock;

    @Mock
    StorageStrategyFactory storageStrategyFactoryMock;

    @Mock
    SnapshotStrategy snapshotStrategyMock;

    @Mock
    SnapshotDao snapshotDaoMock;

    @Mock
    DataStoreManager dataStoreManager;

    @Mock
    VolumeVO volumeVoMock;

    List<DataStoreRole> dataStoreRoles = Arrays.asList(DataStoreRole.values());

    @Before
    public void init() {
        snapshotHelperSpy.snapshotService = snapshotServiceMock;
        snapshotHelperSpy.snapshotDataStoreDao = snapshotDataStoreDaoMock;
        snapshotHelperSpy.snapshotFactory = snapshotDataFactoryMock;
        snapshotHelperSpy.storageStrategyFactory = storageStrategyFactoryMock;
        snapshotHelperSpy.snapshotDao = snapshotDaoMock;
        snapshotHelperSpy.dataStorageManager = dataStoreManager;
    }

    @Test
    public void validateExpungeTemporarySnapshotNotAKvmSnapshotOnPrimaryStorageDoNothing() {
        snapshotHelperSpy.expungeTemporarySnapshot(false, snapshotInfoMock);
        Mockito.verifyNoInteractions(snapshotServiceMock, snapshotDataStoreDaoMock);
    }

    @Test
    public void validateExpungeTemporarySnapshotKvmSnapshotOnPrimaryStorageExpungesSnapshot() {
        DataStore store = Mockito.mock(DataStore.class);
        Mockito.when(store.getRole()).thenReturn(DataStoreRole.Image);
        Mockito.when(store.getId()).thenReturn(1L);
        Mockito.when(snapshotInfoMock.getDataStore()).thenReturn(store);
        Mockito.doReturn(true).when(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Mockito.doReturn(true).when(snapshotDataStoreDaoMock).expungeReferenceBySnapshotIdAndDataStoreRole(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());

        snapshotHelperSpy.expungeTemporarySnapshot(true, snapshotInfoMock);

        Mockito.verify(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Mockito.verify(snapshotDataStoreDaoMock).expungeReferenceBySnapshotIdAndDataStoreRole(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateIsKvmSnapshotOnlyInPrimaryStorageBackupToSecondaryTrue() {
        List<Hypervisor.HypervisorType> hypervisorTypes = Arrays.asList(Hypervisor.HypervisorType.values());
        snapshotHelperSpy.backupSnapshotAfterTakingSnapshot = true;

        hypervisorTypes.forEach(type -> {
            Mockito.doReturn(type).when(snapshotInfoMock).getHypervisorType();
            dataStoreRoles.forEach(role -> {
                Assert.assertFalse(snapshotHelperSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
            });
        });
    }

    @Test
    public void validateIsKvmSnapshotOnlyInPrimaryStorageBackupToSecondaryFalse() {
        List<Hypervisor.HypervisorType> hypervisorTypes = Arrays.asList(Hypervisor.HypervisorType.values());
        snapshotHelperSpy.backupSnapshotAfterTakingSnapshot = false;

        hypervisorTypes.forEach(type -> {
            Mockito.doReturn(type).when(snapshotInfoMock).getHypervisorType();
            dataStoreRoles.forEach(role -> {
                if (type == Hypervisor.HypervisorType.KVM && role == DataStoreRole.Primary) {
                    Assert.assertTrue(snapshotHelperSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
                } else {
                    Assert.assertFalse(snapshotHelperSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
                }
            });
        });
    }

    @Test
    public void validateGetSnapshotInfoByIdAndRoleSnapInfoFoundReturnIt() {
        Mockito.doReturn(snapshotInfoMock).when(snapshotDataFactoryMock).getSnapshotOnPrimaryStore(Mockito.anyLong());
        Mockito.doReturn(snapshotInfoMock).when(snapshotDataFactoryMock).getSnapshotWithRoleAndZone(Mockito.anyLong(), Mockito.any(DataStoreRole.class), Mockito.anyLong());

        dataStoreRoles.forEach(role -> {
            SnapshotInfo result = snapshotHelperSpy.getSnapshotInfoByIdAndRole(0, role, 1L);
            Assert.assertEquals(snapshotInfoMock, result);
        });
    }

    @Test
    public void validateGetSnapshotInfoByIdAndRoleSnapInfoNotFoundThrowCloudRuntimeException() {
        Mockito.doReturn(null).when(snapshotDataFactoryMock).getSnapshotOnPrimaryStore(Mockito.anyLong());
        Mockito.doReturn(null).when(snapshotDataFactoryMock).getSnapshotWithRoleAndZone(Mockito.anyLong(), Mockito.any(DataStoreRole.class), Mockito.anyLong());

        dataStoreRoles.forEach(role -> {
            try {
                snapshotHelperSpy.getSnapshotInfoByIdAndRole(0, role, 1L);
                Assert.fail(String.format("Expected a CloudRuntimeException for datastore role: %s", role));
            } catch (CloudRuntimeException ignored) {}
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageFalse() {
        dataStoreRoles.forEach(role -> {
            if (role == DataStoreRole.Image) {
                Assert.assertTrue(snapshotHelperSpy.isSnapshotBackupable(null, role, false));
            } else {
                Assert.assertFalse(snapshotHelperSpy.isSnapshotBackupable(null, role, false));
            }
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNotNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageFalse() {
        dataStoreRoles.forEach(role -> {
            Assert.assertFalse(snapshotHelperSpy.isSnapshotBackupable(snapshotInfoMock, role, false));
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageTrue() {
        dataStoreRoles.forEach(role -> {
            Assert.assertTrue(snapshotHelperSpy.isSnapshotBackupable(null, role, true));
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNotNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageTrue() {
        dataStoreRoles.forEach(role -> {
            Assert.assertTrue(snapshotHelperSpy.isSnapshotBackupable(snapshotInfoMock, role, true));
        });
    }

    @Test
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsSnapshotIsNotBackupable() {
        Mockito.doReturn(false).when(snapshotHelperSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        SnapshotInfo result = snapshotHelperSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);
        Assert.assertEquals(snapshotInfoMock, result);
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsGetSnapshotThrowsCloudRuntimeException(){
        Mockito.doReturn(true).when(snapshotHelperSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        Mockito.when(snapshotDataFactoryMock.getSnapshotOnPrimaryStore(Mockito.anyLong())).thenReturn(null);

        snapshotHelperSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);
    }

    @Test
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsReturnSnapshotInfo(){
        Mockito.doReturn(true).when(snapshotHelperSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        Mockito.when(snapshotInfoMock.getDataStore()).thenReturn(Mockito.mock(DataStore.class));
        Mockito.when(snapshotDataFactoryMock.getSnapshotOnPrimaryStore(Mockito.anyLong())).thenReturn(snapshotInfoMock);
        Mockito.when(snapshotDataFactoryMock.getSnapshotWithRoleAndZone(Mockito.anyLong(), Mockito.any(DataStoreRole.class), Mockito.anyLong())).thenReturn(snapshotInfoMock2);
        Mockito.doReturn(snapshotStrategyMock).when(storageStrategyFactoryMock).getSnapshotStrategy(Mockito.any(), Mockito.any());
        Mockito.doReturn(null).when(snapshotStrategyMock).backupSnapshot(Mockito.any());

        SnapshotInfo result = snapshotHelperSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);

        Assert.assertEquals(snapshotInfoMock2, result);
    }

    private SnapshotDataStoreVO createSnapshotDataStoreVo(long snapshotId, DataStoreRole role) {
        SnapshotDataStoreVO snapshotDataStoreVo = new SnapshotDataStoreVO(0, snapshotId);
        snapshotDataStoreVo.setRole(role);
        return snapshotDataStoreVo;
    }

    @Test
    public void validateGetSnapshotIdsOnlyInPrimaryStorageAllSnapshotsInSecondaryStorageReturnEmpty() {
        List<SnapshotDataStoreVO> snapshotDataStoreVos = new ArrayList<>();
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(1, DataStoreRole.Primary));
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(1, DataStoreRole.Image));
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(2, DataStoreRole.Primary));
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(2, DataStoreRole.Image));
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(3, DataStoreRole.Primary));
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(3, DataStoreRole.Image));

        Mockito.doReturn(snapshotDataStoreVos).when(snapshotDataStoreDaoMock).listReadyByVolumeId(Mockito.anyLong());

        Set<Long> result = snapshotHelperSpy.getSnapshotIdsOnlyInPrimaryStorage(0);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void validateGetSnapshotIdsOnlyInPrimaryStorageEmptySnapshotsReturnEmpty() {
        List<SnapshotDataStoreVO> snapshotDataStoreVos = new ArrayList<>();

        Mockito.doReturn(snapshotDataStoreVos).when(snapshotDataStoreDaoMock).listReadyByVolumeId(Mockito.anyLong());

        Set<Long> result = snapshotHelperSpy.getSnapshotIdsOnlyInPrimaryStorage(0);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void validateGetSnapshotIdsOnlyInPrimaryStorageSomeSnapshotsNotInSecondaryStorageReturnSnapshotIds() {
        List<SnapshotDataStoreVO> snapshotDataStoreVos = new ArrayList<>();
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(1, DataStoreRole.Primary));
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(2, DataStoreRole.Primary));
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(2, DataStoreRole.Image));
        snapshotDataStoreVos.add(createSnapshotDataStoreVo(3, DataStoreRole.Primary));

        Mockito.doReturn(snapshotDataStoreVos).when(snapshotDataStoreDaoMock).listReadyByVolumeId(Mockito.anyLong());

        Set<Long> result = snapshotHelperSpy.getSnapshotIdsOnlyInPrimaryStorage(0);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(1l));
        Assert.assertFalse(result.contains(2l));
        Assert.assertTrue(result.contains(3l));
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateThrowCloudRuntimeExceptionOfSnapshotsOnlyInPrimaryStorage() {
        Mockito.doReturn(new ArrayList<>()).when(snapshotDaoMock).listByIds(Mockito.any());
        snapshotHelperSpy.throwCloudRuntimeExceptionOfSnapshotsOnlyInPrimaryStorage(null, new HashSet<>());
    }

    @Test
    public void checkKvmVolumeSnapshotsOnlyInPrimaryStorageTestNotKvmDoNothing() {
        Arrays.asList(HypervisorType.values()).forEach(hypervisorType -> {
            if (hypervisorType == HypervisorType.KVM) {
                return;
            }

            snapshotHelperSpy.checkKvmVolumeSnapshotsOnlyInPrimaryStorage(null, hypervisorType);
        });

        Mockito.verify(snapshotHelperSpy, Mockito.never()).getSnapshotIdsOnlyInPrimaryStorage(Mockito.anyLong());
    }

    @Test
    public void checkKvmVolumeSnapshotsOnlyInPrimaryStorageTestAllSnapshotsInSecondaryStorageDoNothing() {
        Mockito.doReturn(new HashSet<>()).when(snapshotHelperSpy).getSnapshotIdsOnlyInPrimaryStorage(Mockito.anyLong());

        snapshotHelperSpy.checkKvmVolumeSnapshotsOnlyInPrimaryStorage(volumeVoMock, HypervisorType.KVM);
    }

    @Test (expected = CloudRuntimeException.class)
    public void checkKvmVolumeSnapshotsOnlyInPrimaryStorageTestSomeSnapshotsNotInSecondaryStorageThrowsCloudRuntimeException() {
        Mockito.doReturn(new HashSet<>(Arrays.asList(1l, 2l))).when(snapshotHelperSpy).getSnapshotIdsOnlyInPrimaryStorage(Mockito.anyLong());

        snapshotHelperSpy.checkKvmVolumeSnapshotsOnlyInPrimaryStorage(volumeVoMock, HypervisorType.KVM);
    }
}
