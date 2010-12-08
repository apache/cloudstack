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
package com.cloud.vm;

import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.utils.exception.ExecutionException;

/**
 * A VirtualMachineGuru knows how to process a certain type of virtual machine.
 *
 */
public interface VirtualMachineGuru<T extends VirtualMachine> {
    /**
     * Find the virtual machine by name.
     * @param name
     * @return virtual machine.
     */
    T findByName(String name);
    
    T findById(long id);
    
    T persist(T vm);
    
    boolean finalizeVirtualMachineProfile(VirtualMachineProfile<T> profile, DeployDestination dest, ReservationContext context);
    
    /**
     * finalize the virtual machine deployment.
     * @param cmds commands that were created.
     * @param profile virtual machine profile.
     * @param dest destination to send the command.
     * @return true if everything checks out.  false if not and we should try again.
     */
    boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<T> profile, DeployDestination dest, ReservationContext context);
    
    /**
     * Check the deployment results.
     * @param cmds commands and answers that were sent.
     * @param profile virtual machine profile.
     * @param dest destination it was sent to.
     * @return true if deployment was fine; false if it didn't go well.
     */
    boolean finalizeStart(Commands cmds, VirtualMachineProfile<T> profile, DeployDestination dest, ReservationContext context);
    
    void finalizeStop(VirtualMachineProfile<T> profile, long hostId, String reservationId);
    /**
     * Returns the id parsed from the name.  If it cannot parse the name,
     * then return null.  This method is used to determine if this is
     * the right handler for this vm.
     * 
     * @param vmName vm name coming form the agent.
     * @return id if the handler works for this vm and can parse id.  null if not.
     */
    Long convertToId(String vmName);
    
    /**
     * Retrieves the vm based on the id given.
     * 
     * @param id id of the vm.
     * @return VMInstanceVO
     */
    T get(long id);
    
    /**
     * Complete the start command.  HA calls this when it determines that
     * a vm was starting but the state was not complete.
     * 
     * @param vm vm to execute this on.
     */
    void completeStartCommand(T vm);
    
    /**
     * Complete the stop command.  HA calls this when it determines that
     * a vm was being stopped but it didn't complete.
     * 
     * @param vm vm to stop.
     */
    void completeStopCommand(T vm);
    
    /**
     * start the vm
     * 
     * @param vm to start.
     * @return true if started.  false if not.
     * @throws InsufficientCapacityException if there's not enough capacity to start the vm.
     * @throws StorageUnavailableException if the storage is unavailable.
     * @throws ConcurrentOperationException there's multiple threads working on this vm.
     * @throws ExecutionException 
     * @throws ResourceUnavailableException 
     */
    T start(long vmId, long startEventId) throws InsufficientCapacityException, StorageUnavailableException, ConcurrentOperationException, ExecutionException;

    /**
     * stop the vm
     * 
     * @param vm vm to Stop.
     * @return true if stopped and false if not.
     * @throws AgentUnavailableException if the agent is unavailable.
     */
    boolean stop(T vm, long startEventId) throws AgentUnavailableException;
    
    /**
     * Produce a cleanup command to be sent to the agent to cleanup anything
     * out of the ordinary.
     * @param vm vm to cleanup.  It's possible this is null.
     * @param vmName name of the vm from the agent.
     * @return Command to clean it up.  If not cleanup is needed, then return null.
     */
    Command cleanup(T vm, String vmName);
    
    /**
     * Prepare for migration.
     * 
     * @param vm vm to migrate.
     * @return HostVO if a host is found.
     */
    HostVO prepareForMigration(T vm) throws InsufficientCapacityException, StorageUnavailableException;
    
    /**
     * Migrate the vm.
     */
    boolean migrate(T vm, HostVO host) throws AgentUnavailableException, OperationTimedoutException;
    
    boolean completeMigration(T vm, HostVO host) throws AgentUnavailableException, OperationTimedoutException;
    
    boolean destroy(T vm) throws AgentUnavailableException;
}
