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

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.InstanceBootGroupMemberDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupMembershipGuardTest {

    private static final long VM_ID = 100L;
    private static final long TEMPLATE_ID = 200L;
    private static final long FIRST_GROUP_ID = 10L;
    private static final long SECOND_GROUP_ID = 20L;
    private static final long BOOT_GROUP_ID = 1000L;

    @InjectMocks
    InstanceBootGroupMembershipGuard guard;

    @Mock
    UserVmDao userVmDao;

    @Mock
    VMTemplateDao templateDao;

    @Mock
    AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;

    @Mock
    InstanceBootGroupMemberDao instanceBootGroupMemberDao;

    @Mock
    InstanceGroupVMMapDao instanceGroupVMMapDao;

    @Mock
    InstanceBootGroupReadinessRuleDao instanceBootGroupReadinessRuleDao;

    @Mock
    UserVmVO vm;

    @Before
    public void setUp() {
        when(userVmDao.findById(VM_ID)).thenReturn(vm);
        when(vm.getTemplateId()).thenReturn(TEMPLATE_ID);
        when(templateDao.findByIdIncludingRemoved(TEMPLATE_ID)).thenReturn(null);
        when(autoScaleVmGroupVmMapDao.listByVm(VM_ID)).thenReturn(Collections.emptyList());
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID)).thenReturn(null);
    }

    @Test
    public void testVmNotFoundThrows() {
        when(userVmDao.findById(VM_ID)).thenReturn(null);
        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmEligibleForGroupMembership(VM_ID));
    }

    @Test
    public void testVnfTemplateThrows() {
        VMTemplateVO template = mock(VMTemplateVO.class);
        when(template.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        when(templateDao.findByIdIncludingRemoved(TEMPLATE_ID)).thenReturn(template);

        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmEligibleForGroupMembership(VM_ID));
    }

    @Test
    public void testCksNodeThrows() {
        when(vm.getUserVmType()).thenReturn(UserVmManager.CKS_NODE);
        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmEligibleForGroupMembership(VM_ID));
    }

    @Test
    public void testInAutoScaleGroupThrows() {
        when(autoScaleVmGroupVmMapDao.listByVm(VM_ID)).thenReturn(Collections.singletonList(mock(com.cloud.network.as.AutoScaleVmGroupVmMapVO.class)));
        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmEligibleForGroupMembership(VM_ID));
    }

    @Test
    public void testAlreadyIndependentBootGroupMemberThrows() {
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(mock(InstanceBootGroupMemberVO.class));
        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmEligibleForGroupMembership(VM_ID));
    }

    @Test
    public void testEligibleForGroupMembershipIgnoresCurrentInstanceGroupBootGroupStatus() {
        // validateVmEligibleForGroupMembership no longer looks at the VM's CURRENT instance group
        // membership at all (that's validateVmNotIndirectlyBootGroupManaged's job), so it must pass
        // even when instanceGroupVMMapDao/instanceBootGroupMemberDao are never stubbed to return
        // anything for the VM's groups.
        guard.validateVmEligibleForGroupMembership(VM_ID);
        org.mockito.Mockito.verifyNoInteractions(instanceGroupVMMapDao);
    }

    // ---------------------------------------------------------------- validateVmNotIndirectlyBootGroupManaged

    @Test
    public void testIndirectlyManagedVmNotFoundThrows() {
        when(userVmDao.findById(VM_ID)).thenReturn(null);
        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmNotIndirectlyBootGroupManaged(VM_ID));
    }

    @Test
    public void testNoInstanceGroupMappingsPasses() {
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        guard.validateVmNotIndirectlyBootGroupManaged(VM_ID);
    }

    @Test
    public void testFirstInstanceGroupIsBootGroupMemberThrows() {
        InstanceGroupVMMapVO firstMapping = mock(InstanceGroupVMMapVO.class);
        when(firstMapping.getGroupId()).thenReturn(FIRST_GROUP_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(firstMapping));
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID))
                .thenReturn(mock(InstanceBootGroupMemberVO.class));

        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmNotIndirectlyBootGroupManaged(VM_ID));
    }

    /**
     * Regression test: a VM can belong to more than one Instance Group (instance_group_vm_map has no
     * one-group-per-VM constraint), so a disqualifying group must not be missed just because it isn't
     * the first mapping returned.
     */
    @Test
    public void testSecondInstanceGroupIsBootGroupMemberThrows() {
        InstanceGroupVMMapVO firstMapping = mock(InstanceGroupVMMapVO.class);
        when(firstMapping.getGroupId()).thenReturn(FIRST_GROUP_ID);
        InstanceGroupVMMapVO secondMapping = mock(InstanceGroupVMMapVO.class);
        when(secondMapping.getGroupId()).thenReturn(SECOND_GROUP_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Arrays.asList(firstMapping, secondMapping));

        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID)).thenReturn(null);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, SECOND_GROUP_ID))
                .thenReturn(mock(InstanceBootGroupMemberVO.class));

        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmNotIndirectlyBootGroupManaged(VM_ID));
    }

    @Test
    public void testMultipleInstanceGroupsNoneDisqualifyingPasses() {
        InstanceGroupVMMapVO firstMapping = mock(InstanceGroupVMMapVO.class);
        when(firstMapping.getGroupId()).thenReturn(FIRST_GROUP_ID);
        InstanceGroupVMMapVO secondMapping = mock(InstanceGroupVMMapVO.class);
        when(secondMapping.getGroupId()).thenReturn(SECOND_GROUP_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Arrays.asList(firstMapping, secondMapping));

        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID)).thenReturn(null);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, SECOND_GROUP_ID)).thenReturn(null);

        guard.validateVmNotIndirectlyBootGroupManaged(VM_ID);
    }

    // ---------------------------------------------------------------- validateInstanceGroupEligibleForBootGroupMembership

    @Test
    public void testInstanceGroupEligibleWhenNoMembersDisqualified() {
        InstanceGroupVMMapVO member = mock(InstanceGroupVMMapVO.class);
        when(member.getInstanceId()).thenReturn(VM_ID);
        when(instanceGroupVMMapDao.listByGroupId(FIRST_GROUP_ID)).thenReturn(Collections.singletonList(member));
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());

        guard.validateInstanceGroupEligibleForBootGroupMembership(FIRST_GROUP_ID);
    }

    /**
     * A member VM already indirectly governed via some OTHER Instance Group (one that's already a
     * boot-group member) must disqualify the whole group, not just the VNF/CKS/AutoScale checks.
     */
    @Test
    public void testInstanceGroupNotEligibleWhenMemberAlreadyIndirectlyManaged() {
        InstanceGroupVMMapVO member = mock(InstanceGroupVMMapVO.class);
        when(member.getInstanceId()).thenReturn(VM_ID);
        when(instanceGroupVMMapDao.listByGroupId(FIRST_GROUP_ID)).thenReturn(Collections.singletonList(member));

        InstanceGroupVMMapVO otherMapping = mock(InstanceGroupVMMapVO.class);
        when(otherMapping.getGroupId()).thenReturn(SECOND_GROUP_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(otherMapping));
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, SECOND_GROUP_ID))
                .thenReturn(mock(InstanceBootGroupMemberVO.class));

        assertThrows(InvalidParameterValueException.class, () -> guard.validateInstanceGroupEligibleForBootGroupMembership(FIRST_GROUP_ID));
    }

    // ---------------------------------------------------------------- validateVmNotInBootGroup

    @Test
    public void testValidateVmNotInBootGroupDirectMemberThrows() {
        when(vm.getId()).thenReturn(VM_ID);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(mock(InstanceBootGroupMemberVO.class));

        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmNotInBootGroup(vm));
    }

    @Test
    public void testValidateVmNotInBootGroupInInstanceGroupMemberWithOwnRuleThrows() {
        when(vm.getId()).thenReturn(VM_ID);
        InstanceGroupVMMapVO mapping = mock(InstanceGroupVMMapVO.class);
        when(mapping.getGroupId()).thenReturn(FIRST_GROUP_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        InstanceBootGroupMemberVO groupMember = mock(InstanceBootGroupMemberVO.class);
        when(groupMember.getBootGroupId()).thenReturn(BOOT_GROUP_ID);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID))
                .thenReturn(groupMember);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.singletonList(mock(InstanceBootGroupReadinessRuleVO.class)));

        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmNotInBootGroup(vm));
    }

    /**
     * Being in an Instance Group that is itself a boot-group member is NOT, on its own, a reason to
     * block destroy — boot group orchestration resolves the group's VMs dynamically, so only a VM
     * with its own readiness rule(s) registered in the boot group is actually at risk of going stale.
     */
    @Test
    public void testValidateVmNotInBootGroupInInstanceGroupMemberWithoutOwnRulePasses() {
        when(vm.getId()).thenReturn(VM_ID);
        InstanceGroupVMMapVO mapping = mock(InstanceGroupVMMapVO.class);
        when(mapping.getGroupId()).thenReturn(FIRST_GROUP_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        InstanceBootGroupMemberVO groupMember = mock(InstanceBootGroupMemberVO.class);
        when(groupMember.getBootGroupId()).thenReturn(BOOT_GROUP_ID);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID))
                .thenReturn(groupMember);
        when(instanceBootGroupReadinessRuleDao.listByItem(BOOT_GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, VM_ID))
                .thenReturn(Collections.emptyList());

        guard.validateVmNotInBootGroup(vm);
    }

    @Test
    public void testValidateVmNotInBootGroupNotAMemberPasses() {
        when(vm.getId()).thenReturn(VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());

        guard.validateVmNotInBootGroup(vm);
    }

    // ---------------------------------------------------------------- removeInstanceGroupBootGroupMembershipIfPresent

    @Test
    public void testRemoveInstanceGroupBootGroupMembershipIfPresentExpungesExistingMember() {
        InstanceBootGroupMemberVO member = mock(InstanceBootGroupMemberVO.class);
        when(member.getId()).thenReturn(999L);
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID))
                .thenReturn(member);

        guard.removeInstanceGroupBootGroupMembershipIfPresent(FIRST_GROUP_ID);

        org.mockito.Mockito.verify(instanceBootGroupMemberDao).expunge(999L);
    }

    @Test
    public void testRemoveInstanceGroupBootGroupMembershipIfPresentNoOpWhenNotAMember() {
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID))
                .thenReturn(null);

        guard.removeInstanceGroupBootGroupMembershipIfPresent(FIRST_GROUP_ID);

        org.mockito.Mockito.verify(instanceBootGroupMemberDao, org.mockito.Mockito.never()).expunge(org.mockito.ArgumentMatchers.anyLong());
    }
}
