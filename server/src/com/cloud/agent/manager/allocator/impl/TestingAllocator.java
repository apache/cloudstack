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
import java.util.Map;

import javax.inject.Inject;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public class TestingAllocator extends AdapterBase implements HostAllocator {
    @Inject
    HostDao _hostDao;
    Long _computingHost;
    Long _storageHost;
    Long _routingHost;

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo) {
        return allocateTo(vmProfile, plan, type, avoid, returnUpTo, true);
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
        boolean considerReservedCapacity) {
        return allocateTo(vmProfile, plan, type, avoid, returnUpTo, considerReservedCapacity);
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo, boolean considerReservedCapacity) {
        List<Host> availableHosts = new ArrayList<Host>();
        Host host = null;
        if (type == Host.Type.Routing && _routingHost != null) {
            host = _hostDao.findById(_routingHost);
        } else if (type == Host.Type.Storage && _storageHost != null) {
            host = _hostDao.findById(_storageHost);
        }
        if (host != null) {
            availableHosts.add(host);
        }
        return availableHosts;
    }

    @Override
    public boolean isVirtualMachineUpgradable(VirtualMachine vm, ServiceOffering offering) {
        // currently we do no special checks to rule out a VM being upgradable to an offering, so
        // return true
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        String value = (String)params.get(Host.Type.Routing.toString());
        _routingHost = (value != null) ? Long.parseLong(value) : null;

        value = (String)params.get(Host.Type.Storage.toString());
        _storageHost = (value != null) ? Long.parseLong(value) : null;

        return true;
    }
}
