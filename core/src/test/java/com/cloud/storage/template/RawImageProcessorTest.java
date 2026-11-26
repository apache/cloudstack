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
package com.cloud.storage.template;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.StorageLayer;

@RunWith(MockitoJUnitRunner.class)
public class RawImageProcessorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    StorageLayer storage;

    private RawImageProcessor processor;

    @Before
    public void setUp() throws Exception {
        processor = new RawImageProcessor();
        Map<String, Object> params = new HashMap<>();
        params.put(StorageLayer.InstanceConfigKey, storage);
        processor.configure("Raw Image Processor", params);
    }

    private File image(byte[] header) throws Exception {
        File file = folder.newFile("template.raw");
        byte[] content = new byte[4096];
        System.arraycopy(header, 0, content, 0, header.length);
        Files.write(file.toPath(), content);
        Mockito.when(storage.exists(file.getPath())).thenReturn(true);
        Mockito.lenient().when(storage.getSize(file.getPath())).thenReturn(file.length());
        return file;
    }

    @Test
    public void processAcceptsRawImage() throws Exception {
        image(new byte[] {(byte) 0xeb, 0x63, (byte) 0x90});

        Processor.FormatInfo info = processor.process(folder.getRoot().getPath(), null, "template");

        Assert.assertEquals(4096L, info.virtualSize);
    }

    @Test(expected = InternalErrorException.class)
    public void processRejectsQcow2RegisteredAsRaw() throws Exception {
        image(new byte[] {'Q', 'F', 'I', (byte) 0xfb, 0, 0, 0, 3});

        processor.process(folder.getRoot().getPath(), null, "template");
    }
}
