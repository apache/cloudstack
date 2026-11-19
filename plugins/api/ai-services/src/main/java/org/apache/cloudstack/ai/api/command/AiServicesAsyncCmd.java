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
package org.apache.cloudstack.ai.api.command;

import javax.inject.Inject;

import org.apache.cloudstack.ai.AiServicesConfig;
import org.apache.cloudstack.ai.AiServicesService;
import org.apache.cloudstack.ai.engine.EngineException;
import org.apache.cloudstack.ai.engine.EnginePaths;
import org.apache.cloudstack.ai.engine.EngineJob;
import org.apache.cloudstack.ai.engine.EngineJobPoller;
import org.apache.cloudstack.ai.engine.EngineResponse;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

/**
 * Bridges a CloudStack async job onto an engine job (SPEC.md section 9).
 *
 * "State-changing commands extend BaseAsyncCmd and run as CloudStack
 * async jobs that poll the engine's /v1/jobs/{id}." This method body runs
 * inside the CloudStack job, so blocking here is the point: the API
 * caller already holds a CloudStack job id to poll, and the two job
 * states stay in step because the engine uses CloudStack's own values.
 */
public abstract class AiServicesAsyncCmd extends BaseAsyncCmd {

    @Inject
    protected AiServicesService aiServices;

    /** Engine endpoint this command calls, for example /v1/deployments. */
    protected abstract String enginePath();

    /** The request body, or null for a verb that carries none. */
    protected String engineBody() {
        return null;
    }

    /** HTTP verb; POST unless a command says otherwise. */
    protected String engineMethod() {
        return "POST";
    }

    /** Turn the finished engine job into this command's response. */
    protected abstract void onSuccess(EngineJob job);

    @Override
    public void execute() throws ServerApiException {
        // Through the manager, because that is the only object Spring
        // proxies — see AiServicesService.recordEvent. ApiServer has
        // already written this command's Scheduled event; what the
        // recorder adds is the Completed or Failed that closes it, and
        // the whole poll has to sit inside the call for a job that
        // fails after being accepted to be recorded as Failed.
        aiServices.recordEvent(getEventType(), getEventDescription(), () -> {
            EngineJob job = runAndAwait();
            onSuccess(job);
        });
    }

    /**
     * Start the engine job and wait for it.
     *
     * A 202 is the contract; anything else means the engine did not
     * accept the command, and treating a 200 as success would skip the
     * job entirely and report work that never happened.
     */
    protected EngineJob runAndAwait() throws ServerApiException {
        try {
            EngineResponse accepted = send();
            if (!accepted.isAccepted()) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                        "the AI Services engine did not accept " + getEventType()
                                + ": HTTP " + accepted.getStatusCode()
                                + accepted.explain());
            }

            EngineJob started = EngineJob.parse(accepted.getBody());
            EngineJobPoller poller = new EngineJobPoller(
                    aiServices.getEngineClient(), timeoutMillis());
            return poller.await(started.getJobId(), aiServices.currentCaller());
        } catch (EngineException failure) {
            throw new ServerApiException(errorCodeFor(failure), failure.getMessage());
        }
    }

    private EngineResponse send() {
        String path = enginePath();
        switch (engineMethod()) {
            case "DELETE":
                return aiServices.getEngineClient().delete(path, aiServices.currentCaller());
            case "PATCH":
                return aiServices.getEngineClient()
                        .patch(path, aiServices.currentCaller(), engineBody());
            default:
                return aiServices.getEngineClient()
                        .post(path, aiServices.currentCaller(), engineBody());
        }
    }

    /**
     * Map an engine failure onto a CloudStack error code.
     *
     * A 403 from the engine is a permission decision and must not reach
     * the tenant as an internal error, which would hide why it failed.
     */
    static ApiErrorCode errorCodeFor(EngineException failure) {
        switch (failure.getStatusCode()) {
            case 400:
            case 422:
                return ApiErrorCode.PARAM_ERROR;
            case 403:
                return ApiErrorCode.ACCOUNT_ERROR;
            case 404:
                return ApiErrorCode.PARAM_ERROR;
            case 503:
                return ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR;
            default:
                return ApiErrorCode.INTERNAL_ERROR;
        }
    }

    private long timeoutMillis() {
        Integer seconds = AiServicesConfig.EngineTimeout.value();
        // The engine's own job may take far longer than one HTTP call;
        // mirroring a 70B model is minutes, so the poll budget is
        // separate from the per-request timeout.
        return (seconds == null ? 30L : seconds) * 60L * 1000L;
    }

    /** Append a query parameter to an engine path. */
    protected static String withQuery(String path, String name, Object value) {
        return EnginePaths.withQuery(path, name, value);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
