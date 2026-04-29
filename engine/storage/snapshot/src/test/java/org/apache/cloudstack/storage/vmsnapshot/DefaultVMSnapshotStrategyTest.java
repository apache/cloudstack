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

@RunWith(MockitoJUnitRunner.class)
public class DefaultVMSnapshotStrategyTest {
    @Mock
    VolumeDao volumeDao;
    @Mock
    PrimaryDataStoreDao primaryDataStoreDao;

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
        VolumeVO volumeVO = new VolumeVO("name", 0l, 0l, 0l, 0l, 0l, "folder", "path", Storage.ProvisioningType.THIN, 0l, Volume.Type.ROOT);
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
}
