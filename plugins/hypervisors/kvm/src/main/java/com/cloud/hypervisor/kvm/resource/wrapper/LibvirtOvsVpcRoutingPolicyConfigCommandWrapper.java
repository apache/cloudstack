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
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  OvsVpcRoutingPolicyConfigCommand.class)
public final class LibvirtOvsVpcRoutingPolicyConfigCommandWrapper extends CommandWrapper<OvsVpcRoutingPolicyConfigCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final OvsVpcRoutingPolicyConfigCommand command, final LibvirtComputingResource libvirtComputingResource) {
        try {
            final Script scriptCommand = new Script(libvirtComputingResource.getOvsTunnelPath(), libvirtComputingResource.getTimeout(), logger);
            scriptCommand.add("configure_ovs_bridge_for_routing_policies");
            scriptCommand.add("--bridge", command.getBridgeName());
            scriptCommand.add("--config", command.getVpcConfigInJson());

            final String result = scriptCommand.execute();
            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(command, true, result);
            } else {
                return new Answer(command, false, result);
            }
        } catch  (final Exception e) {
            logger.warn("caught exception while updating host with latest VPC topology", e);
            return new Answer(command, false, e.getMessage());
        }
    }
}
