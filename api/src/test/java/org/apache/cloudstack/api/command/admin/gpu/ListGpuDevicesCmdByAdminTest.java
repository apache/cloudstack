/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command.admin.gpu;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ListGpuDevicesCmdByAdminTest {

    @Test
    public void getId() {
        ListGpuDevicesCmdByAdmin cmd = new ListGpuDevicesCmdByAdmin();
        assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void getHostId() {
        ListGpuDevicesCmdByAdmin cmd = new ListGpuDevicesCmdByAdmin();
        assertNull(cmd.getHostId());
        Long hostId = 1L;
        ReflectionTestUtils.setField(cmd, "hostId", hostId);
        assertEquals(hostId, cmd.getHostId());
    }

    @Test
    public void getGpuCardId() {
        ListGpuDevicesCmdByAdmin cmd = new ListGpuDevicesCmdByAdmin();
        assertNull(cmd.getGpuCardId());
        Long gpuCardId = 1L;
        ReflectionTestUtils.setField(cmd, "gpuCardId", gpuCardId);
        assertEquals(gpuCardId, cmd.getGpuCardId());
    }

    @Test
    public void getVgpuProfileId() {
        ListGpuDevicesCmdByAdmin cmd = new ListGpuDevicesCmdByAdmin();
        assertNull(cmd.getVgpuProfileId());
        Long vgpuProfileId = 1L;
        ReflectionTestUtils.setField(cmd, "vgpuProfileId", vgpuProfileId);
        assertEquals(vgpuProfileId, cmd.getVgpuProfileId());
    }
}
