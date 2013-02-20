package com.cloud.baremetal.manager;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.deploy.AbstractDeployPlannerSelector;
import com.cloud.deploy.DeployPlannerSelector;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.UserVmVO;
@Local(value = {DeployPlannerSelector.class})
public class BaremetalPlannerSelector extends AbstractDeployPlannerSelector{
    
    @Override
    public String selectPlanner(UserVmVO vm) {
        if (vm.getHypervisorType() == HypervisorType.BareMetal) {
            return "BareMetalPlanner";
        }
        return null;
    }

}
