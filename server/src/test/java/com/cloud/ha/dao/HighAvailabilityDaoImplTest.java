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
package com.cloud.ha.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.ha.HaWorkVO;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.VirtualMachine;

@RunWith(MockitoJUnitRunner.class)
public class HighAvailabilityDaoImplTest {

    @Spy
    HighAvailabilityDaoImpl highAvailabilityDaoImpl = new HighAvailabilityDaoImpl();

    @Test
    public void testExpungeByVmListNoVms() {
        Assert.assertEquals(0, highAvailabilityDaoImpl.expungeByVmList(
                new ArrayList<>(), 100L));
        Assert.assertEquals(0, highAvailabilityDaoImpl.expungeByVmList(
                null, 100L));
    }

    @Test
    public void testExpungeByVmList() {
        SearchBuilder<HaWorkVO> sb = Mockito.mock(SearchBuilder.class);
        SearchCriteria<HaWorkVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doAnswer((Answer<Integer>) invocationOnMock -> {
            Long batchSize = (Long)invocationOnMock.getArguments()[1];
            return batchSize == null ? 0 : batchSize.intValue();
        }).when(highAvailabilityDaoImpl).batchExpunge(Mockito.any(SearchCriteria.class), Mockito.anyLong());
        Mockito.when(highAvailabilityDaoImpl.createSearchBuilder()).thenReturn(sb);
        final HaWorkVO mockedVO = Mockito.mock(HaWorkVO.class);
        Mockito.when(sb.entity()).thenReturn(mockedVO);
        List<Long> vmIds = List.of(1L, 2L);
        Object[] array = vmIds.toArray();
        Long batchSize = 50L;
        Assert.assertEquals(batchSize.intValue(), highAvailabilityDaoImpl.expungeByVmList(List.of(1L, 2L), batchSize));
        Mockito.verify(sc).setParameters("vmIds", array);
        Mockito.verify(highAvailabilityDaoImpl, Mockito.times(1))
                .batchExpunge(sc, batchSize);
    }

    @Test
    public void testMarkPendingWorksAsInvestigating() throws Exception {
        SearchBuilder<HaWorkVO> mockTBASearch = Mockito.mock(SearchBuilder.class);
        highAvailabilityDaoImpl.TBASearch = mockTBASearch;
        SearchCriteria<HaWorkVO> mockSearchCriteria = Mockito.mock(SearchCriteria.class);
        UpdateBuilder mockUpdateBuilder = Mockito.mock(UpdateBuilder.class);
        Mockito.when(mockTBASearch.create()).thenReturn(mockSearchCriteria);
        Mockito.doNothing().when(mockSearchCriteria).setParameters(Mockito.eq("time"), Mockito.anyLong());
        Mockito.doNothing().when(mockSearchCriteria).setParameters(Mockito.eq("step"), Mockito.eq(HighAvailabilityManager.Step.Done), Mockito.eq(HighAvailabilityManager.Step.Cancelled));
        HaWorkVO haWorkVO = new HaWorkVO(1L, VirtualMachine.Type.User, null,
                null, 1L, null, 0, 0,
                HighAvailabilityManager.ReasonType.HostMaintenance);
        Mockito.when(highAvailabilityDaoImpl.createForUpdate()).thenReturn(haWorkVO);
        try(MockedStatic<GenericDaoBase> genericDaoBaseMockedStatic = Mockito.mockStatic(GenericDaoBase.class)) {
            genericDaoBaseMockedStatic.when(() -> GenericDaoBase.getUpdateBuilder(Mockito.any())).thenReturn(mockUpdateBuilder);
            Mockito.doReturn(5).when(highAvailabilityDaoImpl).update(Mockito.any(UpdateBuilder.class), Mockito.any(), Mockito.nullable(Integer.class));
            highAvailabilityDaoImpl.markPendingWorksAsInvestigating();
            Mockito.verify(mockTBASearch).create();
            Mockito.verify(mockSearchCriteria).setParameters(Mockito.eq("time"), Mockito.anyLong());
            Mockito.verify(mockSearchCriteria).setParameters(Mockito.eq("step"), Mockito.eq(HighAvailabilityManager.Step.Done), Mockito.eq(HighAvailabilityManager.Step.Cancelled));
            Assert.assertEquals(HighAvailabilityManager.Step.Investigating, haWorkVO.getStep()); // Ensure the step is set correctly
            Mockito.verify(highAvailabilityDaoImpl).update(Mockito.eq(mockUpdateBuilder), Mockito.eq(mockSearchCriteria), Mockito.isNull());
        }
    }

    @Test
    public void testMarkServerPendingWorksAsInvestigating() {
        SearchBuilder<HaWorkVO> mockSearch = Mockito.mock(SearchBuilder.class);
        Mockito.doReturn(Mockito.mock(HaWorkVO.class)).when(mockSearch).entity();
        Mockito.doReturn(mockSearch).when(highAvailabilityDaoImpl).createSearchBuilder();
        SearchCriteria<HaWorkVO> mockSearchCriteria = Mockito.mock(SearchCriteria.class);
        UpdateBuilder mockUpdateBuilder = Mockito.mock(UpdateBuilder.class);
        Mockito.when(mockSearch.create()).thenReturn(mockSearchCriteria);
        Mockito.doNothing().when(mockSearchCriteria).setParameters(Mockito.eq("server"), Mockito.eq(1L));
        Mockito.doNothing().when(mockSearchCriteria).setParameters(Mockito.eq("step"), Mockito.eq(HighAvailabilityManager.Step.Done), Mockito.eq(HighAvailabilityManager.Step.Cancelled));
        HaWorkVO haWorkVO = new HaWorkVO(1L, VirtualMachine.Type.User, null,
                null, 1L, null, 0, 0,
                HighAvailabilityManager.ReasonType.HostMaintenance);
        Mockito.when(highAvailabilityDaoImpl.createForUpdate()).thenReturn(haWorkVO);
        Mockito.when(highAvailabilityDaoImpl.createForUpdate()).thenReturn(haWorkVO);
        try(MockedStatic<GenericDaoBase> genericDaoBaseMockedStatic = Mockito.mockStatic(GenericDaoBase.class)) {
            genericDaoBaseMockedStatic.when(() -> GenericDaoBase.getUpdateBuilder(Mockito.any())).thenReturn(mockUpdateBuilder);
            Mockito.doReturn(5).when(highAvailabilityDaoImpl).update(Mockito.any(UpdateBuilder.class), Mockito.any(), Mockito.nullable(Integer.class));
            highAvailabilityDaoImpl.markServerPendingWorksAsInvestigating(1L);
            Mockito.verify(mockSearch).create();
            Mockito.verify(mockSearchCriteria).setParameters(Mockito.eq("server"), Mockito.eq(1L));
            Mockito.verify(mockSearchCriteria).setParameters(Mockito.eq("step"), Mockito.eq(HighAvailabilityManager.Step.Done), Mockito.eq(HighAvailabilityManager.Step.Cancelled));
            Assert.assertEquals(HighAvailabilityManager.Step.Investigating, haWorkVO.getStep()); // Ensure the step is set correctly
            Mockito.verify(highAvailabilityDaoImpl).update(Mockito.eq(mockUpdateBuilder), Mockito.eq(mockSearchCriteria), Mockito.isNull());
        }
    }
}
