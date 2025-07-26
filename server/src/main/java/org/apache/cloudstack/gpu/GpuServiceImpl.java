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
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.DiscoverGpuDevicesCmd;
import org.apache.cloudstack.api.command.admin.gpu.ListGpuDevicesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.gpu.ManageGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.UnmanageGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateVgpuProfileCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuCardsCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuDevicesCmd;
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
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GpuServiceImpl extends ManagerBase implements GpuService, PluggableService, Configurable {

    @Inject
    private GpuCardDao gpuCardDao;

    @Inject
    private VgpuProfileDao vgpuProfileDao;

    @Inject
    private GpuDeviceDao gpuDeviceDao;

    @Inject
    private ServiceOfferingDao serviceOfferingDao;

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
        logger.info("Configuring GpuServiceImpl: {}", name);
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
        cmdList.add(ListGpuDevicesCmdByAdmin.class);
        cmdList.add(UnmanageGpuDeviceCmd.class);
        cmdList.add(ManageGpuDeviceCmd.class);
        cmdList.add(DiscoverGpuDevicesCmd.class);
        cmdList.add(CreateGpuDeviceCmd.class);
        cmdList.add(UpdateGpuDeviceCmd.class);
        cmdList.add(DeleteGpuDeviceCmd.class);

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
        final Long videoRam = cmd.getVideoRam();

        // Validate inputs
        validateCreateGpuCardParams(deviceId, deviceName, name, vendorName, vendorId);

        GpuCardVO gpuCard = new GpuCardVO(deviceId, deviceName, name, vendorName, vendorId);
        gpuCard = gpuCardDao.persist(gpuCard);

        // Create passthrough vGPU profile with optional display parameters
        VgpuProfileVO passthroughProfile = new VgpuProfileVO("passthrough", "passthrough", gpuCard.getId(), 1L);
        passthroughProfile.setVideoRam(videoRam);
        vgpuProfileDao.persist(passthroughProfile);

        return gpuCard;
    }

    private void validateCreateGpuCardParams(String deviceId, String deviceName, String name, String vendorName, String vendorId) {
        if (StringUtils.isBlank(deviceId)) {
            throw new InvalidParameterValueException("Device ID cannot be blank");
        } else if (!deviceId.matches("^[a-zA-Z0-9]+$")) {
            throw new InvalidParameterValueException("Device ID must be alphanumeric and in hexadecimal format");
        }
        if (StringUtils.isBlank(deviceName)) {
            throw new InvalidParameterValueException("Device name cannot be blank");
        }
        if (StringUtils.isBlank(name)) {
            throw new InvalidParameterValueException("Display name cannot be blank");
        }
        if (StringUtils.isBlank(vendorName)) {
            throw new InvalidParameterValueException("Vendor name cannot be blank");
        }
        if (StringUtils.isBlank(vendorId)) {
            throw new InvalidParameterValueException("Vendor ID cannot be blank");
        } else if (!vendorId.matches("^[a-zA-Z0-9]+$")) {
            throw new InvalidParameterValueException("Vendor ID must be alphanumeric and in hexadecimal format");
        }

        // Check if a GPU card with the same vendor ID and device ID already exists
        GpuCardVO existingGpuCard = gpuCardDao.findByVendorIdAndDeviceId(vendorId, deviceId);
        if (existingGpuCard != null) {
            throw new InvalidParameterValueException(
                    String.format("GPU card with vendor ID %s and device ID %s already exists", vendorId, deviceId));
        }
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

        // delete gpu profiles associated with this GPU card
        int rowsRemoved = vgpuProfileDao.removeByCardId(id);
        logger.info("Removed {} vGPU profiles associated with GPU card {}", rowsRemoved, gpuCard);

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
        final Long videoRam = cmd.getVideoRam();
        final Long maxHeads = cmd.getMaxHeads();
        final Long maxResolutionX = cmd.getMaxResolutionX();
        final Long maxResolutionY = cmd.getMaxResolutionY();

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

        VgpuProfileVO vgpuProfile = new VgpuProfileVO(profileName, profileDescription, gpuCardId, maxVgpuPerPgpu,
                videoRam, maxHeads, maxResolutionX, maxResolutionY);
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
        final Long videoRam = cmd.getVideoRam();
        final Long maxHeads = cmd.getMaxHeads();
        final Long maxResolutionX = cmd.getMaxResolutionX();
        final Long maxResolutionY = cmd.getMaxResolutionY();

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
        if (videoRam != null) {
            vgpuProfile.setVideoRam(videoRam);
        }
        if (maxHeads != null) {
            vgpuProfile.setMaxHeads(maxHeads);
        }
        if (maxResolutionX != null) {
            vgpuProfile.setMaxResolutionX(maxResolutionX);
        }
        if (maxResolutionY != null) {
            vgpuProfile.setMaxResolutionY(maxResolutionY);
        }
        vgpuProfileDao.update(id, vgpuProfile);

        VgpuProfileResponse response = new VgpuProfileResponse(vgpuProfile,
                gpuCardDao.findById(vgpuProfile.getCardId()));
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
        boolean activeOnly = cmd.getActiveOnly();

        Pair<List<GpuCardVO>, Integer> gpuCardsAndCount = gpuCardDao.searchAndCountGpuCards(id, keyword, vendorId,
                vendorName, deviceId, deviceName, activeOnly, cmd.getStartIndex(), cmd.getPageSizeVal());

        return getGpuCardResponseListResponse(cmd, gpuCardsAndCount.first(), gpuCardsAndCount.second());
    }

    private static ListResponse<GpuCardResponse> getGpuCardResponseListResponse(ListGpuCardsCmd cmd,
            List<GpuCardVO> gpuCards, Integer count) {
        ListResponse<GpuCardResponse> response = new ListResponse<>();
        List<GpuCardResponse> gpuCardResponses = new ArrayList<>();

        for (GpuCardVO gpuCard : gpuCards) {
            GpuCardResponse gpuCardResponse = new GpuCardResponse(gpuCard);
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
        boolean activeOnly = cmd.getActiveOnly();

        Pair<List<VgpuProfileVO>, Integer> vgpuProfilesAndCount = vgpuProfileDao.searchAndCountVgpuProfiles(id, name,
                keyword, gpuCardId, activeOnly, cmd.getStartIndex(), cmd.getPageSizeVal());

        return getVgpuProfileResponseListResponse(cmd, vgpuProfilesAndCount.first(), vgpuProfilesAndCount.second());
    }

    private ListResponse<VgpuProfileResponse> getVgpuProfileResponseListResponse(ListVgpuProfilesCmd cmd,
            List<VgpuProfileVO> vgpuProfiles, Integer count) {
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
            throw new InvalidParameterValueException(
                    String.format("GPU device with bus address %s already exists on host %s", busAddress,
                            host.getName()));
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
            throw new InvalidParameterValueException(
                    String.format("vGPU profile %s does not belong to GPU card %s", vgpuProfile.getName(),
                            gpuCard.getName()));
        }

        // Validate parent GPU device if specified
        if (parentGpuDeviceId != null) {
            GpuDeviceVO parentDevice = gpuDeviceDao.findById(parentGpuDeviceId);
            if (parentDevice == null) {
                throw new InvalidParameterValueException(
                        String.format("Parent GPU device with ID %d not found", parentGpuDeviceId));
            }
            if (!hostId.equals(parentDevice.getHostId())) {
                throw new InvalidParameterValueException("Parent GPU device must be on the same host");
            }
        }

        // Create the GPU device
        GpuDeviceVO gpuDevice = new GpuDeviceVO(gpuCardId, vgpuProfileId, busAddress, hostId, parentGpuDeviceId,
                numaNode, null);
        gpuDevice.setType(type);
        gpuDevice.setState(GpuDevice.State.Free);
        gpuDevice.setManagedState(GpuDevice.ManagedState.Managed);

        gpuDevice = gpuDeviceDao.persist(gpuDevice);

        logger.info("Successfully created GPU device {} on host {}", gpuDevice.getUuid(), host.getName());
        return createGpuDeviceResponse(gpuDevice, ResponseObject.ResponseView.Full);
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

        // Validate inputs
        GpuDeviceVO gpuDevice = gpuDeviceDao.findById(id);
        if (gpuDevice == null) {
            throw new InvalidParameterValueException(String.format("GPU device with ID %d not found", id));
        }

        // Check if device is currently allocated to a VM
        if (gpuDevice.getVmId() != null) {
            throw new InvalidParameterValueException(
                    String.format("Cannot update GPU device %s as it is currently allocated to VM %d",
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
                throw new InvalidParameterValueException(
                        String.format("vGPU profile with ID %d not found", vgpuProfileId));
            }

            // Check if vGPU profile belongs to the GPU card (either current or new)
            Long targetCardId = gpuCardId != null ? gpuCardId : gpuDevice.getCardId();
            if (!vgpuProfile.getCardId().equals(targetCardId)) {
                GpuCardVO targetCard = gpuCardDao.findById(targetCardId);
                throw new InvalidParameterValueException(
                        String.format("vGPU profile %s does not belong to GPU card %s", vgpuProfile.getName(),
                                targetCard.getName()));
            }
        }

        // Validate parent GPU device if specified
        if (parentGpuDeviceId != null) {
            GpuDeviceVO parentDevice = gpuDeviceDao.findById(parentGpuDeviceId);
            if (parentDevice == null) {
                throw new InvalidParameterValueException(
                        String.format("Parent GPU device with ID %d not found", parentGpuDeviceId));
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
        gpuDeviceDao.update(id, gpuDevice);

        logger.info("Successfully updated GPU device {}", gpuDevice.getUuid());
        return createGpuDeviceResponse(gpuDevice, ResponseObject.ResponseView.Full);
    }

    @Override
    public ListResponse<GpuDeviceResponse> listGpuDevices(ListGpuDevicesCmd cmd) {
        Long id = null;
        Long hostId = null;
        Long gpuCardId = null;
        Long vgpuProfileId = null;
        if (cmd instanceof ListGpuDevicesCmdByAdmin) {
            ListGpuDevicesCmdByAdmin adminCmd = (ListGpuDevicesCmdByAdmin) cmd;
            id = adminCmd.getId();
            hostId = adminCmd.getHostId();
            gpuCardId = adminCmd.getGpuCardId();
            vgpuProfileId = adminCmd.getVgpuProfileId();
        }
        String keyword = cmd.getKeyword();
        Long vmId = cmd.getVmId();

        Pair<List<GpuDeviceVO>, Integer> gpuDevicesAndCount = gpuDeviceDao.searchAndCountGpuDevices(id, keyword, hostId,
                vmId, gpuCardId, vgpuProfileId, cmd.getStartIndex(), cmd.getPageSizeVal());

        return getGpuDeviceResponseListResponse(cmd, gpuDevicesAndCount.first(), gpuDevicesAndCount.second());
    }

    @Override
    public boolean disableGpuDevice(UnmanageGpuDeviceCmd cmd) {
        return updateGpuDeviceManagedState(cmd.getIds(), GpuDevice.ManagedState.Unmanaged);
    }

    @Override
    public boolean enableGpuDevice(ManageGpuDeviceCmd cmd) {
        return updateGpuDeviceManagedState(cmd.getIds(), GpuDevice.ManagedState.Managed);
    }

    @Override
    public void deallocateAllGpuDevicesForVm(long vmId) {
        List<GpuDeviceVO> devices = gpuDeviceDao.listByVmId(vmId);
        deallocateGpuDevices(devices);
    }

    @Override
    public void deallocateGpuDevicesForVmOnHost(long vmId, long hostId) {
        List<GpuDeviceVO> devices = gpuDeviceDao.listByHostAndVm(hostId, vmId);
        deallocateGpuDevices(devices);
    }

    private void deallocateGpuDevices(List<GpuDeviceVO> devices) {
        if (CollectionUtils.isNotEmpty(devices)) {
            for (GpuDeviceVO device : devices) {
                device.setState(GpuDevice.State.Free);
                device.setVmId(null);
                gpuDeviceDao.persist(device);
                checkAndUpdateParentGpuDeviceState(device.getParentGpuDeviceId());
            }
        }
    }

    protected void checkAndUpdateParentGpuDeviceState(Long parentGpuDeviceId) {
        if (parentGpuDeviceId != null) {
            GpuDeviceVO parentGpuDevice = gpuDeviceDao.findById(parentGpuDeviceId);
            checkAndUpdateParentGpuDeviceState(parentGpuDevice);
        }
    }

    protected void checkAndUpdateParentGpuDeviceState(GpuDeviceVO parentDevice) {
        if (parentDevice != null) {
            List<GpuDeviceVO> childDevices = gpuDeviceDao.listByParentGpuDeviceId(parentDevice.getId());
            GpuDevice.State finalState = GpuDevice.State.Free;
            for (GpuDeviceVO childDevice : childDevices) {
                if (childDevice.getState().equals(GpuDevice.State.Allocated)) {
                    finalState = GpuDevice.State.PartiallyAllocated;
                } else if (childDevice.getState().equals(GpuDevice.State.Error)) {
                    finalState = GpuDevice.State.Error;
                    break;
                }
            }
            if (!finalState.equals(parentDevice.getState())) {
                parentDevice.setState(finalState);
                gpuDeviceDao.update(parentDevice.getId(), parentDevice);
            }
        }
    }

    @Override
    public void allocateGpuDevicesToVmOnHost(long vmId, long hostId, List<VgpuTypesInfo> gpuDevices) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // Deallocate existing GPU devices for the VM on the host
                deallocateAllGpuDevicesForVm(vmId);

                // Allocate new GPU devices to the VM on the host
                for (VgpuTypesInfo gpuDevice : gpuDevices) {
                    GpuDeviceVO device = gpuDeviceDao.findByHostIdAndBusAddress(hostId, gpuDevice.getBusAddress());
                    if (device != null) {
                        device.setState(GpuDevice.State.Allocated);
                        device.setVmId(vmId);
                        gpuDeviceDao.persist(device);
                        checkAndUpdateParentGpuDeviceState(device.getParentGpuDeviceId());
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
        List<GpuDeviceVO> availableGpuDevices = gpuDeviceDao.listDevicesForAllocation(host.getId(),
                vgpuProfile.getId());
        if (availableGpuDevices.size() >= gpuCount) {
            return true;
        } else {
            // Check if there are already GPU devices assigned to the VM and belonging to the same vGPU profile
            List<GpuDeviceVO> existingGpuDevices = gpuDeviceDao.listByHostAndVm(host.getId(), vmId);
            existingGpuDevices = existingGpuDevices.stream().filter(
                    device -> device.getVgpuProfileId() == vgpuProfile.getId()).collect(Collectors.toList());
            return existingGpuDevices.size() + availableGpuDevices.size() >= gpuCount;
        }
    }

    @Override
    @DB
    public GPUDeviceTO getGPUDevice(VirtualMachine vm, long hostId, VgpuProfile vgpuProfile, int gpuCount) {
        return Transaction.execute(new TransactionCallback<GPUDeviceTO>() {
            @Override
            public GPUDeviceTO doInTransaction(TransactionStatus status) {
                if (vm.getHostId() == hostId) {
                    deallocateAllGpuDevicesForVm(vm.getId());
                }

                List<GpuDeviceVO> availableGpuDevices = gpuDeviceDao.listDevicesForAllocation(hostId,
                        vgpuProfile.getId());

                if (availableGpuDevices.size() < gpuCount) {
                    logger.debug("Not enough GPU devices available for VM {}", vm);
                    throw new CloudRuntimeException(
                            String.format("Not enough GPU devices available for VM %s", vm.getUuid()));
                }

                List<GpuDeviceVO> finalGpuDevices = getGpuDevicesToAllocate(availableGpuDevices, gpuCount);

                GpuCardVO gpuCard = gpuCardDao.findById(vgpuProfile.getCardId());

                ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(vm.getServiceOfferingId());
                List<VgpuTypesInfo> vgpuInfoList = new ArrayList<>();
                for (GpuDeviceVO gpuDevice : finalGpuDevices) {
                    gpuDevice.setState(GpuDevice.State.Allocated);
                    gpuDevice.setVmId(vm.getId());
                    gpuDeviceDao.persist(gpuDevice);

                    VgpuTypesInfo vgpuInfo = new VgpuTypesInfo(gpuDevice.getType(), gpuCard.getName(),
                            vgpuProfile.getName(), gpuDevice.getBusAddress(), gpuCard.getVendorId(),
                            gpuCard.getVendorName(), gpuCard.getDeviceId(), gpuCard.getDeviceName());
                    vgpuInfo.setDisplay(serviceOffering.getGpuDisplay());

                    if (gpuDevice.getParentGpuDeviceId() != null) {
                        GpuDeviceVO parentGpuDevice = gpuDeviceDao.findById(gpuDevice.getParentGpuDeviceId());
                        if (parentGpuDevice != null) {
                            vgpuInfo.setParentBusAddress(parentGpuDevice.getBusAddress());
                            checkAndUpdateParentGpuDeviceState(parentGpuDevice);
                        }
                    }
                    vgpuInfoList.add(vgpuInfo);
                }

                HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = getGpuGroupDetailsFromGpuDevicesOnHost(hostId);
                return new GPUDeviceTO(gpuCard.getName(), vgpuProfile.getName(), gpuCount, groupDetails, vgpuInfoList);
            }
        });
    }

    @Override
    public HashMap<String, HashMap<String, VgpuTypesInfo>> getGpuGroupDetailsFromGpuDevicesOnHost(final long hostId) {
        HashMap<String, HashMap<String, VgpuTypesInfo>> gpuGroupDetails = new HashMap<>();
        List<GpuDeviceVO> gpuDevices = gpuDeviceDao.listByHostId(hostId);
        for (final GpuDeviceVO device : gpuDevices) {
            // Calculate GPU capacity and update gpuGroupDetails
            GpuCardVO card = gpuCardDao.findById(device.getCardId());
            String groupName = card.getName();
            if (!gpuGroupDetails.containsKey(groupName)) {
                gpuGroupDetails.put(groupName, new HashMap<>());
            }
            VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(device.getVgpuProfileId());

            VgpuTypesInfo gpuDeviceInfo = gpuGroupDetails.get(groupName).get(vgpuProfile.getName());
            long remainingCapacity = 0L;
            long maxCapacity = 1L;
            if (GpuDevice.State.Free.equals(device.getState()) && GpuDevice.ManagedState.Managed.equals(
                    device.getManagedState())) {
                remainingCapacity = 1L;
            }
            if (GpuDevice.DeviceType.VGPUOnly.equals(device.getType()) ||
                GpuDevice.ManagedState.Unmanaged.equals(device.getManagedState()) ||
                GpuDevice.State.Error.equals(device.getState())
            ) {
                maxCapacity = 0L;
                remainingCapacity = 0L;
            }
            if (gpuDeviceInfo == null) {
                gpuDeviceInfo = new VgpuTypesInfo(card.getName(), vgpuProfile.getName(), vgpuProfile.getVideoRam(), vgpuProfile.getMaxHeads(),
                        vgpuProfile.getMaxResolutionX(), vgpuProfile.getMaxResolutionY(),
                        vgpuProfile.getMaxVgpuPerPgpu(), remainingCapacity, maxCapacity);
                gpuDeviceInfo.setDeviceName(card.getDeviceName());
                gpuDeviceInfo.setVendorId(card.getVendorId());
                gpuDeviceInfo.setVendorName(card.getVendorName());
                gpuDeviceInfo.setDeviceId(card.getDeviceId());
                gpuGroupDetails.get(groupName).put(vgpuProfile.getName(), gpuDeviceInfo);
            } else {
                // Update the existing VgpuTypesInfo with the new device's information
                gpuDeviceInfo.setRemainingCapacity(gpuDeviceInfo.getRemainingCapacity() + remainingCapacity);
                gpuDeviceInfo.setMaxVmCapacity(gpuDeviceInfo.getMaxCapacity() + maxCapacity);
            }
        }
        return gpuGroupDetails;
    }

    /*
     * For the devices in newGpuDevicesInfo, create the GPU card and vGPU profile if they don't exist.
     * For the devices in existingGpuDevices but not in newGpuDevicesInfo, disable the device.
     * For the devices in newGpuDevicesInfo but not in existingGpuDevices, add them to the host.
     * For the devices in both, update the device's info.
     */
    @Override
    @DB
    public void addGpuDevicesToHost(final Host host, final List<VgpuTypesInfo> newGpuDevicesInfo) {
        List<GpuDeviceVO> existingGpuDevices = gpuDeviceDao.listByHostId(host.getId());
        Map<String, GpuDeviceVO> existingGpuDevicesMap = new HashMap<>();
        Map<String, GpuDeviceVO> gpuDevicesToDisableMap = new HashMap<>();
        for (final GpuDeviceVO device : existingGpuDevices) {
            existingGpuDevicesMap.put(device.getBusAddress(), device);
            gpuDevicesToDisableMap.put(device.getBusAddress(), device);
        }

        GlobalLock lock = GlobalLock.getInternLock("add-gpu-devices-to-host-" + host.getId());
        try {
            if (lock.lock(30)) {
                try {
                    Map<String, GpuCardVO> cardMap = new HashMap<>();
                    Map<String, VgpuProfileVO> vgpuProfileMap = new HashMap<>();

                    for (final VgpuTypesInfo deviceInfo : newGpuDevicesInfo) {
                        GpuCardVO card = getGpuCardAndUpdateMap(deviceInfo, cardMap, vgpuProfileMap);
                        VgpuProfileVO vgpuProfile = getVgpuProfileAndUpdateMap(deviceInfo, card, vgpuProfileMap);

                        GpuDeviceVO existingDevice = existingGpuDevicesMap.get(deviceInfo.getBusAddress());
                        if (existingDevice == null) {
                            createAndAddGpuDeviceToHost(deviceInfo, host, card, vgpuProfile);
                        } else {
                            // Update the device's info
                            GpuDeviceVO parentGpuDevice = null;
                            if (existingDevice.getParentGpuDeviceId() == null
                                && deviceInfo.getParentBusAddress() != null) {
                                parentGpuDevice = gpuDeviceDao.findByHostIdAndBusAddress(host.getId(),
                                        deviceInfo.getParentBusAddress());
                                if (parentGpuDevice != null) {
                                    existingDevice.setParentGpuDeviceId(parentGpuDevice.getId());
                                }
                            }
                            if (existingDevice.getPciRoot() == null) {
                                existingDevice.setPciRoot(deviceInfo.getPciRoot());
                            }
                            setStateAndVmName(deviceInfo, existingDevice, parentGpuDevice);
                            gpuDeviceDao.update(existingDevice.getId(), existingDevice);
                            checkAndUpdateParentGpuDeviceState(existingDevice.getParentGpuDeviceId());
                        }
                        gpuDevicesToDisableMap.remove(deviceInfo.getBusAddress());
                    }

                    // Disable the devices that are not in the new list
                    for (final GpuDeviceVO device : gpuDevicesToDisableMap.values()) {
                        logger.info("Disabling GPU device {} on host {} due to missing address in the new devices on the host.", device, host);
                        device.setState(GpuDevice.State.Error);
                        device.setManagedState(GpuDevice.ManagedState.Unmanaged);
                        gpuDeviceDao.update(device.getId(), device);
                        checkAndUpdateParentGpuDeviceState(device.getParentGpuDeviceId());
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            lock.releaseRef();
        }
    }

    @Override
    public boolean deleteGpuDevices(DeleteGpuDeviceCmd deleteGpuDeviceCmd) {
        List<Long> gpuDeviceIds = deleteGpuDeviceCmd.getIds();
        if (CollectionUtils.isEmpty(gpuDeviceIds)) {
            throw new InvalidParameterValueException("GPU device IDs cannot be empty");
        }

        List<GpuDeviceVO> gpuDevices = gpuDeviceDao.listByIds(gpuDeviceIds);
        if (gpuDevices.isEmpty()) {
            throw new InvalidParameterValueException("No GPU devices found for the provided IDs");
        }

        for (GpuDeviceVO gpuDevice : gpuDevices) {
            if (gpuDevice.getVmId() != null) {
                throw new InvalidParameterValueException(
                        String.format("Cannot delete GPU device %s as it is currently allocated to VM %d",
                                gpuDevice.getUuid(), gpuDevice.getVmId()));
            }
            gpuDeviceDao.remove(gpuDevice.getId());
        }
        return true;
    }

    private GpuCardVO getGpuCardAndUpdateMap(VgpuTypesInfo deviceInfo, Map<String, GpuCardVO> cardMap,
            Map<String, VgpuProfileVO> vgpuProfileMap) {
        String cardMapKey = deviceInfo.getDeviceId() + " - " + deviceInfo.getVendorId();
        GpuCardVO card = cardMap.get(cardMapKey);
        if (card == null) {
            card = gpuCardDao.findByVendorIdAndDeviceId(deviceInfo.getVendorId(), deviceInfo.getDeviceId());
            if (card == null) {
                // Create GPU card if it doesn't exist
                logger.info("Creating new GPU card for vendor ID: {} and device ID: {}", deviceInfo.getVendorId(),
                        deviceInfo.getDeviceId());

                String deviceName = StringUtils.isNotBlank(deviceInfo.getDeviceName()) ?
                        deviceInfo.getDeviceName() :
                        deviceInfo.getGroupName();
                String vendorName = StringUtils.isNotBlank(deviceInfo.getVendorName()) ?
                        deviceInfo.getVendorName() :
                        "Unknown Vendor";
                String cardDisplayName = vendorName + " " + deviceName;

                card = new GpuCardVO(deviceInfo.getDeviceId(), deviceName, cardDisplayName, vendorName,
                        deviceInfo.getVendorId());
                card = gpuCardDao.persist(card);

                // Create default passthrough profile for the new card
                VgpuProfileVO passthroughProfile = new VgpuProfileVO("passthrough", "passthrough", card.getId(), 1L);
                passthroughProfile.setVideoRam(deviceInfo.getVideoRam());
                passthroughProfile.setMaxResolutionX(deviceInfo.getMaxResolutionX());
                passthroughProfile.setMaxResolutionY(deviceInfo.getMaxResolutionY());
                passthroughProfile.setMaxHeads(deviceInfo.getMaxHeads());
                passthroughProfile = vgpuProfileDao.persist(passthroughProfile);

                String vgpuProfileKey = card.getUuid() + " | " + deviceInfo.getModelName();
                vgpuProfileMap.put(vgpuProfileKey, passthroughProfile);
                logger.info("Created GPU card: {} with passthrough profile: {}", card, passthroughProfile);
            }
            cardMap.put(cardMapKey, card);
        }
        return card;
    }

    private VgpuProfileVO getVgpuProfileAndUpdateMap(VgpuTypesInfo deviceInfo, GpuCardVO card,
            Map<String, VgpuProfileVO> vgpuProfileMap) {
        String vgpuProfileKey = card.getUuid() + " | " + deviceInfo.getModelName();
        VgpuProfileVO vgpuProfile = vgpuProfileMap.get(vgpuProfileKey);
        if (vgpuProfile == null) {
            vgpuProfile = vgpuProfileDao.findByNameAndCardId(deviceInfo.getModelName(), card.getId());
            if (vgpuProfile == null) {
                // Create vGPU profile if it doesn't exist
                logger.info("Creating new vGPU profile: {} for GPU card: {}", deviceInfo.getModelName(),
                        card.getName());

                vgpuProfile = new VgpuProfileVO(deviceInfo.getModelName(), deviceInfo.getModelName(), card.getId(),
                        deviceInfo.getMaxVpuPerGpu() != null ? deviceInfo.getMaxVpuPerGpu() : 1L);
                vgpuProfile.setVideoRam(deviceInfo.getVideoRam());
                vgpuProfile.setMaxResolutionX(deviceInfo.getMaxResolutionX());
                vgpuProfile.setMaxResolutionY(deviceInfo.getMaxResolutionY());
                vgpuProfile.setMaxHeads(deviceInfo.getMaxHeads());
                vgpuProfile = vgpuProfileDao.persist(vgpuProfile);

                logger.info("Created vGPU profile: {}", vgpuProfile);
            }
            vgpuProfileMap.put(vgpuProfileKey, vgpuProfile);
        }
        return vgpuProfile;
    }

    private void createAndAddGpuDeviceToHost(VgpuTypesInfo deviceInfo, Host host, GpuCardVO card,
            VgpuProfileVO vgpuProfile) {
        Long parentGpuDeviceId = null;
        GpuDeviceVO parentGpuDevice = null;
        if (deviceInfo.getParentBusAddress() != null) {
            parentGpuDevice = gpuDeviceDao.findByHostIdAndBusAddress(host.getId(),
                    deviceInfo.getParentBusAddress());
            if (parentGpuDevice != null) {
                parentGpuDeviceId = parentGpuDevice.getId();
            }
        }
        GpuDeviceVO gpuDevice = new GpuDeviceVO(card.getId(), vgpuProfile.getId(), deviceInfo.getBusAddress(),
                host.getId(), parentGpuDeviceId, deviceInfo.getNumaNode(), deviceInfo.getPciRoot());
        gpuDevice.setHostId(host.getId());
        gpuDevice.setBusAddress(deviceInfo.getBusAddress());
        gpuDevice.setCardId(card.getId());
        setStateAndVmName(deviceInfo, gpuDevice, parentGpuDevice);
        if (!deviceInfo.isPassthroughEnabled()) {
            gpuDevice.setType(GpuDevice.DeviceType.VGPUOnly);
        }

        gpuDevice = gpuDeviceDao.persist(gpuDevice);
        checkAndUpdateParentGpuDeviceState(parentGpuDevice);
        logger.info("Added new GPU device {} to host {}", gpuDevice, host);
    }

    private void setStateAndVmName(VgpuTypesInfo deviceInfo, GpuDeviceVO device, GpuDeviceVO parentGpuDevice) {
        if (StringUtils.isNotBlank(deviceInfo.getVmName())) {
            VMInstanceVO vm = vmInstanceDao.findVMByInstanceName(deviceInfo.getVmName());
            if (vm != null) {
                device.setVmId(vm.getId());
                device.setState(GpuDevice.State.Allocated);
            } else {
                device.setState(GpuDevice.State.Error);
                logger.warn("VM with name {} not found for GPU device {}. Setting state to Error.",
                        deviceInfo.getVmName(), device);
            }
        } else {
            // If no VM name is provided, it's possible that the device is allocated to a stopped VM or not allocated at all.
            if (device.getVmId() == null && !device.getState().equals(GpuDevice.State.PartiallyAllocated)) {
                device.setState(GpuDevice.State.Free);
            } else {
                VMInstanceVO vm = vmInstanceDao.findById(device.getVmId());
                if (vm != null && vm.getState().equals(VirtualMachine.State.Stopped) && !GpuDetachOnStop.valueIn(vm.getDomainId())) {
                    device.setState(GpuDevice.State.Allocated);
                } else {
                    logger.warn("VM with ID {} not found for GPU device {}. Allocated to a removed VM. Setting state to Free.",
                            device.getVmId(), device);
                    device.setState(GpuDevice.State.Free);
                    device.setVmId(null);
                }
            }
        }
    }

    private boolean updateGpuDeviceManagedState(List<Long> gpuDeviceIds, GpuDevice.ManagedState managedState) {
        if (CollectionUtils.isEmpty(gpuDeviceIds)) {
            throw new InvalidParameterValueException("GPU device IDs cannot be empty");
        }
        List<GpuDeviceVO> gpuDevices = new ArrayList<>();
        Set<Long> hostIds = new HashSet<>();
        for (Long gpuDeviceId : gpuDeviceIds) {
            GpuDeviceVO gpuDevice = gpuDeviceDao.findById(gpuDeviceId);
            if (gpuDevice == null) {
                throw new InvalidParameterValueException(String.format("GPU device with ID %d not found", gpuDeviceId));
            }

            if (gpuDevice.getManagedState().equals(managedState)) {
                logger.debug("GPU device {} is already in resource state: {}. Skipping state update.", gpuDevice,
                        managedState);
            }

            if (gpuDevice.getVmId() != null) {
                throw new InvalidParameterValueException(
                        String.format("Cannot change resource state of GPU device %s as it is in use by VM %d",
                                gpuDevice, gpuDevice.getVmId()));
            }
            gpuDevices.add(gpuDevice);
            hostIds.add(gpuDevice.getHostId());
        }

        for (GpuDeviceVO gpuDevice : gpuDevices) {
            gpuDevice.setManagedState(managedState);
            gpuDeviceDao.update(gpuDevice.getId(), gpuDevice);
        }

        for (Long hostId : hostIds) {
            resourceManager.updateGPUDetails(hostId, getGpuGroupDetailsFromGpuDevicesOnHost(hostId));
        }

        return true;
    }

    private ListResponse<GpuDeviceResponse> getGpuDeviceResponseListResponse(BaseCmd cmd, List<GpuDeviceVO> gpuDevices,
            Integer count) {
        ListResponse<GpuDeviceResponse> response = new ListResponse<>();
        List<GpuDeviceResponse> gpuDeviceResponses = new ArrayList<>();

        ResponseObject.ResponseView view = ResponseObject.ResponseView.Full;
        if (cmd instanceof ListGpuDevicesCmdByAdmin) {
            ListGpuDevicesCmd listCmd = (ListGpuDevicesCmdByAdmin) cmd;
            view = listCmd.getResponseView();
        } else if (cmd instanceof ListGpuDevicesCmd) {
            ListGpuDevicesCmd listCmd = (ListGpuDevicesCmd) cmd;
            view = listCmd.getResponseView();
        }

        for (GpuDeviceVO gpuDevice : gpuDevices) {
            GpuDeviceResponse gpuDeviceResponse = createGpuDeviceResponse(gpuDevice, view);
            gpuDeviceResponses.add(gpuDeviceResponse);
        }

        response.setResponses(gpuDeviceResponses, count);
        response.setResponseName(cmd.getCommandName());
        return response;
    }

    private GpuDeviceResponse createGpuDeviceResponse(GpuDeviceVO gpuDevice, ResponseObject.ResponseView view) {
        GpuDeviceResponse response = new GpuDeviceResponse();
        response.setId(gpuDevice.getUuid());
        if (view.equals(ResponseObject.ResponseView.Full)) {
            response.setBussAddress(gpuDevice.getBusAddress());
            response.setState(gpuDevice.getState());
            response.setManagedState(gpuDevice.getManagedState());
            response.setType(gpuDevice.getType());
            response.setNumaNode(gpuDevice.getNumaNode());

            // Host name lookup
            HostVO host = hostDao.findById(gpuDevice.getHostId());
            if (host != null) {
                response.setHostName(host.getName());
                response.setHostId(host.getUuid());
            }

            if (gpuDevice.getParentGpuDeviceId() != null) {
                GpuDeviceVO parentGpuDevice = gpuDeviceDao.findById(gpuDevice.getParentGpuDeviceId());
                if (parentGpuDevice != null) {
                    response.setParentGpuDeviceId(parentGpuDevice.getUuid());
                } else {
                    logger.debug("Parent GPU device with ID {} not found for GPU device {}",
                            gpuDevice.getParentGpuDeviceId(), gpuDevice.getUuid());
                }
            }
        }
        // GPU card info
        GpuCardVO gpuCard = gpuCardDao.findById(gpuDevice.getCardId());
        if (gpuCard != null) {
            if (view.equals(ResponseObject.ResponseView.Full)) {
                response.setGpuCardId(gpuCard.getUuid());
            }
            response.setGpuCardName(gpuCard.getName());
        }

        // vGPU profile info
        VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(gpuDevice.getVgpuProfileId());
        if (vgpuProfile != null) {
            if (view.equals(ResponseObject.ResponseView.Full)) {
                response.setVgpuProfileId(vgpuProfile.getUuid());
            }
            response.setVgpuProfileName(vgpuProfile.getName());
        }

        if (gpuDevice.getVmId() != null) {
            UserVmVO vm = userVmManager.getVirtualMachine(gpuDevice.getVmId());
            if (vm != null) {
                response.setVmId(vm.getUuid());
                response.setVmName(vm.getInstanceName());
                response.setVmState(vm.getState());
            } else {
                logger.debug("VM with ID {} not found for GPU device {}", gpuDevice.getVmId(), gpuDevice.getUuid());
            }
        }

        return response;
    }

    /**
     * Allocates optimal GPU devices for KVM performance based on NUMA node alignment and PCI root optimization.
     * <p>
     * Performance priority (best to acceptable):
     * 1. All GPUs from same NUMA node and same PCI root (best memory locality and PCI bandwidth)
     * 2. All GPUs from same NUMA node across different PCI roots (good memory locality)
     * 3. GPUs distributed across NUMA nodes with PCI root preference within each node
     *
     * @param availableGpuDevices List of available GPU devices
     * @param gpuCount            Number of GPUs to allocate
     * @return List of optimally selected GPU devices
     */
    public List<GpuDeviceVO> getGpuDevicesToAllocate(List<GpuDeviceVO> availableGpuDevices, int gpuCount) {
        if (availableGpuDevices.size() < gpuCount) {
            throw new CloudRuntimeException(
                    String.format("Insufficient GPU devices available. Required: %d, Available: %d", gpuCount,
                            availableGpuDevices.size()));
        }

        // Strategy 1: Try optimal allocation (same NUMA node + same PCI root)
        List<GpuDeviceVO> optimalSelection = tryOptimalAllocation(availableGpuDevices, gpuCount);
        if (optimalSelection != null) {
            logger.info("Allocated {} GPU devices using optimal strategy (same NUMA node and PCI root)", gpuCount);
            return optimalSelection;
        }

        // Strategy 2: Try single NUMA node allocation across multiple PCI roots
        List<GpuDeviceVO> singleNumaSelection = trySingleNumaAllocation(availableGpuDevices, gpuCount);
        if (singleNumaSelection != null) {
            logger.info("Allocated {} GPU devices using single NUMA node strategy", gpuCount);
            return singleNumaSelection;
        }

        // Strategy 3: Distribute across NUMA nodes with PCI root optimization
        List<GpuDeviceVO> distributedSelection = tryDistributedAllocation(availableGpuDevices, gpuCount);
        logger.info("Allocated {} GPU devices using distributed NUMA strategy", gpuCount);
        return distributedSelection;
    }

    /**
     * Attempts to allocate all GPUs from the same NUMA node and PCI root for optimal performance.
     */
    private List<GpuDeviceVO> tryOptimalAllocation(List<GpuDeviceVO> availableDevices, int gpuCount) {
        Map<String, Map<String, List<GpuDeviceVO>>> topology = buildNumaPciTopology(availableDevices);

        for (Map<String, List<GpuDeviceVO>> numaNode : topology.values()) {
            for (List<GpuDeviceVO> pciRootDevices : numaNode.values()) {
                if (pciRootDevices.size() >= gpuCount) {
                    return selectDevicesFromGroup(pciRootDevices, gpuCount);
                }
            }
        }
        return null;
    }

    /**
     * Attempts to allocate all GPUs from a single NUMA node across multiple PCI roots.
     */
    private List<GpuDeviceVO> trySingleNumaAllocation(List<GpuDeviceVO> availableDevices, int gpuCount) {
        Map<String, List<GpuDeviceVO>> devicesByNuma = groupDevicesByNuma(availableDevices);

        // Prioritize NUMA nodes with balanced device distribution
        List<Map.Entry<String, List<GpuDeviceVO>>> sortedNumaNodes = new ArrayList<>();
        for (Map.Entry<String, List<GpuDeviceVO>> entry : devicesByNuma.entrySet()) {
            if (entry.getValue().size() >= gpuCount) {
                sortedNumaNodes.add(entry);
            }
        }
        sortedNumaNodes.sort(Map.Entry.comparingByValue(Comparator.comparing(List::size)));

        for (Map.Entry<String, List<GpuDeviceVO>> numaEntry : sortedNumaNodes) {
            List<GpuDeviceVO> selected = selectDevicesWithPciOptimization(numaEntry.getValue(), gpuCount);
            if (selected.size() == gpuCount) {
                return selected;
            }
        }
        return null;
    }

    /**
     * Distributes GPU allocation across NUMA nodes while optimizing PCI root usage within each node.
     */
    private List<GpuDeviceVO> tryDistributedAllocation(List<GpuDeviceVO> availableDevices, int gpuCount) {
        Map<String, List<GpuDeviceVO>> devicesByNuma = groupDevicesByNuma(availableDevices);
        List<GpuDeviceVO> selectedDevices = new ArrayList<>();

        // Sort NUMA nodes by device count in order for balanced distribution
        List<Map.Entry<String, List<GpuDeviceVO>>> sortedNumaNodes = new ArrayList<>(devicesByNuma.entrySet());
        sortedNumaNodes.sort(Map.Entry.comparingByValue(Comparator.comparing(List::size)));

        int remainingCount = gpuCount;
        for (Map.Entry<String, List<GpuDeviceVO>> numaEntry : sortedNumaNodes) {
            if (remainingCount <= 0) break;

            int devicesNeeded = Math.min(remainingCount, numaEntry.getValue().size());
            List<GpuDeviceVO> selectedFromNuma = selectDevicesWithPciOptimization(numaEntry.getValue(), devicesNeeded);

            selectedDevices.addAll(selectedFromNuma);
            remainingCount -= selectedFromNuma.size();

            logger.debug("Selected {} devices from NUMA node {}", selectedFromNuma.size(), numaEntry.getKey());
        }

        if (selectedDevices.size() < gpuCount) {
            throw new CloudRuntimeException(
                    String.format("Could not allocate required GPU devices. Required: %d, Allocated: %d", gpuCount,
                            selectedDevices.size()));
        }

        return selectedDevices;
    }

    /**
     * Builds a hierarchical topology map: NUMA node -> PCI root -> devices.
     */
    private Map<String, Map<String, List<GpuDeviceVO>>> buildNumaPciTopology(List<GpuDeviceVO> devices) {
        Map<String, Map<String, List<GpuDeviceVO>>> map = new HashMap<>();
        for (GpuDeviceVO device : devices) {
            map.computeIfAbsent(
                    getNumaNodeKey(device), key -> new HashMap<>()
            ).computeIfAbsent(
                    getPciRootKey(device), k -> new ArrayList<>()
            ).add(device);
        }
        return map;
    }

    /**
     * Groups devices by NUMA node.
     */
    private Map<String, List<GpuDeviceVO>> groupDevicesByNuma(List<GpuDeviceVO> devices) {
        Map<String, List<GpuDeviceVO>> map = new HashMap<>();
        for (GpuDeviceVO device : devices) {
            map.computeIfAbsent(getNumaNodeKey(device), k -> new ArrayList<>()).add(device);
        }
        return map;
    }

    /**
     * Selects devices from a group with PCI root optimization, prioritizing same PCI roots.
     */
    private List<GpuDeviceVO> selectDevicesWithPciOptimization(List<GpuDeviceVO> numaDevices, int count) {
        Map<String, List<GpuDeviceVO>> devicesByPciRoot = new HashMap<>();
        for (GpuDeviceVO numaDevice : numaDevices) {
            devicesByPciRoot.computeIfAbsent(getPciRootKey(numaDevice), k -> new ArrayList<>()).add(numaDevice);
        }

        List<GpuDeviceVO> selected = new ArrayList<>();

        // Sort PCI roots by device count (descending) to prioritize roots with more devices
        List<List<GpuDeviceVO>> sortedPciGroups = new ArrayList<>(devicesByPciRoot.values());
        sortedPciGroups.sort(Comparator.comparing(List<GpuDeviceVO>::size).reversed());

        for (List<GpuDeviceVO> pciGroup : sortedPciGroups) {
            if (selected.size() >= count) break;

            int devicesNeeded = Math.min(count - selected.size(), pciGroup.size());
            selected.addAll(selectDevicesFromGroup(pciGroup, devicesNeeded));
        }

        return selected;
    }

    /**
     * Selects devices from a group, sorted by ID for consistency.
     */
    private List<GpuDeviceVO> selectDevicesFromGroup(List<GpuDeviceVO> devices, int count) {
        List<GpuDeviceVO> toSort = new ArrayList<>(devices);
        toSort.sort(Comparator.comparing(GpuDeviceVO::getId));
        List<GpuDeviceVO> list = new ArrayList<>();
        long limit = count;
        for (GpuDeviceVO device : toSort) {
            if (limit-- == 0) break;
            list.add(device);
        }
        return list;
    }

    /**
     * Gets the NUMA node key, handling null/blank values.
     */
    private String getNumaNodeKey(GpuDeviceVO device) {
        return StringUtils.isNotBlank(device.getNumaNode()) ? device.getNumaNode() : "unknown";
    }

    /**
     * Gets the PCI root key, handling null/blank values.
     */
    private String getPciRootKey(GpuDeviceVO device) {
        return StringUtils.isNotBlank(device.getPciRoot()) ? device.getPciRoot() : "unknown";
    }
}
