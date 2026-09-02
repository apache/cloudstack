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
package com.cloud.agent.manager.allocator.impl;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDetailsDao;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirstFitAllocatorTest {

    @Mock
    HostDao hostDao;
    @Mock
    UserVmDetailsDao userVmDetailsDao;
    @Spy
    @InjectMocks
    FirstFitAllocator firstFitAllocator;

    @Test
    public void testAllocateToWithHostsUsesScopedRuleTagLookup() {
        Host.Type type = Host.Type.Routing;
        long dcId = 1L;
        Long podId = 2L;
        Long clusterId = 3L;
        String offeringTag = "compute";

        DeploymentPlan plan = mock(DeploymentPlan.class);
        when(plan.getDataCenterId()).thenReturn(dcId);
        when(plan.getPodId()).thenReturn(podId);
        when(plan.getClusterId()).thenReturn(clusterId);

        VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);
        ServiceOffering offering = mock(ServiceOffering.class);
        VMTemplateVO template = mock(VMTemplateVO.class);
        Account account = mock(Account.class);
        when(vmProfile.getServiceOffering()).thenReturn(offering);
        when(vmProfile.getTemplate()).thenReturn(template);
        when(vmProfile.getOwner()).thenReturn(account);
        when(offering.getHostTag()).thenReturn(offeringTag);

        HostVO host = mock(HostVO.class);
        List<Host> selectedHosts = List.of(host);
        when(hostDao.listByHostTag(type, clusterId, podId, dcId, offeringTag)).thenReturn(List.of(host));
        when(hostDao.findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId)).thenReturn(new ArrayList<>());
        doReturn(selectedHosts).when(firstFitAllocator).allocateTo(
                Mockito.eq(plan), Mockito.eq(offering), Mockito.eq(template), Mockito.any(ExcludeList.class), Mockito.anyList(), Mockito.eq(1), Mockito.eq(true), Mockito.eq(account));

        List<Host> result = firstFitAllocator.allocateTo(vmProfile, plan, type, new ExcludeList(), selectedHosts, 1, true);

        Assert.assertEquals(1, result.size());
        verify(hostDao).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId);
        verify(hostDao, never()).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag);
    }

    @Test
    public void testAllocateToWithoutHostsUsesScopedRuleTagLookup() {
        Host.Type type = Host.Type.Routing;
        long dcId = 1L;
        Long podId = 2L;
        Long clusterId = 3L;
        String offeringTag = "compute";

        DeploymentPlan plan = mock(DeploymentPlan.class);
        when(plan.getDataCenterId()).thenReturn(dcId);
        when(plan.getPodId()).thenReturn(podId);
        when(plan.getClusterId()).thenReturn(clusterId);

        VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);
        ServiceOffering offering = mock(ServiceOffering.class);
        VMTemplateVO template = mock(VMTemplateVO.class);
        Account account = mock(Account.class);
        when(vmProfile.getServiceOffering()).thenReturn(offering);
        when(vmProfile.getTemplate()).thenReturn(template);
        when(vmProfile.getOwner()).thenReturn(account);
        when(offering.getHostTag()).thenReturn(offeringTag);
        when(userVmDetailsDao.findDetail(Mockito.anyLong(), Mockito.eq("UEFI"))).thenReturn(null);

        HostVO host = mock(HostVO.class);
        List<Host> selectedHosts = List.of(host);
        when(hostDao.listByHostTag(type, clusterId, podId, dcId, offeringTag)).thenReturn(new ArrayList<>(List.of(host)));
        when(hostDao.findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId)).thenReturn(new ArrayList<>());
        when(hostDao.listAllUpAndEnabledNonHAHosts(type, clusterId, podId, dcId, null)).thenReturn(new ArrayList<>());
        doReturn(selectedHosts).when(firstFitAllocator).allocateTo(
                Mockito.eq(plan), Mockito.eq(offering), Mockito.eq(template), Mockito.any(ExcludeList.class), Mockito.anyList(), Mockito.eq(1), Mockito.eq(true), Mockito.eq(account));

        List<Host> result = firstFitAllocator.allocateTo(vmProfile, plan, type, new ExcludeList(), 1, true);

        Assert.assertEquals(1, result.size());
        verify(hostDao).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId);
        verify(hostDao, never()).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag);
    }
}
