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

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.script.Script;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.xml.*", "java.xml.*", "javax.management.*", "org.apache.xerces.*"})
@PrepareForTest(OVAProcessor.class)
public class OVAProcessorTest {
    OVAProcessor processor;

    @Mock
    StorageLayer mockStorageLayer;

    @Before
    public void setUp() throws Exception {
        processor = PowerMockito.spy(new OVAProcessor());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(StorageLayer.InstanceConfigKey, mockStorageLayer);
        processor.configure("OVA Processor", params);
    }

    @Test(expected = InternalErrorException.class)
    public void testProcessWhenUntarFails() throws Exception {
        String templatePath = "/tmp";
        String templateName = "template";

        Mockito.when(mockStorageLayer.exists(Mockito.anyString())).thenReturn(true);

        Script mockScript = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(mockScript);
        PowerMockito.when(mockScript.execute()).thenReturn("error while untaring the file");

        processor.process(templatePath, null, templateName);
    }

    @Test(expected = InternalErrorException.class)
    public void testProcessWhenVirtualSizeThrowsException() throws Exception {
        String templatePath = "/tmp";
        String templateName = "template";

        Mockito.when(mockStorageLayer.exists(Mockito.anyString())).thenReturn(true);
        Mockito.when(mockStorageLayer.getSize(Mockito.anyString())).thenReturn(1000l);
        Mockito.doThrow(new InternalErrorException("virtual size calculation failed")).when(processor).getTemplateVirtualSize(Mockito.anyString(), Mockito.anyString());

        Script mockScript = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(mockScript);
        PowerMockito.when(mockScript.execute()).thenReturn(null);

        processor.process(templatePath, null, templateName);
    }

    @Test
    public void testProcess() throws Exception {
        String templatePath = "/tmp";
        String templateName = "template";
        long virtualSize = 2000;
        long actualSize = 1000;

        Mockito.when(mockStorageLayer.exists(Mockito.anyString())).thenReturn(true);
        Mockito.when(mockStorageLayer.getSize(Mockito.anyString())).thenReturn(actualSize);
        Mockito.doReturn(virtualSize).when(processor).getTemplateVirtualSize(Mockito.anyString(), Mockito.anyString());

        Script mockScript = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(mockScript);
        PowerMockito.when(mockScript.execute()).thenReturn(null);

        Processor.FormatInfo info = processor.process(templatePath, null, templateName);
        Assert.assertEquals(Storage.ImageFormat.OVA, info.format);
        Assert.assertEquals("actual size:", actualSize, info.size);
        Assert.assertEquals("virtual size:", virtualSize, info.virtualSize);
        Assert.assertEquals("template name:", templateName + ".ova", info.filename);
    }

    @Test
    public void testGetVirtualSizeWhenVirtualSizeThrowsException() throws Exception {
        long virtualSize = 2000;
        long actualSize = 1000;
        String templatePath = "/tmp";
        String templateName = "template";
        File mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.length()).thenReturn(actualSize);
        Mockito.when(mockFile.getParent()).thenReturn(templatePath);
        Mockito.when(mockFile.getName()).thenReturn(templateName);
        Mockito.doThrow(new InternalErrorException("virtual size calculation failed")).when(processor).getTemplateVirtualSize(templatePath, templateName);
        Assert.assertEquals(actualSize, processor.getVirtualSize(mockFile));
        Mockito.verify(mockFile, Mockito.times(1)).length();
    }

    @Test
    public void testGetVirtualSize() throws Exception {
        long virtualSize = 2000;
        long actualSize = 1000;
        String templatePath = "/tmp";
        String templateName = "template";
        File mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.length()).thenReturn(actualSize);
        Mockito.when(mockFile.getParent()).thenReturn(templatePath);
        Mockito.when(mockFile.getName()).thenReturn(templateName);
        Mockito.doReturn(virtualSize).when(processor).getTemplateVirtualSize(templatePath, templateName);
        Assert.assertEquals(virtualSize, processor.getVirtualSize(mockFile));
        Mockito.verify(mockFile, Mockito.times(0)).length();
    }

}
