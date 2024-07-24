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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import org.apache.cloudstack.utils.linux.CPUStat;
import org.apache.cloudstack.utils.linux.MemStat;
import org.apache.log4j.Logger;

@ResourceWrapper(handles =  GetHostStatsCommand.class)
public final class LibvirtGetHostStatsCommandWrapper extends CommandWrapper<GetHostStatsCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtGetHostStatsCommandWrapper.class);

    @Override
    public Answer execute(final GetHostStatsCommand command, final LibvirtComputingResource libvirtComputingResource) {
        CPUStat cpuStat = libvirtComputingResource.getCPUStat();
        MemStat memStat = libvirtComputingResource.getMemStat();

        final double cpuUtil = cpuStat.getCpuUsedPercent();
        final double loadAvg = cpuStat.getCpuLoadAverage();

        final Pair<Double, Double> nicStats = libvirtComputingResource.getNicStats(libvirtComputingResource.getPublicBridgeName());

        final HostStatsEntry hostStats = new HostStatsEntry(command.getHostId(), cpuUtil, nicStats.first() / 1024, nicStats.second() / 1024, "host", memStat.getTotal() / 1024, memStat.getAvailable() / 1024, 0, loadAvg);
        return new GetHostStatsAnswer(command, hostStats);
    }
}
