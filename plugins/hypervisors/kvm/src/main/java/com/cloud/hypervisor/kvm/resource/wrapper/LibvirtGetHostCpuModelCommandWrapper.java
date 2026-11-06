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

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetHostCpuModelCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = GetHostCpuModelCommand.class)
public final class LibvirtGetHostCpuModelCommandWrapper extends CommandWrapper<GetHostCpuModelCommand, Answer,
        LibvirtComputingResource> {

    private static final int VIRSH_TIMEOUT_SECONDS = 120;

    @Override
    public Answer execute(final GetHostCpuModelCommand command, final LibvirtComputingResource libvirtComputingResource) {
        // full result, not runSimpleBashScript: that keeps only the first line, which would be "<capabilities>".
        final String caps = Script.runSimpleBashScriptWithFullResult("virsh capabilities 2>&1 || true", VIRSH_TIMEOUT_SECONDS);
        final String hostCpu = extractHostCpu(caps);
        if (StringUtils.isBlank(hostCpu)) {
            logger.warn("Could not read the host <cpu> element from virsh capabilities; output was [{}]", caps);
            return new Answer(command, false, caps);
        }
        return new Answer(command, true, hostCpu);
    }

    // The host <cpu> element from 'virsh capabilities', or null if absent.
    protected static String extractHostCpu(final String capabilitiesXml) {
        if (StringUtils.isBlank(capabilitiesXml)) {
            return null;
        }
        final int start = capabilitiesXml.indexOf("<cpu>");
        final int end = capabilitiesXml.indexOf("</cpu>");
        if (start < 0 || end < 0 || end < start) {
            return null;
        }
        return capabilitiesXml.substring(start, end + "</cpu>".length());
    }
}
