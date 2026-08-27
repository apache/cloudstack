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

package org.apache.cloudstack.vm.bootgroup.readiness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember.MemberType;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMemberVO;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessCheckResultVO;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessRuleVO;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule.RuleType;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule.Status;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.InstanceBootGroupMemberDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessCheckResultDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDetailsDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupReadinessRuleManagerImplTest {

    private static final long BOOT_GROUP_ID = 1L;
    private static final long VM_ID = 100L;
    private static final long VM_ID_2 = 101L;
    private static final long GROUP_ID = 200L;
    private static final long RULE_ID = 10L;

    @InjectMocks
    InstanceBootGroupReadinessRuleManagerImpl manager;

    @Mock
    InstanceBootGroupReadinessRuleDao instanceBootGroupReadinessRuleDao;
    @Mock
    InstanceBootGroupReadinessRuleDetailsDao instanceBootGroupReadinessRuleDetailsDao;
    @Mock
    InstanceBootGroupReadinessCheckResultDao instanceBootGroupReadinessCheckResultDao;
    @Mock
    InstanceBootGroupMemberDao instanceBootGroupMemberDao;
    @Mock
    InstanceGroupVMMapDao instanceGroupVMMapDao;
    @Mock
    InstanceGroupDao instanceGroupDao;
    @Mock
    UserVmDao userVmDao;

    @Before
    public void setUp() {
        manager.setReadinessCheckers(Collections.emptyList());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private InstanceBootGroupReadinessRuleVO ruleVO(long id, long bootGroupId, MemberType itemType, long itemId, RuleType ruleType, boolean enabled) {
        InstanceBootGroupReadinessRuleVO vo = new InstanceBootGroupReadinessRuleVO("rule-" + id, bootGroupId, itemType, itemId, ruleType, enabled);
        ReflectionTestUtils.setField(vo, "id", id);
        return vo;
    }

    private InstanceBootGroupMemberVO memberVO(long bootGroupId, MemberType memberType, long memberId) {
        return new InstanceBootGroupMemberVO(bootGroupId, memberType, memberId, 0);
    }

    private UserVmVO mockVm(long id, VirtualMachine.State state, HypervisorType hypervisorType) {
        UserVmVO vm = mock(UserVmVO.class);
        Mockito.lenient().when(vm.getId()).thenReturn(id);
        Mockito.lenient().when(vm.getState()).thenReturn(state);
        Mockito.lenient().when(vm.getHypervisorType()).thenReturn(hypervisorType);
        return vm;
    }

    private ReadinessChecker mockChecker(RuleType type, Status status, String message) {
        ReadinessChecker checker = mock(ReadinessChecker.class);
        when(checker.getRuleType()).thenReturn(type);
        when(checker.check(any(), any(), anyLong(), anyLong())).thenReturn(new ReadinessChecker.Result(status, message));
        return checker;
    }

    /** Satisfies validateItemBelongsToBootGroup for a direct VirtualMachine-type item. */
    private void stubVmDirectMember(long itemId) {
        when(instanceBootGroupMemberDao.findByMember(MemberType.VirtualMachine, itemId)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.VirtualMachine, itemId));
    }

    /** Satisfies validateItemBelongsToBootGroup for a direct InstanceGroup-type item. */
    private void stubGroupDirectMember(long itemId) {
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, itemId)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.InstanceGroup, itemId));
    }

    // ==================================================================
    // createReadinessRule
    // ==================================================================

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsInvalidRuleTypeForVm() {
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.MemberQuorum, null, true, null);
    }

    @Test
    public void createReadinessRuleAcceptsMemberQuorumForInstanceGroup() {
        stubGroupDirectMember(GROUP_ID);
        Map<String, String> details = new HashMap<>();
        details.put("threshold_type", "COUNT");
        details.put("threshold_value", "2");
        when(instanceBootGroupReadinessRuleDao.persist(any())).thenReturn(ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, true));

        InstanceBootGroupReadinessRule result = manager.createReadinessRule(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, null, true, details);

        assertEquals(RULE_ID, result.getId());
        verify(instanceBootGroupReadinessRuleDetailsDao).addDetail(RULE_ID, "threshold_type", "COUNT", true);
        verify(instanceBootGroupReadinessRuleDetailsDao).addDetail(RULE_ID, "threshold_value", "2", true);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsItemNotInBootGroup() {
        when(instanceBootGroupMemberDao.findByMember(MemberType.VirtualMachine, VM_ID)).thenReturn(null);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, null, true, null);
    }

    @Test
    public void createReadinessRuleAllowsItemViaInstanceGroupMembership() {
        when(instanceBootGroupMemberDao.findByMember(MemberType.VirtualMachine, VM_ID)).thenReturn(null);
        InstanceGroupVMMapVO mapping = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID));
        when(instanceBootGroupReadinessRuleDao.persist(any())).thenReturn(ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true));

        InstanceBootGroupReadinessRule result = manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, null, true, null);
        assertEquals(RULE_ID, result.getId());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsSecondSingletonRuleType() {
        stubVmDirectMember(VM_ID);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.singletonList(ruleVO(5L, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true)));
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, null, true, null);
    }

    @Test
    public void createReadinessRuleAllowsSecondPortCheckRule() {
        // PortCheck is not a singleton rule type, so validateSingletonRuleType never even
        // consults the existing-rules DAO for it — no need to stub listByItem here.
        stubVmDirectMember(VM_ID);
        Map<String, String> details = new HashMap<>();
        details.put("port", "22");
        when(instanceBootGroupReadinessRuleDao.persist(any())).thenReturn(ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, true));

        InstanceBootGroupReadinessRule result = manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, null, true, details);
        assertEquals(RULE_ID, result.getId());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsGuestAgentLivenessOnNonKvm() {
        stubVmDirectMember(VM_ID);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.emptyList());
        UserVmVO vm22141 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.VMware);
        when(userVmDao.findById(VM_ID)).thenReturn(vm22141);
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.GuestAgentLiveness, null, true, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsGuestAgentLivenessVmNotFound() {
        stubVmDirectMember(VM_ID);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.emptyList());
        when(userVmDao.findById(VM_ID)).thenReturn(null);
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.GuestAgentLiveness, null, true, null);
    }

    @Test
    public void createReadinessRuleAllowsGuestAgentLivenessOnKvm() {
        stubVmDirectMember(VM_ID);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.emptyList());
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        when(instanceBootGroupReadinessRuleDao.persist(any())).thenReturn(ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.GuestAgentLiveness, true));

        InstanceBootGroupReadinessRule result = manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.GuestAgentLiveness, null, true, null);
        assertEquals(RULE_ID, result.getId());
    }

    @Test
    public void createReadinessRuleDoesNotValidateGuestAgentLivenessForGroupScope() {
        stubGroupDirectMember(GROUP_ID);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.emptyList());
        when(instanceBootGroupReadinessRuleDao.persist(any())).thenReturn(ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.GuestAgentLiveness, true));

        InstanceBootGroupReadinessRule result = manager.createReadinessRule(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.GuestAgentLiveness, null, true, null);

        assertEquals(RULE_ID, result.getId());
        verify(userVmDao, never()).findById(anyLong());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsInvalidPortCheckDetails() {
        stubVmDirectMember(VM_ID);
        Map<String, String> details = new HashMap<>();
        details.put("port", "70000");
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, null, true, details);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsMissingPortCheckPort() {
        stubVmDirectMember(VM_ID);
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, null, true, new HashMap<>());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsNonTcpProtocol() {
        stubVmDirectMember(VM_ID);
        Map<String, String> details = new HashMap<>();
        details.put("port", "22");
        details.put("protocol", "udp");
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, null, true, details);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsMissingMemberQuorumDetails() {
        stubGroupDirectMember(GROUP_ID);
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, null, true, new HashMap<>());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsInvalidMemberQuorumThresholdType() {
        stubGroupDirectMember(GROUP_ID);
        Map<String, String> details = new HashMap<>();
        details.put("threshold_type", "BOGUS");
        details.put("threshold_value", "2");
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, null, true, details);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createReadinessRuleRejectsNonNumericMemberQuorumThreshold() {
        stubGroupDirectMember(GROUP_ID);
        Map<String, String> details = new HashMap<>();
        details.put("threshold_type", "PERCENTAGE");
        details.put("threshold_value", "abc");
        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, null, true, details);
    }

    @Test
    public void createReadinessRuleGeneratesDefaultNameWhenBlank() {
        stubVmDirectMember(VM_ID);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.emptyList());
        ArgumentCaptor<InstanceBootGroupReadinessRuleVO> captor = ArgumentCaptor.forClass(InstanceBootGroupReadinessRuleVO.class);
        when(instanceBootGroupReadinessRuleDao.persist(captor.capture())).thenReturn(ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true));

        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, "  ", true, null);

        assertEquals(String.format("%s-%s-%d", RuleType.Ping.name(), MemberType.VirtualMachine.name(), VM_ID), captor.getValue().getName());
    }

    @Test
    public void createReadinessRuleUsesGivenName() {
        stubVmDirectMember(VM_ID);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.emptyList());
        ArgumentCaptor<InstanceBootGroupReadinessRuleVO> captor = ArgumentCaptor.forClass(InstanceBootGroupReadinessRuleVO.class);
        when(instanceBootGroupReadinessRuleDao.persist(captor.capture())).thenReturn(ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true));

        manager.createReadinessRule(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, "my-rule", true, null);

        assertEquals("my-rule", captor.getValue().getName());
    }

    // ==================================================================
    // updateReadinessRule
    // ==================================================================

    @Test(expected = InvalidParameterValueException.class)
    public void updateReadinessRuleNotFoundThrows() {
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(null);
        manager.updateReadinessRule(RULE_ID, "name", true, null);
    }

    @Test
    public void updateReadinessRuleNoChanges() {
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(rule);

        manager.updateReadinessRule(RULE_ID, null, null, null);

        verify(instanceBootGroupReadinessRuleDao).update(eq(RULE_ID), any());
        verify(instanceBootGroupReadinessRuleDetailsDao, never()).addDetail(anyLong(), any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void updateReadinessRuleNameAndEnabled() {
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(rule);

        manager.updateReadinessRule(RULE_ID, "new-name", false, null);

        assertEquals("new-name", rule.getName());
        assertEquals(false, rule.isEnabled());
        verify(instanceBootGroupReadinessRuleDao).update(RULE_ID, rule);
    }

    @Test
    public void updateReadinessRuleMergesAndValidatesDetails() {
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, true);
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(rule);
        Map<String, String> existing = new HashMap<>();
        existing.put("port", "8080");
        existing.put("protocol", "tcp");
        when(instanceBootGroupReadinessRuleDetailsDao.getDetails(RULE_ID)).thenReturn(existing);

        Map<String, String> update = new HashMap<>();
        update.put("port", "9090");
        manager.updateReadinessRule(RULE_ID, null, null, update);

        verify(instanceBootGroupReadinessRuleDetailsDao).addDetail(RULE_ID, "port", "9090", true);
        verify(instanceBootGroupReadinessRuleDao).update(RULE_ID, rule);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void updateReadinessRuleRejectsInvalidMergedDetails() {
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, true);
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(rule);
        when(instanceBootGroupReadinessRuleDetailsDao.getDetails(RULE_ID)).thenReturn(new HashMap<>());

        Map<String, String> update = new HashMap<>();
        update.put("port", "invalid");
        try {
            manager.updateReadinessRule(RULE_ID, null, null, update);
        } finally {
            verify(instanceBootGroupReadinessRuleDao, never()).update(anyLong(), any());
        }
    }

    // ==================================================================
    // deleteReadinessRule
    // ==================================================================

    @Test(expected = InvalidParameterValueException.class)
    public void deleteReadinessRuleNotFoundThrows() {
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(null);
        manager.deleteReadinessRule(RULE_ID);
    }

    @Test
    public void deleteReadinessRuleSuccess() {
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(rule);

        boolean result = manager.deleteReadinessRule(RULE_ID);

        assertTrue(result);
        verify(instanceBootGroupReadinessRuleDetailsDao).removeDetails(RULE_ID);
        verify(instanceBootGroupReadinessCheckResultDao).deleteByRuleId(RULE_ID);
        verify(instanceBootGroupReadinessRuleDao).remove(RULE_ID);
    }

    // ==================================================================
    // findById / getRuleDetails
    // ==================================================================

    @Test
    public void findByIdDelegatesToDao() {
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.findById(RULE_ID)).thenReturn(rule);
        assertEquals(rule, manager.findById(RULE_ID));
    }

    @Test
    public void getRuleDetailsDelegatesToDetailsDao() {
        Map<String, String> details = Collections.singletonMap("k", "v");
        when(instanceBootGroupReadinessRuleDetailsDao.getDetails(RULE_ID)).thenReturn(details);
        assertEquals(details, manager.getRuleDetails(RULE_ID));
    }

    // ==================================================================
    // evaluateVmReadiness / resolveVmReadiness (dispatch path)
    // ==================================================================

    @Test
    public void evaluateVmReadinessVmNotFoundIsNotReady() {
        when(userVmDao.findById(VM_ID)).thenReturn(null);
        Status status = manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null);
        assertEquals(Status.NotReady, status);
    }

    @Test
    public void evaluateVmReadinessNonRunningVmIsNotReadyWithoutDispatch() {
        UserVmVO vm70096 = mockVm(VM_ID, VirtualMachine.State.Stopped, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm70096);
        Status status = manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null);
        assertEquals(Status.NotReady, status);
        verify(instanceBootGroupReadinessRuleDao, never()).listEnabledByItem(anyLong(), any(), anyLong());
        verify(instanceBootGroupReadinessCheckResultDao, never()).upsert(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    public void evaluateVmReadinessNoRulesIsReady() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.emptyList());
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());

        Status status = manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null);
        assertEquals(Status.Ready, status);
    }

    @Test
    public void evaluateVmReadinessDispatchesDirectRuleAndCaches() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.singletonList(rule));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        manager.setReadinessCheckers(Collections.singletonList(mockChecker(RuleType.Ping, Status.Ready, "pong")));

        Status status = manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null);

        assertEquals(Status.Ready, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Ready), eq("pong"), any(Date.class));
    }

    @Test
    public void evaluateVmReadinessAppendsAttemptLabelToMessage() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.singletonList(rule));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        manager.setReadinessCheckers(Collections.singletonList(mockChecker(RuleType.Ping, Status.NotReady, "no reply")));

        manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, "2/5");

        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.NotReady), eq("no reply (attempt 2/5)"), any(Date.class));
    }

    @Test
    public void evaluateVmReadinessSkipsAttemptLabelWhenBlank() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.singletonList(rule));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        manager.setReadinessCheckers(Collections.singletonList(mockChecker(RuleType.Ping, Status.Ready, "pong")));

        manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null);

        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Ready), eq("pong"), any(Date.class));
    }

    @Test
    public void evaluateVmReadinessNoCheckerRegisteredSynthesizesError() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        InstanceBootGroupReadinessRuleVO rule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.CustomScript, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.singletonList(rule));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        // no checkers registered at all (set in @Before)

        Status status = manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, "1/3");

        assertEquals(Status.Error, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Error),
                eq("No checker implemented yet for rule type CustomScript (attempt 1/3)"), any(Date.class));
    }

    @Test
    public void evaluateVmReadinessErrorTakesPrecedenceOverNotReady() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(11L, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        InstanceBootGroupReadinessRuleVO portRule = ruleVO(12L, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Arrays.asList(pingRule, portRule));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        List<ReadinessChecker> checkers = Arrays.asList(
                mockChecker(RuleType.Ping, Status.NotReady, "not yet"),
                mockChecker(RuleType.PortCheck, Status.Error, "boom"));
        manager.setReadinessCheckers(checkers);

        Status status = manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null);
        assertEquals(Status.Error, status);
    }

    @Test
    public void evaluateVmReadinessAllReadyIsReady() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(11L, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.singletonList(pingRule));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        manager.setReadinessCheckers(Collections.singletonList(mockChecker(RuleType.Ping, Status.Ready, "pong")));

        assertEquals(Status.Ready, manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null));
    }

    @Test
    public void evaluateVmReadinessDispatchesInheritedGroupRuleAtMemberVmId() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.emptyList());

        InstanceGroupVMMapVO mapping = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID));
        InstanceBootGroupReadinessRuleVO groupPingRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.singletonList(groupPingRule));
        manager.setReadinessCheckers(Collections.singletonList(mockChecker(RuleType.Ping, Status.Ready, "pong")));

        Status status = manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null);

        assertEquals(Status.Ready, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(VM_ID), eq(Status.Ready), eq("pong"), any(Date.class));
    }

    @Test
    public void evaluateVmReadinessBudgetDoesNotIncreaseAcrossRules() {
        UserVmVO vm85153 = mockVm(VM_ID, VirtualMachine.State.Running, HypervisorType.KVM);
        when(userVmDao.findById(VM_ID)).thenReturn(vm85153);
        InstanceBootGroupReadinessRuleVO rule1 = ruleVO(11L, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, true);
        InstanceBootGroupReadinessRuleVO rule2 = ruleVO(12L, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Arrays.asList(rule1, rule2));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());

        ReadinessChecker checker = mock(ReadinessChecker.class);
        when(checker.getRuleType()).thenReturn(RuleType.PortCheck);
        ArgumentCaptor<Long> remainingCaptor = ArgumentCaptor.forClass(Long.class);
        when(checker.check(any(), any(), anyLong(), remainingCaptor.capture())).thenReturn(new ReadinessChecker.Result(Status.Ready, "ok"));
        manager.setReadinessCheckers(Collections.singletonList(checker));

        manager.evaluateVmReadiness(BOOT_GROUP_ID, VM_ID, 10000L, null);

        List<Long> remaining = remainingCaptor.getAllValues();
        assertEquals(2, remaining.size());
        assertTrue("budget for first rule should be at most the initial budget", remaining.get(0) <= 10000L);
        assertTrue("budget must not increase across rules", remaining.get(1) <= remaining.get(0));
    }

    // ==================================================================
    // findInheritedGroupRules
    // ==================================================================

    @Test
    public void findInheritedGroupRulesEmptyWhenVmNotInAnyGroup() {
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        assertTrue(manager.findInheritedGroupRules(BOOT_GROUP_ID, VM_ID).isEmpty());
    }

    @Test
    public void findInheritedGroupRulesEmptyWhenGroupNotABootGroupMember() {
        InstanceGroupVMMapVO mapping = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(null);
        assertTrue(manager.findInheritedGroupRules(BOOT_GROUP_ID, VM_ID).isEmpty());
    }

    @Test
    public void findInheritedGroupRulesEmptyWhenGroupBelongsToDifferentBootGroup() {
        InstanceGroupVMMapVO mapping = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(memberVO(999L, MemberType.InstanceGroup, GROUP_ID));
        assertTrue(manager.findInheritedGroupRules(BOOT_GROUP_ID, VM_ID).isEmpty());
    }

    @Test
    public void findInheritedGroupRulesFiltersToMemberTargetedTypes() {
        InstanceGroupVMMapVO mapping = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID));

        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(11L, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.Ping, true);
        InstanceBootGroupReadinessRuleVO quorumRule = ruleVO(12L, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, true);
        InstanceBootGroupReadinessRuleVO scriptRule = ruleVO(13L, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.CustomScript, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID))
                .thenReturn(Arrays.asList(pingRule, quorumRule, scriptRule));

        List<InstanceBootGroupReadinessRule> result = manager.findInheritedGroupRules(BOOT_GROUP_ID, VM_ID);

        assertEquals(1, result.size());
        assertEquals(RuleType.Ping, result.get(0).getRuleType());
    }

    @Test
    public void findInheritedGroupRulesSkipsNonMatchingMappingThenMatches() {
        InstanceGroupVMMapVO mapping1 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        InstanceGroupVMMapVO mapping2 = new InstanceGroupVMMapVO(GROUP_ID + 1, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Arrays.asList(mapping1, mapping2));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(null);
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID + 1)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID + 1));
        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(11L, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID + 1, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID + 1)).thenReturn(Collections.singletonList(pingRule));

        List<InstanceBootGroupReadinessRule> result = manager.findInheritedGroupRules(BOOT_GROUP_ID, VM_ID);
        assertEquals(1, result.size());
    }

    // ==================================================================
    // invalidateCachedReadinessOnRestart
    // ==================================================================

    @Test
    public void invalidateCachedReadinessOnRestartNoBootGroupInvolvementIsNoOp() {
        when(instanceBootGroupMemberDao.findByMember(MemberType.VirtualMachine, VM_ID)).thenReturn(null);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());

        manager.invalidateCachedReadinessOnRestart(VM_ID);

        verify(instanceBootGroupReadinessCheckResultDao, never()).upsert(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    public void invalidateCachedReadinessOnRestartDirectMemberInvalidatesOwnRules() {
        when(instanceBootGroupMemberDao.findByMember(MemberType.VirtualMachine, VM_ID)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID));
        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.singletonList(pingRule));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());

        manager.invalidateCachedReadinessOnRestart(VM_ID);

        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Unknown), any(), any(Date.class));
    }

    @Test
    public void invalidateCachedReadinessOnRestartInheritedGroupRulesInvalidateOnlyMemberTargetedTypes() {
        when(instanceBootGroupMemberDao.findByMember(MemberType.VirtualMachine, VM_ID)).thenReturn(null);
        InstanceGroupVMMapVO mapping = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID));

        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(11L, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.Ping, true);
        InstanceBootGroupReadinessRuleVO quorumRule = ruleVO(12L, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID))
                .thenReturn(Arrays.asList(pingRule, quorumRule));

        manager.invalidateCachedReadinessOnRestart(VM_ID);

        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(11L), eq(VM_ID), eq(Status.Unknown), any(), any(Date.class));
        verify(instanceBootGroupReadinessCheckResultDao, never()).upsert(eq(12L), anyLong(), any(), any(), any());
    }

    @Test
    public void invalidateCachedReadinessOnRestartGroupNotABootGroupMemberIsNoOp() {
        when(instanceBootGroupMemberDao.findByMember(MemberType.VirtualMachine, VM_ID)).thenReturn(null);
        InstanceGroupVMMapVO mapping = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(null);

        manager.invalidateCachedReadinessOnRestart(VM_ID);

        verify(instanceBootGroupReadinessCheckResultDao, never()).upsert(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    public void invalidateCachedReadinessOnRestartInvalidatesBothDirectAndInheritedRules() {
        when(instanceBootGroupMemberDao.findByMember(MemberType.VirtualMachine, VM_ID)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID));
        InstanceBootGroupReadinessRuleVO directRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID, RuleType.PortCheck, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, VM_ID)).thenReturn(Collections.singletonList(directRule));

        InstanceGroupVMMapVO mapping = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(MemberType.InstanceGroup, GROUP_ID)).thenReturn(memberVO(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID));
        InstanceBootGroupReadinessRuleVO groupRule = ruleVO(11L, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.GuestAgentLiveness, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.singletonList(groupRule));

        manager.invalidateCachedReadinessOnRestart(VM_ID);

        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Unknown), any(), any(Date.class));
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(11L), eq(VM_ID), eq(Status.Unknown), any(), any(Date.class));
    }

    // ==================================================================
    // evaluateInstanceGroupReadiness
    // ==================================================================

    private void stubNoRuleVm(long vmId, VirtualMachine.State state) {
        UserVmVO vm56538 = mockVm(vmId, state, HypervisorType.KVM);
        when(userVmDao.findById(vmId)).thenReturn(vm56538);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.VirtualMachine, vmId)).thenReturn(Collections.emptyList());
        when(instanceGroupVMMapDao.listByInstanceId(vmId)).thenReturn(Collections.emptyList());
    }

    @Test
    public void evaluateInstanceGroupReadinessAllMembersReadyNoRules() {
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.emptyList());
        InstanceGroupVMMapVO m1 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        InstanceGroupVMMapVO m2 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID_2);
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Arrays.asList(m1, m2));
        stubNoRuleVm(VM_ID, VirtualMachine.State.Running);
        stubNoRuleVm(VM_ID_2, VirtualMachine.State.Running);

        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, Collections.emptySet());
        assertEquals(Status.Ready, status);
    }

    @Test
    public void evaluateInstanceGroupReadinessMidRetryMemberIsNotReadyNotError() {
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.emptyList());
        InstanceGroupVMMapVO m1 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        InstanceGroupVMMapVO m2 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID_2);
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Arrays.asList(m1, m2));
        stubNoRuleVm(VM_ID, VirtualMachine.State.Running);
        stubNoRuleVm(VM_ID_2, VirtualMachine.State.Starting);

        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, Collections.emptySet());
        assertEquals(Status.NotReady, status);
    }

    @Test
    public void evaluateInstanceGroupReadinessPermanentlyFailedMemberIsError() {
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.emptyList());
        InstanceGroupVMMapVO m1 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        InstanceGroupVMMapVO m2 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID_2);
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Arrays.asList(m1, m2));
        stubNoRuleVm(VM_ID, VirtualMachine.State.Running);
        stubNoRuleVm(VM_ID_2, VirtualMachine.State.Starting);

        Set<Long> permanentlyFailed = new HashSet<>(Collections.singletonList(VM_ID_2));
        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, permanentlyFailed);
        assertEquals(Status.Error, status);
    }

    @Test
    public void evaluateInstanceGroupReadinessQuorumExcludesOwnMemberStatuses() {
        InstanceBootGroupReadinessRuleVO quorumRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.singletonList(quorumRule));
        Map<String, String> details = new HashMap<>();
        details.put("threshold_type", "COUNT");
        details.put("threshold_value", "1");
        when(instanceBootGroupReadinessRuleDetailsDao.getDetails(RULE_ID)).thenReturn(details);

        InstanceGroupVMMapVO m1 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        InstanceGroupVMMapVO m2 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID_2);
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Arrays.asList(m1, m2));
        stubNoRuleVm(VM_ID, VirtualMachine.State.Running);
        stubNoRuleVm(VM_ID_2, VirtualMachine.State.Stopped); // NotReady, but must not affect result since quorum-governed

        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, Collections.emptySet());

        assertEquals(Status.Ready, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Ready), any(), any(Date.class));
    }

    @Test
    public void evaluateInstanceGroupReadinessQuorumMetOverridesFailingMemberTargetedRule() {
        InstanceBootGroupReadinessRuleVO guestAgentRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.GuestAgentLiveness, true);
        InstanceBootGroupReadinessRuleVO quorumRule = ruleVO(20L, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID))
                .thenReturn(Arrays.asList(guestAgentRule, quorumRule));
        when(instanceBootGroupReadinessRuleDetailsDao.getDetails(20L)).thenReturn(thresholdDetails("COUNT", "1"));

        InstanceGroupVMMapVO m1 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        InstanceGroupVMMapVO m2 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID_2);
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Arrays.asList(m1, m2));
        stubNoRuleVm(VM_ID, VirtualMachine.State.Running);
        stubNoRuleVm(VM_ID_2, VirtualMachine.State.Running);

        InstanceBootGroupReadinessCheckResultVO ready = new InstanceBootGroupReadinessCheckResultVO(RULE_ID, VM_ID, Status.Ready, "ok", new Date());
        InstanceBootGroupReadinessCheckResultVO notReady = new InstanceBootGroupReadinessCheckResultVO(RULE_ID, VM_ID_2, Status.NotReady, "no agent", new Date());
        when(instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(RULE_ID, VM_ID)).thenReturn(ready);
        when(instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(RULE_ID, VM_ID_2)).thenReturn(notReady);

        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, Collections.emptySet());

        assertEquals(Status.Ready, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.NotReady), any(), any(Date.class));
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(20L), eq(0L), eq(Status.Ready), any(), any(Date.class));
    }

    @Test
    public void evaluateInstanceGroupReadinessMemberTargetedRuleAggregatesCachedResults() {
        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.singletonList(pingRule));

        InstanceGroupVMMapVO m1 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        InstanceGroupVMMapVO m2 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID_2);
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Arrays.asList(m1, m2));
        stubNoRuleVm(VM_ID, VirtualMachine.State.Running);
        stubNoRuleVm(VM_ID_2, VirtualMachine.State.Running);

        InstanceBootGroupReadinessCheckResultVO cached1 = new InstanceBootGroupReadinessCheckResultVO(RULE_ID, VM_ID, Status.Ready, "ok", new Date());
        InstanceBootGroupReadinessCheckResultVO cached2 = new InstanceBootGroupReadinessCheckResultVO(RULE_ID, VM_ID_2, Status.Ready, "ok", new Date());
        when(instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(RULE_ID, VM_ID)).thenReturn(cached1);
        when(instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(RULE_ID, VM_ID_2)).thenReturn(cached2);

        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, Collections.emptySet());

        assertEquals(Status.Ready, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Ready), eq("2 of 2 member(s) ready via Ping"), any(Date.class));
    }

    @Test
    public void evaluateInstanceGroupReadinessMemberTargetedRuleNoMembersIsNotReady() {
        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.singletonList(pingRule));
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Collections.emptyList());

        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, Collections.emptySet());

        assertEquals(Status.NotReady, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.NotReady), eq("Instance group has no members"), any(Date.class));
    }

    @Test
    public void evaluateInstanceGroupReadinessMemberTargetedRuleAnyErrorIsError() {
        InstanceBootGroupReadinessRuleVO pingRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.Ping, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.singletonList(pingRule));

        InstanceGroupVMMapVO m1 = new InstanceGroupVMMapVO(GROUP_ID, VM_ID);
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Collections.singletonList(m1));
        stubNoRuleVm(VM_ID, VirtualMachine.State.Running);
        InstanceBootGroupReadinessCheckResultVO cached = new InstanceBootGroupReadinessCheckResultVO(RULE_ID, VM_ID, Status.Error, "unreachable", new Date());
        when(instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(RULE_ID, VM_ID)).thenReturn(cached);

        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, Collections.emptySet());
        assertEquals(Status.Error, status);
    }

    @Test
    public void evaluateInstanceGroupReadinessUnimplementedGroupRuleTypeIsError() {
        InstanceBootGroupReadinessRuleVO scriptRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.CustomScript, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.singletonList(scriptRule));
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(Collections.emptyList());

        Status status = manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, Collections.emptySet());

        assertEquals(Status.Error, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Error),
                eq("No evaluator implemented yet for rule type CustomScript"), any(Date.class));
    }

    // ==================================================================
    // evaluateInstanceQuorum (via evaluateInstanceGroupReadiness + MemberQuorum rule)
    // ==================================================================

    private Status evaluateQuorum(Map<String, String> details, List<InstanceGroupVMMapVO> members, Set<Long> permanentlyFailed) {
        InstanceBootGroupReadinessRuleVO quorumRule = ruleVO(RULE_ID, BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID, RuleType.MemberQuorum, true);
        when(instanceBootGroupReadinessRuleDao.listEnabledByItem(BOOT_GROUP_ID, MemberType.InstanceGroup, GROUP_ID)).thenReturn(Collections.singletonList(quorumRule));
        when(instanceBootGroupReadinessRuleDetailsDao.getDetails(RULE_ID)).thenReturn(details);
        when(instanceGroupVMMapDao.listByGroupId(GROUP_ID)).thenReturn(members);
        return manager.evaluateInstanceGroupReadiness(BOOT_GROUP_ID, GROUP_ID, permanentlyFailed);
    }

    private Map<String, String> thresholdDetails(String type, String value) {
        Map<String, String> details = new HashMap<>();
        details.put("threshold_type", type);
        details.put("threshold_value", value);
        return details;
    }

    @Test
    public void evaluateInstanceQuorumNoMembersIsNotReady() {
        Status status = evaluateQuorum(thresholdDetails("COUNT", "1"), Collections.emptyList(), Collections.emptySet());
        assertEquals(Status.NotReady, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.NotReady), eq("Instance group has no members"), any(Date.class));
    }

    @Test
    public void evaluateInstanceQuorumPercentageThresholdMet() {
        List<InstanceGroupVMMapVO> members = new ArrayList<>();
        for (long i = 0; i < 4; i++) {
            members.add(new InstanceGroupVMMapVO(GROUP_ID, VM_ID + i));
            stubNoRuleVm(VM_ID + i, i < 3 ? VirtualMachine.State.Running : VirtualMachine.State.Stopped);
        }
        Status status = evaluateQuorum(thresholdDetails("PERCENTAGE", "50"), members, Collections.emptySet());
        assertEquals(Status.Ready, status);
    }

    @Test
    public void evaluateInstanceQuorumPercentageThresholdNotMetButAchievable() {
        List<InstanceGroupVMMapVO> members = new ArrayList<>();
        for (long i = 0; i < 4; i++) {
            members.add(new InstanceGroupVMMapVO(GROUP_ID, VM_ID + i));
            stubNoRuleVm(VM_ID + i, i < 1 ? VirtualMachine.State.Running : VirtualMachine.State.Stopped);
        }
        Status status = evaluateQuorum(thresholdDetails("PERCENTAGE", "50"), members, Collections.emptySet());
        assertEquals(Status.NotReady, status);
    }

    @Test
    public void evaluateInstanceQuorumPercentageThresholdUnreachableIsError() {
        List<InstanceGroupVMMapVO> members = new ArrayList<>();
        Set<Long> permanentlyFailed = new HashSet<>();
        for (long i = 0; i < 4; i++) {
            long vmId = VM_ID + i;
            members.add(new InstanceGroupVMMapVO(GROUP_ID, vmId));
            if (i < 1) {
                stubNoRuleVm(vmId, VirtualMachine.State.Running);
            } else {
                stubNoRuleVm(vmId, VirtualMachine.State.Stopped);
                permanentlyFailed.add(vmId);
            }
        }
        // 1 ready, achievableCount = 4 - 3 = 1, 25% < 90% -> unreachable
        Status status = evaluateQuorum(thresholdDetails("PERCENTAGE", "90"), members, permanentlyFailed);
        assertEquals(Status.Error, status);
    }

    @Test
    public void evaluateInstanceQuorumCountThresholdMet() {
        List<InstanceGroupVMMapVO> members = new ArrayList<>();
        for (long i = 0; i < 3; i++) {
            members.add(new InstanceGroupVMMapVO(GROUP_ID, VM_ID + i));
            stubNoRuleVm(VM_ID + i, i < 2 ? VirtualMachine.State.Running : VirtualMachine.State.Stopped);
        }
        Status status = evaluateQuorum(thresholdDetails("COUNT", "2"), members, Collections.emptySet());
        assertEquals(Status.Ready, status);
    }

    @Test
    public void evaluateInstanceQuorumInvalidThresholdValueIsError() {
        List<InstanceGroupVMMapVO> members = Collections.singletonList(new InstanceGroupVMMapVO(GROUP_ID, VM_ID));
        stubNoRuleVm(VM_ID, VirtualMachine.State.Running);
        Status status = evaluateQuorum(thresholdDetails("COUNT", "abc"), members, Collections.emptySet());
        assertEquals(Status.Error, status);
        verify(instanceBootGroupReadinessCheckResultDao).upsert(eq(RULE_ID), eq(0L), eq(Status.Error),
                eq("Invalid threshold configuration: COUNT=abc"), any(Date.class));
    }
}
