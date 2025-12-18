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

package com.cloud.network.lb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.lb.dao.ElasticLbVmMapDao;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class LoadBalanceRuleHandlerTest {

    @InjectMocks
    private LoadBalanceRuleHandler loadBalanceRuleHandler;

    @Mock
    private VirtualMachineManager virtualMachineManagerMock;

    @Mock
    private DomainRouterDao domainRouterDaoMock;

    @Mock
    private ElasticLbVmMapDao elasticLbVmMapDao;

    @Mock
    private PodVlanMapDao podVlanMapDao;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(loadBalanceRuleHandler, "_itMgr", virtualMachineManagerMock);
        ReflectionTestUtils.setField(loadBalanceRuleHandler, "_routerDao", domainRouterDaoMock);
        ReflectionTestUtils.setField(loadBalanceRuleHandler, "_elbVmMapDao", elasticLbVmMapDao);
        ReflectionTestUtils.setField(loadBalanceRuleHandler, "_podVlanMapDao", podVlanMapDao);
    }

    @Test
    public void testAddCandidateVmIsPodIpMatchesWhenIdsHaveSameValue() throws Exception {
        DomainRouterVO candidateVmMock = mock(DomainRouterVO.class);
        when(candidateVmMock.getPodIdToDeployIn()).thenReturn(new Long(1));
        Long podIdForDirectIp = new Long(1);
        List<DomainRouterVO> candidateVms = new ArrayList<DomainRouterVO>();

        LoadBalanceRuleHandler.addCandidateVmIsPodIpMatches(candidateVmMock, podIdForDirectIp, candidateVms);

        assertEquals(1, candidateVms.size());
    }

    @Test
    public void testAddCandidateVmIsPodIpMatchesWhenPodIdForDirectIpIsNull() throws Exception {
        DomainRouterVO candidateVmMock = mock(DomainRouterVO.class);
        when(candidateVmMock.getPodIdToDeployIn()).thenReturn(new Long(1));
        Long podIdForDirectIp = null;
        List<DomainRouterVO> candidateVms = new ArrayList<DomainRouterVO>();

        LoadBalanceRuleHandler.addCandidateVmIsPodIpMatches(candidateVmMock, podIdForDirectIp, candidateVms);

        assertEquals(0, candidateVms.size());
    }

    // PodIdToDeployIn should not be null according to column specification in DomainRouterVO
    @Test(expected = NullPointerException.class)
    public void testAddCandidateVmIsPodIpMatchesWhenPodIdToDeployInIsNull() throws Exception {
        DomainRouterVO candidateVmMock = mock(DomainRouterVO.class);
        when(candidateVmMock.getPodIdToDeployIn()).thenReturn(null);
        Long podIdForDirectIp = new Long(1);
        List<DomainRouterVO> candidateVms = new ArrayList<DomainRouterVO>();

        LoadBalanceRuleHandler.addCandidateVmIsPodIpMatches(candidateVmMock, podIdForDirectIp, candidateVms);
    }

    @Test(expected = NullPointerException.class)
    public void testAddCandidateVmIsPodIpMatchesWhenCandidateVmsIsNull() throws Exception {
        DomainRouterVO candidateVmMock = mock(DomainRouterVO.class);
        when(candidateVmMock.getPodIdToDeployIn()).thenReturn(new Long(1));
        Long podIdForDirectIp = new Long(1);
        List<DomainRouterVO> candidateVms = null;

        LoadBalanceRuleHandler.addCandidateVmIsPodIpMatches(candidateVmMock, podIdForDirectIp, candidateVms);
    }

    @Test(expected = NullPointerException.class)
    public void testStartWhenElbVmIsNull() throws Exception {
        DomainRouterVO elbVm = null;
        Map<Param, Object> params = new HashMap<Param, Object>();

        loadBalanceRuleHandler.start(elbVm, params);
    }

    @Test
    public void testStartWhenParamsIsNull() throws Exception {
        DomainRouterVO elbVmMock = mock(DomainRouterVO.class);
        String uuid = "uuid";
        when(elbVmMock.getUuid()).thenReturn(uuid);
        long id = 1L;
        when(elbVmMock.getId()).thenReturn(id);
        Map<Param, Object> params = null;

        loadBalanceRuleHandler.start(elbVmMock, params);

        verify(virtualMachineManagerMock, times(1)).start(uuid, params);
        verify(domainRouterDaoMock, times(1)).findById(id);
    }

    @Test
    public void testStartWhenParamsIsEmpty() throws Exception {
        DomainRouterVO elbVmMock = mock(DomainRouterVO.class);
        String uuid = "uuid";
        when(elbVmMock.getUuid()).thenReturn(uuid);
        long id = 1L;
        when(elbVmMock.getId()).thenReturn(id);
        Map<Param, Object> params = new HashMap<Param, Object>();

        loadBalanceRuleHandler.start(elbVmMock, params);

        verify(virtualMachineManagerMock, times(1)).start(uuid, params);
        verify(domainRouterDaoMock, times(1)).findById(id);
    }

    @Test
    public void testStart() throws Exception {
        DomainRouterVO elbVmMock = mock(DomainRouterVO.class);
        String uuid = "uuid";
        when(elbVmMock.getUuid()).thenReturn(uuid);
        long id = 1L;
        when(elbVmMock.getId()).thenReturn(id);
        Map<Param, Object> params = new HashMap<Param, Object>();
        params.put(mock(Param.class), new Object());

        loadBalanceRuleHandler.start(elbVmMock, params);

        verify(virtualMachineManagerMock, times(1)).start(uuid, params);
        verify(domainRouterDaoMock, times(1)).findById(id);
    }

    @Test
    public void testFindElbVmWithCapacityWhenIpAddrIsNull() throws Exception {
        IPAddressVO ipAddr = null;

        DomainRouterVO actual = loadBalanceRuleHandler.findElbVmWithCapacity(ipAddr);

        assertNull(actual);
    }

    @Test
    public void testFindElbVmWithCapacityWhenThereAreNoUnusedElbVms() throws Exception {
        IPAddressVO ipAddr = mock(IPAddressVO.class);
        when(this.elasticLbVmMapDao.listUnusedElbVms()).thenReturn(new ArrayList<DomainRouterVO>(1));

        DomainRouterVO actual = loadBalanceRuleHandler.findElbVmWithCapacity(ipAddr);

        assertNull(actual);
    }

    @Test
    public void testFindElbVmWithCapacityWhenThereAreUnusedElbVmsAndOneMatchesThePodId() throws Exception {
        Long podId = 1L;
        IPAddressVO ipAddrMock = mock(IPAddressVO.class);
        when(ipAddrMock.getVlanId()).thenReturn(podId);
        PodVlanMapVO podVlanMapVoMock = mock(PodVlanMapVO.class);
        when(podVlanMapVoMock.getPodId()).thenReturn(podId);
        when(podVlanMapDao.listPodVlanMapsByVlan(podId)).thenReturn(podVlanMapVoMock);
        DomainRouterVO unusedElbVmThatMatchesPodId = mock(DomainRouterVO.class);
        when(unusedElbVmThatMatchesPodId.getPodIdToDeployIn()).thenReturn(podId);
        List<DomainRouterVO> unusedElbVms = Arrays.asList(new DomainRouterVO[] {unusedElbVmThatMatchesPodId, mock(DomainRouterVO.class)});
        when(this.elasticLbVmMapDao.listUnusedElbVms()).thenReturn(unusedElbVms);

        DomainRouterVO expected = unusedElbVmThatMatchesPodId;
        DomainRouterVO actual = loadBalanceRuleHandler.findElbVmWithCapacity(ipAddrMock);

        assertNotNull(actual);
        assertEquals(expected, actual);
    }

}
