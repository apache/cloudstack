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

package org.apache.cloudstack.framework.extensions.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.extension.Extension;
import org.junit.Test;

public class ExtensionBaseCommandTest {

    @Test
    public void extensionIdReturnsCorrectValue() {
        Extension extension = mock(Extension.class);
        when(extension.getId()).thenReturn(12345L);
        ExtensionBaseCommand command = new ExtensionBaseCommand(extension);
        assertEquals(12345L, command.getExtensionId());
    }

    @Test
    public void extensionNameReturnsCorrectValue() {
        Extension extension = mock(Extension.class);
        when(extension.getName()).thenReturn("TestExtension");
        ExtensionBaseCommand command = new ExtensionBaseCommand(extension);
        assertEquals("TestExtension", command.getExtensionName());
    }

    @Test
    public void extensionUserDefinedReturnsTrueWhenSet() {
        Extension extension = mock(Extension.class);
        when(extension.isUserDefined()).thenReturn(true);
        ExtensionBaseCommand command = new ExtensionBaseCommand(extension);
        assertTrue(command.isExtensionUserDefined());
    }

    @Test
    public void extensionRelativePathReturnsCorrectValue() {
        Extension extension = mock(Extension.class);
        when(extension.getRelativePath()).thenReturn("/entry/point");
        ExtensionBaseCommand command = new ExtensionBaseCommand(extension);
        assertEquals("/entry/point", command.getExtensionRelativePath());
    }

    @Test
    public void extensionStateReturnsCorrectValue() {
        Extension extension = mock(Extension.class);
        Extension.State state = Extension.State.Enabled;
        when(extension.getState()).thenReturn(state);
        ExtensionBaseCommand command = new ExtensionBaseCommand(extension);
        assertEquals(state, command.getExtensionState());
    }

    @Test
    public void executeInSequenceReturnsFalse() {
        Extension extension = mock(Extension.class);
        ExtensionBaseCommand command = new ExtensionBaseCommand(extension);
        assertFalse(command.executeInSequence());
    }
}
