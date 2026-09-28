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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

public class TraceContextDataProviderTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";
    private static final String OTHER_TRACE_ID = "d75597dcda4f6e7b9c1a2b3c4d5e6f70";
    private static final String OTHER_SPAN_ID = "aabbccddeeff0011";

    private final TraceContextDataProvider provider = new TraceContextDataProvider();

    private static Scope activate(String traceId, String spanId) {
        return Context.root()
                .with(Span.wrap(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault())))
                .makeCurrent();
    }

    @Test
    public void suppliesTraceAndSpanIdOfTheActiveSpan() {
        try (Scope ignored = activate(TRACE_ID, SPAN_ID)) {
            Map<String, String> contextData = provider.supplyContextData();

            Assert.assertEquals(TRACE_ID, contextData.get(LogContext.TRACE_ID_KEY));
            Assert.assertEquals(SPAN_ID, contextData.get(LogContext.SPAN_ID_KEY));
        }
    }

    @Test
    public void suppliesNothingWhenNoSpanIsActive() {
        Assert.assertTrue(provider.supplyContextData().isEmpty());
    }

    @Test
    public void suppliesNothingOnceTheSpanScopeIsClosed() {
        Scope scope = activate(TRACE_ID, SPAN_ID);
        scope.close();

        Assert.assertTrue(provider.supplyContextData().isEmpty());
    }

    @Test
    public void followsTheInnermostActiveSpan() {
        try (Scope outer = activate(TRACE_ID, SPAN_ID)) {
            try (Scope inner = activate(OTHER_TRACE_ID, OTHER_SPAN_ID)) {
                Assert.assertEquals(OTHER_TRACE_ID, provider.supplyContextData().get(LogContext.TRACE_ID_KEY));
            }
            Assert.assertEquals(TRACE_ID, provider.supplyContextData().get(LogContext.TRACE_ID_KEY));
        }
    }

    @Test
    public void isRegisteredForServiceLoaderDiscovery() {
        Assert.assertNotNull("Log4j2 discovers the provider through META-INF/services; without that "
                        + "entry the trace ids never reach a log line",
                TraceContextDataProviderTest.class.getClassLoader()
                        .getResource("META-INF/services/org.apache.logging.log4j.core.util.ContextDataProvider"));
    }
}
