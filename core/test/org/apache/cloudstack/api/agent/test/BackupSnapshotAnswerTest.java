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
package org.apache.cloudstack.api.agent.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.storage.StoragePool;

public class BackupSnapshotAnswerTest {
    private BackupSnapshotCommand bsc;
    private BackupSnapshotAnswer bsa;

    @Before
    public void setUp() {

        StoragePool pool = Mockito.mock(StoragePool.class);

        bsc = new BackupSnapshotCommand(
                "secondaryStoragePoolURL", 101L, 102L, 103L, 104L, 105L,
                "volumePath", pool, "snapshotUuid", "snapshotName",
                "prevSnapshotUuid", "prevBackupUuid", false, "vmName", 5);
        bsa = new BackupSnapshotAnswer(bsc, true, "results", "bussname", false);
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = bsa.executeInSequence();
        assertFalse(b);
    }

    @Test
    public void testIsFull() {
        boolean b = bsa.isFull();
        assertFalse(b);
    }

    @Test
    public void testGetBackupSnapshotName() {
        String name = bsa.getBackupSnapshotName();
        assertTrue(name.equals("bussname"));
    }

    @Test
    public void testGetResult() {
        boolean b = bsa.getResult();
        assertTrue(b);
    }

    @Test
    public void testDetails() {
        String details = bsa.getDetails();
        assertTrue(details.equals("results"));
    }
}
