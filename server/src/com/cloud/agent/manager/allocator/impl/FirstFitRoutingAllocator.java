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

package com.cloud.agent.manager.allocator.impl;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Local;
import org.apache.log4j.NDC;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value={HostAllocator.class})
public class FirstFitRoutingAllocator extends FirstFitAllocator {
    @Override
    public List<Host> allocateTo(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, Type type,
			ExcludeList avoid, int returnUpTo) {
        try {
            NDC.push("FirstFitRoutingAllocator");
            if (type != Host.Type.Routing) {
                // FirstFitRoutingAllocator is to find space on routing capable hosts only
                return new ArrayList<Host>();
            }
            //all hosts should be of type routing anyway.
            return super.allocateTo(vmProfile, plan, type, avoid, returnUpTo);
        } finally {
            NDC.pop();
        }
    }
}
