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
import static org.junit.Assert.fail;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DeleteCustomActionCmdTest {

    private DeleteCustomActionCmd cmd;
    private ExtensionsManager extensionsManager;

    @Before
    public void setUp() throws Exception {
        cmd = Mockito.spy(new DeleteCustomActionCmd());
        extensionsManager = Mockito.mock(ExtensionsManager.class);
        java.lang.reflect.Field field = DeleteCustomActionCmd.class.getDeclaredField("extensionsManager");
        field.setAccessible(true);
        field.set(cmd, extensionsManager);
    }

    @Test
    public void getIdReturnsNullWhenUnset() throws Exception {
        java.lang.reflect.Field field = DeleteCustomActionCmd.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(cmd, null);
        assertNull(cmd.getId());
    }

    @Test
    public void getIdReturnsValueWhenSet() throws Exception {
        Long id = 12345L;
        java.lang.reflect.Field field = DeleteCustomActionCmd.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(cmd, id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void executeSetsSuccessResponseWhenManagerReturnsTrue() throws Exception {
        Mockito.when(extensionsManager.deleteCustomAction(cmd)).thenReturn(true);
        Mockito.doNothing().when(cmd).setResponseObject(Mockito.any());
        cmd.execute();
        Mockito.verify(extensionsManager).deleteCustomAction(cmd);
        Mockito.verify(cmd).setResponseObject(Mockito.any(SuccessResponse.class));
    }

    @Test
    public void executeThrowsServerApiExceptionWhenManagerReturnsFalse() throws Exception {
        Mockito.when(extensionsManager.deleteCustomAction(cmd)).thenReturn(false);
        try {
            cmd.execute();
            fail("Expected ServerApiException");
        } catch (ServerApiException e) {
            assertEquals("Failed to delete extension custom action", e.getDescription());
        }
    }
}
