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
package org.apache.cloudstack.backup;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class NASBackupProviderTest {

    private static void overrideDefaultConfigValue(final ConfigKey<?> key, final String value) throws Exception {
        final Field field = ConfigKey.class.getDeclaredField("_defaultValue");
        field.setAccessible(true);
        field.set(key, value);
    }

    @Test
    public void executionPolicyReachesTheCommand() throws Exception {
        final NASBackupProvider provider = new NASBackupProvider();
        final TakeBackupCommand command = new TakeBackupCommand("vm-1", "/backups/vm-1");

        overrideDefaultConfigValue(NASBackupProvider.NASBackupParallelExecution, "false");
        Assert.assertFalse(provider.applyExecutionPolicy(command, 1L));
        Assert.assertTrue("disabled setting must make the command sequential", command.executeInSequence());

        overrideDefaultConfigValue(NASBackupProvider.NASBackupParallelExecution, "true");
        Assert.assertTrue(provider.applyExecutionPolicy(command, 1L));
        Assert.assertFalse("enabled setting must let the command run concurrently", command.executeInSequence());
    }

    @Test
    public void perHostCapHoldsAndReleases() {
        final NASBackupProvider provider = new NASBackupProvider();
        provider.acquireHostBackupSlot(7L, 2, 1);
        provider.acquireHostBackupSlot(7L, 2, 1);
        Assert.assertEquals(2, provider.getInFlightBackups(7L));
        try {
            provider.acquireHostBackupSlot(7L, 2, 1);
            Assert.fail("third backup on a host capped at 2 must not get a slot");
        } catch (CloudRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("host 7"));
        }
        Assert.assertEquals(2, provider.getInFlightBackups(7L));
        provider.releaseHostBackupSlot(7L);
        provider.acquireHostBackupSlot(7L, 2, 1);
        Assert.assertEquals(2, provider.getInFlightBackups(7L));
        provider.releaseHostBackupSlot(7L);
        provider.releaseHostBackupSlot(7L);
        Assert.assertEquals(0, provider.getInFlightBackups(7L));
    }

    @Test
    public void hostsAreIndependent() {
        final NASBackupProvider provider = new NASBackupProvider();
        provider.acquireHostBackupSlot(1L, 1, 1);
        provider.acquireHostBackupSlot(2L, 1, 1);
        Assert.assertEquals(1, provider.getInFlightBackups(1L));
        Assert.assertEquals(1, provider.getInFlightBackups(2L));
        provider.releaseHostBackupSlot(1L);
        provider.releaseHostBackupSlot(2L);
    }

    @Test
    public void waiterProceedsWhenASlotFrees() throws Exception {
        final NASBackupProvider provider = new NASBackupProvider();
        provider.acquireHostBackupSlot(9L, 1, 10);
        final CountDownLatch acquired = new CountDownLatch(1);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final Thread waiter = new Thread(() -> {
            try {
                provider.acquireHostBackupSlot(9L, 1, 10);
                acquired.countDown();
            } catch (CloudRuntimeException e) {
                failed.set(true);
            }
        });
        waiter.start();
        Assert.assertFalse("waiter must block while the only slot is held", acquired.await(300, TimeUnit.MILLISECONDS));
        provider.releaseHostBackupSlot(9L);
        Assert.assertTrue("waiter must get the slot once it is released", acquired.await(5, TimeUnit.SECONDS));
        Assert.assertFalse(failed.get());
        Assert.assertEquals(1, provider.getInFlightBackups(9L));
        provider.releaseHostBackupSlot(9L);
        waiter.join(1000);
    }

    @Test
    public void releaseOnUnknownHostIsHarmless() {
        final NASBackupProvider provider = new NASBackupProvider();
        provider.releaseHostBackupSlot(404L);
        Assert.assertEquals(0, provider.getInFlightBackups(404L));
    }
}
