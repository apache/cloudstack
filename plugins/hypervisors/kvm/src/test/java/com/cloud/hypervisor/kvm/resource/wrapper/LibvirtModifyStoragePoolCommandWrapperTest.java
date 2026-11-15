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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage.StoragePoolType;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtModifyStoragePoolCommandWrapperTest {

    @Mock
    private LibvirtComputingResource libvirtComputingResource;

    @Mock
    private KVMStoragePoolManager storagePoolManager;

    @Mock
    private ModifyStoragePoolCommand command;

    @Mock
    private StorageFilerTO storageFilerTO;

    @Mock
    private KVMStoragePool storagePool;

    private LibvirtModifyStoragePoolCommandWrapper wrapper;

    @Before
    public void setUp() {
        wrapper = new LibvirtModifyStoragePoolCommandWrapper();
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
    }

    @Test
    public void testAddClvmStoragePoolWithoutDetails() {
        when(command.getAdd()).thenReturn(true);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(null);

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getHost()).thenReturn("192.168.1.100");
        when(storageFilerTO.getPort()).thenReturn(0);
        when(storageFilerTO.getPath()).thenReturn("/vg0");
        when(storageFilerTO.getUserInfo()).thenReturn(null);
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.CLVM);

        when(storagePool.getCapacity()).thenReturn(1000000L);
        when(storagePool.getAvailable()).thenReturn(500000L);
        when(storagePool.getDetails()).thenReturn(new HashMap<>());

        when(storagePoolManager.createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM), anyMap()))
            .thenReturn(storagePool);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue("Answer should indicate success. Details: " + answer.getDetails(), answer.getResult());
        assertTrue("Answer should be ModifyStoragePoolAnswer", answer instanceof ModifyStoragePoolAnswer);

        // Verify the details were passed correctly
        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(storagePoolManager).createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM), detailsCaptor.capture());

        Map<String, String> capturedDetails = detailsCaptor.getValue();
        assertNotNull("Details should not be null", capturedDetails);
        assertEquals("CLVM_SECURE_ZERO_FILL should default to false",
                "false", capturedDetails.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL));
    }

    @Test
    public void testAddClvmNgStoragePoolWithEmptyDetails() {
        when(command.getAdd()).thenReturn(true);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(new HashMap<>());

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getHost()).thenReturn("192.168.1.100");
        when(storageFilerTO.getPort()).thenReturn(0);
        when(storageFilerTO.getPath()).thenReturn("/vg0");
        when(storageFilerTO.getUserInfo()).thenReturn(null);
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.CLVM_NG);

        when(storagePool.getCapacity()).thenReturn(2000000L);
        when(storagePool.getAvailable()).thenReturn(1000000L);
        when(storagePool.getDetails()).thenReturn(new HashMap<>());

        when(storagePoolManager.createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM_NG), anyMap()))
            .thenReturn(storagePool);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue("Answer should indicate success", answer.getResult());

        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(storagePoolManager).createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM_NG), detailsCaptor.capture());

        Map<String, String> capturedDetails = detailsCaptor.getValue();
        assertNotNull("Details should not be null", capturedDetails);
        assertEquals("CLVM_SECURE_ZERO_FILL should default to false",
                "false", capturedDetails.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL));
    }

    @Test
    public void testAddClvmStoragePoolWithExistingSecureZeroFillSetting() {
        Map<String, String> details = new HashMap<>();
        details.put(KVMStoragePool.CLVM_SECURE_ZERO_FILL, "true");
        details.put("someOtherKey", "someValue");

        when(command.getAdd()).thenReturn(true);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(details);

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getHost()).thenReturn("192.168.1.100");
        when(storageFilerTO.getPort()).thenReturn(0);
        when(storageFilerTO.getPath()).thenReturn("/vg0");
        when(storageFilerTO.getUserInfo()).thenReturn(null);
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.CLVM);

        when(storagePool.getCapacity()).thenReturn(1000000L);
        when(storagePool.getAvailable()).thenReturn(500000L);
        when(storagePool.getDetails()).thenReturn(new HashMap<>());

        when(storagePoolManager.createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM), anyMap()))
            .thenReturn(storagePool);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue("Answer should indicate success", answer.getResult());

        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(storagePoolManager).createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM), detailsCaptor.capture());

        Map<String, String> capturedDetails = detailsCaptor.getValue();
        assertNotNull("Details should not be null", capturedDetails);
        assertEquals("CLVM_SECURE_ZERO_FILL should preserve existing value",
                "true", capturedDetails.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL));
        assertEquals("Other details should be preserved",
                "someValue", capturedDetails.get("someOtherKey"));
    }

    @Test
    public void testAddClvmStoragePoolPreservesOtherDetailsWhenAddingDefault() {
        Map<String, String> details = new HashMap<>();
        details.put("customKey1", "value1");
        details.put("customKey2", "value2");

        when(command.getAdd()).thenReturn(true);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(details);

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getHost()).thenReturn("192.168.1.100");
        when(storageFilerTO.getPort()).thenReturn(0);
        when(storageFilerTO.getPath()).thenReturn("/vg0");
        when(storageFilerTO.getUserInfo()).thenReturn(null);
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.CLVM_NG);

        when(storagePool.getCapacity()).thenReturn(1000000L);
        when(storagePool.getAvailable()).thenReturn(500000L);
        when(storagePool.getDetails()).thenReturn(new HashMap<>());

        when(storagePoolManager.createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM_NG), anyMap()))
            .thenReturn(storagePool);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue("Answer should indicate success", answer.getResult());

        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(storagePoolManager).createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM_NG), detailsCaptor.capture());

        Map<String, String> capturedDetails = detailsCaptor.getValue();
        assertNotNull("Details should not be null", capturedDetails);
        assertEquals("CLVM_SECURE_ZERO_FILL should default to false",
                "false", capturedDetails.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL));
        assertEquals("Custom details should be preserved",
                "value1", capturedDetails.get("customKey1"));
        assertEquals("Custom details should be preserved",
                "value2", capturedDetails.get("customKey2"));
    }

    @Test
    public void testDeleteClvmStoragePoolSuccess() {
        when(command.getAdd()).thenReturn(false);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(null);

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.CLVM);

        when(storagePoolManager.deleteStoragePool(StoragePoolType.CLVM, "pool-uuid", null))
            .thenReturn(true);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue("Answer should indicate success", answer.getResult());
        verify(storagePoolManager).deleteStoragePool(StoragePoolType.CLVM, "pool-uuid", null);
    }

    @Test
    public void testDeleteClvmNgStoragePoolSuccess() {
        when(command.getAdd()).thenReturn(false);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(null);

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.CLVM_NG);

        when(storagePoolManager.deleteStoragePool(StoragePoolType.CLVM_NG, "pool-uuid", null))
            .thenReturn(true);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue("Answer should indicate success", answer.getResult());
        verify(storagePoolManager).deleteStoragePool(StoragePoolType.CLVM_NG, "pool-uuid", null);
    }

    @Test
    public void testDeleteClvmStoragePoolFailure() {
        when(command.getAdd()).thenReturn(false);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(null);

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.CLVM);

        when(storagePoolManager.deleteStoragePool(StoragePoolType.CLVM, "pool-uuid", null))
            .thenReturn(false);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertFalse("Answer should indicate failure", answer.getResult());
        assertEquals("Failed to delete storage pool", answer.getDetails());
    }

    @Test
    public void testAddClvmStoragePoolCreationFailure() {
        when(command.getAdd()).thenReturn(true);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(null);

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getHost()).thenReturn("192.168.1.100");
        when(storageFilerTO.getPort()).thenReturn(0);
        when(storageFilerTO.getPath()).thenReturn("/vg0");
        when(storageFilerTO.getUserInfo()).thenReturn(null);
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.CLVM);

        when(storagePoolManager.createStoragePool(eq("pool-uuid"), eq("192.168.1.100"), eq(0),
                eq("/vg0"), eq(null), eq(StoragePoolType.CLVM), anyMap()))
            .thenReturn(null);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertFalse("Answer should indicate failure", answer.getResult());
        assertEquals(" Failed to create storage pool", answer.getDetails());
    }

    @Test
    public void testAddNfsStoragePoolDoesNotSetClvmSecureZeroFill() {
        when(command.getAdd()).thenReturn(true);
        when(command.getPool()).thenReturn(storageFilerTO);
        when(command.getDetails()).thenReturn(null);

        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(storageFilerTO.getHost()).thenReturn("nfs.server.com");
        when(storageFilerTO.getPort()).thenReturn(0);
        when(storageFilerTO.getPath()).thenReturn("/export/nfs");
        when(storageFilerTO.getUserInfo()).thenReturn(null);
        when(storageFilerTO.getType()).thenReturn(StoragePoolType.NetworkFilesystem);

        when(storagePool.getCapacity()).thenReturn(1000000L);
        when(storagePool.getAvailable()).thenReturn(500000L);
        when(storagePool.getDetails()).thenReturn(new HashMap<>());

        when(storagePoolManager.createStoragePool(eq("pool-uuid"), eq("nfs.server.com"), eq(0),
                eq("/export/nfs"), eq(null), eq(StoragePoolType.NetworkFilesystem), anyMap()))
            .thenReturn(storagePool);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue("Answer should indicate success", answer.getResult());

        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(storagePoolManager).createStoragePool(eq("pool-uuid"), eq("nfs.server.com"), eq(0),
                eq("/export/nfs"), eq(null), eq(StoragePoolType.NetworkFilesystem), detailsCaptor.capture());

        Map<String, String> capturedDetails = detailsCaptor.getValue();
        assertNotNull("Details should not be null", capturedDetails);
        assertEquals("CLVM_SECURE_ZERO_FILL gets added for all pools",
                "false", capturedDetails.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL));
    }
}
