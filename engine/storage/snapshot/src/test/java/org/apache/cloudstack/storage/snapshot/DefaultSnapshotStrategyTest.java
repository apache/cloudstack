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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.fsm.NoTransitionException;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSnapshotStrategyTest {

    @InjectMocks
    DefaultSnapshotStrategy defaultSnapshotStrategySpy = Mockito.spy(DefaultSnapshotStrategy.class);

    @Mock
    SnapshotDataFactory snapshotDataFactoryMock;

    @Mock
    SnapshotInfo snapshotInfo1Mock, snapshotInfo2Mock;

    @Mock
    SnapshotObject snapshotObjectMock;

    @Mock
    SnapshotDao snapshotDaoMock;

    @Mock
    SnapshotVO snapshotVoMock;

    @Mock
    DataStore dataStoreMock;

    @Mock
    VolumeDetailsDao volumeDetailsDaoMock;

    @Mock
    SnapshotService snapshotServiceMock;

    Map<String, SnapshotInfo> mapStringSnapshotInfoInstance = new LinkedHashMap<>();

    @Before
    public void setup() {
        mapStringSnapshotInfoInstance.put("secondary storage", snapshotInfo1Mock);
        mapStringSnapshotInfoInstance.put("primary storage", snapshotInfo1Mock);
    }

    @Test
    public void validateRetrieveSnapshotEntries() {
        Long snapshotId = 1l;
        Mockito.doReturn(snapshotInfo1Mock, snapshotInfo2Mock).when(snapshotDataFactoryMock).getSnapshot(Mockito.anyLong(), Mockito.any(DataStoreRole.class), Mockito.anyBoolean());
        Map<String, SnapshotInfo> result = defaultSnapshotStrategySpy.retrieveSnapshotEntries(snapshotId);

        Mockito.verify(snapshotDataFactoryMock).getSnapshot(snapshotId, DataStoreRole.Image, false);
        Mockito.verify(snapshotDataFactoryMock).getSnapshot(snapshotId, DataStoreRole.Primary, false);

        Assert.assertTrue(result.containsKey("secondary storage"));
        Assert.assertTrue(result.containsKey("primary storage"));
        Assert.assertEquals(snapshotInfo1Mock, result.get("secondary storage"));
        Assert.assertEquals(snapshotInfo2Mock, result.get("primary storage"));
    }

    @Test
    public void validateUpdateSnapshotToDestroyed() {
        Mockito.doReturn(true).when(snapshotDaoMock).update(Mockito.anyLong(), Mockito.any());
        defaultSnapshotStrategySpy.updateSnapshotToDestroyed(snapshotVoMock);

        Mockito.verify(snapshotVoMock).setState(Snapshot.State.Destroyed);
    }

    @Test
    public void validateDestroySnapshotEntriesAndFilesFailToDeleteReturnsFalse() {
        Mockito.doReturn(false).when(defaultSnapshotStrategySpy).deleteSnapshotInfos(Mockito.any());
        Assert.assertFalse(defaultSnapshotStrategySpy.destroySnapshotEntriesAndFiles(snapshotVoMock));
    }

    @Test
    public void validateDestroySnapshotEntriesAndFilesDeletesSuccessfullyReturnsTrue() {
        Mockito.doReturn(true).when(defaultSnapshotStrategySpy).deleteSnapshotInfos(Mockito.any());
        Assert.assertTrue(defaultSnapshotStrategySpy.destroySnapshotEntriesAndFiles(snapshotVoMock));
    }

    @Test
    public void validateDeleteSnapshotInfosFailToDeleteReturnsFalse() {
        Mockito.doReturn(mapStringSnapshotInfoInstance).when(defaultSnapshotStrategySpy).retrieveSnapshotEntries(Mockito.anyLong());
        Mockito.doReturn(false).when(defaultSnapshotStrategySpy).deleteSnapshotInfo(Mockito.any(), Mockito.anyString(), Mockito.any());
        Assert.assertFalse(defaultSnapshotStrategySpy.deleteSnapshotInfos(snapshotVoMock));
    }

    @Test
    public void validateDeleteSnapshotInfosDeletesSuccessfullyReturnsTrue() {
        Mockito.doReturn(mapStringSnapshotInfoInstance).when(defaultSnapshotStrategySpy).retrieveSnapshotEntries(Mockito.anyLong());
        Mockito.doReturn(true).when(defaultSnapshotStrategySpy).deleteSnapshotInfo(Mockito.any(), Mockito.anyString(), Mockito.any());
        Assert.assertTrue(defaultSnapshotStrategySpy.deleteSnapshotInfos(snapshotVoMock));
    }

    @Test
    public void validateDeleteSnapshotInfoSnapshotInfoIsNullOnSecondaryStorageReturnsTrue() {
        Assert.assertNull(defaultSnapshotStrategySpy.deleteSnapshotInfo(null, "secondary storage", snapshotVoMock));
    }

    @Test
    public void validateDeleteSnapshotInfoSnapshotInfoIsNullOnPrimaryStorageReturnsFalse() {
        Assert.assertTrue(defaultSnapshotStrategySpy.deleteSnapshotInfo(null, "primary storage", snapshotVoMock));
    }

    @Test
    public void deleteSnapshotInfoTestReturnTrueIfCanDeleteTheSnapshotOnPrimaryStorage() throws NoTransitionException {
        Mockito.doReturn(dataStoreMock).when(snapshotInfo1Mock).getDataStore();
        Mockito.doReturn(snapshotObjectMock).when(defaultSnapshotStrategySpy).castSnapshotInfoToSnapshotObject(snapshotInfo1Mock);
        Mockito.doNothing().when(snapshotObjectMock).processEvent(Mockito.any(Snapshot.Event.class));
        Mockito.doReturn(true).when(snapshotServiceMock).deleteSnapshot(Mockito.any());

        boolean result = defaultSnapshotStrategySpy.deleteSnapshotInfo(snapshotInfo1Mock, "primary storage", snapshotVoMock);
        Assert.assertTrue(result);
    }

    @Test
    public void deleteSnapshotInfoTestReturnFalseIfCannotDeleteTheSnapshotOnPrimaryStorage() throws NoTransitionException {
        Mockito.doReturn(dataStoreMock).when(snapshotInfo1Mock).getDataStore();
        Mockito.doReturn(snapshotObjectMock).when(defaultSnapshotStrategySpy).castSnapshotInfoToSnapshotObject(snapshotInfo1Mock);
        Mockito.doNothing().when(snapshotObjectMock).processEvent(Mockito.any(Snapshot.Event.class));
        Mockito.doReturn(false).when(snapshotServiceMock).deleteSnapshot(Mockito.any());

        boolean result = defaultSnapshotStrategySpy.deleteSnapshotInfo(snapshotInfo1Mock, "primary storage", snapshotVoMock);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteSnapshotInfoTestReturnFalseIfDeleteSnapshotOnPrimaryStorageThrowsACloudRuntimeException() throws NoTransitionException {
        Mockito.doReturn(dataStoreMock).when(snapshotInfo1Mock).getDataStore();
        Mockito.doReturn(snapshotObjectMock).when(defaultSnapshotStrategySpy).castSnapshotInfoToSnapshotObject(snapshotInfo1Mock);
        Mockito.doNothing().when(snapshotObjectMock).processEvent(Mockito.any(Snapshot.Event.class));
        Mockito.doThrow(CloudRuntimeException.class).when(snapshotServiceMock).deleteSnapshot(Mockito.any());

        boolean result = defaultSnapshotStrategySpy.deleteSnapshotInfo(snapshotInfo1Mock, "primary storage", snapshotVoMock);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteSnapshotInfoTestReturnTrueIfCanDeleteTheSnapshotChainForSecondaryStorage() throws NoTransitionException {
        Mockito.doReturn(dataStoreMock).when(snapshotInfo1Mock).getDataStore();
        Mockito.doReturn(snapshotObjectMock).when(defaultSnapshotStrategySpy).castSnapshotInfoToSnapshotObject(snapshotInfo1Mock);
        Mockito.doNothing().when(defaultSnapshotStrategySpy).verifyIfTheSnapshotIsBeingUsedByAnyVolume(snapshotObjectMock);
        Mockito.doNothing().when(snapshotObjectMock).processEvent(Mockito.any(Snapshot.Event.class));
        Mockito.doReturn(true).when(defaultSnapshotStrategySpy).deleteSnapshotChain(Mockito.any(), Mockito.anyString());

        boolean result = defaultSnapshotStrategySpy.deleteSnapshotInfo(snapshotInfo1Mock, "secondary storage", snapshotVoMock);
        Assert.assertTrue(result);
    }

    @Test
    public void deleteSnapshotInfoTestReturnTrueIfCannotDeleteTheSnapshotChainForSecondaryStorage() throws NoTransitionException {
        Mockito.doReturn(dataStoreMock).when(snapshotInfo1Mock).getDataStore();
        Mockito.doReturn(snapshotObjectMock).when(defaultSnapshotStrategySpy).castSnapshotInfoToSnapshotObject(snapshotInfo1Mock);
        Mockito.doNothing().when(defaultSnapshotStrategySpy).verifyIfTheSnapshotIsBeingUsedByAnyVolume(snapshotObjectMock);
        Mockito.doNothing().when(snapshotObjectMock).processEvent(Mockito.any(Snapshot.Event.class));
        Mockito.doReturn(false).when(defaultSnapshotStrategySpy).deleteSnapshotChain(Mockito.any(), Mockito.anyString());

        boolean result = defaultSnapshotStrategySpy.deleteSnapshotInfo(snapshotInfo1Mock, "secondary storage", snapshotVoMock);
        Assert.assertTrue(result);
    }

    @Test
    public void validateDeleteSnapshotInfoSnapshotProcessSnapshotEventThrowsNoTransitionExceptionReturnsFalse() throws NoTransitionException {
        Mockito.doReturn(dataStoreMock).when(snapshotInfo1Mock).getDataStore();
        Mockito.doReturn(snapshotObjectMock).when(defaultSnapshotStrategySpy).castSnapshotInfoToSnapshotObject(snapshotInfo1Mock);
        Mockito.doThrow(NoTransitionException.class).when(snapshotObjectMock).processEvent(Mockito.any(Snapshot.Event.class));

        Assert.assertFalse(defaultSnapshotStrategySpy.deleteSnapshotInfo(snapshotInfo1Mock, "secondary storage", snapshotVoMock));
    }

    @Test
    public void verifyIfTheSnapshotIsBeingUsedByAnyVolumeTestDetailsIsEmptyDoNothing() throws NoTransitionException {
        Mockito.doReturn(new ArrayList<>()).when(volumeDetailsDaoMock).findDetails(Mockito.any(), Mockito.any(), Mockito.any());
        defaultSnapshotStrategySpy.verifyIfTheSnapshotIsBeingUsedByAnyVolume(snapshotObjectMock);
        Mockito.verify(snapshotObjectMock, Mockito.never()).processEvent(Mockito.any(Snapshot.Event.class));
    }

    @Test
    public void verifyIfTheSnapshotIsBeingUsedByAnyVolumeTestDetailsIsNullDoNothing() throws NoTransitionException {
        Mockito.doReturn(null).when(volumeDetailsDaoMock).findDetails(Mockito.any(), Mockito.any(), Mockito.any());
        defaultSnapshotStrategySpy.verifyIfTheSnapshotIsBeingUsedByAnyVolume(snapshotObjectMock);
        Mockito.verify(snapshotObjectMock, Mockito.never()).processEvent(Mockito.any(Snapshot.Event.class));
    }

    @Test(expected = CloudRuntimeException.class)
    public void verifyIfTheSnapshotIsBeingUsedByAnyVolumeTestDetailsIsNotEmptyThrowCloudRuntimeException() throws NoTransitionException {
        Mockito.doReturn(List.of(new VolumeDetailVO())).when(volumeDetailsDaoMock).findDetails(Mockito.any(), Mockito.any(), Mockito.any());
        defaultSnapshotStrategySpy.verifyIfTheSnapshotIsBeingUsedByAnyVolume(snapshotObjectMock);
    }

    @Test
    public void deleteSnapshotInPrimaryStorageTestReturnTrueIfDeleteReturnsTrue() throws NoTransitionException {
        Mockito.doReturn(true).when(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Mockito.doNothing().when(snapshotObjectMock).processEvent(Mockito.any(Snapshot.Event.class));
        Assert.assertTrue(defaultSnapshotStrategySpy.deleteSnapshotInPrimaryStorage(null, null, null, snapshotObjectMock));
    }

    @Test
    public void deleteSnapshotInPrimaryStorageTestReturnFalseIfDeleteReturnsFalse() throws NoTransitionException {
        Mockito.doReturn(false).when(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Assert.assertFalse(defaultSnapshotStrategySpy.deleteSnapshotInPrimaryStorage(null, null, null, null));
    }

    @Test
    public void deleteSnapshotInPrimaryStorageTestReturnFalseIfDeleteThrowsException() throws NoTransitionException {
        Mockito.doThrow(CloudRuntimeException.class).when(snapshotServiceMock).deleteSnapshot(Mockito.any());
        Assert.assertFalse(defaultSnapshotStrategySpy.deleteSnapshotInPrimaryStorage(null, null, null, null));
    }
}
