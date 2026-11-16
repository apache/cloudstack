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

import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VolumeForImportResponse;
import org.apache.cloudstack.storage.volume.VolumeImportUnmanageService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ListVolumesForImportCmdTest {

    VolumeImportUnmanageService volumeImportService = Mockito.spy(VolumeImportUnmanageService.class);

    @Test
    public void testListVolumesForImportCmd() {
        Long storageId = 2L;
        String filePath = "file path";

        ListVolumesForImportCmd cmd = new ListVolumesForImportCmd();
        ReflectionTestUtils.setField(cmd, "storageId", storageId);
        ReflectionTestUtils.setField(cmd, "path", filePath);
        ReflectionTestUtils.setField(cmd,"volumeImportService", volumeImportService);

        Assert.assertEquals(storageId, cmd.getStorageId());
        Assert.assertEquals(filePath, cmd.getPath());

        ListResponse<VolumeForImportResponse> response = Mockito.mock(ListResponse.class);
        Mockito.when(volumeImportService.listVolumesForImport(cmd)).thenReturn(response);
        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertEquals(response, cmd.getResponseObject());
    }
}
