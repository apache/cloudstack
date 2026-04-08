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
package com.cloud.hypervisor.kvm.resource;

import com.cloud.agent.api.VgpuTypesInfo;
import org.apache.cloudstack.gpu.GpuDevice;

public class LibvirtGpuDef {

    private VgpuTypesInfo vgpuType;

    public LibvirtGpuDef() {}

    public void defGpu(VgpuTypesInfo vgpuType) {
        this.vgpuType = vgpuType;
    }

    @Override
    public String toString() {
        StringBuilder gpuBuilder = new StringBuilder();
        GpuDevice.DeviceType deviceType = vgpuType.getDeviceType();

        if (deviceType == GpuDevice.DeviceType.MDEV) {
            // Generate XML for MDEV device (vGPU, including MIG instances)
            generateMdevXml(gpuBuilder);
        } else {
            // Generate XML for PCI device (passthrough GPU or VF)
            generatePciXml(gpuBuilder);
        }

        return gpuBuilder.toString();
    }

    private void generateMdevXml(StringBuilder gpuBuilder) {
        String mdevUuid = vgpuType.getBusAddress(); // For MDEV devices, busAddress contains the UUID
        String displayAttribute = vgpuType.isDisplay() ? "on' ramfb='on" : "off";

        gpuBuilder.append("<hostdev mode='subsystem' type='mdev' model='vfio-pci' display='").append(displayAttribute).append("'>\n");
        gpuBuilder.append("  <source>\n");
        gpuBuilder.append("    <address uuid='").append(mdevUuid).append("'/>\n");
        gpuBuilder.append("  </source>\n");
        gpuBuilder.append("</hostdev>\n");
    }

    private void generatePciXml(StringBuilder gpuBuilder) {
        String busAddress = vgpuType.getBusAddress();

        // For VDI use cases with display=on, ramfb provides early boot framebuffer
        // before GPU driver loads. This is critical for:
        // - Windows VDI guests (require framebuffer during boot)
        // - UEFI/OVMF firmware environments
        // - ARM64 hosts (cache coherency issues with traditional VGA)
        // - Multi-monitor VDI setups (primary display)
        String managed = "yes";
        // To support passthrough NVIDIA GPUs with SR-IOV & vendor specific GPU integration
        if (vgpuType.getVendorId().equals("10de") && !vgpuType.getModelName().equals("passthrough")) {
            managed = "no";
        }
        if (vgpuType.isDisplay()) {
            gpuBuilder.append("<hostdev mode='subsystem' type='pci' managed='").append(managed).append("' display='on' ramfb='on'>\n");
        } else {
            // Compute-only workloads don't need display or ramfb
            gpuBuilder.append("<hostdev mode='subsystem' type='pci' managed='").append(managed).append("' display='off'>\n");
        }
        gpuBuilder.append("  <driver name='vfio'/>\n");
        gpuBuilder.append("  <source>\n");

        // Parse the bus address into domain, bus, slot, function. Two input formats are accepted:
        //   - "dddd:bb:ss.f"  full PCI address with domain (e.g. 0000:00:02.0)
        //   - "bb:ss.f"       legacy short BDF; domain defaults to 0000
        String domain = "0x0000";
        String bus = "0x00";
        String slot = "0x00";
        String function = "0x0";

        if (busAddress != null && !busAddress.isEmpty()) {
            String[] parts = busAddress.split(":");
            String slotFunction = null;
            if (parts.length == 3) {
                domain = "0x" + parts[0];
                bus = "0x" + parts[1];
                slotFunction = parts[2];
            } else if (parts.length == 2) {
                bus = "0x" + parts[0];
                slotFunction = parts[1];
            }
            if (slotFunction != null) {
                String[] slotFunctionParts = slotFunction.split("\\.");
                if (slotFunctionParts.length > 0) {
                    slot = "0x" + slotFunctionParts[0];
                    if (slotFunctionParts.length > 1) {
                        function = "0x" + slotFunctionParts[1].trim();
                    }
                }
            }
        }

        gpuBuilder.append("    <address domain='").append(domain).append("' bus='").append(bus).append("' slot='")
                .append(slot).append("' function='").append(function.trim()).append("'/>\n");
        gpuBuilder.append("  </source>\n");
        gpuBuilder.append("</hostdev>\n");
    }
}
