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

import com.cloud.agent.AgentManager;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NASBackupProvider extends AdapterBase implements BackupProvider, Configurable {
    private static final Logger LOG = LogManager.getLogger(NASBackupProvider.class);

    public static final ConfigKey<Boolean> NASBackupParallelExecution = new ConfigKey<>("Advanced", Boolean.class,
            "backup.nas.parallel.execution.enabled",
            "true",
            "Let NAS take-backup commands run concurrently on a KVM host instead of queueing behind every earlier command on that host "
            + "and holding up every later one. Concurrency is bounded per host by backup.nas.parallel.max.per.host. "
            + "Disable to restore strictly sequential execution.",
            true, ConfigKey.Scope.Zone);

    public static final ConfigKey<Integer> NASBackupParallelMaxPerHost = new ConfigKey<>("Advanced", Integer.class,
            "backup.nas.parallel.max.per.host",
            "2",
            "Maximum number of NAS take-backup commands in flight on one KVM host when parallel execution is enabled; further "
            + "backups for that host wait on the management server that received them. The limit holds across all management "
            + "servers: in-flight backups are counted from the database under a cluster-wide lock per host. Keep it below the "
            + "agent's worker thread count (workers in agent.properties, default 5) so start, stop, reboot and migrate commands "
            + "are never queued behind backups.",
            true, ConfigKey.Scope.Zone);

    public static final ConfigKey<Integer> NASBackupParallelQueueTimeout = new ConfigKey<>("Advanced", Integer.class,
            "backup.nas.parallel.queue.timeout",
            "7200",
            "Seconds a NAS take-backup may wait for a free per-host slot before it fails.",
            true, ConfigKey.Scope.Zone);

    public static final ConfigKey<Integer> NASBackupTimeout = new ConfigKey<>("Advanced", Integer.class,
            "backup.nas.backup.timeout",
            "7200",
            "Seconds the management server waits for a NAS take-backup to finish on the host (commands.timeout for "
            + "TakeBackupCommand takes precedence when set). The default matches the KVM agent's cmds.timeout (7200), after "
            + "which the agent stops the backup script; keep this at or above cmds.timeout. A backup that has not answered "
            + "in time may still be running on the host, so it stays BackingUp and keeps its host slot until it is twice "
            + "this old, when it is marked Error (it stays in the backup list and can be deleted).",
            true, ConfigKey.Scope.Zone);

    /** Seconds to wait for a host's cluster-wide lock; it is only held for the count-and-reserve step. */
    private static final int HOST_SLOT_LOCK_WAIT_SECONDS = 30;

    /**
     * The least time a BackingUp row is held beyond the take-backup wait before it is treated as abandoned. The hold
     * is the wait again (so twice the wait in all), but never less than this.
     */
    private static final int MIN_ABANDONED_MARGIN_SECONDS = 300;

    /** Bounds of the back-off between two attempts to reserve a slot on a full host. */
    protected long slotPollMinMillis = 1000L;
    protected long slotPollMaxMillis = 15000L;

    /** Wakes the waiters on this management server as soon as one of its own backups finishes. */
    private final Object localSlotRelease = new Object();

    @Inject
    private BackupDao backupDao;

    @Inject
    private BackupRepositoryDao backupRepositoryDao;

    @Inject
    private BackupOfferingDao backupOfferingDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private ClusterDao clusterDao;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private StoragePoolHostDao storagePoolHostDao;

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;

    @Inject
    private AgentManager agentManager;

    protected Host getLastVMHypervisorHost(VirtualMachine vm) {
        Long hostId = vm.getLastHostId();
        if (hostId == null) {
            LOG.debug("Cannot find last host for vm. This should never happen, please check your database.");
            return null;
        }
        Host host = hostDao.findById(hostId);

        if (host.getStatus() == Status.Up) {
            return host;
        } else {
            // Try to find any Up host in the same cluster
            for (final Host hostInCluster : hostDao.findHypervisorHostInCluster(host.getClusterId())) {
                if (hostInCluster.getStatus() == Status.Up) {
                    LOG.debug("Found Host {}", hostInCluster);
                    return hostInCluster;
                }
            }
        }
        // Try to find any Host in the zone
        for (final HostVO hostInZone : hostDao.listByDataCenterIdAndHypervisorType(host.getDataCenterId(), Hypervisor.HypervisorType.KVM)) {
            if (hostInZone.getStatus() == Status.Up) {
                LOG.debug("Found Host {}", hostInZone);
                return hostInZone;
            }
        }
        return null;
    }

    protected Host getVMHypervisorHost(VirtualMachine vm) {
        Long hostId = vm.getHostId();
        if (hostId == null && VirtualMachine.State.Running.equals(vm.getState())) {
            throw new CloudRuntimeException(String.format("Unable to find the hypervisor host for %s. Make sure the virtual machine is running", vm.getName()));
        }
        if (VirtualMachine.State.Stopped.equals(vm.getState())) {
            hostId = vm.getLastHostId();
        }
        if (hostId == null) {
            throw new CloudRuntimeException(String.format("Unable to find the hypervisor host for stopped VM: %s", vm));
        }
        final Host host = hostDao.findById(hostId);
        if (host == null || !Status.Up.equals(host.getStatus()) || !Hypervisor.HypervisorType.KVM.equals(host.getHypervisorType())) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        }
        return host;
    }

    @Override
    public boolean takeBackup(final VirtualMachine vm) {
        final Host host = getVMHypervisorHost(vm);

        final BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(vm.getBackupOfferingId());
        if (backupRepository == null) {
            throw new CloudRuntimeException("No valid backup repository found for the VM, please check the attached backup offering");
        }

        final Date creationDate = new Date();
        final String backupPath = String.format("%s/%s", vm.getInstanceName(),
                new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(creationDate));

        TakeBackupCommand command = new TakeBackupCommand(vm.getInstanceName(), backupPath);
        command.setBackupRepoType(backupRepository.getType());
        command.setBackupRepoAddress(backupRepository.getAddress());
        command.setMountOptions(backupRepository.getMountOptions());
        final long zoneId = vm.getDataCenterId();
        final boolean parallel = applyExecutionPolicy(command, zoneId);
        final int waitSeconds = takeBackupCommandWaitSeconds(zoneId);
        command.setWait(waitSeconds);

        if (VirtualMachine.State.Stopped.equals(vm.getState())) {
            List<VolumeVO> vmVolumes = volumeDao.findByInstance(vm.getId());
            vmVolumes.sort(Comparator.comparing(Volume::getDeviceId));
            List<String> volumePaths = getVolumePaths(vmVolumes);
            command.setVolumePaths(volumePaths);
        }

        // Concurrent backups are bounded per host across all management servers: the BackingUp row of this
        // backup is the reservation of one of the host's slots, created only while the host has a free slot.
        // The wait happens here, on the management server, never on the agent's worker threads.
        final BackupVO backupVO = parallel
                ? reserveHostBackupSlot(vm, host.getId(), backupPath, NASBackupParallelMaxPerHost.valueIn(zoneId), NASBackupParallelQueueTimeout.valueIn(zoneId))
                : createBackupObject(vm, backupPath);
        boolean succeeded = false;
        boolean outcomeUnknown = false;
        try {
            final BackupAnswer answer;
            try {
                answer = (BackupAnswer) agentManager.send(host.getId(), command);
            } catch (AgentUnavailableException e) {
                throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
            } catch (OperationTimedoutException e) {
                // The host did not answer in time, but its backup may still be running and writing to the NAS.
                // Keep the BackingUp row, and with it the host slot, so the backup stays visible and the host is
                // not given another backup early. markAbandonedBackups turns it into Error once it is too old.
                outcomeUnknown = true;
                LOG.warn("NAS backup {} of VM {} did not answer within {} seconds; it may still be running on host {}. "
                        + "It stays BackingUp and keeps its host slot until it is {} seconds old, then it is marked Error",
                        backupVO.getUuid(), vm.getInstanceName(), waitSeconds, host.getId(), abandonedAfterSeconds(waitSeconds));
                throw new CloudRuntimeException(String.format("Backup did not finish within %d seconds and may still be "
                        + "running on the host; it stays in BackingUp state until the host is done or it is marked Error", waitSeconds));
            }
            if (answer == null || !answer.getResult()) {
                return false;
            }
            succeeded = true;
            backupVO.setDate(new Date());
            backupVO.setSize(answer.getSize());
            backupVO.setStatus(Backup.Status.BackedUp);
            backupVO.setBackedUpVolumes(BackupManagerImpl.createVolumeInfoFromVolumes(volumeDao.findByInstance(vm.getId())));
            return backupDao.update(backupVO.getId(), backupVO);
        } finally {
            if (!succeeded && !outcomeUnknown) {
                // Releases the slot: the host reported a failure or never got the command, so nothing is running.
                backupVO.setStatus(Backup.Status.Failed);
                backupDao.update(backupVO.getId(), backupVO);
                backupDao.remove(backupVO.getId());
            }
            if (parallel) {
                wakeLocalSlotWaiters();
            }
        }
    }

    /**
     * Applies the zone's execution policy to the command and returns true when the command will run
     * concurrently with other commands on the host (and so must be gated per host).
     */
    protected boolean applyExecutionPolicy(final TakeBackupCommand command, final long zoneId) {
        final boolean parallel = Boolean.TRUE.equals(NASBackupParallelExecution.valueIn(zoneId));
        command.setExecuteInSequence(!parallel);
        return parallel;
    }

    static String hostBackupLockName(final long hostId) {
        return "nas-backup-host-" + hostId;
    }

    /**
     * Reserves one of the host's backup slots and returns the BackingUp backup row that holds it. The row is
     * the reservation: it is created only while fewer than maxPerHost NAS backups are in flight on the host,
     * counted from the database under the host's cluster-wide lock, so the limit holds across every management
     * server. The lock is held only for the count-and-insert step, never for the backup itself. The slot is
     * released when the row leaves BackingUp (BackedUp, or Failed and removed). When the host is full this
     * waits with back-off, up to timeoutSeconds, then fails with CloudRuntimeException without leaving a row.
     */
    protected BackupVO reserveHostBackupSlot(final VirtualMachine vm, final long hostId, final String backupPath,
            final Integer maxPerHost, final Integer timeoutSeconds) {
        final int max = Math.max(1, maxPerHost == null ? 1 : maxPerHost);
        final int timeout = Math.max(0, timeoutSeconds == null ? 0 : timeoutSeconds);
        final long deadline = System.currentTimeMillis() + timeout * 1000L;
        final int[] lastCount = {-1};
        long pause = slotPollMinMillis;
        while (true) {
            final BackupVO reserved = runUnderHostLock(hostId, () -> {
                final int inFlight = countInFlightBackups(hostId, vm.getDataCenterId());
                lastCount[0] = inFlight;
                if (inFlight >= max) {
                    return null;
                }
                final BackupVO backup = createBackupObject(vm, backupPath);
                LOG.debug("Host {} now has {} of {} NAS backups in flight", hostId, inFlight + 1, max);
                return backup;
            });
            if (reserved != null) {
                return reserved;
            }
            final long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                throw new CloudRuntimeException(String.format(
                        "Timed out after %d seconds waiting for a NAS backup slot on host %d (%d of %d in flight); "
                        + "raise %s or %s, or retry when the host's backups finish",
                        timeout, hostId, Math.max(lastCount[0], 0), max, NASBackupParallelMaxPerHost.key(), NASBackupParallelQueueTimeout.key()));
            }
            synchronized (localSlotRelease) {
                try {
                    localSlotRelease.wait(Math.max(1L, Math.min(pause, remaining)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CloudRuntimeException("Interrupted while waiting for a NAS backup slot on host " + hostId);
                }
            }
            pause = Math.min(pause * 2, slotPollMaxMillis);
        }
    }

    /**
     * Runs the action while holding the host's cluster-wide database lock and returns its result, or null when
     * the lock could not be taken in time (the caller then retries like it does for a full host).
     */
    protected <T> T runUnderHostLock(final long hostId, final Supplier<T> action) {
        final GlobalLock lock = GlobalLock.getInternLock(hostBackupLockName(hostId));
        try {
            if (!lock.lock(HOST_SLOT_LOCK_WAIT_SECONDS)) {
                LOG.debug("Could not take lock {} within {} seconds, will retry", lock.getName(), HOST_SLOT_LOCK_WAIT_SECONDS);
                return null;
            }
            try {
                return action.get();
            } finally {
                lock.unlock();
            }
        } finally {
            lock.releaseRef();
        }
    }

    /**
     * Counts the NAS backups in flight on the host, from the database, so every management server sees the same
     * number. A backup belongs to the host its VM is on, or was last on when stopped, which is how
     * getVMHypervisorHost picks the host the command is sent to. A running VM cannot migrate while libvirt runs
     * its backup job, so the attribution does not drift for running VMs. A row keeps its slot after the take-backup
     * wait runs out, because the host may still be running it, and is only left out once it is twice the wait old
     * (abandoned: for example its management server stopped mid-backup), so a slot can never leak forever.
     */
    protected int countInFlightBackups(final long hostId, final long zoneId) {
        final List<BackupVO> backingUp = backupDao.listByZoneAndStatus(zoneId, Backup.Status.BackingUp);
        if (CollectionUtils.isEmpty(backingUp)) {
            return 0;
        }
        final long abandonedBefore = abandonedBefore(zoneId);
        int inFlight = 0;
        for (final BackupVO backup : backingUp) {
            if (backup.getDate() != null && backup.getDate().getTime() < abandonedBefore) {
                LOG.debug("Not counting abandoned BackingUp backup {} (started {}) towards host {}", backup.getUuid(), backup.getDate(), hostId);
                continue;
            }
            final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
            if (offering != null && !getName().equalsIgnoreCase(offering.getProvider())) {
                continue;
            }
            final VirtualMachine backupVm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
            if (backupVm == null) {
                continue;
            }
            final Long vmHostId = backupVm.getHostId() != null ? backupVm.getHostId() : backupVm.getLastHostId();
            if (vmHostId != null && vmHostId == hostId) {
                inFlight++;
            }
        }
        return inFlight;
    }

    /** Seconds after which a BackingUp row that has not answered is treated as abandoned: twice the wait, at least wait + 5 minutes. */
    static long abandonedAfterSeconds(final int waitSeconds) {
        return (long) waitSeconds + Math.max(waitSeconds, MIN_ABANDONED_MARGIN_SECONDS);
    }

    /** BackingUp rows of the zone that started before this time are abandoned. */
    protected long abandonedBefore(final long zoneId) {
        return System.currentTimeMillis() - abandonedAfterSeconds(takeBackupCommandWaitSeconds(zoneId)) * 1000L;
    }

    /**
     * Marks the VM's abandoned NAS backups Error: rows still BackingUp twice the take-backup wait after they started.
     * By then the agent has stopped the backup script (cmds.timeout) and no management server is waiting for an
     * answer, but the outcome on the NAS is unknown, so the row is kept, visible and deletable, instead of removed.
     * Runs from the periodic backup sync task on every management server; marking the same row twice is harmless.
     */
    protected int markAbandonedBackups(final VirtualMachine vm) {
        final List<Backup> backups = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
        if (CollectionUtils.isEmpty(backups)) {
            return 0;
        }
        final long abandonedBefore = abandonedBefore(vm.getDataCenterId());
        int marked = 0;
        for (final Backup backup : backups) {
            if (!Backup.Status.BackingUp.equals(backup.getStatus()) || backup.getDate() == null
                    || backup.getDate().getTime() >= abandonedBefore) {
                continue;
            }
            final BackupVO row = backupDao.findById(backup.getId());
            if (row == null || !Backup.Status.BackingUp.equals(row.getStatus())) {
                continue;
            }
            LOG.warn("NAS backup {} of VM {} started at {} and never reported back; marking it Error. Its files on the "
                    + "backup repository may be incomplete, delete the backup to remove them", row.getUuid(), vm.getInstanceName(), row.getDate());
            row.setStatus(Backup.Status.Error);
            backupDao.update(row.getId(), row);
            marked++;
        }
        return marked;
    }

    /**
     * The longest a management server waits for a TakeBackupCommand answer: commands.timeout if set for it,
     * else backup.nas.backup.timeout, else (when that is 0) the global wait. The provider sets it as the command's wait.
     */
    protected int takeBackupCommandWaitSeconds(final long zoneId) {
        int wait = Math.max(0, Objects.requireNonNullElse(NASBackupTimeout.valueIn(zoneId), 0));
        if (wait == 0) {
            wait = Math.max(0, Objects.requireNonNullElse(AgentManager.Wait.value(), 0));
        }
        final String granular = AgentManager.GranularWaitTimeForCommands.value();
        if (granular != null) {
            for (final String entry : granular.split(",")) {
                final String[] pair = entry.trim().split("=");
                if (pair.length == 2 && TakeBackupCommand.class.getSimpleName().equals(pair[0].trim())) {
                    try {
                        final int value = Integer.parseInt(pair[1].trim());
                        if (value > 0) {
                            wait = value;
                        }
                    } catch (NumberFormatException e) {
                        LOG.debug("Ignoring invalid commands.timeout entry {}", entry);
                    }
                }
            }
        }
        return wait;
    }

    protected void wakeLocalSlotWaiters() {
        synchronized (localSlotRelease) {
            localSlotRelease.notifyAll();
        }
    }

    private BackupVO createBackupObject(VirtualMachine vm, String backupPath) {
        BackupVO backup = new BackupVO();
        backup.setVmId(vm.getId());
        backup.setExternalId(backupPath);
        backup.setType("FULL");
        backup.setDate(new Date());
        long virtualSize = 0L;
        for (final Volume volume: volumeDao.findByInstance(vm.getId())) {
            if (Volume.State.Ready.equals(volume.getState())) {
                virtualSize += volume.getSize();
            }
        }
        backup.setProtectedSize(Long.valueOf(virtualSize));
        backup.setStatus(Backup.Status.BackingUp);
        backup.setBackupOfferingId(vm.getBackupOfferingId());
        backup.setAccountId(vm.getAccountId());
        backup.setDomainId(vm.getDomainId());
        backup.setZoneId(vm.getDataCenterId());
        return backupDao.persist(backup);
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        List<Backup.VolumeInfo> backedVolumes = backup.getBackedUpVolumes();
        List<VolumeVO> volumes = backedVolumes.stream()
                .map(volume -> volumeDao.findByUuid(volume.getUuid()))
                .sorted((v1, v2) -> Long.compare(v1.getDeviceId(), v2.getDeviceId()))
                .collect(Collectors.toList());

        LOG.debug("Restoring vm {} from backup {} on the NAS Backup Provider", vm, backup);
        BackupRepository backupRepository = getBackupRepository(vm, backup);

        final Host host = getLastVMHypervisorHost(vm);
        RestoreBackupCommand restoreCommand = new RestoreBackupCommand();
        restoreCommand.setBackupPath(backup.getExternalId());
        restoreCommand.setBackupRepoType(backupRepository.getType());
        restoreCommand.setBackupRepoAddress(backupRepository.getAddress());
        restoreCommand.setMountOptions(backupRepository.getMountOptions());
        restoreCommand.setVmName(vm.getName());
        restoreCommand.setVolumePaths(getVolumePaths(volumes));
        restoreCommand.setBackupFiles(getBackupFiles(backedVolumes));
        restoreCommand.setVmExists(vm.getRemoved() == null);
        restoreCommand.setVmState(vm.getState());

        BackupAnswer answer = null;
        try {
            answer = (BackupAnswer) agentManager.send(host.getId(), restoreCommand);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to initiate backup timed out, please try again");
        }
        return answer.getResult();
    }

    private List<String> getBackupFiles(List<Backup.VolumeInfo> backedVolumes) {
        List<String> backupFiles = new ArrayList<>();
        for (Backup.VolumeInfo backedVolume : backedVolumes) {
            backupFiles.add(backedVolume.getPath());
        }
        return backupFiles;
    }

    private List<String> getVolumePaths(List<VolumeVO> volumes) {
        List<String> volumePaths = new ArrayList<>();
        for (VolumeVO volume : volumes) {
            StoragePoolVO storagePool = primaryDataStoreDao.findById(volume.getPoolId());
            if (Objects.isNull(storagePool)) {
                throw new CloudRuntimeException("Unable to find storage pool associated to the volume");
            }
            String volumePathPrefix;
            if (ScopeType.HOST.equals(storagePool.getScope())) {
                volumePathPrefix = storagePool.getPath();
            } else if (Storage.StoragePoolType.SharedMountPoint.equals(storagePool.getPoolType())) {
                volumePathPrefix = storagePool.getPath();
            } else {
                volumePathPrefix = String.format("/mnt/%s", storagePool.getUuid());
            }
            volumePaths.add(String.format("%s/%s", volumePathPrefix, volume.getPath()));
        }
        return volumePaths;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        final VolumeVO volume = volumeDao.findByUuid(volumeUuid);
        final VirtualMachine backupSourceVm = vmInstanceDao.findById(backup.getVmId());
        final StoragePoolHostVO dataStore = storagePoolHostDao.findByUuid(dataStoreUuid);
        final HostVO hostVO = hostDao.findByIp(hostIp);

        Backup.VolumeInfo matchingVolume = getBackedUpVolumeInfo(backup.getBackedUpVolumes(), volumeUuid);
        if (matchingVolume == null) {
            throw new CloudRuntimeException(String.format("Unable to find volume %s in the list of backed up volumes for backup %s, cannot proceed with restore", volumeUuid, backup));
        }
        Long backedUpVolumeSize = matchingVolume.getSize();

        LOG.debug("Restoring vm volume {} from backup {} on the NAS Backup Provider", volume, backup);
        BackupRepository backupRepository = getBackupRepository(backupSourceVm, backup);

        VolumeVO restoredVolume = new VolumeVO(Volume.Type.DATADISK, null, backup.getZoneId(),
                backup.getDomainId(), backup.getAccountId(), 0, null,
                backup.getSize(), null, null, null);
        String volumeUUID = UUID.randomUUID().toString();
        restoredVolume.setName("RestoredVol-"+volume.getName());
        restoredVolume.setProvisioningType(volume.getProvisioningType());
        restoredVolume.setUpdated(new Date());
        restoredVolume.setUuid(volumeUUID);
        restoredVolume.setRemoved(null);
        restoredVolume.setDisplayVolume(true);
        restoredVolume.setPoolId(dataStore.getPoolId());
        restoredVolume.setPath(restoredVolume.getUuid());
        restoredVolume.setState(Volume.State.Copying);
        restoredVolume.setFormat(Storage.ImageFormat.QCOW2);
        restoredVolume.setSize(backedUpVolumeSize);
        restoredVolume.setDiskOfferingId(volume.getDiskOfferingId());

        RestoreBackupCommand restoreCommand = new RestoreBackupCommand();
        restoreCommand.setBackupPath(backup.getExternalId());
        restoreCommand.setBackupRepoType(backupRepository.getType());
        restoreCommand.setBackupRepoAddress(backupRepository.getAddress());
        restoreCommand.setVmName(vmNameAndState.first());
        restoreCommand.setVolumePaths(Collections.singletonList(String.format("%s/%s", dataStore.getLocalPath(), volumeUUID)));
        restoreCommand.setBackupFiles(Collections.singletonList(matchingVolume.getPath()));
        restoreCommand.setDiskType(volume.getVolumeType().name().toLowerCase(Locale.ROOT));
        restoreCommand.setMountOptions(backupRepository.getMountOptions());
        restoreCommand.setVmExists(null);
        restoreCommand.setVmState(vmNameAndState.second());

        BackupAnswer answer = null;
        try {
            answer = (BackupAnswer) agentManager.send(hostVO.getId(), restoreCommand);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to initiate backup timed out, please try again");
        }

        if (answer.getResult()) {
            try {
                volumeDao.persist(restoredVolume);
            } catch (Exception e) {
                throw new CloudRuntimeException("Unable to create restored volume due to: " + e);
            }
        }

        return new Pair<>(answer.getResult(), answer.getDetails());
    }

    private BackupRepository getBackupRepository(VirtualMachine vm, Backup backup) {
        BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(vm.getBackupOfferingId());
        final String errorMessage = "No valid backup repository found for the VM, please check the attached backup offering";
        if (backupRepository == null) {
            logger.warn(errorMessage + "Re-attempting with the backup offering associated with the backup");
        }
        backupRepository = backupRepositoryDao.findByBackupOfferingId(backup.getBackupOfferingId());
        if (backupRepository == null) {
            throw new CloudRuntimeException(errorMessage);
        }
        return backupRepository;
    }

    private Backup.VolumeInfo getBackedUpVolumeInfo(List<Backup.VolumeInfo> backedUpVolumes, String volumeUuid) {
        return backedUpVolumes.stream()
                .filter(v -> v.getUuid().equals(volumeUuid))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        final BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(backup.getBackupOfferingId());
        if (backupRepository == null) {
            throw new CloudRuntimeException("No valid backup repository found for the VM, please check the attached backup offering");
        }

        final VirtualMachine vm  = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        final Host host = getLastVMHypervisorHost(vm);

        DeleteBackupCommand command = new DeleteBackupCommand(backup.getExternalId(), backupRepository.getType(),
                backupRepository.getAddress(), backupRepository.getMountOptions());

        BackupAnswer answer = null;
        try {
            answer = (BackupAnswer) agentManager.send(host.getId(), command);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to initiate backup timed out, please try again");
        }

        if (answer != null && answer.getResult()) {
            return backupDao.remove(backup.getId());
        }

        return false;
    }

    @Override
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms) {
        final Map<VirtualMachine, Backup.Metric> metrics = new HashMap<>();
        if (CollectionUtils.isEmpty(vms)) {
            LOG.warn("Unable to get VM Backup Metrics because the list of VMs is empty.");
            return metrics;
        }

        for (final VirtualMachine vm : vms) {
            Long vmBackupSize = 0L;
            Long vmBackupProtectedSize = 0L;
            for (final Backup backup: backupDao.listByVmId(null, vm.getId())) {
                if (Objects.nonNull(backup.getSize())) {
                    vmBackupSize += backup.getSize();
                }
                if (Objects.nonNull(backup.getProtectedSize())) {
                    vmBackupProtectedSize += backup.getProtectedSize();
                }
            }
            Backup.Metric vmBackupMetric = new Backup.Metric(vmBackupSize,vmBackupProtectedSize);
            LOG.debug("Metrics for VM {} is [backup size: {}, data size: {}].", vm, vmBackupMetric.getBackupSize(), vmBackupMetric.getDataSize());
            metrics.put(vm, vmBackupMetric);
        }
        return metrics;
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        return Hypervisor.HypervisorType.KVM.equals(vm.getHypervisorType());
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        return true;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public void syncBackups(VirtualMachine vm, Backup.Metric metric) {
        // TODO: check and sum/return backups metrics on per VM basis
        markAbandonedBackups(vm);
    }

    @Override
    public List<BackupOffering> listBackupOfferings(Long zoneId) {
        final List<BackupRepository> repositories = backupRepositoryDao.listByZoneAndProvider(zoneId, getName());
        final List<BackupOffering> offerings = new ArrayList<>();
        for (final BackupRepository repository : repositories) {
            offerings.add(new NasBackupOffering(repository.getName(), repository.getUuid()));
        }
        return offerings;
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        return true;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                NASBackupParallelExecution,
                NASBackupParallelMaxPerHost,
                NASBackupParallelQueueTimeout,
                NASBackupTimeout
        };
    }

    @Override
    public String getName() {
        return "nas";
    }

    @Override
    public String getDescription() {
        return "NAS Backup Plugin";
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }
}
