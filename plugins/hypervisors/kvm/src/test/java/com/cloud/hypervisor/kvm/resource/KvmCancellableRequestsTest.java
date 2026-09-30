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
package com.cloud.hypervisor.kvm.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.hypervisor.kvm.resource.disconnecthook.DisconnectHook;

public class KvmCancellableRequestsTest {

    private static final long SEQ = 42L;

    private final KvmCancellableRequests requests = new KvmCancellableRequests();
    private final List<DisconnectHook> dropped = new ArrayList<>();

    private static final class CountingHook extends DisconnectHook {
        final AtomicInteger runs = new AtomicInteger();

        CountingHook() {
            super("counting", 2000);
        }

        @Override
        public void run() {
            runs.incrementAndGet();
        }
    }

    @After
    public void tearDown() {
        requests.end(SEQ);
    }

    @Test
    public void unknownSequenceIsNeitherCancellableNorCancelled() {
        Assert.assertFalse(requests.isCancellable(999L));
        Assert.assertFalse(requests.cancel(999L, dropped::add));
        Assert.assertFalse(requests.wasCancelRequested(999L));
    }

    @Test
    public void requestWithoutAHookHasNothingToStop() {
        requests.begin(SEQ, new CheckHealthCommand());

        Assert.assertFalse(requests.isCancellable(SEQ));
        Assert.assertFalse(requests.cancel(SEQ, dropped::add));
        Assert.assertTrue(requests.wasCancelRequested(SEQ));
    }

    @Test
    public void hookRegisteredOnTheExecutingThreadIsRunOnceOnCancel() {
        final CountingHook hook = new CountingHook();
        requests.begin(SEQ, new CheckHealthCommand());
        requests.attach(hook);

        Assert.assertTrue(requests.isCancellable(SEQ));
        Assert.assertTrue(requests.cancel(SEQ, dropped::add));
        Assert.assertEquals(1, hook.runs.get());
        Assert.assertEquals(1, dropped.size());

        // a Thread runs only once
        Assert.assertTrue(requests.cancel(SEQ, dropped::add));
        Assert.assertEquals(1, hook.runs.get());
    }

    @Test
    public void detachedHookIsNotRun() {
        final CountingHook hook = new CountingHook();
        requests.begin(SEQ, new CheckHealthCommand());
        requests.attach(hook);
        requests.detach(hook);

        Assert.assertEquals(0, requests.hookCount(SEQ));
        Assert.assertFalse(requests.isCancellable(SEQ));
        Assert.assertEquals(0, hook.runs.get());
    }

    @Test
    public void hookAttachedFromAnUnscopedThreadIsIgnored() throws Exception {
        final CountingHook hook = new CountingHook();
        requests.begin(SEQ, new CheckHealthCommand());

        final Thread other = new Thread(() -> requests.attach(hook));
        other.start();
        other.join();

        Assert.assertEquals(0, requests.hookCount(SEQ));
    }

    @Test
    public void zeroSequenceLeavesTheThreadUnscoped() {
        requests.begin(0L, new CheckHealthCommand());
        requests.attach(new CountingHook());

        Assert.assertFalse(requests.isCancellable(0L));
        requests.end(0L);
    }

    @Test
    public void endForgetsTheSequence() {
        requests.begin(SEQ, new CheckHealthCommand());
        requests.attach(new CountingHook());
        requests.end(SEQ);

        Assert.assertFalse(requests.isCancellable(SEQ));
        Assert.assertEquals(0, requests.hookCount(SEQ));
    }
}
