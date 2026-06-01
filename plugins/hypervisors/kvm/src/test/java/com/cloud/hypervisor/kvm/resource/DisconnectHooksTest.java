//
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
//
package com.cloud.hypervisor.kvm.resource;

import com.cloud.hypervisor.kvm.resource.disconnecthook.DisconnectHook;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DisconnectHooksTest {
    class TestHook extends DisconnectHook {
        private boolean _started = false;
        private boolean _executed = false;
        private boolean _withTimeout = false;

        private long _runtime;

        public TestHook() { super("foo"); }

        public TestHook(long timeout, long runtime) {
            super("foo", timeout);
            _withTimeout = true;
            _runtime = runtime;
        }

        @Override
        public void run() {
            _started = true;
            if (this._withTimeout) {
                try {
                    Thread.sleep(this._runtime);
                } catch (InterruptedException e) {
                    throw new RuntimeException("TestHook interrupted while sleeping");
                }
            }
            this._executed = true;
        }

        protected boolean hasRun() {
            return _executed;
        }

        protected boolean hasStarted() {
            return _started;
        }
    }

    LibvirtComputingResource libvirtComputingResource;

    @Before
    public void setup() {
        libvirtComputingResource = new LibvirtComputingResource();
    }

    @Test
    public void addHookWithoutRun() {
        TestHook hook = new TestHook();
        libvirtComputingResource = new LibvirtComputingResource();
        libvirtComputingResource.addDisconnectHook(hook);

        // test that we added hook but did not run it
        Assert.assertEquals(1, libvirtComputingResource._disconnectHooks.size());
        Assert.assertFalse(hook.hasRun());
    }

    @Test
    public void addHookWithRun() {
        TestHook hook = new TestHook();
        libvirtComputingResource = new LibvirtComputingResource();
        libvirtComputingResource.addDisconnectHook(hook);

        // test that we added hook but did not run it
        Assert.assertEquals(1, libvirtComputingResource._disconnectHooks.size());
        Assert.assertFalse(hook.hasRun());

        // test that we run and remove hook on disconnect
        libvirtComputingResource.disconnected();

        Assert.assertTrue(hook.hasRun());
        Assert.assertEquals(0, libvirtComputingResource._disconnectHooks.size());
    }

    @Test
    public void addAndRemoveHooksWithAndWithoutRun() {
        TestHook hook1 = new TestHook();
        TestHook hook2 = new TestHook();
        libvirtComputingResource.addDisconnectHook(hook1);
        libvirtComputingResource.addDisconnectHook(hook2);

        Assert.assertEquals(2, libvirtComputingResource._disconnectHooks.size());
        Assert.assertFalse(hook1.hasRun());
        Assert.assertFalse(hook2.hasRun());

        // remove first hook but leave second hook
        libvirtComputingResource.removeDisconnectHook(hook1);
        libvirtComputingResource.disconnected();

        // ensure removed hook did not run
        Assert.assertFalse(hook1.hasRun());

        // ensure remaining hook did run
        Assert.assertTrue(hook2.hasRun());
        Assert.assertEquals(0, libvirtComputingResource._disconnectHooks.size());
    }

    @Test
    public void addAndRunHooksOneWithTimeout() {
        // test that hook stops running when we exceed timeout
        long timeout = 500;
        TestHook hook1 = new TestHook(timeout, timeout + 100);
        TestHook hook2 = new TestHook();
        libvirtComputingResource.addDisconnectHook(hook1);
        libvirtComputingResource.addDisconnectHook(hook2);
        libvirtComputingResource.disconnected();
        Assert.assertTrue(hook2.hasRun());

        try {
            Thread.sleep(timeout);
        } catch (Exception ignored){}

        Assert.assertTrue(hook1.hasStarted());
        Assert.assertFalse(hook1.isAlive());
        Assert.assertFalse(hook1.hasRun());

        Assert.assertEquals(0, libvirtComputingResource._disconnectHooks.size());
    }

    @Test
    public void addAndRunTwoHooksWithTimeout() {
        // test that hooks stop running when we exceed timeout
        // test for parallel timeout rather than additive
        long timeout = 500;
        TestHook hook1 = new TestHook(timeout, timeout + 100);
        TestHook hook2 = new TestHook(timeout, timeout + 100);
        libvirtComputingResource.addDisconnectHook(hook1);
        libvirtComputingResource.addDisconnectHook(hook2);
        libvirtComputingResource.disconnected();

        // if the timeouts were additive (e.g. if we were sequentially looping through join(timeout)), the second Hook
        // would get enough time to complete (500 for first Hook and 500 for itself) and not be interrupted.
        try {
            Thread.sleep(timeout*2);
        } catch (Exception ignored){}

        Assert.assertTrue(hook1.hasStarted());
        Assert.assertFalse(hook1.isAlive());
        Assert.assertFalse(hook1.hasRun());
        Assert.assertTrue(hook2.hasStarted());
        Assert.assertFalse(hook2.isAlive());
        Assert.assertFalse(hook2.hasRun());

        Assert.assertEquals(0, libvirtComputingResource._disconnectHooks.size());
    }

    @Test
    public void addAndRunTimeoutHooksToCompletion() {
        // test we can run to completion if we don't take as long as timeout, and they run parallel
        long timeout = 500;
        TestHook hook1 = new TestHook(timeout, timeout - 100);
        TestHook hook2 = new TestHook(timeout, timeout - 100);
        libvirtComputingResource.addDisconnectHook(hook1);
        libvirtComputingResource.addDisconnectHook(hook2);
        libvirtComputingResource.disconnected();

        try {
            Thread.sleep(timeout);
        } catch (Exception ignored){}

        Assert.assertTrue(hook1.hasStarted());
        Assert.assertTrue(hook1.hasRun());
        Assert.assertTrue(hook2.hasStarted());
        Assert.assertTrue(hook2.hasRun());
        Assert.assertEquals(0, libvirtComputingResource._disconnectHooks.size());
    }
}
