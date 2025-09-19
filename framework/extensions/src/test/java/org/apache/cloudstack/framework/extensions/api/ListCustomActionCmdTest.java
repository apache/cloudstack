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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class ListCustomActionCmdTest {

    private ListCustomActionCmd cmd;
    private ExtensionsManager extensionsManager;

    @Before
    public void setUp() {
        cmd = new ListCustomActionCmd();
        extensionsManager = mock(ExtensionsManager.class);
        ReflectionTestUtils.setField(cmd, "extensionsManager", extensionsManager);
    }

    private void setField(String fieldName, Object value) {
        ReflectionTestUtils.setField(cmd, fieldName, value);
    }

    @Test
    public void getIdReturnsNullWhenUnset() {
        setField("id", null);
        assertNull(cmd.getId());
    }

    @Test
    public void getIdReturnsValueWhenSet() {
        Long id = 42L;
        setField("id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void getNameReturnsNullWhenUnset() {
        setField("name", null);
        assertNull(cmd.getName());
    }

    @Test
    public void getNameReturnsValueWhenSet() {
        String name = "customAction";
        setField("name", name);
        assertEquals(name, cmd.getName());
    }

    @Test
    public void getExtensionIdReturnsNullWhenUnset() {
        setField("extensionId", null);
        assertNull(cmd.getExtensionId());
    }

    @Test
    public void getExtensionIdReturnsValueWhenSet() {
        Long extensionId = 99L;
        setField("extensionId", extensionId);
        assertEquals(extensionId, cmd.getExtensionId());
    }

    @Test
    public void getResourceTypeReturnsNullWhenUnset() {
        setField("resourceType", null);
        assertNull(cmd.getResourceType());
    }

    @Test
    public void getResourceTypeReturnsValueWhenSet() {
        String resourceType = "VM";
        setField("resourceType", resourceType);
        assertEquals(resourceType, cmd.getResourceType());
    }

    @Test
    public void getResourceIdReturnsNullWhenUnset() {
        setField("resourceId", null);
        assertNull(cmd.getResourceId());
    }

    @Test
    public void getResourceIdReturnsValueWhenSet() {
        String resourceId = "abc-123";
        setField("resourceId", resourceId);
        assertEquals(resourceId, cmd.getResourceId());
    }

    @Test
    public void isEnabledReturnsNullWhenUnset() {
        setField("enabled", null);
        assertNull(cmd.isEnabled());
    }

    @Test
    public void isEnabledReturnsTrueWhenSetTrue() {
        setField("enabled", Boolean.TRUE);
        assertTrue(cmd.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseWhenSetFalse() {
        setField("enabled", Boolean.FALSE);
        assertFalse(cmd.isEnabled());
    }

    @Test
    public void executeSetsListResponse() {
        List<ExtensionCustomActionResponse> responses = Arrays.asList(mock(ExtensionCustomActionResponse.class));
        when(extensionsManager.listCustomActions(cmd)).thenReturn(responses);

        ListCustomActionCmd spyCmd = Mockito.spy(cmd);
        doNothing().when(spyCmd).setResponseObject(any());

        spyCmd.execute();

        verify(extensionsManager).listCustomActions(spyCmd);
        verify(spyCmd).setResponseObject(any(ListResponse.class));
    }
}
