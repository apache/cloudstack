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
package org.apache.cloudstack.storage.volume;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.ClvmLockManager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;

/**
 * Tests for CLVM lock management methods in VolumeServiceImpl.
 */
@RunWith(MockitoJUnitRunner.class)
public class VolumeServiceImplClvmTest {

    @Spy
    @InjectMocks
    private VolumeServiceImpl volumeService;

    @Mock
    private VolumeDao volumeDao;

    @Mock
    private PrimaryDataStoreDao storagePoolDao;

    @Mock
    private HostDao _hostDao;

    @Mock
    private VMInstanceDao vmDao;

    @Mock
    private VolumeDataFactory volFactory;

    @Mock
    private VolumeInfo volumeInfoMock;

    @Mock
    private VolumeVO volumeVOMock;

    @Mock
    private StoragePoolVO storagePoolVOMock;

    @Mock
    private HostVO hostVOMock;

    @Mock
    private VMInstanceVO vmInstanceVOMock;

    @Mock
    private ClvmLockManager clvmLockManager;

    private static final Long VOLUME_ID = 1L;
    private static final Long POOL_ID_1 = 100L;
    private static final Long POOL_ID_2 = 200L;
    private static final Long HOST_ID_1 = 10L;
    private static final Long HOST_ID_2 = 20L;
    private static final String POOL_PATH_VG1 = "/vg1";

    @Before
    public void setup() {
        when(volumeInfoMock.getId()).thenReturn(VOLUME_ID);
        when(volumeInfoMock.getUuid()).thenReturn("test-volume-uuid");

        volumeService.storagePoolDao = storagePoolDao;
        volumeService._hostDao = _hostDao;
        volumeService.vmDao = vmDao;
        volumeService.volFactory = volFactory;
        volumeService._volumeDao = volumeDao;
        volumeService.clvmLockManager = clvmLockManager;
    }

    @Test
    public void testAreBothPoolsClvmType_BothCLVM() {
        assertTrue(volumeService.areBothPoolsClvmType(StoragePoolType.CLVM, StoragePoolType.CLVM));
    }

    @Test
    public void testAreBothPoolsClvmType_BothCLVM_NG() {
        assertTrue(volumeService.areBothPoolsClvmType(StoragePoolType.CLVM_NG, StoragePoolType.CLVM_NG));
    }

    @Test
    public void testAreBothPoolsClvmType_MixedCLVMAndCLVM_NG() {
        assertTrue(volumeService.areBothPoolsClvmType(StoragePoolType.CLVM, StoragePoolType.CLVM_NG));
        assertTrue(volumeService.areBothPoolsClvmType(StoragePoolType.CLVM_NG, StoragePoolType.CLVM));
    }

    @Test
    public void testAreBothPoolsClvmType_OneCLVMOneNFS() {
        assertFalse(volumeService.areBothPoolsClvmType(StoragePoolType.CLVM, StoragePoolType.NetworkFilesystem));
        assertFalse(volumeService.areBothPoolsClvmType(StoragePoolType.NetworkFilesystem, StoragePoolType.CLVM));
    }

    @Test
    public void testAreBothPoolsClvmType_OneCLVM_NGOneNFS() {
        assertFalse(volumeService.areBothPoolsClvmType(StoragePoolType.CLVM_NG, StoragePoolType.NetworkFilesystem));
        assertFalse(volumeService.areBothPoolsClvmType(StoragePoolType.NetworkFilesystem, StoragePoolType.CLVM_NG));
    }

    @Test
    public void testAreBothPoolsClvmType_BothNFS() {
        assertFalse(volumeService.areBothPoolsClvmType(StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem));
    }

    @Test
    public void testAreBothPoolsClvmType_NullVolumePoolType() {
        assertFalse(volumeService.areBothPoolsClvmType(null, StoragePoolType.CLVM));
    }

    @Test
    public void testAreBothPoolsClvmType_NullVmPoolType() {
        assertFalse(volumeService.areBothPoolsClvmType(StoragePoolType.CLVM, null));
    }

    @Test
    public void testAreBothPoolsClvmType_BothNull() {
        assertFalse(volumeService.areBothPoolsClvmType(null, null));
    }


    @Test
    public void testIsLockTransferRequired_NonCLVMPool() {
        assertFalse(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.NetworkFilesystem, StoragePoolType.CLVM,
                POOL_ID_1, POOL_ID_1, HOST_ID_1));
    }

    @Test
    public void testIsLockTransferRequired_DifferentPools() {
        assertFalse(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM, StoragePoolType.CLVM,
                POOL_ID_1, POOL_ID_2, HOST_ID_1));
    }

    @Test
    public void testIsLockTransferRequired_NullPoolIds() {
        assertFalse(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM, StoragePoolType.CLVM,
                null, POOL_ID_1, HOST_ID_1));

        assertFalse(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM, StoragePoolType.CLVM,
                POOL_ID_1, null, HOST_ID_1));
    }

    @Test
    public void testIsLockTransferRequired_DetachedVolumeReady() {
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVOMock);
        when(volumeVOMock.getState()).thenReturn(Volume.State.Ready);
        when(volumeVOMock.getInstanceId()).thenReturn(null); // Detached

        when(volumeService.findVolumeLockHost(volumeInfoMock)).thenReturn(null);

        assertTrue(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM, StoragePoolType.CLVM,
                POOL_ID_1, POOL_ID_1, HOST_ID_1));
    }

    @Test
    public void testIsLockTransferRequired_DetachedVolumeNotReady() {
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVOMock);
        when(volumeVOMock.getState()).thenReturn(Volume.State.Allocated);

        when(volumeService.findVolumeLockHost(volumeInfoMock)).thenReturn(null);

        assertFalse(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM, StoragePoolType.CLVM,
                POOL_ID_1, POOL_ID_1, HOST_ID_1));
    }

    @Test
    public void testIsLockTransferRequired_DifferentHosts() {
        when(volumeService.findVolumeLockHost(volumeInfoMock)).thenReturn(HOST_ID_1);

        assertTrue(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM, StoragePoolType.CLVM,
                POOL_ID_1, POOL_ID_1, HOST_ID_2));
    }

    @Test
    public void testIsLockTransferRequired_SameHost() {
        when(volumeService.findVolumeLockHost(volumeInfoMock)).thenReturn(HOST_ID_1);

        assertFalse(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM, StoragePoolType.CLVM,
                POOL_ID_1, POOL_ID_1, HOST_ID_1));
    }

    @Test
    public void testIsLockTransferRequired_NullVmHostId() {
        when(volumeService.findVolumeLockHost(volumeInfoMock)).thenReturn(HOST_ID_1);

        assertFalse(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM, StoragePoolType.CLVM,
                POOL_ID_1, POOL_ID_1, null));
    }

    @Test
    public void testIsLockTransferRequired_CLVM_NG_DifferentHosts() {
        when(volumeService.findVolumeLockHost(volumeInfoMock)).thenReturn(HOST_ID_1);

        assertTrue(volumeService.isLockTransferRequired(
                volumeInfoMock, StoragePoolType.CLVM_NG, StoragePoolType.CLVM_NG,
                POOL_ID_1, POOL_ID_1, HOST_ID_2));
    }

    @Test
    public void testIsLightweightMigrationNeeded_NonCLVMPools() {
        assertFalse(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem,
                POOL_PATH_VG1, POOL_PATH_VG1));
    }

    @Test
    public void testIsLightweightMigrationNeeded_OneCLVMOneNFS() {
        assertFalse(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.NetworkFilesystem,
                POOL_PATH_VG1, POOL_PATH_VG1));
    }

    @Test
    public void testIsLightweightMigrationNeeded_SameVG() {
        assertTrue(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                "/vg1", "/vg1"));
    }

    @Test
    public void testIsLightweightMigrationNeeded_SameVG_NoSlash() {
        assertTrue(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                "vg1", "vg1"));
    }

    @Test
    public void testIsLightweightMigrationNeeded_SameVG_MixedSlash() {
        assertTrue(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                "/vg1", "vg1"));

        assertTrue(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                "vg1", "/vg1"));
    }

    @Test
    public void testIsLightweightMigrationNeeded_DifferentVG() {
        assertFalse(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                "/vg1", "/vg2"));
    }

    @Test
    public void testIsLightweightMigrationNeeded_CLVM_NG_SameVG() {
        assertTrue(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM_NG, StoragePoolType.CLVM_NG,
                "/vg1", "/vg1"));
    }

    @Test
    public void testIsLightweightMigrationNeeded_CLVM_NG_DifferentVG() {
        assertFalse(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM_NG, StoragePoolType.CLVM_NG,
                "/vg1", "/vg2"));
    }

    @Test
    public void testIsLightweightMigrationNeeded_MixedCLVM_CLVM_NG_SameVG() {
        assertTrue(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM_NG,
                "/vg1", "/vg1"));

        assertTrue(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM_NG, StoragePoolType.CLVM,
                "/vg1", "/vg1"));
    }

    @Test
    public void testIsLightweightMigrationNeeded_NullVolumePath() {
        assertFalse(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                null, "/vg1"));
    }

    @Test
    public void testIsLightweightMigrationNeeded_NullVmPath() {
        assertFalse(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                "/vg1", null));
    }

    @Test
    public void testIsLightweightMigrationNeeded_BothPathsNull() {
        assertFalse(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                null, null));
    }

    @Test
    public void testIsLightweightMigrationNeeded_ComplexVGNames() {
        assertTrue(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                "/cloudstack-vg-01", "/cloudstack-vg-01"));

        assertFalse(volumeService.isLightweightMigrationNeeded(
                StoragePoolType.CLVM, StoragePoolType.CLVM,
                "/cloudstack-vg-01", "/cloudstack-vg-02"));
    }

    @Test
    public void testTransferVolumeLock_Success() {
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(volumeInfoMock.getId()).thenReturn(VOLUME_ID);
        when(volumeInfoMock.getPath()).thenReturn("/dev/vg1/volume-1");
        when(storagePoolDao.findById(POOL_ID_1)).thenReturn(storagePoolVOMock);
        when(storagePoolVOMock.getName()).thenReturn("test-pool");
        when(clvmLockManager.transferClvmVolumeLock(
                "test-volume-uuid", VOLUME_ID, "/dev/vg1/volume-1", storagePoolVOMock, HOST_ID_1, HOST_ID_2))
                .thenReturn(true);

        assertTrue(volumeService.transferVolumeLock(volumeInfoMock, HOST_ID_1, HOST_ID_2));
    }

    @Test
    public void testTransferVolumeLock_Failure() {
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(volumeInfoMock.getId()).thenReturn(VOLUME_ID);
        when(volumeInfoMock.getPath()).thenReturn("/dev/vg1/volume-1");
        when(storagePoolDao.findById(POOL_ID_1)).thenReturn(storagePoolVOMock);
        when(storagePoolVOMock.getName()).thenReturn("test-pool");
        when(clvmLockManager.transferClvmVolumeLock(
                "test-volume-uuid", VOLUME_ID, "/dev/vg1/volume-1", storagePoolVOMock, HOST_ID_1, HOST_ID_2))
                .thenReturn(false);

        assertFalse(volumeService.transferVolumeLock(volumeInfoMock, HOST_ID_1, HOST_ID_2));
    }

    @Test
    public void testTransferVolumeLock_PoolNotFound() {
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(storagePoolDao.findById(POOL_ID_1)).thenReturn(null);

        assertFalse(volumeService.transferVolumeLock(volumeInfoMock, HOST_ID_1, HOST_ID_2));
    }

    @Test
    public void testFindVolumeLockHost_NullVolume() {
        Long result = volumeService.findVolumeLockHost(null);
        assertNull(result);
    }

    @Test
    public void testFindVolumeLockHost_ExplicitLockFound() {
        when(clvmLockManager.getClvmLockHostId(VOLUME_ID, "test-volume-uuid"))
                .thenReturn(HOST_ID_1);

        Long result = volumeService.findVolumeLockHost(volumeInfoMock);
        assertEquals(HOST_ID_1, result);
    }

    @Test
    public void testFindVolumeLockHost_FromAttachedVM() {
        when(clvmLockManager.getClvmLockHostId(VOLUME_ID, "test-volume-uuid"))
                .thenReturn(null);
        when(volumeInfoMock.getInstanceId()).thenReturn(100L);
        when(vmDao.findById(100L)).thenReturn(vmInstanceVOMock);
        when(vmInstanceVOMock.getUuid()).thenReturn("vm-uuid");
        when(vmInstanceVOMock.getHostId()).thenReturn(HOST_ID_1);

        Long result = volumeService.findVolumeLockHost(volumeInfoMock);
        assertEquals(HOST_ID_1, result);
    }

    @Test
    public void testFindVolumeLockHost_FallbackToClusterHost() {
        when(clvmLockManager.getClvmLockHostId(VOLUME_ID, "test-volume-uuid"))
                .thenReturn(null);
        when(volumeInfoMock.getInstanceId()).thenReturn(null);
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(storagePoolDao.findById(POOL_ID_1)).thenReturn(storagePoolVOMock);
        when(storagePoolVOMock.getClusterId()).thenReturn(10L);
        when(hostVOMock.getId()).thenReturn(HOST_ID_1);
        when(hostVOMock.getStatus()).thenReturn(com.cloud.host.Status.Up);
        when(_hostDao.findByClusterId(10L)).thenReturn(java.util.Collections.singletonList(hostVOMock));

        Long result = volumeService.findVolumeLockHost(volumeInfoMock);
        assertEquals(HOST_ID_1, result);
    }

    @Test
    public void testFindVolumeLockHost_NoHostFound() {
        when(clvmLockManager.getClvmLockHostId(VOLUME_ID, "test-volume-uuid"))
                .thenReturn(null);
        when(volumeInfoMock.getInstanceId()).thenReturn(null);
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(storagePoolDao.findById(POOL_ID_1)).thenReturn(storagePoolVOMock);
        when(storagePoolVOMock.getClusterId()).thenReturn(10L);
        when(_hostDao.findByClusterId(10L)).thenReturn(java.util.Collections.emptyList());

        Long result = volumeService.findVolumeLockHost(volumeInfoMock);
        assertNull(result);
    }

    @Test
    public void testPerformLockMigration_Success() {
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(volumeInfoMock.getId()).thenReturn(VOLUME_ID);
        when(volumeInfoMock.getPath()).thenReturn("/dev/vg1/volume-1");
        when(clvmLockManager.getClvmLockHostId(VOLUME_ID, "test-volume-uuid")).thenReturn(HOST_ID_1);
        when(storagePoolDao.findById(POOL_ID_1)).thenReturn(storagePoolVOMock);
        when(storagePoolVOMock.getName()).thenReturn("test-pool");
        when(clvmLockManager.transferClvmVolumeLock(
                "test-volume-uuid", VOLUME_ID, "/dev/vg1/volume-1", storagePoolVOMock, HOST_ID_1, HOST_ID_2))
                .thenReturn(true);
        when(volFactory.getVolume(VOLUME_ID)).thenReturn(volumeInfoMock);

        VolumeInfo result = volumeService.performLockMigration(volumeInfoMock, HOST_ID_2);
        assertNotNull(result);
    }

    @Test
    public void testPerformLockMigration_SameHost() {
        when(clvmLockManager.getClvmLockHostId(VOLUME_ID, "test-volume-uuid")).thenReturn(HOST_ID_1);

        VolumeInfo result = volumeService.performLockMigration(volumeInfoMock, HOST_ID_1);
        assertEquals(volumeInfoMock, result);
    }

    @Test
    public void testPerformLockMigration_SourceHostNull() {
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(volumeInfoMock.getId()).thenReturn(VOLUME_ID);
        when(clvmLockManager.getClvmLockHostId(VOLUME_ID, "test-volume-uuid")).thenReturn(null);
        when(volumeInfoMock.getInstanceId()).thenReturn(null);
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(storagePoolDao.findById(POOL_ID_1)).thenReturn(storagePoolVOMock);
        when(storagePoolVOMock.getClusterId()).thenReturn(null);

        VolumeInfo result = volumeService.performLockMigration(volumeInfoMock, HOST_ID_2);
        assertNotNull(result);
    }

    @Test(expected = com.cloud.utils.exception.CloudRuntimeException.class)
    public void testPerformLockMigration_NullVolume() {
        volumeService.performLockMigration(null, HOST_ID_2);
    }

    @Test(expected = com.cloud.utils.exception.CloudRuntimeException.class)
    public void testPerformLockMigration_TransferFails() {
        when(volumeInfoMock.getPoolId()).thenReturn(POOL_ID_1);
        when(volumeInfoMock.getId()).thenReturn(VOLUME_ID);
        when(volumeInfoMock.getPath()).thenReturn("/dev/vg1/volume-1");
        when(clvmLockManager.getClvmLockHostId(VOLUME_ID, "test-volume-uuid")).thenReturn(HOST_ID_1);
        when(storagePoolDao.findById(POOL_ID_1)).thenReturn(storagePoolVOMock);
        when(storagePoolVOMock.getName()).thenReturn("test-pool");
        when(clvmLockManager.transferClvmVolumeLock(
                "test-volume-uuid", VOLUME_ID, "/dev/vg1/volume-1", storagePoolVOMock, HOST_ID_1, HOST_ID_2))
                .thenReturn(false);

        volumeService.performLockMigration(volumeInfoMock, HOST_ID_2);
    }
}
