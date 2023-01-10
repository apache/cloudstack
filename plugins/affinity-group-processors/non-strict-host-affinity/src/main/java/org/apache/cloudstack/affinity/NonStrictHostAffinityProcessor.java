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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMReservationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.configuration.Config;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class NonStrictHostAffinityProcessor extends AffinityProcessorBase implements AffinityGroupProcessor {

    @Inject
    protected UserVmDao vmDao;
    @Inject
    protected VMInstanceDao vmInstanceDao;
    @Inject
    protected AffinityGroupDao affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao affinityGroupVMMapDao;
    @Inject
    protected ConfigurationDao configDao;

    @Inject
    protected VMReservationDao reservationDao;

    private int vmCapacityReleaseInterval;

    @Override
    public void process(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws AffinityConflictException {
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

        if (logger.isDebugEnabled()) {
            logger.debug("Processing affinity group " + group.getName() + " for VM Id: " + vm.getId());
        }

        List<Long> groupVMIds = affinityGroupVMMapDao.listVmIdsByAffinityGroup(group.getId());
        groupVMIds.remove(vm.getId());

        for (Long groupVMId : groupVMIds) {
            VMInstanceVO groupVM = vmInstanceDao.findById(groupVMId);
            if (groupVM != null && !groupVM.isRemoved()) {
                processVmInAffinityGroup(plan, groupVM);
            }
        }
    }

    protected void processVmInAffinityGroup(DeploymentPlan plan, VMInstanceVO groupVM) {
        if (groupVM.getHostId() != null) {
            Integer priority = adjustHostPriority(plan, groupVM.getHostId());
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Updated host %s priority to %s , since VM %s is present on the host",
                        groupVM.getHostId(), priority, groupVM.getId()));
            }
        } else if (Arrays.asList(VirtualMachine.State.Starting, VirtualMachine.State.Stopped).contains(groupVM.getState()) && groupVM.getLastHostId() != null) {
            long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - groupVM.getUpdateTime().getTime()) / 1000;
            if (secondsSinceLastUpdate < vmCapacityReleaseInterval) {
                Integer priority = adjustHostPriority(plan, groupVM.getLastHostId());
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Updated host %s priority to %s , since VM %s" +
                            " is present on the host, in %s state but has reserved capacity",
                            groupVM.getLastHostId(), priority, groupVM.getId(), groupVM.getState()));
                }
            }
        }
    }

    protected Integer adjustHostPriority(DeploymentPlan plan, Long hostId) {
        plan.adjustHostPriority(hostId, DeploymentPlan.HostPriorityAdjustment.HIGHER);
        return plan.getHostPriorities().get(hostId);
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        vmCapacityReleaseInterval = NumbersUtil.parseInt(configDao.getValue(Config.CapacitySkipcountingHours.key()), 3600);
        return true;
    }

    @Override
    public boolean check(VirtualMachineProfile vmProfile, DeployDestination plannedDestination) throws AffinityConflictException {
        return true;
    }

}
