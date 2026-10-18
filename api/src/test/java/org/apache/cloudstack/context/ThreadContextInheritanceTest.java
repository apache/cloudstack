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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.ThreadContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Log4j 1.x backed the MDC with an InheritableThreadLocal, so a thread spawned while an API
 * request was being served saw the request's trace id for free. Log4j2 uses a plain ThreadLocal
 * unless log4j2.isThreadContextMapInheritable is set, and the ids would silently vanish from
 * every thread a request spawns. The management server sets the flag in JAVA_OPTS
 * (packaging/systemd/cloudstack-management.default) and surefire sets it for this module; this
 * test fails if either is dropped.
 */
public class ThreadContextInheritanceTest {

    private static final String TRACE_ID = "trace-from-parent";

    @After
    public void tearDown() {
        ThreadContext.clearMap();
    }

    @Test
    public void childThreadInheritsContextOfSpawningThread() throws InterruptedException {
        ThreadContext.put(LogContext.TRACEID_KEY, TRACE_ID);

        AtomicReference<String> seenByChild = new AtomicReference<>();
        Thread child = new Thread(() -> seenByChild.set(ThreadContext.get(LogContext.TRACEID_KEY)));
        child.start();
        child.join();

        Assert.assertEquals(TRACE_ID, seenByChild.get());
    }

    @Test
    public void childThreadDoesNotLeakContextBackToParent() throws InterruptedException {
        Thread child = new Thread(() -> ThreadContext.put(LogContext.TRACEID_KEY, "trace-from-child"));
        child.start();
        child.join();

        Assert.assertNull(ThreadContext.get(LogContext.TRACEID_KEY));
    }
}
