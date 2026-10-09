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
package org.apache.cloudstack.cluster;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class ClusterDrsPowerManagementTest {

    private final ClusterDrsServiceImpl drs = new ClusterDrsServiceImpl();

    @Test
    public void releasesHostWhenUnderUtilizedAndRemainingHostsCanCarryLoad() {
        // 4 hosts of 100 each, 90 used total (22.5%): below low 0.30, and after dropping one host 90/300 = 30% <= high 0.75.
        Assert.assertTrue(drs.clusterCanReleaseHost(90d, 400d, 100d, 4, 0.30f, 0.75f));
    }

    @Test
    public void keepsHostWhenClusterIsNotUnderUtilized() {
        // 250/400 = 62.5% is above the low threshold, so nothing is powered off.
        Assert.assertFalse(drs.clusterCanReleaseHost(250d, 400d, 100d, 4, 0.30f, 0.75f));
    }

    @Test
    public void keepsHostWhenRemainingHostsWouldExceedHighThreshold() {
        // 40/200 = 20% used (below the low threshold), but the candidate is a large host (170 of 200): dropping it
        // leaves only 30 of capacity, so 40/30 = 133% would blow past the high threshold. Keep it.
        Assert.assertFalse(drs.clusterCanReleaseHost(40d, 200d, 170d, 2, 0.30f, 0.75f));
    }

    @Test
    public void neverReleasesTheLastHost() {
        Assert.assertFalse(drs.clusterCanReleaseHost(10d, 100d, 100d, 1, 0.30f, 0.75f));
    }

    @Test
    public void wakesHostWhenOverUtilizedAndOneWasPoweredOff() {
        // 320/400 = 80% is above high 0.75 and one host is available to wake.
        Assert.assertTrue(drs.clusterNeedsWakeup(320d, 400d, 0.75f, 1));
    }

    @Test
    public void doesNotWakeWhenNoHostWasPoweredOff() {
        Assert.assertFalse(drs.clusterNeedsWakeup(320d, 400d, 0.75f, 0));
    }

    @Test
    public void doesNotWakeWhenBelowHighThreshold() {
        Assert.assertFalse(drs.clusterNeedsWakeup(200d, 400d, 0.75f, 1));
    }

    @Test
    public void forecastProjectsARisingTrendAboveTheLastSample() {
        double forecast = drs.forecastUtilization(Arrays.asList(0.40d, 0.50d, 0.60d, 0.70d));
        Assert.assertTrue("rising trend should forecast above the last sample", forecast > 0.70d);
    }

    @Test
    public void forecastOnFlatSeriesEqualsTheLastSample() {
        Assert.assertEquals(0.50d, drs.forecastUtilization(Arrays.asList(0.50d, 0.50d, 0.50d)), 0.0001d);
    }

    @Test
    public void forecastOnFallingTrendIsBelowTheLastSample() {
        double forecast = drs.forecastUtilization(Arrays.asList(0.80d, 0.70d, 0.60d, 0.50d));
        Assert.assertTrue("falling trend should forecast below the last sample", forecast < 0.50d);
    }

    @Test
    public void forecastClampsToOne() {
        double forecast = drs.forecastUtilization(Arrays.asList(0.70d, 0.85d, 0.99d));
        Assert.assertTrue("forecast must never exceed 1.0", forecast <= 1.0d);
    }

    @Test
    public void forecastOfSingleSampleIsThatSample() {
        Assert.assertEquals(0.42d, drs.forecastUtilization(Collections.singletonList(0.42d)), 0.0001d);
    }

    @Test
    public void forecastOfEmptyHistoryIsZero() {
        Assert.assertEquals(0d, drs.forecastUtilization(Collections.emptyList()), 0.0001d);
    }

    @Test
    public void rollingHistoryIsTrimmedToWindowNewestLast() {
        drs.recordAndGetUtilizationHistory(99L, 0.10d, 3);
        drs.recordAndGetUtilizationHistory(99L, 0.20d, 3);
        drs.recordAndGetUtilizationHistory(99L, 0.30d, 3);
        java.util.List<Double> history = drs.recordAndGetUtilizationHistory(99L, 0.40d, 3);
        Assert.assertEquals(Arrays.asList(0.20d, 0.30d, 0.40d), history);
    }

    @Test
    public void evacuationPlanPlacesEveryVmWhenTheyFit() {
        java.util.Map<Long, Double> vmNeeds = new java.util.HashMap<>();
        vmNeeds.put(1L, 30d);
        vmNeeds.put(2L, 20d);
        java.util.Map<Long, Double> hostFree = new java.util.HashMap<>();
        hostFree.put(10L, 50d);
        hostFree.put(11L, 40d);

        java.util.Map<Long, Long> plan = drs.planHostEvacuation(vmNeeds, hostFree);

        Assert.assertEquals(2, plan.size());
        Assert.assertTrue(plan.containsKey(1L) && plan.containsKey(2L));
    }

    @Test
    public void evacuationPlanIsEmptyWhenAVmCannotBePlaced() {
        java.util.Map<Long, Double> vmNeeds = new java.util.HashMap<>();
        vmNeeds.put(1L, 100d);
        java.util.Map<Long, Double> hostFree = new java.util.HashMap<>();
        hostFree.put(10L, 50d);

        Assert.assertTrue(drs.planHostEvacuation(vmNeeds, hostFree).isEmpty());
    }

    @Test
    public void evacuationPlanPacksLargestFirst() {
        java.util.Map<Long, Double> vmNeeds = new java.util.HashMap<>();
        vmNeeds.put(1L, 60d);
        vmNeeds.put(2L, 40d);
        vmNeeds.put(3L, 40d);
        java.util.Map<Long, Double> hostFree = new java.util.HashMap<>();
        hostFree.put(10L, 60d);
        hostFree.put(11L, 80d);

        java.util.Map<Long, Long> plan = drs.planHostEvacuation(vmNeeds, hostFree);

        Assert.assertEquals("all three VMs placed", 3, plan.size());
    }
}
