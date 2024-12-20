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
import java.util.Arrays;
import java.util.List;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;

@RunWith(MockitoJUnitRunner.class)
public class RandomAllocatorTest {

    @Mock
    HostDao hostDao;

    @Spy
    @InjectMocks
    RandomAllocator randomAllocator;

    @Mock
    ResourceManager resourceManagerMock;

    private final Host.Type type = Host.Type.Routing;

    private final Long clusterId = 1L;

    private final Long podId = 1L;

    private final Long zoneId = 1L;

    private final List<HostVO> emptyList = new ArrayList<>();

    private final String hostTag = "hostTag";

    private final HostVO host1 = Mockito.mock(HostVO.class);

    private final HostVO host2 = Mockito.mock(HostVO.class);

    private final HostVO host3 = Mockito.mock(HostVO.class);

    private final VMTemplateVO vmTemplateVO = Mockito.mock(VMTemplateVO.class);

    private final ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);

    private final DeploymentPlanner.ExcludeList excludeList = Mockito.mock(DeploymentPlanner.ExcludeList.class);

    private final VirtualMachineProfile virtualMachineProfile = Mockito.mock(VirtualMachineProfile.class);

    private final DeploymentPlan deploymentPlan = Mockito.mock(DeploymentPlan.class);

    private final boolean considerReservedCapacity = true;


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
        ArrayList<HostVO> noTemplateTaggedHosts = new ArrayList<>(Arrays.asList(host1, host2));
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(new ArrayList<>());
        randomAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(noTemplateTaggedHosts, type, id, id, id, offeringTag, templateTag);
        Assert.assertTrue(CollectionUtils.isEmpty(noTemplateTaggedHosts));

        // Different template tagged host
        ArrayList<HostVO> differentTemplateTaggedHost = new ArrayList<>(Arrays.asList(host1, host2));
        HostVO host3 = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(List.of(host3));
        randomAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(differentTemplateTaggedHost, type, id, id, id, offeringTag, templateTag);
        Assert.assertTrue(CollectionUtils.isEmpty(differentTemplateTaggedHost));

        // Matching template tagged host
        ArrayList<HostVO> matchingTemplateTaggedHost = new ArrayList<>(Arrays.asList(host1, host2));
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(List.of(host1));
        randomAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(matchingTemplateTaggedHost, type, id, id, id, offeringTag, templateTag);
        Assert.assertFalse(CollectionUtils.isEmpty(matchingTemplateTaggedHost));
        Assert.assertEquals(1, matchingTemplateTaggedHost.size());

        // No template tag
        ArrayList<HostVO> noTemplateTag = new ArrayList<>(Arrays.asList(host1, host2));
        randomAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(noTemplateTag, type, id, id, id, offeringTag, null);
        Assert.assertFalse(CollectionUtils.isEmpty(noTemplateTag));
        Assert.assertEquals(2, noTemplateTag.size());

        // No offering tag
        ArrayList<HostVO> noOfferingTag = new ArrayList<>(Arrays.asList(host1, host2));
        randomAllocator.retainHostsMatchingServiceOfferingAndTemplateTags(noOfferingTag, type, id, id, id, null, templateTag);
        Assert.assertFalse(CollectionUtils.isEmpty(noOfferingTag));
        Assert.assertEquals(1, noOfferingTag.size());
    }

    @Test
    public void findSuitableHostsTestHostTypeStorageShouldReturnNull() {
        List<Host> suitableHosts = randomAllocator.findSuitableHosts(virtualMachineProfile, deploymentPlan, Host.Type.Storage, excludeList, null, HostAllocator.RETURN_UPTO_ALL, considerReservedCapacity);

        Assert.assertNull(suitableHosts);
    }

    @Test
    public void findSuitableHostsTestNoAvailableHostsShouldReturnNull() {
        Mockito.doReturn(serviceOffering).when(virtualMachineProfile).getServiceOffering();
        Mockito.doReturn(vmTemplateVO).when(virtualMachineProfile).getTemplate();
        Mockito.doReturn(hostTag).when(serviceOffering).getHostTag();
        Mockito.doReturn(emptyList).when(randomAllocator).retrieveHosts(Mockito.any(Host.Type.class), Mockito.nullable(List.class), Mockito.any(VMTemplateVO.class), Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        List<Host> suitableHosts = randomAllocator.findSuitableHosts(virtualMachineProfile, deploymentPlan, type, excludeList, null, HostAllocator.RETURN_UPTO_ALL, considerReservedCapacity);

        Assert.assertNull(suitableHosts);
    }

    @Test
    public void findSuitableHostsTestAvailableHostsShouldCallFilterAvailableHostsOnce() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2));

        Mockito.doReturn(serviceOffering).when(virtualMachineProfile).getServiceOffering();
        Mockito.doReturn(vmTemplateVO).when(virtualMachineProfile).getTemplate();
        Mockito.doReturn(hostTag).when(serviceOffering).getHostTag();
        Mockito.doReturn(hosts).when(randomAllocator).retrieveHosts(Mockito.any(Host.Type.class), Mockito.nullable(List.class), Mockito.any(VMTemplateVO.class), Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(hosts).when(randomAllocator).filterAvailableHosts(Mockito.any(DeploymentPlanner.ExcludeList.class), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyList(), Mockito.any(ServiceOffering.class));
        List<Host> suitableHosts = randomAllocator.findSuitableHosts(virtualMachineProfile, deploymentPlan, type, excludeList, null, HostAllocator.RETURN_UPTO_ALL, considerReservedCapacity);

        Mockito.verify(randomAllocator, Mockito.times(1)).filterAvailableHosts(Mockito.any(DeploymentPlanner.ExcludeList.class), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyList(), Mockito.any(ServiceOffering.class));
        Assert.assertEquals(2, suitableHosts.size());
    }

    @Test
    public void filterAvailableHostsTestAvailableHostsReachedReturnUpToLimitShouldReturnOnlyHostsWithinLimit() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2));
        int returnUpTo = 1;

        Mockito.doReturn(false).when(excludeList).shouldAvoid(Mockito.any(Host.class));
        Mockito.doReturn(true).when(randomAllocator).hostHasCpuCapabilityAndCapacity(Mockito.anyBoolean(), Mockito.any(ServiceOffering.class), Mockito.any(Host.class));
        List<Host> suitableHosts = randomAllocator.filterAvailableHosts(excludeList, returnUpTo, considerReservedCapacity, hosts, serviceOffering);

        Assert.assertEquals(1, suitableHosts.size());
    }

    @Test
    public void filterAvailableHostsTestReturnUpToAllShouldReturnAllAvailableHosts() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2));
        int returnUpTo = HostAllocator.RETURN_UPTO_ALL;

        Mockito.doReturn(false).when(excludeList).shouldAvoid(Mockito.any(Host.class));
        Mockito.doReturn(true).when(randomAllocator).hostHasCpuCapabilityAndCapacity(Mockito.anyBoolean(), Mockito.any(ServiceOffering.class), Mockito.any(Host.class));
        List<Host> suitableHosts = randomAllocator.filterAvailableHosts(excludeList, returnUpTo, considerReservedCapacity, hosts, serviceOffering);

        Assert.assertEquals(2, suitableHosts.size());
    }

    @Test
    public void filterAvailableHostsTestHost1InAvoidShouldOnlyReturnHost2() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2));
        int returnUpTo = HostAllocator.RETURN_UPTO_ALL;

        Mockito.doReturn(true).when(excludeList).shouldAvoid(host1);
        Mockito.doReturn(false).when(excludeList).shouldAvoid(host2);
        Mockito.doReturn(true).when(randomAllocator).hostHasCpuCapabilityAndCapacity(Mockito.anyBoolean(), Mockito.any(ServiceOffering.class), Mockito.any(Host.class));
        List<Host> suitableHosts = randomAllocator.filterAvailableHosts(excludeList, returnUpTo, considerReservedCapacity, hosts, serviceOffering);

        Assert.assertEquals(1, suitableHosts.size());
        Assert.assertEquals(host2, suitableHosts.get(0));
    }

    @Test
    public void filterAvailableHostsTestOnlyHost2HasCpuCapacityAndCapabilityShouldReturnOnlyHost2() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2));
        int returnUpTo = HostAllocator.RETURN_UPTO_ALL;

        Mockito.doReturn(false).when(excludeList).shouldAvoid(Mockito.any(Host.class));
        Mockito.doReturn(false).when(randomAllocator).hostHasCpuCapabilityAndCapacity(considerReservedCapacity, serviceOffering, host1);
        Mockito.doReturn(true).when(randomAllocator).hostHasCpuCapabilityAndCapacity(considerReservedCapacity, serviceOffering, host2);
        List<Host> suitableHosts = randomAllocator.filterAvailableHosts(excludeList, returnUpTo, considerReservedCapacity, hosts, serviceOffering);

        Assert.assertEquals(1, suitableHosts.size());
        Assert.assertEquals(host2, suitableHosts.get(0));
    }

    @Test
    public void retrieveHostsTestProvidedHostsNullAndNoHostTagAndNoTagRuleShouldOnlyReturnHostsWithNoTags() {
        List<HostVO> upAndEnabledHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsWithNoRuleTagsAndHostTags = new ArrayList<>(Arrays.asList(host1));

        Mockito.doReturn(upAndEnabledHosts).when(resourceManagerMock).listAllUpAndEnabledHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(hostsWithNoRuleTagsAndHostTags).when(hostDao).listAllHostsThatHaveNoRuleTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(emptyList).when(hostDao).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.nullable(String.class));
        Mockito.doNothing().when(randomAllocator).filterHostsBasedOnGuestOsRules(Mockito.any(VMTemplateVO.class), Mockito.anyList());
        List<HostVO> availableHosts = randomAllocator.retrieveHosts(type, null, vmTemplateVO, null, clusterId, podId, zoneId);

        Assert.assertEquals(1, availableHosts.size());
        Assert.assertEquals(host1, availableHosts.get(0));
    }

    @Test
    public void retrieveHostsTestProvidedHostsNullAndOnlyHostTagsRulesShouldReturnHostsThatMatchRuleTagsAndHostsWithNoTags() {
        List<HostVO> upAndEnabledHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsWithNoRuleTagsAndHostTags = new ArrayList<>(Arrays.asList(host1));
        List<HostVO> hostsMatchingRuleTags = new ArrayList<>(Arrays.asList(host2));

        Mockito.doReturn(upAndEnabledHosts).when(resourceManagerMock).listAllUpAndEnabledHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(hostsWithNoRuleTagsAndHostTags).when(hostDao).listAllHostsThatHaveNoRuleTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(hostsMatchingRuleTags).when(hostDao).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.nullable(String.class));
        Mockito.doNothing().when(randomAllocator).filterHostsBasedOnGuestOsRules(Mockito.any(VMTemplateVO.class), Mockito.anyList());
        List<HostVO> availableHosts = randomAllocator.retrieveHosts(type, null, vmTemplateVO, null, clusterId, podId, zoneId);

        Assert.assertEquals(2, availableHosts.size());
        Assert.assertEquals(host1, availableHosts.get(0));
        Assert.assertEquals(host2, availableHosts.get(1));
    }

    @Test
    public void retrieveHostsTestProvidedHostsNullProvidedHostTagsNotNullAndNoHostWithMatchingRuleTagsShouldReturnHostWithMatchingTags() {
        List<HostVO> upAndEnabledHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsWithMatchingTags = new ArrayList<>(Arrays.asList(host1));

        Mockito.doReturn(upAndEnabledHosts).when(resourceManagerMock).listAllUpAndEnabledHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(hostsWithMatchingTags).when(hostDao).listByHostTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(emptyList).when(hostDao).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.nullable(String.class));
        Mockito.doNothing().when(randomAllocator).filterHostsBasedOnGuestOsRules(Mockito.any(VMTemplateVO.class), Mockito.anyList());
        List<HostVO> availableHosts = randomAllocator.retrieveHosts(type, null, vmTemplateVO, hostTag, clusterId, podId, zoneId);

        Assert.assertEquals(1, availableHosts.size());
        Assert.assertEquals(host1, availableHosts.get(0));
    }

    @Test
    public void retrieveHostsTestProvidedHostsNullProvidedHostTagsNotNullAndHostWithMatchingRuleTagsShouldReturnHostWithHostMatchingTagsAndRuleTags() {
        List<HostVO> upAndEnabledHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsWithMatchingTags = new ArrayList<>(Arrays.asList(host1));
        List<HostVO> hostsMatchingRuleTags = new ArrayList<>(Arrays.asList(host3));

        Mockito.doReturn(upAndEnabledHosts).when(resourceManagerMock).listAllUpAndEnabledHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(hostsWithMatchingTags).when(hostDao).listByHostTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(hostsMatchingRuleTags).when(hostDao).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.nullable(String.class));
        Mockito.doNothing().when(randomAllocator).filterHostsBasedOnGuestOsRules(Mockito.any(VMTemplateVO.class), Mockito.anyList());
        List<HostVO> availableHosts = randomAllocator.retrieveHosts(type, null, vmTemplateVO, hostTag, clusterId, podId, zoneId);

        Assert.assertEquals(2, availableHosts.size());
        Assert.assertEquals(host1, availableHosts.get(0));
        Assert.assertEquals(host3, availableHosts.get(1));
    }

    @Test
    public void retrieveHostsTestProvidedHostsNotNullAndNoHostTagAndNoTagRuleShouldOnlyReturnHostsWithNoTags() {
        List<HostVO> providedHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsWithNoRuleTagsAndHostTags = new ArrayList<>(Arrays.asList(host1));

        Mockito.doReturn(hostsWithNoRuleTagsAndHostTags).when(hostDao).listAllHostsThatHaveNoRuleTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(emptyList).when(hostDao).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.nullable(String.class));
        Mockito.doNothing().when(randomAllocator).filterHostsBasedOnGuestOsRules(Mockito.any(VMTemplateVO.class), Mockito.anyList());
        List<HostVO> availableHosts = randomAllocator.retrieveHosts(type, providedHosts, vmTemplateVO, null, clusterId, podId, zoneId);

        Assert.assertEquals(1, availableHosts.size());
        Assert.assertEquals(host1, availableHosts.get(0));
    }

    @Test
    public void retrieveHostsTestProvidedHostsNotNullAndOnlyHostTagsRulesShouldReturnHostsThatMatchRuleTagsAndHostsWithNoTags() {
        List<HostVO> providedHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsWithNoRuleTagsAndHostTags = new ArrayList<>(Arrays.asList(host1));
        List<HostVO> hostsMatchingRuleTags = new ArrayList<>(Arrays.asList(host2));

        Mockito.doReturn(hostsWithNoRuleTagsAndHostTags).when(hostDao).listAllHostsThatHaveNoRuleTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(hostsMatchingRuleTags).when(hostDao).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.nullable(String.class));
        Mockito.doNothing().when(randomAllocator).filterHostsBasedOnGuestOsRules(Mockito.any(VMTemplateVO.class), Mockito.anyList());
        List<HostVO> availableHosts = randomAllocator.retrieveHosts(type, providedHosts, vmTemplateVO, null, clusterId, podId, zoneId);

        Assert.assertEquals(2, availableHosts.size());
        Assert.assertEquals(host1, availableHosts.get(0));
        Assert.assertEquals(host2, availableHosts.get(1));
    }

    @Test
    public void retrieveHostsTestProvidedHostsNotNullProvidedHostTagsNotNullAndNoHostWithMatchingRuleTagsShouldReturnHostWithMatchingTags() {
        List<HostVO> providedHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsWithMatchingTags = new ArrayList<>(Arrays.asList(host1));

        Mockito.doReturn(hostsWithMatchingTags).when(hostDao).listByHostTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(emptyList).when(hostDao).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.nullable(String.class));
        Mockito.doNothing().when(randomAllocator).filterHostsBasedOnGuestOsRules(Mockito.any(VMTemplateVO.class), Mockito.anyList());
        List<HostVO> availableHosts = randomAllocator.retrieveHosts(type, providedHosts, vmTemplateVO, hostTag, clusterId, podId, zoneId);

        Assert.assertEquals(1, availableHosts.size());
        Assert.assertEquals(host1, availableHosts.get(0));
    }

    @Test
    public void retrieveHostsTestProvidedHostsNullNotProvidedHostTagsNotNullAndHostWithMatchingRuleTagsShouldReturnHostWithHostMatchingTagsAndRuleTags() {
        List<HostVO> providedHosts = new ArrayList<>(Arrays.asList(host1, host2));
        List<HostVO> hostsWithMatchingTags = new ArrayList<>(Arrays.asList(host1));
        List<HostVO> hostsMatchingRuleTags = new ArrayList<>(Arrays.asList(host3));

        Mockito.doReturn(hostsWithMatchingTags).when(hostDao).listByHostTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(hostsMatchingRuleTags).when(hostDao).findHostsWithTagRuleThatMatchComputeOfferingTags(Mockito.nullable(String.class));
        Mockito.doNothing().when(randomAllocator).filterHostsBasedOnGuestOsRules(Mockito.any(VMTemplateVO.class), Mockito.anyList());
        List<HostVO> availableHosts = randomAllocator.retrieveHosts(type, providedHosts, vmTemplateVO, hostTag, clusterId, podId, zoneId);

        Assert.assertEquals(2, availableHosts.size());
        Assert.assertEquals(host1, availableHosts.get(0));
        Assert.assertEquals(host3, availableHosts.get(1));
    }
}
