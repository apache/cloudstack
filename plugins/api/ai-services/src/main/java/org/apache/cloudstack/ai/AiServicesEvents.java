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

/**
 * Action event types (SPEC.md section 9).
 *
 * Every state change appears in CloudStack's event stream, which is what
 * section 14 means by "every action appears in CloudStack events". These
 * follow CloudStack's own dotted convention so existing event consumers
 * and alerting need no special handling.
 */
public final class AiServicesEvents {

    private AiServicesEvents() {
    }

    public static final String MODEL_MIRROR = "AI.MODEL.MIRROR";
    public static final String MODEL_PUBLISH = "AI.MODEL.PUBLISH";
    public static final String MODEL_DEPLOY = "AI.MODEL.DEPLOY";
    public static final String MODEL_UPDATE = "AI.MODEL.UPDATE";
    public static final String MODEL_DELETE = "AI.MODEL.DELETE";
    public static final String ROLLOUT_PROMOTE = "AI.ROLLOUT.PROMOTE";
    public static final String ROLLOUT_ROLLBACK = "AI.ROLLOUT.ROLLBACK";
    public static final String PROFILE_UPDATE = "AI.PROFILE.UPDATE";
    public static final String KEY_CREATE = "AI.KEY.CREATE";
    public static final String KEY_REVOKE = "AI.KEY.REVOKE";
    public static final String BUDGET_THRESHOLD = "AI.BUDGET.THRESHOLD";
    public static final String NODE_DRAIN = "AI.NODE.DRAIN";
    /**
     * Setting a budget, as against crossing one.
     *
     * BUDGET_THRESHOLD is an alert raised by spend; this is the
     * operator action that decided what the limit is, and an audit of
     * "who raised this account's ceiling" needs the second, not the
     * first.
     */
    public static final String BUDGET_UPDATE = "AI.BUDGET.UPDATE";
    /**
     * A playground session, which mints a key that expires in fifteen
     * minutes. Short-lived is not the same as unaudited: it is still a
     * credential against a model, issued to someone.
     */
    public static final String PLAYGROUND_SESSION = "AI.PLAYGROUND.SESSION";

    // Tier 2 (SPEC.md section 8).
    public static final String ADAPTER_UPLOAD = "AI.ADAPTER.UPLOAD";
    public static final String ADAPTER_DELETE = "AI.ADAPTER.DELETE";
    public static final String BATCH_CREATE = "AI.BATCH.CREATE";
    public static final String BATCH_CANCEL = "AI.BATCH.CANCEL";
    /** v2, with the guardrail profiles of section 8. */
    public static final String GUARDRAIL_BLOCK = "AI.GUARDRAIL.BLOCK";
    public static final String CERTIFICATE_UPLOAD = "AI.CERTIFICATE.UPLOAD";
    /**
     * Replacing a certificate, which is not an edit: a proxy reads its
     * TLS material once at boot, so a replacement replaces the proxy VM
     * and the endpoint goes down with it (CLAUDE.md). An operator
     * reading the log should see that as its own action.
     */
    public static final String CERTIFICATE_REPLACE = "AI.CERTIFICATE.REPLACE";
    public static final String CERTIFICATE_DELETE = "AI.CERTIFICATE.DELETE";

    public static String[] all() {
        return new String[] {
                MODEL_MIRROR, MODEL_PUBLISH, MODEL_DEPLOY, MODEL_UPDATE, MODEL_DELETE,
                ROLLOUT_PROMOTE, ROLLOUT_ROLLBACK, PROFILE_UPDATE, KEY_CREATE,
                KEY_REVOKE, BUDGET_THRESHOLD, BUDGET_UPDATE, PLAYGROUND_SESSION,
                NODE_DRAIN, GUARDRAIL_BLOCK,
                // v2. These were defined but left out of all(), so
                // nothing that walks this list — the convention and
                // uniqueness checks included — ever saw them.
                ADAPTER_UPLOAD, ADAPTER_DELETE, BATCH_CREATE, BATCH_CANCEL,
                CERTIFICATE_UPLOAD, CERTIFICATE_REPLACE, CERTIFICATE_DELETE,
        };
    }
}
