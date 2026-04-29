//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  CheckNetworkCommand.class)
public final class LibvirtCheckNetworkCommandWrapper extends CommandWrapper<CheckNetworkCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final CheckNetworkCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final List<PhysicalNetworkSetupInfo> phyNics = command.getPhysicalNetworkInfoList();
        String errMsg = null;

        for (final PhysicalNetworkSetupInfo nic : phyNics) {
            if (!libvirtComputingResource.checkNetwork(Networks.TrafficType.Guest, nic.getGuestNetworkName())) {
                errMsg = "Can not find network: " + nic.getGuestNetworkName();
                break;
            } else if (!libvirtComputingResource.checkNetwork(Networks.TrafficType.Management, nic.getPrivateNetworkName())) {
                errMsg = "Can not find network: " + nic.getPrivateNetworkName();
                break;
            } else if (!libvirtComputingResource.checkNetwork(Networks.TrafficType.Public, nic.getPublicNetworkName())) {
                errMsg = "Can not find network: " + nic.getPublicNetworkName();
                break;
            }
        }

        if (errMsg != null) {
            return new CheckNetworkAnswer(command, false, errMsg);
        } else {
            return new CheckNetworkAnswer(command, true, null);
        }
    }
}
