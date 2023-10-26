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
package org.apache.cloudstack.api.command.user.snapshot;

import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.query.QueryService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.dc.DataCenter;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.snapshot.SnapshotApiService;
import com.cloud.utils.db.UUIDManager;

@RunWith(MockitoJUnitRunner.class)
public class CopySnapshotCmdTest {

    @Test
    public void testGetId() {
        final CopySnapshotCmd cmd = new CopySnapshotCmd();
        Long id = 100L;
        ReflectionTestUtils.setField(cmd, "id", id);
        Assert.assertEquals(id, cmd.getId());
    }

    @Test
    public void testGetSourceZoneId() {
        final CopySnapshotCmd cmd = new CopySnapshotCmd();
        Long id = 200L;
        ReflectionTestUtils.setField(cmd, "sourceZoneId", id);
        Assert.assertEquals(id, cmd.getSourceZoneId());
    }

    @Test
    public void testGetDestZoneIdWithSingleId() {
        final CopySnapshotCmd cmd = new CopySnapshotCmd();
        Long id = 300L;
        ReflectionTestUtils.setField(cmd, "destZoneId", id);
        Assert.assertEquals(1, cmd.getDestinationZoneIds().size());
        Assert.assertEquals(id, cmd.getDestinationZoneIds().get(0));
    }

    @Test
    public void testGetDestZoneIdWithMultipleId() {
        final CopySnapshotCmd cmd = new CopySnapshotCmd();
        List<Long> ids = List.of(400L, 500L);
        ReflectionTestUtils.setField(cmd, "destZoneIds", ids);
        Assert.assertEquals(ids.size(), cmd.getDestinationZoneIds().size());
        Assert.assertEquals(ids.get(0), cmd.getDestinationZoneIds().get(0));
        Assert.assertEquals(ids.get(1), cmd.getDestinationZoneIds().get(1));
    }

    @Test
    public void testGetDestZoneIdWithBothParams() {
        final CopySnapshotCmd cmd = new CopySnapshotCmd();
        List<Long> ids = List.of(400L, 500L);
        ReflectionTestUtils.setField(cmd, "destZoneIds", ids);
        ReflectionTestUtils.setField(cmd, "destZoneId", 100L);
        Assert.assertEquals(ids.size(), cmd.getDestinationZoneIds().size());
        Assert.assertEquals(ids.get(0), cmd.getDestinationZoneIds().get(0));
        Assert.assertEquals(ids.get(1), cmd.getDestinationZoneIds().get(1));
    }

    @Test (expected = ServerApiException.class)
    public void testExecuteWrongNoParams() {
        final CopySnapshotCmd cmd = new CopySnapshotCmd();
        try {
            cmd.execute();
        } catch (ResourceUnavailableException e) {
            Assert.fail(String.format("Exception: %s", e.getMessage()));
        }
    }

    @Test (expected = ServerApiException.class)
    public void testExecuteWrongBothParams() {
        final CopySnapshotCmd cmd = new CopySnapshotCmd();
        List<Long> ids = List.of(400L, 500L);
        ReflectionTestUtils.setField(cmd, "destZoneIds", ids);
        ReflectionTestUtils.setField(cmd, "destZoneId", 100L);
        try {
            cmd.execute();
        } catch (ResourceUnavailableException e) {
            Assert.fail(String.format("Exception: %s", e.getMessage()));
        }
    }

    @Test
    public void testExecuteSuccess() {
        SnapshotApiService snapshotApiService = Mockito.mock(SnapshotApiService.class);
        QueryService queryService = Mockito.mock(QueryService.class);
        UUIDManager uuidManager = Mockito.mock(UUIDManager.class);
        final CopySnapshotCmd cmd = new CopySnapshotCmd();
        cmd._snapshotService = snapshotApiService;
        cmd._queryService = queryService;
        cmd._uuidMgr = uuidManager;
        Snapshot snapshot = Mockito.mock(Snapshot.class);
        final Long id = 100L;
        ReflectionTestUtils.setField(cmd, "destZoneId", id);
        SnapshotResponse snapshotResponse = Mockito.mock(SnapshotResponse.class);
        try {
            Mockito.when(snapshotApiService.copySnapshot(cmd)).thenReturn(snapshot);
            Mockito.when(queryService.listSnapshot(cmd)).thenReturn(snapshotResponse);
            Mockito.when(uuidManager.getUuid(DataCenter.class, id)).thenReturn(UUID.randomUUID().toString());
            cmd.execute();
        } catch (ResourceAllocationException | ResourceUnavailableException e) {
            Assert.fail(String.format("Exception: %s", e.getMessage()));
        }
    }
}
