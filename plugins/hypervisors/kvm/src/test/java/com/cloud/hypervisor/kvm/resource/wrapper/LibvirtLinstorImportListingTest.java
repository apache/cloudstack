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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.Arrays;
import java.util.Collections;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVolumesOnStorageAnswer;
import com.cloud.agent.api.GetVolumesOnStorageCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LibvirtLinstorImportListingTest {
    private KVMStoragePool pool;
    private KVMStoragePoolManager manager;
    private LibvirtComputingResource resource;
    private StorageFilerTO filer;
    private final LibvirtGetVolumesOnStorageCommandWrapper wrapper = new LibvirtGetVolumesOnStorageCommandWrapper();

    @Before
    public void setup() {
        pool = mock(KVMStoragePool.class);
        manager = mock(KVMStoragePoolManager.class);
        resource = mock(LibvirtComputingResource.class);
        filer = mock(StorageFilerTO.class);
        when(filer.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        when(filer.getUuid()).thenReturn("pool");
        when(resource.getStoragePoolMgr()).thenReturn(manager);
        when(manager.getStoragePool(Storage.StoragePoolType.Linstor, "pool", true, true)).thenReturn(pool);
    }

    private VolumeOnStorageTO volume(String name, boolean locked) {
        VolumeOnStorageTO volume = new VolumeOnStorageTO(null, name, name, "/dev/drbd/by-res/cs-" + name + "/0", "RAW", 1024, 1024);
        volume.addDetail(VolumeOnStorageTO.Detail.IS_LOCKED, Boolean.toString(locked));
        return volume;
    }

    private void verifyNoStorageMutation() {
        verify(manager, never()).connectPhysicalDisk(any(), any(), any(), any());
        verify(manager, never()).disconnectPhysicalDisk(any(), any(), any());
        verify(pool, never()).getPhysicalDisk(any());
        verify(pool, never()).listPhysicalDisks();
    }

    @Test
    public void listingUsesMetadataWithoutQemuOrConnectingAndKeepsKeyword() {
        when(pool.getVolumesForImport(null)).thenReturn(Arrays.asList(volume("alpha", false), volume("beta", true)));
        try (MockedConstruction<QemuImg> qemu = mockConstruction(QemuImg.class)) {
            GetVolumesOnStorageAnswer answer = (GetVolumesOnStorageAnswer) wrapper.execute(
                    new GetVolumesOnStorageCommand(filer, null, "beta"), resource);
            assertTrue(answer.getResult());
            assertEquals(1, answer.getVolumes().size());
            assertEquals("beta", answer.getVolumes().get(0).getName());
            assertEquals("true", answer.getVolumes().get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
            assertTrue(qemu.constructed().isEmpty());
        }
        verifyNoStorageMutation();
    }

    @Test
    public void singlePathAlsoUsesReadOnlyMetadata() {
        when(pool.getVolumesForImport("alpha")).thenReturn(Collections.singletonList(volume("alpha", false)));
        try (MockedConstruction<QemuImg> qemu = mockConstruction(QemuImg.class)) {
            GetVolumesOnStorageAnswer answer = (GetVolumesOnStorageAnswer) wrapper.execute(
                    new GetVolumesOnStorageCommand(filer, "alpha", null), resource);
            assertTrue(answer.getResult());
            assertEquals("alpha", answer.getVolumes().get(0).getPath());
            assertTrue(qemu.constructed().isEmpty());
        }
        verifyNoStorageMutation();
    }

    @Test
    public void wrongGroupDoesNotConnectOrDisconnect() {
        when(pool.getVolumesForImport("alpha")).thenThrow(new CloudRuntimeException("resource group mismatch"));
        Answer answer = wrapper.execute(new GetVolumesOnStorageCommand(filer, "alpha", null), resource);
        assertFalse(answer.getResult());
        assertEquals("resource group mismatch", answer.getDetails());
        verifyNoStorageMutation();
    }

    @Test
    public void controllerFailureDoesNotReturnSuccessfulEmptyList() {
        when(pool.getVolumesForImport(null)).thenThrow(new CloudRuntimeException("controller unavailable"));
        Answer answer = wrapper.execute(new GetVolumesOnStorageCommand(filer, null, null), resource);
        assertFalse(answer.getResult());
        assertEquals("controller unavailable", answer.getDetails());
        verifyNoStorageMutation();
    }
}
