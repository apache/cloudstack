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
package com.cloud.hypervisor.kvm.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloud.hypervisor.kvm.resource.UsageExemptBilling.ExemptCount;

public class UsageExemptBillingTest {

    private Path dir;
    private Path stateFile;
    private UsageExemptBilling billing;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("usage-exempt-billing");
        stateFile = dir.resolve("run/state.json");
        billing = new UsageExemptBilling(stateFile);
    }

    @After
    public void tearDown() throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Collections.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    private long[] report(long sent, long received, long exemptSent, long exemptReceived) {
        return billing.subtractExempt("vnet1", 51, sent, received, new ExemptCount(exemptSent, exemptReceived));
    }

    @Test
    public void subtractsExemptBytesFromBothDirections() {
        Assert.assertArrayEquals(new long[] {4000, 7300}, report(5000, 8000, 1000, 700));
    }

    @Test
    public void neverReportsLessThanLastTime() {
        Assert.assertEquals(4000, report(5000, 0, 1000, 0)[0]);
        // exempt traffic counted by nft but not yet by libvirt would push the figure down
        Assert.assertEquals(4000, report(5500, 0, 2000, 0)[0]);
        Assert.assertEquals(4500, report(6500, 0, 2000, 0)[0]);
    }

    @Test
    public void keepsExemptBytesWhenTheCountGoesDown() {
        Assert.assertEquals(7000, report(0, 10000, 0, 3000)[1]);
        // the counter was wiped and started again
        Assert.assertEquals(12000 - 3000 - 500, report(0, 12000, 0, 500)[1]);
    }

    @Test
    public void keepCountsFromLostChainsCatchesAResetTheNextReportWouldMiss() {
        Assert.assertEquals(4000, report(5000, 0, 1000, 0)[0]);
        Assert.assertTrue(billing.keepCountsFromLostChains(51, true, false));
        // the new counter has already counted past the old one
        Assert.assertEquals(7000 - 1000 - 1500, report(7000, 0, 1500, 0)[0]);
    }

    @Test
    public void keepCountsFromLostChainsIgnoresOtherTaps() {
        report(5000, 0, 1000, 0);
        Assert.assertFalse(billing.keepCountsFromLostChains(52, true, true));
    }

    @Test
    public void newIfindexStartsAfresh() {
        Assert.assertEquals(9000, report(10000, 0, 1000, 0)[0]);
        Assert.assertEquals(300, billing.subtractExempt("vnet1", 77, 300, 0, new ExemptCount())[0]);
    }

    @Test
    public void libvirtTotalsGoingDownStartsAfresh() {
        Assert.assertEquals(9000, report(10000, 0, 1000, 0)[0]);
        Assert.assertEquals(300, report(300, 0, 0, 0)[0]);
    }

    @Test
    public void unreadableCountsKeepTheExemptBytesFromLastTime() {
        Assert.assertEquals(4000, report(5000, 0, 1000, 0)[0]);
        Assert.assertEquals(5000, billing.subtractExempt("vnet1", 51, 6000, 0, null)[0]);
    }

    @Test
    public void recordSurvivesAnAgentRestart() {
        Assert.assertEquals(4000, report(5000, 0, 1000, 0)[0]);
        billing.saveState((device, ifindex) -> true);
        Assert.assertTrue(new UsageExemptBilling(stateFile).hasSavedState());

        UsageExemptBilling restarted = new UsageExemptBilling(stateFile);
        Assert.assertEquals(4000, restarted.subtractExempt("vnet1", 51, 5500, 0, new ExemptCount(2000, 0))[0]);
    }

    @Test
    public void saveStateDropsTapsThatAreGone() {
        report(5000, 0, 1000, 0);
        billing.saveState((device, ifindex) -> false);
        UsageExemptBilling restarted = new UsageExemptBilling(stateFile);
        Assert.assertEquals(5500 - 2000, restarted.subtractExempt("vnet1", 51, 5500, 0, new ExemptCount(2000, 0))[0]);
    }

    @Test
    public void billableStaysBetweenLastReportAndTotal() {
        Assert.assertEquals(700, UsageExemptBilling.billable(1000, 300, 0));
        Assert.assertEquals(800, UsageExemptBilling.billable(1000, 300, 800));
        Assert.assertEquals(0, UsageExemptBilling.billable(1000, 5000, 0));
    }
}
