/**
 * 
 */
package com.cloud.deploy;

import java.util.Set;

import com.cloud.utils.component.Adapter;
import com.cloud.vm.VirtualMachineProfile;

public interface DeploymentPlanner extends Adapter {
    DeployDestination plan(VirtualMachineProfile vm, DeploymentPlan plan, Set<DeployDestination> avoid);
}
