//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtGetVolumeStatsCommandWrapperTest {

    private static final String POOL_UUID = "pool-uuid";
    private static final StoragePoolType POOL_TYPE = StoragePoolType.RBD;

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private KVMStoragePoolManager storagePoolMgr;
    @Mock
    private KVMStoragePool storagePool;

    private final LibvirtGetVolumeStatsCommandWrapper wrapper = new LibvirtGetVolumeStatsCommandWrapper();

    // No libvirt in unit tests: getConnection() returns null instead of loading the native library.
    private MockedStatic<LibvirtConnection> libvirtConnection;

    @Before
    public void setUp() {
        libvirtConnection = Mockito.mockStatic(LibvirtConnection.class);
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
    }

    @After
    public void tearDown() {
        libvirtConnection.close();
    }

    private KVMPhysicalDisk disk(long size, long virtualSize) {
        KVMPhysicalDisk disk = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(disk.getSize()).thenReturn(size);
        Mockito.when(disk.getVirtualSize()).thenReturn(virtualSize);
        return disk;
    }

    private Map<String, VolumeStatsEntry> execute(String... volumeUuids) {
        GetVolumeStatsCommand cmd = new GetVolumeStatsCommand(POOL_TYPE, POOL_UUID, Arrays.asList(volumeUuids));
        GetVolumeStatsAnswer answer = (GetVolumeStatsAnswer) wrapper.execute(cmd, libvirtComputingResource);
        return answer.getVolumeStats();
    }

    @Test
    public void testPoolIsLookedUpOnceForAllVolumes() {
        Mockito.when(storagePoolMgr.getStoragePool(POOL_TYPE, POOL_UUID)).thenReturn(storagePool);
        KVMPhysicalDisk disk = disk(10L, 20L);
        Mockito.when(storagePool.getPhysicalDisk(Mockito.anyString())).thenReturn(disk);

        Map<String, VolumeStatsEntry> stats = execute("vol-1", "vol-2", "vol-3");

        Assert.assertEquals(3, stats.size());
        Mockito.verify(storagePoolMgr, Mockito.times(1)).getStoragePool(POOL_TYPE, POOL_UUID);
    }

    @Test
    public void testVolumeThatCannotBeResolvedDoesNotDropTheOthers() {
        Mockito.when(storagePoolMgr.getStoragePool(POOL_TYPE, POOL_UUID)).thenReturn(storagePool);
        KVMPhysicalDisk disk = disk(10L, 20L);
        Mockito.when(storagePool.getPhysicalDisk("vol-ok")).thenReturn(disk);
        Mockito.when(storagePool.getPhysicalDisk("vol-missing")).thenReturn(null);
        Mockito.when(storagePool.getPhysicalDisk("vol-error")).thenThrow(new CloudRuntimeException("image not found"));

        Map<String, VolumeStatsEntry> stats = execute("vol-missing", "vol-ok", "vol-error");

        Assert.assertEquals(1, stats.size());
        VolumeStatsEntry entry = stats.get("vol-ok");
        Assert.assertEquals(10L, entry.getPhysicalSize());
        Assert.assertEquals(20L, entry.getVirtualSize());
    }

    @Test
    public void testUnavailablePoolReturnsNoStats() {
        Mockito.when(storagePoolMgr.getStoragePool(POOL_TYPE, POOL_UUID)).thenReturn(null);

        Assert.assertNull(execute("vol-1", "vol-2"));
        Mockito.verifyNoInteractions(storagePool);
    }

    @Test
    public void testPoolLookupErrorReturnsNoStats() {
        Mockito.when(storagePoolMgr.getStoragePool(POOL_TYPE, POOL_UUID)).thenThrow(new CloudRuntimeException("pool not found"));

        Assert.assertNull(execute("vol-1"));
    }

    @Test
    public void testNoStatsWhenNoVolumeCanBeResolved() {
        Mockito.when(storagePoolMgr.getStoragePool(POOL_TYPE, POOL_UUID)).thenReturn(storagePool);
        Mockito.when(storagePool.getPhysicalDisk(Mockito.anyString())).thenReturn(null);

        Assert.assertNull(execute("vol-1", "vol-2"));
    }
}
