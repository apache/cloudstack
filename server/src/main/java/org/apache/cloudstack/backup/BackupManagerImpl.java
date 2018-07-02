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

import com.cloud.storage.ScopeType;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.utils.Pair;
import org.apache.cloudstack.api.command.admin.backup.DeleteBackupPolicyCmd;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupPolicyCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProvidersCmd;
import org.apache.cloudstack.api.command.admin.vm.ImportVMCmdByAdmin;
import org.apache.cloudstack.api.command.user.backup.CreateVMBackupCmd;
import org.apache.cloudstack.api.command.user.backup.DeleteVMBackupCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupPoliciesCmd;
import org.apache.cloudstack.api.command.user.backup.ListVMBackupRestorePoints;
import org.apache.cloudstack.api.command.user.backup.ListVMBackupsCmd;
import org.apache.cloudstack.api.command.user.backup.RestoreVMFromBackupCmd;
import org.apache.cloudstack.api.command.user.backup.RestoreVolumeFromBackupAndAttachToVMCmd;
import org.apache.cloudstack.api.command.user.backup.StartVMBackupCmd;
import org.apache.cloudstack.backup.dao.BackupPolicyDao;
import org.apache.cloudstack.backup.dao.VMBackupDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.lang.BooleanUtils;
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
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.usage.dao.UsageVMBackupDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
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
    private BackupPolicyDao backupPolicyDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private AccountService accountService;
    @Inject
    private VMBackupDao backupDao;
    @Inject
    private UsageVMBackupDao usageVMBackupDao;
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
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_IMPORT_POLICY, eventDescription = "importing backup policy", async = true)
    public BackupPolicy importBackupPolicy(final Long zoneId, final String policyExternalId,
                                           final String policyName, final String policyDescription) {
        final BackupProvider provider = getBackupProvider(zoneId);
        if (!provider.isBackupPolicy(zoneId, policyExternalId)) {
            throw new CloudRuntimeException("Policy " + policyExternalId + " does not exist on provider " + provider.getName() + " on zone " + zoneId);
        }

        final BackupPolicyVO policy = new BackupPolicyVO(zoneId, policyExternalId, policyName, policyDescription);
        final BackupPolicyVO savedPolicy = backupPolicyDao.persist(policy);
        if (savedPolicy == null) {
            throw new CloudRuntimeException("Unable to create backup policy: " + policyExternalId + ", name: " + policyName);
        }
        LOG.debug("Successfully created backup policy " + policyName + " mapped to backup provider policy " + policyExternalId);
        return savedPolicy;
    }

    /**
     * List external backup policies for the Backup and Recovery provider registered in the zone zoneId
     */
    private List<BackupPolicy> listExternalPolicies(Long zoneId) {
        Account account = CallContext.current().getCallingAccount();
        if (!accountService.isRootAdmin(account.getId())) {
            throw new PermissionDeniedException("Parameter external can only be specified by a Root Admin, permission denied");
        }
        BackupProvider backupProvider = getBackupProvider(zoneId);
        LOG.debug("Listing external backup policies for the backup provider registered in zone " + zoneId);
        return backupProvider.listBackupPolicies(zoneId);
    }

    /**
     * List imported backup policies in the zone zoneId
     */
    private List<BackupPolicy> listInternalPolicies(Long zoneId) {
        LOG.debug("Listing imported backup policies on zone " + zoneId);
        return backupPolicyDao.listByZone(zoneId);
    }

    /**
     * List imported backup policy with id policyId
     */
    private List<BackupPolicy> listInternalPolicyById(Long policyId) {
        BackupPolicyVO policy = backupPolicyDao.findById(policyId);
        if (policy == null) {
            throw new CloudRuntimeException("Policy " + policyId + " does not exist");
        }
        LOG.debug("Listing imported backup policy with id: " + policyId);
        return Collections.singletonList(policy);
    }

    @Override
    public List<BackupPolicy> listBackupPolicies(final Long zoneId, final Boolean external, final Long policyId) {
        if (policyId != null) {
            return listInternalPolicyById(policyId);
        } else {
            return BooleanUtils.isTrue(external) ? listExternalPolicies(zoneId) : listInternalPolicies(zoneId);
        }
    }

    @Override
    public List<VMBackup> listVMBackups(final Long vmId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            return new ArrayList<>(backupDao.listAll());
        }
        return backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_CREATE, eventDescription = "creating VM backup", async = true)
    public VMBackup createBackup(final String name, final String description, final Long vmId, final Long policyId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM does not exist");
        }
        final BackupPolicyVO policy = backupPolicyDao.findById(policyId);
        if (policy == null) {
            throw new CloudRuntimeException("Policy does not exist");
        }
        VMBackupVO backup = new VMBackupVO(name, description, policyId, vmId, VMBackup.Status.Allocated, vm.getAccountId(), vm.getDataCenterId());
        setBackupVolumes(backup, vm);
        backup = backupDao.persist(backup);
        if (backup == null) {
            throw new CloudRuntimeException("Failed to save backup object in database");
        }
        final BackupProvider backupProvider = getBackupProvider(vm.getDataCenterId());
        backup = (VMBackupVO) backupProvider.createVMBackup(policy, vm, backup);
        if (backup == null) {
            throw new CloudRuntimeException("Backup provider failed to create backup for VM: " + vm.getUuid());
        }
        if (backupDao.update(backup.getId(), backup)) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_CREATE, vm.getAccountId(), vm.getDataCenterId(), backup.getId(),
                    vm.getUuid(), backup.getPolicyId(), backup.getVmId(), null,
                    VMBackup.class.getSimpleName(), backup.getUuid());
        }
        return backup;
    }

    private void setBackupVolumes(VMBackupVO backup, VMInstanceVO vm) {
        List<VolumeVO> vmVolumes = volumeDao.findByInstance(vm.getId());
        backup.setBackedUpVolumes(vmVolumes);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_START, eventDescription = "starting VM backup", async = true)
    public boolean startVMBackup(final Long vmBackupId) {
        final VMBackup vmBackup = backupDao.findById(vmBackupId);
        if (vmBackup == null) {
            throw new CloudRuntimeException("VM Backup id " + vmBackupId + " does not exist");
        }
        final BackupProvider backupProvider = getBackupProvider(vmBackup.getZoneId());
        return backupProvider.startBackup(vmBackup);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_DELETE, eventDescription = "deleting VM backup", async = true)
    public boolean deleteBackup(final Long backupId) {
        final VMBackupVO backup = backupDao.findByIdIncludingRemoved(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        final Long vmId = backup.getVmId();
        final VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " does not exist");
        }

        final BackupProvider backupProvider = getBackupProvider(backup.getZoneId());
        boolean result = backupProvider.removeVMBackup(vm, backup);
        if (result) {
            backup.setStatus(VMBackup.Status.Expunged);
            if (backupDao.update(backup.getId(), backup)) {
                backupDao.remove(backup.getId());
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_DELETE, vm.getAccountId(), vm.getDataCenterId(), backup.getId(),
                        vm.getUuid(), backup.getPolicyId(), backup.getVmId(), null,
                        VMBackup.class.getSimpleName(), backup.getUuid());
                return true;
            }
        }
        // FIXME: fsm to deal with GC+deletion
        backup.setStatus(VMBackup.Status.Removed);
        if (backupDao.update(backup.getId(), backup)) {
            backupDao.remove(backupId);
            return true;
        }

        return result;
    }

    public boolean importVM(long zoneId, long domainId, long accountId, long userId,
                            String vmInternalName, Hypervisor.HypervisorType hypervisorType, VMBackup backup) {
        //TODO: Remove it from the backup manager interface
        HypervisorGuru guru = hypervisorGuruManager.getGuru(hypervisorType);
        try {
            guru.importVirtualMachine(zoneId, domainId, accountId, userId, vmInternalName, backup);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CloudRuntimeException("Error during vm import: " + e.getMessage());
        }
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_RESTORE, eventDescription = "restoring VM from backup", async = true)
    public boolean restoreVMFromBackup(final Long backupId, final String restorePointId) {
        VMBackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        Long vmId = backup.getVmId();
        BackupProvider backupProvider = getBackupProvider(backup.getZoneId());
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " does not exist");
        }
        if (!vm.getState().equals(VirtualMachine.State.Stopped)) {
            throw new CloudRuntimeException("VM should be stopped before being restored from backup");
        }
        if (!backupProvider.restoreVMFromBackup(vm, backup.getExternalId(), restorePointId)) {
            throw new CloudRuntimeException("Error restoring VM " + vm.getId() + " from backup " + backup.getId());
        }
        importVM(vm.getDataCenterId(), vm.getDomainId(), vm.getAccountId(), vm.getUserId(),
                vm.getInstanceName(), vm.getHypervisorType(), backup);
        return true;
    }

    private VMBackup.VolumeInfo getVolumeInfo(List<VMBackup.VolumeInfo> backedUpVolumes, String volumeUuid) {
        for (VMBackup.VolumeInfo volInfo : backedUpVolumes) {
            if (volInfo.getUuid().equals(volumeUuid)) {
                return volInfo;
            }
        }
        return null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_BACKUP_RESTORE, eventDescription = "restoring VM from backup", async = true)
    public boolean restoreBackupVolumeAndAttachToVM(final String backedUpVolumeUuid, final Long vmId, final Long backupId, final String restorePointId) throws Exception {
        VMBackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        BackupProvider backupProvider = getBackupProvider(backup.getZoneId());

        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " does not exist");
        }

        Pair<String, String> restoreInfo = getRestoreVolumeHostAndDatastore(vm);
        String hostIp = restoreInfo.first();
        String datastoreUuid = restoreInfo.second();

        LOG.debug("Asking provider to restore volume " + backedUpVolumeUuid + " from backup " + backupId +
                " and restore point " + restorePointId + " and attach it to VM: " + vm.getUuid());
        Pair<Boolean, String> result = backupProvider.restoreBackedUpVolume(backup.getZoneId(), backup.getUuid(),
                                                        restorePointId, backedUpVolumeUuid, hostIp, datastoreUuid);
        if (!result.first()) {
            throw new CloudRuntimeException("Error restoring volume " + backedUpVolumeUuid);
        }
        if (!attachVolumeToVM(backup.getZoneId(), result.second(), backup.getBackedUpVolumes(),
                            backedUpVolumeUuid, vm, datastoreUuid)) {
            throw new CloudRuntimeException("Error attaching volume " + backedUpVolumeUuid + " to VM " + vm.getUuid());
        }
        return true;
    }

    /**
     * Get the pair: hostIp, datastoreUuid in which to restore the volume, based on the VM to be attached information
     */
    private Pair<String, String> getRestoreVolumeHostAndDatastore(VMInstanceVO vm) {
        List<VolumeVO> rootVmVolume = volumeDao.findByInstanceAndType(vm.getId(), Volume.Type.ROOT);
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
    private boolean attachVolumeToVM(Long zoneId, String restoredVolumeLocation, List<VMBackup.VolumeInfo> backedUpVolumes,
                                     String volumeUuid, VMInstanceVO vm, String datastoreUuid) throws Exception {
        HypervisorGuru guru = hypervisorGuruManager.getGuru(vm.getHypervisorType());
        VMBackup.VolumeInfo volumeInfo = getVolumeInfo(backedUpVolumes, volumeUuid);
        StoragePoolVO pool = primaryDataStoreDao.findByUuid(datastoreUuid);

        LOG.debug("Attaching the restored volume to VM " + vm.getId());
        try {
            return guru.attachRestoredVolumeToVirtualMachine(zoneId, restoredVolumeLocation, volumeInfo, vm, pool.getId());
        } catch (Exception e) {
            throw new CloudRuntimeException("Error attach restored volume to VM " + vm.getUuid());
        }
    }

    @Override
    public boolean deleteBackupPolicy(final Long policyId) {
        BackupPolicyVO policy = backupPolicyDao.findById(policyId);
        if (policy == null) {
            throw new CloudRuntimeException("Could not find a backup policy with id: " + policyId);
        }
        return backupPolicyDao.expunge(policy.getId());
    }

    @Override
    public List<VMBackup.RestorePoint> listVMBackupRestorePoints(final Long backupId) {
        VMBackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Could not find backup " + backupId);
        }
        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("Could not find VM: " + backup.getVmId());
        }
        BackupProvider backupProvider = getBackupProvider(backup.getZoneId());
        return backupProvider.listVMBackupRestorePoints(backup.getUuid(), vm);
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
        // VM Backup Policy APIs
        cmdList.add(ListBackupProvidersCmd.class);
        cmdList.add(ListBackupPoliciesCmd.class);
        cmdList.add(ImportBackupPolicyCmd.class);
        cmdList.add(DeleteBackupPolicyCmd.class);
        // VM Backup APIs
        cmdList.add(ListVMBackupsCmd.class);
        cmdList.add(CreateVMBackupCmd.class);
        cmdList.add(StartVMBackupCmd.class);
        cmdList.add(DeleteVMBackupCmd.class);
        cmdList.add(RestoreVMFromBackupCmd.class);
        cmdList.add(RestoreVolumeFromBackupAndAttachToVMCmd.class);
        cmdList.add(ImportVMCmdByAdmin.class);
        cmdList.add(ListVMBackupRestorePoints.class);
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
                    for (final VMBackup backup : backupDao.listByZoneAndState(dataCenter.getId(), VMBackup.Status.Removed)) {
                        backupManager.deleteBackup(backup.getId());
                    }

                    // Sync backup size usages
                    final List<VMBackup> backups = backupDao.listByZoneAndState(dataCenter.getId(), null);
                    if (backups.isEmpty()) {
                        continue;
                    }
                    final BackupProvider backupProvider = getBackupProvider(dataCenter.getId());
                    final Map<VMBackup, VMBackup.Metric> metrics = backupProvider.getBackupMetrics(dataCenter.getId(), backups);
                    for (final VMBackup backup : metrics.keySet()) {
                        final VMBackup.Metric metric = metrics.get(backup);
                        final VMBackupVO backupVO = (VMBackupVO) backup;
                        backupVO.setSize(metric.getBackupSize());
                        backupVO.setProtectedSize(metric.getDataSize());
                        if (backupDao.update(backupVO.getId(), backupVO)) {
                            usageVMBackupDao.updateMetrics(backup);
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
