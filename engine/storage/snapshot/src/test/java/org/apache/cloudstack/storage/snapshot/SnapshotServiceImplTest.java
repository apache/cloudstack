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

import com.cloud.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.apache.cloudstack.storage.heuristics.HeuristicRuleHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class SnapshotServiceImplTest {

    @Spy
    @InjectMocks
    private SnapshotServiceImpl snapshotService = new SnapshotServiceImpl();

    @Mock
    VolumeDataFactory volFactory;

    @Mock
    SnapshotDataFactory _snapshotFactory;

    @Mock
    HeuristicRuleHelper heuristicRuleHelperMock;

    @Mock
    SnapshotInfo snapshotMock;

    @Mock
    VolumeInfo volumeInfoMock;

    @Mock
    DataStoreManager dataStoreManagerMock;

    private static final long DUMMY_ID = 1L;

    @Test
    public void testRevertSnapshotWithNoPrimaryStorageEntry() throws Exception {
        Mockito.when(snapshotMock.getId()).thenReturn(DUMMY_ID);
        Mockito.when(snapshotMock.getVolumeId()).thenReturn(DUMMY_ID);
        Mockito.when(_snapshotFactory.getSnapshotOnPrimaryStore(1L)).thenReturn(null);
        Mockito.when(volFactory.getVolume(DUMMY_ID, DataStoreRole.Primary)).thenReturn(volumeInfoMock);

        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(store);

        PrimaryDataStoreDriver driver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(store.getDriver()).thenReturn(driver);

        SnapshotResult result = Mockito.mock(SnapshotResult.class);
        try (MockedConstruction<AsyncCallFuture> ignored = Mockito.mockConstruction(AsyncCallFuture.class, (mock, context) -> {
            Mockito.when(mock.get()).thenReturn(result);
            Mockito.when(result.isFailed()).thenReturn(false);
        })) {
            Assert.assertTrue(snapshotService.revertSnapshot(snapshotMock));
        }
    }

    @Test
    public void getImageStoreForSnapshotTestShouldListFreeImageStoresWithNoHeuristicRule() {
        Mockito.when(heuristicRuleHelperMock.getImageStoreIfThereIsHeuristicRule(Mockito.anyLong(), Mockito.any(HeuristicType.class), Mockito.any(SnapshotInfo.class))).
                thenReturn(null);
        Mockito.when(snapshotMock.getDataCenterId()).thenReturn(DUMMY_ID);

        snapshotService.getImageStoreForSnapshot(DUMMY_ID, snapshotMock);

        Mockito.verify(dataStoreManagerMock, Mockito.times(1)).getImageStoreWithFreeCapacity(Mockito.anyLong());
    }

    @Test
    public void getImageStoreForSnapshotTestShouldReturnImageStoreReturnedByTheHeuristicRule() {
        DataStore dataStore = Mockito.mock(DataStore.class);
        Mockito.when(heuristicRuleHelperMock.getImageStoreIfThereIsHeuristicRule(Mockito.anyLong(), Mockito.any(HeuristicType.class), Mockito.any(SnapshotInfo.class))).
                thenReturn(dataStore);

        snapshotService.getImageStoreForSnapshot(DUMMY_ID, snapshotMock);

        Mockito.verify(dataStoreManagerMock, Mockito.times(0)).getImageStoreWithFreeCapacity(Mockito.anyLong());
    }
}
