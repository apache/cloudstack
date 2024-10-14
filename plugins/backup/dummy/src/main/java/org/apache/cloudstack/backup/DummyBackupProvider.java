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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.storage.dao.VolumeDao;
import org.apache.cloudstack.backup.dao.BackupDao;

import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

public class DummyBackupProvider extends AdapterBase implements BackupProvider {


    @Inject
    private BackupDao backupDao;
    @Inject
    private VolumeDao volumeDao;

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
        logger.debug("Creating VM backup for VM " + vm.getInstanceName() + " from backup offering " + backupOffering.getName());
        ((VMInstanceVO) vm).setBackupExternalId("dummy-external-backup-id");
        return true;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        logger.debug("Restoring vm " + vm.getUuid() + "from backup " + backup.getUuid() + " on the Dummy Backup Provider");
        return true;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        logger.debug("Restoring volume " + volumeUuid + "from backup " + backup.getUuid() + " on the Dummy Backup Provider");
        throw new CloudRuntimeException("Dummy plugin does not support this feature");
    }

    @Override
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms) {
        final Map<VirtualMachine, Backup.Metric> metrics = new HashMap<>();
        final Backup.Metric metric = new Backup.Metric(1000L, 100L);
        if (vms == null || vms.isEmpty()) {
            return metrics;
        }
        for (VirtualMachine vm : vms) {
            if (vm != null) {
                metrics.put(vm, metric);
            }
        }
        return metrics;
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        logger.debug("Removing VM ID " + vm.getUuid() + " from backup offering by the Dummy Backup Provider");
        return true;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return true;
    }

    @Override
    public boolean takeBackup(VirtualMachine vm) {
        logger.debug("Starting backup for VM ID " + vm.getUuid() + " on Dummy provider");

        BackupVO backup = new BackupVO();
        backup.setVmId(vm.getId());
        backup.setExternalId("dummy-external-id");
        backup.setType("FULL");
        backup.setDate(new Date());
        backup.setSize(1024L);
        backup.setProtectedSize(1024000L);
        backup.setStatus(Backup.Status.BackedUp);
        backup.setBackupOfferingId(vm.getBackupOfferingId());
        backup.setAccountId(vm.getAccountId());
        backup.setDomainId(vm.getDomainId());
        backup.setZoneId(vm.getDataCenterId());
        backup.setBackedUpVolumes(BackupManagerImpl.createVolumeInfoFromVolumes(volumeDao.findByInstance(vm.getId())));
        return backupDao.persist(backup) != null;
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        return true;
    }

    @Override
    public void syncBackups(VirtualMachine vm, Backup.Metric metric) {
    }
}
