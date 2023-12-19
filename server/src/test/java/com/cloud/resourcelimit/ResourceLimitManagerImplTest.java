// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.resourcelimit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.configuration.Resource;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.vpc.MockResourceLimitManagerImpl;

@RunWith(MockitoJUnitRunner.class)
public class ResourceLimitManagerImplTest {
    private static final Logger s_logger = Logger.getLogger(ResourceLimitManagerImplTest.class);

    MockResourceLimitManagerImpl _resourceLimitService = new MockResourceLimitManagerImpl();

    @Spy
    ResourceLimitManagerImpl resourceLimitManager = new ResourceLimitManagerImpl();

    private List<String> hostTags = List.of("htag1", "htag2", "htag3");
    private List<String> storageTags = List.of("stag1", "stag2");

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    @Before
    public void setUp() {
        try {
            overrideDefaultConfigValue(ResourceLimitService.ResourceLimitHostTags, "_defaultValue", StringUtils.join(hostTags, ","));
            overrideDefaultConfigValue(ResourceLimitService.ResourceLimitStorageTags, "_defaultValue", StringUtils.join(storageTags, ","));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            s_logger.error("Failed to update configurations");
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInjected() throws Exception {
        s_logger.info("Starting test for Resource Limit manager");
        updateResourceCount();
        updateResourceLimit();
        //listResourceLimits();
        s_logger.info("Resource Limit Manager: TEST PASSED");
    }

    protected void updateResourceCount() {
        // update resource count for an account
        Long accountId = (long)1;
        Long domainId = (long)1;
        String msg = "Update Resource Count for account: TEST FAILED";
        Assert.assertNull(msg, _resourceLimitService.recalculateResourceCount(accountId, domainId, null));

        // update resource count for a domain
        accountId = null;
        msg = "Update Resource Count for domain: TEST FAILED";
        Assert.assertNull(msg, _resourceLimitService.recalculateResourceCount(accountId, domainId, null));
    }

    protected void updateResourceLimit() {
        // update resource Limit for an account for resource_type = 8 (CPU)
        resourceLimitServiceCall((long)1, (long)1, 8, (long)20);

        // update resource Limit for a domain for resource_type = 8 (CPU)
        resourceLimitServiceCall(null, (long)1, 8, (long)40);

        // update resource Limit for an account for resource_type = 9 (Memory (in MiB))
        resourceLimitServiceCall((long)1, (long)1, 9, (long)4096);

        // update resource Limit for a domain for resource_type = 9 (Memory (in MiB))
        resourceLimitServiceCall(null, (long)1, 9, (long)10240);

        // update resource Limit for an account for resource_type = 10 (Primary storage (in GiB))
        resourceLimitServiceCall((long)1, (long)1, 10, (long)200);

        // update resource Limit for a domain for resource_type = 10 (Primary storage (in GiB))
        resourceLimitServiceCall(null, (long)1, 10, (long)200);

        // update resource Limit for an account for resource_type = 11 (Secondary storage (in GiB))
        resourceLimitServiceCall((long)1, (long)1, 10, (long)400);

        // update resource Limit for a domain for resource_type = 11 (Secondary storage (in GiB))
        resourceLimitServiceCall(null, (long)1, 10, (long)400);
    }

    private void resourceLimitServiceCall(Long accountId, Long domainId, Integer resourceType, Long max) {
        String msg = "Update Resource Limit: TEST FAILED";
        ResourceLimit result = null;
        try {
            result = _resourceLimitService.updateResourceLimit(accountId, domainId, resourceType, max, null);
            Assert.assertFalse(msg, (result != null || (result == null && max != null && max.longValue() == -1L)));
        } catch (Exception ex) {
            Assert.fail(msg);
        }
    }

    @Test
    public void testRemoveUndesiredTaggedLimits() {
        String desiredTag = "tag1";
        String undesiredTag = "tag2";
        List<ResourceLimitVO> limits = new ArrayList<>();
        limits.add(new ResourceLimitVO(Resource.ResourceType.cpu, 100L, 1L, Resource.ResourceOwnerType.Account, desiredTag));
        limits.add(new ResourceLimitVO(Resource.ResourceType.cpu, 100L, 1L, Resource.ResourceOwnerType.Account, undesiredTag));
        resourceLimitManager.removeUndesiredTaggedLimits(limits, List.of(desiredTag), null);
        Assert.assertEquals(1, limits.size());
        Assert.assertEquals(desiredTag, limits.get(0).getTag());
    }

    @Test
    public void testGetResourceLimitHostTags() {
        List<String> tags = resourceLimitManager.getResourceLimitHostTags();
        Assert.assertEquals(3, tags.size());
        for (int i = 0; i < tags.size(); ++i) {
            Assert.assertEquals(hostTags.get(i), tags.get(i));
        }
    }

    @Test
    public void testGetResourceLimitHostTags1() {
        ServiceOffering serviceOffering = Mockito.mock(ServiceOffering.class);
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(serviceOffering.getHostTag()).thenReturn(hostTags.get(0));
        Mockito.when(template.getTemplateTag()).thenReturn(hostTags.get(1));
        List<String> tags = resourceLimitManager.getResourceLimitHostTags(serviceOffering, template);
        Assert.assertEquals(2, tags.size());
        Assert.assertEquals(hostTags.get(0), tags.get(0));
        Assert.assertEquals(hostTags.get(1), tags.get(1));
    }

    @Test
    public void testGetResourceLimitStorageTags() {
        List<String> tags = resourceLimitManager.getResourceLimitStorageTags();
        Assert.assertEquals(2, tags.size());
        for (int i = 0; i < tags.size(); ++i) {
            Assert.assertEquals(storageTags.get(i), tags.get(i));
        }
    }

    @Test
    public void testGetResourceLimitStorageTags1() {
        DiskOffering diskOffering = Mockito.mock(DiskOffering.class);
        Mockito.when(diskOffering.getTags()).thenReturn(storageTags.get(1));
        Mockito.when(diskOffering.getTagsArray()).thenReturn(new String[]{storageTags.get(1)});
        List<String> tags = resourceLimitManager.getResourceLimitStorageTags(diskOffering);
        Assert.assertEquals(1, tags.size());
        Assert.assertEquals(storageTags.get(1), tags.get(0));
    }

    @Test
    public void testCheckVmResourceLimit() {
        ServiceOffering serviceOffering = Mockito.mock(ServiceOffering.class);
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(serviceOffering.getHostTag()).thenReturn(hostTags.get(0));
        Mockito.when(serviceOffering.getCpu()).thenReturn(2);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(256);
        Mockito.when(template.getTemplateTag()).thenReturn(hostTags.get(0));
        Account account = Mockito.mock(Account.class);
        try {
            Mockito.doNothing().when(resourceLimitManager).checkResourceLimitWithTag(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
            resourceLimitManager.checkVmResourceLimit(account, true, serviceOffering, template);
            List<String> tags = new ArrayList<>();
            tags.add(null);
            tags.add(hostTags.get(0));
            for (String tag: tags) {
                Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.user_vm, tag);
                Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.cpu, tag, 2L);
                Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.memory, tag, 256L);
            }
        } catch (ResourceAllocationException e) {
            Assert.fail("Exception encountered: " + e.getMessage());
        }
    }

    @Test
    public void testCheckVmCpuResourceLimit() {
        ServiceOffering serviceOffering = Mockito.mock(ServiceOffering.class);
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(serviceOffering.getHostTag()).thenReturn(hostTags.get(0));
        Mockito.when(template.getTemplateTag()).thenReturn(hostTags.get(0));
        Account account = Mockito.mock(Account.class);
        long cpu = 2L;
        try {
            Mockito.doNothing().when(resourceLimitManager).checkResourceLimitWithTag(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
            resourceLimitManager.checkVmCpuResourceLimit(account, true, serviceOffering, template, cpu);
            Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.cpu, null, cpu);
            Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.cpu, hostTags.get(0), cpu);
        } catch (ResourceAllocationException e) {
            Assert.fail("Exception encountered: " + e.getMessage());
        }
    }

    @Test
    public void testCheckVmMemoryResourceLimit() {
        ServiceOffering serviceOffering = Mockito.mock(ServiceOffering.class);
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(serviceOffering.getHostTag()).thenReturn(hostTags.get(0));
        Mockito.when(template.getTemplateTag()).thenReturn(hostTags.get(0));
        Account account = Mockito.mock(Account.class);
        long delta = 256L;
        try {
            Mockito.doNothing().when(resourceLimitManager).checkResourceLimitWithTag(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
            resourceLimitManager.checkVmMemoryResourceLimit(account, true, serviceOffering, template, delta);
            Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.memory, null, delta);
            Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.memory, hostTags.get(0), delta);
        } catch (ResourceAllocationException e) {
            Assert.fail("Exception encountered: " + e.getMessage());
        }
    }

    @Test
    public void testCheckVolumeResourceLimit() {
        String checkTag = storageTags.get(0);
        DiskOffering diskOffering = Mockito.mock(DiskOffering.class);
        Mockito.when(diskOffering.getTags()).thenReturn(checkTag);
        Mockito.when(diskOffering.getTagsArray()).thenReturn(new String[]{checkTag});
        Account account = Mockito.mock(Account.class);
        try {
            Mockito.doNothing().when(resourceLimitManager).checkResourceLimitWithTag(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
            resourceLimitManager.checkVolumeResourceLimit(account, true, 100L, diskOffering);
            List<String> tags = new ArrayList<>();
            tags.add(null);
            tags.add(checkTag);
            for (String tag: tags) {
                Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.volume, tag);
                Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimitWithTag(account, Resource.ResourceType.primary_storage, tag, 100L);
            }
        } catch (ResourceAllocationException e) {
            Assert.fail("Exception encountered: " + e.getMessage());
        }
    }
}
