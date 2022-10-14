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
import java.util.Map;

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
     * @param vm
     * @param backup
     * @param policy
     * @return
     */
    boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering);

    /**
     * Removes a VM from a backup offering or policy
     * @param vm
     * @return
     */
    boolean removeVMFromBackupOffering(VirtualMachine vm);

    /**
     * Whether the provide will delete backups on removal of VM from the offfering
     * @return boolean result
     */
    boolean willDeleteBackupsOnOfferingRemoval();

    /**
     * Starts and creates an adhoc backup process
     * for a previously registered VM backup
     * @param backup
     * @return
     */
    boolean takeBackup(VirtualMachine vm);

    /**
     * Delete an existing backup
     * @param backuo The backup to exclude
     * @param forced Indicates if backup will be force removed or not
     * @return
     */
    boolean deleteBackup(Backup backup, boolean forced);

    /**
     * Restore VM from backup
     */
    boolean restoreVMFromBackup(VirtualMachine vm, Backup backup);

    /**
     * Restore a volume from a backup
     */
    Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid);

    /**
     * Returns backup metrics for a list of VMs in a zone
     * @param zoneId
     * @param vms
     * @return
     */
    Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms);

    /**
     * This method should reconcile and create backup entries for any backups created out-of-band
     * @param vm
     * @param metric
     */
    void syncBackups(VirtualMachine vm, Backup.Metric metric);
}
