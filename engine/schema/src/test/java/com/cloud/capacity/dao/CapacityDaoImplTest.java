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
package com.cloud.capacity.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.capacity.CapacityVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class CapacityDaoImplTest {
    @Spy
    @InjectMocks
    CapacityDaoImpl capacityDao = new CapacityDaoImpl();

    private SearchBuilder<CapacityVO> searchBuilder;
    private SearchCriteria<CapacityVO> searchCriteria;

    @Before
    public void setUp() {
        searchBuilder = mock(SearchBuilder.class);
        CapacityVO capacityVO = mock(CapacityVO.class);
        when(searchBuilder.entity()).thenReturn(capacityVO);
        searchCriteria = mock(SearchCriteria.class);
        doReturn(searchBuilder).when(capacityDao).createSearchBuilder();
        when(searchBuilder.create()).thenReturn(searchCriteria);
    }

    @Test
    public void testListByHostIdTypes() {
        // Prepare inputs
        Long hostId = 1L;
        List<Short> capacityTypes = Arrays.asList((short)1, (short)2);
        CapacityVO capacity1 = new CapacityVO();
        CapacityVO capacity2 = new CapacityVO();
        List<CapacityVO> mockResult = Arrays.asList(capacity1, capacity2);
        doReturn(mockResult).when(capacityDao).listBy(any(SearchCriteria.class));
        List<CapacityVO> result = capacityDao.listByHostIdTypes(hostId, capacityTypes);
        verify(searchBuilder).and(eq("hostId"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder).and(eq("type"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder).done();
        verify(searchCriteria).setParameters("hostId", hostId);
        verify(searchCriteria).setParameters("type", capacityTypes.toArray());
        verify(capacityDao).listBy(searchCriteria);
        assertEquals(2, result.size());
        assertSame(capacity1, result.get(0));
        assertSame(capacity2, result.get(1));
    }

    @Test
    public void testListByHostIdTypesEmptyResult() {
        Long hostId = 1L;
        List<Short> capacityTypes = Arrays.asList((short)1, (short)2);
        doReturn(Collections.emptyList()).when(capacityDao).listBy(any(SearchCriteria.class));
        List<CapacityVO> result = capacityDao.listByHostIdTypes(hostId, capacityTypes);
        verify(searchBuilder).and(Mockito.eq("hostId"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder).and(eq("type"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder).done();
        verify(searchCriteria).setParameters("hostId", hostId);
        verify(searchCriteria).setParameters("type", capacityTypes.toArray());
        verify(capacityDao).listBy(searchCriteria);
        assertTrue(result.isEmpty());
    }
}
