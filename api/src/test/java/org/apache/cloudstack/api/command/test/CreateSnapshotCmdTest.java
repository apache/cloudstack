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
package org.apache.cloudstack.api.command.test;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotCmd;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.storage.Snapshot;
import com.cloud.storage.VolumeApiService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;

import junit.framework.TestCase;

public class CreateSnapshotCmdTest extends TestCase {

    private CreateSnapshotCmd createSnapshotCmd;
    private ResponseGenerator responseGenerator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    @Before
    public void setUp() {

        createSnapshotCmd = new CreateSnapshotCmd() {

            @Override
            public String getCommandName() {
                return "createsnapshotresponse";
            }

            @Override
            public Long getVolumeId(){
                return 1L;
            }

            @Override
            public long getEntityOwnerId(){
                return 1L;
            }

            @Override
            protected String getVolumeUuid() {
                return "123";
            }
        };

    }

    @Test
    public void testCreateSuccess() {

        AccountService accountService = Mockito.mock(AccountService.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(accountService.getAccount(anyLong())).thenReturn(account);

        VolumeApiService volumeApiService = Mockito.mock(VolumeApiService.class);
        Snapshot snapshot = Mockito.mock(Snapshot.class);
        try {
            Mockito.when(volumeApiService.takeSnapshot(nullable(Long.class), nullable(Long.class), isNull(),
                    nullable(Account.class), nullable(Boolean.class), nullable(Snapshot.LocationType.class), nullable(Boolean.class), nullable(Map.class), nullable(List.class))).thenReturn(snapshot);

        } catch (Exception e) {
            Assert.fail("Received exception when success expected " + e.getMessage());
        }

        responseGenerator = Mockito.mock(ResponseGenerator.class);
        SnapshotResponse snapshotResponse = Mockito.mock(SnapshotResponse.class);
        Mockito.when(responseGenerator.createSnapshotResponse(snapshot)).thenReturn(snapshotResponse);
        Mockito.doNothing().when(snapshotResponse).setAccountName(anyString());

        createSnapshotCmd._accountService = accountService;
        createSnapshotCmd._responseGenerator = responseGenerator;
        createSnapshotCmd._volumeService = volumeApiService;

        try {
            createSnapshotCmd.execute();
        } catch (Exception e) {
            Assert.fail("Received exception when success expected " + e.getMessage());
        }
    }

    @Test
    public void testCreateFailure() {

        AccountService accountService = Mockito.mock(AccountService.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(accountService.getAccount(anyLong())).thenReturn(account);

        VolumeApiService volumeApiService = Mockito.mock(VolumeApiService.class);

        try {
                Mockito.when(volumeApiService.takeSnapshot(nullable(Long.class), nullable(Long.class), nullable(Long.class),
                        nullable(Account.class), nullable(Boolean.class), nullable(Snapshot.LocationType.class), nullable(Boolean.class), anyObject(), Mockito.anyList())).thenReturn(null);
        } catch (Exception e) {
            Assert.fail("Received exception when success expected " + e.getMessage());
        }

        createSnapshotCmd._accountService = accountService;
        createSnapshotCmd._volumeService = volumeApiService;

        try {
            createSnapshotCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to create snapshot due to an internal error creating snapshot for volume 123", exception.getDescription());
        }
    }

    @Test
    public void testParsingTags() {
        final CreateSnapshotCmd createSnapshotCmd = new CreateSnapshotCmd();
        final Map<String, String> tag1 = new HashMap<>();
        tag1.put("key", "key1");
        tag1.put("value", "value1");
        final Map<String, String> tag2 = new HashMap<>();
        tag2.put("key", "key2");
        tag2.put("value", "value2");
        final Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put("key1", "value1");
        expectedTags.put("key2", "value2");

        final Map<String, Map<String, String>> tagsParams = new HashMap<>();
        tagsParams.put("0", tag1);
        tagsParams.put("1", tag2);
        ReflectionTestUtils.setField(createSnapshotCmd, "tags", tagsParams);
        Assert.assertEquals(createSnapshotCmd.getTags(), expectedTags);
    }

    @Test
    public void testGetZoneIds() {
        final CreateSnapshotCmd cmd = new CreateSnapshotCmd();
        List<Long> ids = List.of(400L, 500L);
        ReflectionTestUtils.setField(cmd, "zoneIds", ids);
        Assert.assertEquals(ids.size(), cmd.getZoneIds().size());
        Assert.assertEquals(ids.get(0), cmd.getZoneIds().get(0));
        Assert.assertEquals(ids.get(1), cmd.getZoneIds().get(1));
    }
}
