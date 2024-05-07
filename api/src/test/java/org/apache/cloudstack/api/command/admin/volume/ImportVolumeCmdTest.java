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

package org.apache.cloudstack.api.command.admin.volume;

import com.cloud.event.EventTypes;
import com.cloud.user.AccountService;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.storage.volume.VolumeImportUnmanageService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ImportVolumeCmdTest {

    VolumeImportUnmanageService volumeImportService = Mockito.spy(VolumeImportUnmanageService.class);
    AccountService accountService = Mockito.spy(AccountService.class);

    @Test
    public void testImportVolumeCmd() {
        String name = "volume name";
        String path = "file path";
        Long storageId = 2L;
        Long diskOfferingId = 3L;
        String accountName = "account";
        Long domainId = 4L;
        Long projectId = 5L;
        long accountId = 6L;

        Mockito.when(accountService.finalyzeAccountId(accountName, domainId, projectId, true)).thenReturn(accountId);

        ImportVolumeCmd cmd = new ImportVolumeCmd();
        ReflectionTestUtils.setField(cmd, "path", path);
        ReflectionTestUtils.setField(cmd, "name", name);
        ReflectionTestUtils.setField(cmd, "storageId", storageId);
        ReflectionTestUtils.setField(cmd, "diskOfferingId", diskOfferingId);
        ReflectionTestUtils.setField(cmd, "accountName", accountName);
        ReflectionTestUtils.setField(cmd, "domainId", domainId);
        ReflectionTestUtils.setField(cmd, "projectId", projectId);
        ReflectionTestUtils.setField(cmd,"volumeImportService", volumeImportService);
        ReflectionTestUtils.setField(cmd, "_accountService", accountService);

        Assert.assertEquals(path, cmd.getPath());
        Assert.assertEquals(name, cmd.getName());
        Assert.assertEquals(storageId, cmd.getStorageId());
        Assert.assertEquals(diskOfferingId, cmd.getDiskOfferingId());
        Assert.assertEquals(accountName, cmd.getAccountName());
        Assert.assertEquals(domainId, cmd.getDomainId());
        Assert.assertEquals(projectId, cmd.getProjectId());

        Assert.assertEquals(EventTypes.EVENT_VOLUME_IMPORT, cmd.getEventType());
        Assert.assertEquals("Importing unmanaged Volume with path: " + path, cmd.getEventDescription());
        Assert.assertEquals(accountId, cmd.getEntityOwnerId());

        VolumeResponse response = Mockito.mock(VolumeResponse.class);
        Mockito.when(volumeImportService.importVolume(cmd)).thenReturn(response);
        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertEquals(response, cmd.getResponseObject());
    }
}
