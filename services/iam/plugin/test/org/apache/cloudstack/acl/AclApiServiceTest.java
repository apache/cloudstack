package org.apache.cloudstack.acl;

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

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.acl.api.AclApiService;
import org.apache.cloudstack.acl.api.AclApiServiceImpl;
import org.apache.cloudstack.acl.api.response.AclGroupResponse;
import org.apache.cloudstack.acl.api.response.AclPermissionResponse;
import org.apache.cloudstack.acl.api.response.AclPolicyResponse;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.iam.api.AclGroup;
import org.apache.cloudstack.iam.api.AclPolicy;
import org.apache.cloudstack.iam.api.AclPolicyPermission;
import org.apache.cloudstack.iam.api.AclPolicyPermission.Permission;
import org.apache.cloudstack.iam.api.IAMService;
import org.apache.cloudstack.iam.server.AclGroupVO;
import org.apache.cloudstack.iam.server.AclPolicyPermissionVO;
import org.apache.cloudstack.iam.server.AclPolicyVO;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import com.cloud.api.ApiServerService;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class AclApiServiceTest {

    @Inject
    IAMService _iamSrv;

    @Inject
    DomainDao _domainDao;

    @Inject
    AclApiService _aclSrv;

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
    public void createAclGroupTest() {
        AclGroup group = new AclGroupVO("group1", "tester group1");
        List<AclGroup> groups = new ArrayList<AclGroup>();
        groups.add(group);
        Pair<List<AclGroup>, Integer> grpList = new Pair<List<AclGroup>, Integer>(groups, 1);
        when(_iamSrv.createAclGroup("group1", "tester group1", callerDomainPath)).thenReturn(group);
        when(_iamSrv.listAclGroups(null, null, callerDomainPath, 0L, 20L)).thenReturn(grpList);

        AclGroup createdGrp = _aclSrv.createAclGroup(caller, "group1", "tester group1");
        assertNotNull("Acl group 'group1' failed to create ", createdGrp);
        ListResponse<AclGroupResponse> grpResp = _aclSrv.listAclGroups(null, null, callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        AclGroupResponse resp = grpResp.getResponses().get(0);
        assertEquals("Error in created group name", "group1", resp.getName());
    }

    @Test
    public void deleteAclGroupTest() {
        when(_iamSrv.deleteAclGroup(1L)).thenReturn(true);
        assertTrue("failed to delete acl group 1", _aclSrv.deleteAclGroup(1L));
    }

    @Test
    public void listAclGroupTest() {
        AclGroup group = new AclGroupVO("group1", "tester group1");
        List<AclGroup> groups = new ArrayList<AclGroup>();
        groups.add(group);
        when(_iamSrv.listAclGroups(callerId)).thenReturn(groups);
        List<AclGroup> grps = _aclSrv.listAclGroups(callerId);
        assertTrue(grps != null && grps.size() == 1);
        AclGroup grp = grps.get(0);
        assertEquals("Error to retrieve group", "group1", grp.getName());
    }

    @Test
    public void addRemoveAccountToGroupTest() {
        AclGroup group = new AclGroupVO("group1", "tester group1");
        List<AclGroup> groups = new ArrayList<AclGroup>();
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
        Pair<List<AclGroup>, Integer> grpList = new Pair<List<AclGroup>, Integer>(groups, 1);
        when(_iamSrv.listAclGroups(null, "group1", callerDomainPath, 0L, 20L)).thenReturn(grpList);
        _aclSrv.addAccountsToGroup(acctIds, groupId);
        ListResponse<AclGroupResponse> grpResp = _aclSrv.listAclGroups(null, "group1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        AclGroupResponse resp = grpResp.getResponses().get(0);
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
        grpResp = _aclSrv.listAclGroups(null, "group1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        resp = grpResp.getResponses().get(0);
        acctNames = resp.getAccountNameList();
        assertEquals("There should be 1 accounts in the group", 1, acctNames.size());
        assertFalse("account2 should not belong to the group anymore", acctNames.contains("account2"));
    }

    @Test
    public void createAclPolicyTest() {
        AclPolicy policy = new AclPolicyVO("policy1", "tester policy1");
        List<AclPolicy> policies = new ArrayList<AclPolicy>();
        policies.add(policy);
        Pair<List<AclPolicy>, Integer> policyList = new Pair<List<AclPolicy>, Integer>(policies, 1);
        when(_iamSrv.createAclPolicy("policy1", "tester policy1", null)).thenReturn(policy);
        when(_iamSrv.listAclPolicies(null, null, callerDomainPath, 0L, 20L)).thenReturn(policyList);

        AclPolicy createdPolicy = _aclSrv.createAclPolicy(caller, "policy1", "tester policy1", null);
        assertNotNull("Acl policy 'policy1' failed to create ", createdPolicy);
        ListResponse<AclPolicyResponse> policyResp = _aclSrv.listAclPolicies(null, null, callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", policyResp.getCount() == 1);
        AclPolicyResponse resp = policyResp.getResponses().get(0);
        assertEquals("Error in created group name", "policy1", resp.getName());
    }

    @Test
    public void deleteAclPolicyTest() {
        when(_iamSrv.deleteAclPolicy(1L)).thenReturn(true);
        assertTrue("failed to delete acl policy 1", _aclSrv.deleteAclPolicy(1L));
    }

    @Test
    public void listAclPolicyTest() {
        AclPolicy policy = new AclPolicyVO("policy1", "tester policy1");
        List<AclPolicy> policies = new ArrayList<AclPolicy>();
        policies.add(policy);
        when(_iamSrv.listAclPolicies(callerId)).thenReturn(policies);
        List<AclPolicy> polys = _aclSrv.listAclPolicies(callerId);
        assertTrue(polys != null && polys.size() == 1);
        AclPolicy p = polys.get(0);
        assertEquals("Error to retrieve group", "policy1", p.getName());
    }

    @Test
    public void addRemovePolicyToGroupTest() {
        AclGroup group = new AclGroupVO("group1", "tester group1");
        List<AclGroup> groups = new ArrayList<AclGroup>();
        groups.add(group);
        Long groupId = group.getId();
        List<Long> policyIds = new ArrayList<Long>();
        policyIds.add(100L);
        policyIds.add(200L);
        AclPolicy policy1 = new AclPolicyVO("policy1", "my first policy");
        AclPolicy policy2 = new AclPolicyVO("policy2", "my second policy");
        List<AclPolicy> policies = new ArrayList<AclPolicy>();
        policies.add(policy1);
        policies.add(policy2);
        when(_iamSrv.attachAclPoliciesToGroup(policyIds, groupId)).thenReturn(group);
        when(_iamSrv.listAclPoliciesByGroup(groupId)).thenReturn(policies);
        Pair<List<AclGroup>, Integer> grpList = new Pair<List<AclGroup>, Integer>(groups, 1);
        when(_iamSrv.listAclGroups(null, "group1", callerDomainPath, 0L, 20L)).thenReturn(grpList);
        _aclSrv.attachAclPoliciesToGroup(policyIds, groupId);
        ListResponse<AclGroupResponse> grpResp = _aclSrv.listAclGroups(null, "group1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        AclGroupResponse resp = grpResp.getResponses().get(0);
        Set<String> policyNames = resp.getPolicyList();
        assertEquals("There should be 2 policies in the group", 2, policyNames.size());
        assertTrue("policy1 should be assigned to the group", policyNames.contains("policy1"));
        assertTrue("policy2 should be assigned to the group", policyNames.contains("policy2"));
        // remove "policy2" from group1
        policyIds.remove(1);
        policies.remove(policy2);
        when(_iamSrv.removeAclPoliciesFromGroup(policyIds, groupId)).thenReturn(group);
        _aclSrv.removeAclPoliciesFromGroup(policyIds, groupId);
        grpResp = _aclSrv.listAclGroups(null, "group1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", grpResp.getCount() == 1);
        resp = grpResp.getResponses().get(0);
        policyNames = resp.getPolicyList();
        assertEquals("There should be 1 policy attached to the group", 1, policyNames.size());
        assertFalse("policy2 should not belong to the group anymore", policyNames.contains("policy2"));
    }

    @Test
    public void addRemovePermissionToPolicyTest() {
        AclPolicy policy = new AclPolicyVO("policy1", "tester policy1");
        List<AclPolicy> policies = new ArrayList<AclPolicy>();
        policies.add(policy);
        Long policyId = policy.getId();
        Long resId = 200L;
        Class clz = ListVMsCmd.class;
        when(_apiServer.getCmdClass("listVirtualMachines")).thenReturn(clz);
        when(
                _iamSrv.addAclPermissionToAclPolicy(policyId, AclEntityType.VirtualMachine.toString(), PermissionScope.RESOURCE.toString(), resId, "listVirtualMachines",
                        AccessType.ListEntry.toString(), Permission.Allow)).thenReturn(policy);
        _aclSrv.addAclPermissionToAclPolicy(policyId, AclEntityType.VirtualMachine.toString(), PermissionScope.RESOURCE, resId, "listVirtualMachines", Permission.Allow);
        Pair<List<AclPolicy>, Integer> policyList = new Pair<List<AclPolicy>, Integer>(policies, 1);
        List<AclPolicyPermission> policyPerms = new ArrayList<AclPolicyPermission>();
        AclPolicyPermission perm = new AclPolicyPermissionVO(policyId, "listVirtualMachines", AclEntityType.VirtualMachine.toString(), AccessType.ListEntry.toString(),
                PermissionScope.RESOURCE.toString(),
                resId, Permission.Allow);
        policyPerms.add(perm);
        when(_iamSrv.listAclPolicies(null, "policy1", callerDomainPath, 0L, 20L)).thenReturn(policyList);
        when(_iamSrv.listPolicyPermissions(policyId)).thenReturn(policyPerms);
        ListResponse<AclPolicyResponse> policyResp = _aclSrv.listAclPolicies(null, "policy1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", policyResp.getCount() == 1);
        AclPolicyResponse resp = policyResp.getResponses().get(0);
        Set<AclPermissionResponse> permList = resp.getPermissionList();
        assertTrue("Permission list should not be empty", permList != null && permList.size() > 0);
        AclPermissionResponse permResp = permList.iterator().next();
        assertEquals("There should be one permission for listVirtualMachines", "listVirtualMachines", permResp.getAction());

        //remove permission from policy
        policyPerms.remove(perm);
        _aclSrv.removeAclPermissionFromAclPolicy(policyId, AclEntityType.VirtualMachine.toString(), PermissionScope.RESOURCE, resId, "listVirtualMachines");
        policyResp = _aclSrv.listAclPolicies(null, "policy1", callerDomainId, 0L, 20L);
        assertTrue("No. of response items should be one", policyResp.getCount() == 1);
        resp = policyResp.getResponses().get(0);
        permList = resp.getPermissionList();
        assertTrue("Permission list should be empty", permList != null && permList.size() == 0);
    }

    @After
    public void tearDown() {
    }

    @Configuration
    @ComponentScan(basePackageClasses = {AclApiServiceImpl.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
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
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }
        
        @Bean
        public MessageBus messageBus() {
            return Mockito.mock(MessageBus.class);
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
