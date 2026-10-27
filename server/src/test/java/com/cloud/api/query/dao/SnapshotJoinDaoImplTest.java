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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import com.cloud.api.query.vo.SnapshotJoinVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotJoinDaoImplTest {

    @Spy
    @InjectMocks
    SnapshotJoinDaoImpl snapshotJoinDao = new SnapshotJoinDaoImpl();

    @Mock
    SearchCriteria<SnapshotJoinVO> mockSearchCriteria;

    @Before
    public void setUp() {
        SnapshotJoinVO mockSnap = mock(SnapshotJoinVO.class);
        SearchBuilder<SnapshotJoinVO> mockSearchBuilder = mock(SearchBuilder.class);
        when(mockSearchBuilder.entity()).thenReturn(mockSnap);
        doReturn(mockSearchBuilder).when(snapshotJoinDao).createSearchBuilder();
        when(mockSearchBuilder.create()).thenReturn(mockSearchCriteria);
    }

    @Test
    public void testFindById_WithNullZoneId_EmptyResult() {
        Long zoneId = null;
        long id = 1L;
        doReturn(Collections.emptyList()).when(snapshotJoinDao).search(mockSearchCriteria, null);
        List<SnapshotJoinVO> result = snapshotJoinDao.findById(zoneId, id);
        assertNull(result);
        verify(mockSearchCriteria).setParameters("id", id);
        verify(mockSearchCriteria, never()).setParameters("zoneId", zoneId);
    }

    @Test
    public void testFindById_WithValidZoneId_EmptyResult() {
        Long zoneId = 1L;
        long id = 1L;
        doReturn(Collections.emptyList()).when(snapshotJoinDao).search(mockSearchCriteria, null);
        List<SnapshotJoinVO> result = snapshotJoinDao.findById(zoneId, id);
        assertNull(result);
        verify(mockSearchCriteria).setParameters("id", id);
        verify(mockSearchCriteria).setParameters("zoneId", zoneId);
    }

    @Test
    public void testFindById_WithValidResults() {
        Long zoneId = 1L;
        long id = 1L;
        SnapshotJoinVO snapshot1 = mock(SnapshotJoinVO.class);
        when(snapshot1.getSnapshotStorePair()).thenReturn("Primary_1");
        SnapshotJoinVO snapshot2 = mock(SnapshotJoinVO.class);
        when(snapshot2.getSnapshotStorePair()).thenReturn("Image_1");
        doReturn(Arrays.asList(snapshot1, snapshot2)).when(snapshotJoinDao).search(mockSearchCriteria, null);
        List<SnapshotJoinVO> result = snapshotJoinDao.findById(zoneId, id);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Image_1", result.get(0).getSnapshotStorePair());
        verify(mockSearchCriteria).setParameters("id", id);
        verify(mockSearchCriteria).setParameters("zoneId", zoneId);
    }

    @Test
    public void testFindById_WithNullResults() {
        long id = 1L;
        doReturn(null).when(snapshotJoinDao).search(mockSearchCriteria, null);
        List<SnapshotJoinVO> result = snapshotJoinDao.findById(null, id);
        assertNull(result);
    }
}
