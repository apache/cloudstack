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

package com.cloud.hypervisor.xenserver.resource.wrapper.xen56;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.hypervisor.xenserver.resource.XenServer56Resource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  CheckOnHostCommand.class)
public final class XenServer56CheckOnHostCommandWrapper extends CommandWrapper<CheckOnHostCommand, Answer, XenServer56Resource> {

    private static final Logger s_logger = Logger.getLogger(XenServer56CheckOnHostCommandWrapper.class);

    @Override
    public Answer execute(final CheckOnHostCommand command, final XenServer56Resource xenServer56) {
        final Boolean alive = xenServer56.checkHeartbeat(command.getHost().getGuid());
        String msg = "";
        if (alive == null) {
            msg = " cannot determine ";
        } else if ( alive == true) {
            msg = "Heart beat is still going";
        } else {
            msg = "Heart beat is gone so dead.";
        }
        s_logger.debug(msg);
        return new CheckOnHostAnswer(command, alive, msg);
    }
}
