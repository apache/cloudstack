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
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.FileUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.utils.validation.ChecksumUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;

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
        } catch (CloudRuntimeException e) {
            return new PatchSystemVmAnswer(cmd, e.getMessage());
        }

        final String[] lines = result.getDetails().split("&");
        // TODO: do we fail, or patch anyway??
        if (lines.length != 2) {
            return new PatchSystemVmAnswer(cmd, result.getDetails());
        }

        String scriptChecksum = lines[1].trim();
        String checksum = ChecksumUtil.calculateCurrentChecksum(sysVMName, "vms/cloud-scripts.tgz").trim();

        if (!StringUtils.isEmpty(checksum) && checksum.equals(scriptChecksum) && !cmd.isForced()) {
            String msg = String.format("No change in the scripts checksum, not patching systemVM %s", sysVMName);
            s_logger.info(msg);
            return new PatchSystemVmAnswer(cmd, msg, lines[0], lines[1]);
        }

        Pair<Boolean, String> patchResult = null;
        try {
            FileUtil.scpPatchFiles(controlIp, VRScripts.CONFIG_CACHE_LOCATION, sshPort, pemFile, serverResource.systemVmPatchFiles, LibvirtComputingResource.BASEPATH);
            patchResult = SshHelper.sshExecute(controlIp, sshPort, "root",
                    pemFile, null, "/var/cache/cloud/patch-sysvms.sh", 10000, 10000, 600000);
        } catch (Exception e) {
            return new PatchSystemVmAnswer(cmd, e.getMessage());
        }

        if (patchResult.first()) {
            String scriptVersion = lines[1];
            if (StringUtils.isNotEmpty(patchResult.second())) {
                String res = patchResult.second().replace("\n", " ");
                String[] output = res.split(":");
                if (output.length != 2) {
                    s_logger.warn("Failed to get the latest script version");
                } else {
                    scriptVersion = output[1].split(" ")[0];
                }
            }
            return new PatchSystemVmAnswer(cmd, String.format("Successfully patched systemVM %s ", sysVMName), lines[0], scriptVersion);
        }
        return new PatchSystemVmAnswer(cmd, patchResult.second());
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
}
