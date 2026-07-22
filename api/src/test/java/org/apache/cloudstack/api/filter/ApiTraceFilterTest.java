/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.cloudstack.context.LogContext;
import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;

public class ApiTraceFilterTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    private final ApiTraceFilter filter = new ApiTraceFilter();

    @After
    public void tearDown() {
        LogContext.unregister();
    }

    @Test
    public void putsMosaicTraceContextOnMdcWhenSpanActive() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader(LogContext.X_B3_TRACEID_KEY)).thenReturn(null);
        String[] duringChain = new String[2];
        FilterChain chain = (rq, rs) -> {
            duringChain[0] = (String) MDC.get(LogContext.MOSAIC_TRACE_ID_KEY);
            duringChain[1] = (String) MDC.get(LogContext.MOSAIC_SPAN_ID_KEY);
        };

        SpanContext spanContext =
                SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
        try (Scope scope = Span.wrap(spanContext).makeCurrent()) {
            filter.doFilter(req, Mockito.mock(ServletResponse.class), chain);
        }

        // Present on the MDC while the request is in flight, from the active span.
        Assert.assertEquals(TRACE_ID, duringChain[0]);
        Assert.assertEquals(SPAN_ID, duringChain[1]);
        // Removed once the request completes, so pooled threads do not leak it.
        Assert.assertNull(MDC.get(LogContext.MOSAIC_TRACE_ID_KEY));
        Assert.assertNull(MDC.get(LogContext.MOSAIC_SPAN_ID_KEY));
    }

    @Test
    public void leavesMosaicKeysUnsetWhenNoActiveSpan() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader(LogContext.X_B3_TRACEID_KEY)).thenReturn(null);
        boolean[] chainInvoked = {false};
        String[] duringChain = new String[1];
        FilterChain chain = (rq, rs) -> {
            chainInvoked[0] = true;
            duringChain[0] = (String) MDC.get(LogContext.MOSAIC_TRACE_ID_KEY);
        };

        // No span in scope: Span.current() is the invalid default span.
        filter.doFilter(req, Mockito.mock(ServletResponse.class), chain);

        Assert.assertTrue(chainInvoked[0]);
        Assert.assertNull(duringChain[0]);
        Assert.assertNull(MDC.get(LogContext.MOSAIC_TRACE_ID_KEY));
    }
}
