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
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.framework.extensions.api.response.DownloadExtensionResponse;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@RunWith(MockitoJUnitRunner.class)
public class DownloadExtensionCmdTest {

    @Mock
    private ExtensionsManager extensionsManager;

    @Spy
    @InjectMocks
    private DownloadExtensionCmd cmd;

    @Test
    public void getIdReturnsExpected() {
        ReflectionTestUtils.setField(cmd, "id", 123L);
        assertEquals(Long.valueOf(123L), cmd.getId());
    }

    @Test
    public void getManagementServerIdReturnsExpected() {
        ReflectionTestUtils.setField(cmd, "managementServerId", 123L);
        assertEquals(Long.valueOf(123L), cmd.getManagementServerId());
    }

    @Test
    public void executeSetsResponseObjectWhenDownloadSucceeds() {
        DownloadExtensionResponse response = mock(DownloadExtensionResponse.class);
        when(extensionsManager.downloadExtension(any(DownloadExtensionCmd.class))).thenReturn(response);
        cmd.execute();
        verify(cmd).setResponseObject(response);
    }

    @Test
    public void executeThrowsExceptionWhenDownloadFails() {
        when(extensionsManager.downloadExtension(cmd)).thenReturn(null);
        ServerApiException exception = assertThrows(ServerApiException.class, cmd::execute);
        assertEquals(ApiErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        assertEquals("Failed to download extension", exception.getDescription());
    }

    @Test
    public void getEntityOwnerIdReturnsSystemAccount() {
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }

    @Test
    public void getApiResourceTypeReturnsExtension() {
        assertEquals(ApiCommandResourceType.Extension, cmd.getApiResourceType());
    }

    @Test
    public void getEventTypeReturnsCorrectEventType() {
        assertEquals(EventTypes.EVENT_EXTENSION_DOWNLOAD, cmd.getEventType());
    }

    @Test
    public void getEventDescriptionReturnsCorrectDescription() {
        Long id = 123L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals("Download extension: 123", cmd.getEventDescription());
    }

}
