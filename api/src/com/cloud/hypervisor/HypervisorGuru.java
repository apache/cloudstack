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
package com.cloud.hypervisor;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface HypervisorGuru extends Adapter {
    HypervisorType getHypervisorType();
    
    /**
     * Convert from a virtual machine to the
     * virtual machine that the hypervisor expects. 
     * @param vm 
     * @return
     */
    <T extends VirtualMachine> VirtualMachineTO implement(VirtualMachineProfile<T> vm);
    
    /**
     * Give hypervisor guru opportunity to decide if certain command needs to be delegated to other host, mainly to secondary storage VM host
     * @param hostId original hypervisor host
     * @param cmd command that is going to be sent, hypervisor guru usually needs to register various context objects into the command object
     * 
     * @return delegated host id if the command will be delegated
     */
    long getCommandHostDelegation(long hostId, Command cmd);
    
    /**
     *  @return true if VM can be migrated independently with CloudStack, and therefore CloudStack needs to track and reflect host change
     *  into CloudStack database, false if CloudStack enforces VM sync logic
     *  
     */
    boolean trackVmHostChange();
}
