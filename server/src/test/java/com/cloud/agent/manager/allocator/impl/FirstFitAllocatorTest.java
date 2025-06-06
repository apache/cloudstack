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

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.capacity.CapacityManager;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDetailsDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class FirstFitAllocatorTest {

    @Mock
    HostDao hostDaoMock;

    @Mock
    ResourceManager resourceManagerMock;

    @Mock
    UserVmDetailsDao userVmDetailsDaoMock;

    @Mock
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @Mock
    CapacityManager capacityMgr;

    @Mock
    ConfigurationDao configDao;

    @Spy
    @InjectMocks
    FirstFitAllocator firstFitAllocatorSpy;

    private final Host.Type type = Host.Type.Routing;

    private final Long clusterId = 1L;

    private final Long podId = 1L;

    private final Long dcId = 1L;

    private final List<HostVO> emptyList = new ArrayList<>();

    private final String hostTag = "hostTag";

    private final String templateTag = "templateTag";

    private final HostVO host1 = Mockito.mock(HostVO.class);

    private final HostVO host2 = Mockito.mock(HostVO.class);

    private final HostVO host3 = Mockito.mock(HostVO.class);

    private final ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);

    private final DeploymentPlanner.ExcludeList excludeList = Mockito.mock(DeploymentPlanner.ExcludeList.class);

    private final VirtualMachineProfile virtualMachineProfile = Mockito.mock(VirtualMachineProfile.class);

    private final VMTemplateVO vmTemplateVO = Mockito.mock(VMTemplateVO.class);

    private final Account account = Mockito.mock(Account.class);

    private final DeploymentPlan deploymentPlan = Mockito.mock(DeploymentPlan.class);

    private final DeploymentPlanner.ExcludeList avoid = Mockito.mock(DeploymentPlanner.ExcludeList.class);

    private final ServiceOffering offering = Mockito.mock(ServiceOffering.class);

    private final boolean considerReservedCapacity = true;

    @Before
    public void setUp() {
        Mockito.when(deploymentPlan.getDataCenterId()).thenReturn(1L);
        Mockito.when(offering.getId()).thenReturn(123L);
    }

    @Test
    public void testConfigure() throws Exception {
        Mockito.when(configDao.getConfiguration(ArgumentMatchers.anyMap())).thenReturn(new HashMap<>());
        Assert.assertTrue(firstFitAllocatorSpy._checkHvm);
        Assert.assertTrue(firstFitAllocatorSpy.configure("test", new HashMap<>()));
    }

    @Test
    public void testAllocateTo_SuccessfulMatch() {
        List<Host> inputHosts = Arrays.asList(host1, host2);

        // All hosts are allowed
        Mockito.when(avoid.shouldAvoid(host1)).thenReturn(false);
        Mockito.when(avoid.shouldAvoid(host2)).thenReturn(false);

        // No GPU requirement
        Mockito.when(serviceOfferingDetailsDao.findDetail(ArgumentMatchers.eq(123L), ArgumentMatchers.anyString())).thenReturn(null);

        // CPU capability and capacity is met
        Mockito.when(capacityMgr.checkIfHostReachMaxGuestLimit(ArgumentMatchers.any())).thenReturn(false);
        Mockito.when(capacityMgr.checkIfHostHasCpuCapabilityAndCapacity(ArgumentMatchers.eq(host1), ArgumentMatchers.eq(offering),
                        ArgumentMatchers.eq(true)))
                .thenReturn(new Pair<>(true, true));
        Mockito.when(capacityMgr.checkIfHostHasCpuCapabilityAndCapacity(ArgumentMatchers.eq(host2), ArgumentMatchers.eq(offering),
                        ArgumentMatchers.eq(true)))
                .thenReturn(new Pair<>(true, false));

        List<Host> result = firstFitAllocatorSpy.allocateTo(deploymentPlan, offering, null, avoid, inputHosts, 2, true, account);

        // Only host1 should be returned
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(host1));
        Assert.assertFalse(result.contains(host2));
    }

    @Test
    public void testAllocateTo_AvoidSetAndGuestLimit() {
        List<Host> inputHosts = Arrays.asList(host1, host2);

        Mockito.when(avoid.shouldAvoid(host1)).thenReturn(true); // Avoided
        Mockito.when(avoid.shouldAvoid(host2)).thenReturn(false);

        Mockito.when(capacityMgr.checkIfHostReachMaxGuestLimit(host2)).thenReturn(true); // Reached limit

        List<Host> result = firstFitAllocatorSpy.allocateTo(deploymentPlan, offering, null, avoid, inputHosts, 2, true, account);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testAllocateTo_GPUNotAvailable() {
        List<Host> inputHosts = Arrays.asList(host1);
        Mockito.when(avoid.shouldAvoid(host1)).thenReturn(false);

        // GPU required but not available
        var vgpuDetail = Mockito.mock(com.cloud.service.ServiceOfferingDetailsVO.class);
        var pciDetail = Mockito.mock(com.cloud.service.ServiceOfferingDetailsVO.class);
        Mockito.when(serviceOfferingDetailsDao.findDetail(ArgumentMatchers.eq(123L), ArgumentMatchers.eq("vgpuType"))).thenReturn(vgpuDetail);
        Mockito.when(serviceOfferingDetailsDao.findDetail(ArgumentMatchers.eq(123L), ArgumentMatchers.eq("pciDevice"))).thenReturn(pciDetail);
        Mockito.when(pciDetail.getValue()).thenReturn("NVIDIA");
        Mockito.when(vgpuDetail.getValue()).thenReturn("GRID");

        Mockito.when(resourceManagerMock.isGPUDeviceAvailable(ArgumentMatchers.eq(host1), ArgumentMatchers.eq("NVIDIA"), ArgumentMatchers.eq("GRID"))).thenReturn(false);

        List<Host> result = firstFitAllocatorSpy.allocateTo(deploymentPlan, offering, null, avoid, inputHosts, 1, true, account);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void allocateToTestHostTypeStorageShouldReturnNull() {
        List<Host> suitableHosts = firstFitAllocatorSpy.allocateTo(virtualMachineProfile, deploymentPlan, Host.Type.Storage, excludeList, null, HostAllocator.RETURN_UPTO_ALL, considerReservedCapacity);

        Assert.assertNull(suitableHosts);
    }

    @Test
    public void allocateToTestSuitableHostsEmptyShouldReturnNull() {
        Mockito.doReturn(serviceOffering).when(virtualMachineProfile).getServiceOffering();
        Mockito.doReturn(vmTemplateVO).when(virtualMachineProfile).getTemplate();
        Mockito.doReturn(account).when(virtualMachineProfile).getOwner();
        Mockito.doReturn(hostTag).when(serviceOffering).getHostTag();
        Mockito.doReturn(templateTag).when(vmTemplateVO).getTemplateTag();
        Mockito.doReturn(emptyList).when(firstFitAllocatorSpy).retrieveHosts(Mockito.any(VirtualMachineProfile.class), Mockito.any(Host.Type.class), Mockito.nullable(List.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
        List<Host> suitableHosts = firstFitAllocatorSpy.allocateTo(virtualMachineProfile, deploymentPlan, type, excludeList, null, HostAllocator.RETURN_UPTO_ALL, considerReservedCapacity);

        Assert.assertNull(suitableHosts);
    }

    @Test
    public void allocateToTestSuitableHostsNotEmptyShouldCallAllocateToMethod() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2));

        Mockito.doReturn(serviceOffering).when(virtualMachineProfile).getServiceOffering();
        Mockito.doReturn(vmTemplateVO).when(virtualMachineProfile).getTemplate();
        Mockito.doReturn(account).when(virtualMachineProfile).getOwner();
        Mockito.doReturn(hostTag).when(serviceOffering).getHostTag();
        Mockito.doReturn(templateTag).when(vmTemplateVO).getTemplateTag();
        Mockito.doReturn(hosts).when(firstFitAllocatorSpy).retrieveHosts(Mockito.any(VirtualMachineProfile.class), Mockito.any(Host.Type.class), Mockito.nullable(List.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(hosts).when(firstFitAllocatorSpy).allocateTo(Mockito.any(DeploymentPlan.class), Mockito.any(ServiceOffering.class), Mockito.any(VMTemplateVO.class), Mockito.any(DeploymentPlanner.ExcludeList.class), Mockito.anyList(), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.any(Account.class));
        Mockito.doNothing().when(firstFitAllocatorSpy).addHostsToAvoidSet(Mockito.any(Host.Type.class), Mockito.any(DeploymentPlanner.ExcludeList.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList());
        List<Host> suitableHosts = firstFitAllocatorSpy.allocateTo(virtualMachineProfile, deploymentPlan, type, excludeList, null, HostAllocator.RETURN_UPTO_ALL, considerReservedCapacity);

        Mockito.verify(firstFitAllocatorSpy, Mockito.times(1)).allocateTo(deploymentPlan, serviceOffering, vmTemplateVO, excludeList, hosts, HostAllocator.RETURN_UPTO_ALL, considerReservedCapacity, account);
        Assert.assertEquals(2, suitableHosts.size());
    }

    @Test
    public void allocateToTestProvidedHostsNotNullShouldCallAddHostsToAvoidSetMethod() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2));

        Mockito.doReturn(serviceOffering).when(virtualMachineProfile).getServiceOffering();
        Mockito.doReturn(vmTemplateVO).when(virtualMachineProfile).getTemplate();
        Mockito.doReturn(account).when(virtualMachineProfile).getOwner();
        Mockito.doReturn(hostTag).when(serviceOffering).getHostTag();
        Mockito.doReturn(templateTag).when(vmTemplateVO).getTemplateTag();
        Mockito.doReturn(hosts).when(firstFitAllocatorSpy).retrieveHosts(Mockito.any(VirtualMachineProfile.class), Mockito.any(Host.Type.class), Mockito.nullable(List.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(hosts).when(firstFitAllocatorSpy).allocateTo(Mockito.any(DeploymentPlan.class), Mockito.any(ServiceOffering.class), Mockito.any(VMTemplateVO.class), Mockito.any(DeploymentPlanner.ExcludeList.class), Mockito.anyList(), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.any(Account.class));
        Mockito.doNothing().when(firstFitAllocatorSpy).addHostsToAvoidSet(Mockito.any(Host.Type.class), Mockito.any(DeploymentPlanner.ExcludeList.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList());
        firstFitAllocatorSpy.allocateTo(virtualMachineProfile, deploymentPlan, type, excludeList, null, HostAllocator.RETURN_UPTO_ALL, considerReservedCapacity);

        Mockito.verify(firstFitAllocatorSpy, Mockito.times(1)).addHostsToAvoidSet(Mockito.any(Host.Type.class), Mockito.any(DeploymentPlanner.ExcludeList.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList());
    }

    @Test
    public void retrieveHostsTestHostsToFilterIsNullAndHaTagNotNullShouldReturnOnlyHostsWithHaTag() {
        List<HostVO> allUpAndEnabledHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> hostsWithHaTag = new ArrayList<>(Arrays.asList(host1, host2));
        String hostVmTag = "haVmTag";

        Mockito.doReturn(hostVmTag).when(virtualMachineProfile).getParameter(Mockito.any(VirtualMachineProfile.Param.class));
        Mockito.doReturn(allUpAndEnabledHosts).when(resourceManagerMock).listAllUpAndEnabledHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(hostsWithHaTag).when(hostDaoMock).listByHostTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(firstFitAllocatorSpy).filterHostsWithUefiEnabled(Mockito.any(Host.Type.class), Mockito.any(VirtualMachineProfile.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList());
        Mockito.doNothing().when(firstFitAllocatorSpy).addHostsBasedOnTagRules(Mockito.anyString(), Mockito.anyList());
        List<HostVO> resultHosts = firstFitAllocatorSpy.retrieveHosts(virtualMachineProfile, type, emptyList, clusterId, podId, dcId, hostTag, templateTag);

        Assert.assertEquals(2, resultHosts.size());
        Assert.assertEquals(host1, resultHosts.get(0));
        Assert.assertEquals(host2, resultHosts.get(1));
    }

    @Test
    public void retrieveHostsTestHostsToFilterIsNotNullAndHaTagNotNullShouldReturnOnlyHostsToFilterWithHaTag() {
        List<HostVO> hostsToFilter = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> hostsWithHaTag = new ArrayList<>(Arrays.asList(host1, host2));
        String hostVmTag = "haVmTag";

        Mockito.doReturn(hostVmTag).when(virtualMachineProfile).getParameter(Mockito.any(VirtualMachineProfile.Param.class));
        Mockito.doReturn(hostsWithHaTag).when(hostDaoMock).listByHostTag(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(firstFitAllocatorSpy).filterHostsWithUefiEnabled(Mockito.any(Host.Type.class), Mockito.any(VirtualMachineProfile.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList());
        Mockito.doNothing().when(firstFitAllocatorSpy).addHostsBasedOnTagRules(Mockito.anyString(), Mockito.anyList());
        List<HostVO> resultHosts = firstFitAllocatorSpy.retrieveHosts(virtualMachineProfile, type, hostsToFilter, clusterId, podId, dcId, hostTag, templateTag);

        Assert.assertEquals(2, resultHosts.size());
        Assert.assertEquals(host1, resultHosts.get(0));
        Assert.assertEquals(host2, resultHosts.get(1));
    }

    @Test
    public void retrieveHostsTestHostsToFilterIsNullAndNoHaTagAndNoHostTagShouldReturnOnlyAllUpAndEnabledNonHaHosts() {
        List<HostVO> allUpAndEnabledHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> upAndEnabledHostsWithNoHa = new ArrayList<>(Arrays.asList(host1, host2));

        Mockito.doReturn(allUpAndEnabledHosts).when(resourceManagerMock).listAllUpAndEnabledHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(upAndEnabledHostsWithNoHa).when(resourceManagerMock).listAllUpAndEnabledNonHAHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(firstFitAllocatorSpy).filterHostsWithUefiEnabled(Mockito.any(Host.Type.class), Mockito.any(VirtualMachineProfile.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList());
        Mockito.doNothing().when(firstFitAllocatorSpy).addHostsBasedOnTagRules(Mockito.nullable(String.class), Mockito.anyList());
        List<HostVO> resultHosts = firstFitAllocatorSpy.retrieveHosts(virtualMachineProfile, type, emptyList, clusterId, podId, dcId, null, null);

        Assert.assertEquals(2, resultHosts.size());
        Assert.assertEquals(host1, resultHosts.get(0));
        Assert.assertEquals(host2, resultHosts.get(1));
    }

    @Test
    public void retrieveHostsTestHostsToFilterIsNullAndNoHaTagWithHostTagShouldCallRetainHostsMatchingServiceOfferingAndTemplateTags() {
        List<HostVO> allUpAndEnabledHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));

        Mockito.doReturn(allUpAndEnabledHosts).when(resourceManagerMock).listAllUpAndEnabledHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(firstFitAllocatorSpy).retainHostsMatchingServiceOfferingAndTemplateTags(Mockito.anyList(), Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(firstFitAllocatorSpy).filterHostsWithUefiEnabled(Mockito.any(Host.Type.class), Mockito.any(VirtualMachineProfile.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList());
        Mockito.doNothing().when(firstFitAllocatorSpy).addHostsBasedOnTagRules(Mockito.anyString(), Mockito.anyList());
        firstFitAllocatorSpy.retrieveHosts(virtualMachineProfile, type, emptyList, clusterId, podId, dcId, hostTag, templateTag);

        Mockito.verify(firstFitAllocatorSpy, Mockito.times(1)).retainHostsMatchingServiceOfferingAndTemplateTags(Mockito.anyList(), Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void addHostsToAvoidSetTestAllHostsWereConsideredForAllocationShouldNotAddAnyHostToTheAvoidSet() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));

        Mockito.doReturn(suitableHosts).when(hostDaoMock).listAllUpAndEnabledNonHAHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.nullable(String.class));
        firstFitAllocatorSpy.addHostsToAvoidSet(type, excludeList, clusterId, podId, dcId, suitableHosts);

        Assert.assertTrue(excludeList.getHostsToAvoid().isEmpty());
    }

    @Test
    public void addHostsToAvoidSetTestNotAllHostsWereConsideredForAllocationShouldAddHostToTheAvoidSet() {
        List<HostVO> allUpAndEnabledNonHAHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> consideredHosts = new ArrayList<>(Arrays.asList(host2, host3));

        Mockito.doReturn(1L).when(host1).getId();
        Mockito.doCallRealMethod().when(excludeList).addHost(Mockito.anyLong());
        Mockito.doCallRealMethod().when(excludeList).getHostsToAvoid();
        Mockito.doReturn(allUpAndEnabledNonHAHosts).when(hostDaoMock).listAllUpAndEnabledNonHAHosts(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.nullable(String.class));
        firstFitAllocatorSpy.addHostsToAvoidSet(type, excludeList, clusterId, podId, dcId, consideredHosts);

        Assert.assertEquals(1, excludeList.getHostsToAvoid().size());
        Assert.assertTrue(excludeList.getHostsToAvoid().contains(1L));
    }

    @Test
    public void filterHostsWithUefiEnabledTestNoDetailWithUefiShouldNotFilterAnyHost() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        UserVmDetailVO userVmDetailVO = null;

        Mockito.doReturn(userVmDetailVO).when(userVmDetailsDaoMock).findDetail(Mockito.anyLong(), Mockito.anyString());
        firstFitAllocatorSpy.filterHostsWithUefiEnabled(type, virtualMachineProfile, clusterId, podId, dcId, suitableHosts);

        Assert.assertEquals(3, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host2, suitableHosts.get(1));
        Assert.assertEquals(host3, suitableHosts.get(2));
    }

    @Test
    public void filterHostsWithUefiEnabledTestDetailWithUefiWithInvalidModeShouldNotFilterAnyHost() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        UserVmDetailVO userVmDetailVO = Mockito.mock(UserVmDetailVO.class);
        String bootMode = "Invalid mode";

        Mockito.doReturn(bootMode).when(userVmDetailVO).getValue();
        Mockito.doReturn(userVmDetailVO).when(userVmDetailsDaoMock).findDetail(Mockito.anyLong(), Mockito.anyString());
        firstFitAllocatorSpy.filterHostsWithUefiEnabled(type, virtualMachineProfile, clusterId, podId, dcId, suitableHosts);

        Assert.assertEquals(3, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host2, suitableHosts.get(1));
        Assert.assertEquals(host3, suitableHosts.get(2));
    }

    @Test
    public void filterHostsWithUefiEnabledTestDetailWithUefiWithLegacyModeShouldFilterHost() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> uefiHosts = new ArrayList<>(Arrays.asList(host2, host3));
        UserVmDetailVO userVmDetailVO = Mockito.mock(UserVmDetailVO.class);
        String bootMode = ApiConstants.BootMode.LEGACY.toString();

        Mockito.doReturn(bootMode).when(userVmDetailVO).getValue();
        Mockito.doReturn(userVmDetailVO).when(userVmDetailsDaoMock).findDetail(Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(uefiHosts).when(hostDaoMock).listByHostCapability(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        firstFitAllocatorSpy.filterHostsWithUefiEnabled(type, virtualMachineProfile, clusterId, podId, dcId, suitableHosts);

        Assert.assertEquals(2, suitableHosts.size());
        Assert.assertEquals(host2, suitableHosts.get(0));
        Assert.assertEquals(host3, suitableHosts.get(1));
    }

    @Test
    public void filterHostsWithUefiEnabledTestDetailWithUefiWithSecureModeShouldFilterHost() {
        List<HostVO> suitableHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<HostVO> uefiHosts = new ArrayList<>(Arrays.asList(host2, host3));
        UserVmDetailVO userVmDetailVO = Mockito.mock(UserVmDetailVO.class);
        String bootMode = ApiConstants.BootMode.SECURE.toString();

        Mockito.doReturn(bootMode).when(userVmDetailVO).getValue();
        Mockito.doReturn(userVmDetailVO).when(userVmDetailsDaoMock).findDetail(Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(uefiHosts).when(hostDaoMock).listByHostCapability(Mockito.any(Host.Type.class), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        firstFitAllocatorSpy.filterHostsWithUefiEnabled(type, virtualMachineProfile, clusterId, podId, dcId, suitableHosts);

        Assert.assertEquals(2, suitableHosts.size());
        Assert.assertEquals(host2, suitableHosts.get(0));
        Assert.assertEquals(host3, suitableHosts.get(1));
    }

    @Test
    public void offeringRequestedVGpuAndHostDoesNotHaveItTestNoVGpuRequestedShouldReturnFalse() {
        ServiceOfferingDetailsVO requestedVGpuType = null;

        Mockito.doReturn(1L).when(serviceOffering).getId();
        Mockito.doReturn(requestedVGpuType).when(serviceOfferingDetailsDao).findDetail(Mockito.anyLong(), Mockito.anyString());
        boolean result = firstFitAllocatorSpy.offeringRequestedVGpuAndHostDoesNotHaveIt(serviceOffering, excludeList, host1);

        Assert.assertFalse(result);
    }

    @Test
    public void offeringRequestedVGpuAndHostDoesNotHaveItTestVGpuRequestedButHostDoesNotHaveItShouldAddTheHostToTheAvoidListAndReturnTrue() {
        ServiceOfferingDetailsVO requestedVGpuType = Mockito.mock(ServiceOfferingDetailsVO.class);

        Mockito.doReturn(1L).when(host1).getId();
        Mockito.doCallRealMethod().when(excludeList).addHost(Mockito.anyLong());
        Mockito.doCallRealMethod().when(excludeList).getHostsToAvoid();
        Mockito.doReturn(1L).when(serviceOffering).getId();
        Mockito.doReturn(requestedVGpuType).when(serviceOfferingDetailsDao).findDetail(Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(false).when(resourceManagerMock).isGPUDeviceAvailable(Mockito.any(Host.class), Mockito.nullable(String.class), Mockito.nullable(String.class));
        boolean result = firstFitAllocatorSpy.offeringRequestedVGpuAndHostDoesNotHaveIt(serviceOffering, excludeList, host1);

        Assert.assertTrue(result);
        Assert.assertEquals(1, excludeList.getHostsToAvoid().size());
        Assert.assertTrue(excludeList.getHostsToAvoid().contains(1L));
    }

    @Test
    public void offeringRequestedVGpuAndHostDoesNotHaveItTestVGpuRequestedAndHostDoesHaveItShouldReturnFalse() {
        ServiceOfferingDetailsVO requestedVGpuType = Mockito.mock(ServiceOfferingDetailsVO.class);

        Mockito.doReturn(1L).when(serviceOffering).getId();
        Mockito.doReturn(requestedVGpuType).when(serviceOfferingDetailsDao).findDetail(Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(true).when(resourceManagerMock).isGPUDeviceAvailable(Mockito.any(Host.class), Mockito.nullable(String.class), Mockito.nullable(String.class));
        boolean result = firstFitAllocatorSpy.offeringRequestedVGpuAndHostDoesNotHaveIt(serviceOffering, excludeList, host1);

        Assert.assertFalse(result);
        Mockito.verify(excludeList, Mockito.never()).addHost(Mockito.anyLong());
    }

    @Test
    public void filterHostWithNoHvmIfTemplateRequestedTestTemplateDoesNotRequireHvm() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2, host3));

        Mockito.doReturn(false).when(vmTemplateVO).isRequiresHvm();
        List<Host> suitableHosts = firstFitAllocatorSpy.filterHostWithNoHvmIfTemplateRequested(vmTemplateVO, hosts);

        Assert.assertEquals(3, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host2, suitableHosts.get(1));
        Assert.assertEquals(host3, suitableHosts.get(2));
    }

    @Test
    public void filterHostWithNoHvmIfTemplateRequestedTestTemplateRequiresHvmShouldReturnOnlyHvmHosts() {
        List<HostVO> hosts = new ArrayList<>(Arrays.asList(host1, host2, host3));

        Mockito.doReturn(true).when(vmTemplateVO).isRequiresHvm();
        Mockito.doReturn(true).when(firstFitAllocatorSpy).hostSupportsHVM(host1);
        Mockito.doReturn(false).when(firstFitAllocatorSpy).hostSupportsHVM(host2);
        Mockito.doReturn(true).when(firstFitAllocatorSpy).hostSupportsHVM(host3);
        List<Host> suitableHosts = firstFitAllocatorSpy.filterHostWithNoHvmIfTemplateRequested(vmTemplateVO, hosts);

        Assert.assertEquals(2, suitableHosts.size());
        Assert.assertEquals(host1, suitableHosts.get(0));
        Assert.assertEquals(host3, suitableHosts.get(1));
    }

    @Test
    public void prioritizeHostsByGpuEnabledTestServiceOfferingRequestedVGpuShouldDoNothing() {
        List<Host> hosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        ServiceOfferingDetailsVO requestedVGpuType = Mockito.mock(ServiceOfferingDetailsVO.class);

        Mockito.doReturn(requestedVGpuType).when(serviceOfferingDetailsDao).findDetail(Mockito.anyLong(), Mockito.anyString());
        firstFitAllocatorSpy.prioritizeHostsByGpuEnabled(serviceOffering, hosts);

        Assert.assertEquals(3, hosts.size());
        Assert.assertEquals(host1, hosts.get(0));
        Assert.assertEquals(host2, hosts.get(1));
        Assert.assertEquals(host3, hosts.get(2));
    }

    @Test
    public void prioritizeHostsByGpuEnabledTestServiceOfferingDidNotRequestVGpuShouldReorderList() {
        List<Host> allHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        ServiceOfferingDetailsVO requestedVGpuType = null;

        Mockito.doReturn(requestedVGpuType).when(serviceOfferingDetailsDao).findDetail(Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(1L).when(host1).getId();
        Mockito.doReturn(2L).when(host2).getId();
        Mockito.doReturn(3L).when(host3).getId();
        Mockito.doReturn(true).when(resourceManagerMock).isHostGpuEnabled(1L);
        Mockito.doReturn(false).when(resourceManagerMock).isHostGpuEnabled(2L);
        Mockito.doReturn(false).when(resourceManagerMock).isHostGpuEnabled(3L);
        firstFitAllocatorSpy.prioritizeHostsByGpuEnabled(serviceOffering, allHosts);

        Assert.assertEquals(3, allHosts.size());
        Assert.assertEquals(host2, allHosts.get(0));
        Assert.assertEquals(host3, allHosts.get(1));
        Assert.assertEquals(host1, allHosts.get(2));
    }

    @Test
    public void prioritizeHostsByGpuEnabledTestServiceOfferingDidNotRequestVGpuShouldNotReorderListIfThereIsNoHostWithVGpu() {
        List<Host> allHosts = new ArrayList<>(Arrays.asList(host1, host2, host3));
        ServiceOfferingDetailsVO requestedVGpuType = null;

        Mockito.doReturn(requestedVGpuType).when(serviceOfferingDetailsDao).findDetail(Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(1L).when(host1).getId();
        Mockito.doReturn(2L).when(host2).getId();
        Mockito.doReturn(3L).when(host3).getId();
        Mockito.doReturn(false).when(resourceManagerMock).isHostGpuEnabled(Mockito.anyLong());
        firstFitAllocatorSpy.prioritizeHostsByGpuEnabled(serviceOffering, allHosts);

        Assert.assertEquals(3, allHosts.size());
        Assert.assertEquals(host1, allHosts.get(0));
        Assert.assertEquals(host2, allHosts.get(1));
        Assert.assertEquals(host3, allHosts.get(2));
    }

    @Test
    public void prioritizeHostsByHvmCapabilityTestTemplateDidNotRequestedHvmShouldPutHostThatDoesNotSupportHvmInStartOfThePriorityList() {
        List<Host> hostsToCheck = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<Host> prioritizedHosts = new ArrayList<>();

        Mockito.doReturn(false).when(vmTemplateVO).isRequiresHvm();
        Mockito.doReturn(true).when(firstFitAllocatorSpy).hostSupportsHVM(host1);
        Mockito.doReturn(false).when(firstFitAllocatorSpy).hostSupportsHVM(host2);
        Mockito.doReturn(true).when(firstFitAllocatorSpy).hostSupportsHVM(host3);
        firstFitAllocatorSpy.prioritizeHostsByHvmCapability(vmTemplateVO, hostsToCheck, prioritizedHosts);

        Assert.assertEquals(3, prioritizedHosts.size());
        Assert.assertEquals(host2, prioritizedHosts.get(0));
        Assert.assertEquals(host1, prioritizedHosts.get(1));
        Assert.assertEquals(host3, prioritizedHosts.get(2));
    }

    @Test
    public void prioritizeHostsByHvmCapabilityTestTemplateRequiresHvmShouldNotReorderList() {
        List<Host> hostsToCheck = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<Host> prioritizedHosts = new ArrayList<>();

        Mockito.doReturn(true).when(vmTemplateVO).isRequiresHvm();
        firstFitAllocatorSpy.prioritizeHostsByHvmCapability(vmTemplateVO, hostsToCheck, prioritizedHosts);

        Assert.assertEquals(3, prioritizedHosts.size());
        Assert.assertEquals(host1, prioritizedHosts.get(0));
        Assert.assertEquals(host2, prioritizedHosts.get(1));
        Assert.assertEquals(host3, prioritizedHosts.get(2));
    }

    @Test
    public void prioritizeHostsWithMatchingGuestOsTestShouldPutMatchingHostInHighPriorityAndHostsThatDoesNotMatchInLowPriorityList() {
        List<Host> hostsToCheck = new ArrayList<>(Arrays.asList(host1, host2, host3));
        List<Host> highPriorityHosts = new ArrayList<>();
        List<Host> lowPriorityHosts = new ArrayList<>();
        String guestOsCategory1 = "guestOsCategory1";
        String guestOsCategory2 = "guestOsCategory2";

        Mockito.doReturn(guestOsCategory1).when(firstFitAllocatorSpy).getTemplateGuestOSCategory(vmTemplateVO);
        Mockito.doReturn(guestOsCategory1).when(firstFitAllocatorSpy).getHostGuestOSCategory(host1);
        Mockito.doReturn(guestOsCategory2).when(firstFitAllocatorSpy).getHostGuestOSCategory(host2);
        Mockito.doReturn(null).when(firstFitAllocatorSpy).getHostGuestOSCategory(host3);
        firstFitAllocatorSpy.prioritizeHostsWithMatchingGuestOs(vmTemplateVO,hostsToCheck, highPriorityHosts, lowPriorityHosts);

        Assert.assertEquals(1, highPriorityHosts.size());
        Assert.assertEquals(host1, highPriorityHosts.get(0));
        Assert.assertEquals(1, lowPriorityHosts.size());
        Assert.assertEquals(host2, lowPriorityHosts.get(0));
    }
}
