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

import com.cloud.exception.ResourceAllocationException;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.DeleteBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupOfferingsCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupsCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;

/**
 * Backup and Recover Manager Interface
 */
public interface BackupManager extends BackupService, Configurable, PluggableService, Manager {

    ConfigKey<Boolean> BackupFrameworkEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "backup.framework.enabled",
            "false",
            "Is backup and recovery framework enabled.", false, ConfigKey.Scope.Zone);

    ConfigKey<String> BackupProviderPlugin = new ConfigKey<>("Advanced", String.class,
            "backup.framework.provider.plugin",
            "dummy",
            "The backup and recovery provider plugin.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    ConfigKey<Long> BackupSyncPollingInterval = new ConfigKey<>("Advanced", Long.class,
            "backup.framework.sync.interval",
            "300",
            "The backup and recovery background sync task polling interval in seconds.", true, BackupFrameworkEnabled.key());

    ConfigKey<Boolean> BackupEnableAttachDetachVolumes = new ConfigKey<>("Advanced", Boolean.class,
            "backup.enable.attach.detach.of.volumes",
            "false",
            "Enable volume attach/detach operations for VMs that are assigned to Backup Offerings.", true);

    static final ConfigKey<Integer> BackupHourlyMax = new ConfigKey<Integer>("Advanced", Integer.class,
            "backup.max.hourly",
            "8",
            "Maximum recurring hourly backups to be retained for an instance. If the limit is reached, early backups from the start of the hour are deleted so that newer ones can be saved. This limit does not apply to manual backups. If set to 0, recurring hourly backups can not be scheduled.",
            false,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Integer> BackupDailyMax = new ConfigKey<Integer>("Advanced", Integer.class,
            "backup.max.daily",
            "8",
            "Maximum recurring daily backups to be retained for an instance. If the limit is reached, backups from the start of the day are deleted so that newer ones can be saved. This limit does not apply to manual backups. If set to 0, recurring daily backups can not be scheduled.",
            false,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Integer> BackupWeeklyMax = new ConfigKey<Integer>("Advanced", Integer.class,
            "backup.max.weekly",
            "8",
            "Maximum recurring weekly backups to be retained for an instance. If the limit is reached, backups from the beginning of the week are deleted so that newer ones can be saved. This limit does not apply to manual backups. If set to 0, recurring weekly backups can not be scheduled.",
            false,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Integer> BackupMonthlyMax = new ConfigKey<Integer>("Advanced", Integer.class,
            "backup.max.monthly",
            "8",
            "Maximum recurring monthly backups to be retained for an instance. If the limit is reached, backups from the beginning of the month are deleted so that newer ones can be saved. This limit does not apply to manual backups. If set to 0, recurring monthly backups can not be scheduled.",
            false,
             ConfigKey.Scope.Global,
            null);

    /**
     * List backup provider offerings
     * @param zoneId zone id
     */
    List<BackupOffering> listBackupProviderOfferings(final Long zoneId);

    /**
     * Add a new Backup and Recovery policy to CloudStack by mapping an existing external backup offering to a name and description
     * @param cmd import backup offering cmd
     */
    BackupOffering importBackupOffering(final ImportBackupOfferingCmd cmd);

    /**
     * List backup offerings
     * @param ListBackupOfferingsCmd API cmd
     */
    Pair<List<BackupOffering>, Integer> listBackupOfferings(final ListBackupOfferingsCmd cmd);

    /**
     * Deletes a backup offering
     */
    boolean deleteBackupOffering(final Long policyId);

    /**
     * Assigns a VM to a backup offering
     * @param vmId
     * @param offeringId
     * @return
     */
    boolean assignVMToBackupOffering(final Long vmId, final Long offeringId);

    /**
     * Removes a VM from a backup offering
     * @param vmId
     * @param forced
     * @return
     */
    boolean removeVMFromBackupOffering(final Long vmId, final boolean forced);

    /**
     * Creates or Updates a VM backup schedule
     * @param cmd
     * @return
     */
    BackupSchedule configureBackupSchedule(CreateBackupScheduleCmd cmd);

    /**
     * Lists VM backup schedule for a VM
     * @param vmId
     * @return
     */
    List<BackupSchedule> listBackupSchedule(Long vmId);

    /**
     * Deletes VM backup schedule for a VM
     * @param cmd
     * @return
     */
    boolean deleteBackupSchedule(DeleteBackupScheduleCmd cmd);

    /**
     * Creates backup of a VM
     * @param vmId Virtual Machine ID
     * @param scheduleId Virtual Machine Backup Schedule ID
     * @return returns operation success
     */
    boolean createBackup(final Long vmId, final Long scheduleId) throws ResourceAllocationException;

    /**
     * List existing backups for a VM
     */
    Pair<List<Backup>, Integer> listBackups(final ListBackupsCmd cmd);

    /**
     * Restore a full VM from backup
     */
    boolean restoreBackup(final Long backupId);

    /**
     * Restore a backed up volume and attach it to a VM
     */
    boolean restoreBackupVolumeAndAttachToVM(final String backedUpVolumeUuid, final Long backupId, final Long vmId) throws Exception;

    /**
     * Deletes a backup
     * @param backupId The Id of Backup to exclude
     * @param forced Indicates if backup will be force removed or not
     * @return returns operation success
     */
    boolean deleteBackup(final Long backupId, final Boolean forced);

    BackupOffering updateBackupOffering(UpdateBackupOfferingCmd updateBackupOfferingCmd);
}
