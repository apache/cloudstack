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

package org.apache.cloudstack.context;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class RequestEntityCache {
    private static final class Key {
        final Class<?> type;
        final long id;
        Key(Class<?> type, long id) { this.type = type; this.id = id; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key k = (Key) o; return id == k.id && type.equals(k.type);
        }
        @Override public int hashCode() { return 31 * type.hashCode() + Long.hashCode(id); }
    }
    private static final class Entry {
        final Object value; final long expireAtNanos; // 0 = never expires
        Entry(Object v, long exp) { value = v; expireAtNanos = exp; }
    }

    private final ConcurrentHashMap<Key, Entry> map = new ConcurrentHashMap<>();
    private final long ttlNanos; // 0 = no TTL

    public RequestEntityCache(Duration ttl) { this.ttlNanos = ttl == null ? 0 : ttl.toNanos(); }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type, long id, Supplier<T> loader) {
        final long now = System.nanoTime();
        final Key key = new Key(type, id);
        Entry e = map.get(key);
        if (e != null && (e.expireAtNanos == 0 || now < e.expireAtNanos)) {
            return (T) e.value;
        }
        T loaded = loader.get();
        long exp = ttlNanos == 0 ? 0 : now + ttlNanos;
        if (loaded != null) map.put(key, new Entry(loaded, exp));
        return loaded;
    }

    public void clear() { map.clear(); }
}
