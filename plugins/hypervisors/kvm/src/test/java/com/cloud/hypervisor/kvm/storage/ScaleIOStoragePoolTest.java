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
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOStoragePoolTest {

    ScaleIOStoragePool pool;

    StorageAdaptor adapter;

    @Mock
    StorageLayer storageLayer;

    @Before
    public void setUp() throws Exception {
        final String uuid = "345fc603-2d7e-47d2-b719-a0110b3732e6";
        final String systemId = "218ce1797566a00f";
        final StoragePoolType type = StoragePoolType.PowerFlex;
        Map<String,String> details = new HashMap<String, String>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);

        adapter = spy(new ScaleIOStorageAdaptor(storageLayer));
        pool = new ScaleIOStoragePool(uuid, "192.168.1.19", 443, "a519be2f00000000", type, details, adapter);
    }

    @Test
    public void testAttributes() {
        assertEquals(0, pool.getCapacity());
        assertEquals(0, pool.getUsed());
        assertEquals(0, pool.getAvailable());
        assertEquals("345fc603-2d7e-47d2-b719-a0110b3732e6", pool.getUuid());
        assertEquals("192.168.1.19", pool.getSourceHost());
        assertEquals(443, pool.getSourcePort());
        assertEquals("a519be2f00000000", pool.getSourceDir());
        assertEquals(StoragePoolType.PowerFlex, pool.getType());
        assertEquals("218ce1797566a00f", pool.getDetails().get(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID));

        pool.setCapacity(131072);
        pool.setUsed(24576);
        pool.setAvailable(106496);

        assertEquals(131072, pool.getCapacity());
        assertEquals(24576, pool.getUsed());
        assertEquals(106496, pool.getAvailable());
    }

    @Test
    public void testSdcIdAttribute() {
        final String uuid = "345fc603-2d7e-47d2-b719-a0110b3732e6";
        final String systemId = "218ce1797566a00f";
        final String sdcId = "301b852c00000003";
        final StoragePoolType type = StoragePoolType.PowerFlex;
        Map<String,String> details = new HashMap<String, String>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);

        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class)) {
            when(Script.runSimpleBashScript(
                    "/opt/emc/scaleio/sdc/bin/drv_cfg --query_mdms|grep 218ce1797566a00f|awk '{print $5}'")).thenReturn(
                    sdcId);

            ScaleIOStoragePool pool1 = new ScaleIOStoragePool(uuid, "192.168.1.19", 443, "a519be2f00000000", type,
                    details, adapter);
            assertEquals(systemId, pool1.getDetails().get(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID));
            assertEquals(sdcId, pool1.getDetails().get(ScaleIOGatewayClient.SDC_ID));
        }
    }

    @Test
    public void testSdcGuidAttribute() {
        final String uuid = "345fc603-2d7e-47d2-b719-a0110b3732e6";
        final String systemId = "218ce1797566a00f";
        final String sdcGuid = "B0E3BFB8-C20B-43BF-93C8-13339E85AA50";
        final StoragePoolType type = StoragePoolType.PowerFlex;
        Map<String,String> details = new HashMap<String, String>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);

        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class)) {
            when(Script.runSimpleBashScript(
                    "/opt/emc/scaleio/sdc/bin/drv_cfg --query_mdms|grep 218ce1797566a00f|awk '{print $5}'")).thenReturn(
                    null);
            when(Script.runSimpleBashScript("/opt/emc/scaleio/sdc/bin/drv_cfg --query_guid")).thenReturn(sdcGuid);

            ScaleIOStoragePool pool1 = new ScaleIOStoragePool(uuid, "192.168.1.19", 443, "a519be2f00000000", type,
                    details, adapter);
            assertEquals(systemId, pool1.getDetails().get(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID));
            assertEquals(sdcGuid, pool1.getDetails().get(ScaleIOGatewayClient.SDC_GUID));
        }
    }

    @Test
    public void testDefaults() {
        assertEquals(PhysicalDiskFormat.RAW, pool.getDefaultFormat());
        assertEquals(StoragePoolType.PowerFlex, pool.getType());

        assertNull(pool.getAuthUserName());
        assertNull(pool.getAuthSecret());

        Assert.assertFalse(pool.supportsConfigDriveIso());
        assertTrue(pool.isExternalSnapshot());
    }

    public void testGetPhysicalDiskWithWildcardFileFilter() throws Exception {
        final String volumePath = "6c3362b500000001:vol-139-3d2c-12f0";
        final String systemId = "218ce1797566a00f";

        // TODO: Mock file in dir

        try (MockedConstruction<File> ignored = Mockito.mockConstruction(File.class, (mock, context) -> {
            File[] files = new File[1];
            String volumeId = ScaleIOUtil.getVolumePath(volumePath);
            String diskFilePath =
                    ScaleIOUtil.DISK_PATH + File.separator + ScaleIOUtil.DISK_NAME_PREFIX + systemId + "-" + volumeId;
            files[0] = new File(diskFilePath);
            Mockito.when(mock.listFiles(any(FileFilter.class))).thenReturn(files);
        })) {
            KVMPhysicalDisk disk = adapter.getPhysicalDisk(volumePath, pool);
            assertNull(disk);
        }
    }


    @Test
    public void testGetPhysicalDiskWithSystemId() throws Exception {
        final String volumePath = "6c3362b500000001:vol-139-3d2c-12f0";
        final String volumeId = ScaleIOUtil.getVolumePath(volumePath);
        final String systemId = "218ce1797566a00f";
        try (MockedStatic<ScaleIOUtil> ignored = Mockito.mockStatic(ScaleIOUtil.class, Mockito.CALLS_REAL_METHODS)) {
            when(ScaleIOUtil.getSystemIdForVolume(volumeId)).thenReturn(systemId);

            // TODO: Mock file exists

            KVMPhysicalDisk disk = adapter.getPhysicalDisk(volumePath, pool);
            assertNull(disk);
        }
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

        assertEquals("/dev/disk/by-id/emc-vol-218ce1797566a00f-6c3362b500000001", disk.getPath());

        when(adapter.getPhysicalDisk(volumeId, pool)).thenReturn(disk);

        final boolean result = adapter.connectPhysicalDisk(volumePath, pool, null);
        assertTrue(result);
    }
}
