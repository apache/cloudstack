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

public class CreateVgpuProfileCmdTest {

    @Test
    public void getName() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertNull(cmd.getName());
        String name = "Test VGPU Profile";
        ReflectionTestUtils.setField(cmd, "name", name);
        assertEquals(name, cmd.getName());
    }

    @Test
    public void getDescription() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertNull(cmd.getDescription());
        String description = "Test VGPU Profile Description";
        ReflectionTestUtils.setField(cmd, "description", description);
        assertEquals(description, cmd.getDescription());
    }

    @Test
    public void getCardId() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertNull(cmd.getCardId());
        Long cardId = 1L;
        ReflectionTestUtils.setField(cmd, "cardId", cardId);
        assertEquals(cardId, cmd.getCardId());
    }

    @Test
    public void getMaxVgpuPerPgpu() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertNull(cmd.getMaxVgpuPerPgpu());
        Long maxVgpuPerPgpu = 8L;
        ReflectionTestUtils.setField(cmd, "maxVgpuPerPgpu", maxVgpuPerPgpu);
        assertEquals(maxVgpuPerPgpu, cmd.getMaxVgpuPerPgpu());
    }

    @Test
    public void getVideoRam() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertNull(cmd.getVideoRam());
        Long videoRam = 8192L; // 8 GB
        ReflectionTestUtils.setField(cmd, "videoRam", videoRam);
        assertEquals(videoRam, cmd.getVideoRam());
    }

    @Test
    public void getMaxHeads() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertNull(cmd.getMaxHeads());
        Long maxHeads = 2L;
        ReflectionTestUtils.setField(cmd, "maxHeads", maxHeads);
        assertEquals(maxHeads, cmd.getMaxHeads());
    }

    @Test
    public void getMaxResolutionX() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertNull(cmd.getMaxResolutionX());
        Long maxResolutionX = 1920L; // 1920 pixels
        ReflectionTestUtils.setField(cmd, "maxResolutionX", maxResolutionX);
        assertEquals(maxResolutionX, cmd.getMaxResolutionX());
    }

    @Test
    public void getMaxResolutionY() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertNull(cmd.getMaxResolutionY());
        Long maxResolutionY = 1080L; // 1080 pixels
        ReflectionTestUtils.setField(cmd, "maxResolutionY", maxResolutionY);
        assertEquals(maxResolutionY, cmd.getMaxResolutionY());
    }

    @Test
    public void getEntityOwnerId() {
        CreateVgpuProfileCmd cmd = new CreateVgpuProfileCmd();
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
}
