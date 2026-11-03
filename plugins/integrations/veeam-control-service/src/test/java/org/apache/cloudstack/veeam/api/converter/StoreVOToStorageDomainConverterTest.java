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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.junit.Test;

import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolStatus;

public class StoreVOToStorageDomainConverterTest {

    @Test
    public void testToStorageDomain_FromPrimaryPool() {
        final StoragePoolJoinVO pool = mock(StoragePoolJoinVO.class);
        when(pool.getUuid()).thenReturn("pool-1");
        when(pool.getName()).thenReturn("Primary-1");
        when(pool.getCapacityBytes()).thenReturn(1000L);
        when(pool.getUsedBytes()).thenReturn(250L);
        when(pool.getStatus()).thenReturn(StoragePoolStatus.Up);
        when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(pool.getZoneUuid()).thenReturn("dc-1");

        final StorageDomain sd = StoreVOToStorageDomainConverter.toStorageDomain(pool);

        assertEquals("pool-1", sd.getId());
        assertEquals("data", sd.getType());
        assertEquals("active", sd.getStatus());
        assertEquals("750", sd.getAvailable());
        assertEquals("250", sd.getUsed());
        assertEquals("1000", sd.getCommitted());
        assertEquals("nfs", sd.getStorage().getType());
        assertEquals("dc-1", sd.getDataCenters().getItems().get(0).getId());
        assertTrue(sd.getLink().stream().anyMatch(l -> "disks".equals(l.getRel())));
    }

    @Test
    public void testToStorageDomain_FromImageStore() {
        final ImageStoreJoinVO store = mock(ImageStoreJoinVO.class);
        when(store.getUuid()).thenReturn("img-1");
        when(store.getName()).thenReturn("Secondary-1");
        when(store.getProviderName()).thenReturn("glance");
        when(store.getZoneUuid()).thenReturn("dc-2");

        final StorageDomain sd = StoreVOToStorageDomainConverter.toStorageDomain(store);

        assertEquals("img-1", sd.getId());
        assertEquals("image", sd.getType());
        assertEquals("unattached", sd.getStatus());
        assertEquals("glance", sd.getStorage().getType());
        assertEquals("dc-2", sd.getDataCenters().getItems().get(0).getId());
        assertTrue(sd.getLink().stream().anyMatch(l -> "images".equals(l.getRel())));
    }

    @Test
    public void testListConverters() {
        final StoragePoolJoinVO pool = mock(StoragePoolJoinVO.class);
        when(pool.getUuid()).thenReturn("p");
        when(pool.getName()).thenReturn("P");
        when(pool.getCapacityBytes()).thenReturn(10L);
        when(pool.getUsedBytes()).thenReturn(2L);
        when(pool.getStatus()).thenReturn(StoragePoolStatus.Up);
        when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.Filesystem);
        when(pool.getZoneUuid()).thenReturn("z");

        final ImageStoreJoinVO store = mock(ImageStoreJoinVO.class);
        when(store.getUuid()).thenReturn("s");
        when(store.getName()).thenReturn("S");
        when(store.getProviderName()).thenReturn("glance");
        when(store.getZoneUuid()).thenReturn("z");

        assertEquals(1, StoreVOToStorageDomainConverter.toStorageDomainListFromPools(List.of(pool)).size());
        assertEquals(1, StoreVOToStorageDomainConverter.toStorageDomainListFromStores(List.of(store)).size());
    }
}
