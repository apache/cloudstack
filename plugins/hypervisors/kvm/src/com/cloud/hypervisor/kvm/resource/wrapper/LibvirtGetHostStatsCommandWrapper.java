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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  GetHostStatsCommand.class)
public final class LibvirtGetHostStatsCommandWrapper extends CommandWrapper<GetHostStatsCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtGetHostStatsCommandWrapper.class);

    @Override
    public Answer execute(final GetHostStatsCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
        final String bashScriptPath = libvirtUtilitiesHelper.retrieveBashScriptPath();


        final Script cpuScript = new Script(bashScriptPath, s_logger);
        cpuScript.add("-c");
        cpuScript.add("idle=$(top -b -n 1| awk -F, '/^[%]*[Cc]pu/{$0=$4; gsub(/[^0-9.,]+/,\"\"); print }'); echo $idle");

        final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = cpuScript.execute(parser);
        if (result != null) {
            s_logger.debug("Unable to get the host CPU state: " + result);
            return new Answer(command, false, result);
        }
        final double cpuUtil = 100.0D - Double.parseDouble(parser.getLine());

        long freeMem = 0;
        final Script memScript = new Script(bashScriptPath, s_logger);
        memScript.add("-c");
        memScript.add("freeMem=$(free|grep cache:|awk '{print $4}');echo $freeMem");
        final OutputInterpreter.OneLineParser Memparser = new OutputInterpreter.OneLineParser();
        result = memScript.execute(Memparser);
        if (result != null) {
            s_logger.debug("Unable to get the host Mem state: " + result);
            return new Answer(command, false, result);
        }
        freeMem = Long.parseLong(Memparser.getLine());

        final Script totalMem = new Script(bashScriptPath, s_logger);
        totalMem.add("-c");
        totalMem.add("free|grep Mem:|awk '{print $2}'");
        final OutputInterpreter.OneLineParser totMemparser = new OutputInterpreter.OneLineParser();
        result = totalMem.execute(totMemparser);
        if (result != null) {
            s_logger.debug("Unable to get the host Mem state: " + result);
            return new Answer(command, false, result);
        }
        final long totMem = Long.parseLong(totMemparser.getLine());

        final Pair<Double, Double> nicStats = libvirtComputingResource.getNicStats(libvirtComputingResource.getPublicBridgeName());

        final HostStatsEntry hostStats = new HostStatsEntry(command.getHostId(), cpuUtil, nicStats.first() / 1024, nicStats.second() / 1024, "host", totMem, freeMem, 0, 0);
        return new GetHostStatsAnswer(command, hostStats);
    }
}