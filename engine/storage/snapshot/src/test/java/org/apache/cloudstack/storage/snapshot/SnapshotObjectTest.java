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
package org.apache.cloudstack.storage.snapshot;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotObjectTest {

    private static final long PARENT_SNAPSHOT_ID = 2L;

    @Mock
    SnapshotDataStoreDao snapshotStoreDao;

    @Mock
    SnapshotDataFactory snapshotFactory;

    @Mock
    DataStore store;

    @Mock
    SnapshotVO snapshotVO;

    SnapshotObject snapshotObject;

    @Before
    public void setUp() {
        snapshotObject = new SnapshotObject();
        snapshotObject.configure(snapshotVO, store);
        snapshotObject.snapshotStoreDao = snapshotStoreDao;
        snapshotObject.snapshotFactory = snapshotFactory;
    }

    @Test
    public void testGetCorrectIncrementalParentNoRefsReturnsNull() {
        Mockito.when(snapshotStoreDao.findBySnapshotId(PARENT_SNAPSHOT_ID)).thenReturn(Collections.emptyList());

        Assert.assertNull(snapshotObject.getCorrectIncrementalParent(PARENT_SNAPSHOT_ID));
    }

    @Test
    public void testGetCorrectIncrementalParentPrefersCheckpointBearingRef() {
        SnapshotDataStoreVO refWithoutCheckpoint = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(refWithoutCheckpoint.getKvmCheckpointPath()).thenReturn(null);
        SnapshotDataStoreVO refWithCheckpoint = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(refWithCheckpoint.getKvmCheckpointPath()).thenReturn("checkpoints/2/5/uuid");
        Mockito.when(refWithCheckpoint.getDataStoreId()).thenReturn(5L);
        Mockito.when(refWithCheckpoint.getRole()).thenReturn(DataStoreRole.Image);
        Mockito.when(snapshotStoreDao.findBySnapshotId(PARENT_SNAPSHOT_ID)).thenReturn(List.of(refWithoutCheckpoint, refWithCheckpoint));

        SnapshotInfo parentInfo = Mockito.mock(SnapshotInfo.class);
        Mockito.when(snapshotFactory.getSnapshot(PARENT_SNAPSHOT_ID, 5L, DataStoreRole.Image)).thenReturn(parentInfo);

        Assert.assertEquals(parentInfo, snapshotObject.getCorrectIncrementalParent(PARENT_SNAPSHOT_ID));
        Mockito.verify(parentInfo).setKvmIncrementalSnapshot(true);
    }

    @Test
    public void testGetCorrectIncrementalParentFallsBackToPlainParentWithoutCheckpointRefs() {
        SnapshotDataStoreVO refWithoutCheckpoint = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(refWithoutCheckpoint.getKvmCheckpointPath()).thenReturn(null);
        Mockito.when(snapshotStoreDao.findBySnapshotId(PARENT_SNAPSHOT_ID)).thenReturn(List.of(refWithoutCheckpoint));

        SnapshotInfo parentInfo = Mockito.mock(SnapshotInfo.class);
        Mockito.when(snapshotFactory.getSnapshot(PARENT_SNAPSHOT_ID, store)).thenReturn(parentInfo);

        Assert.assertEquals(parentInfo, snapshotObject.getCorrectIncrementalParent(PARENT_SNAPSHOT_ID));
        Mockito.verify(parentInfo, Mockito.never()).setKvmIncrementalSnapshot(Mockito.anyBoolean());
    }
}
