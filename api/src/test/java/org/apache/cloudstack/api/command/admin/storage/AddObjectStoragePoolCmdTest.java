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
package org.apache.cloudstack.api.command.admin.storage;

import com.cloud.exception.DiscoveryException;
import com.cloud.storage.StorageService;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.object.ObjectStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class AddObjectStoragePoolCmdTest {

    @Mock
    StorageService storageService;

    @Mock
    ObjectStore objectStore;

    @Mock
    ResponseGenerator responseGenerator;

    @Spy
    AddObjectStoragePoolCmd addObjectStoragePoolCmdSpy;

    String name = "testObjStore";

    String url = "testURL";

    String provider = "Simulator";

    Map<String, String> details;

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        details = new HashMap<>();
        addObjectStoragePoolCmdSpy = Mockito.spy(new AddObjectStoragePoolCmd());
        ReflectionTestUtils.setField(addObjectStoragePoolCmdSpy, "name", name);
        ReflectionTestUtils.setField(addObjectStoragePoolCmdSpy, "url", url);
        ReflectionTestUtils.setField(addObjectStoragePoolCmdSpy, "providerName", provider);
        ReflectionTestUtils.setField(addObjectStoragePoolCmdSpy, "details", details);
        addObjectStoragePoolCmdSpy._storageService = storageService;
        addObjectStoragePoolCmdSpy._responseGenerator = responseGenerator;
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
        closeable.close();
    }

    @Test
    public void testAddObjectStore() throws DiscoveryException {
        Mockito.doReturn(objectStore).when(storageService).discoverObjectStore(Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any());
        ObjectStoreResponse objectStoreResponse = new ObjectStoreResponse();
        Mockito.doReturn(objectStoreResponse).when(responseGenerator).createObjectStoreResponse(any());
        addObjectStoragePoolCmdSpy.execute();

        Mockito.verify(storageService, Mockito.times(1))
                .discoverObjectStore(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
