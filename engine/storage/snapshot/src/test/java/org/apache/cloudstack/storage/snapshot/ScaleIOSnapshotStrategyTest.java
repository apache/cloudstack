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
package org.apache.cloudstack.storage.snapshot;

import java.util.Date;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Snapshot;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOSnapshotStrategyTest {

    @Spy
    @InjectMocks
    private ScaleIOSnapshotStrategy scaleIOSnapshotStrategy;
    @Mock
    private SnapshotDataStoreDao snapshotStoreDao;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private VolumeDao volumeDao;

    private void verifyCantHandleAnyOperation(VolumeVO volumeVO) {
        Snapshot snapshot = Mockito.mock(Snapshot.class);
        Mockito.when(snapshot.getVolumeId()).thenReturn(1L);
        Mockito.when(volumeDao.findByIdIncludingRemoved(1L)).thenReturn(volumeVO);

        for (SnapshotOperation op : SnapshotOperation.values()) {
            Assert.assertEquals(StrategyPriority.CANT_HANDLE, scaleIOSnapshotStrategy.canHandle(snapshot, null, op));
        }
        Mockito.verifyNoInteractions(snapshotStoreDao, primaryDataStoreDao);
    }

    @Test
    public void canHandleTestVolumeExistsAndSnapshotStoredOnScaleIO() {
        Snapshot snapshot = Mockito.mock(Snapshot.class);
        Mockito.when(snapshot.getVolumeId()).thenReturn(1L);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getRemoved()).thenReturn(null);
        Mockito.when(volumeDao.findByIdIncludingRemoved(1L)).thenReturn(volumeVO);
        Mockito.doReturn(true).when(scaleIOSnapshotStrategy).isSnapshotStoredOnScaleIOStoragePoolAndOperationForSameZone(Mockito.any(), Mockito.any());

        for (SnapshotOperation op : SnapshotOperation.values()) {
            StrategyPriority expected = (op == SnapshotOperation.REVERT || op == SnapshotOperation.DELETE) ? StrategyPriority.HIGHEST : StrategyPriority.CANT_HANDLE;
            Assert.assertEquals(expected, scaleIOSnapshotStrategy.canHandle(snapshot, null, op));
        }
    }

    @Test
    public void canHandleTestVolumeRowMissing() {
        verifyCantHandleAnyOperation(null);
    }

    @Test
    public void canHandleTestVolumeRemoved() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getRemoved()).thenReturn(new Date());
        verifyCantHandleAnyOperation(volumeVO);
    }
}
