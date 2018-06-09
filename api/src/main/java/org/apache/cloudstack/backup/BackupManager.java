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

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;

/**
 * Backup and Recover Manager Interface
 */
public interface BackupManager extends BackupService, Configurable, PluggableService, Manager {

    ConfigKey<Boolean> BackupFrameworkEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "backup.framework.enabled",
            "false",
            "Is backup and recovery framework enabled.", true, ConfigKey.Scope.Zone);

    ConfigKey<String> BackupProviderPlugin = new ConfigKey<>("Advanced", String.class,
            "backup.framework.provider.plugin",
            "dummy",
            "The backup and recovery provider plugin.", true, ConfigKey.Scope.Zone);

    /**
     * Add a new Backup and Recovery policy to CloudStack by mapping an existing external backup policy to a name and description
     * @param zoneId zone id
     * @param policyExternalId backup policy external id
     * @param policyName internal name for the backup policy
     * @param policyDescription internal description for the backup policy
     */
    BackupPolicy importBackupPolicy(Long zoneId, String policyExternalId, String policyName, String policyDescription);

    /**
     * Assign VM to existing backup policy
     */
    boolean addVMToBackupPolicy(Long policyId, Long virtualMachineId);

    /**
     * Remove a VM from a backup policy
     */
    boolean removeVMFromBackupPolicy(Long policyId, Long vmId);

    /**
     * Return mappings between backup policy and VMs
     */
    List<BackupPolicyVMMap> listBackupPolicyVMMappings(Long vmId, Long zoneId, Long policyId);

    /**
     * List existing backups for a VM
     */
    List<Backup> listVMBackups(Long vmId);

    /**
     * List backup policies
     * @param zoneId zone id
     * @param external if true, only external backup policies are listed
     * @param policyId if not null, only the policy with this id is listed
     */
    List<BackupPolicy> listBackupPolicies(Long zoneId, Boolean external, Long policyId);

    /**
     * Creates backup of a VM
     * @param vmId Virtual Machine ID
     * @return returns operation success
     */
    Backup createBackup(Long vmId);

    /**
     * Deletes a backup
     * @return returns operation success
     */
    boolean deleteBackup(Long backupId);

    /**
     * Restore a full VM from backup
     */
    boolean restoreVMFromBackup(Long backupId);

    /**
     * Restore a backed up volume and attach it to a VM
     */
    boolean restoreBackupVolumeAndAttachToVM(Long volumeId, Long vmId, Long backupId);

    /**
     * Deletes a backup policy
     */
    boolean deleteBackupPolicy(Long policyId);
}
