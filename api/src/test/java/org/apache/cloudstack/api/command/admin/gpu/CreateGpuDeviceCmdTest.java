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

import com.cloud.user.Account;
import org.apache.cloudstack.gpu.GpuDevice;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CreateGpuDeviceCmdTest {

    @Test
    public void getHostId() {
        CreateGpuDeviceCmd cmd = new CreateGpuDeviceCmd();
        assertNull(cmd.getHostId());
        Long hostId = 1L;
        ReflectionTestUtils.setField(cmd, "hostId", hostId);
        assertEquals(hostId, cmd.getHostId());
    }

    @Test
    public void getBusAddress() {
        CreateGpuDeviceCmd cmd = new CreateGpuDeviceCmd();
        assertNull(cmd.getBusAddress());
        String busAddress = "0000:00:1f.6";
        ReflectionTestUtils.setField(cmd, "busAddress", busAddress);
        assertEquals(busAddress, cmd.getBusAddress());
    }

    @Test
    public void getGpuCardId() {
        CreateGpuDeviceCmd cmd = new CreateGpuDeviceCmd();
        assertNull(cmd.getGpuCardId());
        Long gpuCardId = 1L;
        ReflectionTestUtils.setField(cmd, "gpuCardId", gpuCardId);
        assertEquals(gpuCardId, cmd.getGpuCardId());
    }

    @Test
    public void getVgpuProfileId() {
        CreateGpuDeviceCmd cmd = new CreateGpuDeviceCmd();
        assertNull(cmd.getVgpuProfileId());
        Long vgpuProfileId = 1L;
        ReflectionTestUtils.setField(cmd, "vgpuProfileId", vgpuProfileId);
        assertEquals(vgpuProfileId, cmd.getVgpuProfileId());
    }

    @Test
    public void getType() {
        CreateGpuDeviceCmd cmd = new CreateGpuDeviceCmd();
        assertEquals(GpuDevice.DeviceType.PCI, cmd.getType());
        String type = "MDEV";
        ReflectionTestUtils.setField(cmd, "type", type);
        assertEquals(GpuDevice.DeviceType.MDEV, cmd.getType());
    }

    @Test
    public void getParentGpuDeviceId() {
        CreateGpuDeviceCmd cmd = new CreateGpuDeviceCmd();
        assertNull(cmd.getParentGpuDeviceId());
        Long parentGpuDeviceId = 1L;
        ReflectionTestUtils.setField(cmd, "parentGpuDeviceId", parentGpuDeviceId);
        assertEquals(parentGpuDeviceId, cmd.getParentGpuDeviceId());
    }

    @Test
    public void getNumaNode() {
        CreateGpuDeviceCmd cmd = new CreateGpuDeviceCmd();
        assertEquals("-1", cmd.getNumaNode());
        String numaNode = "0";
        ReflectionTestUtils.setField(cmd, "numaNode", numaNode);
        assertEquals(numaNode, cmd.getNumaNode());
    }

    @Test
    public void getEntityOwnerId() {
        CreateGpuDeviceCmd cmd = new CreateGpuDeviceCmd();
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
}
