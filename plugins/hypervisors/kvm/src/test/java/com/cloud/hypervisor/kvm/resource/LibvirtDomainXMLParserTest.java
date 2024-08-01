/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.hypervisor.kvm.resource;

import java.io.File;
import java.util.List;

import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.ChannelDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.MemBalloonDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.RngDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.WatchDogDef;

import junit.framework.TestCase;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtDomainXMLParserTest extends TestCase {

    @Test
    public void testDomainXMLParser() {
        int vncPort = 5900;

        DiskDef.DiskBus diskBus = DiskDef.DiskBus.VIRTIO;
        DiskDef.DiskType diskType = DiskDef.DiskType.FILE;
        DiskDef.DeviceType deviceType = DiskDef.DeviceType.DISK;
        DiskDef.DiskFmtType diskFormat = DiskDef.DiskFmtType.QCOW2;
        DiskDef.DiskCacheMode diskCache = DiskDef.DiskCacheMode.NONE;

        InterfaceDef.NicModel ifModel = InterfaceDef.NicModel.VIRTIO;
        InterfaceDef.GuestNetType ifType = InterfaceDef.GuestNetType.BRIDGE;

        ChannelDef.ChannelType channelType = ChannelDef.ChannelType.UNIX;
        String guestAgentPath = "/var/lib/libvirt/qemu/guest-agent.org.qemu.guest_agent.0";
        String guestAgentName = "org.qemu.guest_agent.0";

        String diskLabel ="vda";
        String diskPath = "/var/lib/libvirt/images/my-test-image.qcow2";

        String diskLabel2 ="vdb";
        String diskPath2 = "/var/lib/libvirt/images/my-test-image2.qcow2";
        String secretUuid = "5644d664-a238-3a9b-811c-961f609d29f4";

        String xml = "<domain type='kvm' id='10'>" +
                     "<name>s-2970-VM</name>" +
                     "<uuid>4d2c1526-865d-4fc9-a1ac-dbd1801a22d0</uuid>" +
                     "<description>Debian GNU/Linux 6(64-bit)</description>" +
                     "<memory unit='KiB'>262144</memory>" +
                     "<currentMemory unit='KiB'>262144</currentMemory>" +
                     "<vcpu placement='static'>1</vcpu>" +
                     "<cputune>" +
                     "<shares>250</shares>" +
                     "</cputune>" +
                     "<resource>" +
                     "<partition>/machine</partition>" +
                     "</resource>" +
                     "<os>" +
                     "<type arch='x86_64' machine='pc-i440fx-1.5'>hvm</type>" +
                     "<boot dev='cdrom'/>" +
                     "<boot dev='hd'/>" +
                     "</os>" +
                     "<features>" +
                     "<acpi/>" +
                     "<apic/>" +
                     "<pae/>" +
                     "</features>" +
                     "<clock offset='utc'/>" +
                     "<on_poweroff>destroy</on_poweroff>" +
                     "<on_reboot>restart</on_reboot>" +
                     "<on_crash>destroy</on_crash>" +
                     "<devices>" +
                     "<emulator>/usr/bin/kvm-spice</emulator>" +
                     "<disk type='" + diskType.toString() + "' device='" + deviceType.toString() + "'>" +
                     "<driver name='qemu' type='" + diskFormat.toString() + "' cache='" + diskCache.toString() + "'/>" +
                     "<source file='" + diskPath + "'/>" +
                     "<target dev='" + diskLabel + "' bus='" + diskBus.toString() + "'/>" +
                     "<alias name='virtio-disk0'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x08' function='0x0'/>" +
                     "</disk>" +
                     "<disk type='" + diskType.toString() + "' device='" + deviceType.toString() + "'>" +
                     "<driver name='qemu' type='" + diskFormat.toString() + "' cache='" + diskCache.toString() + "'/>" +
                     "<source file='" + diskPath2 + "'/>" +
                     "<target dev='" + diskLabel2 +"' bus='" + diskBus.toString() + "'/>" +
                     "<alias name='virtio-disk1'/>" +
                     "<encryption format='luks'>" +
                     "<secret type='passphrase' uuid='" + secretUuid + "'/>" +
                     "</encryption>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x09' function='0x0'/>" +
                     "</disk>" +
                     "<disk type='file' device='cdrom'>" +
                     "<driver name='qemu' type='raw' cache='none'/>" +
                     "<source file='/usr/share/cloudstack-common/vms/systemvm.iso'/>" +
                     "<target dev='hdc' bus='ide'/>" +
                     "<readonly/>" +
                     "<alias name='ide0-1-0'/>" +
                     "<address type='drive' controller='0' bus='1' target='0' unit='0'/>" +
                     "</disk>" +
                     "<controller type='usb' index='0'>" +
                     "<alias name='usb0'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>" +
                     "</controller>" +
                     "<controller type='pci' index='0' model='pci-root'>" +
                     "<alias name='pci0'/>" +
                     "</controller>" +
                     "<controller type='ide' index='0'>" +
                     "<alias name='ide0'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x1'/>" +
                     "</controller>" +
                     "<controller type='virtio-serial' index='0'>" +
                     "<alias name='virtio-serial0'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>" +
                     "</controller>" +
                     "<interface type='" + ifType.toString() + "'>" +
                     "<mac address='0e:00:a9:fe:02:00'/>" +
                     "<source bridge='cloud0'/>" +
                     "<target dev='vnet0'/>" +
                     "<model type='" + ifModel.toString() + "'/>" +
                     "<alias name='net0'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>" +
                     "</interface>" +
                     "<interface type='" + ifType.toString() + "'>" +
                     "<mac address='06:c5:94:00:05:65'/>" +
                     "<source bridge='cloudbr1'/>" +
                     "<target dev='vnet1'/>" +
                     "<model type='" + ifModel.toString() + "'/>" +
                     "<alias name='net1'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>" +
                     "</interface>" +
                     "<interface type='" + ifType.toString() + "'>" +
                     "<mac address='06:c9:f4:00:04:40'/>" +
                     "<source bridge='cloudbr0'/>" +
                     "<target dev='vnet2'/>" +
                     "<model type='" + ifModel.toString() + "'/>" +
                     "<alias name='net2'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>" +
                     "</interface>" +
                     "<interface type='" + ifType.toString() + "'>" +
                     "<mac address='06:7e:c6:00:05:68'/>" +
                     "<source bridge='cloudbr1'/>" +
                     "<target dev='vnet3'/>" +
                     "<model type='" + ifModel.toString() + "'/>" +
                     "<alias name='net3'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>" +
                     "</interface>" +
                     "<serial type='pty'>" +
                     "<source path='/dev/pts/3'/>" +
                     "<target port='0'/>" +
                     "<alias name='serial0'/>" +
                     "</serial>" +
                     "<console type='pty' tty='/dev/pts/3'>" +
                     "<source path='/dev/pts/3'/>" +
                     "<target type='serial' port='0'/>" +
                     "<alias name='serial0'/>" +
                     "</console>" +
                     "<input type='tablet' bus='usb'>" +
                     "<alias name='input0'/>" +
                     "</input>" +
                     "<input type='mouse' bus='ps2'/>" +
                     "<graphics type='vnc' port='" + vncPort + "' autoport='yes' listen='0.0.0.0'>" +
                     "<listen type='address' address='0.0.0.0'/>" +
                     "</graphics>" +
                     "<video>" +
                     "<model type='cirrus' vram='9216' heads='1'/>" +
                     "<alias name='video0'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>" +
                     "</video>" +
                     "<memballoon model='virtio'>" +
                     "<stats period='60'/>" +
                     "<alias name='balloon0'/>" +
                     "<address type='pci' domain='0x0000' bus='0x00' slot='0x09' function='0x0'/>" +
                     "</memballoon>" +
                     "<rng model='virtio'>" +
                     "<rate period='5000' bytes='4096' />" +
                     "<backend model='random'>/dev/random</backend>" +
                     "</rng>" +
                     "<channel type='unix'>" +
                     "<source mode='bind' path='" + guestAgentPath + "'/>" +
                     "<target type='virtio' name='" + guestAgentName + "'/>" +
                     "<alias name='channel0'/>" +
                     "<address type='virtio-serial' controller='0' bus='0' port='1'/>" +
                     "</channel>" +
                     "<watchdog model='i6300esb' action='reset'/>" +
                     "</devices>" +
                     "<seclabel type='none'/>" +
                     "</domain>";

        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        parser.parseDomainXML(xml);

        assertEquals(vncPort - 5900, (int)parser.getVncPort());

        List<DiskDef> disks = parser.getDisks();
        /* Disk 0 is the first disk, the QCOW2 file backed virto disk */
        int diskId = 0;

        assertEquals(diskLabel, disks.get(diskId).getDiskLabel());
        assertEquals(diskPath, disks.get(diskId).getDiskPath());
        assertEquals(diskCache, disks.get(diskId).getCacheMode());
        assertEquals(diskBus, disks.get(diskId).getBusType());
        assertEquals(diskType, disks.get(diskId).getDiskType());
        assertEquals(deviceType, disks.get(diskId).getDeviceType());
        assertEquals(diskFormat, disks.get(diskId).getDiskFormatType());

        DiskDef.LibvirtDiskEncryptDetails encryptDetails = disks.get(1).getLibvirtDiskEncryptDetails();
        assertNotNull(encryptDetails);
        assertEquals(QemuObject.EncryptFormat.LUKS, encryptDetails.getEncryptFormat());
        assertEquals(secretUuid, encryptDetails.getPassphraseUuid());

        List<ChannelDef> channels = parser.getChannels();
        for (int i = 0; i < channels.size(); i++) {
            assertEquals(channelType, channels.get(i).getChannelType());
            assertEquals(channelType, channels.get(i).getChannelType());
        }

        /* Qemu Guest Agent port/channel */
        assertEquals(new File(guestAgentPath), channels.get(0).getPath());
        assertEquals(guestAgentName, channels.get(0).getName());

        List<InterfaceDef> ifs = parser.getInterfaces();
        for (int i = 0; i < ifs.size(); i++) {
            assertEquals(ifModel, ifs.get(i).getModel());
            assertEquals(ifType, ifs.get(i).getNetType());
            assertEquals(Integer.valueOf(i + 3), ifs.get(i).getSlot());
            assertEquals("vnet" + i, ifs.get(i).getDevName());
        }

        MemBalloonDef memBalloon = parser.getMemBalloon();
        assertEquals(MemBalloonDef.MemBalloonModel.VIRTIO, memBalloon.getMemBalloonModel());
        assertEquals("60", memBalloon.getMemBalloonStatsPeriod());

        List<RngDef> rngs = parser.getRngs();
        assertEquals("/dev/random", rngs.get(0).getPath());
        assertEquals(RngDef.RngBackendModel.RANDOM, rngs.get(0).getRngBackendModel());
        assertEquals(4096, rngs.get(0).getRngRateBytes());
        assertEquals(5000, rngs.get(0).getRngRatePeriod());

        List<WatchDogDef> watchDogs = parser.getWatchDogs();
        assertEquals(WatchDogDef.WatchDogModel.I6300ESB, watchDogs.get(0).getModel());
        assertEquals(WatchDogDef.WatchDogAction.RESET, watchDogs.get(0).getAction());
    }

    @Test
    public void testDomainXMLParserWithoutModelName() {
        String xml = "<domain type='kvm'>\n" +
                "  <name>testkiran</name>\n" +
                "  <uuid>aafaaabc-8657-4efc-9c52-3422d4e04088</uuid>\n" +
                "  <memory unit='KiB'>2097152</memory>\n" +
                "  <currentMemory unit='KiB'>2097152</currentMemory>\n" +
                "  <vcpu placement='static'>8</vcpu>\n" +
                "  <os>\n" +
                "    <type arch='x86_64' machine='pc-i440fx-rhel7.0.0'>hvm</type>\n" +
                "    <boot dev='hd'/>\n" +
                "  </os>\n" +
                "  <features>\n" +
                "    <acpi/>\n" +
                "    <apic/>\n" +
                "  </features>\n" +
                "  <cpu mode='host-model' check='partial'>\n" +
                "    <model fallback='allow'/>\n" +
                "    <topology sockets='1' cores='4' threads='2' />" +
                "  </cpu>\n" +
                "  <clock offset='utc'>\n" +
                "    <timer name='rtc' tickpolicy='catchup'/>\n" +
                "    <timer name='pit' tickpolicy='delay'/>\n" +
                "    <timer name='hpet' present='no'/>\n" +
                "  </clock>\n" +
                "  <on_poweroff>destroy</on_poweroff>\n" +
                "  <on_reboot>restart</on_reboot>\n" +
                "  <on_crash>destroy</on_crash>\n" +
                "  <pm>\n" +
                "    <suspend-to-mem enabled='no'/>\n" +
                "    <suspend-to-disk enabled='no'/>\n" +
                "  </pm>\n" +
                "  <devices>\n" +
                "    <emulator>/usr/libexec/qemu-kvm</emulator>\n" +
                "    <disk type='file' device='disk'>\n" +
                "      <driver name='qemu' type='qcow2'/>\n" +
                "      <source file='/var/lib/libvirt/images/ubuntu-22.04.qcow2'/>\n" +
                "      <target dev='vda' bus='virtio'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>\n" +
                "    </disk>\n" +
                "    <disk type='file' device='disk'>\n" +
                "      <driver name='qemu' type='qcow2'/>\n" +
                "      <source file='/var/lib/libvirt/images/testkiran.qcow2'/>\n" +
                "      <target dev='vdb' bus='virtio'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x08' function='0x0'/>\n" +
                "    </disk>\n" +
                "    <controller type='usb' index='0' model='ich9-ehci1'>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x7'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='0' model='ich9-uhci1'>\n" +
                "      <master startport='0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0' multifunction='on'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='0' model='ich9-uhci2'>\n" +
                "      <master startport='2'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x1'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='0' model='ich9-uhci3'>\n" +
                "      <master startport='4'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x2'/>\n" +
                "    </controller>\n" +
                "    <controller type='pci' index='0' model='pci-root'/>\n" +
                "    <controller type='virtio-serial' index='0'>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <interface type='network'>\n" +
                "      <mac address='52:54:00:09:73:b8'/>\n" +
                "      <source network='default'/>\n" +
                "      <model type='virtio'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <serial type='pty'>\n" +
                "      <target type='isa-serial' port='0'>\n" +
                "        <model name='isa-serial'/>\n" +
                "      </target>\n" +
                "    </serial>\n" +
                "    <console type='pty'>\n" +
                "      <target type='serial' port='0'/>\n" +
                "    </console>\n" +
                "    <channel type='spicevmc'>\n" +
                "      <target type='virtio' name='com.redhat.spice.0'/>\n" +
                "      <address type='virtio-serial' controller='0' bus='0' port='1'/>\n" +
                "    </channel>\n" +
                "    <input type='tablet' bus='usb'>\n" +
                "      <address type='usb' bus='0' port='1'/>\n" +
                "    </input>\n" +
                "    <input type='mouse' bus='ps2'/>\n" +
                "    <input type='keyboard' bus='ps2'/>\n" +
                "    <graphics type='vnc' port='-1' autoport='yes'>\n" +
                "      <listen type='address'/>\n" +
                "    </graphics>\n" +
                "    <graphics type='spice' autoport='yes'>\n" +
                "      <listen type='address'/>\n" +
                "      <image compression='off'/>\n" +
                "    </graphics>\n" +
                "    <sound model='ich6'>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n" +
                "    </sound>\n" +
                "    <video>\n" +
                "      <model type='qxl' ram='65536' vram='65536' vgamem='16384' heads='1' primary='yes'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
                "    </video>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <address type='usb' bus='0' port='2'/>\n" +
                "    </redirdev>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <address type='usb' bus='0' port='3'/>\n" +
                "    </redirdev>\n" +
                "    <memballoon model='virtio'>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x09' function='0x0'/>\n" +
                "    </memballoon>\n" +
                "  </devices>\n" +
                "</domain>";

        LibvirtDomainXMLParser libvirtDomainXMLParser = new LibvirtDomainXMLParser();
        try {
            libvirtDomainXMLParser.parseDomainXML(xml);
        } catch (Exception e) {
            System.out.println("Got exception " + e.getMessage());
            throw e;
        }
        Assert.assertEquals("CPU socket count is parsed", 1, libvirtDomainXMLParser.getCpuModeDef().getSockets());
        Assert.assertEquals("CPU cores count is parsed", 4, libvirtDomainXMLParser.getCpuModeDef().getCoresPerSocket());
        Assert.assertEquals("CPU threads count is parsed", 2, libvirtDomainXMLParser.getCpuModeDef().getThreadsPerCore());
    }
}
