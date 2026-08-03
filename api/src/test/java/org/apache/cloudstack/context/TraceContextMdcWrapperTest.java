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
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;

public class TraceContextMdcWrapperTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";
    private static final String OTHER_TRACE_ID = "d75597dcda4f6e7b9c1a2b3c4d5e6f70";
    private static final String OTHER_SPAN_ID = "aabbccddeeff0011";

    // Minimal delegate so we test the wrapper in isolation, no real context storage.
    private final ContextStorage noopDelegate = new ContextStorage() {
        @Override
        public Scope attach(Context toAttach) {
            return () -> { };
        }

        @Override
        public Context current() {
            return Context.root();
        }
    };

    private final TraceContextMdcWrapper wrapper = new TraceContextMdcWrapper(noopDelegate);

    @After
    public void tearDown() {
        MDC.remove(LogContext.MOSAIC_TRACE_ID_KEY);
        MDC.remove(LogContext.MOSAIC_SPAN_ID_KEY);
    }

    private static Context contextWithSpan(String traceId, String spanId) {
        return Context.root().with(Span.wrap(
                SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault())));
    }

    @Test
    public void putsTraceContextOnMdcWhileScopeOpenAndRestoresOnClose() {
        Scope scope = wrapper.attach(contextWithSpan(TRACE_ID, SPAN_ID));
        Assert.assertEquals(TRACE_ID, MDC.get(LogContext.MOSAIC_TRACE_ID_KEY));
        Assert.assertEquals(SPAN_ID, MDC.get(LogContext.MOSAIC_SPAN_ID_KEY));

        scope.close();
        Assert.assertNull(MDC.get(LogContext.MOSAIC_TRACE_ID_KEY));
        Assert.assertNull(MDC.get(LogContext.MOSAIC_SPAN_ID_KEY));
    }

    @Test
    public void leavesMdcUnsetWhenNoActiveSpan() {
        Scope scope = wrapper.attach(Context.root());
        Assert.assertNull(MDC.get(LogContext.MOSAIC_TRACE_ID_KEY));
        Assert.assertNull(MDC.get(LogContext.MOSAIC_SPAN_ID_KEY));
        scope.close();
    }

    @Test
    public void restoresOuterSpanWhenNestedScopeCloses() {
        Scope outer = wrapper.attach(contextWithSpan(TRACE_ID, SPAN_ID));
        Scope inner = wrapper.attach(contextWithSpan(OTHER_TRACE_ID, OTHER_SPAN_ID));
        Assert.assertEquals(OTHER_TRACE_ID, MDC.get(LogContext.MOSAIC_TRACE_ID_KEY));

        inner.close();
        Assert.assertEquals(TRACE_ID, MDC.get(LogContext.MOSAIC_TRACE_ID_KEY));

        outer.close();
        Assert.assertNull(MDC.get(LogContext.MOSAIC_TRACE_ID_KEY));
    }
}
