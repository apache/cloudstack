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

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  GetVmStatsCommand.class)
public final class LibvirtGetVmStatsCommandWrapper extends CommandWrapper<GetVmStatsCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtGetVmStatsCommandWrapper.class);

    @Override
    public Answer execute(final GetVmStatsCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final List<String> vmNames = command.getVmNames();
        try {
            final HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
            for (final String vmName : vmNames) {

                final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

                final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
                final VmStatsEntry statEntry = libvirtComputingResource.getVmStat(conn, vmName);
                if (statEntry == null) {
                    continue;
                }

                vmStatsNameMap.put(vmName, statEntry);
            }
            return new GetVmStatsAnswer(command, vmStatsNameMap);
        } catch (final LibvirtException e) {
            s_logger.debug("Can't get vm stats: " + e.toString());
            return new GetVmStatsAnswer(command, null);
        }
    }
}