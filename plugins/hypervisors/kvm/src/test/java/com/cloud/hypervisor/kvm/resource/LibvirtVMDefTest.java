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

import junit.framework.TestCase;

import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.ChannelDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.SCSIDef;

public class LibvirtVMDefTest extends TestCase {

    public void testInterfaceEtehrnet() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defEthernet("targetDeviceName", "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO);

        String expected =
            "<interface type='ethernet'>\n"
                    + "<target dev='targetDeviceName'/>\n"
                    + "<mac address='00:11:22:aa:bb:dd'/>\n"
                    + "<model type='virtio'/>\n"
                    + "<link state='up'/>\n"
                    + "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

    public void testInterfaceDirectNet() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defDirectNet("targetDeviceName", null, "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO, "private");

        String expected =
            "<interface type='" + LibvirtVMDef.InterfaceDef.GuestNetType.DIRECT + "'>\n"
                    + "<source dev='targetDeviceName' mode='private'/>\n"
                    + "<mac address='00:11:22:aa:bb:dd'/>\n"
                    + "<model type='virtio'/>\n"
                    + "<link state='up'/>\n"
                    + "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

    public void testInterfaceBridgeSlot() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defBridgeNet("targetDeviceName", null, "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO);
        ifDef.setSlot(16);

        String expected =
                "<interface type='" + LibvirtVMDef.InterfaceDef.GuestNetType.BRIDGE + "'>\n"
                        + "<source bridge='targetDeviceName'/>\n"
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
                        + "<mac address='00:11:22:aa:bb:dd'/>\n"
                        + "<model type='virtio'/>\n"
                        + "<link state='down'/>\n"
                        + "<address type='pci' domain='0x0000' bus='0x00' slot='0x10' function='0x0'/>\n"
                        + "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

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

    public void testChannelDef() {
        ChannelDef.ChannelType type = ChannelDef.ChannelType.UNIX;
        ChannelDef.ChannelState state = ChannelDef.ChannelState.CONNECTED;
        String name = "v-136-VM.vport";
        File path = new File("/var/lib/libvirt/qemu/" + name);

        ChannelDef channelDef = new ChannelDef(name, type, state, path);

        assertEquals(state, channelDef.getChannelState());
        assertEquals(type, channelDef.getChannelType());
        assertEquals(name, channelDef.getName());
        assertEquals(path, channelDef.getPath());
    }

    public void testWatchDogDef() {
        LibvirtVMDef.WatchDogDef.WatchDogModel model = LibvirtVMDef.WatchDogDef.WatchDogModel.I6300ESB;
        LibvirtVMDef.WatchDogDef.WatchDogAction action = LibvirtVMDef.WatchDogDef.WatchDogAction.RESET;

        LibvirtVMDef.WatchDogDef def = new LibvirtVMDef.WatchDogDef(action, model);
        assertEquals(def.getModel(), model);
        assertEquals(def.getAction(), action);
    }

    public void testSCSIDef() {
        SCSIDef def = new SCSIDef();
        String str = def.toString();
        String expected = "<controller type='scsi' index='0' model='virtio-scsi'>\n" +
                "<address type='pci' domain='0x0000' bus='0x00' slot='0x09' function='0x0'/>\n" +
                "</controller>\n";
        assertEquals(str, expected);
    }

    public void testMetadataDef() {
        LibvirtVMDef.MetadataDef metadataDef = new LibvirtVMDef.MetadataDef();

        metadataDef.getMetadataNode(LibvirtVMDef.NuageExtensionDef.class).addNuageExtension("mac1", "ip1");
        metadataDef.getMetadataNode(LibvirtVMDef.NuageExtensionDef.class).addNuageExtension("mac2", "ip2");

        String xmlDef = metadataDef.toString();
        String expectedXml = "<metadata>\n" +
                "<nuage-extension xmlns='nuagenetworks.net/nuage/cna'>\n" +
                "  <interface mac='mac2' vsp-vr-ip='ip2'></interface>\n" +
                "  <interface mac='mac1' vsp-vr-ip='ip1'></interface>\n" +
                "</nuage-extension>\n" +
                "</metadata>\n";

        assertEquals(xmlDef, expectedXml);
    }

}
