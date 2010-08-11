/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.ha;

import java.util.List;

import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;

/**
 * HighAvailabilityManager checks to make sure the VMs are running fine.
 */
public interface HighAvailabilityManager extends Manager {

    enum Step {
        Scheduled,
        Investigating,
        Fencing,
        Stopping,
        Restarting,
        Preparing,
        Migrating,
        Checking,
        Cancelled,
        Done,
        Error,
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
    void scheduleRestart(final VMInstanceVO vm, boolean investigate);

    void cancelDestroy(VMInstanceVO vm, Long hostId);
    
    void scheduleDestroy(VMInstanceVO vm, long hostId);
    
    /**
     * Schedule restarts for all vms running on the host.
     * @param host host.
     */
    void scheduleRestartForVmsOnHost(final HostVO host);

    /**
     * Register a handler to take care of a VM.  If a handler for a certain
     * type of VM is not handled, then it is not part of the HA process.
     * 
     * @param type virtual machine type.
     * @param handler handler that can handle starting and stopping the machine.
     */
    void registerHandler(final VirtualMachine.Type type, final VirtualMachineManager<? extends VMInstanceVO> handler);

    /**
     * Unregisters a handler.  Not likely called but here for completeness.
     * @param type virtual machine type to unregister.
     */
    void unregisterHandler(final VirtualMachine.Type type);

    /**
     * Schedule the vm for migration.
     * 
     * @param vm
     * @return true if schedule worked.
     */
    boolean scheduleMigration(final VMInstanceVO vm);
    
    List<VMInstanceVO> findTakenMigrationWork();

    /**
     * Stops a VM.
     * 
     * @param vm virtual machine to stop.
     * @param host host the virtual machine is on.
     * @param verifyHost make sure it is the same host as the schedule time.
     */
    void scheduleStop(final VMInstanceVO vm, long hostId, boolean verifyHost);

    void cancelScheduledMigrations(HostVO host);
}
