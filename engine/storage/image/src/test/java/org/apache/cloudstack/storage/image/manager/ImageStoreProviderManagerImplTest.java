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
package org.apache.cloudstack.storage.image.manager;

import com.cloud.server.StatsCollector;
import com.cloud.utils.Pair;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ImageStoreProviderManagerImplTest {

    @Mock
    ImageStoreDao imageStoreDao;

    @Mock
    StatsCollector statsCollectorMock;

    @InjectMocks
    ImageStoreProviderManagerImpl imageStoreProviderManager = new ImageStoreProviderManagerImpl();

    @Test
    public void testGetImageStoreZoneId() {
        final long storeId = 1L;
        final long zoneId = 1L;
        ImageStoreVO imageStoreVO = Mockito.mock(ImageStoreVO.class);
        Mockito.when(imageStoreVO.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(imageStoreDao.findById(storeId)).thenReturn(imageStoreVO);
        long value = imageStoreProviderManager.getImageStoreZoneId(storeId);
        Assert.assertEquals(zoneId, value);
    }

    private Pair<List<DataStore>, List<DataStore>> prepareUnorderedAndOrderedImageStoresForCapacityTests(boolean hasStoragesWithEnoughCapacity) {
        DataStore store1 = Mockito.mock(DataStore.class);
        Mockito.doReturn(100L).when(statsCollectorMock).imageStoreCurrentFreeCapacity(store1);
        Mockito.doReturn(false).when(statsCollectorMock).imageStoreHasEnoughCapacity(store1);
        DataStore store2 = Mockito.mock(DataStore.class);
        Mockito.doReturn(200L).when(statsCollectorMock).imageStoreCurrentFreeCapacity(store2);
        Mockito.doReturn(hasStoragesWithEnoughCapacity).when(statsCollectorMock).imageStoreHasEnoughCapacity(store2);
        DataStore store3 = Mockito.mock(DataStore.class);
        Mockito.doReturn(300L).when(statsCollectorMock).imageStoreCurrentFreeCapacity(store3);
        Mockito.doReturn(hasStoragesWithEnoughCapacity).when(statsCollectorMock).imageStoreHasEnoughCapacity(store3);
        DataStore store4 = Mockito.mock(DataStore.class);
        Mockito.doReturn(400L).when(statsCollectorMock).imageStoreCurrentFreeCapacity(store4);
        Mockito.doReturn(false).when(statsCollectorMock).imageStoreHasEnoughCapacity(store4);

        List<DataStore> unordered = Arrays.asList(store1, store2, store3, store4);
        List<DataStore> orderedAndEnoughCapacity = new ArrayList<>();
        if (hasStoragesWithEnoughCapacity) {
            orderedAndEnoughCapacity.add(store3);
            orderedAndEnoughCapacity.add(store2);
        }

        return new Pair<>(unordered, orderedAndEnoughCapacity);
    }

    @Test
    public void getImageStoreWithFreeCapacityTestImageStoresWithEnoughCapacityExistReturnsImageStoreWithMostFreeCapacity() {
        Pair<List<DataStore>, List<DataStore>> unorderedAndOrdered = prepareUnorderedAndOrderedImageStoresForCapacityTests(true);

        DataStore result = imageStoreProviderManager.getImageStoreWithFreeCapacity(unorderedAndOrdered.first());

        Assert.assertEquals(unorderedAndOrdered.second().get(0), result);
    }

    @Test
    public void getImageStoreWithFreeCapacityTestImageStoresWithEnoughCapacityDoNotExistReturnsNull() {
        Pair<List<DataStore>, List<DataStore>> unorderedAndOrdered = prepareUnorderedAndOrderedImageStoresForCapacityTests(false);

        DataStore result = imageStoreProviderManager.getImageStoreWithFreeCapacity(unorderedAndOrdered.first());

        Assert.assertNull(result);
    }

    @Test
    public void orderImageStoresOnFreeCapacityTestReturnsImageStoresOrderedFromMostToLeast() {
        Pair<List<DataStore>, List<DataStore>> unorderedAndOrdered = prepareUnorderedAndOrderedImageStoresForCapacityTests(true);

        List<DataStore> result = imageStoreProviderManager.orderImageStoresOnFreeCapacity(unorderedAndOrdered.first());

        Assert.assertEquals(unorderedAndOrdered.second(), result);
    }

}
