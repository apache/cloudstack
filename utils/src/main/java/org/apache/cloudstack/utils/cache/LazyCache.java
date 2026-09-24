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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

public class LazyCache<K, V> {

    private final LoadingCache<K, V> cache;

    /**
     * Creates a cache that refreshes entries asynchronously after the given duration
     * (refreshAfterWrite), serving the stale value while the reload runs.
     */
    public LazyCache(long maximumSize, long refreshAfterWriteSeconds, Function<K, V> loader) {
        this(maximumSize, refreshAfterWriteSeconds, true, loader);
    }

    /**
     * Creates a cache whose staleness strategy is selectable:
     * <ul>
     *   <li>refreshAfterWrite=true: after the duration, the next access returns the stale value and
     *       triggers an async reload - callers never block, but a stale value can be served briefly
     *       (weaker cross-node consistency).</li>
     *   <li>refreshAfterWrite=false: after the duration the entry expires; the next access blocks and
     *       loads a fresh value (stronger consistency, at the cost of a blocking load).</li>
     * </ul>
     */
    public LazyCache(long maximumSize, long durationSeconds, boolean refreshAfterWrite, Function<K, V> loader) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder().maximumSize(maximumSize);
        if (refreshAfterWrite) {
            builder.refreshAfterWrite(durationSeconds, TimeUnit.SECONDS);
        } else {
            builder.expireAfterWrite(durationSeconds, TimeUnit.SECONDS);
        }
        this.cache = builder.build(loader::apply);
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public void invalidate(K key) {
        cache.invalidate(key);
    }

    public void clear() {
        cache.invalidateAll();
    }
}
