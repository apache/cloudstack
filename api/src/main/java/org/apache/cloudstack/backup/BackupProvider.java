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

import com.cloud.utils.Pair;
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
    List<BackupOffering> listBackupOfferings(Long zoneId);

    /**
     * True if a backup offering exists on the backup provider
     */
    boolean isValidProviderOffering(Long zoneId, String uuid);

    /**
     * Assign a VM to a backup offering or policy
     * @param vm the machine to back up
     * @param backupOffering the SLA definition for the backup
     * @return succeeded?
     */
    boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering);

    /**
     * Removes a VM from a backup offering or policy
     * @param vm the machine to stop backing up
     * @return succeeded?
     */
    boolean removeVMFromBackupOffering(VirtualMachine vm);

    /**
     * Whether the provider will delete backups on removal of VM from the offering
     * @return boolean result
     */
    boolean willDeleteBackupsOnOfferingRemoval();

    /**
     * Starts and creates an adhoc backup process
     * for a previously registered VM backup
     *
     * @param vm        the machine to make a backup of
     * @param quiesceVM instance will be quiesced for checkpointing for backup. Applicable only to NAS plugin.
     * @return the result and {code}Backup{code} {code}Object{code}
     */
    Pair<Boolean, Backup> takeBackup(VirtualMachine vm, Boolean quiesceVM);

    /**
     * Delete an existing backup
     * @param backup The backup to exclude
     * @param forced Indicates if backup will be force removed or not
     * @return succeeded?
     */
    boolean deleteBackup(Backup backup, boolean forced);

    boolean restoreBackupToVM(VirtualMachine vm, Backup backup, String hostIp, String dataStoreUuid);

    /**
     * Restore VM from backup
     */
    boolean restoreVMFromBackup(VirtualMachine vm, Backup backup);

    /**
     * Restore a volume from a backup
     */
    Pair<Boolean, String> restoreBackedUpVolume(Backup backup, Backup.VolumeInfo backupVolumeInfo, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState);

    /**
     * Syncs backup metrics (backup size, protected size) from the plugin and stores it within the provider
     * @param zoneId the zone for which to return metrics
     */
    void syncBackupMetrics(Long zoneId);

    /**
     * Returns a list of Backup.RestorePoint
     * @param vm the machine to get the restore points for
     */
    List<Backup.RestorePoint> listRestorePoints(VirtualMachine vm);

    /**
     * Creates and returns an entry in the backups table by getting the information from restorePoint and vm.
     *
     * @param restorePoint the restore point to create a backup for
     * @param vm           The machine for which to create a backup
     */
    Backup createNewBackupEntryForRestorePoint(Backup.RestorePoint restorePoint, VirtualMachine vm);

    /**
     * Returns if the backup provider supports creating new instance from backup
     */
    boolean supportsInstanceFromBackup();

    /**
     * Returns the backup storage usage (Used, Total) for a backup provider
     * @param zoneId the zone for which to return metrics
     * @return a pair of Used size and Total size for the backup storage
     */
    Pair<Long, Long> getBackupStorageStats(Long zoneId);

    /**
     * Gets the backup storage usage (Used, Total) from the plugin and stores it in db
     * @param zoneId the zone for which to return metrics
     */
    void syncBackupStorageStats(Long zoneId);

}
