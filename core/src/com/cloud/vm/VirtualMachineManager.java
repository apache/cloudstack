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
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.utils.exception.ExecutionException;

/**
 * HighAvailabilityHandler specifies the methods that are used to control
 * VMs during the sync process and the HA process.  While different types of
 * VMs have a lot in common, they allocate resources differently and it
 * doesn't make sense to
 *
 */
public interface VirtualMachineManager<T extends VMInstanceVO> {

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
