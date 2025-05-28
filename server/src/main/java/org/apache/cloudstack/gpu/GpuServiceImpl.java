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
import com.cloud.gpu.GpuOfferingDetailVO;
import com.cloud.gpu.GpuOfferingVO;
import com.cloud.gpu.VgpuProfileVO;
import com.cloud.gpu.dao.GpuCardDao;
import com.cloud.gpu.dao.GpuDeviceDao;
import com.cloud.gpu.dao.GpuOfferingDao;
import com.cloud.gpu.dao.GpuOfferingDetailsDao;
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
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.BaseCmd;
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
    private GpuOfferingDao gpuOfferingDao;

    @Inject
    private GpuOfferingDetailsDao gpuOfferingDetailsDao;

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

        // GPU Offering Commands
        cmdList.add(CreateGpuOfferingCmd.class);
        cmdList.add(UpdateGpuOfferingCmd.class);
        cmdList.add(ListGpuOfferingsCmd.class);

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
        final Long vramSize = cmd.getVramSize();

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

        GpuCardVO gpuCard = new GpuCardVO(deviceId, deviceName, name, vendorName, vendorId, vramSize);
        gpuCard = gpuCardDao.persist(gpuCard);
        vgpuProfileDao.persist(new VgpuProfileVO("passthrough", "passthrough", gpuCard.getId(), gpuCard.getVramSize()));
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
        final Long vramSize = cmd.getVramSize();

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
        if (vramSize != null) {
            gpuCard.setVramSize(vramSize);
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
        final Long vramSize = cmd.getVramSize();

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

        VgpuProfileVO vgpuProfile = new VgpuProfileVO(profileName, profileDescription, gpuCardId, vramSize);
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
        final Long vramSize = cmd.getVramSize();

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
        if (vramSize != null) {
            vgpuProfile.setVramSize(vramSize);
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

        Pair<List<GpuDeviceVO>, Integer> gpuDevicesAndCount =
                gpuDeviceDao.searchAndCountGpuDevices(id, keyword, hostId, gpuCardId, vgpuProfileId,
                        cmd.getStartIndex(), cmd.getPageSizeVal());

        return getGpuDeviceResponseListResponse(cmd, gpuDevicesAndCount.first(), gpuDevicesAndCount.second());
    }

    @Override
    public boolean disableGpuDevice(DisableGpuDeviceCmd cmd) {
        return updateGpuDeviceState(cmd.getId(), GpuDevice.State.Disabled);
    }

    @Override
    public boolean enableGpuDevice(EnableGpuDeviceCmd cmd) {
        return updateGpuDeviceState(cmd.getId(), GpuDevice.State.Free);
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
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GPU_OFFERING_CREATE, eventDescription = "creating GPU offering")
    public GpuOfferingResponse createGpuOffering(CreateGpuOfferingCmd cmd) {
        final String name = cmd.getName();
        final String description = cmd.getDescription();
        final List<Long> vgpuProfileIds = cmd.getVgpuProfileIds();

        // Validate inputs
        if (StringUtils.isEmpty(name)) {
            throw new InvalidParameterValueException("GPU offering name cannot be empty");
        }

        GpuOfferingVO existingGpuOffering = gpuOfferingDao.findByName(name);
        if (existingGpuOffering != null) {
            throw new InvalidParameterValueException(String.format("GPU offering with name %s already exists", name));
        }

        List<VgpuProfile> vgpuProfileList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(vgpuProfileIds)) {
            for (Long vgpuProfileId : vgpuProfileIds) {
                VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(vgpuProfileId);
                if (vgpuProfile == null) {
                    throw new InvalidParameterValueException(
                            String.format("vGpu profile with id %d not found.", vgpuProfileId));
                }
                vgpuProfileList.add(vgpuProfile);
            }
        }

        // Create the GPU offering
        final GpuOfferingVO gpuOffering = new GpuOfferingVO(name, description);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // Persist the GPU offering
                GpuOfferingVO newGpuOffering = gpuOfferingDao.persist(gpuOffering);

                // Add vGPU profile IDs if provided
                if (vgpuProfileIds != null && !vgpuProfileIds.isEmpty()) {
                    List<GpuOfferingDetailVO> detailList = new ArrayList<>();

                    for (VgpuProfile vgpuProfile : vgpuProfileList) {
                        detailList.add(new GpuOfferingDetailVO(gpuOffering.getId(),
                                GpuOfferingDetailVO.VgpuProfileId, String.valueOf(vgpuProfile.getId()), true));
                    }
                    gpuOfferingDetailsDao.saveDetails(detailList);
                    newGpuOffering.setVgpuProfiles(vgpuProfileList);
                }
            }
        });

        GpuOfferingResponse response = createGpuOfferingResponse(gpuOffering);
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GPU_OFFERING_EDIT, eventDescription = "updating GPU offering")
    public GpuOfferingResponse updateGpuOffering(UpdateGpuOfferingCmd cmd) {
        final Long id = cmd.getId();
        final String name = cmd.getName();
        final String description = cmd.getDescription();
        final Integer sortKey = cmd.getSortKey();
        final List<Long> vgpuProfileIds = cmd.getVgpuProfileIds();
        final GpuOffering.State state = cmd.getState();

        // Validate inputs
        final GpuOfferingVO gpuOffering = gpuOfferingDao.findById(id);
        if (gpuOffering == null) {
            throw new InvalidParameterValueException(String.format("GPU offering with ID %d not found", id));
        }

        // Check for name uniqueness if the name is being updated
        if (name != null && !name.equals(gpuOffering.getName())) {
            GpuOfferingVO existingGpuOffering = gpuOfferingDao.findByName(name);
            if (existingGpuOffering != null) {
                throw new InvalidParameterValueException(
                        String.format("GPU offering with name %s already exists", name));
            }
        }

        List<VgpuProfile> vgpuProfileList = new ArrayList<>();
        if (vgpuProfileIds != null) {
            for (Long vgpuProfileId : vgpuProfileIds) {
                VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(vgpuProfileId);
                if (vgpuProfile == null) {
                    throw new InvalidParameterValueException(
                            String.format("vGPU profile with ID %d not found", vgpuProfileId));
                }
                vgpuProfileList.add(vgpuProfile);
            }
        }

        // Update the GPU offering
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                if (name != null) {
                    gpuOffering.setName(name);
                }
                if (description != null) {
                    gpuOffering.setDescription(description);
                }
                if (sortKey != null) {
                    gpuOffering.setSortKey(sortKey);
                }
                if (state != null) {
                    gpuOffering.setState(state);
                }

                gpuOfferingDao.update(gpuOffering.getId(), gpuOffering);

                // Update vGPU profile IDs if provided
                if (vgpuProfileIds != null) {
                    // First, remove existing associations
                    gpuOfferingDetailsDao.removeDetails(gpuOffering.getId());

                    // Then add the new ones if not empty
                    List<GpuOfferingDetailVO> detailList = new ArrayList<>();

                    for (VgpuProfile vgpuProfile : vgpuProfileList) {
                        detailList.add(new GpuOfferingDetailVO(gpuOffering.getId(),
                                GpuOfferingDetailVO.VgpuProfileId, String.valueOf(vgpuProfile.getId()), true));
                    }
                    gpuOfferingDetailsDao.saveDetails(detailList);

                    // Refresh vGPU profiles list
                    gpuOffering.setVgpuProfiles(vgpuProfileList);
                }
            }
        });

        GpuOfferingResponse response = createGpuOfferingResponse(gpuOffering);
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    @Override
    public ListResponse<GpuOfferingResponse> listGpuOfferings(ListGpuOfferingsCmd cmd) {
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        String name = cmd.getName();
        GpuOffering.State state = cmd.getState();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();

        Pair<List<GpuOfferingVO>, Integer> gpuOfferingAndCount =
                gpuOfferingDao.searchAndCountGpuOfferings(id, keyword, name, state, startIndex, pageSize);
        return getGpuOfferingResponseListResponse(cmd, gpuOfferingAndCount.first(), gpuOfferingAndCount.second());
    }

    @Override
    public boolean isGPUDeviceAvailable(Host host, Long vmId, GpuOffering gpuOffering, int gpuCount) {
        gpuOfferingDao.loadVgpuProfiles((GpuOfferingVO) gpuOffering);
        List<VgpuProfile> vgpuProfiles = gpuOffering.getVgpuProfiles();
        List<Long> vgpuProfileIdList = vgpuProfiles.stream().map(VgpuProfile::getId).collect(Collectors.toList());
        List<GpuDeviceVO> availableGpuDevices = gpuDeviceDao.listDevicesForAllocation(host.getId(), vgpuProfileIdList);
        if (availableGpuDevices.size() >= gpuCount) {
            return true;
        } else {
            // Check if there are already GPU devices assigned to the VM
            List<GpuDeviceVO> existingGpuDevices = gpuDeviceDao.listByHostAndVm(host.getId(), vmId);
            return existingGpuDevices.size() + availableGpuDevices.size() >= gpuCount;
        }
    }

    @Override
    public GPUDeviceTO getGPUDevice(VirtualMachine vm, GpuOffering gpuOffering, int gpuCount) {
        int requiredNumberOfDevices = gpuCount;
        List<GpuDeviceVO> finalGpuDevices = new ArrayList<>();
        List<GpuDeviceVO> existingGpuDevices = gpuDeviceDao.listByHostAndVm(vm.getHostId(), vm.getId());
        gpuOfferingDao.loadVgpuProfiles((GpuOfferingVO) gpuOffering);
        List<VgpuProfile> vgpuProfiles = gpuOffering.getVgpuProfiles();
        Map<Long, VgpuProfile> vgpuProfileIdMap =
                vgpuProfiles.stream().collect(Collectors.toMap(VgpuProfile::getId, vgpuProfile -> vgpuProfile));
        if (existingGpuDevices != null && !existingGpuDevices.isEmpty()) {
            logger.debug("VM {} already has GPU devices {} assigned", vm, existingGpuDevices);
            for (GpuDeviceVO existingDevice : existingGpuDevices) {
                if (finalGpuDevices.size() == gpuCount) {
                    break;
                }
                if (vgpuProfileIdMap.containsKey(existingDevice.getVgpuProfileId())) {
                    finalGpuDevices.add(existingDevice);
                    --requiredNumberOfDevices;
                } else {
                    logger.debug("VM {} has GPU device {} not in vGPU profile list", vm, existingDevice);
                }
            }
        }

        List<GpuDeviceVO> availableGpuDevices =
                gpuDeviceDao.listDevicesForAllocation(vm.getHostId(), new ArrayList<>(vgpuProfileIdMap.keySet()));

        if (availableGpuDevices.size() < requiredNumberOfDevices) {
            throw new CloudRuntimeException(
                    String.format("Not enough GPU devices available for VM %s on host %d", vm, vm.getHostId()));
        }

        for (int i = 0; i < requiredNumberOfDevices; i++) {
            finalGpuDevices.add(availableGpuDevices.get(i));
        }

        List<VgpuTypesInfo> vgpuInfoList = new ArrayList<>();
        for (GpuDeviceVO gpuDevice : finalGpuDevices) {
            gpuDevice.setState(GpuDevice.State.Allocated);
            gpuDevice.setVmId(vm.getId());
            gpuDeviceDao.persist(gpuDevice);
            GpuCardVO gpuCard = gpuCardDao.findById(gpuDevice.getCardId());
            VgpuTypesInfo vgpuInfo =
                    new VgpuTypesInfo(gpuDevice.getType(), gpuCard.getName(),
                            vgpuProfileIdMap.get(gpuDevice.getVgpuProfileId()).getName(),
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
        return new GPUDeviceTO(gpuOffering.getName(), gpuOffering.getName(), gpuCount, groupDetails, vgpuInfoList);
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
                long maxVgpuPerPgpu = card.getVramSize() / vgpuProfile.getVramSize();
                gpuDeviceInfo =
                        new VgpuTypesInfo(card.getDeviceName(), vgpuProfile.getName(), card.getVramSize(), null, null,
                                null, maxVgpuPerPgpu, GpuDevice.State.Free.equals(device.getState()) ? 1L : 0L,
                                1L);
                gpuGroupDetails.get(card.getDeviceName()).put(vgpuProfile.getName(), gpuDeviceInfo);
            } else {
                // Update the existing VgpuTypesInfo with the new device's information
                if (GpuDevice.State.Free.equals(device.getState())) {
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
                    continue;
                }
                cardMap.put(cardMapKey, card);
            }

            String vgpuProfileKey = card.getUuid() + " | " + deviceInfo.getModelName();
            VgpuProfileVO vgpuProfile = vgpuProfileMap.get(vgpuProfileKey);
            if (vgpuProfile == null) {
                vgpuProfile = vgpuProfileDao.findByNameAndCardId(deviceInfo.getModelName(), card.getId());
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
                GpuDeviceVO gpuDevice = new GpuDeviceVO(card.getId(), vgpuProfile.getId(),
                        deviceInfo.getBusAddress(), host.getId(), parentGpuDeviceId);
                gpuDevice.setHostId(host.getId());
                gpuDevice.setBusAddress(deviceInfo.getBusAddress());
                gpuDevice.setCardId(card.getId());
                setStateAndVmName(deviceInfo, gpuDevice);

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
            gpuDeviceDao.remove(device.getId());
        }
    }

    private void setStateAndVmName(VgpuTypesInfo deviceInfo, GpuDeviceVO device) {
        if (!deviceInfo.isPassthroughEnabled()) {
            device.setState(GpuDevice.State.HasVGPUs);
        }

        if (StringUtils.isNotBlank(deviceInfo.getVmName())) {
            VMInstanceVO vm = vmInstanceDao.findVMByInstanceNameIncludingRemoved(deviceInfo.getVmName());
            if (vm != null) {
                device.setVmId(vm.getId());
            }
        }
    }

    private ListResponse<GpuOfferingResponse> getGpuOfferingResponseListResponse(
            BaseCmd cmd, List<GpuOfferingVO> gpuOfferings, Integer count
    ) {
        ListResponse<GpuOfferingResponse> response = new ListResponse<>();
        List<GpuOfferingResponse> gpuOfferingResponses =
                gpuOfferings.stream().map(this::createGpuOfferingResponse).collect(Collectors.toList());

        response.setResponses(gpuOfferingResponses, count);
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    private GpuOfferingResponse createGpuOfferingResponse(GpuOfferingVO gpuOffering) {
        GpuOfferingResponse response = new GpuOfferingResponse(gpuOffering);
        List<VgpuProfileResponse> vgpuProfileResponses = new ArrayList<>();
        if (gpuOffering.getVgpuProfiles() == null) {
            gpuOfferingDao.loadVgpuProfiles(gpuOffering);
        }
        for (VgpuProfile vgpuProfile : gpuOffering.getVgpuProfiles()) {
            VgpuProfileVO vgpuProfileVO = vgpuProfileDao.findById(vgpuProfile.getId());
            if (vgpuProfileVO != null) {
                vgpuProfileResponses.add(
                        new VgpuProfileResponse(vgpuProfileVO, gpuCardDao.findById(vgpuProfile.getCardId())));
            }
        }
        response.setVgpuProfiles(vgpuProfileResponses);
        return response;
    }

    private boolean updateGpuDeviceState(long gpuDeviceId, GpuDevice.State state) {
        GpuDeviceVO gpuDevice = gpuDeviceDao.findById(gpuDeviceId);
        if (gpuDevice == null) {
            throw new InvalidParameterValueException(String.format("GPU device with ID %d not found", gpuDeviceId));
        }
        if (!List.of(GpuDevice.State.Free, GpuDevice.State.Disabled).contains(gpuDevice.getState())) {
            throw new InvalidParameterValueException(
                    String.format("GPU device %s cannot be changed from %s to state: %s", gpuDevice,
                            gpuDevice.getState(), state));
        }
        if (gpuDevice.getState() == state) {
            throw new InvalidParameterValueException(
                    String.format("GPU device %s is already in state: %s", gpuDevice, state));
        }
        if (gpuDevice.getVmId() != null) {
            throw new InvalidParameterValueException(
                    String.format("Cannot change state of GPU device %s as it is in use by VM %d", gpuDevice,
                            gpuDevice.getVmId()));
        }
        gpuDevice.setState(state);
        return gpuDeviceDao.update(gpuDeviceId, gpuDevice);
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
}
