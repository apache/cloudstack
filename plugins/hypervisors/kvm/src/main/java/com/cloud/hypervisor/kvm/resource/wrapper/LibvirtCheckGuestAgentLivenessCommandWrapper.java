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
import com.cloud.agent.api.CheckGuestAgentLivenessAnswer;
import com.cloud.agent.api.CheckGuestAgentLivenessCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.google.gson.JsonParser;

@ResourceWrapper(handles = CheckGuestAgentLivenessCommand.class)
public class LibvirtCheckGuestAgentLivenessCommandWrapper extends CommandWrapper<CheckGuestAgentLivenessCommand, Answer, LibvirtComputingResource> {

    private static final int AGENT_PING_TIMEOUT_SECONDS = 5;

    @Override
    public Answer execute(CheckGuestAgentLivenessCommand command, LibvirtComputingResource serverResource) {
        String vmName = command.getVmName();
        Domain domain = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = serverResource.getLibvirtUtilitiesHelper();
            Connect connect = libvirtUtilitiesHelper.getConnection();
            domain = serverResource.getDomain(connect, vmName);
            if (domain == null) {
                return new CheckGuestAgentLivenessAnswer(command, false, String.format("VM %s was not found", vmName));
            }

            DomainState domainState = domain.getInfo().state;
            if (domainState != DomainState.VIR_DOMAIN_RUNNING) {
                return new CheckGuestAgentLivenessAnswer(command, false, String.format("VM %s is in %s state", vmName, domainState));
            }

            String result = domain.qemuAgentCommand(QemuCommand.buildQemuCommand(QemuCommand.AGENT_PING, null), AGENT_PING_TIMEOUT_SECONDS, 0);
            if (result != null && new JsonParser().parse(result).isJsonObject() && !new JsonParser().parse(result).getAsJsonObject().has("error")) {
                return new CheckGuestAgentLivenessAnswer(command, true, "guest agent responded");
            }
            return new CheckGuestAgentLivenessAnswer(command, false, "guest agent did not respond as expected: " + result);
        } catch (LibvirtException e) {
            return new CheckGuestAgentLivenessAnswer(command, false, "guest agent did not respond: " + e.getMessage());
        } finally {
            if (domain != null) {
                try {
                    domain.free();
                } catch (LibvirtException e) {
                    logger.trace("Ignore error ", e);
                }
            }
        }
    }
}
