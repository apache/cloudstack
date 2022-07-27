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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SnapshotServiceImpl.class})
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
    AsyncCallFuture<SnapshotResult> futureMock;

    @Mock
    AsyncCallbackDispatcher<SnapshotServiceImpl, CommandResult> caller;

    @Before
    public void testSetUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRevertSnapshotWithNoPrimaryStorageEntry() throws Exception {
        SnapshotInfo snapshot = Mockito.mock(SnapshotInfo.class);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);

        Mockito.when(snapshot.getId()).thenReturn(1L);
        Mockito.when(snapshot.getVolumeId()).thenReturn(1L);
        Mockito.when(_snapshotFactory.getSnapshot(1L, DataStoreRole.Primary)).thenReturn(null);
        Mockito.when(volFactory.getVolume(1L, DataStoreRole.Primary)).thenReturn(volumeInfo);

        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(store);

        PrimaryDataStoreDriver driver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(store.getDriver()).thenReturn(driver);
        Mockito.doNothing().when(driver).revertSnapshot(snapshot, null, caller);

        SnapshotResult result = Mockito.mock(SnapshotResult.class);
        PowerMockito.whenNew(AsyncCallFuture.class).withNoArguments().thenReturn(futureMock);
        Mockito.when(futureMock.get()).thenReturn(result);
        Mockito.when(result.isFailed()).thenReturn(false);

        Assert.assertEquals(true, snapshotService.revertSnapshot(snapshot));
    }

}
