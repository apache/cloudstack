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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.backup.DeleteBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProviderOfferingsCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProvidersCmd;
import org.apache.cloudstack.api.command.user.backup.AssignVirtualMachineToBackupOfferingCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.DeleteBackupCmd;
import org.apache.cloudstack.api.command.user.backup.DeleteBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupOfferingsCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupsCmd;
import org.apache.cloudstack.api.command.user.backup.RemoveVirtualMachineFromBackupOfferingCmd;
import org.apache.cloudstack.api.command.user.backup.RestoreBackupCmd;
import org.apache.cloudstack.api.command.user.backup.RestoreVolumeFromBackupAndAttachToVMCmd;
import org.apache.cloudstack.api.command.user.backup.UpdateBackupScheduleCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupScheduleDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.storage.ScopeType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.usage.dao.UsageBackupDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;
import com.google.gson.Gson;

@Component
public class BackupManagerImpl extends ManagerBase implements BackupManager {
    private static final Logger LOG = Logger.getLogger(BackupManagerImpl.class);

    @Inject
    private BackupDao backupDao;
    @Inject
    private BackupScheduleDao backupScheduleDao;
    @Inject
    private BackupOfferingDao backupOfferingDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private AccountService accountService;
    @Inject
    private AccountManager accountManager;
    @Inject
    private UsageBackupDao usageBackupDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private BackgroundPollManager backgroundPollManager;
    @Inject
    private HostDao hostDao;
    @Inject
    private HypervisorGuruManager hypervisorGuruManager;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;

    private static Map<String, BackupProvider> backupProvidersMap = new HashMap<>();
    private List<BackupProvider> backupProviders;

    @Override
    public List<BackupOffering> listBackupProviderOfferings(final Long zoneId) {
        if (zoneId == null || zoneId < 1) {
            throw new CloudRuntimeException("Invalid zone ID passed");
        }
        final Account account = CallContext.current().getCallingAccount();
        if (!accountService.isRootAdmin(account.getId())) {
            throw new PermissionDeniedException("Parameter external can only be specified by a Root Admin, permission denied");
        }
        final BackupProvider backupProvider = getBackupProvider(zoneId);
        LOG.debug("Listing external backup offerings for the backup provider configured for zone ID " + zoneId);
        return backupProvider.listBackupOfferings(zoneId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_IMPORT_OFFERING, eventDescription = "importing backup offering", async = true)
    public BackupOffering importBackupOffering(final Long zoneId, final String offeringExternalId,
                                               final String offeringName, final String offeringDescription) {
        final BackupOffering existingOffering = backupOfferingDao.findByExternalId(offeringExternalId, zoneId);
        if (existingOffering != null) {
            throw new CloudRuntimeException("A backup offering with external ID " + offeringExternalId + " already exists");
        }
        if (backupOfferingDao.findByName(offeringName, zoneId) != null) {
            throw new CloudRuntimeException("A backup offering with the same name already exists in this zone");
        }

        final BackupProvider provider = getBackupProvider(zoneId);
        if (!provider.isBackupOffering(zoneId, offeringExternalId)) {
            throw new CloudRuntimeException("Backup offering '" + offeringExternalId + "' does not exist on provider " + provider.getName() + " on zone " + zoneId);
        }

        final BackupOfferingVO offering = new BackupOfferingVO(zoneId, offeringExternalId, provider.getName(), offeringName, offeringDescription);
        final BackupOfferingVO savedOffering = backupOfferingDao.persist(offering);
        if (savedOffering == null) {
            throw new CloudRuntimeException("Unable to create backup offering: " + offeringExternalId + ", name: " + offeringName);
        }
        LOG.debug("Successfully created backup offering " + offeringName + " mapped to backup provider offering " + offeringExternalId);
        return savedOffering;
    }

    @Override
    public Pair<List<BackupOffering>, Integer> listBackupOfferings(final ListBackupOfferingsCmd cmd) {
        final Long offeringId = cmd.getOfferingId();
        final Long zoneId = cmd.getZoneId();
        final String keyword = cmd.getKeyword();

        if (offeringId != null) {
            BackupOfferingVO offering = backupOfferingDao.findById(offeringId);
            if (offering == null) {
                throw new CloudRuntimeException("Offering ID " + offeringId + " does not exist");
            }
            return new Pair<>(Collections.singletonList(offering), 1);
        }

        final Filter searchFilter = new Filter(BackupOfferingVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<BackupOfferingVO> sb = backupOfferingDao.createSearchBuilder();
        sb.and("zone_id", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);

        final SearchCriteria<BackupOfferingVO> sc = sb.create();

        if (zoneId != null) {
            sc.setParameters("zone_id", zoneId);
        }

        if (keyword != null) {
            sc.setParameters("name", "%" + keyword + "%");
        }
        Pair<List<BackupOfferingVO>, Integer> result = backupOfferingDao.searchAndCount(sc, searchFilter);
        return new Pair<>(new ArrayList<>(result.first()), result.second());
    }

    @Override
    public boolean deleteBackupOffering(final Long offeringId) {
        if (!backupDao.listByOfferingId(offeringId).isEmpty()) {
            throw new CloudRuntimeException("Cannot allow deletion of backup offering due to use in existing VM backups, please delete the VM backups using the offering first.");
        }
        final BackupOfferingVO offering = backupOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new CloudRuntimeException("Could not find a backup offering with id: " + offeringId);
        }
        return backupOfferingDao.remove(offering.getId());
    }

    private String createVolumeInfoFromVolumes(List<VolumeVO> vmVolumes) {
        List<Backup.VolumeInfo> list = new ArrayList<>();
        for (VolumeVO vol : vmVolumes) {
            list.add(new Backup.VolumeInfo(vol.getUuid(), vol.getPath(), vol.getVolumeType(), vol.getSize()));
        }
        return new Gson().toJson(list.toArray(), Backup.VolumeInfo[].class);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_OFFERING_ASSIGN, eventDescription = "assign VM to backup offering", async = true)
    public boolean assignVMToBackupOffering(Long vmId, Long offeringId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Did not find VM by provided ID");
        }

        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        final BackupOfferingVO offering = backupOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new CloudRuntimeException("Provided backup offering does not exist");
        }

        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        if (backupProvider == null) {
            throw new CloudRuntimeException("Failed to get the backup provider for the zone, please contact the administrator");
        }

        boolean result = false;
        try {
            vm.setBackupOfferingId(offering.getId());
            vm.setBackupVolumes(createVolumeInfoFromVolumes(volumeDao.findByInstance(vm.getId())));
            result = backupProvider.assignVMToBackupOffering(vm, offering);
        } catch (Exception e) {
            LOG.error("Exception caught while assigning VM to backup offering by the backup provider", e);
            throw e;
        }

        if (result && vmInstanceDao.update(vm.getId(), vm)) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_ASSIGN, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                    vm.getUuid(), vm.getBackupOfferingId(), vm.getId(), null,
                    Backup.class.getSimpleName(), vm.getUuid());
            return true;
        }
        throw new CloudRuntimeException("Failed to update VM backup in the database, please try again");
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_OFFERING_REMOVE, eventDescription = "remove VM from backup offering", async = true)
    public boolean removeVMFromBackupOffering(final Long vmId, final boolean forced) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Did not find VM by provided ID");
        }

        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        final BackupOfferingVO offering = backupOfferingDao.findById(vm.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("No previously configured backup offering found for the VM");
        }

        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        if (backupProvider == null) {
            throw new CloudRuntimeException("Failed to get the backup provider for the zone, please contact the administrator");
        }

        if (!forced && backupProvider.willDeleteBackupsOnOfferingRemoval()) {
            throw new CloudRuntimeException("The backend provider will only allow removal of VM from the offering if forced:true is provided " +
                    "that will also delete the backups.");
        }

        vm.setBackupOfferingId(null);
        vm.setBackupExternalId(null);
        vm.setBackupVolumes(null);
        boolean result = backupProvider.removeVMFromBackupOffering(vm);
        if (result && vmInstanceDao.update(vm.getId(), vm)) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_REMOVE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                    vm.getUuid(), vm.getBackupOfferingId(), vm.getId(), null,
                    Backup.class.getSimpleName(), vm.getUuid());
            return true;
        }
        return false;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_SCHEDULE_CONFIGURE, eventDescription = "configuring VM backup schedule", async = true)
    public BackupSchedule configureBackupSchedule(CreateBackupScheduleCmd cmd) {
        final Long vmId = cmd.getVmId();
        final DateUtil.IntervalType intervalType = cmd.getIntervalType();
        final String scheduleString = cmd.getSchedule();
        final TimeZone timeZone = TimeZone.getTimeZone(cmd.getTimezone());

        if (intervalType == null) {
            throw new CloudRuntimeException("Invalid interval type provided");
        }

        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Did not find VM by provided ID");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        final String timezoneId = timeZone.getID();
        if (!timezoneId.equals(cmd.getTimezone())) {
            LOG.warn("Using timezone: " + timezoneId + " for running this snapshot policy as an equivalent of " + cmd.getTimezone());
        }

        try {
            DateUtil.getNextRunTime(intervalType, cmd.getSchedule(), timezoneId, null);
        } catch (Exception e) {
            throw new InvalidParameterValueException("Invalid schedule: " + cmd.getSchedule() + " for interval type: " + cmd.getIntervalType());
        }

        final BackupScheduleVO schedule = backupScheduleDao.findByVM(vmId);
        if (schedule == null) {
            return backupScheduleDao.persist(new BackupScheduleVO(vmId, intervalType, scheduleString, timezoneId));
        }

        schedule.setScheduleType(intervalType);
        schedule.setSchedule(scheduleString);
        schedule.setTimezone(timezoneId);
        backupScheduleDao.update(schedule.getId(), schedule);
        return backupScheduleDao.findByVM(vmId);
    }

    @Override
    public BackupSchedule listBackupSchedule(final Long vmId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Did not find VM by provided ID");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        return backupScheduleDao.findByVM(vmId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_SCHEDULE_DELETE, eventDescription = "deleting VM backup schedule", async = true)
    public boolean deleteBackupSchedule(final Long vmId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Did not find VM by provided ID");
        }

        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        final BackupSchedule schedule = backupScheduleDao.findByVM(vmId);
        if (schedule == null) {
            throw new CloudRuntimeException("VM has no backup schedule defined, no need to delete anything.");
        }
        return backupScheduleDao.remove(schedule.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_CREATE, eventDescription = "creating VM backup", async = true)
    public boolean createBackup(final Long vmId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Did not find VM by provided ID");
        }

        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getBackupOfferingId() == null) {
            throw new CloudRuntimeException("VM has not backup offering configured, cannot create backup before assigning it to a backup offering");
        }

        final BackupOffering offering = backupOfferingDao.findById(vm.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("VM backup offering not found");
        }

        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        if (backupProvider != null && backupProvider.takeBackup(vm)) {
            return true;
        }
        throw new CloudRuntimeException("Failed to create VM backup");
    }

    @Override
    public Pair<List<Backup>, Integer> listBackups(final ListBackupsCmd cmd) {
        final Long id = cmd.getId();
        final Long vmId = cmd.getVmId();
        final Long zoneId = cmd.getZoneId();
        final Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        if (vmId != null) {
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
            if (vm != null) {
                accountManager.checkAccess(caller, null, true, vm);
            }
        }

        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, Project.ListProjectResourcesCriteria>(cmd.getDomainId(),
                cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(SnapshotVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<BackupVO> sb = backupDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
        sb.and("vmId", sb.entity().getVmId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);

        SearchCriteria<BackupVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (vmId != null) {
            sc.setParameters("vmId", vmId);
        }

        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }

        Pair<List<BackupVO>, Integer> result = backupDao.searchAndCount(sc, searchFilter);
        return new Pair<>(new ArrayList<>(result.first()), result.second());
    }

    public boolean importRestoredVM(long zoneId, long domainId, long accountId, long userId,
                                    String vmInternalName, Hypervisor.HypervisorType hypervisorType, Backup backup) {
        VirtualMachine vm = null;
        HypervisorGuru guru = hypervisorGuruManager.getGuru(hypervisorType);
        try {
            vm = guru.importVirtualMachine(zoneId, domainId, accountId, userId, vmInternalName, backup);
        } catch (final Exception e) {
            LOG.error("Failed to import VM from backup restoration", e);
            throw new CloudRuntimeException("Error during vm backup restoration and import: " + e.getMessage());
        }
        if (vm == null) {
            LOG.error("Failed to import restored VM " + vmInternalName + " with hypervisor type " + hypervisorType + " using backup of VM ID " + backup.getVmId());
        }
        return vm != null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_RESTORE, eventDescription = "restoring VM from backup", async = true)
    public boolean restoreBackup(final Long backupId) {
        final BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }

        final VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("VM " + backup.getVmId() + " couldn't be found on existing or removed VMs");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getRemoved() == null && !vm.getState().equals(VirtualMachine.State.Stopped) &&
                !vm.getState().equals(VirtualMachine.State.Destroyed)) {
            throw new CloudRuntimeException("Existing VM should be stopped before being restored from backup");
        }

        final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(vm.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("Failed to find VM backup offering");
        }
        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        if (!backupProvider.restoreVMFromBackup(vm, backup)) {
            throw new CloudRuntimeException("Error restoring VM " + vm.getId() + " from backup " + backup.getId());
        }
        return importRestoredVM(vm.getDataCenterId(), vm.getDomainId(), vm.getAccountId(), vm.getUserId(),
                vm.getInstanceName(), vm.getHypervisorType(), backup);
    }

    private Backup.VolumeInfo getVolumeInfo(List<Backup.VolumeInfo> backedUpVolumes, String volumeUuid) {
        for (Backup.VolumeInfo volInfo : backedUpVolumes) {
            if (volInfo.getUuid().equals(volumeUuid)) {
                return volInfo;
            }
        }
        return null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_RESTORE, eventDescription = "restoring VM from backup", async = true)
    public boolean restoreBackupVolumeAndAttachToVM(final String backedUpVolumeUuid, final Long backupId, final Long vmId) throws Exception {
        if (Strings.isNullOrEmpty(backedUpVolumeUuid)) {
            throw new CloudRuntimeException("Invalid volume ID passed");
        }
        final BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Provided backup not found");
        }

        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Provided VM not found");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (backup.getZoneId() != vm.getDataCenterId()) {
            throw new CloudRuntimeException("Cross zone backup restoration of volume is not allowed");
        }

        final VMInstanceVO vmFromBackup = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vmFromBackup == null) {
            throw new CloudRuntimeException("VM reference for the provided VM backup not found");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vmFromBackup);

        Pair<String, String> restoreInfo = getRestoreVolumeHostAndDatastore(vm);
        String hostIp = restoreInfo.first();
        String datastoreUuid = restoreInfo.second();

        LOG.debug("Asking provider to restore volume " + backedUpVolumeUuid + " from backup " + backupId +
                " (with external ID " + backup.getExternalId() + ") and attach it to VM: " + vm.getUuid());

        final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(vm.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("Failed to find VM backup offering");
        }

        BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        Pair<Boolean, String> result = backupProvider.restoreBackedUpVolume(backup, backedUpVolumeUuid, hostIp, datastoreUuid);
        if (!result.first()) {
            throw new CloudRuntimeException("Error restoring volume " + backedUpVolumeUuid);
        }
        if (!attachVolumeToVM(vm.getDataCenterId(), result.second(), vmFromBackup.getBackupVolumes(),
                            backedUpVolumeUuid, vm, datastoreUuid, backup)) {
            throw new CloudRuntimeException("Error attaching volume " + backedUpVolumeUuid + " to VM " + vm.getUuid());
        }
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_DELETE, eventDescription = "deleting VM backup", async = true)
    public boolean deleteBackup(final Long backupId) {
        final BackupVO backup = backupDao.findByIdIncludingRemoved(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        final Long vmId = backup.getVmId();
        final VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " does not exist");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);
        final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(vm.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("VM backup offering ID " + vm.getBackupOfferingId() + " does not exist");
        }
        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        boolean result = backupProvider.deleteBackup(backup);
        if (result) {
            return backupDao.remove(backup.getId());
        }
        throw new CloudRuntimeException("Failed to delete the backup");
    }

    /**
     * Get the pair: hostIp, datastoreUuid in which to restore the volume, based on the VM to be attached information
     */
    private Pair<String, String> getRestoreVolumeHostAndDatastore(VMInstanceVO vm) {
        List<VolumeVO> rootVmVolume = volumeDao.findIncludingRemovedByInstanceAndType(vm.getId(), Volume.Type.ROOT);
        Long poolId = rootVmVolume.get(0).getPoolId();
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(poolId);
        String datastoreUuid = storagePoolVO.getUuid();
        String hostIp = vm.getHostId() == null ?
                            getHostIp(storagePoolVO) :
                            hostDao.findById(vm.getHostId()).getPrivateIpAddress();
        return new Pair<>(hostIp, datastoreUuid);
    }

    /**
     * Find a host IP from storage pool access
     */
    private String getHostIp(StoragePoolVO storagePoolVO) {
        List<HostVO> hosts = null;
        if (storagePoolVO.getScope().equals(ScopeType.CLUSTER)) {
            hosts = hostDao.findByClusterId(storagePoolVO.getClusterId());

        } else if (storagePoolVO.getScope().equals(ScopeType.ZONE)) {
            hosts = hostDao.findByDataCenterId(storagePoolVO.getDataCenterId());
        }
        return hosts.get(0).getPrivateIpAddress();
    }

    /**
     * Attach volume to VM
     */
    private boolean attachVolumeToVM(Long zoneId, String restoredVolumeLocation, List<Backup.VolumeInfo> backedUpVolumes,
                                     String volumeUuid, VMInstanceVO vm, String datastoreUuid, Backup backup) throws Exception {
        HypervisorGuru guru = hypervisorGuruManager.getGuru(vm.getHypervisorType());
        Backup.VolumeInfo volumeInfo = getVolumeInfo(backedUpVolumes, volumeUuid);
        if (volumeInfo == null) {
            throw new CloudRuntimeException("Failed to find volume in the backedup volumes of ID " + volumeUuid);
        }
        volumeInfo.setType(Volume.Type.DATADISK);

        LOG.debug("Attaching the restored volume to VM " + vm.getId());
        StoragePoolVO pool = primaryDataStoreDao.findByUuid(datastoreUuid);
        try {
            return guru.attachRestoredVolumeToVirtualMachine(zoneId, restoredVolumeLocation, volumeInfo, vm, pool.getId(), backup);
        } catch (Exception e) {
            throw new CloudRuntimeException("Error attach restored volume to VM " + vm.getUuid() + " due to: " + e.getMessage());
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        backgroundPollManager.submitTask(new BackupSyncTask(this));
        return true;
    }

    public boolean isDisabled(final DataCenter zone) {
        return zone == null || Grouping.AllocationState.Disabled.equals(zone.getAllocationState()) || !BackupFrameworkEnabled.valueIn(zone.getId());
    }

    @Override
    public List<BackupProvider> listBackupProviders() {
        return backupProviders;
    }

    @Override
    public BackupProvider getBackupProvider(final Long zoneId) {
        final String name = BackupProviderPlugin.valueIn(zoneId);
        return getBackupProvider(name);
    }

    public BackupProvider getBackupProvider(final String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new CloudRuntimeException("Invalid backup provider name provided");
        }
        if (!backupProvidersMap.containsKey(name)) {
            throw new CloudRuntimeException("Failed to find backup provider by the name: " + name);
        }
        return backupProvidersMap.get(name);
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        // Offerings
        cmdList.add(ListBackupProvidersCmd.class);
        cmdList.add(ListBackupProviderOfferingsCmd.class);
        cmdList.add(ImportBackupOfferingCmd.class);
        cmdList.add(ListBackupOfferingsCmd.class);
        cmdList.add(DeleteBackupOfferingCmd.class);
        // Assignment
        cmdList.add(AssignVirtualMachineToBackupOfferingCmd.class);
        cmdList.add(RemoveVirtualMachineFromBackupOfferingCmd.class);
        // Schedule
        cmdList.add(CreateBackupScheduleCmd.class);
        cmdList.add(UpdateBackupScheduleCmd.class);
        cmdList.add(ListBackupScheduleCmd.class);
        cmdList.add(DeleteBackupScheduleCmd.class);
        // Operations
        cmdList.add(CreateBackupCmd.class);
        cmdList.add(ListBackupsCmd.class);
        cmdList.add(RestoreBackupCmd.class);
        cmdList.add(DeleteBackupCmd.class);
        cmdList.add(RestoreVolumeFromBackupAndAttachToVMCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                BackupFrameworkEnabled,
                BackupProviderPlugin,
                BackupSyncPollingInterval
        };
    }

    public void setBackupProviders(final List<BackupProvider> backupProviders) {
        this.backupProviders = backupProviders;
    }

    @Override
    public boolean start() {
        initializeBackupProviderMap();
        return true;
    }

    private void initializeBackupProviderMap() {
        if (backupProviders != null) {
            for (final BackupProvider backupProvider : backupProviders) {
                backupProvidersMap.put(backupProvider.getName().toLowerCase(), backupProvider);
            }
        }
    }

    ////////////////////////////////////////////////////
    /////////////// Background Tasks ///////////////////
    ////////////////////////////////////////////////////

    /**
     * This background task syncs backups from providers side in CloudStack db
     * along with creation of usage records
     */
    private final class BackupSyncTask extends ManagedContextRunnable implements BackgroundPollTask {
        private BackupManager backupManager;

        public BackupSyncTask(final BackupManager backupManager) {
            this.backupManager = backupManager;
        }

        @Override
        protected void runInContext() {
            final int SYNC_INTERVAL = BackupSyncPollingInterval.value().intValue();
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Backup sync background task is running...");
                }
                for (final DataCenter dataCenter : dataCenterDao.listAllZones()) {
                    if (isDisabled(dataCenter)) {
                        continue;
                    }

                    final BackupProvider backupProvider = getBackupProvider(dataCenter.getId());
                    if (backupProvider == null) {
                        LOG.warn("Backup provider not available or configured for zone ID " + dataCenter.getId());
                        continue;
                    }

                    List<VMInstanceVO> vms = vmInstanceDao.listByZoneWithBackups(dataCenter.getId());
                    if (vms == null || vms.isEmpty()) {
                        continue;
                    }

                    // Sync backup usage metrics
                    final Map<VirtualMachine, Backup.Metric> metrics = backupProvider.getBackupMetrics(dataCenter.getId(), new ArrayList<>(vms));
                    final GlobalLock syncBackupMetricsLock = GlobalLock.getInternLock("BackupSyncTask_metrics_zone_" + dataCenter.getId());
                    if (syncBackupMetricsLock.lock(SYNC_INTERVAL)) {
                        try {
                            for (final VirtualMachine vm : metrics.keySet()) {
                                final Backup.Metric metric = metrics.get(vm);
                                if (metric != null) {
                                    usageBackupDao.updateMetrics(vm, metric);
                                }
                            }
                        } finally {
                            syncBackupMetricsLock.unlock();
                        }
                    }

                    // Sync out-of-band backups
                    for (final VirtualMachine vm : vms) {
                        final GlobalLock syncBackupsLock = GlobalLock.getInternLock("BackupSyncTask_backup_vm_" + vm.getId());
                        if (syncBackupsLock.lock(SYNC_INTERVAL)) {
                            try {
                                backupProvider.syncBackups(vm, metrics.get(vm));
                            } finally {
                                syncBackupsLock.unlock();
                            }
                        }
                    }
                }
            } catch (final Throwable t) {
                LOG.error("Error trying to run backup-sync background task", t);
            }
        }

        @Override
        public Long getDelay() {
            return BackupSyncPollingInterval.value() * 1000L;
        }
    }
}
