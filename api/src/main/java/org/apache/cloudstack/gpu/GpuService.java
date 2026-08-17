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
package org.apache.cloudstack.gpu;

import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.host.Host;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.UnmanageGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.DiscoverGpuDevicesCmd;
import org.apache.cloudstack.api.command.admin.gpu.ManageGpuDeviceCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuDevicesCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateVgpuProfileCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuCardsCmd;
import org.apache.cloudstack.api.command.user.gpu.ListVgpuProfilesCmd;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.api.response.GpuDeviceResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VgpuProfileResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.HashMap;
import java.util.List;

public interface GpuService extends Manager {

    ConfigKey<Boolean> GpuDetachOnStop = new ConfigKey<>(Boolean.class, "gpu.detach.on.stop", "Advanced", "false",
            "Whether to detach GPU devices from VM on stop or keep them allocated", true, ConfigKey.Scope.Domain, null);

    GpuCard createGpuCard(CreateGpuCardCmd cmd);

    GpuCard updateGpuCard(UpdateGpuCardCmd cmd);

    boolean deleteGpuCard(DeleteGpuCardCmd cmd);

    VgpuProfileResponse createVgpuProfile(CreateVgpuProfileCmd cmd);

    VgpuProfileResponse updateVgpuProfile(UpdateVgpuProfileCmd cmd);

    boolean deleteVgpuProfile(DeleteVgpuProfileCmd cmd);

    ListResponse<GpuCardResponse> listGpuCards(ListGpuCardsCmd cmd);

    ListResponse<VgpuProfileResponse> listVgpuProfiles(ListVgpuProfilesCmd cmd);

    GpuDeviceResponse createGpuDevice(CreateGpuDeviceCmd cmd);

    GpuDeviceResponse updateGpuDevice(UpdateGpuDeviceCmd cmd);

    ListResponse<GpuDeviceResponse> listGpuDevices(ListGpuDevicesCmd cmd);

    boolean disableGpuDevice(UnmanageGpuDeviceCmd cmd);

    boolean enableGpuDevice(ManageGpuDeviceCmd cmd);

    /**
     * Deallocate GPU devices for a VM on a host.
     *
     * @param vmId The ID of the VM to deallocate GPU devices for.
     */
    void deallocateAllGpuDevicesForVm(long vmId);


    /**
     * Deallocate GPU devices for a VM on a host.
     *
     * @param vmId The ID of the VM to deallocate GPU devices for.
     */
    void deallocateGpuDevicesForVmOnHost(long vmId, long hostId);

    /**
     * Deallocate existing GPU devices for a VM on a host and allocate new GPU devices to the VM.
     *
     * @param vmId       The ID of the VM to allocate GPU devices to.
     * @param hostId     The ID of the host to allocate GPU devices to.
     * @param gpuDevices The list of GPU devices to allocate to the VM.
     */
    void allocateGpuDevicesToVmOnHost(long vmId, long hostId, List<VgpuTypesInfo> gpuDevices);

    /**
     * Discover GPU devices on a host by using the getGPUStatistics command and updating the GPU details for the host.
     *
     * @param cmd The command to discover GPU devices.
     * @return The list of GPU devices.
     */
    ListResponse<GpuDeviceResponse> discoverGpuDevices(DiscoverGpuDevicesCmd cmd);

    /**
     * Check if GPU devices are available for a VM on a host by checking the number of available GPU devices for the
     * vGPU profile.
     *
     * @param host        The host to check GPU devices for.
     * @param vmId        The ID of the VM to check GPU devices for.
     * @param vgpuProfile The vGPU profile to check GPU devices for.
     * @param gpuCount    The number of GPU devices to check for.
     * @return True if GPU devices are available, false otherwise.
     */
    boolean isGPUDeviceAvailable(Host host, Long vmId, VgpuProfile vgpuProfile, int gpuCount);

    /**
     * Get GPU devices for a VM on a host by checking the number of available GPU devices for the vGPU profile.
     * If the VM already has GPU devices assigned, deallocate them and allocate new GPU devices to the VM.
     * The new GPU devices are allocated optimally to the VM.
     *
     * @param vm          The VM to get GPU devices for.
     * @param vgpuProfile The vGPU profile to get GPU devices for.
     * @param gpuCount    The number of GPU devices to get.
     * @return The GPU devices.
     */
    GPUDeviceTO getGPUDevice(VirtualMachine vm, long hostId, VgpuProfile vgpuProfile, int gpuCount);

    /**
     * Gets the GPU group details from the GPU devices on a host.
     * This fetches the GPU devices from the host and prepares the GPU group details for the host.
     * The GPU group details are a map of GPU group name (Card's device name) to a map of vGPU profile name to
     * VgpuTypesInfo.
     * The VgpuTypesInfo contains the information about the GPU device.
     *
     * @param hostId The host ID to get GPU group details for.
     * @return The GPU group details.
     */
    HashMap<String, HashMap<String, VgpuTypesInfo>> getGpuGroupDetailsFromGpuDevicesOnHost(long hostId);

    /**
     * This method is used to add the GPU devices to the host when the host is discovered or when the GPU devices are
     * updated.
     *
     * @param host              The host to add the GPU devices to.
     * @param newGpuDevicesInfo The list of GPU devices to add to the host.
     */
    void addGpuDevicesToHost(Host host, List<VgpuTypesInfo> newGpuDevicesInfo);

    boolean deleteGpuDevices(DeleteGpuDeviceCmd deleteGpuDeviceCmd);
}
