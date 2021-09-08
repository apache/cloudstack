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

package org.apache.cloudstack.storage.volume;

import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import junit.framework.TestCase;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VolumeServiceTest extends TestCase{

    @Spy
    VolumeServiceImpl volumeServiceImplSpy;

    @Mock
    VolumeDataFactory volumeDataFactoryMock;

    @Mock
    VolumeInfo volumeInfoMock;

    @Mock
    AsyncCallFuture<VolumeService.VolumeApiResult> asyncCallFutureVolumeApiResultMock;

    @Mock
    VolumeService.VolumeApiResult volumeApiResultMock;

    @Mock
    VolumeDao volumeDaoMock;

    @Mock
    SnapshotManager snapshotManagerMock;

    @Mock
    VolumeVO volumeVoMock;

    @Before
    public void setup(){
        volumeServiceImplSpy = Mockito.spy(new VolumeServiceImpl());
        volumeServiceImplSpy.volFactory = volumeDataFactoryMock;
        volumeServiceImplSpy.volDao = volumeDaoMock;
        volumeServiceImplSpy.snapshotMgr = snapshotManagerMock;
    }

    @Test(expected = InterruptedException.class)
    public void validateExpungeSourceVolumeAfterMigrationThrowInterruptedExceptionOnFirstFutureGetCall() throws InterruptedException, ExecutionException{
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(Mockito.anyLong());
        Mockito.doReturn(asyncCallFutureVolumeApiResultMock).when(volumeServiceImplSpy).expungeVolumeAsync(Mockito.any());
        Mockito.doThrow(new InterruptedException()).when(asyncCallFutureVolumeApiResultMock).get();

        volumeServiceImplSpy.expungeSourceVolumeAfterMigration(new VolumeVO() {}, false);
    }

    @Test(expected = ExecutionException.class)
    public void validateExpungeSourceVolumeAfterMigrationThrowExecutionExceptionOnFirstFutureGetCall() throws InterruptedException, ExecutionException{
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(Mockito.anyLong());
        Mockito.doReturn(asyncCallFutureVolumeApiResultMock).when(volumeServiceImplSpy).expungeVolumeAsync(Mockito.any());
        Mockito.doThrow(new ExecutionException() {}).when(asyncCallFutureVolumeApiResultMock).get();

        volumeServiceImplSpy.expungeSourceVolumeAfterMigration(new VolumeVO() {}, false);
    }

    @Test
    public void validateExpungeSourceVolumeAfterMigrationVolumeApiResultSucceedDoNoMoreInteractions() throws InterruptedException, ExecutionException{
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(Mockito.anyLong());
        Mockito.doReturn(asyncCallFutureVolumeApiResultMock).when(volumeServiceImplSpy).expungeVolumeAsync(Mockito.any());
        Mockito.doReturn(volumeApiResultMock).when(asyncCallFutureVolumeApiResultMock).get();
        Mockito.doReturn(true).when(volumeApiResultMock).isSuccess();

        volumeServiceImplSpy.expungeSourceVolumeAfterMigration(new VolumeVO() {}, false);
        Mockito.verify(volumeApiResultMock, Mockito.never()).getResult();
    }

    @Test
    public void validateExpungeSourceVolumeAfterMigrationVolumeApiResultFailedDoNotRetryExpungeVolume() throws InterruptedException, ExecutionException{
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(Mockito.anyLong());
        Mockito.doReturn(asyncCallFutureVolumeApiResultMock).when(volumeServiceImplSpy).expungeVolumeAsync(Mockito.any());
        Mockito.doReturn(volumeApiResultMock).when(asyncCallFutureVolumeApiResultMock).get();
        Mockito.doReturn(false).when(volumeApiResultMock).isSuccess();
        boolean retryExpungeVolume = false;

        volumeServiceImplSpy.expungeSourceVolumeAfterMigration(new VolumeVO() {}, retryExpungeVolume);
        Mockito.verify(volumeServiceImplSpy, Mockito.times(1)).expungeVolumeAsync(volumeInfoMock);
    }

    @Test (expected = InterruptedException.class)
    public void validateExpungeSourceVolumeAfterMigrationVolumeApiResultFailedRetryExpungeVolumeThrowInterruptedException() throws InterruptedException, ExecutionException{
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(Mockito.anyLong());
        Mockito.doReturn(asyncCallFutureVolumeApiResultMock).when(volumeServiceImplSpy).expungeVolumeAsync(Mockito.any());
        Mockito.doReturn(volumeApiResultMock).doThrow(new InterruptedException()).when(asyncCallFutureVolumeApiResultMock).get();
        Mockito.doReturn(false).when(volumeApiResultMock).isSuccess();
        boolean retryExpungeVolume = true;

        volumeServiceImplSpy.expungeSourceVolumeAfterMigration(new VolumeVO() {}, retryExpungeVolume);
    }

    @Test (expected = ExecutionException.class)
    public void validateExpungeSourceVolumeAfterMigrationVolumeApiResultFailedRetryExpungeVolumeThrowExecutionException() throws InterruptedException, ExecutionException{
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(Mockito.anyLong());
        Mockito.doReturn(asyncCallFutureVolumeApiResultMock).when(volumeServiceImplSpy).expungeVolumeAsync(Mockito.any());
        Mockito.doReturn(volumeApiResultMock).doThrow(new ExecutionException(){}).when(asyncCallFutureVolumeApiResultMock).get();
        Mockito.doReturn(false).when(volumeApiResultMock).isSuccess();
        boolean retryExpungeVolume = true;

        volumeServiceImplSpy.expungeSourceVolumeAfterMigration(new VolumeVO() {}, retryExpungeVolume);
    }

    @Test
    public void validateCopyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigrationReturnTrueOrFalse() throws ExecutionException, InterruptedException{
        VolumeObject volumeObject = new VolumeObject();
        volumeObject.configure(null, new VolumeVO() {});

        Mockito.doNothing().when(snapshotManagerMock).copySnapshotPoliciesBetweenVolumes(Mockito.any(), Mockito.any());
        Mockito.doReturn(true, false).when(volumeServiceImplSpy).destroySourceVolumeAfterMigration(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());

        boolean result = volumeServiceImplSpy.copyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigration(ObjectInDataStoreStateMachine.Event.DestroyRequested, null,
          volumeObject, volumeObject, true);
        boolean result2 = volumeServiceImplSpy.copyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigration(ObjectInDataStoreStateMachine.Event.DestroyRequested, null,
          volumeObject, volumeObject, true);

        Assert.assertTrue(result);
        Assert.assertFalse(result2);
    }

    @Test (expected = Exception.class)
    public void validateCopyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigrationThrowAnyOtherException() throws
      ExecutionException, InterruptedException{
        VolumeObject volumeObject = new VolumeObject();
        volumeObject.configure(null, new VolumeVO() {});

        volumeServiceImplSpy.copyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigration(ObjectInDataStoreStateMachine.Event.DestroyRequested, null, volumeObject,
          volumeObject, true);
    }

    @Test
    public void validateDestroySourceVolumeAfterMigrationReturnTrue() throws ExecutionException, InterruptedException{
        VolumeObject volumeObject = new VolumeObject();
        volumeObject.configure(null, new VolumeVO() {});

        Mockito.doReturn(true).when(volumeDaoMock).updateUuid(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(volumeServiceImplSpy).destroyVolume(Mockito.anyLong());
        Mockito.doNothing().when(volumeServiceImplSpy).expungeSourceVolumeAfterMigration(Mockito.any(), Mockito.anyBoolean());

        boolean result = volumeServiceImplSpy.destroySourceVolumeAfterMigration(ObjectInDataStoreStateMachine.Event.DestroyRequested, null, volumeObject,
          volumeObject, true);

        Assert.assertTrue(result);
    }

    @Test
    public void validateDestroySourceVolumeAfterMigrationExpungeSourceVolumeAfterMigrationThrowExceptionReturnFalse() throws
      ExecutionException, InterruptedException{
        VolumeObject volumeObject = new VolumeObject();
        volumeObject.configure(null, new VolumeVO() {});

        List<Exception> exceptions = new ArrayList<>(Arrays.asList(new InterruptedException(), new ExecutionException() {}));

        for (Exception exception : exceptions) {
            Mockito.doReturn(true).when(volumeDaoMock).updateUuid(Mockito.anyLong(), Mockito.anyLong());
            Mockito.doNothing().when(volumeServiceImplSpy).destroyVolume(Mockito.anyLong());
            Mockito.doThrow(exception).when(volumeServiceImplSpy).expungeSourceVolumeAfterMigration(Mockito.any(), Mockito.anyBoolean());

            boolean result = volumeServiceImplSpy.destroySourceVolumeAfterMigration(ObjectInDataStoreStateMachine.Event.DestroyRequested, null,
              volumeObject, volumeObject, true);

            Assert.assertFalse(result);
        }
    }

    @Test (expected = Exception.class)
    public void validateDestroySourceVolumeAfterMigrationThrowAnyOtherException() throws
      ExecutionException, InterruptedException{
        VolumeObject volumeObject = new VolumeObject();
        volumeObject.configure(null, new VolumeVO() {});

        volumeServiceImplSpy.destroySourceVolumeAfterMigration(ObjectInDataStoreStateMachine.Event.DestroyRequested, null, volumeObject,
          volumeObject, true);
    }
}
