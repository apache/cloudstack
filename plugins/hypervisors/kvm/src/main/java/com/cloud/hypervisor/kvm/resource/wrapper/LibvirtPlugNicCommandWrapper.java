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
import org.apache.cloudstack.utils.security.ParserUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ResourceWrapper(handles =  PlugNicCommand.class)
public final class LibvirtPlugNicCommandWrapper extends CommandWrapper<PlugNicCommand, Answer, LibvirtComputingResource> {

    /** Highest PCI slot number on a bus (0x00 is the host bridge). */
    private static final int MAX_PCI_SLOT = 0x1f;

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

            // Pin the PCI slot to the lowest free one above the existing NICs so the guest sees a
            // deterministic, monotonic NIC order across hot-plugs (see findNextAvailablePciSlot).
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
     * Picks the PCI slot for the NIC being hot-plugged: the lowest free slot above the highest slot
     * already used by a NIC. The choice is deterministic and monotonic with respect to the NICs that
     * are already present (a new NIC never lands below an existing one), which is what keeps the
     * guest's predictable interface names stable across hot-plugs. It does not guarantee contiguity:
     * slots between the last NIC and the new one may already be taken by other devices (disks,
     * controllers, balloon), in which case the next free slot above them is used.
     *
     * @return the slot to assign, or {@code null} to let libvirt auto-assign (domain XML unavailable
     *         or unparseable, or no free slot left).
     */
    protected Integer findNextAvailablePciSlot(final Domain vm, final List<InterfaceDef> pluggedNics) {
        try {
            final String domXml = vm.getXMLDesc(0);
            // getXMLDesc can return null on certain libvirt error paths; fall back to libvirt's own choice.
            if (domXml == null) {
                logger.debug("Domain XML unavailable, letting libvirt auto-assign PCI slot");
                return null;
            }
            final Set<Integer> usedSlots = getUsedPciSlots(domXml);
            if (usedSlots == null) {
                return null;
            }
            final Integer slot = getFirstFreeSlotAbove(getHighestNicSlot(pluggedNics), usedSlots);
            if (slot == null) {
                logger.warn("No free PCI slots available, letting libvirt auto-assign");
            }
            return slot;
        } catch (final LibvirtException e) {
            logger.warn("Failed to get domain XML for PCI slot calculation, letting libvirt auto-assign", e);
            return null;
        }
    }

    /**
     * Collects the slot numbers of every {@code <address type='pci' .../>} element in the domain XML,
     * whichever device or bus they belong to. Returns {@code null} if the XML cannot be parsed.
     */
    protected Set<Integer> getUsedPciSlots(final String domXml) {
        final Set<Integer> usedSlots = new HashSet<>();
        try {
            final DocumentBuilder builder = ParserUtils.getSaferDocumentBuilderFactory().newDocumentBuilder();
            final Document doc = builder.parse(new InputSource(new StringReader(domXml)));
            final NodeList addresses = doc.getElementsByTagName("address");
            for (int i = 0; i < addresses.getLength(); i++) {
                final Element address = (Element) addresses.item(i);
                if (!"pci".equals(address.getAttribute("type")) || address.getAttribute("slot").isEmpty()) {
                    continue;
                }
                usedSlots.add(Integer.decode(address.getAttribute("slot")));
            }
        } catch (final ParserConfigurationException | SAXException | IOException | NumberFormatException e) {
            logger.warn("Failed to parse domain XML for PCI slot calculation, letting libvirt auto-assign", e);
            return null;
        }
        return usedSlots;
    }

    /** Highest PCI slot used by an existing NIC, or 0 when no NIC carries a slot. */
    protected static int getHighestNicSlot(final List<InterfaceDef> pluggedNics) {
        int highest = 0;
        for (final InterfaceDef pluggedNic : pluggedNics) {
            if (pluggedNic.getSlot() != null && pluggedNic.getSlot() > highest) {
                highest = pluggedNic.getSlot();
            }
        }
        return highest;
    }

    /**
     * Lowest slot strictly above {@code from} (and no higher than {@link #MAX_PCI_SLOT}) that is not in
     * {@code usedSlots}; {@code null} when the range is exhausted. Slot 0 is reserved for the host bridge.
     */
    protected static Integer getFirstFreeSlotAbove(final int from, final Set<Integer> usedSlots) {
        for (int slot = from + 1; slot <= MAX_PCI_SLOT; slot++) {
            if (!usedSlots.contains(slot)) {
                return slot;
            }
        }
        return null;
    }
}
