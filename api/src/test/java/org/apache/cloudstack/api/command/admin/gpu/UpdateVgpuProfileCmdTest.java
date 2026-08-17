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

public class UpdateVgpuProfileCmdTest {

    @Test
    public void getId() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void getProfileName() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertNull(cmd.getProfileName());
        String profileName = "Test VGPU Profile";
        ReflectionTestUtils.setField(cmd, "profileName", profileName);
        assertEquals(profileName, cmd.getProfileName());
    }

    @Test
    public void getDescription() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertNull(cmd.getDescription());
        String description = "Test VGPU Profile Description";
        ReflectionTestUtils.setField(cmd, "description", description);
        assertEquals(description, cmd.getDescription());
    }

    @Test
    public void getMaxVgpuPerPgpu() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertNull(cmd.getMaxVgpuPerPgpu());
        Long maxVgpuPerPgpu = 8L;
        ReflectionTestUtils.setField(cmd, "maxVgpuPerPgpu", maxVgpuPerPgpu);
        assertEquals(maxVgpuPerPgpu, cmd.getMaxVgpuPerPgpu());
    }

    @Test
    public void getVideoRam() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertNull(cmd.getVideoRam());
        Long videoRam = 8192L; // 8 GB
        ReflectionTestUtils.setField(cmd, "videoRam", videoRam);
        assertEquals(videoRam, cmd.getVideoRam());
    }

    @Test
    public void getMaxHeads() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertNull(cmd.getMaxHeads());
        Long maxHeads = 2L;
        ReflectionTestUtils.setField(cmd, "maxHeads", maxHeads);
        assertEquals(maxHeads, cmd.getMaxHeads());
    }

    @Test
    public void getMaxResolutionX() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertNull(cmd.getMaxResolutionX());
        Long maxResolutionX = 1920L; // Example resolution
        ReflectionTestUtils.setField(cmd, "maxResolutionX", maxResolutionX);
        assertEquals(maxResolutionX, cmd.getMaxResolutionX());
    }

    @Test
    public void getMaxResolutionY() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertNull(cmd.getMaxResolutionY());
        Long maxResolutionY = 1080L; // Example resolution
        ReflectionTestUtils.setField(cmd, "maxResolutionY", maxResolutionY);
        assertEquals(maxResolutionY, cmd.getMaxResolutionY());
    }

    @Test
    public void getEntityOwnerId() {
        UpdateVgpuProfileCmd cmd = new UpdateVgpuProfileCmd();
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
}
