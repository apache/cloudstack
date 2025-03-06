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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.storage.Volume;
import com.cloud.storage.dao.VolumeDao;

import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

public class DummyBackupProvider extends AdapterBase implements BackupProvider {
    private static final Logger LOG = LogManager.getLogger(DummyBackupProvider.class);

    @Inject
    private BackupDao backupDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private BackupManager backupManager;

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public String getDescription() {
        return "Dummy Backup Plugin";
    }

    @Override
    public List<BackupOffering> listBackupOfferings(Long zoneId) {
        logger.debug("Listing backup policies on Dummy B&R Plugin");
        BackupOffering policy1 = new BackupOfferingVO(1, "gold-policy", "dummy", "Golden Policy", "Gold description", true);
        BackupOffering policy2 = new BackupOfferingVO(1, "silver-policy", "dummy", "Silver Policy", "Silver description", true);
        return Arrays.asList(policy1, policy2);
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        logger.debug("Checking if backup offering exists on the Dummy Backup Provider");
        return true;
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        logger.debug("Creating VM backup for VM {} from backup offering {}", vm, backupOffering);
        ((VMInstanceVO) vm).setBackupExternalId("dummy-external-backup-id");
        return true;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        logger.debug("Restoring vm {} from backup {} on the Dummy Backup Provider", vm, backup);
        return true;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        logger.debug("Restoring volume {} from backup {} on the Dummy Backup Provider", volumeUuid, backup);
        throw new CloudRuntimeException("Dummy plugin does not support this feature");
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
            LOG.debug("Metrics for VM {} is [backup size: {}, data size: {}].", vm, vmBackupMetric.getBackupSize(), vmBackupMetric.getDataSize());
            metrics.put(vm, vmBackupMetric);
        }
        return metrics;
    }

    @Override
    public List<Backup.RestorePoint> listRestorePoints(VirtualMachine vm) {
        return null;
    }

    @Override
    public Backup createNewBackupEntryForRestorePoint(Backup.RestorePoint restorePoint, VirtualMachine vm, Backup.Metric metric) {
        return null;
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        logger.debug("Removing VM {} from backup offering by the Dummy Backup Provider", vm);
        return true;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public Pair<Boolean, Backup> takeBackup(VirtualMachine vm) {
        logger.debug("Starting backup for VM {} on Dummy provider", vm);

        BackupVO backup = new BackupVO();
        backup.setVmId(vm.getId());
        backup.setExternalId("dummy-external-id");
        backup.setType("FULL");
        backup.setDate(new Date());
        long virtualSize = 0L;
        for (final Volume volume: volumeDao.findByInstance(vm.getId())) {
            if (Volume.State.Ready.equals(volume.getState())) {
                virtualSize += volume.getSize();
            }
        }
        backup.setSize(virtualSize);
        backup.setProtectedSize(virtualSize);
        backup.setStatus(Backup.Status.BackedUp);
        backup.setBackupOfferingId(vm.getBackupOfferingId());
        backup.setAccountId(vm.getAccountId());
        backup.setDomainId(vm.getDomainId());
        backup.setZoneId(vm.getDataCenterId());
        backup.setName(vm.getHostName() + '-' + new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX").format(new Date()));
        backup.setBackedUpVolumes(BackupManagerImpl.createVolumeInfoFromVolumes(volumeDao.findByInstance(vm.getId())));
        Map<String, String> details = backupManager.getVmDetailsForBackup(vm);
        backup.setDetails(details);
        Map<String, String> diskOfferingDetails = backupManager.getDiskOfferingDetailsForBackup(vm.getId());
        backup.addDetails(diskOfferingDetails);

        backup = backupDao.persist(backup);
        return new Pair<>(true, backup);
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        return backupDao.remove(backup.getId());
    }

    @Override
    public boolean supportsInstanceFromBackup() {
        return true;
    }

    @Override
    public Pair<Long, Long> getBackupStorageStats(Long zoneId) {
        return new Pair<>(8L * 1024 * 1024 * 1024, 10L * 1024 * 1024 * 1024);
    }

    @Override
    public void syncBackupStorageStats(Long zoneId) {
    }

    @Override
    public boolean restoreBackupToVM(VirtualMachine vm, Backup backup, String hostIp, String dataStoreUuid) {
        return true;
    }
}
