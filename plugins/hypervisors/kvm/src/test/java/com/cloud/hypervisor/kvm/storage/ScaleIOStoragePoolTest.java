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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileFilter;

import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;

@PrepareForTest(ScaleIOUtil.class)
@RunWith(PowerMockRunner.class)
public class ScaleIOStoragePoolTest {

    ScaleIOStoragePool pool;

    StorageAdaptor adapter;

    @Mock
    StorageLayer storageLayer;

    @Before
    public void setUp() throws Exception {
        final String uuid = "345fc603-2d7e-47d2-b719-a0110b3732e6";
        final StoragePoolType type = StoragePoolType.PowerFlex;

        adapter = spy(new ScaleIOStorageAdaptor(storageLayer));
        pool = new ScaleIOStoragePool(uuid, "192.168.1.19", 443, "a519be2f00000000", type, adapter);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAttributes() {
        assertEquals(pool.getCapacity(), 0);
        assertEquals(pool.getUsed(), 0);
        assertEquals(pool.getAvailable(), 0);
        assertEquals(pool.getUuid(), "345fc603-2d7e-47d2-b719-a0110b3732e6");
        assertEquals(pool.getSourceHost(), "192.168.1.19");
        assertEquals(pool.getSourcePort(), 443);
        assertEquals(pool.getSourceDir(), "a519be2f00000000");
        assertEquals(pool.getType(), StoragePoolType.PowerFlex);

        pool.setCapacity(131072);
        pool.setUsed(24576);
        pool.setAvailable(106496);

        assertEquals(pool.getCapacity(), 131072);
        assertEquals(pool.getUsed(), 24576);
        assertEquals(pool.getAvailable(), 106496);
    }

    @Test
    public void testDefaults() {
        assertEquals(pool.getDefaultFormat(), PhysicalDiskFormat.RAW);
        assertEquals(pool.getType(), StoragePoolType.PowerFlex);

        assertNull(pool.getAuthUserName());
        assertNull(pool.getAuthSecret());

        Assert.assertFalse(pool.supportsConfigDriveIso());
        assertTrue(pool.isExternalSnapshot());
    }

    public void testGetPhysicalDiskWithWildcardFileFilter() throws Exception {
        final String volumePath = "6c3362b500000001:vol-139-3d2c-12f0";
        final String systemId = "218ce1797566a00f";

        File dir = PowerMockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(dir);

        // TODO: Mock file in dir
        File[] files = new File[1];
        String volumeId = ScaleIOUtil.getVolumePath(volumePath);
        String diskFilePath = ScaleIOUtil.DISK_PATH + File.separator + ScaleIOUtil.DISK_NAME_PREFIX + systemId + "-" + volumeId;
        files[0] = new File(diskFilePath);
        PowerMockito.when(dir.listFiles(any(FileFilter.class))).thenReturn(files);

        KVMPhysicalDisk disk = adapter.getPhysicalDisk(volumePath, pool);
        assertNull(disk);
    }

    @Test
    public void testGetPhysicalDiskWithSystemId() throws Exception {
        final String volumePath = "6c3362b500000001:vol-139-3d2c-12f0";
        final String volumeId = ScaleIOUtil.getVolumePath(volumePath);
        final String systemId = "218ce1797566a00f";
        PowerMockito.mockStatic(ScaleIOUtil.class);
        when(ScaleIOUtil.getSystemIdForVolume(volumeId)).thenReturn(systemId);

        // TODO: Mock file exists
        File file = PowerMockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        PowerMockito.when(file.exists()).thenReturn(true);

        KVMPhysicalDisk disk = adapter.getPhysicalDisk(volumePath, pool);
        assertNull(disk);
    }

    @Test
    public void testConnectPhysicalDisk() {
        final String volumePath = "6c3362b500000001:vol-139-3d2c-12f0";
        final String volumeId = ScaleIOUtil.getVolumePath(volumePath);
        final String systemId = "218ce1797566a00f";
        final String diskFilePath = ScaleIOUtil.DISK_PATH + File.separator + ScaleIOUtil.DISK_NAME_PREFIX + systemId + "-" + volumeId;
        KVMPhysicalDisk disk = new KVMPhysicalDisk(diskFilePath, volumePath, pool);
        disk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
        disk.setSize(8192);
        disk.setVirtualSize(8192);

        assertEquals(disk.getPath(), "/dev/disk/by-id/emc-vol-218ce1797566a00f-6c3362b500000001");

        when(adapter.getPhysicalDisk(volumeId, pool)).thenReturn(disk);

        final boolean result = adapter.connectPhysicalDisk(volumePath, pool, null);
        assertTrue(result);
    }
}