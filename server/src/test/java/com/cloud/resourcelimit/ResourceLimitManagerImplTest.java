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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.configuration.Resource;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vpc.MockResourceLimitManagerImpl;

@RunWith(MockitoJUnitRunner.class)
public class ResourceLimitManagerImplTest {
    private static final Logger s_logger = Logger.getLogger(ResourceLimitManagerImplTest.class);

    MockResourceLimitManagerImpl _resourceLimitService = new MockResourceLimitManagerImpl();

    @Spy
    @InjectMocks
    ResourceLimitManagerImpl resourceLimitManager;

    @Mock
    VMInstanceDao vmDao;
    @Mock
    AccountDao accountDao;
    @Mock
    AccountManager accountManager;
    @Mock
    ResourceLimitDao resourceLimitDao;
    @Mock
    DomainDao domainDao;
    @Mock
    ProjectDao projectDao;
    @Mock
    ResourceCountDao resourceCountDao;
    @Mock
    UserVmJoinDao userVmJoinDao;
    @Mock
    ServiceOfferingDao serviceOfferingDao;
    @Mock
    VMTemplateDao vmTemplateDao;

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

    @Test
    public void testGetResourceLimitTagsForLimitSearch() {
        Pair<List<String>, List<String>> result = resourceLimitManager.getResourceLimitTagsForLimitSearch(null);
        Assert.assertEquals(hostTags, result.first());
        Assert.assertEquals(storageTags, result.second());
        String nonExistentTag = "sometag";
        result = resourceLimitManager.getResourceLimitTagsForLimitSearch(nonExistentTag);
        Assert.assertTrue(CollectionUtils.isEmpty(result.first()));
        Assert.assertTrue(CollectionUtils.isEmpty(result.second()));
        String hostTag = "htag2";
        result = resourceLimitManager.getResourceLimitTagsForLimitSearch(hostTag);
        Assert.assertTrue(CollectionUtils.isNotEmpty(result.first()));
        Assert.assertEquals(1, result.first().size());
        Assert.assertEquals(hostTag, result.first().get(0));
        Assert.assertTrue(CollectionUtils.isEmpty(result.second()));
        String storageTag = "stag1";
        result = resourceLimitManager.getResourceLimitTagsForLimitSearch(storageTag);
        Assert.assertTrue(CollectionUtils.isNotEmpty(result.second()));
        Assert.assertEquals(1, result.second().size());
        Assert.assertEquals(storageTag, result.second().get(0));
        Assert.assertTrue(CollectionUtils.isEmpty(result.first()));
    }

    @Test
    public void testIsTaggedResourceCountRecalculationNotNeeded() {
        Assert.assertTrue(resourceLimitManager.isTaggedResourceCountRecalculationNotNeeded(
                Resource.ResourceType.network, List.of("h1", "h2"), List.of("s1", "s2")));
        Assert.assertTrue(resourceLimitManager.isTaggedResourceCountRecalculationNotNeeded(
                Resource.ResourceType.cpu, new ArrayList<>(), new ArrayList<>()));
        Assert.assertFalse(resourceLimitManager.isTaggedResourceCountRecalculationNotNeeded(
                Resource.ResourceType.cpu, List.of("h1", "h2"), new ArrayList<>()));
    }

    @Test
    public void testAddTaggedResourceLimits() {
        List<ResourceLimitVO> limits = new ArrayList<>();
        resourceLimitManager.addTaggedResourceLimits(limits, null, hostTags, Resource.ResourceOwnerType.Account, 1L);
        Assert.assertTrue(CollectionUtils.isEmpty(limits));
        resourceLimitManager.addTaggedResourceLimits(limits, List.of(Resource.ResourceType.cpu), null, Resource.ResourceOwnerType.Account, 1L);
        Assert.assertTrue(CollectionUtils.isEmpty(limits));
        limits = new ArrayList<>();
        limits.add(Mockito.mock(ResourceLimitVO.class));
        int size = limits.size();
        AccountVO account = Mockito.mock(AccountVO.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountDao.findById(1L)).thenReturn(account);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(true);
        resourceLimitManager.addTaggedResourceLimits(limits, List.of(Resource.ResourceType.cpu), hostTags, Resource.ResourceOwnerType.Account, 1L);
        Assert.assertEquals(size + hostTags.size(), limits.size());
    }

    @Test
    public void testFindCorrectResourceLimitForAccount() {
        AccountVO account = Mockito.mock(AccountVO.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(true);
        long result = resourceLimitManager.findCorrectResourceLimitForAccount(account, Resource.ResourceType.cpu, hostTags.get(0));
        Assert.assertEquals(Resource.RESOURCE_UNLIMITED, result);

        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(false);
        ResourceLimitVO limit = new ResourceLimitVO();
        limit.setMax(10L);
        Mockito.when(resourceLimitDao.findByOwnerIdAndTypeAndTag(1L, Resource.ResourceOwnerType.Account, Resource.ResourceType.cpu, hostTags.get(0))).thenReturn(limit);
        result = resourceLimitManager.findCorrectResourceLimitForAccount(account, Resource.ResourceType.cpu, hostTags.get(0));
        Assert.assertEquals(10L, result);

        long defaultAccountCpuMax = 25L;
        Map<String, Long> accountResourceLimitMap = new HashMap<>();
        accountResourceLimitMap.put(Resource.ResourceType.cpu.name(), defaultAccountCpuMax);
        resourceLimitManager.accountResourceLimitMap = accountResourceLimitMap;
        Mockito.when(resourceLimitDao.findByOwnerIdAndTypeAndTag(1L, Resource.ResourceOwnerType.Account, Resource.ResourceType.cpu, hostTags.get(0))).thenReturn(null);
        result = resourceLimitManager.findCorrectResourceLimitForAccount(account, Resource.ResourceType.cpu, hostTags.get(0));
        Assert.assertEquals(defaultAccountCpuMax, result);
    }

    @Test
    public void testFindCorrectResourceLimitForAccountId1() {
//        long accountId = 1L;
//        Mockito.when(accountManager.isRootAdmin(accountId)).thenReturn(true);
//        long result = resourceLimitManager.findCorrectResourceLimitForAccount(accountId, null, Resource.ResourceType.cpu);
//        Assert.assertEquals(Resource.RESOURCE_UNLIMITED, result);
//
//        accountId = 2L;
//        Mockito.when(accountManager.isRootAdmin(accountId)).thenReturn(false);
//        Long limit = 100L;
//        long result = resourceLimitManager.findCorrectResourceLimitForAccount(accountId, limit, Resource.ResourceType.cpu);
//        Assert.assertEquals(limit.longValue(), result);
//
//        long defaultAccountCpuMax = 25L;
//        Mockito.when(accountManager.isRootAdmin(accountId)).thenReturn(false);
//        Map<String, Long> accountResourceLimitMap = new HashMap<>();
//        accountResourceLimitMap.put(Resource.ResourceType.cpu.name(), defaultAccountCpuMax);
//        resourceLimitManager.accountResourceLimitMap = accountResourceLimitMap;
//        result = resourceLimitManager.findCorrectResourceLimitForAccount(accountId, null, Resource.ResourceType.cpu);
//        Assert.assertEquals(defaultAccountCpuMax, result);
    }

    @Test
    public void testFindCorrectResourceLimitForDomain() {
        DomainVO domain = Mockito.mock(DomainVO.class);
        Mockito.when(domain.getId()).thenReturn(1L);
        long result = resourceLimitManager.findCorrectResourceLimitForDomain(domain, Resource.ResourceType.cpu, hostTags.get(0));
        Assert.assertEquals(Resource.RESOURCE_UNLIMITED, result);

        Mockito.when(domain.getId()).thenReturn(2L);
        Mockito.when(domain.getParent()).thenReturn(null);
        ResourceLimitVO limit = new ResourceLimitVO();
        limit.setMax(100L);
        Mockito.when(resourceLimitDao.findByOwnerIdAndTypeAndTag(2L, Resource.ResourceOwnerType.Domain, Resource.ResourceType.cpu, hostTags.get(0))).thenReturn(limit);
        result = resourceLimitManager.findCorrectResourceLimitForDomain(domain, Resource.ResourceType.cpu, hostTags.get(0));
        Assert.assertEquals(100L, result);

        Mockito.when(domain.getId()).thenReturn(3L);
        DomainVO parentDomain = Mockito.mock(DomainVO.class);
        Mockito.when(domain.getParent()).thenReturn(5L);
        Mockito.when(domainDao.findById(5L)).thenReturn(parentDomain);
        limit = new ResourceLimitVO();
        limit.setMax(200L);
        Mockito.when(resourceLimitDao.findByOwnerIdAndTypeAndTag(3L, Resource.ResourceOwnerType.Domain, Resource.ResourceType.cpu, hostTags.get(0))).thenReturn(null);
        Mockito.when(resourceLimitDao.findByOwnerIdAndTypeAndTag(5L, Resource.ResourceOwnerType.Domain, Resource.ResourceType.cpu, hostTags.get(0))).thenReturn(limit);
        result = resourceLimitManager.findCorrectResourceLimitForDomain(domain, Resource.ResourceType.cpu, hostTags.get(0));
        Assert.assertEquals(200L, result);

        long defaultDomainCpuMax = 250L;
        Mockito.when(domain.getId()).thenReturn(4L);
        Mockito.when(domain.getParent()).thenReturn(null);
        Map<String, Long> domainResourceLimitMap = new HashMap<>();
        domainResourceLimitMap.put(Resource.ResourceType.cpu.name(), defaultDomainCpuMax);
        resourceLimitManager.domainResourceLimitMap = domainResourceLimitMap;
        Mockito.when(resourceLimitDao.findByOwnerIdAndTypeAndTag(4L, Resource.ResourceOwnerType.Domain, Resource.ResourceType.cpu, hostTags.get(0))).thenReturn(null);
        result = resourceLimitManager.findCorrectResourceLimitForDomain(domain, Resource.ResourceType.cpu, hostTags.get(0));
        Assert.assertEquals(defaultDomainCpuMax, result);
    }

    @Test
    public void testCheckResourceLimitWithTag() {
        AccountVO account = Mockito.mock(AccountVO.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(true);
        try {
            resourceLimitManager.checkResourceLimitWithTag(account, Resource.ResourceType.cpu, hostTags.get(0), 1);
        } catch (ResourceAllocationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCheckResourceLimitWithTagNonAdmin() throws ResourceAllocationException {
        AccountVO account = Mockito.mock(AccountVO.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(false);
        Mockito.doReturn(new ArrayList<ResourceLimitVO>()).when(resourceLimitManager).lockAccountAndOwnerDomainRows(Mockito.anyLong(), Mockito.any(Resource.ResourceType.class), Mockito.anyString());
        Mockito.doNothing().when(resourceLimitManager).checkAccountResourceLimit(account, null, Resource.ResourceType.cpu, hostTags.get(0), 1);
        Mockito.doNothing().when(resourceLimitManager).checkDomainResourceLimit(account, null, Resource.ResourceType.cpu, hostTags.get(0), 1);
        try {
            resourceLimitManager.checkResourceLimitWithTag(account, Resource.ResourceType.cpu, hostTags.get(0), 1);
        } catch (ResourceAllocationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCheckResourceLimitWithTagProject() throws ResourceAllocationException {
        AccountVO account = Mockito.mock(AccountVO.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(account.getType()).thenReturn(Account.Type.PROJECT);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(false);
        ProjectVO projectVO = Mockito.mock(ProjectVO.class);
        Mockito.when(projectDao.findByProjectAccountId(Mockito.anyLong())).thenReturn(projectVO);
        Mockito.doReturn(new ArrayList<ResourceLimitVO>()).when(resourceLimitManager).lockAccountAndOwnerDomainRows(Mockito.anyLong(), Mockito.any(Resource.ResourceType.class), Mockito.anyString());
        Mockito.doNothing().when(resourceLimitManager).checkAccountResourceLimit(account, projectVO, Resource.ResourceType.cpu, hostTags.get(0), 1);
        Mockito.doNothing().when(resourceLimitManager).checkDomainResourceLimit(account, projectVO, Resource.ResourceType.cpu, hostTags.get(0), 1);
        try {
            resourceLimitManager.checkResourceLimitWithTag(account, Resource.ResourceType.cpu, hostTags.get(0), 1);
        } catch (ResourceAllocationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRemoveResourceLimitAndCountForNonMatchingTags() {
        resourceLimitManager.removeResourceLimitAndCountForNonMatchingTags(1L, Resource.ResourceOwnerType.Account, hostTags, storageTags);
        Mockito.verify(resourceLimitDao, Mockito.times(1))
                .removeResourceLimitsForNonMatchingTags(1L, Resource.ResourceOwnerType.Account, ResourceLimitService.HostTagsSupportingTypes, hostTags);
        Mockito.verify(resourceLimitDao, Mockito.times(1))
                .removeResourceLimitsForNonMatchingTags(1L, Resource.ResourceOwnerType.Account, ResourceLimitService.StorageTagsSupportingTypes, storageTags);
        Mockito.verify(resourceCountDao, Mockito.times(1))
                .removeResourceCountsForNonMatchingTags(1L, Resource.ResourceOwnerType.Account, ResourceLimitService.HostTagsSupportingTypes, hostTags);
        Mockito.verify(resourceCountDao, Mockito.times(1))
                .removeResourceCountsForNonMatchingTags(1L, Resource.ResourceOwnerType.Account, ResourceLimitService.StorageTagsSupportingTypes, storageTags);
    }

    @Test
    public void testRecalculateAccountTaggedResourceCountNegative() {
        List<ResourceCountVO> result = resourceLimitManager.recalculateAccountTaggedResourceCount(1L, Resource.ResourceType.network, hostTags, storageTags);
        CollectionUtils.isEmpty(result);
        result = resourceLimitManager.recalculateAccountTaggedResourceCount(1L, Resource.ResourceType.cpu, null, storageTags);
        CollectionUtils.isEmpty(result);
        result = resourceLimitManager.recalculateAccountTaggedResourceCount(1L, Resource.ResourceType.volume, hostTags, null);
        CollectionUtils.isEmpty(result);
    }

    @Test
    public void testRecalculateAccountTaggedResourceCountHostTypes() {
        long accountId = 1L;
        Resource.ResourceType type = Resource.ResourceType.cpu;
        for (String tag: hostTags) {
            Mockito.doReturn(10L).when(resourceLimitManager).recalculateAccountResourceCount(accountId, type, tag);
        }
        List<ResourceCountVO> result = resourceLimitManager.recalculateAccountTaggedResourceCount(accountId, type, hostTags, storageTags);
        Assert.assertEquals(hostTags.size(), result.size());
    }

    @Test
    public void testRecalculateAccountTaggedResourceCountStorageTypes() {
        long accountId = 1L;
        Resource.ResourceType type = Resource.ResourceType.volume;
        for (String tag: storageTags) {
            Mockito.doReturn(10L).when(resourceLimitManager).recalculateAccountResourceCount(accountId, type, tag);
        }
        List<ResourceCountVO> result = resourceLimitManager.recalculateAccountTaggedResourceCount(accountId, type, hostTags, storageTags);
        Assert.assertEquals(storageTags.size(), result.size());
    }

    @Test
    public void testRecalculateDomainTaggedResourceCountNegative() {
        List<ResourceCountVO> result = resourceLimitManager.recalculateDomainTaggedResourceCount(1L, Resource.ResourceType.network, hostTags, storageTags);
        CollectionUtils.isEmpty(result);
        result = resourceLimitManager.recalculateDomainTaggedResourceCount(1L, Resource.ResourceType.cpu, null, storageTags);
        CollectionUtils.isEmpty(result);
        result = resourceLimitManager.recalculateDomainTaggedResourceCount(1L, Resource.ResourceType.volume, hostTags, null);
        CollectionUtils.isEmpty(result);
    }

    @Test
    public void testRecalculateDomainTaggedResourceCountHostTypes() {
        long domainId = 1L;
        Resource.ResourceType type = Resource.ResourceType.cpu;
        for (String tag: hostTags) {
            Mockito.doReturn(10L).when(resourceLimitManager).recalculateDomainResourceCount(domainId, type, tag);
        }
        List<ResourceCountVO> result = resourceLimitManager.recalculateDomainTaggedResourceCount(domainId, type, hostTags, storageTags);
        Assert.assertEquals(hostTags.size(), result.size());
    }

    @Test
    public void testRecalculateDomainTaggedResourceCountStorageTypes() {
        long domainId = 1L;
        Resource.ResourceType type = Resource.ResourceType.volume;
        for (String tag: storageTags) {
            Mockito.doReturn(10L).when(resourceLimitManager).recalculateDomainResourceCount(domainId, type, tag);
        }
        List<ResourceCountVO> result = resourceLimitManager.recalculateDomainTaggedResourceCount(domainId, type, hostTags, storageTags);
        Assert.assertEquals(storageTags.size(), result.size());
    }

    @Test
    public void testRecalculateResourceCount() {
        Long accountId = 1L;
        Long domainId = null;
        Integer typeId = Resource.ResourceType.user_vm.getOrdinal();
        Mockito.doReturn(new ArrayList<>()).when(resourceLimitManager).recalculateResourceCount(accountId, domainId, typeId, null);
        resourceLimitManager.recalculateResourceCount(accountId, domainId, typeId);
        Mockito.verify(resourceLimitManager, Mockito.times(1)).recalculateResourceCount(accountId, domainId, typeId, null);
    }

    @Test
    public void testGetVmsWithAccountAndTagNoTag() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(VirtualMachineManager.ResourceCountRunningVMsonly, "_defaultValue", "false");
        List<VirtualMachine.State> states = Arrays.asList(VirtualMachine.State.Destroyed, VirtualMachine.State.Error, VirtualMachine.State.Expunging);
        List<UserVmJoinVO> vmList = List.of(Mockito.mock(UserVmJoinVO.class));
        Mockito.when(userVmJoinDao.listByAccountServiceOfferingTemplateAndNotInState(1L, states, null, null)).thenReturn(vmList);
        List<UserVmJoinVO> result = resourceLimitManager.getVmsWithAccountAndTag(1L, null);
        Assert.assertEquals(vmList.size(), result.size());
    }

    @Test
    public void testGetVmsWithAccountAndTagNegative() {
        String tag = hostTags.get(0);
        Mockito.when(serviceOfferingDao.listByHostTag(tag)).thenReturn(null);
        Mockito.when(vmTemplateDao.listByTemplateTag(tag)).thenReturn(null);
        List<UserVmJoinVO> result = resourceLimitManager.getVmsWithAccountAndTag(1L, hostTags.get(0));
        Assert.assertTrue(CollectionUtils.isEmpty(result));
    }

    @Test
    public void testGetVmsWithAccountAndTag() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(VirtualMachineManager.ResourceCountRunningVMsonly, "_defaultValue", "true");
        String tag = hostTags.get(0);
        ServiceOfferingVO serviceOfferingVO = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingVO.getId()).thenReturn(1L);
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateVO.getId()).thenReturn(1L);
        Mockito.when(serviceOfferingDao.listByHostTag(tag)).thenReturn(List.of(serviceOfferingVO));
        Mockito.when(vmTemplateDao.listByTemplateTag(tag)).thenReturn(List.of(templateVO));
        List<UserVmJoinVO> vmList = List.of(Mockito.mock(UserVmJoinVO.class));
        Mockito.when(userVmJoinDao.listByAccountServiceOfferingTemplateAndNotInState(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList(), Mockito.anyList())).thenReturn(vmList);
        List<UserVmJoinVO> result = resourceLimitManager.getVmsWithAccountAndTag(1L, tag);
        Assert.assertEquals(vmList.size(), result.size());
    }

    @Test
    public void testGetVmsWithAccount() {
        Long accountId = 1L;
        Mockito.doReturn(new ArrayList<>()).when(resourceLimitManager).getVmsWithAccountAndTag(accountId, null);
        resourceLimitManager.getVmsWithAccount(accountId);
        Mockito.verify(resourceLimitManager, Mockito.times(1)).getVmsWithAccountAndTag(accountId, null);
    }
}
