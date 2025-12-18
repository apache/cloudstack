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
package com.cloud.vm.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.NicVO;

@RunWith(MockitoJUnitRunner.class)
public class NicDaoImplTest {

    @Spy
    NicDaoImpl nicDaoImplSpy;

    @Test
    public void testSearchRemovedByVmsNoVms() {
        Assert.assertTrue(CollectionUtils.isEmpty(nicDaoImplSpy.searchRemovedByVms(
                new ArrayList<>(), 100L)));
        Assert.assertTrue(CollectionUtils.isEmpty(nicDaoImplSpy.searchRemovedByVms(
                null, 100L)));
    }

    @Test
    public void testSearchRemovedByVms() {
        SearchBuilder<NicVO> sb = Mockito.mock(SearchBuilder.class);
        SearchCriteria<NicVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doReturn(new ArrayList<>()).when(nicDaoImplSpy).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.any(Filter.class), Mockito.eq(null),
                Mockito.eq(false));
        Mockito.when(nicDaoImplSpy.createSearchBuilder()).thenReturn(sb);
        final NicVO mockedVO = Mockito.mock(NicVO.class);
        Mockito.when(sb.entity()).thenReturn(mockedVO);
        List<Long> vmIds = List.of(1L, 2L);
        Object[] array = vmIds.toArray();
        Long batchSize = 50L;
        nicDaoImplSpy.searchRemovedByVms(List.of(1L, 2L), batchSize);
        Mockito.verify(sc).setParameters("vmIds", array);
        Mockito.verify(nicDaoImplSpy, Mockito.times(1)).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.any(Filter.class), Mockito.eq(null),
                Mockito.eq(false));
    }
}
