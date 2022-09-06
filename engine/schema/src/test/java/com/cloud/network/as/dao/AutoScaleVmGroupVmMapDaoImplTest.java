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
import com.cloud.utils.db.SearchCriteria;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

@RunWith(PowerMockRunner.class)
public class AutoScaleVmGroupVmMapDaoImplTest {

    @Mock
    SearchCriteria<AutoScaleVmGroupVmMapVO> searchCriteriaAutoScaleVmGroupVmMapVOMock;

    @Mock
    List<AutoScaleVmGroupVmMapVO> listAutoScaleVmGroupVmMapVOMock;

    @Test
    public void testListByGroup() throws Exception {
        AutoScaleVmGroupVmMapDaoImpl AutoScaleVmGroupVmMapDaoImplSpy = PowerMockito.spy(new AutoScaleVmGroupVmMapDaoImpl());

        PowerMockito.doReturn(searchCriteriaAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy).createSearchCriteria();
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd(Mockito.anyString(), Mockito.any(), Mockito.any());
        PowerMockito.doReturn(listAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy, "listBy", Mockito.any(SearchCriteria.class));

        long groupId = 4L;

        List<AutoScaleVmGroupVmMapVO> result = AutoScaleVmGroupVmMapDaoImplSpy.listByGroup(groupId);

        Assert.assertEquals(listAutoScaleVmGroupVmMapVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd("vmGroupId", SearchCriteria.Op.EQ, groupId);
    }

    @Test
    public void testListByVm() throws Exception {
        AutoScaleVmGroupVmMapDaoImpl AutoScaleVmGroupVmMapDaoImplSpy = PowerMockito.spy(new AutoScaleVmGroupVmMapDaoImpl());

        PowerMockito.doReturn(searchCriteriaAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy).createSearchCriteria();
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd(Mockito.anyString(), Mockito.any(), Mockito.any());
        PowerMockito.doReturn(listAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy, "listBy", Mockito.any(SearchCriteria.class));

        long vmId = 100L;

        List<AutoScaleVmGroupVmMapVO> result = AutoScaleVmGroupVmMapDaoImplSpy.listByVm(vmId);

        Assert.assertEquals(listAutoScaleVmGroupVmMapVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd("instanceId", SearchCriteria.Op.EQ, vmId);
    }

    @Test
    public void testRemoveByVm() throws Exception {
        AutoScaleVmGroupVmMapDaoImpl AutoScaleVmGroupVmMapDaoImplSpy = PowerMockito.spy(new AutoScaleVmGroupVmMapDaoImpl());

        PowerMockito.doReturn(searchCriteriaAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy).createSearchCriteria();
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd(Mockito.anyString(), Mockito.any(), Mockito.any());
        PowerMockito.doReturn(2).when(AutoScaleVmGroupVmMapDaoImplSpy).remove(Mockito.any(SearchCriteria.class));

        long vmId = 3L;

        boolean result = AutoScaleVmGroupVmMapDaoImplSpy.removeByVm(vmId);

        Assert.assertEquals(true, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd("instanceId", SearchCriteria.Op.EQ, vmId);
        Mockito.verify(AutoScaleVmGroupVmMapDaoImplSpy).remove(searchCriteriaAutoScaleVmGroupVmMapVOMock);
    }

    @Test
    public void testRemoveByGroup() throws Exception {
        AutoScaleVmGroupVmMapDaoImpl AutoScaleVmGroupVmMapDaoImplSpy = PowerMockito.spy(new AutoScaleVmGroupVmMapDaoImpl());

        PowerMockito.doReturn(searchCriteriaAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy).createSearchCriteria();
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd(Mockito.anyString(), Mockito.any(), Mockito.any());
        PowerMockito.doReturn(2).when(AutoScaleVmGroupVmMapDaoImplSpy).remove(Mockito.any(SearchCriteria.class));

        long groupId = 4L;

        boolean result = AutoScaleVmGroupVmMapDaoImplSpy.removeByGroup(groupId);

        Assert.assertEquals(true, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd("vmGroupId", SearchCriteria.Op.EQ, groupId);
        Mockito.verify(AutoScaleVmGroupVmMapDaoImplSpy).remove(searchCriteriaAutoScaleVmGroupVmMapVOMock);
    }

    @Test
    public void testRemoveByGroupAndVm() throws Exception {
        AutoScaleVmGroupVmMapDaoImpl AutoScaleVmGroupVmMapDaoImplSpy = PowerMockito.spy(new AutoScaleVmGroupVmMapDaoImpl());

        PowerMockito.doReturn(searchCriteriaAutoScaleVmGroupVmMapVOMock).when(AutoScaleVmGroupVmMapDaoImplSpy).createSearchCriteria();
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd(Mockito.anyString(), Mockito.any(), Mockito.any());
        PowerMockito.doReturn(2).when(AutoScaleVmGroupVmMapDaoImplSpy).remove(Mockito.any(SearchCriteria.class));

        long vmId = 3L;
        long groupId = 4L;

        int result = AutoScaleVmGroupVmMapDaoImplSpy.remove(groupId, vmId);

        Assert.assertEquals(2, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd("vmGroupId", SearchCriteria.Op.EQ, groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupVmMapVOMock).addAnd("instanceId", SearchCriteria.Op.EQ, vmId);
        Mockito.verify(AutoScaleVmGroupVmMapDaoImplSpy).remove(searchCriteriaAutoScaleVmGroupVmMapVOMock);
    }
}
