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
import java.util.Map;

import com.cloud.capacity.Capacity;
import com.cloud.exception.ResourceAllocationException;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.DeleteBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupOfferingsCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupsCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.storage.Volume;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDiskInfo;

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

    ConfigKey<Long> DefaultMaxAccountBackups = new ConfigKey<Long>("Account Defaults", Long.class,
            "max.account.backups",
            "20",
            "The default maximum number of backups that can be created for an account",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxAccountBackupStorage = new ConfigKey<Long>("Account Defaults", Long.class,
            "max.account.backup.storage",
            "400",
            "The default maximum backup storage space (in GiB) that can be used for an account",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxProjectBackups = new ConfigKey<Long>("Project Defaults", Long.class,
            "max.project.backups",
            "20",
            "The default maximum number of backups that can be created for a project",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxProjectBackupStorage = new ConfigKey<Long>("Project Defaults", Long.class,
            "max.project.backup.storage",
            "400",
            "The default maximum backup storage space (in GiB) that can be used for a project",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxDomainBackups = new ConfigKey<Long>("Domain Defaults", Long.class,
            "max.domain.backups",
            "40",
            "The default maximum number of backups that can be created for a domain",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxDomainBackupStorage = new ConfigKey<Long>("Domain Defaults", Long.class,
            "max.domain.backup.storage",
            "800",
            "The default maximum backup storage space (in GiB) that can be used for a domain",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Float> BackupStorageCapacityThreshold = new ConfigKey<>("Alert", Float.class,
            "zone.backupStorage.capacity.notificationthreshold",
            "0.75",
            "Percentage (as a value between 0 and 1) of backup storage utilization above which alerts will be sent about low storage available.",
            true,
            ConfigKey.Scope.Zone,
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
     * @param cmd CreateBackupCmd
     * @param job The async job associated with the backup retention
     * @return returns operation success
     */
    boolean createBackup(CreateBackupCmd cmd, Object job) throws ResourceAllocationException;

    /**
     * List existing backups for a VM
     */
    Pair<List<Backup>, Integer> listBackups(final ListBackupsCmd cmd);

    /**
     * Restore a full VM from backup
     */
    boolean restoreBackup(final Long backupId);

    Map<Long, Network.IpAddresses> getIpToNetworkMapFromBackup(Backup backup, boolean preserveIps, List<Long> networkIds);

    Boolean canCreateInstanceFromBackup(Long backupId);

    /**
     * Restore a backup to a new Instance
     */
    boolean restoreBackupToVM(Long backupId, Long vmId) throws ResourceUnavailableException;

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

    void validateBackupForZone(Long zoneId);

    BackupOffering updateBackupOffering(UpdateBackupOfferingCmd updateBackupOfferingCmd);

    VmDiskInfo getRootDiskInfoFromBackup(Backup backup);

    List<VmDiskInfo> getDataDiskInfoListFromBackup(Backup backup);

    void checkVmDisksSizeAgainstBackup(List<VmDiskInfo> vmDiskInfoList, Backup backup);

    Map<String, String> getBackupDetailsFromVM(VirtualMachine vm);

    String createVolumeInfoFromVolumes(List<Volume> vmVolumes);

    String getBackupNameFromVM(VirtualMachine vm);

    BackupResponse createBackupResponse(Backup backup, Boolean listVmDetails);

    Capacity getBackupStorageUsedStats(Long zoneId);

    void checkAndRemoveBackupOfferingBeforeExpunge(VirtualMachine vm);
}
