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
package org.apache.cloudstack.api.command.user.guest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.cpu.CPU;

@RunWith(MockitoJUnitRunner.class)
public class ListGuestOsCategoriesCmdTest {

    @Test
    public void testIsFeatured() {
        ListGuestOsCategoriesCmd cmd = new ListGuestOsCategoriesCmd();
        Assert.assertNull(cmd.isFeatured());
        ReflectionTestUtils.setField(cmd, "featured", false);
        Assert.assertFalse(cmd.isFeatured());
        ReflectionTestUtils.setField(cmd, "featured", true);
        Assert.assertTrue(cmd.isFeatured());
    }

    @Test
    public void testIsIso() {
        ListGuestOsCategoriesCmd cmd = new ListGuestOsCategoriesCmd();
        Assert.assertNull(cmd.isIso());
        ReflectionTestUtils.setField(cmd, "iso", false);
        Assert.assertFalse(cmd.isIso());
        ReflectionTestUtils.setField(cmd, "iso", true);
        Assert.assertTrue(cmd.isIso());
    }

    @Test
    public void testIsVnf() {
        ListGuestOsCategoriesCmd cmd = new ListGuestOsCategoriesCmd();
        Assert.assertNull(cmd.isVnf());
        ReflectionTestUtils.setField(cmd, "vnf", false);
        Assert.assertFalse(cmd.isVnf());
        ReflectionTestUtils.setField(cmd, "vnf", true);
        Assert.assertTrue(cmd.isVnf());
    }

    @Test
    public void testGetZoneId() {
        ListGuestOsCategoriesCmd cmd = new ListGuestOsCategoriesCmd();
        Assert.assertNull(cmd.getZoneId());
        Long zoneId = 100L;
        ReflectionTestUtils.setField(cmd, "zoneId", zoneId);
        Assert.assertEquals(zoneId, cmd.getZoneId());
    }

    @Test
    public void testGetArch() {
        ListGuestOsCategoriesCmd cmd = new ListGuestOsCategoriesCmd();
        Assert.assertNull(cmd.getArch());
        CPU.CPUArch arch = CPU.CPUArch.getDefault();
        ReflectionTestUtils.setField(cmd, "arch", arch.getType());
        Assert.assertEquals(arch, cmd.getArch());
    }

    @Test
    public void testIsShowIcon() {
        ListGuestOsCategoriesCmd cmd = new ListGuestOsCategoriesCmd();
        Assert.assertFalse(cmd.isShowIcon());
        ReflectionTestUtils.setField(cmd, "showIcon", false);
        Assert.assertFalse(cmd.isShowIcon());
        ReflectionTestUtils.setField(cmd, "showIcon", true);
        Assert.assertTrue(cmd.isShowIcon());
    }
}
