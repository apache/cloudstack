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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.storage.Storage.StoragePoolType;

public class KVMStoragePoolManagerTest {

    @Test(timeout = 60000)
    public void testCreateWaitingForPoolLockDoesNotHoldManagerLock() throws Exception {
        /*
         * A teardown of a pool holds that pool's lock, and can sit in an umount for as long as the
         * storage takes to answer. A create of the same pool has to wait for it, but must do so
         * without holding the manager wide lock, or the creates of every other pool on the host wait
         * behind the umount too.
         */
        final KVMStoragePoolManager manager = Mockito.mock(KVMStoragePoolManager.class, Mockito.CALLS_REAL_METHODS);
        final KVMStoragePool pool = Mockito.mock(KVMStoragePool.class);
        final String uuid = String.valueOf(UUID.randomUUID());
        Mockito.doReturn(pool).when(manager).createStoragePoolSynchronized(eq(uuid), any(), anyInt(), any(), any(), any(), any(), anyBoolean());

        final AtomicReference<KVMStoragePool> created = new AtomicReference<>();
        final Thread create;
        synchronized (KVMStoragePoolLocks.get(uuid)) {
            // the teardown in progress
            create = new Thread(() -> created.set(manager.createStoragePool(uuid, "127.0.0.1", 0, "/export/primary",
                    null, StoragePoolType.NetworkFilesystem)));
            create.start();
            while (create.getState() != Thread.State.BLOCKED) {
                Assert.assertTrue("create finished while the pool's lock was held", create.isAlive());
                Thread.sleep(10);
            }

            final CountDownLatch managerLockTaken = new CountDownLatch(1);
            final Thread otherPool = new Thread(() -> {
                synchronized (manager) {
                    managerLockTaken.countDown();
                }
            });
            otherPool.start();
            Assert.assertTrue("the manager wide lock was held by a create waiting for its pool's lock",
                    managerLockTaken.await(30, TimeUnit.SECONDS));
            Assert.assertNull(created.get());
        }

        create.join(30000);
        Assert.assertSame(pool, created.get());
    }
}
