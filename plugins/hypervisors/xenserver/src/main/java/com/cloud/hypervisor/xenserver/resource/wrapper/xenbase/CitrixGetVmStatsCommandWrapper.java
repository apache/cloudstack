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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  GetVmStatsCommand.class)
public final class CitrixGetVmStatsCommandWrapper extends CommandWrapper<GetVmStatsCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixGetVmStatsCommandWrapper.class);

    @Override
    public Answer execute(final GetVmStatsCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final List<String> vmNames = command.getVmNames();
        final HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
        if (vmNames.size() == 0) {
            return new GetVmStatsAnswer(command, vmStatsNameMap);
        }
        try {

            // Determine the UUIDs of the requested VMs
            final List<String> vmUUIDs = new ArrayList<String>();

            for (final String vmName : vmNames) {
                final VM vm = citrixResourceBase.getVM(conn, vmName);
                vmUUIDs.add(vm.getUuid(conn));
            }

            final HashMap<String, VmStatsEntry> vmStatsUUIDMap = citrixResourceBase.getVmStats(conn, command, vmUUIDs, command.getHostGuid());
            if (vmStatsUUIDMap == null) {
                return new GetVmStatsAnswer(command, vmStatsNameMap);
            }

            for (final Map.Entry<String,VmStatsEntry>entry : vmStatsUUIDMap.entrySet()) {
                vmStatsNameMap.put(vmNames.get(vmUUIDs.indexOf(entry.getKey())), entry.getValue());
            }

            return new GetVmStatsAnswer(command, vmStatsNameMap);
        } catch (final XenAPIException e) {
            final String msg = "Unable to get VM stats" + e.toString();
            s_logger.warn(msg, e);
            return new GetVmStatsAnswer(command, vmStatsNameMap);
        } catch (final XmlRpcException e) {
            final String msg = "Unable to get VM stats" + e.getMessage();
            s_logger.warn(msg, e);
            return new GetVmStatsAnswer(command, vmStatsNameMap);
        }
    }
}
