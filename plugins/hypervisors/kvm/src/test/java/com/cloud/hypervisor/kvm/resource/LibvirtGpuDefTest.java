package com.cloud.hypervisor.kvm.resource;

import com.cloud.agent.api.VgpuTypesInfo;
import junit.framework.TestCase;
import org.apache.cloudstack.gpu.GpuDevice;
import org.junit.Test;

public class LibvirtGpuDefTest extends TestCase {

    @Test
    public void testGpuDefWithPciPassthrough() {
        LibvirtGpuDef gpuDef = new LibvirtGpuDef();
        VgpuTypesInfo pciGpuInfo = new VgpuTypesInfo(
                GpuDevice.DeviceType.PCI,
                "passthrough",
                "passthrough",
                "00:02.0",
                "10de",
                "NVIDIA Corporation",
                "1b38",
                "Tesla T4"
        );
        gpuDef.defGpu(pciGpuInfo);

        String gpuXml = gpuDef.toString();

        assertTrue(gpuXml.contains("<hostdev mode='subsystem' type='pci' managed='yes'>"));
        assertTrue(gpuXml.contains("<driver name='vfio'/>"));
        assertTrue(gpuXml.contains("<address domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>"));
        assertTrue(gpuXml.contains("</hostdev>"));
    }

    @Test
    public void testGpuDefWithMdevDevice() {
        LibvirtGpuDef gpuDef = new LibvirtGpuDef();
        VgpuTypesInfo mdevGpuInfo = new VgpuTypesInfo(
                GpuDevice.DeviceType.MDEV,
                "nvidia-63",
                "GRID T4-2Q",
                "4b20d080-1b54-4048-85b3-a6a62d165c01",
                "10de",
                "NVIDIA Corporation",
                "1eb8",
                "Tesla T4"
        );
        gpuDef.defGpu(mdevGpuInfo);

        String gpuXml = gpuDef.toString();

        assertTrue(gpuXml.contains("<hostdev mode='subsystem' type='mdev' managed='no'>"));
        assertTrue(gpuXml.contains("<address uuid='4b20d080-1b54-4048-85b3-a6a62d165c01'/>"));
        assertTrue(gpuXml.contains("</hostdev>"));
        assertFalse(gpuXml.contains("vfio")); // MDEV should not contain vfio driver element
    }

    @Test
    public void testGpuDefWithSriovVirtualFunction() {
        LibvirtGpuDef gpuDef = new LibvirtGpuDef();
        VgpuTypesInfo vfGpuInfo = new VgpuTypesInfo(
                GpuDevice.DeviceType.PCI,
                "VF-Profile",
                "VF-Profile",
                "00:10.1",
                "8086",
                "Intel Corporation",
                "1515",
                "X710 Virtual Function"
        );
        gpuDef.defGpu(vfGpuInfo);

        String gpuXml = gpuDef.toString();

        assertTrue(gpuXml.contains("<hostdev mode='subsystem' type='pci' managed='yes'>"));
        assertTrue(gpuXml.contains("<driver name='vfio'/>"));
        assertTrue(gpuXml.contains("<address domain='0x0000' bus='0x00' slot='0x10' function='0x1'/>"));
        assertTrue(gpuXml.contains("</hostdev>"));
    }

    @Test
    public void testGpuDefWithComplexPciAddress() {
        LibvirtGpuDef gpuDef = new LibvirtGpuDef();
        VgpuTypesInfo complexPciGpuInfo = new VgpuTypesInfo(
                GpuDevice.DeviceType.PCI,
                "passthrough",
                "passthrough",
                "81:00.0",
                "1002",
                "Advanced Micro Devices",
                "73a3",
                "Navi 21"
        );
        gpuDef.defGpu(complexPciGpuInfo);

        String gpuXml = gpuDef.toString();

        assertTrue(gpuXml.contains("<hostdev mode='subsystem' type='pci' managed='yes'>"));
        assertTrue(gpuXml.contains("<driver name='vfio'/>"));
        assertTrue(gpuXml.contains("<address domain='0x0000' bus='0x81' slot='0x00' function='0x0'/>"));
        assertTrue(gpuXml.contains("</hostdev>"));
    }

    @Test
    public void testGpuDefWithNullDeviceType() {
        LibvirtGpuDef gpuDef = new LibvirtGpuDef();
        VgpuTypesInfo nullTypeGpuInfo = new VgpuTypesInfo(
                null, // null device type should default to PCI behavior
                "passthrough",
                "passthrough",
                "00:05.0",
                "10de",
                "NVIDIA Corporation",
                "1db4",
                "V100"
        );
        gpuDef.defGpu(nullTypeGpuInfo);

        String gpuXml = gpuDef.toString();

        // Should default to PCI behavior when device type is null
        assertTrue(gpuXml.contains("<hostdev mode='subsystem' type='pci' managed='yes'>"));
        assertTrue(gpuXml.contains("<driver name='vfio'/>"));
        assertTrue(gpuXml.contains("<address domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>"));
        assertTrue(gpuXml.contains("</hostdev>"));
    }
}
