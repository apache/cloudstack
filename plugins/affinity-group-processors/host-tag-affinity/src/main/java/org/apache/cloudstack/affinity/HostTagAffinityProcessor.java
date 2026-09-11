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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

/**
 * Soft VM-to-host placement preference: the affinity group name is treated as a host tag, and hosts
 * carrying that tag in the VM's zone have their deployment priority raised. A preference, not a
 * constraint — no host is excluded. Note: the priority channel is not honored by automatic DRS.
 */
public class HostTagAffinityProcessor extends AffinityProcessorBase implements AffinityGroupProcessor {

    @Inject
    protected AffinityGroupDao affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao affinityGroupVMMapDao;
    @Inject
    protected HostDao hostDao;

    @Override
    public void process(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, List<VirtualMachine> vmList) throws AffinityConflictException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        List<AffinityGroupVMMapVO> vmGroupMappings = affinityGroupVMMapDao.findByVmIdType(vm.getId(), getType());

        for (AffinityGroupVMMapVO vmGroupMapping : vmGroupMappings) {
            if (vmGroupMapping != null) {
                processAffinityGroup(vmGroupMapping, plan, vm);
            }
        }
    }

    protected void processAffinityGroup(AffinityGroupVMMapVO vmGroupMapping, DeploymentPlan plan, VirtualMachine vm) {
        AffinityGroupVO group = affinityGroupDao.findById(vmGroupMapping.getAffinityGroupId());
        if (group == null || StringUtils.isBlank(group.getName())) {
            return;
        }

        String hostTag = group.getName();
        List<HostVO> preferredHosts = hostDao.listByHostTag(Host.Type.Routing, null, null, vm.getDataCenterId(), hostTag);
        if (CollectionUtils.isEmpty(preferredHosts)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("No hosts carry tag [%s] in zone %s for VM %s; host-tag affinity is a no-op.",
                        hostTag, vm.getDataCenterId(), vm));
            }
            return;
        }

        for (HostVO host : preferredHosts) {
            Integer priority = adjustHostPriority(plan, host.getId());
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Raised host %s priority to %s (VM %s prefers hosts tagged [%s]).",
                        host.getId(), priority, vm, hostTag));
            }
        }
    }

    protected Integer adjustHostPriority(DeploymentPlan plan, Long hostId) {
        plan.adjustHostPriority(hostId, DeploymentPlan.HostPriorityAdjustment.HIGHER);
        return plan.getHostPriorities().get(hostId);
    }
}
