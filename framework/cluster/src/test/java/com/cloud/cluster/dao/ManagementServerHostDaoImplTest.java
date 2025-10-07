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

package com.cloud.cluster.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.management.ManagementServerHost;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class ManagementServerHostDaoImplTest {

    @Spy
    @InjectMocks
    private ManagementServerHostDaoImpl managementServerHostDao;

    @Test
    public void listUpByIdsReturnsEmptyListWhenInputIsEmpty() {
        List<ManagementServerHostVO> result = managementServerHostDao.listUpByIds(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void listUpByIdsReturnsEmptyListWhenNoMatchingIds() {;
        SearchBuilder<ManagementServerHostVO> mockSb = mock(SearchBuilder.class);
        SearchCriteria<ManagementServerHostVO> mocSC = mock(SearchCriteria.class);
        when(mockSb.entity()).thenReturn(mock(ManagementServerHostVO.class));
        when(mockSb.create()).thenReturn(mocSC);
        doReturn(mockSb).when(managementServerHostDao).createSearchBuilder();
        doReturn(Collections.emptyList()).when(managementServerHostDao).listBy(any(SearchCriteria.class));
        List<ManagementServerHostVO> result = managementServerHostDao.listUpByIds(List.of(1L, 2L));
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(managementServerHostDao).createSearchBuilder();
        verify(mockSb).and(eq("ids"), anyLong(), eq(SearchCriteria.Op.IN));
        verify(mockSb).and(eq("state"), nullable(ManagementServerHost.State.class), eq(SearchCriteria.Op.EQ));
        verify(mockSb).done();
        verify(mocSC).setParameters(eq("ids"), anyLong(), anyLong());
        verify(mocSC).setParameters("state", ManagementServerHost.State.Up);
    }

    @Test
    public void listUpByIdsReturnsMatchingHostsWhenIdsAreValid() {
        ManagementServerHostVO host1 = mock(ManagementServerHostVO.class);
        ManagementServerHostVO host2 = mock(ManagementServerHostVO.class);
        doReturn(List.of(host1, host2)).when(managementServerHostDao).listBy(any(SearchCriteria.class));
        List<ManagementServerHostVO> result = managementServerHostDao.listUpByIds(List.of(1L, 2L));
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(host1));
        assertTrue(result.contains(host2));
    }
}
