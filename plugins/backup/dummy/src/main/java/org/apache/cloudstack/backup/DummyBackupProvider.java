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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;

public class DummyBackupProvider extends AdapterBase implements BackupProvider {

    private static final Logger s_logger = Logger.getLogger(DummyBackupProvider.class);

    @Inject
    private BackupDao backupDao;

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
        s_logger.debug("Listing backup policies on Dummy B&R Plugin");
        BackupOffering policy1 = new BackupOfferingVO(1, "aaaa-aaaa", "dummy", "Golden Policy", "Gold description");
        BackupOffering policy2 = new BackupOfferingVO(1, "bbbb-bbbb", "dummy", "Silver Policy", "Silver description");
        return Arrays.asList(policy1, policy2);
    }

    @Override
    public boolean isBackupOffering(Long zoneId, String uuid) {
        s_logger.debug("Checking if backup offering exists on the Dummy Backup Provider");
        return true;
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        s_logger.debug("Creating VM backup for VM " + vm.getInstanceName() + " from backup offering " + backupOffering.getName());
        return true;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        s_logger.debug("Restoring vm " + vm.getUuid() + "from backup " + backup.getUuid() + " on the Dummy Backup Provider");
        return true;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid) {
        s_logger.debug("Restoring volume " + volumeUuid + "from backup " + backup.getUuid() + " on the Dummy Backup Provider");
        return new Pair<>(true, null);
    }

    @Override
    public Map<Backup, Backup.Metric> getBackupMetrics(Long zoneId, List<Backup> backupList) {
        final Map<Backup, Backup.Metric> metrics = new HashMap<>();
        final Backup.Metric metric = new Backup.Metric(1000L, 100L);
        for (Backup backup : backupList) {
            metrics.put(backup, metric);
        }
        return metrics;
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        s_logger.debug("Removing VM ID " + vm.getUuid() + " from backup offering by the Dummy Backup Provider");
        final List<Backup> backups = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
        for (final Backup backup : backups) {
            if (backup.getExternalId().equals(vm.getBackupExternalId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public boolean takeBackup(VirtualMachine vm) {
        s_logger.debug("Starting backup for VM ID " + vm.getUuid() + " on Dummy provider");
        return true;
    }

    @Override
    public boolean deleteBackup(Backup backup) {
        return true;
    }

    @Override
    public List<Backup.RestorePoint> listRestorePoints(VirtualMachine vm) {
        return Arrays.asList(
                new Backup.RestorePoint("aaaaaaaa", "22/08/2017", "Full"),
                new Backup.RestorePoint("bbbbbbbb", "23/08/2017", "Incremental"));
    }
}
