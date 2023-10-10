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

import com.cloud.network.as.AutoScaleVmGroupVmMapVO;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AutoScaleVmGroupVmMapDaoImplTest {

    @Mock
    SearchBuilder<AutoScaleVmGroupVmMapVO> searchBuilderAutoScaleVmGroupVmMapVOMock;

    @Mock
    SearchCriteria<AutoScaleVmGroupVmMapVO> searchCriteriaAutoScaleVmGroupVmMapVOMock;

    @Mock
    List<AutoScaleVmGroupVmMapVO> listAutoScaleVmGroupVmMapVOMock;

    @Mock
    GenericSearchBuilder<AutoScaleVmGroupVmMapVO, Integer> searchBuilderCountAvailableVmsByGroup;

    @Mock
    SearchCriteria<Integer> searchCriteriaCountAvailableVmsByGroup;

    @Spy
    AutoScaleVmGroupVmMapDaoImpl AutoScaleVmGroupVmMapDaoImplSpy;

    @Before
    public void setUp() {
        AutoScaleVmGroupVmMapDaoImplSpy.AllFieldsSearch = searchBuilderAutoScaleVmGroupVmMapVOMock;
        AutoScaleVmGroupVmMapDaoImplSpy.CountBy = searchBuilderCountAvailableVmsByGroup;
        Mockito.doReturn(searchCriteriaAutoScaleVmGroupVmMapVOMock).when(searchBuilderAutoScaleVmGroupVmMapVOMock).create();
        Mockito.doReturn(searchCriteriaCountAvailableVmsByGroup).when(searchBuilderCountAvailableVmsByGroup).create();
    }

    @Test
    public void testCountAvailableVmsByGroup() throws Exception {
        Mockito.doReturn(Arrays.asList(5)).when(AutoScaleVmGroupVmMapDaoImplSpy).customSearch(Mockito.any(SearchCriteria.class), Mockito.eq(null));

        long groupId = 4L;

        int result = AutoScaleVmGroupVmMapDaoImplSpy.countAvailableVmsByGroup(groupId);

        Assert.assertEquals(5, result);

        Mockito.verify(searchCriteriaCountAvailableVmsByGroup).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaCountAvailableVmsByGroup).setJoinParameters("vmSearch", "states", new Object[] {VirtualMachine.State.Starting, VirtualMachine.State.Running, VirtualMachine.State.Stopping, VirtualMachine.State.Migrating});
    }

    @Test
    public void testCountByGroup() throws Exception {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters(Mockito.anyString(), Mockito.any());
        Mockito.doReturn(6).when(AutoScaleVmGroupVmMapDaoImplSpy).getCountIncludingRemoved(Mockito.any(SearchCriteria.class));

        long groupId = 4L;

        int result = AutoScaleVmGroupVmMapDaoImplSpy.countByGroup(groupId);

        Assert.assertEquals(6, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("vmGroupId", groupId);
    }

    @Test
    public void testListByGroup() throws Exception {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters(Mockito.anyString(), Mockito.any());
        Mockito.doReturn(listAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        long groupId = 4L;

        List<AutoScaleVmGroupVmMapVO> result = AutoScaleVmGroupVmMapDaoImplSpy.listByGroup(groupId);

        Assert.assertEquals(listAutoScaleVmGroupVmMapVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("vmGroupId", groupId);
    }

    @Test
    public void testListByVm() throws Exception {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters(Mockito.anyString(), Mockito.any());
        Mockito.doReturn(listAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        long vmId = 100L;

        List<AutoScaleVmGroupVmMapVO> result = AutoScaleVmGroupVmMapDaoImplSpy.listByVm(vmId);

        Assert.assertEquals(listAutoScaleVmGroupVmMapVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("instanceId", vmId);
    }

    @Test
    public void testRemoveByVm() {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters(Mockito.anyString(), Mockito.any());
        Mockito.doReturn(2).when(AutoScaleVmGroupVmMapDaoImplSpy).remove(Mockito.any(SearchCriteria.class));

        long vmId = 3L;

        boolean result = AutoScaleVmGroupVmMapDaoImplSpy.removeByVm(vmId);

        Assert.assertTrue(result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("instanceId", vmId);
        Mockito.verify(AutoScaleVmGroupVmMapDaoImplSpy).remove(searchCriteriaAutoScaleVmGroupVmMapVOMock);
    }

    @Test
    public void testRemoveByGroup() {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters(Mockito.anyString(), Mockito.any());
        Mockito.doReturn(2).when(AutoScaleVmGroupVmMapDaoImplSpy).remove(Mockito.any(SearchCriteria.class));

        long groupId = 4L;

        boolean result = AutoScaleVmGroupVmMapDaoImplSpy.removeByGroup(groupId);

        Assert.assertTrue(result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(AutoScaleVmGroupVmMapDaoImplSpy).remove(searchCriteriaAutoScaleVmGroupVmMapVOMock);
    }

    @Test
    public void testRemoveByGroupAndVm() {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters(Mockito.anyString(), Mockito.any());
        Mockito.doReturn(2).when(AutoScaleVmGroupVmMapDaoImplSpy).remove(Mockito.any(SearchCriteria.class));

        long vmId = 3L;
        long groupId = 4L;

        int result = AutoScaleVmGroupVmMapDaoImplSpy.remove(groupId, vmId);

        Assert.assertEquals(2, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("instanceId", vmId);
        Mockito.verify(AutoScaleVmGroupVmMapDaoImplSpy).remove(searchCriteriaAutoScaleVmGroupVmMapVOMock);
    }

    @Test
    public void testRemoveByVmFailed() {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters(Mockito.anyString(), Mockito.any());
        Mockito.doReturn(-1).when(AutoScaleVmGroupVmMapDaoImplSpy).remove(Mockito.any(SearchCriteria.class));

        long vmId = 3L;

        boolean result = AutoScaleVmGroupVmMapDaoImplSpy.removeByVm(vmId);

        Assert.assertFalse(result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("instanceId", vmId);
        Mockito.verify(AutoScaleVmGroupVmMapDaoImplSpy).remove(searchCriteriaAutoScaleVmGroupVmMapVOMock);
    }

    @Test
    public void testRemoveByGroupFailed() {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters(Mockito.anyString(), Mockito.any());
        Mockito.doReturn(-1).when(AutoScaleVmGroupVmMapDaoImplSpy).remove(Mockito.any(SearchCriteria.class));

        long groupId = 4L;

        boolean result = AutoScaleVmGroupVmMapDaoImplSpy.removeByGroup(groupId);

        Assert.assertFalse(result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).setParameters("vmGroupId", groupId);
        Mockito.verify(AutoScaleVmGroupVmMapDaoImplSpy).remove(searchCriteriaAutoScaleVmGroupVmMapVOMock);
    }
}
