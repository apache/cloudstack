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
import com.cloud.agent.api.OvsDestroyBridgeCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;

@ResourceWrapper(handles =  OvsDestroyBridgeCommand.class)
public final class CitrixOvsDestroyBridgeCommandWrapper extends CommandWrapper<OvsDestroyBridgeCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixOvsDestroyBridgeCommandWrapper.class);

    @Override
    public Answer execute(final OvsDestroyBridgeCommand command, final CitrixResourceBase citrixResourceBase) {
        try {
            final Connection conn = citrixResourceBase.getConnection();

            final Network nw = citrixResourceBase.findOrCreateTunnelNetwork(conn, command.getBridgeName());
            citrixResourceBase.cleanUpTmpDomVif(conn, nw);

            citrixResourceBase.destroyTunnelNetwork(conn, nw, command.getHostId());

            s_logger.debug("OVS Bridge destroyed");

            return new Answer(command, true, null);
        } catch (final Exception e) {
            s_logger.warn("caught execption when destroying ovs bridge", e);
            return new Answer(command, false, e.getMessage());
        }
    }
}
