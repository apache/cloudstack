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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.cloudstack.backup.FinalizeImageTransferCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Answer;
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

    private boolean stopImageServer() {
        String unitName = "cloudstack-image-server";
        final int imageServerPort = 54323;

        Script checkScript = new Script("/bin/bash", logger);
        checkScript.add("-c");
        checkScript.add(String.format("systemctl is-active --quiet %s", unitName));
        String checkResult = checkScript.execute();
        if (checkResult != null) {
            logger.info(String.format("Image server not running, resetting failed state"));
            resetService(unitName);
            // Still try to remove firewall rule in case it exists
            removeFirewallRule(imageServerPort);
            return true;
        }

        Script stopScript = new Script("/bin/bash", logger);
        stopScript.add("-c");
        stopScript.add(String.format("systemctl stop %s", unitName));
        stopScript.execute();
        resetService(unitName);
        logger.info(String.format("Image server %s stopped", unitName));

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
            logger.debug(String.format("Firewall rule removal result for port %d: %s", port, result));
        } else {
            logger.info(String.format("Firewall rule removed for port %d (or did not exist)", port));
        }
    }

    public Answer execute(FinalizeImageTransferCommand cmd, LibvirtComputingResource resource) {
        final String transferId = cmd.getTransferId();
        if (StringUtils.isBlank(transferId)) {
            return new Answer(cmd, false, "transferId is empty.");
        }

        final File transferFile = new File("/tmp/imagetransfer", transferId);
        if (transferFile.exists() && !transferFile.delete()) {
            return new Answer(cmd, false, "Failed to delete transfer config file: " + transferFile.getAbsolutePath());
        }

        try (Stream<Path> stream = Files.list(Paths.get("/tmp/imagetransfer"))) {
            if (!stream.findAny().isPresent()) {
                stopImageServer();
            }
        } catch (IOException e) {
            logger.warn("Failed to list /tmp/imagetransfer", e);
        }

        return new Answer(cmd, true, "Image transfer finalized.");
    }
}
