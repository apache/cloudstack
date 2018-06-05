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
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;

public class DummyBackupProvider extends AdapterBase implements BackupProvider {

    private static final Logger s_logger = Logger.getLogger(DummyBackupProvider.class);

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
    public boolean startBackup(BackupPolicy policy, VirtualMachine vm) {
        return true;
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

        BackupTO backup1 = new BackupTO(zoneId, vm.getAccountId(),
                "xxxx-xxxx", "Backup-1", "VM-" + vm.getInstanceName() + "-backup-1",
                null, vm.getId(), null, Backup.Status.BackedUp, new Date());

        BackupTO backup2 = new BackupTO(zoneId, vm.getAccountId(), "yyyy-yyyy",
                "Backup-2", "VM-" + vm.getInstanceName() + "-backup-2",
                backup1.getExternalId(), vm.getId(), null, Backup.Status.BackedUp, new Date());

        return Arrays.asList(backup1, backup2);
    }
}
