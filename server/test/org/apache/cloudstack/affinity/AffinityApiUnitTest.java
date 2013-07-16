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
package org.apache.cloudstack.affinity;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class AffinityApiUnitTest {

    @Inject
    AffinityGroupService _affinityService;

    @Inject
    AccountManager _acctMgr;

    @Inject
    AffinityGroupProcessor _processor;

    @Inject
    AffinityGroupDao _groupDao;

    @Inject
    UserVmDao _vmDao;

    @Inject
    AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    AffinityGroupDao _affinityGroupDao;

    @Inject
    ActionEventUtils _eventUtils;

    @Inject
    AccountDao _accountDao;

    @Inject
    EventDao _eventDao;

    @Inject
    DedicatedResourceDao _dedicatedDao;


    private static long domainId = 5L;


    @BeforeClass
    public static void setUpClass() throws ConfigurationException {
    }

    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        AccountVO acct = new AccountVO(200L);
        acct.setType(Account.ACCOUNT_TYPE_NORMAL);
        acct.setAccountName("user");
        acct.setDomainId(domainId);

        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, acct);

        when(_acctMgr.finalizeOwner((Account) anyObject(), anyString(), anyLong(), anyLong())).thenReturn(acct);
        when(_processor.getType()).thenReturn("mock");
        when(_accountDao.findByIdIncludingRemoved(0L)).thenReturn(acct);

        AffinityGroupVO group = new AffinityGroupVO("group1", "mock", "mock group", domainId, 200L);
        Mockito.when(_affinityGroupDao.persist(Mockito.any(AffinityGroupVO.class))).thenReturn(group);
        Mockito.when(_affinityGroupDao.findById(Mockito.anyLong())).thenReturn(group);
        Mockito.when(_affinityGroupDao.findByAccountAndName(Mockito.anyLong(), Mockito.anyString())).thenReturn(group);
        Mockito.when(_affinityGroupDao.lockRow(Mockito.anyLong(), anyBoolean())).thenReturn(group);
        Mockito.when(_affinityGroupDao.expunge(Mockito.anyLong())).thenReturn(true);
        Mockito.when(_eventDao.persist(Mockito.any(EventVO.class))).thenReturn(new EventVO());
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void createAffinityGroupTest() {
        when(_groupDao.isNameInUse(anyLong(), anyLong(), eq("group1"))).thenReturn(false);
        AffinityGroup group = _affinityService.createAffinityGroup("user", domainId, "group1", "mock",
                "affinity group one");
        assertNotNull("Affinity group 'group1' of type 'mock' failed to create ", group);

    }

    @Test(expected = InvalidParameterValueException.class)
    public void invalidAffinityTypeTest() {
        AffinityGroup group = _affinityService.createAffinityGroup("user", domainId, "group1", "invalid",
                "affinity group one");

    }

    @Test(expected = InvalidParameterValueException.class)
    public void uniqueAffinityNameTest() {
        when(_groupDao.isNameInUse(anyLong(), anyLong(), eq("group1"))).thenReturn(true);
        AffinityGroup group2 = _affinityService.createAffinityGroup("user", domainId, "group1", "mock",
                "affinity group two");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteAffinityGroupInvalidIdTest() throws ResourceInUseException {
        when(_groupDao.findById(20L)).thenReturn(null);
        _affinityService.deleteAffinityGroup(20L, "user", domainId, "group1");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteAffinityGroupInvalidIdName() throws ResourceInUseException {
        when(_groupDao.findByAccountAndName(200L, "group1")).thenReturn(null);
        _affinityService.deleteAffinityGroup(null, "user", domainId, "group1");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteAffinityGroupNullIdName() throws ResourceInUseException {
        _affinityService.deleteAffinityGroup(null, "user", domainId, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void updateAffinityGroupVMRunning() throws ResourceInUseException {

        UserVmVO vm = new UserVmVO(10L, "test", "test", 101L, HypervisorType.Any, 21L, false, false, domainId, 200L,
                5L, "", "test", 1L);
        vm.setState(VirtualMachine.State.Running);
        when(_vmDao.findById(10L)).thenReturn(vm);

        List<Long> affinityGroupIds = new ArrayList<Long>();
        affinityGroupIds.add(20L);

        _affinityService.updateVMAffinityGroups(10L, affinityGroupIds);
    }

    @Configuration
    @ComponentScan(basePackageClasses = {AffinityGroupServiceImpl.class, ActionEventUtils.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public AccountDao accountDao() {
            return Mockito.mock(AccountDao.class);
        }

        @Bean
        public AccountService accountService() {
            return Mockito.mock(AccountService.class);
        }

        @Bean
        public AffinityGroupProcessor affinityGroupProcessor() {
            return Mockito.mock(AffinityGroupProcessor.class);
        }

        @Bean
        public AffinityGroupDao affinityGroupDao() {
            return Mockito.mock(AffinityGroupDao.class);
        }

        @Bean
        public AffinityGroupVMMapDao affinityGroupVMMapDao() {
            return Mockito.mock(AffinityGroupVMMapDao.class);
        }

        @Bean
        public DedicatedResourceDao dedicatedResourceDao() {
            return Mockito.mock(DedicatedResourceDao.class);
        }

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public EventDao eventDao() {
            return Mockito.mock(EventDao.class);
        }

        @Bean
        public UserVmDao userVMDao() {
            return Mockito.mock(UserVmDao.class);
        }

        @Bean
        public UserDao userDao() {
            return Mockito.mock(UserDao.class);
        }

        public static class Library implements TypeFilter {

            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }
}
