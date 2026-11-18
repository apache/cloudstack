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

import org.apache.cloudstack.utils.qemu.QemuCommand;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FreezeThawVMAnswer;
import com.cloud.agent.api.FreezeThawVMCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.google.gson.JsonParser;

@ResourceWrapper(handles = FreezeThawVMCommand.class)
public class LibvirtFreezeThawVMCommandWrapper extends CommandWrapper<FreezeThawVMCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(FreezeThawVMCommand command, LibvirtComputingResource serverResource) {
        String vmName = command.getVmName();
        Domain domain = null;

        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = serverResource.getLibvirtUtilitiesHelper();
            Connect connect = libvirtUtilitiesHelper.getConnection();
            domain = serverResource.getDomain(connect, vmName);
            if (domain == null) {
                return new FreezeThawVMAnswer(command, false, String.format("Failed to %s due to %s was not found",
                        command.getOption(), vmName));
            }
            DomainState domainState = domain.getInfo().state ;
            if (domainState != DomainState.VIR_DOMAIN_RUNNING) {
                return new FreezeThawVMAnswer(command, false,
                        String.format("%s of VM failed due to vm %s is in %s state", command.getOption(),
                                vmName, domainState));
            }

            String result = getResultOfQemuCommand(command.getOption(), domain);
            logger.debug(String.format("Result of %s command is %s", command.getOption(), result));
            if (result == null || (result.startsWith("error"))) {
                return new FreezeThawVMAnswer(command, false, String.format("Failed to %s vm %s due to result status is: %s",
                        command.getOption(), vmName, result));
            }
            String status = getResultOfQemuCommand(FreezeThawVMCommand.STATUS, domain);
            logger.debug(String.format("Status of %s command is %s", command.getOption(), status));
            if (status != null && new JsonParser().parse(status).isJsonObject()) {
                String statusResult = new JsonParser().parse(status).getAsJsonObject().get("return").getAsString();
                if (statusResult.equals(command.getOption())) {
                    return new FreezeThawVMAnswer(command, true, String.format("%s of VM - %s is successful", command.getOption(), vmName));
                }
            }
            return new FreezeThawVMAnswer(command, false, String.format("Failed to %s vm %s due to result status is: %s",
                    command.getOption(), vmName, status));
        } catch (LibvirtException libvirtException) {
            return new FreezeThawVMAnswer(command, false,  String.format("Failed to %s VM - %s due to %s",
                    command.getOption(), vmName, libvirtException.getMessage()));
        } finally {
            if (domain != null) {
                try {
                    domain.free();
                } catch (LibvirtException e) {
                    logger.trace("Ingore error ", e);
                }
            }
        }
    }

    private String getResultOfQemuCommand(String cmd, Domain domain) throws LibvirtException {
        String result = null;
        if (cmd.equals(FreezeThawVMCommand.FREEZE)) {
            result = domain.qemuAgentCommand(QemuCommand.buildQemuCommand(QemuCommand.AGENT_FREEZE, null), 10, 0);
        } else if (cmd.equals(FreezeThawVMCommand.THAW)) {
            result = domain.qemuAgentCommand(QemuCommand.buildQemuCommand(QemuCommand.AGENT_THAW, null), 10, 0);
        } else if (cmd.equals(FreezeThawVMCommand.STATUS)) {
            result = domain.qemuAgentCommand(QemuCommand.buildQemuCommand(QemuCommand.AGENT_FREEZE_STATUS, null), 10, 0);
        }
        return result;
    }
}
