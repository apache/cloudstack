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
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.InstanceBootGroupMemberDao;
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

    /**
     * Rejects a VM that is a VNF appliance, currently in any AutoScale VM group, already an
     * independent boot-group member, or currently in an Instance Group that is itself already a
     * boot-group member. Used both when adding a VM to a plain Instance Group and when adding it
     * directly to a boot group.
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

        if (CollectionUtils.isNotEmpty(autoScaleVmGroupVmMapDao.listByVm(vmId))) {
            throw new InvalidParameterValueException(String.format(
                    "VM %s is part of an AutoScale VM group and cannot be added to an instance group or boot group", vm));
        }

        if (instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, vmId) != null) {
            throw new InvalidParameterValueException(String.format(
                    "VM %s is already an independent member of an instance boot group", vm));
        }

        List<InstanceGroupVMMapVO> currentMappings = instanceGroupVMMapDao.listByInstanceId(vmId);
        if (CollectionUtils.isNotEmpty(currentMappings)) {
            long currentGroupId = currentMappings.get(0).getGroupId();
            if (instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, currentGroupId) != null) {
                throw new InvalidParameterValueException(String.format(
                        "VM %s is currently in an instance group that is already a member of an instance boot group", vm));
            }
        }
    }

    /**
     * Rejects an Instance Group for boot-group membership if any VM currently in it fails
     * {@link #validateVmEligibleForGroupMembership(long)}.
     */
    public void validateInstanceGroupEligibleForBootGroupMembership(long instanceGroupId) {
        List<InstanceGroupVMMapVO> members = instanceGroupVMMapDao.listByGroupId(instanceGroupId);
        for (InstanceGroupVMMapVO member : members) {
            validateVmEligibleForGroupMembership(member.getInstanceId());
        }
    }
}
