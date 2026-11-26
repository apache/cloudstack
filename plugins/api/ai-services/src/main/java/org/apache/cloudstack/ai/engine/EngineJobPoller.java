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

/**
 * Waits for an engine job to finish (SPEC.md section 9).
 *
 * "State-changing commands extend BaseAsyncCmd and run as CloudStack
 * async jobs that poll the engine's /v1/jobs/{id}." This runs inside
 * that CloudStack job, so blocking here is the intended behaviour: the
 * caller already has a CloudStack job id to poll.
 */
public class EngineJobPoller {

    /** Lets tests run the poll loop without real time passing. */
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    /** Lets tests advance a deadline without real time passing. */
    public interface Clock {
        long millis();
    }

    private static final long DEFAULT_INTERVAL_MILLIS = 2000L;

    private final EngineClient client;
    private final long intervalMillis;
    private final long timeoutMillis;
    private final Sleeper sleeper;
    private final Clock clock;

    public EngineJobPoller(EngineClient client, long timeoutMillis) {
        this(client, DEFAULT_INTERVAL_MILLIS, timeoutMillis, Thread::sleep,
                System::currentTimeMillis);
    }

    EngineJobPoller(
            EngineClient client,
            long intervalMillis,
            long timeoutMillis,
            Sleeper sleeper,
            Clock clock) {
        this.client = client;
        this.intervalMillis = intervalMillis;
        this.timeoutMillis = timeoutMillis;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    /**
     * Poll until the job leaves the pending state.
     *
     * @throws EngineException on failure, timeout, or an unreachable engine.
     *         A timeout is deliberately not a success: the work may still be
     *         running, and reporting it as done would be a lie the operator
     *         acts on.
     */
    public EngineJob await(String jobId, CallerContext caller) {
        long deadline = clock.millis() + timeoutMillis;

        while (true) {
            EngineResponse answer = client.get("/v1/jobs/" + jobId, caller);
            if (!answer.isSuccess()) {
                throw new EngineException(
                        "could not read engine job " + jobId + ": HTTP "
                                + answer.getStatusCode(),
                        answer.getStatusCode(), null);
            }

            EngineJob job = EngineJob.parse(answer.getBody());
            if (job.isFailure()) {
                throw new EngineException(
                        job.getErrorText(),
                        job.getErrorCode() == null ? 0 : job.getErrorCode(),
                        null);
            }
            if (!job.isPending()) {
                return job;
            }

            if (clock.millis() >= deadline) {
                throw new EngineException(
                        "engine job " + jobId + " did not finish within "
                                + (timeoutMillis / 1000) + "s; it may still be running");
            }

            try {
                sleeper.sleep(intervalMillis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new EngineException("interrupted waiting for engine job " + jobId);
            }
        }
    }
}
