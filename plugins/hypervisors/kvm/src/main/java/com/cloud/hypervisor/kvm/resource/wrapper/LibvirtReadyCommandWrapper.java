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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.HashMap;
import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.host.Host;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

import org.apache.log4j.Logger;

@ResourceWrapper(handles =  ReadyCommand.class)
public final class LibvirtReadyCommandWrapper extends CommandWrapper<ReadyCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtReadyCommandWrapper.class);

    @Override
    public Answer execute(final ReadyCommand command, final LibvirtComputingResource libvirtComputingResource) {
        Map<String, String> hostDetails = new HashMap<String, String>();

        if (hostSupportsUefi() && libvirtComputingResource.isUefiPropertiesFileLoaded()) {
            hostDetails.put(Host.HOST_UEFI_ENABLE, Boolean.TRUE.toString());
        }

        return new ReadyAnswer(command, hostDetails);
    }

    private boolean hostSupportsUefi() {
        String cmd = "rpm -qa | grep -i ovmf";
        s_logger.debug("Running command : " + cmd);
        int result = Script.runSimpleBashScriptForExitValue(cmd);
        s_logger.debug("Got result : " + result);
        return result == 0;
    }
}