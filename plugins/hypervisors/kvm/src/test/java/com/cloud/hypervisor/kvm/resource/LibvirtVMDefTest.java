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

import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.ChannelDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.SCSIDef;
import junit.framework.TestCase;
import org.apache.cloudstack.utils.linux.MemStat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MemStat.class)
public class LibvirtVMDefTest extends TestCase {

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

    @Ignore
    @Test
    public void testInterfaceEthernet() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defEthernet("targetDeviceName", "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO, "/bin/if_up",100,1500);

        String expected =
            "<interface type='ethernet'>\n"
                    + "<target dev='targetDeviceName'/>\n"
                    + "<mtu size='1500'/>\n"
                    + "<mac address='00:11:22:aa:bb:dd'/>\n"
                    + "<model type='virtio'/>\n"
                    + "<script path='/bin/if_up'/>\n"
                    + "<link state='up'/>\n"
                    + "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

    @Ignore
    @Test
    public void testInterfaceDirectNet() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defDirectNet("targetDeviceName", null, "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO, "private", 100, 1500);

        String expected =
            "<interface type='" + LibvirtVMDef.InterfaceDef.GuestNetType.DIRECT + "'>\n"
                    + "<source dev='targetDeviceName' mode='private'/>\n"
                    + "<mtu size='1500'/>\n"
                    + "<mac address='00:11:22:aa:bb:dd'/>\n"
                    + "<model type='virtio'/>\n"
                    + "<link state='up'/>\n"
                    + "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

    @Test
    public void testInterfaceBridgeSlot() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defBridgeNet("targetDeviceName", null, "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO, 1500);
        ifDef.setSlot(16);

        String expected =
                "<interface type='" + LibvirtVMDef.InterfaceDef.GuestNetType.BRIDGE + "'>\n"
                        + "<source bridge='targetDeviceName'/>\n"
                        + "<mtu size='1500'/>\n"
                        + "<mac address='00:11:22:aa:bb:dd'/>\n"
                        + "<model type='virtio'/>\n"
                        + "<link state='up'/>\n"
                        + "<address type='pci' domain='0x0000' bus='0x00' slot='0x10' function='0x0'/>\n"
                        + "</interface>\n";

        assertEquals(expected, ifDef.toString());

        ifDef.setLinkStateUp(false);
        ifDef.setDevName("vnet11");

        expected =
                "<interface type='" + LibvirtVMDef.InterfaceDef.GuestNetType.BRIDGE + "'>\n"
                        + "<source bridge='targetDeviceName'/>\n"
                        + "<target dev='vnet11'/>\n"
                        + "<mtu size='1500'/>\n"
                        + "<mac address='00:11:22:aa:bb:dd'/>\n"
                        + "<model type='virtio'/>\n"
                        + "<link state='down'/>\n"
                        + "<address type='pci' domain='0x0000' bus='0x00' slot='0x10' function='0x0'/>\n"
                        + "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

    @Test
    public void testCpuModeDef() {
        LibvirtVMDef.CpuModeDef cpuModeDef = new LibvirtVMDef.CpuModeDef();
        cpuModeDef.setMode("custom");
        cpuModeDef.setModel("Nehalem");

        String expected1 = "<cpu mode='custom' match='exact'><model fallback='allow'>Nehalem</model></cpu>";

        assertEquals(expected1, cpuModeDef.toString());

        cpuModeDef.setMode("host-model");
        String expected2 = "<cpu mode='host-model'><model fallback='allow'></model></cpu>";

        assertEquals(expected2, cpuModeDef.toString());

        cpuModeDef.setMode("host-passthrough");
        String expected3 = "<cpu mode='host-passthrough'></cpu>";
        assertEquals(expected3, cpuModeDef.toString());

    }

    @Test
    public void testCpuModeDefCpuFeatures() {
        LibvirtVMDef.CpuModeDef cpuModeDef = new LibvirtVMDef.CpuModeDef();
        cpuModeDef.setMode("custom");
        cpuModeDef.setModel("Nehalem");

        String expected_start = "<cpu mode='custom' match='exact'><model fallback='allow'>Nehalem</model>";
        String expected_end = "</cpu>";

        List<String> features1 = Arrays.asList("feature1", "feature2");
        cpuModeDef.setFeatures(features1);
        String expected1 = "<feature policy='require' name='feature1'/><feature policy='require' name='feature2'/>";
        assertEquals(expected_start + expected1 + expected_end, cpuModeDef.toString());

        List<String> features2 = Arrays.asList("-feature1", "-feature2");
        cpuModeDef.setFeatures(features2);
        String expected2 = "<feature policy='disable' name='feature1'/><feature policy='disable' name='feature2'/>";
        assertEquals(expected_start + expected2 + expected_end, cpuModeDef.toString());

        List<String> features3 = Arrays.asList("-feature1", "feature2");
        cpuModeDef.setFeatures(features3);
        String expected3 = "<feature policy='disable' name='feature1'/><feature policy='require' name='feature2'/>";
        assertEquals(expected_start + expected3 + expected_end, cpuModeDef.toString());
    }

    @Test
    public void testDiskDef() {
        String filePath = "/var/lib/libvirt/images/disk.qcow2";
        String diskLabel = "vda";

        DiskDef disk = new DiskDef();
        DiskDef.DiskBus bus = DiskDef.DiskBus.VIRTIO;
        DiskDef.DiskFmtType type = DiskDef.DiskFmtType.QCOW2;
        DiskDef.DiskCacheMode cacheMode = DiskDef.DiskCacheMode.WRITEBACK;

        disk.defFileBasedDisk(filePath, diskLabel, bus, type);
        disk.setCacheMode(cacheMode);

        assertEquals(filePath, disk.getDiskPath());
        assertEquals(diskLabel, disk.getDiskLabel());
        assertEquals(bus, disk.getBusType());
        assertEquals(DiskDef.DeviceType.DISK, disk.getDeviceType());

        String xmlDef = disk.toString();
        String expectedXml = "<disk  device='disk' type='file'>\n<driver name='qemu' type='" + type.toString() + "' cache='" + cacheMode.toString() + "' />\n" +
                             "<source file='" + filePath + "'/>\n<target dev='" + diskLabel + "' bus='" + bus.toString() + "'/>\n</disk>\n";

        assertEquals(xmlDef, expectedXml);
    }

    @Test
    public void testDiskDefWithBurst() {
        String filePath = "/var/lib/libvirt/images/disk.qcow2";
        String diskLabel = "vda";

        DiskDef disk = new DiskDef();
        DiskDef.DiskBus bus = DiskDef.DiskBus.VIRTIO;
        DiskDef.DiskFmtType type = DiskDef.DiskFmtType.QCOW2;
        disk.defFileBasedDisk(filePath, diskLabel, bus, type);


        Long iopsReadRate = 500L;
        Long iopsReadRateMax = 2000L;
        Long iopsReadRateMaxLength = 120L;
        Long iopsWriteRate = 501L;
        Long iopsWriteRateMax = 2001L;
        Long iopsWriteRateMaxLength = 121L;
        Long bytesReadRate = 1000L;
        Long bytesReadRateMax = 2500L;
        Long bytesReadRateMaxLength = 122L;
        Long bytesWriteRate = 1001L;
        Long bytesWriteRateMax = 2501L;
        Long bytesWriteRateMaxLength = 123L;


        disk.setIopsReadRate(iopsReadRate);
        disk.setIopsReadRateMax(iopsReadRateMax);
        disk.setIopsReadRateMaxLength(iopsReadRateMaxLength);
        disk.setIopsWriteRate(iopsWriteRate);
        disk.setIopsWriteRateMax(iopsWriteRateMax);
        disk.setIopsWriteRateMaxLength(iopsWriteRateMaxLength);
        disk.setBytesReadRate(bytesReadRate);
        disk.setBytesReadRateMax(bytesReadRateMax);
        disk.setBytesReadRateMaxLength(bytesReadRateMaxLength);
        disk.setBytesWriteRate(bytesWriteRate);
        disk.setBytesWriteRateMax(bytesWriteRateMax);
        disk.setBytesWriteRateMaxLength(bytesWriteRateMaxLength);

        LibvirtVMDef.setGlobalQemuVersion(2006000L);
        LibvirtVMDef.setGlobalLibvirtVersion(9008L);

        String xmlDef = disk.toString();
        String expectedXml = "<disk  device='disk' type='file'>\n<driver name='qemu' type='" + type.toString() + "' cache='none' />\n" +
                "<source file='" + filePath + "'/>\n<target dev='" + diskLabel + "' bus='" + bus.toString() + "'/>\n" +
                "<iotune>\n<read_bytes_sec>"+bytesReadRate+"</read_bytes_sec>\n<write_bytes_sec>"+bytesWriteRate+"</write_bytes_sec>\n" +
                "<read_iops_sec>"+iopsReadRate+"</read_iops_sec>\n<write_iops_sec>"+iopsWriteRate+"</write_iops_sec>\n" +
                "<read_bytes_sec_max>"+bytesReadRateMax+"</read_bytes_sec_max>\n<write_bytes_sec_max>"+bytesWriteRateMax+"</write_bytes_sec_max>\n" +
                "<read_iops_sec_max>"+iopsReadRateMax+"</read_iops_sec_max>\n<write_iops_sec_max>"+iopsWriteRateMax+"</write_iops_sec_max>\n" +
                "<read_bytes_sec_max_length>"+bytesReadRateMaxLength+"</read_bytes_sec_max_length>\n<write_bytes_sec_max_length>"+bytesWriteRateMaxLength+"</write_bytes_sec_max_length>\n" +
                "<read_iops_sec_max_length>"+iopsReadRateMaxLength+"</read_iops_sec_max_length>\n<write_iops_sec_max_length>"+iopsWriteRateMaxLength+"</write_iops_sec_max_length>\n</iotune>\n</disk>\n";

                assertEquals(xmlDef, expectedXml);
    }

    @Test
    public void testHypervEnlightDef() {
        LibvirtVMDef.FeaturesDef featuresDef = new LibvirtVMDef.FeaturesDef();
        LibvirtVMDef.HyperVEnlightenmentFeatureDef hyperVEnlightenmentFeatureDef = new LibvirtVMDef.HyperVEnlightenmentFeatureDef();
        hyperVEnlightenmentFeatureDef.setFeature("relaxed", true);
        hyperVEnlightenmentFeatureDef.setFeature("vapic", true);
        hyperVEnlightenmentFeatureDef.setFeature("spinlocks", true);
        hyperVEnlightenmentFeatureDef.setRetries(8096);
        featuresDef.addHyperVFeature(hyperVEnlightenmentFeatureDef);
        String defs = featuresDef.toString();
        assertTrue(defs.contains("relaxed"));
        assertTrue(defs.contains("vapic"));
        assertTrue(defs.contains("spinlocks"));

        featuresDef = new LibvirtVMDef.FeaturesDef();
        featuresDef.addFeatures("pae");
        defs = featuresDef.toString();
        assertFalse(defs.contains("relaxed"));
        assertFalse(defs.contains("vapic"));
        assertFalse(defs.contains("spinlocks"));
        assertTrue("Windows PV".contains("Windows PV"));

    }

    @Test
    public void testRngDef() {
        LibvirtVMDef.RngDef.RngBackendModel backendModel = LibvirtVMDef.RngDef.RngBackendModel.RANDOM;
        String path = "/dev/random";
        int period = 2000;
        int bytes = 2048;

        LibvirtVMDef.RngDef def = new LibvirtVMDef.RngDef(path, backendModel, bytes, period);
        assertEquals(def.getPath(), path);
        assertEquals(def.getRngBackendModel(), backendModel);
        assertEquals(def.getRngModel(), LibvirtVMDef.RngDef.RngModel.VIRTIO);
        assertEquals(def.getRngRateBytes(), bytes);
        assertEquals(def.getRngRatePeriod(), period);
    }

    @Test
    public void testChannelDef() {
        ChannelDef.ChannelType type = ChannelDef.ChannelType.UNIX;
        ChannelDef.ChannelState state = ChannelDef.ChannelState.CONNECTED;
        String name = "v-136-VM.org.qemu.guest_agent.0";
        File path = new File("/var/lib/libvirt/qemu/" + name);

        ChannelDef channelDef = new ChannelDef(name, type, state, path);

        assertEquals(state, channelDef.getChannelState());
        assertEquals(type, channelDef.getChannelType());
        assertEquals(name, channelDef.getName());
        assertEquals(path, channelDef.getPath());
    }

    @Test
    public void testWatchDogDef() {
        LibvirtVMDef.WatchDogDef.WatchDogModel model = LibvirtVMDef.WatchDogDef.WatchDogModel.I6300ESB;
        LibvirtVMDef.WatchDogDef.WatchDogAction action = LibvirtVMDef.WatchDogDef.WatchDogAction.RESET;

        LibvirtVMDef.WatchDogDef def = new LibvirtVMDef.WatchDogDef(action, model);
        assertEquals(def.getModel(), model);
        assertEquals(def.getAction(), action);
    }

    @Test
    public void testSCSIDef() {
        SCSIDef def = new SCSIDef((short)0, 0, 0, 9, 0, 4);
        String str = def.toString();
        String expected = "<controller type='scsi' index='0' model='virtio-scsi'>\n" +
                "<address type='pci' domain='0x0000' bus='0x00' slot='0x09' function='0x0'/>\n" +
                "<driver queues='4'/>\n" +
                "</controller>\n";
        assertEquals(str, expected);
    }
}
