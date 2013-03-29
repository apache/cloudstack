package org.apache.cloudstack.affinity;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public class AffinityProcessorBase extends AdapterBase implements AffinityGroupProcessor {

    protected String _type;

    @Override
    public void process(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan, ExcludeList avoid)
            throws AffinityConflictException {

    }

    @Override
    public String getType() {
        return _type;
    }

    public void setType(String type) {
        _type = type;
    }
}
