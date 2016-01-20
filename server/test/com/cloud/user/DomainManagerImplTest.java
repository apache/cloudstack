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

package com.cloud.user;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.region.RegionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class DomainManagerImplTest {
    @Mock
    DomainDao _domainDao;
    @Mock
    AccountManager _accountMgr;
    @Mock
    ResourceCountDao _resourceCountDao;
    @Mock
    AccountDao _accountDao;
    @Mock
    DiskOfferingDao _diskOfferingDao;
    @Mock
    ServiceOfferingDao _offeringsDao;
    @Mock
    ProjectDao _projectDao;
    @Mock
    ProjectManager _projectMgr;
    @Mock
    RegionManager _regionMgr;
    @Mock
    ResourceLimitDao _resourceLimitDao;
    @Mock
    DedicatedResourceDao _dedicatedDao;
    @Mock
    NetworkOrchestrationService _networkMgr;
    @Mock
    NetworkDomainDao _networkDomainDao;
    @Mock
    MessageBus _messageBus;
    @Mock
    ConfigurationManager _configMgr;

    DomainManagerImpl domainManager;

    @Before
    public void setup() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        domainManager = new DomainManagerImpl();
        for (Field field : DomainManagerImpl.class.getDeclaredFields()) {
            if (field.getAnnotation(Inject.class) != null) {
                field.setAccessible(true);
                try {
                    Field mockField = this.getClass().getDeclaredField(
                            field.getName());
                    field.set(domainManager, mockField.get(this));
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    public void testFindDomainByIdOrPathNullOrEmpty() {
        final DomainVO domain = new DomainVO("someDomain", 123, 1L, "network.domain");
        Mockito.when(_domainDao.findById(1L)).thenReturn(domain);
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(null, null));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(0L, ""));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(-1L, " "));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(null, "       "));
    }

    @Test
    public void testFindDomainByIdOrPathValidPathAndInvalidId() {
        final DomainVO domain = new DomainVO("someDomain", 123, 1L, "network.domain");
        Mockito.when(_domainDao.findDomainByPath(Mockito.eq("/someDomain/"))).thenReturn(domain);
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(null, "/someDomain/"));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(0L, " /someDomain/"));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(-1L, "/someDomain/ "));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(null, "   /someDomain/   "));
    }

    @Test
    public void testFindDomainByIdOrPathInvalidPathAndInvalidId() {
        Mockito.when(_domainDao.findDomainByPath(Mockito.anyString())).thenReturn(null);
        Assert.assertNull(domainManager.findDomainByIdOrPath(null, "/nonExistingDomain/"));
        Assert.assertNull(domainManager.findDomainByIdOrPath(0L, " /nonExistingDomain/"));
        Assert.assertNull(domainManager.findDomainByIdOrPath(-1L, "/nonExistingDomain/ "));
        Assert.assertNull(domainManager.findDomainByIdOrPath(null, "   /nonExistingDomain/   "));
    }


    @Test
    public void testFindDomainByIdOrPathValidId() {
        final DomainVO domain = new DomainVO("someDomain", 123, 1L, "network.domain");
        Mockito.when(_domainDao.findById(1L)).thenReturn(domain);
        Mockito.when(_domainDao.findDomainByPath(Mockito.eq("/validDomain/"))).thenReturn(new DomainVO());
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(1L, null));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(1L, ""));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(1L, " "));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(1L, "       "));
        Assert.assertEquals(domain, domainManager.findDomainByIdOrPath(1L, "/validDomain/"));
    }

    @Test
    public void deleteDomain() {
        DomainVO domain = new DomainVO();
        domain.setId(20l);
        domain.setAccountId(30l);
        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        Mockito.when(_domainDao.findById(20l)).thenReturn(domain);
        Mockito.doNothing().when(_accountMgr).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.when(_domainDao.update(Mockito.eq(20l), Mockito.any(DomainVO.class))).thenReturn(true);
        Mockito.when(_accountDao.search(Mockito.any(SearchCriteria.class), (Filter)org.mockito.Matchers.isNull())).thenReturn(new ArrayList<AccountVO>());
        Mockito.when(_networkDomainDao.listNetworkIdsByDomain(Mockito.anyLong())).thenReturn(new ArrayList<Long>());
        Mockito.when(_accountDao.findCleanupsForRemovedAccounts(Mockito.anyLong())).thenReturn(new ArrayList<AccountVO>());
        Mockito.when(_dedicatedDao.listByDomainId(Mockito.anyLong())).thenReturn(new ArrayList<DedicatedResourceVO>());
        Mockito.when(_domainDao.remove(Mockito.anyLong())).thenReturn(true);
        Mockito.when(_configMgr.releaseDomainSpecificVirtualRanges(Mockito.anyLong())).thenReturn(true);
        Mockito.when(_diskOfferingDao.listByDomainId(Mockito.anyLong())).thenReturn(new ArrayList<DiskOfferingVO>());
        Mockito.when(_offeringsDao.findServiceOfferingByDomainId(Mockito.anyLong())).thenReturn(new ArrayList<ServiceOfferingVO>());

        try {
            Assert.assertTrue(domainManager.deleteDomain(20l, false));
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void deleteDomainCleanup() {
        DomainVO domain = new DomainVO();
        domain.setId(20l);
        domain.setAccountId(30l);
        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        Mockito.when(_domainDao.findById(20l)).thenReturn(domain);
        Mockito.doNothing().when(_accountMgr).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.when(_domainDao.update(Mockito.eq(20l), Mockito.any(DomainVO.class))).thenReturn(true);
        Mockito.when(_domainDao.createSearchCriteria()).thenReturn(Mockito.mock(SearchCriteria.class));
        Mockito.when(_domainDao.search(Mockito.any(SearchCriteria.class), (Filter)org.mockito.Matchers.isNull())).thenReturn(new ArrayList<DomainVO>());
        Mockito.when(_accountDao.createSearchCriteria()).thenReturn(Mockito.mock(SearchCriteria.class));
        Mockito.when(_accountDao.search(Mockito.any(SearchCriteria.class), (Filter)org.mockito.Matchers.isNull())).thenReturn(new ArrayList<AccountVO>());
        Mockito.when(_networkDomainDao.listNetworkIdsByDomain(Mockito.anyLong())).thenReturn(new ArrayList<Long>());
        Mockito.when(_accountDao.findCleanupsForRemovedAccounts(Mockito.anyLong())).thenReturn(new ArrayList<AccountVO>());
        Mockito.when(_dedicatedDao.listByDomainId(Mockito.anyLong())).thenReturn(new ArrayList<DedicatedResourceVO>());
        Mockito.when(_domainDao.remove(Mockito.anyLong())).thenReturn(true);
        Mockito.when(_resourceCountDao.removeEntriesByOwner(Mockito.anyLong(), Mockito.eq(ResourceOwnerType.Domain))).thenReturn(1l);
        Mockito.when(_resourceLimitDao.removeEntriesByOwner(Mockito.anyLong(), Mockito.eq(ResourceOwnerType.Domain))).thenReturn(1l);
        Mockito.when(_configMgr.releaseDomainSpecificVirtualRanges(Mockito.anyLong())).thenReturn(true);
        Mockito.when(_diskOfferingDao.listByDomainId(Mockito.anyLong())).thenReturn(new ArrayList<DiskOfferingVO>());
        Mockito.when(_offeringsDao.findServiceOfferingByDomainId(Mockito.anyLong())).thenReturn(new ArrayList<ServiceOfferingVO>());

        try {
            Assert.assertTrue(domainManager.deleteDomain(20l, true));
        } finally {
            CallContext.unregister();
        }
    }
}
