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

package org.apache.cloudstack.framework.extensions.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.junit.Test;

public class CreateExtensionCmdTest {
    CreateExtensionCmd cmd = new CreateExtensionCmd();

    @Test
    public void testGetNameReturnsNullWhenUnset() {
        assertNull(cmd.getName());
    }

    @Test
    public void testGetNameReturnsValueWhenSet() {
        String name = "name";
        setField(cmd, "name", name);
        assertEquals(name, cmd.getName());
    }

    @Test
    public void testGetTypeReturnsNullWhenUnset() {
        setField(cmd, "type", null);
        assertNull(cmd.getType());
    }

    @Test
    public void testGetDescriptionReturnsValueWhenSet() {
        String description = "description";
        setField(cmd, "description", description);
        assertEquals(description, cmd.getDescription());
    }

    @Test
    public void testGetPathReturnsValueWhenSet() {
        String path = "/entry";
        setField(cmd, "path", path);
        assertEquals(path, cmd.getPath());
    }

    @Test
    public void testGetStateReturnsNullWhenUnset() {
        setField(cmd, "state", null);
        assertNull(cmd.getState());
    }

    @Test
    public void testIsOrchestratorRequiresPrepareVm() {
        assertNull(cmd.isOrchestratorRequiresPrepareVm());
        setField(cmd, "orchestratorRequiresPrepareVm", true);
        assertTrue(cmd.isOrchestratorRequiresPrepareVm());
        setField(cmd, "orchestratorRequiresPrepareVm", false);
        assertFalse(cmd.isOrchestratorRequiresPrepareVm());
    }

    @Test
    public void testGetDetailsReturnsNullWhenUnset() {
        setField(cmd, "details", null);
        assertTrue(MapUtils.isEmpty(cmd.getDetails()));
    }

    @Test
    public void testGetDetailsReturnsMap() {
        Map<String, Map<String, String>> details = new HashMap<>();
        Map<String, String> inner = new HashMap<>();
        inner.put("key", "value");
        details.put("details", inner);
        setField(cmd, "details", details);
        assertTrue(MapUtils.isNotEmpty(cmd.getDetails()));
    }
}
