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
package com.cloud.upgrade.dao;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDaoImpl;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Storage;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class Upgrade41700to41710Test {
    @Spy
    Upgrade41700to41710 upgrade41700to41710;

    @Test
    public void testGetStorageDao_FirstInvocationCreatesInstance() {
        PrimaryDataStoreDao dao1 = upgrade41700to41710.getStorageDao();
        Assert.assertNotNull(dao1);
        Assert.assertTrue(dao1 instanceof PrimaryDataStoreDaoImpl);
    }

    @Test
    public void testGetStorageDao_SubsequentInvocationReturnsSameInstance() {
        PrimaryDataStoreDao dao1 = upgrade41700to41710.getStorageDao();
        PrimaryDataStoreDao dao2 = upgrade41700to41710.getStorageDao();
        Assert.assertSame(dao1, dao2);
    }

    @Test
    public void testGetVolumeDao_FirstInvocationCreatesInstance() {
        VolumeDao dao1 = upgrade41700to41710.getVolumeDao();
        Assert.assertNotNull(dao1);
        Assert.assertTrue(dao1 instanceof VolumeDaoImpl);
    }

    @Test
    public void testGetVolumeDao_SubsequentInvocationReturnsSameInstance() {
        VolumeDao dao1 = upgrade41700to41710.getVolumeDao();
        VolumeDao dao2 = upgrade41700to41710.getVolumeDao();
        Assert.assertSame(dao1, dao2);
    }

    @Test
    public void testUpdateStorPoolStorageType_WithPoolIds() {
        PrimaryDataStoreDao storageDao = Mockito.mock(PrimaryDataStoreDao.class);
        Mockito.doReturn(storageDao).when(upgrade41700to41710).getStorageDao();
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        SearchBuilder<StoragePoolVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        Mockito.when(storageDao.createSearchBuilder()).thenReturn(searchBuilder);
        Mockito.when(searchBuilder.entity()).thenReturn(pool);
        Mockito.when(searchBuilder.create()).thenReturn(Mockito.mock(SearchCriteria.class));
        GenericSearchBuilder<StoragePoolVO, Long> gSb = Mockito.mock(GenericSearchBuilder.class);
        Mockito.doReturn(gSb).when(storageDao).createSearchBuilder(Mockito.any());
        Mockito.when(gSb.create()).thenReturn(Mockito.mock(SearchCriteria.class));
        Mockito.when(gSb.entity()).thenReturn(pool);
        Mockito.when(storageDao.createForUpdate()).thenReturn(pool);
        Mockito.doNothing().when(upgrade41700to41710).updateStorageTypeForStorPoolVolumes(Mockito.any());

        Mockito.when(storageDao.update(Mockito.any(StoragePoolVO.class), Mockito.any())).thenReturn(2);
        Mockito.when(storageDao.customSearch(Mockito.any(), Mockito.any())).thenReturn(List.of(1L, 2L));
        upgrade41700to41710.updateStorPoolStorageType();
        Mockito.verify(storageDao, Mockito.times(1)).update(Mockito.any(StoragePoolVO.class), Mockito.any());
        Mockito.verify(upgrade41700to41710, Mockito.times(1)).updateStorageTypeForStorPoolVolumes(Mockito.any());
    }

    @Test
    public void testUpdateStorageTypeForStorPoolVolumes_EmptyPoolIds() {
        VolumeDao volumeDao = Mockito.mock(VolumeDao.class);
        List<Long> storagePoolIds = Collections.emptyList();
        upgrade41700to41710.updateStorageTypeForStorPoolVolumes(storagePoolIds);
        Mockito.verify(volumeDao, Mockito.never()).update(Mockito.any(VolumeVO.class), Mockito.any());
    }

    @Test
    public void testUpdateStorageTypeForStorPoolVolumes_WithPoolIds() {
        VolumeDao volumeDao = Mockito.mock(VolumeDao.class);
        List<Long> storagePoolIds = List.of(1L, 2L, 3L);
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        SearchBuilder<VolumeVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        SearchCriteria<VolumeVO> searchCriteria = Mockito.mock(SearchCriteria.class);
        Mockito.when(volumeDao.createForUpdate()).thenReturn(volume);
        Mockito.when(volumeDao.createSearchBuilder()).thenReturn(searchBuilder);
        Mockito.when(searchBuilder.entity()).thenReturn(volume);
        Mockito.when(searchBuilder.create()).thenReturn(searchCriteria);
        Mockito.when(volumeDao.update(Mockito.any(VolumeVO.class), Mockito.any())).thenReturn(3);
        Mockito.doReturn(volumeDao).when(upgrade41700to41710).getVolumeDao();
        upgrade41700to41710.updateStorageTypeForStorPoolVolumes(storagePoolIds);
        Mockito.verify(volumeDao).createForUpdate();
        Mockito.verify(volume).setPoolType(Storage.StoragePoolType.StorPool);
        Mockito.verify(volumeDao).update(Mockito.eq(volume), Mockito.eq(searchCriteria));
        Mockito.verify(searchCriteria).setParameters("poolId", storagePoolIds.toArray());
    }
}
