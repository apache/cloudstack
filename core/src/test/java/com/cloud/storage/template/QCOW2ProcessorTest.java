/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.storage.template;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageLayer;

@RunWith(PowerMockRunner.class)
@PrepareForTest(QCOW2Processor.class)
public class QCOW2ProcessorTest {
    QCOW2Processor processor;

    @Mock
    StorageLayer mockStorageLayer;

    @Before
    public void setUp() throws Exception {
        processor = Mockito.spy(new QCOW2Processor());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(StorageLayer.InstanceConfigKey, mockStorageLayer);
        processor.configure("VHD Processor", params);
    }

    @Test(expected = InternalErrorException.class)
    public void testProcessWhenVirtualSizeThrowsException() throws Exception {
        String templatePath = "/tmp";
        String templateName = "template";

        Mockito.when(mockStorageLayer.exists(Mockito.anyString())).thenReturn(true);
        File mockFile = Mockito.mock(File.class);

        Mockito.when(mockStorageLayer.getFile(Mockito.anyString())).thenReturn(mockFile);
        Mockito.when(mockStorageLayer.getSize(Mockito.anyString())).thenReturn(1000L);
        Mockito.doThrow(new IOException("virtual size calculation failed")).when(processor).getTemplateVirtualSize((File)Mockito.any());

        processor.process(templatePath, null, templateName);
    }

    @Test
    public void testProcess() throws Exception {
        String templatePath = "/tmp";
        String templateName = "template";
        long virtualSize = 2000;
        long actualSize = 1000;

        Mockito.when(mockStorageLayer.exists(Mockito.anyString())).thenReturn(true);
        File mockFile = Mockito.mock(File.class);

        Mockito.when(mockStorageLayer.getFile(Mockito.anyString())).thenReturn(mockFile);
        Mockito.when(mockStorageLayer.getSize(Mockito.anyString())).thenReturn(actualSize);
        Mockito.doReturn(virtualSize).when(processor).getTemplateVirtualSize((File)Mockito.any());

        Processor.FormatInfo info = processor.process(templatePath, null, templateName);
        Assert.assertEquals(Storage.ImageFormat.QCOW2, info.format);
        Assert.assertEquals(actualSize, info.size);
        Assert.assertEquals(virtualSize, info.virtualSize);
        Assert.assertEquals(templateName + ".qcow2", info.filename);
    }

    @Test
    public void testGetVirtualSizeWhenVirtualSizeThrowsException() throws Exception {
        long virtualSize = 2000;
        long actualSize = 1000;
        File mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.length()).thenReturn(actualSize);
        Mockito.doThrow(new IOException("virtual size calculation failed")).when(processor).getTemplateVirtualSize((File)Mockito.any());
        Assert.assertEquals(actualSize, processor.getVirtualSize(mockFile));
        Mockito.verify(mockFile, Mockito.times(1)).length();
    }

    @Test
    public void testGetVirtualSize() throws Exception {
        long virtualSize = 2000;
        long actualSize = 1000;
        File mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.length()).thenReturn(actualSize);
        Mockito.doReturn(virtualSize).when(processor).getTemplateVirtualSize((File)Mockito.any());
        Assert.assertEquals(virtualSize, processor.getVirtualSize(mockFile));
        Mockito.verify(mockFile, Mockito.times(0)).length();
    }
}
