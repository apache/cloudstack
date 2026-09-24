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
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

public class SingleCache<V> {

    private final LoadingCache<Integer, V> cache;

    /**
     * Creates a single-value cache that refreshes asynchronously after the given duration
     * (refreshAfterWrite), serving the stale value while the reload runs.
     */
    public SingleCache(long refreshAfterWriteSeconds, Supplier<V> loader) {
        this(refreshAfterWriteSeconds, true, loader);
    }

    /**
     * Creates a single-value cache whose staleness strategy is selectable. See
     * {@link LazyCache#LazyCache(long, long, boolean, java.util.function.Function)} for the semantics
     * of refreshAfterWrite versus expireAfterWrite.
     */
    public SingleCache(long durationSeconds, boolean refreshAfterWrite, Supplier<V> loader) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder().maximumSize(1);
        if (refreshAfterWrite) {
            builder.refreshAfterWrite(durationSeconds, TimeUnit.SECONDS);
        } else {
            builder.expireAfterWrite(durationSeconds, TimeUnit.SECONDS);
        }
        this.cache = builder.build(key -> loader.get());
    }

    public V get() {
        return cache.get(0);
    }

    public void invalidate() {
        cache.invalidate(0);
    }

    public void clear() {
        cache.invalidateAll();
    }
}
