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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** What an engine replica answered. */
public class EngineResponse {

    private final int statusCode;
    private final String body;

    public EngineResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /** True for an async command's 202, which carries a job id to poll. */
    public boolean isAccepted() {
        return statusCode == 202;
    }

    /**
     * The engine's own account of a failure, ready to append, or "".
     *
     * <p>The engine answers in RFC 9457 problem JSON so that its
     * caller can say why something failed, and this plugin is its
     * only caller. Both the sync and the async path reported a bare
     * status until a live round trip showed what that costs: a
     * capacity call said "returned 503 for /v1/capacity" while the
     * body said "capacity has not been observed yet", and a refused
     * batch said "HTTP 400" while the body named the field.
     *
     * <p>It is appended to the status rather than replacing it,
     * because the status is what gets grepped for, and a body that is
     * not problem JSON yields nothing: a proxy or load balancer may
     * have answered, and a parse failure must not displace the real
     * one.
     */
    public String explain() {
        if (body.trim().isEmpty()) {
            return "";
        }
        try {
            JsonObject problem = JsonParser.parseString(body).getAsJsonObject();
            for (String field : new String[] {"detail", "title"}) {
                if (problem.has(field) && problem.get(field).isJsonPrimitive()) {
                    String said = problem.get(field).getAsString().trim();
                    if (!said.isEmpty()) {
                        return ": " + said;
                    }
                }
            }
        } catch (RuntimeException notProblemJson) {
            return "";
        }
        return "";
    }
}
