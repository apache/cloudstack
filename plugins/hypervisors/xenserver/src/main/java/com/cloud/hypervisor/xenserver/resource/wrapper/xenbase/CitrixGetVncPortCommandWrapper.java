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

import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  GetVncPortCommand.class)
public final class CitrixGetVncPortCommandWrapper extends CommandWrapper<GetVncPortCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixGetVncPortCommandWrapper.class);

    @Override
    public Answer execute(final GetVncPortCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, command.getName());
            if (vms.size() == 1) {
                String consoleurl;
                consoleurl = "consoleurl=" + citrixResourceBase.getVncUrl(conn, vms.iterator().next()) + "&" + "sessionref=" + conn.getSessionReference();
                return new GetVncPortAnswer(command, consoleurl, -1);
            } else {
                return new GetVncPortAnswer(command, "There are " + vms.size() + " VMs named " + command.getName());
            }
        } catch (final Exception e) {
            final String msg = "Unable to get vnc port due to " + e.toString();
            s_logger.warn(msg, e);
            return new GetVncPortAnswer(command, msg);
        }
    }
}
