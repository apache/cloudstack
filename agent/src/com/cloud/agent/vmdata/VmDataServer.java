/**
 *  Copyright (C) 2011 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.agent.vmdata;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.utils.component.Manager;

/**
 * Maintains vm data (user data, meta-data, password) that can be fetched via HTTP
 * by user vms
 *
 */
public interface VmDataServer extends Manager {

    public Answer handleVmDataCommand(VmDataCommand cmd);

    public void handleVmStarted(VirtualMachineTO vm);

    public void handleVmStopped(String vmName);
}
