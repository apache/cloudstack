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

package org.apache.cloudstack.utils.cache;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SingleCacheTest {
    private final long durationSeconds = 1;
    private Supplier<String> mockLoader;

    @Before
    public void setUp() {
        mockLoader = Mockito.mock(Supplier.class);
        Mockito.when(mockLoader.get()).thenReturn("value");
    }

    @Test
    public void testValueIsCachedBetweenReads() {
        SingleCache<String> cache = new SingleCache<>(durationSeconds, mockLoader);
        assertEquals("value", cache.get());
        assertEquals("value", cache.get());
        Mockito.verify(mockLoader, Mockito.times(1)).get();
    }

    @Test
    public void testRefreshAfterWriteMode() throws InterruptedException {
        SingleCache<String> cache = new SingleCache<>(durationSeconds, true, mockLoader);
        cache.get();
        Thread.sleep((long) (1.1 * durationSeconds * 1000));
        cache.get();
        // refreshAfterWrite reloads asynchronously on the get after the interval; wait deterministically.
        Mockito.verify(mockLoader, Mockito.timeout(2000).times(2)).get();
    }

    @Test
    public void testExpireAfterWriteModeReloadsSynchronously() throws InterruptedException {
        SingleCache<String> cache = new SingleCache<>(durationSeconds, false, mockLoader);
        cache.get();
        Thread.sleep((long) (1.1 * durationSeconds * 1000));
        cache.get();
        // expireAfterWrite reloads synchronously on the get after expiry, so no async wait is needed.
        Mockito.verify(mockLoader, Mockito.times(2)).get();
    }
}
