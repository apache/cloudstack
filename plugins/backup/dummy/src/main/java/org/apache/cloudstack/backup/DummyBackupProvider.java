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

import com.cloud.utils.Pair;
import org.apache.cloudstack.backup.dao.VMBackupDao;
import org.apache.log4j.Logger;

import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;

public class DummyBackupProvider extends AdapterBase implements BackupProvider {

    private static final Logger s_logger = Logger.getLogger(DummyBackupProvider.class);

    @Inject
    private VMBackupDao backupDao;

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
        BackupPolicy policy1 = new BackupPolicyVO("aaaa-aaaa", "Golden Policy", "Gold description");
        BackupPolicy policy2 = new BackupPolicyVO("bbbb-bbbb", "Silver Policy", "Silver description");
        return Arrays.asList(policy1, policy2);
    }

    @Override
    public boolean isBackupPolicy(Long zoneId, String uuid) {
        s_logger.debug("Checking if backup policy exists on the Dummy VMBackup Provider");
        return true;
    }

    @Override
    public VMBackup createVMBackup(BackupPolicy policy, VirtualMachine vm, VMBackup backup) {
        s_logger.debug("Creating VM backup for VM " + vm.getInstanceName() + " from backup policy " + policy.getName());

        List<VMBackup> backups = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
        VMBackupVO dummyBackup = (VMBackupVO) backup;
        dummyBackup.setStatus(VMBackup.Status.BackedUp);
        dummyBackup.setCreated(new Date());
        backups.add(dummyBackup);
        return dummyBackup;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, String backupUuid, String restorePointId) {
        s_logger.debug("Restoring vm " + vm.getUuid() + "from backup " + backupUuid + " on the Dummy Backup Provider");
        return true;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(long zoneId, String backupUuid, String restorePointId, String volumeUuid,
                                                       String hostIp, String dataStoreUuid) {
        s_logger.debug("Restoring volume " + volumeUuid + "from backup " + backupUuid + " on the Dummy VMBackup Provider");
        return null;
    }

    @Override
    public List<VMBackup> listVMBackups(Long zoneId, VirtualMachine vm) {
        s_logger.debug("Listing VM " + vm.getInstanceName() + "backups on the Dummy VMBackup Provider");
        return backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
    }

    @Override
    public Map<VMBackup, VMBackup.Metric> getBackupMetrics(Long zoneId, List<VMBackup> backupList) {
        final Map<VMBackup, VMBackup.Metric> metrics = new HashMap<>();
        final VMBackup.Metric metric = new VMBackup.Metric(1000L, 100L);
        for (VMBackup backup : backupList) {
            metrics.put(backup, metric);
        }
        return metrics;
    }

    @Override
    public boolean removeVMBackup(VirtualMachine vm, VMBackup backup) {
        s_logger.debug("Removing VM backup " + backup.getUuid() + " for VM " + vm.getInstanceName() + " on the Dummy Backup Provider");

        List<VMBackup> backups = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
        for (VMBackup vmBackup : backups) {
            if (vmBackup.getExternalId().equals(backup.getExternalId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean startBackup(VMBackup vmBackup) {
        return true;
    }

    @Override
    public List<VMBackup.RestorePoint> listVMBackupRestorePoints(String backupUuid, VirtualMachine vm) {
        return null;
    }
}
