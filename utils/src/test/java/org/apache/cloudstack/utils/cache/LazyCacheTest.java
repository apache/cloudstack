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

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LazyCacheTest {
    private final long expireSeconds = 1;
    private final String cacheValuePrefix = "ComputedValueFor:";
    private LazyCache<String, String> cache;
    private Function<String, String> mockLoader;

    @Before
    public void setUp() {
        mockLoader = Mockito.mock(Function.class);
        Mockito.when(mockLoader.apply(Mockito.anyString())).thenAnswer(invocation -> cacheValuePrefix + invocation.getArgument(0));
        cache = new LazyCache<>(4, expireSeconds, mockLoader);
    }

    @Test
    public void testCacheMissAndLoader() {
        String key = "key1";
        String value = cache.get(key);
        assertEquals(cacheValuePrefix + key, value);
        Mockito.verify(mockLoader).apply(key);
    }

    @Test
    public void testLoaderNotCalledIfPresent() {
        String key = "key2";
        cache.get(key);
        try {
            Thread.sleep((long)(0.9 * expireSeconds * 1000));
        } catch (InterruptedException ie) {
            Assert.fail(String.format("Exception occurred: %s", ie.getMessage()));
        }
        cache.get(key);
        Mockito.verify(mockLoader, Mockito.times(1)).apply(key);
    }

    @Test
    public void testCacheExpiration() {
        String key = "key3";
        cache.get(key);
        try {
            Thread.sleep((long)(1.1 * expireSeconds * 1000));
        } catch (InterruptedException ie) {
            Assert.fail(String.format("Exception occurred: %s", ie.getMessage()));
        }
        cache.get(key);
        Mockito.verify(mockLoader, Mockito.times(2)).apply(key);
    }

    @Test
    public void testInvalidateKey() {
        String key = "key4";
        cache.get(key);
        cache.invalidate(key);
        cache.get(key);
        Mockito.verify(mockLoader, Mockito.times(2)).apply(key);
    }

    @Test
    public void testClearCache() {
        String key1 = "key5";
        String key2 = "key6";
        cache.get(key1);
        cache.get(key2);
        cache.clear();
        cache.get(key1);
        Mockito.verify(mockLoader, Mockito.times(2)).apply(key1);
        Mockito.verify(mockLoader, Mockito.times(1)).apply(key2);
    }

    @Test
    public void testMaximumSize() {
        String key = "key7";
        cache.get(key);
        for (int i = 0; i < 4; i++) {
            cache.get(String.format("newkey-%d", i));
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            Assert.fail(String.format("Exception occurred: %s", ie.getMessage()));
        }
        cache.get(key);
        Mockito.verify(mockLoader, Mockito.times(2)).apply(key);
    }
}
