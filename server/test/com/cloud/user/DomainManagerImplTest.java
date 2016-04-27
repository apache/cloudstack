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
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.dao.AccountDao;
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

}
