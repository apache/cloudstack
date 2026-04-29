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

import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmNetworkStatsAnswer;
import com.cloud.agent.api.GetVmNetworkStatsCommand;
import com.cloud.agent.api.VmNetworkStatsEntry;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  GetVmNetworkStatsCommand.class)
public final class LibvirtGetVmNetworkStatsCommandWrapper extends CommandWrapper<GetVmNetworkStatsCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final GetVmNetworkStatsCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final List<String> vmNames = command.getVmNames();
        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

        try {
            final HashMap<String, List<VmNetworkStatsEntry>> vmNetworkStatsNameMap = new HashMap<String, List<VmNetworkStatsEntry>>();
            final Connect conn = libvirtUtilitiesHelper.getConnection();
            for (final String vmName : vmNames) {
                try {
                    final List<VmNetworkStatsEntry> statEntry = libvirtComputingResource.getVmNetworkStat(conn, vmName);
                    if (statEntry == null) {
                        continue;
                    }

                    vmNetworkStatsNameMap.put(vmName, statEntry);
                } catch (LibvirtException e) {
                    logger.warn("Can't get vm network stats: " + e.toString() + ", continue");
                }
            }
            return new GetVmNetworkStatsAnswer(command, "", command.getHostName(), vmNetworkStatsNameMap);
        } catch (final LibvirtException e) {
            logger.debug("Can't get vm network stats: " + e.toString());
            return new GetVmNetworkStatsAnswer(command, null, null, null);
        }
    }
}
