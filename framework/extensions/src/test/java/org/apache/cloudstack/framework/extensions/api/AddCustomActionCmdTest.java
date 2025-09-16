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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.commons.collections.MapUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.user.Account;

@RunWith(MockitoJUnitRunner.class)
public class AddCustomActionCmdTest {

    private AddCustomActionCmd cmd;
    private ExtensionsManager extensionsManager;

    @Before
    public void setUp() throws Exception {
        cmd = new AddCustomActionCmd();
        extensionsManager = mock(ExtensionsManager.class);
        cmd.extensionsManager = extensionsManager;
    }

    private void setField(String fieldName, Object value) {
        ReflectionTestUtils.setField(cmd, fieldName, value);
    }

    @Test
    public void testGetters() {
        Long extensionId = 42L;
        String name = "actionName";
        String description = "desc";
        String resourceType = "VM";
        List<String> allowedRoleTypes = Arrays.asList(RoleType.Admin.name(), RoleType.User.name());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "param1");
        String successMessage = "Success!";
        String errorMessage = "Error!";
        Integer timeout = 10;
        Boolean enabled = true;
        Map<String, Map<String, String>> details = new HashMap<>();
        Map<String, String> inner = new HashMap<>();
        inner.put("vendor", "acme");
        details.put("details", inner);
        setField("details", details);

        setField("extensionId", extensionId);
        setField("name", name);
        setField("description", description);
        setField("resourceType", resourceType);
        setField("allowedRoleTypes", allowedRoleTypes);
        setField("parameters", parameters);
        setField("successMessage", successMessage);
        setField("errorMessage", errorMessage);
        setField("timeout", timeout);
        setField("enabled", enabled);
        setField("details", details);

        assertEquals(extensionId, cmd.getExtensionId());
        assertEquals(name, cmd.getName());
        assertEquals(description, cmd.getDescription());
        assertEquals(resourceType, cmd.getResourceType());
        assertEquals(allowedRoleTypes, cmd.getAllowedRoleTypes());
        assertEquals(parameters, cmd.getParametersMap());
        assertEquals(successMessage, cmd.getSuccessMessage());
        assertEquals(errorMessage, cmd.getErrorMessage());
        assertEquals(timeout, cmd.getTimeout());
        assertTrue(cmd.isEnabled());
        assertTrue(MapUtils.isNotEmpty(cmd.getDetails()));
    }

    @Test
    public void testIsEnabledReturnsFalseWhenNull() {
        setField("enabled", null);
        assertFalse(cmd.isEnabled());
    }

    @Test
    public void testIsEnabledReturnsFalseWhenFalse() {
        setField("enabled", Boolean.FALSE);
        assertFalse(cmd.isEnabled());
    }

    @Test
    public void testIsEnabledReturnsTrueWhenTrue() {
        setField("enabled", Boolean.TRUE);
        assertTrue(cmd.isEnabled());
    }

    @Test
    public void testGetAllowedRoleTypesReturnsNullWhenUnset() {
        setField("allowedRoleTypes", null);
        assertNull(cmd.getAllowedRoleTypes());
    }

    @Test
    public void testGetAllowedRoleTypesReturnsEmptyList() {
        setField("allowedRoleTypes", Collections.emptyList());
        assertEquals(0, cmd.getAllowedRoleTypes().size());
    }

    @Test
    public void testGetParametersMapReturnsNullWhenUnset() {
        setField("parameters", null);
        assertNull(cmd.getParametersMap());
    }

    @Test
    public void testGetParametersMapReturnsMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("foo", "bar");
        setField("parameters", parameters);
        assertEquals(parameters, cmd.getParametersMap());
    }

    @Test
    public void testGetDetailsReturnsNullWhenUnset() {
        setField("details", null);
        assertTrue(MapUtils.isEmpty(cmd.getDetails()));
    }

    @Test
    public void testGetDetailsReturnsMap() {
        Map<String, Map<String, String>> details = new HashMap<>();
        Map<String, String> inner = new HashMap<>();
        inner.put("key", "value");
        details.put("details", inner);
        setField("details", details);
        assertTrue(MapUtils.isNotEmpty(cmd.getDetails()));
    }

    @Test
    public void testGetDescriptionReturnsNullWhenUnset() {
        setField("description", null);
        assertNull(cmd.getDescription());
    }

    @Test
    public void testGetSuccessMessageReturnsNullWhenUnset() {
        setField("successMessage", null);
        assertNull(cmd.getSuccessMessage());
    }

    @Test
    public void testGetErrorMessageReturnsNullWhenUnset() {
        setField("errorMessage", null);
        assertNull(cmd.getErrorMessage());
    }

    @Test
    public void testGetTimeoutReturnsNullWhenUnset() {
        setField("timeout", null);
        assertNull(cmd.getTimeout());
    }

    @Test
    public void testExecuteCallsExtensionsManagerAndSetsResponse() {
        ExtensionCustomAction extensionCustomAction = mock(ExtensionCustomAction.class);
        ExtensionCustomActionResponse response = mock(ExtensionCustomActionResponse.class);

        when(extensionsManager.addCustomAction(any(AddCustomActionCmd.class))).thenReturn(extensionCustomAction);
        when(extensionsManager.createCustomActionResponse(extensionCustomAction)).thenReturn(response);

        AddCustomActionCmd spyCmd = spy(cmd);
        doNothing().when(spyCmd).setResponseObject(any());

        spyCmd.execute();

        verify(extensionsManager).addCustomAction(spyCmd);
        verify(extensionsManager).createCustomActionResponse(extensionCustomAction);
        verify(response).setResponseName(spyCmd.getCommandName());
        verify(spyCmd).setResponseObject(response);
    }

    @Test
    public void testGetEntityOwnerIdReturnsSystemAccount() {
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetApiResourceTypeReturnsExtensionCustomAction() {
        assertEquals(ApiCommandResourceType.ExtensionCustomAction, cmd.getApiResourceType());
    }
}
