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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  RebootRouterCommand.class)
public final class LibvirtRebootRouterCommandWrapper extends CommandWrapper<RebootRouterCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final RebootRouterCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();

        final RebootCommand rebootCommand = new RebootCommand(command.getVmName(), true);
        final Answer answer = wrapper.execute(rebootCommand, libvirtComputingResource);

        final VirtualRoutingResource virtualRouterResource = libvirtComputingResource.getVirtRouterResource();
        if (virtualRouterResource.connect(command.getPrivateIpAddress())) {
            libvirtComputingResource.networkUsage(command.getPrivateIpAddress(), "create", null);

            return answer;
        } else {
            return new Answer(command, false, "Failed to connect to virtual router " + command.getVmName());
        }
    }
}
