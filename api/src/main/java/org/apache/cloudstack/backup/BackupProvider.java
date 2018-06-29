//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.backup;

import java.util.List;

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.vm.VirtualMachine;

public interface BackupProvider {

    /**
     * Returns the unique name of the provider
     * @return returns provider name
     */
    String getName();

    /**
     * Returns description about the backup and recovery provider plugin
     * @return returns description
     */
    String getDescription();

    /**
     * Returns the list of existing backup policies on the provider
     * @return backup policies list
     */
    List<BackupPolicy> listBackupPolicies(Long zoneId);

    /**
     * True if policy with id uuid exists on the backup provider
     */
    boolean isBackupPolicy(Long zoneId, String uuid);

    /**
     * Assign VM to backup policy
     * @return true if VM is successfully assigned, false if not
     */
    boolean addVMToBackupPolicy(BackupPolicy policy, VirtualMachine vm);

    /**
     * Remove a VM form a backup policy
     */
    boolean removeVMFromBackupPolicy(BackupPolicy policy, VirtualMachine vm);

    /**
     * Starts ad-hoc backup of a VM assigned to a policy
     * @param policy
     * @param vm
     * @return true if backup successfully starts
     */
    Backup createVMBackup(BackupPolicy policy, VirtualMachine vm);

    /**
     * Restore VM from backup
     */
    boolean restoreVMFromBackup(String vmUuid, String backupUuid);

    /**
     * Restore a volume from a backup
     */
    VolumeTO restoreVolumeFromBackup(String volumeUuid, String backupUuid);

    /**
     * List VM Backups
     */
    List<Backup> listVMBackups(Long zoneId, VirtualMachine vm);

    /**
     * Remove a VM backup
     */
    boolean removeVMBackup(VirtualMachine vm, String backupId);
}
