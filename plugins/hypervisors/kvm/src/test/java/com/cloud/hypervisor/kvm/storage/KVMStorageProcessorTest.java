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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class KVMStorageProcessorTest {

    @Mock
    KVMStoragePoolManager storagePoolManager;
    @Mock
    LibvirtComputingResource resource;

    private static final Long TEMPLATE_ID = 202l;
    private static final String EXPECTED_DIRECT_DOWNLOAD_DIR = "template/2/202";

    @Spy
    @InjectMocks
    private KVMStorageProcessor storageProcessor;

    @Before
    public void setUp() throws ConfigurationException {
        MockitoAnnotations.initMocks(this);
        storageProcessor = new KVMStorageProcessor(storagePoolManager, resource);
    }

    @Test
    public void testCloneVolumeFromBaseTemplate() throws Exception {

    }

    @Test
    public void testCopyVolumeFromImageCacheToPrimary() throws Exception {

    }
}
