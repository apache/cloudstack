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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtPlugNicCommandWrapperTest {

    /**
     * Typical q35-less layout: NICs at 0x03 and 0x06, other PCI devices at 0x01, 0x02, 0x04, 0x05 and 0x07,
     * plus non-PCI addresses (drive, usb) that must be ignored.
     */
    private static final String DOMAIN_XML =
            "<domain type='kvm'>\n"
            + "  <name>i-2-42-VM</name>\n"
            + "  <devices>\n"
            + "    <disk type='file' device='disk'>\n"
            + "      <target dev='vda' bus='virtio'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>\n"
            + "    </disk>\n"
            + "    <disk type='file' device='cdrom'>\n"
            + "      <target dev='hdc' bus='ide'/>\n"
            + "      <address type='drive' controller='0' bus='1' target='0' unit='0'/>\n"
            + "    </disk>\n"
            + "    <controller type='usb' index='0'>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>\n"
            + "    </controller>\n"
            + "    <controller type='virtio-serial' index='0'>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n"
            + "    </controller>\n"
            + "    <interface type='bridge'>\n"
            + "      <mac address='02:00:7c:98:00:01'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n"
            + "    </interface>\n"
            + "    <interface type='bridge'>\n"
            + "      <mac address='02:00:7c:98:00:02'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>\n"
            + "    </interface>\n"
            + "    <channel type='unix'>\n"
            + "      <address type='virtio-serial' controller='0' bus='0' port='1'/>\n"
            + "    </channel>\n"
            + "    <video>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n"
            + "    </video>\n"
            + "    <memballoon model='virtio'>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>\n"
            + "    </memballoon>\n"
            + "  </devices>\n"
            + "</domain>\n";

    @Mock
    private Domain domain;

    private final LibvirtPlugNicCommandWrapper wrapper = new LibvirtPlugNicCommandWrapper();

    private static InterfaceDef nicAtSlot(final Integer slot) {
        final InterfaceDef nic = new InterfaceDef();
        nic.setSlot(slot);
        return nic;
    }

    private static List<InterfaceDef> nicsFromXml() {
        return Arrays.asList(nicAtSlot(0x03), nicAtSlot(0x06));
    }

    @Test
    public void getUsedPciSlotsOnlyCountsPciAddresses() {
        final Set<Integer> used = wrapper.getUsedPciSlots(DOMAIN_XML);
        assertEquals(Set.of(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07), used);
    }

    @Test
    public void getUsedPciSlotsReturnsNullOnMalformedXml() {
        assertNull(wrapper.getUsedPciSlots("<domain><devices><interface>"));
    }

    @Test
    public void getHighestNicSlotIgnoresNicsWithoutAddress() {
        assertEquals(0x06, LibvirtPlugNicCommandWrapper.getHighestNicSlot(Arrays.asList(nicAtSlot(0x03), nicAtSlot(null), nicAtSlot(0x06))));
        assertEquals(0, LibvirtPlugNicCommandWrapper.getHighestNicSlot(Collections.emptyList()));
    }

    @Test
    public void getFirstFreeSlotAboveSkipsOccupiedSlotsAndStopsAtBusEnd() {
        assertEquals(Integer.valueOf(0x08), LibvirtPlugNicCommandWrapper.getFirstFreeSlotAbove(0x06, Set.of(0x07)));
        assertEquals(Integer.valueOf(0x07), LibvirtPlugNicCommandWrapper.getFirstFreeSlotAbove(0x06, Collections.emptySet()));
        assertNull(LibvirtPlugNicCommandWrapper.getFirstFreeSlotAbove(0x1f, Collections.emptySet()));
    }

    @Test
    public void findNextAvailablePciSlotPicksLowestFreeSlotAboveLastNic() throws LibvirtException {
        when(domain.getXMLDesc(0)).thenReturn(DOMAIN_XML);
        // 0x07 is taken by the balloon, so the NIC goes to 0x08: monotonic after the last NIC, not contiguous.
        assertEquals(Integer.valueOf(0x08), wrapper.findNextAvailablePciSlot(domain, nicsFromXml()));
    }

    @Test
    public void findNextAvailablePciSlotFallsBackWhenXmlUnavailable() throws LibvirtException {
        when(domain.getXMLDesc(0)).thenReturn(null);
        assertNull(wrapper.findNextAvailablePciSlot(domain, nicsFromXml()));
    }

    @Test
    public void findNextAvailablePciSlotFallsBackWhenLibvirtFails() throws LibvirtException {
        when(domain.getXMLDesc(0)).thenThrow(Mockito.mock(LibvirtException.class));
        assertNull(wrapper.findNextAvailablePciSlot(domain, nicsFromXml()));
    }

    @Test
    public void findNextAvailablePciSlotFallsBackWhenBusIsFull() throws LibvirtException {
        when(domain.getXMLDesc(0)).thenReturn(DOMAIN_XML);
        assertNull(wrapper.findNextAvailablePciSlot(domain, Collections.singletonList(nicAtSlot(0x1f))));
    }
}
