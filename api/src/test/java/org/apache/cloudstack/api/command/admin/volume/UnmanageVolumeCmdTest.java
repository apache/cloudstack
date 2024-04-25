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
import com.cloud.storage.Volume;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.storage.volume.VolumeImportUnmanageService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class UnmanageVolumeCmdTest {

    VolumeImportUnmanageService volumeImportService = Mockito.spy(VolumeImportUnmanageService.class);
    ResponseGenerator responseGenerator = Mockito.spy(ResponseGenerator.class);

    @Test
    public void testUnmanageVolumeCmd() {
        long accountId = 2L;
        Long volumeId = 3L;
        Volume volume = Mockito.mock(Volume.class);

        Mockito.when(responseGenerator.findVolumeById(volumeId)).thenReturn(volume);
        Mockito.when(volume.getAccountId()).thenReturn(accountId);

        UnmanageVolumeCmd cmd = new UnmanageVolumeCmd();
        ReflectionTestUtils.setField(cmd, "volumeId", volumeId);
        ReflectionTestUtils.setField(cmd,"volumeImportService", volumeImportService);
        ReflectionTestUtils.setField(cmd,"_responseGenerator", responseGenerator);

        Assert.assertEquals(volumeId, cmd.getVolumeId());
        Assert.assertEquals(accountId, cmd.getEntityOwnerId());
        Assert.assertEquals(volumeId, cmd.getApiResourceId());
        Assert.assertEquals(ApiCommandResourceType.Volume, cmd.getApiResourceType());
        Assert.assertEquals(EventTypes.EVENT_VOLUME_UNMANAGE, cmd.getEventType());
        Assert.assertEquals("Unmanaging Volume with ID " + volumeId, cmd.getEventDescription());

        Mockito.when(volumeImportService.unmanageVolume(volumeId)).thenReturn(true);
        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Object response = cmd.getResponseObject();
        Assert.assertTrue(response instanceof SuccessResponse);
    }
}
