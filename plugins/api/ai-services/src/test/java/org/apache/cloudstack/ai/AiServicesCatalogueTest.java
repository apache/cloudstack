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
package org.apache.cloudstack.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.cloudstack.ai.dao.AiDeploymentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Events, usage types and the ownership row (SPEC.md section 9). */
class AiServicesCatalogueTest {

    @Test
    void everyEventTheSpecNamesExists() {
        Set<String> events = new HashSet<>(java.util.Arrays.asList(AiServicesEvents.all()));

        for (String expected : new String[] {
                "AI.MODEL.MIRROR", "AI.MODEL.PUBLISH", "AI.MODEL.DEPLOY",
                "AI.MODEL.UPDATE", "AI.MODEL.DELETE", "AI.ROLLOUT.PROMOTE",
                "AI.ROLLOUT.ROLLBACK", "AI.PROFILE.UPDATE", "AI.KEY.CREATE",
                "AI.KEY.REVOKE", "AI.BUDGET.THRESHOLD", "AI.NODE.DRAIN",
                "AI.GUARDRAIL.BLOCK"}) {
            assertTrue(events.contains(expected), "missing event " + expected);
        }
    }

    @Test
    void eventsFollowCloudStacksDottedConvention() {
        for (String event : AiServicesEvents.all()) {
            assertTrue(event.startsWith("AI."), event);
            assertEquals(event.toUpperCase(java.util.Locale.ROOT), event);
        }
    }

    @Test
    void eventNamesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (String event : AiServicesEvents.all()) {
            assertTrue(seen.add(event), "duplicate event " + event);
        }
    }

    @Test
    void usageTypesSitAboveCloudStacksOwnRange() {
        // CloudStack's built-in usage types stop well below 100, so a
        // provider's billing pipeline can tell ours apart unambiguously.
        for (int type : AiServicesUsageTypes.all()) {
            assertTrue(type >= 100, "usage type " + type + " could collide");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 101, 102, 103, 104})
    void everyUsageTypeDescribesItself(int type) {
        assertTrue(AiServicesUsageTypes.describe(type).startsWith("AI "));
    }

    @Test
    void anUnknownUsageTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AiServicesUsageTypes.describe(7));
    }

    @Test
    void theSpecsFourBillableThingsAreCovered() {
        // Tokens, GPU hours, adapter premium and batch tokens.
        assertEquals(5, AiServicesUsageTypes.all().length);
    }

    @Test
    void anOwnershipRowCarriesItsOwner() {
        AiDeploymentVO row = new AiDeploymentVO("llama", 5L, 2L, 1L, "eng-1");

        assertEquals("llama", row.getName());
        assertEquals(5L, row.getAccountId());
        assertEquals(2L, row.getDomainId());
        assertEquals(1L, row.getZoneId());
        assertEquals("eng-1", row.getEngineId());
    }

    @Test
    void anOwnershipRowGetsAUuidAndACreationTime() {
        AiDeploymentVO row = new AiDeploymentVO("m", 1L, 1L, 1L, "e");

        assertNotNull(row.getUuid());
        assertEquals(36, row.getUuid().length());
        assertNotNull(row.getCreated());
    }

    @Test
    void aNewRowStartsAllocatedAndNotRemoved() {
        AiDeploymentVO row = new AiDeploymentVO("m", 1L, 1L, 1L, "e");

        assertEquals("Allocated", row.getState());
        assertEquals(null, row.getRemoved());
    }

    @Test
    void theStateTracksWhatTheEngineReports() {
        AiDeploymentVO row = new AiDeploymentVO("m", 1L, 1L, 1L, "e");

        row.setState("Running");

        assertEquals("Running", row.getState());
    }

    @Test
    void twoRowsGetDifferentUuids() {
        assertTrue(!new AiDeploymentVO("m", 1L, 1L, 1L, "e").getUuid()
                .equals(new AiDeploymentVO("m", 1L, 1L, 1L, "e").getUuid()));
    }

    @Test
    void aRowPrintsReadablyForLogs() {
        String printed = new AiDeploymentVO("llama", 5L, 2L, 1L, "e").toString();

        assertTrue(printed.contains("llama"));
        assertTrue(printed.contains("account=5"));
    }
}
