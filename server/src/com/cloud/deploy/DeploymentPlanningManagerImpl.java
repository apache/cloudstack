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
package com.cloud.deploy;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.affinity.AffinityGroupProcessor;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.log4j.Logger;

import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { DeploymentPlanningManager.class })
public class DeploymentPlanningManagerImpl extends ManagerBase implements DeploymentPlanningManager, Manager {

    private static final Logger s_logger = Logger.getLogger(DeploymentPlanningManagerImpl.class);
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected AffinityGroupDao _affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;

    protected List<DeploymentPlanner> _planners;
    public List<DeploymentPlanner> getPlanners() {
        return _planners;
    }
    public void setPlanners(List<DeploymentPlanner> _planners) {
        this._planners = _planners;
    }

    protected List<AffinityGroupProcessor> _affinityProcessors;
    public List<AffinityGroupProcessor> getAffinityGroupProcessors() {
        return _affinityProcessors;
    }
    public void setAffinityGroupProcessors(List<AffinityGroupProcessor> affinityProcessors) {
        this._affinityProcessors = affinityProcessors;
    }

    @Override
    public DeployDestination planDeployment(VirtualMachineProfile<? extends VirtualMachine> vmProfile,
            DeploymentPlan plan, ExcludeList avoids) throws InsufficientServerCapacityException,
            AffinityConflictException {

        // call affinitygroup chain
        VirtualMachine vm = vmProfile.getVirtualMachine();
        long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        if (vmGroupCount > 0) {
            for (AffinityGroupProcessor processor : _affinityProcessors) {
                processor.process(vmProfile, plan, avoids);
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deploy avoids pods: " + avoids.getPodsToAvoid() + ", clusters: "
                    + avoids.getClustersToAvoid() + ", hosts: " + avoids.getHostsToAvoid());
        }

        // call planners
        DeployDestination dest = null;
        for (DeploymentPlanner planner : _planners) {
            if (planner.canHandle(vmProfile, plan, avoids)) {
                dest = planner.plan(vmProfile, plan, avoids);
            } else {
                continue;
            }
            if (dest != null) {
                avoids.addHost(dest.getHost().getId());
                break;
            }

        }
        return dest;
    }

}
