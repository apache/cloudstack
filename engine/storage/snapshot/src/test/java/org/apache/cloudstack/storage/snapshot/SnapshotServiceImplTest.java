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

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.storage.command.CommandResult;
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

import com.cloud.storage.DataStoreRole;

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
    AsyncCallbackDispatcher<SnapshotServiceImpl, CommandResult> caller;

    @Test
    public void testRevertSnapshotWithNoPrimaryStorageEntry() throws Exception {
        SnapshotInfo snapshot = Mockito.mock(SnapshotInfo.class);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);

        Mockito.when(snapshot.getId()).thenReturn(1L);
        Mockito.when(snapshot.getVolumeId()).thenReturn(1L);
        Mockito.when(_snapshotFactory.getSnapshotOnPrimaryStore(1L)).thenReturn(null);
        Mockito.when(volFactory.getVolume(1L, DataStoreRole.Primary)).thenReturn(volumeInfo);

        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(store);

        PrimaryDataStoreDriver driver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(store.getDriver()).thenReturn(driver);

        SnapshotResult result = Mockito.mock(SnapshotResult.class);
        try (MockedConstruction<AsyncCallFuture> ignored = Mockito.mockConstruction(AsyncCallFuture.class, (mock, context) -> {
            Mockito.when(mock.get()).thenReturn(result);
            Mockito.when(result.isFailed()).thenReturn(false);
        })) {
            Assert.assertTrue(snapshotService.revertSnapshot(snapshot));
        }
    }
}
