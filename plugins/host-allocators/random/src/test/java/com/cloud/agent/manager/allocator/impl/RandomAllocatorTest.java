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

import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.capacity.CapacityManager;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachineProfile;

@RunWith(MockitoJUnitRunner.class)
public class RandomAllocatorTest {

    @Mock
    HostDao hostDao;
    @Mock
    CapacityManager capacityManager;
    @InjectMocks
    RandomAllocator randomAllocator;

    @Test
    public void testListHostsByTags() {
        Host.Type type = Host.Type.Routing;
        Long id = 1L;
        String templateTag = "tag1";
        String offeringTag = "tag2";
        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.listByHostTag(type, id, id, id, offeringTag)).thenReturn(List.of(host1, host2));

        // No template tagged host
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(new ArrayList<>());
        List<HostVO> result = randomAllocator.listHostsByTags(type, id, id, id, offeringTag, templateTag);
        Assert.assertTrue(CollectionUtils.isEmpty(result));

        // Different template tagged host
        HostVO host3 = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(List.of(host3));
        result = randomAllocator.listHostsByTags(type, id, id, id, offeringTag, templateTag);
        Assert.assertTrue(CollectionUtils.isEmpty(result));

        // Matching template tagged host
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(List.of(host1));
        result = randomAllocator.listHostsByTags(type, id, id, id, offeringTag, templateTag);
        Assert.assertFalse(CollectionUtils.isEmpty(result));
        Assert.assertEquals(1, result.size());

        // No template tag
        result = randomAllocator.listHostsByTags(type, id, id, id, offeringTag, null);
        Assert.assertFalse(CollectionUtils.isEmpty(result));
        Assert.assertEquals(2, result.size());

        // No offering tag
        result = randomAllocator.listHostsByTags(type, id, id, id, null, templateTag);
        Assert.assertFalse(CollectionUtils.isEmpty(result));
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testAllocateToUsesScopedRuleTagLookup() {
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
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(offering);
        Mockito.when(vmProfile.getTemplate()).thenReturn(template);
        Mockito.when(offering.getHostTag()).thenReturn(offeringTag);

        HostVO host = Mockito.mock(HostVO.class);
        List<Host> hosts = List.of(host);
        Mockito.when(hostDao.listByHostTag(type, clusterId, podId, dcId, offeringTag)).thenReturn(List.of(host));
        Mockito.when(hostDao.findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId)).thenReturn(new ArrayList<>());
        Mockito.when(capacityManager.checkIfHostHasCpuCapabilityAndCapacity(host, offering, true)).thenReturn(new Pair<>(true, true));

        List<Host> result = randomAllocator.allocateTo(vmProfile, plan, type, new ExcludeList(), hosts, 1, true);

        Assert.assertEquals(1, result.size());
        Mockito.verify(hostDao).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag, clusterId, podId, dcId);
        Mockito.verify(hostDao, Mockito.never()).findHostsWithTagRuleThatMatchComputeOferringTags(offeringTag);
    }
}
