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
package com.cloud.vm;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class ItWorkDaoImplTest {

    @Spy
    ItWorkDaoImpl itWorkDaoImplSpy;

    @Test
    public void testExpungeByVmListNoVms() {
        Assert.assertEquals(0, itWorkDaoImplSpy.expungeByVmList(
                new ArrayList<>(), 100L));
        Assert.assertEquals(0, itWorkDaoImplSpy.expungeByVmList(
                null, 100L));
    }

    @Test
    public void testExpungeByVmList() {
        SearchBuilder<ItWorkVO> sb = Mockito.mock(SearchBuilder.class);
        SearchCriteria<ItWorkVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doAnswer((Answer<Integer>) invocationOnMock -> {
            Long batchSize = (Long)invocationOnMock.getArguments()[1];
            return batchSize == null ? 0 : batchSize.intValue();
        }).when(itWorkDaoImplSpy).batchExpunge(Mockito.any(SearchCriteria.class), Mockito.anyLong());
        Mockito.when(itWorkDaoImplSpy.createSearchBuilder()).thenReturn(sb);
        final ItWorkVO mockedVO = Mockito.mock(ItWorkVO.class);
        Mockito.when(sb.entity()).thenReturn(mockedVO);
        List<Long> vmIds = List.of(1L, 2L);
        Object[] array = vmIds.toArray();
        Long batchSize = 50L;
        Assert.assertEquals(batchSize.intValue(), itWorkDaoImplSpy.expungeByVmList(List.of(1L, 2L), batchSize));
        Mockito.verify(sc).setParameters("vmIds", array);
        Mockito.verify(itWorkDaoImplSpy, Mockito.times(1))
                .batchExpunge(sc, batchSize);
    }
}
