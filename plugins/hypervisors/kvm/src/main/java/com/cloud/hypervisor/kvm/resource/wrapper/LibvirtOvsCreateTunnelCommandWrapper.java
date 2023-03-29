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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.OvsCreateTunnelAnswer;
import com.cloud.agent.api.OvsCreateTunnelCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = OvsCreateTunnelCommand.class)
public final class LibvirtOvsCreateTunnelCommandWrapper extends CommandWrapper<OvsCreateTunnelCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtOvsCreateTunnelCommandWrapper.class);

    @Override
    public Answer execute(final OvsCreateTunnelCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String bridge = command.getNetworkName();
        try {
            if (!libvirtComputingResource.findOrCreateTunnelNetwork(bridge)) {
                s_logger.debug("Error during bridge setup");
                return new OvsCreateTunnelAnswer(command, false, "Cannot create network", bridge);
            }

            libvirtComputingResource.configureTunnelNetwork(command.getNetworkId(), command.getFrom(), command.getNetworkName());
            final Script scriptCommand = new Script(libvirtComputingResource.getOvsTunnelPath(), libvirtComputingResource.getTimeout(), s_logger);
            scriptCommand.add("create_tunnel");
            scriptCommand.add("--bridge", bridge);
            scriptCommand.add("--remote_ip", command.getRemoteIp());
            scriptCommand.add("--key", command.getKey().toString());
            scriptCommand.add("--src_host", command.getFrom().toString());
            scriptCommand.add("--dst_host", command.getTo().toString());

            final String result = scriptCommand.execute();
            if (result != null) {
                return new OvsCreateTunnelAnswer(command, true, result, null, bridge);
            } else {
                return new OvsCreateTunnelAnswer(command, false, result, bridge);
            }
        } catch (final Exception e) {
            s_logger.warn("Caught execption when creating ovs tunnel", e);
            return new OvsCreateTunnelAnswer(command, false, e.getMessage(), bridge);
        }
    }
}
