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
public class SnapshotHelperTest {

    SnapshotHelper SnapshotHelperSpy = Mockito.spy(SnapshotHelper.class);

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
        SnapshotHelperSpy.snapshotService = snapshotServiceMock;
        SnapshotHelperSpy.snapshotDataStoreDao = snapshotDataStoreDaoMock;
        SnapshotHelperSpy.snapshotFactory = snapshotDataFactoryMock;
        SnapshotHelperSpy.storageStrategyFactory = storageStrategyFactoryMock;
    }

    @Test
    public void validateExpungeTemporarySnapshotNotAKvmSnapshotOnPrimaryStorageDoNothing() {
        SnapshotHelperSpy.expungeTemporarySnapshot(false, snapshotInfoMock);
        Mockito.verifyNoInteractions(snapshotServiceMock, snapshotDataStoreDaoMock);
    }

    @Test
    public void validateExpungeTemporarySnapshotKvmSnapshotOnPrimaryStorageExpungesSnapshot() {
        Mockito.doReturn(true).when(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Mockito.doReturn(true).when(snapshotDataStoreDaoMock).expungeReferenceBySnapshotIdAndDataStoreRole(Mockito.anyLong(), Mockito.any());

        SnapshotHelperSpy.expungeTemporarySnapshot(true, snapshotInfoMock);

        Mockito.verify(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Mockito.verify(snapshotDataStoreDaoMock).expungeReferenceBySnapshotIdAndDataStoreRole(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateIsKvmSnapshotOnlyInPrimaryStorageBackupToSecondaryTrue() {
        List<Hypervisor.HypervisorType> hypervisorTypes = Arrays.asList(Hypervisor.HypervisorType.values());
        SnapshotHelperSpy.backupSnapshotAfterTakingSnapshot = true;

        hypervisorTypes.forEach(type -> {
            Mockito.doReturn(type).when(snapshotInfoMock).getHypervisorType();
            dataStoreRoles.forEach(role -> {
                Assert.assertFalse(SnapshotHelperSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
            });
        });
    }

    @Test
    public void validateIsKvmSnapshotOnlyInPrimaryStorageBackupToSecondaryFalse() {
        List<Hypervisor.HypervisorType> hypervisorTypes = Arrays.asList(Hypervisor.HypervisorType.values());
        SnapshotHelperSpy.backupSnapshotAfterTakingSnapshot = false;

        hypervisorTypes.forEach(type -> {
            Mockito.doReturn(type).when(snapshotInfoMock).getHypervisorType();
            dataStoreRoles.forEach(role -> {
                if (type == Hypervisor.HypervisorType.KVM && role == DataStoreRole.Primary) {
                    Assert.assertTrue(SnapshotHelperSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
                } else {
                    Assert.assertFalse(SnapshotHelperSpy.isKvmSnapshotOnlyInPrimaryStorage(snapshotInfoMock, role));
                }
            });
        });
    }

    @Test
    public void validateGetSnapshotInfoByIdAndRoleSnapInfoFoundReturnIt() {
        Mockito.doReturn(snapshotInfoMock).when(snapshotDataFactoryMock).getSnapshot(Mockito.anyLong(), Mockito.any(DataStoreRole.class));

        dataStoreRoles.forEach(role -> {
            SnapshotInfo result = SnapshotHelperSpy.getSnapshotInfoByIdAndRole(0, role);
            Assert.assertEquals(snapshotInfoMock, result);
        });
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateGetSnapshotInfoByIdAndRoleSnapInfoNotFoundThrowCloudRuntimeException() {
        Mockito.doReturn(null).when(snapshotDataFactoryMock).getSnapshot(Mockito.anyLong(), Mockito.any(DataStoreRole.class));

        dataStoreRoles.forEach(role -> {
            SnapshotHelperSpy.getSnapshotInfoByIdAndRole(0, role);
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageFalse() {
        dataStoreRoles.forEach(role -> {
            if (role == DataStoreRole.Image) {
                Assert.assertTrue(SnapshotHelperSpy.isSnapshotBackupable(null, role, false));
            } else {
                Assert.assertFalse(SnapshotHelperSpy.isSnapshotBackupable(null, role, false));
            }
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNotNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageFalse() {
        dataStoreRoles.forEach(role -> {
            Assert.assertFalse(SnapshotHelperSpy.isSnapshotBackupable(snapshotInfoMock, role, false));
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageTrue() {
        dataStoreRoles.forEach(role -> {
            Assert.assertTrue(SnapshotHelperSpy.isSnapshotBackupable(null, role, true));
        });
    }

    @Test
    public void validateIsSnapshotBackupableSnapInfoNotNullAndAllRolesAndKvmSnapshotOnlyInPrimaryStorageTrue() {
        dataStoreRoles.forEach(role -> {
            Assert.assertTrue(SnapshotHelperSpy.isSnapshotBackupable(snapshotInfoMock, role, true));
        });
    }

    @Test
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsSnapshotIsNotBackupable(){
        Mockito.doReturn(false).when(SnapshotHelperSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        SnapshotInfo result = SnapshotHelperSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);
        Assert.assertEquals(snapshotInfoMock, result);
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsGetSnapshotThrowsCloudRuntimeException(){
        Mockito.doReturn(true).when(SnapshotHelperSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        Mockito.doThrow(CloudRuntimeException.class).when(SnapshotHelperSpy).getSnapshotInfoByIdAndRole(Mockito.anyLong(), Mockito.any());

        SnapshotHelperSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);
    }

    @Test
    public void validateBackupSnapshotToSecondaryStorageIfNotExistsReturnSnapshotInfo(){
        Mockito.doReturn(true).when(SnapshotHelperSpy).isSnapshotBackupable(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        Mockito.doReturn(snapshotInfoMock, snapshotInfoMock2).when(SnapshotHelperSpy).getSnapshotInfoByIdAndRole(Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(snapshotStrategyMock).when(storageStrategyFactoryMock).getSnapshotStrategy(Mockito.any(), Mockito.any());
        Mockito.doReturn(null).when(snapshotStrategyMock).backupSnapshot(Mockito.any());

        SnapshotInfo result = SnapshotHelperSpy.backupSnapshotToSecondaryStorageIfNotExists(snapshotInfoMock, DataStoreRole.Image, snapshotInfoMock, true);

        Assert.assertEquals(snapshotInfoMock2, result);
    }
}
