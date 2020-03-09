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
package com.cloud.hypervisor.kvm.storage;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import javax.naming.ConfigurationException;

import com.cloud.utils.script.Script;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({ Script.class })
@RunWith(PowerMockRunner.class)
public class KVMStorageProcessorTest {

    @Mock
    KVMStoragePoolManager storagePoolManager;
    @Mock
    LibvirtComputingResource resource;

    @InjectMocks
    private KVMStorageProcessor storageProcessor;

    private static final String directDownloadTemporaryPath = "/var/lib/libvirt/images/dd";
    private static final long templateSize = 80000L;

    @Before
    public void setUp() throws ConfigurationException {
        MockitoAnnotations.initMocks(this);
        storageProcessor = new KVMStorageProcessor(storagePoolManager, resource);
        PowerMockito.mockStatic(Script.class);
        Mockito.when(resource.getDirectDownloadTemporaryDownloadPath()).thenReturn(directDownloadTemporaryPath);
    }

    @Test
    public void testIsEnoughSpaceForDownloadTemplateOnTemporaryLocationAssumeEnoughSpaceWhenNotProvided() {
        boolean result = storageProcessor.isEnoughSpaceForDownloadTemplateOnTemporaryLocation(null);
        Assert.assertTrue(result);
    }

    @Test
    public void testIsEnoughSpaceForDownloadTemplateOnTemporaryLocationNotEnoughSpace() {
        String output = String.valueOf(templateSize - 30000L);
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).thenReturn(output);
        boolean result = storageProcessor.isEnoughSpaceForDownloadTemplateOnTemporaryLocation(templateSize);
        Assert.assertFalse(result);
    }

    @Test
    public void testIsEnoughSpaceForDownloadTemplateOnTemporaryLocationEnoughSpace() {
        String output = String.valueOf(templateSize + 30000L);
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).thenReturn(output);
        boolean result = storageProcessor.isEnoughSpaceForDownloadTemplateOnTemporaryLocation(templateSize);
        Assert.assertTrue(result);
    }

    @Test
    public void testIsEnoughSpaceForDownloadTemplateOnTemporaryLocationNotExistingLocation() {
        String output = String.format("df: ‘%s’: No such file or directory", directDownloadTemporaryPath);
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).thenReturn(output);
        boolean result = storageProcessor.isEnoughSpaceForDownloadTemplateOnTemporaryLocation(templateSize);
        Assert.assertFalse(result);
    }
}
