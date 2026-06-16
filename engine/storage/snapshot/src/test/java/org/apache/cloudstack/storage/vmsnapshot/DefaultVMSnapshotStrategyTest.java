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
package org.apache.cloudstack.storage.vmsnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;

@RunWith(MockitoJUnitRunner.class)
public class DefaultVMSnapshotStrategyTest {
    @Mock
    VolumeDao volumeDao;
    @Mock
    PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    UserVmDao userVmDao;

    @Spy
    @InjectMocks
    private final DefaultVMSnapshotStrategy defaultVMSnapshotStrategy = new DefaultVMSnapshotStrategy();

    protected List<VolumeVO> persistedVolumes = new ArrayList<>();


    private void setupVolumeDaoPersistMock() {
        persistedVolumes.clear();
        Mockito.when(volumeDao.persist(Mockito.any())).thenAnswer((Answer<VolumeVO>) invocation -> {
            VolumeVO volume = (VolumeVO)invocation.getArguments()[0];
            persistedVolumes.add(volume);
            return volume;
        });
    }

    @Test
    public void testUpdateVolumePath() {
        setupVolumeDaoPersistMock();
        VolumeObjectTO vol1 = Mockito.mock(VolumeObjectTO.class);
        Mockito.when(vol1.getDataStoreUuid()).thenReturn(null);
        Mockito.when(vol1.getPath()).thenReturn(null);
        Mockito.when(vol1.getChainInfo()).thenReturn(null);
        VolumeObjectTO vol2 = Mockito.mock(VolumeObjectTO.class);
        Long volumeId = 1L;
        String newDSUuid = UUID.randomUUID().toString();
        String oldVolPath = "old";
        String newVolPath = "new";
        String oldVolChain = "old-chain";
        String newVolChain = "new-chain";
        Long vmSnapshotChainSize = 1000L;
        Long oldPoolId = 1L;
        Long newPoolId = 2L;
        Mockito.when(vol2.getDataStoreUuid()).thenReturn(newDSUuid);
        Mockito.when(vol2.getPath()).thenReturn(newVolPath);
        Mockito.when(vol2.getChainInfo()).thenReturn(newVolChain);
        Mockito.when(vol2.getSize()).thenReturn(vmSnapshotChainSize);
        Mockito.when(vol2.getId()).thenReturn(volumeId);
        VolumeVO volumeVO = new VolumeVO("name", 0L, 0L, 0L, 0L, 0L, "folder", "path", Storage.ProvisioningType.THIN, 0L, Volume.Type.ROOT);
        volumeVO.setPoolId(oldPoolId);
        volumeVO.setChainInfo(oldVolChain);
        volumeVO.setPath(oldVolPath);
        Mockito.when(volumeDao.findById(volumeId)).thenReturn(volumeVO);
        StoragePoolVO storagePoolVO = Mockito.mock(StoragePoolVO.class);
        Mockito.when(storagePoolVO.getId()).thenReturn(newPoolId);
        Mockito.when(primaryDataStoreDao.findPoolByUUID(newDSUuid)).thenReturn(storagePoolVO);
        Mockito.when(volumeDao.findById(volumeId)).thenReturn(volumeVO);
        defaultVMSnapshotStrategy.updateVolumePath(List.of(vol1, vol2));
        Assert.assertEquals(1, persistedVolumes.size());
        VolumeVO persistedVolume = persistedVolumes.get(0);
        Assert.assertNotNull(persistedVolume);
        Assert.assertEquals(newPoolId, persistedVolume.getPoolId());
        Assert.assertEquals(newVolPath, persistedVolume.getPath());
        Assert.assertEquals(vmSnapshotChainSize, persistedVolume.getVmSnapshotChainSize());
        Assert.assertEquals(newVolChain, persistedVolume.getChainInfo());
    }

    @Test
    public void testCanHandleRunningVMOnClvmStorageCantHandle() {
        Long vmId = 1L;
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);

        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getState()).thenReturn(State.Running);
        Mockito.when(userVmDao.findById(vmId)).thenReturn(vm);

        VolumeVO volumeOnClvm = createVolume(vmId, 1L);
        List<VolumeVO> volumes = List.of(volumeOnClvm);
        Mockito.when(volumeDao.findByInstance(vmId)).thenReturn(volumes);

        StoragePoolVO clvmPool = createStoragePool("clvm-pool", Storage.StoragePoolType.CLVM);
        Mockito.when(primaryDataStoreDao.findById(1L)).thenReturn(clvmPool);

        StrategyPriority result = defaultVMSnapshotStrategy.canHandle(vmSnapshot);

        Assert.assertEquals("Should return CANT_HANDLE for running VM on CLVM storage",
                StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    public void testCanHandleStoppedVMOnClvmStorageCanHandle() {
        Long vmId = 1L;
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);

        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getState()).thenReturn(State.Stopped);
        Mockito.when(userVmDao.findById(vmId)).thenReturn(vm);

        StrategyPriority result = defaultVMSnapshotStrategy.canHandle(vmSnapshot);
        Assert.assertEquals("Should return DEFAULT for stopped VM on CLVM storage",
                StrategyPriority.DEFAULT, result);
    }

    @Test
    public void testCanHandleRunningVMOnNfsStorageCanHandle() {
        Long vmId = 1L;
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);

        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getState()).thenReturn(State.Running);
        Mockito.when(userVmDao.findById(vmId)).thenReturn(vm);

        VolumeVO volumeOnNfs = createVolume(vmId, 1L);
        List<VolumeVO> volumes = List.of(volumeOnNfs);
        Mockito.when(volumeDao.findByInstance(vmId)).thenReturn(volumes);

        StoragePoolVO nfsPool = createStoragePool("nfs-pool", Storage.StoragePoolType.NetworkFilesystem);
        Mockito.when(primaryDataStoreDao.findById(1L)).thenReturn(nfsPool);

        StrategyPriority result = defaultVMSnapshotStrategy.canHandle(vmSnapshot);

        Assert.assertEquals("Should return DEFAULT for running VM on NFS storage",
                StrategyPriority.DEFAULT, result);
    }

    @Test
    public void testCanHandleRunningVMWithMixedStorageClvmAndNfsCantHandle() {
        // Arrange - VM has volumes on both CLVM and NFS
        Long vmId = 1L;
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);

        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getState()).thenReturn(State.Running);
        Mockito.when(userVmDao.findById(vmId)).thenReturn(vm);

        VolumeVO volumeOnClvm = createVolume(vmId, 1L);
        VolumeVO volumeOnNfs = createVolume(vmId, 2L);
        List<VolumeVO> volumes = List.of(volumeOnClvm, volumeOnNfs);
        Mockito.when(volumeDao.findByInstance(vmId)).thenReturn(volumes);

        StoragePoolVO clvmPool = createStoragePool("clvm-pool", Storage.StoragePoolType.CLVM);
        StoragePoolVO nfsPool = createStoragePool("nfs-pool", Storage.StoragePoolType.NetworkFilesystem);
        Mockito.when(primaryDataStoreDao.findById(1L)).thenReturn(clvmPool);

        StrategyPriority result = defaultVMSnapshotStrategy.canHandle(vmSnapshot);

        Assert.assertEquals("Should return CANT_HANDLE if any volume is on CLVM storage for running VM",
                StrategyPriority.CANT_HANDLE, result);
    }

    private VolumeVO createVolume(Long vmId, Long poolId) {
        VolumeVO volume = new VolumeVO("volume", 0L, 0L, 0L, 0L, 0L,
                "folder", "path", Storage.ProvisioningType.THIN, 0L, Volume.Type.ROOT);
        volume.setInstanceId(vmId);
        volume.setPoolId(poolId);
        return volume;
    }

    private StoragePoolVO createStoragePool(String name, Storage.StoragePoolType poolType) {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getName()).thenReturn(name);
        Mockito.when(pool.getPoolType()).thenReturn(poolType);
        return pool;
    }
}
