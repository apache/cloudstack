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

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;

@RunWith(MockitoJUnitRunner.class)
public class CephSnapshotStrategyTest {

    @Spy
    @InjectMocks
    private CephSnapshotStrategy cephSnapshotStrategy;
    @Mock
    private SnapshotDataStoreDao snapshotStoreDao;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private VolumeDao volumeDao;

    @Test
    public void canHandleTestNotReomvedAndSnapshotStoredOnRbd() {
        configureAndVerifyCanHandle(null, true);
    }

    @Test
    public void canHandleTestNotReomvedAndSnapshotNotStoredOnRbd() {
        configureAndVerifyCanHandle(null, false);
    }

    @Test
    public void canHandleTestReomvedAndSnapshotNotStoredOnRbd() {
        configureAndVerifyCanHandle(null, false);
    }

    @Test
    public void canHandleTestReomvedAndSnapshotStoredOnRbd() {
        configureAndVerifyCanHandle(null, true);
    }

    private void configureAndVerifyCanHandle(Date removed, boolean isSnapshotStoredOnRbdStoragePool) {
        Snapshot snapshot = Mockito.mock(Snapshot.class);
        SnapshotOperation[] snapshotOps = SnapshotOperation.values();

        Mockito.when(snapshot.getVolumeId()).thenReturn(0l);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getRemoved()).thenReturn(removed);
        Mockito.when(volumeDao.findByIdIncludingRemoved(Mockito.anyLong())).thenReturn(volumeVO);
        Mockito.doReturn(isSnapshotStoredOnRbdStoragePool).when(cephSnapshotStrategy).isSnapshotStoredOnRbdStoragePool(Mockito.any());

        for (int i = 0; i < snapshotOps.length - 1; i++) {
            StrategyPriority strategyPriority = cephSnapshotStrategy.canHandle(snapshot, snapshotOps[i]);
            if (snapshotOps[i] == SnapshotOperation.REVERT && isSnapshotStoredOnRbdStoragePool) {
                Assert.assertEquals(StrategyPriority.HIGHEST, strategyPriority);
            } else if (snapshotOps[i] == SnapshotOperation.DELETE && isSnapshotStoredOnRbdStoragePool) {
                Assert.assertEquals(StrategyPriority.HIGHEST, strategyPriority);
            } else {
                Assert.assertEquals(StrategyPriority.CANT_HANDLE, strategyPriority);
            }
        }
    }

    @Test
    public void revertSnapshotTest() {
        ImageFormat[] imageFormatValues = ImageFormat.values();

        for (int i = 0; i < imageFormatValues.length - 1; i++) {
            Mockito.reset(cephSnapshotStrategy);
            SnapshotInfo snapshotInfo = Mockito.mock(SnapshotInfo.class);
            VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
            Mockito.when(snapshotInfo.getBaseVolume()).thenReturn(volumeInfo);
            Mockito.when(volumeInfo.getFormat()).thenReturn(imageFormatValues[i]);
            Mockito.doNothing().when(cephSnapshotStrategy).executeRevertSnapshot(Mockito.any(), Mockito.any());

            boolean revertResult = cephSnapshotStrategy.revertSnapshot(snapshotInfo);

            if (imageFormatValues[i] == ImageFormat.RAW) {
                Assert.assertTrue(revertResult);
                Mockito.verify(cephSnapshotStrategy).executeRevertSnapshot(Mockito.any(), Mockito.any());
            } else {
                Assert.assertFalse(revertResult);
                Mockito.verify(cephSnapshotStrategy, Mockito.times(0)).executeRevertSnapshot(Mockito.any(), Mockito.any());
            }
        }

    }

}
