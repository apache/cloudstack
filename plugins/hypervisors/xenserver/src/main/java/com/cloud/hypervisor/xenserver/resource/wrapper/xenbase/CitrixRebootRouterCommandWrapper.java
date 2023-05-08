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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;

@ResourceWrapper(handles =  RebootRouterCommand.class)
public final class CitrixRebootRouterCommandWrapper extends CommandWrapper<RebootRouterCommand, Answer, CitrixResourceBase> {

    @Override
    public Answer execute(final RebootRouterCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();

        final RebootCommand rebootCommand = new RebootCommand(command.getVmName(), true);
        final Answer answer = wrapper.execute(rebootCommand, citrixResourceBase);

        if (answer.getResult()) {
            final String cnct = citrixResourceBase.connect(conn, command.getVmName(), command.getPrivateIpAddress(), 0);
            citrixResourceBase.networkUsage(conn, command.getPrivateIpAddress(), "create", null);

            if (cnct == null) {
                return answer;
            } else {
                return new Answer(command, false, cnct);
            }
        }
        return answer;
    }
}
