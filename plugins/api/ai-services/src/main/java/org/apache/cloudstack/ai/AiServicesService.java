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

import org.apache.cloudstack.ai.engine.CallerContext;
import org.apache.cloudstack.ai.engine.EngineClient;

import com.cloud.utils.component.PluggableService;

/**
 * The AI Services plugin's front door (SPEC.md section 9).
 *
 * Extends {@link PluggableService} so CloudStack registers the commands
 * this plugin provides and applies role permissions to them.
 */
public interface AiServicesService extends PluggableService {

    /** Whether AI Services is enabled in the given zone. */
    boolean isEnabledInZone(long zoneId);

    /** The engine client, configured from ai.services.engine.*. */
    EngineClient getEngineClient();

    /** The caller context for the current CloudStack session. */
    CallerContext currentCaller();

    /**
     * Remove every AI resource an account owns (SPEC.md section 9).
     *
     * @return how many ownership rows were removed.
     */
    int cleanupAccount(long accountId);

    /**
     * Run a command's work so CloudStack records how it ended.
     *
     * Every write command calls this instead of doing its work inline,
     * because `ActionEventInterceptor` is a Spring AOP advisor and so
     * only sees methods on *beans*. A command is not one — CloudStack
     * builds it with `ComponentContext.inject`, which autowires the
     * instance and returns it unproxied — so `@ActionEvent` on a
     * command's own `execute()` is never consulted, however correctly
     * it is written. The manager is a bean, which is why CloudStack's
     * own commands delegate to a manager for exactly this.
     *
     * The work is a `Runnable` rather than a value-returning call
     * because the interceptor only needs to know whether it threw:
     * returning normally records Completed, throwing records Failed,
     * and the command keeps its own result through its own fields.
     *
     * @param eventType        one of {@link AiServicesEvents}.
     * @param eventDescription what the operator should read.
     * @param work             the command's work; exceptions propagate.
     */
    void recordEvent(String eventType, String eventDescription, Runnable work);
}
