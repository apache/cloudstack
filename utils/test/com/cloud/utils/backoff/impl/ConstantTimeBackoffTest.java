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

package com.cloud.utils.backoff.impl;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;

public class ConstantTimeBackoffTest {
    final static private Log LOG = LogFactory.getLog(ConstantTimeBackoffTest.class);

    @Test
    public void waitBeforeRetryWithInterrupt() throws InterruptedException {
        final ConstantTimeBackoff backoff = new ConstantTimeBackoff();
        backoff.setTimeToWait(10);
        Assert.assertTrue(backoff.getWaiters().isEmpty());
        Thread waitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                backoff.waitBeforeRetry();
            }
        });
        waitThread.start();
        Thread.sleep(100);
        Assert.assertFalse(backoff.getWaiters().isEmpty());
        waitThread.interrupt();
        Thread.sleep(100);
        Assert.assertTrue(backoff.getWaiters().isEmpty());
    }

    @Test
    public void waitBeforeRetry() throws InterruptedException {
        final ConstantTimeBackoff backoff = new ConstantTimeBackoff();
        // let's not wait too much in a test
        backoff.setTimeToWait(0);
        // check if the list of waiters is empty
        Assert.assertTrue(backoff.getWaiters().isEmpty());
        // call the waitBeforeRetry which will wait 0 ms and return
        backoff.waitBeforeRetry();
        // on normal exit the list of waiters should be cleared
        Assert.assertTrue(backoff.getWaiters().isEmpty());
    }

    @Test
    public void configureEmpty() {
        // at this point this is the only way rhe configure method gets invoked,
        // therefore have to make sure it works correctly
        final ConstantTimeBackoff backoff = new ConstantTimeBackoff();
        backoff.configure("foo", new HashMap<String, Object>());
        Assert.assertEquals(5000, backoff.getTimeToWait());
    }

    @Test
    public void configureWithValue() {
        final ConstantTimeBackoff backoff = new ConstantTimeBackoff();
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("seconds", "100");
        backoff.configure("foo", params);
        Assert.assertEquals(100000, backoff.getTimeToWait());
    }

    /**
     * Test that wakeup returns false when trying to wake a non existing thread.
     */
    @Test
    public void wakeupNotExisting() {
        final ConstantTimeBackoff backoff = new ConstantTimeBackoff();
        Assert.assertFalse(backoff.wakeup("NOT EXISTING THREAD"));
    }

    /**
     * Test that wakeup will return true if the thread is waiting.
     */
    @Test
    public void wakeupExisting() throws InterruptedException {
        final ConstantTimeBackoff backoff = new ConstantTimeBackoff();
        backoff.setTimeToWait(10);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LOG.debug("before");
                backoff.waitBeforeRetry();
                LOG.debug("after");
            }
        });
        thread.start();
        LOG.debug("thread started");
        Thread.sleep(100);
        LOG.debug("testing wakeup");
        Assert.assertTrue(backoff.wakeup(thread.getName()));
    }
}
