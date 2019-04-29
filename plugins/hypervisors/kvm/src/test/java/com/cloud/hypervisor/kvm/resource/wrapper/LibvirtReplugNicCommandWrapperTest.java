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

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.cloudstack.utils.linux.MemStat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ReplugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.kvm.resource.BridgeVifDriver;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.OvsVifDriver;
import com.cloud.network.Networks;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {Script.class, MemStat.class})
public class LibvirtReplugNicCommandWrapperTest {
    private static final String part_1 =
            "<domain type='kvm' id='143'>\n"
            + "  <name>i-85-285-VM</name>\n"
            + "  <uuid>8825b180-468f-4227-beb7-6b06fd342116</uuid>\n"
            + "  <description>CentOS 5.5 (64-bit)</description>\n"
            + "  <memory unit='KiB'>262144</memory>\n"
            + "  <currentMemory unit='KiB'>262144</currentMemory>\n"
            + "  <vcpu placement='static'>1</vcpu>\n"
            + "  <cputune>\n"
            + "    <shares>256</shares>\n"
            + "  </cputune>\n"
            + "  <sysinfo type='smbios'>\n"
            + "    <system>\n"
            + "      <entry name='manufacturer'>Apache Software Foundation</entry>\n"
            + "      <entry name='product'>CloudStack KVM Hypervisor</entry>\n"
            + "      <entry name='uuid'>8825b180-468f-4227-beb7-6b06fd342116</entry>\n"
            + "    </system>\n"
            + "  </sysinfo>\n"
            + "  <os>\n"
            + "    <type arch='x86_64' machine='pc-i440fx-rhel7.0.0'>hvm</type>\n"
            + "    <boot dev='cdrom'/>\n"
            + "    <boot dev='hd'/>\n"
            + "    <smbios mode='sysinfo'/>\n"
            + "  </os>\n"
            + "  <features>\n"
            + "    <acpi/>\n"
            + "    <apic/>\n"
            + "    <pae/>\n"
            + "  </features>\n"
            + "  <clock offset='utc'>\n"
            + "    <timer name='kvmclock'/>\n"
            + "  </clock>\n"
            + "  <on_poweroff>destroy</on_poweroff>\n"
            + "  <on_reboot>restart</on_reboot>\n"
            + "  <on_crash>destroy</on_crash>\n"
            + "  <devices>\n"
            + "    <emulator>/usr/libexec/qemu-kvm</emulator>\n"
            + "    <disk type='file' device='disk'>\n"
            + "      <driver name='qemu' type='qcow2' cache='none'/>\n"
            + "      <source file='/mnt/4436eeec-abec-3ef8-b733-c9541df20361/0c4aae69-2652-4a04-b460-1abb5a1a695c'/>\n"
            + "      <backingStore type='file' index='1'>\n"
            + "        <format type='raw'/>\n"
            + "        <source file='/mnt/4436eeec-abec-3ef8-b733-c9541df20361/d9ce07e5-9e13-11e7-816b-faac09070700'/>\n"
            + "        <backingStore/>\n"
            + "      </backingStore>\n"
            + "      <target dev='vda' bus='virtio'/>\n"
            + "      <serial>0c4aae6926524a04b460</serial>\n"
            + "      <alias name='virtio-disk0'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>\n"
            + "    </disk>\n"
            + "    <disk type='file' device='cdrom'>\n"
            + "      <driver name='qemu' type='raw' cache='none'/>\n"
            + "      <backingStore/>\n"
            + "      <target dev='hdc' bus='ide'/>\n"
            + "      <readonly/>\n"
            + "      <alias name='ide0-1-0'/>\n"
            + "      <address type='drive' controller='0' bus='1' target='0' unit='0'/>\n"
            + "    </disk>\n"
            + "    <controller type='usb' index='0'>\n"
            + "      <alias name='usb'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>\n"
            + "    </controller>\n"
            + "    <controller type='pci' index='0' model='pci-root'>\n"
            + "      <alias name='pci.0'/>\n"
            + "    </controller>\n"
            + "    <controller type='ide' index='0'>\n"
            + "      <alias name='ide'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x1'/>\n"
            + "    </controller>\n"
            + "    <controller type='virtio-serial' index='0'>\n"
            + "      <alias name='virtio-serial0'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n"
            + "    </controller>\n";
    private static final String part_2 =
            "    <interface type='bridge'>\n"
            + "      <mac address='02:00:7c:98:00:02'/>\n"
            + "      <source bridge='breth2-234'/>\n"
            + "      <bandwidth>\n"
            + "        <inbound average='25600' peak='25600'/>\n"
            + "        <outbound average='25600' peak='25600'/>\n"
            + "      </bandwidth>\n"
            + "      <target dev='vnet10'/>\n"
            + "      <model type='virtio'/>\n"
            + "      <link state='up'/>\n"
            + "      <alias name='net0'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n"
            + "    </interface>\n";
    private static final String part_3 =
            "    <serial type='pty'>\n"
            + "      <source path='/dev/pts/4'/>\n"
            + "      <target port='0'/>\n"
            + "      <alias name='serial0'/>\n"
            + "    </serial>\n"
            + "    <console type='pty' tty='/dev/pts/4'>\n"
            + "      <source path='/dev/pts/4'/>\n"
            + "      <target type='serial' port='0'/>\n"
            + "      <alias name='serial0'/>\n"
            + "    </console>\n"
            + "    <channel type='unix'>\n"
            + "      <source mode='bind' path='/var/lib/libvirt/qemu/i-85-285-VM.org.qemu.guest_agent.0'/>\n"
            + "      <target type='virtio' name='org.qemu.guest_agent.0' state='disconnected'/>\n"
            + "      <alias name='channel0'/>\n"
            + "      <address type='virtio-serial' controller='0' bus='0' port='1'/>\n"
            + "    </channel>\n"
            + "    <input type='tablet' bus='usb'>\n"
            + "      <alias name='input0'/>\n"
            + "    </input>\n"
            + "    <input type='mouse' bus='ps2'/>\n"
            + "    <input type='keyboard' bus='ps2'/>\n"
            + "    <graphics type='vnc' port='5903' autoport='yes' listen='10.100.100.11'>\n"
            + "      <listen type='address' address='10.100.100.11'/>\n"
            + "    </graphics>\n"
            + "    <video>\n"
            + "      <model type='cirrus' vram='16384' heads='1'/>\n"
            + "      <alias name='video0'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n"
            + "    </video>\n"
            + "    <memballoon model='none'>\n"
            + "      <alias name='balloon0'/>\n"
            + "    </memballoon>\n"
            + "  </devices>\n"
            + "</domain>\n";

    private static final String fullfile = part_1 + part_2 + part_3;

    private LibvirtComputingResource res;
    private final Domain _domain = mock(Domain.class);

    final String memInfo = "MemTotal:        5830236 kB\n" +
            "MemFree:          156752 kB\n" +
            "Buffers:          326836 kB\n" +
            "Cached:          2606764 kB\n" +
            "SwapCached:            0 kB\n" +
            "Active:          4260808 kB\n" +
            "Inactive:         949392 kB\n";

    @Before
    public void setUp() throws Exception {
        Scanner scanner = new Scanner(memInfo);
        PowerMockito.whenNew(Scanner.class).withAnyArguments().thenReturn(scanner);

        // Use a spy because we only want to override getVifDriverClass
        LibvirtComputingResource resReal = new LibvirtComputingResource();
        res = spy(resReal);

        Connect conn = mock(Connect.class);
        LibvirtUtilitiesHelper helper = mock(LibvirtUtilitiesHelper.class);

        when(_domain.getXMLDesc(0))
                .thenReturn(fullfile)
                .thenReturn(part_1 + part_3);
        when(conn.domainLookupByName(anyString())).thenReturn(_domain);
        when(helper.getConnectionByVmName(anyString())).thenReturn(conn);
        PowerMockito.mockStatic(Script.class);
        BDDMockito.given(Script.findScript(anyString(), anyString())).willReturn("dummypath/tofile.sh");

        Map<String, String> pifs = new HashMap<>();
        pifs.put("alubr0", "alubr0");

        Map<String, Object> params = new HashMap<>();
        params.put("libvirt.computing.resource", res);
        params.put("libvirt.host.pifs", pifs);

        BridgeVifDriver bridgeVifDriver = spy(new BridgeVifDriver());
        OvsVifDriver ovsVifDriver = spy(new OvsVifDriver());

        doNothing().when(bridgeVifDriver).getPifs();
        doNothing().when(ovsVifDriver).getPifs();

        doReturn(helper).when(res).getLibvirtUtilitiesHelper();
        doReturn(bridgeVifDriver).when(res).getVifDriver(eq(Networks.TrafficType.Guest), anyString());
        doReturn(ovsVifDriver).when(res).getVifDriver(Networks.TrafficType.Guest, "alubr0");
        doReturn(bridgeVifDriver).when(res).getVifDriver(not(eq(Networks.TrafficType.Guest)));
        doReturn(Arrays.asList(bridgeVifDriver, ovsVifDriver)).when(res).getAllVifDrivers();

        bridgeVifDriver.configure(params);
        ovsVifDriver.configure(params);
    }

    @Test
    public void testReplugNic() throws LibvirtException {

        final String expectedDetachXml =
                "<interface type='bridge'>\n"
                        + "<source bridge='breth2-234'/>\n"
                        + "<target dev='vnet10'/>\n"
                        + "<mac address='02:00:7c:98:00:02'/>\n"
                        + "<model type='virtio'/>\n"
                        + "<link state='up'/>\n"
                        + "</interface>\n";
        final String expectedAttachXml =
                "<interface type='bridge'>\n"
                        + "<source bridge='alubr0'/>\n"
                        + "<target dev='vnet10'/>\n"
                        + "<mtu size='1500'/>\n"
                        + "<mac address='02:00:7c:98:00:02'/>\n"
                        + "<model type='virtio'/>\n"
                        + "<virtualport type='openvswitch'>\n"
                        + "</virtualport>\n"
                        + "<link state='down'/>\n"
                        + "<address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n"
                        + "</interface>\n";
        final String expectedUpdateXml =
                "<interface type='bridge'>\n"
                        + "<source bridge='alubr0'/>\n"
                        + "<target dev='vnet10'/>\n"
                        + "<mtu size='1500'/>\n"
                        + "<mac address='02:00:7c:98:00:02'/>\n"
                        + "<model type='virtio'/>\n"
                        + "<virtualport type='openvswitch'>\n"
                        + "</virtualport>\n"
                        + "<link state='up'/>\n"
                        + "<address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n"
                        + "</interface>\n";

        final LibvirtReplugNicCommandWrapper wrapper = new LibvirtReplugNicCommandWrapper();
        final NicTO nic = new NicTO();
        nic.setType(Networks.TrafficType.Guest);
        nic.setName("alubr0");
        nic.setBroadcastType(Networks.BroadcastDomainType.Vsp);
        nic.setMac("02:00:7c:98:00:02");
        nic.setMtu(1500);
        final ReplugNicCommand command = new ReplugNicCommand(nic, "i-85-285-VM", VirtualMachine.Type.User);
        final Answer result = wrapper.execute(command, res);

        verify(_domain).detachDevice(expectedDetachXml);
        verify(_domain).attachDevice(expectedAttachXml);
        verify(_domain).updateDeviceFlags(expectedUpdateXml, LibvirtReplugNicCommandWrapper.DomainAffect.LIVE.getValue());
    }

}
