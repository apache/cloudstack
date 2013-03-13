package com.cloud.deploy;

import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface DeploymentPlanningManager extends Manager {

    /**
     * Manages vm deployment stages: First Process Affinity/Anti-affinity - Call
     * the chain of AffinityGroupProcessor adapters to set deploymentplan scope
     * and exclude list Secondly, Call DeploymentPlanner - to use heuristics to
     * find the best spot to place the vm/volume. Planner will drill down to the
     * write set of clusters to look for placement based on various heuristics.
     * Lastly, Call Allocators - Given a cluster, allocators matches the
     * requirements to capabilities of the physical resource (host, storage
     * pool).
     *
     * @throws AffinityConflictException
     *
     *
     *
     */
    DeployDestination planDeployment(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan,
            ExcludeList avoids) throws InsufficientServerCapacityException, AffinityConflictException;
}
