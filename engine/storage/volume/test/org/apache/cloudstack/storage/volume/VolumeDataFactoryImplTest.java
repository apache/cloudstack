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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.DataStoreManagerImpl;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.datastore.manager.PrimaryDataStoreProviderManagerImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.storage.StorageManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import org.mockito.internal.util.reflection.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ComponentContext.class, PrimaryDataStoreImpl.class })
public class VolumeDataFactoryImplTest {
    //PrimaryDataStoreProviderManager mocks
    @Mock
    PrimaryDataStoreDao _dsDao;

    @Mock
    DataStoreProviderManager _providerMgr;

    @Mock
    StorageManager _storageMgr;

    @InjectMocks
    @Spy
    PrimaryDataStoreProviderManagerImpl primaryStoreMgr = new PrimaryDataStoreProviderManagerImpl();

    //DataStoreManager mocks
    @Mock
    VolumeDataStoreDao _volumeDataStoreDao;

    @Mock
    VolumeDao _volDao;

    @InjectMocks
    @Spy
    DataStoreManagerImpl _dsManagerImpl = new DataStoreManagerImpl();

    //The final mock
    @InjectMocks
    VolumeDataFactoryImpl _volumeDataFactory;

    /**
     * Set up the mocks. The variable parameters are used to set up different scenarios for testing,
     * such as what happens when a pool cannot be found. For example, the volume reports a different pool ID
     * than the one specified, that means the code will not be able to find a pool.
     * @param volumePoolId - The pool ID reported by the volume.
     * @param poolId - The ID of the pool that actually exists in the scenario.
     */
    public void setupMocks(Long volumePoolId, Long poolId) {
        //Common data stuff
        String providerName = "TestProviderName";

        //PrimaryDataStoreProviderManagerImpl mocks
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.getId()).thenReturn(poolId);
        when(pool.getStorageProviderName()).thenReturn(providerName);
        when(_dsDao.findById(eq(poolId))).thenReturn(pool);

        DataStoreProvider provider = mock(DataStoreProvider.class);
        when(provider.getName()).thenReturn(providerName);
        when(_providerMgr.getDataStoreProvider(eq(providerName))).thenReturn(provider);

        @SuppressWarnings("unchecked")
        Map<String, PrimaryDataStoreDriver> driverMaps = mock(Map.class);
        PrimaryDataStoreDriver driver = mock(PrimaryDataStoreDriver.class);
        when(driverMaps.get(providerName)).thenReturn(driver);
        Whitebox.setInternalState(primaryStoreMgr, "driverMaps", driverMaps);

        PowerMockito.mockStatic(PrimaryDataStoreImpl.class);
        PrimaryDataStoreImpl dsImpl = mock(PrimaryDataStoreImpl.class);
        when(PrimaryDataStoreImpl.createDataStore(any(StoragePoolVO.class), any(PrimaryDataStoreDriver.class), eq(provider)))
            .thenReturn(dsImpl);

        //DataStoreManager mocks
        VolumeDataStoreVO volDataStore = mock(VolumeDataStoreVO.class);
        when(volDataStore.getDataStoreId()).thenReturn(1l);

        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getId()).thenReturn(1l);
        when(vol.getPoolId()).thenReturn(volumePoolId);
        when(_volDao.findByIdIncludingRemoved(anyLong())).thenReturn(vol);

        PowerMockito.mockStatic(ComponentContext.class);
        VolumeObject volObj = spy(new VolumeObject());
        when(ComponentContext.inject(VolumeObject.class)).thenReturn(volObj);

        when(_volumeDataStoreDao.findByVolume(anyLong())).thenReturn(volDataStore);
    }

    /**
     * Verify that a volume is returned from getVolumeForExpunge and that it has a
     * data store attached to it, since the pool reports ID 1, and pool ID 1 exists.
     */
    @Test
    public void testGetVolumeForExpungeWithStoragePool() {
        setupMocks(1l, 1l);
        VolumeInfo volInfo = _volumeDataFactory.getVolumeForExpunge(1);
        Assert.assertNotNull(volInfo);
        Assert.assertNotNull(volInfo.getDataStore());
    }

    /**
     * Verify that a volume is returned from getVolumeForExpunge and that it does
     * not have a data store attached to it, since the pool ID is 2 and the volume reports ID 1.
     */
    @Test
    public void testGetVolumeForExpungeWithoutStoragePool() {
        setupMocks(1l, 2l);
        VolumeInfo volInfo = _volumeDataFactory.getVolumeForExpunge(1);
        Assert.assertNotNull(volInfo);
        Assert.assertNull(volInfo.getDataStore());
    }

    /**
     * Verify that an exception is thrown when the pool doesn't exist and the regular
     * getVolume method is called.
     */
    @Test(expected = CloudRuntimeException.class)
    public void testGetVolumeWithoutStoragePool() {
        setupMocks(1l, 2l);
        _volumeDataFactory.getVolume(1l);
    }

    /**
     * Verify that a volume is returned from regular getVolume call when the storage pool
     * exists.
     */
    @Test
    public void testGetVolumeWithStoragePool() {
        setupMocks(1l, 1l);
        VolumeInfo volInfo = _volumeDataFactory.getVolume(1l);
        Assert.assertNotNull(volInfo);
        Assert.assertNotNull(volInfo.getDataStore());
    }
}
