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

import junit.framework.TestCase;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.utils.Pair;

public class LibvirtVMDefTest extends TestCase {

    public void testInterfaceEtehrnet() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defEthernet("targetDeviceName", "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO);

        String expected =
            "<interface type='ethernet'>\n" + "<target dev='targetDeviceName'/>\n" + "<mac address='00:11:22:aa:bb:dd'/>\n" + "<model type='virtio'/>\n"
                + "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

    public void testInterfaceDirectNet() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defDirectNet("targetDeviceName", null, "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO, "private");

        String expected =
            "<interface type='" + LibvirtVMDef.InterfaceDef.GuestNetType.DIRECT + "'>\n" + "<source dev='targetDeviceName' mode='private'/>\n" +
                "<mac address='00:11:22:aa:bb:dd'/>\n" + "<model type='virtio'/>\n" + "</interface>\n";

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
        hyperVEnlightenmentFeatureDef.setRelaxed(true);
        featuresDef.addHyperVFeature(hyperVEnlightenmentFeatureDef);
        String defs = featuresDef.toString();
        assertTrue(defs.contains("relaxed"));

        featuresDef = new LibvirtVMDef.FeaturesDef();
        featuresDef.addFeatures("pae");
        defs = featuresDef.toString();
        assertFalse(defs.contains("relaxed"));

        assertTrue("Windows Server 2008 R2".contains("Windows Server 2008"));

        Pair<Integer,Integer> hostOsVersion = new Pair<Integer,Integer>(6,5);
        assertTrue((hostOsVersion.first() == 6 && hostOsVersion.second() >= 5) || (hostOsVersion.first() >= 7));
        hostOsVersion = new Pair<Integer,Integer>(7,1);
        assertTrue((hostOsVersion.first() == 6 && hostOsVersion.second() >= 5) || (hostOsVersion.first() >= 7));
    }

}
