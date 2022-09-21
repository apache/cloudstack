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

import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

@RunWith(PowerMockRunner.class)
public class AutoScaleVmGroupDaoImplTest {

    @Mock
    SearchBuilder<AutoScaleVmGroupVO> searchBuilderAutoScaleVmGroupVOMock;

    @Mock
    SearchCriteria<AutoScaleVmGroupVO> searchCriteriaAutoScaleVmGroupVOMock;

    @Mock
    List<AutoScaleVmGroupVO> listAutoScaleVmGroupVOMock;

    @Mock
    AutoScaleVmGroupVO autoScaleVmGroupVOMock;

    @Spy
    AutoScaleVmGroupDaoImpl AutoScaleVmGroupDaoImplSpy = PowerMockito.spy(new AutoScaleVmGroupDaoImpl());

    @Before
    public void setUp() {
        AutoScaleVmGroupDaoImplSpy.AllFieldsSearch = searchBuilderAutoScaleVmGroupVOMock;
        Mockito.doReturn(searchCriteriaAutoScaleVmGroupVOMock).when(searchBuilderAutoScaleVmGroupVOMock).create();
    }

    @Test
    public void testListByLoadBalancer() throws Exception {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVOMock).setParameters(Mockito.anyString(), Mockito.any());
        PowerMockito.doReturn(listAutoScaleVmGroupVOMock).when(AutoScaleVmGroupDaoImplSpy, "listBy", Mockito.any(SearchCriteria.class));

        long loadBalancerId = 100L;

        List<AutoScaleVmGroupVO> result = AutoScaleVmGroupDaoImplSpy.listByLoadBalancer(loadBalancerId);

        Assert.assertEquals(listAutoScaleVmGroupVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVOMock).setParameters("loadBalancerId", loadBalancerId);
    }

    @Test
    public void testListByProfile() throws Exception {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVOMock).setParameters(Mockito.anyString(), Mockito.any());
        PowerMockito.doReturn(listAutoScaleVmGroupVOMock).when(AutoScaleVmGroupDaoImplSpy, "listBy", Mockito.any(SearchCriteria.class));

        long profileId = 101L;

        List<AutoScaleVmGroupVO> result = AutoScaleVmGroupDaoImplSpy.listByProfile(profileId);

        Assert.assertEquals(listAutoScaleVmGroupVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVOMock).setParameters("profileId", profileId);
    }

    @Test
    public void testListByAccount() throws Exception {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVOMock).setParameters(Mockito.anyString(), Mockito.any());
        PowerMockito.doReturn(listAutoScaleVmGroupVOMock).when(AutoScaleVmGroupDaoImplSpy, "listBy", Mockito.any(SearchCriteria.class));

        long accountId = 102L;

        List<AutoScaleVmGroupVO> result = AutoScaleVmGroupDaoImplSpy.listByAccount(accountId);

        Assert.assertEquals(listAutoScaleVmGroupVOMock, result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVOMock).setParameters("accountId", accountId);
    }

    @Test
    public void testUpdateState1() throws Exception {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVOMock).setParameters(Mockito.anyString(), Mockito.any());
        PowerMockito.doReturn(null).when(AutoScaleVmGroupDaoImplSpy, "findOneBy", Mockito.any(SearchCriteria.class));

        long groupId = 10L;
        AutoScaleVmGroup.State oldState = AutoScaleVmGroup.State.ENABLED;
        AutoScaleVmGroup.State newState = AutoScaleVmGroup.State.DISABLED;

        boolean result = AutoScaleVmGroupDaoImplSpy.updateState(groupId, oldState, newState);
        Assert.assertFalse(result);
    }

    @Test
    public void testUpdateState2() throws Exception {
        Mockito.doNothing().when(searchCriteriaAutoScaleVmGroupVOMock).setParameters(Mockito.anyString(), Mockito.any());
        PowerMockito.doReturn(autoScaleVmGroupVOMock).when(AutoScaleVmGroupDaoImplSpy, "findOneBy", Mockito.any(SearchCriteria.class));
        Mockito.doNothing().when(autoScaleVmGroupVOMock).setState(Mockito.any(AutoScaleVmGroup.State.class));
        PowerMockito.doReturn(true).when(AutoScaleVmGroupDaoImplSpy).update(Mockito.anyLong(), Mockito.any(AutoScaleVmGroupVO.class));

        long groupId = 10L;
        AutoScaleVmGroup.State oldState = AutoScaleVmGroup.State.ENABLED;
        AutoScaleVmGroup.State newState = AutoScaleVmGroup.State.DISABLED;

        boolean result = AutoScaleVmGroupDaoImplSpy.updateState(groupId, oldState, newState);
        Assert.assertTrue(result);

        Mockito.verify(searchCriteriaAutoScaleVmGroupVOMock, Mockito.times(1)).setParameters("id", groupId);
        Mockito.verify(searchCriteriaAutoScaleVmGroupVOMock, Mockito.times(1)).setParameters("state", oldState);
        Mockito.verify(autoScaleVmGroupVOMock, Mockito.times(1)).setState(newState);
        Mockito.verify(AutoScaleVmGroupDaoImplSpy, Mockito.times(1)).update(groupId, autoScaleVmGroupVOMock);
    }
}
