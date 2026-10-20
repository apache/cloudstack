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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRc;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.DrbdResource;
import com.linbit.linstor.api.model.ResourceLayer;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.Volume;

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

    private static ApiCallRcList success() {
        ApiCallRc rc = new ApiCallRc();
        rc.setRetCode(ApiConsts.MASK_SUCCESS);
        rc.setMessage("ok");
        ApiCallRcList list = new ApiCallRcList();
        list.add(rc);
        return list;
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

    @Test
    public void connectPhysicalDiskWaitsForTheLocalDevice() throws ApiException {
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
        when(api.resourceMakeAvailableOnNode(eq("cs-vol-1"), eq(localNode), any())).thenReturn(success());
        when(api.viewResources(Collections.singletonList(localNode), Collections.singletonList("cs-vol-1"),
                Collections.emptyList(), null, null, null)).thenReturn(Collections.emptyList());

        adaptor.connectPhysicalDisk("vol-1", pool, null, false);
    }

    @Test
    public void waitForDeviceReturnsForAnExistingDevice() {
        new LinstorStorageAdaptor().waitForDevice("/dev/null");
    }
}
