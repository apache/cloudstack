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

import org.apache.cloudstack.backup.StartNBDServerAnswer;
import org.apache.cloudstack.backup.StartNBDServerCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = StartNBDServerCommand.class)
public class LibvirtStartNBDServerCommandWrapper extends CommandWrapper<StartNBDServerCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public Answer execute(StartNBDServerCommand cmd, LibvirtComputingResource resource) {
        String volumePath = cmd.getVolumePath();
        String socket = cmd.getSocket();
        String exportName = cmd.getExportName();
        String transferId = cmd.getTransferId();

        if (StringUtils.isBlank(volumePath)) {
            return new StartNBDServerAnswer(cmd, false, "Volume path is required for the nbd server");
        }
        if (StringUtils.isBlank(exportName)) {
            return new StartNBDServerAnswer(cmd, false, "Export name is required for the nbd server");
        }
        if (StringUtils.isBlank(socket)) {
            return new StartNBDServerAnswer(cmd, false, "Socket is required for the nbd server");
        }

        String unitName = "qemu-nbd-" + transferId.hashCode();

        Script checkScript = new Script("/bin/bash", logger);
        checkScript.add("-c");
        checkScript.add(String.format("systemctl is-active --quiet %s", unitName));
        String checkResult = checkScript.execute();
        if (checkResult == null) {
            return new StartNBDServerAnswer(cmd, false, "A qemu-nbd service is already running on the port.");
        }

        File dir = new File("/tmp/imagetransfer");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String socketName = "/tmp/imagetransfer/" + socket + ".sock";
        String systemdRunCmd = String.format(
                "systemd-run --unit=%s --property=Restart=no qemu-nbd --export-name %s --socket %s --persistent %s %s",
                unitName,
                exportName,
                socketName,
                cmd.getDirection().equals("download") ? "--read-only" : "",
                volumePath
        );


        Script startScript = new Script("/bin/bash", logger);
        startScript.add("-c");
        startScript.add(systemdRunCmd);
        String startResult = startScript.execute();

        if (startResult != null) {
            logger.error(String.format("Failed to start qemu-nbd service: %s", startResult));
            return new StartNBDServerAnswer(cmd, false, "Failed to start qemu-nbd service: " + startResult);
        }

        // Wait with timeout until the service is up
        int maxWaitSeconds = 10;
        int pollIntervalMs = 1000;
        int maxAttempts = (maxWaitSeconds * 1000) / pollIntervalMs;
        boolean serviceActive = false;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Script verifyScript = new Script("/bin/bash", logger);
            verifyScript.add("-c");
            verifyScript.add(String.format("systemctl is-active --quiet %s", unitName));
            String verifyResult = verifyScript.execute();
            if (verifyResult == null) {
                serviceActive = true;
                logger.info(String.format("qemu-nbd service %s is now active (attempt %d)", unitName, attempt + 1));
                break;
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new StartNBDServerAnswer(cmd, false, "Interrupted while waiting for qemu-nbd service to start");
            }
        }

        if (!serviceActive) {
            logger.error(String.format("qemu-nbd service %s failed to become active within %d seconds", unitName, maxWaitSeconds));
            return new StartNBDServerAnswer(cmd, false,
                    String.format("qemu-nbd service failed to start within %d seconds", maxWaitSeconds));
        }

        String transferUrl = String.format("nbd+unix:///%s", cmd.getSocket());
        return new StartNBDServerAnswer(cmd, true, "qemu-nbd service started for upload",
                    transferId, transferUrl);
    }
}
