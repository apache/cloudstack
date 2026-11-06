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


package com.cloud.hypervisor.kvm.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClvmSanlockHaCheckerTest {

    private static final String HOST_STATUS =
            "1 timestamp 437999\n"
            + "    last_check=438020\n"
            + "    last_live=438020\n"
            + "    last_req=0\n"
            + "    owner_id=1\n"
            + "    owner_generation=2\n"
            + "    timestamp=437999\n"
            + "    io_timeout=10\n"
            + "    owner_name=46a23acd-5b2c-479d-8474-583e8fced6a8.ref-trl-12\n"
            + "2 timestamp 437955\n"
            + "    last_check=438200\n"
            + "    last_live=438020\n"
            + "    last_req=0\n"
            + "    owner_id=2\n"
            + "    owner_generation=1\n"
            + "    timestamp=437955\n"
            + "    io_timeout=10\n"
            + "    owner_name=31028f82-31c0-4c3d-a78a-8cc92afde498.ref-trl-12\n";

    @Test
    public void testParseLeaseStateForLiveHost() {
        ClvmSanlockHaChecker.LeaseState state = ClvmSanlockHaChecker.parseLeaseState(HOST_STATUS, 1);
        assertNotNull(state);
        assertEquals(438020, state.lastCheck);
        assertEquals(438020, state.lastLive);
        assertEquals(10, state.ioTimeout);
        assertTrue(ClvmSanlockHaChecker.isLeaseAlive(state));
    }

    @Test
    public void testParseLeaseStateForHostWhoseLeaseStoppedRenewing() {
        ClvmSanlockHaChecker.LeaseState state = ClvmSanlockHaChecker.parseLeaseState(HOST_STATUS, 2);
        assertNotNull(state);
        assertEquals(180, state.lastCheck - state.lastLive);
        assertFalse(ClvmSanlockHaChecker.isLeaseAlive(state));
    }

    @Test
    public void testLeaseStillAliveBeforeWatchdogGuarantee() {
        assertTrue(ClvmSanlockHaChecker.isLeaseAlive(new ClvmSanlockHaChecker.LeaseState(1139, 1000, 10)));
        assertFalse(ClvmSanlockHaChecker.isLeaseAlive(new ClvmSanlockHaChecker.LeaseState(1140, 1000, 10)));
    }

    @Test
    public void testParseLeaseStateForUnknownHostId() {
        assertNull(ClvmSanlockHaChecker.parseLeaseState(HOST_STATUS, 3));
        assertNull(ClvmSanlockHaChecker.parseLeaseState("", 1));
    }

    @Test
    public void testParseHostId() {
        assertEquals(Long.valueOf(2), ClvmSanlockHaChecker.parseHostId("host_id=2"));
        assertNull(ClvmSanlockHaChecker.parseHostId("  Configuration node local/host_id not found"));
        assertNull(ClvmSanlockHaChecker.parseHostId(null));
    }

    @Test
    public void testParseHostIdIgnoresNumberTooLarge() {
        assertNull(ClvmSanlockHaChecker.parseHostId("host_id=99999999999999999999"));
    }

    @Test
    public void testParseLeaseStateIgnoresMalformedNumbers() {
        assertNull(ClvmSanlockHaChecker.parseLeaseState("1 timestamp 10\n\tlast_check=abc\n\tlast_live=5\n\tio_timeout=10\n", 1));
        assertNull(ClvmSanlockHaChecker.parseLeaseState("99999999999999999999 timestamp 10\n\tlast_check=1\n", 1));
    }

    @Test
    public void testUnknownVgOrHostIdAssumesAlive() {
        assertTrue(ClvmSanlockHaChecker.isHostLeaseAlive(null, 1L));
        assertTrue(ClvmSanlockHaChecker.isHostLeaseAlive("sharedvg", null));
    }
}
