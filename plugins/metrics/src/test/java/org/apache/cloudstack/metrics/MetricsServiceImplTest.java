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
package org.apache.cloudstack.metrics;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ListVMsUsageHistoryCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.response.VmMetricsStatsResponse;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VmStatsVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VmStatsDao;


@RunWith(MockitoJUnitRunner.class)
public class MetricsServiceImplTest {

    @Spy
    @InjectMocks
    MetricsServiceImpl spy;

    @Mock
    ListVMsUsageHistoryCmd listVMsUsageHistoryCmdMock;

    @Mock
    UserVmVO userVmVOMock;

    @Mock
    SearchBuilder<UserVmVO> sbMock;

    @Mock
    SearchCriteria<UserVmVO> scMock;

    @Mock
    UserVmDao userVmDaoMock;

    @Mock
    VmStatsDao vmStatsDaoMock;

    @Captor
    ArgumentCaptor<String> stringCaptor1, stringCaptor2;

    @Captor
    ArgumentCaptor<Object[]> objectArrayCaptor;

    @Captor
    ArgumentCaptor<SearchCriteria.Op> opCaptor;
    long fakeVmId1 = 1L, fakeVmId2 = 2L;

    Pair<List<? extends VMInstanceVO>, Integer> expectedVmListAndCounter;

    @Mock
    Pair<List<? extends VMInstanceVO>, Integer> expectedVmListAndCounterMock;

    @Mock
    Map<Long,List<VmStatsVO>> vmStatsMapMock;

    @Mock
    VmStatsVO vmStatsVOMock;

    private void prepareSearchCriteriaWhenUseSetParameters() {
        Mockito.doNothing().when(scMock).setParameters(Mockito.anyString(), Mockito.any());
    }

    private void preparesearchForUserVmsInternalTest() {
        expectedVmListAndCounter = new Pair<>(Arrays.asList(userVmVOMock), 1);

        Mockito.doReturn(1L).when(listVMsUsageHistoryCmdMock).getStartIndex();
        Mockito.doReturn(2L).when(listVMsUsageHistoryCmdMock).getPageSizeVal();

        Mockito.doReturn(sbMock).when(userVmDaoMock).createSearchBuilder();
        Mockito.doReturn(fakeVmId1).when(userVmVOMock).getId();
        Mockito.doReturn(userVmVOMock).when(sbMock).entity();
        Mockito.doReturn(scMock).when(sbMock).create();

        Mockito.doReturn(new Pair<List<UserVmVO>, Integer>(Arrays.asList(userVmVOMock), 1))
        .when(userVmDaoMock).searchAndCount(Mockito.any(), Mockito.any());
    }

    @Test
    public void searchForUserVmsInternalTestWithOnlyOneId() {
        preparesearchForUserVmsInternalTest();
        prepareSearchCriteriaWhenUseSetParameters();
        Mockito.doReturn(fakeVmId1).when(listVMsUsageHistoryCmdMock).getId();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getIds();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getName();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getKeyword();

        Pair<List<UserVmVO>, Integer> result = spy.searchForUserVmsInternal(listVMsUsageHistoryCmdMock);

        Mockito.verify(scMock).setParameters(stringCaptor1.capture(), objectArrayCaptor.capture());
        Assert.assertEquals("idIN", stringCaptor1.getValue());
        Assert.assertEquals(Arrays.asList(fakeVmId1), objectArrayCaptor.getAllValues());
        Assert.assertEquals(expectedVmListAndCounter, result);
    }

    @Test
    public void searchForUserVmsInternalTestWithAListOfIds() {
        List<Long> expected =  Arrays.asList(fakeVmId1, fakeVmId2);
        preparesearchForUserVmsInternalTest();
        prepareSearchCriteriaWhenUseSetParameters();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getId();
        Mockito.doReturn(expected).when(listVMsUsageHistoryCmdMock).getIds();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getName();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getKeyword();

        Pair<List<UserVmVO>, Integer> result = spy.searchForUserVmsInternal(listVMsUsageHistoryCmdMock);

        Mockito.verify(scMock).setParameters(stringCaptor1.capture(), objectArrayCaptor.capture());
        Assert.assertEquals("idIN", stringCaptor1.getValue());
        Assert.assertEquals(expected, objectArrayCaptor.getAllValues());
        Assert.assertEquals(expectedVmListAndCounter, result);
    }

    @Test
    public void searchForUserVmsInternalTestWithName() {
        preparesearchForUserVmsInternalTest();
        prepareSearchCriteriaWhenUseSetParameters();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getId();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getIds();
        Mockito.doReturn("fakeName").when(listVMsUsageHistoryCmdMock).getName();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getKeyword();

        Pair<List<UserVmVO>, Integer> result = spy.searchForUserVmsInternal(listVMsUsageHistoryCmdMock);

        Mockito.verify(scMock).setParameters(stringCaptor1.capture(), objectArrayCaptor.capture());
        Assert.assertEquals("displayName", stringCaptor1.getValue());
        Assert.assertEquals("%fakeName%", objectArrayCaptor.getValue());
        Assert.assertEquals(expectedVmListAndCounter, result);
    }

    @Test
    public void searchForUserVmsInternalTestWithKeyword() {
        preparesearchForUserVmsInternalTest();
        prepareSearchCriteriaWhenUseSetParameters();
        Mockito.doReturn(scMock).when(userVmDaoMock).createSearchCriteria();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getId();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getIds();
        Mockito.doReturn(null).when(listVMsUsageHistoryCmdMock).getName();
        Mockito.doReturn("fakeKeyword").when(listVMsUsageHistoryCmdMock).getKeyword();

        Pair<List<UserVmVO>, Integer> result = spy.searchForUserVmsInternal(listVMsUsageHistoryCmdMock);

        Mockito.verify(scMock, Mockito.times(2)).addOr(stringCaptor1.capture(), opCaptor.capture(), objectArrayCaptor.capture());
        List<String> conditions = stringCaptor1.getAllValues();
        List<Object[]> params = objectArrayCaptor.getAllValues();
        Assert.assertEquals("displayName", conditions.get(0));
        Assert.assertEquals("state", conditions.get(1));
        Assert.assertEquals("%fakeKeyword%", params.get(0));
        Assert.assertEquals("fakeKeyword", params.get(1));
        Assert.assertEquals(expectedVmListAndCounter, result);
    }

    @Test
    public void searchForVmMetricsStatsInternalTestWithAPopulatedListOfVms() {
        Mockito.doNothing().when(spy).validateDateParams(Mockito.any(), Mockito.any());
        Mockito.doReturn(new ArrayList<VmStatsVO>()).when(spy).findVmStatsAccordingToDateParams(
                Mockito.anyLong(), Mockito.any(), Mockito.any());
        Mockito.doReturn(fakeVmId1).when(userVmVOMock).getId();
        Map<Long,List<VmStatsVO>> expected = new HashMap<Long,List<VmStatsVO>>();
        expected.put(fakeVmId1, new ArrayList<VmStatsVO>());
        Date startDate = Mockito.mock(Date.class);
        Date endDate = Mockito.mock(Date.class);

        Map<Long,List<VmStatsVO>> result = spy.searchForVmMetricsStatsInternal(
                startDate, endDate, Arrays.asList(userVmVOMock));

        Mockito.verify(userVmVOMock).getId();
        Mockito.verify(spy).findVmStatsAccordingToDateParams(
                Mockito.anyLong(), Mockito.any(), Mockito.any());
        Assert.assertEquals(expected, result);
    }

    @Test
    public void searchForVmMetricsStatsInternalTestWithAnEmptyListOfVms() {
        Mockito.doNothing().when(spy).validateDateParams(Mockito.any(), Mockito.any());
        Map<Long,List<VmStatsVO>> expected = new HashMap<Long,List<VmStatsVO>>();
        Date startDate = Mockito.mock(Date.class);
        Date endDate = Mockito.mock(Date.class);
        Map<Long,List<VmStatsVO>> result = spy.searchForVmMetricsStatsInternal(
                startDate, endDate, new ArrayList<UserVmVO>());

        Mockito.verify(userVmVOMock, Mockito.never()).getId();
        Mockito.verify(spy, Mockito.never()).findVmStatsAccordingToDateParams(
                Mockito.anyLong(), Mockito.any(), Mockito.any());
        Assert.assertEquals(expected, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateDateParamsTestWithEndDateBeforeStartDate() {
        Date startDate = new Date();
        Date endDate = DateUtils.addSeconds(startDate, -1);

        spy.validateDateParams(startDate, endDate);
    }

    @Test
    public void findVmStatsAccordingToDateParamsTestWithStartDateAndEndDate() {
        Date startDate = new Date();
        Date endDate = DateUtils.addSeconds(startDate, 1);
        Mockito.doReturn(new ArrayList<VmStatsVO>()).when(vmStatsDaoMock).findByVmIdAndTimestampBetween(
                Mockito.anyLong(), Mockito.any(), Mockito.any());

        spy.findVmStatsAccordingToDateParams(fakeVmId1, startDate, endDate);

        Mockito.verify(vmStatsDaoMock).findByVmIdAndTimestampBetween(
              Mockito.anyLong(), Mockito.any(), Mockito.any());
    }

    @Test
    public void findVmStatsAccordingToDateParamsTestWithOnlyStartDate() {
        Date startDate = new Date();
        Date endDate = null;
        Mockito.doReturn(new ArrayList<VmStatsVO>()).when(vmStatsDaoMock).findByVmIdAndTimestampGreaterThanEqual(
                Mockito.anyLong(), Mockito.any());

        spy.findVmStatsAccordingToDateParams(fakeVmId1, startDate, endDate);

        Mockito.verify(vmStatsDaoMock).findByVmIdAndTimestampGreaterThanEqual(
              Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void findVmStatsAccordingToDateParamsTestWithOnlyEndDate() {
        Date startDate = null;
        Date endDate = new Date();
        Mockito.doReturn(new ArrayList<VmStatsVO>()).when(vmStatsDaoMock).findByVmIdAndTimestampLessThanEqual(
                Mockito.anyLong(), Mockito.any());

        spy.findVmStatsAccordingToDateParams(fakeVmId1, startDate, endDate);

        Mockito.verify(vmStatsDaoMock).findByVmIdAndTimestampLessThanEqual(
              Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void findVmStatsAccordingToDateParamsTestWithNoDate() {
        Mockito.doReturn(new ArrayList<VmStatsVO>()).when(vmStatsDaoMock).findByVmId(Mockito.anyLong());

        spy.findVmStatsAccordingToDateParams(fakeVmId1, null, null);

        Mockito.verify(vmStatsDaoMock).findByVmId(Mockito.anyLong());
    }

    @Test
    public void createVmMetricsStatsResponseTestWithValidInput() {
        Mockito.doReturn("").when(userVmVOMock).getUuid();
        Mockito.doReturn("").when(userVmVOMock).getName();
        Mockito.doReturn("").when(userVmVOMock).getDisplayName();
        Mockito.doReturn(fakeVmId1).when(userVmVOMock).getId();
        Mockito.doReturn(Arrays.asList(userVmVOMock)).when(expectedVmListAndCounterMock).first();
        Mockito.doReturn(null).when(vmStatsMapMock).get(Mockito.any());
        Mockito.doReturn(null).when(spy).createStatsResponse(Mockito.any());

        ListResponse<VmMetricsStatsResponse> result = spy.createVmMetricsStatsResponse(
                expectedVmListAndCounterMock.first(), vmStatsMapMock);

        Assert.assertEquals(Integer.valueOf(1), result.getCount());
    }

    @Test(expected = NullPointerException.class)
    public void createVmMetricsStatsResponseTestWithNoUserVmList() {
        spy.createVmMetricsStatsResponse(null, vmStatsMapMock);
    }

    @Test(expected = NullPointerException.class)
    public void createVmMetricsStatsResponseTestWithNoVmStatsList() {
        spy.createVmMetricsStatsResponse(expectedVmListAndCounterMock.first(), null);
    }

    @Test
    public void createStatsResponseTestWithValidStatsData() {
        final String fakeVmStatsData = "{\"vmId\":1,\"cpuUtilization\":15.615905273486222,\"networkReadKBs\":0.1,"
                + "\"networkWriteKBs\":0.2,\"diskReadIOs\":0.1,\"diskWriteIOs\":0.2,\"diskReadKBs\":1.1,"
                + "\"diskWriteKBs\":2.1,\"memoryKBs\":262144.0,\"intfreememoryKBs\":262144.0,\"targetmemoryKBs\":262144.0,"
                + "\"numCPUs\":1,\"entityType\":\"vm\"}";
        Mockito.doReturn(new Date()).when(vmStatsVOMock).getTimestamp();
        Mockito.doReturn(fakeVmStatsData).when(vmStatsVOMock).getVmStatsData();

        spy.createStatsResponse(Arrays.asList(vmStatsVOMock));
    }
}
