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
package org.apache.cloudstack.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.util.ContextDataProvider;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/**
 * Publishes the active OpenTelemetry span's ids to every log event, so management-server
 * log lines can be correlated with the traces they belong to. Loaded by Log4j2 through
 * {@code META-INF/services/org.apache.logging.log4j.core.util.ContextDataProvider}; the
 * key names come from {@link LogContext#TRACE_ID_KEY} and {@link LogContext#SPAN_ID_KEY},
 * which are read from server.properties.
 *
 * This is a Log4j2 ContextDataProvider rather than a ContextStorage wrapper on purpose.
 * A wrapper has to be installed before the first OpenTelemetry context is used, which is
 * not guaranteed under -javaagent (the agent's premain runs first, and a late
 * ContextStorage.addWrapper is silently ignored), and it pays for two ThreadContext
 * writes on every span attach - each of which copies the whole context map in Log4j2.
 * A provider is consulted lazily, only when a log event is actually created.
 *
 * Without -javaagent there is no active span, so this returns an empty map and adds
 * nothing to the log line.
 */
public class TraceContextDataProvider implements ContextDataProvider {

    @Override
    public Map<String, String> supplyContextData() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (!spanContext.isValid()) {
            return Collections.emptyMap();
        }
        Map<String, String> contextData = new HashMap<>(2);
        contextData.put(LogContext.TRACE_ID_KEY, spanContext.getTraceId());
        contextData.put(LogContext.SPAN_ID_KEY, spanContext.getSpanId());
        return contextData;
    }
}
