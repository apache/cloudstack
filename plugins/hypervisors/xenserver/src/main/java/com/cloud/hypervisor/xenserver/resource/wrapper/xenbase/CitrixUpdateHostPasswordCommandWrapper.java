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

import static com.cloud.hypervisor.xenserver.resource.wrapper.xenbase.XenServerUtilitiesHelper.SCRIPT_CMD_PATH;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UpdateHostPasswordCommand;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;

@ResourceWrapper(handles =  UpdateHostPasswordCommand.class)
public final class CitrixUpdateHostPasswordCommandWrapper extends CommandWrapper<UpdateHostPasswordCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixUpdateHostPasswordCommandWrapper.class);

    @Override
    public Answer execute(final UpdateHostPasswordCommand command, final CitrixResourceBase citrixResourceBase) {
        final String hostIp = command.getHostIp();
        final String username = command.getUsername();
        final String newPassword = command.getNewPassword();

        final XenServerUtilitiesHelper xenServerUtilitiesHelper = citrixResourceBase.getXenServerUtilitiesHelper();
        final String cmdLine = xenServerUtilitiesHelper.buildCommandLine(SCRIPT_CMD_PATH, VRScripts.UPDATE_HOST_PASSWD, username, newPassword);

        Pair<Boolean, String> result;
        try {
            s_logger.debug("Executing command in Host: " + cmdLine);
            final String hostPassword = citrixResourceBase.getPwdFromQueue();
            result = xenServerUtilitiesHelper.executeSshWrapper(hostIp, 22, username, null, hostPassword, cmdLine.toString());
        } catch (final Exception e) {
            return new Answer(command, false, e.getMessage());
        }
        // Add new password to the queue.
        citrixResourceBase.replaceOldPasswdInQueue(newPassword);
        return new Answer(command, result.first(), result.second());
    }
}
