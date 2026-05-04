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
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.VirtualMachine;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ResourceWrapper(handles =  PlugNicCommand.class)
public final class LibvirtPlugNicCommandWrapper extends CommandWrapper<PlugNicCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final PlugNicCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final NicTO nic = command.getNic();
        final String vmName = command.getVmName();
        final VirtualMachine.Type vmType = command.getVMType();
        Domain vm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            vm = libvirtComputingResource.getDomain(conn, vmName);

            final List<InterfaceDef> pluggedNics = libvirtComputingResource.getInterfaces(conn, vmName);
            Integer nicnum = 0;
            for (final InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                    logger.debug("found existing nic for mac " + pluggedNic.getMacAddress() + " at index " + nicnum);
                    return new PlugNicAnswer(command, true, "success");
                }
                nicnum++;
            }
            final VifDriver vifDriver = libvirtComputingResource.getVifDriver(nic.getType(), nic.getName());
            final InterfaceDef interfaceDef = vifDriver.plug(nic, "Other PV", "", null);
            if (command.getDetails() != null) {
                libvirtComputingResource.setInterfaceDefQueueSettings(command.getDetails(), null, interfaceDef);
            }

            // Explicitly assign PCI slot to ensure sequential NIC naming in the guest.
            // Without this, libvirt auto-assigns the next free PCI slot which may be
            // non-sequential with existing NICs (e.g. ens9 instead of ens5), causing
            // guest network configuration to fail.
            Integer nextSlot = findNextAvailablePciSlot(vm, pluggedNics);
            if (nextSlot != null) {
                interfaceDef.setSlot(nextSlot);
                logger.debug("Assigning PCI slot 0x" + String.format("%02x", nextSlot) + " to hot-plugged NIC");
            }

            vm.attachDevice(interfaceDef.toString());

            // apply default network rules on new nic
            if (vmType == VirtualMachine.Type.User && nic.isSecurityGroupEnabled()) {
                final Long vmId = Long.valueOf(vmName.split("-")[2]);
                libvirtComputingResource.applyDefaultNetworkRulesOnNic(conn, vmName, vmId, nic, false, false);
            }

            if (vmType == VirtualMachine.Type.User) {
                libvirtComputingResource.detachAndAttachConfigDriveISO(conn, vmName);
            }

            return new PlugNicAnswer(command, true, "success");
        } catch (final LibvirtException e) {
            final String msg = " Plug Nic failed due to " + e.toString();
            logger.warn(msg, e);
            return new PlugNicAnswer(command, false, msg);
        } catch (final InternalErrorException e) {
            final String msg = " Plug Nic failed due to " + e.toString();
            logger.warn(msg, e);
            return new PlugNicAnswer(command, false, msg);
        } finally {
            if (vm != null) {
                try {
                    vm.free();
                } catch (final LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }

    /**
     * Finds the next available PCI slot for a hot-plugged NIC by examining
     * all PCI slots currently in use by the domain. This ensures the new NIC
     * gets a sequential PCI address relative to existing NICs, resulting in
     * predictable interface naming in the guest OS (e.g. ens5 instead of ens9).
     */
    private Integer findNextAvailablePciSlot(final Domain vm, final List<InterfaceDef> pluggedNics) {
        try {
            String domXml = vm.getXMLDesc(0);

            // Parse all PCI slot numbers currently in use
            Set<Integer> usedSlots = new HashSet<>();
            Pattern slotPattern = Pattern.compile("slot='0x([0-9a-fA-F]+)'");
            Matcher matcher = slotPattern.matcher(domXml);
            while (matcher.find()) {
                usedSlots.add(Integer.parseInt(matcher.group(1), 16));
            }

            // Find the highest PCI slot used by existing NICs
            int maxNicSlot = 0;
            for (InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getSlot() != null && pluggedNic.getSlot() > maxNicSlot) {
                    maxNicSlot = pluggedNic.getSlot();
                }
            }

            // Find next free slot starting from maxNicSlot + 1
            // PCI slots range from 0x01 to 0x1f (slot 0 is reserved for host bridge)
            for (int slot = maxNicSlot + 1; slot <= 0x1f; slot++) {
                if (!usedSlots.contains(slot)) {
                    return slot;
                }
            }

            logger.warn("No free PCI slots available, letting libvirt auto-assign");
            return null;
        } catch (LibvirtException e) {
            logger.warn("Failed to get domain XML for PCI slot calculation, letting libvirt auto-assign", e);
            return null;
        }
    }
}
