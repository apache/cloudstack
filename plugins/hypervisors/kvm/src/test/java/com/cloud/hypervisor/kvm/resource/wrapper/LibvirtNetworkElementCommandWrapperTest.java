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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.network.Networks;
import com.cloud.utils.ExecutionResult;
import org.apache.cloudstack.utils.linux.MemStat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Scanner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {MemStat.class})
public class LibvirtNetworkElementCommandWrapperTest {
    private static final String fullfile = "<domain type='kvm' id='143'>\n"
            + "  <name>r-3-VM</name>\n"
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
            + "    </controller>\n"
            + "    <interface type='bridge'>\n"
            + "      <mac address='02:00:7c:98:00:01'/>\n"
            + "      <source bridge='cloud0'/>\n"
            + "      <bandwidth>\n"
            + "        <inbound average='25600' peak='25600'/>\n"
            + "        <outbound average='25600' peak='25600'/>\n"
            + "      </bandwidth>\n"
            + "      <target dev='vnet1'/>\n"
            + "      <model type='virtio'/>\n"
            + "      <virtualport type='openvswitch'>\n"
            + "      </virtualport>\n"
            + "      <link state='up'/>\n"
            + "      <alias name='net0'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n"
            + "    </interface>\n"
            + "    <interface type='bridge'>\n"
            + "      <mac address='02:00:7c:98:00:02'/>\n"
            + "      <source bridge='publicbr'/>\n"
            + "      <bandwidth>\n"
            + "        <inbound average='25600' peak='25600'/>\n"
            + "        <outbound average='25600' peak='25600'/>\n"
            + "      </bandwidth>\n"
            + "      <target dev='vnet2'/>\n"
            + "      <model type='virtio'/>\n"
            + "      <virtualport type='openvswitch'>\n"
            + "      </virtualport>\n"
            + "      <link state='up'/>\n"
            + "      <alias name='net1'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n"
            + "    </interface>\n"
            + "    <interface type='bridge'>\n"
            + "      <mac address='02:00:7c:98:00:03'/>\n"
            + "      <source bridge='guestbr-100'/>\n"
            + "      <bandwidth>\n"
            + "        <inbound average='25600' peak='25600'/>\n"
            + "        <outbound average='25600' peak='25600'/>\n"
            + "      </bandwidth>\n"
            + "      <target dev='vnet3'/>\n"
            + "      <model type='virtio'/>\n"
            + "      <virtualport type='openvswitch'>\n"
            + "      </virtualport>\n"
            + "      <link state='up'/>\n"
            + "      <alias name='net2'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>\n"
            + "    </interface>\n"
            + "    <interface type='bridge'>\n"
            + "      <mac address='02:00:7c:98:00:04'/>\n"
            + "      <source bridge='guestbr-101'/>\n"
            + "      <bandwidth>\n"
            + "        <inbound average='25600' peak='25600'/>\n"
            + "        <outbound average='25600' peak='25600'/>\n"
            + "      </bandwidth>\n"
            + "      <target dev='vnet4'/>\n"
            + "      <model type='virtio'/>\n"
            + "      <virtualport type='openvswitch'>\n"
            + "      </virtualport>\n"
            + "      <link state='up'/>\n"
            + "      <alias name='net3'/>\n"
            + "      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>\n"
            + "    </interface>\n"
            + "    <serial type='pty'>\n"
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
        LibvirtComputingResource resReal = new LibvirtComputingResource() {
            {
                _linkLocalBridgeName = "cloud0";
                _guestBridgeName = "guestbr";
                _publicBridgeName = "publicbr";
                _privBridgeName = "mgmtbr";
            }
        };

        res = spy(resReal);

        Connect conn = mock(Connect.class);
        LibvirtUtilitiesHelper helper = mock(LibvirtUtilitiesHelper.class);

        when(_domain.getXMLDesc(0)).thenReturn(fullfile);
        when(conn.domainLookupByName(anyString())).thenReturn(_domain);
        when(helper.getConnectionByVmName(anyString())).thenReturn(conn);

        doReturn(helper).when(res).getLibvirtUtilitiesHelper();
    }

    @Test
    public void testPrepareIpAssocVpcCommand() throws LibvirtException {
        IpAddressTO ip = new IpAddressTO(1, "171.31.1.3",
                true, false, false,
                "vlan://untagged",
                "172.31.1.1",
                "255.255.0.0",
                "02:00:7c:98:00:02",
                0,
                true);
        ip.setTrafficType(Networks.TrafficType.Public);
        IpAddressTO[] ips = new IpAddressTO[] {
                ip
        };
        final IpAssocVpcCommand command = new IpAssocVpcCommand(ips);
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, "127.0.0.1");
        ExecutionResult result = res.prepareCommand(command);

        assertEquals(1, ips[0].getNicDevId().intValue());
    }

    @Test
    public void testVpcPrivateGateway() throws LibvirtException {
        IpAddressTO ip = new IpAddressTO(1, "171.31.1.3",
                                         true, false, false,
                                         "vlan://untagged",
                                         "172.31.1.1",
                                         "255.255.0.0",
                                         "02:00:7c:98:00:03",
                                         0,
                                         false);
        ip.setTrafficType(Networks.TrafficType.Guest);
        IpAddressTO[] ips = new IpAddressTO[] {
                ip
        };
        final IpAssocVpcCommand command = new IpAssocVpcCommand(ips);
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, "127.0.0.1");
        ExecutionResult result = res.prepareCommand(command);

        assertEquals(2, ips[0].getNicDevId().intValue());
    }

}
