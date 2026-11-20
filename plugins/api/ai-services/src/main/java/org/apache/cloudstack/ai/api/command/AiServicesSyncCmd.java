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

import org.apache.cloudstack.ai.AiServicesService;
import org.apache.cloudstack.ai.engine.EngineException;
import org.apache.cloudstack.ai.engine.EnginePaths;
import org.apache.cloudstack.ai.engine.EngineResponse;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A command that calls the engine and answers immediately
 * (SPEC.md section 9).
 *
 * Every response is already filtered by the engine against the caller
 * context, so these commands pass identity through rather than deciding
 * visibility themselves — one place to get it right instead of thirty.
 */
public abstract class AiServicesSyncCmd extends BaseCmd {

    @Inject
    protected AiServicesService aiServices;

    protected abstract String enginePath();

    protected String engineMethod() {
        return "GET";
    }

    protected String engineBody() {
        return null;
    }

    /** Build this command's response from the engine's answer. */
    protected abstract void onSuccess(JsonObject body);

    /**
     * The event this command records, or null if it records none.
     *
     * A read is not an event, and a list command inherits this null —
     * which is the whole gate: {@link #execute()} records nothing
     * unless a command names a type here. The generator emits an
     * override for every write that declares one in the command map.
     */
    public String getEventType() {
        return null;
    }

    /** What an operator should read in the event log. */
    public String getEventDescription() {
        return null;
    }

    @Override
    public void execute() throws ServerApiException {
        String eventType = getEventType();
        if (eventType == null) {
            doExecute();
            return;
        }
        // Through the manager, because that is the only object Spring
        // proxies — see AiServicesService.recordEvent. The work has to
        // happen *inside* this call for a failure to be recorded as
        // Failed rather than leaving the command's event unfinished.
        aiServices.recordEvent(eventType, getEventDescription(), this::doExecute);
    }

    private void doExecute() {
        JsonObject body = callEngine();
        onSuccess(body);
    }

    protected JsonObject callEngine() throws ServerApiException {
        try {
            EngineResponse answer = send();
            if (!answer.isSuccess()) {
                throw new ServerApiException(
                        AiServicesAsyncCmd.errorCodeFor(
                                new EngineException("", answer.getStatusCode(), null)),
                        "the AI Services engine returned " + answer.getStatusCode()
                                + " for " + enginePath() + answer.explain());
            }
            return parse(answer.getBody());
        } catch (EngineException failure) {
            throw new ServerApiException(
                    AiServicesAsyncCmd.errorCodeFor(failure), failure.getMessage());
        }
    }

    private EngineResponse send() {
        String path = enginePath();
        switch (engineMethod()) {
            case "POST":
                return aiServices.getEngineClient()
                        .post(path, aiServices.currentCaller(), engineBody());
            case "PATCH":
                return aiServices.getEngineClient()
                        .patch(path, aiServices.currentCaller(), engineBody());
            case "DELETE":
                return aiServices.getEngineClient().delete(path, aiServices.currentCaller());
            default:
                return aiServices.getEngineClient().get(path, aiServices.currentCaller());
        }
    }

    static JsonObject parse(String body) {
        if (body == null || body.trim().isEmpty()) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException malformed) {
            throw new EngineException(
                    "the AI Services engine returned an unreadable body: "
                            + malformed.getMessage());
        }
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
