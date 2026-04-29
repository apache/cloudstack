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
package com.cloud.hypervisor.vmware.util;

import com.cloud.utils.concurrency.NamedThreadFactory;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VmwareContextPoolTest {

    private class PoolClient implements Runnable {
        private final VmwareContextPool pool;
        private volatile Boolean canRun = true;
        private int counter = 0;

        public PoolClient(final VmwareContextPool pool) {
            this.pool = pool;
        }

        public int count() {
            return counter;
        }

        public void stop() {
            canRun = false;
        }

        @Override
        public void run() {
            final String poolKey = pool.composePoolKey(vmwareAddress, vmwareUsername);
            while (canRun) {
                pool.registerContext(createDummyContext(pool, poolKey));
                counter++;
            }
        }
    }

    private VmwareContextPool vmwareContextPool;
    private VmwareContext vmwareContext;
    private String vmwareAddress = "address";
    private String vmwareUsername = "username";

    private int contextLength = 10;
    private Duration idleCheckInterval = Duration.millis(1000L);

    public VmwareContext createDummyContext(final VmwareContextPool pool, final String poolKey) {
        VmwareClient vimClient = new VmwareClient("someAddress");
        VmwareContext context = new VmwareContext(vimClient, "someAddress");
        context.setPoolInfo(pool, poolKey);
        return context;
    }

    @Before
    public void setUp() throws Exception {
        final String poolKey = vmwareContextPool.composePoolKey(vmwareAddress, vmwareUsername);
        vmwareContextPool = new VmwareContextPool(contextLength, idleCheckInterval);
        vmwareContext = createDummyContext(vmwareContextPool, poolKey);
    }

    @Test
    public void testRegisterContext() throws Exception {
        vmwareContextPool.registerContext(vmwareContext);
        Assert.assertEquals(vmwareContextPool.getContext(vmwareAddress, vmwareUsername), vmwareContext);
    }

    @Test
    public void testUnregisterContext() throws Exception {
        vmwareContextPool.unregisterContext(vmwareContext);
        Assert.assertNull(vmwareContextPool.getContext(vmwareAddress, vmwareUsername));
    }

    @Test
    public void testComposePoolKey() throws Exception {
        Assert.assertEquals(vmwareContextPool.composePoolKey(vmwareAddress, vmwareUsername), vmwareUsername + "@" + vmwareAddress);
    }

    @Test
    public void testMultithreadedPoolClients() throws Exception {
        vmwareContextPool = Mockito.spy(vmwareContextPool);
        final ExecutorService executor = Executors.newFixedThreadPool(10, new NamedThreadFactory("VmwareContextPoolClients"));
        final List<PoolClient> clients = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            final PoolClient client = new PoolClient(vmwareContextPool);
            clients.add(client);
            executor.submit(client);
        }
        Thread.sleep(1000);
        executor.shutdown();
        int totalRegistrations = 0;
        for (final PoolClient client : clients) {
            client.stop();
            totalRegistrations += client.count();
        }
        Mockito.verify(vmwareContextPool, Mockito.atLeast(totalRegistrations)).registerContext(Mockito.any(VmwareContext.class));
    }
}
