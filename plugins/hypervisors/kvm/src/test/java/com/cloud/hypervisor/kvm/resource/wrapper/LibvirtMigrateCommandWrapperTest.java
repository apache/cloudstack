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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.cloudstack.utils.linux.MemStat;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.StorageVol;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo.DiskType;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo.DriverType;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo.Source;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.utils.exception.CloudRuntimeException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {LibvirtConnection.class, LibvirtMigrateCommandWrapper.class, MemStat.class})
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
"      <iotune>\n" +
"        <write_iops_sec>500</write_iops_sec>\n" +
"        <write_iops_sec_max>5000</write_iops_sec_max>\n" +
"        <write_iops_sec_max_length>60</write_iops_sec_max_length>\n" +
"      </iotune>\n" +
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
"      <iotune>\n" +
"        <write_iops_sec>500</write_iops_sec>\n" +
"        <write_iops_sec_max>5000</write_iops_sec_max>\n" +
"        <write_iops_sec_max_length>60</write_iops_sec_max_length>\n" +
"      </iotune>\n" +
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

    LibvirtMigrateCommandWrapper libvirtMigrateCmdWrapper = new LibvirtMigrateCommandWrapper();

    final String memInfo = "MemTotal:        5830236 kB\n" +
            "MemFree:          156752 kB\n" +
            "Buffers:          326836 kB\n" +
            "Cached:          2606764 kB\n" +
            "SwapCached:            0 kB\n" +
            "Active:          4260808 kB\n" +
            "Inactive:         949392 kB\n";

    @Before
    public void setup() throws Exception {
        Scanner scanner = new Scanner(memInfo);
        PowerMockito.whenNew(Scanner.class).withAnyArguments().thenReturn(scanner);
    }

    @Test
    public void testReplaceIpForVNCInDescFile() {
        final String targetIp = "192.168.22.21";
        final String result = libvirtMigrateCmdWrapper.replaceIpForVNCInDescFile(fullfile, targetIp);
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
        final String result = libvirtMigrateCmdWrapper.replaceIpForVNCInDescFile(xmlDesc, targetIp);
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
        final String result = libvirtMigrateCmdWrapper.replaceIpForVNCInDescFile(xmlDesc, targetIp);
        assertTrue("transformation does not live up to expectation:\n" + result, expectedXmlDesc.equals(result));
    }

    @Test
    public void testMigrationUri() {
        final String ip = "10.1.1.1";
        LibvirtComputingResource lcr = new LibvirtComputingResource();
        if (lcr.isHostSecured()) {
            assertEquals(libvirtMigrateCmdWrapper.createMigrationURI(ip, lcr), String.format("qemu+tls://%s/system", ip));
        } else {
            assertEquals(libvirtMigrateCmdWrapper.createMigrationURI(ip, lcr), String.format("qemu+tcp://%s/system", ip));
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testMigrationUriException() {
        libvirtMigrateCmdWrapper.createMigrationURI(null, new LibvirtComputingResource());
    }

    @Test
    public void deleteLocalVolumeTest() throws Exception {
        PowerMockito.mockStatic(LibvirtConnection.class);
        Connect conn = Mockito.mock(Connect.class);

        PowerMockito.doReturn(conn).when(LibvirtConnection.class, "getConnection");

        StorageVol storageVolLookupByPath = Mockito.mock(StorageVol.class);
        Mockito.when(conn.storageVolLookupByPath("localPath")).thenReturn(storageVolLookupByPath);

        libvirtMigrateCmdWrapper.deleteLocalVolume("localPath");

        PowerMockito.verifyStatic(Mockito.times(1));
        LibvirtConnection.getConnection();
        InOrder inOrder = Mockito.inOrder(conn, storageVolLookupByPath);
        inOrder.verify(conn, Mockito.times(1)).storageVolLookupByPath("localPath");
        inOrder.verify(storageVolLookupByPath, Mockito.times(1)).delete(0);
    }

    @Test
    public void searchDiskDefOnMigrateDiskInfoListTest() {
        configureAndVerifyTestSearchDiskDefOnMigrateDiskInfoList("f3d49ecc-870c-475a-89fa-fd0124420a9b", "/var/lib/libvirt/images/f3d49ecc-870c-475a-89fa-fd0124420a9b", false);
    }

    @Test
    public void searchDiskDefOnMigrateDiskInfoListTestExpectNull() {
        configureAndVerifyTestSearchDiskDefOnMigrateDiskInfoList("f3d49ecc-870c-475a-89fa-fd0124420a9b", "/var/lib/libvirt/images/f3d49ecc-870c-89fa-fd0124420a9b", true);
    }

    private void configureAndVerifyTestSearchDiskDefOnMigrateDiskInfoList(String serialNumber, String diskPath, boolean isExpectedDiskInfoNull) {
        MigrateDiskInfo migrateDiskInfo = new MigrateDiskInfo(serialNumber, DiskType.FILE, DriverType.QCOW2, Source.FILE, "sourceText");
        List<MigrateDiskInfo> migrateDiskInfoList = new ArrayList<>();
        migrateDiskInfoList.add(migrateDiskInfo);

        DiskDef disk = new DiskDef();
        disk.setDiskPath(diskPath);

        MigrateDiskInfo returnedMigrateDiskInfo = libvirtMigrateCmdWrapper.searchDiskDefOnMigrateDiskInfoList(migrateDiskInfoList, disk);

        if (isExpectedDiskInfoNull)
            Assert.assertEquals(null, returnedMigrateDiskInfo);
        else
            Assert.assertEquals(migrateDiskInfo, returnedMigrateDiskInfo);
    }

    @Test
    public void deleteOrDisconnectDisksOnSourcePoolTest() {
        LibvirtMigrateCommandWrapper spyLibvirtMigrateCmdWrapper = PowerMockito.spy(libvirtMigrateCmdWrapper);
        Mockito.doNothing().when(spyLibvirtMigrateCmdWrapper).deleteLocalVolume("volPath");

        List<MigrateDiskInfo> migrateDiskInfoList = new ArrayList<>();
        MigrateDiskInfo migrateDiskInfo0 = createMigrateDiskInfo(true);
        MigrateDiskInfo migrateDiskInfo2 = createMigrateDiskInfo(false);

        List<DiskDef> disks = new ArrayList<>();
        DiskDef diskDef0 = new DiskDef();
        DiskDef diskDef1 = new DiskDef();
        DiskDef diskDef2 = new DiskDef();

        diskDef0.setDiskPath("volPath");
        disks.add(diskDef0);
        disks.add(diskDef1);
        disks.add(diskDef2);

        LibvirtComputingResource libvirtComputingResource = Mockito.spy(new LibvirtComputingResource());
        Mockito.doReturn(true).when(libvirtComputingResource).cleanupDisk(diskDef1);

        Mockito.doReturn(migrateDiskInfo0).when(spyLibvirtMigrateCmdWrapper).searchDiskDefOnMigrateDiskInfoList(migrateDiskInfoList, diskDef0);
        Mockito.doReturn(null).when(spyLibvirtMigrateCmdWrapper).searchDiskDefOnMigrateDiskInfoList(migrateDiskInfoList, diskDef1);
        Mockito.doReturn(migrateDiskInfo2).when(spyLibvirtMigrateCmdWrapper).searchDiskDefOnMigrateDiskInfoList(migrateDiskInfoList, diskDef2);

        spyLibvirtMigrateCmdWrapper.deleteOrDisconnectDisksOnSourcePool(libvirtComputingResource, migrateDiskInfoList, disks);

        InOrder inOrder = Mockito.inOrder(spyLibvirtMigrateCmdWrapper, libvirtComputingResource);
        inOrderVerifyDeleteOrDisconnect(inOrder, spyLibvirtMigrateCmdWrapper, libvirtComputingResource, migrateDiskInfoList, diskDef0, 1, 0);
        inOrderVerifyDeleteOrDisconnect(inOrder, spyLibvirtMigrateCmdWrapper, libvirtComputingResource, migrateDiskInfoList, diskDef1, 0, 1);
        inOrderVerifyDeleteOrDisconnect(inOrder, spyLibvirtMigrateCmdWrapper, libvirtComputingResource, migrateDiskInfoList, diskDef2, 0, 1);
    }

    private MigrateDiskInfo createMigrateDiskInfo(boolean isSourceDiskOnStorageFileSystem) {
        MigrateDiskInfo migrateDiskInfo = new MigrateDiskInfo("serialNumber", DiskType.FILE, DriverType.QCOW2, Source.FILE, "sourceText");
        migrateDiskInfo.setSourceDiskOnStorageFileSystem(isSourceDiskOnStorageFileSystem);
        return migrateDiskInfo;
    }

    private void inOrderVerifyDeleteOrDisconnect(InOrder inOrder, LibvirtMigrateCommandWrapper lw, LibvirtComputingResource virtResource, List<MigrateDiskInfo> diskInfoList,
            DiskDef disk, int timesDelete, int timesCleanup) {
        inOrder.verify(lw).searchDiskDefOnMigrateDiskInfoList(diskInfoList, disk);
        inOrder.verify(lw, Mockito.times(timesDelete)).deleteLocalVolume("volPath");
        inOrder.verify(virtResource, Mockito.times(timesCleanup)).cleanupDisk(disk);
    }

    static void assertXpath(final Document doc, final String xPathExpr,
                            final String expected) {
        try {
            Assert.assertEquals(expected, XPathFactory.newInstance().newXPath()
                    .evaluate(xPathExpr, doc));
        } catch (final XPathExpressionException e) {
            Assert.fail("Could not evaluate xpath" + xPathExpr + ":"
                    + e.getMessage());
        }
    }

    @Test
    public void testReplaceStorage() throws Exception {
        Map<String, MigrateDiskInfo> mapMigrateStorage = new HashMap<String, MigrateDiskInfo>();

        MigrateDiskInfo diskInfo = new MigrateDiskInfo("123456", DiskType.BLOCK, DriverType.RAW, Source.FILE, "sourctest");
        mapMigrateStorage.put("/mnt/812ea6a3-7ad0-30f4-9cab-01e3f2985b98/4650a2f7-fce5-48e2-beaa-bcdf063194e6", diskInfo);
        final String result = libvirtMigrateCmdWrapper.replaceStorage(fullfile, mapMigrateStorage);

        InputStream in = IOUtils.toInputStream(result);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(in);
        assertXpath(doc, "/domain/devices/disk/iotune/write_iops_sec", "500");
        assertXpath(doc, "/domain/devices/disk/@type", "block");
        assertXpath(doc, "/domain/devices/disk/driver/@type", "raw");
    }

}
