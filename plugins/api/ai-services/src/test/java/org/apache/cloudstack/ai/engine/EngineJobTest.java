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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Parsing engine jobs (SPEC.md section 10).
 *
 * The engine uses CloudStack's own status values deliberately, so these
 * assertions are really about that contract holding.
 */
class EngineJobTest {

    @Test
    void statusZeroIsPending() {
        EngineJob job = EngineJob.parse("{\"jobid\": \"j-1\", \"jobstatus\": 0}");

        assertTrue(job.isPending());
        assertFalse(job.isSuccess());
        assertEquals(EngineJob.PENDING, job.getStatus());
    }

    @Test
    void statusOneIsSuccess() {
        EngineJob job = EngineJob.parse(
                "{\"jobid\": \"j-1\", \"jobstatus\": 1, \"jobresult\": {\"ok\": true}}");

        assertTrue(job.isSuccess());
        assertTrue(job.getResult().get("ok").getAsBoolean());
    }

    @Test
    void statusTwoIsFailureWithItsText() {
        EngineJob job = EngineJob.parse(
                "{\"jobid\": \"j-1\", \"jobstatus\": 2, \"jobresult\":"
                        + " {\"errortext\": \"nope\", \"errorcode\": 530}}");

        assertTrue(job.isFailure());
        assertEquals("nope", job.getErrorText());
        assertEquals(530, job.getErrorCode());
    }

    @Test
    void aFailureWithoutDetailStillSaysSomething() {
        EngineJob job = EngineJob.parse(
                "{\"jobid\": \"j-1\", \"jobstatus\": 2, \"jobresult\": {}}");

        assertTrue(job.getErrorText().contains("no detail"));
        assertNull(job.getErrorCode());
    }

    @Test
    void theJobIdIsCarried() {
        assertEquals("abc-123",
                EngineJob.parse("{\"jobid\": \"abc-123\", \"jobstatus\": 0}").getJobId());
    }

    @Test
    void theAcceptedBodyOfAnAsyncCommandParses() {
        // A 202 carries only the job id; status defaults to pending.
        EngineJob job = EngineJob.parse("{\"jobid\": \"j-9\"}");

        assertEquals("j-9", job.getJobId());
        assertTrue(job.isPending());
    }

    @Test
    void aBodyWithoutAJobIdIsAnError() {
        EngineException failure = assertThrows(
                EngineException.class, () -> EngineJob.parse("{\"jobstatus\": 1}"));

        assertTrue(failure.getMessage().contains("no job id"));
    }

    @Test
    void anEmptyJobIdIsAnError() {
        assertThrows(EngineException.class,
                () -> EngineJob.parse("{\"jobid\": \"\", \"jobstatus\": 1}"));
    }

    @Test
    void unparseableJsonIsAnError() {
        EngineException failure = assertThrows(
                EngineException.class, () -> EngineJob.parse("<html>nope</html>"));

        assertTrue(failure.getMessage().contains("unreadable"));
    }

    @Test
    void aSuccessWithNoResultIsStillASuccess() {
        EngineJob job = EngineJob.parse("{\"jobid\": \"j-1\", \"jobstatus\": 1}");

        assertTrue(job.isSuccess());
        assertNull(job.getResult());
    }
}
