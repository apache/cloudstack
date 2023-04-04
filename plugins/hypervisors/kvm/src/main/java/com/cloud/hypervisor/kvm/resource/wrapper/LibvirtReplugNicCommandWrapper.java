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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.List;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ReplugNicAnswer;
import com.cloud.agent.api.ReplugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  ReplugNicCommand.class)
public final class LibvirtReplugNicCommandWrapper extends CommandWrapper<ReplugNicCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtReplugNicCommandWrapper.class);
    public enum DomainAffect {
        CURRENT(0), LIVE(1), CONFIG(2), BOTH(3);

        private int value;
        DomainAffect(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public Answer execute(final ReplugNicCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final NicTO nic = command.getNic();
        final String vmName = command.getVmName();
        Domain vm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            vm = libvirtComputingResource.getDomain(conn, vmName);

            InterfaceDef oldPluggedNic = findPluggedNic(libvirtComputingResource, nic, vmName, conn);

            final VifDriver newVifDriver = libvirtComputingResource.getVifDriver(nic.getType(), nic.getName());
            final InterfaceDef interfaceDef = newVifDriver.plug(nic, "Other PV", oldPluggedNic.getModel().toString(), null);

            interfaceDef.setSlot(oldPluggedNic.getSlot());
            interfaceDef.setDevName(oldPluggedNic.getDevName());
            interfaceDef.setLinkStateUp(false);

            oldPluggedNic.setSlot(null);

            int i = 0;
            do {
                i++;
                s_logger.debug("ReplugNic: Detaching interface" + oldPluggedNic + " (Attempt: " + i + ")");
                vm.detachDevice(oldPluggedNic.toString());
            } while (findPluggedNic(libvirtComputingResource, nic, vmName, conn) != null && i <= 10);

            s_logger.debug("ReplugNic: Attaching interface" + interfaceDef);
            vm.attachDevice(interfaceDef.toString());

            interfaceDef.setLinkStateUp(true);
            s_logger.debug("ReplugNic: Updating interface" + interfaceDef);
            vm.updateDeviceFlags(interfaceDef.toString(), DomainAffect.LIVE.getValue());

            // We don't know which "traffic type" is associated with
            // each interface at this point, so inform all vif drivers
            for (final VifDriver vifDriver : libvirtComputingResource.getAllVifDrivers()) {
                vifDriver.unplug(oldPluggedNic, true);
            }

            return new ReplugNicAnswer(command, true, "success");
        } catch (final LibvirtException | InternalErrorException e) {
            final String msg = " Plug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new ReplugNicAnswer(command, false, msg);
        } finally {
            if (vm != null) {
                try {
                    vm.free();
                } catch (final LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }

    private InterfaceDef findPluggedNic(LibvirtComputingResource libvirtComputingResource, NicTO nic, String vmName, Connect conn) {
        InterfaceDef oldPluggedNic = null;

        final List<InterfaceDef> pluggedNics = libvirtComputingResource.getInterfaces(conn, vmName);

        for (final InterfaceDef pluggedNic : pluggedNics) {
            if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                oldPluggedNic = pluggedNic;
            }
        }

        return oldPluggedNic;
    }
}
