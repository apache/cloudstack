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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckCpuCompatibilityCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = CheckCpuCompatibilityCommand.class)
public final class LibvirtCheckCpuCompatibilityCommandWrapper extends CommandWrapper<CheckCpuCompatibilityCommand, Answer,
        LibvirtComputingResource> {

    private static final int VIRSH_TIMEOUT_SECONDS = 120;

    @Override
    public Answer execute(final CheckCpuCompatibilityCommand command, final LibvirtComputingResource libvirtComputingResource) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("cloudstack-cpu-compare-", ".xml");
            Files.write(tempFile, command.getCpuXml() == null ? new byte[0] : command.getCpuXml().getBytes(StandardCharsets.UTF_8));

            // full result (not runSimpleBashScript, which keeps only the first line) with a forced zero exit,
            // so the full verdict is parsed: an incompatible host, and the "Unknown CPU model" detail that
            // virsh prints on a later line for a bogus model, both reach the caller instead of being dropped.
            final String output = Script.runSimpleBashScriptWithFullResult("virsh cpu-compare " + tempFile.toString() + " 2>&1 || true", VIRSH_TIMEOUT_SECONDS);
            final boolean compatible = isCompatible(output);
            logger.debug("CPU compatibility check for VM [{}] on this host returned compatible={}, output=[{}]",
                    command.getVmName(), compatible, output);
            return new Answer(command, compatible, output);
        } catch (IOException e) {
            logger.warn("Failed to run CPU compatibility check for VM [{}]", command.getVmName(), e);
            return new Answer(command, false, e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    logger.trace("Failed to delete temp CPU XML file: {}", tempFile);
                }
            }
        }
    }

    // Incompatible when virsh reports "incompatible" (the verdict current libvirt emits); "not a superset"
    // is also treated as incompatible defensively. Blank/unknown output fails open (compatible=true) so a
    // missing or odd virsh does not block migration.
    protected static boolean isCompatible(final String output) {
        if (output == null || output.trim().isEmpty()) {
            return true;
        }
        final String lower = output.toLowerCase();
        if (lower.contains("incompatible") || lower.contains("not a superset")) {
            return false;
        }
        return true;
    }
}
