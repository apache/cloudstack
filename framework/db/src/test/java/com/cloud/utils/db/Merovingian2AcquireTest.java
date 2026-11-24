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
package com.cloud.utils.db;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Merovingian2AcquireTest {
    private static final String KEY = "networks42";

    // the lock table calls are stubbed, so no database is needed
    private final Merovingian2 lockController = Mockito.mock(Merovingian2.class, Mockito.CALLS_REAL_METHODS);

    @Test
    public void acquireWithoutWaitTakesFreeLock() {
        Mockito.doReturn(0).when(lockController).owns(KEY);
        Mockito.doReturn(true).when(lockController).doAcquire(Mockito.eq(KEY), Mockito.anyString(), Mockito.anyInt());

        Assert.assertTrue(lockController.acquire(KEY, 0));
        Mockito.verify(lockController).doAcquire(Mockito.eq(KEY), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void acquireWithoutWaitTriesOnceWhenLockIsHeldElsewhere() {
        Mockito.doReturn(-1).when(lockController).owns(KEY);

        Assert.assertFalse(lockController.acquire(KEY, 0));
        Mockito.verify(lockController, Mockito.times(1)).owns(KEY);
        Mockito.verify(lockController, Mockito.never()).doAcquire(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void acquireWithoutWaitReentersLockHeldByThisThread() {
        Mockito.doReturn(1).when(lockController).owns(KEY);
        Mockito.doReturn(true).when(lockController).increment(Mockito.eq(KEY), Mockito.anyString(), Mockito.anyInt());

        Assert.assertTrue(lockController.acquire(KEY, 0));
        Mockito.verify(lockController, Mockito.never()).doAcquire(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void acquireWithoutWaitFailsWhenInsertLosesRace() {
        Mockito.doReturn(0).when(lockController).owns(KEY);
        Mockito.doReturn(false).when(lockController).doAcquire(Mockito.eq(KEY), Mockito.anyString(), Mockito.anyInt());

        Assert.assertFalse(lockController.acquire(KEY, 0));
        Mockito.verify(lockController, Mockito.times(1)).doAcquire(Mockito.eq(KEY), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void acquireWithTimeoutStillWaitsForHeldLock() {
        // held on the first try, free on the second; the timeout is long enough for the 5 s retry interval
        Mockito.doReturn(-1).doReturn(0).when(lockController).owns(KEY);
        Mockito.doReturn(true).when(lockController).doAcquire(Mockito.eq(KEY), Mockito.anyString(), Mockito.anyInt());

        long start = System.currentTimeMillis();
        Assert.assertTrue(lockController.acquire(KEY, 30));
        long waited = System.currentTimeMillis() - start;

        Assert.assertTrue("waited only " + waited + " ms", waited >= 4900);
        Mockito.verify(lockController, Mockito.times(2)).owns(KEY);
        Mockito.verify(lockController, Mockito.times(1)).doAcquire(Mockito.eq(KEY), Mockito.anyString(), Mockito.anyInt());
    }
}
