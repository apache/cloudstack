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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BaselineCpuCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = BaselineCpuCommand.class)
public final class LibvirtBaselineCpuCommandWrapper extends CommandWrapper<BaselineCpuCommand, Answer,
        LibvirtComputingResource> {

    private static final int VIRSH_TIMEOUT_SECONDS = 120;

    @Override
    public Answer execute(final BaselineCpuCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final List<String> hostCpuXmls = command.getHostCpuXmls();
        if (CollectionUtils.isEmpty(hostCpuXmls)) {
            return new Answer(command, false, "no host CPU definitions supplied");
        }
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("cloudstack-cpu-baseline-", ".xml");
            Files.write(tempFile, String.join("\n", hostCpuXmls).getBytes(StandardCharsets.UTF_8));

            // full result, not runSimpleBashScript: that keeps only the first line (the opening <cpu> tag),
            // so the <model> would never be seen.
            final String output = Script.runSimpleBashScriptWithFullResult("virsh cpu-baseline " + tempFile.toString() + " 2>&1 || true", VIRSH_TIMEOUT_SECONDS);
            final boolean computed = output != null && output.contains("<model");
            logger.debug("cpu-baseline over {} host CPU(s) returned computed={}, output=[{}]", hostCpuXmls.size(), computed, output);
            return new Answer(command, computed, output);
        } catch (IOException e) {
            logger.warn("Failed to compute the cluster CPU baseline", e);
            return new Answer(command, false, e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    logger.trace("Failed to delete temp CPU baseline file: {}", tempFile);
                }
            }
        }
    }
}
