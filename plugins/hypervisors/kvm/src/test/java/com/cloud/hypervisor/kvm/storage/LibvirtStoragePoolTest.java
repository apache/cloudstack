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

import java.io.File;
import java.nio.file.Files;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.StoragePool;
import org.mockito.Mockito;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

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

    private LibvirtStoragePool getRbdPool(String authUsername, String authSecret) {
        LibvirtStoragePool pool = new LibvirtStoragePool("0f7a58bd-1a85-4b1f-9f91-12f3d1ecf5a5", "myfirstpool", StoragePoolType.RBD,
                Mockito.mock(LibvirtStorageAdaptor.class), Mockito.mock(StoragePool.class));
        pool.setSourceHost("10.0.0.1,fd00::1");
        pool.setSourcePort(6789);
        pool.setSourceDir("rbdpool");
        pool.setAuthUsername(authUsername);
        pool.setAuthSecret(authSecret);
        return pool;
    }

    @Test
    public void testAddRbdConnectionArgsWithCephx() {
        Script cmd = Mockito.mock(Script.class);
        getRbdPool("cephuser", "cephkey").addRbdConnectionArgs(cmd);
        Mockito.verify(cmd).add("-s", "10.0.0.1:6789,[fd00::1]:6789");
        Mockito.verify(cmd).add("-o", "rbdpool");
        Mockito.verify(cmd).add("-n", "cephuser");
        // the key is not an argument of the script, it would be visible in the process list
        Mockito.verifyNoMoreInteractions(cmd);
    }

    @Test
    public void testAddRbdConnectionArgsWithoutCephx() {
        for (String[] noAuth : new String[][] {{null, null}, {"", ""}, {" ", null}}) {
            Script cmd = Mockito.mock(Script.class);
            getRbdPool(noAuth[0], noAuth[1]).addRbdConnectionArgs(cmd);
            Mockito.verify(cmd).add("-s", "10.0.0.1:6789,[fd00::1]:6789");
            Mockito.verify(cmd).add("-o", "rbdpool");
            Mockito.verifyNoMoreInteractions(cmd);
        }
    }

    @Test
    public void testAddRbdConnectionArgsWithCephUserOnlyOrKeyOnly() {
        for (String[] partial : new String[][] {{"cephuser", null}, {"cephuser", " "}, {null, "cephkey"}, {"", "cephkey"}}) {
            Script cmd = Mockito.mock(Script.class);
            try {
                getRbdPool(partial[0], partial[1]).addRbdConnectionArgs(cmd);
                fail("Expected a CloudRuntimeException for user [" + partial[0] + "] and key [" + partial[1] + "]");
            } catch (CloudRuntimeException expected) {
                Mockito.verifyNoInteractions(cmd);
            }
        }
    }

    @Test
    public void testHeartBeatScriptEnvironmentHasTheCephKey() {
        assertEquals(java.util.Map.of("CEPH_KEY", "cephkey"), getRbdPool("cephuser", "cephkey").getHeartBeatScriptEnvironment());
    }

    @Test
    public void testHeartBeatScriptEnvironmentWithoutCephx() {
        assertNull(getRbdPool(null, null).getHeartBeatScriptEnvironment());
        assertNull(getRbdPool("", " ").getHeartBeatScriptEnvironment());
    }

    @Test
    public void testHeartBeatScriptEnvironmentOfOtherPoolTypes() {
        LibvirtStoragePool pool = new LibvirtStoragePool("0f7a58bd-1a85-4b1f-9f91-12f3d1ecf5a5", "myfirstpool", StoragePoolType.NetworkFilesystem,
                Mockito.mock(LibvirtStorageAdaptor.class), Mockito.mock(StoragePool.class));
        pool.setAuthSecret("cephkey");
        assertNull(pool.getHeartBeatScriptEnvironment());
    }

    @Test
    public void testVolumeListOfRbdPoolIsGivenInAFile() throws java.io.IOException {
        Script cmd = Mockito.mock(Script.class);
        String volumes = "uuid-1,uuid-2,uuid-3";
        java.io.File file = getRbdPool("cephuser", "cephkey").addVolumeListArgs(cmd, volumes);
        try {
            assertNotNull(file);
            assertEquals(volumes, new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8));
            Mockito.verify(cmd).add("-U", file.getAbsolutePath());
            Mockito.verifyNoMoreInteractions(cmd);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    public void testVolumeListOfRbdPoolCanBeVeryLong() throws java.io.IOException {
        // more than what an argument of a command can have (128 KiB)
        String volumes = String.join(",", java.util.Collections.nCopies(10000, "6d93a798-9fbe-41e2-8b69-fc6b5782f89a"));
        java.io.File file = getRbdPool("cephuser", "cephkey").addVolumeListArgs(Mockito.mock(Script.class), volumes);
        try {
            assertNotNull(file);
            assertEquals(volumes.length(), file.length());
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    public void testVolumeListOfOtherPoolTypesIsAnArgument() {
        Script cmd = Mockito.mock(Script.class);
        LibvirtStoragePool pool = new LibvirtStoragePool("0f7a58bd-1a85-4b1f-9f91-12f3d1ecf5a5", "myfirstpool", StoragePoolType.NetworkFilesystem,
                Mockito.mock(LibvirtStorageAdaptor.class), Mockito.mock(StoragePool.class));
        assertNull(pool.addVolumeListArgs(cmd, "uuid-1,uuid-2"));
        Mockito.verify(cmd).add("-u", "uuid-1,uuid-2");
        Mockito.verifyNoMoreInteractions(cmd);
    }

    @Test
    public void testResolvePhysicalDiskFileUsesNestedRelativePathInsidePool() throws Exception {
        File poolPath = Files.createTempDirectory("libvirt-storage-pool").toFile();
        try {
            LibvirtStoragePool pool = createNetworkFilesystemPool();
            String volumeUid = "cloudstack-cbt/migration/virt-v2v-output/disk.qcow2";
            File expectedVolume = new File(poolPath, volumeUid);
            assertTrue(expectedVolume.getParentFile().mkdirs());
            assertTrue(expectedVolume.createNewFile());

            File resolvedVolume = pool.resolvePhysicalDiskFile(poolPath.getAbsolutePath(), volumeUid, "disk.qcow2");

            assertEquals(expectedVolume.getCanonicalPath(), resolvedVolume.getCanonicalPath());
        } finally {
            deleteRecursively(poolPath);
        }
    }

    @Test
    public void testResolvePhysicalDiskFileRejectsRelativePathOutsidePool() throws Exception {
        File poolPath = Files.createTempDirectory("libvirt-storage-pool").toFile();
        try {
            LibvirtStoragePool pool = createNetworkFilesystemPool();

            File resolvedVolume = pool.resolvePhysicalDiskFile(poolPath.getAbsolutePath(), "../disk.qcow2", "disk.qcow2");

            assertEquals(new File(poolPath, "disk.qcow2").getCanonicalPath(), resolvedVolume.getCanonicalPath());
        } finally {
            deleteRecursively(poolPath);
        }
    }

    private LibvirtStoragePool createNetworkFilesystemPool() {
        StorageAdaptor adapter = Mockito.mock(LibvirtStorageAdaptor.class);
        StoragePool storage = Mockito.mock(StoragePool.class);
        return new LibvirtStoragePool("pool-uuid", "pool-name", StoragePoolType.NetworkFilesystem, adapter, storage);
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
