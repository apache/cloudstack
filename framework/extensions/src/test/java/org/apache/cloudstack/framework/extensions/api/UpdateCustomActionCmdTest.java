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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.commons.collections.MapUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class UpdateCustomActionCmdTest {

    private UpdateCustomActionCmd cmd;
    private ExtensionsManager extensionsManager;

    @Before
    public void setUp() {
        cmd = Mockito.spy(new UpdateCustomActionCmd());
        extensionsManager = mock(ExtensionsManager.class);
        ReflectionTestUtils.setField(cmd, "extensionsManager", extensionsManager);
    }

    @Test
    public void idReturnsValueWhenSet() {
        long id = 12345L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void descriptionReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "description", null);
        assertNull(cmd.getDescription());
    }

    @Test
    public void descriptionReturnsValueWhenSet() {
        String description = "Custom action description";
        ReflectionTestUtils.setField(cmd, "description", description);
        assertEquals(description, cmd.getDescription());
    }

    @Test
    public void resourceTypeReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "resourceType", null);
        assertNull(cmd.getResourceType());
    }

    @Test
    public void resourceTypeReturnsValueWhenSet() {
        String resourceType = "VM";
        ReflectionTestUtils.setField(cmd, "resourceType", resourceType);
        assertEquals(resourceType, cmd.getResourceType());
    }

    @Test
    public void allowedRoleTypesReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "allowedRoleTypes", null);
        assertNull(cmd.getAllowedRoleTypes());
    }

    @Test
    public void allowedRoleTypesReturnsValueWhenSet() {
        List<String> roles = Arrays.asList("Admin", "User");
        ReflectionTestUtils.setField(cmd, "allowedRoleTypes", roles);
        assertEquals(roles, cmd.getAllowedRoleTypes());
    }

    @Test
    public void parametersMapReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "parameters", null);
        assertNull(cmd.getParametersMap());
    }

    @Test
    public void parametersMapReturnsValueWhenSet() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "param1");
        ReflectionTestUtils.setField(cmd, "parameters", params);
        assertEquals(params, cmd.getParametersMap());
    }

    @Test
    public void isCleanupParametersReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "cleanupParameters", null);
        assertNull(cmd.isCleanupParameters());
    }

    @Test
    public void isCleanupParametersReturnsValueWhenSet() {
        ReflectionTestUtils.setField(cmd, "cleanupParameters", Boolean.TRUE);
        assertTrue(cmd.isCleanupParameters());
    }

    @Test
    public void successMessageReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "successMessage", null);
        assertNull(cmd.getSuccessMessage());
    }

    @Test
    public void successMessageReturnsValueWhenSet() {
        String msg = "Success!";
        ReflectionTestUtils.setField(cmd, "successMessage", msg);
        assertEquals(msg, cmd.getSuccessMessage());
    }

    @Test
    public void errorMessageReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "errorMessage", null);
        assertNull(cmd.getErrorMessage());
    }

    @Test
    public void errorMessageReturnsValueWhenSet() {
        String msg = "Error!";
        ReflectionTestUtils.setField(cmd, "errorMessage", msg);
        assertEquals(msg, cmd.getErrorMessage());
    }

    @Test
    public void timeoutReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "timeout", null);
        assertNull(cmd.getTimeout());
    }

    @Test
    public void timeoutReturnsValueWhenSet() {
        Integer timeout = 10;
        ReflectionTestUtils.setField(cmd, "timeout", timeout);
        assertEquals(timeout, cmd.getTimeout());
    }

    @Test
    public void isEnabledReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "enabled", null);
        assertNull(cmd.isEnabled());
    }

    @Test
    public void isEnabledReturnsValueWhenSet() {
        ReflectionTestUtils.setField(cmd, "enabled", Boolean.FALSE);
        assertFalse(cmd.isEnabled());
    }

    @Test
    public void detailsReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "details", null);
        assertTrue(MapUtils.isEmpty(cmd.getDetails()));
    }

    @Test
    public void detailsReturnsValueWhenSet() {
        Map<String, Map<String, String>> details = new HashMap<>();
        Map<String, String> inner = new HashMap<>();
        inner.put("vendor", "acme");
        details.put("details", inner);
        ReflectionTestUtils.setField(cmd, "details", details);
        assertTrue(MapUtils.isNotEmpty(cmd.getDetails()));
    }

    @Test
    public void isCleanupDetailsReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "cleanupDetails", null);
        assertNull(cmd.isCleanupDetails());
    }

    @Test
    public void isCleanupDetailsReturnsValueWhenSet() {
        ReflectionTestUtils.setField(cmd, "cleanupDetails", Boolean.TRUE);
        assertTrue(cmd.isCleanupDetails());
    }

    @Test
    public void executeSetsCustomActionResponseWhenManagerSucceeds() {
        ExtensionCustomAction customAction = mock(ExtensionCustomAction.class);
        ExtensionCustomActionResponse response = mock(ExtensionCustomActionResponse.class);
        when(extensionsManager.updateCustomAction(cmd)).thenReturn(customAction);
        when(extensionsManager.createCustomActionResponse(customAction)).thenReturn(response);

        doNothing().when(cmd).setResponseObject(any());

        cmd.execute();

        verify(extensionsManager).updateCustomAction(cmd);
        verify(extensionsManager).createCustomActionResponse(customAction);
        verify(response).setResponseName(cmd.getCommandName());
        verify(cmd).setResponseObject(response);
    }

    @Test
    public void executeThrowsServerApiExceptionWhenManagerFails() {
        when(extensionsManager.updateCustomAction(cmd))
                .thenThrow(new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update custom action"));

        try {
            cmd.execute();
            fail("Expected ServerApiException");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Failed to update custom action", e.getDescription());
        }
    }
}
