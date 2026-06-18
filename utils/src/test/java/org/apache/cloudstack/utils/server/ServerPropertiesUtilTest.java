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

package org.apache.cloudstack.utils.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.PropertiesUtil;

@RunWith(MockitoJUnitRunner.class)
public class ServerPropertiesUtilTest {

    @After
    public void clearCache() {
        ServerPropertiesUtil.propertiesRef.set(null);
    }

    @Test
    public void returnsPropertyValueWhenPropertiesAreLoaded() {
        Properties mockProperties = mock(Properties.class);
        when(mockProperties.getProperty("key")).thenReturn("value");
        ServerPropertiesUtil.propertiesRef.set(mockProperties);
        String result = ServerPropertiesUtil.getProperty("key");
        assertEquals("value", result);
    }

    @Test
    public void returnsNullWhenPropertyDoesNotExist() {
        Properties mockProperties = mock(Properties.class);
        ServerPropertiesUtil.propertiesRef.set(mockProperties);
        assertNull(ServerPropertiesUtil.getProperty("nonexistentKey"));
    }

    @Test
    public void loadsPropertiesFromFileWhenNotCached() throws Exception {
        File tempFile = Files.createTempFile("server", ".properties").toFile();
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "key=value\n");
        try (MockedStatic<PropertiesUtil> mocked = mockStatic(PropertiesUtil.class)) {
            mocked.when(() -> PropertiesUtil.findConfigFile(ServerPropertiesUtil.PROPERTIES_FILE))
                    .thenReturn(tempFile);
            assertEquals("value", ServerPropertiesUtil.getProperty("key"));
        }
    }

    @Test
    public void returnsNullWhenPropertiesFileNotFound() {
        try (MockedStatic<PropertiesUtil> mocked = mockStatic(PropertiesUtil.class)) {
            mocked.when(() -> PropertiesUtil.findConfigFile(ServerPropertiesUtil.PROPERTIES_FILE))
                    .thenReturn(null);
            assertNull(ServerPropertiesUtil.getProperty("key"));
        }
    }

    @Test
    public void returnsNullWhenIOExceptionOccurs() throws IOException {
        File tempFile = Files.createTempFile("bad", ".properties").toFile();
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "\u0000\u0000\u0000");
        try (MockedStatic<PropertiesUtil> mocked = mockStatic(PropertiesUtil.class)) {
            mocked.when(() -> PropertiesUtil.findConfigFile(ServerPropertiesUtil.PROPERTIES_FILE))
                    .thenReturn(tempFile);
            assertNull(ServerPropertiesUtil.getProperty("key"));
        }
    }
}
