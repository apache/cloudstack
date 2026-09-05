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
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UpdateGpuCardCmdTest {

    @Test
    public void getId() {
        UpdateGpuCardCmd cmd = new UpdateGpuCardCmd();
        assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void getDeviceName() {
        UpdateGpuCardCmd cmd = new UpdateGpuCardCmd();
        assertNull(cmd.getDeviceName());
        String deviceName = "GPU-1234";
        ReflectionTestUtils.setField(cmd, "deviceName", deviceName);
        assertEquals(deviceName, cmd.getDeviceName());
    }

    @Test
    public void getName() {
        UpdateGpuCardCmd cmd = new UpdateGpuCardCmd();
        assertNull(cmd.getName());
        String name = "Test GPU Card";
        ReflectionTestUtils.setField(cmd, "name", name);
        assertEquals(name, cmd.getName());
    }

    @Test
    public void getVendorName() {
        UpdateGpuCardCmd cmd = new UpdateGpuCardCmd();
        assertNull(cmd.getVendorName());
        String vendorName = "NVIDIA";
        ReflectionTestUtils.setField(cmd, "vendorName", vendorName);
        assertEquals(vendorName, cmd.getVendorName());
    }

    @Test
    public void getEntityOwnerId() {
        UpdateGpuCardCmd cmd = new UpdateGpuCardCmd();
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
}
