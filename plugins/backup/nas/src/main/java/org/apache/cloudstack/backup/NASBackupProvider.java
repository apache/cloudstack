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
import com.cloud.agent.api.Answer;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.ScopeType;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

public class NASBackupProvider extends AdapterBase implements BackupProvider, Configurable {
    private static final Logger LOG = LogManager.getLogger(NASBackupProvider.class);

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
                    LOG.debug("Found Host " + hostInCluster.getName());
                    return hostInCluster;
                }
            }
        }
        // Try to find any Host in the zone
        for (final HostVO hostInZone : hostDao.listByDataCenterIdAndHypervisorType(host.getDataCenterId(), Hypervisor.HypervisorType.KVM)) {
            if (hostInZone.getStatus() == Status.Up) {
                LOG.debug("Found Host " + hostInZone.getName());
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
            throw new CloudRuntimeException(String.format("Unable to find the hypervisor host for stopped VM: %s."));
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

        if (VirtualMachine.State.Stopped.equals(vm.getState())) {
            List<VolumeVO> vmVolumes = volumeDao.findByInstance(vm.getId());
            List<String> volumePaths = getVolumePaths(vmVolumes);
            command.setVolumePaths(volumePaths);
        }

        BackupAnswer answer = null;
        try {
            answer = (BackupAnswer) agentManager.send(host.getId(), command);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to initiate backup timed out, please try again");
        }

        if (answer != null && answer.getResult()) {
            BackupVO backup = new BackupVO();
            backup.setVmId(vm.getId());
            backup.setExternalId(backupPath);
            backup.setType("FULL");
            backup.setDate(creationDate);
            backup.setSize(answer.getSize());
            Long virtualSize = 0L;
            for (final Volume volume: volumeDao.findByInstance(vm.getId())) {
                if (Volume.State.Ready.equals(volume.getState())) {
                    virtualSize += volume.getSize();
                }
            }
            backup.setProtectedSize(virtualSize);
            backup.setStatus(Backup.Status.BackedUp);
            backup.setBackupOfferingId(vm.getBackupOfferingId());
            backup.setAccountId(vm.getAccountId());
            backup.setDomainId(vm.getDomainId());
            backup.setZoneId(vm.getDataCenterId());
            return backupDao.persist(backup) != null;
        }

        return false;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        final Long zoneId = backup.getZoneId();

        List<VolumeVO> volumes = volumeDao.findByInstance(vm.getId());
        volumes.removeIf(x -> !Volume.State.Ready.equals(x.getState()));
        volumes.sort(Comparator.comparing(VolumeVO::getDeviceId));

        LOG.debug("Restoring vm " + vm.getUuid() + "from backup " + backup.getUuid() + " on the NAS Backup Provider");
        BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(vm.getBackupOfferingId());
        final String errorMessage = "No valid backup repository found for the VM, please check the attached backup offering";
        if (backupRepository == null) {
            logger.warn(errorMessage + "Re-attempting with the backup offering associated with the backup");
        }
        backupRepository = backupRepositoryDao.findByBackupOfferingId(backup.getBackupOfferingId());
        if (backupRepository == null) {
            throw new CloudRuntimeException(errorMessage);
        }

        // Find where the VM was last running
        final Host host = getLastVMHypervisorHost(vm);
        RestoreBackupCommand restoreCommand = new RestoreBackupCommand();
        restoreCommand.setBackupPath(backup.getExternalId());
        restoreCommand.setBackupRepoType(backupRepository.getType());
        restoreCommand.setBackupRepoAddress(backupRepository.getAddress());
        restoreCommand.setVmName(vm.getName());
        List<String> volumePaths = getVolumePaths(volumes);

        restoreCommand.setVolumePaths(volumePaths);
        restoreCommand.setVmExists(vm.getRemoved() == null);
        // TODO: get KVM agent to restore VM backup

        Answer answer = null;
        try {
            answer = agentManager.send(host.getId(), restoreCommand);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to initiate backup timed out, please try again");
        }
        return true;
    }

    private List<String> getVolumePaths(List<VolumeVO> volumes) {
        List<String> volumePaths = new ArrayList<>();
        for (VolumeVO volume : volumes) {
            StoragePoolVO storagePool = primaryDataStoreDao.findById(volume.getPoolId());
            if (Objects.isNull(storagePool)) {
                throw new CloudRuntimeException("Unable to find storage pool associated to the volume");
            }
            String volumePathPrefix = String.format("/mnt/%s", storagePool.getUuid());
            if (ScopeType.HOST.equals(storagePool.getScope())) {
                volumePathPrefix = storagePool.getPath();
            }
            volumePaths.add(String.format("%s/%s", volumePathPrefix, volume.getPath()));
        }
        return volumePaths;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid) {
        final Volume volume = volumeDao.findByUuid(volumeUuid);
        final VirtualMachine backupSourceVm = vmInstanceDao.findById(backup.getVmId());
        final StoragePoolHostVO dataStore = storagePoolHostDao.findByUuid(dataStoreUuid);
        final HostVO hostVO = hostDao.findByIp(hostIp);
        final Long zoneId = backup.getZoneId();

        // TODO: Find volume from backup volumes
        volume.getDeviceId(); // restore by device ID

        VolumeVO restoredVolume = new VolumeVO(Volume.Type.DATADISK, null, backup.getZoneId(),
                backup.getDomainId(), backup.getAccountId(), 0, null,
                backup.getSize(), null, null, null);

        // TODO: fill restored volume VO
        try {
            volumeDao.persist(restoredVolume);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to craft restored volume due to: "+e);
        }
        // TODO: get KVM agent to copy/restore the specific volume to
        return null;
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        final BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(backup.getBackupOfferingId());
        if (backupRepository == null) {
            throw new CloudRuntimeException("No valid backup repository found for the VM, please check the attached backup offering");
        }

        // TODO: this can be any host in the cluster or last host
        final VirtualMachine vm  = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        final Host host = getVMHypervisorHost(vm);

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
                vmBackupSize += backup.getSize();
                vmBackupProtectedSize += backup.getProtectedSize();
            }
            Backup.Metric vmBackupMetric = new Backup.Metric(vmBackupSize,vmBackupProtectedSize);
            LOG.debug(String.format("Metrics for VM [uuid: %s, name: %s] is [backup size: %s, data size: %s].", vm.getUuid(),
                    vm.getInstanceName(), vmBackupMetric.getBackupSize(), vmBackupMetric.getDataSize()));
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
