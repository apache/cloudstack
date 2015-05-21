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

import java.util.List;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.utils.script.Script;

public final class LibvirtPvlanSetupCommandWrapper extends CommandWrapper<PvlanSetupCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtPvlanSetupCommandWrapper.class);

    @Override
    public Answer execute(final PvlanSetupCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String primaryPvlan = command.getPrimary();
        final String isolatedPvlan = command.getIsolated();
        final String op = command.getOp();
        final String dhcpName = command.getDhcpName();
        final String dhcpMac = command.getDhcpMac();
        final String dhcpIp = command.getDhcpIp();
        final String vmMac = command.getVmMac();
        boolean add = true;

        String opr = "-A";
        if (op.equals("delete")) {
            opr = "-D";
            add = false;
        }

        String result = null;
        try {
            final String guestBridgeName = libvirtComputingResource.getGuestBridgeName();
            final int timeout = libvirtComputingResource.getTimeout();

            if (command.getType() == PvlanSetupCommand.Type.DHCP) {
                final String ovsPvlanDhcpHostPath = libvirtComputingResource.getOvsPvlanDhcpHostPath();
                final Script script = new Script(ovsPvlanDhcpHostPath, timeout, s_logger);

                if (add) {
                    final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
                    final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(dhcpName);

                    final List<InterfaceDef> ifaces = libvirtComputingResource.getInterfaces(conn, dhcpName);
                    final InterfaceDef guestNic = ifaces.get(0);
                    script.add(opr, "-b", guestBridgeName, "-p", primaryPvlan, "-i", isolatedPvlan, "-n", dhcpName, "-d", dhcpIp, "-m", dhcpMac, "-I",
                            guestNic.getDevName());
                } else {
                    script.add(opr, "-b", guestBridgeName, "-p", primaryPvlan, "-i", isolatedPvlan, "-n", dhcpName, "-d", dhcpIp, "-m", dhcpMac);
                }

                result = script.execute();

                if (result != null) {
                    s_logger.warn("Failed to program pvlan for dhcp server with mac " + dhcpMac);
                    return new Answer(command, false, result);
                } else {
                    s_logger.info("Programmed pvlan for dhcp server with mac " + dhcpMac);
                }
            } else if (command.getType() == PvlanSetupCommand.Type.VM) {
                final String ovsPvlanVmPath = libvirtComputingResource.getOvsPvlanVmPath();

                final Script script = new Script(ovsPvlanVmPath, timeout, s_logger);
                script.add(opr, "-b", guestBridgeName, "-p", primaryPvlan, "-i", isolatedPvlan, "-v", vmMac);
                result = script.execute();

                if (result != null) {
                    s_logger.warn("Failed to program pvlan for vm with mac " + vmMac);
                    return new Answer(command, false, result);
                } else {
                    s_logger.info("Programmed pvlan for vm with mac " + vmMac);
                }
            }
        } catch (final LibvirtException e) {
            s_logger.error("Error whislt executing OVS Setup command! ==> " + e.getMessage());
            return new Answer(command, false, e.getMessage());
        }
        return new Answer(command, true, result);
    }
}