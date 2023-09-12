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
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.OvsCreateGreTunnelAnswer;
import com.cloud.agent.api.OvsCreateGreTunnelCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  OvsCreateGreTunnelCommand.class)
public final class CitrixOvsCreateGreTunnelCommandWrapper extends CommandWrapper<OvsCreateGreTunnelCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixOvsCreateGreTunnelCommandWrapper.class);

    @Override
    public Answer execute(final OvsCreateGreTunnelCommand command, final CitrixResourceBase citrixResourceBase) {
        citrixResourceBase.setIsOvs(true);

        final Connection conn = citrixResourceBase.getConnection();
        String bridge = "unknown";
        try {
            final Network nw = citrixResourceBase.setupvSwitchNetwork(conn);
            bridge = nw.getBridge(conn);

            final String result = citrixResourceBase.callHostPlugin(conn, "ovsgre", "ovs_create_gre", "bridge", bridge, "remoteIP", command.getRemoteIp(), "greKey",
                    command.getKey(), "from", Long.toString(command.getFrom()), "to", Long.toString(command.getTo()));
            final String[] res = result.split(":");
            if (res.length != 2 || res.length == 2 && res[1].equalsIgnoreCase("[]")) {
                return new OvsCreateGreTunnelAnswer(command, false, result, citrixResourceBase.getHost().getIp(), bridge);
            } else {
                return new OvsCreateGreTunnelAnswer(command, true, result, citrixResourceBase.getHost().getIp(), bridge, Integer.parseInt(res[1]));
            }
        } catch (final BadServerResponse e) {
            s_logger.error("An error occurred while creating a GRE tunnel to " + command.getRemoteIp() + " on host " + citrixResourceBase.getHost().getIp(), e);
        } catch (final XenAPIException e) {
            s_logger.error("An error occurred while creating a GRE tunnel to " + command.getRemoteIp() + " on host " + citrixResourceBase.getHost().getIp(), e);
        } catch (final XmlRpcException e) {
            s_logger.error("An error occurred while creating a GRE tunnel to " + command.getRemoteIp() + " on host " + citrixResourceBase.getHost().getIp(), e);
        }

        return new OvsCreateGreTunnelAnswer(command, false, "EXCEPTION", citrixResourceBase.getHost().getIp(), bridge);
    }
}
