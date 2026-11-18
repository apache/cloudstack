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

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class DeleteExtensionCmdTest {

    private DeleteExtensionCmd cmd;
    private ExtensionsManager extensionsManager;

    @Before
    public void setUp() {
        cmd = Mockito.spy(new DeleteExtensionCmd());
        extensionsManager = Mockito.mock(ExtensionsManager.class);
        cmd.extensionsManager = extensionsManager;
    }

    @Test
    public void getIdReturnsNullWhenUnset() {
        ReflectionTestUtils.setField(cmd, "id", null);
        assertNull(cmd.getId());
    }

    @Test
    public void getIdReturnsValueWhenSet() {
        Long id = 12345L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void isCleanupReturnsFalseWhenUnset() {
        ReflectionTestUtils.setField(cmd, "cleanup", null);
        assertFalse(cmd.isCleanup());
    }

    @Test
    public void isCleanupReturnsTrueWhenSetTrue() {
        ReflectionTestUtils.setField(cmd, "cleanup", true);
        assertTrue(cmd.isCleanup());
    }

    @Test
    public void executeSetsSuccessResponseWhenManagerReturnsTrue() {
        Mockito.when(extensionsManager.deleteExtension(cmd)).thenReturn(true);
        Mockito.doNothing().when(cmd).setResponseObject(Mockito.any());
        cmd.execute();
        Mockito.verify(extensionsManager).deleteExtension(cmd);
        Mockito.verify(cmd).setResponseObject(Mockito.any(SuccessResponse.class));
    }

    @Test
    public void executeThrowsServerApiExceptionWhenManagerReturnsFalse() {
        Mockito.when(extensionsManager.deleteExtension(cmd)).thenReturn(false);
        try {
            cmd.execute();
            fail("Expected ServerApiException");
        } catch (ServerApiException e) {
            assertEquals("Failed to delete extension", e.getDescription());
        }
    }
}
