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
package com.cloud.dc.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.isNull;
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
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.ClusterVO;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDaoImplTest {
    @Spy
    @InjectMocks
    ClusterDaoImpl clusterDao = new ClusterDaoImpl();

    private GenericSearchBuilder<ClusterVO, Long> genericSearchBuilder;

    @Before
    public void setUp() {
        genericSearchBuilder = mock(SearchBuilder.class);
        ClusterVO entityVO = mock(ClusterVO.class);
        when(genericSearchBuilder.entity()).thenReturn(entityVO);
        doReturn(genericSearchBuilder).when(clusterDao).createSearchBuilder(Long.class);
    }

    @Test
    public void testListAllIds() {
        List<Long> mockIds = Arrays.asList(1L, 2L, 3L);
        doReturn(mockIds).when(clusterDao).customSearch(any(), isNull());
        List<Long> result = clusterDao.listAllIds();
        verify(clusterDao).customSearch(genericSearchBuilder.create(), null);
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(1L), result.get(0));
        assertEquals(Long.valueOf(2L), result.get(1));
        assertEquals(Long.valueOf(3L), result.get(2));
    }

    @Test
    public void testListAllIdsEmptyResult() {
        doReturn(Collections.emptyList()).when(clusterDao).customSearch(any(), isNull());
        List<Long> result = clusterDao.listAllIds();
        verify(clusterDao).customSearch(genericSearchBuilder.create(), null);
        assertTrue(result.isEmpty());
    }
}
