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
package com.cloud.resource.hypervisor;

import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.Start2Answer;
import com.cloud.agent.api.Start2Command;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.resource.ServerResource;

/**
 * HypervisorResource specifies all of the commands a hypervisor agent needs
 * to implement in order to interface with CloudStack.
 *
 */
public interface HypervisorResource extends ServerResource {
    /**
     * Starts a VM.  All information regarding the VM
     * are carried within the command.
     * @param cmd carries all the information necessary to start a VM
     * @return Start2Answer answer.
     */
    Start2Answer execute(Start2Command cmd);
    
    /**
     * Stops a VM.  Must return true as long as the VM does not exist.
     * @param cmd information necessary to identify the VM to stop.
     * @return StopAnswer 
     */
    StopAnswer execute(StopCommand cmd);
    
    /**
     * Reboots a VM.
     * @param cmd information necessary to identify the VM to reboot.
     * @return RebootAnswer
     */
    RebootAnswer execute(RebootCommand cmd);
}
