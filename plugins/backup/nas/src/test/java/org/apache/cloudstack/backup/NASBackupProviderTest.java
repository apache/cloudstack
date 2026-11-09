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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

public class NASBackupProviderTest {

    private static void overrideDefaultConfigValue(final ConfigKey<?> key, final String value) throws Exception {
        final Field field = ConfigKey.class.getDeclaredField("_defaultValue");
        field.setAccessible(true);
        field.set(key, value);
        final Field cached = ConfigKey.class.getDeclaredField("_value");
        cached.setAccessible(true);
        cached.set(key, null);
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

    /**
     * The database as every management server sees it: the backups table, a VM placement table and the
     * per-host cluster-wide lock (GlobalLock in production, a JVM lock here).
     */
    private static final class SharedDb {
        final List<BackupVO> rows = new ArrayList<>();
        final AtomicLong nextId = new AtomicLong(1);
        final Map<Long, VMInstanceVO> vms = new HashMap<>();
        final Map<Long, ReentrantLock> hostLocks = new HashMap<>();
        final BackupDao backupDao = Mockito.mock(BackupDao.class);
        final VMInstanceDao vmInstanceDao = Mockito.mock(VMInstanceDao.class);
        final BackupOfferingDao backupOfferingDao = Mockito.mock(BackupOfferingDao.class);
        final VolumeDao volumeDao = Mockito.mock(VolumeDao.class);

        SharedDb() {
            Mockito.when(volumeDao.findByInstance(Mockito.anyLong())).thenReturn(Collections.emptyList());
            Mockito.when(backupOfferingDao.findByIdIncludingRemoved(Mockito.anyLong()))
                    .thenReturn(new BackupOfferingVO(1L, "nas-repo", "nas", "nas", "nas", true));
            Mockito.when(backupDao.persist(Mockito.any(BackupVO.class))).thenAnswer(inv -> {
                final BackupVO backup = inv.getArgument(0);
                setField(backup, "id", nextId.getAndIncrement());
                synchronized (rows) {
                    rows.add(backup);
                }
                return backup;
            });
            Mockito.when(backupDao.listByZoneAndStatus(Mockito.anyLong(), Mockito.any(Backup.Status.class))).thenAnswer(inv -> {
                final long zoneId = inv.getArgument(0);
                final Backup.Status status = inv.getArgument(1);
                synchronized (rows) {
                    return rows.stream().filter(b -> b.getRemoved() == null && b.getZoneId() == zoneId && b.getStatus() == status)
                            .collect(Collectors.toList());
                }
            });
            Mockito.when(backupDao.remove(Mockito.anyLong())).thenAnswer(inv -> {
                final long id = inv.getArgument(0);
                synchronized (rows) {
                    rows.stream().filter(b -> b.getId() == id).forEach(b -> b.setRemoved(new Date()));
                }
                return true;
            });
            Mockito.when(backupDao.update(Mockito.anyLong(), Mockito.any(BackupVO.class))).thenReturn(true);
            Mockito.when(backupDao.findById(Mockito.anyLong())).thenAnswer(inv -> {
                final long id = inv.getArgument(0);
                synchronized (rows) {
                    return rows.stream().filter(b -> b.getId() == id && b.getRemoved() == null).findFirst().orElse(null);
                }
            });
            Mockito.when(backupDao.listByVmId(Mockito.any(), Mockito.anyLong())).thenAnswer(inv -> {
                final long vmId = inv.getArgument(1);
                synchronized (rows) {
                    return rows.stream().filter(b -> b.getRemoved() == null && b.getVmId() == vmId).collect(Collectors.toList());
                }
            });
            Mockito.when(vmInstanceDao.findByIdIncludingRemoved(Mockito.anyLong())).thenAnswer(inv -> vms.get((Long) inv.getArgument(0)));
        }

        VMInstanceVO vm(final long vmId, final Long hostId, final Long lastHostId) {
            final VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
            Mockito.when(vm.getId()).thenReturn(vmId);
            Mockito.when(vm.getHostId()).thenReturn(hostId);
            Mockito.when(vm.getLastHostId()).thenReturn(lastHostId);
            Mockito.when(vm.getDataCenterId()).thenReturn(1L);
            Mockito.when(vm.getBackupOfferingId()).thenReturn(1L);
            Mockito.when(vm.getInstanceName()).thenReturn("i-2-" + vmId + "-VM");
            vms.put(vmId, vm);
            return vm;
        }

        int inFlightOn(final long hostId) {
            synchronized (rows) {
                return (int) rows.stream().filter(b -> b.getRemoved() == null && b.getStatus() == Backup.Status.BackingUp)
                        .filter(b -> {
                            final VMInstanceVO vm = vms.get(b.getVmId());
                            return vm != null && Long.valueOf(hostId).equals(vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId());
                        }).count();
            }
        }

        synchronized ReentrantLock hostLock(final long hostId) {
            return hostLocks.computeIfAbsent(hostId, id -> new ReentrantLock());
        }

        /** One management server: its own provider instance, sharing nothing with the others except this database. */
        NASBackupProvider managementServer() {
            final NASBackupProvider provider = new NASBackupProvider() {
                @Override
                protected <T> T runUnderHostLock(final long hostId, final Supplier<T> action) {
                    final ReentrantLock lock = hostLock(hostId);
                    lock.lock();
                    try {
                        return action.get();
                    } finally {
                        lock.unlock();
                    }
                }
            };
            provider.slotPollMinMillis = 5L;
            provider.slotPollMaxMillis = 20L;
            setField(provider, "backupDao", backupDao);
            setField(provider, "vmInstanceDao", vmInstanceDao);
            setField(provider, "backupOfferingDao", backupOfferingDao);
            setField(provider, "volumeDao", volumeDao);
            return provider;
        }
    }

    private static void setField(final Object target, final String name, final Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("no field " + name);
    }

    @Test
    public void perHostCapHoldsAndReleases() {
        final SharedDb db = new SharedDb();
        final NASBackupProvider provider = db.managementServer();
        final BackupVO first = provider.reserveHostBackupSlot(db.vm(1L, 7L, 7L), 7L, "a", 2, 1);
        provider.reserveHostBackupSlot(db.vm(2L, 7L, 7L), 7L, "b", 2, 1);
        Assert.assertEquals(2, provider.countInFlightBackups(7L, 1L));
        try {
            provider.reserveHostBackupSlot(db.vm(3L, 7L, 7L), 7L, "c", 2, 1);
            Assert.fail("third backup on a host capped at 2 must not get a slot");
        } catch (CloudRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("host 7"));
        }
        Assert.assertEquals("a refused reservation must not leave a row behind", 2, db.rows.size());
        first.setStatus(Backup.Status.BackedUp);
        provider.reserveHostBackupSlot(db.vm(4L, 7L, 7L), 7L, "d", 2, 1);
        Assert.assertEquals(2, provider.countInFlightBackups(7L, 1L));
    }

    @Test
    public void hostsAreIndependentAndStoppedVmsCountOnTheirLastHost() {
        final SharedDb db = new SharedDb();
        final NASBackupProvider provider = db.managementServer();
        provider.reserveHostBackupSlot(db.vm(1L, 1L, 1L), 1L, "a", 1, 1);
        provider.reserveHostBackupSlot(db.vm(2L, null, 2L), 2L, "b", 1, 1);
        Assert.assertEquals(1, provider.countInFlightBackups(1L, 1L));
        Assert.assertEquals(1, provider.countInFlightBackups(2L, 1L));
        Assert.assertEquals(0, provider.countInFlightBackups(3L, 1L));
    }

    @Test
    public void staleAndForeignRowsDoNotHoldASlot() {
        final SharedDb db = new SharedDb();
        final NASBackupProvider provider = db.managementServer();
        final BackupVO stale = provider.reserveHostBackupSlot(db.vm(1L, 5L, 5L), 5L, "a", 1, 1);
        stale.setDate(new Date(System.currentTimeMillis() - (NASBackupProvider.abandonedAfterSeconds(provider.takeBackupCommandWaitSeconds(1L)) + 60L) * 1000L));
        Assert.assertEquals("an abandoned BackingUp row must not hold a slot", 0, provider.countInFlightBackups(5L, 1L));

        final BackupVO other = provider.reserveHostBackupSlot(db.vm(2L, 5L, 5L), 5L, "b", 1, 1);
        Mockito.when(db.backupOfferingDao.findByIdIncludingRemoved(Mockito.anyLong()))
                .thenReturn(new BackupOfferingVO(1L, "x", "veeam", "veeam", "veeam", true));
        Assert.assertEquals("backups of another provider must not count", 0, provider.countInFlightBackups(5L, 1L));
        Assert.assertNotNull(other);
    }

    @Test
    public void waiterProceedsWhenASlotFrees() throws Exception {
        final SharedDb db = new SharedDb();
        final NASBackupProvider provider = db.managementServer();
        final BackupVO held = provider.reserveHostBackupSlot(db.vm(1L, 9L, 9L), 9L, "a", 1, 10);
        final VMInstanceVO second = db.vm(2L, 9L, 9L);
        final CountDownLatch acquired = new CountDownLatch(1);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final Thread waiter = new Thread(() -> {
            try {
                provider.reserveHostBackupSlot(second, 9L, "b", 1, 10);
                acquired.countDown();
            } catch (CloudRuntimeException e) {
                failed.set(true);
            }
        });
        waiter.start();
        Assert.assertFalse("waiter must block while the only slot is held", acquired.await(300, TimeUnit.MILLISECONDS));
        held.setStatus(Backup.Status.BackedUp);
        provider.wakeLocalSlotWaiters();
        Assert.assertTrue("waiter must get the slot once it is released", acquired.await(5, TimeUnit.SECONDS));
        Assert.assertFalse(failed.get());
        Assert.assertEquals(1, provider.countInFlightBackups(9L, 1L));
        waiter.join(1000);
    }

    /**
     * Two management servers, each with its own provider and no shared memory, send backups for the same host
     * at the same time. The combined in-flight count read from the shared database must never exceed the cap.
     */
    @Test
    public void capHoldsAcrossManagementServers() throws Exception {
        final SharedDb db = new SharedDb();
        final NASBackupProvider ms1 = db.managementServer();
        final NASBackupProvider ms2 = db.managementServer();
        final int max = 2;
        final int backups = 12;
        final AtomicInteger peak = new AtomicInteger();
        final AtomicInteger done = new AtomicInteger();
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        final ExecutorService pool = Executors.newFixedThreadPool(backups);
        final CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < backups; i++) {
            final NASBackupProvider provider = i % 2 == 0 ? ms1 : ms2;
            final VMInstanceVO vm = db.vm(100L + i, 42L, 42L);
            pool.submit(() -> {
                try {
                    start.await();
                    final BackupVO backup = provider.reserveHostBackupSlot(vm, 42L, "p" + vm.getId(), max, 30);
                    peak.accumulateAndGet(db.inFlightOn(42L), Math::max);
                    Thread.sleep(20);
                    peak.accumulateAndGet(db.inFlightOn(42L), Math::max);
                    backup.setStatus(Backup.Status.BackedUp);
                    provider.wakeLocalSlotWaiters();
                    done.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        Assert.assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        Assert.assertTrue("unexpected errors: " + errors, errors.isEmpty());
        Assert.assertEquals(backups, done.get());
        Assert.assertTrue("peak " + peak.get() + " exceeded the cap of " + max, peak.get() <= max);
        Assert.assertEquals(max, peak.get());
    }

    /** A provider wired to one host (id 11) and an agent that answers every command with the given behaviour. */
    private static NASBackupProvider wiredToHost(final SharedDb db, final Answer<Object> agentBehaviour) throws Exception {
        final NASBackupProvider provider = db.managementServer();
        final HostDao hostDao = Mockito.mock(HostDao.class);
        final HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(11L);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(hostDao.findById(11L)).thenReturn(host);
        final BackupRepositoryDao repositoryDao = Mockito.mock(BackupRepositoryDao.class);
        Mockito.when(repositoryDao.findByBackupOfferingId(Mockito.anyLong())).thenReturn(Mockito.mock(BackupRepository.class));
        final AgentManager agentManager = Mockito.mock(AgentManager.class);
        Mockito.when(agentManager.send(Mockito.anyLong(), Mockito.any(Command.class))).thenAnswer(agentBehaviour);
        setField(provider, "hostDao", hostDao);
        setField(provider, "backupRepositoryDao", repositoryDao);
        setField(provider, "agentManager", agentManager);
        overrideDefaultConfigValue(NASBackupProvider.NASBackupParallelExecution, "true");
        return provider;
    }

    private static VMInstanceVO runningVm(final SharedDb db, final long vmId) {
        final VMInstanceVO vm = db.vm(vmId, 11L, 11L);
        Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        return vm;
    }

    /**
     * Damans227's case: the management server stops waiting while the host is still writing the backup. The row
     * must stay BackingUp and keep its slot, on this and on every other management server.
     */
    @Test
    public void timedOutBackupKeepsItsRowAndItsSlot() throws Exception {
        final SharedDb db = new SharedDb();
        final AtomicInteger sentWait = new AtomicInteger(-1);
        final NASBackupProvider ms1 = wiredToHost(db, inv -> {
            sentWait.set(((Command) inv.getArgument(1)).getWait());
            throw new OperationTimedoutException(null, 11L, 1L, 1, false);
        });
        try {
            ms1.takeBackup(runningVm(db, 1L));
            Assert.fail("a backup that did not answer in time must not report success");
        } catch (CloudRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("may still be running"));
        }
        Assert.assertEquals("the command must wait backup.nas.backup.timeout, not the global wait",
                Integer.parseInt(NASBackupProvider.NASBackupTimeout.defaultValue()), sentWait.get());
        Assert.assertEquals(1, db.rows.size());
        final BackupVO row = db.rows.get(0);
        Assert.assertEquals("the backup must stay visible while the host may still run it", Backup.Status.BackingUp, row.getStatus());
        Assert.assertNull("the backup must not be removed", row.getRemoved());
        Assert.assertEquals("the slot must stay held", 1, ms1.countInFlightBackups(11L, 1L));

        final NASBackupProvider ms2 = db.managementServer();
        try {
            ms2.reserveHostBackupSlot(runningVm(db, 2L), 11L, "b", 1, 0);
            Assert.fail("another management server must see the slot as taken");
        } catch (CloudRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("1 of 1 in flight"));
        }
        Assert.assertEquals("the sync task must leave a backup the host may still be running alone", 0, ms2.markAbandonedBackups(db.vms.get(1L)));
        Assert.assertEquals(Backup.Status.BackingUp, row.getStatus());
    }

    @Test
    public void backupTheHostReportsFailedReleasesItsSlot() throws Exception {
        final SharedDb db = new SharedDb();
        final NASBackupProvider provider = wiredToHost(db, inv -> new BackupAnswer(inv.getArgument(1), false, "disk full"));
        Assert.assertFalse(provider.takeBackup(runningVm(db, 1L)));
        Assert.assertEquals(1, db.rows.size());
        Assert.assertEquals(Backup.Status.Failed, db.rows.get(0).getStatus());
        Assert.assertNotNull(db.rows.get(0).getRemoved());
        Assert.assertEquals("a backup the host reported failed must not hold a slot", 0, provider.countInFlightBackups(11L, 1L));
    }

    @Test
    public void backupThatNeverReachedTheHostReleasesItsSlot() throws Exception {
        final SharedDb db = new SharedDb();
        final NASBackupProvider provider = wiredToHost(db, inv -> {
            throw new AgentUnavailableException(11L);
        });
        try {
            provider.takeBackup(runningVm(db, 1L));
            Assert.fail("an unreachable host must fail the backup");
        } catch (CloudRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("Unable to contact"));
        }
        Assert.assertNotNull(db.rows.get(0).getRemoved());
        Assert.assertEquals(0, provider.countInFlightBackups(11L, 1L));
    }

    @Test
    public void successfulBackupIsBackedUpAndReleasesItsSlot() throws Exception {
        final SharedDb db = new SharedDb();
        final NASBackupProvider provider = wiredToHost(db, inv -> {
            final BackupAnswer answer = new BackupAnswer(inv.getArgument(1), true, "42");
            answer.setSize(42L);
            return answer;
        });
        Assert.assertTrue(provider.takeBackup(runningVm(db, 1L)));
        Assert.assertEquals(Backup.Status.BackedUp, db.rows.get(0).getStatus());
        Assert.assertNull(db.rows.get(0).getRemoved());
        Assert.assertEquals(0, provider.countInFlightBackups(11L, 1L));
    }

    /**
     * A BackingUp row twice the wait old has been given up by its management server (or that management server
     * stopped) and the agent has stopped the script: it frees its slot and the sync task marks it Error, keeping it.
     */
    @Test
    public void abandonedBackupIsMarkedErrorAndKept() throws Exception {
        final SharedDb db = new SharedDb();
        final NASBackupProvider provider = db.managementServer();
        final VMInstanceVO vm = runningVm(db, 1L);
        final long wait = provider.takeBackupCommandWaitSeconds(1L);
        final BackupVO pastWait = provider.reserveHostBackupSlot(vm, 11L, "a", 5, 1);
        pastWait.setDate(new Date(System.currentTimeMillis() - (wait + 60L) * 1000L));
        final BackupVO abandoned = provider.reserveHostBackupSlot(vm, 11L, "b", 5, 1);
        abandoned.setDate(new Date(System.currentTimeMillis() - (2 * wait + 60L) * 1000L));
        final BackupVO done = provider.reserveHostBackupSlot(vm, 11L, "c", 5, 1);
        done.setDate(new Date(System.currentTimeMillis() - (3 * wait) * 1000L));
        done.setStatus(Backup.Status.BackedUp);

        Assert.assertEquals("only the row past the wait but not abandoned holds a slot", 1, provider.countInFlightBackups(11L, 1L));
        provider.syncBackups(vm, null);
        Assert.assertEquals(Backup.Status.BackingUp, pastWait.getStatus());
        Assert.assertEquals(Backup.Status.Error, abandoned.getStatus());
        Assert.assertNull("an abandoned backup stays in the list so it can be seen and deleted", abandoned.getRemoved());
        Assert.assertEquals(Backup.Status.BackedUp, done.getStatus());
        Assert.assertEquals("marking is idempotent across management servers", 0, db.managementServer().markAbandonedBackups(vm));
    }

    @Test
    public void abandonedWindowIsTwiceTheWaitAndAtLeastFiveMinutesMore() {
        Assert.assertEquals(14400L, NASBackupProvider.abandonedAfterSeconds(7200));
        Assert.assertEquals(360L, NASBackupProvider.abandonedAfterSeconds(60));
    }

    @Test
    public void commandsTimeoutForTakeBackupBoundsTheStaleWindow() throws Exception {
        final NASBackupProvider provider = new NASBackupProvider();
        overrideDefaultConfigValue(AgentManager.GranularWaitTimeForCommands, "CopyCommand=60, TakeBackupCommand=14400");
        try {
            Assert.assertEquals(14400, provider.takeBackupCommandWaitSeconds(1L));
        } finally {
            overrideDefaultConfigValue(AgentManager.GranularWaitTimeForCommands, "");
        }
        Assert.assertEquals(Integer.parseInt(NASBackupProvider.NASBackupTimeout.defaultValue()), provider.takeBackupCommandWaitSeconds(1L));
        overrideDefaultConfigValue(NASBackupProvider.NASBackupTimeout, "0");
        try {
            Assert.assertEquals(Integer.parseInt(AgentManager.Wait.defaultValue()), provider.takeBackupCommandWaitSeconds(1L));
        } finally {
            overrideDefaultConfigValue(NASBackupProvider.NASBackupTimeout, "7200");
        }
    }

    @Test
    public void hostLockNameIsPerHost() {
        Assert.assertEquals("nas-backup-host-42", NASBackupProvider.hostBackupLockName(42L));
    }

    /**
     * Spring ignores @Inject on static fields, so an annotation that drifts onto a ConfigKey constant
     * silently leaves the DAO it used to belong to unset (hostDao was null here once).
     */
    @Test
    public void everyInjectedFieldIsAnInstanceFieldAndHostDaoIsInjected() throws Exception {
        for (final Field field : NASBackupProvider.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                Assert.assertFalse("@Inject on static field " + field.getName() + " is ignored by Spring",
                        Modifier.isStatic(field.getModifiers()));
            }
            if (field.getType().getSimpleName().endsWith("Dao") || field.getType().getSimpleName().endsWith("Manager")) {
                Assert.assertTrue("dependency field " + field.getName() + " is not annotated with @Inject",
                        field.isAnnotationPresent(Inject.class));
            }
        }
        final Field hostDao = NASBackupProvider.class.getDeclaredField("hostDao");
        Assert.assertEquals(HostDao.class, hostDao.getType());
        Assert.assertTrue(hostDao.isAnnotationPresent(Inject.class));
    }
}
