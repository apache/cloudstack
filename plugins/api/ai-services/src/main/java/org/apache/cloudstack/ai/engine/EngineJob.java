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

/**
 * One engine async job (SPEC.md section 10).
 *
 * The engine deliberately uses CloudStack's own status values — 0
 * pending, 1 success, 2 failure — so the plugin maps a job onto a
 * CloudStack async job without translating anything.
 */
public class EngineJob {

    public static final int PENDING = 0;
    public static final int SUCCESS = 1;
    public static final int FAILURE = 2;

    private final String jobId;
    private final int status;
    private final JsonObject result;
    private final String errorText;
    private final Integer errorCode;

    EngineJob(String jobId, int status, JsonObject result, String errorText, Integer errorCode) {
        this.jobId = jobId;
        this.status = status;
        this.result = result;
        this.errorText = errorText;
        this.errorCode = errorCode;
    }

    /** Parse a /v1/jobs/{id} body, or the 202 that started it. */
    public static EngineJob parse(String body) {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException malformed) {
            throw new EngineException(
                    "the engine returned an unreadable job document: " + malformed.getMessage());
        }

        String id = root.has("jobid") && !root.get("jobid").isJsonNull()
                ? root.get("jobid").getAsString()
                : null;
        if (id == null || id.isEmpty()) {
            throw new EngineException("the engine returned no job id");
        }

        int status = root.has("jobstatus") ? root.get("jobstatus").getAsInt() : PENDING;
        JsonObject result = root.has("jobresult") && root.get("jobresult").isJsonObject()
                ? root.getAsJsonObject("jobresult")
                : null;

        String errorText = null;
        Integer errorCode = null;
        if (status == FAILURE && result != null) {
            errorText = result.has("errortext") && !result.get("errortext").isJsonNull()
                    ? result.get("errortext").getAsString()
                    : "the engine reported a failure with no detail";
            if (result.has("errorcode") && !result.get("errorcode").isJsonNull()) {
                errorCode = result.get("errorcode").getAsInt();
            }
        }

        return new EngineJob(id, status, result, errorText, errorCode);
    }

    public String getJobId() {
        return jobId;
    }

    public int getStatus() {
        return status;
    }

    public boolean isPending() {
        return status == PENDING;
    }

    public boolean isSuccess() {
        return status == SUCCESS;
    }

    public boolean isFailure() {
        return status == FAILURE;
    }

    public JsonObject getResult() {
        return result;
    }

    public String getErrorText() {
        return errorText;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
