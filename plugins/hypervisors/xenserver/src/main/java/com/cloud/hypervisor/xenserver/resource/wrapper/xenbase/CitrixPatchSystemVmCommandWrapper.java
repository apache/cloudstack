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
package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PatchSystemVmAnswer;
import com.cloud.agent.api.PatchSystemVmCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.validation.ChecksumUtil;
import com.xensource.xenapi.Connection;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

@ResourceWrapper(handles = PatchSystemVmCommand.class)
public class CitrixPatchSystemVmCommandWrapper extends CommandWrapper<PatchSystemVmCommand, Answer, CitrixResourceBase> {
    private static int sshPort = CitrixResourceBase.DEFAULTDOMRSSHPORT;
    private static File pemFile = new File(CitrixResourceBase.SSHPRVKEYPATH);

    @Override
    public Answer execute(PatchSystemVmCommand command, CitrixResourceBase serverResource) {
        final String controlIp = command.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        final String sysVMName = command.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        final Connection conn = serverResource.getConnection();

        ExecutionResult result;
        try {
            result = getSystemVmVersionAndChecksum(serverResource, controlIp);
        } catch (CloudRuntimeException e) {
            return new PatchSystemVmAnswer(command, e.getMessage());
        }

        final String[] lines = result.getDetails().split("&");
        // TODO: do we fail, or patch anyway??
        if (lines.length != 2) {
            return new PatchSystemVmAnswer(command, result.getDetails());
        }

        String scriptChecksum = lines[1].trim();
        String checksum = ChecksumUtil.calculateCurrentChecksum(sysVMName, "vms/cloud-scripts.tgz").trim();
        if (!StringUtils.isEmpty(checksum) && checksum.equals(scriptChecksum) && !command.isForced()) {
            String msg = String.format("No change in the scripts checksum, not patching systemVM %s", sysVMName);
            logger.info(msg);
            return new PatchSystemVmAnswer(command, msg, lines[0], lines[1]);
        }

        String patchResult = null;
        try {
            serverResource.copyPatchFilesToVR(controlIp, VRScripts.CONFIG_CACHE_LOCATION);
            patchResult = serverResource.callHostPlugin(conn, "vmops", "runPatchScriptInDomr", "domrip", controlIp);
        } catch (Exception e) {
            return new PatchSystemVmAnswer(command, e.getMessage());
        }

        if (patchResult.startsWith("succ#")) {
            String scriptVersion = lines[1];
            String res = patchResult.replace("\n", " ");
            String[] output = res.split(":");
            if (output.length != 2) {
                logger.warn("Failed to get the latest script version");
            } else {
                scriptVersion = output[1].split(" ")[0];
            }

            return new PatchSystemVmAnswer(command, String.format("Successfully patched systemVM %s ", sysVMName), lines[0], scriptVersion);
        }
        return new PatchSystemVmAnswer(command, patchResult.substring(5));

    }

    private ExecutionResult getSystemVmVersionAndChecksum(CitrixResourceBase serverResource, String controlIp) {
        ExecutionResult result;
        try {
            result = serverResource.executeInVR(controlIp, VRScripts.VERSION, null);
            if (!result.isSuccess()) {
                String errMsg = String.format("GetSystemVMVersionCmd on %s failed, message %s", controlIp, result.getDetails());
                logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (final Exception e) {
            final String msg = "GetSystemVMVersionCmd failed due to " + e;
            logger.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
        return result;
    }


}
