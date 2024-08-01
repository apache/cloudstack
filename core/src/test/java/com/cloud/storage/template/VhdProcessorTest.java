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
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;


import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageLayer;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VhdProcessorTest {
    VhdProcessor processor;

    @Mock
    StorageLayer mockStorageLayer;

    @Before
    public void setUp() throws Exception {
        processor = Mockito.spy(new VhdProcessor());
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
        Mockito.doThrow(new IOException("virtual size calculation failed")).when(processor).getTemplateVirtualSize((File) Mockito.any());

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
        Mockito.doReturn(virtualSize).when(processor).getTemplateVirtualSize((File) Mockito.any());

        Processor.FormatInfo info = processor.process(templatePath, null, templateName);
        Assert.assertEquals(Storage.ImageFormat.VHD, info.format);
        Assert.assertEquals(actualSize, info.size);
        Assert.assertEquals(virtualSize, info.virtualSize);
        Assert.assertEquals(templateName + ".vhd", info.filename);
    }

    @Test
    public void testGetVirtualSizeWhenVirtualSizeThrowsException() throws Exception {
        long virtualSize = 2000;
        long actualSize = 1000;
        File mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.length()).thenReturn(actualSize);
        Mockito.doThrow(new IOException("virtual size calculation failed")).when(processor).getTemplateVirtualSize((File) Mockito.any());
        Assert.assertEquals(actualSize, processor.getVirtualSize(mockFile));
        Mockito.verify(mockFile,Mockito.times(1)).length();
    }

    @Test
    public void testGetVirtualSize() throws Exception{
        long virtualSize = 2000;
        long actualSize = 1000;
        File mockFile = Mockito.mock(File.class);
        Mockito.doReturn(virtualSize).when(processor).getTemplateVirtualSize((File) Mockito.any());
        Assert.assertEquals(virtualSize, processor.getVirtualSize(mockFile));
        Mockito.verify(mockFile,Mockito.times(0)).length();
    }

    @Test
    public void testVhdGetVirtualSize() throws Exception {
        String vhdPath = URLDecoder.decode(getClass().getResource("/vhds/test.vhd").getFile(), Charset.defaultCharset().name());
        long expectedVirtualSizeBytes = 104857600;
        long actualVirtualSizeBytes = processor.getVirtualSize(new File(vhdPath));
        Assert.assertEquals(expectedVirtualSizeBytes, actualVirtualSizeBytes);
    }

    @Test
    public void testGzipVhdGetVirtualSize() throws Exception {
        String gzipVhdPath = URLDecoder.decode(getClass().getResource("/vhds/test.vhd.gz").getFile(), Charset.defaultCharset().name());
        long expectedVirtualSizeBytes = 104857600;
        long actualVirtualSizeBytes = processor.getVirtualSize(new File(gzipVhdPath));
        Assert.assertEquals(expectedVirtualSizeBytes, actualVirtualSizeBytes);
    }

    @Test
    public void testBzip2VhdGetVirtualSize() throws Exception {
        String bzipVhdPath = URLDecoder.decode(getClass().getResource("/vhds/test.vhd.bz2").getFile(), Charset.defaultCharset().name());
        long expectedVirtualSizeBytes = 104857600;
        long actualVirtualSizeBytes = processor.getVirtualSize(new File(bzipVhdPath));
        Assert.assertEquals(expectedVirtualSizeBytes, actualVirtualSizeBytes);
    }

    @Test
    public void testZipVhdGetVirtualSize() throws Exception {
        String zipVhdPath = URLDecoder.decode(getClass().getResource("/vhds/test.vhd.zip").getFile(), Charset.defaultCharset().name());
        long expectedVirtualSizeBytes = 341; //Zip is not a supported format, virtual size should return the filesize as a fallback
        long actualVirtualSizeBytes = processor.getVirtualSize(new File(zipVhdPath));
        Assert.assertEquals(expectedVirtualSizeBytes, actualVirtualSizeBytes);
    }
}
