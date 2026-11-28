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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapter;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterContext;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDataObject;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.provider.AdaptivePrimaryDatastoreAdapterFactoryMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.domain.dao.DomainDao;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AdaptiveDataStoreDriverImplDeleteTest {

    private static final long OBJECT_ID = 206L;
    private static final long ACCOUNT_ID = 2L;

    private AdaptiveDataStoreDriverImpl driver;
    private VMTemplateDao templateDao;
    private VolumeDao volumeDao;
    private SnapshotDao snapshotDao;

    @Before
    public void setUp() {
        driver = spy(new AdaptiveDataStoreDriverImpl(mock(AdaptivePrimaryDatastoreAdapterFactoryMap.class)));
        templateDao = mock(VMTemplateDao.class);
        volumeDao = mock(VolumeDao.class);
        snapshotDao = mock(SnapshotDao.class);
        driver._vmTemplateDao = templateDao;
        driver._volumeDao = volumeDao;
        driver._snapshotDao = snapshotDao;
        driver._accountDao = mock(AccountDao.class);
        driver._domainDao = mock(DomainDao.class);
    }

    private TemplateInfo removedTemplate() {
        TemplateInfo template = mock(TemplateInfo.class);
        when(template.getId()).thenReturn(OBJECT_ID);
        VMTemplateVO templateVO = mock(VMTemplateVO.class);
        when(templateVO.getAccountId()).thenReturn(ACCOUNT_ID);
        when(templateDao.findById(OBJECT_ID)).thenReturn(null);
        when(templateDao.findByIdIncludingRemoved(OBJECT_ID)).thenReturn(templateVO);
        return template;
    }

    @Test
    public void contextForRemovedTemplateKeepsItsAccount() {
        ProviderAdapterContext ctx = driver.newManagedVolumeContext(removedTemplate());

        Assert.assertEquals(Long.valueOf(ACCOUNT_ID), ctx.getAccountId());
        Assert.assertEquals(Long.valueOf(0L), ctx.getDomainId());
    }

    @Test
    public void contextForMissingRecordsDoesNotCrash() {
        TemplateInfo template = mock(TemplateInfo.class);
        when(template.getId()).thenReturn(OBJECT_ID);
        VolumeInfo volume = mock(VolumeInfo.class);
        when(volume.getId()).thenReturn(OBJECT_ID);
        SnapshotInfo snapshot = mock(SnapshotInfo.class);
        when(snapshot.getId()).thenReturn(OBJECT_ID);

        Assert.assertNull(driver.newManagedVolumeContext(template).getAccountId());
        Assert.assertNull(driver.newManagedVolumeContext(volume).getAccountId());
        Assert.assertNull(driver.newManagedVolumeContext(snapshot).getAccountId());
    }

    @Test
    public void contextForRemovedVolumeAndSnapshotKeepsTheirAccount() {
        VolumeInfo volume = mock(VolumeInfo.class);
        when(volume.getId()).thenReturn(OBJECT_ID);
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getAccountId()).thenReturn(ACCOUNT_ID);
        when(volumeDao.findByIdIncludingRemoved(OBJECT_ID)).thenReturn(volumeVO);

        SnapshotInfo snapshot = mock(SnapshotInfo.class);
        when(snapshot.getId()).thenReturn(OBJECT_ID);
        SnapshotVO snapshotVO = mock(SnapshotVO.class);
        when(snapshotVO.getAccountId()).thenReturn(ACCOUNT_ID);
        when(snapshotDao.findByIdIncludingRemoved(OBJECT_ID)).thenReturn(snapshotVO);

        Assert.assertEquals(Long.valueOf(ACCOUNT_ID), driver.newManagedVolumeContext(volume).getAccountId());
        Assert.assertEquals(Long.valueOf(ACCOUNT_ID), driver.newManagedVolumeContext(snapshot).getAccountId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deleteOfRemovedTemplateReachesTheArray() {
        TemplateInfo template = removedTemplate();
        DataStore store = mock(DataStore.class);
        when(store.getId()).thenReturn(5L);

        PrimaryDataStoreDao poolDao = mock(PrimaryDataStoreDao.class);
        driver._storagePoolDao = poolDao;
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.getId()).thenReturn(5L);
        when(poolDao.findById(5L)).thenReturn(pool);
        when(poolDao.getDetails(anyLong())).thenReturn(new HashMap<>());

        ProviderAdapter api = mock(ProviderAdapter.class);
        doReturn(api).when(driver).getAPI(eq(pool), any());
        ProviderAdapterDataObject inData = new ProviderAdapterDataObject();
        inData.setExternalName("pure-fc-neg::tpl-5-0-2-206");
        doReturn(inData).when(driver).newManagedDataObject(template, pool);

        AsyncCompletionCallback<CommandResult> callback = mock(AsyncCompletionCallback.class);
        driver.deleteAsync(store, template, callback);

        verify(api).delete(any(), eq(inData));
        ArgumentCaptor<CommandResult> captor = ArgumentCaptor.forClass(CommandResult.class);
        verify(callback).complete(captor.capture());
        Assert.assertTrue(captor.getValue().isSuccess());
    }
}
