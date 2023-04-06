//
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
//

package org.apache.cloudstack.storage.datastore.driver.client;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.driver.ScaleIOPrimaryDataStoreDriver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOPrimaryDataStoreDriverTest {

    @Spy
    @InjectMocks
    ScaleIOPrimaryDataStoreDriver scaleIOPrimaryDataStoreDriver;

    @Mock
    StoragePoolDetailsDao storagePoolDetailsDao;

    @Test
    public void testSameScaleIOStorageInstance() {
        DataStore srcStore = Mockito.mock(DataStore.class);
        DataStore destStore = Mockito.mock(DataStore.class);
        when(srcStore.getId()).thenReturn(1L);
        when(destStore.getId()).thenReturn(2L);

        StoragePoolDetailVO srcPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String srcPoolSystemId = "610204d03e3ad60f";
        when(srcPoolSystemIdDetail.getValue()).thenReturn(srcPoolSystemId);

        StoragePoolDetailVO destPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String destPoolSystemId = "610204d03e3ad60f";
        when(destPoolSystemIdDetail.getValue()).thenReturn(destPoolSystemId);

        when(storagePoolDetailsDao.findDetail(1L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(srcPoolSystemIdDetail);
        when(storagePoolDetailsDao.findDetail(2L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(destPoolSystemIdDetail);

        boolean result = scaleIOPrimaryDataStoreDriver.isSameScaleIOStorageInstance(srcStore, destStore);

        Assert.assertTrue(result);
    }

    @Test
    public void testDifferentScaleIOStorageInstance() {
        DataStore srcStore = Mockito.mock(DataStore.class);
        DataStore destStore = Mockito.mock(DataStore.class);
        when(srcStore.getId()).thenReturn(1L);
        when(destStore.getId()).thenReturn(2L);

        StoragePoolDetailVO srcPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String srcPoolSystemId = "610204d03e3ad60f";
        when(srcPoolSystemIdDetail.getValue()).thenReturn(srcPoolSystemId);

        StoragePoolDetailVO destPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String destPoolSystemId = "7332760565f6340f";
        when(destPoolSystemIdDetail.getValue()).thenReturn(destPoolSystemId);

        when(storagePoolDetailsDao.findDetail(1L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(srcPoolSystemIdDetail);
        when(storagePoolDetailsDao.findDetail(2L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(destPoolSystemIdDetail);

        boolean result = scaleIOPrimaryDataStoreDriver.isSameScaleIOStorageInstance(srcStore, destStore);

        Assert.assertFalse(result);
    }

    @Test (expected = CloudRuntimeException.class)
    public void testCheckVolumeOnDifferentScaleIOStorageInstanceSystemIdShouldNotBeNull() {
        DataStore srcStore = Mockito.mock(DataStore.class);
        DataStore destStore = Mockito.mock(DataStore.class);
        when(srcStore.getId()).thenReturn(1L);
        when(destStore.getId()).thenReturn(2L);

        StoragePoolDetailVO srcPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String srcPoolSystemId = "610204d03e3ad60f";
        when(srcPoolSystemIdDetail.getValue()).thenReturn(srcPoolSystemId);

        StoragePoolDetailVO destPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        when(destPoolSystemIdDetail.getValue()).thenReturn(null);

        when(storagePoolDetailsDao.findDetail(1L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(srcPoolSystemIdDetail);
        when(storagePoolDetailsDao.findDetail(2L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(destPoolSystemIdDetail);

        scaleIOPrimaryDataStoreDriver.isSameScaleIOStorageInstance(srcStore, destStore);
    }
}