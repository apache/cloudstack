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

import static org.junit.Assert.assertSame;
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

    @Test
    public void listByZoneHypervisorAndTypeReturnsMatchingPoolsWhenTypesAreProvided() {
        long zoneId = 42L;
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        List<Storage.StoragePoolType> types = Arrays.asList(Storage.StoragePoolType.NetworkFilesystem, Storage.StoragePoolType.Filesystem);
        Filter filter = mock(Filter.class);
        List<StoragePoolJoinVO> expectedPools = Collections.singletonList(mock(StoragePoolJoinVO.class));

        doReturn(expectedPools).when(storagePoolJoinDao).listBy(searchCriteria, filter);

        List<StoragePoolJoinVO> result = storagePoolJoinDao.listByZoneHypervisorAndType(zoneId, hypervisorType, types, filter);

        assertSame(expectedPools, result);
        verify(searchCriteria).setParameters("zoneId", zoneId);
        verify(searchCriteria).setParameters(eq("hypervisors"), aryEq(new Object[]{Hypervisor.HypervisorType.Any, hypervisorType}));
        verify(searchCriteria).setParameters(eq("types"), aryEq(types.toArray()));
        verify(storagePoolJoinDao).listBy(searchCriteria, filter);
    }

    @Test
    public void listByZoneHypervisorAndTypeSkipsTypeFilterWhenTypesAreNull() {
        long zoneId = 7L;
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.VMware;
        Filter filter = mock(Filter.class);
        List<StoragePoolJoinVO> expectedPools = Collections.emptyList();

        doReturn(expectedPools).when(storagePoolJoinDao).listBy(searchCriteria, filter);

        List<StoragePoolJoinVO> result = storagePoolJoinDao.listByZoneHypervisorAndType(zoneId, hypervisorType, null, filter);

        assertSame(expectedPools, result);
        verify(searchCriteria).setParameters("zoneId", zoneId);
        verify(searchCriteria).setParameters(eq("hypervisors"), aryEq(new Object[]{Hypervisor.HypervisorType.Any, hypervisorType}));
        verify(searchCriteria, never()).setParameters(eq("types"), any(Object[].class));
        verify(storagePoolJoinDao).listBy(searchCriteria, filter);
    }

    @Test
    public void listByZoneHypervisorAndTypeSkipsTypeFilterForEmptyTypesAndPassesNullFilter() {
        long zoneId = 9L;
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.XenServer;
        List<StoragePoolJoinVO> expectedPools = Collections.singletonList(mock(StoragePoolJoinVO.class));

        doReturn(expectedPools).when(storagePoolJoinDao).listBy(searchCriteria, null);

        List<StoragePoolJoinVO> result = storagePoolJoinDao.listByZoneHypervisorAndType(zoneId, hypervisorType, Collections.emptyList(), null);

        assertSame(expectedPools, result);
        verify(searchCriteria).setParameters("zoneId", zoneId);
        verify(searchCriteria).setParameters(eq("hypervisors"), aryEq(new Object[]{Hypervisor.HypervisorType.Any, hypervisorType}));
        verify(searchCriteria, never()).setParameters(eq("types"), any(Object[].class));
        verify(storagePoolJoinDao).listBy(searchCriteria, null);
    }
}
