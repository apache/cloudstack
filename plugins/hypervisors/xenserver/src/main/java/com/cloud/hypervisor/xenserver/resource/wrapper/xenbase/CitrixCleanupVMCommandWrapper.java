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

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CleanupVMCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  CleanupVMCommand.class)
public class CitrixCleanupVMCommandWrapper extends CommandWrapper<CleanupVMCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCleanupVMCommandWrapper.class);

    @Override
    public Answer execute(final CleanupVMCommand command, final CitrixResourceBase citrixResourceBase) {
        if (!citrixResourceBase.isDestroyHaltedVms()) {
            s_logger.debug(String.format("Cleanup VM is not needed for host with version %s",
                    citrixResourceBase.getHost().getProductVersion()));
            return new Answer(command);
        }
        final String vmName = command.getVmName();
        try {
            final Connection conn = citrixResourceBase.getConnection();
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if (vms.isEmpty()) {
                return new Answer(command, false, "VM does not exist");
            }
            // destroy vm which is in HALTED state on this host
            final Iterator<VM> iter = vms.iterator();
            while (iter.hasNext()) {
                final VM vm = iter.next();
                final VM.Record vmr = vm.getRecord(conn);
                if (!Types.VmPowerState.HALTED.equals(vmr.powerState)) {
                    final String msg = String.format("VM %s is not in %s state", vmName, Types.VmPowerState.HALTED);
                    s_logger.error(msg);
                    return new Answer(command, false, msg);
                }
                if (citrixResourceBase.isRefNull(vmr.residentOn)) {
                    continue;
                }
                if (vmr.residentOn.getUuid(conn).equals(citrixResourceBase.getHost().getUuid())) {
                    continue;
                }
                iter.remove();
            }
            for (final VM vm : vms) {
                citrixResourceBase.destroyVm(vm, conn, true);
            }

        } catch (final Exception e) {
            final String msg = String.format("Clean up VM %s fail due to %s", vmName, e);
            s_logger.error(msg, e);
            return new Answer(command, false, e.getMessage());
        }
        return new Answer(command);
    }
}
