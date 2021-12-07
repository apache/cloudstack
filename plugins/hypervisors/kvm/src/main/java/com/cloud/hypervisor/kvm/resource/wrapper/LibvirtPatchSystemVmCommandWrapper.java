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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PatchSystemVmAnswer;
import com.cloud.agent.api.PatchSystemVmCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.EncryptionUtil;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ResourceWrapper(handles = PatchSystemVmCommand.class)
public class LibvirtPatchSystemVmCommandWrapper extends CommandWrapper<PatchSystemVmCommand, Answer, LibvirtComputingResource> {
    private static final Logger s_logger = Logger.getLogger(LibvirtPatchSystemVmCommandWrapper.class);
    private static int sshPort = Integer.parseInt(LibvirtComputingResource.DEFAULTDOMRSSHPORT);
    private static File pemFile = new File(LibvirtComputingResource.SSHPRVKEYPATH);

    @Override
    public Answer execute(PatchSystemVmCommand cmd, LibvirtComputingResource serverResource) {
        final String controlIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        final String sysVMName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        ExecutionResult result;
        try {
            result = getSystemVmVersionAndChecksum(serverResource, controlIp);
            scpPatchFiles(controlIp);
        } catch (CloudRuntimeException e) {
            return new PatchSystemVmAnswer(cmd, e.getMessage());
        }

        final String[] lines = result.getDetails().split("&");
        // TODO: do we fail, or patch anyway??
        if (lines.length != 2) {
            return new PatchSystemVmAnswer(cmd, result.getDetails());
        }

        String scriptChecksum = lines[1].trim();
        String checksum = calculateCurrentChecksum(sysVMName).trim();

        if (!StringUtils.isEmpty(checksum) && checksum.equals(scriptChecksum)) {
            if (!cmd.isForced()) {
                String msg = String.format("No change in the scripts checksum, not patching systemVM %s", sysVMName);
                s_logger.info(msg);
                return new PatchSystemVmAnswer(cmd, msg, lines[0], lines[1]);
            }
        }

        Pair<Boolean, String> patchResult = null;
        try {
            patchResult = SshHelper.sshExecute(controlIp, sshPort, "root",
                    pemFile, null, "/home/cloud/patch-sysvms.sh", 10000, 10000, 60000);
        } catch (Exception e) {
            return new PatchSystemVmAnswer(cmd, e.getMessage());
        }

        if (patchResult.first()) {
            return new PatchSystemVmAnswer(cmd, String.format("Successfully patched systemVM %s ", sysVMName), lines[0], lines[1]);
        }
        return new PatchSystemVmAnswer(cmd, patchResult.second());
    }

    private String calculateCurrentChecksum(String name) {
        String cloudScriptsPath = Script.findScript("", "vms/cloud-scripts.tgz");
        if (cloudScriptsPath == null) {
            throw new CloudRuntimeException(String.format("Unable to find cloudScripts path, cannot update SystemVM %s", name));
        }
        String md5sum = EncryptionUtil.calculateChecksum(new File(cloudScriptsPath));
        return md5sum;
    }

    private ExecutionResult getSystemVmVersionAndChecksum(LibvirtComputingResource serverResource, String controlIp) {
        ExecutionResult result;
        try {
            result = serverResource.executeInVR(controlIp, VRScripts.VERSION, null);
            if (!result.isSuccess()) {
                String errMsg = String.format("GetSystemVMVersionCmd on %s failed, message %s", controlIp, result.getDetails());
                s_logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (final Exception e) {
            final String msg = "GetSystemVMVersionCmd failed due to " + e;
            s_logger.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
        return result;
    }

    private void scpPatchFiles(String controlIp) {
        try {
            List<String> srcFiles = Arrays.asList(LibvirtComputingResource.newSrcFiles);
            srcFiles = srcFiles.stream()
                    .map(file -> LibvirtComputingResource.BASEPATH + file) // Using Lambda notation to update the entries
                    .collect(Collectors.toList());
            String[] newSrcFiles = srcFiles.toArray(new String[0]);
            SshHelper.scpTo(controlIp, sshPort, "root", pemFile, null,
                    "/home/cloud/", newSrcFiles, "0755");
        } catch (Exception e) {
            String errMsg = "Failed to scp files to system VM";
            s_logger.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
    }
}

