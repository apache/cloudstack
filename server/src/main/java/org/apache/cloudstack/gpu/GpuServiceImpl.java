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
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.gpu.GpuCardVO;
import com.cloud.gpu.GpuDeviceVO;
import com.cloud.gpu.VgpuProfileVO;
import com.cloud.gpu.dao.GpuCardDao;
import com.cloud.gpu.dao.GpuDeviceDao;
import com.cloud.gpu.dao.VgpuProfileDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.DisableGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.DiscoverGpuDevicesCmd;
import org.apache.cloudstack.api.command.admin.gpu.EnableGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.ListGpuDevicesCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuDeviceCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuCardsCmd;
import org.apache.cloudstack.api.command.user.gpu.ListVgpuProfilesCmd;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.api.response.GpuDeviceResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VgpuProfileResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GpuServiceImpl extends ManagerBase implements GpuService, PluggableService, Configurable {
    private static final Logger s_logger = LogManager.getLogger(GpuServiceImpl.class);

    @Inject
    private GpuCardDao gpuCardDao;

    @Inject
    private VgpuProfileDao vgpuProfileDao;

    @Inject
    private GpuDeviceDao gpuDeviceDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private UserVmManager userVmManager;

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private ResourceManager resourceManager;

    @Override
    public boolean configure(String name, java.util.Map<String, Object> params) throws ConfigurationException {
        s_logger.info("Configuring GpuServiceImpl: {}", name);
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        // GPU Card Commands
        cmdList.add(CreateGpuCardCmd.class);
        cmdList.add(UpdateGpuCardCmd.class);
        cmdList.add(DeleteGpuCardCmd.class);
        cmdList.add(ListGpuCardsCmd.class);

        // vGPU Profile Commands
        cmdList.add(CreateVgpuProfileCmd.class);
        cmdList.add(UpdateVgpuProfileCmd.class);
        cmdList.add(DeleteVgpuProfileCmd.class);
        cmdList.add(ListVgpuProfilesCmd.class);

        // GPU Device Commands
        cmdList.add(ListGpuDevicesCmd.class);
        cmdList.add(DisableGpuDeviceCmd.class);
        cmdList.add(EnableGpuDeviceCmd.class);
        cmdList.add(DiscoverGpuDevicesCmd.class);
        cmdList.add(CreateGpuDeviceCmd.class);
        cmdList.add(UpdateGpuDeviceCmd.class);

        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return GpuService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{GpuDetachOnStop};
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GPU_CARD_CREATE, eventDescription = "creating GPU Card")
    public GpuCardVO createGpuCard(CreateGpuCardCmd cmd) {
        final String deviceId = cmd.getDeviceId();
        final String deviceName = cmd.getDeviceName();
        final String name = cmd.getName();
        final String vendorName = cmd.getVendorName();
        final String vendorId = cmd.getVendorId();

        // Validate inputs
        if (StringUtils.isEmpty(deviceId)) {
            throw new InvalidParameterValueException("Device ID cannot be empty");
        }
        if (StringUtils.isEmpty(deviceName)) {
            throw new InvalidParameterValueException("Device name cannot be empty");
        }
        if (StringUtils.isEmpty(name)) {
            throw new InvalidParameterValueException("Display name cannot be empty");
        }
        if (StringUtils.isEmpty(vendorName)) {
            throw new InvalidParameterValueException("Vendor name cannot be empty");
        }
        if (StringUtils.isEmpty(vendorId)) {
            throw new InvalidParameterValueException("Vendor ID cannot be empty");
        }

        // Check if a GPU card with the same vendor ID and device ID already exists
        GpuCardVO existingGpuCard = gpuCardDao.findByVendorIdAndDeviceId(vendorId, deviceId);
        if (existingGpuCard != null) {
            throw new InvalidParameterValueException(
                    String.format("GPU card with vendor ID %s and device ID %s already exists", vendorId, deviceId));
        }

        GpuCardVO gpuCard = new GpuCardVO(deviceId, deviceName, name, vendorName, vendorId);
        gpuCard = gpuCardDao.persist(gpuCard);
        vgpuProfileDao.persist(new VgpuProfileVO("passthrough", "passthrough", gpuCard.getId(), 1L));
        return gpuCard;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GPU_CARD_EDIT, eventDescription = "Updating GPU Card")
    public GpuCardVO updateGpuCard(UpdateGpuCardCmd cmd) {
        final Long id = cmd.getId();
        final String deviceName = cmd.getDeviceName();
        final String name = cmd.getName();
        final String vendorName = cmd.getVendorName();

        // Validate inputs
        GpuCardVO gpuCard = gpuCardDao.findById(id);
        if (gpuCard == null) {
            throw new InvalidParameterValueException("GPU card with ID " + id + " not found");
        }

        if (deviceName != null) {
            gpuCard.setDeviceName(deviceName);
        }
        if (name != null) {
            gpuCard.setName(name);
        }
        if (vendorName != null) {
            gpuCard.setVendorName(vendorName);
        }
        gpuCardDao.update(id, gpuCard);
        return gpuCard;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GPU_CARD_DELETE, eventDescription = "deleting the GPU Card")
    public boolean deleteGpuCard(DeleteGpuCardCmd cmd) {
        final Long id = cmd.getId();

        // Validate inputs
        GpuCardVO gpuCard = gpuCardDao.findById(id);
        if (gpuCard == null) {
            throw new InvalidParameterValueException("GPU card with ID " + id + " not found");
        }

        // Check if a GPU card is in use
        if (gpuDeviceDao.isGpuCardInUse(id)) {
            throw new InvalidParameterValueException(
                    "Cannot delete GPU card " + gpuCard + " as it is in use by one or more GPU devices");
        }

        return gpuCardDao.remove(id);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VGPU_PROFILE_CREATE, eventDescription = "creating vGPU profile")
    public VgpuProfileResponse createVgpuProfile(CreateVgpuProfileCmd cmd) {
        final String profileName = cmd.getName();
        final String profileDescription = cmd.getDescription();
        final Long gpuCardId = cmd.getCardId();
        final Long maxVgpuPerPgpu = cmd.getMaxVgpuPerPgpu();

        // Validate inputs
        if (StringUtils.isBlank(profileName)) {
            throw new InvalidParameterValueException("vGPU profile name cannot be empty");
        }

        // Check if the GPU card ID is valid
        GpuCardVO gpuCard = gpuCardDao.findById(gpuCardId);
        if (gpuCard == null) {
            throw new InvalidParameterValueException(String.format("GPU card with ID %d not found", gpuCardId));
        }

        // Check if a vGPU profile with the same name already exists
        VgpuProfileVO existingProfile = vgpuProfileDao.findByNameAndCardId(profileName, gpuCardId);
        if (existingProfile != null) {
            throw new InvalidParameterValueException(
                    String.format("vGPU profile with name %s already exists", profileName));
        }

        VgpuProfileVO vgpuProfile = new VgpuProfileVO(profileName, profileDescription, gpuCardId, maxVgpuPerPgpu);
        vgpuProfile = vgpuProfileDao.persist(vgpuProfile);

        VgpuProfileResponse response = new VgpuProfileResponse(vgpuProfile, gpuCard);
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VGPU_PROFILE_EDIT, eventDescription = "updating vGPU profile")
    public VgpuProfileResponse updateVgpuProfile(UpdateVgpuProfileCmd cmd) {
        final Long id = cmd.getId();
        final String profileName = cmd.getProfileName();
        final String profileDescription = cmd.getDescription();
        final Long maxVgpuPerPgpu = cmd.getMaxVgpuPerPgpu();

        // Validate inputs
        VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(id);
        if (vgpuProfile == null) {
            throw new InvalidParameterValueException(String.format("vGPU profile with ID %d not found", id));
        }

        // Check if a vGPU profile with the same name already exists (if the name is being updated)
        if (profileName != null && !profileName.equals(vgpuProfile.getName())) {
            VgpuProfileVO existingProfile = vgpuProfileDao.findByNameAndCardId(profileName, vgpuProfile.getCardId());
            if (existingProfile != null) {
                throw new InvalidParameterValueException(
                        String.format("vGPU profile with name %s already exists", profileName));
            }
        }

        if (profileName != null) {
            vgpuProfile.setName(profileName);
        }
        if (profileDescription != null) {
            vgpuProfile.setDescription(profileDescription);
        }
        if (maxVgpuPerPgpu != null) {
            vgpuProfile.setMaxVgpuPerPgpu(maxVgpuPerPgpu);
        }
        vgpuProfileDao.update(id, vgpuProfile);

        VgpuProfileResponse response =
                new VgpuProfileResponse(vgpuProfile, gpuCardDao.findById(vgpuProfile.getCardId()));
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VGPU_PROFILE_DELETE, eventDescription = "Deleting vGPU profile")
    public boolean deleteVgpuProfile(DeleteVgpuProfileCmd cmd) {
        final Long id = cmd.getId();

        // Validate inputs
        VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(id);
        if (vgpuProfile == null) {
            throw new InvalidParameterValueException(String.format("vGPU profile with ID %d not found", id));
        }

        // Check if vGPU profile is in use
        if (gpuDeviceDao.isVgpuProfileInUse(id)) {
            throw new InvalidParameterValueException(String.format(
                    "Cannot delete vGPU profile with ID %d as it is in use by one or more GPU " + "devices", id));
        }

        return vgpuProfileDao.remove(id);
    }

    @Override
    public ListResponse<GpuCardResponse> listGpuCards(ListGpuCardsCmd cmd) {
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        String vendorName = cmd.getVendorName();
        String vendorId = cmd.getVendorId();
        String deviceId = cmd.getDeviceId();
        String deviceName = cmd.getDeviceName();

        Pair<List<GpuCardVO>, Integer> gpuCardsAndCount =
                gpuCardDao.searchAndCountGpuCards(id, keyword, vendorId, vendorName, deviceId, deviceName,
                        cmd.getStartIndex(), cmd.getPageSizeVal());

        return getGpuCardResponseListResponse(cmd, gpuCardsAndCount.first(), gpuCardsAndCount.second());
    }

    @NotNull
    private static ListResponse<GpuCardResponse> getGpuCardResponseListResponse(ListGpuCardsCmd cmd,
                                                                                List<GpuCardVO> gpuCards,
                                                                                Integer count) {
        ListResponse<GpuCardResponse> response = new ListResponse<>();
        List<GpuCardResponse> gpuCardResponses = new ArrayList<>();

        for (GpuCardVO gpuCard : gpuCards) {
            GpuCardResponse gpuCardResponse = new GpuCardResponse(gpuCard);

            // Set account info
            response.setResponseName(cmd.getCommandName());
            gpuCardResponses.add(gpuCardResponse);
        }

        response.setResponses(gpuCardResponses, count);
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    @Override
    public ListResponse<VgpuProfileResponse> listVgpuProfiles(ListVgpuProfilesCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getName();
        String keyword = cmd.getKeyword();
        Long gpuCardId = cmd.getCardId();

        Pair<List<VgpuProfileVO>, Integer> vgpuProfilesAndCount =
                vgpuProfileDao.searchAndCountVgpuProfiles(id, name, keyword, gpuCardId, cmd.getStartIndex(),
                        cmd.getPageSizeVal());

        return getVgpuProfileResponseListResponse(cmd, vgpuProfilesAndCount.first(), vgpuProfilesAndCount.second());
    }

    @NotNull
    private ListResponse<VgpuProfileResponse> getVgpuProfileResponseListResponse(ListVgpuProfilesCmd cmd,
                                                                                 List<VgpuProfileVO> vgpuProfiles,
                                                                                 Integer count) {
        ListResponse<VgpuProfileResponse> response = new ListResponse<>();
        List<VgpuProfileResponse> vgpuProfileResponses = new ArrayList<>();

        Map<Long, GpuCardVO> cardMap = new HashMap<>();
        for (VgpuProfileVO vgpuProfile : vgpuProfiles) {
            GpuCardVO gpuCard = cardMap.get(vgpuProfile.getCardId());
            if (gpuCard == null) {
                gpuCard = gpuCardDao.findById(vgpuProfile.getCardId());
                cardMap.put(vgpuProfile.getCardId(), gpuCard);
            }
            VgpuProfileResponse vgpuProfileResponse = new VgpuProfileResponse(vgpuProfile, gpuCard);
            vgpuProfileResponse.setResponseName(cmd.getCommandName());
            vgpuProfileResponses.add(vgpuProfileResponse);
        }

        response.setResponses(vgpuProfileResponses, count);
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    @Override
    public ListResponse<GpuDeviceResponse> listGpuDevices(ListGpuDevicesCmd cmd) {
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        Long hostId = cmd.getHostId();
        Long gpuCardId = cmd.getGpuCardId();
        Long vgpuProfileId = cmd.getVgpuProfileId();
        Long vmId = cmd.getVmId();

        Pair<List<GpuDeviceVO>, Integer> gpuDevicesAndCount = gpuDeviceDao.searchAndCountGpuDevices(
                id, keyword, hostId, vmId, gpuCardId, vgpuProfileId, cmd.getStartIndex(), cmd.getPageSizeVal());

        return getGpuDeviceResponseListResponse(cmd, gpuDevicesAndCount.first(), gpuDevicesAndCount.second());
    }

    @Override
    public boolean disableGpuDevice(DisableGpuDeviceCmd cmd) {
        return updateGpuDeviceResourceState(cmd.getIds(), GpuDevice.ResourceState.Disabled);
    }

    @Override
    public boolean enableGpuDevice(EnableGpuDeviceCmd cmd) {
        return updateGpuDeviceResourceState(cmd.getIds(), GpuDevice.ResourceState.Enabled);
    }

    @Override
    public void deallocateGpuDevicesForVmOnHost(long vmId, GpuDevice.State state) {
        List<GpuDeviceVO> devices = gpuDeviceDao.listByVmId(vmId);

        for (GpuDeviceVO device : devices) {
            device.setState(state);
            if (state.equals(GpuDevice.State.Free)) {
                device.setVmId(null);
            } else {
                device.setVmId(vmId);
            }
            gpuDeviceDao.persist(device);
        }
    }

    @Override
    public void assignGpuDevicesToVmOnHost(long vmId, long hostId, List<VgpuTypesInfo> gpuDevices) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                deallocateGpuDevicesForVmOnHost(vmId, GpuDevice.State.Free);

                for (VgpuTypesInfo gpuDevice : gpuDevices) {
                    GpuDeviceVO device = gpuDeviceDao.findByHostIdAndBusAddress(hostId, gpuDevice.getBusAddress());
                    if (device != null) {
                        device.setState(GpuDevice.State.Allocated);
                        device.setVmId(vmId);
                        gpuDeviceDao.persist(device);
                    } else {
                        throw new CloudRuntimeException(
                                String.format("GPU device not found for VM %d on host %d", vmId, hostId));
                    }
                }
            }
        });
    }

    @Override
    public ListResponse<GpuDeviceResponse> discoverGpuDevices(DiscoverGpuDevicesCmd cmd) {
        final Long hostId = cmd.getId();
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            throw new InvalidParameterValueException(String.format("Host with ID %d not found", hostId));
        }
        if (!Status.Up.equals(host.getStatus())) {
            throw new InvalidParameterValueException(String.format("Host [%s] is not in Up status", host));
        }

        // Get GPU stats on the host and update GPU details
        // getGPUStatistics() fetches the stats
        HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = resourceManager.getGPUStatistics(host);
        if (!MapUtils.isEmpty(groupDetails)) {
            resourceManager.updateGPUDetails(host.getId(), groupDetails);
        }

        // Return the list of GPU devices for the host
        List<GpuDeviceVO> gpuDevices = gpuDeviceDao.listByHostId(hostId);
        return getGpuDeviceResponseListResponse(cmd, gpuDevices, gpuDevices.size());
    }

    @Override
    public boolean isGPUDeviceAvailable(Host host, Long vmId, VgpuProfile vgpuProfile, int gpuCount) {
        List<GpuDeviceVO> availableGpuDevices = gpuDeviceDao.listDevicesForAllocation(host.getId(), List.of(vgpuProfile.getId()));
        // TODO: Add checks for grouping
        if (availableGpuDevices.size() >= gpuCount) {
            return true;
        } else {
            // Check if there are already GPU devices assigned to the VM and belonging to the same vGPU profile
            List<GpuDeviceVO> existingGpuDevices = gpuDeviceDao.listByHostAndVm(host.getId(), vmId);
            existingGpuDevices = existingGpuDevices.stream()
                    .filter(device -> device.getVgpuProfileId() == vgpuProfile.getId())
                    .collect(Collectors.toList());
            return existingGpuDevices.size() + availableGpuDevices.size() >= gpuCount;
        }
    }

    @Override
    @DB
    public GPUDeviceTO getGPUDevice(VirtualMachine vm, VgpuProfile vgpuProfile, int gpuCount) {
        return Transaction.execute(new TransactionCallback<GPUDeviceTO>() {
            @Override
            public GPUDeviceTO doInTransaction(TransactionStatus status) {
                List<GpuDeviceVO> existingGpuDevices = gpuDeviceDao.listByVmId(vm.getId());
                if (existingGpuDevices != null && !existingGpuDevices.isEmpty()) {
                    logger.debug("VM {} already has GPU devices {} assigned. Unassigning them.", vm, existingGpuDevices);
                    for (GpuDeviceVO existingDevice : existingGpuDevices) {
                        existingDevice.setVmId(null);
                        existingDevice.setState(GpuDevice.State.Free);
                        gpuDeviceDao.update(existingDevice.getId(), existingDevice);
                    }
                }

                List<GpuDeviceVO> availableGpuDevices = gpuDeviceDao.listDevicesForAllocation(vm.getHostId(), List.of(vgpuProfile.getId()));

                if (availableGpuDevices.size() < gpuCount) {
                    throw new CloudRuntimeException(
                            String.format("Not enough GPU devices available for VM %s on host %d", vm, vm.getHostId()));
                }

                List<GpuDeviceVO> finalGpuDevices = allocateOptimalGpuDevices(availableGpuDevices, gpuCount);

                GpuCardVO gpuCard = gpuCardDao.findById(vgpuProfile.getCardId());

                List<VgpuTypesInfo> vgpuInfoList = new ArrayList<>();
                for (GpuDeviceVO gpuDevice : finalGpuDevices) {
                    gpuDevice.setState(GpuDevice.State.Allocated);
                    gpuDevice.setVmId(vm.getId());
                    gpuDeviceDao.persist(gpuDevice);
                    VgpuTypesInfo vgpuInfo = new VgpuTypesInfo(gpuDevice.getType(), gpuCard.getName(),
                                    vgpuProfile.getName(),
                                    gpuDevice.getBusAddress(), gpuCard.getVendorId(), gpuCard.getVendorName(),
                                    gpuCard.getDeviceId(), gpuCard.getDeviceName());
                    if (gpuDevice.getParentGpuDeviceId() != null) {
                        GpuDeviceVO parentGpuDevice = gpuDeviceDao.findById(gpuDevice.getParentGpuDeviceId());
                        if (parentGpuDevice != null) {
                            vgpuInfo.setParentBusAddress(parentGpuDevice.getBusAddress());
                        }
                    }
                    vgpuInfoList.add(vgpuInfo);
                }

                HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails =
                        getGpuGroupDetailsFromGpuDevices(hostDao.findById(vm.getHostId()));
                return new GPUDeviceTO(gpuCard.getName(), vgpuProfile.getName(), gpuCount, groupDetails, vgpuInfoList);
            }
        });
    }

    /**
     * Allocates optimal GPU devices based on NUMA node alignment, PCIe root alignment, and performance optimization.
     * 
     * @param availableGpuDevices List of available GPU devices
     * @param gpuCount Number of GPUs to allocate
     * @return List of optimally selected GPU devices
     */
    private List<GpuDeviceVO> allocateOptimalGpuDevices(List<GpuDeviceVO> availableGpuDevices, int gpuCount) {
        List<GpuDeviceVO> selectedDevices = new ArrayList<>();
        
        // Group devices by NUMA node
        Map<String, List<GpuDeviceVO>> devicesByNuma = availableGpuDevices.stream()
                .collect(Collectors.groupingBy(device -> 
                    StringUtils.isNotBlank(device.getNumaNode()) ? device.getNumaNode() : "unknown"));
        
        // Sort NUMA nodes by device count (ascending - prefer nodes with fewer devices)
        List<Map.Entry<String, List<GpuDeviceVO>>> sortedNumaNodes = devicesByNuma.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((list1, list2) -> Integer.compare(list1.size(), list2.size())))
                .collect(Collectors.toList());
        
        // Strategy 1: Try to allocate all GPUs from a single NUMA node with same PCIe root
        for (Map.Entry<String, List<GpuDeviceVO>> numaEntry : sortedNumaNodes) {
            List<GpuDeviceVO> numaDevices = numaEntry.getValue();
            if (numaDevices.size() >= gpuCount) {
                // Group by PCIe root within this NUMA node
                Map<String, List<GpuDeviceVO>> devicesByPciRoot = numaDevices.stream()
                        .collect(Collectors.groupingBy(device -> 
                            StringUtils.isNotBlank(device.getPciRoot()) ? device.getPciRoot() : "unknown"));
                
                // Try to find a PCIe root with enough devices
                for (List<GpuDeviceVO> pciRootDevices : devicesByPciRoot.values()) {
                    if (pciRootDevices.size() >= gpuCount) {
                        // Sort by performance criteria (prefer devices with lower IDs for consistency)
                        pciRootDevices.sort(Comparator.comparing(GpuDeviceVO::getId));
                        selectedDevices.addAll(pciRootDevices.subList(0, gpuCount));
                        s_logger.info("Allocated {} GPU devices from single NUMA node {} and PCIe root", 
                                     gpuCount, numaEntry.getKey());
                        return selectedDevices;
                    }
                }
                
                // If no single PCIe root has enough devices, use devices from same NUMA node
                numaDevices.sort(Comparator.comparing(GpuDeviceVO::getId));
                selectedDevices.addAll(numaDevices.subList(0, gpuCount));
                s_logger.info("Allocated {} GPU devices from single NUMA node {} across multiple PCIe roots", 
                             gpuCount, numaEntry.getKey());
                return selectedDevices;
            }
        }
        
        // Strategy 2: Allocate across multiple NUMA nodes, prioritizing nodes with fewer devices
        int remainingCount = gpuCount;
        for (Map.Entry<String, List<GpuDeviceVO>> numaEntry : sortedNumaNodes) {
            List<GpuDeviceVO> numaDevices = numaEntry.getValue();
            int devicesToTake = Math.min(remainingCount, numaDevices.size());
            
            // Within each NUMA node, prioritize same PCIe root if possible
            Map<String, List<GpuDeviceVO>> devicesByPciRoot = numaDevices.stream()
                    .collect(Collectors.groupingBy(device -> 
                        StringUtils.isNotBlank(device.getPciRoot()) ? device.getPciRoot() : "unknown"));
            
            List<GpuDeviceVO> selectedFromNuma = new ArrayList<>();
            
            // First, try to get devices from same PCIe roots
            for (List<GpuDeviceVO> pciRootDevices : devicesByPciRoot.values()) {
                pciRootDevices.sort(Comparator.comparing(GpuDeviceVO::getId));
                int fromThisPciRoot = Math.min(devicesToTake - selectedFromNuma.size(), pciRootDevices.size());
                selectedFromNuma.addAll(pciRootDevices.subList(0, fromThisPciRoot));
                
                if (selectedFromNuma.size() >= devicesToTake) {
                    break;
                }
            }
            
            selectedDevices.addAll(selectedFromNuma);
            remainingCount -= selectedFromNuma.size();
            
            s_logger.info("Allocated {} GPU devices from NUMA node {}", selectedFromNuma.size(), numaEntry.getKey());
            
            if (remainingCount <= 0) {
                break;
            }
        }
        
        if (selectedDevices.size() < gpuCount) {
            throw new CloudRuntimeException(
                    String.format("Could not allocate optimal GPU devices. Required: %d, Available: %d", 
                                 gpuCount, selectedDevices.size()));
        }
        
        s_logger.info("Successfully allocated {} GPU devices across multiple NUMA nodes with optimal strategy", 
                     gpuCount);
        return selectedDevices;
    }

    @Override
    public HashMap<String, HashMap<String, VgpuTypesInfo>> getGpuGroupDetailsFromGpuDevices(final Host host) {
        HashMap<String, HashMap<String, VgpuTypesInfo>> gpuGroupDetails = new HashMap<>();
        List<GpuDeviceVO> gpuDevices = gpuDeviceDao.listByHostId(host.getId());
        for (final GpuDeviceVO device : gpuDevices) {
            // TODO: Verify this information
            // Group name is Card's device name
            // Model name is VgpuProfile's name. passthrough for passthrough

            // Calculate GPU capacity and update gpuGroupDetails
            GpuCardVO card = gpuCardDao.findById(device.getCardId());
            if (!gpuGroupDetails.containsKey(card.getDeviceName())) {
                gpuGroupDetails.put(card.getDeviceName(), new HashMap<>());
            }
            VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(device.getVgpuProfileId());

            VgpuTypesInfo gpuDeviceInfo = gpuGroupDetails.get(card.getDeviceName()).get(vgpuProfile.getName());
            if (gpuDeviceInfo == null) {
                long remainingCapacity = 0L;
                if (GpuDevice.State.Free.equals(device.getState()) &&
                    GpuDevice.ResourceState.Enabled.equals(device.getResourceState())) {
                    remainingCapacity = 1L;
                }
                gpuDeviceInfo = new VgpuTypesInfo(card.getDeviceName(), vgpuProfile.getName(), null,
                        null, null, null, vgpuProfile.getMaxVgpuPerPgpu(),
                        remainingCapacity, 1L);
                gpuGroupDetails.get(card.getDeviceName()).put(vgpuProfile.getName(), gpuDeviceInfo);
            } else {
                // Update the existing VgpuTypesInfo with the new device's information
                if (GpuDevice.State.Free.equals(device.getState()) &&
                    GpuDevice.ResourceState.Enabled.equals(device.getResourceState())) {
                    gpuDeviceInfo.setRemainingCapacity(gpuDeviceInfo.getRemainingCapacity() + 1);
                }
                gpuDeviceInfo.setMaxVmCapacity(gpuDeviceInfo.getMaxCapacity() + 1);
            }
        }
        return gpuGroupDetails;
    }

    @Override
    public void addGpuDevicesToHost(final Host host, final List<VgpuTypesInfo> newGpuDevicesInfo) {
        // Check if the host already has a GPU device with the same bus address
        // If the device exists for the host but not in ssCmd, remove it
        // If the device exists in ssCmd but not in the host, add it to the host
        // If the device exists in both, update device's info
        List<GpuDeviceVO> existingGpuDevices = gpuDeviceDao.listByHostId(host.getId());
        Map<String, GpuDeviceVO> existingGpuDevicesMap = new HashMap<>();
        Map<String, GpuDeviceVO> gpuDevicesToDeleteMap = new HashMap<>();
        for (final GpuDeviceVO device : existingGpuDevices) {
            // TODO: Key might change depending on the actual implementation
            existingGpuDevicesMap.put(device.getBusAddress(), device);
            gpuDevicesToDeleteMap.put(device.getBusAddress(), device);
        }

        Map<String, GpuCardVO> cardMap = new HashMap<>();
        Map<String, VgpuProfileVO> vgpuProfileMap = new HashMap<>();

        for (final VgpuTypesInfo deviceInfo : newGpuDevicesInfo) {
            String cardMapKey = deviceInfo.getDeviceId() + " - " + deviceInfo.getVendorId();
            GpuCardVO card = cardMap.get(cardMapKey);
            if (card == null) {
                card = gpuCardDao.findByVendorIdAndDeviceId(deviceInfo.getVendorId(), deviceInfo.getDeviceId());
                if (card == null) {
                    // Create GPU card if it doesn't exist
                    s_logger.info("Creating new GPU card for vendor ID: {} and device ID: {}",
                                  deviceInfo.getVendorId(), deviceInfo.getDeviceId());

                    String deviceName = StringUtils.isNotBlank(deviceInfo.getDeviceName()) ?
                                       deviceInfo.getDeviceName() : deviceInfo.getGroupName();
                    String vendorName = StringUtils.isNotBlank(deviceInfo.getVendorName()) ?
                                       deviceInfo.getVendorName() : "Unknown Vendor";
                    String cardDisplayName = vendorName + " " + deviceName;

                    card = new GpuCardVO(deviceInfo.getDeviceId(), deviceName, cardDisplayName,
                                        vendorName, deviceInfo.getVendorId());
                    card = gpuCardDao.persist(card);

                    // Create default passthrough profile for the new card
                    VgpuProfileVO passthroughProfile = new VgpuProfileVO("passthrough", "passthrough",
                                                                         card.getId(), 1L);
                    vgpuProfileDao.persist(passthroughProfile);

                    s_logger.info("Created GPU card: {} with passthrough profile", card);
                }
                cardMap.put(cardMapKey, card);
            }

            String vgpuProfileKey = card.getUuid() + " | " + deviceInfo.getModelName();
            VgpuProfileVO vgpuProfile = vgpuProfileMap.get(vgpuProfileKey);
            if (vgpuProfile == null) {
                vgpuProfile = vgpuProfileDao.findByNameAndCardId(deviceInfo.getModelName(), card.getId());
                if (vgpuProfile == null) {
                    // Create vGPU profile if it doesn't exist
                    s_logger.info("Creating new vGPU profile: {} for GPU card: {}",
                                  deviceInfo.getModelName(), card.getName());

                    String profileDescription = "Auto-created profile for " + deviceInfo.getModelName();

                    vgpuProfile = new VgpuProfileVO(deviceInfo.getModelName(), profileDescription,
                                                   card.getId(), deviceInfo.getMaxVpuPerGpu() != null ? deviceInfo.getMaxVpuPerGpu() : 1L);
                    vgpuProfile = vgpuProfileDao.persist(vgpuProfile);

                    s_logger.info("Created vGPU profile: {}", vgpuProfile);
                }
                vgpuProfileMap.put(vgpuProfileKey, vgpuProfile);
            }

            GpuDeviceVO existingDevice = existingGpuDevicesMap.get(deviceInfo.getBusAddress());
            if (existingDevice == null) {
                Long parentGpuDeviceId = null;
                if (deviceInfo.getParentBusAddress() != null) {
                    GpuDeviceVO parentGpuDevice = gpuDeviceDao.findByHostIdAndBusAddress(
                            host.getId(), deviceInfo.getParentBusAddress());
                    if (parentGpuDevice != null) {
                        parentGpuDeviceId = parentGpuDevice.getId();
                    }
                }
                GpuDeviceVO gpuDevice = new GpuDeviceVO(card.getId(), vgpuProfile.getId(), deviceInfo.getBusAddress(),
                        host.getId(), parentGpuDeviceId, deviceInfo.getNumaNode(), deviceInfo.getPciRoot());
                gpuDevice.setHostId(host.getId());
                gpuDevice.setBusAddress(deviceInfo.getBusAddress());
                gpuDevice.setCardId(card.getId());
                setStateAndVmName(deviceInfo, gpuDevice);
                if (!deviceInfo.isPassthroughEnabled()) {
                    gpuDevice.setType(GpuDevice.DeviceType.VGPUOnly);
                }

                gpuDeviceDao.persist(gpuDevice);
            } else {
                // Update the device's info
                existingDevice.setCardId(card.getId());
                existingDevice.setVgpuProfileId(vgpuProfile.getId());
                if (existingDevice.getParentGpuDeviceId() == null && deviceInfo.getParentBusAddress() != null) {
                    GpuDeviceVO parentGpuDevice = gpuDeviceDao.findByHostIdAndBusAddress(host.getId(),
                            deviceInfo.getParentBusAddress());
                    if (parentGpuDevice != null) {
                        existingDevice.setParentGpuDeviceId(parentGpuDevice.getId());
                    }
                }
                setStateAndVmName(deviceInfo, existingDevice);
                gpuDeviceDao.update(existingDevice.getId(), existingDevice);
            }
            gpuDevicesToDeleteMap.remove(deviceInfo.getBusAddress());
        }

        // Remove the devices that are not in the new list
        for (final GpuDeviceVO device : gpuDevicesToDeleteMap.values()) {
            device.setState(GpuDevice.State.Error);
            device.setResourceState(GpuDevice.ResourceState.Disabled);
            gpuDeviceDao.update(device.getId(), device);
        }
    }

    private void setStateAndVmName(VgpuTypesInfo deviceInfo, GpuDeviceVO device) {
        device.setState(GpuDevice.State.Free);
        device.setVmId(null);

        if (StringUtils.isNotBlank(deviceInfo.getVmName())) {
            VMInstanceVO vm = vmInstanceDao.findVMByInstanceName(deviceInfo.getVmName());
            if (vm != null) {
                device.setVmId(vm.getId());
                device.setState(GpuDevice.State.Allocated);
            }
        }
    }

    private boolean updateGpuDeviceResourceState(List<Long> gpuDeviceIds, GpuDevice.ResourceState resourceState) {
        if (CollectionUtils.isEmpty(gpuDeviceIds)) {
            throw new InvalidParameterValueException("GPU device IDs cannot be empty");
        }
        List<GpuDeviceVO> gpuDevices = new ArrayList<>();
        for (Long gpuDeviceId : gpuDeviceIds) {
            GpuDeviceVO gpuDevice = gpuDeviceDao.findById(gpuDeviceId);
            if (gpuDevice == null) {
                throw new InvalidParameterValueException(
                        String.format("GPU device with ID %d not found", gpuDeviceId));
            }

            if (gpuDevice.getResourceState().equals(resourceState)) {
                logger.debug("GPU device {} is already in resource state: {}. Skipping state update.", gpuDevice, resourceState);
            }

            if (gpuDevice.getVmId() != null) {
                throw new InvalidParameterValueException(
                        String.format("Cannot change resource state of GPU device %s as it is in use by VM %d",
                                gpuDevice,
                                gpuDevice.getVmId()));
            }
            gpuDevices.add(gpuDevice);
        }

        for (GpuDeviceVO gpuDevice : gpuDevices) {

            gpuDevice.setResourceState(resourceState);
            gpuDeviceDao.update(gpuDevice.getId(), gpuDevice);
        }
        return true;
    }

    private ListResponse<GpuDeviceResponse> getGpuDeviceResponseListResponse(BaseCmd cmd, List<GpuDeviceVO> gpuDevices,
                                                                             Integer count) {
        ListResponse<GpuDeviceResponse> response = new ListResponse<>();
        List<GpuDeviceResponse> gpuDeviceResponses = new ArrayList<>();

        for (GpuDeviceVO gpuDevice : gpuDevices) {
            GpuDeviceResponse gpuDeviceResponse = createGpuDeviceResponse(gpuDevice);
            gpuDeviceResponses.add(gpuDeviceResponse);
        }

        response.setResponses(gpuDeviceResponses, count);
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    private GpuDeviceResponse createGpuDeviceResponse(GpuDeviceVO gpuDevice) {
        GpuDeviceResponse response = new GpuDeviceResponse();
        response.setId(gpuDevice.getUuid());
        response.setBussAddress(gpuDevice.getBusAddress());
        response.setState(gpuDevice.getState());
        response.setResourceState(gpuDevice.getResourceState());
        response.setType(gpuDevice.getType());

        // Host name lookup
        HostVO host = hostDao.findById(gpuDevice.getHostId());
        if (host != null) {
            response.setHostName(host.getName());
            response.setHostId(host.getUuid());
        }

        // GPU card info
        GpuCardVO gpuCard = gpuCardDao.findById(gpuDevice.getCardId());
        if (gpuCard != null) {
            response.setGpuCardId(gpuCard.getUuid());
            response.setGpuCardName(gpuCard.getName());
        }

        // vGPU profile info
        VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(gpuDevice.getVgpuProfileId());
        if (vgpuProfile != null) {
            response.setVgpuProfileId(vgpuProfile.getUuid());
            response.setVgpuProfileName(vgpuProfile.getName());
        }

        if (gpuDevice.getVmId() != null) {
            UserVmVO vm = userVmManager.getVirtualMachine(gpuDevice.getVmId());
            if (vm != null) {
                response.setVmId(vm.getUuid());
                response.setVmName(vm.getInstanceName());
            } else {
                s_logger.debug("VM with ID {} not found for GPU device {}", gpuDevice.getVmId(), gpuDevice.getUuid());
            }
        }

        if (gpuDevice.getParentGpuDeviceId() != null) {
            GpuDeviceVO parentGpuDevice = gpuDeviceDao.findById(gpuDevice.getParentGpuDeviceId());
            if (parentGpuDevice != null) {
                response.setParentGpuDeviceId(parentGpuDevice.getUuid());
            } else {
                s_logger.debug("Parent GPU device with ID {} not found for GPU device {}", gpuDevice.getParentGpuDeviceId(), gpuDevice.getUuid());
            }
        }

        return response;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GPU_CARD_DELETE, eventDescription = "creating GPU device")
    public GpuDeviceResponse createGpuDevice(CreateGpuDeviceCmd cmd) {
        final Long hostId = cmd.getHostId();
        String busAddress = cmd.getBusAddress();
        final Long gpuCardId = cmd.getGpuCardId();
        final Long vgpuProfileId = cmd.getVgpuProfileId();
        final GpuDevice.DeviceType type = cmd.getType();
        final Long parentGpuDeviceId = cmd.getParentGpuDeviceId();
        final String numaNode = cmd.getNumaNode();
        final String pciRoot = cmd.getPciRoot();

        // Validate inputs
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            throw new InvalidParameterValueException(String.format("Host with ID %d not found", hostId));
        }

        if (StringUtils.isBlank(busAddress)) {
            throw new InvalidParameterValueException("Bus address cannot be empty");
        }
        busAddress = busAddress.trim();

        // Check if a GPU device with the same bus address already exists on this host
        GpuDeviceVO existingDevice = gpuDeviceDao.findByHostIdAndBusAddress(hostId, busAddress);
        if (existingDevice != null) {
            throw new InvalidParameterValueException(String.format(
                    "GPU device with bus address %s already exists on host %s", busAddress, host.getName()));
        }

        // Validate GPU card
        GpuCardVO gpuCard = gpuCardDao.findById(gpuCardId);
        if (gpuCard == null) {
            throw new InvalidParameterValueException(String.format("GPU card with ID %d not found", gpuCardId));
        }

        // Validate vGPU profile
        VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(vgpuProfileId);
        if (vgpuProfile == null) {
            throw new InvalidParameterValueException(String.format("vGPU profile with ID %d not found", vgpuProfileId));
        }

        // Validate that the vGPU profile belongs to the specified GPU card
        if (!vgpuProfile.getCardId().equals(gpuCardId)) {
            throw new InvalidParameterValueException(String.format(
                    "vGPU profile %s does not belong to GPU card %s", vgpuProfile.getName(), gpuCard.getName()));
        }

        // Validate parent GPU device if specified
        if (parentGpuDeviceId != null) {
            GpuDeviceVO parentDevice = gpuDeviceDao.findById(parentGpuDeviceId);
            if (parentDevice == null) {
                throw new InvalidParameterValueException(String.format("Parent GPU device with ID %d not found", parentGpuDeviceId));
            }
            if (!hostId.equals(parentDevice.getHostId())) {
                throw new InvalidParameterValueException("Parent GPU device must be on the same host");
            }
        }

        // Create the GPU device
        GpuDeviceVO gpuDevice = new GpuDeviceVO(gpuCardId, vgpuProfileId, busAddress, hostId, parentGpuDeviceId, numaNode, pciRoot);
        gpuDevice.setType(type);
        gpuDevice.setState(GpuDevice.State.Free);
        gpuDevice.setResourceState(GpuDevice.ResourceState.Enabled);

        gpuDevice = gpuDeviceDao.persist(gpuDevice);

        s_logger.info("Successfully created GPU device {} on host {}", gpuDevice.getUuid(), host.getName());
        return createGpuDeviceResponse(gpuDevice);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GPU_DEVICE_EDIT, eventDescription = "updating GPU device")
    public GpuDeviceResponse updateGpuDevice(UpdateGpuDeviceCmd cmd) {
        final Long id = cmd.getId();
        final Long gpuCardId = cmd.getGpuCardId();
        final Long vgpuProfileId = cmd.getVgpuProfileId();
        final GpuDevice.DeviceType type = cmd.getType();
        final Long parentGpuDeviceId = cmd.getParentGpuDeviceId();
        final String numaNode = cmd.getNumaNode();
        final String pciRoot = cmd.getPciRoot();

        // Validate inputs
        GpuDeviceVO gpuDevice = gpuDeviceDao.findById(id);
        if (gpuDevice == null) {
            throw new InvalidParameterValueException(String.format("GPU device with ID %d not found", id));
        }

        // Check if device is currently allocated to a VM
        if (gpuDevice.getVmId() != null) {
            throw new InvalidParameterValueException(String.format(
                    "Cannot update GPU device %s as it is currently allocated to VM %d",
                    gpuDevice.getUuid(), gpuDevice.getVmId()));
        }

        // Validate GPU card if specified
        if (gpuCardId != null) {
            GpuCardVO gpuCard = gpuCardDao.findById(gpuCardId);
            if (gpuCard == null) {
                throw new InvalidParameterValueException(String.format("GPU card with ID %d not found", gpuCardId));
            }
        }

        // Validate vGPU profile if specified
        VgpuProfileVO vgpuProfile = null;
        if (vgpuProfileId != null) {
            vgpuProfile = vgpuProfileDao.findById(vgpuProfileId);
            if (vgpuProfile == null) {
                throw new InvalidParameterValueException(String.format("vGPU profile with ID %d not found", vgpuProfileId));
            }

            // Check if vGPU profile belongs to the GPU card (either current or new)
            Long targetCardId = gpuCardId != null ? gpuCardId : gpuDevice.getCardId();
            if (!vgpuProfile.getCardId().equals(targetCardId)) {
                GpuCardVO targetCard = gpuCardDao.findById(targetCardId);
                throw new InvalidParameterValueException(String.format(
                        "vGPU profile %s does not belong to GPU card %s", vgpuProfile.getName(), targetCard.getName()));
            }
        }

        // Validate parent GPU device if specified
        if (parentGpuDeviceId != null) {
            GpuDeviceVO parentDevice = gpuDeviceDao.findById(parentGpuDeviceId);
            if (parentDevice == null) {
                throw new InvalidParameterValueException(String.format("Parent GPU device with ID %d not found", parentGpuDeviceId));
            }
            if (parentDevice.getHostId() != gpuDevice.getHostId()) {
                throw new InvalidParameterValueException("Parent GPU device must be on the same host");
            }
            if (parentDevice.getId() == gpuDevice.getId()) {
                throw new InvalidParameterValueException("GPU device cannot be its own parent");
            }
        }

        // Update the GPU device
        if (gpuCardId != null) {
            gpuDevice.setCardId(gpuCardId);
        }
        if (vgpuProfileId != null) {
            gpuDevice.setVgpuProfileId(vgpuProfileId);
        }
        if (type != null) {
            gpuDevice.setType(type);
        }
        if (parentGpuDeviceId != null) {
            gpuDevice.setParentGpuDeviceId(parentGpuDeviceId);
        }
        if (numaNode != null) {
            gpuDevice.setNumaNode(numaNode);
        }
        if (pciRoot != null) {
            gpuDevice.setPciRoot(pciRoot);
        }
        gpuDeviceDao.update(id, gpuDevice);

        s_logger.info("Successfully updated GPU device {}", gpuDevice.getUuid());
        return createGpuDeviceResponse(gpuDevice);
    }
}
