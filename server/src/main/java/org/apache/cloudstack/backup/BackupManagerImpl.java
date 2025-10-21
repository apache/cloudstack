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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.command.admin.backup.DeleteBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProviderOfferingsCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProvidersCmd;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.vm.CreateVMFromBackupCmdByAdmin;
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
import org.apache.cloudstack.api.command.user.backup.repository.AddBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.DeleteBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.ListBackupRepositoriesCmd;
import org.apache.cloudstack.api.command.user.backup.repository.UpdateBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.vm.CreateVMFromBackupCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupScheduleDao;
import org.apache.cloudstack.context.CallContext;
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
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.amazonaws.util.CollectionUtils;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project;
import com.cloud.serializer.GsonHelper;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
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
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDiskInfo;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class BackupManagerImpl extends ManagerBase implements BackupManager {

    @Inject
    private BackupDao backupDao;
    @Inject
    private BackupDetailsDao backupDetailsDao;
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
    private DomainManager domainManager;
    @Inject
    private AccountDao accountDao;
    @Inject
    private DomainDao domainDao;
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
    private UserVmDao userVmDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private VMTemplateDao vmTemplateDao;
    @Inject
    private UserVmJoinDao userVmJoinDao;
    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private NetworkService networkService;
    @Inject
    private ApiDispatcher apiDispatcher;
    @Inject
    private AsyncJobManager asyncJobManager;
    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private VolumeApiService volumeApiService;
    @Inject
    private ResourceLimitService resourceLimitMgr;
    @Inject
    private AlertManager alertManager;
    @Inject
    private GuestOSDao _guestOSDao;

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
        validateBackupForZone(zoneId);
        final Account account = CallContext.current().getCallingAccount();
        if (!accountService.isRootAdmin(account.getId())) {
            throw new PermissionDeniedException("Parameter external can only be specified by a Root Admin, permission denied");
        }
        final BackupProvider backupProvider = getBackupProvider(zoneId);
        logger.debug("Listing external backup offerings for the backup provider configured for zone {}", dataCenterDao.findById(zoneId));
        return backupProvider.listBackupOfferings(zoneId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_IMPORT_OFFERING, eventDescription = "importing backup offering", async = true)
    public BackupOffering importBackupOffering(final ImportBackupOfferingCmd cmd) {
        validateBackupForZone(cmd.getZoneId());
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
        logger.debug("Successfully created backup offering " + cmd.getName() + " mapped to backup provider offering " + cmd.getExternalId());
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
        CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        if (Account.Type.NORMAL == caller.getType()) {
            sb.and("user_backups_allowed", sb.entity().isUserDrivenBackupAllowed(), SearchCriteria.Op.EQ);
        }
        final SearchCriteria<BackupOfferingVO> sc = sb.create();

        if (zoneId != null) {
            sc.setParameters("zone_id", zoneId);
        }

        if (keyword != null) {
            sc.setParameters("name", "%" + keyword + "%");
        }
        if (Account.Type.NORMAL == caller.getType()) {
            sc.setParameters("user_backups_allowed", true);
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

        if (backupDao.listByOfferingId(offering.getId()).size() > 0) {
            throw new CloudRuntimeException("Backup Offering cannot be removed as it has backups associated with it.");
        }

        if (vmInstanceDao.listByZoneAndBackupOffering(offering.getZoneId(), offering.getId()).size() > 0) {
            throw new CloudRuntimeException("Backup offering is assigned to VMs, remove the assignment(s) in order to remove the offering.");
        }

        validateBackupForZone(offering.getZoneId());
        return backupOfferingDao.remove(offering.getId());
    }

    private String getNicDetailsAsJson(final Long vmId) {
        final List<UserVmJoinVO> userVmJoinVOs = userVmJoinDao.searchByIds(vmId);
        if (userVmJoinVOs != null && !userVmJoinVOs.isEmpty()) {
            final List<Map<String, String>> nics = new ArrayList<>();
            final Set<String> seen = new HashSet<>();
            for (UserVmJoinVO userVmJoinVO : userVmJoinVOs) {
                Map<String, String> nicInfo = new HashMap<>();
                String key = userVmJoinVO.getNetworkUuid();
                if (seen.add(key)) {
                    nicInfo.put(ApiConstants.NETWORK_ID, userVmJoinVO.getNetworkUuid());
                    nicInfo.put(ApiConstants.IP_ADDRESS, userVmJoinVO.getIpAddress());
                    nicInfo.put(ApiConstants.IP6_ADDRESS, userVmJoinVO.getIp6Address());
                    nicInfo.put(ApiConstants.MAC_ADDRESS, userVmJoinVO.getMacAddress());
                    nics.add(nicInfo);
                }
            }
            if (!nics.isEmpty()) {
                return new Gson().toJson(nics);
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getBackupDetailsFromVM(VirtualMachine vm) {
        HashMap<String, String> details = new HashMap<>();

        ServiceOffering serviceOffering = serviceOfferingDao.findById(vm.getServiceOfferingId());
        details.put(ApiConstants.SERVICE_OFFERING_ID, serviceOffering.getUuid());
        VirtualMachineTemplate template = vmTemplateDao.findById(vm.getTemplateId());
        if (template != null) {
            long guestOSId = template.getGuestOSId();
            details.put(ApiConstants.TEMPLATE_ID, template.getUuid());
            GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
            if (guestOS != null) {
                details.put(ApiConstants.OS_TYPE_ID, guestOS.getUuid());
                details.put(ApiConstants.OS_NAME, guestOS.getDisplayName());
            }
        }

        List<VMInstanceDetailVO> vmDetails = vmInstanceDetailsDao.listDetails(vm.getId());
        HashMap<String, String> settings = new HashMap<>();
        for (VMInstanceDetailVO detail : vmDetails) {
            settings.put(detail.getName(), detail.getValue());
        }
        if (!settings.isEmpty()) {
            details.put(ApiConstants.VM_SETTINGS, new Gson().toJson(settings));
        }

        String nicsJson = getNicDetailsAsJson(vm.getId());
        if (nicsJson != null) {
            details.put(ApiConstants.NICS, nicsJson);
        }
        return details;
    }

    @Override
    public String getBackupNameFromVM(VirtualMachine vm) {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, new Date());
        return (vm.getHostName() + '-' + displayTime);
    }

    @Override
    public String createVolumeInfoFromVolumes(List<Volume> vmVolumes) {
        List<Backup.VolumeInfo> list = new ArrayList<>();
        vmVolumes.sort(Comparator.comparing(Volume::getDeviceId));
        for (Volume vol : vmVolumes) {
            DiskOfferingVO diskOffering = diskOfferingDao.findById(vol.getDiskOfferingId());
            Backup.VolumeInfo volumeInfo = new Backup.VolumeInfo(vol.getUuid(), vol.getPath(), vol.getVolumeType(), vol.getSize(),
                vol.getDeviceId(), diskOffering.getUuid(), vol.getMinIops(), vol.getMaxIops());
            list.add(volumeInfo);
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

        validateBackupForZone(vm.getDataCenterId());

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
                    vm.setBackupVolumes(createVolumeInfoFromVolumes(new ArrayList<>(volumeDao.findByInstance(vmId))));

                    if (!backupProvider.assignVMToBackupOffering(vm, offering)) {
                        throw new CloudRuntimeException("Failed to assign the VM to the backup offering, please try removing the assignment and try again.");
                    }

                    if (!vmInstanceDao.update(vmId, vm)) {
                        backupProvider.removeVMFromBackupOffering(vm);
                        throw new CloudRuntimeException("Failed to update VM assignment to the backup offering in the DB, please try again.");
                    }

                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_ASSIGN, vm.getAccountId(), vm.getDataCenterId(), vmId,
                            "Backup-" + vm.getHostName() + "-" + vm.getUuid(), vm.getBackupOfferingId(), null, null, Backup.class.getSimpleName(), vm.getUuid());
                    logger.debug(String.format("VM [%s] successfully added to Backup Offering [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(vm,
                            "uuid", "instanceName", "backupOfferingId", "backupVolumes"), ReflectionToStringBuilderUtils.reflectOnlySelectedFields(offering,
                                    "uuid", "name", "externalId", "provider")));
                } catch (Exception e) {
                    String msg = String.format("Failed to assign VM [%s] to the Backup Offering [%s], using provider [name: %s, class: %s], due to: [%s].",
                            ReflectionToStringBuilderUtils.reflectOnlySelectedFields(vm, "uuid", "instanceName", "backupOfferingId", "backupVolumes"),
                            ReflectionToStringBuilderUtils.reflectOnlySelectedFields(offering, "uuid", "name", "externalId", "provider"),
                            backupProvider.getName(), backupProvider.getClass().getSimpleName(), e.getMessage());
                    logger.error(msg);
                    logger.debug(msg, e);
                    return null;
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

        validateBackupForZone(vm.getDataCenterId());
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
            result = backupProvider.removeVMFromBackupOffering(vm);
            Long backupOfferingId = vm.getBackupOfferingId();
            vm.setBackupOfferingId(null);
            vm.setBackupVolumes(null);
            vm.setBackupExternalId(null);
            if (result && backupProvider.willDeleteBackupsOnOfferingRemoval()) {
                final List<Backup> backups = backupDao.listByVmId(null, vm.getId());
                for (final Backup backup : backups) {
                    backupDao.remove(backup.getId());
                }
            }
            if ((result || forced) && vmInstanceDao.update(vm.getId(), vm)) {
                final List<Backup> backups = backupDao.listByVmId(null, vm.getId());
                if (backups.size() == 0) {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_REMOVED_AND_BACKUPS_DELETED, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                            "Backup-" + vm.getHostName() + "-" + vm.getUuid(), backupOfferingId, null, null,
                            Backup.class.getSimpleName(), vm.getUuid());
                }
                final List<BackupScheduleVO> backupSchedules = backupScheduleDao.listByVM(vm.getId());
                for(BackupSchedule backupSchedule: backupSchedules) {
                    backupScheduleDao.remove(backupSchedule.getId());
                }
                result = true;
            }
        } catch (Exception e) {
            logger.error("Exception caught when trying to remove VM [{}] from the backup offering [{}] due to: [{}].", vm, offering, e.getMessage(), e);
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
        validateBackupForZone(vm.getDataCenterId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getBackupOfferingId() == null) {
            throw new CloudRuntimeException("Cannot configure backup schedule for the VM without having any backup offering");
        }

        final BackupOffering offering = backupOfferingDao.findById(vm.getBackupOfferingId());
        if (offering == null || !offering.isUserDrivenBackupAllowed()) {
            throw new CloudRuntimeException("The selected backup offering does not allow user-defined backup schedule");
        }

        final int maxBackups = validateAndGetDefaultBackupRetentionIfRequired(cmd.getMaxBackups(), offering, vm);

        if (!"nas".equals(offering.getProvider()) && cmd.getQuiesceVM() != null) {
            throw new InvalidParameterValueException("Quiesce VM option is supported only for NAS backup provider");
        }

        final String timezoneId = timeZone.getID();
        if (!timezoneId.equals(cmd.getTimezone())) {
            logger.warn("Using timezone: " + timezoneId + " for running this snapshot policy as an equivalent of " + cmd.getTimezone());
        }

        Date nextDateTime = null;
        try {
            nextDateTime = DateUtil.getNextRunTime(intervalType, cmd.getSchedule(), timezoneId, null);
        } catch (Exception e) {
            throw new InvalidParameterValueException("Invalid schedule: " + cmd.getSchedule() + " for interval type: " + cmd.getIntervalType());
        }

        final BackupScheduleVO schedule = backupScheduleDao.findByVMAndIntervalType(vmId, intervalType);
        if (schedule == null) {
            return backupScheduleDao.persist(new BackupScheduleVO(vmId, intervalType, scheduleString, timezoneId, nextDateTime, maxBackups, cmd.getQuiesceVM(), vm.getAccountId(), vm.getDomainId()));
        }

        schedule.setScheduleType((short) intervalType.ordinal());
        schedule.setSchedule(scheduleString);
        schedule.setTimezone(timezoneId);
        schedule.setScheduledTimestamp(nextDateTime);
        schedule.setMaxBackups(maxBackups);
        schedule.setQuiesceVM(cmd.getQuiesceVM());
        backupScheduleDao.update(schedule.getId(), schedule);
        return backupScheduleDao.findById(schedule.getId());
    }

    /**
     * Validates the provided backup retention value and returns 0 as the default value if required.
     *
     * @param maxBackups The number of backups to retain, can be null
     * @param offering The backup offering
     * @param vm The VM associated with the backup schedule
     * @return The validated number of backups to retain. If maxBackups is null, returns 0 as the default value
     * @throws InvalidParameterValueException if the backup offering's provider is Veeam, or maxBackups is less than 0 or greater than the account and domain backup limits
     */
    protected int validateAndGetDefaultBackupRetentionIfRequired(Integer maxBackups, BackupOffering offering, VirtualMachine vm) {
        if (maxBackups == null) {
            return 0;
        }
        if ("veeam".equals(offering.getProvider())) {
            throw new InvalidParameterValueException("The maximum amount of backups to retain cannot be directly configured via Apache CloudStack for Veeam. " +
                    "Retention is managed directly in Veeam based on the settings specified when creating the backup job.");
        }
        if (maxBackups < 0) {
            throw new InvalidParameterValueException("maxbackups value for backup schedule must be a non-negative integer.");
        }

        Account owner = accountManager.getAccount(vm.getAccountId());
        long accountLimit = resourceLimitMgr.findCorrectResourceLimitForAccount(owner, Resource.ResourceType.backup, null);
        boolean exceededAccountLimit = accountLimit != -1 && maxBackups > accountLimit;

        long domainLimit = resourceLimitMgr.findCorrectResourceLimitForDomain(domainManager.getDomain(owner.getDomainId()), Resource.ResourceType.backup, null);
        boolean exceededDomainLimit = domainLimit != -1 && maxBackups > domainLimit;

        if (!accountManager.isRootAdmin(owner.getId()) && (exceededAccountLimit || exceededDomainLimit)) {
            throw new InvalidParameterValueException(
                    String.format("'maxbackups' should not exceed the domain/%s backup limit.", owner.getType() == Account.Type.PROJECT ? "project" : "account")
            );
        }

        return maxBackups;
    }

    public List<BackupSchedule> listBackupSchedules(ListBackupScheduleCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long id = cmd.getId();
        Long vmId = cmd.getVmId();
        List<Long> permittedAccounts = new ArrayList<>();
        Long domainId = null;
        Boolean isRecursive = null;
        String keyword = cmd.getKeyword();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = null;

        if (vmId != null) {
            final VMInstanceVO vm = findVmById(vmId);
            validateBackupForZone(vm.getDataCenterId());
            accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);
        }

        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject =
                new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, true, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(BackupScheduleVO.class, "id", false, null, null);
        SearchBuilder<BackupScheduleVO> searchBuilder = backupScheduleDao.createSearchBuilder();

        accountManager.buildACLSearchBuilder(searchBuilder, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        searchBuilder.and("id", searchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        if (vmId != null) {
            searchBuilder.and("vmId", searchBuilder.entity().getVmId(), SearchCriteria.Op.EQ);
        }
        if (keyword != null && !keyword.isEmpty()) {
            SearchBuilder<VMInstanceVO> vmSearch = vmInstanceDao.createSearchBuilder();
            vmSearch.and("hostName", vmSearch.entity().getHostName(), SearchCriteria.Op.LIKE);
            searchBuilder.join("vmJoin", vmSearch, searchBuilder.entity().getVmId(), vmSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<BackupScheduleVO> sc = searchBuilder.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.setParameters("id", id);
        }
        if (vmId != null) {
            sc.setParameters("vmId", vmId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sc.setJoinParameters("vmJoin", "hostName", "%" + keyword + "%");
        }

        Pair<List<BackupScheduleVO>, Integer> result = backupScheduleDao.searchAndCount(sc, searchFilter);
        return new ArrayList<>(result.first());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_SCHEDULE_DELETE, eventDescription = "deleting VM backup schedule")
    public boolean deleteBackupSchedule(DeleteBackupScheduleCmd cmd) {
        Long vmId = cmd.getVmId();
        Long id = cmd.getId();
        if (ObjectUtils.allNull(vmId, id)) {
            throw new InvalidParameterValueException("Either instance ID or ID of backup schedule needs to be specified.");
        }

        if (Objects.nonNull(id)) {
            BackupSchedule schedule = backupScheduleDao.findById(id);
            if (schedule == null) {
                throw new InvalidParameterValueException("Could not find the requested backup schedule.");
            }
            checkCallerAccessToBackupScheduleVm(schedule.getVmId());
            return backupScheduleDao.remove(schedule.getId());
        }

        checkCallerAccessToBackupScheduleVm(vmId);
        return deleteAllVmBackupSchedules(vmId);
    }

    /**
     * Checks if the backup framework is enabled for the zone in which the VM with specified ID is allocated and
     * if the caller has access to the VM.
     *
     * @param vmId The ID of the virtual machine to check access for
     * @throws PermissionDeniedException if the caller doesn't have access to the VM
     * @throws CloudRuntimeException if the backup framework is disabled
     */
    protected void checkCallerAccessToBackupScheduleVm(long vmId) {
        VMInstanceVO vm = findVmById(vmId);
        validateBackupForZone(vm.getDataCenterId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);
    }

    /**
     * Deletes all backup schedules associated with a specific VM.
     *
     * @param vmId The ID of the virtual machine whose backup schedules should be deleted
     * @return true if all backup schedules were successfully deleted, false if any deletion failed
     */
    protected boolean deleteAllVmBackupSchedules(long vmId) {
        List<BackupScheduleVO> vmBackupSchedules = backupScheduleDao.listByVM(vmId);
        boolean success = true;
        for (BackupScheduleVO vmBackupSchedule : vmBackupSchedules) {
            success = success && backupScheduleDao.remove(vmBackupSchedule.getId());
        }
        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_CREATE, eventDescription = "creating VM backup", async = true)
    public boolean createBackup(CreateBackupCmd cmd, Object job) throws ResourceAllocationException {
        Long vmId = cmd.getVmId();

        final VMInstanceVO vm = findVmById(vmId);
        validateBackupForZone(vm.getDataCenterId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getBackupOfferingId() == null) {
            throw new CloudRuntimeException("VM has not backup offering configured, cannot create backup before assigning it to a backup offering");
        }

        final BackupOffering offering = backupOfferingDao.findById(vm.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("VM backup offering not found");
        }

        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        if (backupProvider == null) {
            throw new CloudRuntimeException("VM backup provider not found for the offering");
        }

        if (!offering.isUserDrivenBackupAllowed()) {
            throw new CloudRuntimeException("The assigned backup offering does not allow ad-hoc user backup");
        }

        if (!"nas".equals(offering.getProvider()) && cmd.getQuiesceVM() != null) {
            throw new InvalidParameterValueException("Quiesce VM option is supported only for NAS backup provider");
        }

        Long backupScheduleId = getBackupScheduleId(job);
        boolean isScheduledBackup = backupScheduleId != null;
        Account owner = accountManager.getAccount(vm.getAccountId());
        try {
            resourceLimitMgr.checkResourceLimit(owner, Resource.ResourceType.backup);
        } catch (ResourceAllocationException e) {
            if (isScheduledBackup) {
                sendExceededBackupLimitAlert(owner.getUuid(), Resource.ResourceType.backup);
            }
            throw e;
        }

        Long backupSize = 0L;
        for (final Volume volume: volumeDao.findByInstance(vmId)) {
            if (Volume.State.Ready.equals(volume.getState())) {
                Long volumeSize = volumeApiService.getVolumePhysicalSize(volume.getFormat(), volume.getPath(), volume.getChainInfo());
                if (volumeSize == null) {
                    volumeSize = volume.getSize();
                }
                backupSize += volumeSize;
            }
        }
        try {
            resourceLimitMgr.checkResourceLimit(owner, Resource.ResourceType.backup_storage, backupSize);
        } catch (ResourceAllocationException e) {
            if (isScheduledBackup) {
                sendExceededBackupLimitAlert(owner.getUuid(), Resource.ResourceType.backup_storage);
            }
            throw e;
        }

        ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, vm.getAccountId(),
                EventTypes.EVENT_VM_BACKUP_CREATE, "creating backup for VM ID:" + vm.getUuid(),
                vmId, ApiCommandResourceType.VirtualMachine.toString(),
                true, 0);

        Pair<Boolean, Backup> result = backupProvider.takeBackup(vm, cmd.getQuiesceVM());
        if (!result.first()) {
            throw new CloudRuntimeException("Failed to create VM backup");
        }
        Backup backup = result.second();
        if (backup != null) {
            BackupVO vmBackup = backupDao.findById(result.second().getId());
            vmBackup.setBackupScheduleId(backupScheduleId);
            if (cmd.getName() != null) {
                vmBackup.setName(cmd.getName());
            }
            vmBackup.setDescription(cmd.getDescription());
            backupDao.update(vmBackup.getId(), vmBackup);
            resourceLimitMgr.incrementResourceCount(vm.getAccountId(), Resource.ResourceType.backup);
            resourceLimitMgr.incrementResourceCount(vm.getAccountId(), Resource.ResourceType.backup_storage, backup.getSize());
        }
        if (isScheduledBackup) {
            deleteOldestBackupFromScheduleIfRequired(vmId, backupScheduleId);
        }
        return true;
    }

    /**
     * Sends an alert when the backup limit has been exceeded for a given account.
     *
     * @param ownerUuid The UUID of the account owner that exceeded the limit
     * @param resourceType The type of resource limit that was exceeded (either {@link Resource.ResourceType#backup} or {@link Resource.ResourceType#backup_storage})
     *
     */
    protected void sendExceededBackupLimitAlert(String ownerUuid, Resource.ResourceType resourceType) {
        String message = String.format("Failed to create backup: backup %s limit exceeded for account with ID: %s.",
                resourceType == Resource.ResourceType.backup ? "resource" : "storage space resource" , ownerUuid);
        logger.warn(message);
        alertManager.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L,
                message, message + " Please, use the 'updateResourceLimit' API to increase the backup limit.");
    }

    /**
     * Gets the backup schedule ID from the async job's payload.
     *
     * @param job The asynchronous job associated with the creation of the backup
     * @return The backup schedule ID. Returns null if the backup has been manually created
     */
    protected Long getBackupScheduleId(Object job) {
        if (!(job instanceof AsyncJobVO)) {
            return null;
        }

        AsyncJobVO asyncJob = (AsyncJobVO) job;
        logger.debug("Trying to retrieve [{}] parameter from the job [ID: {}] parameters.", ApiConstants.SCHEDULE_ID, asyncJob.getId());
        String jobParamsRaw = asyncJob.getCmdInfo();

        if (!jobParamsRaw.contains(ApiConstants.SCHEDULE_ID)) {
            logger.info("Job [ID: {}] parameters do not include the [{}] parameter. Thus, the current backup is a manual backup.", asyncJob.getId(), ApiConstants.SCHEDULE_ID);
            return null;
        }

        TypeToken<Map<String, String>> jobParamsType = new TypeToken<>(){};
        Map<String, String> jobParams = GsonHelper.getGson().fromJson(jobParamsRaw, jobParamsType.getType());
        long backupScheduleId = NumberUtils.toLong(jobParams.get(ApiConstants.SCHEDULE_ID));
        logger.info("Job [ID: {}] parameters include the [{}] parameter, whose value is equal to [{}]. Thus, the current backup is a scheduled backup.", asyncJob.getId(), ApiConstants.SCHEDULE_ID, backupScheduleId);
        return backupScheduleId == 0L ? null : backupScheduleId;
    }

    /**
     * Deletes the oldest backups from the schedule. If the backup schedule is not active, the schedule's retention is equal to 0,
     * or the number of backups to be deleted is lower than one, then no backups are deleted.
     *
     * @param vmId The ID of the VM associated with the backups
     * @param backupScheduleId Backup schedule ID of the backups
     */
    protected void deleteOldestBackupFromScheduleIfRequired(Long vmId, long backupScheduleId) {
        BackupScheduleVO backupScheduleVO = backupScheduleDao.findById(backupScheduleId);
        if (backupScheduleVO == null || backupScheduleVO.getMaxBackups() == 0) {
            logger.info("The schedule does not have a retention specified and, hence, not deleting any backups from it.", vmId);
            return;
        }

        logger.debug("Checking if it is required to delete the oldest backups from the schedule with ID [{}], to meet its retention requirement of [{}] backups.", backupScheduleId, backupScheduleVO.getMaxBackups());
        List<BackupVO> backups = backupDao.listBySchedule(backupScheduleId);
        int amountOfBackupsToDelete = backups.size() - backupScheduleVO.getMaxBackups();
        if (amountOfBackupsToDelete > 0) {
            deleteExcessBackups(backups, amountOfBackupsToDelete, backupScheduleId);
        } else {
            logger.debug("Not required to delete any backups from the schedule [ID: {}]: [backups size: {}] and [retention: {}].", backupScheduleId, backups.size(), backupScheduleVO.getMaxBackups());
        }
    }

    /**
     * Deletes a certain number of backups associated with a schedule.
     *
     * @param backups List of backups associated with a schedule
     * @param amountOfBackupsToDelete Number of backups to be deleted from the list of backups
     * @param backupScheduleId ID of the backup schedule associated with the backups
     */
    protected void deleteExcessBackups(List<BackupVO> backups, int amountOfBackupsToDelete, long backupScheduleId) {
        logger.debug("Deleting the [{}] oldest backups from the schedule [ID: {}].", amountOfBackupsToDelete, backupScheduleId);

        for (int i = 0; i < amountOfBackupsToDelete; i++) {
            BackupVO backup = backups.get(i);
            if (deleteBackup(backup.getId(), false)) {
                String eventDescription = String.format("Successfully deleted backup for VM [ID: %s], suiting the retention specified in the backup schedule [ID: %s]", backup.getVmId(), backupScheduleId);
                logger.info(eventDescription);
                ActionEventUtils.onCompletedActionEvent(
                        User.UID_SYSTEM, backup.getAccountId(), EventVO.LEVEL_INFO,
                        EventTypes.EVENT_VM_BACKUP_DELETE, eventDescription, backup.getId(), ApiCommandResourceType.Backup.toString(), 0
                );
            }
        }
    }

    @Override
    public Pair<List<Backup>, Integer> listBackups(final ListBackupsCmd cmd) {
        final Long id = cmd.getId();
        final Long vmId = cmd.getVmId();
        final String name = cmd.getName();
        final Long zoneId = cmd.getZoneId();
        final Long backupOfferingId = cmd.getBackupOfferingId();
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
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("backupOfferingId", sb.entity().getBackupOfferingId(), SearchCriteria.Op.EQ);

        if (keyword != null) {
            sb.or().op("keywordName", sb.entity().getName(), SearchCriteria.Op.LIKE);
            SearchBuilder<VMInstanceVO> vmSearch = vmInstanceDao.createSearchBuilder();
            sb.join("vmSearch", vmSearch, sb.entity().getVmId(), vmSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            sb.or("vmSearch", "keywordVmName", vmSearch.entity().getHostName(), SearchCriteria.Op.LIKE);
            sb.cp();
        }

        SearchCriteria<BackupVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (vmId != null) {
            sc.setParameters("vmId", vmId);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }

        if (backupOfferingId != null) {
            sc.setParameters("backupOfferingId", backupOfferingId);
        }

        if (keyword != null) {
            String keywordMatch = "%" + keyword + "%";
            sc.setParameters("keywordName", keywordMatch);
            sc.setParameters("keywordVmName", keywordMatch);
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
            logger.error(String.format("Failed to import VM [vmInternalName: %s] from backup restoration [%s] with hypervisor [type: %s] due to: [%s].", vmInternalName,
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backup, "id", "uuid", "vmId", "externalId", "backupType"), hypervisorType, e.getMessage()), e);
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_BACKUP_RESTORE,
                    String.format("Failed to import VM %s from backup %s with hypervisor [type: %s]", vmInternalName, backup.getUuid(), hypervisorType),
                    vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),0);
            throw new CloudRuntimeException("Error during vm backup restoration and import: " + e.getMessage());
        }
        if (vm == null) {
            String message = String.format("Failed to import restored VM %s  with hypervisor type %s using backup of VM ID %s",
                    vmInternalName, hypervisorType, backup.getVmId());
            logger.error(message);
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_BACKUP_RESTORE,
                    message, vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),0);
        } else {
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_BACKUP_RESTORE,
                    String.format("Restored VM %s from backup %s", vm.getUuid(), backup.getUuid()),
                    vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),0);
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
        if (backup.getStatus() != Backup.Status.BackedUp) {
            throw new CloudRuntimeException("Backup should be in BackedUp state");
        }
        validateBackupForZone(backup.getZoneId());

        final VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm == null || VirtualMachine.State.Expunging.equals(vm.getState())) {
            throw new CloudRuntimeException("The Instance from which the backup was taken could not be found.");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getRemoved() == null && !vm.getState().equals(VirtualMachine.State.Stopped) &&
                !vm.getState().equals(VirtualMachine.State.Destroyed)) {
            throw new CloudRuntimeException("Existing VM should be stopped before being restored from backup");
        }

        // This is done to handle historic backups if any with Veeam / Networker plugins
        List<Backup.VolumeInfo> backupVolumes = CollectionUtils.isNullOrEmpty(backup.getBackedUpVolumes()) ?
                vm.getBackupVolumeList() : backup.getBackedUpVolumes();
        List<VolumeVO> vmVolumes = volumeDao.findByInstance(vm.getId());
        if (vmVolumes.size() != backupVolumes.size()) {
            throw new CloudRuntimeException("Unable to restore VM with the current backup as the backup has different number of disks as the VM");
        }

        BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(vm.getBackupOfferingId());
        String errorMessage = "Failed to find backup offering of the VM backup.";
        if (offering == null) {
            logger.warn(errorMessage);
        }
        logger.debug("Attempting to get backup offering from VM backup");
        offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException(errorMessage);
        }
        String backupDetailsInMessage = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backup, "uuid", "externalId", "vmId", "name");
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
            ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventTypes.EVENT_VM_BACKUP_RESTORE,
                    String.format("Restoring VM %s from backup %s", vm.getUuid(), backup.getUuid()),
                    vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),
                    true, 0);

            final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
            if (!backupProvider.restoreVMFromBackup(vm, backup)) {
                ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_BACKUP_RESTORE,
                        String.format("Failed to restore VM %s from backup %s", vm.getInstanceName(), backup.getUuid()),
                        vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),0);
                throw new CloudRuntimeException("Error restoring VM from backup with uuid " + backup.getUuid());
            }
        // The restore process is executed by a backup provider outside of ACS, I am using the catch-all (Exception) to
        // ensure that no provider-side exception is missed. Therefore, we have a proper handling of exceptions, and rollbacks if needed.
        } catch (Exception e) {
            logger.error(String.format("Failed to restore backup [%s] due to: [%s].", backupDetailsInMessage, e.getMessage()), e);
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
        logger.debug(String.format("Trying to update state of VM [%s] with event [%s].", vm, event));
        Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<VMInstanceVO>) status -> {
            try {
                if (!virtualMachineManager.stateTransitTo(vm, event, vm.getHostId())) {
                    throw new CloudRuntimeException(String.format("Unable to change state of VM [%s] to [%s].", vm, next));
                }
            } catch (NoTransitionException e) {
                String errMsg = String.format("Failed to update state of VM [%s] with event [%s] due to [%s].", vm, event, e.getMessage());
                logger.error(errMsg, e);
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
        logger.debug(String.format("Trying to update state of volume [%s] with event [%s].", volume, event));
        try {
            if (!volumeApiService.stateTransitTo(volume, event)) {
                throw new CloudRuntimeException(String.format("Unable to change state of volume [%s] to [%s].", volume, next));
            }
        } catch (NoTransitionException e) {
            String errMsg = String.format("Failed to update state of volume [%s] with event [%s] due to [%s].", volume, event, e.getMessage());
            logger.error(errMsg, e);
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
    public void checkVmDisksSizeAgainstBackup(List<VmDiskInfo> vmDiskInfoList, Backup backup) {
        List<VmDiskInfo> vmDiskInfoListFromBackup = getDataDiskInfoListFromBackup(backup);
        int index = 0;
        if (vmDiskInfoList.size() != vmDiskInfoListFromBackup.size()) {
            throw new InvalidParameterValueException("Unable to create Instance from Backup " +
                    "as the backup has a different number of disks than the Instance.");
        }
        for (VmDiskInfo vmDiskInfo : vmDiskInfoList) {
            if (index < vmDiskInfoListFromBackup.size()) {
                if (vmDiskInfo.getSize() < vmDiskInfoListFromBackup.get(index).getSize()) {
                    throw new InvalidParameterValueException(
                            String.format("Instance volume size %d[GiB] cannot be less than the backed-up volume size %d[GiB].",
                            vmDiskInfo.getSize(), vmDiskInfoListFromBackup.get(index).getSize()));
                }
            }
            index++;
        }
    }

    @Override
    public VmDiskInfo getRootDiskInfoFromBackup(Backup backup) {
        List<Backup.VolumeInfo> volumes = backup.getBackedUpVolumes();
        VmDiskInfo rootDiskOffering = null;
        if (volumes == null || volumes.isEmpty()) {
            throw new CloudRuntimeException("Failed to get backed-up volumes info from backup");
        }
        for (Backup.VolumeInfo volume : volumes) {
            if (volume.getType() == Volume.Type.ROOT) {
                DiskOfferingVO diskOffering = diskOfferingDao.findByUuid(volume.getDiskOfferingId());
                if (diskOffering == null) {
                    throw new CloudRuntimeException(String.format("Unable to find the root disk offering with uuid (%s) " +
                            "stored in backup. Please specify a valid root disk offering id while creating the instance",
                            volume.getDiskOfferingId()));
                }
                Long size = volume.getSize() / (1024 * 1024 * 1024);
                rootDiskOffering = new VmDiskInfo(diskOffering, size, volume.getMinIops(), volume.getMaxIops());
            }
        }
        if (rootDiskOffering == null) {
            throw new CloudRuntimeException("Failed to get the root disk in backed-up volumes info from backup");
        }
        return rootDiskOffering;
    }

    @Override
    public List<VmDiskInfo> getDataDiskInfoListFromBackup(Backup backup) {
        List<VmDiskInfo> vmDiskInfoList = new ArrayList<>();
        List<Backup.VolumeInfo> volumes = backup.getBackedUpVolumes();
        if (volumes == null || volumes.isEmpty()) {
            throw new CloudRuntimeException("Failed to get backed-up Volumes info from backup");
        }
        for (Backup.VolumeInfo volume : volumes) {
            if (volume.getType() == Volume.Type.DATADISK) {
                DiskOfferingVO diskOffering = diskOfferingDao.findByUuid(volume.getDiskOfferingId());
                if (diskOffering == null || diskOffering.getState().equals(DiskOffering.State.Inactive)) {
                    throw new CloudRuntimeException("Unable to find the disk offering with uuid (" + volume.getDiskOfferingId() + ") stored in backup. " +
                            "Please specify a valid disk offering id while creating the instance");
                }
                Long size = volume.getSize() / (1024 * 1024 * 1024);
                vmDiskInfoList.add(new VmDiskInfo(diskOffering, size, volume.getMinIops(), volume.getMaxIops(), volume.getDeviceId()));
            }
        }
        return vmDiskInfoList;
    }

    @Override
    public Map<Long, Network.IpAddresses> getIpToNetworkMapFromBackup(Backup backup, boolean preserveIps, List<Long> networkIds)
    {
        Map<Long, Network.IpAddresses> ipToNetworkMap = new LinkedHashMap<Long, Network.IpAddresses>();

        String nicsJson = backup.getDetail(ApiConstants.NICS);
        if (nicsJson == null) {
            throw new CloudRuntimeException("Backup doesn't contain network information. " +
                    "Please specify at least one valid network while creating instance");
        }

        Type type = new TypeToken<List<Map<String, String>>>(){}.getType();
        List<Map<String, String>> nics = new Gson().fromJson(nicsJson, type);

        for (Map<String, String> nic : nics) {
            String networkUuid = nic.get(ApiConstants.NETWORK_ID);
            if (networkUuid == null) {
                throw new CloudRuntimeException("Backup doesn't contain network information. " +
                        "Please specify at least one valid network while creating instance");
            }

            Network network = networkDao.findByUuid(networkUuid);
            if (network == null) {
                throw new CloudRuntimeException("Unable to find network with the uuid " + networkUuid + " stored in backup. " +
                        "Please specify a valid network id while creating the instance");
            }

            Long networkId = network.getId();
            Network.IpAddresses ipAddresses = null;

            if (preserveIps) {
                String ip = nic.get(ApiConstants.IP_ADDRESS);
                String ipv6 = nic.get(ApiConstants.IP6_ADDRESS);
                String mac = nic.get(ApiConstants.MAC_ADDRESS);
                ipAddresses = networkService.getIpAddressesFromIps(ip, ipv6, mac);
            }

            ipToNetworkMap.put(networkId, ipAddresses);
            networkIds.add(networkId);
        }
        return ipToNetworkMap;
    }

    private void processRestoreBackupToVMFailure(VMInstanceVO vm, Backup backup, Long eventId) {
        updateVolumeState(vm, Volume.Event.RestoreFailed, Volume.State.Ready);
        updateVmState(vm, VirtualMachine.Event.RestoringFailed, VirtualMachine.State.Stopped);
        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_CREATE_FROM_BACKUP,
                String.format("Failed to create Instance %s from backup %s", vm.getInstanceName(), backup.getUuid()),
                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), eventId);
    }

    @Override
    public Boolean canCreateInstanceFromBackup(final Long backupId) {
        final BackupVO backup = backupDao.findById(backupId);
        BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("Failed to find backup offering");
        }
        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        return backupProvider.supportsInstanceFromBackup();
    }

    @Override
    public Boolean canCreateInstanceFromBackupAcrossZones(final Long backupId) {
        final BackupVO backup = backupDao.findById(backupId);
        BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("Failed to find backup offering");
        }
        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        return backupProvider.crossZoneInstanceCreationEnabled(offering);
    }

    @Override
    public boolean restoreBackupToVM(final Long backupId, final Long vmId) throws CloudRuntimeException {
        final BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        if (backup.getStatus() != Backup.Status.BackedUp) {
            throw new CloudRuntimeException("Backup should be in BackedUp state");
        }
        validateBackupForZone(backup.getZoneId());

        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Instance with ID " + backup.getVmId() + " couldn't be found.");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getRemoved() != null) {
            throw new CloudRuntimeException("Instance with ID " + backup.getVmId() + " couldn't be found.");
        }
        if (!vm.getState().equals(VirtualMachine.State.Stopped)) {
            throw new CloudRuntimeException("The VM should be in stopped state");
        }

        List<Backup.VolumeInfo> backupVolumes = backup.getBackedUpVolumes();
        if (backupVolumes == null) {
            throw new CloudRuntimeException("Backed up volumes info not found in the backup");
        }

        List<VolumeVO> vmVolumes = volumeDao.findByInstance(vmId);
        if (vmVolumes.size() != backupVolumes.size()) {
            throw new CloudRuntimeException("Unable to create Instance from backup as the backup has a different number of disks than the Instance");
        }

        BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("Failed to find backup offering");
        }
        final BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        if (!backupProvider.supportsInstanceFromBackup()) {
            throw new CloudRuntimeException("Create instance from backup is not supported by the " + offering.getProvider() + " provider.");
        }

        String backupDetailsInMessage = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backup, "uuid", "externalId", "name");
        Pair<Boolean, String> result = null;
        Long eventId = null;
        try {
            updateVmState(vm, VirtualMachine.Event.RestoringRequested, VirtualMachine.State.Restoring);
            updateVolumeState(vm, Volume.Event.RestoreRequested, Volume.State.Restoring);
            eventId = ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventTypes.EVENT_VM_CREATE_FROM_BACKUP,
                    String.format("Creating Instance %s from backup %s", vm.getInstanceName(), backup.getUuid()),
                    vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),
                    true, 0);

            String host = null;
            String dataStore = null;
            if (!"nas".equals(offering.getProvider())) {
                Pair<HostVO, StoragePoolVO> restoreInfo = getRestoreVolumeHostAndDatastore(vm);
                host = restoreInfo.first().getPrivateIpAddress();
                dataStore = restoreInfo.second().getUuid();
            }
            result = backupProvider.restoreBackupToVM(vm, backup, host, dataStore);

        } catch (Exception e) {
            logger.error(String.format("Failed to create Instance [%s] from backup [%s] due to: [%s]", vm.getInstanceName(), backupDetailsInMessage, e.getMessage()), e);
            processRestoreBackupToVMFailure(vm, backup, eventId);
            throw new CloudRuntimeException(String.format("Error while creating Instance [%s] from backup [%s].", vm.getUuid(), backupDetailsInMessage));
        }

        if (result != null && !result.first()) {
            String error_msg = String.format("Failed to create Instance [%s] from backup [%s] due to: %s.", vm.getInstanceName(), backupDetailsInMessage, result.second());
            logger.error(error_msg);
            processRestoreBackupToVMFailure(vm, backup, eventId);
            throw new CloudRuntimeException(error_msg);
        }

        updateVolumeState(vm, Volume.Event.RestoreSucceeded, Volume.State.Ready);
        updateVmState(vm, VirtualMachine.Event.RestoringSuccess, VirtualMachine.State.Stopped);
        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_CREATE_FROM_BACKUP,
                String.format("Successfully created Instance %s from backup %s", vm.getInstanceName(), backup.getUuid()),
                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),eventId);
        return true;
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
        if (backup.getStatus() != Backup.Status.BackedUp) {
            throw new CloudRuntimeException("Backup should be in BackedUp state");
        }
        validateBackupForZone(backup.getZoneId());

        final VMInstanceVO vm = findVmById(vmId);
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        if (vm.getBackupOfferingId() != null && !BackupEnableAttachDetachVolumes.value()) {
            throw new CloudRuntimeException("The selected VM has backups, cannot restore and attach volume to the VM.");
        }

        if (backup.getZoneId() != vm.getDataCenterId()) {
            throw new CloudRuntimeException("Cross zone backup restoration of volume is not allowed");
        }

        List<Backup.VolumeInfo> volumeInfoList = backup.getBackedUpVolumes();
        if (volumeInfoList == null) {
            final VMInstanceVO vmFromBackup = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
            if (vmFromBackup == null) {
                throw new CloudRuntimeException("VM reference for the provided VM backup not found");
            } else if (vmFromBackup == null || vmFromBackup.getBackupVolumeList() == null) {
                throw new CloudRuntimeException("Volumes metadata not found in the backup");
            }
            volumeInfoList = vm.getBackupVolumeList();
        }
        Backup.VolumeInfo backupVolumeInfo = getVolumeInfo(volumeInfoList, backedUpVolumeUuid);
        if (backupVolumeInfo == null) {
            throw new CloudRuntimeException("Failed to find volume with Id " + backedUpVolumeUuid + " in the backed-up volumes metadata");
        }

        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);
        final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException("Failed to find VM backup offering");
        }

        BackupProvider backupProvider = getBackupProvider(offering.getProvider());
        VolumeVO backedUpVolume = volumeDao.findByUuid(backedUpVolumeUuid);
        Pair<HostVO, StoragePoolVO> restoreInfo;
        if (!"nas".equals(offering.getProvider()) || (backedUpVolume == null)) {
            restoreInfo = getRestoreVolumeHostAndDatastore(vm);
        } else {
            restoreInfo = getRestoreVolumeHostAndDatastoreForNas(vm, backedUpVolume);
        }

        HostVO host = restoreInfo.first();
        StoragePoolVO datastore = restoreInfo.second();

        logger.debug("Asking provider to restore volume {} from backup {} (with external" +
                " ID {}) and attach it to VM: {}", backedUpVolumeUuid, backup, backup.getExternalId(), vm);

        logger.debug(String.format("Trying to restore volume using host private IP address: [%s].", host.getPrivateIpAddress()));

        String[] hostPossibleValues = {host.getPrivateIpAddress(), host.getName()};
        String[] datastoresPossibleValues = {datastore.getUuid(), datastore.getName()};

        Pair<Boolean, String> result = restoreBackedUpVolume(backupVolumeInfo, backup, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        if (BooleanUtils.isFalse(result.first())) {
            throw new CloudRuntimeException(String.format("Error restoring volume [%s] of VM [%s] to host [%s] using backup provider [%s] due to: [%s].",
                    backedUpVolumeUuid, vm.getUuid(), host.getUuid(), backupProvider.getName(), result.second()));
        }
        if (!attachVolumeToVM(vm.getDataCenterId(), result.second(), backupVolumeInfo,
                            backedUpVolumeUuid, vm, datastore.getUuid(), backup)) {
            throw new CloudRuntimeException(String.format("Error attaching volume [%s] to VM [%s].", backedUpVolumeUuid, vm.getUuid()));
        }
        return true;
    }

    protected Pair<Boolean, String> restoreBackedUpVolume(final Backup.VolumeInfo backupVolumeInfo, final BackupVO backup,
            BackupProvider backupProvider, String[] hostPossibleValues, String[] datastoresPossibleValues, VMInstanceVO vm) {
        Pair<Boolean, String> result = new  Pair<>(false, "");
        for (String hostData : hostPossibleValues) {
            for (String datastoreData : datastoresPossibleValues) {
                logger.debug(String.format("Trying to restore volume [UUID: %s], using host [%s] and datastore [%s].",
                        backupVolumeInfo.getUuid(), hostData, datastoreData));

                try {
                    result = backupProvider.restoreBackedUpVolume(backup, backupVolumeInfo, hostData, datastoreData, new Pair<>(vm.getName(), vm.getState()));

                    if (BooleanUtils.isTrue(result.first())) {
                        return result;
                    }
                } catch (Exception e) {
                    logger.debug(String.format("Failed to restore volume [UUID: %s], using host [%s] and datastore [%s] due to: [%s].",
                            backupVolumeInfo.getUuid(), hostData, datastoreData, e.getMessage()), e);
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
            logger.warn("Instance {} not found for backup {} during delete backup", vmId, backup.toString());
        }
        logger.debug("Deleting backup {} belonging to instance {}", backup.toString(), vmId);

        validateBackupForZone(backup.getZoneId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm == null ? backup : vm);
        final BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        if (offering == null) {
            throw new CloudRuntimeException(String.format("Backup offering with ID [%s] does not exist.", backup.getBackupOfferingId()));
        }
        final BackupProvider backupProvider = getBackupProvider(backup.getZoneId());
        boolean result = backupProvider.deleteBackup(backup, forced);
        if (result) {
            resourceLimitMgr.decrementResourceCount(backup.getAccountId(), Resource.ResourceType.backup);
            Long backupSize = backup.getSize() != null ? backup.getSize() : 0L;
            resourceLimitMgr.decrementResourceCount(backup.getAccountId(), Resource.ResourceType.backup_storage, backupSize);
            if (backupDao.remove(backup.getId())) {
                checkAndGenerateUsageForLastBackupDeletedAfterOfferingRemove(vm, backup);
                return true;
            } else {
                return false;
            }
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

    private Pair<HostVO, StoragePoolVO> getRestoreVolumeHostAndDatastoreForNas(VMInstanceVO vm, VolumeVO backedVolume) {
        Long poolId = backedVolume.getPoolId();
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
    private boolean attachVolumeToVM(Long zoneId, String restoredVolumeLocation, Backup.VolumeInfo backupVolumeInfo,
                                     String volumeUuid, VMInstanceVO vm, String datastoreUuid, Backup backup) throws Exception {
        HypervisorGuru guru = hypervisorGuruManager.getGuru(vm.getHypervisorType());
        backupVolumeInfo.setType(Volume.Type.DATADISK);

        logger.info("Attaching the restored volume {} to VM {}.", () -> ReflectionToStringBuilder.toString(backupVolumeInfo, ToStringStyle.JSON_STYLE), () -> vm);
        StoragePoolVO pool = primaryDataStoreDao.findByUuid(datastoreUuid);
        try {
            return guru.attachRestoredVolumeToVirtualMachine(zoneId, restoredVolumeLocation, backupVolumeInfo, vm, pool.getId(), backup);
        } catch (Exception e) {
            throw new CloudRuntimeException("Error attach restored volume to VM " + vm.getUuid() + " due to: " + e.getMessage());
        }
    }

    private void checkAndGenerateUsageForLastBackupDeletedAfterOfferingRemove(VirtualMachine vm, Backup backup) {
        if (vm != null &&
                (vm.getBackupOfferingId() == null || vm.getBackupOfferingId() != backup.getBackupOfferingId())) {
            List<Backup> backups = backupDao.listByVmIdAndOffering(vm.getDataCenterId(), vm.getId(), backup.getBackupOfferingId());
            if (backups.size() == 0) {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_REMOVED_AND_BACKUPS_DELETED, vm.getAccountId(),
                        vm.getDataCenterId(), vm.getId(), "Backup-" + vm.getHostName() + "-" + vm.getUuid(),
                        backup.getBackupOfferingId(), null, null, Backup.class.getSimpleName(), vm.getUuid());
            }
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

    @Override
    public void validateBackupForZone(final Long zoneId) {
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
        cmdList.add(AddBackupRepositoryCmd.class);
        cmdList.add(UpdateBackupRepositoryCmd.class);
        cmdList.add(DeleteBackupRepositoryCmd.class);
        cmdList.add(ListBackupRepositoriesCmd.class);
        cmdList.add(CreateVMFromBackupCmd.class);
        cmdList.add(CreateVMFromBackupCmdByAdmin.class);
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
                BackupEnableAttachDetachVolumes,
                DefaultMaxAccountBackups,
                DefaultMaxAccountBackupStorage,
                DefaultMaxProjectBackups,
                DefaultMaxProjectBackupStorage,
                DefaultMaxDomainBackups,
                DefaultMaxDomainBackupStorage,
                BackupStorageCapacityThreshold
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
                    logger.debug("Next backup scheduled time for VM ID " + backupSchedule.getVmId() + " is " + nextScheduledTime);
                    break;
            default:
                logger.debug("Found async backup job [id: {}, uuid: {}, vmId: {}] with " +
                        "status [{}] and cmd information: [cmd: {}, cmdInfo: {}].",
                        asyncJob.getId(), asyncJob.getUuid(), backupSchedule.getVmId(),
                        asyncJob.getStatus(), asyncJob.getCmd(), asyncJob.getCmdInfo());
                break;
            }
        }
    }

    @DB
    public void scheduleBackups() {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        logger.debug("Backup backup.poll is being called at " + displayTime);

        final List<BackupScheduleVO> backupsToBeExecuted = backupScheduleDao.getSchedulesToExecute(currentTimestamp);
        for (final BackupScheduleVO backupSchedule: backupsToBeExecuted) {
            final Long backupScheduleId = backupSchedule.getId();
            final Long vmId = backupSchedule.getVmId();
            final Boolean quiesceVm = backupSchedule.getQuiesceVM();

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
                logger.debug("Skip backup for VM ({}) since its account has been removed or disabled.", vm);
                continue;
            }

            if (logger.isDebugEnabled()) {
                final Date scheduledTimestamp = backupSchedule.getScheduledTimestamp();
                displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
                logger.debug(String.format("Scheduling 1 backup for VM (%s) for backup schedule (%s) at [%s].",
                        vm, backupSchedule, displayTime));
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
                params.put(ApiConstants.SCHEDULE_ID, String.valueOf(backupScheduleId));
                if (quiesceVm != null) {
                    params.put(ApiConstants.QUIESCE_VM, "" + quiesceVm.toString());
                }
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
                logger.error(String.format("Scheduling backup failed due to: [%s].", e.getMessage()), e);
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
                logger.warn("Catch throwable in backup scheduler ", t);
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
    protected final class BackupSyncTask extends ManagedContextRunnable implements BackgroundPollTask {
        private BackupManager backupManager;

        public BackupSyncTask(final BackupManager backupManager) {
            this.backupManager = backupManager;
        }

        @Override
        protected void runInContext() {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("Backup sync background task is running...");
                }
                for (final DataCenter dataCenter : dataCenterDao.listAllZones()) {
                    if (dataCenter == null || isDisabled(dataCenter.getId())) {
                        logger.debug("Backup Sync Task is not enabled in zone [{}]. Skipping this zone!", dataCenter == null ? "NULL Zone!" : dataCenter);
                        continue;
                    }

                    final BackupProvider backupProvider = getBackupProvider(dataCenter.getId());
                    if (backupProvider == null) {
                        logger.warn("Backup provider not available or configured for zone {}", dataCenter);
                        continue;
                    }

                    backupProvider.syncBackupStorageStats(dataCenter.getId());

                    syncOutOfBandBackups(backupProvider, dataCenter);

                    updateBackupUsageRecords(backupProvider, dataCenter);
                }
            } catch (final Throwable t) {
                logger.error(String.format("Error trying to run backup-sync background task due to: [%s].", t.getMessage()), t);
            }
        }

        private void syncOutOfBandBackups(final BackupProvider backupProvider, DataCenter dataCenter) {
            List<VMInstanceVO> vms = vmInstanceDao.listByZoneAndBackupOffering(dataCenter.getId(), null);
            if (vms == null || vms.isEmpty()) {
                logger.debug("Can't find any VM to sync backups in zone {}", dataCenter);
                return;
            }
            backupProvider.syncBackupMetrics(dataCenter.getId());
            for (final VMInstanceVO vm : vms) {
                try {
                     logger.debug(String.format("Trying to sync backups of VM [%s] using backup provider [%s].", vm, backupProvider.getName()));
                     // Sync out-of-band backups
                     syncBackups(backupProvider, vm);
                } catch (final Exception e) {
                    logger.error("Failed to sync backup usage metrics and out-of-band backups of VM [{}] due to: [{}].", vm, e.getMessage(), e);
                }
            }
        }

        private void updateBackupUsageRecords(final BackupProvider backupProvider, DataCenter dataCenter) {
            List<Long> vmIdsWithBackups = backupDao.listVmIdsWithBackupsInZone(dataCenter.getId());
            List<VMInstanceVO> vmsWithBackups;
            if (vmIdsWithBackups.size() == 0) {
                vmsWithBackups = new ArrayList<>();
            } else {
                vmsWithBackups = vmInstanceDao.listByIdsIncludingRemoved(vmIdsWithBackups);
            }
            List<VMInstanceVO> vmsWithBackupOffering = vmInstanceDao.listByZoneAndBackupOffering(dataCenter.getId(), null); //should return including removed
            Set<VMInstanceVO> vms =  Stream.concat(vmsWithBackups.stream(), vmsWithBackupOffering.stream()) .collect(Collectors.toSet());

            for (final VirtualMachine vm : vms) {

                Map<Long, Pair<Long, Long>> backupOfferingToSizeMap = new HashMap<>();
                List<Backup> backups = backupDao.listByVmId(null, vm.getId());
                if (backups.isEmpty() && vm.getBackupOfferingId() != null) {
                    backupOfferingToSizeMap.put(vm.getBackupOfferingId(), new Pair<>(0L, 0L));
                }
                for (final Backup backup: backups) {
                    Long backupSize = 0L;
                    Long backupProtectedSize = 0L;
                    if (Objects.nonNull(backup.getSize())) {
                        backupSize = backup.getSize();
                    }
                    if (Objects.nonNull(backup.getProtectedSize())) {
                        backupProtectedSize = backup.getProtectedSize();
                    }
                    Long offeringId = backup.getBackupOfferingId();
                    if (backupOfferingToSizeMap.containsKey(offeringId)) {
                        Pair<Long, Long> sizes = backupOfferingToSizeMap.get(offeringId);
                        sizes.set(sizes.first() + backupSize, sizes.second() + backupProtectedSize);
                    } else {
                        backupOfferingToSizeMap.put(offeringId, new Pair<>(backupSize, backupProtectedSize));
                    }
                }

                for (final Map.Entry<Long, Pair<Long, Long>> entry : backupOfferingToSizeMap.entrySet()) {
                    Long offeringId = entry.getKey();
                    Pair<Long, Long> sizes = entry.getValue();
                    Long backupSize = sizes.first();
                    Long protectedSize = sizes.second();
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_USAGE_METRIC, vm.getAccountId(),
                            vm.getDataCenterId(), vm.getId(), "Backup-" + vm.getHostName() + "-" + vm.getUuid(),
                            offeringId, null, backupSize, protectedSize,
                            Backup.class.getSimpleName(), vm.getUuid());
                }
            }
        }

        private Backup checkAndUpdateIfBackupEntryExistsForRestorePoint(Backup.RestorePoint restorePoint, List<Backup> backupsInDb, VirtualMachine vm) {
            for (final Backup backupInDb : backupsInDb) {
                logger.debug(String.format("Checking if Backup %s with external ID %s for VM %s is valid", backupsInDb, backupInDb.getName(), vm));
                if (restorePoint.getId().equals(backupInDb.getExternalId())) {
                    logger.debug(String.format("Found Backup %s in both Database and Provider", backupInDb));
                    if (restorePoint.getDataSize() != null && restorePoint.getBackupSize() != null) {
                        logger.debug(String.format("Update backup [%s] from [size: %s, protected size: %s] to [size: %s, protected size: %s].",
                                backupInDb, backupInDb.getSize(), backupInDb.getProtectedSize(), restorePoint.getBackupSize(), restorePoint.getDataSize()));

                        resourceLimitMgr.decrementResourceCount(backupInDb.getAccountId(), Resource.ResourceType.backup_storage, backupInDb.getSize());
                        ((BackupVO) backupInDb).setSize(restorePoint.getBackupSize());
                        ((BackupVO) backupInDb).setProtectedSize(restorePoint.getDataSize());
                        resourceLimitMgr.incrementResourceCount(backupInDb.getAccountId(), Resource.ResourceType.backup_storage, backupInDb.getSize());

                        backupDao.update(backupInDb.getId(), ((BackupVO) backupInDb));
                    }
                    return backupInDb;
                }
            }
            return null;
        }

        private void processRemoveList(List<Long> removeList, VirtualMachine vm) {
            for (final Long backupIdToRemove : removeList) {
                logger.warn(String.format("Removing backup with ID: [%s].", backupIdToRemove));
                Backup backup = backupDao.findById(backupIdToRemove);
                resourceLimitMgr.decrementResourceCount(backup.getAccountId(), Resource.ResourceType.backup);
                resourceLimitMgr.decrementResourceCount(backup.getAccountId(), Resource.ResourceType.backup_storage, backup.getSize());
                boolean result = backupDao.remove(backupIdToRemove);
                if (result) {
                    checkAndGenerateUsageForLastBackupDeletedAfterOfferingRemove(vm, backup);
                } else {
                    logger.error("Failed to remove backup db entry ith ID: {} during sync backups", backupIdToRemove);
                }
            }
        }

        private void syncBackups(BackupProvider backupProvider, VirtualMachine vm) {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    final List<Backup> backupsInDb = backupDao.listByVmId(null, vm.getId());
                    List<Backup.RestorePoint> restorePoints = backupProvider.listRestorePoints(vm);
                    if (restorePoints == null) {
                        return;
                    }

                    final List<Long> removeList = backupsInDb.stream().map(InternalIdentity::getId).collect(Collectors.toList());
                    for (final Backup.RestorePoint restorePoint : restorePoints) {
                        if (!(restorePoint.getId() == null || restorePoint.getType() == null || restorePoint.getCreated() == null)) {
                            Backup existingBackupEntry = checkAndUpdateIfBackupEntryExistsForRestorePoint(restorePoint, backupsInDb, vm);
                            if (existingBackupEntry != null) {
                                removeList.remove(existingBackupEntry.getId());
                                continue;
                            }
                        }

                        Backup backup = backupProvider.createNewBackupEntryForRestorePoint(restorePoint, vm);
                        if (backup != null) {
                            logger.warn("Added backup found in provider [" + backup + "]");
                            resourceLimitMgr.incrementResourceCount(vm.getAccountId(), Resource.ResourceType.backup);
                            resourceLimitMgr.incrementResourceCount(vm.getAccountId(), Resource.ResourceType.backup_storage, backup.getSize());

                            logger.debug(String.format("Creating a new entry in backups: [id: %s, uuid: %s, vm_id: %s, external_id: %s, type: %s, date: %s, backup_offering_id: %s, account_id: %s, "
                                            + "domain_id: %s, zone_id: %s].", backup.getId(), backup.getUuid(), backup.getVmId(), backup.getExternalId(), backup.getType(), backup.getDate(),
                                    backup.getBackupOfferingId(), backup.getAccountId(), backup.getDomainId(), backup.getZoneId()));

                            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_BACKUP_CREATE,
                                    String.format("Created backup %s for VM ID: %s", backup.getUuid(), vm.getUuid()),
                                    vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),0);
                        }
                    }
                    processRemoveList(removeList, vm);
                }
            });
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
        logger.debug("Trying to update Backup Offering {} to {}.",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backupOfferingVO, "uuid", "name", "description", "userDrivenBackupAllowed"),
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(updateBackupOfferingCmd, "name", "description", "allowUserDrivenBackups"));

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
            logger.warn(String.format("Couldn't update Backup offering (%s) with [%s].", backupOfferingVO, String.join(", ", fields)));
        }

        BackupOfferingVO response = backupOfferingDao.findById(id);
        CallContext.current().setEventDetails(String.format("Backup Offering updated [%s].",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(response, "id", "name", "description", "userDrivenBackupAllowed", "externalId")));
        return response;
    }

    Map<String, String> getDetailsFromBackupDetails(Long backupId) {
        Map<String, String> details = backupDetailsDao.listDetailsKeyPairs(backupId, true);
        if (details == null) {
            return null;
        }
        if (details.containsKey(ApiConstants.TEMPLATE_ID)) {
            VirtualMachineTemplate template = vmTemplateDao.findByUuid(details.get(ApiConstants.TEMPLATE_ID));
            if (template != null) {
                details.put(ApiConstants.TEMPLATE_NAME, template.getName());
                details.put(ApiConstants.IS_ISO, String.valueOf(template.getFormat().equals(Storage.ImageFormat.ISO)));
            }
        }
        if (details.containsKey(ApiConstants.SERVICE_OFFERING_ID)) {
            ServiceOffering serviceOffering = serviceOfferingDao.findByUuid(details.get(ApiConstants.SERVICE_OFFERING_ID));
            if (serviceOffering != null) {
                details.put(ApiConstants.SERVICE_OFFERING_NAME, serviceOffering.getName());
            }
        }
        if (details.containsKey(ApiConstants.NICS)) {
            Type type = new TypeToken<List<Map<String, String>>>() {}.getType();
            List<Map<String, String>> nics = new Gson().fromJson(details.get(ApiConstants.NICS), type);

            for (Map<String, String> nic : nics) {
                String networkUuid = nic.get(ApiConstants.NETWORK_ID);
                if (networkUuid != null) {
                    Network network = networkDao.findByUuid(networkUuid);
                    if (network != null) {
                        nic.put(ApiConstants.NETWORK_NAME, network.getName());
                    }
                }
            }
            details.put(ApiConstants.NICS, new Gson().toJson(nics));
        }
        return details;
    }

    @Override
    public BackupResponse createBackupResponse(Backup backup, Boolean listVmDetails) {
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        AccountVO account = accountDao.findByIdIncludingRemoved(backup.getAccountId());
        DomainVO domain = domainDao.findByIdIncludingRemoved(backup.getDomainId());
        DataCenterVO zone = dataCenterDao.findByIdIncludingRemoved(backup.getZoneId());
        Long offeringId = backup.getBackupOfferingId();
        BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(offeringId);

        BackupResponse response = new BackupResponse();
        response.setId(backup.getUuid());
        response.setName(backup.getName());
        response.setDescription(backup.getDescription());
        if (vm != null) {
            response.setVmName(vm.getHostName());
            response.setVmId(vm.getUuid());
            if (vm.getBackupOfferingId() == null || vm.getBackupOfferingId() != backup.getBackupOfferingId()) {
                response.setVmOfferingRemoved(true);
            }
        }
        if (vm == null || VirtualMachine.State.Expunging.equals(vm.getState())) {
            response.setVmExpunged(true);
        }
        response.setExternalId(backup.getExternalId());
        response.setType(backup.getType());
        response.setDate(backup.getDate());
        response.setSize(backup.getSize());
        response.setProtectedSize(backup.getProtectedSize());
        response.setStatus(backup.getStatus());
        response.setIntervalType("MANUAL");
        if (backup.getBackupScheduleId() != null) {
            BackupScheduleVO scheduleVO = backupScheduleDao.findById(backup.getBackupScheduleId());
            if (scheduleVO != null) {
                response.setIntervalType(scheduleVO.getScheduleType().toString());
            }
        }
        // ACS 4.20: For backups taken prior this release the backup.backed_volumes column would be empty hence use vm_instance.backup_volumes
        String backedUpVolumes = "";
        if (Objects.isNull(backup.getBackedUpVolumes())) {
            if (vm != null) {
                backedUpVolumes = new Gson().toJson(vm.getBackupVolumeList().toArray(), Backup.VolumeInfo[].class);
            }
        } else {
            backedUpVolumes = new Gson().toJson(backup.getBackedUpVolumes().toArray(), Backup.VolumeInfo[].class);
        }
        response.setVolumes(backedUpVolumes);
        response.setBackupOfferingId(offering.getUuid());
        response.setBackupOffering(offering.getName());
        response.setAccountId(account.getUuid());
        response.setAccount(account.getAccountName());
        response.setDomainId(domain.getUuid());
        response.setDomain(domain.getName());
        response.setZoneId(zone.getUuid());
        response.setZone(zone.getName());

        if (Boolean.TRUE.equals(listVmDetails)) {
            Map<String, String> vmDetails = new HashMap<>();
            if (vm != null) {
                vmDetails.put(ApiConstants.HYPERVISOR, vm.getHypervisorType().toString());
            }
            Map<String, String> details = getDetailsFromBackupDetails(backup.getId());
            vmDetails.putAll(details);
            response.setVmDetails(vmDetails);
        }

        response.setObjectName("backup");
        return response;
    }

    @Override
    public CapacityVO getBackupStorageUsedStats(Long zoneId) {
        if (isDisabled(zoneId)) {
            return new CapacityVO(null, zoneId, null, null, 0L, 0L, Capacity.CAPACITY_TYPE_BACKUP_STORAGE);
        }
        final BackupProvider backupProvider = getBackupProvider(zoneId);
        Pair<Long, Long> backupUsage = backupProvider.getBackupStorageStats(zoneId);
        return new CapacityVO(null, zoneId, null, null, backupUsage.first(), backupUsage.second(), Capacity.CAPACITY_TYPE_BACKUP_STORAGE);
    }

    @Override
    public void checkAndRemoveBackupOfferingBeforeExpunge(VirtualMachine vm) {
        if (vm.getBackupOfferingId() == null) {
            return;
        }
        List<Backup> backupsForVm = backupDao.listByVmIdAndOffering(vm.getDataCenterId(), vm.getId(), vm.getBackupOfferingId());
        if (org.apache.commons.collections.CollectionUtils.isEmpty(backupsForVm)) {
            removeVMFromBackupOffering(vm.getId(), true);
        } else {
            throw new CloudRuntimeException(String.format("This Instance [uuid: %s, name: %s] has a "
                            + "Backup Offering [id: %s, external id: %s] with %s backups. Please, remove the backup offering "
                            + "before proceeding to VM exclusion!", vm.getUuid(), vm.getInstanceName(), vm.getBackupOfferingId(),
                    vm.getBackupExternalId(), backupsForVm.size()));
        }
    }
}
