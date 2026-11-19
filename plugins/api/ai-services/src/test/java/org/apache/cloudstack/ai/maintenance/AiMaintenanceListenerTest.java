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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.events.Event;
import org.junit.jupiter.api.Test;

/**
 * Subscribing to host maintenance (SPEC.md section 7).
 *
 * <p>CloudStack 4.23 offers no synchronous pre-maintenance hook, so the
 * event bus is the only way a plugin hears about this at all — and the
 * bus is not enabled in a default install. These check both halves: that
 * a maintenance event reaches the drain, and that an install with no bus
 * is told so rather than quietly doing nothing.
 */
class AiMaintenanceListenerTest {

    private static final class RecordingDrain extends MaintenanceDrain {
        final List<Long> hosts = new ArrayList<>();

        RecordingDrain() {
            super(hostId -> List.of(), (n, c, t) -> Outcome.DRAINED, (h, n, r) -> {
            }, 900);
        }

        @Override
        public boolean beforeMaintenance(long hostId, org.apache.cloudstack.ai.engine.CallerContext caller) {
            hosts.add(hostId);
            return true;
        }
    }

    private Event maintenanceEvent(String entityId) {
        Event event = new Event("management-server", "ActionEvent", "MAINT.PREPARE", "Host", entityId);
        Map<String, String> details = new HashMap<>();
        details.put("entityuuid", entityId);
        event.setDescription("{\"entityuuid\":\"" + entityId + "\"}");
        return event;
    }

    @Test
    void aMaintenanceEventDrainsThatHost() {
        RecordingDrain drain = new RecordingDrain();
        AiMaintenanceListener listener = new AiMaintenanceListener(drain, uuid -> 42L);

        listener.onEvent(maintenanceEvent("host-uuid"));

        assertEquals(List.of(42L), drain.hosts);
    }

    @Test
    void anUnrelatedEventIsIgnored() {
        RecordingDrain drain = new RecordingDrain();
        AiMaintenanceListener listener = new AiMaintenanceListener(drain, uuid -> 42L);

        listener.onEvent(new Event("management-server", "ActionEvent", "VM.START", "VirtualMachine", "x"));

        assertTrue(drain.hosts.isEmpty());
    }

    @Test
    void anEventForAnUnknownHostIsIgnored() {
        // A host the plugin cannot resolve is not one it has nodes on.
        RecordingDrain drain = new RecordingDrain();
        AiMaintenanceListener listener = new AiMaintenanceListener(drain, uuid -> null);

        listener.onEvent(maintenanceEvent("ghost"));

        assertTrue(drain.hosts.isEmpty());
    }

    @Test
    void aFailureInTheDrainDoesNotEscapeIntoTheBus() {
        // An exception thrown back at the event bus can unsubscribe the
        // listener or stall other subscribers. This one logs and returns.
        AiMaintenanceListener listener = new AiMaintenanceListener(
                new MaintenanceDrain(hostId -> List.of("a"), (n, c, t) -> {
                    throw new IllegalStateException("the engine is unreachable");
                }, (h, n, r) -> {
                }, 900),
                uuid -> 42L);

        listener.onEvent(maintenanceEvent("host-uuid"));
    }

    @Test
    void theSubscribedTopicIsHostMaintenance() {
        assertEquals("MAINT.PREPARE", AiMaintenanceListener.MAINTENANCE_EVENT);
        assertEquals("Host", AiMaintenanceListener.HOST_CATEGORY);
    }

    @Test
    void theTopicFiltersToHostMaintenanceOnly() {
        assertNotNull(AiMaintenanceListener.topic());
        assertEquals("Host", AiMaintenanceListener.topic().getResourceType());
    }

    @Test
    void anInstallWithNoEventBusIsReportedAsInactive() {
        // The whole point: without a bus this never fires, and an
        // operator must be told rather than assume drains are automatic.
        AiMaintenanceListener listener = new AiMaintenanceListener(new RecordingDrain(), uuid -> 42L);

        assertFalse(listener.subscribe(null));
        assertFalse(listener.isActive());
    }

    @Test
    void theInactiveWarningSaysWhatToDoInstead() {
        AiMaintenanceListener listener = new AiMaintenanceListener(new RecordingDrain(), uuid -> 42L);
        listener.subscribe(null);

        assertTrue(listener.inactiveReason().contains("drainAiNode"));
        assertTrue(listener.inactiveReason().toLowerCase().contains("event bus"));
    }
}
