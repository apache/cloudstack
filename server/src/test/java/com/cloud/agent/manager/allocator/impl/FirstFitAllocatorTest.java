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

        DeploymentPlan plan = Mockito.mock(DeploymentPlan.class);
        Mockito.when(plan.getDataCenterId()).thenReturn(dcId);
        Mockito.when(plan.getPodId()).thenReturn(podId);
        Mockito.when(plan.getClusterId()).thenReturn(clusterId);

        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(offering);
        Mockito.when(vmProfile.getTemplate()).thenReturn(template);
        Mockito.when(vmProfile.getOwner()).thenReturn(account);
        Mockito.when(offering.getHostTag()).thenReturn(offeringTag);

        HostVO host = Mockito.mock(HostVO.class);
        List<Host> selectedHosts = List.of(host);
        Mockito.when(hostDao.listByHostTag(type, clusterId, podId, dcId, offeringTag)).thenReturn(List.of(host));
        Mockito.when(hostDao.findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId)).thenReturn(new ArrayList<>());
        Mockito.doReturn(selectedHosts).when(firstFitAllocator).allocateTo(
                Mockito.eq(plan), Mockito.eq(offering), Mockito.eq(template), Mockito.any(ExcludeList.class), Mockito.anyList(), Mockito.eq(1), Mockito.eq(true), Mockito.eq(account));

        List<Host> result = firstFitAllocator.allocateTo(vmProfile, plan, type, new ExcludeList(), selectedHosts, 1, true);

        Assert.assertEquals(1, result.size());
        Mockito.verify(hostDao).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId);
        Mockito.verify(hostDao, Mockito.never()).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag);
    }

    @Test
    public void testAllocateToWithoutHostsUsesScopedRuleTagLookup() {
        Host.Type type = Host.Type.Routing;
        long dcId = 1L;
        Long podId = 2L;
        Long clusterId = 3L;
        String offeringTag = "compute";

        DeploymentPlan plan = Mockito.mock(DeploymentPlan.class);
        Mockito.when(plan.getDataCenterId()).thenReturn(dcId);
        Mockito.when(plan.getPodId()).thenReturn(podId);
        Mockito.when(plan.getClusterId()).thenReturn(clusterId);

        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(offering);
        Mockito.when(vmProfile.getTemplate()).thenReturn(template);
        Mockito.when(vmProfile.getOwner()).thenReturn(account);
        Mockito.when(offering.getHostTag()).thenReturn(offeringTag);
        Mockito.when(userVmDetailsDao.findDetail(Mockito.anyLong(), Mockito.eq("UEFI"))).thenReturn(null);

        HostVO host = Mockito.mock(HostVO.class);
        List<Host> selectedHosts = List.of(host);
        Mockito.when(hostDao.listByHostTag(type, clusterId, podId, dcId, offeringTag)).thenReturn(new ArrayList<>(List.of(host)));
        Mockito.when(hostDao.findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId)).thenReturn(new ArrayList<>());
        Mockito.when(hostDao.listAllUpAndEnabledNonHAHosts(type, clusterId, podId, dcId, null)).thenReturn(new ArrayList<>());
        Mockito.doReturn(selectedHosts).when(firstFitAllocator).allocateTo(
                Mockito.eq(plan), Mockito.eq(offering), Mockito.eq(template), Mockito.any(ExcludeList.class), Mockito.anyList(), Mockito.eq(1), Mockito.eq(true), Mockito.eq(account));

        List<Host> result = firstFitAllocator.allocateTo(vmProfile, plan, type, new ExcludeList(), 1, true);

        Assert.assertEquals(1, result.size());
        Mockito.verify(hostDao).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId);
        Mockito.verify(hostDao, Mockito.never()).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag);
    }
}
