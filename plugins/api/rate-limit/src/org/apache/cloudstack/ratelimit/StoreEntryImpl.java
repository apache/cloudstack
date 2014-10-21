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
package org.apache.cloudstack.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of limit store entry.
 *
 */
public class StoreEntryImpl implements StoreEntry {

    private final long expiry;

    private final AtomicInteger counter;

    StoreEntryImpl(int timeToLive) {
        this.expiry = System.currentTimeMillis() + timeToLive * 1000;
        this.counter = new AtomicInteger(0);
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() > expiry;
    }

    @Override
    public long getExpireDuration() {
        if (isExpired())
            return 0; // already expired
        else {
            return expiry - System.currentTimeMillis();
        }
    }

    @Override
    public int incrementAndGet() {
        return this.counter.incrementAndGet();
    }

    @Override
    public int getCounter() {
        return this.counter.get();
    }
}
