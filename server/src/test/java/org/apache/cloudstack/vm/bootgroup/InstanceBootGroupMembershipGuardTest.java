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
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupMembershipGuardTest {

    private static final long VM_ID = 100L;
    private static final long TEMPLATE_ID = 200L;
    private static final long FIRST_GROUP_ID = 10L;
    private static final long SECOND_GROUP_ID = 20L;

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
    public void testNoInstanceGroupMappingsPasses() {
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());
        guard.validateVmEligibleForGroupMembership(VM_ID);
    }

    @Test
    public void testFirstInstanceGroupIsBootGroupMemberThrows() {
        InstanceGroupVMMapVO firstMapping = mock(InstanceGroupVMMapVO.class);
        when(firstMapping.getGroupId()).thenReturn(FIRST_GROUP_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(firstMapping));
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID))
                .thenReturn(mock(InstanceBootGroupMemberVO.class));

        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmEligibleForGroupMembership(VM_ID));
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

        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmEligibleForGroupMembership(VM_ID));
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

        guard.validateVmEligibleForGroupMembership(VM_ID);
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
    public void testValidateVmNotInBootGroupInInstanceGroupMemberThrows() {
        when(vm.getId()).thenReturn(VM_ID);
        InstanceGroupVMMapVO mapping = mock(InstanceGroupVMMapVO.class);
        when(mapping.getGroupId()).thenReturn(FIRST_GROUP_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(mapping));
        when(instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, FIRST_GROUP_ID))
                .thenReturn(mock(InstanceBootGroupMemberVO.class));

        assertThrows(InvalidParameterValueException.class, () -> guard.validateVmNotInBootGroup(vm));
    }

    @Test
    public void testValidateVmNotInBootGroupNotAMemberPasses() {
        when(vm.getId()).thenReturn(VM_ID);
        when(instanceGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.emptyList());

        guard.validateVmNotInBootGroup(vm);
    }
}
