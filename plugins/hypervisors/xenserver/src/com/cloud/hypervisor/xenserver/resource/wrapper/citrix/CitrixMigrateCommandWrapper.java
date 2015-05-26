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

package com.cloud.hypervisor.xenserver.resource.wrapper.citrix;

import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  MigrateCommand.class)
public final class CitrixMigrateCommandWrapper extends CommandWrapper<MigrateCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixMigrateCommandWrapper.class);

    @Override
    public Answer execute(final MigrateCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final String vmName = command.getVmName();

        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);

            final String ipaddr = command.getDestinationIp();

            final Set<Host> hosts = Host.getAll(conn);
            Host dsthost = null;
            if(hosts != null) {
                for (final Host host : hosts) {
                    if (host.getAddress(conn).equals(ipaddr)) {
                        dsthost = host;
                        break;
                    }
                }
            }
            if (dsthost == null) {
                final String msg = "Migration failed due to unable to find host " + ipaddr + " in XenServer pool " + citrixResourceBase.getHost().getPool();
                s_logger.warn(msg);
                return new MigrateAnswer(command, false, msg, null);
            }
            for (final VM vm : vms) {
                final Set<VBD> vbds = vm.getVBDs(conn);
                for (final VBD vbd : vbds) {
                    final VBD.Record vbdRec = vbd.getRecord(conn);
                    if (vbdRec.type.equals(Types.VbdType.CD) && !vbdRec.empty) {
                        vbd.eject(conn);
                        break;
                    }
                }
                citrixResourceBase.migrateVM(conn, dsthost, vm, vmName);
                vm.setAffinity(conn, dsthost);
            }
            return new MigrateAnswer(command, true, "migration succeeded", null);
        } catch (final Exception e) {
            s_logger.warn(e.getMessage(), e);
            return new MigrateAnswer(command, false, e.getMessage(), null);
        }
    }
}