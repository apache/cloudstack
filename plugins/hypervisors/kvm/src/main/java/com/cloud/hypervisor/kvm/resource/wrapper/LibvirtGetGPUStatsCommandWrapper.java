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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetGPUStatsAnswer;
import com.cloud.agent.api.GetGPUStatsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = GetGPUStatsCommand.class)
public final class LibvirtGetGPUStatsCommandWrapper extends CommandWrapper<GetGPUStatsCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final GetGPUStatsCommand command, final LibvirtComputingResource libvirtComputingResource) {
        return new GetGPUStatsAnswer(command, libvirtComputingResource.getGpuDevices());
    }

//
//    private List<VgpuTypesInfo> getGPUDevices() {
//        Script cmd = new Script("/bin/bash", logger);
//        cmd.add("-c");
//        cmd.add("lspci -nn -m");
//
//        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
//        String result = cmd.execute(parser);
//        if (result == null && parser.getLines() != null) {
//            String[] lines = parser.getLines().split("\\n");
//            List<VgpuTypesInfo> gpuDevices = new ArrayList<>();
//        /* Sample output
//
//        00:02.0 "VGA compatible controller [0300]" "Cirrus Logic [1013]" "GD 5446 [00b8]" "Red Hat, Inc. [1af4]"
//        "QEMU Virtual Machine [1100]"
//        00:08.0 "3D controller [0302]" "NVIDIA Corporation [10de]" "GA107M [GeForce RTX 3050 Ti Mobile] [25a0]" -ra1
//        "Dell [1028]" "Device [0b19]"
//
//            For example, the first line contains:
//            Bus Address: 00:02.0
//            Vendor ID: 1013
//            Device ID: 00b8
//            Vendor Name: Cirrus Logic
//            Device Name: GD 5446
//            Model Name: passthrough
//            The second line contains:
//            Bus Address: 00:08.0
//            Vendor ID: 10de
//            Device ID: 25a0
//            Vendor Name: NVIDIA Corporation
//            Device Name: GA107M [GeForce RTX 3050 Ti Mobile]
//            Model Name: passthrough
//         */
//            for (String line : lines) {
//                if (line.toLowerCase().contains("vga") || line.toLowerCase().contains("3d")
//                    || line.toLowerCase().contains("nvidia") || line.toLowerCase().contains("amd")
//                    || line.toLowerCase().contains("gpu")) {
//                    String[] gpuDeviceDetails = line.split("\"");
//                    String busAddress = gpuDeviceDetails[0];
//
//                    String vendorId = StringUtils.right(gpuDeviceDetails[3], 5).split("]")[0];
//                    String vendorName = StringUtils.left(gpuDeviceDetails[3], gpuDeviceDetails[3].length() - 6).trim();
//
//                    String deviceId = StringUtils.right(gpuDeviceDetails[5], 5).split("]")[0];
//                    String deviceName = StringUtils.left(gpuDeviceDetails[5], gpuDeviceDetails[5].length() - 6).trim();
//
//                    String modelName = "passthrough";
//                    if (line.toLowerCase().contains("mig") || line.toLowerCase().contains("vgpu")) {
//                        modelName = "vgpu";
//                    }
//                    gpuDevices.add(new VgpuTypesInfo(modelName, modelName, busAddress,
//                            vendorId, vendorName, deviceId, deviceName));
//                }
//            }
//            return gpuDevices;
//        } else {
//            logger.debug("Failed to get GPU devices: " + result);
//            return new ArrayList<>();
//        }
//    }
}
