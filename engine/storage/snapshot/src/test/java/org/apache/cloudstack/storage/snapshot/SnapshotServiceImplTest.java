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

import com.cloud.agent.api.Answer;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ImageStore;
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
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.heuristics.HeuristicRuleHelper;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
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
    SnapshotInfo snapshotInfoMock;

    @Mock
    VolumeInfo volumeInfoMock;

    @Mock
    DataStoreManager dataStoreManagerMock;

    @Mock
    private SnapshotResult snapshotResultMock;

    @Mock
    private CreateObjectAnswer createObjectAnswerMock;

    @Mock
    private ImageStore imageStoreMock;

    @Mock
    private SnapshotDataStoreVO snapshotDataStoreVoMock;

    @Mock
    private SnapshotDataStoreDao snapshotDataStoreDaoMock;

    @Mock
    private DataStore dataStoreMock;

    @Mock
    private SnapshotObjectTO snapshotObjectTOMock;


    private static final long DUMMY_ID = 1L;

    @Test
    public void testRevertSnapshotWithNoPrimaryStorageEntry() throws Exception {
        Mockito.when(snapshotInfoMock.getId()).thenReturn(DUMMY_ID);
        Mockito.when(snapshotInfoMock.getVolumeId()).thenReturn(DUMMY_ID);
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
            Assert.assertTrue(snapshotService.revertSnapshot(snapshotInfoMock));
        }
    }

    @Test
    public void getImageStoreForSnapshotTestShouldListFreeImageStoresWithNoHeuristicRule() {
        Mockito.when(heuristicRuleHelperMock.getImageStoreIfThereIsHeuristicRule(Mockito.anyLong(), Mockito.any(HeuristicType.class), Mockito.any(SnapshotInfo.class))).
                thenReturn(null);
        Mockito.when(snapshotInfoMock.getDataCenterId()).thenReturn(DUMMY_ID);

        snapshotService.getImageStoreForSnapshot(DUMMY_ID, snapshotInfoMock);

        Mockito.verify(dataStoreManagerMock, Mockito.times(1)).getImageStoreWithFreeCapacity(Mockito.anyLong());
    }

    @Test
    public void getImageStoreForSnapshotTestShouldReturnImageStoreReturnedByTheHeuristicRule() {
        DataStore dataStore = Mockito.mock(DataStore.class);
        Mockito.when(heuristicRuleHelperMock.getImageStoreIfThereIsHeuristicRule(Mockito.anyLong(), Mockito.any(HeuristicType.class), Mockito.any(SnapshotInfo.class))).
                thenReturn(dataStore);

        snapshotService.getImageStoreForSnapshot(DUMMY_ID, snapshotInfoMock);

        Mockito.verify(dataStoreManagerMock, Mockito.times(0)).getImageStoreWithFreeCapacity(Mockito.anyLong());
    }

    @Test
    public void updateSnapSizeAndCheckpointPathIfPossibleTestResultIsFalse() {
        Mockito.doReturn(createObjectAnswerMock).when(snapshotResultMock).getAnswer();
        Mockito.doReturn(false).when(createObjectAnswerMock).getResult();

        snapshotService.updateSnapSizeAndCheckpointPathIfPossible(snapshotResultMock, snapshotInfoMock);

        Mockito.verify(snapshotInfoMock, Mockito.never()).getImageStore();
    }

    @Test
    public void updateSnapSizeAndCheckpointPathIfPossibleTestResultIsTrueAnswerIsNotCreateObjectAnswer() {
        Answer answer = new Answer(null, true, null);

        Mockito.doReturn(answer).when(snapshotResultMock).getAnswer();

        snapshotService.updateSnapSizeAndCheckpointPathIfPossible(snapshotResultMock, snapshotInfoMock);

        Mockito.verify(snapshotInfoMock, Mockito.never()).getImageStore();
    }

    @Test
    public void updateSnapSizeAndCheckpointPathIfPossibleTestResultIsTrueAnswerIsCreateObjectAnswerAndImageStoreIsNotNullAndPhysicalSizeIsZero() {
        Mockito.doReturn(createObjectAnswerMock).when(snapshotResultMock).getAnswer();
        Mockito.doReturn(true).when(createObjectAnswerMock).getResult();

        Mockito.doReturn(snapshotInfoMock).when(snapshotResultMock).getSnapshot();

        Mockito.doReturn(dataStoreMock).when(snapshotInfoMock).getImageStore();

        Mockito.doReturn(snapshotDataStoreVoMock).when(snapshotDataStoreDaoMock).findBySnapshotIdAndDataStoreRoleAndState(Mockito.anyLong(), Mockito.any(), Mockito.any());

        Mockito.doReturn(snapshotObjectTOMock).when(createObjectAnswerMock).getData();
        Mockito.doReturn("checkpath").when(snapshotObjectTOMock).getCheckpointPath();
        Mockito.doReturn(0L).when(snapshotObjectTOMock).getPhysicalSize();

        snapshotService.updateSnapSizeAndCheckpointPathIfPossible(snapshotResultMock, snapshotInfoMock);

        Mockito.verify(snapshotDataStoreVoMock).setKvmCheckpointPath("checkpath");
        Mockito.verify(snapshotDataStoreVoMock, Mockito.never()).setPhysicalSize(Mockito.anyLong());

        Mockito.verify(snapshotDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void updateSnapSizeAndCheckpointPathIfPossibleTestResultIsTrueAnswerIsCreateObjectAnswerAndImageStoreIsNotNullAndPhysicalSizeGreaterThanZero() {
        Mockito.doReturn(createObjectAnswerMock).when(snapshotResultMock).getAnswer();
        Mockito.doReturn(true).when(createObjectAnswerMock).getResult();

        Mockito.doReturn(snapshotInfoMock).when(snapshotResultMock).getSnapshot();

        Mockito.doReturn(dataStoreMock).when(snapshotInfoMock).getImageStore();

        Mockito.doReturn(snapshotDataStoreVoMock).when(snapshotDataStoreDaoMock).findBySnapshotIdAndDataStoreRoleAndState(Mockito.anyLong(), Mockito.any(), Mockito.any());

        Mockito.doReturn(snapshotObjectTOMock).when(createObjectAnswerMock).getData();
        Mockito.doReturn("checkpath").when(snapshotObjectTOMock).getCheckpointPath();
        Mockito.doReturn(1000L).when(snapshotObjectTOMock).getPhysicalSize();

        snapshotService.updateSnapSizeAndCheckpointPathIfPossible(snapshotResultMock, snapshotInfoMock);

        Mockito.verify(snapshotDataStoreVoMock).setKvmCheckpointPath("checkpath");
        Mockito.verify(snapshotDataStoreVoMock).setPhysicalSize(1000L);

        Mockito.verify(snapshotDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void updateSnapSizeAndCheckpointPathIfPossibleTestResultIsTrueAnswerIsCreateObjectAnswerAndImageStoreIsNullAndPhysicalSizeIsZero() {
        Mockito.doReturn(createObjectAnswerMock).when(snapshotResultMock).getAnswer();
        Mockito.doReturn(true).when(createObjectAnswerMock).getResult();

        Mockito.doReturn(snapshotInfoMock).when(snapshotResultMock).getSnapshot();

        Mockito.doReturn(null).when(snapshotInfoMock).getImageStore();
        Mockito.doReturn(dataStoreMock).when(snapshotInfoMock).getDataStore();

        Mockito.doReturn(snapshotDataStoreVoMock).when(snapshotDataStoreDaoMock).findByStoreSnapshot(Mockito.any(), Mockito.anyLong(), Mockito.anyLong());

        Mockito.doReturn(snapshotObjectTOMock).when(createObjectAnswerMock).getData();
        Mockito.doReturn("checkpath").when(snapshotObjectTOMock).getCheckpointPath();
        Mockito.doReturn(0L).when(snapshotObjectTOMock).getPhysicalSize();

        snapshotService.updateSnapSizeAndCheckpointPathIfPossible(snapshotResultMock, snapshotInfoMock);

        Mockito.verify(snapshotDataStoreVoMock).setKvmCheckpointPath("checkpath");
        Mockito.verify(snapshotDataStoreVoMock, Mockito.never()).setPhysicalSize(Mockito.anyLong());

        Mockito.verify(snapshotDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void updateSnapSizeAndCheckpointPathIfPossibleTestResultIsTrueAnswerIsCreateObjectAnswerAndImageStoreIsNullAndPhysicalSizeGreaterThanZero() {
        Mockito.doReturn(createObjectAnswerMock).when(snapshotResultMock).getAnswer();
        Mockito.doReturn(true).when(createObjectAnswerMock).getResult();

        Mockito.doReturn(snapshotInfoMock).when(snapshotResultMock).getSnapshot();

        Mockito.doReturn(null).when(snapshotInfoMock).getImageStore();
        Mockito.doReturn(dataStoreMock).when(snapshotInfoMock).getDataStore();

        Mockito.doReturn(snapshotDataStoreVoMock).when(snapshotDataStoreDaoMock).findByStoreSnapshot(Mockito.any(), Mockito.anyLong(), Mockito.anyLong());

        Mockito.doReturn(snapshotObjectTOMock).when(createObjectAnswerMock).getData();
        Mockito.doReturn("checkpath").when(snapshotObjectTOMock).getCheckpointPath();
        Mockito.doReturn(1000L).when(snapshotObjectTOMock).getPhysicalSize();

        snapshotService.updateSnapSizeAndCheckpointPathIfPossible(snapshotResultMock, snapshotInfoMock);

        Mockito.verify(snapshotDataStoreVoMock).setKvmCheckpointPath("checkpath");
        Mockito.verify(snapshotDataStoreVoMock).setPhysicalSize(1000L);

        Mockito.verify(snapshotDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());
    }
}
