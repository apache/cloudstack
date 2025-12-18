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
package com.cloud.agent.manager.allocator.impl;

import java.util.ArrayList;
import java.util.List;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.logging.log4j.ThreadContext;

public class FirstFitRoutingAllocator extends FirstFitAllocator {
    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo) {
        try {
            ThreadContext.push("FirstFitRoutingAllocator");
            if (type != Host.Type.Routing) {
                // FirstFitRoutingAllocator is to find space on routing capable hosts only
                return new ArrayList<Host>();
            }
            //all hosts should be of type routing anyway.
            return super.allocateTo(vmProfile, plan, type, avoid, returnUpTo);
        } finally {
            ThreadContext.pop();
        }
    }
}
