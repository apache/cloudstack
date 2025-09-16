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
package org.apache.cloudstack.framework.jobs.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class VmWorkJobDaoImplTest {
    @Mock
    AsyncJobDao asyncJobDao;

    @Spy
    @InjectMocks
    VmWorkJobDaoImpl vmWorkJobDaoImpl;

    private GenericSearchBuilder<VmWorkJobVO, Long> genericVmWorkJobSearchBuilder;
    private SearchBuilder<AsyncJobVO> asyncJobSearchBuilder;
    private SearchCriteria<Long> searchCriteria;

    @Before
    public void setUp() {
        genericVmWorkJobSearchBuilder = mock(GenericSearchBuilder.class);
        VmWorkJobVO entityVO = mock(VmWorkJobVO.class);
        when(genericVmWorkJobSearchBuilder.entity()).thenReturn(entityVO);
        asyncJobSearchBuilder = mock(SearchBuilder.class);
        AsyncJobVO asyncJobVO = mock(AsyncJobVO.class);
        when(asyncJobSearchBuilder.entity()).thenReturn(asyncJobVO);
        searchCriteria = mock(SearchCriteria.class);
        when(vmWorkJobDaoImpl.createSearchBuilder(Long.class)).thenReturn(genericVmWorkJobSearchBuilder);
        when(asyncJobDao.createSearchBuilder()).thenReturn(asyncJobSearchBuilder);
        when(genericVmWorkJobSearchBuilder.create()).thenReturn(searchCriteria);
    }

    @Test
    public void testExpungeByVmListNoVms() {
        Assert.assertEquals(0, vmWorkJobDaoImpl.expungeByVmList(
                new ArrayList<>(), 100L));
        Assert.assertEquals(0, vmWorkJobDaoImpl.expungeByVmList(
                null, 100L));
    }

    @Test
    public void testExpungeByVmList() {
        SearchBuilder<VmWorkJobVO> sb = mock(SearchBuilder.class);
        SearchCriteria<VmWorkJobVO> sc = mock(SearchCriteria.class);
        when(sb.create()).thenReturn(sc);
        doAnswer((Answer<Integer>) invocationOnMock -> {
            Long batchSize = (Long)invocationOnMock.getArguments()[1];
            return batchSize == null ? 0 : batchSize.intValue();
        }).when(vmWorkJobDaoImpl).batchExpunge(any(SearchCriteria.class), anyLong());
        when(vmWorkJobDaoImpl.createSearchBuilder()).thenReturn(sb);
        final VmWorkJobVO mockedVO = mock(VmWorkJobVO.class);
        when(sb.entity()).thenReturn(mockedVO);
        List<Long> vmIds = List.of(1L, 2L);
        Object[] array = vmIds.toArray();
        Long batchSize = 50L;
        Assert.assertEquals(batchSize.intValue(), vmWorkJobDaoImpl.expungeByVmList(List.of(1L, 2L), batchSize));
        verify(sc).setParameters("vmIds", array);
        verify(vmWorkJobDaoImpl, times(1))
                .batchExpunge(sc, batchSize);
    }

    @Test
    public void testListVmIdsWithPendingJob() {
        List<Long> mockVmIds = Arrays.asList(101L, 102L, 103L);
        doReturn(mockVmIds).when(vmWorkJobDaoImpl).customSearch(any(SearchCriteria.class), isNull());
        List<Long> result = vmWorkJobDaoImpl.listVmIdsWithPendingJob();
        verify(genericVmWorkJobSearchBuilder).join(eq("asyncJobSearch"), eq(asyncJobSearchBuilder), any(), any(), eq(JoinBuilder.JoinType.INNER));
        verify(genericVmWorkJobSearchBuilder).and(eq("removed"), any(), eq(SearchCriteria.Op.NULL));
        verify(genericVmWorkJobSearchBuilder).create();
        verify(asyncJobSearchBuilder).and(eq("status"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchCriteria).setJoinParameters(eq("asyncJobSearch"), eq("status"), eq(JobInfo.Status.IN_PROGRESS));
        verify(vmWorkJobDaoImpl).customSearch(searchCriteria, null);
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(101L), result.get(0));
        assertEquals(Long.valueOf(102L), result.get(1));
        assertEquals(Long.valueOf(103L), result.get(2));
    }

    @Test
    public void testListVmIdsWithPendingJobEmptyResult() {
        doReturn(Collections.emptyList()).when(vmWorkJobDaoImpl).customSearch(any(SearchCriteria.class), isNull());
        List<Long> result = vmWorkJobDaoImpl.listVmIdsWithPendingJob();
        verify(genericVmWorkJobSearchBuilder).join(eq("asyncJobSearch"), eq(asyncJobSearchBuilder), any(), any(), eq(JoinBuilder.JoinType.INNER));
        verify(genericVmWorkJobSearchBuilder).and(eq("removed"), any(), eq(SearchCriteria.Op.NULL));
        verify(genericVmWorkJobSearchBuilder).create();
        verify(asyncJobSearchBuilder).and(eq("status"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchCriteria).setJoinParameters(eq("asyncJobSearch"), eq("status"), eq(JobInfo.Status.IN_PROGRESS));
        verify(vmWorkJobDaoImpl).customSearch(searchCriteria, null);
        assertTrue(result.isEmpty());
    }
}
