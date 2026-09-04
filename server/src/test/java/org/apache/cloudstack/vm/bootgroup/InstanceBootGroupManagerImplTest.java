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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRuleService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.InstanceBootGroupDetailsDao;
import com.cloud.vm.dao.InstanceBootGroupMemberDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupManagerImplTest {

    private static final long ACCOUNT_ID = 1L;
    private static final long DOMAIN_ID = 1L;
    private static final long GROUP_ID = 500L;
    private static final long MEMBER_ID_1 = 601L;
    private static final long MEMBER_ID_2 = 602L;
    private static final long VM_ID_1 = 701L;
    private static final long VM_ID_2 = 702L;
    private static final long INSTANCE_GROUP_ID = 801L;

    @InjectMocks
    InstanceBootGroupManagerImpl manager;

    @Mock
    InstanceBootGroupMemberDao instanceBootGroupMemberDao;
    @Mock
    UserVmService userVmService;
    @Mock
    UserVmDao userVmDao;
    @Mock
    InstanceGroupDao instanceGroupDao;
    @Mock
    InstanceGroupVMMapDao instanceGroupVMMapDao;
    @Mock
    VirtualMachineManager virtualMachineManager;
    @Mock
    InstanceBootGroupReadinessRuleService instanceBootGroupReadinessRuleService;
    @Mock
    InstanceBootGroupDetailsDao instanceBootGroupDetailsDao;

    private static final long CALLER_USER_ID = 2L;
    private static final long CALLER_ACCOUNT_ID = 3L;

    private static final Class<?> VM_PROGRESS_CLASS;

    static {
        try {
            VM_PROGRESS_CLASS = Class.forName(InstanceBootGroupManagerImpl.class.getName() + "$VmProgress");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // CallContext orchestration (runTierConcurrently / waitForTierReady) runs its per-VM actions on
    // real pooled threads, so a MockedStatic<CallContext> (thread-confined) can't be used here — the
    // worker threads would fall through to the real static methods and NPE on the uninitialized
    // entity manager. Instead we back CallContext with a minimal real registration, resolvable from
    // any thread via its normal ThreadLocal-based mechanism.
    @Before
    public void setUp() {
        EntityManager entityManager = mock(EntityManager.class);
        Account callerAccount = mock(Account.class);
        when(callerAccount.getId()).thenReturn(CALLER_ACCOUNT_ID);
        User callerUser = mock(User.class);
        when(callerUser.getId()).thenReturn(CALLER_USER_ID);
        when(entityManager.findById(eq(Account.class), any(Long.class))).thenReturn(callerAccount);
        when(entityManager.findById(eq(User.class), any(Long.class))).thenReturn(callerUser);
        CallContext.init(entityManager);
        CallContext.register(callerUser, callerAccount);
    }

    @After
    public void tearDown() {
        CallContext.unregisterAll();
    }

    //
    // Helpers
    //

    private InstanceBootGroupVO newGroup(long id, String name) {
        InstanceBootGroupVO group = Mockito.spy(new InstanceBootGroupVO(name, "desc", ACCOUNT_ID, DOMAIN_ID));
        Mockito.lenient().doReturn(id).when(group).getId();
        return group;
    }

    private InstanceBootGroupMemberVO newMember(long id, long bootGroupId, InstanceBootGroupMember.MemberType type, long memberId, int order) {
        InstanceBootGroupMemberVO member = Mockito.spy(new InstanceBootGroupMemberVO(bootGroupId, type, memberId, order));
        Mockito.lenient().doReturn(id).when(member).getId();
        return member;
    }

    private Object invokePrivate(String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = InstanceBootGroupManagerImpl.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        try {
            return m.invoke(manager, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private Object newVmProgress(long vmId, Long memberId) throws Exception {
        Constructor<?> ctor = VM_PROGRESS_CLASS.getDeclaredConstructor(long.class, Long.class);
        ctor.setAccessible(true);
        return ctor.newInstance(vmId, memberId);
    }

    private void setProgressField(Object progress, String field, Object value) throws Exception {
        Field f = VM_PROGRESS_CLASS.getDeclaredField(field);
        f.setAccessible(true);
        f.set(progress, value);
    }

    private Object getProgressField(Object progress, String field) throws Exception {
        Field f = VM_PROGRESS_CLASS.getDeclaredField(field);
        f.setAccessible(true);
        return f.get(progress);
    }

    //
    // effective* config-override resolution
    //

    @Test
    public void testEffectiveTimeoutSecondsUsesOverrideWhenPresent() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        when(instanceBootGroupDetailsDao.getDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessAttemptTimeoutSeconds.key()))
                .thenReturn("120");
        long result = (Long) invokePrivate("effectiveTimeoutSeconds", new Class<?>[]{InstanceBootGroupVO.class}, group);
        assertEquals(120L, result);
    }

    @Test
    public void testEffectiveTimeoutSecondsFallsBackToConfigDefault() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        when(instanceBootGroupDetailsDao.getDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessAttemptTimeoutSeconds.key()))
                .thenReturn(null);
        long result = (Long) invokePrivate("effectiveTimeoutSeconds", new Class<?>[]{InstanceBootGroupVO.class}, group);
        assertEquals(300L, result);
    }

    @Test
    public void testEffectiveMaxRetryAttemptsUsesOverrideWhenPresent() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        when(instanceBootGroupDetailsDao.getDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessMaxRetryAttempts.key()))
                .thenReturn("2");
        long result = (Long) invokePrivate("effectiveMaxRetryAttempts", new Class<?>[]{InstanceBootGroupVO.class}, group);
        assertEquals(2L, result);
    }

    @Test
    public void testEffectiveMaxRetryAttemptsFallsBackToConfigDefault() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        long result = (Long) invokePrivate("effectiveMaxRetryAttempts", new Class<?>[]{InstanceBootGroupVO.class}, group);
        assertEquals(5L, result);
    }

    @Test
    public void testEffectiveInitialDelaySecondsUsesOverrideWhenPresent() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        when(instanceBootGroupDetailsDao.getDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key()))
                .thenReturn("5");
        long result = (Long) invokePrivate("effectiveInitialDelaySeconds", new Class<?>[]{InstanceBootGroupVO.class}, group);
        assertEquals(5L, result);
    }

    @Test
    public void testEffectiveInitialDelaySecondsFallsBackToConfigDefault() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        long result = (Long) invokePrivate("effectiveInitialDelaySeconds", new Class<?>[]{InstanceBootGroupVO.class}, group);
        assertEquals(30L, result);
    }

    @Test
    public void testEffectiveRebootOnRetryUsesOverrideWhenPresent() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        when(instanceBootGroupDetailsDao.getDetail(GROUP_ID, InstanceBootGroupManagerImpl.ReadinessRebootOnRetry.key()))
                .thenReturn("true");
        boolean result = (Boolean) invokePrivate("effectiveRebootOnRetry", new Class<?>[]{InstanceBootGroupVO.class}, group);
        assertTrue(result);
    }

    @Test
    public void testEffectiveRebootOnRetryFallsBackToConfigDefault() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        boolean result = (Boolean) invokePrivate("effectiveRebootOnRetry", new Class<?>[]{InstanceBootGroupVO.class}, group);
        assertFalse(result);
    }

    @Test
    public void testEffectivePollIntervalSecondsIsGlobalOnly() throws Exception {
        long result = (Long) invokePrivate("effectivePollIntervalSeconds", new Class<?>[]{});
        assertEquals(10L, result);
        verifyNoInteractions(instanceBootGroupDetailsDao);
    }

    @Test
    public void testEffectiveReadinessCheckConcurrencyIsGlobalOnly() throws Exception {
        long result = (Long) invokePrivate("effectiveReadinessCheckConcurrency", new Class<?>[]{});
        assertEquals(10L, result);
        verifyNoInteractions(instanceBootGroupDetailsDao);
    }

    //
    // Tier grouping
    //

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupByOrderGroupsAndSortsAscending() throws Exception {
        InstanceBootGroupMemberVO m3 = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 901L, 3);
        InstanceBootGroupMemberVO m1a = newMember(2L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 902L, 1);
        InstanceBootGroupMemberVO m2 = newMember(3L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 903L, 2);
        InstanceBootGroupMemberVO m1b = newMember(4L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 904L, 1);
        List<InstanceBootGroupMemberVO> members = List.of(m3, m1a, m2, m1b);

        Map<Integer, List<InstanceBootGroupMemberVO>> tiers = (Map<Integer, List<InstanceBootGroupMemberVO>>)
                invokePrivate("groupByOrder", new Class<?>[]{List.class}, members);

        assertEquals(List.of(1, 2, 3), new ArrayList<>(tiers.keySet()));
        assertEquals(2, tiers.get(1).size());
        assertEquals(1, tiers.get(2).size());
        assertEquals(1, tiers.get(3).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupByOrderDescendingSortsDescending() throws Exception {
        InstanceBootGroupMemberVO m3 = newMember(1L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 901L, 3);
        InstanceBootGroupMemberVO m1 = newMember(2L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 902L, 1);
        InstanceBootGroupMemberVO m2 = newMember(3L, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, 903L, 2);
        List<InstanceBootGroupMemberVO> members = List.of(m3, m1, m2);

        Map<Integer, List<InstanceBootGroupMemberVO>> tiers = (Map<Integer, List<InstanceBootGroupMemberVO>>)
                invokePrivate("groupByOrderDescending", new Class<?>[]{List.class}, members);

        assertEquals(List.of(3, 2, 1), new ArrayList<>(tiers.keySet()));
    }

    //
    // resolveVmIds
    //

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveVmIdsDirectVmMember() throws Exception {
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_1, 1);
        List<Long> ids = (List<Long>) invokePrivate("resolveVmIds", new Class<?>[]{List.class}, List.of(member));
        assertEquals(List.of(VM_ID_1), ids);
        verifyNoInteractions(instanceGroupVMMapDao);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveVmIdsInstanceGroupMemberExpandsToGroupVms() throws Exception {
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 1);
        InstanceGroupVMMapVO map1 = new InstanceGroupVMMapVO(INSTANCE_GROUP_ID, VM_ID_1);
        InstanceGroupVMMapVO map2 = new InstanceGroupVMMapVO(INSTANCE_GROUP_ID, VM_ID_2);
        when(instanceGroupVMMapDao.listByGroupId(INSTANCE_GROUP_ID)).thenReturn(List.of(map1, map2));

        List<Long> ids = (List<Long>) invokePrivate("resolveVmIds", new Class<?>[]{List.class}, List.of(member));
        assertEquals(List.of(VM_ID_1, VM_ID_2), ids);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveVmIdsMixedMembers() throws Exception {
        InstanceBootGroupMemberVO vmMember = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_1, 1);
        InstanceBootGroupMemberVO groupMember = newMember(MEMBER_ID_2, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 2);
        InstanceGroupVMMapVO map1 = new InstanceGroupVMMapVO(INSTANCE_GROUP_ID, VM_ID_2);
        when(instanceGroupVMMapDao.listByGroupId(INSTANCE_GROUP_ID)).thenReturn(List.of(map1));

        List<Long> ids = (List<Long>) invokePrivate("resolveVmIds", new Class<?>[]{List.class}, List.of(vmMember, groupMember));
        assertEquals(List.of(VM_ID_1, VM_ID_2), ids);
    }

    //
    // anchorInitialDelay
    //

    @Test
    public void testAnchorInitialDelayNotAlreadyRunningAnchorsToNow() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        Object progress = newVmProgress(VM_ID_1, MEMBER_ID_1);
        UserVmVO vm = mock(UserVmVO.class);

        long before = System.currentTimeMillis();
        invokePrivate("anchorInitialDelay", new Class<?>[]{InstanceBootGroupVO.class, VM_PROGRESS_CLASS, UserVmVO.class, boolean.class},
                group, progress, vm, false);
        long after = System.currentTimeMillis();

        long entered = (Long) getProgressField(progress, "enteredWaitAtMs");
        long lastBooted = (Long) getProgressField(progress, "lastBootedAtMs");
        assertTrue(entered >= before && entered <= after);
        assertEquals(entered, lastBooted);
    }

    @Test
    public void testAnchorInitialDelayAlreadyRunningUsesPowerStateUpdateTime() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        Object progress = newVmProgress(VM_ID_1, MEMBER_ID_1);
        UserVmVO vm = mock(UserVmVO.class);
        Date powerStateTime = new Date(System.currentTimeMillis() - 5000);
        when(vm.getPowerStateUpdateTime()).thenReturn(powerStateTime);

        invokePrivate("anchorInitialDelay", new Class<?>[]{InstanceBootGroupVO.class, VM_PROGRESS_CLASS, UserVmVO.class, boolean.class},
                group, progress, vm, true);

        long lastBooted = (Long) getProgressField(progress, "lastBootedAtMs");
        assertEquals(powerStateTime.getTime(), lastBooted);
    }

    @Test
    public void testAnchorInitialDelayAlreadyRunningWithNullPowerStateFallsBackToNow() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        Object progress = newVmProgress(VM_ID_1, MEMBER_ID_1);
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getPowerStateUpdateTime()).thenReturn(null);

        long before = System.currentTimeMillis();
        invokePrivate("anchorInitialDelay", new Class<?>[]{InstanceBootGroupVO.class, VM_PROGRESS_CLASS, UserVmVO.class, boolean.class},
                group, progress, vm, true);
        long after = System.currentTimeMillis();

        long lastBooted = (Long) getProgressField(progress, "lastBootedAtMs");
        assertTrue(lastBooted >= before && lastBooted <= after);
    }

    //
    // halt
    //

    @Test
    public void testHaltDoesNotThrow() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        invokePrivate("halt", new Class<?>[]{InstanceBootGroupVO.class, String.class}, group, "some reason");
    }

    //
    // checkInstanceGroupMembersReady
    //

    private static final Class<?>[] CHECK_GROUP_MEMBERS_READY_PARAMS =
            new Class<?>[]{InstanceBootGroupVO.class, List.class, Map.class, Map.class};

    @Test
    public void testCheckInstanceGroupMembersReadyEmptyTierMembersIsVacuouslySettled() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        Map<Long, Object> progressByVmId = new HashMap<>();
        Map<Long, Boolean> membersReadyStatus = new HashMap<>();

        invokePrivate("checkInstanceGroupMembersReady", CHECK_GROUP_MEMBERS_READY_PARAMS,
                group, Collections.emptyList(), progressByVmId, membersReadyStatus);

        verifyNoInteractions(instanceGroupDao);
        verifyNoInteractions(instanceBootGroupReadinessRuleService);
        assertTrue(membersReadyStatus.isEmpty());
    }

    @Test
    public void testCheckInstanceGroupMembersReadySkipsAlreadyReadyMember() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO groupMember = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 1);
        Map<Long, Object> progressByVmId = new HashMap<>();
        Map<Long, Boolean> membersReadyStatus = new HashMap<>();
        membersReadyStatus.put(MEMBER_ID_1, true);

        invokePrivate("checkInstanceGroupMembersReady", CHECK_GROUP_MEMBERS_READY_PARAMS,
                group, List.of(groupMember), progressByVmId, membersReadyStatus);

        verifyNoInteractions(instanceGroupDao);
        verifyNoInteractions(instanceBootGroupReadinessRuleService);
    }

    @Test
    public void testCheckInstanceGroupMembersReadySkipsWhenMembersNotSettledYet() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO groupMember = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 1);
        Object progress = newVmProgress(VM_ID_1, MEMBER_ID_1); // ready=false, gaveUp=false

        Map<Long, Object> progressByVmId = new HashMap<>();
        progressByVmId.put(VM_ID_1, progress);
        Map<Long, Boolean> membersReadyStatus = new HashMap<>();
        membersReadyStatus.put(MEMBER_ID_1, false);
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(mock(InstanceGroupVO.class));

        invokePrivate("checkInstanceGroupMembersReady", CHECK_GROUP_MEMBERS_READY_PARAMS,
                group, List.of(groupMember), progressByVmId, membersReadyStatus);

        verifyNoInteractions(instanceBootGroupReadinessRuleService);
        assertFalse(membersReadyStatus.get(MEMBER_ID_1));
    }

    @Test
    public void testCheckInstanceGroupMembersReadyEvaluatesImmediatelyWhenNoMatchingVmProgress() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO groupMember = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 1);
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(mock(InstanceGroupVO.class));
        when(instanceBootGroupReadinessRuleService.evaluateInstanceGroupReadiness(GROUP_ID, INSTANCE_GROUP_ID, Collections.emptySet()))
                .thenReturn(InstanceBootGroupReadinessRule.Status.Ready);

        Map<Long, Object> progressByVmId = new HashMap<>(); // empty: no progress references this member
        Map<Long, Boolean> membersReadyStatus = new HashMap<>();
        membersReadyStatus.put(MEMBER_ID_1, false);

        invokePrivate("checkInstanceGroupMembersReady", CHECK_GROUP_MEMBERS_READY_PARAMS,
                group, List.of(groupMember), progressByVmId, membersReadyStatus);

        assertTrue(membersReadyStatus.get(MEMBER_ID_1));
        verify(instanceBootGroupReadinessRuleService).evaluateInstanceGroupReadiness(GROUP_ID, INSTANCE_GROUP_ID, Collections.emptySet());
    }

    @Test
    public void testCheckInstanceGroupMembersReadyPassesGaveUpVmIdsAsPermanentlyFailedAndMarksReady() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO groupMember = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 1);
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(mock(InstanceGroupVO.class));

        Object progress1 = newVmProgress(VM_ID_1, MEMBER_ID_1);
        setProgressField(progress1, "gaveUp", true);
        Object progress2 = newVmProgress(VM_ID_2, MEMBER_ID_1);
        setProgressField(progress2, "ready", true);

        Map<Long, Object> progressByVmId = new HashMap<>();
        progressByVmId.put(VM_ID_1, progress1);
        progressByVmId.put(VM_ID_2, progress2);
        Map<Long, Boolean> membersReadyStatus = new HashMap<>();
        membersReadyStatus.put(MEMBER_ID_1, false);

        when(instanceBootGroupReadinessRuleService.evaluateInstanceGroupReadiness(GROUP_ID, INSTANCE_GROUP_ID, Set.of(VM_ID_1)))
                .thenReturn(InstanceBootGroupReadinessRule.Status.Ready);

        invokePrivate("checkInstanceGroupMembersReady", CHECK_GROUP_MEMBERS_READY_PARAMS,
                group, List.of(groupMember), progressByVmId, membersReadyStatus);

        assertTrue(membersReadyStatus.get(MEMBER_ID_1));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCheckInstanceGroupMembersReadyHaltsAndThrowsOnErrorStatus() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO groupMember = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 1);
        InstanceGroupVO ig = mock(InstanceGroupVO.class);
        when(ig.getName()).thenReturn("grp");
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(ig);
        when(instanceBootGroupReadinessRuleService.evaluateInstanceGroupReadiness(eq(GROUP_ID), eq(INSTANCE_GROUP_ID), any()))
                .thenReturn(InstanceBootGroupReadinessRule.Status.Error);

        Map<Long, Object> progressByVmId = new HashMap<>();
        Map<Long, Boolean> membersReadyStatus = new HashMap<>();
        membersReadyStatus.put(MEMBER_ID_1, false);

        invokePrivate("checkInstanceGroupMembersReady", CHECK_GROUP_MEMBERS_READY_PARAMS,
                group, List.of(groupMember), progressByVmId, membersReadyStatus);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCheckInstanceGroupMembersReadyHaltsAndThrowsWhenNotReadyAndAllMembersSettled() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO groupMember = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, INSTANCE_GROUP_ID, 1);
        InstanceGroupVO ig = mock(InstanceGroupVO.class);
        when(ig.getName()).thenReturn("grp");
        when(instanceGroupDao.findById(INSTANCE_GROUP_ID)).thenReturn(ig);

        Object progress1 = newVmProgress(VM_ID_1, MEMBER_ID_1);
        setProgressField(progress1, "ready", true);
        Map<Long, Object> progressByVmId = new HashMap<>();
        progressByVmId.put(VM_ID_1, progress1);
        Map<Long, Boolean> membersReadyStatus = new HashMap<>();
        membersReadyStatus.put(MEMBER_ID_1, false);

        when(instanceBootGroupReadinessRuleService.evaluateInstanceGroupReadiness(eq(GROUP_ID), eq(INSTANCE_GROUP_ID), any()))
                .thenReturn(InstanceBootGroupReadinessRule.Status.NotReady);

        invokePrivate("checkInstanceGroupMembersReady", CHECK_GROUP_MEMBERS_READY_PARAMS,
                group, List.of(groupMember), progressByVmId, membersReadyStatus);
    }

    //
    // startInstanceBootGroup / stopInstanceBootGroup / rebootInstanceBootGroup — small fast end-to-end flows
    //

    @Test
    public void testStartInstanceBootGroupSingleTierSingleVmReadyImmediately() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_1, 1);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(List.of(member));

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(userVmDao.findById(VM_ID_1)).thenReturn(vm);

        when(instanceBootGroupDetailsDao.getDetail(eq(GROUP_ID), eq(InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key())))
                .thenReturn("0");
        when(instanceBootGroupReadinessRuleService.evaluateVmReadiness(eq(GROUP_ID), eq(VM_ID_1), anyLong(), anyString()))
                .thenReturn(InstanceBootGroupReadinessRule.Status.Ready);

        manager.startInstanceBootGroup(group);

        verify(userVmService).startVirtualMachine(vm, null);
        verify(instanceBootGroupReadinessRuleService, Mockito.atLeastOnce())
                .evaluateVmReadiness(eq(GROUP_ID), eq(VM_ID_1), anyLong(), anyString());
    }

    @Test
    public void testStartInstanceBootGroupAlreadyRunningVmSkipsStartCall() throws Exception {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_1, 1);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(List.of(member));

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(vm.getPowerStateUpdateTime()).thenReturn(new Date(System.currentTimeMillis() - 1000));
        when(userVmDao.findById(VM_ID_1)).thenReturn(vm);

        when(instanceBootGroupDetailsDao.getDetail(eq(GROUP_ID), eq(InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key())))
                .thenReturn("0");
        when(instanceBootGroupReadinessRuleService.evaluateVmReadiness(eq(GROUP_ID), eq(VM_ID_1), anyLong(), anyString()))
                .thenReturn(InstanceBootGroupReadinessRule.Status.Ready);

        manager.startInstanceBootGroup(group);

        verify(userVmService, never()).startVirtualMachine(any(), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testStartInstanceBootGroupHaltsOnExhaustedRetriesAndSkipsLaterTiers() throws Throwable {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO member1 = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_1, 1);
        InstanceBootGroupMemberVO member2 = newMember(MEMBER_ID_2, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_2, 2);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(List.of(member1, member2));

        UserVmVO vm1 = mock(UserVmVO.class);
        when(vm1.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(vm1.getName()).thenReturn("vm1");
        when(userVmDao.findById(VM_ID_1)).thenReturn(vm1);

        when(instanceBootGroupDetailsDao.getDetail(eq(GROUP_ID), eq(InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key())))
                .thenReturn("0");
        when(instanceBootGroupDetailsDao.getDetail(eq(GROUP_ID), eq(InstanceBootGroupManagerImpl.ReadinessMaxRetryAttempts.key())))
                .thenReturn("0");
        when(instanceBootGroupReadinessRuleService.evaluateVmReadiness(eq(GROUP_ID), eq(VM_ID_1), anyLong(), anyString()))
                .thenReturn(InstanceBootGroupReadinessRule.Status.NotReady);

        try {
            manager.startInstanceBootGroup(group);
        } finally {
            // Only vm1 (tier 1) is ever started; tier 2 (vm2) is never reached because tier 1 halts.
            verify(userVmService, Mockito.times(1)).startVirtualMachine(any(), any());
            verify(userVmService).startVirtualMachine(vm1, null);
            verify(userVmDao, never()).findById(VM_ID_2);
        }
    }

    @Test
    public void testStopInstanceBootGroupStopsTiersInReverseOrder() {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO member1 = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_1, 1);
        InstanceBootGroupMemberVO member2 = newMember(MEMBER_ID_2, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_2, 2);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(List.of(member1, member2));

        UserVmVO vm1 = mock(UserVmVO.class);
        when(vm1.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID_1)).thenReturn(vm1);

        UserVmVO vm2 = mock(UserVmVO.class);
        when(vm2.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID_2)).thenReturn(vm2);

        manager.stopInstanceBootGroup(group, false);

        InOrder inOrder = Mockito.inOrder(userVmService);
        inOrder.verify(userVmService).stopVirtualMachine(VM_ID_2, false);
        inOrder.verify(userVmService).stopVirtualMachine(VM_ID_1, false);
    }

    @Test
    public void testStopInstanceBootGroupSkipsAlreadyStoppedVm() {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_1, 1);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(List.of(member));

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(userVmDao.findById(VM_ID_1)).thenReturn(vm);

        manager.stopInstanceBootGroup(group, false);

        verify(userVmService, never()).stopVirtualMachine(anyLong(), any(Boolean.class));
    }

    @Test
    public void testStopInstanceBootGroupPropagatesForcedFlag() {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupMemberVO member = newMember(MEMBER_ID_1, GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID_1, 1);
        when(instanceBootGroupMemberDao.listByBootGroupId(GROUP_ID)).thenReturn(List.of(member));

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID_1)).thenReturn(vm);

        manager.stopInstanceBootGroup(group, true);

        verify(userVmService).stopVirtualMachine(VM_ID_1, true);
    }

    @Test
    public void testRebootInstanceBootGroupCallsStopThenStart() {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupManagerImpl spyManager = Mockito.spy(manager);
        Mockito.doNothing().when(spyManager).stopInstanceBootGroup(group, false);
        Mockito.doNothing().when(spyManager).startInstanceBootGroup(group);

        spyManager.rebootInstanceBootGroup(group, false);

        InOrder inOrder = Mockito.inOrder(spyManager);
        inOrder.verify(spyManager).stopInstanceBootGroup(group, false);
        inOrder.verify(spyManager).startInstanceBootGroup(group);
    }

    @Test
    public void testRebootInstanceBootGroupPropagatesForcedFlagToStop() {
        InstanceBootGroupVO group = newGroup(GROUP_ID, "group1");
        InstanceBootGroupManagerImpl spyManager = Mockito.spy(manager);
        Mockito.doNothing().when(spyManager).stopInstanceBootGroup(group, true);
        Mockito.doNothing().when(spyManager).startInstanceBootGroup(group);

        spyManager.rebootInstanceBootGroup(group, true);

        verify(spyManager).stopInstanceBootGroup(group, true);
        verify(spyManager, never()).stopInstanceBootGroup(group, false);
    }

    //
    // Configurable / ManagerBase plumbing
    //

    @Test
    public void testGetConfigKeysReturnsAllKeys() {
        ConfigKey<?>[] keys = manager.getConfigKeys();
        assertEquals(7, keys.length);
    }

    @Test
    public void testGetConfigComponentName() {
        assertEquals("InstanceBootGroupManagerImpl", manager.getConfigComponentName());
    }

    @Test
    public void testConfigureReturnsTrue() throws Exception {
        assertTrue(manager.configure("InstanceBootGroupManagerImpl", Collections.emptyMap()));
    }
}
