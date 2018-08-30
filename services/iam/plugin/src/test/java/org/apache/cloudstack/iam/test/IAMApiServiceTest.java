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
package org.apache.cloudstack.iam.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.iam.IAMGroupResponse;
import org.apache.cloudstack.api.response.iam.IAMPermissionResponse;
import org.apache.cloudstack.api.response.iam.IAMPolicyResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.iam.IAMApiService;
import org.apache.cloudstack.iam.IAMApiServiceImpl;
import org.apache.cloudstack.iam.api.IAMGroup;
import org.apache.cloudstack.iam.api.IAMPolicy;
import org.apache.cloudstack.iam.api.IAMPolicyPermission;
import org.apache.cloudstack.iam.api.IAMPolicyPermission.Permission;
import org.apache.cloudstack.iam.api.IAMService;
import org.apache.cloudstack.iam.server.IAMGroupVO;
import org.apache.cloudstack.iam.server.IAMPolicyPermissionVO;
import org.apache.cloudstack.iam.server.IAMPolicyVO;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.api.ApiServerService;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VirtualMachine;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class IAMApiServiceTest {

    @Inject
    IAMService _iamSrv;

    @Inject
    DomainDao _domainDao;

    @Inject
    IAMApiService _aclSrv;

    @Inject
    AccountManager _accountMgr;

    @Inject
    AccountDao _accountDao;

    @Inject
    ApiServerService _apiServer;

    private static Account caller;
    private static Long callerId;
    private static String callerAccountName = "tester";
    private static Long callerDomainId = 3L;
    private static String callerDomainPath = "/root/testdomain";
    private static DomainVO callerDomain;

    @BeforeClass
    public static void setUpClass() throws ConfigurationException {
    }

    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        caller = new AccountVO(callerAccountName, callerDomainId, null, Account.ACCOUNT_TYPE_ADMIN, UUID.randomUUID().toString());
        callerId = caller.getId();
        callerDomain = new DomainVO();
        callerDomain.setPath(callerDomainPath);
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());
        CallContext.register(user, caller);

        when(_domainDao.findById(callerDomainId)).thenReturn(callerDomain);
        doNothing().when(_accountMgr).checkAccess(caller, callerDomain);
    }

    @Test
    public void createIAMGroupTest() {
        IAMGroup group = new IAMGroupVO("group1", "tester group1");
        List<IAMGroup> groups = new ArrayList<IAMGroup>();
        groups.add(group);
        Pair<List<IAMGroup>, Integer> grpList = new Pair<List<IAMGroup>, Integer>(groups, 1);
        when(_iamSrv.createIAMGroup("group1", "tester group1", callerDomainPath)).thenReturn(group);
        when(_iamSrv.listIAMGroups(null, null, callerDomainPath, 0L, 20L)).thenReturn(grpList);

        IAMGroup createdGrp = _aclSrv.createIAMGroup(caller, "group1", "tester group1");
        assertNotNull("IAM group 'group1' failed to create ", createdGrp);
        ListResponse<IAMGroupResponse> grpResp = _aclSrv.listIAMGroups(null, null, callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        IAMGroupResponse resp = grpResp.getResponses().get(0);
        assertEquals("Error in created group name", "group1", resp.getName());
    }

    @Test
    public void deleteIAMGroupTest() {
        when(_iamSrv.deleteIAMGroup(1L)).thenReturn(true);
        assertTrue("failed to delete acl group 1", _aclSrv.deleteIAMGroup(1L));
    }

    @Test
    public void listIAMGroupTest() {
        IAMGroup group = new IAMGroupVO("group1", "tester group1");
        List<IAMGroup> groups = new ArrayList<IAMGroup>();
        groups.add(group);
        when(_iamSrv.listIAMGroups(callerId)).thenReturn(groups);
        List<IAMGroup> grps = _aclSrv.listIAMGroups(callerId);
        assertTrue(grps != null && grps.size() == 1);
        IAMGroup grp = grps.get(0);
        assertEquals("Error to retrieve group", "group1", grp.getName());
    }

    @Test
    public void addRemoveAccountToGroupTest() {
        IAMGroup group = new IAMGroupVO("group1", "tester group1");
        List<IAMGroup> groups = new ArrayList<IAMGroup>();
        groups.add(group);
        Long groupId = group.getId();
        List<Long> acctIds = new ArrayList<Long>();
        AccountVO acct1 = new AccountVO(100L);
        acct1.setAccountName("account1");
        AccountVO acct2 = new AccountVO(200L);
        acct2.setAccountName("account2");
        acctIds.add(acct1.getId());
        acctIds.add(acct2.getId());
        when(_accountDao.findById(acct1.getId())).thenReturn(acct1);
        when(_accountDao.findById(acct2.getId())).thenReturn(acct2);
        when(_iamSrv.addAccountsToGroup(acctIds, groupId)).thenReturn(group);
        when(_iamSrv.listAccountsByGroup(groupId)).thenReturn(acctIds);
        Pair<List<IAMGroup>, Integer> grpList = new Pair<List<IAMGroup>, Integer>(groups, 1);
        when(_iamSrv.listIAMGroups(null, "group1", callerDomainPath, 0L, 20L)).thenReturn(grpList);
        _aclSrv.addAccountsToGroup(acctIds, groupId);
        ListResponse<IAMGroupResponse> grpResp = _aclSrv.listIAMGroups(null, "group1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        IAMGroupResponse resp = grpResp.getResponses().get(0);
        Set<String> acctNames = resp.getAccountNameList();
        assertEquals("There should be 2 accounts in the group", 2, acctNames.size());
        assertTrue("account1 should be assigned to the group", acctNames.contains("account1"));
        assertTrue("account2 should be assigned to the group", acctNames.contains("account2"));
        // remove "account2" from group1
        acctIds.remove(1);
        List<Long> rmAccts = new ArrayList<Long>();
        rmAccts.add(acct2.getId());
        when(_iamSrv.removeAccountsFromGroup(rmAccts, groupId)).thenReturn(group);
        _aclSrv.removeAccountsFromGroup(acctIds, groupId);
        grpResp = _aclSrv.listIAMGroups(null, "group1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        resp = grpResp.getResponses().get(0);
        acctNames = resp.getAccountNameList();
        assertEquals("There should be 1 accounts in the group", 1, acctNames.size());
        assertFalse("account2 should not belong to the group anymore", acctNames.contains("account2"));
    }

    @Test
    public void createIAMPolicyTest() {
        IAMPolicy policy = new IAMPolicyVO("policy1", "tester policy1");
        List<IAMPolicy> policies = new ArrayList<IAMPolicy>();
        policies.add(policy);
        Pair<List<IAMPolicy>, Integer> policyList = new Pair<List<IAMPolicy>, Integer>(policies, 1);
        when(_iamSrv.createIAMPolicy("policy1", "tester policy1", null, callerDomainPath)).thenReturn(policy);
        when(_iamSrv.listIAMPolicies(null, null, callerDomainPath, 0L, 20L)).thenReturn(policyList);

        IAMPolicy createdPolicy = _aclSrv.createIAMPolicy(caller, "policy1", "tester policy1", null);
        assertNotNull("IAM policy 'policy1' failed to create ", createdPolicy);
        ListResponse<IAMPolicyResponse> policyResp = _aclSrv.listIAMPolicies(null, null, callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", policyResp.getCount() == 1);
        IAMPolicyResponse resp = policyResp.getResponses().get(0);
        assertEquals("Error in created group name", "policy1", resp.getName());
    }

    @Test
    public void deleteIAMPolicyTest() {
        when(_iamSrv.deleteIAMPolicy(1L)).thenReturn(true);
        assertTrue("failed to delete acl policy 1", _aclSrv.deleteIAMPolicy(1L));
    }

    @Test
    public void listIAMPolicyTest() {
        IAMPolicy policy = new IAMPolicyVO("policy1", "tester policy1");
        List<IAMPolicy> policies = new ArrayList<IAMPolicy>();
        policies.add(policy);
        when(_iamSrv.listIAMPolicies(callerId)).thenReturn(policies);
        List<IAMPolicy> polys = _aclSrv.listIAMPolicies(callerId);
        assertTrue(polys != null && polys.size() == 1);
        IAMPolicy p = polys.get(0);
        assertEquals("Error to retrieve group", "policy1", p.getName());
    }

    @Test
    public void addRemovePolicyToGroupTest() {
        IAMGroup group = new IAMGroupVO("group1", "tester group1");
        List<IAMGroup> groups = new ArrayList<IAMGroup>();
        groups.add(group);
        Long groupId = group.getId();
        List<Long> policyIds = new ArrayList<Long>();
        policyIds.add(100L);
        policyIds.add(200L);
        IAMPolicy policy1 = new IAMPolicyVO("policy1", "my first policy");
        IAMPolicy policy2 = new IAMPolicyVO("policy2", "my second policy");
        List<IAMPolicy> policies = new ArrayList<IAMPolicy>();
        policies.add(policy1);
        policies.add(policy2);
        when(_iamSrv.attachIAMPoliciesToGroup(policyIds, groupId)).thenReturn(group);
        when(_iamSrv.listIAMPoliciesByGroup(groupId)).thenReturn(policies);
        Pair<List<IAMGroup>, Integer> grpList = new Pair<List<IAMGroup>, Integer>(groups, 1);
        when(_iamSrv.listIAMGroups(null, "group1", callerDomainPath, 0L, 20L)).thenReturn(grpList);
        _aclSrv.attachIAMPoliciesToGroup(policyIds, groupId);
        ListResponse<IAMGroupResponse> grpResp = _aclSrv.listIAMGroups(null, "group1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        IAMGroupResponse resp = grpResp.getResponses().get(0);
        Set<String> policyNames = resp.getPolicyList();
        assertEquals("There should be 2 policies in the group", 2, policyNames.size());
        assertTrue("policy1 should be assigned to the group", policyNames.contains("policy1"));
        assertTrue("policy2 should be assigned to the group", policyNames.contains("policy2"));
        // remove "policy2" from group1
        policyIds.remove(1);
        policies.remove(policy2);
        when(_iamSrv.removeIAMPoliciesFromGroup(policyIds, groupId)).thenReturn(group);
        _aclSrv.removeIAMPoliciesFromGroup(policyIds, groupId);
        grpResp = _aclSrv.listIAMGroups(null, "group1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        resp = grpResp.getResponses().get(0);
        policyNames = resp.getPolicyList();
        assertEquals("There should be 1 policy attached to the group", 1, policyNames.size());
        assertFalse("policy2 should not belong to the group anymore", policyNames.contains("policy2"));
    }

    @Test
    public void addRemovePermissionToPolicyTest() {
        IAMPolicy policy = new IAMPolicyVO("policy1", "tester policy1");
        List<IAMPolicy> policies = new ArrayList<IAMPolicy>();
        policies.add(policy);
        Long policyId = policy.getId();
        Long resId = 200L;
        Class clz = ListVMsCmd.class;
        when(_apiServer.getCmdClass("listVirtualMachines")).thenReturn(clz);
        when(
                _iamSrv.addIAMPermissionToIAMPolicy(policyId, VirtualMachine.class.getSimpleName(),
                        PermissionScope.RESOURCE.toString(), resId, "listVirtualMachines",
                        AccessType.UseEntry.toString(), Permission.Allow, false)).thenReturn(policy);
        _aclSrv.addIAMPermissionToIAMPolicy(policyId, VirtualMachine.class.getSimpleName(),
                PermissionScope.RESOURCE, resId, "listVirtualMachines", Permission.Allow, false, false);
        Pair<List<IAMPolicy>, Integer> policyList = new Pair<List<IAMPolicy>, Integer>(policies, 1);
        List<IAMPolicyPermission> policyPerms = new ArrayList<IAMPolicyPermission>();
        IAMPolicyPermission perm = new IAMPolicyPermissionVO(policyId, "listVirtualMachines",
                VirtualMachine.class.getSimpleName(), AccessType.UseEntry.toString(),
                PermissionScope.RESOURCE.toString(),
                resId, Permission.Allow, false);
        policyPerms.add(perm);
        when(_iamSrv.listIAMPolicies(null, "policy1", callerDomainPath, 0L, 20L)).thenReturn(policyList);
        when(_iamSrv.listPolicyPermissions(policyId)).thenReturn(policyPerms);
        ListResponse<IAMPolicyResponse> policyResp = _aclSrv.listIAMPolicies(null, "policy1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", policyResp.getCount() == 1);
        IAMPolicyResponse resp = policyResp.getResponses().get(0);
        Set<IAMPermissionResponse> permList = resp.getPermissionList();
        assertTrue("Permission list should not be empty", permList != null && permList.size() > 0);
        IAMPermissionResponse permResp = permList.iterator().next();
        assertEquals("There should be one permission for listVirtualMachines", "listVirtualMachines", permResp.getAction());

        //remove permission from policy
        policyPerms.remove(perm);
        _aclSrv.removeIAMPermissionFromIAMPolicy(policyId, VirtualMachine.class.getSimpleName(),
                PermissionScope.RESOURCE, resId, "listVirtualMachines");
        policyResp = _aclSrv.listIAMPolicies(null, "policy1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", policyResp.getCount() == 1);
        resp = policyResp.getResponses().get(0);
        permList = resp.getPermissionList();
        assertTrue("Permission list should be empty", permList != null && permList.size() == 0);
    }

    @After
    public void tearDown() {
    }

    @Configuration
    @ComponentScan(basePackageClasses = {IAMApiServiceImpl.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public DomainDao domainDao() {
            return Mockito.mock(DomainDao.class);
        }

        @Bean
        public IAMService iamService() {
            return Mockito.mock(IAMService.class);
        }

        @Bean
        public AccountDao accountDao() {
            return Mockito.mock(AccountDao.class);
        }

        @Bean
        public NetworkDomainDao networkDomainDao() {
            return Mockito.mock(NetworkDomainDao.class);
        }

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public MessageBus messageBus() {
            return Mockito.mock(MessageBus.class);
        }

        @Bean
        public EntityManager entityMgr() {
            return Mockito.mock(EntityManager.class);
        }

        @Bean
        public ApiServerService apiServerService() {
            return Mockito.mock(ApiServerService.class);
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
