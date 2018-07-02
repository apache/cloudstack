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

import com.cloud.hypervisor.Hypervisor;
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

    ConfigKey<Long> BackupSyncPollingInterval = new ConfigKey<>("Advanced", Long.class,
            "backup.framework.sync.interval",
            "300",
            "The backup and recovery background sync task polling interval in seconds.", true);
    /**
     * Add a new Backup and Recovery policy to CloudStack by mapping an existing external backup policy to a name and description
     * @param zoneId zone id
     * @param policyExternalId backup policy external id
     * @param policyName internal name for the backup policy
     * @param policyDescription internal description for the backup policy
     */
    BackupPolicy importBackupPolicy(final Long zoneId, final String policyExternalId,
                                    final String policyName, final String policyDescription);

    /**
     * List backup policies
     * @param zoneId zone id
     * @param external if true, only external backup policies are listed
     * @param policyId if not null, only the policy with this id is listed
     */
    List<BackupPolicy> listBackupPolicies(final Long zoneId, final Boolean external, final Long policyId);

    /**
     * Deletes a backup policy
     */
    boolean deleteBackupPolicy(final Long policyId);

    /**
     * List existing backups for a VM
     */
    List<VMBackup> listVMBackups(final Long vmId);

    /**
     * Creates backup of a VM
     * @param vmId Virtual Machine ID
     * @return returns operation success
     */
    VMBackup createBackup(final String name, final String description, final Long vmId, final Long policyId);

    /**
     * Assign VM to existing backup policy
     */
    boolean startVMBackup(final Long vmBackupId);

    /**
     * Deletes a backup
     * @return returns operation success
     */
    boolean deleteBackup(final Long backupId);

    /**
     * Restore a full VM from backup
     */
    boolean restoreVMFromBackup(final Long backupId, final String restorePointId);

    /**
     * Restore a backed up volume and attach it to a VM
     */
    boolean restoreBackupVolumeAndAttachToVM(final String backedUpVolumeUuid, final Long vmId, final Long backupId, final String restorePointId) throws Exception;

    boolean importVM(long zoneId, long domainId, long accountId, long userId,
                     String vmInternalName, Hypervisor.HypervisorType hypervisorType, VMBackup backup);

    List<VMBackup.RestorePoint> listVMBackupRestorePoints(final Long backupId);
}
