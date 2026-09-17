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
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

@ResourceWrapper(handles = CheckGuestAgentLivenessCommand.class)
public class LibvirtCheckGuestAgentLivenessCommandWrapper extends CommandWrapper<CheckGuestAgentLivenessCommand, Answer, LibvirtComputingResource> {

    private static final int MIN_AGENT_PING_TIMEOUT_SECONDS = 1;
    private static final int MAX_RESULT_MESSAGE_LENGTH = 256;

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

            int timeoutSeconds = Math.max(command.getWait(), MIN_AGENT_PING_TIMEOUT_SECONDS);
            String result = domain.qemuAgentCommand(QemuCommand.buildQemuCommand(QemuCommand.AGENT_PING, null), timeoutSeconds, 0);
            return parseJsonResult(command, result);
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

    private CheckGuestAgentLivenessAnswer parseJsonResult(CheckGuestAgentLivenessCommand command, String result) {
        if (result == null || result.isBlank()) {
            logger.error("Guest agent returned empty response");
            return new CheckGuestAgentLivenessAnswer(command, false, "guest agent returned empty response");
        }
        try {
            JsonElement parsedResult = JsonParser.parseString(result);
            if (parsedResult.isJsonObject() && parsedResult.getAsJsonObject().has("return") && !parsedResult.getAsJsonObject().has("error")) {
                return new CheckGuestAgentLivenessAnswer(command, true, "guest agent responded");
            }
        } catch (JsonParseException e) {
            return new CheckGuestAgentLivenessAnswer(command, false, "guest agent returned invalid JSON: " + abbreviateResultForMessage(result));
        }
        return new CheckGuestAgentLivenessAnswer(command, false, "guest agent did not respond as expected: " + abbreviateResultForMessage(result));
    }

    private String abbreviateResultForMessage(String result) {
        if (result.length() <= MAX_RESULT_MESSAGE_LENGTH) {
            return result;
        }
        return result.substring(0, MAX_RESULT_MESSAGE_LENGTH) + "...";
    }
}
