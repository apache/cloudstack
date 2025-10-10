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

package org.apache.cloudstack.framework.extensions.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

public class SyncExtensionCmdTest {

    @Test
    public void returnsIdWhenGetIdIsCalled() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        ReflectionTestUtils.setField(cmd, "id", 123L);
        assertEquals(Long.valueOf(123), cmd.getId());
    }
    
    @Test
    public void returnsSourceManagementServerIdWhenGetSourceManagementServerIdIsCalled() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        ReflectionTestUtils.setField(cmd, "sourceManagementServerId", 456L);
        assertEquals(Long.valueOf(456), cmd.getSourceManagementServerId());
    }
    
    @Test
    public void returnsTargetManagementServerIdsWhenGetTargetManagementServerIdsIsCalled() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        List<Long> targetIds = Arrays.asList(789L, 101L);
        ReflectionTestUtils.setField(cmd, "targetManagementServerIds", targetIds);
        assertEquals(targetIds, cmd.getTargetManagementServerIds());
    }
    
    @Test
    public void returnsFilesWhenGetFilesIsCalled() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        List<String> files = Arrays.asList("file1.txt", "file2.txt");
        ReflectionTestUtils.setField(cmd, "files", files);
        assertEquals(files, cmd.getFiles());
    }
    
    @Test
    public void executesSuccessfullyWhenSyncExtensionSucceeds() throws Exception {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        ExtensionsManager mockManager = mock(ExtensionsManager.class);
        ReflectionTestUtils.setField(cmd, "extensionsManager", mockManager);
        when(mockManager.syncExtension(cmd)).thenReturn(true);
    
        cmd.execute();
    
        SuccessResponse response = (SuccessResponse) cmd.getResponseObject();
        assertTrue(response.getSuccess());
    }
    
    @Test(expected = ServerApiException.class)
    public void throwsExceptionWhenSyncExtensionFails() throws Exception {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        ExtensionsManager mockManager = mock(ExtensionsManager.class);
        ReflectionTestUtils.setField(cmd, "extensionsManager", mockManager);
        when(mockManager.syncExtension(cmd)).thenReturn(false);
    
        cmd.execute();
    }
    
    @Test
    public void returnsSystemAccountIdWhenGetEntityOwnerIdIsCalled() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
    
    @Test
    public void returnsExtensionResourceTypeWhenGetApiResourceTypeIsCalled() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        assertEquals(ApiCommandResourceType.Extension, cmd.getApiResourceType());
    }
    
    @Test
    public void returnsIdWhenGetApiResourceIdIsCalled() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        ReflectionTestUtils.setField(cmd, "id", 123L);
        assertEquals(Long.valueOf(123), cmd.getApiResourceId());
    }
    
    @Test
    public void returnsCorrectEventType() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        assertEquals(EventTypes.EVENT_EXTENSION_SYNC, cmd.getEventType());
    }
    
    @Test
    public void returnsCorrectEventDescription() {
        SyncExtensionCmd cmd = new SyncExtensionCmd();
        ReflectionTestUtils.setField(cmd, "id", 123L);
        assertEquals("Sync extension: 123", cmd.getEventDescription());
    }
}
