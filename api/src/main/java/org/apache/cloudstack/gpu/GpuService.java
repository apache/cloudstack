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
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuOfferingCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.DisableGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.DiscoverGpuDevicesCmd;
import org.apache.cloudstack.api.command.admin.gpu.EnableGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.ListGpuDevicesCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuOfferingCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateVgpuProfileCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuCardsCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuOfferingsCmd;
import org.apache.cloudstack.api.command.user.gpu.ListVgpuProfilesCmd;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.api.response.GpuDeviceResponse;
import org.apache.cloudstack.api.response.GpuOfferingResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VgpuProfileResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.HashMap;
import java.util.List;

public interface GpuService extends Manager {

    ConfigKey<Boolean> GpuDetachOnStop = new ConfigKey<>(Boolean.class,
            "gpu.detach.on.stop",
            "Advanced",
            "false",
            "Whether to detach GPU devices from VM on stop or keep them allocated",
            true, ConfigKey.Scope.Domain, null);

    /**
     * Creates a GPU card in the database
     *
     * @param cmd the API command
     * @return the created GPU card object
     */
    GpuCard createGpuCard(CreateGpuCardCmd cmd);

    /**
     * Updates a GPU card in the database
     *
     * @param cmd the API command
     * @return the updated GPU card object
     */
    GpuCard updateGpuCard(UpdateGpuCardCmd cmd);

    /**
     * Deletes a GPU card from the database
     *
     * @param cmd the API command
     * @return true if successful, false otherwise
     */
    boolean deleteGpuCard(DeleteGpuCardCmd cmd);

    /**
     * Creates a vGPU profile in the database
     *
     * @param cmd the API command
     * @return the created vGPU profile object
     */
    VgpuProfileResponse createVgpuProfile(CreateVgpuProfileCmd cmd);

    /**
     * Updates a vGPU profile in the database
     *
     * @param cmd the API command
     * @return the updated vGPU profile object
     */
    VgpuProfileResponse updateVgpuProfile(UpdateVgpuProfileCmd cmd);

    /**
     * Deletes a vGPU profile from the database
     *
     * @param cmd the API command
     * @return true if successful, false otherwise
     */
    boolean deleteVgpuProfile(DeleteVgpuProfileCmd cmd);

    /**
     * Lists GPU cards based on criteria
     *
     * @param cmd the API command
     * @return a list of GPU card responses
     */
    ListResponse<GpuCardResponse> listGpuCards(ListGpuCardsCmd cmd);

    /**
     * Lists vGPU profiles based on criteria
     *
     * @param cmd the API command
     * @return a list of vGPU profile responses
     */
    ListResponse<VgpuProfileResponse> listVgpuProfiles(ListVgpuProfilesCmd cmd);

    /**
     * Lists GPU devices based on criteria
     *
     * @param cmd the API command
     * @return a list of GPU device responses
     */
    ListResponse<GpuDeviceResponse> listGpuDevices(ListGpuDevicesCmd cmd);

    boolean disableGpuDevice(DisableGpuDeviceCmd cmd);

    boolean enableGpuDevice(EnableGpuDeviceCmd cmd);

    void deallocateGpuDevicesForVmOnHost(long vm, GpuDevice.State state);

    void assignGpuDevicesToVmOnHost(long vmId, long hostId, List<VgpuTypesInfo> gpuDevices);

    ListResponse<GpuDeviceResponse> discoverGpuDevices(DiscoverGpuDevicesCmd cmd);

    /**
     * Creates a GPU offering in the database
     *
     * @param cmd the API command
     * @return the created GPU offering
     */
    GpuOfferingResponse createGpuOffering(CreateGpuOfferingCmd cmd);

    /**
     * Updates a GPU offering in the database
     *
     * @param cmd the API command
     * @return the updated GPU offering
     */
    GpuOfferingResponse updateGpuOffering(UpdateGpuOfferingCmd cmd);

    /**
     * Lists GPU offerings based on criteria
     *
     * @param listGpuOfferingsCmd the API command
     * @return a list of GPU offering responses
     */
    ListResponse<GpuOfferingResponse> listGpuOfferings(ListGpuOfferingsCmd listGpuOfferingsCmd);

    boolean isGPUDeviceAvailable(Host host, Long vmId, GpuOffering gpuOffering, int gpuCount);

    GPUDeviceTO getGPUDevice(VirtualMachine vm, GpuOffering gpuOffering, int gpuCount);

    HashMap<String, HashMap<String, VgpuTypesInfo>> getGpuGroupDetailsFromGpuDevices(Host host);

    void addGpuDevicesToHost(Host host, List<VgpuTypesInfo> newGpuDevicesInfo);
}
