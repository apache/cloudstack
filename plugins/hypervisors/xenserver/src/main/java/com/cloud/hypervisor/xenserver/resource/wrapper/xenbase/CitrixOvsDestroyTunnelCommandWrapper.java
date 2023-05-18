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
import com.cloud.agent.api.OvsDestroyTunnelCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;

@ResourceWrapper(handles =  OvsDestroyTunnelCommand.class)
public final class CitrixOvsDestroyTunnelCommandWrapper extends CommandWrapper<OvsDestroyTunnelCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixOvsDestroyTunnelCommandWrapper.class);

    @Override
    public Answer execute(final OvsDestroyTunnelCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        try {
            final Network nw = citrixResourceBase.findOrCreateTunnelNetwork(conn, command.getBridgeName());
            if (nw == null) {
                s_logger.warn("Unable to find tunnel network for GRE key:" + command.getBridgeName());
                return new Answer(command, false, "No network found");
            }

            final String bridge = nw.getBridge(conn);
            final String result = citrixResourceBase.callHostPlugin(conn, "ovstunnel", "destroy_tunnel", "bridge", bridge, "in_port", command.getInPortName());

            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(command, true, result);
            } else {
                return new Answer(command, false, result);
            }
        } catch (final Exception e) {
            s_logger.warn("caught execption when destroy ovs tunnel", e);
            return new Answer(command, false, e.getMessage());
        }
    }
}
