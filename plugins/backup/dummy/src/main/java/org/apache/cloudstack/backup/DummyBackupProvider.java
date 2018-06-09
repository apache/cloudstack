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
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;

import javax.inject.Inject;

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
        return "Dummy B&R Plugin";
    }

    @Override
    public List<BackupPolicy> listBackupPolicies(Long zoneId) {
        s_logger.debug("Listing backup policies on Dummy B&R Plugin");
        BackupPolicy policy1 = new BackupPolicyTO("aaaa-aaaa", "Golden Policy", "Gold description");
        BackupPolicy policy2 = new BackupPolicyTO("bbbb-bbbb", "Silver Policy", "Silver description");
        return Arrays.asList(policy1, policy2);
    }

    @Override
    public boolean isBackupPolicy(Long zoneId, String uuid) {
        s_logger.debug("Checking if backup policy exists on the Dummy Backup Provider");
        return true;
    }

    @Override
    public boolean addVMToBackupPolicy(BackupPolicy policy, VirtualMachine vm) {
        s_logger.debug("Assigning VM " + vm.getInstanceName() + " to backup policy " + policy.getName());
        return true;
    }

    @Override
    public boolean removeVMFromBackupPolicy(BackupPolicy policy, VirtualMachine vm) {
        s_logger.debug("Removing VM " + vm.getInstanceName() + " from backup policy " + policy.getName());
        return true;
    }

    @Override
    public Backup createVMBackup(BackupPolicy policy, VirtualMachine vm) {
        s_logger.debug("Creating VM backup for VM " + vm.getInstanceName() + " from backup policy " + policy.getName());

        List<Backup> backups = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
        String backupNumber = String.valueOf(backups.size() + 1);
        Backup lastBackup = null;
        if (backups.size() > 0) {
            backups.sort(Comparator.comparing(Backup::getStartTime));
            lastBackup = backups.get(backups.size() - 1);
        }
        BackupTO newBackup = new BackupTO(vm.getDataCenterId(), vm.getAccountId(),
                "xxxx-xxxx-" + vm.getUuid() + "-" + backupNumber, "Backup-" + vm.getUuid() + backupNumber,
                "VM-" + vm.getInstanceName() + "-backup-" + backupNumber,
                lastBackup != null ? lastBackup.getExternalId() : null, vm.getId(), null,
                Backup.Status.BackedUp, new Date());
        backups.add(newBackup);

        return newBackup;
    }

    @Override
    public boolean restoreVMFromBackup(String vmUuid, String backupUuid) {
        s_logger.debug("Restoring vm " + vmUuid + "from backup " + backupUuid + " on the Dummy Backup Provider");
        return true;
    }

    @Override
    public VolumeTO restoreVolumeFromBackup(String volumeUuid, String backupUuid) {
        s_logger.debug("Restoring volume " + volumeUuid + "from backup " + backupUuid + " on the Dummy Backup Provider");
        return new VolumeTO(0L, Volume.Type.DATADISK, Storage.StoragePoolType.NetworkFilesystem, "pool-aaaa", "volumeTest",
                "/test", "volTest", 1024L, "", "");
    }

    @Override
    public List<Backup> listVMBackups(Long zoneId, VirtualMachine vm) {
        s_logger.debug("Listing VM " + vm.getInstanceName() + "backups on the Dummy Backup Provider");
        return backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
    }

    @Override
    public boolean removeVMBackup(VirtualMachine vm, String backupId) {
        s_logger.debug("Removing VM backup " + backupId + " for VM " + vm.getInstanceName() + " on the Dummy Backup Provider");

        List<Backup> backups = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
        for (Backup backup : backups) {
            if (backup.getExternalId().equals(backupId)) {
                return true;
            }
        }
        return false;
    }
}
