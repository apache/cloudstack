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

import static org.apache.cloudstack.framework.config.ConfigKey.Scope.Cluster;

import com.cloud.deploy.DeploymentPlanner;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VMInstanceVO;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.List;

/**
 * HighAvailabilityManager checks to make sure the VMs are running fine.
 */
public interface HighAvailabilityManager extends Manager {

    ConfigKey<Boolean> ForceHA = new ConfigKey<>("Advanced", Boolean.class, "force.ha", "false",
        "Force High-Availability to happen even if the VM says no.", true, Cluster);

    ConfigKey<Integer> HAWorkers = new ConfigKey<>("Advanced", Integer.class, "ha.workers", "5",
        "The number of High-Availability worker threads.", true, Cluster);

    ConfigKey<Integer> InvestigateRetryInterval = new ConfigKey<>("Advanced", Integer.class, "investigate.retry.interval",
        "60", "The time (in seconds) between VM pings when the agent is disconnected.", true, Cluster);

    ConfigKey<Integer> MigrateRetryInterval = new ConfigKey<>("Advanced", Integer.class, "migrate.retry.interval",
        "120", "The time (in seconds) between migration retries.", true, Cluster);

    ConfigKey<Integer> RestartRetryInterval = new ConfigKey<>("Advanced", Integer.class, "restart.retry.interval",
        "600", "The time (in seconds) between retries to restart a VM.", true, Cluster);

    ConfigKey<Integer> StopRetryInterval = new ConfigKey<>("Advanced", Integer.class, "stop.retry.interval",
        "600", "The time in seconds between retries to stop or destroy a VM.", true, Cluster);

    ConfigKey<Long> TimeBetweenCleanup = new ConfigKey<>("Advanced", Long.class,
        "time.between.cleanup", "86400", "The time in seconds to wait before the"
        + " cleanup thread runs for the different HA-Worker-Threads. The cleanup thread finds all the work items "
        + "that were successful and is now ready to be purged from the the database (table: op_ha_work).",
        true, Cluster);

    ConfigKey<Integer> MaxRetries = new ConfigKey<>("Advanced", Integer.class, "max.retries",
        "5", "The number of times to try a restart for the different Work-Types: "
        + "Migrating - VMs off of a host, Destroy - a VM, Stop - a VM for storage pool migration purposes,"
        + " CheckStop - checks if a VM has been stopped, ForceStop - force a VM to stop even if the "
        + "states don't allow it, Destroy - a VM and HA - restart a VM.", true, Cluster);

    ConfigKey<Long> TimeToSleep = new ConfigKey<>("Advanced", Long.class, "time.to.sleep",
        "60", "The time in seconds to sleep before checking the database (table: op_ha_work) "
        + "for new working types (Migration, Stop, CheckStop, ForceStop, Destroy and HA), if no work items are found.",
        true, Cluster);

    ConfigKey<Long> TimeBetweenFailures = new ConfigKey<>("Advanced", Long.class,
        "time.between.failures", "3600", "The time in seconds before try to cleanup all the VMs"
        + " which are registered for the HA event that were successful and are now ready to be purged.",
        true, Cluster);

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

    boolean hasPendingMigrationsWork(long vmId);
    /**
     * @return
     */
    String getHaTag();

    DeploymentPlanner getHAPlanner();
}
