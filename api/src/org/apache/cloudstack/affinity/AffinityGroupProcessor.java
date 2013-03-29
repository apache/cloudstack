package org.apache.cloudstack.affinity;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface AffinityGroupProcessor extends Adapter {

    /**
     * process() is called to apply any user preferences to the deployment plan
     * and avoid set for the given VM placement.
     *
     * @param vm
     *            virtual machine.
     * @param plan
     *            deployment plan that tells you where it's being deployed to.
     * @param avoid
     *            avoid these data centers, pods, clusters, or hosts.
     */
    void process(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan, ExcludeList avoid)
            throws AffinityConflictException;

    /**
     * getType() should return the affinity/anti-affinity group being
     * implemented
     *
     * @return String Affinity/Anti-affinity type
     */
    String getType();
}