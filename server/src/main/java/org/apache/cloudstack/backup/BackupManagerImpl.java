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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.backup.DeleteBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProviderOfferingsCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProvidersCmd;
import org.apache.cloudstack.api.command.admin.vm.ImportVMCmdByAdmin;
import org.apache.cloudstack.api.command.user.backup.CreateBackupCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.DeleteBackupCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupOfferingsCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupsCmd;
import org.apache.cloudstack.api.command.user.backup.RestoreBackupCmd;
import org.apache.cloudstack.api.command.user.backup.RestoreVolumeFromBackupAndAttachToVMCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
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
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.usage.dao.UsageBackupDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;

@Component
public class BackupManagerImpl extends ManagerBase implements BackupManager {
    private static final Logger LOG = Logger.getLogger(BackupManagerImpl.class);

    @Inject
    private BackupOfferingDao backupOfferingDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private AccountService accountService;
    @Inject
    private AccountManager accountManager;
    @Inject
    private BackupDao backupDao;
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
            throw new CloudRuntimeException("Policy " + offeringExternalId + " does not exist on provider " + provider.getName() + " on zone " + zoneId);
        }

        final BackupOfferingVO policy = new BackupOfferingVO(zoneId, offeringExternalId, offeringName, offeringDescription);
        final BackupOfferingVO savedPolicy = backupOfferingDao.persist(policy);
        if (savedPolicy == null) {
            throw new CloudRuntimeException("Unable to create backup offering: " + offeringExternalId + ", name: " + offeringName);
        }
        LOG.debug("Successfully created backup offering " + offeringName + " mapped to backup provider offering " + offeringExternalId);
        return savedPolicy;
    }

    @Override
    public List<BackupOffering> listBackupOfferings(final Long zoneId, final Long offeringId) {
        if (offeringId != null) {
            BackupOfferingVO policy = backupOfferingDao.findById(offeringId);
            if (policy == null) {
                throw new CloudRuntimeException("Offering ID " + offeringId + " does not exist");
            }
            return Collections.singletonList(policy);
        }
        return backupOfferingDao.listByZone(zoneId);
    }

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
        LOG.debug("Listing external backup policies for the backup provider registered in zone " + zoneId);
        return backupProvider.listBackupOfferings(zoneId);    }

    @Override
    public List<Backup> listBackups(final Long id, final Long vmId) {
        final Account callerAccount = CallContext.current().getCallingAccount();
        final VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            if (id != null) {
                return Collections.singletonList(backupDao.findById(id));
            }
            if (accountService.isRootAdmin(callerAccount.getId())) {
                return new ArrayList<>(backupDao.listAll());
            } else {
                return new ArrayList<>(backupDao.listByAccountId(callerAccount.getId()));
            }
        }
        accountManager.checkAccess(callerAccount, null, true, vm);
        return backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
    }

    @Override
    public List<Backup.RestorePoint> listBackupRestorePoints(final Long backupId) {
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Could not find backup " + backupId);
        }
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("Could not find VM: " + backup.getVmId());
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);
        BackupProvider backupProvider = getBackupProvider(backup.getZoneId());
        return backupProvider.listBackupRestorePoints(backup.getUuid(), vm);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_CREATE, eventDescription = "creating VM backup", async = true)
    public Backup createBackup(final Long vmId, final Long offeringId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Did not find VM by provided ID");
        }

        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        final Backup vmBackup = backupDao.findByVmId(vm.getId());
        if (vmBackup != null) {
            if (offeringId != null && !vmBackup.getOfferingId().equals(offeringId)) {
                throw new CloudRuntimeException("VM backup(s) already exists and a different offering ID is provided than previously configured");
            }
            final BackupProvider backupProvider = getBackupProvider(vmBackup.getZoneId());
            if (backupProvider != null && backupProvider.takeBackup(vmBackup)) {
                return vmBackup;
            }
            LOG.warn("VM already has backup(s), failed to take backup");
            return null;
        }

        final BackupOfferingVO policy = backupOfferingDao.findById(offeringId);
        if (policy == null) {
            throw new CloudRuntimeException("Provided backup offering does not exist");
        }

        BackupVO backup = new BackupVO(vmId, offeringId, Backup.Status.Allocated, vm.getAccountId(), vm.getDataCenterId());
        setBackupVolumes(backup, vm);
        backup = backupDao.persist(backup);
        if (backup == null) {
            throw new CloudRuntimeException("Failed to persist backup object in database");
        }

        final BackupProvider backupProvider = getBackupProvider(vm.getDataCenterId());
        if (backupProvider == null) {
            throw new CloudRuntimeException("Failed to find backup provider for the zone");
        }
        backup = (BackupVO) backupProvider.createBackup(policy, vm, backup);
        if (backup == null) {
            throw new CloudRuntimeException("Backup provider failed to create backup for VM: " + vm.getUuid());
        }
        if (backupDao.update(backup.getId(), backup)) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_CREATE, vm.getAccountId(), vm.getDataCenterId(), backup.getId(),
                    vm.getUuid(), backup.getOfferingId(), backup.getVmId(), null,
                    Backup.class.getSimpleName(), backup.getUuid());
        }
        return backup;
    }

    private void setBackupVolumes(BackupVO backup, VMInstanceVO vm) {
        List<VolumeVO> vmVolumes = volumeDao.findByInstance(vm.getId());
        List<Backup.VolumeInfo> volInfo = createVolumeInfoFromVolumes(vmVolumes);
        backup.setBackedUpVolumes(volInfo);
    }

    private List<Backup.VolumeInfo> createVolumeInfoFromVolumes(List<VolumeVO> vmVolumes) {
        List<Backup.VolumeInfo> list = new ArrayList<>();
        for (VolumeVO vol : vmVolumes) {
            list.add(new Backup.VolumeInfo(vol.getUuid(), vol.getPath(), vol.getVolumeType(), vol.getSize()));
        }
        return list;
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
        final BackupProvider backupProvider = getBackupProvider(backup.getZoneId());
        boolean result = backupProvider.removeBackup(vm, backup);
        if (result) {
            backup.setStatus(Backup.Status.Expunged);
            if (backupDao.update(backup.getId(), backup)) {
                backupDao.remove(backup.getId());
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_DELETE, vm.getAccountId(), vm.getDataCenterId(), backup.getId(),
                        vm.getUuid(), backup.getOfferingId(), backup.getVmId(), null,
                        Backup.class.getSimpleName(), backup.getUuid());
                return true;
            }
        }
        // FIXME: fsm to deal with GC+deletion
        backup.setStatus(Backup.Status.Removed);
        if (backupDao.update(backup.getId(), backup)) {
            backupDao.remove(backupId);
            return true;
        }

        return result;
    }

    public boolean importVM(long zoneId, long domainId, long accountId, long userId,
                            String vmInternalName, Hypervisor.HypervisorType hypervisorType, Backup backup) {
        //TODO: Remove it from the backup manager interface
        //TODO: refactor with VM ingestion feature in future
        HypervisorGuru guru = hypervisorGuruManager.getGuru(hypervisorType);
        try {
            guru.importVirtualMachine(zoneId, domainId, accountId, userId, vmInternalName, backup);
        } catch (Exception e) {
            LOG.error("Failed to import VM", e);
            throw new CloudRuntimeException("Error during vm import: " + e.getMessage());
        }
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_RESTORE, eventDescription = "restoring VM from backup", async = true)
    public boolean restoreBackup(final Long backupId, final String restorePointId) {
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        Long vmId = backup.getVmId();
        BackupProvider backupProvider = getBackupProvider(backup.getZoneId());
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " couldn't be found on existing or removed VMs");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);
        if (vm.getRemoved() == null && !vm.getState().equals(VirtualMachine.State.Stopped) &&
                !vm.getState().equals(VirtualMachine.State.Destroyed)) {
            throw new CloudRuntimeException("Existing VM should be stopped before being restored from backup");
        }
        if (!backupProvider.restoreVMFromBackup(vm, backup.getExternalId(), restorePointId)) {
            throw new CloudRuntimeException("Error restoring VM " + vm.getId() + " from backup " + backup.getId());
        }
        importVM(vm.getDataCenterId(), vm.getDomainId(), vm.getAccountId(), vm.getUserId(),
                vm.getInstanceName(), vm.getHypervisorType(), backup);
        return true;
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
    public boolean restoreBackupVolumeAndAttachToVM(final String backedUpVolumeUuid, final Long vmId, final Long backupId, final String restorePointId) throws Exception {
        if (Strings.isNullOrEmpty(backedUpVolumeUuid)) {
            throw new CloudRuntimeException("Invalid volume ID passed");
        }
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " does not exist");
        }

        VMInstanceVO vmFromBackup = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vmFromBackup != null) {
            accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vmFromBackup);
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);

        Pair<String, String> restoreInfo = getRestoreVolumeHostAndDatastore(vm);
        String hostIp = restoreInfo.first();
        String datastoreUuid = restoreInfo.second();

        LOG.debug("Asking provider to restore volume " + backedUpVolumeUuid + " from backup " + backupId +
                " and restore point " + restorePointId + " and attach it to VM: " + vm.getUuid());
        BackupProvider backupProvider = getBackupProvider(backup.getZoneId());
        Pair<Boolean, String> result = backupProvider.restoreBackedUpVolume(backup.getZoneId(), backup.getUuid(),
                                                        restorePointId, backedUpVolumeUuid, hostIp, datastoreUuid);
        if (!result.first()) {
            throw new CloudRuntimeException("Error restoring volume " + backedUpVolumeUuid);
        }
        if (!attachVolumeToVM(backup.getZoneId(), result.second(), backup.getBackedUpVolumes(),
                            backedUpVolumeUuid, vm, datastoreUuid, backup)) {
            throw new CloudRuntimeException("Error attaching volume " + backedUpVolumeUuid + " to VM " + vm.getUuid());
        }
        return true;
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
    public boolean deleteBackupOffering(final Long policyId) {
        if (!backupDao.listByPolicyId(policyId).isEmpty()) {
            throw new CloudRuntimeException("Cannot allow deletion of backup offering due to use in existing VM backups, please delete the VM backups first");
        }
        BackupOfferingVO policy = backupOfferingDao.findById(policyId);
        if (policy == null) {
            throw new CloudRuntimeException("Could not find a backup offering with id: " + policyId);
        }
        return backupOfferingDao.expunge(policy.getId());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        backgroundPollManager.submitTask(new BackupSyncTask(this));
        return true;
    }

    public boolean isEnabled(final Long zoneId) {
        return BackupFrameworkEnabled.valueIn(zoneId);
    }

    @Override
    public List<BackupProvider> listBackupProviders() {
        return backupProviders;
    }

    @Override
    public BackupProvider getBackupProvider(final Long zoneId) {
        final String name = BackupProviderPlugin.valueIn(zoneId);
        if (Strings.isNullOrEmpty(name)) {
            throw new CloudRuntimeException("Invalid backup provider name configured in zone id: " + zoneId);
        }
        if (!backupProvidersMap.containsKey(name)) {
            throw new CloudRuntimeException("Failed to find backup provider for zone id:" + zoneId);
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
        // Operations
        cmdList.add(CreateBackupCmd.class);
        cmdList.add(ListBackupsCmd.class);
        cmdList.add(RestoreBackupCmd.class);
        cmdList.add(DeleteBackupCmd.class);
        cmdList.add(RestoreVolumeFromBackupAndAttachToVMCmd.class);
        cmdList.add(ImportVMCmdByAdmin.class);
        // Schedule
        cmdList.add(CreateBackupScheduleCmd.class);
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
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Backup sync background task is running...");
                }
                for (final DataCenter dataCenter : dataCenterDao.listAllZones()) {
                    if (dataCenter == null || !isEnabled(dataCenter.getId())) {
                        continue;
                    }

                    // GC and expunge removed backups
                    for (final Backup backup : backupDao.listByZoneAndState(dataCenter.getId(), Backup.Status.Removed)) {
                        backupManager.deleteBackup(backup.getId());
                    }

                    // Sync backup size usages
                    final List<Backup> backups = backupDao.listByZoneAndState(dataCenter.getId(), null);
                    if (backups.isEmpty()) {
                        continue;
                    }
                    final BackupProvider backupProvider = getBackupProvider(dataCenter.getId());
                    final Map<Backup, Backup.Metric> metrics = backupProvider.getBackupMetrics(dataCenter.getId(), backups);
                    for (final Backup backup : metrics.keySet()) {
                        final Backup.Metric metric = metrics.get(backup);
                        final BackupVO backupVO = (BackupVO) backup;
                        backupVO.setSize(metric.getBackupSize());
                        backupVO.setProtectedSize(metric.getDataSize());
                        if (backupDao.update(backupVO.getId(), backupVO)) {
                            usageBackupDao.updateMetrics(backup);
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
