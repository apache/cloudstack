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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckConvertInstanceAnswer;
import com.cloud.agent.api.CheckConvertInstanceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  CheckConvertInstanceCommand.class)
public class LibvirtCheckConvertInstanceCommandWrapper extends CommandWrapper<CheckConvertInstanceCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCheckConvertInstanceCommandWrapper.class);

    protected static final String checkIfConversionIsSupportedCommand = "which virt-v2v";

    @Override
    public Answer execute(CheckConvertInstanceCommand cmd, LibvirtComputingResource serverResource) {
        if (!isInstanceConversionSupportedOnHost()) {
            String msg = String.format("Cannot convert the instance from VMware as the virt-v2v binary is not found on host %s. " +
                    "Please install virt-v2v on the host before attempting the instance conversion", serverResource.getPrivateIp());
            s_logger.info(msg);
            return new CheckConvertInstanceAnswer(cmd, false, msg);
        }

        return new CheckConvertInstanceAnswer(cmd, true, "");
    }

    protected boolean isInstanceConversionSupportedOnHost() {
        int exitValue = Script.runSimpleBashScriptForExitValue(checkIfConversionIsSupportedCommand);
        return exitValue == 0;
    }
}
