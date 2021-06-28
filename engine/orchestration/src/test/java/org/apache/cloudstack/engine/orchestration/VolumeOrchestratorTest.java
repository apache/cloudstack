/*
 * Copyright 2021 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cloudstack.engine.orchestration;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.exception.CloudRuntimeException;
import java.util.Arrays;
import java.util.List;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VolumeOrchestratorTest {

    VolumeOrchestrator volumeOrchestratorSpy = Mockito.spy(VolumeOrchestrator.class);

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

    List<DataStoreRole> dataStoreRoles = Arrays.asList(DataStoreRole.values());

    @Before
    public void init() {
        volumeOrchestratorSpy._snapshotSrv = snapshotServiceMock;
        volumeOrchestratorSpy._snapshotDataStoreDao = snapshotDataStoreDaoMock;
        volumeOrchestratorSpy.snapshotFactory = snapshotDataFactoryMock;
        volumeOrchestratorSpy._storageStrategyFactory = storageStrategyFactoryMock;
    }

    @Test
    public void validateExpungeTemporarySnapshotNotAKvmSnapshotOnPrimaryStorageDoNothing() {
        volumeOrchestratorSpy.expungeTemporarySnapshot(false, snapshotInfoMock);
        Mockito.verifyNoInteractions(snapshotServiceMock, snapshotDataStoreDaoMock);
    }

    @Test
    public void validateExpungeTemporarySnapshotKvmSnapshotOnPrimaryStorageExpungesSnapshot() {
        Mockito.doReturn(true).when(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Mockito.doReturn(true).when(snapshotDataStoreDaoMock).expungeReferenceBySnapshotIdAndDataStoreRole(Mockito.anyLong(), Mockito.any());

        volumeOrchestratorSpy.expungeTemporarySnapshot(true, snapshotInfoMock);

        Mockito.verify(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Mockito.verify(snapshotDataStoreDaoMock).expungeReferenceBySnapshotIdAndDataStoreRole(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateIsKvmSnapshotOnlyInPrimaryStorageBackupToSecondaryTrue() {
        List<Hypervisor.HypervisorType> hypervisorTypes = Arrays.asList(Hypervisor.HypervisorType.values());
        volumeOrchestratorSpy.backupSnapshotAfterTakingSnapshot = true;

        hypervisorTypes.forEach(type -> {
            Mockito.doReturn(type).when(snapshotInfoMock).getHypervisorType();
            dataStoreRoles.forEach(role -> {
                Assert.assertFalse(volumeOrchestratorSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
            });
        });
    }

    @Test
    public void validateIsKvmSnapshotOnlyInPrimaryStorageBackupToSecondaryFalse() {
        List<Hypervisor.HypervisorType> hypervisorTypes = Arrays.asList(Hypervisor.HypervisorType.values());
        volumeOrchestratorSpy.backupSnapshotAfterTakingSnapshot = false;

        hypervisorTypes.forEach(type -> {
            Mockito.doReturn(type).when(snapshotInfoMock).getHypervisorType();
            dataStoreRoles.forEach(role -> {
                if (type == Hypervisor.HypervisorType.KVM && role == DataStoreRole.Primary) {
                    Assert.assertTrue(volumeOrchestratorSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
                } else {
                    Assert.assertFalse(volumeOrchestratorSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
                }
            });
        });
    }

    @Test
    public void validateGetSnapshotInfoByIdAndRoleSnapInfoFoundReturnIt() {
        Mockito.doReturn(snapshotInfoMock).when(snapshotDataFactoryMock).getSnapshot(Mockito.anyLong(), Mockito.any(DataStoreRole.class));

        dataStoreRoles.forEach(role -> {
            SnapshotInfo result = volumeOrchestratorSpy.getSnapshotInfoByIdAndRole(0, role);
            Assert.assertEquals(snapshotInfoMock, result);
        });
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateGetSnapshotInfoByIdAndRoleSnapInfoNotFoundThrowCloudRuntimeException() {
        Mockito.doReturn(null).when(snapshotDataFactoryMock).getSnapshot(Mockito.anyLong(), Mockito.any(DataStoreRole.class));

        dataStoreRoles.forEach(role -> {
            volumeOrchestratorSpy.getSnapshotInfoByIdAndRole(0, role);
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageFalse() {
        dataStoreRoles.forEach(role -> {
            if (role == DataStoreRole.Image) {
                Assert.assertTrue(volumeOrchestratorSpy.isSnapshotBackupable(null, role, false));
            } else {
                Assert.assertFalse(volumeOrchestratorSpy.isSnapshotBackupable(null, role, false));
            }
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNotNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageFalse() {
        dataStoreRoles.forEach(role -> {
            Assert.assertFalse(volumeOrchestratorSpy.isSnapshotBackupable(snapshotInfoMock, role, false));
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageTrue() {
        dataStoreRoles.forEach(role -> {
            Assert.assertTrue(volumeOrchestratorSpy.isSnapshotBackupable(null, role, true));
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNotNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageTrue() {
        dataStoreRoles.forEach(role -> {
            Assert.assertTrue(volumeOrchestratorSpy.isSnapshotBackupable(snapshotInfoMock, role, true));
        });
    }

    @Test
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsSnapshotIsNotBackupable(){
        Mockito.doReturn(false).when(volumeOrchestratorSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        SnapshotInfo result = volumeOrchestratorSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);
        Assert.assertEquals(snapshotInfoMock, result);
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsGetSnapshotThrowsCloudRuntimeException(){
        Mockito.doReturn(true).when(volumeOrchestratorSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        Mockito.doThrow(CloudRuntimeException.class).when(volumeOrchestratorSpy).getSnapshotInfoByIdAndRole(Mockito.anyLong(), Mockito.any());

        volumeOrchestratorSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);
    }

    @Test
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsReturnSnapshotInfo(){
        Mockito.doReturn(true).when(volumeOrchestratorSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        Mockito.doReturn(snapshotInfoMock, snapshotInfoMock2).when(volumeOrchestratorSpy).getSnapshotInfoByIdAndRole(Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(snapshotStrategyMock).when(storageStrategyFactoryMock).getSnapshotStrategy(Mockito.any(), Mockito.any());
        Mockito.doReturn(null).when(snapshotStrategyMock).backupSnapshot(Mockito.any());

        SnapshotInfo result = volumeOrchestratorSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);

        Assert.assertEquals(snapshotInfoMock2, result);
    }
}
