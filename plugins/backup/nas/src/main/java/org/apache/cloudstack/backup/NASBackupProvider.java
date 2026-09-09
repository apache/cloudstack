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
import java.util.concurrent.ConcurrentHashMap;
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
import java.util.stream.Collectors;

public class NASBackupProvider extends AdapterBase implements BackupProvider, Configurable {
    private static final Logger LOG = LogManager.getLogger(NASBackupProvider.class);

    @Inject
    private BackupDao backupDao;

    @Inject
    private BackupRepositoryDao backupRepositoryDao;

    @Inject
    private BackupOfferingDao backupOfferingDao;

    @Inject
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
            "Maximum number of NAS take-backup commands in flight on one KVM host when parallel execution is enabled; further backups "
            + "for that host wait on the management server. Keep it below the agent's worker thread count (default 5) so start, stop, "
            + "reboot and migrate commands are never queued behind backups.",
            true, ConfigKey.Scope.Zone);

    public static final ConfigKey<Integer> NASBackupParallelQueueTimeout = new ConfigKey<>("Advanced", Integer.class,
            "backup.nas.parallel.queue.timeout",
            "7200",
            "Seconds a NAS take-backup may wait for a free per-host slot before it fails.",
            true, ConfigKey.Scope.Zone);

    /** In-flight take-backup commands per host, so backups never occupy every agent worker thread. */
    private final ConcurrentHashMap<Long, HostBackupSlots> hostBackupSlots = new ConcurrentHashMap<>();

    private static final class HostBackupSlots {
        private int inFlight;
    }

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

        if (VirtualMachine.State.Stopped.equals(vm.getState())) {
            List<VolumeVO> vmVolumes = volumeDao.findByInstance(vm.getId());
            vmVolumes.sort(Comparator.comparing(Volume::getDeviceId));
            List<String> volumePaths = getVolumePaths(vmVolumes);
            command.setVolumePaths(volumePaths);
        }

        // Concurrent backups are bounded per host: wait here, on the management server, for one of the
        // host's slots so the agent's worker threads are never all taken by long-running backups.
        boolean slotHeld = false;
        if (parallel) {
            acquireHostBackupSlot(host.getId(), NASBackupParallelMaxPerHost.valueIn(zoneId), NASBackupParallelQueueTimeout.valueIn(zoneId));
            slotHeld = true;
        }
        BackupVO backupVO = null;
        BackupAnswer answer = null;
        try {
            backupVO = createBackupObject(vm, backupPath);
            answer = (BackupAnswer) agentManager.send(host.getId(), command);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to initiate backup timed out, please try again");
        } finally {
            if (slotHeld) {
                releaseHostBackupSlot(host.getId());
            }
        }

        if (answer != null && answer.getResult()) {
            backupVO.setDate(new Date());
            backupVO.setSize(answer.getSize());
            backupVO.setStatus(Backup.Status.BackedUp);
            backupVO.setBackedUpVolumes(BackupManagerImpl.createVolumeInfoFromVolumes(volumeDao.findByInstance(vm.getId())));
            return backupDao.update(backupVO.getId(), backupVO);
        } else {
            backupVO.setStatus(Backup.Status.Failed);
            backupDao.remove(backupVO.getId());
        }
        return Objects.nonNull(answer) && answer.getResult();
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

    /**
     * Waits for one of the host's backup slots. The caller holds it for the whole agent round-trip and
     * releases it in a finally block. The wait happens on the management server (the async job thread),
     * never on the agent's worker threads. Fails with CloudRuntimeException once the timeout is reached.
     */
    protected void acquireHostBackupSlot(final long hostId, final Integer maxPerHost, final Integer timeoutSeconds) {
        final int max = Math.max(1, maxPerHost == null ? 1 : maxPerHost);
        final int timeout = Math.max(0, timeoutSeconds == null ? 0 : timeoutSeconds);
        final HostBackupSlots slots = hostBackupSlots.computeIfAbsent(hostId, id -> new HostBackupSlots());
        final long deadline = System.currentTimeMillis() + timeout * 1000L;
        synchronized (slots) {
            while (slots.inFlight >= max) {
                final long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new CloudRuntimeException(String.format(
                            "Timed out after %d seconds waiting for a NAS backup slot on host %d (%d of %d in flight); "
                            + "raise %s or %s, or retry when the host's backups finish",
                            timeout, hostId, slots.inFlight, max, NASBackupParallelMaxPerHost.key(), NASBackupParallelQueueTimeout.key()));
                }
                try {
                    slots.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CloudRuntimeException("Interrupted while waiting for a NAS backup slot on host " + hostId);
                }
            }
            slots.inFlight++;
            LOG.debug("Host {} now has {} of {} NAS backups in flight", hostId, slots.inFlight, max);
        }
    }

    protected void releaseHostBackupSlot(final long hostId) {
        final HostBackupSlots slots = hostBackupSlots.get(hostId);
        if (slots == null) {
            return;
        }
        synchronized (slots) {
            if (slots.inFlight > 0) {
                slots.inFlight--;
            }
            slots.notifyAll();
        }
    }

    protected int getInFlightBackups(final long hostId) {
        final HostBackupSlots slots = hostBackupSlots.get(hostId);
        if (slots == null) {
            return 0;
        }
        synchronized (slots) {
            return slots.inFlight;
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
                NASBackupParallelQueueTimeout
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
