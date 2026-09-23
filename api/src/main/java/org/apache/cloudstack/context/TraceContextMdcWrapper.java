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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.ThreadContext;

/**
 * Mirrors the active OpenTelemetry span onto the Log4j MDC so management-server log
 * lines carry trace and span ids on every thread that has an active
 * span (API requests, agent-command dispatch, async jobs), not just the servlet path.
 * The MDC key names come from {@link LogContext#TRACE_ID_KEY} and
 * {@link LogContext#SPAN_ID_KEY}, which are environment driven.
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
        String previousTraceId = ThreadContext.get(LogContext.TRACE_ID_KEY);
        String previousSpanId = ThreadContext.get(LogContext.SPAN_ID_KEY);
        SpanContext spanContext = Span.fromContext(toAttach).getSpanContext();
        if (spanContext.isValid()) {
            ThreadContext.put(LogContext.TRACE_ID_KEY, spanContext.getTraceId());
            ThreadContext.put(LogContext.SPAN_ID_KEY, spanContext.getSpanId());
        } else {
            ThreadContext.remove(LogContext.TRACE_ID_KEY);
            ThreadContext.remove(LogContext.SPAN_ID_KEY);
        }
        Scope delegateScope = delegate.attach(toAttach);
        return () -> {
            delegateScope.close();
            restore(LogContext.TRACE_ID_KEY, previousTraceId);
            restore(LogContext.SPAN_ID_KEY, previousSpanId);
        };
    }

    private static void restore(String key, String previous) {
        if (previous != null) {
            ThreadContext.put(key, previous);
        } else {
            ThreadContext.remove(key);
        }
    }

    @Override
    public Context current() {
        return delegate.current();
    }
}
