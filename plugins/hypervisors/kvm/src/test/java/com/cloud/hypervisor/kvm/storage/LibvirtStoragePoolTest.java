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

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.StoragePool;
import org.mockito.Mockito;

import com.cloud.storage.Storage.StoragePoolType;

import junit.framework.TestCase;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStoragePoolTest extends TestCase {

    @Test
    public void testAttributes() {
        String uuid = "4c4fb08b-373e-4f30-a120-3aa3a43f31da";
        String name = "myfirstpool";

        StoragePoolType type = StoragePoolType.NetworkFilesystem;

        StorageAdaptor adapter = Mockito.mock(LibvirtStorageAdaptor.class);
        StoragePool storage = Mockito.mock(StoragePool.class);

        LibvirtStoragePool pool = new LibvirtStoragePool(uuid, name, type, adapter, storage);
        assertEquals(pool.getCapacity(), 0);
        assertEquals(pool.getUsed(), 0);
        assertEquals(pool.getName(), name);
        assertEquals(pool.getUuid(), uuid);
        assertEquals(pool.getAvailable(), 0);
        assertEquals(pool.getStoragePoolType(), type);

        pool.setCapacity(2048);
        pool.setUsed(1024);
        pool.setAvailable(1023);

        assertEquals(pool.getCapacity(), 2048);
        assertEquals(pool.getUsed(), 1024);
        assertEquals(pool.getAvailable(), 1023);
    }

    @Test
    public void testDefaultFormats() {
        String uuid = "f40cbf53-1f37-4c62-8912-801edf398f47";
        String name = "myfirstpool";

        StorageAdaptor adapter = Mockito.mock(LibvirtStorageAdaptor.class);
        StoragePool storage = Mockito.mock(StoragePool.class);

        LibvirtStoragePool nfsPool = new LibvirtStoragePool(uuid, name, StoragePoolType.NetworkFilesystem, adapter, storage);
        assertEquals(nfsPool.getDefaultFormat(), PhysicalDiskFormat.QCOW2);
        assertEquals(nfsPool.getStoragePoolType(), StoragePoolType.NetworkFilesystem);

        LibvirtStoragePool rbdPool = new LibvirtStoragePool(uuid, name, StoragePoolType.RBD, adapter, storage);
        assertEquals(rbdPool.getDefaultFormat(), PhysicalDiskFormat.RAW);
        assertEquals(rbdPool.getStoragePoolType(), StoragePoolType.RBD);

        LibvirtStoragePool clvmPool = new LibvirtStoragePool(uuid, name, StoragePoolType.CLVM, adapter, storage);
        assertEquals(clvmPool.getDefaultFormat(), PhysicalDiskFormat.RAW);
        assertEquals(clvmPool.getStoragePoolType(), StoragePoolType.CLVM);
    }

    @Test
    public void testExternalSnapshot() {
        String uuid = "60b46738-c5d0-40a9-a79e-9a4fe6295db7";
        String name = "myfirstpool";

        StorageAdaptor adapter = Mockito.mock(LibvirtStorageAdaptor.class);
        StoragePool storage = Mockito.mock(StoragePool.class);

        LibvirtStoragePool nfsPool = new LibvirtStoragePool(uuid, name, StoragePoolType.NetworkFilesystem, adapter, storage);
        if (nfsPool.getType() != StoragePoolType.NetworkFilesystem) {
            System.out.println("tested");
        }
        assertFalse(nfsPool.isExternalSnapshot());

        LibvirtStoragePool rbdPool = new LibvirtStoragePool(uuid, name, StoragePoolType.RBD, adapter, storage);
        assertTrue(rbdPool.isExternalSnapshot());

        LibvirtStoragePool clvmPool = new LibvirtStoragePool(uuid, name, StoragePoolType.CLVM, adapter, storage);
        assertTrue(clvmPool.isExternalSnapshot());
    }

    @Test
    public void testIsPoolSupportHA() {
        String uuid = "0f7a58bd-1a85-4b1f-9f91-12f3d1ecf5a5";
        String name = "myfirstpool";

        StorageAdaptor adapter = Mockito.mock(LibvirtStorageAdaptor.class);
        StoragePool storage = Mockito.mock(StoragePool.class);

        // NetworkFilesystem, SharedMountPoint and RBD all support the KVM Host-HA
        // heartbeat/VM-activity check mechanism.
        assertTrue(new LibvirtStoragePool(uuid, name, StoragePoolType.NetworkFilesystem, adapter, storage).isPoolSupportHA());
        assertTrue(new LibvirtStoragePool(uuid, name, StoragePoolType.SharedMountPoint, adapter, storage).isPoolSupportHA());
        assertTrue(new LibvirtStoragePool(uuid, name, StoragePoolType.RBD, adapter, storage).isPoolSupportHA());

        // Other pool types have no HA support.
        assertFalse(new LibvirtStoragePool(uuid, name, StoragePoolType.CLVM, adapter, storage).isPoolSupportHA());
        assertFalse(new LibvirtStoragePool(uuid, name, StoragePoolType.Filesystem, adapter, storage).isPoolSupportHA());
    }

    private String getRbdMonitors(String sourceHost, int sourcePort) {
        LibvirtStoragePool pool = new LibvirtStoragePool("0f7a58bd-1a85-4b1f-9f91-12f3d1ecf5a5", "myfirstpool", StoragePoolType.RBD,
                Mockito.mock(LibvirtStorageAdaptor.class), Mockito.mock(StoragePool.class));
        pool.setSourceHost(sourceHost);
        pool.setSourcePort(sourcePort);
        return pool.getRbdMonitors();
    }

    @Test
    public void testRbdMonitorsWithoutPort() {
        assertEquals("10.0.0.1", getRbdMonitors("10.0.0.1", 0));
        assertEquals("10.0.0.1,10.0.0.2,10.0.0.3", getRbdMonitors("10.0.0.1,10.0.0.2,10.0.0.3", 0));
        assertEquals("fd00::1,fd00::2", getRbdMonitors("fd00::1,fd00::2", 0));
    }

    @Test
    public void testRbdMonitorsIpv4WithPort() {
        assertEquals("10.0.0.1:6789", getRbdMonitors("10.0.0.1", 6789));
        assertEquals("10.0.0.1:3300,10.0.0.2:3300,10.0.0.3:3300", getRbdMonitors("10.0.0.1,10.0.0.2,10.0.0.3", 3300));
    }

    @Test
    public void testRbdMonitorsIpv6WithPort() {
        assertEquals("[fd00::1]:3300", getRbdMonitors("fd00::1", 3300));
        assertEquals("[fd00::1]:3300,[fd00::2]:3300", getRbdMonitors("fd00::1,fd00::2", 3300));
        // already enclosed in square brackets
        assertEquals("[fd00::1]:3300,[fd00::2]:3300", getRbdMonitors("[fd00::1],[fd00::2]", 3300));
    }

    @Test
    public void testRbdMonitorsMixedIpv4AndIpv6WithPort() {
        assertEquals("10.0.0.1:3300,[fd00::1]:3300,[fd00::2]:3300,mon4.example.com:3300",
                getRbdMonitors("10.0.0.1, fd00::1,[fd00::2] ,mon4.example.com", 3300));
    }
}
