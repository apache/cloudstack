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
package com.cloud.ha;

import java.util.List;

import com.cloud.deploy.DeploymentPlanner;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VMInstanceVO;

/**
 * HighAvailabilityManager checks to make sure the VMs are running fine.
 */
public interface HighAvailabilityManager extends Manager {
    public enum WorkType {
        Migration,  // Migrating VMs off of a host.
        Stop,       // Stops a VM for storage pool migration purposes.  This should be obsolete now.
        CheckStop,  // Checks if a VM has been stopped.
        ForceStop,  // Force a VM to stop even if the states don't allow it.  Use this only if you know the VM is stopped on the physical hypervisor.
        Destroy,    // Destroy a VM.
        HA;         // Restart a VM.
    }

    enum Step {
        Scheduled, Investigating, Fencing, Stopping, Restarting, Migrating, Cancelled, Done, Error,
    }

    /**
     * Investigate why a host has disconnected and migrate the VMs on it
     * if necessary.
     *
     * @param host - the host that has disconnected.
     */
    Status investigate(long hostId);

    /**
     * Restart a vm that has gone away due to various reasons.  Whether a
     * VM is restarted depends on various reasons.
     *   1. Is the VM really dead.  This method will try to find out.
     *   2. Is the VM HA enabled?  If not, the VM is simply stopped.
     *
     * All VMs that enter HA mode is not allowed to be operated on until it
     * has been determined that the VM is dead.
     *
     * @param vm the vm that has gone away.
     * @param investigate must be investigated before we do anything with this vm.
     */
    void scheduleRestart(VMInstanceVO vm, boolean investigate);

    void cancelDestroy(VMInstanceVO vm, Long hostId);

    void scheduleDestroy(VMInstanceVO vm, long hostId);

    /**
     * Schedule restarts for all vms running on the host.
     * @param host host.
     * @param investigate TODO
     */
    void scheduleRestartForVmsOnHost(HostVO host, boolean investigate);

    /**
     * Schedule the vm for migration.
     *
     * @param vm
     * @return true if schedule worked.
     */
    boolean scheduleMigration(VMInstanceVO vm);

    List<VMInstanceVO> findTakenMigrationWork();

    /**
     * Schedules a work item to stop a VM.  This method schedules a work
     * item to do one of three things.
     *
     * 1. Perform a regular stop of a VM: WorkType.Stop
     * 2. Perform a force stop of a VM: WorkType.ForceStop
     * 3. Check if a VM has been stopped: WorkType.CheckStop
     *
     * @param vm virtual machine to stop.
     * @param host host the virtual machine is on.
     * @param type which type of stop is requested.
     */
    void scheduleStop(VMInstanceVO vm, long hostId, WorkType type);

    void cancelScheduledMigrations(HostVO host);

    boolean hasPendingHaWork(long vmId);

    /**
     * @return
     */
    String getHaTag();

    DeploymentPlanner getHAPlanner();
}
