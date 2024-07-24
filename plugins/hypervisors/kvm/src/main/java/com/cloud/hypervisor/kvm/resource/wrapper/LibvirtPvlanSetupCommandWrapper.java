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
import org.joda.time.Duration;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = PvlanSetupCommand.class)
public final class LibvirtPvlanSetupCommandWrapper extends CommandWrapper<PvlanSetupCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtPvlanSetupCommandWrapper.class);

    @Override
    public Answer execute(final PvlanSetupCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String primaryPvlan = command.getPrimary();
        final String isolatedPvlan = command.getIsolated();
        final String pvlanType = "-" + command.getPvlanType();
        final String op = command.getOp();
        final String dhcpMac = command.getDhcpMac();
        final String vmMac = command.getVmMac() == null ? dhcpMac : command.getVmMac();
        final String dhcpIp = command.getDhcpIp();

        String opr = "-A";
        if (op.equals("delete")) {
            opr = "-D";
        }

        String result = null;

        final String guestBridgeName = libvirtComputingResource.getGuestBridgeName();
        final Duration timeout = libvirtComputingResource.getTimeout();

        if (command.getType() == PvlanSetupCommand.Type.DHCP) {
            final String ovsPvlanDhcpHostPath = libvirtComputingResource.getOvsPvlanDhcpHostPath();
            final Script script = new Script(ovsPvlanDhcpHostPath, timeout, s_logger);

            script.add(opr, pvlanType, "-b", guestBridgeName, "-p", primaryPvlan, "-s", isolatedPvlan, "-m", dhcpMac,
                    "-d", dhcpIp);
            result = script.execute();

            if (result != null) {
                s_logger.warn("Failed to program pvlan for dhcp server with mac " + dhcpMac);
            } else {
                s_logger.info("Programmed pvlan for dhcp server with mac " + dhcpMac);
            }
        }

        // We run this even for DHCP servers since they're all vms after all
        final String ovsPvlanVmPath = libvirtComputingResource.getOvsPvlanVmPath();
        final Script script = new Script(ovsPvlanVmPath, timeout, s_logger);
        script.add(opr, pvlanType, "-b", guestBridgeName, "-p", primaryPvlan, "-s", isolatedPvlan, "-m", vmMac);
        result = script.execute();

        if (result != null) {
            s_logger.warn("Failed to program pvlan for vm with mac " + vmMac);
            return new Answer(command, false, result);
        } else {
            s_logger.info("Programmed pvlan for vm with mac " + vmMac);
        }

        return new Answer(command, true, result);
    }
}
