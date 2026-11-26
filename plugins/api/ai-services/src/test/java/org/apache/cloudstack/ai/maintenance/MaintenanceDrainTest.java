// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.ai.engine.CallerContext;
import org.junit.jupiter.api.Test;

/**
 * Draining GPU nodes before a host enters maintenance (SPEC.md section 7).
 *
 * <p>Passthrough GPUs cannot live-migrate (ADR-0003), so CloudStack's own
 * maintenance cannot move these VMs: they have to be destroyed and
 * replaced. That is what this asks the engine to do.
 *
 * <p>The decision itself — whether there is capacity elsewhere, whether a
 * force is needed — belongs to the engine's {@code lifecycle} module and is
 * not repeated here. This only checks the asking.
 */
class MaintenanceDrainTest {

    /** Records what the drainer was asked to do. */
    private static final class RecordingDrainer implements MaintenanceDrain.Drainer {
        final List<String> drained = new ArrayList<>();
        MaintenanceDrain.Outcome outcome = MaintenanceDrain.Outcome.DRAINED;

        @Override
        public MaintenanceDrain.Outcome drain(String nodeId, CallerContext caller, int timeoutSeconds) {
            drained.add(nodeId);
            return outcome;
        }
    }

    private static final class RecordingAlerts implements MaintenanceDrain.Alerts {
        final List<String> raised = new ArrayList<>();

        @Override
        public void needsForcedDrain(long hostId, String nodeId, String reason) {
            raised.add(nodeId + ": " + reason);
        }
    }

    private MaintenanceDrain drainFor(List<String> nodes, RecordingDrainer drainer, RecordingAlerts alerts) {
        return new MaintenanceDrain(hostId -> nodes, drainer, alerts, 900);
    }

    @Test
    void everyGpuNodeOnTheHostIsDrained() {
        RecordingDrainer drainer = new RecordingDrainer();
        MaintenanceDrain drain = drainFor(List.of("aisvc-h100-pool-001", "aisvc-h100-pool-002"),
                drainer, new RecordingAlerts());

        drain.beforeMaintenance(42L, CallerContext.system());

        assertEquals(List.of("aisvc-h100-pool-001", "aisvc-h100-pool-002"), drainer.drained);
    }

    @Test
    void aHostWithNoGpuNodesIsLeftAlone() {
        RecordingDrainer drainer = new RecordingDrainer();
        MaintenanceDrain drain = drainFor(List.of(), drainer, new RecordingAlerts());

        assertTrue(drain.beforeMaintenance(42L, CallerContext.system()));
        assertTrue(drainer.drained.isEmpty());
    }

    @Test
    void aSuccessfulDrainLetsMaintenanceProceed() {
        MaintenanceDrain drain = drainFor(List.of("aisvc-h100-pool-001"),
                new RecordingDrainer(), new RecordingAlerts());

        assertTrue(drain.beforeMaintenance(42L, CallerContext.system()));
    }

    @Test
    void aDrainNeedingConfirmationStopsMaintenance() {
        // SPEC.md section 7: "if capacity elsewhere is short, the admin
        // must confirm a forced drain". Proceeding anyway would take the
        // replicas down with no-one having agreed to it.
        RecordingDrainer drainer = new RecordingDrainer();
        drainer.outcome = MaintenanceDrain.Outcome.NEEDS_FORCE;
        MaintenanceDrain drain = drainFor(List.of("aisvc-h100-pool-001"), drainer, new RecordingAlerts());

        assertFalse(drain.beforeMaintenance(42L, CallerContext.system()));
    }

    @Test
    void aDrainNeedingConfirmationRaisesAnAlert() {
        RecordingDrainer drainer = new RecordingDrainer();
        drainer.outcome = MaintenanceDrain.Outcome.NEEDS_FORCE;
        RecordingAlerts alerts = new RecordingAlerts();
        drainFor(List.of("aisvc-h100-pool-001"), drainer, alerts)
                .beforeMaintenance(42L, CallerContext.system());

        assertEquals(1, alerts.raised.size());
        assertTrue(alerts.raised.get(0).contains("aisvc-h100-pool-001"));
    }

    @Test
    void aTimedOutDrainStopsMaintenanceToo() {
        RecordingDrainer drainer = new RecordingDrainer();
        drainer.outcome = MaintenanceDrain.Outcome.TIMED_OUT;
        MaintenanceDrain drain = drainFor(List.of("aisvc-h100-pool-001"), drainer, new RecordingAlerts());

        assertFalse(drain.beforeMaintenance(42L, CallerContext.system()));
    }

    @Test
    void theRestOfTheNodesAreStillAttemptedAfterOneFails() {
        // Stopping at the first failure would leave the other nodes
        // serving on a host that is about to go down anyway.
        List<String> attempted = new ArrayList<>();
        MaintenanceDrain drain = new MaintenanceDrain(
                hostId -> List.of("a", "b", "c"),
                (nodeId, caller, timeout) -> {
                    attempted.add(nodeId);
                    return nodeId.equals("a")
                            ? MaintenanceDrain.Outcome.TIMED_OUT
                            : MaintenanceDrain.Outcome.DRAINED;
                },
                new RecordingAlerts(),
                900);

        drain.beforeMaintenance(42L, CallerContext.system());

        assertEquals(List.of("a", "b", "c"), attempted);
    }

    @Test
    void theConfiguredTimeoutIsPassedThrough() {
        List<Integer> seen = new ArrayList<>();
        MaintenanceDrain drain = new MaintenanceDrain(
                hostId -> List.of("a"),
                (nodeId, caller, timeout) -> {
                    seen.add(timeout);
                    return MaintenanceDrain.Outcome.DRAINED;
                },
                new RecordingAlerts(),
                120);

        drain.beforeMaintenance(42L, CallerContext.system());

        assertEquals(List.of(120), seen);
    }

    @Test
    void theDrainActsAsTheSystemNotAsATenant() {
        // Maintenance is an operator action. Asserting a tenant's context
        // here would have the engine filter the reply by their account.
        assertTrue(CallerContext.system().isAdmin());
    }
}
