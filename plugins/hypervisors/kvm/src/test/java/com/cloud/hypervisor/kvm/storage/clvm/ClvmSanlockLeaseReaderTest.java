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

package com.cloud.hypervisor.kvm.storage.clvm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cloudstack.storage.clvm.command.ClvmLockTransferAnswer.LeaseMode;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class ClvmSanlockLeaseReaderTest {

    private static final String HEADER = "  offset                            lockspace                                         resource  timestamp  own  gen lver\n";

    // Captured with sanlock 3.8.4: "sanlock direct dump /dev/mapper/sharedvg-lvmlock:<offset>:1048576"
    private static final String EXCLUSIVE = HEADER
            + "75497472                         lvm_sharedvg           8Oicxc-uWrd-HEQB-L1Rr-IGZF-L08Y-IMmOoK 0001213684 0002 0001 1\n";

    private static final String SHARED = HEADER
            + "74448896                         lvm_sharedvg           OBe1FA-KGjN-RsXD-j93O-ovWz-uyM7-Gbro7e 0000000000 0002 0001 3\n"
            + "                                                                                                          0001 0002 SH\n"
            + "                                                                                                          0002 0001 SH\n";

    private static final String RELEASED = HEADER
            + "69206016                         lvm_sharedvg                                             VGLK 0000000000 0002 0001 54890\n";

    @Test
    public void testParseDumpExclusive() {
        ClvmSanlockLeaseReader.Lease lease = ClvmSanlockLeaseReader.parseDump(EXCLUSIVE, "8Oicxc-uWrd-HEQB-L1Rr-IGZF-L08Y-IMmOoK");
        assertEquals(LeaseMode.EXCLUSIVE, lease.getMode());
        assertEquals(Long.valueOf(2), lease.getOwnerId());
        assertEquals(Long.valueOf(1), lease.ownerGeneration);
    }

    @Test
    public void testParseDumpShared() {
        ClvmSanlockLeaseReader.Lease lease = ClvmSanlockLeaseReader.parseDump(SHARED, "OBe1FA-KGjN-RsXD-j93O-ovWz-uyM7-Gbro7e");
        assertEquals(LeaseMode.SHARED, lease.getMode());
        assertNull(lease.getOwnerId());
        assertEquals(Arrays.asList(1L, 2L), lease.getSharedOwnerIds());
    }

    @Test
    public void testParseDumpReleasedIgnoresStaleOwner() {
        ClvmSanlockLeaseReader.Lease lease = ClvmSanlockLeaseReader.parseDump(RELEASED, "VGLK");
        assertEquals(LeaseMode.FREE, lease.getMode());
        assertNull(lease.getOwnerId());
        assertEquals(Collections.emptyList(), lease.getSharedOwnerIds());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testParseDumpWithoutTheLease() {
        ClvmSanlockLeaseReader.parseDump(EXCLUSIVE, "other-uuid");
    }

    @Test
    public void testParseLockArgs() {
        assertArrayEquals(new String[] {"rQXPx2-5Ecb-PdRO-oUlx-VqV9-RIYu-thyuIm", "73400320"},
                ClvmSanlockLeaseReader.parseLockArgs("  rQXPx2-5Ecb-PdRO-oUlx-VqV9-RIYu-thyuIm 1.0.0:73400320"));
    }

    @Test
    public void testParseLockArgsSkipsLockStartWarning() {
        assertArrayEquals(new String[] {"rQXPx2-5Ecb-PdRO-oUlx-VqV9-RIYu-thyuIm", "73400320"},
                ClvmSanlockLeaseReader.parseLockArgs("  VG sharedvg2 lock skipped: lock start in progress\n"
                        + "  rQXPx2-5Ecb-PdRO-oUlx-VqV9-RIYu-thyuIm 1.0.0:73400320\n"));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testParseLockArgsWithoutSanlockLock() {
        ClvmSanlockLeaseReader.parseLockArgs("  rQXPx2-5Ecb-PdRO-oUlx-VqV9-RIYu-thyuIm");
    }

    @Test
    public void testLvmlockDeviceEscapesDashes() {
        assertEquals("/dev/mapper/sharedvg-lvmlock", ClvmSanlockLeaseReader.lvmlockDevice("sharedvg"));
        assertEquals("/dev/mapper/shared--vg-lvmlock", ClvmSanlockLeaseReader.lvmlockDevice("shared-vg"));
    }

    @Test
    public void testOwnerAliveOnlyWithSameGeneration() {
        ClvmSanlockLeaseReader.Lease lease = ClvmSanlockLeaseReader.parseDump(EXCLUSIVE, "8Oicxc-uWrd-HEQB-L1Rr-IGZF-L08Y-IMmOoK");
        assertTrue(ClvmSanlockLeaseReader.isOwnerAlive(new ClvmSanlockHaChecker.LeaseState(1100, 1100, 10, 1L), lease));
        assertFalse(ClvmSanlockLeaseReader.isOwnerAlive(new ClvmSanlockHaChecker.LeaseState(1100, 1100, 10, 2L), lease));
    }

    @Test
    public void testOwnerRenewingOnlyWithFreshLeaseAndSameGeneration() {
        ClvmSanlockLeaseReader.Lease lease = ClvmSanlockLeaseReader.parseDump(EXCLUSIVE, "8Oicxc-uWrd-HEQB-L1Rr-IGZF-L08Y-IMmOoK");
        assertTrue(ClvmSanlockLeaseReader.isOwnerRenewing(new ClvmSanlockHaChecker.LeaseState(1020, 1000, 10, 1L), lease));
        assertFalse(ClvmSanlockLeaseReader.isOwnerRenewing(new ClvmSanlockHaChecker.LeaseState(1030, 1000, 10, 1L), lease));
        assertFalse(ClvmSanlockLeaseReader.isOwnerRenewing(new ClvmSanlockHaChecker.LeaseState(1020, 1000, 10, 2L), lease));
        assertTrue(ClvmSanlockLeaseReader.isOwnerRenewing(null, lease));
    }

    @Test
    public void testOwnerNotAliveWhenLockspaceLeaseStoppedRenewing() {
        ClvmSanlockLeaseReader.Lease lease = ClvmSanlockLeaseReader.parseDump(EXCLUSIVE, "8Oicxc-uWrd-HEQB-L1Rr-IGZF-L08Y-IMmOoK");
        assertFalse(ClvmSanlockLeaseReader.isOwnerAlive(new ClvmSanlockHaChecker.LeaseState(1140, 1000, 10, 1L), lease));
    }
}
