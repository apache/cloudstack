package com.cloud.deploy;

import javax.ejb.Local;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.UserVmVO;

@Local(value = {DeployPlannerSelector.class})
public class HypervisorVmPlannerSelector extends AbstractDeployPlannerSelector {
    @Override
    public String selectPlanner(UserVmVO vm) {
        if (vm.getHypervisorType() != HypervisorType.BareMetal) {
            return "FirstFitPlanner";
        }
        return null;
    }
}
