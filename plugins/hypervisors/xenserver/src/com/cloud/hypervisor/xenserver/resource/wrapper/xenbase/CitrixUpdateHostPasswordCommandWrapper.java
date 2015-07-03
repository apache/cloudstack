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
import com.cloud.agent.api.UpdateHostPasswordCommand;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.ssh.SshHelper;

@ResourceWrapper(handles =  UpdateHostPasswordCommand.class)
public final class CitrixUpdateHostPasswordCommandWrapper extends CommandWrapper<UpdateHostPasswordCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixUpdateHostPasswordCommandWrapper.class);
    private static final int TIMEOUT = 10000;

    @Override
    public Answer execute(final UpdateHostPasswordCommand command, final CitrixResourceBase citrixResourceBase) {
        final String hostIp = command.getHostIp();
        final String username = command.getUsername();
        final String newPassword = command.getNewPassword();

        final StringBuffer cmdLine = new StringBuffer();
        cmdLine.append("sh /opt/cloud/bin/");
        cmdLine.append(VRScripts.UPDATE_HOST_PASSWD);
        cmdLine.append(' ');
        cmdLine.append(username);
        cmdLine.append(' ');
        cmdLine.append(newPassword);

        Pair<Boolean, String> result;

        try {
            s_logger.debug("Executing command in Host: " + cmdLine);
            result = SshHelper.sshExecute(hostIp, 22, username, null, citrixResourceBase.getPwdFromQueue(), cmdLine.toString(), 60000, 60000, TIMEOUT);
        } catch (final Exception e) {
            return new Answer(command, false, e.getMessage());
        }
        // Add new password to the stack.
        citrixResourceBase.addToPwdQueue(command.getNewPassword());
        return new Answer(command, result.first(), result.second());
    }
}