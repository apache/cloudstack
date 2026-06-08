// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class SnapshotTest {
    @Test
    public void gettersSetters() {
        Snapshot snapshot = new Snapshot();
        snapshot.setDate(1714465200000L);
        snapshot.setSnapshotType("regular");
        snapshot.setSnapshotStatus("ok");
        snapshot.setDescription("Daily backup");
        snapshot.setPersistMemorystate("false");
        assertEquals(Long.valueOf(1714465200000L), snapshot.getDate());
        assertEquals("regular", snapshot.getSnapshotType());
        assertEquals("ok", snapshot.getSnapshotStatus());
        assertEquals("Daily backup", snapshot.getDescription());
        assertEquals("false", snapshot.getPersistMemorystate());
    }

    @Test
    public void json_ContainsSnapshotTypeAndDate() throws Exception {
        Mapper mapper = new Mapper();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotType("memory");
        snapshot.setDate(1000L);
        String json = mapper.toJson(snapshot);
        assertTrue(json.contains("\"snapshot_type\":\"memory\""));
        assertTrue(json.contains("\"date\":1000"));
    }
}
