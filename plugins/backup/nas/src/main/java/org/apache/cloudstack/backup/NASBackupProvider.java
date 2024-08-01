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
import org.apache.cloudstack.backup.dao.BackupOfferingDaoImpl;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class NASBackupProvider extends AdapterBase implements BackupProvider, Configurable {
    private static final Logger LOG = LogManager.getLogger(NASBackupProvider.class);
    private final ConfigKey<String> NasType = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.nas.target.type", "nfs",
            "The NAS storage target type. Only supported: nfs and cephfs", true, ConfigKey.Scope.Zone);
    private final ConfigKey<String> NfsPool = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.nas.nfs.pool", "",
            "The NFS NAS storage pool URL (format <domain|ip>:<path>", true, ConfigKey.Scope.Zone);

    private final ConfigKey<String> CephFSPool = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.nas.cephfs.pool", "",
            "The CephFS storage pool URL (format: <comma-separated domain|ip>:<path>)", true, ConfigKey.Scope.Zone);

    private final ConfigKey<String> CephFSPoolCredentials = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.nas.cephfs.credentials", "",
            "The CephFS storage pool URL (format: <name=username,secret=secretkey>)", true, ConfigKey.Scope.Zone);

    @Inject
    private BackupDao backupDao;

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
    private AgentManager agentManager;

    protected String getNasType(final Long zoneId) {
        return NasType.valueIn(zoneId);
    }

    protected String getBackupStoragePath(final Long zoneId) {
        final String type = getNasType(zoneId);
        if ("nfs".equalsIgnoreCase(type)) {
            return NfsPool.valueIn(zoneId);
        }
        throw new CloudRuntimeException("NAS backup plugin not configured");
    }

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

    protected Host getRunningVMHypervisorHost(VirtualMachine vm) {
        Long hostId = vm.getHostId();
        if (hostId == null) {
            throw new CloudRuntimeException("Unable to find the HYPERVISOR for " + vm.getName() + ". Make sure the virtual machine is running");
        }
        return hostDao.findById(hostId);
    }

    @Override
    public boolean takeBackup(VirtualMachine vm) {
        final Host host = getRunningVMHypervisorHost(vm);
        if (host == null || !Status.Up.equals(host.getStatus()) || !Hypervisor.HypervisorType.KVM.equals(host.getHypervisorType())) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        }

        final BackupOfferingVO backupOffering = new BackupOfferingDaoImpl().findById(vm.getBackupOfferingId());

        final String backupStoragePath = getBackupStoragePath(vm.getDataCenterId());
        final String nasType = getNasType(vm.getDataCenterId());
        final Map<String, String> backupDetails = Map.of(
                "type", nasType
        );
        final String backupPath = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());

        TakeBackupCommand command = new TakeBackupCommand(vm.getInstanceName(), backupPath, backupStoragePath, backupDetails);

        BackupAnswer answer = null;
        try {
            answer = (BackupAnswer) agentManager.send(host.getId(), command);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to initiate backup timed out, please try again");
        }

        if (answer != null) {
            BackupVO backup = new BackupVO();
            backup.setVmId(vm.getId());
            backup.setExternalId(String.format("%s|%s|%s", nasType, backupStoragePath, backupPath));
            backup.setType("FULL");
            backup.setDate(new Date());
            backup.setSize(answer.getSize());
            backup.setProtectedSize(answer.getVirtualSize());
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

        LOG.debug("Restoring vm " + vm.getUuid() + "from backup " + backup.getUuid() + " on the NAS Backup Provider");

        // Find where the VM was last running
        final Host hostVO = getLastVMHypervisorHost(vm);

        // TODO: get KVM agent to restore VM backup

        return true;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid) {
        final Volume volume = volumeDao.findByUuid(volumeUuid);
        final VirtualMachine backupSourceVm = vmInstanceDao.findById(backup.getVmId());
        final StoragePoolHostVO dataStore = storagePoolHostDao.findByUuid(dataStoreUuid);
        final HostVO hostVO = hostDao.findByIp(hostIp);
        final Long zoneId = backup.getZoneId();

        // TODO: Find volume from backup volumes

        VolumeVO restoredVolume = new VolumeVO(Volume.Type.DATADISK, null, backup.getZoneId(),
                backup.getDomainId(), backup.getAccountId(), 0, null,
                backup.getSize(), null, null, null);

        // TODO: fill restored volume VO
        try {
            volumeDao.persist(restoredVolume);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to craft restored volume due to: "+e);
        }

        // TODO: get KVM agent to copy/restore the specific volume to datastore

        return null;
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {

        final Long zoneId = backup.getZoneId();
        final String externalBackupId = backup.getExternalId();

        // TODO: delete backup from NAS

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
        BackupOffering policy = new BackupOfferingVO(zoneId, "default", getName(), "Default", "Default Backup Offering", true);
        return List.of(policy);
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        return true;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                NasType,
                NfsPool,
                CephFSPool,
                CephFSPoolCredentials
        };
    }

    @Override
    public String getName() {
        return "nas";
    }

    @Override
    public String getDescription() {
        return "NAS KVM Backup Plugin";
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }
}
