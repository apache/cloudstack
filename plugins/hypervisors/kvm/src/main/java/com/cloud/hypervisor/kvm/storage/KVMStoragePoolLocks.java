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
package com.cloud.hypervisor.kvm.storage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * One monitor per storage pool uuid, shared by {@link KVMStoragePoolManager} and
 * {@link LibvirtStorageAdaptor} so that both take the same lock for a pool.
 *
 * The adaptor holds it across the whole of createStoragePool and deleteStoragePool. The refcount
 * alone cannot keep a pool alive: deleteStoragePool decides to tear the pool down when the count
 * reaches zero and then destroys and unmounts it, and a createStoragePool that finds the still
 * active pool and takes a reference in between would have it torn down underneath it.
 *
 * Whoever also takes the manager wide lock must take this one first. A teardown can sit in an
 * umount for as long as the storage takes to answer, and a create of that pool that waited for it
 * while holding the manager wide lock would hold up the creates of every other pool on the host.
 *
 * Entries are never removed, as there is one per pool the host has ever used.
 */
final class KVMStoragePoolLocks {
    private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();

    private KVMStoragePoolLocks() {
    }

    static Object get(String uuid) {
        return LOCKS.computeIfAbsent(uuid, k -> new Object());
    }
}
