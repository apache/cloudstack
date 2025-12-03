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

package org.apache.cloudstack.api.command.admin.storage;

import com.cloud.utils.Pair;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.storage.browser.StorageBrowser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownloadImageStoreObjectCmdTest {

    @Mock
    private StorageBrowser storageBrowser;

    @InjectMocks
    @Spy
    private DownloadImageStoreObjectCmd cmd;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testExecute() throws Exception {
        ReflectionTestUtils.setField(cmd, "storeId", 1L);
        ReflectionTestUtils.setField(cmd, "path", "path/to/object");
        ExtractResponse response = mock(ExtractResponse.class);
        when(storageBrowser.downloadImageStoreObject(cmd)).thenReturn(response);

        cmd.execute();

        verify(storageBrowser).downloadImageStoreObject(cmd);
        verify(response).setResponseName("downloadImageStoreObjectResponse".toLowerCase());
        verify(response).setObjectName("downloadImageStoreObjectResponse".toLowerCase());
        verify(cmd).setResponseObject(response);
    }

    @Test
    public void testGetPath() {
        List<Pair<String, String>> pair = List.of(
                new Pair<>("", null),
                new Pair<>("", ""),
                new Pair<>("", "/"),
                new Pair<>("etc", "etc"),
                new Pair<>("etc", "/etc"),
                new Pair<>("etc/passwd", "etc/passwd"),
                new Pair<>("etc/passwd", "//etc/passwd"),
                new Pair<>("", "/etc/passwd/../../.."),
                new Pair<>("etc/passwd", "../../etc/passwd"),
                new Pair<>("etc/passwd", "/../../etc/passwd"),
                new Pair<>("etc/passwd", ";../../../etc/passwd"),
                new Pair<>("etc/passwd", "///etc/passwd"),
                new Pair<>("etc/passwd", "/abc/xyz/../../../etc/passwd")
        );

        for (Pair<String, String> p : pair) {
            String expectedPath = p.first();
            String path = p.second();
            ReflectionTestUtils.setField(cmd, "path", path);
            Assert.assertEquals(expectedPath, cmd.getPath());
        }
    }

    @Test
    public void testGetEventType() {
        String eventType = cmd.getEventType();

        Assert.assertEquals("IMAGE.STORE.OBJECT.DOWNLOAD", eventType);
    }

    @Test
    public void testGetEventDescription() {
        ReflectionTestUtils.setField(cmd, "storeId", 1L);
        ReflectionTestUtils.setField(cmd, "path", "path/to/object");
        String eventDescription = cmd.getEventDescription();

        Assert.assertEquals("Downloading object at path path/to/object on image store 1", eventDescription);
    }
}
