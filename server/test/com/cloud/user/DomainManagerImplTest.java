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

import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.region.RegionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

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

    @Spy
    @InjectMocks
    DomainManagerImpl domainManager = new DomainManagerImpl();

    @Mock
    DomainVO domain;
    @Mock
    Account adminAccount;
    @Mock
    GlobalLock lock;

    List<AccountVO> domainAccountsForCleanup;
    List<Long> domainNetworkIds;
    List<DedicatedResourceVO> domainDedicatedResources;

    private static final long DOMAIN_ID = 3l;
    private static final long ACCOUNT_ID = 1l;

    private static boolean testDomainCleanup = false;

    @Before
    public void setup() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Mockito.doReturn(adminAccount).when(domainManager).getCaller();
        Mockito.doReturn(lock).when(domainManager).getGlobalLock("AccountCleanup");
        Mockito.when(lock.lock(Mockito.anyInt())).thenReturn(true);
        Mockito.when(_domainDao.findById(DOMAIN_ID)).thenReturn(domain);
        Mockito.when(domain.getAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(domain.getId()).thenReturn(DOMAIN_ID);
        Mockito.when(_domainDao.remove(DOMAIN_ID)).thenReturn(true);
        domainAccountsForCleanup = new ArrayList<AccountVO>();
        domainNetworkIds = new ArrayList<Long>();
        domainDedicatedResources = new ArrayList<DedicatedResourceVO>();
        Mockito.when(_accountDao.findCleanupsForRemovedAccounts(DOMAIN_ID)).thenReturn(domainAccountsForCleanup);
        Mockito.when(_networkDomainDao.listNetworkIdsByDomain(DOMAIN_ID)).thenReturn(domainNetworkIds);
        Mockito.when(_dedicatedDao.listByDomainId(DOMAIN_ID)).thenReturn(domainDedicatedResources);
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

    @Test(expected=InvalidParameterValueException.class)
    public void testDeleteDomainNullDomain() {
        Mockito.when(_domainDao.findById(DOMAIN_ID)).thenReturn(null);
        domainManager.deleteDomain(DOMAIN_ID, testDomainCleanup);
    }

    @Test(expected=PermissionDeniedException.class)
    public void testDeleteDomainRootDomain() {
        Mockito.when(_domainDao.findById(Domain.ROOT_DOMAIN)).thenReturn(domain);
        domainManager.deleteDomain(Domain.ROOT_DOMAIN, testDomainCleanup);
    }

    @Test
    public void testDeleteDomainNoCleanup() {
        domainManager.deleteDomain(DOMAIN_ID, testDomainCleanup);
        Mockito.verify(domainManager).deleteDomain(domain, testDomainCleanup);
        Mockito.verify(domainManager).removeDomainWithNoAccountsForCleanupNetworksOrDedicatedResources(domain);
        Mockito.verify(domainManager).cleanupDomainOfferings(DOMAIN_ID);
        Mockito.verify(lock).unlock();
    }

    @Test
    public void testRemoveDomainWithNoAccountsForCleanupNetworksOrDedicatedResourcesRemoveDomain() {
        domainManager.removeDomainWithNoAccountsForCleanupNetworksOrDedicatedResources(domain);
        Mockito.verify(domainManager).publishRemoveEventsAndRemoveDomain(domain);
    }

    @Test(expected=CloudRuntimeException.class)
    public void testRemoveDomainWithNoAccountsForCleanupNetworksOrDedicatedResourcesDontRemoveDomain() {
        domainNetworkIds.add(2l);
        domainManager.removeDomainWithNoAccountsForCleanupNetworksOrDedicatedResources(domain);
        Mockito.verify(domainManager).failRemoveOperation(domain, domainAccountsForCleanup, domainNetworkIds, false);
    }

    @Test
    public void testPublishRemoveEventsAndRemoveDomainSuccessfulDelete() {
        domainManager.publishRemoveEventsAndRemoveDomain(domain);
        Mockito.verify(_messageBus).publish(Mockito.anyString(), Matchers.eq(DomainManager.MESSAGE_PRE_REMOVE_DOMAIN_EVENT),
                Matchers.eq(PublishScope.LOCAL), Matchers.eq(domain));
        Mockito.verify(_messageBus).publish(Mockito.anyString(), Matchers.eq(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT),
                Matchers.eq(PublishScope.LOCAL), Matchers.eq(domain));
        Mockito.verify(_domainDao).remove(DOMAIN_ID);
    }

    @Test(expected=CloudRuntimeException.class)
    public void testPublishRemoveEventsAndRemoveDomainExceptionDelete() {
        Mockito.when(_domainDao.remove(DOMAIN_ID)).thenReturn(false);
        domainManager.publishRemoveEventsAndRemoveDomain(domain);
        Mockito.verify(_messageBus).publish(Mockito.anyString(), Matchers.eq(DomainManager.MESSAGE_PRE_REMOVE_DOMAIN_EVENT),
                Matchers.eq(PublishScope.LOCAL), Matchers.eq(domain));
        Mockito.verify(_messageBus, Mockito.never()).publish(Mockito.anyString(), Matchers.eq(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT),
                Matchers.eq(PublishScope.LOCAL), Matchers.eq(domain));
        Mockito.verify(_domainDao).remove(DOMAIN_ID);
    }

    @Test(expected=CloudRuntimeException.class)
    public void testFailRemoveOperation() {
        domainManager.failRemoveOperation(domain, domainAccountsForCleanup, domainNetworkIds, true);
    }

}
