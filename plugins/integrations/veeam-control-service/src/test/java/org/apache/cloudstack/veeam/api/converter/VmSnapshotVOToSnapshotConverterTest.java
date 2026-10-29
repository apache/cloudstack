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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.junit.Test;

import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;

public class VmSnapshotVOToSnapshotConverterTest {

    @Test
    public void testToSnapshot_MapsReadyDiskAndMemorySnapshot() {
        final VMSnapshotVO vo = mock(VMSnapshotVO.class);
        when(vo.getUuid()).thenReturn("snap-1");
        when(vo.getDescription()).thenReturn("desc");
        when(vo.getCreated()).thenReturn(new Date(1234L));
        when(vo.getType()).thenReturn(VMSnapshotVO.Type.DiskAndMemory);
        when(vo.getState()).thenReturn(VMSnapshot.State.Ready);

        final Snapshot snapshot = VmSnapshotVOToSnapshotConverter.toSnapshot(vo, "vm-1");

        assertEquals("snap-1", snapshot.getId());
        assertTrue(snapshot.getHref().contains("/api/vms/vm-1/snapshots/snap-1"));
        assertEquals("vm-1", snapshot.getVm().getId());
        assertEquals("desc", snapshot.getDescription());
        assertEquals(Long.valueOf(1234L), snapshot.getDate());
        assertEquals("true", snapshot.getPersistMemorystate());
        assertEquals("ok", snapshot.getSnapshotStatus());
        assertEquals("link", snapshot.getActions().asMap().keySet().iterator().next());
    }

    @Test
    public void testToSnapshot_MapsNonReadyToLockedAndToSnapshotList() {
        final VMSnapshotVO vo = mock(VMSnapshotVO.class);
        when(vo.getUuid()).thenReturn("snap-2");
        when(vo.getDescription()).thenReturn("desc2");
        when(vo.getCreated()).thenReturn(new Date(5678L));
        when(vo.getType()).thenReturn(VMSnapshotVO.Type.Disk);
        when(vo.getState()).thenReturn(VMSnapshot.State.Creating);

        final Snapshot snapshot = VmSnapshotVOToSnapshotConverter.toSnapshot(vo, "vm-2");
        assertEquals("false", snapshot.getPersistMemorystate());
        assertEquals("locked", snapshot.getSnapshotStatus());

        final List<Snapshot> list = VmSnapshotVOToSnapshotConverter.toSnapshotList(List.of(vo), "vm-2");
        assertEquals(1, list.size());
        assertEquals("snap-2", list.get(0).getId());
    }
}
