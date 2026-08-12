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

import org.apache.cloudstack.backup.StartNBDServerAnswer;
import org.apache.cloudstack.backup.StartNBDServerCommand;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = StartNBDServerCommand.class)
public class LibvirtStartNBDServerCommandWrapper extends CommandWrapper<StartNBDServerCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    private String createSystemdRunCmd(StartNBDServerCommand cmd, String unitName, String volumePath, String exportName, String socket) throws IOException {
        String socketName = "/tmp/imagetransfer/" + socket + ".sock";

        String bitmapArg = "";
        if (StringUtils.isNotBlank(cmd.getFromCheckpointId())
                && isBitmapPresentOnDisk(volumePath, cmd.getFromCheckpointId())) {
            bitmapArg = "-B " + cmd.getFromCheckpointId();
        }

        byte[] passphrase = cmd.getPassphrase(); // may be null
        String readOnlyArg = cmd.getDirection().equals("download") ? "--read-only" : "";
        String imageArg = volumePath;
        String secretArg = "";
        if (passphrase != null && passphrase.length > 0) {
            KeyFile srcKey = new KeyFile(passphrase);
            secretArg = String.format("--object secret,id=sec0,file=%s", srcKey);
            imageArg = String.format("--image-opts driver=qcow2,file.driver=file,file.filename=%s,encrypt.key-secret=sec0", volumePath);
        }

        // --persistent: Don't stop the service when the last client disconnects.
        // --shared=NUM: Allow up to NUM clients to share the device (default 1), 0 for unlimited. Number of parallel connections is managed by the image server.
        String systemdRunCmd = String.format(
                "systemd-run --unit=%s --property=Restart=no qemu-nbd %s " +
                        "--export-name %s --socket %s --persistent --shared=0 %s %s %s",
                unitName,
                secretArg,
                exportName,
                socketName,
                bitmapArg,
                readOnlyArg,
                imageArg
        );
        return systemdRunCmd;
    }

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

        String systemdRunCmd = "";
        try {
            systemdRunCmd = createSystemdRunCmd(cmd, unitName, volumePath, exportName, socket);
        } catch (IOException e) {
            logger.error("Failed to create the KeyFile for qemu-nbd service", e);
            return new StartNBDServerAnswer(cmd, false, "Failed to create systemd run command for qemu-nbd service: " + e.getMessage());
        }

        Script startScript = new Script("/bin/bash", logger);
        startScript.add("-c");
        startScript.add(systemdRunCmd);
        String startResult = startScript.execute();

        if (startResult != null) {
            logger.error(String.format("Failed to start qemu-nbd service: %s", startResult));
            return new StartNBDServerAnswer(cmd, false, "Failed to start qemu-nbd service: " + startResult);
        }

        // Wait with timeout until the service is up
        int maxWaitSeconds = 20;
        int pollIntervalMs = 5000;
        int maxAttempts = (maxWaitSeconds * 1000) / pollIntervalMs;
        boolean serviceActive = false;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new StartNBDServerAnswer(cmd, false, "Interrupted while waiting for qemu-nbd service to start");
            }
            Script verifyScript = new Script("/bin/bash", logger);
            verifyScript.add("-c");
            verifyScript.add(String.format("systemctl is-active --quiet %s", unitName));
            String verifyResult = verifyScript.execute();
            if (verifyResult == null) {
                serviceActive = true;
                logger.info(String.format("qemu-nbd service %s is now active (attempt %d)", unitName, attempt + 1));
                break;
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

    private boolean isBitmapPresentOnDisk(String volumePath, String fromCheckpointId) {
        String qemuImgInfo = Script.runBashScriptIgnoreExitValue(
                String.format("qemu-img info --output=json %s", volumePath), 0);
        if (StringUtils.isBlank(qemuImgInfo)) {
            logger.warn("Unable to read qemu-img info output for disk path [{}].", volumePath);
            return false;
        }
        try {
            JSONObject info = new JSONObject(qemuImgInfo);
            JSONObject formatSpecific = info.optJSONObject("format-specific");
            if (formatSpecific == null) {
                return false;
            }
            JSONObject formatData = formatSpecific.optJSONObject("data");
            if (formatData == null) {
                return false;
            }
            JSONArray bitmaps = formatData.optJSONArray("bitmaps");
            if (bitmaps == null) {
                return false;
            }
            for (int i = 0; i < bitmaps.length(); i++) {
                JSONObject bitmap = bitmaps.optJSONObject(i);
                if (bitmap == null) {
                    continue;
                }
                if (fromCheckpointId.equals(bitmap.optString("name"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse qemu-img info output for disk path [{}].", volumePath, e);
        }
        return false;
    }
}
