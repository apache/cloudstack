//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.cloudstack.backup.FinalizeImageTransferCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.ImageServerControlSocket;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = FinalizeImageTransferCommand.class)
public class LibvirtFinalizeImageTransferCommandWrapper extends CommandWrapper<FinalizeImageTransferCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());
    private void resetService(String unitName) {
        Script resetScript = new Script("/bin/bash", logger);
        resetScript.add("-c");
        resetScript.add(String.format("systemctl reset-failed %s || true", unitName));
        resetScript.execute();
    }

    private boolean stopImageServer(int imageServerPort) {
        String unitName = "cloudstack-image-server";

        Script checkScript = new Script("/bin/bash", logger);
        checkScript.add("-c");
        checkScript.add(String.format("systemctl is-active --quiet %s", unitName));
        String checkResult = checkScript.execute();
        if (checkResult != null) {
            logger.info("Image server not running, resetting failed state");
            resetService(unitName);
            removeFirewallRule(imageServerPort);
            return true;
        }

        Script stopScript = new Script("/bin/bash", logger);
        stopScript.add("-c");
        stopScript.add(String.format("systemctl stop %s", unitName));
        stopScript.execute();
        resetService(unitName);
        logger.info("Image server {} stopped", unitName);

        removeFirewallRule(imageServerPort);

        return true;
    }

    private void removeFirewallRule(int port) {
        String rule = String.format("-p tcp -m state --state NEW -m tcp --dport %d -j ACCEPT", port);
        Script removeScript = new Script("/bin/bash", logger);
        removeScript.add("-c");
        removeScript.add(String.format("iptables -D INPUT %s || true", rule));
        String result = removeScript.execute();
        if (result != null && !result.isEmpty() && !result.contains("iptables: Bad rule")) {
            logger.debug("Firewall rule removal result for port {}: {}", port, result);
        } else {
            logger.info("Firewall rule removed for port {} (or did not exist)", port);
        }
    }

    public Answer execute(FinalizeImageTransferCommand cmd, LibvirtComputingResource resource) {
        final String transferId = cmd.getTransferId();
        final int imageServerPort = LibvirtComputingResource.IMAGE_SERVER_DEFAULT_PORT;
        if (StringUtils.isBlank(transferId)) {
            return new Answer(cmd, false, "transferId is empty.");
        }

        int activeTransfers = ImageServerControlSocket.unregisterTransfer(transferId);
        if (activeTransfers < 0) {
            logger.warn("Could not reach image server to unregister transfer {}; assuming server is down", transferId);
            stopImageServer(imageServerPort);
            return new Answer(cmd, true, "Image transfer finalized (server unreachable, forced stop).");
        }

        if (activeTransfers == 0) {
            stopImageServer(imageServerPort);
        }

        return new Answer(cmd, true, "Image transfer finalized.");
    }
}
