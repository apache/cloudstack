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
package org.apache.cloudstack.ai.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/**
 * Waiting on an engine job (SPEC.md sections 9 and 10).
 *
 * Driven with a fake clock and sleeper, so the poll loop is exercised
 * without any real time passing.
 */
class EngineJobPollerTest {

    private static final CallerContext CALLER =
            new CallerContext("u-1", "acme", "/Customers/Acme", "Admin");

    private final AtomicLong now = new AtomicLong(0);
    private final List<Long> sleeps = new ArrayList<>();

    private EngineJobPoller pollerOver(List<String> bodies, long timeoutMillis) {
        List<String> remaining = new ArrayList<>(bodies);
        EngineTransport transport = (method, url, headers, body) -> {
            String next = remaining.isEmpty()
                    ? bodies.get(bodies.size() - 1)
                    : remaining.remove(0);
            return new EngineResponse(200, next);
        };
        EngineClient client =
                new EngineClient(Collections.singletonList("https://engine"), transport);
        return new EngineJobPoller(
                client,
                100L,
                timeoutMillis,
                millis -> {
                    sleeps.add(millis);
                    now.addAndGet(millis);
                },
                now::get);
    }

    private static String pending(String id) {
        return "{\"jobid\": \"" + id + "\", \"jobstatus\": 0}";
    }

    private static String succeeded(String id) {
        return "{\"jobid\": \"" + id + "\", \"jobstatus\": 1,"
                + " \"jobresult\": {\"deployment\": {\"id\": \"d-1\"}}}";
    }

    private static String failed(String id) {
        return "{\"jobid\": \"" + id + "\", \"jobstatus\": 2,"
                + " \"jobresult\": {\"errortext\": \"Insufficient capacity\","
                + " \"errorcode\": 533}}";
    }

    @Test
    void anAlreadyFinishedJobReturnsWithoutSleeping() {
        EngineJob job = pollerOver(
                Collections.singletonList(succeeded("j-1")), 10_000L).await("j-1", CALLER);

        assertTrue(job.isSuccess());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void itPollsUntilTheJobFinishes() {
        EngineJobPoller poller = pollerOver(
                java.util.Arrays.asList(pending("j-1"), pending("j-1"), succeeded("j-1")),
                10_000L);

        EngineJob job = poller.await("j-1", CALLER);

        assertTrue(job.isSuccess());
        assertEquals(2, sleeps.size());
    }

    @Test
    void theResultIsCarriedBack() {
        EngineJob job = pollerOver(
                Collections.singletonList(succeeded("j-1")), 10_000L).await("j-1", CALLER);

        assertEquals("d-1",
                job.getResult().getAsJsonObject("deployment").get("id").getAsString());
    }

    @Test
    void aFailedJobRaisesItsErrorText() {
        EngineJobPoller poller =
                pollerOver(Collections.singletonList(failed("j-1")), 10_000L);

        EngineException failure =
                assertThrows(EngineException.class, () -> poller.await("j-1", CALLER));

        assertEquals("Insufficient capacity", failure.getMessage());
        assertEquals(533, failure.getStatusCode());
    }

    @Test
    void aTimeoutIsNotReportedAsSuccess() {
        // The work may still be running; calling it done is a lie the
        // operator would act on.
        EngineJobPoller poller =
                pollerOver(Collections.singletonList(pending("j-1")), 250L);

        EngineException failure =
                assertThrows(EngineException.class, () -> poller.await("j-1", CALLER));

        assertTrue(failure.getMessage().contains("did not finish"));
        assertTrue(failure.getMessage().contains("may still be running"));
    }

    @Test
    void anUnreadableJobResponseIsAnError() {
        EngineJobPoller poller =
                pollerOver(Collections.singletonList("not json"), 10_000L);

        assertThrows(EngineException.class, () -> poller.await("j-1", CALLER));
    }

    @Test
    void aNonSuccessStatusFromTheJobEndpointIsAnError() {
        EngineTransport transport =
                (method, url, headers, body) -> new EngineResponse(404, "{}");
        EngineClient client =
                new EngineClient(Collections.singletonList("https://engine"), transport);
        EngineJobPoller poller = new EngineJobPoller(client, 1000L);

        EngineException failure =
                assertThrows(EngineException.class, () -> poller.await("j-1", CALLER));

        assertTrue(failure.getMessage().contains("could not read engine job"));
        assertEquals(404, failure.getStatusCode());
    }
}
