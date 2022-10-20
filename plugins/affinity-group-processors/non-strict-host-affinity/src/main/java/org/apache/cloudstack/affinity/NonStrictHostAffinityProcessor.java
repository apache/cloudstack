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

import org.apache.log4j.Logger;

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

    private static final Logger s_logger = Logger.getLogger(NonStrictHostAffinityProcessor.class);
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected AffinityGroupDao _affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;
    private int _vmCapacityReleaseInterval;
    @Inject
    protected ConfigurationDao _configDao;

    @Inject
    protected VMReservationDao _reservationDao;

    @Override
    public void process(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws AffinityConflictException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        List<AffinityGroupVMMapVO> vmGroupMappings = _affinityGroupVMMapDao.findByVmIdType(vm.getId(), getType());

        for (AffinityGroupVMMapVO vmGroupMapping : vmGroupMappings) {
            if (vmGroupMapping != null) {
                processAffinityGroup(vmGroupMapping, plan, vm);
            }
        }

    }

    protected void processAffinityGroup(AffinityGroupVMMapVO vmGroupMapping, DeploymentPlan plan, VirtualMachine vm) {
        AffinityGroupVO group = _affinityGroupDao.findById(vmGroupMapping.getAffinityGroupId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Processing affinity group " + group.getName() + " for VM Id: " + vm.getId());
        }

        List<Long> groupVMIds = _affinityGroupVMMapDao.listVmIdsByAffinityGroup(group.getId());
        groupVMIds.remove(vm.getId());

        for (Long groupVMId : groupVMIds) {
            VMInstanceVO groupVM = _vmInstanceDao.findById(groupVMId);
            if (groupVM != null && !groupVM.isRemoved()) {
                if (groupVM.getHostId() != null) {
                    plan.addHostPriority(groupVM.getHostId(), DeploymentPlan.HostPriority.HIGH);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Marked host " + groupVM.getHostId() + " to as low priority, since VM " + groupVM.getId() + " is present on the host");
                    }
                } else if (Arrays.asList(VirtualMachine.State.Starting, VirtualMachine.State.Stopped).contains(groupVM.getState()) && groupVM.getLastHostId() != null) {
                    long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - groupVM.getUpdateTime().getTime()) / 1000;
                    if (secondsSinceLastUpdate < _vmCapacityReleaseInterval) {
                        plan.addHostPriority(groupVM.getLastHostId(), DeploymentPlan.HostPriority.HIGH);
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Marked host " + groupVM.getLastHostId() + " as low priority, since VM " + groupVM.getId() +
                                    " is present on the host, in Stopped state but has reserved capacity");
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _vmCapacityReleaseInterval = NumbersUtil.parseInt(_configDao.getValue(Config.CapacitySkipcountingHours.key()), 3600);
        return true;
    }

    @Override
    public boolean check(VirtualMachineProfile vmProfile, DeployDestination plannedDestination) throws AffinityConflictException {
        return true;
    }

}
