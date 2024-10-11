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
package org.apache.cloudstack.storage.datastore.driver;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class StorPoolPrimaryDataStoreDriverTest {

    @Mock
    private VMInstanceDao vmInstanceDao;

    @Mock
    private ResourceTagDao _resourceTagDao;

    @Mock
    private AsyncCompletionCallback<CopyCommandResult> callback;
    @Mock
    private PrimaryDataStoreDao storagePool;
    @Mock
    private StoragePoolDetailsDao detailsDao;
    @Mock
    private VolumeDao volumeDao;

    DataStore srcStore;
    DataStore destStore;

    DataObject srcObj;
    DataObject destObj;

    VolumeObjectTO srcTO;
    VolumeObjectTO dstTO;

    PrimaryDataStoreTO dstPrimaryTo;
    MockedStatic<StorPoolUtil> utilities;
    StorPoolUtil.SpConnectionDesc conn;

    @Before
    public void setUp(){
        utilities = Mockito.mockStatic(StorPoolUtil.class);
        conn = new StorPoolUtil.SpConnectionDesc("1.1.1.1:81", "123", "tmp");

        srcStore = mock(DataStore.class);
        destStore = mock(DataStore.class);

        srcObj = mock(VolumeInfo.class);
        destObj = mock(VolumeInfo.class);

        srcTO = mock(VolumeObjectTO.class);
        dstTO = mock(VolumeObjectTO.class);

        dstPrimaryTo = mock(PrimaryDataStoreTO.class);
    }

    @After
    public void tearDown(){
        utilities.close();
    }
    @InjectMocks
    private StorPoolPrimaryDataStoreDriver storPoolPrimaryDataStoreDriver;

    @Test
    public void testMigrateVolumePassed(){


        VMInstanceVO vm = mock(VMInstanceVO.class);
        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, vm);
        when(vm.getState()).thenReturn(State.Running);

        when(StorPoolUtil.getSpConnection(destObj.getDataStore().getUuid(), destObj.getDataStore().getId(), detailsDao, storagePool)).thenReturn(conn);
        StorPoolUtil.SpApiResponse resp = new StorPoolUtil.SpApiResponse();
        when(StorPoolUtil.volumeUpdateTemplate("~t.t.t", conn)).thenReturn(resp);

        when(volumeDao.findById(srcObj.getId())).thenReturn(mock(VolumeVO.class));
        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
        utilities.verify(() -> StorPoolUtil.volumeUpdateTemplate("~t.t.t", conn), times(1));
        Assert.assertNull(resp.getError());
    }
    @Test
    public void testMigrateVolumeNotPassed() {

        VMInstanceVO vm = mock(VMInstanceVO.class);

        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, vm);
        when(vm.getState()).thenReturn(State.Running);

        when(StorPoolUtil.getSpConnection(destObj.getDataStore().getUuid(), destObj.getDataStore().getId(), detailsDao, storagePool)).thenReturn(conn);
        StorPoolUtil.SpApiResponse resp = new StorPoolUtil.SpApiResponse();
        setResponseError(resp);
        when(StorPoolUtil.volumeUpdateTemplate("~t.t.t", conn)).thenReturn(resp);
        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
        Assert.assertNotNull(resp.getError());
    }

    @Test
    public void testCopyVolumeAttachedToVmPassed() {

        VMInstanceVO vm = mock(VMInstanceVO.class);

        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, vm);
        when(vm.getState()).thenReturn(State.Stopped);
        String vmUuid = UUID.randomUUID().toString();
        when(vm.getUuid()).thenReturn(vmUuid);

        when(StorPoolUtil.getSpConnection(destObj.getDataStore().getUuid(), destObj.getDataStore().getId(), detailsDao, storagePool)).thenReturn(conn);

        StorPoolUtil.SpApiResponse response = new StorPoolUtil.SpApiResponse();
        String volumeUuid = UUID.randomUUID().toString();
        when(srcObj.getUuid()).thenReturn(volumeUuid);
        when(StorPoolUtil.volumeCopy(volumeUuid, "~t.t.t", "volume", null, vmUuid, "", conn)).thenReturn(response);
        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
        Assert.assertNull(response.getError());
    }

    @Test
    public void testCopyVolumeAttachedToVmNotPassed() {

        VMInstanceVO vm = mock(VMInstanceVO.class);

        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, vm);
        when(vm.getState()).thenReturn(State.Stopped);
        String vmUuid = UUID.randomUUID().toString();
        when(vm.getUuid()).thenReturn(vmUuid);

        when(StorPoolUtil.getSpConnection(destObj.getDataStore().getUuid(), destObj.getDataStore().getId(), detailsDao, storagePool)).thenReturn(conn);
        StorPoolUtil.SpApiResponse response = new StorPoolUtil.SpApiResponse();
        setResponseError(response);
        String volumeUuid = UUID.randomUUID().toString();
        when(srcObj.getUuid()).thenReturn(volumeUuid);
        when(StorPoolUtil.volumeCopy(volumeUuid, "~t.t.t", "volume", null, vmUuid, "", conn)).thenReturn(response);
        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
        Assert.assertNotNull(response.getError());
    }
    @Test
    public void testCopyVolumeNotAttachedToVmNotPassed() {
        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, null);

        when(StorPoolUtil.getSpConnection(destObj.getDataStore().getUuid(), destObj.getDataStore().getId(), detailsDao, storagePool)).thenReturn(conn);
        StorPoolUtil.SpApiResponse response = new StorPoolUtil.SpApiResponse();
        setResponseError(response);
        String volumeUuid = UUID.randomUUID().toString();
        when(srcObj.getUuid()).thenReturn(volumeUuid);
        when(StorPoolUtil.volumeCopy(volumeUuid, "~t.t.t", "volume", null, null, null, conn)).thenReturn(response);
        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
        utilities.verify(() -> StorPoolUtil.volumeCopy(volumeUuid, "~t.t.t", "volume", null, null, null, conn), times(1));

        Assert.assertNotNull(response.getError());
    }

    @Test
    public void testCopyVolumeNotAttachedToVmPassed() {

        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, null);

        when(StorPoolUtil.getSpConnection(destObj.getDataStore().getUuid(), destObj.getDataStore().getId(), detailsDao, storagePool)).thenReturn(conn);
        StorPoolUtil.SpApiResponse response = new StorPoolUtil.SpApiResponse();
        String volumeUuid = UUID.randomUUID().toString();
        when(srcObj.getUuid()).thenReturn(volumeUuid);
        when(StorPoolUtil.volumeCopy(volumeUuid, "~t.t.t", "volume", null, null, null, conn)).thenReturn(response);
        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
        utilities.verify(() -> StorPoolUtil.volumeCopy(volumeUuid, "~t.t.t", "volume", null, null, null, conn), times(1));
        Assert.assertNull(response.getError());
    }

    private void setReturnsWhenSourceAndDestinationAreVolumes(DataStore srcStore, DataStore destStore, DataObject srcObj, DataObject destObj, VolumeObjectTO srcTO, VolumeObjectTO dstTO, PrimaryDataStoreTO dstPrimaryTo, VMInstanceVO vm) {
        when(srcStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(destStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(srcObj.getDataStore()).thenReturn(srcStore);
        when(destObj.getDataStore()).thenReturn(destStore);
        when(srcObj.getType()).thenReturn(DataObjectType.VOLUME);
        when(destObj.getType()).thenReturn(DataObjectType.VOLUME);
        when(destObj.getTO()).thenReturn(dstTO);
        when(srcObj.getTO()).thenReturn(srcTO);

        when(srcObj.getDataStore().getDriver()).thenReturn(storPoolPrimaryDataStoreDriver);
        when(destObj.getTO().getDataStore()).thenReturn(dstPrimaryTo);
        when(destObj.getDataStore().getUuid()).thenReturn("SP_API_HTTP=1.1.1.1:81;SP_AUTH_TOKEN=token;SP_TEMPLATE=template_name");
        when(destObj.getDataStore().getId()).thenReturn(1L);

        when(srcTO.getPath()).thenReturn("/dev/storpool-byid/t.t.t");
        when(dstPrimaryTo.getPoolType()).thenReturn(StoragePoolType.StorPool);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vm);
    }

    private static void setResponseError(StorPoolUtil.SpApiResponse resp) {
        StorPoolUtil.SpApiError respErr = new StorPoolUtil.SpApiError();
        respErr.setName("error");
        respErr.setDescr("Failed");
        resp.setError(respErr);
    }
}
