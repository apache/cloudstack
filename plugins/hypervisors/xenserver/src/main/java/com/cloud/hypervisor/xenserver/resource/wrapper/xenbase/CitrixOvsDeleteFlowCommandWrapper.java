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
import com.cloud.agent.api.OvsDeleteFlowCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  OvsDeleteFlowCommand.class)
public final class CitrixOvsDeleteFlowCommandWrapper extends CommandWrapper<OvsDeleteFlowCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixOvsDeleteFlowCommandWrapper.class);

    @Override
    public Answer execute(final OvsDeleteFlowCommand command, final CitrixResourceBase citrixResourceBase) {
        citrixResourceBase.setIsOvs(true);

        final Connection conn = citrixResourceBase.getConnection();
        try {
            final Network nw = citrixResourceBase.setupvSwitchNetwork(conn);
            final String bridge = nw.getBridge(conn);
            final String result = citrixResourceBase.callHostPlugin(conn, "ovsgre", "ovs_delete_flow", "bridge", bridge, "vmName", command.getVmName());

            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(command, true, "success to delete flows for " + command.getVmName());
            } else {
                return new Answer(command, false, result);
            }
        } catch (final BadServerResponse e) {
            s_logger.error("Failed to delete flow", e);
        } catch (final XenAPIException e) {
            s_logger.error("Failed to delete flow", e);
        } catch (final XmlRpcException e) {
            s_logger.error("Failed to delete flow", e);
        }
        return new Answer(command, false, "failed to delete flow for " + command.getVmName());
    }
}
