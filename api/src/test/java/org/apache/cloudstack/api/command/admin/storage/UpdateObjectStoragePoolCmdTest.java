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

import com.cloud.storage.StorageService;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.object.ObjectStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;

public class UpdateObjectStoragePoolCmdTest {

    @Mock
    private StorageService storageService;

    @Spy
    UpdateObjectStoragePoolCmd updateObjectStoragePoolCmd;

    @Mock
    ObjectStore objectStore;

    @Mock
    ResponseGenerator responseGenerator;

    private String name = "testObjStore";

    private String url = "testURL";

    private String provider = "Simulator";

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        updateObjectStoragePoolCmd = Mockito.spy(new UpdateObjectStoragePoolCmd());
        updateObjectStoragePoolCmd._storageService = storageService;
        updateObjectStoragePoolCmd._responseGenerator = responseGenerator;
        ReflectionTestUtils.setField(updateObjectStoragePoolCmd, "name", name);
        ReflectionTestUtils.setField(updateObjectStoragePoolCmd, "url", url);
        ReflectionTestUtils.setField(updateObjectStoragePoolCmd, "id", 1L);
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
        closeable.close();
    }

    @Test
    public void testUpdateObjectStore() {
        Mockito.doReturn(objectStore).when(storageService).updateObjectStore(1L, updateObjectStoragePoolCmd);
        ObjectStoreResponse objectStoreResponse = new ObjectStoreResponse();
        Mockito.doReturn(objectStoreResponse).when(responseGenerator).createObjectStoreResponse(any());
        updateObjectStoragePoolCmd.execute();
        Mockito.verify(storageService, Mockito.times(1))
                .updateObjectStore(1L, updateObjectStoragePoolCmd);
    }

}
