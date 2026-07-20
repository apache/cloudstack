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

package org.apache.cloudstack.kms;

import org.apache.cloudstack.framework.kms.KMSException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for KMSManagerImpl's retryOperation() logic, covering
 * timeout enforcement, retry-on-transient-failure, and non-retryable fast-fail.
 * <p>
 * Config values (retry count, delay, timeout) are spied on so tests remain
 * fast without needing a full management-server config context.
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplRetryTest {

    @Spy
    @InjectMocks
    private KMSManagerImpl kmsManager;

    private ExecutorService executor;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kms-test");
            t.setDaemon(true);
            return t;
        });
        ReflectionTestUtils.setField(kmsManager, "kmsOperationExecutor", executor);
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Configure the spy to use a 1-second timeout, the given retry count, and no delay.
     */
    private void useShortConfig(int retries) {
        doReturn(1).when(kmsManager).getOperationTimeoutSec();
        doReturn(retries).when(kmsManager).getRetryCount();
        doReturn(0).when(kmsManager).getRetryDelayMs();
    }

    /**
     * Normal path: operation completes immediately, result returned.
     */
    @Test
    public void testRetryOperation_succeedsImmediately() throws Exception {
        useShortConfig(0);

        String result = kmsManager.retryOperation(() -> "ok");

        assertEquals("ok", result);
    }

    /**
     * Timeout path: operation never finishes within the configured timeout.
     * retryOperation() must unblock and throw a retryable KMSException.
     */
    @Test
    public void testRetryOperation_timesOutAndThrowsKMSException() {
        useShortConfig(0);

        try {
            kmsManager.retryOperation(() -> {
                Thread.sleep(5_000);
                return "should never reach here";
            });
            fail("Expected KMSException due to timeout");
        } catch (KMSException e) {
            assertTrue("Exception should be retryable (transient timeout)", e.isRetryable());
            assertTrue("Message should mention timeout", e.getMessage().contains("timed out"));
        } catch (Exception e) {
            fail("Expected KMSException, got: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Retry path: operation fails with a retryable KMSException on the first
     * attempt and succeeds on the second.  retryOperation() should return the
     * successful result.
     */
    @Test
    public void testRetryOperation_retriesOnTransientFailureAndSucceeds() throws Exception {
        useShortConfig(2);
        AtomicInteger attempts = new AtomicInteger(0);

        String result = kmsManager.retryOperation(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw KMSException.transientError("temporary HSM error", null);
            }
            return "recovered";
        });

        assertEquals("recovered", result);
        assertEquals("Should have taken exactly 2 attempts", 2, attempts.get());
    }

    /**
     * Non-retryable path: a KMSException with isRetryable() == false must be
     * re-thrown immediately without consuming any retry budget.
     */
    @Test
    public void testRetryOperation_nonRetryableExceptionFastFails() {
        useShortConfig(3);
        AtomicInteger attempts = new AtomicInteger(0);

        try {
            kmsManager.retryOperation(() -> {
                attempts.incrementAndGet();
                throw KMSException.invalidParameter("bad key label");
            });
            fail("Expected non-retryable KMSException");
        } catch (KMSException e) {
            assertFalse("Exception should NOT be retryable", e.isRetryable());
        } catch (Exception e) {
            fail("Expected KMSException, got: " + e.getClass().getName());
        }

        assertEquals("Non-retryable exception must not trigger retries", 1, attempts.get());
    }

    /**
     * Retry exhaustion on timeout: all attempts time out; retryOperation()
     * must eventually throw after exhausting the retry budget.
     */
    @Test
    public void testRetryOperation_exhaustsRetriesOnRepeatedTimeout() {
        useShortConfig(2); // 3 total attempts (initial + 2 retries), each timing out after 1s
        AtomicInteger attempts = new AtomicInteger(0);

        try {
            kmsManager.retryOperation(() -> {
                attempts.incrementAndGet();
                Thread.sleep(5_000);
                return "never";
            });
            fail("Expected KMSException after retry exhaustion");
        } catch (KMSException e) {
            assertTrue("Final exception should be retryable (timeout)", e.isRetryable());
        } catch (Exception e) {
            fail("Expected KMSException, got: " + e.getClass().getName());
        }

        assertEquals("Should have attempted exactly 3 times (1 initial + 2 retries)", 3, attempts.get());
    }
}
