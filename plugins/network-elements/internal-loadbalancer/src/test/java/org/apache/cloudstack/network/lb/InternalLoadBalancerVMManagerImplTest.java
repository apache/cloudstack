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
package org.apache.cloudstack.network.lb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.network.Network;
import com.cloud.network.router.VirtualRouter;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;

@RunWith(MockitoJUnitRunner.class)
public class InternalLoadBalancerVMManagerImplTest {
    @Mock
    private DomainRouterDao internalLbVmDao;

    @Mock
    private VirtualMachineManager virtualMachineManager;

    @InjectMocks
    private InternalLoadBalancerVMManagerImpl service;

    // Dummy objects for testing.
    private Account account;
    private ServiceOffering serviceOffering;
    private VMTemplateVO template1;
    private VMTemplateVO template2;
    private DeploymentPlan plan;

    @Before
    public void setUp() {
        account = mock(Account.class);
        serviceOffering = mock(ServiceOffering.class);
        template1 = mock(VMTemplateVO.class);
        template2 = mock(VMTemplateVO.class);
        plan = new DataCenterDeployment(1L);
    }

    @Test
    public void testCreateOrUpdateInternalLb_New() {
        long id = 1L;
        long internalLbProviderId = 2L;
        long userId = 100L;
        Long vpcId = 200L;
        when(template1.isDynamicallyScalable()).thenReturn(true);
        when(serviceOffering.getLimitCpuUse()).thenReturn(true);
        when(internalLbVmDao.persist(any(DomainRouterVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DomainRouterVO result = service.createOrUpdateInternalLb(null, id, internalLbProviderId, account,
                userId, vpcId, serviceOffering, template1);
        verify(internalLbVmDao).persist(any(DomainRouterVO.class));
        assertEquals(template1.getId(), result.getTemplateId());
        assertTrue(result.isDynamicallyScalable());
        assertEquals(VirtualRouter.Role.INTERNAL_LB_VM, result.getRole());
        assertTrue(result.limitCpuUse());
    }

    @Test
    public void testCreateOrUpdateInternalLb_Update() {
        long id = 1L;
        long internalLbProviderId = 2L;
        long userId = 100L;
        Long vpcId = 200L;
        DomainRouterVO existing = mock(DomainRouterVO.class);
        when(existing.getId()).thenReturn(id);
        when(template1.isDynamicallyScalable()).thenReturn(true);
        final boolean[] dsResult = {false};
        doAnswer((Answer<Void>) invocation -> {
            dsResult[0] = invocation.getArgument(0);
            return null;
        }).when(existing).setDynamicallyScalable(anyBoolean());
        DomainRouterVO result = service.createOrUpdateInternalLb(existing, id, internalLbProviderId, account, userId, vpcId, serviceOffering, template1);
        verify(internalLbVmDao).update(existing.getId(), existing);
        assertEquals(template1.getId(), result.getTemplateId());
        assertTrue(dsResult[0]);
    }

    @Test
    public void testDeployInternalLbVmWithTemplates_SuccessfulFirstTemplate() throws Exception {
        long id = 1L;
        long internalLbProviderId = 2L;
        long userId = 100L;
        Long vpcId = 200L;
        List<VMTemplateVO> templates = Arrays.asList(template1);
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<>();
        when(internalLbVmDao.persist(any(DomainRouterVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(internalLbVmDao.findById(anyLong())).thenReturn(mock(DomainRouterVO.class));
        DomainRouterVO result = service.deployInternalLbVmWithTemplates(null, id, plan, internalLbProviderId, account, userId, vpcId, serviceOffering, networks, templates);
        assertNotNull(result);
        verify(virtualMachineManager).allocate(anyString(), eq(template1), eq(serviceOffering), eq(networks), eq(plan), isNull());
    }

    @Test
    public void testDeployInternalLbVmWithTemplates_RetryOnInsufficientCapacity() throws Exception {
        long id = 1L;
        long internalLbProviderId = 2L;
        long userId = 100L;
        Long vpcId = 200L;

        List<VMTemplateVO> templates = Arrays.asList(template1, template2);
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<>();
        when(internalLbVmDao.persist(any(DomainRouterVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(internalLbVmDao.findById(anyLong())).thenReturn(mock(DomainRouterVO.class));
        doThrow(new InsufficientServerCapacityException("Not enough capacity", id))
                .when(virtualMachineManager).allocate(anyString(), eq(template1), eq(serviceOffering), eq(networks), eq(plan), isNull());
        DomainRouterVO result = service.deployInternalLbVmWithTemplates(null, id, plan, internalLbProviderId, account, userId, vpcId, serviceOffering, networks, templates);
        assertNotNull(result);
        verify(virtualMachineManager).allocate(anyString(), eq(template1), eq(serviceOffering), eq(networks), eq(plan), isNull());
        verify(virtualMachineManager).allocate(anyString(), eq(template2), eq(serviceOffering), eq(networks), eq(plan), isNull());
    }

    @Test(expected = InsufficientCapacityException.class)
    public void testDeployInternalLbVmWithTemplates_AllTemplatesFail() throws Exception {
        long id = 1L;
        long internalLbProviderId = 2L;
        long userId = 100L;
        Long vpcId = 200L;
        List<VMTemplateVO> templates = Arrays.asList(template1, template2);
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<>();
        when(internalLbVmDao.persist(any(DomainRouterVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new InsufficientServerCapacityException("Insufficient capacity", id))
                .when(virtualMachineManager).allocate(anyString(), any(VMTemplateVO.class), eq(serviceOffering), eq(networks), eq(plan), isNull());
        service.deployInternalLbVmWithTemplates(null, id, plan, internalLbProviderId, account, userId, vpcId, serviceOffering, networks, templates);
    }
}
