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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.OvsCreateTunnelAnswer;
import com.cloud.agent.api.OvsCreateTunnelCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;

@ResourceWrapper(handles =  OvsCreateTunnelCommand.class)
public final class CitrixOvsCreateTunnelCommandWrapper extends CommandWrapper<OvsCreateTunnelCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixOvsCreateTunnelCommandWrapper.class);

    @Override
    public Answer execute(final OvsCreateTunnelCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        String bridge = "unknown";
        try {
            final Network nw = citrixResourceBase.findOrCreateTunnelNetwork(conn, command.getNetworkName());
            if (nw == null) {
                s_logger.debug("Error during bridge setup");
                return new OvsCreateTunnelAnswer(command, false, "Cannot create network", bridge);
            }

            citrixResourceBase.configureTunnelNetwork(conn, command.getNetworkId(), command.getFrom(), command.getNetworkName());
            bridge = nw.getBridge(conn);
            final String result =
                    citrixResourceBase.callHostPlugin(conn, "ovstunnel", "create_tunnel", "bridge", bridge, "remote_ip", command.getRemoteIp(),
                            "key", command.getKey().toString(), "from",
                            command.getFrom().toString(), "to", command.getTo().toString(), "cloudstack-network-id",
                            command.getNetworkUuid());
            final String[] res = result.split(":");

            if (res.length == 2 && res[0].equalsIgnoreCase("SUCCESS")) {
                return new OvsCreateTunnelAnswer(command, true, result, res[1], bridge);
            } else {
                return new OvsCreateTunnelAnswer(command, false, result, bridge);
            }
        } catch (final Exception e) {
            s_logger.debug("Error during tunnel setup");
            s_logger.warn("Caught execption when creating ovs tunnel", e);
            return new OvsCreateTunnelAnswer(command, false, e.getMessage(), bridge);
        }
    }
}
