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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class RegisterExtensionCmdTest {

    private RegisterExtensionCmd cmd;
    private ExtensionsManager extensionsManager;

    @Before
    public void setUp() {
        cmd = Mockito.spy(new RegisterExtensionCmd());
        extensionsManager = mock(ExtensionsManager.class);
        ReflectionTestUtils.setField(cmd, "extensionsManager", extensionsManager);
    }

    @Test
    public void extensionIdReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "extensionId", null);
        assertNull(cmd.getExtensionId());
    }

    @Test
    public void extensionIdReturnsValueWhenSet() {
        Long extensionId = 12345L;
        ReflectionTestUtils.setField(cmd, "extensionId", extensionId);
        assertEquals(extensionId, cmd.getExtensionId());
    }

    @Test
    public void resourceIdReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "resourceId", null);
        assertNull(cmd.getResourceId());
    }

    @Test
    public void resourceIdReturnsValueWhenSet() {
        String resourceId = "resource-123";
        ReflectionTestUtils.setField(cmd, "resourceId", resourceId);
        assertEquals(resourceId, cmd.getResourceId());
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
    public void detailsReturnsEmptyMapWhenUnset() {
        ReflectionTestUtils.setField(cmd, "details", null);
        Map<String, String> details = cmd.getDetails();
        assertNotNull(details);
        assertTrue(details.isEmpty());
    }

    @Test
    public void executeSetsExtensionResponseWhenManagerSucceeds() {
        Extension extension = mock(Extension.class);
        ExtensionResponse response = mock(ExtensionResponse.class);
        when(extensionsManager.registerExtensionWithResource(cmd)).thenReturn(extension);
        when(extensionsManager.createExtensionResponse(extension, EnumSet.of(ApiConstants.ExtensionDetails.all)))
                .thenReturn(response);

        doNothing().when(cmd).setResponseObject(any());

        cmd.execute();

        verify(extensionsManager).registerExtensionWithResource(cmd);
        verify(extensionsManager).createExtensionResponse(extension, EnumSet.of(ApiConstants.ExtensionDetails.all));
        verify(cmd).setResponseObject(response);
    }

    @Test
    public void executeThrowsServerApiExceptionWhenManagerFails() {
        when(extensionsManager.registerExtensionWithResource(cmd))
                .thenThrow(new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to register extension"));

        try {
            cmd.execute();
            fail("Expected ServerApiException");
        } catch (ServerApiException e) {
            assertEquals("Failed to register extension", e.getDescription());
        }
    }
}
