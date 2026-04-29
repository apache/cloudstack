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

/**
 * Interface to define how an api limit store should work.
 *
 */
public interface LimitStore {

    /**
     * Returns a store entry for the given account. A value of null means that there is no
     * such entry and the calling client must call create to avoid
     * other clients potentially being blocked without any hope of progressing. A non-null
     * entry means that it has not expired and can be used to determine whether the current client should be allowed to
     * proceed with the rate-limited action or not.
     *
     */
    StoreEntry get(Long account);

    /**
     * Creates a new store entry
     *
     * @param account
     *            the user account, key to the store
     * @param timeToLiveInSecs
     *            the positive time-to-live in seconds
     * @return a non-null entry
     */
    StoreEntry create(Long account, int timeToLiveInSecs);

    void resetCounters();

}
