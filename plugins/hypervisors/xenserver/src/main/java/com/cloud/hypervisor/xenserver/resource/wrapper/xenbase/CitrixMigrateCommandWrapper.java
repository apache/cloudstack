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

import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  MigrateCommand.class)
public class CitrixMigrateCommandWrapper extends CommandWrapper<MigrateCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixMigrateCommandWrapper.class);

    @Override
    public Answer execute(final MigrateCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final String vmName = command.getVmName();
        final String dstHostIpAddr = command.getDestinationIp();

        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);

            final Set<Host> hosts = Host.getAll(conn);
            Host dsthost = null;
            if(hosts != null) {
                for (final Host host : hosts) {
                    if (host.getAddress(conn).equals(dstHostIpAddr)) {
                        dsthost = host;
                        break;
                    }
                }
            }
            if (dsthost == null) {
                final String msg = "Migration failed due to unable to find host " + dstHostIpAddr + " in XenServer pool " + citrixResourceBase.getHost().getPool();
                s_logger.warn(msg);
                return new MigrateAnswer(command, false, msg, null);
            }
            for (final VM vm : vms) {
                final Set<VBD> vbds = vm.getVBDs(conn);
                for (final VBD vbd : vbds) {
                    final VBD.Record vbdRec = vbd.getRecord(conn);
                    if (vbdRec.type.equals(Types.VbdType.CD) && !vbdRec.empty) {
                        vbd.eject(conn);
                        // for config drive vbd destroy the vbd.
                        if (!vbdRec.userdevice.equals(citrixResourceBase._attachIsoDeviceNum)) {
                            if (vbdRec.currentlyAttached) {
                                vbd.destroy(conn);
                            }
                        }
                        continue;
                    }
                }
                citrixResourceBase.migrateVM(conn, dsthost, vm, vmName);
                vm.setAffinity(conn, dsthost);

                destroyMigratedVmNetworkRulesOnSourceHost(command, citrixResourceBase, conn, dsthost);
            }

            // The iso can be attached to vm only once the vm is (present in the host) migrated.
            // Attach the config drive iso device to VM
            VM vm = vms.iterator().next();
            if (!citrixResourceBase.attachConfigDriveIsoToVm(conn, vm)) {
                s_logger.debug("Config drive ISO attach failed after migration for vm "+vmName);
            }

            return new MigrateAnswer(command, true, "migration succeeded", null);
        } catch (final Exception e) {
            s_logger.warn(e.getMessage(), e);
            return new MigrateAnswer(command, false, e.getMessage(), null);
        }
    }

    /**
     * On networks with security group, it removes the network rules related to the migrated VM from the source host.
     */
    protected void destroyMigratedVmNetworkRulesOnSourceHost(final MigrateCommand command, final CitrixResourceBase citrixResourceBase, final Connection conn, Host dsthost)
            throws BadServerResponse, XenAPIException, XmlRpcException {
        if (citrixResourceBase.canBridgeFirewall()) {
            final String result = citrixResourceBase.callHostPlugin(conn, "vmops", "destroy_network_rules_for_vm", "vmName", command.getVmName());
            if (BooleanUtils.toBoolean(result)) {
                s_logger.debug(String.format("Removed network rules from source host [%s] for migrated vm [%s]", dsthost.getHostname(conn), command.getVmName()));
            } else {
                s_logger.warn(String.format("Failed to remove network rules from source host [%s] for migrated vm [%s]", dsthost.getHostname(conn), command.getVmName()));
            }
        }
    }
}
