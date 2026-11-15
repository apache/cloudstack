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

package org.apache.cloudstack.api.command.admin.vm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DeployVMCmdByAdminTest {

    @InjectMocks
    private DeployVMCmdByAdmin cmd;

    @Test
    public void testIsBlankInstance_default() {
        assertFalse(cmd.isBlankInstance());
    }

    @Test
    public void testIsBlankInstance_true() {
        ReflectionTestUtils.setField(cmd, "blankInstance", true);
        assertTrue(cmd.isBlankInstance());
    }

    @Test
    public void testIsBlankInstance_false() {
        ReflectionTestUtils.setField(cmd, "blankInstance", false);
        assertFalse(cmd.isBlankInstance());
    }

    @Test
    public void testSetBlankInstance_default() {
        Object obj = ReflectionTestUtils.getField(cmd, "blankInstance");
        assertNull(obj);
    }

    @Test
    public void testSetBlankInstance_true() {
        cmd.setBlankInstance(true);
        Object obj = ReflectionTestUtils.getField(cmd, "blankInstance");
        assertNotNull(obj);
        assertTrue((boolean)obj);
    }

    @Test
    public void testSetBlankInstance_false() {
        cmd.setBlankInstance(false);
        Object obj = ReflectionTestUtils.getField(cmd, "blankInstance");
        assertNotNull(obj);
        assertFalse((boolean)obj);
    }
}
