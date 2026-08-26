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

package org.apache.cloudstack.vm.bootgroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.command.user.bootgroup.AddMemberToInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.CreateInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.CreateInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.command.user.bootgroup.DeleteInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.DeleteInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.command.user.bootgroup.ListInstanceBootGroupMembersCmd;
import org.apache.cloudstack.api.command.user.bootgroup.ListInstanceBootGroupReadinessRulesCmd;
import org.apache.cloudstack.api.command.user.bootgroup.RebootInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.RemoveInstanceBootGroupMemberCmd;
import org.apache.cloudstack.api.command.user.bootgroup.StartInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.StopInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupMemberCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.query.dao.InstanceBootGroupJoinDao;
import org.apache.cloudstack.api.response.InstanceBootGroupMemberChildResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupMemberResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupReadinessRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRuleService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.InstanceBootGroupDao;
import com.cloud.vm.dao.InstanceBootGroupDetailsDao;
import com.cloud.vm.dao.InstanceBootGroupMemberDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessCheckResultDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDetailsDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupApiServiceImplTest {

    private static final long ACCOUNT_ID = 1L;
    private static final long DOMAIN_ID = 10L;
    private static final long GROUP_ID = 100L;
    private static final long VM_ID = 200L;
    private static final long VM2_ID = 201L;
    private static final long INSTANCE_GROUP_ID = 300L;
    private static final long MEMBER_ID = 400L;
    private static final long RULE_ID = 500L;

    @InjectMocks
    InstanceBootGroupApiServiceImpl service;

    @Mock
    InstanceBootGroupDao instanceBootGroupDao;
    @Mock
    InstanceBootGroupJoinDao instanceBootGroupJoinDao;
    @Mock
    InstanceBootGroupMemberDao instanceBootGroupMemberDao;
    @Mock
    AccountManager accountManager;
    @Mock
    UserVmDao userVmDao;
    @Mock
    InstanceGroupDao instanceGroupDao;
    @Mock
    InstanceBootGroupManager instanceBootGroupManager;
    @Mock
    InstanceBootGroupMembershipGuard instanceBootGroupMembershipGuard;
    @Mock
    InstanceBootGroupReadinessRuleService instanceBootGroupReadinessRuleService;
    @Mock
    InstanceBootGroupReadinessRuleDao instanceBootGroupReadinessRuleDao;
    @Mock
    InstanceBootGroupReadinessRuleDetailsDao instanceBootGroupReadinessRuleDetailsDao;
    @Mock
    InstanceBootGroupReadinessCheckResultDao instanceBootGroupReadinessCheckResultDao;
    @Mock
    InstanceBootGroupDetailsDao instanceBootGroupDetailsDao;
    @Mock
    InstanceGroupVMMapDao instanceGroupVMMapDao;

    @Mock
    Account callerMock;

    private MockedStatic<CallContext> callContextMocked;
    private CallContext callContextMock;

    @Before
    public void setUp() {
        callContextMocked = Mockito.mockStatic(CallContext.class);
        callContextMock = mock(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContextMock);
        when(callContextMock.getCallingAccount()).thenReturn(callerMock);
        Mockito.lenient().when(callerMock.getId()).thenReturn(ACCOUNT_ID);
        Mockito.lenient().when(callerMock.getDomainId()).thenReturn(DOMAIN_ID);

        doNothing().when(accountManager).checkAccess(any(Account.class), any(), eq(true), any());
    }

    @After
    public void tearDown() {
        callContextMocked.close();
    }

    // ---------------------------------------------------------------- helpers

    private InstanceBootGroupVO newGroup(long id, String name, long accountId) {
        InstanceBootGroupVO group = new InstanceBootGroupVO(name, "desc", accountId, DOMAIN_ID);
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private InstanceGroupVO newInstanceGroup(long id, String name, long accountId) {
        InstanceGroupVO group = new InstanceGroupVO(name, accountId);
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private UserVmVO newVm(long id, String displayName, String hostName, long accountId, VirtualMachine.State state) {
        UserVmVO vm = new UserVmVO(id, "i-1-" + id + "-VM", displayName, 1L, HypervisorType.KVM, 1L, false, false,
                DOMAIN_ID, accountId, 1L, 1L, null, null, null, "i-1-" + id + "-VM");
        vm.setHostName(hostName);
        vm.setState(state);
        return vm;
    }

    private InstanceBootGroupMemberVO newMember(long id, long bootGroupId, InstanceBootGroupMember.MemberType type, long memberId, int order) {
        InstanceBootGroupMemberVO member = new InstanceBootGroupMemberVO(bootGroupId, type, memberId, order);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private InstanceBootGroupReadinessRuleVO newRule(long id, long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId,
            InstanceBootGroupReadinessRule.RuleType ruleType, boolean enabled, String name) {
        InstanceBootGroupReadinessRuleVO rule = new InstanceBootGroupReadinessRuleVO(name, bootGroupId, itemType, itemId, ruleType, enabled);
        ReflectionTestUtils.setField(rule, "id", id);
        return rule;
    }

    private String field(Object response, String name) {
        Object value = ReflectionTestUtils.getField(response, name);
        return value == null ? null : value.toString();
    }

    // ---------------------------------------------------------------- createInstanceBootGroup

    @Test
    public void testCreateInstanceBootGroupSuccessWithOverrides() {
        CreateInstanceBootGroupCmd cmd = mock(CreateInstanceBootGroupCmd.class);
        when(cmd.getName()).thenReturn("group1");
        when(cmd.getDescription()).thenReturn("desc");
        when(cmd.getAccountName()).thenReturn("acct");
        when(cmd.getDomainId()).thenReturn(DOMAIN_ID);
        when(cmd.getProjectId()).thenReturn(null);
        when(cmd.getReadinessAttemptTimeoutSeconds()).thenReturn(100L);
        when(cmd.getReadinessMaxRetryAttempts()).thenReturn(-1L);
        when(cmd.getReadinessRebootOnRetry()).thenReturn(true);
        when(cmd.getReadinessInitialDelaySeconds()).thenReturn(null);

        Account owner = mock(Account.class);
        when(owner.getId()).thenReturn(ACCOUNT_ID);
        when(owner.getDomainId()).thenReturn(DOMAIN_ID);
        when(accountManager.finalizeOwner(callerMock, "acct", DOMAIN_ID, null)).thenReturn(owner);
        when(instanceBootGroupDao.isNameInUse(ACCOUNT_ID, "group1")).thenReturn(false);
        when(instanceBootGroupDao.persist(any(InstanceBootGroupVO.class))).thenAnswer(inv -> {
            InstanceBootGroupVO vo = inv.getArgument(0);
            ReflectionTestUtils.setField(vo, "id", GROUP_ID);
            return vo;
        });

        InstanceBootGroup result = service.createInstanceBootGroup(cmd);

        assertNotNull(result);
        assertEquals(GROUP_ID, result.getId());
        verify(callContextMock).setEventResourceId(GROUP_ID);
        verify(instanceBootGroupDetailsDao).setDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessAttemptTimeoutSeconds.key(), "100");
        verify(instanceBootGroupDetailsDao).setDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessMaxRetryAttempts.key(), null);
        verify(instanceBootGroupDetailsDao).setDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessRebootOnRetry.key(), "true");
        verify(instanceBootGroupDetailsDao, never()).setDetail(eq(GROUP_ID), eq(InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key()), any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateInstanceBootGroupNameInUseThrows() {
        CreateInstanceBootGroupCmd cmd = mock(CreateInstanceBootGroupCmd.class);
        when(cmd.getName()).thenReturn("group1");
        Account owner = mock(Account.class);
        when(owner.getId()).thenReturn(ACCOUNT_ID);
        when(accountManager.finalizeOwner(any(), any(), any(), any())).thenReturn(owner);
        when(instanceBootGroupDao.isNameInUse(ACCOUNT_ID, "group1")).thenReturn(true);

        service.createInstanceBootGroup(cmd);
    }

    // ---------------------------------------------------------------- deleteInstanceBootGroup

    @Test
    public void testDeleteInstanceBootGroupSuccess() {
        DeleteInstanceBootGroupCmd cmd = mock(DeleteInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {
            transactionMock.when(() -> Transaction.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<Boolean> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            boolean result = service.deleteInstanceBootGroup(cmd);

            assertTrue(result);
            verify(instanceBootGroupMemberDao).deleteByBootGroupId(GROUP_ID);
            verify(instanceBootGroupDao).remove(GROUP_ID);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteInstanceBootGroupNotFound() {
        DeleteInstanceBootGroupCmd cmd = mock(DeleteInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(null);

        service.deleteInstanceBootGroup(cmd);
    }

    // ---------------------------------------------------------------- updateInstanceBootGroup

    @Test
    public void testUpdateInstanceBootGroupChangeNameSuccess() {
        UpdateInstanceBootGroupCmd cmd = mock(UpdateInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getName()).thenReturn("newname");
        when(cmd.getDescription()).thenReturn("newdesc");
        when(cmd.getReadinessAttemptTimeoutSeconds()).thenReturn(null);
        when(cmd.getReadinessMaxRetryAttempts()).thenReturn(null);
        when(cmd.getReadinessRebootOnRetry()).thenReturn(null);
        when(cmd.getReadinessInitialDelaySeconds()).thenReturn(null);

        InstanceBootGroupVO group = newGroup(GROUP_ID, "oldname", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        Account owner = mock(Account.class);
        when(owner.getId()).thenReturn(ACCOUNT_ID);
        when(accountManager.getAccount(ACCOUNT_ID)).thenReturn(owner);
        when(instanceBootGroupDao.isNameInUse(ACCOUNT_ID, "newname")).thenReturn(false);

        InstanceBootGroup result = service.updateInstanceBootGroup(cmd);

        assertNotNull(result);
        assertEquals("newname", group.getName());
        assertEquals("newdesc", group.getDescription());
        verify(instanceBootGroupDao).update(GROUP_ID, group);
    }

    @Test
    public void testUpdateInstanceBootGroupSameNameSkipsUniquenessCheck() {
        UpdateInstanceBootGroupCmd cmd = mock(UpdateInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getName()).thenReturn("samename");
        InstanceBootGroupVO group = newGroup(GROUP_ID, "samename", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        service.updateInstanceBootGroup(cmd);

        verify(instanceBootGroupDao, never()).isNameInUse(anyLong(), any());
        verify(accountManager, never()).getAccount(anyLong());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateInstanceBootGroupNameInUseThrows() {
        UpdateInstanceBootGroupCmd cmd = mock(UpdateInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getName()).thenReturn("newname");
        InstanceBootGroupVO group = newGroup(GROUP_ID, "oldname", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        Account owner = mock(Account.class);
        when(owner.getId()).thenReturn(ACCOUNT_ID);
        when(accountManager.getAccount(ACCOUNT_ID)).thenReturn(owner);
        when(instanceBootGroupDao.isNameInUse(ACCOUNT_ID, "newname")).thenReturn(true);

        service.updateInstanceBootGroup(cmd);
    }

    @Test
    public void testUpdateInstanceBootGroupOverridesSetAndCleared() {
        UpdateInstanceBootGroupCmd cmd = mock(UpdateInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getReadinessAttemptTimeoutSeconds()).thenReturn(-1L);
        when(cmd.getReadinessMaxRetryAttempts()).thenReturn(7L);
        when(cmd.getReadinessRebootOnRetry()).thenReturn(false);
        when(cmd.getReadinessInitialDelaySeconds()).thenReturn(15L);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        service.updateInstanceBootGroup(cmd);

        verify(instanceBootGroupDetailsDao).setDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessAttemptTimeoutSeconds.key(), null);
        verify(instanceBootGroupDetailsDao).setDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessMaxRetryAttempts.key(), "7");
        verify(instanceBootGroupDetailsDao).setDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessRebootOnRetry.key(), "false");
        verify(instanceBootGroupDetailsDao).setDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key(), "15");
    }

    // ---------------------------------------------------------------- getGroupAndCheckAccess

    @Test(expected = InvalidParameterValueException.class)
    public void testGetGroupAndCheckAccessNotFoundThrows() {
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(null);
        service.getGroupAndCheckAccess(GROUP_ID);
    }

    @Test
    public void testGetGroupAndCheckAccessSuccessDelegatesToAccountManager() {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        InstanceBootGroupVO result = service.getGroupAndCheckAccess(GROUP_ID);

        assertEquals(group, result);
        verify(accountManager).checkAccess(callerMock, null, true, group);
    }

    // ---------------------------------------------------------------- addMemberToInstanceBootGroup

    @Test
    public void testAddMemberToInstanceBootGroupVirtualMachineSuccess() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(0);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(null);

        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        UserVmVO vm = newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID)).thenReturn(null);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(new ArrayList<>());
        when(instanceBootGroupMemberDao.persist(any(InstanceBootGroupMemberVO.class))).thenAnswer(inv -> inv.getArgument(0));

        InstanceBootGroupMember result = service.addMemberToInstanceBootGroup(cmd);

        assertNotNull(result);
        assertEquals(InstanceBootGroupMember.MemberType.VirtualMachine, result.getMemberType());
        assertEquals(VM_ID, result.getMemberId());
        verify(instanceBootGroupMembershipGuard).validateVmEligibleForGroupMembership(VM_ID);
    }

    @Test
    public void testAddMemberToInstanceBootGroupInstanceGroupSuccess() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(0);
        when(cmd.getVirtualMachineId()).thenReturn(null);
        when(cmd.getInstanceGroupId()).thenReturn(INSTANCE_GROUP_ID);

        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        InstanceGroupVO instanceGroup = newInstanceGroup(INSTANCE_GROUP_ID, "ig1", ACCOUNT_ID);
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(instanceGroup);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID)).thenReturn(null);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(new ArrayList<>());
        when(instanceBootGroupMemberDao.persist(any(InstanceBootGroupMemberVO.class))).thenAnswer(inv -> inv.getArgument(0));

        InstanceBootGroupMember result = service.addMemberToInstanceBootGroup(cmd);

        assertNotNull(result);
        assertEquals(InstanceBootGroupMember.MemberType.InstanceGroup, result.getMemberType());
        assertEquals(INSTANCE_GROUP_ID, result.getMemberId());
        verify(instanceBootGroupMembershipGuard).validateInstanceGroupEligibleForBootGroupMembership(INSTANCE_GROUP_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAddMemberToInstanceBootGroupNegativeOrderThrows() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(-1);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        service.addMemberToInstanceBootGroup(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAddMemberToInstanceBootGroupBothIdsSpecifiedThrows() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(0);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(INSTANCE_GROUP_ID);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        service.addMemberToInstanceBootGroup(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAddMemberToInstanceBootGroupNeitherIdSpecifiedThrows() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(0);
        when(cmd.getVirtualMachineId()).thenReturn(null);
        when(cmd.getInstanceGroupId()).thenReturn(null);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        service.addMemberToInstanceBootGroup(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAddMemberToInstanceBootGroupAlreadyMemberThrows() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(0);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(null);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        UserVmVO vm = newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);
        InstanceBootGroupMemberVO existing = newMember(999L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 0);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID)).thenReturn(existing);

        service.addMemberToInstanceBootGroup(cmd);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testAddMemberToInstanceBootGroupDifferentAccountThrows() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(0);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(null);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        UserVmVO vm = newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID + 1, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);

        service.addMemberToInstanceBootGroup(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAddMemberToInstanceBootGroupInstanceGroupNotFoundThrows() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(0);
        when(cmd.getVirtualMachineId()).thenReturn(null);
        when(cmd.getInstanceGroupId()).thenReturn(INSTANCE_GROUP_ID);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(null);

        service.addMemberToInstanceBootGroup(cmd);
    }

    @Test
    public void testAddMemberToInstanceBootGroupShiftsCollidingSiblingsUp() {
        AddMemberToInstanceBootGroupCmd cmd = mock(AddMemberToInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        when(cmd.getOrder()).thenReturn(2);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(null);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        UserVmVO vm = newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID)).thenReturn(null);

        InstanceBootGroupMemberVO siblingBelow = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 11L, 1);
        InstanceBootGroupMemberVO siblingAtOrder = newMember(2L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 12L, 2);
        InstanceBootGroupMemberVO siblingAbove = newMember(3L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 13L, 5);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(Arrays.asList(siblingBelow, siblingAtOrder, siblingAbove));
        when(instanceBootGroupMemberDao.persist(any(InstanceBootGroupMemberVO.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addMemberToInstanceBootGroup(cmd);

        verify(instanceBootGroupMemberDao, never()).update(eq(1L), any());
        ArgumentCaptor<InstanceBootGroupMemberVO> captor = ArgumentCaptor.forClass(InstanceBootGroupMemberVO.class);
        verify(instanceBootGroupMemberDao).update(eq(2L), captor.capture());
        assertEquals(3, captor.getValue().getOrder());
        verify(instanceBootGroupMemberDao).update(eq(3L), captor.capture());
        assertEquals(6, captor.getValue().getOrder());
    }

    // ---------------------------------------------------------------- removeInstanceBootGroupMember

    @Test
    public void testRemoveInstanceBootGroupMemberSuccess() {
        RemoveInstanceBootGroupMemberCmd cmd = mock(RemoveInstanceBootGroupMemberCmd.class);
        when(cmd.getId()).thenReturn(MEMBER_ID);
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 0);
        when(instanceBootGroupMemberDao.findById(MEMBER_ID)).thenReturn(member);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        boolean result = service.removeInstanceBootGroupMember(cmd);

        assertTrue(result);
        verify(instanceBootGroupMemberDao).expunge(MEMBER_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRemoveInstanceBootGroupMemberNotFoundThrows() {
        RemoveInstanceBootGroupMemberCmd cmd = mock(RemoveInstanceBootGroupMemberCmd.class);
        when(cmd.getId()).thenReturn(MEMBER_ID);
        when(instanceBootGroupMemberDao.findById(MEMBER_ID)).thenReturn(null);

        service.removeInstanceBootGroupMember(cmd);
    }

    // ---------------------------------------------------------------- updateInstanceBootGroupMember (reorder)

    @Test
    public void testUpdateInstanceBootGroupMemberNotFoundThrows() {
        UpdateInstanceBootGroupMemberCmd cmd = mock(UpdateInstanceBootGroupMemberCmd.class);
        when(cmd.getId()).thenReturn(MEMBER_ID);
        when(instanceBootGroupMemberDao.findById(MEMBER_ID)).thenReturn(null);
        try {
            service.updateInstanceBootGroupMember(cmd);
            org.junit.Assert.fail("expected InvalidParameterValueException");
        } catch (InvalidParameterValueException expected) {
            // expected
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateInstanceBootGroupMemberNegativeOrderThrows() {
        UpdateInstanceBootGroupMemberCmd cmd = mock(UpdateInstanceBootGroupMemberCmd.class);
        when(cmd.getId()).thenReturn(MEMBER_ID);
        when(cmd.getOrder()).thenReturn(-1);
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 2);
        when(instanceBootGroupMemberDao.findById(MEMBER_ID)).thenReturn(member);

        service.updateInstanceBootGroupMember(cmd);
    }

    @Test
    public void testUpdateInstanceBootGroupMemberNoChangeDoesNotShiftOrPersist() {
        UpdateInstanceBootGroupMemberCmd cmd = mock(UpdateInstanceBootGroupMemberCmd.class);
        when(cmd.getId()).thenReturn(MEMBER_ID);
        when(cmd.getOrder()).thenReturn(2);
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 2);
        when(instanceBootGroupMemberDao.findById(MEMBER_ID)).thenReturn(member);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        service.updateInstanceBootGroupMember(cmd);

        verify(instanceBootGroupMemberDao, never()).update(anyLong(), any());
        verify(instanceBootGroupMemberDao, never()).listByBootGroupId(anyLong());
    }

    @Test
    public void testUpdateInstanceBootGroupMemberMoveDownShiftsBetweenSiblingsDown() {
        UpdateInstanceBootGroupMemberCmd cmd = mock(UpdateInstanceBootGroupMemberCmd.class);
        when(cmd.getId()).thenReturn(MEMBER_ID);
        when(cmd.getOrder()).thenReturn(3);
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 1);
        when(instanceBootGroupMemberDao.findById(MEMBER_ID)).thenReturn(member);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        InstanceBootGroupMemberVO siblingUnaffectedLow = newMember(11L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 1L, 0);
        InstanceBootGroupMemberVO siblingB = newMember(12L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 2L, 2);
        InstanceBootGroupMemberVO siblingC = newMember(13L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 3L, 3);
        InstanceBootGroupMemberVO siblingUnaffectedHigh = newMember(14L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 4L, 5);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID))
                .thenReturn(Arrays.asList(member, siblingUnaffectedLow, siblingB, siblingC, siblingUnaffectedHigh));

        service.updateInstanceBootGroupMember(cmd);

        verify(instanceBootGroupMemberDao, never()).update(eq(11L), any());
        verify(instanceBootGroupMemberDao, never()).update(eq(14L), any());
        ArgumentCaptor<InstanceBootGroupMemberVO> captor = ArgumentCaptor.forClass(InstanceBootGroupMemberVO.class);
        verify(instanceBootGroupMemberDao).update(eq(12L), captor.capture());
        assertEquals(1, captor.getValue().getOrder());
        verify(instanceBootGroupMemberDao).update(eq(13L), captor.capture());
        assertEquals(2, captor.getValue().getOrder());
        verify(instanceBootGroupMemberDao).update(eq(MEMBER_ID), captor.capture());
        assertEquals(3, captor.getValue().getOrder());
    }

    @Test
    public void testUpdateInstanceBootGroupMemberMoveUpShiftsBetweenSiblingsUp() {
        UpdateInstanceBootGroupMemberCmd cmd = mock(UpdateInstanceBootGroupMemberCmd.class);
        when(cmd.getId()).thenReturn(MEMBER_ID);
        when(cmd.getOrder()).thenReturn(1);
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 3);
        when(instanceBootGroupMemberDao.findById(MEMBER_ID)).thenReturn(member);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        InstanceBootGroupMemberVO siblingUnaffectedLow = newMember(11L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 1L, 0);
        InstanceBootGroupMemberVO siblingB = newMember(12L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 2L, 1);
        InstanceBootGroupMemberVO siblingC = newMember(13L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 3L, 2);
        InstanceBootGroupMemberVO siblingUnaffectedHigh = newMember(14L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 4L, 4);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID))
                .thenReturn(Arrays.asList(member, siblingUnaffectedLow, siblingB, siblingC, siblingUnaffectedHigh));

        service.updateInstanceBootGroupMember(cmd);

        verify(instanceBootGroupMemberDao, never()).update(eq(11L), any());
        verify(instanceBootGroupMemberDao, never()).update(eq(14L), any());
        ArgumentCaptor<InstanceBootGroupMemberVO> captor = ArgumentCaptor.forClass(InstanceBootGroupMemberVO.class);
        verify(instanceBootGroupMemberDao).update(eq(12L), captor.capture());
        assertEquals(2, captor.getValue().getOrder());
        verify(instanceBootGroupMemberDao).update(eq(13L), captor.capture());
        assertEquals(3, captor.getValue().getOrder());
        verify(instanceBootGroupMemberDao).update(eq(MEMBER_ID), captor.capture());
        assertEquals(1, captor.getValue().getOrder());
    }

    // ---------------------------------------------------------------- listInstanceBootGroupMembers / readiness

    private ListInstanceBootGroupMembersCmd baseListMembersCmd(boolean readiness, boolean children, boolean ignoreState) {
        ListInstanceBootGroupMembersCmd cmd = mock(ListInstanceBootGroupMembersCmd.class);
        when(cmd.getBootGroupId()).thenReturn(GROUP_ID);
        when(cmd.getMemberType()).thenReturn(null);
        when(cmd.isReadinessDetailRequested()).thenReturn(readiness);
        when(cmd.isChildrenDetailRequested()).thenReturn(children);
        when(cmd.isIgnoreInstanceState()).thenReturn(ignoreState);
        return cmd;
    }

    @Test
    public void testListInstanceBootGroupMembersSortedByOrder() {
        ListInstanceBootGroupMembersCmd cmd = baseListMembersCmd(false, false, false);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        InstanceBootGroupMemberVO memberHighOrder = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 5);
        InstanceBootGroupMemberVO memberLowOrder = newMember(2L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM2_ID, 1);
        ReflectionTestUtils.setField(memberHighOrder, "uuid", "uuid-high-order");
        ReflectionTestUtils.setField(memberLowOrder, "uuid", "uuid-low-order");
        when(instanceBootGroupMemberDao.searchAndCountByBootGroupId(GROUP_ID))
                .thenReturn(new com.cloud.utils.Pair<>(new ArrayList<>(Arrays.asList(memberHighOrder, memberLowOrder)), 2));
        when(userVmDao.findById(VM_ID)).thenReturn(newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running));
        when(userVmDao.findById(VM2_ID)).thenReturn(newVm(VM2_ID, "vm2", "vm2host", ACCOUNT_ID, VirtualMachine.State.Running));

        ListResponse<InstanceBootGroupMemberResponse> response = service.listInstanceBootGroupMembers(cmd);

        assertEquals(2, response.getResponses().size());
        assertEquals("uuid-low-order", field(response.getResponses().get(0), "id"));
        assertEquals("uuid-high-order", field(response.getResponses().get(1), "id"));
    }

    @Test
    public void testListInstanceBootGroupMembersNoRulesRunningVmIsReady() {
        ListInstanceBootGroupMembersCmd cmd = baseListMembersCmd(true, false, false);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        InstanceBootGroupMemberVO member = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 0);
        when(instanceBootGroupMemberDao.searchAndCountByBootGroupId(GROUP_ID))
                .thenReturn(new com.cloud.utils.Pair<>(new ArrayList<>(Collections.singletonList(member)), 1));
        when(userVmDao.findById(VM_ID)).thenReturn(newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running));
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessRuleService.findInheritedGroupRules(GROUP_ID, VM_ID)).thenReturn(Collections.emptyList());

        ListResponse<InstanceBootGroupMemberResponse> response = service.listInstanceBootGroupMembers(cmd);

        InstanceBootGroupMemberResponse memberResponse = response.getResponses().get(0);
        assertEquals("Ready", field(memberResponse, "readinessStatus"));
        assertEquals("None", field(memberResponse, "readinessMode"));
        assertTrue(field(memberResponse, "readinessMessage").contains("No readiness rules attached"));
    }

    @Test
    public void testListInstanceBootGroupMembersNoRulesStoppedVmIsNotReady() {
        ListInstanceBootGroupMembersCmd cmd = baseListMembersCmd(true, false, false);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        InstanceBootGroupMemberVO member = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 0);
        when(instanceBootGroupMemberDao.searchAndCountByBootGroupId(GROUP_ID))
                .thenReturn(new com.cloud.utils.Pair<>(new ArrayList<>(Collections.singletonList(member)), 1));
        when(userVmDao.findById(VM_ID)).thenReturn(newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Stopped));
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessRuleService.findInheritedGroupRules(GROUP_ID, VM_ID)).thenReturn(Collections.emptyList());

        ListResponse<InstanceBootGroupMemberResponse> response = service.listInstanceBootGroupMembers(cmd);

        InstanceBootGroupMemberResponse memberResponse = response.getResponses().get(0);
        assertEquals("NotReady", field(memberResponse, "readinessStatus"));
    }

    @Test
    public void testListInstanceBootGroupMembersWithRulesStoppedVmForcedNotReadyRegardlessOfCache() {
        ListInstanceBootGroupMembersCmd cmd = baseListMembersCmd(true, false, false);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        InstanceBootGroupMemberVO member = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 0);
        when(instanceBootGroupMemberDao.searchAndCountByBootGroupId(GROUP_ID))
                .thenReturn(new com.cloud.utils.Pair<>(new ArrayList<>(Collections.singletonList(member)), 1));
        when(userVmDao.findById(VM_ID)).thenReturn(newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Stopped));
        InstanceBootGroupReadinessRuleVO rule = newRule(RULE_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID,
                InstanceBootGroupReadinessRule.RuleType.Ping, true, "ping-rule");
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.singletonList(rule));
        when(instanceBootGroupReadinessRuleService.findInheritedGroupRules(GROUP_ID, VM_ID)).thenReturn(Collections.emptyList());

        ListResponse<InstanceBootGroupMemberResponse> response = service.listInstanceBootGroupMembers(cmd);

        InstanceBootGroupMemberResponse memberResponse = response.getResponses().get(0);
        assertEquals("NotReady", field(memberResponse, "readinessStatus"));
        assertTrue(field(memberResponse, "readinessMessage").contains("Instance state is Stopped"));
        verify(instanceBootGroupReadinessCheckResultDao, never()).findByRuleAndVm(RULE_ID, 0L);
    }

    @Test
    public void testListInstanceBootGroupMembersIgnoreVmStateUsesCachedRuleResult() {
        ListInstanceBootGroupMembersCmd cmd = baseListMembersCmd(true, false, true);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        InstanceBootGroupMemberVO member = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID, 0);
        when(instanceBootGroupMemberDao.searchAndCountByBootGroupId(GROUP_ID))
                .thenReturn(new com.cloud.utils.Pair<>(new ArrayList<>(Collections.singletonList(member)), 1));
        when(userVmDao.findById(VM_ID)).thenReturn(newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Stopped));
        InstanceBootGroupReadinessRuleVO rule = newRule(RULE_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID,
                InstanceBootGroupReadinessRule.RuleType.Ping, true, "ping-rule");
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.singletonList(rule));
        when(instanceBootGroupReadinessRuleService.findInheritedGroupRules(GROUP_ID, VM_ID)).thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(RULE_ID, 0L))
                .thenReturn(new InstanceBootGroupReadinessCheckResultVO(RULE_ID, 0L, InstanceBootGroupReadinessRule.Status.NotReady, "timeout", new Date()));

        ListResponse<InstanceBootGroupMemberResponse> response = service.listInstanceBootGroupMembers(cmd);

        InstanceBootGroupMemberResponse memberResponse = response.getResponses().get(0);
        assertEquals("NotReady", field(memberResponse, "readinessStatus"));
        assertEquals("Ping: timeout", field(memberResponse, "readinessMessage"));
        assertEquals("RuleBased", field(memberResponse, "readinessMode"));
    }

    @Test
    public void testListInstanceBootGroupMembersInstanceGroupMemberQuorumExcludesOwnStatusFromAggregate() {
        ListInstanceBootGroupMembersCmd cmd = baseListMembersCmd(true, true, false);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        InstanceBootGroupMemberVO member = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 0);
        when(instanceBootGroupMemberDao.searchAndCountByBootGroupId(GROUP_ID))
                .thenReturn(new com.cloud.utils.Pair<>(new ArrayList<>(Collections.singletonList(member)), 1));
        InstanceGroupVO instanceGroup = newInstanceGroup(INSTANCE_GROUP_ID, "ig1", ACCOUNT_ID);
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(instanceGroup);

        InstanceGroupVMMapVO map1 = new InstanceGroupVMMapVO(INSTANCE_GROUP_ID, VM_ID);
        InstanceGroupVMMapVO map2 = new InstanceGroupVMMapVO(INSTANCE_GROUP_ID, VM2_ID);
        when(instanceGroupVMMapDao.listByGroupId(INSTANCE_GROUP_ID)).thenReturn(Arrays.asList(map1, map2));
        when(userVmDao.listByIds(any())).thenReturn(Arrays.asList(
                newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running),
                newVm(VM2_ID, "vm2", "vm2host", ACCOUNT_ID, VirtualMachine.State.Stopped)));
        when(userVmDao.findById(VM_ID)).thenReturn(newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running));
        when(userVmDao.findById(VM2_ID)).thenReturn(newVm(VM2_ID, "vm2", "vm2host", ACCOUNT_ID, VirtualMachine.State.Stopped));

        InstanceBootGroupReadinessRuleVO quorumRule = newRule(RULE_ID, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID,
                InstanceBootGroupReadinessRule.RuleType.MemberQuorum, true, "quorum-rule");
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID))
                .thenReturn(Collections.singletonList(quorumRule));
        when(instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(RULE_ID, 0L))
                .thenReturn(new InstanceBootGroupReadinessCheckResultVO(RULE_ID, 0L, InstanceBootGroupReadinessRule.Status.Ready, "quorum met", new Date()));
        // VM children have no direct rules of their own
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM2_ID))
                .thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessRuleService.findInheritedGroupRules(eq(GROUP_ID), anyLong())).thenReturn(Collections.emptyList());

        ListResponse<InstanceBootGroupMemberResponse> response = service.listInstanceBootGroupMembers(cmd);

        InstanceBootGroupMemberResponse memberResponse = response.getResponses().get(0);
        // Own MemberQuorum result is Ready, so the group is Ready even though one child VM is NotReady.
        assertEquals("Ready", field(memberResponse, "readinessStatus"));
        assertEquals("1 of 2 member VM(s) not ready", field(memberResponse, "readinessMessage"));
        assertEquals("RuleBased", field(memberResponse, "readinessMode"));
    }

    @Test
    public void testListInstanceBootGroupMembersInstanceGroupWithoutQuorumRequiresAllChildrenReady() {
        ListInstanceBootGroupMembersCmd cmd = baseListMembersCmd(true, true, false);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        InstanceBootGroupMemberVO member = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 0);
        when(instanceBootGroupMemberDao.searchAndCountByBootGroupId(GROUP_ID))
                .thenReturn(new com.cloud.utils.Pair<>(new ArrayList<>(Collections.singletonList(member)), 1));
        InstanceGroupVO instanceGroup = newInstanceGroup(INSTANCE_GROUP_ID, "ig1", ACCOUNT_ID);
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(instanceGroup);

        InstanceGroupVMMapVO map1 = new InstanceGroupVMMapVO(INSTANCE_GROUP_ID, VM_ID);
        InstanceGroupVMMapVO map2 = new InstanceGroupVMMapVO(INSTANCE_GROUP_ID, VM2_ID);
        when(instanceGroupVMMapDao.listByGroupId(INSTANCE_GROUP_ID)).thenReturn(Arrays.asList(map1, map2));
        UserVmVO runningVm = newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running);
        UserVmVO stoppedVm = newVm(VM2_ID, "vm2", "vm2host", ACCOUNT_ID, VirtualMachine.State.Stopped);
        when(userVmDao.listByIds(any())).thenReturn(Arrays.asList(runningVm, stoppedVm));
        when(userVmDao.findById(VM_ID)).thenReturn(runningVm);
        when(userVmDao.findById(VM2_ID)).thenReturn(stoppedVm);

        // No group-scope rules attached -> ChildDependent mode, all children must be Ready.
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID))
                .thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM2_ID))
                .thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessRuleService.findInheritedGroupRules(eq(GROUP_ID), anyLong())).thenReturn(Collections.emptyList());

        ListResponse<InstanceBootGroupMemberResponse> response = service.listInstanceBootGroupMembers(cmd);

        InstanceBootGroupMemberResponse memberResponse = response.getResponses().get(0);
        assertEquals("NotReady", field(memberResponse, "readinessStatus"));
        assertEquals("1 of 2 member VM(s) not ready", field(memberResponse, "readinessMessage"));
        assertEquals("ChildDependent", field(memberResponse, "readinessMode"));

        @SuppressWarnings("unchecked")
        List<InstanceBootGroupMemberChildResponse> children =
                (List<InstanceBootGroupMemberChildResponse>) ReflectionTestUtils.getField(memberResponse, "children");
        assertNotNull(children);
        assertEquals(2, children.size());
    }

    // ---------------------------------------------------------------- start/stop/reboot

    @Test
    public void testStartInstanceBootGroupDelegatesToManager() {
        StartInstanceBootGroupCmd cmd = mock(StartInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        InstanceBootGroup result = service.startInstanceBootGroup(cmd);

        assertEquals(group, result);
        verify(instanceBootGroupManager).startInstanceBootGroup(group);
    }

    @Test
    public void testStopInstanceBootGroupDelegatesToManager() {
        StopInstanceBootGroupCmd cmd = mock(StopInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        InstanceBootGroup result = service.stopInstanceBootGroup(cmd);

        assertEquals(group, result);
        verify(instanceBootGroupManager).stopInstanceBootGroup(group);
    }

    @Test
    public void testRebootInstanceBootGroupDelegatesToManager() {
        RebootInstanceBootGroupCmd cmd = mock(RebootInstanceBootGroupCmd.class);
        when(cmd.getId()).thenReturn(GROUP_ID);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        InstanceBootGroup result = service.rebootInstanceBootGroup(cmd);

        assertEquals(group, result);
        verify(instanceBootGroupManager).rebootInstanceBootGroup(group);
    }

    // ---------------------------------------------------------------- readiness rule create/update/delete

    @Test
    public void testCreateInstanceBootGroupReadinessRuleSuccess() {
        CreateInstanceBootGroupReadinessRuleCmd cmd = mock(CreateInstanceBootGroupReadinessRuleCmd.class);
        when(cmd.getBootGroupId()).thenReturn(GROUP_ID);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(null);
        when(cmd.getRuleType()).thenReturn("Ping");
        when(cmd.getName()).thenReturn("rule1");
        when(cmd.isEnabled()).thenReturn(true);
        when(cmd.getDetails()).thenReturn(null);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        UserVmVO vm = newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);
        InstanceBootGroupReadinessRule created = mock(InstanceBootGroupReadinessRule.class);
        when(instanceBootGroupReadinessRuleService.createReadinessRule(GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID,
                InstanceBootGroupReadinessRule.RuleType.Ping, "rule1", true, null)).thenReturn(created);

        InstanceBootGroupReadinessRule result = service.createInstanceBootGroupReadinessRule(cmd);

        assertEquals(created, result);
        verify(accountManager).checkAccess(callerMock, null, true, vm);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateInstanceBootGroupReadinessRuleInvalidRuleTypeThrows() {
        CreateInstanceBootGroupReadinessRuleCmd cmd = mock(CreateInstanceBootGroupReadinessRuleCmd.class);
        when(cmd.getBootGroupId()).thenReturn(GROUP_ID);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(null);
        when(cmd.getRuleType()).thenReturn("NotARealType");
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        UserVmVO vm = newVm(VM_ID, "vm1", "vm1host", ACCOUNT_ID, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);

        service.createInstanceBootGroupReadinessRule(cmd);
    }

    @Test
    public void testUpdateInstanceBootGroupReadinessRuleSuccess() {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = mock(UpdateInstanceBootGroupReadinessRuleCmd.class);
        when(cmd.getId()).thenReturn(RULE_ID);
        when(cmd.getName()).thenReturn("newname");
        when(cmd.getEnabled()).thenReturn(false);
        when(cmd.getDetails()).thenReturn(null);
        InstanceBootGroupReadinessRuleVO rule = newRule(RULE_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID,
                InstanceBootGroupReadinessRule.RuleType.Ping, true, "oldname");
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(rule);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        InstanceBootGroupReadinessRule updated = mock(InstanceBootGroupReadinessRule.class);
        when(instanceBootGroupReadinessRuleService.updateReadinessRule(RULE_ID, "newname", false, null)).thenReturn(updated);

        InstanceBootGroupReadinessRule result = service.updateInstanceBootGroupReadinessRule(cmd);

        assertEquals(updated, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateInstanceBootGroupReadinessRuleNotFoundThrows() {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = mock(UpdateInstanceBootGroupReadinessRuleCmd.class);
        when(cmd.getId()).thenReturn(RULE_ID);
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(null);

        service.updateInstanceBootGroupReadinessRule(cmd);
    }

    @Test
    public void testDeleteInstanceBootGroupReadinessRuleSuccess() {
        DeleteInstanceBootGroupReadinessRuleCmd cmd = mock(DeleteInstanceBootGroupReadinessRuleCmd.class);
        when(cmd.getId()).thenReturn(RULE_ID);
        InstanceBootGroupReadinessRuleVO rule = newRule(RULE_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID,
                InstanceBootGroupReadinessRule.RuleType.Ping, true, "rule1");
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(rule);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);
        when(instanceBootGroupReadinessRuleService.deleteReadinessRule(RULE_ID)).thenReturn(true);

        boolean result = service.deleteInstanceBootGroupReadinessRule(cmd);

        assertTrue(result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteInstanceBootGroupReadinessRuleNotFoundThrows() {
        DeleteInstanceBootGroupReadinessRuleCmd cmd = mock(DeleteInstanceBootGroupReadinessRuleCmd.class);
        when(cmd.getId()).thenReturn(RULE_ID);
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(null);

        service.deleteInstanceBootGroupReadinessRule(cmd);
    }

    // ---------------------------------------------------------------- listInstanceBootGroupReadinessRules

    @Test
    public void testListInstanceBootGroupReadinessRulesSurfacesInheritedRulesForVm() {
        ListInstanceBootGroupReadinessRulesCmd cmd = mock(ListInstanceBootGroupReadinessRulesCmd.class);
        when(cmd.getBootGroupId()).thenReturn(GROUP_ID);
        when(cmd.getId()).thenReturn(null);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(null);
        when(cmd.getRuleType()).thenReturn(null);
        when(cmd.getKeyword()).thenReturn(null);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        InstanceBootGroupReadinessRuleVO directRule = newRule(RULE_ID, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID,
                InstanceBootGroupReadinessRule.RuleType.GuestAgentLiveness, true, "direct-rule");
        when(instanceBootGroupReadinessRuleDao.searchAndCountByBootGroupId(eq(GROUP_ID), eq((Long) null),
                eq(InstanceBootGroupMember.MemberType.VirtualMachine), eq(VM_ID), eq((InstanceBootGroupReadinessRule.RuleType) null),
                any(), any(), any()))
                .thenReturn(new com.cloud.utils.Pair<>(Collections.singletonList(directRule), 1));

        InstanceBootGroupReadinessRuleVO inheritedRule = newRule(RULE_ID + 1, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID,
                InstanceBootGroupReadinessRule.RuleType.Ping, true, "inherited-rule");
        when(instanceBootGroupReadinessRuleService.findInheritedGroupRules(GROUP_ID, VM_ID)).thenReturn(Collections.singletonList(inheritedRule));

        ListResponse<InstanceBootGroupReadinessRuleResponse> response = service.listInstanceBootGroupReadinessRules(cmd);

        assertEquals(2, response.getResponses().size());
        assertEquals(Integer.valueOf(2), response.getCount());
        InstanceBootGroupReadinessRuleResponse directResponse = response.getResponses().get(0);
        InstanceBootGroupReadinessRuleResponse inheritedResponse = response.getResponses().get(1);
        assertEquals("false", field(directResponse, "inherited"));
        assertEquals("true", field(inheritedResponse, "inherited"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListInstanceBootGroupReadinessRulesBothIdsSpecifiedThrows() {
        ListInstanceBootGroupReadinessRulesCmd cmd = mock(ListInstanceBootGroupReadinessRulesCmd.class);
        when(cmd.getBootGroupId()).thenReturn(GROUP_ID);
        when(cmd.getVirtualMachineId()).thenReturn(VM_ID);
        when(cmd.getInstanceGroupId()).thenReturn(INSTANCE_GROUP_ID);
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        service.listInstanceBootGroupReadinessRules(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListInstanceBootGroupReadinessRulesInvalidRuleTypeThrows() {
        ListInstanceBootGroupReadinessRulesCmd cmd = mock(ListInstanceBootGroupReadinessRulesCmd.class);
        when(cmd.getBootGroupId()).thenReturn(GROUP_ID);
        when(cmd.getVirtualMachineId()).thenReturn(null);
        when(cmd.getRuleType()).thenReturn("Bogus");
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1", ACCOUNT_ID);
        when(instanceBootGroupDao.findById(GROUP_ID)).thenReturn(group);

        service.listInstanceBootGroupReadinessRules(cmd);
    }
}
