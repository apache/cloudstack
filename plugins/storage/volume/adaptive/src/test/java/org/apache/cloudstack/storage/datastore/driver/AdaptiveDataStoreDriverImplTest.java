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
package org.apache.cloudstack.storage.datastore.driver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapter;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterContext;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDataObject;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolume;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.provider.AdaptivePrimaryDatastoreAdapterFactoryMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AdaptiveDataStoreDriverImplTest {

    private static final long SRC_POOL_ID = 3L;
    private static final long DEST_POOL_ID = 4L;

    private AdaptiveDataStoreDriverImpl driver;
    private PrimaryDataStoreDao storagePoolDao;

    private StoragePoolVO srcPool;
    private StoragePoolVO destPool;
    private VolumeInfo srcVolume;
    private VolumeInfo destVolume;
    private ProviderAdapter srcApi;
    private ProviderAdapter destApi;
    private ProviderAdapterDataObject srcObject;
    private ProviderAdapterDataObject destObject;
    private Map<String, String> destDetails;

    @Before
    public void setUp() {
        driver = spy(new AdaptiveDataStoreDriverImpl(mock(AdaptivePrimaryDatastoreAdapterFactoryMap.class)));
        storagePoolDao = mock(PrimaryDataStoreDao.class);
        driver._storagePoolDao = storagePoolDao;

        srcPool = mock(StoragePoolVO.class);
        destPool = mock(StoragePoolVO.class);
        when(storagePoolDao.findById(SRC_POOL_ID)).thenReturn(srcPool);
        when(storagePoolDao.findById(DEST_POOL_ID)).thenReturn(destPool);
        when(destPool.getId()).thenReturn(DEST_POOL_ID);

        Map<String, String> srcDetails = new HashMap<>();
        srcDetails.put("pod", "pod-a");
        destDetails = new HashMap<>();
        destDetails.put("pod", "pod-b");
        when(storagePoolDao.getDetails(SRC_POOL_ID)).thenReturn(srcDetails);
        when(storagePoolDao.getDetails(DEST_POOL_ID)).thenReturn(destDetails);

        srcVolume = volumeOn(SRC_POOL_ID);
        destVolume = volumeOn(DEST_POOL_ID);
        when(destVolume.getSize()).thenReturn(20L * 1024 * 1024 * 1024);

        srcApi = mock(ProviderAdapter.class);
        destApi = mock(ProviderAdapter.class);
        doReturn(srcApi).when(driver).getAPI(eq(srcPool), any());
        doReturn(destApi).when(driver).getAPI(eq(destPool), any());

        srcObject = new ProviderAdapterDataObject();
        destObject = new ProviderAdapterDataObject();
        doReturn(srcObject).when(driver).newManagedDataObject(srcVolume, srcPool);
        doReturn(destObject).when(driver).newManagedDataObject(destVolume, destPool);
        doReturn(new ProviderAdapterContext()).when(driver).newManagedVolumeContext(destVolume);
        doReturn(true).when(driver).canCopy(srcVolume, destVolume);
        doReturn("path").when(driver).generatePathInfo(any(), any());
    }

    private VolumeInfo volumeOn(long poolId) {
        DataStore store = mock(DataStore.class);
        when(store.getId()).thenReturn(poolId);
        VolumeInfo volume = mock(VolumeInfo.class);
        when(volume.getDataStore()).thenReturn(store);
        return volume;
    }

    private ProviderVolume copiedVolume() {
        ProviderVolume outVolume = mock(ProviderVolume.class);
        when(outVolume.getAllocatedSizeInBytes()).thenReturn(20L * 1024 * 1024 * 1024);
        return outVolume;
    }

    @SuppressWarnings("unchecked")
    private CopyCommandResult runCopy() {
        AsyncCompletionCallback<CopyCommandResult> callback = mock(AsyncCompletionCallback.class);
        driver.copyAsync(srcVolume, destVolume, callback);
        ArgumentCaptor<CopyCommandResult> captor = ArgumentCaptor.forClass(CopyCommandResult.class);
        verify(callback).complete(captor.capture());
        return captor.getValue();
    }

    @Test
    public void copyAsyncCreatesVolumeThroughDestinationPool() {
        ProviderVolume outVolume = copiedVolume();
        when(destApi.copy(any(), eq(srcObject), eq(destObject), anyLong())).thenReturn(outVolume);
        doNothing().when(driver).persistVolumeData(any(), any(), any(), any(), any());

        CopyCommandResult result = runCopy();

        Assert.assertTrue(result.isSuccess());
        verify(destApi).copy(any(), eq(srcObject), eq(destObject), anyLong());
        verify(srcApi, never()).copy(any(), any(), any(), anyLong());
        verify(driver).persistVolumeData(eq(destPool), eq(destDetails), eq(destVolume), eq(outVolume), eq(null));
    }

    @Test
    public void copyAsyncFailsWhenDestinationCannotReachSource() {
        when(destApi.copy(any(), eq(srcObject), eq(destObject), anyLong()))
                .thenThrow(new RuntimeException("Unable to find current volume to copy from"));

        CopyCommandResult result = runCopy();

        Assert.assertFalse(result.isSuccess());
        verify(srcApi, never()).copy(any(), any(), any(), anyLong());
        verify(driver, never()).persistVolumeData(any(), any(), any(), any(), any());
    }
}
