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
package com.cloud.api.query.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class StoragePoolJoinDaoImplTest {

    @Spy
    @InjectMocks
    private StoragePoolJoinDaoImpl storagePoolJoinDao = new StoragePoolJoinDaoImpl();

    @Mock
    private SearchCriteria<StoragePoolJoinVO> searchCriteria;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        StoragePoolJoinVO storagePoolJoinVO = mock(StoragePoolJoinVO.class);
        SearchBuilder<StoragePoolJoinVO> searchBuilder = mock(SearchBuilder.class);
        when(searchBuilder.entity()).thenReturn(storagePoolJoinVO);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        doReturn(searchBuilder).when(storagePoolJoinDao).createSearchBuilder();
    }

    private StoragePoolJoinVO mockPoolRow(long id) {
        StoragePoolJoinVO row = mock(StoragePoolJoinVO.class);
        when(row.getId()).thenReturn(id);
        return row;
    }

    @Test
    public void listAvailableByZonesHypervisorAndTypeReturnsMatchingPoolsWhenTypesAreProvided() {
        List<Long> zoneIds = Arrays.asList(42L, 43L);
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        List<Storage.StoragePoolType> types = Arrays.asList(Storage.StoragePoolType.NetworkFilesystem, Storage.StoragePoolType.Filesystem);
        Filter filter = mock(Filter.class);
        StoragePoolJoinVO pool = mockPoolRow(5L);

        doReturn(Collections.singletonList(mockPoolRow(5L))).when(storagePoolJoinDao).listBy(searchCriteria, filter);
        doReturn(Collections.singletonList(pool)).when(storagePoolJoinDao).searchByIds(5L);

        List<StoragePoolJoinVO> result = storagePoolJoinDao.listAvailableByZonesHypervisorAndType(zoneIds, hypervisorType, types, "data%", filter);

        assertEquals(Collections.singletonList(pool), result);
        verify(searchCriteria).setParameters(eq("zoneIds"), aryEq(zoneIds.toArray()));
        verify(searchCriteria).setParameters(eq("hypervisors"), aryEq(new Object[]{Hypervisor.HypervisorType.Any, hypervisorType}));
        verify(searchCriteria).setParameters(eq("types"), aryEq(types.toArray()));
        verify(searchCriteria).setParameters("name", "data%");
        verify(storagePoolJoinDao).listBy(searchCriteria, filter);
    }

    @Test
    public void listAvailableByZonesHypervisorAndTypeSkipsTypeFilterWhenTypesAreNull() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.VMware;
        Filter filter = mock(Filter.class);

        doReturn(Collections.emptyList()).when(storagePoolJoinDao).listBy(searchCriteria, filter);

        List<StoragePoolJoinVO> result = storagePoolJoinDao.listAvailableByZonesHypervisorAndType(Collections.singletonList(7L), hypervisorType, null, null, filter);

        assertTrue(result.isEmpty());
        verify(searchCriteria).setParameters(eq("zoneIds"), aryEq(new Object[]{7L}));
        verify(searchCriteria).setParameters(eq("hypervisors"), aryEq(new Object[]{Hypervisor.HypervisorType.Any, hypervisorType}));
        verify(searchCriteria, never()).setParameters(eq("types"), any(Object[].class));
        verify(searchCriteria, never()).setParameters(eq("name"), any());
        verify(storagePoolJoinDao).listBy(searchCriteria, filter);
        verify(storagePoolJoinDao, never()).searchByIds(any());
    }

    @Test
    public void listAvailableByZonesHypervisorAndTypeSkipsTypeFilterForEmptyTypesAndPassesNullFilter() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.XenServer;

        doReturn(Collections.emptyList()).when(storagePoolJoinDao).listBy(searchCriteria, null);

        storagePoolJoinDao.listAvailableByZonesHypervisorAndType(Collections.singletonList(9L), hypervisorType, Collections.emptyList(), null, null);

        verify(searchCriteria, never()).setParameters(eq("types"), any(Object[].class));
        verify(storagePoolJoinDao).listBy(searchCriteria, null);
    }

    @Test
    public void listAvailableByZonesHypervisorAndTypeReturnsEmptyListForNoZones() {
        assertTrue(storagePoolJoinDao.listAvailableByZonesHypervisorAndType(Collections.emptyList(),
                Hypervisor.HypervisorType.KVM, null, null, null).isEmpty());
        verify(storagePoolJoinDao, never()).createSearchBuilder();
    }

    @Test
    public void listOneRowPerPoolReturnsSingleRowPerPoolInIdOrder() {
        StoragePoolJoinVO pool1Tag1 = mockPoolRow(1L);
        StoragePoolJoinVO pool1Tag2 = mockPoolRow(1L);
        StoragePoolJoinVO pool2 = mockPoolRow(2L);
        StoragePoolJoinVO pool3Group1 = mockPoolRow(3L);
        StoragePoolJoinVO pool3Group2 = mockPoolRow(3L);
        doReturn(Arrays.asList(pool1Tag1, pool3Group1, pool1Tag2, pool2, pool3Group2)).when(storagePoolJoinDao).searchByIds(3L, 1L, 2L, 4L);

        List<StoragePoolJoinVO> result = storagePoolJoinDao.listOneRowPerPool(Arrays.asList(3L, 1L, 2L, 4L));

        assertEquals(Arrays.asList(pool3Group1, pool1Tag1, pool2), result);
    }
}
