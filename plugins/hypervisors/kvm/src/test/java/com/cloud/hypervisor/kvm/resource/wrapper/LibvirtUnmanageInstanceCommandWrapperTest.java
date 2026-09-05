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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;


@RunWith(MockitoJUnitRunner.class)
public class LibvirtUnmanageInstanceCommandWrapperTest {
    @Spy
    LibvirtUnmanageInstanceCommandWrapper unmanageInstanceCommandWrapper = new LibvirtUnmanageInstanceCommandWrapper();

    @Test
    public void testCleanupConfigDriveFromDomain() throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, SAXException {
        String domainXML = "<domain type='kvm' id='6'>\n" +
                "  <name>i-2-6-VM</name>\n" +
                "  <uuid>071628d0-84f1-421e-a9cf-d18bca2283bc</uuid>\n" +
                "  <description>CentOS 5.5 (64-bit)</description>\n" +
                "  <memory unit='KiB'>524288</memory>\n" +
                "  <currentMemory unit='KiB'>524288</currentMemory>\n" +
                "  <vcpu placement='static'>1</vcpu>\n" +
                "  <cputune>\n" +
                "    <shares>250</shares>\n" +
                "  </cputune>\n" +
                "  <resource>\n" +
                "    <partition>/machine</partition>\n" +
                "  </resource>\n" +
                "  <sysinfo type='smbios'>\n" +
                "    <system>\n" +
                "      <entry name='manufacturer'>Apache Software Foundation</entry>\n" +
                "      <entry name='product'>CloudStack KVM Hypervisor</entry>\n" +
                "      <entry name='serial'>071628d0-84f1-421e-a9cf-d18bca2283bc</entry>\n" +
                "      <entry name='uuid'>071628d0-84f1-421e-a9cf-d18bca2283bc</entry>\n" +
                "    </system>\n" +
                "  </sysinfo>\n" +
                "  <os>\n" +
                "    <type arch='x86_64' machine='pc-i440fx-rhel7.6.0'>hvm</type>\n" +
                "    <boot dev='cdrom'/>\n" +
                "    <boot dev='hd'/>\n" +
                "    <smbios mode='sysinfo'/>\n" +
                "  </os>\n" +
                "  <features>\n" +
                "    <acpi/>\n" +
                "    <apic/>\n" +
                "    <pae/>\n" +
                "  </features>\n" +
                "  <cpu mode='custom' match='exact' check='full'>\n" +
                "    <model fallback='forbid'>qemu64</model>\n" +
                "    <topology sockets='1' dies='1' cores='1' threads='1'/>\n" +
                "    <feature policy='require' name='x2apic'/>\n" +
                "    <feature policy='require' name='hypervisor'/>\n" +
                "    <feature policy='require' name='lahf_lm'/>\n" +
                "    <feature policy='disable' name='svm'/>\n" +
                "  </cpu>\n" +
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
                "      <source file='/mnt/c88eb8f7-4516-3e35-a226-8737b9470a92/7617e6b5-570c-40d6-99a9-c74bb174c988' index='3'/>\n" +
                "      <backingStore type='file' index='4'>\n" +
                "        <format type='qcow2'/>\n" +
                "        <source file='/mnt/c88eb8f7-4516-3e35-a226-8737b9470a92/7408cffa-9860-11f0-a923-1e0097000303'/>\n" +
                "        <backingStore/>\n" +
                "      </backingStore>\n" +
                "      <target dev='vda' bus='virtio'/>\n" +
                "      <serial>9329e4fa9db546c78b1a</serial>\n" +
                "      <alias name='virtio-disk0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>\n" +
                "    </disk>\n" +
                "    <disk type='file' device='cdrom'>\n" +
                "      <driver name='qemu'/>\n" +
                "      <target dev='hdc' bus='ide'/>\n" +
                "      <readonly/>\n" +
                "      <alias name='ide0-1-0'/>\n" +
                "      <address type='drive' controller='0' bus='1' target='0' unit='0'/>\n" +
                "    </disk>\n" +
                "    <disk type='file' device='cdrom'>\n" +
                "      <driver name='qemu' type='raw'/>\n" +
                "      <source file='/mnt/fec63e8a-d390-31e0-a02d-8e634aabded7/i-2-6-VM.iso' index='1'/>\n" +
                "      <backingStore/>\n" +
                "      <target dev='hdd' bus='ide'/>\n" +
                "      <readonly/>\n" +
                "      <alias name='ide0-1-1'/>\n" +
                "      <address type='drive' controller='0' bus='1' target='0' unit='1'/>\n" +
                "    </disk>\n" +
                "    <controller type='usb' index='0' model='piix3-uhci'>\n" +
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
                "    <controller type='virtio-serial' index='0'>\n" +
                "      <alias name='virtio-serial0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <interface type='bridge'>\n" +
                "      <mac address='02:01:00:cc:00:02'/>\n" +
                "      <source bridge='breth1-2765'/>\n" +
                "      <bandwidth>\n" +
                "        <inbound average='25000' peak='25000'/>\n" +
                "        <outbound average='25000' peak='25000'/>\n" +
                "      </bandwidth>\n" +
                "      <target dev='vnet6'/>\n" +
                "      <model type='virtio'/>\n" +
                "      <link state='up'/>\n" +
                "      <alias name='net0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <serial type='pty'>\n" +
                "      <source path='/dev/pts/1'/>\n" +
                "      <target type='isa-serial' port='0'>\n" +
                "        <model name='isa-serial'/>\n" +
                "      </target>\n" +
                "      <alias name='serial0'/>\n" +
                "    </serial>\n" +
                "    <console type='pty' tty='/dev/pts/1'>\n" +
                "      <source path='/dev/pts/1'/>\n" +
                "      <target type='serial' port='0'/>\n" +
                "      <alias name='serial0'/>\n" +
                "    </console>\n" +
                "    <channel type='unix'>\n" +
                "      <source mode='bind' path='/var/lib/libvirt/qemu/i-2-6-VM.org.qemu.guest_agent.0'/>\n" +
                "      <target type='virtio' name='org.qemu.guest_agent.0' state='disconnected'/>\n" +
                "      <alias name='channel0'/>\n" +
                "      <address type='virtio-serial' controller='0' bus='0' port='1'/>\n" +
                "    </channel>\n" +
                "    <input type='tablet' bus='usb'>\n" +
                "      <alias name='input0'/>\n" +
                "      <address type='usb' bus='0' port='1'/>\n" +
                "    </input>\n" +
                "    <input type='mouse' bus='ps2'>\n" +
                "      <alias name='input1'/>\n" +
                "    </input>\n" +
                "    <input type='keyboard' bus='ps2'>\n" +
                "      <alias name='input2'/>\n" +
                "    </input>\n" +
                "    <graphics type='vnc' port='5901' autoport='yes' listen='10.0.35.235' passwd='l1wi9CFu'>\n" +
                "      <listen type='address' address='10.0.35.235'/>\n" +
                "    </graphics>\n" +
                "    <audio id='1' type='none'/>\n" +
                "    <video>\n" +
                "      <model type='cirrus' vram='16384' heads='1' primary='yes'/>\n" +
                "      <alias name='video0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
                "    </video>\n" +
                "    <watchdog model='i6300esb' action='none'>\n" +
                "      <alias name='watchdog0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>\n" +
                "    </watchdog>\n" +
                "    <memballoon model='virtio'>\n" +
                "      <alias name='balloon0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>\n" +
                "    </memballoon>\n" +
                "  </devices>\n" +
                "  <seclabel type='dynamic' model='dac' relabel='yes'>\n" +
                "    <label>+0:+0</label>\n" +
                "    <imagelabel>+0:+0</imagelabel>\n" +
                "  </seclabel>\n" +
                "</domain>\n";

        String cleanupXML = unmanageInstanceCommandWrapper.cleanupConfigDrive(domainXML, "i-2-6-VM");
        Assert.assertNotEquals(domainXML, cleanupXML);
    }


    @Test
    public void testCleanupConfigDriveFromDomainNoConfigDrive() throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, SAXException {
        String domainXML = "<domain type='kvm' id='6'>\n" +
                "  <name>i-2-6-VM</name>\n" +
                "  <uuid>071628d0-84f1-421e-a9cf-d18bca2283bc</uuid>\n" +
                "  <description>CentOS 5.5 (64-bit)</description>\n" +
                "  <memory unit='KiB'>524288</memory>\n" +
                "  <currentMemory unit='KiB'>524288</currentMemory>\n" +
                "  <vcpu placement='static'>1</vcpu>\n" +
                "  <cputune>\n" +
                "    <shares>250</shares>\n" +
                "  </cputune>\n" +
                "  <resource>\n" +
                "    <partition>/machine</partition>\n" +
                "  </resource>\n" +
                "  <sysinfo type='smbios'>\n" +
                "    <system>\n" +
                "      <entry name='manufacturer'>Apache Software Foundation</entry>\n" +
                "      <entry name='product'>CloudStack KVM Hypervisor</entry>\n" +
                "      <entry name='serial'>071628d0-84f1-421e-a9cf-d18bca2283bc</entry>\n" +
                "      <entry name='uuid'>071628d0-84f1-421e-a9cf-d18bca2283bc</entry>\n" +
                "    </system>\n" +
                "  </sysinfo>\n" +
                "  <os>\n" +
                "    <type arch='x86_64' machine='pc-i440fx-rhel7.6.0'>hvm</type>\n" +
                "    <boot dev='cdrom'/>\n" +
                "    <boot dev='hd'/>\n" +
                "    <smbios mode='sysinfo'/>\n" +
                "  </os>\n" +
                "  <features>\n" +
                "    <acpi/>\n" +
                "    <apic/>\n" +
                "    <pae/>\n" +
                "  </features>\n" +
                "  <cpu mode='custom' match='exact' check='full'>\n" +
                "    <model fallback='forbid'>qemu64</model>\n" +
                "    <topology sockets='1' dies='1' cores='1' threads='1'/>\n" +
                "    <feature policy='require' name='x2apic'/>\n" +
                "    <feature policy='require' name='hypervisor'/>\n" +
                "    <feature policy='require' name='lahf_lm'/>\n" +
                "    <feature policy='disable' name='svm'/>\n" +
                "  </cpu>\n" +
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
                "      <source file='/mnt/c88eb8f7-4516-3e35-a226-8737b9470a92/7617e6b5-570c-40d6-99a9-c74bb174c988' index='3'/>\n" +
                "      <backingStore type='file' index='4'>\n" +
                "        <format type='qcow2'/>\n" +
                "        <source file='/mnt/c88eb8f7-4516-3e35-a226-8737b9470a92/7408cffa-9860-11f0-a923-1e0097000303'/>\n" +
                "        <backingStore/>\n" +
                "      </backingStore>\n" +
                "      <target dev='vda' bus='virtio'/>\n" +
                "      <serial>9329e4fa9db546c78b1a</serial>\n" +
                "      <alias name='virtio-disk0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>\n" +
                "    </disk>\n" +
                "    <disk type='file' device='cdrom'>\n" +
                "      <driver name='qemu'/>\n" +
                "      <target dev='hdc' bus='ide'/>\n" +
                "      <readonly/>\n" +
                "      <alias name='ide0-1-0'/>\n" +
                "      <address type='drive' controller='0' bus='1' target='0' unit='0'/>\n" +
                "    </disk>\n" +
                "    <controller type='usb' index='0' model='piix3-uhci'>\n" +
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
                "    <controller type='virtio-serial' index='0'>\n" +
                "      <alias name='virtio-serial0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <interface type='bridge'>\n" +
                "      <mac address='02:01:00:cc:00:02'/>\n" +
                "      <source bridge='breth1-2765'/>\n" +
                "      <bandwidth>\n" +
                "        <inbound average='25000' peak='25000'/>\n" +
                "        <outbound average='25000' peak='25000'/>\n" +
                "      </bandwidth>\n" +
                "      <target dev='vnet6'/>\n" +
                "      <model type='virtio'/>\n" +
                "      <link state='up'/>\n" +
                "      <alias name='net0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <serial type='pty'>\n" +
                "      <source path='/dev/pts/1'/>\n" +
                "      <target type='isa-serial' port='0'>\n" +
                "        <model name='isa-serial'/>\n" +
                "      </target>\n" +
                "      <alias name='serial0'/>\n" +
                "    </serial>\n" +
                "    <console type='pty' tty='/dev/pts/1'>\n" +
                "      <source path='/dev/pts/1'/>\n" +
                "      <target type='serial' port='0'/>\n" +
                "      <alias name='serial0'/>\n" +
                "    </console>\n" +
                "    <channel type='unix'>\n" +
                "      <source mode='bind' path='/var/lib/libvirt/qemu/i-2-6-VM.org.qemu.guest_agent.0'/>\n" +
                "      <target type='virtio' name='org.qemu.guest_agent.0' state='disconnected'/>\n" +
                "      <alias name='channel0'/>\n" +
                "      <address type='virtio-serial' controller='0' bus='0' port='1'/>\n" +
                "    </channel>\n" +
                "    <input type='tablet' bus='usb'>\n" +
                "      <alias name='input0'/>\n" +
                "      <address type='usb' bus='0' port='1'/>\n" +
                "    </input>\n" +
                "    <input type='mouse' bus='ps2'>\n" +
                "      <alias name='input1'/>\n" +
                "    </input>\n" +
                "    <input type='keyboard' bus='ps2'>\n" +
                "      <alias name='input2'/>\n" +
                "    </input>\n" +
                "    <graphics type='vnc' port='5901' autoport='yes' listen='10.0.35.235' passwd='l1wi9CFu'>\n" +
                "      <listen type='address' address='10.0.35.235'/>\n" +
                "    </graphics>\n" +
                "    <audio id='1' type='none'/>\n" +
                "    <video>\n" +
                "      <model type='cirrus' vram='16384' heads='1' primary='yes'/>\n" +
                "      <alias name='video0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
                "    </video>\n" +
                "    <watchdog model='i6300esb' action='none'>\n" +
                "      <alias name='watchdog0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>\n" +
                "    </watchdog>\n" +
                "    <memballoon model='virtio'>\n" +
                "      <alias name='balloon0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>\n" +
                "    </memballoon>\n" +
                "  </devices>\n" +
                "  <seclabel type='dynamic' model='dac' relabel='yes'>\n" +
                "    <label>+0:+0</label>\n" +
                "    <imagelabel>+0:+0</imagelabel>\n" +
                "  </seclabel>\n" +
                "</domain>\n";

        String cleanupXML = unmanageInstanceCommandWrapper.cleanupConfigDrive(domainXML, "i-2-6-VM");
        Assert.assertEquals(domainXML, cleanupXML);
    }
}
