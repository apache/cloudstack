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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.exception.CloudRuntimeException;

public class LibvirtMigrateCommandWrapperTest {
    String fullfile =
"<domain type='kvm' id='4'>\n" +
"  <name>i-6-6-VM</name>\n" +
"  <uuid>f197b32b-8da2-4a57-bb8a-d01bacc5cd33</uuid>\n" +
"  <description>Other PV (64-bit)</description>\n" +
"  <memory unit='KiB'>262144</memory>\n" +
"  <currentMemory unit='KiB'>262144</currentMemory>\n" +
"  <vcpu placement='static'>1</vcpu>\n" +
"  <cputune>\n" +
"    <shares>100</shares>\n" +
"  </cputune>\n" +
"  <resource>\n" +
"    <partition>/machine</partition>\n" +
"  </resource>\n" +
"  <sysinfo type='smbios'>\n" +
"    <system>\n" +
"      <entry name='manufacturer'>Apache Software Foundation</entry>\n" +
"      <entry name='product'>CloudStack KVM Hypervisor</entry>\n" +
"      <entry name='uuid'>f197b32b-8da2-4a57-bb8a-d01bacc5cd33</entry>\n" +
"    </system>\n" +
"  </sysinfo>\n" +
"  <os>\n" +
"    <type arch='x86_64' machine='pc-i440fx-rhel7.0.0'>hvm</type>\n" +
"    <boot dev='cdrom'/>\n" +
"    <boot dev='hd'/>\n" +
"    <smbios mode='sysinfo'/>\n" +
"  </os>\n" +
"  <features>\n" +
"    <acpi/>\n" +
"    <apic/>\n" +
"    <pae/>\n" +
"  </features>\n" +
"  <clock offset='utc'>\n" +
"    <timer name='kvmclock'/>\n" +
"  </clock>\n" +
"  <on_poweroff>destroy</on_poweroff>\n" +
"  <on_reboot>restart</on_reboot>\n" +
"  <on_crash>destroy</on_crash>\n" +
"  <devices>\n" +
"    <emulator>/usr/libexec/qemu-kvm</emulator>\n" +
"    <disk type='file' device='disk'>\n" +
"      <driver name='qemu' type='qcow2' cache='none'/>\n" +
"      <source file='/mnt/812ea6a3-7ad0-30f4-9cab-01e3f2985b98/4650a2f7-fce5-48e2-beaa-bcdf063194e6'/>\n" +
"      <backingStore type='file' index='1'>\n" +
"        <format type='raw'/>\n" +
"        <source file='/mnt/812ea6a3-7ad0-30f4-9cab-01e3f2985b98/bb4d4df4-c004-11e5-94ed-5254001daa61'/>\n" +
"        <backingStore/>\n" +
"      </backingStore>\n" +
"      <target dev='vda' bus='virtio'/>\n" +
"      <serial>4650a2f7fce548e2beaa</serial>\n" +
"      <alias name='virtio-disk0'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n" +
"    </disk>\n" +
"    <disk type='file' device='cdrom'>\n" +
"      <driver name='qemu' type='raw' cache='none'/>\n" +
"      <backingStore/>\n" +
"      <target dev='hdc' bus='ide'/>\n" +
"      <readonly/>\n" +
"      <alias name='ide0-1-0'/>\n" +
"      <address type='drive' controller='0' bus='1' target='0' unit='0'/>\n" +
"    </disk>\n" +
"    <controller type='usb' index='0'>\n" +
"      <alias name='usb'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>\n" +
"    </controller>\n" +
"    <controller type='pci' index='0' model='pci-root'>\n" +
"      <alias name='pci.0'/>\n" +
"    </controller>\n" +
"    <controller type='ide' index='0'>\n" +
"      <alias name='ide'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x1'/>\n" +
"    </controller>\n" +
"    <interface type='bridge'>\n" +
"      <mac address='06:fe:b4:00:00:06'/>\n" +
"      <source bridge='breth0-50'/>\n" +
"      <bandwidth>\n" +
"        <inbound average='25600' peak='25600'/>\n" +
"        <outbound average='25600' peak='25600'/>\n" +
"      </bandwidth>\n" +
"      <target dev='vnet4'/>\n" +
"      <model type='virtio'/>\n" +
"      <alias name='net0'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n" +
"    </interface>\n" +
"    <serial type='pty'>\n" +
"      <source path='/dev/pts/2'/>\n" +
"      <target port='0'/>\n" +
"      <alias name='serial0'/>\n" +
"    </serial>\n" +
"    <console type='pty' tty='/dev/pts/2'>\n" +
"      <source path='/dev/pts/2'/>\n" +
"      <target type='serial' port='0'/>\n" +
"      <alias name='serial0'/>\n" +
"    </console>\n" +
"    <input type='tablet' bus='usb'>\n" +
"      <alias name='input0'/>\n" +
"    </input>\n" +
"    <input type='mouse' bus='ps2'/>\n" +
"    <input type='keyboard' bus='ps2'/>\n" +
"    <graphics type='vnc' port='5902' autoport='yes' listen='192.168.22.22'>\n" +
"      <listen type='address' address='192.168.22.22'/>\n" +
"    </graphics>\n" +
"    <video>\n" +
"      <model type='cirrus' vram='16384' heads='1'/>\n" +
"      <alias name='video0'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
"    </video>\n" +
"    <memballoon model='none'>\n" +
"      <alias name='balloon0'/>\n" +
"    </memballoon>\n" +
"  </devices>\n" +
"</domain>";
    String targetfile =
"<domain type='kvm' id='4'>\n" +
"  <name>i-6-6-VM</name>\n" +
"  <uuid>f197b32b-8da2-4a57-bb8a-d01bacc5cd33</uuid>\n" +
"  <description>Other PV (64-bit)</description>\n" +
"  <memory unit='KiB'>262144</memory>\n" +
"  <currentMemory unit='KiB'>262144</currentMemory>\n" +
"  <vcpu placement='static'>1</vcpu>\n" +
"  <cputune>\n" +
"    <shares>100</shares>\n" +
"  </cputune>\n" +
"  <resource>\n" +
"    <partition>/machine</partition>\n" +
"  </resource>\n" +
"  <sysinfo type='smbios'>\n" +
"    <system>\n" +
"      <entry name='manufacturer'>Apache Software Foundation</entry>\n" +
"      <entry name='product'>CloudStack KVM Hypervisor</entry>\n" +
"      <entry name='uuid'>f197b32b-8da2-4a57-bb8a-d01bacc5cd33</entry>\n" +
"    </system>\n" +
"  </sysinfo>\n" +
"  <os>\n" +
"    <type arch='x86_64' machine='pc-i440fx-rhel7.0.0'>hvm</type>\n" +
"    <boot dev='cdrom'/>\n" +
"    <boot dev='hd'/>\n" +
"    <smbios mode='sysinfo'/>\n" +
"  </os>\n" +
"  <features>\n" +
"    <acpi/>\n" +
"    <apic/>\n" +
"    <pae/>\n" +
"  </features>\n" +
"  <clock offset='utc'>\n" +
"    <timer name='kvmclock'/>\n" +
"  </clock>\n" +
"  <on_poweroff>destroy</on_poweroff>\n" +
"  <on_reboot>restart</on_reboot>\n" +
"  <on_crash>destroy</on_crash>\n" +
"  <devices>\n" +
"    <emulator>/usr/libexec/qemu-kvm</emulator>\n" +
"    <disk type='file' device='disk'>\n" +
"      <driver name='qemu' type='qcow2' cache='none'/>\n" +
"      <source file='/mnt/812ea6a3-7ad0-30f4-9cab-01e3f2985b98/4650a2f7-fce5-48e2-beaa-bcdf063194e6'/>\n" +
"      <backingStore type='file' index='1'>\n" +
"        <format type='raw'/>\n" +
"        <source file='/mnt/812ea6a3-7ad0-30f4-9cab-01e3f2985b98/bb4d4df4-c004-11e5-94ed-5254001daa61'/>\n" +
"        <backingStore/>\n" +
"      </backingStore>\n" +
"      <target dev='vda' bus='virtio'/>\n" +
"      <serial>4650a2f7fce548e2beaa</serial>\n" +
"      <alias name='virtio-disk0'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n" +
"    </disk>\n" +
"    <disk type='file' device='cdrom'>\n" +
"      <driver name='qemu' type='raw' cache='none'/>\n" +
"      <backingStore/>\n" +
"      <target dev='hdc' bus='ide'/>\n" +
"      <readonly/>\n" +
"      <alias name='ide0-1-0'/>\n" +
"      <address type='drive' controller='0' bus='1' target='0' unit='0'/>\n" +
"    </disk>\n" +
"    <controller type='usb' index='0'>\n" +
"      <alias name='usb'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>\n" +
"    </controller>\n" +
"    <controller type='pci' index='0' model='pci-root'>\n" +
"      <alias name='pci.0'/>\n" +
"    </controller>\n" +
"    <controller type='ide' index='0'>\n" +
"      <alias name='ide'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x1'/>\n" +
"    </controller>\n" +
"    <interface type='bridge'>\n" +
"      <mac address='06:fe:b4:00:00:06'/>\n" +
"      <source bridge='breth0-50'/>\n" +
"      <bandwidth>\n" +
"        <inbound average='25600' peak='25600'/>\n" +
"        <outbound average='25600' peak='25600'/>\n" +
"      </bandwidth>\n" +
"      <target dev='vnet4'/>\n" +
"      <model type='virtio'/>\n" +
"      <alias name='net0'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n" +
"    </interface>\n" +
"    <serial type='pty'>\n" +
"      <source path='/dev/pts/2'/>\n" +
"      <target port='0'/>\n" +
"      <alias name='serial0'/>\n" +
"    </serial>\n" +
"    <console type='pty' tty='/dev/pts/2'>\n" +
"      <source path='/dev/pts/2'/>\n" +
"      <target type='serial' port='0'/>\n" +
"      <alias name='serial0'/>\n" +
"    </console>\n" +
"    <input type='tablet' bus='usb'>\n" +
"      <alias name='input0'/>\n" +
"    </input>\n" +
"    <input type='mouse' bus='ps2'/>\n" +
"    <input type='keyboard' bus='ps2'/>\n" +
"    <graphics type='vnc' port='5902' autoport='yes' listen='192.168.22.21'>\n" +
"      <listen type='address' address='192.168.22.21'/>\n" +
"    </graphics>\n" +
"    <video>\n" +
"      <model type='cirrus' vram='16384' heads='1'/>\n" +
"      <alias name='video0'/>\n" +
"      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
"    </video>\n" +
"    <memballoon model='none'>\n" +
"      <alias name='balloon0'/>\n" +
"    </memballoon>\n" +
"  </devices>\n" +
"</domain>";

    @Test
    public void testReplaceIpForVNCInDescFile() {
        final String targetIp = "192.168.22.21";
        final LibvirtMigrateCommandWrapper lw = new LibvirtMigrateCommandWrapper();
        final String result = lw.replaceIpForVNCInDescFile(fullfile, targetIp);
        assertTrue("transformation does not live up to expectation:\n" + result, targetfile.equals(result));
    }

    @Test
    public void testReplaceIpForVNCInDesc() {
        final String xmlDesc =
                "<domain type='kvm' id='3'>" +
                "  <devices>" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='10.10.10.1'>" +
                "      <listen type='address' address='10.10.10.1'/>" +
                "    </graphics>" +
                "  </devices>" +
                "</domain>";
        final String expectedXmlDesc =
                "<domain type='kvm' id='3'>" +
                "  <devices>" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='10.10.10.10'>" +
                "      <listen type='address' address='10.10.10.10'/>" +
                "    </graphics>" +
                "  </devices>" +
                "</domain>";
        final String targetIp = "10.10.10.10";
        final LibvirtMigrateCommandWrapper lw = new LibvirtMigrateCommandWrapper();
        final String result = lw.replaceIpForVNCInDescFile(xmlDesc, targetIp);
        assertTrue("transformation does not live up to expectation:\n" + result, expectedXmlDesc.equals(result));
    }

    @Test
    public void testReplaceFqdnForVNCInDesc() {
        final String xmlDesc =
                "<domain type='kvm' id='3'>" +
                "  <devices>" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='localhost.local'>" +
                "      <listen type='address' address='localhost.local'/>" +
                "    </graphics>" +
                "  </devices>" +
                "</domain>";
        final String expectedXmlDesc =
                "<domain type='kvm' id='3'>" +
                "  <devices>" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='localhost.localdomain'>" +
                "      <listen type='address' address='localhost.localdomain'/>" +
                "    </graphics>" +
                "  </devices>" +
                "</domain>";
        final String targetIp = "localhost.localdomain";
        final LibvirtMigrateCommandWrapper lw = new LibvirtMigrateCommandWrapper();
        final String result = lw.replaceIpForVNCInDescFile(xmlDesc, targetIp);
        assertTrue("transformation does not live up to expectation:\n" + result, expectedXmlDesc.equals(result));
    }

    @Test
    public void testMigrationUri() {
        final String ip = "10.1.1.1";
        LibvirtMigrateCommandWrapper lw = new LibvirtMigrateCommandWrapper();
        LibvirtComputingResource lcr = new LibvirtComputingResource();
        if (lcr.isHostSecured()) {
            assertEquals(lw.createMigrationURI(ip, lcr), String.format("qemu+tls://%s/system", ip));
        } else {
            assertEquals(lw.createMigrationURI(ip, lcr), String.format("qemu+tcp://%s/system", ip));
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testMigrationUriException() {
        LibvirtMigrateCommandWrapper lw = new LibvirtMigrateCommandWrapper();
        lw.createMigrationURI(null, new LibvirtComputingResource());
    }
}
