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
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class DeleteObjectStoragePoolCmdTest {
    @Mock
    private StorageService storageService;

    @Spy
    DeleteObjectStoragePoolCmd deleteObjectStoragePoolCmd;

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        deleteObjectStoragePoolCmd = Mockito.spy(new DeleteObjectStoragePoolCmd());
        deleteObjectStoragePoolCmd._storageService = storageService;
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
        closeable.close();
    }

    @Test
    public void testDeleteObjectStore()  {
        Mockito.doReturn(true).when(storageService).deleteObjectStore(deleteObjectStoragePoolCmd);
        deleteObjectStoragePoolCmd.execute();
        Mockito.verify(storageService, Mockito.times(1))
                .deleteObjectStore(deleteObjectStoragePoolCmd);
    }
}
