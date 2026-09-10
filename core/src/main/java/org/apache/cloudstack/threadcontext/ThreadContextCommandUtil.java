// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.threadcontext;

import com.cloud.agent.api.Command;
import com.cloud.utils.StringUtils;
import org.apache.logging.log4j.ThreadContext;

/**
 * Utility class for Command-specific MDC operations.
 * This class handles propagation of MDC values to and from Command objects.
 *
 * @author mprokopchuk
 */
public class ThreadContextCommandUtil {

    /**
     * Propagate UUID and log context ID from Command trace context to MDC.
     *
     * @param cmd the command containing trace context parameters
     */
    public static void propagateContextFromCommand(Command cmd) {
        if (cmd != null) {
            ThreadContextUtil.setLogContextId(cmd.getTraceContextParam(ThreadContextUtil.CONTEXT_LOG_ID_KEY));
            ThreadContextUtil.setUuid(cmd.getTraceContextParam(ThreadContextUtil.CONTEXT_UUID_KEY));
        }
    }

    /**
     * Set UUID and log context ID in Command trace context from current MDC values.
     * MDC values are authoritative for the current thread; any pre-existing trace context
     * on the command is intentionally overwritten.
     *
     * @param cmd the command to set trace context parameters on
     */
    public static void setContextInCommand(Command cmd) {
        if (cmd != null) {
            String logContextId = (String) ThreadContext.get(ThreadContextUtil.MDC_LOG_CONTEXT_ID_KEY);
            if (StringUtils.isNotEmpty(logContextId)) {
                cmd.setTraceContextParam(ThreadContextUtil.CONTEXT_LOG_ID_KEY, logContextId);
            }

            String uuid = (String) ThreadContext.get(ThreadContextUtil.MDC_UUID_KEY);
            if (StringUtils.isNotEmpty(uuid)) {
                cmd.setTraceContextParam(ThreadContextUtil.CONTEXT_UUID_KEY, uuid);
            }
        }
    }
}
