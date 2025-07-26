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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.commons.collections.MapUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class UpdateExtensionCmdTest {

    private UpdateExtensionCmd cmd;
    private ExtensionsManager extensionsManager;

    @Before
    public void setUp() {
        cmd = Mockito.spy(new UpdateExtensionCmd());
        extensionsManager = mock(ExtensionsManager.class);
        ReflectionTestUtils.setField(cmd, "extensionsManager", extensionsManager);
    }

    @Test
    public void idReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "id", null);
        assertNull(cmd.getId());
    }

    @Test
    public void idReturnsValueWhenSet() {
        Long id = 12345L;
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
        String description = "Extension description";
        ReflectionTestUtils.setField(cmd, "description", description);
        assertEquals(description, cmd.getDescription());
    }

    @Test
    public void orchestratorRequiresPrepareVmReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "orchestratorRequiresPrepareVm", null);
        assertNull(cmd.isOrchestratorRequiresPrepareVm());
    }

    @Test
    public void orchestratorRequiresPrepareVmReturnsValueWhenSet() {
        ReflectionTestUtils.setField(cmd, "orchestratorRequiresPrepareVm", Boolean.TRUE);
        assertTrue(cmd.isOrchestratorRequiresPrepareVm());
    }

    @Test
    public void stateReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "state", null);
        assertNull(cmd.getState());
    }

    @Test
    public void stateReturnsValueWhenSet() {
        String state = "Active";
        ReflectionTestUtils.setField(cmd, "state", state);
        assertEquals(state, cmd.getState());
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
    public void executeSetsExtensionResponseWhenManagerSucceeds() {
        Extension extension = mock(Extension.class);
        ExtensionResponse response = mock(ExtensionResponse.class);
        when(extensionsManager.updateExtension(cmd)).thenReturn(extension);
        when(extensionsManager.createExtensionResponse(extension, EnumSet.of(ApiConstants.ExtensionDetails.all)))
                .thenReturn(response);

        doNothing().when(cmd).setResponseObject(any());

        cmd.execute();

        verify(extensionsManager).updateExtension(cmd);
        verify(extensionsManager).createExtensionResponse(extension, EnumSet.of(ApiConstants.ExtensionDetails.all));
        verify(response).setResponseName(cmd.getCommandName());
        verify(cmd).setResponseObject(response);
    }

    @Test
    public void executeThrowsServerApiExceptionWhenManagerFails() {
        when(extensionsManager.updateExtension(cmd))
                .thenThrow(new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update extension"));

        try {
            cmd.execute();
            fail("Expected ServerApiException");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Failed to update extension", e.getDescription());
        }
    }
}
