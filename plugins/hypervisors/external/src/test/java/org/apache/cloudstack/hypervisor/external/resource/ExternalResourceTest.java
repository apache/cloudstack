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

package org.apache.cloudstack.hypervisor.external.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.apache.cloudstack.extension.Extension;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ExternalResourceTest {
    private ExternalResource externalResource;
    private Logger logger;

    @Before
    public void setUp() {
        externalResource = spy(new ExternalResource());
        logger = mock(Logger.class);
        ReflectionTestUtils.setField(externalResource, "logger", logger);
    }

    @Test
    public void isExtensionDisconnected_WhenExtensionNameAndPathAreBlank_ReturnsTrue() {
        ReflectionTestUtils.setField(externalResource, "extensionName", "");
        ReflectionTestUtils.setField(externalResource, "extensionRelativePath", "");
        boolean result = externalResource.isExtensionDisconnected();
        assertTrue(result);
    }

    @Test
    public void isExtensionDisconnected_WhenExtensionNameAndPathAreNotBlank_ReturnsFalse() {
        ReflectionTestUtils.setField(externalResource, "extensionName", "testExtension");
        ReflectionTestUtils.setField(externalResource, "extensionRelativePath", "path");
        boolean result = externalResource.isExtensionDisconnected();
        assertFalse(result);
    }

    @Test
    public void isExtensionNotEnabled_WhenExtensionStateIsEnabled_ReturnsFalse() {
        ReflectionTestUtils.setField(externalResource, "extensionState", Extension.State.Enabled);
        boolean result = externalResource.isExtensionNotEnabled();
        assertFalse(result);
    }

    @Test
    public void isExtensionNotEnabled_WhenExtensionStateIsNotEnabled_ReturnsTrue() {
        ReflectionTestUtils.setField(externalResource, "extensionState", Extension.State.Disabled);
        boolean result = externalResource.isExtensionNotEnabled();

        assertTrue(result);
    }

    @Test
    public void logAndGetExtensionNotConnectedOrDisabledError_WhenExtensionDisconnected_LogsErrorAndReturnsMessage() {
        ReflectionTestUtils.setField(externalResource, "extensionName", null);
        ReflectionTestUtils.setField(externalResource, "extensionRelativePath", null);
        String result = externalResource.logAndGetExtensionNotConnectedOrDisabledError();
        verify(logger).error("Extension not connected to host: {}", externalResource.getName());
        assertEquals("Extension not connected", result);
    }

    @Test
    public void logAndGetExtensionNotConnectedOrDisabledError_WhenExtensionDisabled_LogsErrorAndReturnsMessage() {
        ReflectionTestUtils.setField(externalResource, "extensionName", "testExtension");
        ReflectionTestUtils.setField(externalResource, "extensionRelativePath", "path");
        ReflectionTestUtils.setField(externalResource, "extensionState", Extension.State.Disabled);
        String result = externalResource.logAndGetExtensionNotConnectedOrDisabledError();
        verify(logger).error("Extension: {} connected to host: {} is not in Enabled state", "testExtension", externalResource.getName());
        assertEquals("Extension is disabled", result);
    }
}
