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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.junit.Test;

import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;

public class VolumeJoinVOToDiskConverterTest {

    @Test
    public void testToDisk_MapsCoreFieldsAndResolverOverridesActualSize() {
        final VolumeJoinVO vol = mock(VolumeJoinVO.class);
        when(vol.getUuid()).thenReturn("vol-1");
        when(vol.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(vol.getName()).thenReturn("root-disk");
        when(vol.getSize()).thenReturn(1000L);
        when(vol.getVolumeStoreSize()).thenReturn(500L);
        when(vol.getFormat()).thenReturn(Storage.ImageFormat.RAW);
        when(vol.getState()).thenReturn(Volume.State.Ready);
        when(vol.getPath()).thenReturn("path-1");
        when(vol.getDiskOfferingUuid()).thenReturn("do-1");
        when(vol.getPoolUuid()).thenReturn("pool-1");

        final Disk disk = VolumeJoinVOToDiskConverter.toDisk(vol, v -> 700L);

        assertEquals("vol-1", disk.getId());
        assertEquals("true", disk.getBootable());
        assertEquals("raw", disk.getFormat());
        assertEquals("ok", disk.getStatus());
        assertEquals("700", disk.getActualSize());
        assertNotNull(disk.getStorageDomains());
        assertTrue(disk.getHref().contains("/api/disks/vol-1"));
    }

    @Test
    public void testToDiskAttachment_MapsVmAndDisk() {
        final VolumeJoinVO vol = mock(VolumeJoinVO.class);
        when(vol.getUuid()).thenReturn("vol-2");
        when(vol.getVmUuid()).thenReturn("vm-2");
        when(vol.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(vol.getName()).thenReturn("data-disk");
        when(vol.getSize()).thenReturn(2048L);
        when(vol.getVolumeStoreSize()).thenReturn(1024L);
        when(vol.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(vol.getState()).thenReturn(Volume.State.Allocated);
        when(vol.getPath()).thenReturn("path-2");
        when(vol.getDiskOfferingUuid()).thenReturn("do-2");
        when(vol.getPoolUuid()).thenReturn("pool-2");

        final DiskAttachment da = VolumeJoinVOToDiskConverter.toDiskAttachment(vol, null);

        assertEquals("vol-2", da.getId());
        assertEquals("vm-2", da.getVm().getId());
        assertEquals("false", da.getBootable());
        assertEquals("virtio_scsi", da.getIface());
        assertEquals("vol-2", da.getDisk().getId());
    }

    @Test
    public void testToDiskListFromVolumeInfos_MapsBootableByVolumeType() {
        final Backup.VolumeInfo root = new Backup.VolumeInfo("root-id", "p1", Volume.Type.ROOT, 10L, 0L, "do", 0L, 0L);
        final Backup.VolumeInfo data = new Backup.VolumeInfo("data-id", "p2", Volume.Type.DATADISK, 20L, 1L, "do", 0L, 0L);

        final List<Disk> result = VolumeJoinVOToDiskConverter.toDiskListFromVolumeInfos(List.of(root, data));

        assertEquals(2, result.size());
        assertEquals("true", result.get(0).getBootable());
        assertEquals("false", result.get(1).getBootable());
        assertEquals("20", result.get(1).getTotalSize());
    }
}
