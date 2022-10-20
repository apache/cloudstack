/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.network.as.dao;

import com.cloud.network.as.AutoScaleVmGroupStatisticsVO;
import com.cloud.server.ResourceTag;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.List;

@RunWith(PowerMockRunner.class)
public class AutoScaleVmGroupStatisticsDaoImplTest {

    @Mock
    SearchCriteria<AutoScaleVmGroupStatisticsVO> searchCriteriaAutoScaleVmGroupStatisticsVOMock;

    @Mock
    SearchBuilder<AutoScaleVmGroupStatisticsVO> searchBuilderAutoScaleVmGroupStatisticsVOMock;

    @Mock
    List<AutoScaleVmGroupStatisticsVO> listAutoScaleVmGroupStatisticsVOMock;

    AutoScaleVmGroupStatisticsDaoImpl AutoScaleVmGroupStatisticsDaoImplSpy = PowerMockito.spy(new AutoScaleVmGroupStatisticsDaoImpl());

    long groupId = 4L;
    long policyId = 5L;
    long counterId = 6L;
    Date date = new Date();
    AutoScaleVmGroupStatisticsVO autoScaleVmGroupStatisticsVO = new AutoScaleVmGroupStatisticsVO(groupId);
    AutoScaleVmGroupStatisticsVO.State state = AutoScaleVmGroupStatisticsVO.State.INACTIVE;

    @Before
    public void setUp() throws Exception {
        AutoScaleVmGroupStatisticsDaoImplSpy.groupAndCounterSearch = searchBuilderAutoScaleVmGroupStatisticsVOMock;
        PowerMockito.doReturn(searchBuilderAutoScaleVmGroupStatisticsVOMock).when(AutoScaleVmGroupStatisticsDaoImplSpy).createSearchBuilder();
        Mockito.doReturn(searchCriteriaAutoScaleVmGroupStatisticsVOMock).when(searchBuilderAutoScaleVmGroupStatisticsVOMock).create();
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters(Mockito.anyString(), Mockito.any());

        PowerMockito.doReturn(listAutoScaleVmGroupStatisticsVOMock).when(AutoScaleVmGroupStatisticsDaoImplSpy, "listBy", Mockito.any(SearchCriteria.class));

        PowerMockito.doReturn(autoScaleVmGroupStatisticsVO).when(AutoScaleVmGroupStatisticsDaoImplSpy).createForUpdate();
        PowerMockito.doReturn(1).when(AutoScaleVmGroupStatisticsDaoImplSpy).update(Mockito.any(AutoScaleVmGroupStatisticsVO.class), Mockito.any(SearchCriteria.class));
        PowerMockito.doReturn(autoScaleVmGroupStatisticsVO).when(AutoScaleVmGroupStatisticsDaoImplSpy).persist(Mockito.any(AutoScaleVmGroupStatisticsVO.class));
    }

    @Test
    public void testRemoveByGroupId1() {
        PowerMockito.doReturn(2).when(AutoScaleVmGroupStatisticsDaoImplSpy).expunge(Mockito.any(SearchCriteria.class));
        boolean result = AutoScaleVmGroupStatisticsDaoImplSpy.removeByGroupId(groupId);
        Assert.assertTrue(result);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
    }

    @Test
    public void testRemoveByGroupId2() {
        PowerMockito.doReturn(-1).when(AutoScaleVmGroupStatisticsDaoImplSpy).expunge(Mockito.any(SearchCriteria.class));
        boolean result = AutoScaleVmGroupStatisticsDaoImplSpy.removeByGroupId(groupId);
        Assert.assertFalse(result);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);

    }

    @Test
    public void testRemoveByGroupId3() {
        PowerMockito.doReturn(-1).when(AutoScaleVmGroupStatisticsDaoImplSpy).expunge(Mockito.any(SearchCriteria.class));
        boolean result = AutoScaleVmGroupStatisticsDaoImplSpy.removeByGroupId(groupId, date);
        Assert.assertFalse(result);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("createdLT", date);

    }

    @Test
    public void testRemoveByGroupAndPolicy1() {
        PowerMockito.doReturn(2).when(AutoScaleVmGroupStatisticsDaoImplSpy).expunge(Mockito.any(SearchCriteria.class));

        boolean result = AutoScaleVmGroupStatisticsDaoImplSpy.removeByGroupAndPolicy(groupId, policyId, null);

        Assert.assertTrue(result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", policyId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock, Mockito.never()).setParameters("createdLT", date);
    }

    @Test
    public void testRemoveByGroupAndPolicy2() {
        PowerMockito.doReturn(-1).when(AutoScaleVmGroupStatisticsDaoImplSpy).expunge(Mockito.any(SearchCriteria.class));

        boolean result = AutoScaleVmGroupStatisticsDaoImplSpy.removeByGroupAndPolicy(groupId, policyId, date);

        Assert.assertFalse(result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", policyId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("createdLT", date);
    }

    @Test
    public void testListDummyRecordsByVmGroup1() {
        List<AutoScaleVmGroupStatisticsVO> result = AutoScaleVmGroupStatisticsDaoImplSpy.listDummyRecordsByVmGroup(groupId, null);

        Assert.assertEquals(listAutoScaleVmGroupStatisticsVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", AutoScaleVmGroupStatisticsVO.DUMMY_ID);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("state", AutoScaleVmGroupStatisticsVO.State.INACTIVE);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock, Mockito.never()).setParameters(Mockito.eq("createdGT"), Mockito.any());
    }

    @Test
    public void testListDummyRecordsByVmGroup2() {
        List<AutoScaleVmGroupStatisticsVO>  result = AutoScaleVmGroupStatisticsDaoImplSpy.listDummyRecordsByVmGroup(groupId, date);

        Assert.assertEquals(listAutoScaleVmGroupStatisticsVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", AutoScaleVmGroupStatisticsVO.DUMMY_ID);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("state", AutoScaleVmGroupStatisticsVO.State.INACTIVE);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("createdGT", date);
    }

    @Test
    public void testListInactiveByVmGroupAndPolicy1() {
        List<AutoScaleVmGroupStatisticsVO> result = AutoScaleVmGroupStatisticsDaoImplSpy.listInactiveByVmGroupAndPolicy(groupId, policyId, null);

        Assert.assertEquals(listAutoScaleVmGroupStatisticsVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", policyId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("state", AutoScaleVmGroupStatisticsVO.State.INACTIVE);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock, Mockito.never()).setParameters(Mockito.eq("createdGT"), Mockito.any());
    }

    @Test
    public void testListInactiveByVmGroupAndPolicy2() {
        List<AutoScaleVmGroupStatisticsVO> result = AutoScaleVmGroupStatisticsDaoImplSpy.listInactiveByVmGroupAndPolicy(groupId, policyId, date);

        Assert.assertEquals(listAutoScaleVmGroupStatisticsVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", policyId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("state", AutoScaleVmGroupStatisticsVO.State.INACTIVE);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("createdGT", date);
    }

    @Test
    public void testlistByVmGroupAndPolicyAndCounter1() {
        List<AutoScaleVmGroupStatisticsVO> result = AutoScaleVmGroupStatisticsDaoImplSpy.listByVmGroupAndPolicyAndCounter(groupId, policyId, counterId, null);

        Assert.assertEquals(listAutoScaleVmGroupStatisticsVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", policyId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("state", AutoScaleVmGroupStatisticsVO.State.ACTIVE);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock, Mockito.never()).setParameters("createdGT", date);
    }

    @Test
    public void testlistByVmGroupAndPolicyAndCounter2() {
        List<AutoScaleVmGroupStatisticsVO> result = AutoScaleVmGroupStatisticsDaoImplSpy.listByVmGroupAndPolicyAndCounter(groupId, policyId, counterId, date);

        Assert.assertEquals(listAutoScaleVmGroupStatisticsVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", policyId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("state", AutoScaleVmGroupStatisticsVO.State.ACTIVE);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("createdGT", date);
    }

    @Test
    public void testUpdateStateByGroup1() {
        AutoScaleVmGroupStatisticsDaoImplSpy.updateStateByGroup(groupId, policyId, state);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("policyId", policyId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("stateNEQ", state);
        Mockito.verify(AutoScaleVmGroupStatisticsDaoImplSpy).update(autoScaleVmGroupStatisticsVO, searchCriteriaAutoScaleVmGroupStatisticsVOMock);

        Assert.assertEquals(state, autoScaleVmGroupStatisticsVO.getState());
    }

    @Test
    public void testUpdateStateByGroup2() {
        AutoScaleVmGroupStatisticsDaoImplSpy.updateStateByGroup(null, null, state);

        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock, Mockito.never()).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock, Mockito.never()).setParameters("policyId", policyId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupStatisticsVOMock).setParameters("stateNEQ", state);
        Mockito.verify(AutoScaleVmGroupStatisticsDaoImplSpy).update(autoScaleVmGroupStatisticsVO, searchCriteriaAutoScaleVmGroupStatisticsVOMock);

        Assert.assertEquals(state, autoScaleVmGroupStatisticsVO.getState());
    }

    @Test
    public void testCreateInactiveDummyRecord() {
        AutoScaleVmGroupStatisticsVO result = AutoScaleVmGroupStatisticsDaoImplSpy.createInactiveDummyRecord(groupId);

        Assert.assertEquals(groupId, result.getVmGroupId());
        Assert.assertEquals(AutoScaleVmGroupStatisticsVO.DUMMY_ID, result.getPolicyId());
        Assert.assertEquals(AutoScaleVmGroupStatisticsVO.DUMMY_ID, result.getCounterId());
        Assert.assertEquals(groupId, (long) result.getResourceId());
        Assert.assertEquals(ResourceTag.ResourceObjectType.AutoScaleVmGroup, result.getResourceType());
        Assert.assertEquals(AutoScaleVmGroupStatisticsVO.INVALID_VALUE, result.getRawValue(), 0);
        Assert.assertEquals(AutoScaleVmGroupStatisticsVO.State.INACTIVE, result.getState());
    }

    @Test
    public void testInit() {
        Mockito.when(searchBuilderAutoScaleVmGroupStatisticsVOMock.entity()).thenReturn(autoScaleVmGroupStatisticsVO);

        AutoScaleVmGroupStatisticsDaoImplSpy.init();

        Mockito.verify(searchBuilderAutoScaleVmGroupStatisticsVOMock, Mockito.times(4)).and(Mockito.anyString(), Mockito.any(), Mockito.eq(SearchCriteria.Op.EQ));
        Mockito.verify(searchBuilderAutoScaleVmGroupStatisticsVOMock, Mockito.times(1)).and(Mockito.anyString(), Mockito.any(), Mockito.eq(SearchCriteria.Op.LT));
        Mockito.verify(searchBuilderAutoScaleVmGroupStatisticsVOMock, Mockito.times(1)).and(Mockito.anyString(), Mockito.any(), Mockito.eq(SearchCriteria.Op.GT));

    }
}
