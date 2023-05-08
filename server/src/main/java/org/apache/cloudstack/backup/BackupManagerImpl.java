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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import com.cloud.storage.VolumeApiService;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VirtualMachineManager;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.backup.DeleteBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProviderOfferingsCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProvidersCmd;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
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
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.projects.Project;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;

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
    @Inject
    private ApiDispatcher apiDispatcher;
    @Inject
    private AsyncJobManager asyncJobManager;
    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private VolumeApiService volumeApiService;
    @Inject
    private VolumeOrchestrationService volumeOrchestrationService;

    private AsyncJobDispatcher asyncJobDispatcher;
    private Timer backupTimer;
    private Date currentTimestamp;

    private static Map<String, BackupProvider> backupProvidersMap = new HashMap<>();
    private List<BackupProvider> backupProviders;

    public AsyncJobDispatcher getAsyncJobDispatcher() {
        return asyncJobDispatcher;
    }

    public void setAsyncJobDispatcher(final AsyncJobDispatcher dispatcher) {
        asyncJobDispatcher = dispatcher;
    }

    @Override
    public List<BackupOffering> listBackupProviderOfferings(final Long zoneId) {
        if (zoneId == null || zoneId < 1) {
            throw new CloudRuntimeException("Invalid zone ID passed");
        }
        validateForZone(zoneId);
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
    public BackupOffering importBackupOffering(final ImportBackupOfferingCmd cmd) {
        validateForZone(cmd.getZoneId());
        final BackupOffering existingOffering = backupOfferingDao.findByExternalId(cmd.getExternalId(), cmd.getZoneId());
        if (existingOffering != null) {
            throw new CloudRuntimeException("A backup offering with external ID " + cmd.getExternalId() + " already exists");
        }
        if (backupOfferingDao.findByName(cmd.getName(), cmd.getZoneId()) != null) {
            throw new CloudRuntimeException("A backup offering with the same name already exists in this zone");
        }

        final BackupProvider provider = getBackupProvider(cmd.getZoneId());
        if (!provider.isValidProviderOffering(cmd.getZoneId(), cmd.getExternalId())) {
            throw new CloudRuntimeException("Backup offering '" + cmd.getExternalId() + "' does not exist on provider " + provider.getName() + " on zone " + cmd.getZoneId());
        }

        final BackupOfferingVO offering = new BackupOfferingVO(cmd.getZoneId(), cmd.getExternalId(), provider.getName(),
                cmd.getName(), cmd.getDescription(), cmd.getUserDrivenBackups());

        final BackupOfferingVO savedOffering = backupOfferingDao.persist(offering);
        if (savedOffering == null) {
            throw new CloudRuntimeException("Unable to create backup offering: " + cmd.getExternalId() + ", name: " + cmd.getName());
        }
        LOG.debug("Successfully created backup offering " + cmd.getName() + " mapped to backup provider offering " + cmd.getExternalId());
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
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);

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
        final BackupOfferingVO offering = backupOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new CloudRuntimeException("Could not find a backup offering with id: " + offeringId);
        }

        if (vmInstanceDao.listByZoneWithBackups(offering.getZoneId(), offering.getId()).size() > 0) {
            throw new CloudRuntimeException("Backup offering is assigned to VMs, remove the assignment(s) in order to remove the offering.");
        }

        validateForZone(offering.getZoneId());
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
        final VMInstanceVO vm = findVmById(vmId);

        if (!Arrays.asList(VirtualMachine.State.Running, VirtualMachine.State.Stopped, VirtualMachine.State.Shutdown).contains(vm.getState())) {
            throw new CloudRuntimeException("VM is not in running or stopped state");
        }

        validateForZone(vm.getDataCenterId());

        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getBackupOfferingId() != null) {
            throw new CloudRuntimeException("VM already is assigned to a backup offering, please remove the VM from its previous offering");
        }

        final BackupOfferingVO offering = backupOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new CloudRuntimeException("Provided backup offering does not exist");
        }

        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        if (backupProvider == null) {
            throw new CloudRuntimeException("Failed to get the backup provider for the zone, please contact the administrator");
        }

        return transactionAssignVMToBackupOffering(vm, offering, backupProvider) != null;
    }

    private VMInstanceVO transactionAssignVMToBackupOffering(VMInstanceVO vm, BackupOfferingVO offering, BackupProvider backupProvider) {
        return Transaction.execute(TransactionLegacy.CLOUD_DB, new TransactionCallback<VMInstanceVO>() {
            @Override
            public VMInstanceVO doInTransaction(final TransactionStatus status) {
                try {
                    long vmId = vm.getId();
                    vm.setBackupOfferingId(offering.getId());
                    vm.setBackupVolumes(createVolumeInfoFromVolumes(volumeDao.findByInstance(vmId)));

                    if (!backupProvider.assignVMToBackupOffering(vm, offering)) {
                        throw new CloudRuntimeException("Failed to assign the VM to the backup offering, please try removing the assignment and try again.");
                    }

                    if (!vmInstanceDao.update(vmId, vm)) {
                        backupProvider.removeVMFromBackupOffering(vm);
                        throw new CloudRuntimeException("Failed to update VM assignment to the backup offering in the DB, please try again.");
                    }

                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_ASSIGN, vm.getAccountId(), vm.getDataCenterId(), vmId,
                            "Backup-" + vm.getHostName() + "-" + vm.getUuid(), vm.getBackupOfferingId(), null, null, Backup.class.getSimpleName(), vm.getUuid());
                    LOG.debug(String.format("VM [%s] successfully added to Backup Offering [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(vm,
                            "uuid", "instanceName", "backupOfferingId", "backupVolumes"), ReflectionToStringBuilderUtils.reflectOnlySelectedFields(offering,
                                    "uuid", "name", "externalId", "provider")));
                } catch (Exception e) {
                    String msg = String.format("Failed to assign VM [%s] to the Backup Offering [%s], using provider [name: %s, class: %s], due to: [%s].",
                            ReflectionToStringBuilderUtils.reflectOnlySelectedFields(vm, "uuid", "instanceName", "backupOfferingId", "backupVolumes"),
                            ReflectionToStringBuilderUtils.reflectOnlySelectedFields(offering, "uuid", "name", "externalId", "provider"),
                            backupProvider.getName(), backupProvider.getClass().getSimpleName(), e.getMessage());
                    LOG.error(msg);
                    LOG.debug(msg, e);
                }
                return vm;
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_OFFERING_REMOVE, eventDescription = "remove VM from backup offering", async = true)
    public boolean removeVMFromBackupOffering(final Long vmId, final boolean forced) {
        final VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            throw new CloudRuntimeException(String.format("Can't find any VM with ID: [%s].", vmId));
        }

        validateForZone(vm.getDataCenterId());
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
            String message = String.format("To remove VM [id: %s, name: %s] from Backup Offering [id: %s, name: %s] using the provider [%s], please specify the "
                    + "forced:true option to allow the deletion of all jobs and backups for this VM or remove the backups that this VM has with the backup "
                    + "offering.", vm.getUuid(), vm.getInstanceName(), offering.getUuid(), offering.getName(), backupProvider.getClass().getSimpleName());
            throw new CloudRuntimeException(message);
        }

        boolean result = false;
        try {
            vm.setBackupOfferingId(null);
            vm.setBackupExternalId(null);
            vm.setBackupVolumes(null);
            result = backupProvider.removeVMFromBackupOffering(vm);
            if (result && backupProvider.willDeleteBackupsOnOfferingRemoval()) {
                final List<Backup> backups = backupDao.listByVmId(null, vm.getId());
                for (final Backup backup : backups) {
                    backupDao.remove(backup.getId());
                }
            }
            if ((result || forced) && vmInstanceDao.update(vm.getId(), vm)) {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_REMOVE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                        "Backup-" + vm.getHostName() + "-" + vm.getUuid(), vm.getBackupOfferingId(), null, null,
                        Backup.class.getSimpleName(), vm.getUuid());
                final BackupSchedule backupSchedule = backupScheduleDao.findByVM(vm.getId());
                if (backupSchedule != null) {
                    backupScheduleDao.remove(backupSchedule.getId());
                }
                result = true;
            }
        } catch (Exception e) {
            LOG.error(String.format("Exception caught when trying to remove VM [uuid: %s, name: %s] from the backup offering [uuid: %s, name: %s] due to: [%s].",
                    vm.getUuid(), vm.getInstanceName(), offering.getUuid(), offering.getName(), e.getMessage()), e);
        }
        return result;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_SCHEDULE_CONFIGURE, eventDescription = "configuring VM backup schedule")
    public BackupSchedule configureBackupSchedule(CreateBackupScheduleCmd cmd) {
        final Long vmId = cmd.getVmId();
        final DateUtil.IntervalType intervalType = cmd.getIntervalType();
        final String scheduleString = cmd.getSchedule();
        final TimeZone timeZone = TimeZone.getTimeZone(cmd.getTimezone());

        if (intervalType == null) {
            throw new CloudRuntimeException("Invalid interval type provided");
        }

        final VMInstanceVO vm = findVmById(vmId);
        validateForZone(vm.getDataCenterId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getBackupOfferingId() == null) {
            throw new CloudRuntimeException("Cannot configure backup schedule for the VM without having any backup offering");
        }

        final BackupOffering offering = backupOfferingDao.findById(vm.getBackupOfferingId());
        if (offering == null || !offering.isUserDrivenBackupAllowed()) {
            throw new CloudRuntimeException("The selected backup offering does not allow user-defined backup schedule");
        }

        final String timezoneId = timeZone.getID();
        if (!timezoneId.equals(cmd.getTimezone())) {
            LOG.warn("Using timezone: " + timezoneId + " for running this snapshot policy as an equivalent of " + cmd.getTimezone());
        }

        Date nextDateTime = null;
        try {
            nextDateTime = DateUtil.getNextRunTime(intervalType, cmd.getSchedule(), timezoneId, null);
        } catch (Exception e) {
            throw new InvalidParameterValueException("Invalid schedule: " + cmd.getSchedule() + " for interval type: " + cmd.getIntervalType());
        }

        final BackupScheduleVO schedule = backupScheduleDao.findByVM(vmId);
        if (schedule == null) {
            return backupScheduleDao.persist(new BackupScheduleVO(vmId, intervalType, scheduleString, timezoneId, nextDateTime));
        }

        schedule.setScheduleType((short) intervalType.ordinal());
        schedule.setSchedule(scheduleString);
        schedule.setTimezone(timezoneId);
        schedule.setScheduledTimestamp(nextDateTime);
        backupScheduleDao.update(schedule.getId(), schedule);
        return backupScheduleDao.findByVM(vmId);
    }

    @Override
    public BackupSchedule listBackupSchedule(final Long vmId) {
        final VMInstanceVO vm = findVmById(vmId);
        validateForZone(vm.getDataCenterId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        return backupScheduleDao.findByVM(vmId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_SCHEDULE_DELETE, eventDescription = "deleting VM backup schedule")
    public boolean deleteBackupSchedule(final Long vmId) {
        final VMInstanceVO vm = findVmById(vmId);
        validateForZone(vm.getDataCenterId());
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
        final VMInstanceVO vm = findVmById(vmId);
        validateForZone(vm.getDataCenterId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getBackupOfferingId() == null) {
            throw new CloudRuntimeException("VM has not backup offering configured, cannot create backup before assigning it to a backup offering");
        }

        final BackupOffering offering = backupOfferingDao.findById(vm.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("VM backup offering not found");
        }

        if (!offering.isUserDrivenBackupAllowed()) {
            throw new CloudRuntimeException("The assigned backup offering does not allow ad-hoc user backup");
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
        final String keyword = cmd.getKeyword();
        List<Long> permittedAccounts = new ArrayList<Long>();

        if (vmId != null) {
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
            if (vm != null) {
                accountManager.checkAccess(caller, null, true, vm);
            }
        }

        final Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, Project.ListProjectResourcesCriteria>(cmd.getDomainId(),
                cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        final Long domainId = domainIdRecursiveListProject.first();
        final Boolean isRecursive = domainIdRecursiveListProject.second();
        final Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        final Filter searchFilter = new Filter(BackupVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<BackupVO> sb = backupDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
        sb.and("vmId", sb.entity().getVmId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);

        if (keyword != null) {
            SearchBuilder<VMInstanceVO> vmSearch = vmInstanceDao.createSearchBuilder();
            vmSearch.and("name", vmSearch.entity().getHostName(), SearchCriteria.Op.LIKE);
            sb.groupBy(sb.entity().getId());
            sb.join("vmSearch", vmSearch, sb.entity().getVmId(), vmSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

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

        if (keyword != null) {
            sc.setJoinParameters("vmSearch", "name", "%" + keyword + "%");
        }

        Pair<List<BackupVO>, Integer> result = backupDao.searchAndCount(sc, searchFilter);
        return new Pair<>(new ArrayList<>(result.first()), result.second());
    }

    public boolean importRestoredVM(long zoneId, long domainId, long accountId, long userId,
                                    String vmInternalName, Hypervisor.HypervisorType hypervisorType, Backup backup) {
        VirtualMachine vm = null;
        HypervisorGuru guru = hypervisorGuruManager.getGuru(hypervisorType);
        try {
            vm = guru.importVirtualMachineFromBackup(zoneId, domainId, accountId, userId, vmInternalName, backup);
        } catch (final Exception e) {
            LOG.error(String.format("Failed to import VM [vmInternalName: %s] from backup restoration [%s] with hypervisor [type: %s] due to: [%s].", vmInternalName,
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backup, "id", "uuid", "vmId", "externalId", "backupType"), hypervisorType, e.getMessage()), e);
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
        validateForZone(backup.getZoneId());

        final VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("VM ID " + backup.getVmId() + " couldn't be found on existing or removed VMs");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getRemoved() == null && !vm.getState().equals(VirtualMachine.State.Stopped) &&
                !vm.getState().equals(VirtualMachine.State.Destroyed)) {
            throw new CloudRuntimeException("Existing VM should be stopped before being restored from backup");
        }

        final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(vm.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("Failed to find backup offering of the VM backup.");
        }

        String backupDetailsInMessage = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backup, "uuid", "externalId", "vmId", "type", "status", "date");
        tryRestoreVM(backup, vm, offering, backupDetailsInMessage);
        updateVolumeState(vm, Volume.Event.RestoreSucceeded, Volume.State.Ready);
        updateVmState(vm, VirtualMachine.Event.RestoringSuccess, VirtualMachine.State.Stopped);

        return importRestoredVM(vm.getDataCenterId(), vm.getDomainId(), vm.getAccountId(), vm.getUserId(),
                vm.getInstanceName(), vm.getHypervisorType(), backup);
    }

    /**
     * Tries to restore a VM from a backup. <br/>
     * First update the VM state to {@link VirtualMachine.Event#RestoringRequested} and its volume states to {@link Volume.Event#RestoreRequested}, <br/>
     * and then try to restore the backup. <br/>
     *
     * If restore fails, then update the VM state to {@link VirtualMachine.Event#RestoringFailed}, and its volumes to {@link Volume.Event#RestoreFailed} and throw an {@link CloudRuntimeException}.
     */
    protected void tryRestoreVM(BackupVO backup, VMInstanceVO vm, BackupOffering offering, String backupDetailsInMessage) {
        try {
            updateVmState(vm, VirtualMachine.Event.RestoringRequested, VirtualMachine.State.Restoring);
            updateVolumeState(vm, Volume.Event.RestoreRequested, Volume.State.Restoring);
            final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
            if (!backupProvider.restoreVMFromBackup(vm, backup)) {
                throw new CloudRuntimeException(String.format("Error restoring %s from backup [%s].", vm, backupDetailsInMessage));
            }
        // The restore process is executed by a backup provider outside of ACS, I am using the catch-all (Exception) to
        // ensure that no provider-side exception is missed. Therefore, we have a proper handling of exceptions, and rollbacks if needed.
        } catch (Exception e) {
            LOG.error(String.format("Failed to restore backup [%s] due to: [%s].", backupDetailsInMessage, e.getMessage()), e);
            updateVolumeState(vm, Volume.Event.RestoreFailed, Volume.State.Ready);
            updateVmState(vm, VirtualMachine.Event.RestoringFailed, VirtualMachine.State.Stopped);
            throw new CloudRuntimeException(String.format("Error restoring VM from backup [%s].", backupDetailsInMessage));
        }
    }

    /**
     * Tries to update the state of given VM, given specified event
     * @param vm The VM to update its state
     * @param event The event to update the VM state
     * @param next The desired state, just needed to add more context to the logs
     */
    private void updateVmState(VMInstanceVO vm, VirtualMachine.Event event, VirtualMachine.State next) {
        LOG.debug(String.format("Trying to update state of VM [%s] with event [%s].", vm, event));
        Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<VMInstanceVO>) status -> {
            try {
                if (!virtualMachineManager.stateTransitTo(vm, event, vm.getHostId())) {
                    throw new CloudRuntimeException(String.format("Unable to change state of VM [%s] to [%s].", vm, next));
                }
            } catch (NoTransitionException e) {
                String errMsg = String.format("Failed to update state of VM [%s] with event [%s] due to [%s].", vm, event, e.getMessage());
                LOG.error(errMsg, e);
                throw new RuntimeException(errMsg);
            }
            return null;
        });
    }

    /**
     * Tries to update all volume states of given VM, given specified event
     * @param vm The VM to which the volumes belong
     * @param event The event to update the volume states
     * @param next The desired state, just needed to add more context to the logs
     */
    private void updateVolumeState(VMInstanceVO vm, Volume.Event event, Volume.State next) {
        Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<VolumeVO>) status -> {
            for (VolumeVO volume : volumeDao.findIncludingRemovedByInstanceAndType(vm.getId(), null)) {
                tryToUpdateStateOfSpecifiedVolume(volume, event, next);
            }
            return null;
        });
    }

    /**
     * Tries to update the state of just one volume using any passed {@link Volume.Event}. Throws an {@link RuntimeException} when fails.
     * @param volume The volume to update it state
     * @param event The event to update the volume state
     * @param next The desired state, just needed to add more context to the logs
     *
     */
    private void tryToUpdateStateOfSpecifiedVolume(VolumeVO volume, Volume.Event event, Volume.State next) {
        LOG.debug(String.format("Trying to update state of volume [%s] with event [%s].", volume, event));
        try {
            if (!volumeApiService.stateTransitTo(volume, event)) {
                throw new CloudRuntimeException(String.format("Unable to change state of volume [%s] to [%s].", volume, next));
            }
        } catch (NoTransitionException e) {
            String errMsg = String.format("Failed to update state of volume [%s] with event [%s] due to [%s].", volume, event, e.getMessage());
            LOG.error(errMsg, e);
            throw new RuntimeException(errMsg);
        }
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
        if (StringUtils.isEmpty(backedUpVolumeUuid)) {
            throw new CloudRuntimeException("Invalid volume ID passed");
        }
        final BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Provided backup not found");
        }
        validateForZone(backup.getZoneId());

        final VMInstanceVO vm = findVmById(vmId);
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getBackupOfferingId() != null && !BackupEnableAttachDetachVolumes.value()) {
            throw new CloudRuntimeException("The selected VM has backups, cannot restore and attach volume to the VM.");
        }

        if (backup.getZoneId() != vm.getDataCenterId()) {
            throw new CloudRuntimeException("Cross zone backup restoration of volume is not allowed");
        }

        final VMInstanceVO vmFromBackup = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vmFromBackup == null) {
            throw new CloudRuntimeException("VM reference for the provided VM backup not found");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vmFromBackup);

        Pair<HostVO, StoragePoolVO> restoreInfo = getRestoreVolumeHostAndDatastore(vm);
        HostVO host = restoreInfo.first();
        StoragePoolVO datastore = restoreInfo.second();

        LOG.debug("Asking provider to restore volume " + backedUpVolumeUuid + " from backup " + backupId +
                " (with external ID " + backup.getExternalId() + ") and attach it to VM: " + vm.getUuid());

        final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("Failed to find VM backup offering");
        }

        BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        LOG.debug(String.format("Trying to restore volume using host private IP address: [%s].", host.getPrivateIpAddress()));

        String[] hostPossibleValues = {host.getPrivateIpAddress(), host.getName()};
        String[] datastoresPossibleValues = {datastore.getUuid(), datastore.getName()};

        updateVmState(vm, VirtualMachine.Event.RestoringRequested, VirtualMachine.State.Restoring);
        Pair<Boolean, String> result = restoreBackedUpVolume(backedUpVolumeUuid, backup, backupProvider, hostPossibleValues, datastoresPossibleValues);

        if (BooleanUtils.isFalse(result.first())) {
            updateVmState(vm, VirtualMachine.Event.RestoringFailed, VirtualMachine.State.Stopped);
            throw new CloudRuntimeException(String.format("Error restoring volume [%s] of VM [%s] to host [%s] using backup provider [%s] due to: [%s].",
                    backedUpVolumeUuid, vm.getUuid(), host.getUuid(), backupProvider.getName(), result.second()));
        }
        if (!attachVolumeToVM(vm.getDataCenterId(), result.second(), vmFromBackup.getBackupVolumeList(),
                            backedUpVolumeUuid, vm, datastore.getUuid(), backup)) {
            updateVmState(vm, VirtualMachine.Event.RestoringFailed, VirtualMachine.State.Stopped);
            throw new CloudRuntimeException(String.format("Error attaching volume [%s] to VM [%s]." + backedUpVolumeUuid, vm.getUuid()));
        }
        updateVmState(vm, VirtualMachine.Event.RestoringSuccess, VirtualMachine.State.Stopped);
        return true;
    }

    protected Pair<Boolean, String> restoreBackedUpVolume(final String backedUpVolumeUuid, final BackupVO backup, BackupProvider backupProvider, String[] hostPossibleValues,
            String[] datastoresPossibleValues) {
        Pair<Boolean, String> result = new  Pair<>(false, "");
        for (String hostData : hostPossibleValues) {
            for (String datastoreData : datastoresPossibleValues) {
                LOG.debug(String.format("Trying to restore volume [UUID: %s], using host [%s] and datastore [%s].",
                        backedUpVolumeUuid, hostData, datastoreData));

                try {
                    result = backupProvider.restoreBackedUpVolume(backup, backedUpVolumeUuid, hostData, datastoreData);

                    if (BooleanUtils.isTrue(result.first())) {
                        return result;
                    }
                } catch (Exception e) {
                    LOG.debug(String.format("Failed to restore volume [UUID: %s], using host [%s] and datastore [%s] due to: [%s].",
                            backedUpVolumeUuid, hostData, datastoreData, e.getMessage()), e);
                }
            }
        }
        return result;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_DELETE, eventDescription = "deleting VM backup", async = true)
    public boolean deleteBackup(final Long backupId, final Boolean forced) {
        final BackupVO backup = backupDao.findByIdIncludingRemoved(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        final Long vmId = backup.getVmId();
        final VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " does not exist");
        }
        validateForZone(vm.getDataCenterId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);
        final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException(String.format("Backup offering with ID [%s] does not exist.", backup.getBackupOfferingId()));
        }
        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        boolean result = backupProvider.deleteBackup(backup, forced);
        if (result) {
            return backupDao.remove(backup.getId());
        }
        throw new CloudRuntimeException("Failed to delete the backup");
    }

    /**
     * Get the pair: hostIp, datastoreUuid in which to restore the volume, based on the VM to be attached information
     */
    private Pair<HostVO, StoragePoolVO> getRestoreVolumeHostAndDatastore(VMInstanceVO vm) {
        List<VolumeVO> rootVmVolume = volumeDao.findIncludingRemovedByInstanceAndType(vm.getId(), Volume.Type.ROOT);
        Long poolId = rootVmVolume.get(0).getPoolId();
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(poolId);
        HostVO hostVO = vm.getHostId() == null ?
                            getFirstHostFromStoragePool(storagePoolVO) :
                            hostDao.findById(vm.getHostId());
        return new Pair<>(hostVO, storagePoolVO);
    }

    /**
     * Find a host from storage pool access
     */
    private HostVO getFirstHostFromStoragePool(StoragePoolVO storagePoolVO) {
        List<HostVO> hosts = null;
        if (storagePoolVO.getScope().equals(ScopeType.CLUSTER)) {
            hosts = hostDao.findByClusterId(storagePoolVO.getClusterId());

        } else if (storagePoolVO.getScope().equals(ScopeType.ZONE)) {
            hosts = hostDao.findByDataCenterId(storagePoolVO.getDataCenterId());
        }
        return hosts.get(0);
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

    public boolean isDisabled(final Long zoneId) {
        return !(BackupFrameworkEnabled.value() && BackupFrameworkEnabled.valueIn(zoneId));
    }

    private void validateForZone(final Long zoneId) {
        if (zoneId == null || isDisabled(zoneId)) {
            throw new CloudRuntimeException("Backup and Recovery feature is disabled for the zone");
        }
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
        if (StringUtils.isEmpty(name)) {
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
        if (!BackupFrameworkEnabled.value()) {
            return cmdList;
        }

        // Offerings
        cmdList.add(ListBackupProvidersCmd.class);
        cmdList.add(ListBackupProviderOfferingsCmd.class);
        cmdList.add(ImportBackupOfferingCmd.class);
        cmdList.add(ListBackupOfferingsCmd.class);
        cmdList.add(DeleteBackupOfferingCmd.class);
        cmdList.add(UpdateBackupOfferingCmd.class);
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
                BackupSyncPollingInterval,
                BackupEnableAttachDetachVolumes
        };
    }

    public void setBackupProviders(final List<BackupProvider> backupProviders) {
        this.backupProviders = backupProviders;
    }

    private void initializeBackupProviderMap() {
        if (backupProviders != null) {
            for (final BackupProvider backupProvider : backupProviders) {
                backupProvidersMap.put(backupProvider.getName().toLowerCase(), backupProvider);
            }
        }
    }

    public void poll(final Date timestamp) {
        currentTimestamp = timestamp;
        GlobalLock scanLock = GlobalLock.getInternLock("backup.poll");
        try {
            if (scanLock.lock(5)) {
                try {
                    checkStatusOfCurrentlyExecutingBackups();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }

        scanLock = GlobalLock.getInternLock("backup.poll");
        try {
            if (scanLock.lock(5)) {
                try {
                    scheduleBackups();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
    }

    @DB
    private Date scheduleNextBackupJob(final BackupScheduleVO backupSchedule) {
        final Date nextTimestamp = DateUtil.getNextRunTime(backupSchedule.getScheduleType(), backupSchedule.getSchedule(),
                backupSchedule.getTimezone(), currentTimestamp);
        return Transaction.execute(new TransactionCallback<Date>() {
            @Override
            public Date doInTransaction(TransactionStatus status) {
                backupSchedule.setScheduledTimestamp(nextTimestamp);
                backupSchedule.setAsyncJobId(null);
                backupScheduleDao.update(backupSchedule.getId(), backupSchedule);
                return nextTimestamp;
            }
        });
    }

    private void checkStatusOfCurrentlyExecutingBackups() {
        final SearchCriteria<BackupScheduleVO> sc = backupScheduleDao.createSearchCriteria();
        sc.addAnd("asyncJobId", SearchCriteria.Op.NNULL);
        final List<BackupScheduleVO> backupSchedules = backupScheduleDao.search(sc, null);
        for (final BackupScheduleVO backupSchedule : backupSchedules) {
            final Long asyncJobId = backupSchedule.getAsyncJobId();
            final AsyncJobVO asyncJob = asyncJobManager.getAsyncJob(asyncJobId);
            switch (asyncJob.getStatus()) {
                case SUCCEEDED:
                case FAILED:
                    final Date nextDateTime = scheduleNextBackupJob(backupSchedule);
                    final String nextScheduledTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, nextDateTime);
                    LOG.debug("Next backup scheduled time for VM ID " + backupSchedule.getVmId() + " is " + nextScheduledTime);
                    break;
            default:
                LOG.debug(String.format("Found async backup job [id: %s, vmId: %s] with status [%s] and cmd information: [cmd: %s, cmdInfo: %s].", asyncJob.getId(), backupSchedule.getVmId(),
                        asyncJob.getStatus(), asyncJob.getCmd(), asyncJob.getCmdInfo()));
                break;
            }
        }
    }

    @DB
    public void scheduleBackups() {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        LOG.debug("Backup backup.poll is being called at " + displayTime);

        final List<BackupScheduleVO> backupsToBeExecuted = backupScheduleDao.getSchedulesToExecute(currentTimestamp);
        for (final BackupScheduleVO backupSchedule: backupsToBeExecuted) {
            final Long backupScheduleId = backupSchedule.getId();
            final Long vmId = backupSchedule.getVmId();

            final VMInstanceVO vm = vmInstanceDao.findById(vmId);
            if (vm == null || vm.getBackupOfferingId() == null) {
                backupScheduleDao.remove(backupScheduleId);
                continue;
            }

            final BackupOffering offering = backupOfferingDao.findById(vm.getBackupOfferingId());
            if (offering == null || !offering.isUserDrivenBackupAllowed()) {
                continue;
            }

            if (isDisabled(vm.getDataCenterId())) {
                continue;
            }

            final Account backupAccount = accountService.getAccount(vm.getAccountId());
            if (backupAccount == null || backupAccount.getState() == Account.State.DISABLED) {
                LOG.debug(String.format("Skip backup for VM [uuid: %s, name: %s] since its account has been removed or disabled.", vm.getUuid(), vm.getInstanceName()));
                continue;
            }

            if (LOG.isDebugEnabled()) {
                final Date scheduledTimestamp = backupSchedule.getScheduledTimestamp();
                displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
                LOG.debug(String.format("Scheduling 1 backup for VM [ID: %s, name: %s, hostName: %s] for backup schedule id: [%s] at [%s].",
                        vm.getId(), vm.getInstanceName(), vm.getHostName(), backupSchedule.getId(), displayTime));
            }

            BackupScheduleVO tmpBackupScheduleVO = null;

            try {
                tmpBackupScheduleVO = backupScheduleDao.acquireInLockTable(backupScheduleId);

                final Long eventId = ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, vm.getAccountId(),
                        EventTypes.EVENT_VM_BACKUP_CREATE, "creating backup for VM ID:" + vm.getUuid(),
                        vmId, ApiCommandResourceType.VirtualMachine.toString(),
                        true, 0);
                final Map<String, String> params = new HashMap<String, String>();
                params.put(ApiConstants.VIRTUAL_MACHINE_ID, "" + vmId);
                params.put("ctxUserId", "1");
                params.put("ctxAccountId", "" + vm.getAccountId());
                params.put("ctxStartEventId", String.valueOf(eventId));

                final CreateBackupCmd cmd = new CreateBackupCmd();
                ComponentContext.inject(cmd);
                apiDispatcher.dispatchCreateCmd(cmd, params);
                params.put("id", "" + vmId);
                params.put("ctxStartEventId", "1");

                AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vm.getAccountId(), CreateBackupCmd.class.getName(),
                        ApiGsonHelper.getBuilder().create().toJson(params), vmId,
                        cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
                job.setDispatcher(asyncJobDispatcher.getName());

                final long jobId = asyncJobManager.submitAsyncJob(job);
                tmpBackupScheduleVO.setAsyncJobId(jobId);
                backupScheduleDao.update(backupScheduleId, tmpBackupScheduleVO);
            } catch (Exception e) {
                LOG.error(String.format("Scheduling backup failed due to: [%s].", e.getMessage()), e);
            } finally {
                if (tmpBackupScheduleVO != null) {
                    backupScheduleDao.releaseFromLockTable(backupScheduleId);
                }
            }
        }
    }

    @Override
    public boolean start() {
        initializeBackupProviderMap();

        currentTimestamp = new Date();
        for (final BackupScheduleVO backupSchedule : backupScheduleDao.listAll()) {
            scheduleNextBackupJob(backupSchedule);
        }
        final TimerTask backupPollTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
            try {
                poll(new Date());
            } catch (final Throwable t) {
                LOG.warn("Catch throwable in backup scheduler ", t);
            }
            }
        };

        backupTimer = new Timer("BackupPollTask");
        backupTimer.schedule(backupPollTask, BackupSyncPollingInterval.value() * 1000L, BackupSyncPollingInterval.value() * 1000L);
        return true;
    }

    private VMInstanceVO findVmById(final Long vmId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException(String.format("Can't find any VM with ID: [%s].", vmId));
        }
        return vm;
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
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Backup sync background task is running...");
                }
                for (final DataCenter dataCenter : dataCenterDao.listAllZones()) {
                    if (dataCenter == null || isDisabled(dataCenter.getId())) {
                        LOG.debug(String.format("Backup Sync Task is not enabled in zone [%s]. Skipping this zone!", dataCenter == null ? "NULL Zone!" : dataCenter.getId()));
                        continue;
                    }

                    final BackupProvider backupProvider = getBackupProvider(dataCenter.getId());
                    if (backupProvider == null) {
                        LOG.warn("Backup provider not available or configured for zone ID " + dataCenter.getId());
                        continue;
                    }

                    List<VMInstanceVO> vms = vmInstanceDao.listByZoneWithBackups(dataCenter.getId(), null);
                    if (vms == null || vms.isEmpty()) {
                        LOG.debug(String.format("Can't find any VM to sync backups in zone [id: %s].", dataCenter.getId()));
                        continue;
                    }

                    final Map<VirtualMachine, Backup.Metric> metrics = backupProvider.getBackupMetrics(dataCenter.getId(), new ArrayList<>(vms));
                    try {
                        for (final VirtualMachine vm : metrics.keySet()) {
                            final Backup.Metric metric = metrics.get(vm);
                            if (metric != null) {
                                // Sync out-of-band backups
                                backupProvider.syncBackups(vm, metric);
                                // Emit a usage event, update usage metric for the VM by the usage server
                                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_USAGE_METRIC, vm.getAccountId(),
                                        vm.getDataCenterId(), vm.getId(), "Backup-" + vm.getHostName() + "-" + vm.getUuid(),
                                        vm.getBackupOfferingId(), null, metric.getBackupSize(), metric.getDataSize(),
                                        Backup.class.getSimpleName(), vm.getUuid());
                            }
                        }
                    } catch (final Throwable e) {
                        LOG.error(String.format("Failed to sync backup usage metrics and out-of-band backups due to: [%s].", e.getMessage()), e);
                    }
                }
            } catch (final Throwable t) {
                LOG.error(String.format("Error trying to run backup-sync background task due to: [%s].", t.getMessage()), t);
            }
        }

        @Override
        public Long getDelay() {
            return BackupSyncPollingInterval.value() * 1000L;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_EDIT, eventDescription = "updating backup offering")
    public BackupOffering updateBackupOffering(UpdateBackupOfferingCmd updateBackupOfferingCmd) {
        Long id = updateBackupOfferingCmd.getId();
        String name = updateBackupOfferingCmd.getName();
        String description = updateBackupOfferingCmd.getDescription();
        Boolean allowUserDrivenBackups = updateBackupOfferingCmd.getAllowUserDrivenBackups();

        BackupOfferingVO backupOfferingVO = backupOfferingDao.findById(id);
        if (backupOfferingVO == null) {
            throw new InvalidParameterValueException(String.format("Unable to find Backup Offering with id: [%s].", id));
        }
        LOG.debug(String.format("Trying to update Backup Offering %s to %s.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backupOfferingVO,"uuid", "name",
                        "description", "userDrivenBackupAllowed"), ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this,"name", "description", "allowUserDrivenBackups")));

        BackupOfferingVO offering = backupOfferingDao.createForUpdate(id);
        List<String> fields = new ArrayList<>();
        if (name != null) {
            offering.setName(name);
            fields.add("name: " + name);
        }

        if (description != null) {
            offering.setDescription(description);
            fields.add("description: " + description);
        }


        if (allowUserDrivenBackups != null){
            offering.setUserDrivenBackupAllowed(allowUserDrivenBackups);
            fields.add("allowUserDrivenBackups: " + allowUserDrivenBackups);
        }

        if (!backupOfferingDao.update(id, offering)) {
            LOG.warn(String.format("Couldn't update Backup offering [id: %s] with [%s].", id, String.join(", ", fields)));
        }

        BackupOfferingVO response = backupOfferingDao.findById(id);
        CallContext.current().setEventDetails(String.format("Backup Offering updated [%s].",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(response, "id", "name", "description", "userDrivenBackupAllowed", "externalId")));
        return response;
    }

}
