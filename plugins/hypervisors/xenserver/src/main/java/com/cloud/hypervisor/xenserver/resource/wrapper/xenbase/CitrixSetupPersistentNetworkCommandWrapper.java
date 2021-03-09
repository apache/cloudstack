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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;


import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.SetupPersistentNetworkAnswer;
import com.cloud.agent.api.SetupPersistentNetworkCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.hypervisor.xenserver.resource.XsHost;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;

@ResourceWrapper(handles = SetupPersistentNetworkCommand.class)
public class CitrixSetupPersistentNetworkCommandWrapper extends CommandWrapper<SetupPersistentNetworkCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixSetupPersistentNetworkCommandWrapper.class);

    @Override
    public Answer execute(SetupPersistentNetworkCommand command, CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final XsHost host = citrixResourceBase.getHost();
        try {
            Network network = citrixResourceBase.getNetwork(conn, command.getNic());
            if (network == null) {
                return new SetupPersistentNetworkAnswer(command, false, "Failed to setup network on host: "+ host.getIp());
            }
            return new SetupPersistentNetworkAnswer(command, true, "Successfully setup network on host: "+ host.getIp());
        } catch (final Exception e) {
            final String msg = " Failed to setup network on host: " + host.getIp() + " due to: " + e.toString();
            s_logger.error(msg, e);
            return new SetupPersistentNetworkAnswer(command, false, msg);
        }
    }
}
