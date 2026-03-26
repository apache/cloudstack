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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.cloud.storage.ClvmLockManager;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
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
    private VolumeInfo volumeInfoMock;

    @Mock
    private VolumeVO volumeVOMock;

    @Mock
    ClvmLockManager clvmLockManager;

    private static final Long VOLUME_ID = 1L;
    private static final Long POOL_ID_1 = 100L;
    private static final Long POOL_ID_2 = 200L;
    private static final Long HOST_ID_1 = 10L;
    private static final Long HOST_ID_2 = 20L;
    private static final String POOL_PATH_VG1 = "/vg1";
    private static final String POOL_PATH_VG2 = "/vg2";

    @Before
    public void setup() {
        when(volumeInfoMock.getId()).thenReturn(VOLUME_ID);
        when(volumeInfoMock.getUuid()).thenReturn("test-volume-uuid");
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
}
