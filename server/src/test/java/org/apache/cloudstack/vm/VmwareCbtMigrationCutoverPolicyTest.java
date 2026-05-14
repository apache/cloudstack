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
package org.apache.cloudstack.vm;

import org.junit.Assert;
import org.junit.Test;

public class VmwareCbtMigrationCutoverPolicyTest {

    @Test
    public void testContinuesUntilMinimumCyclesAreReached() {
        VmwareCbtMigrationCutoverPolicy policy = new VmwareCbtMigrationCutoverPolicy(2, 5, 1, 1024, 0);

        Assert.assertEquals(VmwareCbtMigrationCutoverPolicy.Decision.CONTINUE,
                policy.decide(1, 0, 512, 10));
    }

    @Test
    public void testReadyForCutoverAfterRequiredQuietCycles() {
        VmwareCbtMigrationCutoverPolicy policy = new VmwareCbtMigrationCutoverPolicy(1, 5, 2, 1024, 0);

        Assert.assertEquals(VmwareCbtMigrationCutoverPolicy.Decision.READY_FOR_CUTOVER,
                policy.decide(3, 1, 512, 10));
    }

    @Test
    public void testReadyForCutoverAfterMinimumCyclesWhenNoBlocksChanged() {
        VmwareCbtMigrationCutoverPolicy policy = new VmwareCbtMigrationCutoverPolicy(2, 5, 2, 1024, 0);

        Assert.assertEquals(VmwareCbtMigrationCutoverPolicy.Decision.CONTINUE,
                policy.decide(1, 0, 0, 10));
        Assert.assertEquals(VmwareCbtMigrationCutoverPolicy.Decision.READY_FOR_CUTOVER,
                policy.decide(2, 0, 0, 10));
    }

    @Test
    public void testDirtyRateCanKeepReplicationRunning() {
        VmwareCbtMigrationCutoverPolicy policy = new VmwareCbtMigrationCutoverPolicy(1, 5, 1, 0, 1024);

        Assert.assertFalse(policy.isQuietCycle(2048, 1));
        Assert.assertTrue(policy.isQuietCycle(2048, 2));
    }

    @Test
    public void testReadyForCutoverWhenMaxCyclesReached() {
        VmwareCbtMigrationCutoverPolicy policy = new VmwareCbtMigrationCutoverPolicy(1, 5, 2, 1024, 1024);

        Assert.assertEquals(VmwareCbtMigrationCutoverPolicy.Decision.READY_FOR_CUTOVER_MAX_CYCLES,
                policy.decide(5, 0, 4096, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsMaxCyclesBelowMinCycles() {
        new VmwareCbtMigrationCutoverPolicy(3, 2, 1, 1024, 1024);
    }
}
