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
package com.cloud.hypervisor.kvm.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRc;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.AutoPlaceRequest;
import com.linbit.linstor.api.model.ControllerVersion;
import com.linbit.linstor.api.model.DrbdResource;
import com.linbit.linstor.api.model.LayerType;
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceDefinitionCreate;
import com.linbit.linstor.api.model.ResourceLayer;
import com.linbit.linstor.api.model.ResourceMakeAvailable;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.StorageResource;
import com.linbit.linstor.api.model.Volume;
import com.linbit.linstor.api.model.VolumeDefinitionCreate;

@RunWith(MockitoJUnitRunner.class)
public class LinstorStorageAdaptorTest {

    private static final String DEVICE = "/dev/drbd/by-res/cs-vol-1/0";

    private LinstorStorageAdaptor adaptor;
    private LinstorStoragePool pool;
    private DevelopersApi api;
    private String localNode;

    @Before
    public void setUp() {
        adaptor = spy(new LinstorStorageAdaptor());
        pool = new LinstorStoragePool("pool-uuid", "http://controller:3370", 3370, "cloudstack",
                Storage.StoragePoolType.Linstor, adaptor, false);
        api = mock(DevelopersApi.class);
        doReturn(api).when(adaptor).getLinstorAPI(any());
        doNothing().when(adaptor).waitForDevice(anyString());
        localNode = LinstorStoragePool.getHostname();
    }

    private static ApiCallRcList answers(long retCode, String message) {
        ApiCallRc rc = new ApiCallRc();
        rc.setRetCode(retCode);
        rc.setMessage(message);
        ApiCallRcList list = new ApiCallRcList();
        list.add(rc);
        return list;
    }

    private static ApiCallRcList success() {
        return answers(ApiConsts.MASK_SUCCESS, "ok");
    }

    private static ApiCallRcList error(String message) {
        return answers(ApiConsts.MASK_ERROR, message);
    }

    private void mockDefinitionsCreated() throws ApiException {
        when(api.resourceDefinitionCreate(any())).thenReturn(success());
        when(api.volumeDefinitionCreate(anyString(), any())).thenReturn(success());
    }

    private void mockControllerVersion(String restApiVersion) throws ApiException {
        ControllerVersion version = new ControllerVersion();
        version.setRestApiVersion(restApiVersion);
        when(api.controllerVersion()).thenReturn(version);
    }

    private static ResourceWithVolumes drbdResource() {
        ResourceLayer layer = new ResourceLayer();
        layer.setDrbd(new DrbdResource());
        Volume vlm = new Volume();
        vlm.setVolumeNumber(0);
        vlm.setDevicePath("/dev/drbd1000");
        vlm.setAllocatedSizeKib(0L);
        ResourceWithVolumes rsc = new ResourceWithVolumes();
        rsc.setName("cs-vol-1");
        rsc.setLayerObject(layer);
        rsc.setVolumes(Collections.singletonList(vlm));
        return rsc;
    }

    /** How the controller reports a failed request: an HTTP error carrying the answers in its body. */
    private static ApiException httpError(String message) {
        return new ApiException(500, Collections.emptyMap(),
                String.format("[{\"ret_code\": %d, \"message\": \"%s\"}]", ApiConsts.MASK_ERROR, message));
    }

    private static Volume volume(int number, String devicePath) {
        Volume vlm = new Volume();
        vlm.setVolumeNumber(number);
        vlm.setDevicePath(devicePath);
        return vlm;
    }

    private void mockLocalResource(boolean drbd, Volume... volumes) throws ApiException {
        ResourceLayer layer = new ResourceLayer();
        if (drbd) {
            layer.setDrbd(new DrbdResource());
        } else {
            layer.setStorage(new StorageResource());
        }
        ResourceWithVolumes rsc = new ResourceWithVolumes();
        rsc.setLayerObject(layer);
        rsc.setVolumes(Arrays.asList(volumes));
        when(api.viewResources(eq(Collections.singletonList(localNode)), any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(rsc));
    }

    @Test
    public void createBackupScratchPlacesStorageOnlyResourceLocally() throws ApiException {
        mockDefinitionsCreated();
        when(api.resourceAutoplace(anyString(), any())).thenReturn(success());
        mockLocalResource(false, volume(1, "/dev/storage/scratch_00001"), volume(0, "/dev/storage/scratch_00000"));

        Pair<String, List<String>> scratch = adaptor.createBackupScratch(pool, "i-2-3-VM", Arrays.asList(1025L, 4096L));

        Assert.assertTrue(scratch.first().startsWith(LinstorStorageAdaptor.BACKUP_SCRATCH_PREFIX));
        Assert.assertTrue(scratch.first().length() <= 48);
        Assert.assertEquals(Arrays.asList("/dev/storage/scratch_00000", "/dev/storage/scratch_00001"), scratch.second());

        ArgumentCaptor<ResourceDefinitionCreate> rdCreate = ArgumentCaptor.forClass(ResourceDefinitionCreate.class);
        verify(api).resourceDefinitionCreate(rdCreate.capture());
        ResourceDefinition rd = rdCreate.getValue().getResourceDefinition();
        Assert.assertEquals(scratch.first(), rd.getName());
        Assert.assertEquals("cloudstack", rd.getResourceGroupName());
        Assert.assertEquals("i-2-3-VM", rd.getProps().get(LinstorStorageAdaptor.BACKUP_SCRATCH_VM_PROP));

        // One volume per disk, sizes rounded up to KiB.
        ArgumentCaptor<VolumeDefinitionCreate> vdCreate = ArgumentCaptor.forClass(VolumeDefinitionCreate.class);
        verify(api, times(2)).volumeDefinitionCreate(eq(scratch.first()), vdCreate.capture());
        Assert.assertEquals(Integer.valueOf(0), vdCreate.getAllValues().get(0).getVolumeDefinition().getVolumeNumber());
        Assert.assertEquals(Long.valueOf(2L), vdCreate.getAllValues().get(0).getVolumeDefinition().getSizeKib());
        Assert.assertEquals(Integer.valueOf(1), vdCreate.getAllValues().get(1).getVolumeDefinition().getVolumeNumber());
        Assert.assertEquals(Long.valueOf(4L), vdCreate.getAllValues().get(1).getVolumeDefinition().getSizeKib());

        ArgumentCaptor<AutoPlaceRequest> placement = ArgumentCaptor.forClass(AutoPlaceRequest.class);
        verify(api).resourceAutoplace(eq(scratch.first()), placement.capture());
        Assert.assertEquals(Collections.singletonList(localNode), placement.getValue().getSelectFilter().getNodeNameList());
        Assert.assertEquals(Collections.singletonList(LayerType.STORAGE), placement.getValue().getLayerList());
        Assert.assertEquals(Integer.valueOf(1), placement.getValue().getSelectFilter().getPlaceCount());
        verify(api, never()).resourceMakeAvailableOnNode(anyString(), anyString(), any());
    }

    @Test
    public void createBackupScratchAttachesRemoteReplicaDisklessWithoutLocalStoragePool() throws ApiException {
        mockDefinitionsCreated();
        when(api.resourceAutoplace(anyString(), any()))
                .thenThrow(httpError("Not enough available nodes"))
                .thenReturn(success());
        when(api.resourceMakeAvailableOnNode(anyString(), eq(localNode), any())).thenReturn(success());
        mockLocalResource(true, volume(0, "/dev/drbd1000"), volume(1, "/dev/drbd1001"));

        Pair<String, List<String>> scratch = adaptor.createBackupScratch(pool, "i-2-3-VM", Arrays.asList(1024L, 1024L));

        String rscName = scratch.first();
        Assert.assertEquals(Arrays.asList("/dev/drbd/by-res/" + rscName + "/0", "/dev/drbd/by-res/" + rscName + "/1"),
                scratch.second());

        ArgumentCaptor<AutoPlaceRequest> placement = ArgumentCaptor.forClass(AutoPlaceRequest.class);
        verify(api, times(2)).resourceAutoplace(eq(rscName), placement.capture());
        AutoPlaceRequest remote = placement.getAllValues().get(1);
        Assert.assertEquals(Arrays.asList(LayerType.DRBD, LayerType.STORAGE), remote.getLayerList());
        Assert.assertTrue(remote.getSelectFilter().getNodeNameList().isEmpty());
        Assert.assertEquals(Integer.valueOf(1), remote.getSelectFilter().getPlaceCount());

        ArgumentCaptor<ResourceMakeAvailable> available = ArgumentCaptor.forClass(ResourceMakeAvailable.class);
        verify(api).resourceMakeAvailableOnNode(eq(rscName), eq(localNode), available.capture());
        Assert.assertFalse(available.getValue().isDiskful());
        verify(api, never()).resourceDefinitionDelete(anyString());
    }

    @Test
    public void createBackupScratchDeletesResourceDefinitionWhenItCannotBePlaced() throws ApiException {
        mockDefinitionsCreated();
        when(api.resourceAutoplace(anyString(), any()))
                .thenReturn(error("Not enough available nodes"))
                .thenThrow(httpError("No storage pool with enough space"));
        when(api.resourceDefinitionDelete(anyString())).thenReturn(success());

        try {
            adaptor.createBackupScratch(pool, "i-2-3-VM", Collections.singletonList(1024L));
            Assert.fail("expected CloudRuntimeException");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("No storage pool with enough space"));
        }

        ArgumentCaptor<ResourceDefinitionCreate> rdCreate = ArgumentCaptor.forClass(ResourceDefinitionCreate.class);
        verify(api).resourceDefinitionCreate(rdCreate.capture());
        verify(api).resourceDefinitionDelete(rdCreate.getValue().getResourceDefinition().getName());
    }

    @Test(expected = CloudRuntimeException.class)
    public void deleteBackupScratchFailsWhenStillInUse() throws ApiException {
        when(api.resourceDefinitionDelete("cs-bkscratch-1")).thenReturn(error("Resource is still in use"));

        adaptor.deleteBackupScratch(pool, "cs-bkscratch-1");
    }

    @Test
    public void connectPhysicalDiskWaitsForTheLocalDevice() throws ApiException {
        mockControllerVersion("1.29.0");
        when(api.resourceMakeAvailableOnNode(eq("cs-vol-1"), eq(localNode), any())).thenReturn(success());
        when(api.viewResources(Collections.singletonList(localNode), Collections.singletonList("cs-vol-1"),
                Collections.emptyList(), null, null, null)).thenReturn(Collections.singletonList(drbdResource()));

        Assert.assertTrue(adaptor.connectPhysicalDisk("vol-1", pool, null, false));

        InOrder inOrder = Mockito.inOrder(api, adaptor);
        inOrder.verify(api).resourceMakeAvailableOnNode(eq("cs-vol-1"), eq(localNode), any());
        inOrder.verify(adaptor).waitForDevice(DEVICE);
    }

    @Test
    public void createPhysicalDiskWaitsForTheLocalDevice() throws ApiException {
        when(api.resourceDefinitionList(null, false, null, null, null)).thenReturn(Collections.emptyList());
        when(api.resourceGroupSpawn(eq("cloudstack"), any())).thenReturn(success());
        when(api.viewResources(any(), eq(Collections.singletonList("cs-vol-1")), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(drbdResource()));
        when(api.resourceMakeAvailableOnNode(eq("cs-vol-1"), eq(localNode), any())).thenReturn(success());

        KVMPhysicalDisk disk = adaptor.createPhysicalDisk("vol-1", pool, QemuImg.PhysicalDiskFormat.RAW,
                Storage.ProvisioningType.THIN, 1024L * 1024, null);

        Assert.assertEquals(DEVICE, disk.getPath());
        InOrder inOrder = Mockito.inOrder(api, adaptor);
        inOrder.verify(api).resourceMakeAvailableOnNode(eq("cs-vol-1"), eq(localNode), any());
        inOrder.verify(adaptor).waitForDevice(DEVICE);
    }

    @Test(expected = CloudRuntimeException.class)
    public void connectPhysicalDiskFailsWithoutLocalVolume() throws ApiException {
        mockControllerVersion("1.29.0");
        when(api.resourceMakeAvailableOnNode(eq("cs-vol-1"), eq(localNode), any())).thenReturn(success());
        when(api.viewResources(Collections.singletonList(localNode), Collections.singletonList("cs-vol-1"),
                Collections.emptyList(), null, null, null)).thenReturn(Collections.emptyList());

        adaptor.connectPhysicalDisk("vol-1", pool, null, false);
    }

    @Test
    public void waitForDeviceReturnsForAnExistingDevice() {
        new LinstorStorageAdaptor().waitForDevice("/dev/null");
    }

    private static ResourceDefinition scratchRd(String name, String vmName) {
        ResourceDefinition rd = new ResourceDefinition();
        rd.setName(name);
        Properties props = new Properties();
        props.put(LinstorStorageAdaptor.BACKUP_SCRATCH_VM_PROP, vmName);
        rd.setProps(props);
        return rd;
    }

    private static ResourceWithVolumes resourceOf(String name) {
        ResourceWithVolumes rsc = new ResourceWithVolumes();
        rsc.setName(name);
        return rsc;
    }

    // The controllers this agent has swept are remembered per process, so every test uses its own one.
    private KVMStoragePool registerPool(String controller) {
        return adaptor.createStoragePool("pool-" + controller, controller, 3370, "cloudstack", null,
                Storage.StoragePoolType.Linstor, null, true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createStoragePoolDeletesLeftoverScratchOfThisNode() throws ApiException {
        ResourceDefinition volume = new ResourceDefinition();
        volume.setName("cs-vol-1");
        when(api.resourceDefinitionList(null, false, null, null, null)).thenReturn(Arrays.asList(
                scratchRd("cs-bkscratch-here", "i-2-3-VM"), scratchRd("cs-bkscratch-elsewhere", "i-2-4-VM"), volume));
        ArgumentCaptor<List<String>> rscNames = ArgumentCaptor.forClass(List.class);
        when(api.viewResources(eq(Collections.singletonList(localNode)), rscNames.capture(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(resourceOf("cs-bkscratch-here")));
        when(api.resourceDefinitionDelete("cs-bkscratch-here")).thenReturn(success());

        Assert.assertNotNull(registerPool("http://sweep-1:3370"));

        Assert.assertEquals(2, rscNames.getValue().size());
        Assert.assertFalse(rscNames.getValue().contains("cs-vol-1"));
        verify(api).resourceDefinitionDelete("cs-bkscratch-here");
        verify(api, never()).resourceDefinitionDelete("cs-bkscratch-elsewhere");
    }

    @Test
    public void createStoragePoolSweepsEachControllerOnlyOnce() throws ApiException {
        when(api.resourceDefinitionList(null, false, null, null, null)).thenReturn(Collections.emptyList());

        registerPool("http://sweep-2:3370");
        registerPool("http://sweep-2:3370");

        verify(api, times(1)).resourceDefinitionList(null, false, null, null, null);
    }

    @Test
    public void createStoragePoolRegistersPoolWhenControllerIsUnreachable() throws ApiException {
        when(api.resourceDefinitionList(null, false, null, null, null)).thenThrow(new ApiException("connection refused"));

        KVMStoragePool registered = registerPool("http://sweep-3:3370");

        Assert.assertSame(registered, adaptor.getStoragePool("pool-http://sweep-3:3370"));
        verify(api, never()).resourceDefinitionDelete(anyString());
    }
}
