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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.gpu.GpuCardVO;
import com.cloud.gpu.GpuDeviceVO;
import com.cloud.gpu.VgpuProfileVO;
import com.cloud.gpu.dao.GpuCardDao;
import com.cloud.gpu.dao.GpuDeviceDao;
import com.cloud.gpu.dao.VgpuProfileDao;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.CreateVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.DeleteVgpuProfileCmd;
import org.apache.cloudstack.api.command.admin.gpu.ListGpuDevicesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.gpu.UnmanageGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.DiscoverGpuDevicesCmd;
import org.apache.cloudstack.api.command.admin.gpu.ManageGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuCardCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateGpuDeviceCmd;
import org.apache.cloudstack.api.command.admin.gpu.UpdateVgpuProfileCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuCardsCmd;
import org.apache.cloudstack.api.command.user.gpu.ListVgpuProfilesCmd;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.api.response.GpuDeviceResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VgpuProfileResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GpuServiceImplTest {

    // Test Constants
    private static final String GPU_DEVICE_ID = "1234";
    private static final String GPU_DEVICE_NAME = "RTX 4090";
    private static final String GPU_CARD_NAME = "NVIDIA RTX 4090";
    private static final String GPU_VENDOR_NAME = "NVIDIA";
    private static final String GPU_VENDOR_ID = "10de";
    private static final String VGPU_PROFILE_NAME = "grid_rtx4090-8q";
    private static final String VGPU_PROFILE_DESCRIPTION = "RTX 4090 8GB profile";
    private static final String GPU_BUS_ADDRESS = "0000:01:00.0";
    private static final String GPU_BUS_ADDRESS_2 = "0000:02:00.0";
    private static final String GPU_BUS_ADDRESS_3 = "0000:03:00.0";
    private static final String GPU_CARD_UUID = "gpu-card-uuid";
    private static final String VGPU_PROFILE_UUID = "vgpu-profile-uuid";
    private static final String GPU_DEVICE_UUID = "gpu-device-uuid";
    private static final String HOST_UUID = "host-uuid";
    private static final String HOST_NAME = "test-host";
    private static final String VM_NAME = "test-vm";
    private static final String NUMA_NODE_0 = "numa0";
    private static final String NUMA_NODE_1 = "numa1";
    private static final String PCI_ROOT_1 = "0000:00";
    private static final String PCI_ROOT_2 = "0000:40";
    private static final String PCI_ROOT_3 = "0000:80";
    private static final String TEST_PROFILE_NAME = "test-profile";
    private static final String UPDATED_PROFILE_NAME = "updated-profile";
    private static final String UPDATED_PROFILE_DESCRIPTION = "Updated description";
    private static final String UPDATED_GPU_DEVICE_NAME = "Updated RTX 4090";
    private static final String UPDATED_GPU_CARD_NAME = "Updated NVIDIA RTX 4090";
    private static final String UPDATED_GPU_VENDOR_NAME = "Updated NVIDIA";
    private static final String PASSTHROUGH_PROFILE = "passthrough";
    private static final String COMMAND_CREATE_VGPU_PROFILE = "createVgpuProfile";
    private static final String COMMAND_UPDATE_VGPU_PROFILE = "updateVgpuProfile";
    private static final String COMMAND_LIST_GPU_CARDS = "listGpuCards";
    private static final String COMMAND_LIST_VGPU_PROFILES = "listVgpuProfiles";
    private static final String COMMAND_LIST_GPU_DEVICES = "listGpuDevices";
    private static final String COMMAND_DISCOVER_GPU_DEVICES = "discoverGpuDevices";
    private static final String CONFIG_COMPONENT_NAME = "GpuService";

    // Test IDs
    private static final Long GPU_CARD_ID = 1L;
    private static final Long SERVICE_OFFERING_ID = 1L;
    private static final Long VGPU_PROFILE_ID = 1L;
    private static final Long GPU_DEVICE_ID_LONG = 1L;
    private static final Long HOST_ID = 1L;
    private static final Long VM_ID = 1L;
    private static final Long MAX_VGPU_PER_PGPU = 4L;
    private static final Long UPDATED_MAX_VGPU_PER_PGPU = 8L;
    private static final Long START_INDEX = 0L;
    private static final Long PAGE_SIZE = 20L;
    private static final Long INVALID_ID = 999L;
    private static final Long VM_ID_100 = 100L;
    private static final Long GPU_CARD_ID_2 = 2L;
    private static final Long GPU_DEVICE_ID_2 = 2L;
    private static final Long PARENT_GPU_DEVICE_ID = 2L;
    private static final Long DIFFERENT_HOST_ID = 99L;
    private static final Long DIFFERENT_CARD_ID = 99L;

    @Mock
    private GpuCardDao gpuCardDao;

    @Mock
    private VgpuProfileDao vgpuProfileDao;

    @Mock
    private GpuDeviceDao gpuDeviceDao;

    @Mock
    private ServiceOfferingDao serviceOfferingDao;

    @Mock
    private HostDao hostDao;

    @Mock
    private UserVmManager userVmManager;

    @Mock
    private VMInstanceDao vmInstanceDao;

    @Mock
    private ResourceManager resourceManager;

    @InjectMocks
    @Spy
    private GpuServiceImpl gpuService;

    @Mock
    private GpuCardVO mockGpuCard;

    @Mock
    private VgpuProfileVO mockVgpuProfile;

    @Mock
    private ServiceOfferingVO mockServiceOffering;

    @Mock
    private GpuDeviceVO mockGpuDevice;

    @Mock
    private HostVO mockHost;

    @Before
    public void setUp() {
        // Setup GPU Card mock
        when(mockGpuCard.getId()).thenReturn(GPU_CARD_ID);
        when(mockGpuCard.getUuid()).thenReturn(GPU_CARD_UUID);
        when(mockGpuCard.getDeviceId()).thenReturn(GPU_DEVICE_ID);
        when(mockGpuCard.getDeviceName()).thenReturn(GPU_DEVICE_NAME);
        when(mockGpuCard.getName()).thenReturn(GPU_CARD_NAME);
        when(mockGpuCard.getVendorName()).thenReturn(GPU_VENDOR_NAME);
        when(mockGpuCard.getVendorId()).thenReturn(GPU_VENDOR_ID);

        // Setup vGPU Profile mock
        when(mockVgpuProfile.getId()).thenReturn(VGPU_PROFILE_ID);
        when(mockVgpuProfile.getUuid()).thenReturn(VGPU_PROFILE_UUID);
        when(mockVgpuProfile.getName()).thenReturn(VGPU_PROFILE_NAME);
        when(mockVgpuProfile.getDescription()).thenReturn(VGPU_PROFILE_DESCRIPTION);
        when(mockVgpuProfile.getCardId()).thenReturn(GPU_CARD_ID);
        when(mockVgpuProfile.getMaxVgpuPerPgpu()).thenReturn(MAX_VGPU_PER_PGPU);

        // Setup GPU Device mock
        when(mockGpuDevice.getId()).thenReturn(GPU_DEVICE_ID_LONG);
        when(mockGpuDevice.getUuid()).thenReturn(GPU_DEVICE_UUID);
        when(mockGpuDevice.getBusAddress()).thenReturn(GPU_BUS_ADDRESS);
        when(mockGpuDevice.getHostId()).thenReturn(HOST_ID);
        when(mockGpuDevice.getCardId()).thenReturn(GPU_CARD_ID);
        when(mockGpuDevice.getVgpuProfileId()).thenReturn(VGPU_PROFILE_ID);
        when(mockGpuDevice.getType()).thenReturn(GpuDevice.DeviceType.PCI);
        when(mockGpuDevice.getState()).thenReturn(GpuDevice.State.Free);
        when(mockGpuDevice.getManagedState()).thenReturn(GpuDevice.ManagedState.Managed);
        when(mockGpuDevice.getVmId()).thenReturn(null);
        when(mockGpuDevice.getParentGpuDeviceId()).thenReturn(null);

        // Setup Host mock
        when(mockHost.getId()).thenReturn(HOST_ID);
        when(mockHost.getUuid()).thenReturn(HOST_UUID);
        when(mockHost.getName()).thenReturn(HOST_NAME);
        when(mockHost.getStatus()).thenReturn(Status.Up);

        // Setup Service Offering mock
        when(mockServiceOffering.getGpuDisplay()).thenReturn(false);

    }

    @Test
    public void testConfigure() throws ConfigurationException {
        Map<String, Object> params = new HashMap<>();
        boolean result = gpuService.configure(CONFIG_COMPONENT_NAME, params);
        assertTrue(result);
    }

    @Test
    public void testGetCommands() {
        List<Class<?>> commands = gpuService.getCommands();
        assertNotNull(commands);
        assertFalse(commands.isEmpty());
        assertTrue(commands.contains(CreateGpuCardCmd.class));
        assertTrue(commands.contains(ListGpuCardsCmd.class));
        assertTrue(commands.contains(CreateVgpuProfileCmd.class));
        assertTrue(commands.contains(ListGpuDevicesCmdByAdmin.class));
    }

    @Test
    public void testGetConfigComponentName() {
        String componentName = gpuService.getConfigComponentName();
        assertEquals(CONFIG_COMPONENT_NAME, componentName);
    }

    // GPU Card Tests
    @Test
    public void testCreateGpuCard_Success() {
        CreateGpuCardCmd cmd = mock(CreateGpuCardCmd.class);
        when(cmd.getDeviceId()).thenReturn(GPU_DEVICE_ID);
        when(cmd.getDeviceName()).thenReturn(GPU_DEVICE_NAME);
        when(cmd.getName()).thenReturn(GPU_CARD_NAME);
        when(cmd.getVendorName()).thenReturn(GPU_VENDOR_NAME);
        when(cmd.getVendorId()).thenReturn(GPU_VENDOR_ID);

        when(gpuCardDao.findByVendorIdAndDeviceId(GPU_VENDOR_ID, GPU_DEVICE_ID)).thenReturn(null);
        when(gpuCardDao.persist(any(GpuCardVO.class))).thenReturn(mockGpuCard);
        when(vgpuProfileDao.persist(any(VgpuProfileVO.class))).thenReturn(mockVgpuProfile);

        GpuCardVO result = gpuService.createGpuCard(cmd);

        assertNotNull(result);
        assertEquals(GPU_CARD_UUID, result.getUuid());
        assertEquals(GPU_DEVICE_ID, result.getDeviceId());
        assertEquals(GPU_DEVICE_NAME, result.getDeviceName());
        assertEquals(GPU_CARD_NAME, result.getName());
        assertEquals(GPU_VENDOR_NAME, result.getVendorName());
        assertEquals(GPU_VENDOR_ID, result.getVendorId());
        verify(gpuCardDao).persist(any(GpuCardVO.class));
        verify(vgpuProfileDao).persist(any(VgpuProfileVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuCard_BlankDeviceId() {
        CreateGpuCardCmd cmd = mock(CreateGpuCardCmd.class);
        when(cmd.getDeviceId()).thenReturn("");

        gpuService.createGpuCard(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuCard_NullDeviceId() {
        CreateGpuCardCmd cmd = mock(CreateGpuCardCmd.class);
        when(cmd.getDeviceId()).thenReturn(null);

        gpuService.createGpuCard(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuCard_DuplicateCard() {
        CreateGpuCardCmd cmd = mock(CreateGpuCardCmd.class);
        when(cmd.getDeviceId()).thenReturn(GPU_DEVICE_ID);
        when(cmd.getDeviceName()).thenReturn(GPU_DEVICE_NAME);
        when(cmd.getName()).thenReturn(GPU_CARD_NAME);
        when(cmd.getVendorName()).thenReturn(GPU_VENDOR_NAME);
        when(cmd.getVendorId()).thenReturn(GPU_VENDOR_ID);

        when(gpuCardDao.findByVendorIdAndDeviceId(GPU_VENDOR_ID, GPU_DEVICE_ID)).thenReturn(mockGpuCard);

        gpuService.createGpuCard(cmd);
    }

    @Test
    public void testUpdateGpuCard_Success() {
        UpdateGpuCardCmd cmd = mock(UpdateGpuCardCmd.class);
        when(cmd.getId()).thenReturn(GPU_CARD_ID);
        when(cmd.getDeviceName()).thenReturn(UPDATED_GPU_DEVICE_NAME);
        when(cmd.getName()).thenReturn(UPDATED_GPU_CARD_NAME);
        when(cmd.getVendorName()).thenReturn(UPDATED_GPU_VENDOR_NAME);

        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(gpuCardDao.update(eq(GPU_CARD_ID), any(GpuCardVO.class))).thenReturn(true);

        GpuCardVO result = gpuService.updateGpuCard(cmd);

        assertNotNull(result);
        assertEquals(GPU_CARD_UUID, result.getUuid());
        verify(gpuCardDao).update(eq(GPU_CARD_ID), any(GpuCardVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateGpuCard_NotFound() {
        UpdateGpuCardCmd cmd = mock(UpdateGpuCardCmd.class);
        when(cmd.getId()).thenReturn(INVALID_ID);

        when(gpuCardDao.findById(INVALID_ID)).thenReturn(null);

        gpuService.updateGpuCard(cmd);
    }

    @Test
    public void testDeleteGpuCard_Success() {
        DeleteGpuCardCmd cmd = mock(DeleteGpuCardCmd.class);
        when(cmd.getId()).thenReturn(GPU_CARD_ID);

        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(gpuDeviceDao.isGpuCardInUse(GPU_CARD_ID)).thenReturn(false);
        when(gpuCardDao.remove(GPU_CARD_ID)).thenReturn(true);

        boolean result = gpuService.deleteGpuCard(cmd);

        assertTrue(result);
        verify(gpuCardDao).remove(GPU_CARD_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteGpuCard_InUse() {
        DeleteGpuCardCmd cmd = mock(DeleteGpuCardCmd.class);
        when(cmd.getId()).thenReturn(GPU_CARD_ID);

        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(gpuDeviceDao.isGpuCardInUse(GPU_CARD_ID)).thenReturn(true);

        gpuService.deleteGpuCard(cmd);
    }

    // vGPU Profile Tests
    @Test
    public void testCreateVgpuProfile_Success() {
        CreateVgpuProfileCmd cmd = mock(CreateVgpuProfileCmd.class);
        when(cmd.getName()).thenReturn(VGPU_PROFILE_NAME);
        when(cmd.getDescription()).thenReturn(VGPU_PROFILE_DESCRIPTION);
        when(cmd.getCardId()).thenReturn(GPU_CARD_ID);
        when(cmd.getMaxVgpuPerPgpu()).thenReturn(MAX_VGPU_PER_PGPU);
        when(cmd.getCommandName()).thenReturn(COMMAND_CREATE_VGPU_PROFILE);

        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.persist(any(VgpuProfileVO.class))).thenReturn(mockVgpuProfile);

        VgpuProfileResponse result = gpuService.createVgpuProfile(cmd);

        assertNotNull(result);
        assertEquals(VGPU_PROFILE_UUID, result.getId());
        assertEquals(VGPU_PROFILE_NAME, result.getName());
        assertEquals(VGPU_PROFILE_DESCRIPTION, result.getDescription());
        assertEquals(GPU_CARD_UUID, result.getGpuCardId());
        verify(vgpuProfileDao).persist(any(VgpuProfileVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVgpuProfile_BlankName() {
        CreateVgpuProfileCmd cmd = mock(CreateVgpuProfileCmd.class);
        when(cmd.getName()).thenReturn("");

        gpuService.createVgpuProfile(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVgpuProfile_InvalidCardId() {
        CreateVgpuProfileCmd cmd = mock(CreateVgpuProfileCmd.class);
        when(cmd.getName()).thenReturn(TEST_PROFILE_NAME);
        when(cmd.getCardId()).thenReturn(INVALID_ID);

        when(gpuCardDao.findById(INVALID_ID)).thenReturn(null);

        gpuService.createVgpuProfile(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVgpuProfile_DuplicateName() {
        CreateVgpuProfileCmd cmd = mock(CreateVgpuProfileCmd.class);
        when(cmd.getName()).thenReturn(TEST_PROFILE_NAME);
        when(cmd.getCardId()).thenReturn(GPU_CARD_ID);

        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.findByNameAndCardId(TEST_PROFILE_NAME, GPU_CARD_ID)).thenReturn(mockVgpuProfile);

        gpuService.createVgpuProfile(cmd);
    }

    @Test
    public void testUpdateVgpuProfile_Success() {
        UpdateVgpuProfileCmd cmd = mock(UpdateVgpuProfileCmd.class);
        when(cmd.getId()).thenReturn(VGPU_PROFILE_ID);
        when(cmd.getProfileName()).thenReturn(UPDATED_PROFILE_NAME);
        when(cmd.getDescription()).thenReturn(UPDATED_PROFILE_DESCRIPTION);
        when(cmd.getMaxVgpuPerPgpu()).thenReturn(UPDATED_MAX_VGPU_PER_PGPU);
        when(cmd.getCommandName()).thenReturn(COMMAND_UPDATE_VGPU_PROFILE);

        when(vgpuProfileDao.findById(VGPU_PROFILE_ID)).thenReturn(mockVgpuProfile);
        when(vgpuProfileDao.findByNameAndCardId(UPDATED_PROFILE_NAME, GPU_CARD_ID)).thenReturn(null);
        when(vgpuProfileDao.update(eq(VGPU_PROFILE_ID), any(VgpuProfileVO.class))).thenReturn(true);
        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);

        VgpuProfileResponse result = gpuService.updateVgpuProfile(cmd);

        assertNotNull(result);
        assertEquals(VGPU_PROFILE_UUID, result.getId());
        verify(vgpuProfileDao).update(eq(VGPU_PROFILE_ID), any(VgpuProfileVO.class));
    }

    @Test
    public void testDeleteVgpuProfile_Success() {
        DeleteVgpuProfileCmd cmd = mock(DeleteVgpuProfileCmd.class);
        when(cmd.getId()).thenReturn(VGPU_PROFILE_ID);

        when(vgpuProfileDao.findById(VGPU_PROFILE_ID)).thenReturn(mockVgpuProfile);
        when(gpuDeviceDao.isVgpuProfileInUse(VGPU_PROFILE_ID)).thenReturn(false);
        when(vgpuProfileDao.remove(VGPU_PROFILE_ID)).thenReturn(true);

        boolean result = gpuService.deleteVgpuProfile(cmd);

        assertTrue(result);
        verify(vgpuProfileDao).remove(VGPU_PROFILE_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteVgpuProfile_InUse() {
        DeleteVgpuProfileCmd cmd = mock(DeleteVgpuProfileCmd.class);
        when(cmd.getId()).thenReturn(VGPU_PROFILE_ID);

        when(vgpuProfileDao.findById(VGPU_PROFILE_ID)).thenReturn(mockVgpuProfile);
        when(gpuDeviceDao.isVgpuProfileInUse(VGPU_PROFILE_ID)).thenReturn(true);

        gpuService.deleteVgpuProfile(cmd);
    }

    // List Methods Tests
    @Test
    public void testListGpuCards_Success() {
        ListGpuCardsCmd cmd = mock(ListGpuCardsCmd.class);
        when(cmd.getId()).thenReturn(null);
        when(cmd.getKeyword()).thenReturn(null);
        when(cmd.getVendorName()).thenReturn(null);
        when(cmd.getVendorId()).thenReturn(null);
        when(cmd.getDeviceId()).thenReturn(null);
        when(cmd.getDeviceName()).thenReturn(null);
        when(cmd.getStartIndex()).thenReturn(START_INDEX);
        when(cmd.getPageSizeVal()).thenReturn(PAGE_SIZE);
        when(cmd.getCommandName()).thenReturn(COMMAND_LIST_GPU_CARDS);

        List<GpuCardVO> gpuCards = List.of(mockGpuCard);
        Pair<List<GpuCardVO>, Integer> result = new Pair<>(gpuCards, 1);
        when(gpuCardDao.searchAndCountGpuCards(any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any())).thenReturn(
                result);

        ListResponse<GpuCardResponse> response = gpuService.listGpuCards(cmd);

        assertNotNull(response);
        assertEquals(1, response.getResponses().size());
        assertEquals(GPU_CARD_UUID, response.getResponses().get(0).getId());
    }

    @Test
    public void testListVgpuProfiles_Success() {
        ListVgpuProfilesCmd cmd = mock(ListVgpuProfilesCmd.class);
        when(cmd.getId()).thenReturn(null);
        when(cmd.getName()).thenReturn(null);
        when(cmd.getKeyword()).thenReturn(null);
        when(cmd.getCardId()).thenReturn(null);
        when(cmd.getStartIndex()).thenReturn(START_INDEX);
        when(cmd.getPageSizeVal()).thenReturn(PAGE_SIZE);
        when(cmd.getCommandName()).thenReturn(COMMAND_LIST_VGPU_PROFILES);

        List<VgpuProfileVO> vgpuProfiles = List.of(mockVgpuProfile);
        Pair<List<VgpuProfileVO>, Integer> result = new Pair<>(vgpuProfiles, 1);
        when(vgpuProfileDao.searchAndCountVgpuProfiles(any(), any(), any(), any(), anyBoolean(), any(), any())).thenReturn(result);
        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);

        ListResponse<VgpuProfileResponse> response = gpuService.listVgpuProfiles(cmd);

        assertNotNull(response);
        assertEquals(1, response.getResponses().size());
        assertEquals(VGPU_PROFILE_UUID, response.getResponses().get(0).getId());
    }

    // GPU Device Tests
    @Test
    public void testCreateGpuDevice_Success() {
        CreateGpuDeviceCmd cmd = mock(CreateGpuDeviceCmd.class);
        when(cmd.getHostId()).thenReturn(HOST_ID);
        when(cmd.getBusAddress()).thenReturn(GPU_BUS_ADDRESS);
        when(cmd.getGpuCardId()).thenReturn(GPU_CARD_ID);
        when(cmd.getVgpuProfileId()).thenReturn(VGPU_PROFILE_ID);
        when(cmd.getType()).thenReturn(GpuDevice.DeviceType.PCI);
        when(cmd.getParentGpuDeviceId()).thenReturn(null);
        when(cmd.getNumaNode()).thenReturn(NUMA_NODE_0);

        when(hostDao.findById(HOST_ID)).thenReturn(mockHost);
        when(gpuDeviceDao.findByHostIdAndBusAddress(HOST_ID, GPU_BUS_ADDRESS)).thenReturn(null);
        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.findById(VGPU_PROFILE_ID)).thenReturn(mockVgpuProfile);
        when(gpuDeviceDao.persist(any(GpuDeviceVO.class))).thenReturn(mockGpuDevice);

        GpuDeviceResponse result = gpuService.createGpuDevice(cmd);

        assertNotNull(result);
        assertEquals(GPU_DEVICE_UUID, result.getId());
        assertEquals(GPU_BUS_ADDRESS, result.getBussAddress());
        assertEquals(GPU_CARD_UUID, result.getGpuCardId());
        assertEquals(VGPU_PROFILE_UUID, result.getVgpuProfileId());
        assertEquals(HOST_UUID, result.getHostId());
        verify(gpuDeviceDao).persist(any(GpuDeviceVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuDevice_HostNotFound() {
        CreateGpuDeviceCmd cmd = mock(CreateGpuDeviceCmd.class);
        when(cmd.getHostId()).thenReturn(INVALID_ID);

        when(hostDao.findById(INVALID_ID)).thenReturn(null);

        gpuService.createGpuDevice(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuDevice_DuplicateBusAddress() {
        CreateGpuDeviceCmd cmd = mock(CreateGpuDeviceCmd.class);
        when(cmd.getHostId()).thenReturn(HOST_ID);
        when(cmd.getBusAddress()).thenReturn(GPU_BUS_ADDRESS);

        when(hostDao.findById(HOST_ID)).thenReturn(mockHost);
        when(gpuDeviceDao.findByHostIdAndBusAddress(HOST_ID, GPU_BUS_ADDRESS)).thenReturn(mockGpuDevice);

        gpuService.createGpuDevice(cmd);
    }

    @Test
    public void testUpdateGpuDevice_Success() {
        UpdateGpuDeviceCmd cmd = mock(UpdateGpuDeviceCmd.class);
        when(cmd.getId()).thenReturn(GPU_DEVICE_ID_LONG);
        when(cmd.getGpuCardId()).thenReturn(GPU_CARD_ID_2);
        when(cmd.getVgpuProfileId()).thenReturn(GPU_CARD_ID_2);
        when(cmd.getParentGpuDeviceId()).thenReturn(null);

        GpuDeviceVO device = mock(GpuDeviceVO.class);
        when(device.getCardId()).thenReturn(GPU_CARD_ID);
        when(device.getVgpuProfileId()).thenReturn(VGPU_PROFILE_ID);
        when(device.getVmId()).thenReturn(null);

        VgpuProfileVO newProfile = mock(VgpuProfileVO.class);
        when(newProfile.getCardId()).thenReturn(GPU_CARD_ID_2);

        when(gpuDeviceDao.findById(GPU_DEVICE_ID_LONG)).thenReturn(device);
        when(gpuCardDao.findById(GPU_CARD_ID_2)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.findById(GPU_CARD_ID_2)).thenReturn(newProfile);
        when(gpuDeviceDao.update(eq(GPU_DEVICE_ID_LONG), any(GpuDeviceVO.class))).thenReturn(true);

        GpuDeviceResponse result = gpuService.updateGpuDevice(cmd);

        assertNotNull(result);
        verify(gpuDeviceDao).update(eq(GPU_DEVICE_ID_LONG), any(GpuDeviceVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateGpuDevice_DeviceAllocated() {
        UpdateGpuDeviceCmd cmd = mock(UpdateGpuDeviceCmd.class);
        when(cmd.getId()).thenReturn(GPU_DEVICE_ID_LONG);

        GpuDeviceVO device = mock(GpuDeviceVO.class);
        when(device.getVmId()).thenReturn(VM_ID_100);

        when(gpuDeviceDao.findById(GPU_DEVICE_ID_LONG)).thenReturn(device);

        gpuService.updateGpuDevice(cmd);
    }

    @Test
    public void testListGpuDevices_Success() {
        ListGpuDevicesCmdByAdmin cmd = mock(ListGpuDevicesCmdByAdmin.class);
        when(cmd.getId()).thenReturn(null);
        when(cmd.getKeyword()).thenReturn(null);
        when(cmd.getHostId()).thenReturn(null);
        when(cmd.getGpuCardId()).thenReturn(null);
        when(cmd.getVgpuProfileId()).thenReturn(null);
        when(cmd.getVmId()).thenReturn(null);
        when(cmd.getStartIndex()).thenReturn(START_INDEX);
        when(cmd.getPageSizeVal()).thenReturn(PAGE_SIZE);
        when(cmd.getCommandName()).thenReturn(COMMAND_LIST_GPU_DEVICES);
        when(cmd.getResponseView()).thenReturn(ResponseObject.ResponseView.Full);

        List<GpuDeviceVO> gpuDevices = List.of(mockGpuDevice);
        Pair<List<GpuDeviceVO>, Integer> result = new Pair<>(gpuDevices, 1);
        when(gpuDeviceDao.searchAndCountGpuDevices(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(
                result);

        ListResponse<GpuDeviceResponse> response = gpuService.listGpuDevices(cmd);

        assertNotNull(response);
        assertEquals(1, response.getResponses().size());
        assertEquals(GPU_DEVICE_UUID, response.getResponses().get(0).getId());
    }

    @Test
    public void testDisableGpuDevice_Success() {
        UnmanageGpuDeviceCmd cmd = mock(UnmanageGpuDeviceCmd.class);
        when(cmd.getIds()).thenReturn(List.of(GPU_DEVICE_ID_LONG));

        GpuDeviceVO device = mock(GpuDeviceVO.class);
        when(device.getId()).thenReturn(GPU_DEVICE_ID_LONG);
        when(device.getManagedState()).thenReturn(GpuDevice.ManagedState.Managed);
        when(device.getVmId()).thenReturn(null);
        when(device.getHostId()).thenReturn(HOST_ID);

        when(gpuDeviceDao.findById(GPU_DEVICE_ID_LONG)).thenReturn(device);
        when(gpuDeviceDao.update(eq(GPU_DEVICE_ID_LONG), any(GpuDeviceVO.class))).thenReturn(true);

        doReturn(null).when(gpuService).getGpuGroupDetailsFromGpuDevicesOnHost(anyLong());

        boolean result = gpuService.disableGpuDevice(cmd);

        assertTrue(result);
        verify(gpuDeviceDao).update(eq(GPU_DEVICE_ID_LONG), any(GpuDeviceVO.class));
    }

    @Test
    public void testEnableGpuDevice_Success() {
        ManageGpuDeviceCmd cmd = mock(ManageGpuDeviceCmd.class);
        when(cmd.getIds()).thenReturn(List.of(GPU_DEVICE_ID_LONG));

        GpuDeviceVO device = mock(GpuDeviceVO.class);
        when(device.getId()).thenReturn(GPU_DEVICE_ID_LONG);
        when(device.getManagedState()).thenReturn(GpuDevice.ManagedState.Unmanaged);
        when(device.getVmId()).thenReturn(null);
        when(device.getHostId()).thenReturn(HOST_ID);

        when(gpuDeviceDao.findById(GPU_DEVICE_ID_LONG)).thenReturn(device);
        when(gpuDeviceDao.update(eq(GPU_DEVICE_ID_LONG), any(GpuDeviceVO.class))).thenReturn(true);

        doReturn(null).when(gpuService).getGpuGroupDetailsFromGpuDevicesOnHost(anyLong());

        boolean result = gpuService.enableGpuDevice(cmd);

        assertTrue(result);
        verify(gpuDeviceDao).update(eq(GPU_DEVICE_ID_LONG), any(GpuDeviceVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDisableGpuDevice_EmptyIds() {
        UnmanageGpuDeviceCmd cmd = mock(UnmanageGpuDeviceCmd.class);
        when(cmd.getIds()).thenReturn(new ArrayList<>());

        gpuService.disableGpuDevice(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDisableGpuDevice_DeviceInUse() {
        UnmanageGpuDeviceCmd cmd = mock(UnmanageGpuDeviceCmd.class);
        when(cmd.getIds()).thenReturn(List.of(GPU_DEVICE_ID_LONG));

        GpuDeviceVO device = mock(GpuDeviceVO.class);
        when(device.getVmId()).thenReturn(VM_ID);
        when(device.getManagedState()).thenReturn(GpuDevice.ManagedState.Managed);

        when(gpuDeviceDao.findById(GPU_DEVICE_ID_LONG)).thenReturn(device);

        gpuService.disableGpuDevice(cmd);
    }

    @Test
    public void testDeallocateAllGpuDevicesForVm_Success() {
        List<GpuDeviceVO> devices = List.of(mockGpuDevice);
        when(gpuDeviceDao.listByVmId(VM_ID)).thenReturn(devices);
        when(gpuDeviceDao.persist(any(GpuDeviceVO.class))).thenReturn(mockGpuDevice);

        gpuService.deallocateAllGpuDevicesForVm(VM_ID);

        verify(gpuDeviceDao).persist(any(GpuDeviceVO.class));
    }

    @Test
    public void testDeallocateAllGpuDevicesForVmOnHost_NoDevices() {
        when(gpuDeviceDao.listByVmId(VM_ID)).thenReturn(new ArrayList<>());

        gpuService.deallocateAllGpuDevicesForVm(VM_ID);

        verify(gpuDeviceDao, never()).persist(any(GpuDeviceVO.class));
    }

    @Test
    public void testDiscoverGpuDevices_Success() {
        DiscoverGpuDevicesCmd cmd = mock(DiscoverGpuDevicesCmd.class);
        when(cmd.getId()).thenReturn(HOST_ID);
        when(cmd.getCommandName()).thenReturn(COMMAND_DISCOVER_GPU_DEVICES);

        when(hostDao.findById(HOST_ID)).thenReturn(mockHost);
        when(resourceManager.getGPUStatistics(mockHost)).thenReturn(new HashMap<>());
        when(gpuDeviceDao.listByHostId(HOST_ID)).thenReturn(List.of(mockGpuDevice));

        ListResponse<GpuDeviceResponse> result = gpuService.discoverGpuDevices(cmd);

        assertNotNull(result);
        verify(resourceManager).getGPUStatistics(mockHost);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDiscoverGpuDevices_HostNotFound() {
        DiscoverGpuDevicesCmd cmd = mock(DiscoverGpuDevicesCmd.class);
        when(cmd.getId()).thenReturn(INVALID_ID);

        when(hostDao.findById(INVALID_ID)).thenReturn(null);

        gpuService.discoverGpuDevices(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDiscoverGpuDevices_HostNotUp() {
        DiscoverGpuDevicesCmd cmd = mock(DiscoverGpuDevicesCmd.class);
        when(cmd.getId()).thenReturn(HOST_ID);

        HostVO downHost = mock(HostVO.class);
        when(downHost.getStatus()).thenReturn(Status.Down);

        when(hostDao.findById(HOST_ID)).thenReturn(downHost);

        gpuService.discoverGpuDevices(cmd);
    }

    @Test
    public void testIsGPUDeviceAvailable_Sufficient() {
        VgpuProfile vgpuProfile = mock(VgpuProfile.class);
        when(vgpuProfile.getId()).thenReturn(VGPU_PROFILE_ID);

        GpuDeviceVO additionalDevice = mock(GpuDeviceVO.class);
        List<GpuDeviceVO> availableDevices = List.of(mockGpuDevice, additionalDevice);
        when(gpuDeviceDao.listDevicesForAllocation(HOST_ID, VGPU_PROFILE_ID)).thenReturn(availableDevices);

        boolean result = gpuService.isGPUDeviceAvailable(mockHost, VM_ID, vgpuProfile, 2);

        assertTrue(result);
    }

    @Test
    public void testIsGPUDeviceAvailable_Insufficient() {
        VgpuProfile vgpuProfile = mock(VgpuProfile.class);
        when(vgpuProfile.getId()).thenReturn(VGPU_PROFILE_ID);

        List<GpuDeviceVO> availableDevices = List.of(mockGpuDevice);
        List<GpuDeviceVO> existingDevices = new ArrayList<>();

        when(gpuDeviceDao.listDevicesForAllocation(HOST_ID, VGPU_PROFILE_ID)).thenReturn(availableDevices);
        when(gpuDeviceDao.listByHostAndVm(HOST_ID, VM_ID)).thenReturn(existingDevices);

        boolean result = gpuService.isGPUDeviceAvailable(mockHost, VM_ID, vgpuProfile, 3);

        assertFalse(result);
    }

    @Test
    public void testGetGpuGroupDetailsFromGpuDevicesOnHost_Success() {
        List<GpuDeviceVO> devices = List.of(mockGpuDevice);
        when(gpuDeviceDao.listByHostId(HOST_ID)).thenReturn(devices);
        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.findById(VGPU_PROFILE_ID)).thenReturn(mockVgpuProfile);

        HashMap<String, HashMap<String, VgpuTypesInfo>> result = gpuService.getGpuGroupDetailsFromGpuDevicesOnHost(HOST_ID);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testAddGpuDevicesToHost_NewDevice() {
        VgpuTypesInfo deviceInfo = mock(VgpuTypesInfo.class);
        when(deviceInfo.getBusAddress()).thenReturn(GPU_BUS_ADDRESS_2);
        when(deviceInfo.getDeviceId()).thenReturn(GPU_DEVICE_ID);
        when(deviceInfo.getVendorId()).thenReturn(GPU_VENDOR_ID);
        when(deviceInfo.getDeviceName()).thenReturn(GPU_DEVICE_NAME);
        when(deviceInfo.getVendorName()).thenReturn(GPU_VENDOR_NAME);
        when(deviceInfo.getModelName()).thenReturn(PASSTHROUGH_PROFILE);

        List<VgpuTypesInfo> newDevices = List.of(deviceInfo);
        when(gpuDeviceDao.listByHostId(HOST_ID)).thenReturn(new ArrayList<>());
        when(gpuCardDao.findByVendorIdAndDeviceId(GPU_VENDOR_ID, GPU_DEVICE_ID)).thenReturn(null);
        when(gpuCardDao.persist(any(GpuCardVO.class))).thenReturn(mockGpuCard);
        when(vgpuProfileDao.persist(any(VgpuProfileVO.class))).thenReturn(mockVgpuProfile);
        when(gpuDeviceDao.persist(any(GpuDeviceVO.class))).thenReturn(mockGpuDevice);

        try (MockedStatic<GlobalLock> ignored = Mockito.mockStatic(GlobalLock.class)) {
            GlobalLock lock = mock(GlobalLock.class);
            when(GlobalLock.getInternLock("add-gpu-devices-to-host-" + HOST_ID)).thenReturn(lock);
            when(lock.lock(30)).thenReturn(true);

            gpuService.addGpuDevicesToHost(mockHost, newDevices);

            verify(gpuCardDao).persist(any(GpuCardVO.class));
            verify(vgpuProfileDao, times(1)).persist(any(VgpuProfileVO.class)); // passthrough
            verify(gpuDeviceDao).persist(any(GpuDeviceVO.class));
        }
    }

    @Test
    public void testAddGpuDevicesToHost_ExistingDevice() {
        VgpuTypesInfo deviceInfo = mock(VgpuTypesInfo.class);
        when(deviceInfo.getBusAddress()).thenReturn(GPU_BUS_ADDRESS);
        when(deviceInfo.getDeviceId()).thenReturn(GPU_DEVICE_ID);
        when(deviceInfo.getVendorId()).thenReturn(GPU_VENDOR_ID);
        when(deviceInfo.getModelName()).thenReturn(PASSTHROUGH_PROFILE);

        List<VgpuTypesInfo> newDevices = List.of(deviceInfo);
        List<GpuDeviceVO> existingDevices = List.of(mockGpuDevice);

        when(gpuDeviceDao.listByHostId(HOST_ID)).thenReturn(existingDevices);
        when(gpuCardDao.findByVendorIdAndDeviceId(GPU_VENDOR_ID, GPU_DEVICE_ID)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.findByNameAndCardId(PASSTHROUGH_PROFILE, GPU_CARD_ID)).thenReturn(mockVgpuProfile);
        when(gpuDeviceDao.update(eq(GPU_DEVICE_ID_LONG), any(GpuDeviceVO.class))).thenReturn(true);

        try (MockedStatic<GlobalLock> ignored = Mockito.mockStatic(GlobalLock.class)) {
            GlobalLock lock = mock(GlobalLock.class);
            when(GlobalLock.getInternLock("add-gpu-devices-to-host-" + HOST_ID)).thenReturn(lock);
            when(lock.lock(30)).thenReturn(true);

            gpuService.addGpuDevicesToHost(mockHost, newDevices);
            verify(gpuDeviceDao).update(eq(GPU_DEVICE_ID_LONG), any(GpuDeviceVO.class));
        }
    }

    @Test
    public void testAddGpuDevicesToHost_WithVmName() {
        VgpuTypesInfo deviceInfo = mock(VgpuTypesInfo.class);
        when(deviceInfo.getBusAddress()).thenReturn(GPU_BUS_ADDRESS_2);
        when(deviceInfo.getVmName()).thenReturn(VM_NAME);
        when(deviceInfo.getDeviceId()).thenReturn(GPU_DEVICE_ID);
        when(deviceInfo.getVendorId()).thenReturn(GPU_VENDOR_ID);
        when(deviceInfo.getModelName()).thenReturn(PASSTHROUGH_PROFILE);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(VM_ID_100);

        List<VgpuTypesInfo> newDevices = List.of(deviceInfo);
        when(gpuDeviceDao.listByHostId(HOST_ID)).thenReturn(new ArrayList<>());
        when(gpuCardDao.findByVendorIdAndDeviceId(GPU_VENDOR_ID, GPU_DEVICE_ID)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.findByNameAndCardId(PASSTHROUGH_PROFILE, GPU_CARD_ID)).thenReturn(mockVgpuProfile);
        when(vmInstanceDao.findVMByInstanceName(VM_NAME)).thenReturn(vm);
        when(gpuDeviceDao.persist(any(GpuDeviceVO.class))).thenReturn(mockGpuDevice);

        try (MockedStatic<GlobalLock> ignored = Mockito.mockStatic(GlobalLock.class)) {
            GlobalLock lock = mock(GlobalLock.class);
            when(GlobalLock.getInternLock("add-gpu-devices-to-host-" + HOST_ID)).thenReturn(lock);
            when(lock.lock(30)).thenReturn(true);

            gpuService.addGpuDevicesToHost(mockHost, newDevices);

            verify(vmInstanceDao).findVMByInstanceName(VM_NAME);
            verify(gpuDeviceDao).persist(any(GpuDeviceVO.class));
        }
    }

    @Test
    public void testGetGPUDevice_Success() {
        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.getHostId()).thenReturn(HOST_ID);
        when(vm.getServiceOfferingId()).thenReturn(SERVICE_OFFERING_ID);

        VgpuProfile vgpuProfile = mock(VgpuProfile.class);
        when(vgpuProfile.getId()).thenReturn(VGPU_PROFILE_ID);
        when(vgpuProfile.getCardId()).thenReturn(GPU_CARD_ID);
        when(vgpuProfile.getName()).thenReturn(TEST_PROFILE_NAME);

        List<GpuDeviceVO> availableDevices = List.of(mockGpuDevice);
        // Setup mocks before transaction execution
        when(gpuDeviceDao.listDevicesForAllocation(HOST_ID, VGPU_PROFILE_ID)).thenReturn(availableDevices);
        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(gpuDeviceDao.persist(any(GpuDeviceVO.class))).thenReturn(mockGpuDevice);
        when(gpuDeviceDao.listByHostId(HOST_ID)).thenReturn(availableDevices);
        when(vgpuProfileDao.findById(VGPU_PROFILE_ID)).thenReturn(mockVgpuProfile);
        when(serviceOfferingDao.findById(SERVICE_OFFERING_ID)).thenReturn(mockServiceOffering);
        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {

            transactionMock.when(() -> Transaction.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<GPUDeviceTO> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            GPUDeviceTO result = gpuService.getGPUDevice(vm, HOST_ID, vgpuProfile, 1);

            assertNotNull(result);
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetGPUDevice_InsufficientDevices() {
        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.getHostId()).thenReturn(HOST_ID);

        VgpuProfile vgpuProfile = mock(VgpuProfile.class);
        when(vgpuProfile.getId()).thenReturn(VGPU_PROFILE_ID);

        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {
            when(gpuDeviceDao.listDevicesForAllocation(HOST_ID, VGPU_PROFILE_ID)).thenReturn(new ArrayList<>());

            transactionMock.when(() -> Transaction.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<GPUDeviceTO> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            gpuService.getGPUDevice(vm, HOST_ID, vgpuProfile, 2);
        }
    }

    @Test
    public void testAllocateGpuDevicesToVmOnHost_Success() {
        VgpuTypesInfo deviceInfo = mock(VgpuTypesInfo.class);
        when(deviceInfo.getBusAddress()).thenReturn(GPU_BUS_ADDRESS);

        List<VgpuTypesInfo> gpuDevices = List.of(deviceInfo);
        when(gpuDeviceDao.listByVmId(VM_ID)).thenReturn(new ArrayList<>());
        when(gpuDeviceDao.findByHostIdAndBusAddress(HOST_ID, GPU_BUS_ADDRESS)).thenReturn(mockGpuDevice);
        when(gpuDeviceDao.persist(any(GpuDeviceVO.class))).thenReturn(mockGpuDevice);

        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {
            transactionMock.when(() -> Transaction.execute(any(TransactionCallbackNoReturn.class))).thenAnswer(
                    invocation -> {
                        TransactionCallbackNoReturn callback = invocation.getArgument(0);
                        callback.doInTransactionWithoutResult(null);
                        return null;
                    });

            gpuService.allocateGpuDevicesToVmOnHost(VM_ID, HOST_ID, gpuDevices);

            verify(gpuDeviceDao).findByHostIdAndBusAddress(HOST_ID, GPU_BUS_ADDRESS);
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAllocateGpuDevicesToVmOnHost_DeviceNotFound() {
        VgpuTypesInfo deviceInfo = mock(VgpuTypesInfo.class);
        when(deviceInfo.getBusAddress()).thenReturn(GPU_BUS_ADDRESS);

        List<VgpuTypesInfo> gpuDevices = List.of(deviceInfo);
        when(gpuDeviceDao.listByVmId(VM_ID)).thenReturn(new ArrayList<>());
        when(gpuDeviceDao.findByHostIdAndBusAddress(HOST_ID, GPU_BUS_ADDRESS)).thenReturn(null);

        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {
            transactionMock.when(() -> Transaction.execute(any(TransactionCallbackNoReturn.class))).thenAnswer(
                    invocation -> {
                        TransactionCallbackNoReturn callback = invocation.getArgument(0);
                        callback.doInTransactionWithoutResult(null);
                        return null;
                    });

            gpuService.allocateGpuDevicesToVmOnHost(VM_ID, HOST_ID, gpuDevices);
        }
    }

    // Additional edge case tests
    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuCard_BlankVendorName() {
        CreateGpuCardCmd cmd = mock(CreateGpuCardCmd.class);
        when(cmd.getDeviceId()).thenReturn(GPU_DEVICE_ID);
        when(cmd.getDeviceName()).thenReturn(GPU_DEVICE_NAME);
        when(cmd.getName()).thenReturn(GPU_CARD_NAME);
        when(cmd.getVendorName()).thenReturn("");
        when(cmd.getVendorId()).thenReturn(GPU_VENDOR_ID);

        gpuService.createGpuCard(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuCard_BlankName() {
        CreateGpuCardCmd cmd = mock(CreateGpuCardCmd.class);
        when(cmd.getDeviceId()).thenReturn(GPU_DEVICE_ID);
        when(cmd.getDeviceName()).thenReturn(GPU_DEVICE_NAME);
        when(cmd.getName()).thenReturn("");
        when(cmd.getVendorName()).thenReturn(GPU_VENDOR_NAME);
        when(cmd.getVendorId()).thenReturn(GPU_VENDOR_ID);

        gpuService.createGpuCard(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuDevice_BlankBusAddress() {
        CreateGpuDeviceCmd cmd = mock(CreateGpuDeviceCmd.class);
        when(cmd.getHostId()).thenReturn(HOST_ID);
        when(cmd.getBusAddress()).thenReturn("");

        when(hostDao.findById(HOST_ID)).thenReturn(mockHost);

        gpuService.createGpuDevice(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuDevice_InvalidVgpuProfile() {
        CreateGpuDeviceCmd cmd = mock(CreateGpuDeviceCmd.class);
        when(cmd.getHostId()).thenReturn(HOST_ID);
        when(cmd.getBusAddress()).thenReturn(GPU_BUS_ADDRESS);
        when(cmd.getGpuCardId()).thenReturn(GPU_CARD_ID);
        when(cmd.getVgpuProfileId()).thenReturn(GPU_CARD_ID_2); // Different card ID

        VgpuProfileVO wrongProfile = mock(VgpuProfileVO.class);
        when(wrongProfile.getCardId()).thenReturn(DIFFERENT_CARD_ID); // Different card ID

        when(hostDao.findById(HOST_ID)).thenReturn(mockHost);
        when(gpuDeviceDao.findByHostIdAndBusAddress(HOST_ID, GPU_BUS_ADDRESS)).thenReturn(null);
        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.findById(GPU_CARD_ID_2)).thenReturn(wrongProfile);

        gpuService.createGpuDevice(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateGpuDevice_ParentDeviceOnDifferentHost() {
        CreateGpuDeviceCmd cmd = mock(CreateGpuDeviceCmd.class);
        when(cmd.getHostId()).thenReturn(HOST_ID);
        when(cmd.getBusAddress()).thenReturn(GPU_BUS_ADDRESS);
        when(cmd.getGpuCardId()).thenReturn(GPU_CARD_ID);
        when(cmd.getVgpuProfileId()).thenReturn(VGPU_PROFILE_ID);
        when(cmd.getParentGpuDeviceId()).thenReturn(PARENT_GPU_DEVICE_ID);

        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getHostId()).thenReturn(DIFFERENT_HOST_ID); // Different host

        when(hostDao.findById(HOST_ID)).thenReturn(mockHost);
        when(gpuDeviceDao.findByHostIdAndBusAddress(HOST_ID, GPU_BUS_ADDRESS)).thenReturn(null);
        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);
        when(vgpuProfileDao.findById(VGPU_PROFILE_ID)).thenReturn(mockVgpuProfile);
        when(gpuDeviceDao.findById(PARENT_GPU_DEVICE_ID)).thenReturn(parentDevice);

        gpuService.createGpuDevice(cmd);
    }

    @Test
    public void testIsGPUDeviceAvailable_WithExistingDevices() {
        VgpuProfile vgpuProfile = mock(VgpuProfile.class);
        when(vgpuProfile.getId()).thenReturn(VGPU_PROFILE_ID);

        List<GpuDeviceVO> availableDevices = List.of(mockGpuDevice);

        GpuDeviceVO existingDevice = mock(GpuDeviceVO.class);
        when(existingDevice.getVgpuProfileId()).thenReturn(VGPU_PROFILE_ID);
        List<GpuDeviceVO> existingDevices = List.of(existingDevice);

        when(gpuDeviceDao.listDevicesForAllocation(HOST_ID, VGPU_PROFILE_ID)).thenReturn(availableDevices);
        when(gpuDeviceDao.listByHostAndVm(HOST_ID, VM_ID)).thenReturn(existingDevices);

        boolean result = gpuService.isGPUDeviceAvailable(mockHost, VM_ID, vgpuProfile, 2);

        assertTrue(result);
    }

    @Test
    public void testAddGpuDevicesToHost_DisableRemovedDevices() {
        // Setup existing device that won't be in the new list
        GpuDeviceVO existingDevice = mock(GpuDeviceVO.class);
        when(existingDevice.getId()).thenReturn(GPU_DEVICE_ID_2);
        when(existingDevice.getBusAddress()).thenReturn(GPU_BUS_ADDRESS_3);
        List<GpuDeviceVO> existingDevices = List.of(existingDevice);

        List<VgpuTypesInfo> newDevices = new ArrayList<>();

        when(gpuDeviceDao.listByHostId(HOST_ID)).thenReturn(existingDevices);
        when(gpuDeviceDao.update(eq(GPU_DEVICE_ID_2), any(GpuDeviceVO.class))).thenReturn(true);

        try (MockedStatic<GlobalLock> ignored = Mockito.mockStatic(GlobalLock.class)) {
            GlobalLock lock = mock(GlobalLock.class);
            when(GlobalLock.getInternLock("add-gpu-devices-to-host-" + HOST_ID)).thenReturn(lock);
            when(lock.lock(30)).thenReturn(true);

            gpuService.addGpuDevicesToHost(mockHost, newDevices);
            verify(gpuDeviceDao).update(eq(GPU_DEVICE_ID_2), any(GpuDeviceVO.class));
        }
    }

    @Test
    public void testUpdateVgpuProfile_SameName() {
        UpdateVgpuProfileCmd cmd = mock(UpdateVgpuProfileCmd.class);
        when(cmd.getId()).thenReturn(VGPU_PROFILE_ID);
        when(cmd.getProfileName()).thenReturn(VGPU_PROFILE_NAME); // Same as existing
        when(cmd.getDescription()).thenReturn(UPDATED_PROFILE_DESCRIPTION);
        when(cmd.getCommandName()).thenReturn(COMMAND_UPDATE_VGPU_PROFILE);

        when(vgpuProfileDao.findById(VGPU_PROFILE_ID)).thenReturn(mockVgpuProfile);
        when(vgpuProfileDao.update(eq(VGPU_PROFILE_ID), any(VgpuProfileVO.class))).thenReturn(true);
        when(gpuCardDao.findById(GPU_CARD_ID)).thenReturn(mockGpuCard);

        VgpuProfileResponse result = gpuService.updateVgpuProfile(cmd);

        assertNotNull(result);
        assertEquals(VGPU_PROFILE_UUID, result.getId());
        verify(vgpuProfileDao, never()).findByNameAndCardId(anyString(), anyLong());
    }

    // Tests for the refactored GPU allocation methods
    @Test
    public void testGetGpuDevicesToAllocate_OptimalAllocation() {
        // Create devices in same NUMA node and PCIe root
        List<GpuDeviceVO> availableDevices = createMockGpuDevices(4, NUMA_NODE_0, PCI_ROOT_1);

        List<GpuDeviceVO> result = gpuService.getGpuDevicesToAllocate(availableDevices, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
        // Should select first 2 devices (sorted by ID)
        assertEquals(GPU_DEVICE_ID_LONG.longValue(), result.get(0).getId());
        assertEquals(GPU_DEVICE_ID_2.longValue(), result.get(1).getId());
    }

    // Helper methods for creating mock GPU devices
    private List<GpuDeviceVO> createMockGpuDevices(int count, String numaNode, String pciRoot) {
        return createMockGpuDevices(count, numaNode, pciRoot, GPU_DEVICE_ID_LONG);
    }

    private List<GpuDeviceVO> createMockGpuDevices(int count, String numaNode, String pciRoot, long startId) {
        List<GpuDeviceVO> devices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GpuDeviceVO device = mock(GpuDeviceVO.class);
            Long deviceId = startId + i;
            when(device.getId()).thenReturn(deviceId);
            when(device.getNumaNode()).thenReturn(numaNode);
            when(device.getPciRoot()).thenReturn(pciRoot);
            // Ensure mock is properly configured
            devices.add(device);
        }
        return devices;
    }

    @Test
    public void testGetGpuDevicesToAllocate_SingleNumaAllocation() {
        // Create devices in same NUMA node but different PCIe roots
        List<GpuDeviceVO> availableDevices = new ArrayList<>();
        availableDevices.addAll(createMockGpuDevices(2, NUMA_NODE_0, PCI_ROOT_1, GPU_DEVICE_ID_LONG));
        availableDevices.addAll(createMockGpuDevices(2, NUMA_NODE_0, PCI_ROOT_2, 3L));

        List<GpuDeviceVO> result = gpuService.getGpuDevicesToAllocate(availableDevices, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
        // Should prefer devices from same PCIe root first
        assertTrue(result.stream().allMatch(device -> NUMA_NODE_0.equals(device.getNumaNode())));
    }

    @Test
    public void testGetGpuDevicesToAllocate_DistributedAllocation() {
        // Create devices across different NUMA nodes
        List<GpuDeviceVO> availableDevices = new ArrayList<>();
        availableDevices.addAll(createMockGpuDevices(2, NUMA_NODE_0, PCI_ROOT_1, GPU_DEVICE_ID_LONG));
        availableDevices.addAll(createMockGpuDevices(2, NUMA_NODE_1, PCI_ROOT_3, 3L));

        List<GpuDeviceVO> result = gpuService.getGpuDevicesToAllocate(availableDevices, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
        // Should distribute across NUMA nodes
        long numa0Count = result.stream().filter(device -> NUMA_NODE_0.equals(device.getNumaNode())).count();
        long numa1Count = result.stream().filter(device -> NUMA_NODE_1.equals(device.getNumaNode())).count();
        assertTrue(numa0Count > 0 && numa1Count > 0);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetGpuDevicesToAllocate_InsufficientDevices() {
        List<GpuDeviceVO> availableDevices = createMockGpuDevices(2, NUMA_NODE_0, PCI_ROOT_1);

        gpuService.getGpuDevicesToAllocate(availableDevices, 5);
    }

    @Test
    public void testGetGpuDevicesToAllocate_UnknownNumaAndPci() {
        // Test with devices having null/blank NUMA nodes and PCIe roots
        List<GpuDeviceVO> availableDevices = createMockGpuDevicesWithNullValues(3);

        List<GpuDeviceVO> result = gpuService.getGpuDevicesToAllocate(availableDevices, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    private List<GpuDeviceVO> createMockGpuDevicesWithNullValues(int count) {
        List<GpuDeviceVO> devices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GpuDeviceVO device = mock(GpuDeviceVO.class);
            when(device.getId()).thenReturn((long) (i + 1));
            when(device.getNumaNode()).thenReturn(i == 0 ? null : (i == 1 ? "" : NUMA_NODE_0));
            when(device.getPciRoot()).thenReturn(i == 0 ? null : (i == 1 ? "" : PCI_ROOT_1));
            devices.add(device);
        }
        return devices;
    }

    @Test
    public void testGetGpuDevicesToAllocate_SimpleDebug() {
        // Simple test with minimal setup
        GpuDeviceVO device1 = mock(GpuDeviceVO.class);
        when(device1.getId()).thenReturn(GPU_DEVICE_ID_LONG);
        when(device1.getNumaNode()).thenReturn(NUMA_NODE_0);
        when(device1.getPciRoot()).thenReturn(PCI_ROOT_1);

        GpuDeviceVO device2 = mock(GpuDeviceVO.class);
        when(device2.getId()).thenReturn(GPU_DEVICE_ID_2);
        when(device2.getNumaNode()).thenReturn(NUMA_NODE_0);
        when(device2.getPciRoot()).thenReturn(PCI_ROOT_1);

        List<GpuDeviceVO> availableDevices = Arrays.asList(device1, device2);

        List<GpuDeviceVO> result = gpuService.getGpuDevicesToAllocate(availableDevices, 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(GPU_DEVICE_ID_LONG.longValue(), result.get(0).getId());
    }

    // Tests for checkAndUpdateParentGpuDeviceState methods
    @Test
    public void testCheckAndUpdateParentGpuDeviceState_NullParentId() {
        // Should not throw exception and not call any DAO methods
        gpuService.checkAndUpdateParentGpuDeviceState((Long) null);

        verify(gpuDeviceDao, never()).findById(any());
        verify(gpuDeviceDao, never()).listByParentGpuDeviceId(any());
        verify(gpuDeviceDao, never()).update(anyLong(), any(GpuDeviceVO.class));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_ValidParentId() {
        Long parentDeviceId = PARENT_GPU_DEVICE_ID;
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(parentDeviceId);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.Free);

        when(gpuDeviceDao.findById(parentDeviceId)).thenReturn(parentDevice);
        when(gpuDeviceDao.listByParentGpuDeviceId(parentDeviceId)).thenReturn(new ArrayList<>());

        gpuService.checkAndUpdateParentGpuDeviceState(parentDeviceId);

        verify(gpuDeviceDao).findById(parentDeviceId);
        verify(gpuDeviceDao).listByParentGpuDeviceId(parentDeviceId);
        // Should not update since no child devices and already Free
        verify(gpuDeviceDao, never()).update(eq(parentDeviceId), any(GpuDeviceVO.class));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_NullParentDevice() {
        // Should not throw exception and not call any DAO methods
        gpuService.checkAndUpdateParentGpuDeviceState((GpuDeviceVO) null);

        verify(gpuDeviceDao, never()).listByParentGpuDeviceId(any());
        verify(gpuDeviceDao, never()).update(anyLong(), any(GpuDeviceVO.class));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_NoChildDevices() {
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(PARENT_GPU_DEVICE_ID);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.PartiallyAllocated);

        when(gpuDeviceDao.listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID)).thenReturn(new ArrayList<>());
        when(gpuDeviceDao.update(eq(PARENT_GPU_DEVICE_ID), any(GpuDeviceVO.class))).thenReturn(true);

        gpuService.checkAndUpdateParentGpuDeviceState(parentDevice);

        verify(gpuDeviceDao).listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID);
        verify(parentDevice).setState(GpuDevice.State.Free);
        verify(gpuDeviceDao).update(eq(PARENT_GPU_DEVICE_ID), eq(parentDevice));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_AllFreeChildDevices() {
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(PARENT_GPU_DEVICE_ID);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.PartiallyAllocated);

        // Create child devices all in Free state
        List<GpuDeviceVO> childDevices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            GpuDeviceVO child = mock(GpuDeviceVO.class);
            when(child.getState()).thenReturn(GpuDevice.State.Free);
            childDevices.add(child);
        }

        when(gpuDeviceDao.listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID)).thenReturn(childDevices);
        when(gpuDeviceDao.update(eq(PARENT_GPU_DEVICE_ID), any(GpuDeviceVO.class))).thenReturn(true);

        gpuService.checkAndUpdateParentGpuDeviceState(parentDevice);

        verify(gpuDeviceDao).listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID);
        verify(parentDevice).setState(GpuDevice.State.Free);
        verify(gpuDeviceDao).update(eq(PARENT_GPU_DEVICE_ID), eq(parentDevice));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_SomeAllocatedChildDevices() {
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(PARENT_GPU_DEVICE_ID);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.Free);

        // Create child devices with mixed states - some Free, some Allocated
        List<GpuDeviceVO> childDevices = new ArrayList<>();

        GpuDeviceVO child1 = mock(GpuDeviceVO.class);
        when(child1.getState()).thenReturn(GpuDevice.State.Free);
        childDevices.add(child1);

        GpuDeviceVO child2 = mock(GpuDeviceVO.class);
        when(child2.getState()).thenReturn(GpuDevice.State.Allocated);
        childDevices.add(child2);

        GpuDeviceVO child3 = mock(GpuDeviceVO.class);
        when(child3.getState()).thenReturn(GpuDevice.State.Free);
        childDevices.add(child3);

        when(gpuDeviceDao.listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID)).thenReturn(childDevices);
        when(gpuDeviceDao.update(eq(PARENT_GPU_DEVICE_ID), any(GpuDeviceVO.class))).thenReturn(true);

        gpuService.checkAndUpdateParentGpuDeviceState(parentDevice);

        verify(gpuDeviceDao).listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID);
        verify(parentDevice).setState(GpuDevice.State.PartiallyAllocated);
        verify(gpuDeviceDao).update(eq(PARENT_GPU_DEVICE_ID), eq(parentDevice));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_ErrorChildDevices() {
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(PARENT_GPU_DEVICE_ID);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.Free);

        // Create child devices with mixed states - Error should take priority
        List<GpuDeviceVO> childDevices = new ArrayList<>();

        GpuDeviceVO child1 = mock(GpuDeviceVO.class);
        when(child1.getState()).thenReturn(GpuDevice.State.Free);
        childDevices.add(child1);

        GpuDeviceVO child2 = mock(GpuDeviceVO.class);
        when(child2.getState()).thenReturn(GpuDevice.State.Allocated);
        childDevices.add(child2);

        GpuDeviceVO child3 = mock(GpuDeviceVO.class);
        when(child3.getState()).thenReturn(GpuDevice.State.Error);
        childDevices.add(child3);

        when(gpuDeviceDao.listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID)).thenReturn(childDevices);
        when(gpuDeviceDao.update(eq(PARENT_GPU_DEVICE_ID), any(GpuDeviceVO.class))).thenReturn(true);

        gpuService.checkAndUpdateParentGpuDeviceState(parentDevice);

        verify(gpuDeviceDao).listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID);
        verify(parentDevice).setState(GpuDevice.State.Error);
        verify(gpuDeviceDao).update(eq(PARENT_GPU_DEVICE_ID), eq(parentDevice));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_AllAllocatedChildDevices() {
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(PARENT_GPU_DEVICE_ID);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.Free);

        // Create child devices all in Allocated state
        List<GpuDeviceVO> childDevices = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            GpuDeviceVO child = mock(GpuDeviceVO.class);
            when(child.getState()).thenReturn(GpuDevice.State.Allocated);
            childDevices.add(child);
        }

        when(gpuDeviceDao.listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID)).thenReturn(childDevices);
        when(gpuDeviceDao.update(eq(PARENT_GPU_DEVICE_ID), any(GpuDeviceVO.class))).thenReturn(true);

        gpuService.checkAndUpdateParentGpuDeviceState(parentDevice);

        verify(gpuDeviceDao).listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID);
        verify(parentDevice).setState(GpuDevice.State.PartiallyAllocated);
        verify(gpuDeviceDao).update(eq(PARENT_GPU_DEVICE_ID), eq(parentDevice));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_NoStateChange() {
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(PARENT_GPU_DEVICE_ID);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.Free);

        // Create child devices all in Free state - should not change parent state
        List<GpuDeviceVO> childDevices = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            GpuDeviceVO child = mock(GpuDeviceVO.class);
            when(child.getState()).thenReturn(GpuDevice.State.Free);
            childDevices.add(child);
        }

        when(gpuDeviceDao.listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID)).thenReturn(childDevices);

        gpuService.checkAndUpdateParentGpuDeviceState(parentDevice);

        verify(gpuDeviceDao).listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID);
        verify(parentDevice, never()).setState(any());
        verify(gpuDeviceDao, never()).update(anyLong(), any(GpuDeviceVO.class));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_MultipleErrorStates() {
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(PARENT_GPU_DEVICE_ID);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.PartiallyAllocated);

        // Create child devices with multiple Error states
        List<GpuDeviceVO> childDevices = new ArrayList<>();

        GpuDeviceVO child1 = mock(GpuDeviceVO.class);
        when(child1.getState()).thenReturn(GpuDevice.State.Error);
        childDevices.add(child1);

        GpuDeviceVO child2 = mock(GpuDeviceVO.class);
        childDevices.add(child2);

        when(gpuDeviceDao.listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID)).thenReturn(childDevices);
        when(gpuDeviceDao.update(eq(PARENT_GPU_DEVICE_ID), any(GpuDeviceVO.class))).thenReturn(true);

        gpuService.checkAndUpdateParentGpuDeviceState(parentDevice);

        verify(gpuDeviceDao).listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID);
        verify(parentDevice).setState(GpuDevice.State.Error);
        verify(gpuDeviceDao).update(eq(PARENT_GPU_DEVICE_ID), eq(parentDevice));
    }

    @Test
    public void testCheckAndUpdateParentGpuDeviceState_ErrorTakesPriority() {
        GpuDeviceVO parentDevice = mock(GpuDeviceVO.class);
        when(parentDevice.getId()).thenReturn(PARENT_GPU_DEVICE_ID);
        when(parentDevice.getState()).thenReturn(GpuDevice.State.Free);

        // Create child devices - should break on first Error state found
        List<GpuDeviceVO> childDevices = new ArrayList<>();

        GpuDeviceVO child1 = mock(GpuDeviceVO.class);
        when(child1.getState()).thenReturn(GpuDevice.State.Allocated);
        childDevices.add(child1);

        GpuDeviceVO child2 = mock(GpuDeviceVO.class);
        when(child2.getState()).thenReturn(GpuDevice.State.Error);
        childDevices.add(child2);

        // This child should not affect the final state since Error was found first
        GpuDeviceVO child3 = mock(GpuDeviceVO.class);
        childDevices.add(child3);

        when(gpuDeviceDao.listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID)).thenReturn(childDevices);
        when(gpuDeviceDao.update(eq(PARENT_GPU_DEVICE_ID), any(GpuDeviceVO.class))).thenReturn(true);

        gpuService.checkAndUpdateParentGpuDeviceState(parentDevice);

        verify(gpuDeviceDao).listByParentGpuDeviceId(PARENT_GPU_DEVICE_ID);
        verify(parentDevice).setState(GpuDevice.State.Error);
        verify(gpuDeviceDao).update(eq(PARENT_GPU_DEVICE_ID), eq(parentDevice));
    }
}
