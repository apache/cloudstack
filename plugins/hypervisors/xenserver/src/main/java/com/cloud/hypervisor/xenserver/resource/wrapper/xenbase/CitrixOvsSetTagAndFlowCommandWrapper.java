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
import com.cloud.agent.api.OvsSetTagAndFlowAnswer;
import com.cloud.agent.api.OvsSetTagAndFlowCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  OvsSetTagAndFlowCommand.class)
public final class CitrixOvsSetTagAndFlowCommandWrapper extends CommandWrapper<OvsSetTagAndFlowCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixOvsSetTagAndFlowCommandWrapper.class);

    @Override
    public Answer execute(final OvsSetTagAndFlowCommand command, final CitrixResourceBase citrixResourceBase) {
        citrixResourceBase.setIsOvs(true);

        final Connection conn = citrixResourceBase.getConnection();
        try {
            final Network nw = citrixResourceBase.setupvSwitchNetwork(conn);
            final String bridge = nw.getBridge(conn);

            /*
             * If VM is domainRouter, this will try to set flow and tag on its
             * none guest network nic. don't worry, it will fail silently at
             * host plugin side
             */
            final String result = citrixResourceBase.callHostPlugin(conn, "ovsgre", "ovs_set_tag_and_flow", "bridge", bridge, "vmName", command.getVmName(), "tag",
                    command.getTag(), "vlans", command.getVlans(), "seqno", command.getSeqNo());
            s_logger.debug("set flow for " + command.getVmName() + " " + result);

            if (result != null && result.equalsIgnoreCase("SUCCESS")) {
                return new OvsSetTagAndFlowAnswer(command, true, result);
            } else {
                return new OvsSetTagAndFlowAnswer(command, false, result);
            }
        } catch (final BadServerResponse e) {
            s_logger.error("Failed to set tag and flow", e);
        } catch (final XenAPIException e) {
            s_logger.error("Failed to set tag and flow", e);
        } catch (final XmlRpcException e) {
            s_logger.error("Failed to set tag and flow", e);
        }

        return new OvsSetTagAndFlowAnswer(command, false, "EXCEPTION");
    }
}
