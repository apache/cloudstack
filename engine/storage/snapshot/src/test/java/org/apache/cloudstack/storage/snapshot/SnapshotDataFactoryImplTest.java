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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.component.ComponentContext;


@RunWith(MockitoJUnitRunner.class)
public class SnapshotDataFactoryImplTest {

    @Spy
    @InjectMocks
    private SnapshotDataFactoryImpl snapshotDataFactoryImpl = new SnapshotDataFactoryImpl();

    @Mock
    private SnapshotDataStoreDao snapshotStoreDaoMock;
    @Mock
    private DataStoreManager dataStoreManagerMock;
    @Mock
    private SnapshotDao snapshotDaoMock;

    private long volumeMockId = 1;

    @Test
    public void getSnapshotsByVolumeAndDataStoreTestNoSnapshotDataStoreVOFound() {
        Mockito.doReturn(new ArrayList<>()).when(snapshotStoreDaoMock).listAllByVolumeAndDataStore(volumeMockId, DataStoreRole.Primary);

        List<SnapshotInfo> snapshots = snapshotDataFactoryImpl.getSnapshots(volumeMockId, DataStoreRole.Primary);

        Assert.assertTrue(snapshots.isEmpty());
    }

    @Test
    public void getSnapshotsByVolumeAndDataStoreTest() {
        try (MockedStatic<ComponentContext> componentContextMockedStatic = Mockito.mockStatic(ComponentContext.class)) {
            Mockito.when(ComponentContext.inject(SnapshotObject.class)).thenReturn(new SnapshotObject());

            SnapshotDataStoreVO snapshotDataStoreVoMock = Mockito.mock(SnapshotDataStoreVO.class);

            long snapshotId = 1223;
            long dataStoreId = 34567;
            Mockito.doReturn(snapshotId).when(snapshotDataStoreVoMock).getSnapshotId();
            Mockito.doReturn(dataStoreId).when(snapshotDataStoreVoMock).getDataStoreId();

            SnapshotVO snapshotVoMock = Mockito.mock(SnapshotVO.class);

            DataStoreRole dataStoreRole = DataStoreRole.Primary;
            DataStore dataStoreMock = Mockito.mock(DataStore.class);

            List<SnapshotDataStoreVO> snapshotDataStoreVOs = new ArrayList<>();
            snapshotDataStoreVOs.add(snapshotDataStoreVoMock);

            Mockito.doReturn(snapshotDataStoreVOs).when(snapshotStoreDaoMock).listAllByVolumeAndDataStore(volumeMockId, dataStoreRole);
            Mockito.doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(dataStoreId, dataStoreRole);
            Mockito.doReturn(snapshotVoMock).when(snapshotDaoMock).findById(snapshotId);

            List<SnapshotInfo> snapshots = snapshotDataFactoryImpl.getSnapshots(volumeMockId, dataStoreRole);

            Assert.assertEquals(1, snapshots.size());

            SnapshotInfo snapshotInfo = snapshots.get(0);
            Assert.assertEquals(dataStoreMock, snapshotInfo.getDataStore());
            Assert.assertEquals(snapshotVoMock, ((SnapshotObject) snapshotInfo).getSnapshotVO());

            componentContextMockedStatic.verify(() -> ComponentContext.inject(SnapshotObject.class), Mockito.times(1));
        }
    }
}
