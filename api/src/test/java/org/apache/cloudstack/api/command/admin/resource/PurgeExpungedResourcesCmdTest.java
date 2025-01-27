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

package org.apache.cloudstack.api.command.admin.resource;

import static org.junit.Assert.assertNull;

import java.util.Date;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PurgeExpungedResourcesResponse;
import org.apache.cloudstack.resource.ResourceCleanupService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class PurgeExpungedResourcesCmdTest {
    @Mock
    ResourceCleanupService resourceCleanupService;

    @Spy
    @InjectMocks
    PurgeExpungedResourcesCmd spyCmd = Mockito.spy(new PurgeExpungedResourcesCmd());

    @Test
    public void testGetResourceType() {
        PurgeExpungedResourcesCmd cmd = new PurgeExpungedResourcesCmd();
        assertNull(cmd.getResourceType());
        ReflectionTestUtils.setField(cmd, "resourceType", ResourceCleanupService.ResourceType.VirtualMachine.toString());
        Assert.assertEquals(ResourceCleanupService.ResourceType.VirtualMachine.toString(), cmd.getResourceType());
    }

    @Test
    public void testGetBatchSize() {
        PurgeExpungedResourcesCmd cmd = new PurgeExpungedResourcesCmd();
        assertNull(cmd.getBatchSize());
        Long batchSize = 100L;
        ReflectionTestUtils.setField(cmd, "batchSize", batchSize);
        Assert.assertEquals(batchSize, cmd.getBatchSize());
    }

    @Test
    public void testGetStartDate() {
        PurgeExpungedResourcesCmd cmd = new PurgeExpungedResourcesCmd();
        assertNull(cmd.getStartDate());
        Date date = new Date();
        ReflectionTestUtils.setField(cmd, "startDate", date);
        Assert.assertEquals(date, cmd.getStartDate());
    }

    @Test
    public void testGetEndDate() {
        PurgeExpungedResourcesCmd cmd = new PurgeExpungedResourcesCmd();
        assertNull(cmd.getEndDate());
        Date date = new Date();
        ReflectionTestUtils.setField(cmd, "endDate", date);
        Assert.assertEquals(date, cmd.getEndDate());
    }

    @Test
    public void testExecute() {
        final PurgeExpungedResourcesResponse[] executeResponse = new PurgeExpungedResourcesResponse[1];
        Long result = 100L;
        Mockito.when(resourceCleanupService.purgeExpungedResources(Mockito.any())).thenReturn(result);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            executeResponse[0] = (PurgeExpungedResourcesResponse)invocation.getArguments()[0];
            return null;
        }).when(spyCmd).setResponseObject(Mockito.any());
        spyCmd.execute();
        PurgeExpungedResourcesResponse response = executeResponse[0];
        Assert.assertNotNull(response);
        Assert.assertEquals(result, response.getResourceCount());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteException() {
        Mockito.doThrow(CloudRuntimeException.class).when(resourceCleanupService).purgeExpungedResources(Mockito.any());
        spyCmd.execute();
    }
}
