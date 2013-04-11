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

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.event.EventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.UserContextInitializer;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/affinityContext.xml")
public class AffinityApiUnitTest {

    @Inject
    AffinityGroupServiceImpl _affinityService;

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
    EventUtils _eventUtils;

    @Inject
    AccountDao _accountDao;

    private static long domainId = 5L;


    @BeforeClass
    public static void setUp() throws ConfigurationException {

    }

    @Before
    public void testSetUp() {
        ComponentContext.initComponentsLifeCycle();
        AccountVO acct = new AccountVO(200L);
        acct.setType(Account.ACCOUNT_TYPE_NORMAL);
        acct.setAccountName("user");
        acct.setDomainId(domainId);

        UserContext.registerContext(1, acct, null, true);

        when(_acctMgr.finalizeOwner((Account) anyObject(), anyString(), anyLong(), anyLong())).thenReturn(acct);
        when(_processor.getType()).thenReturn("mock");
        when(_accountDao.findByIdIncludingRemoved(0L)).thenReturn(acct);
    }

    @Test
    public void createAffinityGroupTest() {
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

    @Test(expected = ResourceInUseException.class)
    public void deleteAffinityGroupInUse() throws ResourceInUseException {
        List<AffinityGroupVMMapVO> affinityGroupVmMap = new ArrayList<AffinityGroupVMMapVO>();
        AffinityGroupVMMapVO mapVO = new AffinityGroupVMMapVO(20L, 10L);
        affinityGroupVmMap.add(mapVO);
        when(_affinityGroupVMMapDao.listByAffinityGroup(20L)).thenReturn(affinityGroupVmMap);

        AffinityGroupVO groupVO = new AffinityGroupVO();
        when(_groupDao.findById(20L)).thenReturn(groupVO);
        when(_groupDao.lockRow(20L, true)).thenReturn(groupVO);

        _affinityService.deleteAffinityGroup(20L, "user", domainId, null);
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

}
