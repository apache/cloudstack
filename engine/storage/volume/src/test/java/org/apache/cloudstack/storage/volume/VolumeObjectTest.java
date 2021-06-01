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

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import junit.framework.TestCase;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VolumeObjectTest extends TestCase{

    @Spy
    VolumeObject volumeObjectSpy;

    @Mock
    DataStore dataStoreMock;

    @Mock
    VolumeVO volumeVoMock;

    @Mock
    VolumeDataStoreDao volumeDataStoreDaoMock;

    @Mock
    VolumeDataStoreVO volumeDataStoreVoMock;

    @Mock
    VolumeObjectTO volumeObjectToMock;

    @Mock
    VolumeDao volumeDaoMock;

    @Mock
    ObjectInDataStoreManager objectInDataStoreManagerMock;

    Set<Function<DiskOfferingVO, Long>> diskOfferingVoMethodsWithLongReturn = new HashSet<>();

    List<ObjectInDataStoreStateMachine.Event> objectInDataStoreStateMachineEvents = Arrays.asList(ObjectInDataStoreStateMachine.Event.values());

    List<DataStoreRole> dataStoreRolesExceptImageAndImageCache = new LinkedList<>(Arrays.asList(DataStoreRole.values()));

    @Before
    public void setup(){
        volumeObjectSpy.configure(dataStoreMock, volumeVoMock);
        volumeObjectSpy.volumeStoreDao = volumeDataStoreDaoMock;
        volumeObjectSpy.volumeDao = volumeDaoMock;
        volumeObjectSpy.objectInStoreMgr = objectInDataStoreManagerMock;

        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getBytesReadRate);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getBytesReadRateMax);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getBytesReadRateMaxLength);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getBytesWriteRate);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getBytesWriteRateMax);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getBytesWriteRateMaxLength);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getIopsReadRate);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getIopsReadRateMax);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getIopsReadRateMaxLength);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getIopsWriteRate);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getIopsWriteRateMax);
        diskOfferingVoMethodsWithLongReturn.add(DiskOfferingVO::getIopsWriteRateMaxLength);

        dataStoreRolesExceptImageAndImageCache.remove(DataStoreRole.Image);
        dataStoreRolesExceptImageAndImageCache.remove(DataStoreRole.ImageCache);
    }

    @Test
    public void validateGetLongValueFromDiskOfferingVoMethodNullDiskOfferingMustReturnNull(){
        Mockito.doReturn(null).when(volumeObjectSpy).getDiskOfferingVO();

        diskOfferingVoMethodsWithLongReturn.forEach(method -> Assert.assertNull(volumeObjectSpy.getLongValueFromDiskOfferingVoMethod(method)));
    }

    @Test
    public void validateGetLongValueFromDiskOfferingVoMethodNotNullNullDiskOfferingMustReturnValues(){
        DiskOfferingVO diskOfferingVO = new DiskOfferingVO();
        Mockito.doReturn(diskOfferingVO).when(volumeObjectSpy).getDiskOfferingVO();

        diskOfferingVO.setBytesReadRate(1l);
        diskOfferingVO.setBytesReadRateMax(2l);
        diskOfferingVO.setBytesReadRateMaxLength(3l);
        diskOfferingVO.setBytesWriteRate(4l);
        diskOfferingVO.setBytesWriteRateMax(5l);
        diskOfferingVO.setBytesWriteRateMaxLength(6l);
        diskOfferingVO.setIopsReadRate(7l);
        diskOfferingVO.setIopsReadRateMax(8l);
        diskOfferingVO.setIopsReadRateMaxLength(9l);
        diskOfferingVO.setIopsWriteRate(10l);
        diskOfferingVO.setIopsWriteRateMax(11l);
        diskOfferingVO.setIopsWriteRateMaxLength(12l);

        diskOfferingVoMethodsWithLongReturn.forEach(method -> Assert.assertEquals(method.apply(diskOfferingVO), volumeObjectSpy.getLongValueFromDiskOfferingVoMethod(method)));
    }

    @Test
    public void validateGetMapOfEventsDataStoreIsImage(){
        Map<ObjectInDataStoreStateMachine.Event, Volume.Event> expectedResult = new HashMap<>();
        expectedResult.put(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested, Volume.Event.UploadRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.MigrationRequested, Volume.Event.CopyRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.DestroyRequested, Volume.Event.DestroyRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.ExpungeRequested, Volume.Event.ExpungingRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.OperationSuccessed, Volume.Event.OperationSucceeded);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.MigrationCopySucceeded, Volume.Event.MigrationCopySucceeded);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.OperationFailed, Volume.Event.OperationFailed);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.MigrationCopyFailed, Volume.Event.MigrationCopyFailed);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.ResizeRequested, Volume.Event.ResizeRequested);

        Mockito.doReturn(DataStoreRole.Image).when(dataStoreMock).getRole();

        Assert.assertEquals(expectedResult, volumeObjectSpy.getMapOfEvents());
    }

    @Test
    public void validateGetMapOfEventsDataStoreIsNotImage(){
        Map<ObjectInDataStoreStateMachine.Event, Volume.Event> expectedResult = new HashMap<>();
        expectedResult.put(ObjectInDataStoreStateMachine.Event.CreateRequested, Volume.Event.CreateRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested, Volume.Event.CreateRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.CopyingRequested, Volume.Event.CopyRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.MigrationRequested, Volume.Event.MigrationRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.MigrationCopyRequested, Volume.Event.MigrationCopyRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.DestroyRequested, Volume.Event.DestroyRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.ExpungeRequested, Volume.Event.ExpungingRequested);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.OperationSuccessed, Volume.Event.OperationSucceeded);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.MigrationCopySucceeded, Volume.Event.MigrationCopySucceeded);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.OperationFailed, Volume.Event.OperationFailed);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.MigrationCopyFailed, Volume.Event.MigrationCopyFailed);
        expectedResult.put(ObjectInDataStoreStateMachine.Event.ResizeRequested, Volume.Event.ResizeRequested);

        List<DataStoreRole> roles = new LinkedList<>(Arrays.asList(DataStoreRole.values()));
        roles.remove(DataStoreRole.Image);

        roles.forEach(role -> {
            Mockito.doReturn(role).when(dataStoreMock).getRole();
            Assert.assertEquals(expectedResult, volumeObjectSpy.getMapOfEvents());
        });
    }

    @Test
    public void validateUpdateObjectInDataStoreManagerConcurrentOperationExceptionThrowsCloudRuntimeException() throws NoTransitionException{
        Mockito.doThrow(new ConcurrentOperationException("")).when(objectInDataStoreManagerMock).update(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(volumeObjectSpy).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());

        objectInDataStoreStateMachineEvents.forEach(event -> {
            boolean threwException = false;

            try {
                volumeObjectSpy.updateObjectInDataStoreManager(event, true);
            } catch (CloudRuntimeException e) {
                threwException = true;
            }

            Assert.assertTrue(threwException);
        });

        Mockito.verify(volumeObjectSpy, Mockito.times(objectInDataStoreStateMachineEvents.size())).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void validateUpdateObjectInDataStoreManagerNoTransitionExceptionThrowsCloudRuntimeException() throws NoTransitionException{
        Mockito.doThrow(new NoTransitionException("")).when(objectInDataStoreManagerMock).update(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(volumeObjectSpy).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());

        objectInDataStoreStateMachineEvents.forEach(event -> {
            boolean threwException = false;

            try {
                volumeObjectSpy.updateObjectInDataStoreManager(event, true);
            } catch (CloudRuntimeException e) {
                threwException = true;
            }

            Assert.assertTrue(threwException);
        });

        Mockito.verify(volumeObjectSpy, Mockito.times(objectInDataStoreStateMachineEvents.size())).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void validateUpdateObjectInDataStoreManagerThrowsAnyOtherExceptionDoNotCatch() throws NoTransitionException{
        Mockito.doThrow(new RuntimeException("")).when(objectInDataStoreManagerMock).update(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(volumeObjectSpy).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());

        objectInDataStoreStateMachineEvents.forEach(event -> {
            boolean threwCloudRuntimeException = false;

            try {
                volumeObjectSpy.updateObjectInDataStoreManager(event, true);
            } catch (CloudRuntimeException e) {
                threwCloudRuntimeException = true;
            } catch (RuntimeException e) {
            }

            Assert.assertFalse(threwCloudRuntimeException);
        });

        Mockito.verify(volumeObjectSpy, Mockito.times(objectInDataStoreStateMachineEvents.size())).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void validateUpdateObjectInDataStoreManagerUpdateSuccessfully() throws NoTransitionException{
        Mockito.doReturn(true).when(objectInDataStoreManagerMock).update(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(volumeObjectSpy).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());

        objectInDataStoreStateMachineEvents.forEach(event -> {
            boolean threwCloudRuntimeException = false;

            try {
                volumeObjectSpy.updateObjectInDataStoreManager(event, true);
            } catch (RuntimeException e) {
                threwCloudRuntimeException = true;
            }

            Assert.assertFalse(threwCloudRuntimeException);
        });

        Mockito.verify(volumeObjectSpy, Mockito.times(objectInDataStoreStateMachineEvents.size())).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void validateExpungeEntryOnOperationFailedCallExpungeEntryFalse() {
        objectInDataStoreStateMachineEvents.forEach(event -> {
            volumeObjectSpy.expungeEntryOnOperationFailed(event, false);
        });

        Mockito.verify(objectInDataStoreManagerMock, Mockito.never()).deleteIfNotReady(Mockito.any());
    }

    @Test
    public void validateExpungeEntryOnOperationFailedCallExpungeEntryTrue() {
        Mockito.doReturn(true).when(objectInDataStoreManagerMock).deleteIfNotReady(Mockito.any());

        objectInDataStoreStateMachineEvents.forEach(event -> {
            volumeObjectSpy.expungeEntryOnOperationFailed(event, true);
        });

        Mockito.verify(objectInDataStoreManagerMock, Mockito.times(1)).deleteIfNotReady(Mockito.any());
    }

    @Test
    public void validateExpungeEntryOnOperationFailed() {
        Mockito.doNothing().when(volumeObjectSpy).expungeEntryOnOperationFailed(Mockito.any(), Mockito.anyBoolean());

        objectInDataStoreStateMachineEvents.forEach(event -> {
            volumeObjectSpy.expungeEntryOnOperationFailed(event);
            Mockito.verify(volumeObjectSpy, Mockito.times(1)).expungeEntryOnOperationFailed(event, true);
        });

    }

    @Test
    public void validateUpdateRefCountDataStoreNullReturn(){
        volumeObjectSpy.dataStore = null;

        volumeObjectSpy.updateRefCount(true);
        volumeObjectSpy.updateRefCount(false);

        Mockito.verify(volumeDataStoreDaoMock, Mockito.never()).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void validateUpdateRefCountDataStoreIsNotImage(){
        dataStoreRolesExceptImageAndImageCache.forEach(role -> {
            Mockito.doReturn(role).when(dataStoreMock).getRole();
            volumeObjectSpy.updateRefCount(true);
            volumeObjectSpy.updateRefCount(false);
        });

        Mockito.verify(volumeDataStoreDaoMock, Mockito.never()).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void validateUpdateRefCountDataStoreIsImagerOrImageCacheIncreasingCount(){
        Mockito.doReturn(volumeDataStoreVoMock).when(volumeDataStoreDaoMock).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(volumeDataStoreVoMock).incrRefCnt();
        Mockito.doReturn(true).when(volumeDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());

        Arrays.asList(DataStoreRole.Image, DataStoreRole.ImageCache).forEach(role -> {
            Mockito.doReturn(role).when(dataStoreMock).getRole();
            volumeObjectSpy.updateRefCount(true);
        });

        Mockito.verify(volumeDataStoreDaoMock, Mockito.times(2)).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void validateUpdateRefCountDataStoreIsImagerOrImageCacheDecreasingCount(){
        Mockito.doReturn(volumeDataStoreVoMock).when(volumeDataStoreDaoMock).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(volumeDataStoreVoMock).decrRefCnt();
        Mockito.doReturn(true).when(volumeDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());

        Arrays.asList(DataStoreRole.Image, DataStoreRole.ImageCache).forEach(role -> {
            Mockito.doReturn(role).when(dataStoreMock).getRole();
            volumeObjectSpy.updateRefCount(false);
        });

        Mockito.verify(volumeDataStoreDaoMock, Mockito.times(2)).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void validateIsPrimaryDataStore(){
        List<DataStoreRole> dataStoreRoles = Arrays.asList(DataStoreRole.values());
        dataStoreRoles.forEach(dataStoreRole -> {
            boolean expectedResult = dataStoreRole == DataStoreRole.Primary;
            Mockito.doReturn(dataStoreRole).when(dataStoreMock).getRole();

            boolean result = volumeObjectSpy.isPrimaryDataStore();
            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void validateSetVolumeFormatNullFormatAndSetFormatFalseDoNothing(){
        VolumeObjectTO volumeObjectTo = new VolumeObjectTO();
        volumeObjectTo.setFormat(null);

        volumeObjectSpy.setVolumeFormat(volumeObjectTo, false, volumeVoMock);
        Mockito.verifyNoInteractions(volumeVoMock);
    }

    @Test
    public void validateSetVolumeFormatNullFormatAndSetFormatTrueDoNothing(){
        VolumeObjectTO volumeObjectTo = new VolumeObjectTO();
        volumeObjectTo.setFormat(null);

        volumeObjectSpy.setVolumeFormat(volumeObjectTo, true, volumeVoMock);
        Mockito.verifyNoInteractions(volumeVoMock);
    }

    @Test
    public void validateSetVolumeFormatValidFormatAndSetFormatFalseDoNothing(){
        VolumeObjectTO volumeObjectTo = new VolumeObjectTO();
        List<Storage.ImageFormat> storageImageFormats = Arrays.asList(Storage.ImageFormat.values());

        storageImageFormats.forEach(imageFormat -> {
            volumeObjectTo.setFormat(Storage.ImageFormat.QCOW2);

            volumeObjectSpy.setVolumeFormat(volumeObjectTo, false, volumeVoMock);
        });

        Mockito.verifyNoInteractions(volumeVoMock);
    }

    @Test
    public void validateSetVolumeFormatValidFormatAndSetFormatTrueSetFormat(){
        VolumeObjectTO volumeObjectTo = new VolumeObjectTO();
        VolumeVO volumeVo = new VolumeVO() {};
        List<Storage.ImageFormat> storageImageFormats = Arrays.asList(Storage.ImageFormat.values());

        storageImageFormats.forEach(imageFormat -> {
            volumeObjectTo.setFormat(imageFormat);

            volumeObjectSpy.setVolumeFormat(volumeObjectTo, true, volumeVo);
            Assert.assertEquals(imageFormat, volumeVo.getFormat());
        });
    }

    @Test
    public void validateHandleProcessEventAnswerDownloadAnswerIsPrimaryDataStore(){
        Mockito.doReturn(true).when(volumeObjectSpy).isPrimaryDataStore();
        volumeObjectSpy.handleProcessEventAnswer(new DownloadAnswer() {});
        Mockito.verifyNoInteractions(dataStoreMock, volumeDataStoreDaoMock);
    }

    @Test
    public void validateHandleProcessEventAnswerDownloadAnswerIsNotPrimaryDataStore(){
        Mockito.doReturn(false).when(volumeObjectSpy).isPrimaryDataStore();
        Mockito.doReturn(1l).when(volumeObjectSpy).getId();
        Mockito.doReturn(1l).when(dataStoreMock).getId();
        Mockito.doReturn(volumeDataStoreVoMock).when(volumeDataStoreDaoMock).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(true).when(volumeDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());

        volumeObjectSpy.handleProcessEventAnswer(new DownloadAnswer() {});
        Mockito.verify(volumeDataStoreDaoMock, Mockito.times(1)).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(volumeDataStoreDaoMock, Mockito.times(1)).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateUpdateVolumeInfoSetSetVolumeSizeFalseAndVolumeSizeNullDoNotSetVolumeSize(){
        Mockito.doReturn(null).when(volumeObjectToMock).getSize();
        Mockito.doNothing().when(volumeObjectSpy).setVolumeFormat(Mockito.any(), Mockito.anyBoolean(), Mockito.any());
        Mockito.doReturn(true).when(volumeDaoMock).update(Mockito.anyLong(), Mockito.any());

        volumeObjectSpy.updateVolumeInfo(volumeObjectToMock, volumeVoMock, false, false);

        Mockito.verify(volumeVoMock, Mockito.never()).setSize(Mockito.anyLong());
        Mockito.verify(volumeDaoMock, Mockito.times(1)).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateUpdateVolumeInfoSetSetVolumeSizeTrueAndVolumeSizeNullDoNotSetVolumeSize(){
        Mockito.doReturn(null).when(volumeObjectToMock).getSize();
        Mockito.doNothing().when(volumeObjectSpy).setVolumeFormat(Mockito.any(), Mockito.anyBoolean(), Mockito.any());
        Mockito.doReturn(true).when(volumeDaoMock).update(Mockito.anyLong(), Mockito.any());

        volumeObjectSpy.updateVolumeInfo(volumeObjectToMock, volumeVoMock, true, false);

        Mockito.verify(volumeVoMock, Mockito.never()).setSize(Mockito.anyLong());
        Mockito.verify(volumeDaoMock, Mockito.times(1)).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateUpdateVolumeInfoSetSetVolumeSizeFalseAndVolumeSizeNotNullDoNotSetVolumeSize(){
        Mockito.doReturn(1l).when(volumeObjectToMock).getSize();
        Mockito.doNothing().when(volumeObjectSpy).setVolumeFormat(Mockito.any(), Mockito.anyBoolean(), Mockito.any());
        Mockito.doReturn(true).when(volumeDaoMock).update(Mockito.anyLong(), Mockito.any());

        volumeObjectSpy.updateVolumeInfo(volumeObjectToMock, volumeVoMock, false, false);

        Mockito.verify(volumeVoMock, Mockito.never()).setSize(Mockito.anyLong());
        Mockito.verify(volumeDaoMock, Mockito.times(1)).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateUpdateVolumeInfoSetSetVolumeSizeTrueAndVolumeSizeNotNullVolumeSize(){
        Mockito.doReturn(1l).when(volumeObjectToMock).getSize();
        Mockito.doNothing().when(volumeObjectSpy).setVolumeFormat(Mockito.any(), Mockito.anyBoolean(), Mockito.any());
        Mockito.doReturn(true).when(volumeDaoMock).update(Mockito.anyLong(), Mockito.any());

        volumeObjectSpy.updateVolumeInfo(volumeObjectToMock, volumeVoMock, true, false);

        Mockito.verify(volumeVoMock, Mockito.times(1)).setSize(Mockito.anyLong());
        Mockito.verify(volumeDaoMock, Mockito.times(1)).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateHandleProcessEventAnswerCreateObjectAnswerIsNotPrimaryDataStore(){
        Mockito.doReturn(false).when(volumeObjectSpy).isPrimaryDataStore();
        volumeObjectSpy.handleProcessEventAnswer(new CreateObjectAnswer(volumeObjectToMock), false);
        Mockito.verifyNoInteractions(volumeObjectToMock, volumeDaoMock);
    }

    @Test
    public void validateHandleProcessEventAnswerCreateObjectAnswerPrimaryDataStore(){
        Mockito.doReturn(true).when(volumeObjectSpy).isPrimaryDataStore();
        Mockito.doReturn(1l).when(volumeObjectSpy).getId();
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(Mockito.anyLong());
        Mockito.doNothing().when(volumeObjectSpy).updateVolumeInfo(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());

        volumeObjectSpy.handleProcessEventAnswer(new CreateObjectAnswer(volumeObjectToMock), false);

        Mockito.verify(volumeDaoMock, Mockito.times(1)).findById(Mockito.anyLong());
        Mockito.verify(volumeObjectSpy, Mockito.times(1)).updateVolumeInfo(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void validateHandleProcessEventAnswerCreateObjectAnswer(){
        CreateObjectAnswer createObjectAnswer = new CreateObjectAnswer(volumeObjectToMock);
        Mockito.doNothing().when(volumeObjectSpy).handleProcessEventAnswer(Mockito.any(), Mockito.anyBoolean());

        volumeObjectSpy.handleProcessEventAnswer(createObjectAnswer);

        Mockito.verify(volumeObjectSpy, Mockito.times(1)).handleProcessEventAnswer(createObjectAnswer, true);
    }

    @Test
    public void validateHandleProcessEventCopyCmdAnswerNotPrimaryStoreDoNotSetSize(){
        Mockito.doReturn(1l).when(volumeObjectSpy).getId();
        Mockito.doReturn(1l).when(dataStoreMock).getId();
        Mockito.doReturn(volumeDataStoreVoMock).when(volumeDataStoreDaoMock).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(null).when(volumeObjectToMock).getSize();
        Mockito.doReturn(true).when(volumeDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());

        volumeObjectSpy.handleProcessEventAnswer(new CopyCmdAnswer(volumeObjectToMock) {});

        Mockito.verify(volumeDataStoreDaoMock, Mockito.times(1)).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(volumeDataStoreVoMock, Mockito.never()).setSize(Mockito.anyLong());
        Mockito.verify(volumeDataStoreDaoMock, Mockito.times(1)).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateHandleProcessEventCopyCmdAnswerNotPrimaryStoreSetSize(){
        Mockito.doReturn(1l).when(volumeObjectSpy).getId();
        Mockito.doReturn(1l).when(dataStoreMock).getId();
        Mockito.doReturn(volumeDataStoreVoMock).when(volumeDataStoreDaoMock).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(1l).when(volumeObjectToMock).getSize();
        Mockito.doReturn(true).when(volumeDataStoreDaoMock).update(Mockito.anyLong(), Mockito.any());

        volumeObjectSpy.handleProcessEventAnswer(new CopyCmdAnswer(volumeObjectToMock) {});

        Mockito.verify(volumeDataStoreDaoMock, Mockito.times(1)).findByStoreVolume(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(volumeDataStoreVoMock, Mockito.times(1)).setSize(Mockito.anyLong());
        Mockito.verify(volumeDataStoreDaoMock, Mockito.times(1)).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void validateHandleProcessEventCopyCmdAnswerPrimaryStore(){
        Mockito.doReturn(1l).when(volumeObjectSpy).getId();
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(Mockito.anyLong());
        Mockito.doNothing().when(volumeObjectSpy).updateVolumeInfo(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());

        volumeObjectSpy.handleProcessEventCopyCmdAnswerPrimaryStore(volumeObjectToMock, true, true);

        Mockito.verify(volumeDaoMock, Mockito.times(1)).findById(Mockito.anyLong());
        Mockito.verify(volumeObjectSpy, Mockito.times(1)).updateVolumeInfo(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void validateHandleProcessEventAnswerCopyCmdAnswerIsPrimaryStore(){
        Mockito.doReturn(true).when(volumeObjectSpy).isPrimaryDataStore();
        Mockito.doNothing().when(volumeObjectSpy).handleProcessEventCopyCmdAnswerPrimaryStore(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());

        volumeObjectSpy.handleProcessEventAnswer(new CopyCmdAnswer(volumeObjectToMock), true, false);

        Mockito.verify(volumeObjectSpy, Mockito.times(1)).handleProcessEventCopyCmdAnswerPrimaryStore(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(volumeObjectSpy, Mockito.never()).handleProcessEventCopyCmdAnswerNotPrimaryStore(Mockito.any());
    }

    @Test
    public void validateHandleProcessEventAnswerCopyCmdAnswerIsNotPrimaryStore(){
        Mockito.doReturn(false).when(volumeObjectSpy).isPrimaryDataStore();
        Mockito.doNothing().when(volumeObjectSpy).handleProcessEventCopyCmdAnswerNotPrimaryStore(Mockito.any());

        volumeObjectSpy.handleProcessEventAnswer(new CopyCmdAnswer(volumeObjectToMock), true, false);

        Mockito.verify(volumeObjectSpy, Mockito.never()).handleProcessEventCopyCmdAnswerPrimaryStore(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(volumeObjectSpy, Mockito.times(1)).handleProcessEventCopyCmdAnswerNotPrimaryStore(Mockito.any());
    }

    @Test
    public void validateHandleProcessEventAnswerCopyCmdAnswer(){
        CopyCmdAnswer copyCmdAnswer = new CopyCmdAnswer(volumeObjectToMock);
        Mockito.doNothing().when(volumeObjectSpy).handleProcessEventAnswer(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());

        volumeObjectSpy.handleProcessEventAnswer(copyCmdAnswer);

        Mockito.verify(volumeObjectSpy, Mockito.times(1)).handleProcessEventAnswer(copyCmdAnswer, true, true);
    }
}
