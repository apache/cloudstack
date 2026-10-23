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

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

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

/**
 * Eligibility guard shared by the two places a VM can end up governed by a boot group: joining a
 * plain Instance Group ({@code UserVmManagerImpl.addInstanceToGroup}) and being added directly to a
 * boot group ({@code InstanceBootGroupApiServiceImpl.addMemberToInstanceBootGroup}). Kept as its own
 * leaf component (no dependency on UserVmService/UserVmManager) so UserVmManagerImpl can depend on it
 * without a circular Spring bean dependency back through InstanceBootGroupApiServiceImpl/Manager,
 * which themselves depend on UserVmService.
 */
@Component
public class InstanceBootGroupMembershipGuard {

    @Inject
    private UserVmDao userVmDao;

    @Inject
    private VMTemplateDao templateDao;

    @Inject
    private AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;

    @Inject
    private InstanceBootGroupMemberDao instanceBootGroupMemberDao;

    @Inject
    private InstanceGroupVMMapDao instanceGroupVMMapDao;

    @Inject
    private InstanceBootGroupReadinessRuleDao instanceBootGroupReadinessRuleDao;

    /**
     * Rejects a VM that is a VNF appliance, currently in any AutoScale VM group, or already an
     * independent boot-group member. Used whenever a VM is being assigned to a group — a plain
     * Instance Group ({@code UserVmManagerImpl.addInstanceToGroup}) or directly to a boot group
     * ({@code InstanceBootGroupApiServiceImpl.addMemberToInstanceBootGroup}) — regardless of whether
     * that assignment has anything to do with boot groups. Does NOT look at the VM's current Instance
     * Group membership: a VM already governed by a boot group via its current group is free to move
     * to an unrelated plain group (or be removed from its group) without issue, since boot group
     * orchestration resolves a group's VMs dynamically rather than caching them. Callers that need to
     * reject a VM becoming a DIRECT boot-group member while still indirectly governed via a group
     * should additionally call {@link #validateVmNotIndirectlyBootGroupManaged(long)}.
     */
    public void validateVmEligibleForGroupMembership(long vmId) {
        UserVmVO vm = userVmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find a VM with ID: " + vmId);
        }

        VMTemplateVO template = templateDao.findByIdIncludingRemoved(vm.getTemplateId());
        if (template != null && Storage.TemplateType.VNF.equals(template.getTemplateType())) {
            throw new InvalidParameterValueException(String.format(
                    "VM %s is a VNF appliance and cannot be added to an instance group or boot group", vm));
        }

        if (UserVmManager.CKS_NODE.equals(vm.getUserVmType())) {
            throw new InvalidParameterValueException(String.format(
                    "VM %s is a CKS cluster node and cannot be added to an instance group or boot group", vm));
        }

        if (CollectionUtils.isNotEmpty(autoScaleVmGroupVmMapDao.listByVm(vmId))) {
            throw new InvalidParameterValueException(String.format(
                    "VM %s is part of an AutoScale VM group and cannot be added to an instance group or boot group", vm));
        }

        if (instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, vmId) != null) {
            throw new InvalidParameterValueException(String.format(
                    "VM %s is already an independent member of an instance boot group", vm));
        }
    }

    /**
     * Rejects a VM that is currently in an Instance Group that is itself already a boot-group
     * member — called in addition to {@link #validateVmEligibleForGroupMembership(long)} only where
     * that indirect governance would actually conflict: becoming a second, direct boot-group member,
     * or an Instance Group's own members being checked before that Instance Group itself becomes a
     * boot-group member.
     */
    public void validateVmNotIndirectlyBootGroupManaged(long vmId) {
        UserVmVO vm = userVmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find a VM with ID: " + vmId);
        }

        for (InstanceGroupVMMapVO mapping : instanceGroupVMMapDao.listByInstanceId(vmId)) {
            if (instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, mapping.getGroupId()) != null) {
                throw new InvalidParameterValueException(String.format(
                        "VM %s is currently in an instance group that is already a member of an instance boot group", vm));
            }
        }
    }

    /**
     * Rejects destroying a VM that is a direct boot-group member, or that has its own readiness
     * rule(s) registered in a boot group via an Instance Group it belongs to — either would
     * otherwise leave boot-group configuration referencing a VM that no longer exists. Being in an
     * Instance Group that is itself a boot-group member is NOT, on its own, a reason to block: boot
     * group orchestration resolves an Instance Group's VMs dynamically (via
     * {@code instanceGroupVMMapDao}) at start/stop time, so a VM leaving the group via destroy is no
     * different from any other Instance Group membership change.
     */
    public void validateVmNotInBootGroup(UserVmVO vm) {
        if (instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, vm.getId()) != null) {
            throw new InvalidParameterValueException(String.format(
                    "Instance %s is a member of an Instance Boot Group and cannot be destroyed; " +
                            "remove it from the boot group first", vm.getDisplayName()));
        }

        for (InstanceGroupVMMapVO mapping : instanceGroupVMMapDao.listByInstanceId(vm.getId())) {
            InstanceBootGroupMemberVO groupMember = instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, mapping.getGroupId());
            if (groupMember == null) {
                continue;
            }
            if (!instanceBootGroupReadinessRuleDao.listByItem(groupMember.getBootGroupId(), InstanceBootGroupMember.MemberType.VirtualMachine, vm.getId()).isEmpty()) {
                throw new InvalidParameterValueException(String.format(
                        "Instance %s has its own readiness rule(s) in an Instance Boot Group and cannot be destroyed; "
                                + "remove the readiness rule(s) first", vm.getDisplayName()));
            }
        }
    }

    /**
     * Rejects an Instance Group for boot-group membership if any VM currently in it fails
     * {@link #validateVmEligibleForGroupMembership(long)} or is already indirectly governed by
     * another boot group via some other Instance Group.
     */
    public void validateInstanceGroupEligibleForBootGroupMembership(long instanceGroupId) {
        List<InstanceGroupVMMapVO> members = instanceGroupVMMapDao.listByGroupId(instanceGroupId);
        for (InstanceGroupVMMapVO member : members) {
            validateVmEligibleForGroupMembership(member.getInstanceId());
            validateVmNotIndirectlyBootGroupManaged(member.getInstanceId());
        }
    }

    /**
     * Removes the boot-group membership row for an Instance Group being deleted, if any, so the
     * delete doesn't leave a stale member pointing at a group that no longer exists. Cascades
     * silently rather than blocking the delete, since {@code UserVmManagerImpl.deleteVmGroup} is
     * also invoked during account cleanup, where a hard failure here would be worse than the group
     * simply dropping out of its boot group.
     */
    public void removeInstanceGroupBootGroupMembershipIfPresent(long instanceGroupId) {
        InstanceBootGroupMemberVO member = instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, instanceGroupId);
        if (member != null) {
            instanceBootGroupMemberDao.expunge(member.getId());
        }
    }
}
