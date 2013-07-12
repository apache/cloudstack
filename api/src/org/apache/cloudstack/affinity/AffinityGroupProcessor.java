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

import com.cloud.deploy.DeployDestination;
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

    /**
     * check() is called to see if the planned destination fits the group
     * requirements
     *
     * @param vm
     *            virtual machine.
     * @param plannedDestination
     *            deployment destination where VM is planned to be deployed
     */
    boolean check(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination plannedDestination)
            throws AffinityConflictException;
}