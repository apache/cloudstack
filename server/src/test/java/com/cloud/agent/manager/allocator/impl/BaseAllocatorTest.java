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

import com.cloud.capacity.CapacityManager;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachineProfile;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class BaseAllocatorTest {
    @Mock
    HostDao hostDaoMock;

    @Mock
    CapacityManager capacityManagerMock;

    @InjectMocks
    @Spy
    BaseAllocator baseAllocator = new MockBaseAllocator();

    private final Host.Type type = Host.Type.Routing;

    private final Long clusterId = 1L;

    private final Long podId = 1L;

    private final Long dcId = 1L;

    private final HostVO host1 = Mockito.mock(HostVO.class);

    private final HostVO host2 = Mockito.mock(HostVO.class);

    private final HostVO host3 = Mockito.mock(HostVO.class);

    private final ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);

    private final String hostTag = "hostTag";

    @Test
    public void retainHostsMatchingServiceOfferingAndTemplateTagsTestHasServiceOfferingTagShouldRetainHostsWithServiceOfferingTag() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> hostsWithMathingTags = new ArrayList<>(Arrays.asList(host1, host3));
        String hostTagOnTemplate = "hostTagOnTemplate";
        String hostTagOnOffering = null;

        Mockito.doReturn(hostsWithMathingTags).when(hostDaoMock).listByHostTag(type, clusterId, podId, dcId, hostTagOnTemplate);
        baseAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(suitableHosts, type, clusterId, podId, dcId, hostTagOnTemplate, hostTagOnOffering);

        Assert.assertEquals(2, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host3, suitableHosts.get(1));
    }

    @Test
    public void retainHostsMatchingServiceOfferingAndTemplateTagsTestHasServiceOfferingTagAndHasHostTagOnTemplateShouldRetainHostsWithServiceOfferingTagAndTemplateTag() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> hostsWithMathingServiceTags = new ArrayList<>(Arrays.asList(host1, host3));
        List<HostVO> hostsWithMathingTemplateTags = new ArrayList<>(Arrays.asList(host1, host2));
        String hostTagOnTemplate = "hostTagOnTemplate";
        String hostTagOnOffering = "hostTagOnOffering";

        Mockito.doReturn(hostsWithMathingTemplateTags).when(hostDaoMock).listByHostTag(type, clusterId, podId, dcId, hostTagOnTemplate);
        Mockito.doReturn(hostsWithMathingServiceTags).when(hostDaoMock).listByHostTag(type, clusterId, podId, dcId, hostTagOnOffering);
        baseAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(suitableHosts, type, clusterId, podId, dcId, hostTagOnTemplate, hostTagOnOffering);

        Assert.assertEquals(1, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
    }

    @Test
    public void retainHostsMatchingServiceOfferingAndTemplateTagsTestHasHostTagOnTemplateShouldRetainHostsWithTemplateTag() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> hostsWithMathingServiceTags = new ArrayList<>(Arrays.asList(host1, host3));
        String hostTagOnTemplate = null;
        String hostTagOnOffering = "hostTagOnOffering";

        Mockito.doReturn(hostsWithMathingServiceTags).when(hostDaoMock).listByHostTag(type, clusterId, podId, dcId, hostTagOnOffering);
        baseAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(suitableHosts, type, clusterId, podId, dcId, hostTagOnTemplate, hostTagOnOffering);

        Assert.assertEquals(2, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host3, suitableHosts.get(1));
    }

    @Test
    public void retainHostsMatchingServiceOfferingAndTemplateTagsTestNoServiceTagAndNoTemplateTagShouldHaveAllSuitableHosts() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        String hostTagOnTemplate = null;
        String hostTagOnOffering = null;

        baseAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(suitableHosts, type, clusterId, podId, dcId, hostTagOnTemplate, hostTagOnOffering);

        Assert.assertEquals(3, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host2, suitableHosts.get(1));
        Assert.assertEquals(host3, suitableHosts.get(2));
    }

    @Test
    public void addHostsBasedOnTagRulesTestHostsWithTagRuleIsEmptyShouldNotAddToSuitableHosts() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> emptyList = new ArrayList<>();

        Mockito.doReturn(emptyList).when(hostDaoMock).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.anyString());
        baseAllocator.addHostsBasedOnTagRules(hostTag, suitableHosts);

        Assert.assertEquals(2, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host2, suitableHosts.get(1));
    }

    @Test
    public void addHostsBasedOnTagRulesTestHostsWithTagRuleIsNotEmptyShouldAddToSuitableHosts() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsMatchingRuleTag = new ArrayList<>(Arrays.asList(host3));

        Mockito.doReturn(hostsMatchingRuleTag).when(hostDaoMock).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.anyString());
        baseAllocator.addHostsBasedOnTagRules(hostTag, suitableHosts);

        Assert.assertEquals(3, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host2, suitableHosts.get(1));
        Assert.assertEquals(host3, suitableHosts.get(2));
    }

    @Test
    public void hostHasCpuCapabilityAndCapacityTestHostHasCpuCapabilityAndCpuCapacityShouldReturnTrue() {
        Boolean hasCpuCapability = true;
        Boolean hasCpuCapacity = true;
        Pair<Boolean, Boolean> pair = new Pair<>(hasCpuCapability, hasCpuCapacity);

        Mockito.doReturn(pair).when(capacityManagerMock).checkIfHostHasCpuCapabilityAndCapacity(Mockito.any(Host.class), Mockito.any(ServiceOffering.class), Mockito.anyBoolean());
        boolean result = baseAllocator.hostHasCpuCapabilityAndCapacity(true, serviceOffering, host1);

        Assert.assertTrue(result);
    }

    @Test
    public void hostHasCpuCapabilityAndCapacityTestHostHasCpuCapabilityButNoCpuCapacityShouldReturnFalse() {
        Boolean hasCpuCapability = true;
        Boolean hasCpuCapacity = false;
        Pair<Boolean, Boolean> pair = new Pair<>(hasCpuCapability, hasCpuCapacity);

        Mockito.doReturn(pair).when(capacityManagerMock).checkIfHostHasCpuCapabilityAndCapacity(Mockito.any(Host.class), Mockito.any(ServiceOffering.class), Mockito.anyBoolean());
        boolean result = baseAllocator.hostHasCpuCapabilityAndCapacity(true, serviceOffering, host1);

        Assert.assertFalse(result);
    }

    @Test
    public void hostHasCpuCapabilityAndCapacityTestHostDoesNotHaveCpuCapabilityButHasCpuCapacityShouldReturnFalse() {
        Boolean hasCpuCapability = false;
        Boolean hasCpuCapacity = true;
        Pair<Boolean, Boolean> pair = new Pair<>(hasCpuCapability, hasCpuCapacity);

        Mockito.doReturn(pair).when(capacityManagerMock).checkIfHostHasCpuCapabilityAndCapacity(Mockito.any(Host.class), Mockito.any(ServiceOffering.class), Mockito.anyBoolean());
        boolean result = baseAllocator.hostHasCpuCapabilityAndCapacity(true, serviceOffering, host1);

        Assert.assertFalse(result);
    }

    @Test
    public void hostHasCpuCapabilityAndCapacityTestHostDoesNotHaveCpuCapabilityAndCpuCapacityShouldReturnFalse() {
        Boolean hasCpuCapability = false;
        Boolean hasCpuCapacity = false;
        Pair<Boolean, Boolean> pair = new Pair<>(hasCpuCapability, hasCpuCapacity);

        Mockito.doReturn(pair).when(capacityManagerMock).checkIfHostHasCpuCapabilityAndCapacity(Mockito.any(Host.class), Mockito.any(ServiceOffering.class), Mockito.anyBoolean());
        boolean result = baseAllocator.hostHasCpuCapabilityAndCapacity(true, serviceOffering, host1);

        Assert.assertFalse(result);
    }

    class MockBaseAllocator extends BaseAllocator {

        @Override
        public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Host.Type type, DeploymentPlanner.ExcludeList avoid, int returnUpTo) {
            return null;
        }

        @Override
        public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Host.Type type, DeploymentPlanner.ExcludeList avoid, List<? extends Host> hosts, int returnUpTo, boolean considerReservedCapacity) {
            return null;
        }
    }
}
