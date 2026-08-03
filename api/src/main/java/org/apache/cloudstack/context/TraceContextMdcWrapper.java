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

import org.apache.log4j.MDC;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;

/**
 * Mirrors the active OpenTelemetry span onto the Log4j MDC so management-server log
 * lines carry mosaic_trace_id and mosaic_span_id on every thread that has an active
 * span (API requests, agent-command dispatch, async jobs), not just the servlet path.
 *
 * The OpenTelemetry agent populates the log MDC automatically for Log4j2 and Logback,
 * but not for Log4j 1.2 (reload4j), which the management server uses. This wrapper
 * fills that gap by hooking the OpenTelemetry context lifecycle: whenever a span
 * becomes current on a thread it copies the ids into the MDC, and restores the
 * previous values when that scope closes. Install once at startup via {@link #register()}.
 */
public class TraceContextMdcWrapper implements ContextStorage {

    private final ContextStorage delegate;

    TraceContextMdcWrapper(ContextStorage delegate) {
        this.delegate = delegate;
    }

    /**
     * Install the wrapper. Must be called before the first OpenTelemetry context is
     * used, i.e. at management-server startup, before the server accepts requests.
     */
    public static void register() {
        ContextStorage.addWrapper(TraceContextMdcWrapper::new);
    }

    @Override
    public Scope attach(Context toAttach) {
        Object previousTraceId = MDC.get(LogContext.MOSAIC_TRACE_ID_KEY);
        Object previousSpanId = MDC.get(LogContext.MOSAIC_SPAN_ID_KEY);
        SpanContext spanContext = Span.fromContext(toAttach).getSpanContext();
        if (spanContext.isValid()) {
            MDC.put(LogContext.MOSAIC_TRACE_ID_KEY, spanContext.getTraceId());
            MDC.put(LogContext.MOSAIC_SPAN_ID_KEY, spanContext.getSpanId());
        } else {
            MDC.remove(LogContext.MOSAIC_TRACE_ID_KEY);
            MDC.remove(LogContext.MOSAIC_SPAN_ID_KEY);
        }
        Scope delegateScope = delegate.attach(toAttach);
        return () -> {
            delegateScope.close();
            restore(LogContext.MOSAIC_TRACE_ID_KEY, previousTraceId);
            restore(LogContext.MOSAIC_SPAN_ID_KEY, previousSpanId);
        };
    }

    private static void restore(String key, Object previous) {
        if (previous != null) {
            MDC.put(key, previous);
        } else {
            MDC.remove(key);
        }
    }

    @Override
    public Context current() {
        return delegate.current();
    }
}
