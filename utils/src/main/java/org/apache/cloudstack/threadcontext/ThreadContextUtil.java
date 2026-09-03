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

import com.cloud.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class, helps to propagate {@link ThreadContext} values from parent to child threads.
 *
 * @author mprokopchuk
 */
public class ThreadContextUtil {
    private static final Logger logger = LogManager.getLogger(ThreadContextUtil.class);

    public static final String MDC_UUID_KEY = "uuid";
    public static final String MDC_LOG_CONTEXT_ID_KEY = "logcontextid";
    public static final String CONTEXT_UUID_KEY = "uuid";
    public static final String CONTEXT_LOG_ID_KEY = "logid";

    /**
     * Wrap {@link Runnable} to propagate {@link ThreadContext} values.
     *
     * @param delegate
     * @return
     */
    public static Runnable wrapThreadContext(Runnable delegate) {
        @SuppressWarnings("unchecked")
        Map<String, String> context = ThreadContext.getContext() != null ?
                new HashMap<>(ThreadContext.getContext()) : null;

        return () -> {
            @SuppressWarnings("unchecked")
            Map<String, String> oldContext = ThreadContext.getContext() != null ?
                    new HashMap<>(ThreadContext.getContext()) : null;
            try {
                ThreadContext.clearMap();
                if (context != null) {
                    context.forEach(ThreadContext::put);
                }
                delegate.run();
            } finally {
                ThreadContext.clearMap();
                if (oldContext != null) {
                    oldContext.forEach(ThreadContext::put);
                }
            }
        };
    }

    /**
     * Set UUID in ThreadContext.
     *
     * @param uuid the UUID value to set
     */
    public static void setUuid(String uuid) {
        if (StringUtils.isNotEmpty(uuid)) {
            ThreadContext.put(MDC_UUID_KEY, uuid);
        }
    }

    /**
     * Set log context ID in ThreadContext.
     *
     * @param logContextId the log context ID value to set
     */
    public static void setLogContextId(String logContextId) {
        if (StringUtils.isNotEmpty(logContextId)) {
            ThreadContext.put(MDC_LOG_CONTEXT_ID_KEY, logContextId);
        }
    }

    /**
     * Extract UUID from JSON cmdInfo string and set it in MDC if UUID is not already present.
     * This is specifically used for async job processing.
     *
     * @param cmdInfo the JSON string containing command info
     */
    public static void extractAndSetUuidFromCmdInfo(String cmdInfo) {
        if (StringUtils.isBlank((String) ThreadContext.get(MDC_UUID_KEY)) && StringUtils.isNotBlank(cmdInfo)) {
            try {
                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                Gson gson = new Gson();
                Map<String, String> params = gson.fromJson(cmdInfo, mapType);
                String entityUuid = params.get(CONTEXT_UUID_KEY);
                if (StringUtils.isNotBlank(entityUuid)) {
                    ThreadContext.put(MDC_UUID_KEY, entityUuid);
                }
            } catch (Exception e) {
                logger.warn("Failed to extract UUID from cmdInfo", e);
            }
        }
    }
}
