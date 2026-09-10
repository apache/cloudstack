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
package com.cloud.capacity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Capacity is counted in cluster-overcommitted units, so a VM started under a different
 * overcommit ratio to its cluster's has to be scaled before it is charged.
 */
public class CapacityManagerScaleToClusterUnitsTest {

    private final CapacityManagerImpl capacityManager = new CapacityManagerImpl();

    private long scale(long requested, String vmRatio, float clusterRatio) {
        return capacityManager.scaleToClusterUnits(requested, vmRatio, clusterRatio);
    }

    @Test
    public void testVmInheritingTheClusterRatioIsChargedAsRequested() {
        assertEquals(4000L, scale(4000L, "10.0", 10.0f));
        assertEquals(4000L, scale(4000L, "1.0", 1.0f));
    }

    @Test
    public void testVmWithoutARatioIsChargedAsRequested() {
        // no detail is only written when the VM's ratio equals its cluster's and neither
        // overcommits, in which case the request is already in cluster units
        assertEquals(4000L, scale(4000L, null, 1.0f));
    }

    @Test
    public void testUnreadableRatioDoesNotBreakTheCharge() {
        assertEquals(4000L, scale(4000L, "not a number", 10.0f));
    }

    @Test
    public void testUnovercommittedVmInAnOvercommittedClusterIsChargedItsFullShare() {
        // ratio 1 in a cluster overcommitted 10 times: the VM holds its whole request, so it must
        // occupy ten times the units of an equally sized neighbour running at the cluster ratio
        assertEquals(40000L, scale(4000L, "1.0", 10.0f));
    }

    @Test
    public void testVmMoreOvercommittedThanItsClusterIsChargedLess() {
        assertEquals(2000L, scale(4000L, "20.0", 10.0f));
    }

    @Test
    public void testRatioLeftOverFromAnEarlierClusterSettingIsRescaled() {
        // cluster was 4, is now 8: a VM started under 4 must still occupy the same real share
        assertEquals(8000L, scale(4000L, "4.0", 8.0f));
    }

    @Test
    public void testNonsenseRatiosFallBackToTheRequestedSize() {
        assertEquals(4000L, scale(4000L, "0", 10.0f));
        assertEquals(4000L, scale(4000L, "-1", 10.0f));
    }
}
