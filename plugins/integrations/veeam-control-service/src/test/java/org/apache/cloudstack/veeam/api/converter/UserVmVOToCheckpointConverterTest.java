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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.veeam.api.dto.Checkpoint;
import org.junit.Test;

public class UserVmVOToCheckpointConverterTest {

    @Test
    public void testToCheckpoint_ReturnsNullWhenCheckpointIdMissing() {
        assertNull(UserVmVOToCheckpointConverter.toCheckpoint(null, "10"));
        assertNull(UserVmVOToCheckpointConverter.toCheckpoint("", "10"));
    }

    @Test
    public void testToCheckpoint_ParsesCreationTimeWhenProvided() {
        final Checkpoint checkpoint = UserVmVOToCheckpointConverter.toCheckpoint("chk-1", "1700000000");

        assertNotNull(checkpoint);
        assertEquals("chk-1", checkpoint.getId());
        assertEquals("chk-1", checkpoint.getName());
        assertEquals(String.valueOf(1700000000L * 1000L), checkpoint.getCreationDate());
        assertEquals("created", checkpoint.getState());
    }

    @Test
    public void testToCheckpoint_UsesNowWhenCreateTimeInvalid() {
        final long before = System.currentTimeMillis();
        final Checkpoint checkpoint = UserVmVOToCheckpointConverter.toCheckpoint("chk-2", "not-a-number");
        final long after = System.currentTimeMillis();

        final long creation = Long.parseLong(checkpoint.getCreationDate());
        assertTrue(creation >= before && creation <= after);
    }
}
