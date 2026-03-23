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
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.backup.CreateImageTransferAnswer;
import org.apache.cloudstack.backup.CreateImageTransferCommand;
import org.apache.cloudstack.backup.ImageTransfer;
import org.apache.cloudstack.storage.resource.IpTablesHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.ImageServerControlSocket;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = CreateImageTransferCommand.class)
public class LibvirtCreateImageTransferCommandWrapper extends CommandWrapper<CreateImageTransferCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    private boolean startImageServerIfNotRunning(int imageServerPort, LibvirtComputingResource resource) {
        final String imageServerPackageDir = resource.getImageServerPath();
        final String imageServerParentDir = new File(imageServerPackageDir).getParent();
        final String imageServerModuleName = new File(imageServerPackageDir).getName();
        String unitName = "cloudstack-image-server";

        Script checkScript = new Script("/bin/bash", logger);
        checkScript.add("-c");
        checkScript.add(String.format("systemctl is-active --quiet %s", unitName));
        String checkResult = checkScript.execute();
        if (checkResult == null && ImageServerControlSocket.isReady()) {
            return true;
        }

        if (checkResult != null) {
            String systemdRunCmd = String.format(
                    "systemd-run --unit=%s --property=Restart=no --property=WorkingDirectory=%s /usr/bin/python3 -m %s --listen 0.0.0.0 --port %d",
                    unitName, imageServerParentDir, imageServerModuleName, imageServerPort);

            Script startScript = new Script("/bin/bash", logger);
            startScript.add("-c");
            startScript.add(systemdRunCmd);
            String startResult = startScript.execute();

            if (startResult != null) {
                logger.error(String.format("Failed to start the Image server: %s", startResult));
                return false;
            }
        }

        int maxWaitSeconds = 10;
        int pollIntervalMs = 1000;
        int maxAttempts = (maxWaitSeconds * 1000) / pollIntervalMs;
        boolean serverReady = false;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (ImageServerControlSocket.isReady()) {
                serverReady = true;
                logger.info(String.format("Image server control socket is ready (attempt %d)", attempt + 1));
                break;
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        if (!serverReady) {
            logger.error(String.format("Image server control socket not ready within %d seconds", maxWaitSeconds));
            return false;
        }

        String rule = String.format("-p tcp -m state --state NEW -m tcp --dport %d -j ACCEPT", imageServerPort);
        IpTablesHelper.addConditionally(IpTablesHelper.INPUT_CHAIN, true, rule,
                String.format("Error in opening up image server port %d", imageServerPort));

        return true;
    }

    public Answer execute(CreateImageTransferCommand cmd, LibvirtComputingResource resource) {
        final String transferId = cmd.getTransferId();
        ImageTransfer.Backend backend = cmd.getBackend();

        if (StringUtils.isBlank(transferId)) {
            return new CreateImageTransferAnswer(cmd, false, "transferId is empty.");
        }

        final Map<String, Object> payload = new HashMap<>();
        payload.put("backend", backend.toString());

        if (backend == ImageTransfer.Backend.file) {
            final String filePath = cmd.getFile();
            if (StringUtils.isBlank(filePath)) {
                return new CreateImageTransferAnswer(cmd, false, "file path is empty for file backend.");
            }
            payload.put("file", filePath);
        } else {
            String socket = cmd.getSocket();
            final String exportName = cmd.getExportName();
            if (StringUtils.isBlank(socket)) {
                return new CreateImageTransferAnswer(cmd, false, "Empty socket.");
            }
            if (StringUtils.isBlank(exportName)) {
                return new CreateImageTransferAnswer(cmd, false, "exportName is empty.");
            }
            payload.put("socket", "/tmp/imagetransfer/" + socket + ".sock");
            payload.put("export", exportName);
            String checkpointId = cmd.getCheckpointId();
            if (checkpointId != null) {
                payload.put("export_bitmap", cmd.getCheckpointId());
            }
        }

        final int imageServerPort = 54323;
        if (!startImageServerIfNotRunning(imageServerPort, resource)) {
            return new CreateImageTransferAnswer(cmd, false, "Failed to start image server.");
        }

        if (!ImageServerControlSocket.registerTransfer(transferId, payload)) {
            return new CreateImageTransferAnswer(cmd, false, "Failed to register transfer with image server.");
        }

        final String transferUrl = String.format("http://%s:%d/images/%s", resource.getPrivateIp(), imageServerPort, transferId);
        return new CreateImageTransferAnswer(cmd, true, "Image transfer prepared on KVM host.", transferId, transferUrl);
    }
}
