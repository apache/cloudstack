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
package org.apache.cloudstack.api.command.test;


import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;

import java.util.Map;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.storage.AddImageStoreCmd;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.cloud.storage.ImageStore;
import com.cloud.storage.StorageService;

import junit.framework.TestCase;

public class AddSecondaryStorageCmdTest extends TestCase {

    private AddImageStoreCmd addImageStoreCmd;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    @Before
    public void setUp() {
        addImageStoreCmd = new AddImageStoreCmd();

    }

    @Test
    public void testExecuteForResult() throws Exception {

        StorageService resourceService = Mockito.mock(StorageService.class);
        addImageStoreCmd._storageService = resourceService;

        ImageStore store = Mockito.mock(ImageStore.class);

        Mockito.when(resourceService.discoverImageStore(isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(store);
        ResponseGenerator responseGenerator = Mockito.mock(ResponseGenerator.class);
        addImageStoreCmd._responseGenerator = responseGenerator;

        ImageStoreResponse responseHost = new ImageStoreResponse();
        responseHost.setName("Test");

        Mockito.doReturn(responseHost).when(responseGenerator).createImageStoreResponse(store);

        addImageStoreCmd.execute();

        Mockito.verify(responseGenerator).createImageStoreResponse(store);

        ImageStoreResponse actualResponse = (ImageStoreResponse)addImageStoreCmd.getResponseObject();
        assertEquals(responseHost, actualResponse);
        assertEquals("addimagestoreresponse", actualResponse.getResponseName());

    }

    @Test
    public void testExecuteForNullResult() throws Exception {

        StorageService resourceService = Mockito.mock(StorageService.class);
        addImageStoreCmd._storageService = resourceService;

        Mockito.when(resourceService.discoverImageStore(anyString(), anyString(), anyString(), anyLong(), (Map)anyObject()))
                .thenReturn(null);

        try {
            addImageStoreCmd.execute();
        } catch (ServerApiException exception) {
            assertEquals("Failed to add secondary storage", exception.getDescription());
        }

    }

}
