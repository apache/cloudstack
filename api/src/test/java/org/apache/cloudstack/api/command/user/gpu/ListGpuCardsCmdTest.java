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

package org.apache.cloudstack.api.command.user.gpu;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;


public class ListGpuCardsCmdTest {

    @Test
    public void getId() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        Assert.assertEquals(id, cmd.getId());
    }

    @Test
    public void getVendorName() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getVendorName());
        String vendorName = "vendor name";
        ReflectionTestUtils.setField(cmd, "vendorName", vendorName);
        Assert.assertEquals(vendorName, cmd.getVendorName());
    }

    @Test
    public void getVendorId() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getVendorId());
        String vendorId = "vendor id";
        ReflectionTestUtils.setField(cmd, "vendorId", vendorId);
        Assert.assertEquals(vendorId, cmd.getVendorId());
    }

    @Test
    public void getDeviceId() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getDeviceId());
        String deviceId = "device id";
        ReflectionTestUtils.setField(cmd, "deviceId", deviceId);
        Assert.assertEquals(deviceId, cmd.getDeviceId());
    }

    @Test
    public void getDeviceName() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getDeviceName());
        String deviceName = "device name";
        ReflectionTestUtils.setField(cmd, "deviceName", deviceName);
        Assert.assertEquals(deviceName, cmd.getDeviceName());
    }

    @Test
    public void getActiveOnly() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertFalse(cmd.getActiveOnly());
        Boolean activeOnly = true;
        ReflectionTestUtils.setField(cmd, "activeOnly", activeOnly);
        Assert.assertEquals(activeOnly, cmd.getActiveOnly());
    }
}
