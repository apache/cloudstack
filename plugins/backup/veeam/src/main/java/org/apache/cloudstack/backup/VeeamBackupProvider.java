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

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.backup.Backup.Metric;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.veeam.VeeamClient;
import org.apache.cloudstack.backup.veeam.api.Job;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.dc.VmwareDatacenter;
import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMap;
import com.cloud.dc.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

public class VeeamBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    public static final String BACKUP_IDENTIFIER = "-CSBKP-";

    public ConfigKey<String> VeeamUrl = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.url", "https://localhost:9398/api/",
            "The Veeam backup and recovery URL.", true, ConfigKey.Scope.Zone);

    public ConfigKey<Integer> VeeamVersion = new ConfigKey<>("Advanced", Integer.class,
            "backup.plugin.veeam.version", "0",
            "The version of Veeam backup and recovery. CloudStack will get Veeam server version via PowerShell commands if it is 0 or not set", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> VeeamUsername = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.username", "administrator",
            "The Veeam backup and recovery username.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> VeeamPassword = new ConfigKey<>("Secure", String.class,
            "backup.plugin.veeam.password", "",
            "The Veeam backup and recovery password.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Boolean> VeeamValidateSSLSecurity = new ConfigKey<>("Advanced", Boolean.class, "backup.plugin.veeam.validate.ssl", "false",
            "When set to true, this will validate the SSL certificate when connecting to https/ssl enabled Veeam API service.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Integer> VeeamApiRequestTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.veeam.request.timeout", "300",
            "The Veeam B&R API request timeout in seconds.", true, ConfigKey.Scope.Zone);

    private static ConfigKey<Integer> VeeamRestoreTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.veeam.restore.timeout", "600",
            "The Veeam B&R API restore backup timeout in seconds.", true, ConfigKey.Scope.Zone);

    private static ConfigKey<Integer> VeeamTaskPollInterval = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.veeam.task.poll.interval", "5",
            "The time interval in seconds when the management server polls for Veeam task status.", true, ConfigKey.Scope.Zone);

    private static ConfigKey<Integer> VeeamTaskPollMaxRetry = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.veeam.task.poll.max.retry", "120",
            "The max number of retrying times when the management server polls for Veeam task status.", true, ConfigKey.Scope.Zone);

    @Inject
    private VmwareDatacenterZoneMapDao vmwareDatacenterZoneMapDao;
    @Inject
    private VmwareDatacenterDao vmwareDatacenterDao;
    @Inject
    private BackupDao backupDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private AgentManager agentMgr;
    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private VolumeDao volumeDao;

    protected VeeamClient getClient(final Long zoneId) {
        try {
            return new VeeamClient(VeeamUrl.valueIn(zoneId), VeeamVersion.valueIn(zoneId), VeeamUsername.valueIn(zoneId), VeeamPassword.valueIn(zoneId),
                    VeeamValidateSSLSecurity.valueIn(zoneId), VeeamApiRequestTimeout.valueIn(zoneId), VeeamRestoreTimeout.valueIn(zoneId),
                    VeeamTaskPollInterval.valueIn(zoneId), VeeamTaskPollMaxRetry.valueIn(zoneId));
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Failed to parse Veeam API URL: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Failed to build Veeam API client due to: ", e);
        }
        throw new CloudRuntimeException("Failed to build Veeam API client");
    }

    public List<BackupOffering> listBackupOfferings(final Long zoneId) {
        List<BackupOffering> policies = new ArrayList<>();
        for (final BackupOffering policy : getClient(zoneId).listJobs()) {
            if (!policy.getName().contains(BACKUP_IDENTIFIER)) {
                policies.add(policy);
            }
        }
        return policies;
    }

    @Override
    public boolean isValidProviderOffering(final Long zoneId, final String uuid) {
        List<BackupOffering> policies = listBackupOfferings(zoneId);
        if (CollectionUtils.isEmpty(policies)) {
            return false;
        }
        for (final BackupOffering policy : policies) {
            if (policy.getExternalId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    private VmwareDatacenter findVmwareDatacenterForVM(final VirtualMachine vm) {
        if (vm == null || vm.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
            throw new CloudRuntimeException("The Veeam backup provider is only applicable for VMware VMs");
        }
        final VmwareDatacenterZoneMap zoneMap = vmwareDatacenterZoneMapDao.findByZoneId(vm.getDataCenterId());
        if (zoneMap == null) {
            throw new CloudRuntimeException("Failed to find a mapped VMware datacenter for zone id:" + vm.getDataCenterId());
        }
        final VmwareDatacenter vmwareDatacenter = vmwareDatacenterDao.findById(zoneMap.getVmwareDcId());
        if (vmwareDatacenter == null) {
            throw new CloudRuntimeException("Failed to find a valid VMware datacenter mapped for zone id:" + vm.getDataCenterId());
        }
        return vmwareDatacenter;
    }

    private String getGuestBackupName(final String instanceName, final String uuid) {
        return String.format("%s%s%s", instanceName, BACKUP_IDENTIFIER, uuid);
    }

    @Override
    public boolean assignVMToBackupOffering(final VirtualMachine vm, final BackupOffering backupOffering) {
        final VeeamClient client = getClient(vm.getDataCenterId());
        final Job parentJob = client.listJob(backupOffering.getExternalId());
        final String clonedJobName = getGuestBackupName(vm.getInstanceName(), vm.getUuid());

        if (!client.cloneVeeamJob(parentJob, clonedJobName)) {
            logger.error("Failed to clone pre-defined Veeam job (backup offering) for backup offering [id: {}, name: {}] but will check the list of jobs again if it was eventually succeeded.", backupOffering.getExternalId(), backupOffering.getName());
        }

        for (final BackupOffering job : client.listJobs()) {
            if (job.getName().equals(clonedJobName)) {
                final Job clonedJob = client.listJob(job.getExternalId());
                if (BooleanUtils.isTrue(clonedJob.getScheduleConfigured()) && !clonedJob.getScheduleEnabled()) {
                    client.toggleJobSchedule(clonedJob.getId());
                }
                logger.debug("Veeam job (backup offering) for backup offering [id: {}, name: {}] found, now trying to assign the VM to the job.", backupOffering.getExternalId(), backupOffering.getName());
                final VmwareDatacenter vmwareDC = findVmwareDatacenterForVM(vm);
                if (client.addVMToVeeamJob(job.getExternalId(), vm.getInstanceName(), vmwareDC.getVcenterHost())) {
                    ((VMInstanceVO) vm).setBackupExternalId(job.getExternalId());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeVMFromBackupOffering(final VirtualMachine vm) {
        final VeeamClient client = getClient(vm.getDataCenterId());
        final VmwareDatacenter vmwareDC = findVmwareDatacenterForVM(vm);
        if (vm.getBackupExternalId() == null) {
            throw new CloudRuntimeException("The VM does not have a backup job assigned.");
        }
        try {
            if (!client.removeVMFromVeeamJob(vm.getBackupExternalId(), vm.getInstanceName(), vmwareDC.getVcenterHost())) {
                logger.warn("Failed to remove VM from Veeam Job id: " + vm.getBackupExternalId());
            }
        } catch (Exception e) {
            logger.debug("VM was removed from the job so could not remove again, trying to delete the veeam job now.", e);
        }

        final String clonedJobName = getGuestBackupName(vm.getInstanceName(), vm.getUuid());
        if (!client.deleteJobAndBackup(clonedJobName)) {
            logger.warn("Failed to remove Veeam job and backup for job: " + clonedJobName);
            throw new CloudRuntimeException("Failed to delete Veeam B&R job and backup, an operation may be in progress. Please try again after some time.");
        }
        client.syncBackupRepository();
        return true;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return true;
    }

    @Override
    public boolean takeBackup(final VirtualMachine vm) {
        final VeeamClient client = getClient(vm.getDataCenterId());
        return client.startBackupJob(vm.getBackupExternalId());
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException(String.format("Could not find any VM associated with the Backup [uuid: %s, name: %s, externalId: %s].", backup.getUuid(), backup.getName(), backup.getExternalId()));
        }
        if (!forced) {
            logger.debug(String.format("Veeam backup provider does not have a safe way to remove a single restore point, which results in all backup chain being removed. "
                    + "More information about this limitation can be found in the links: [%s, %s].", "https://forums.veeam.com/powershell-f26/removing-a-single-restorepoint-t21061.html",
                    "https://helpcenter.veeam.com/docs/backup/vsphere/retention_separate_vms.html?ver=110"));
            throw new CloudRuntimeException("Veeam backup provider does not have a safe way to remove a single restore point, which results in all backup chain being removed. "
                    + "Use forced:true to skip this verification and remove the complete backup chain.");
        }
        VeeamClient client = getClient(vm.getDataCenterId());
        boolean result = client.deleteBackup(backup.getExternalId());
        if (BooleanUtils.isFalse(result)) {
            return false;
        }

        client.syncBackupRepository();

        List<Backup> allBackups = backupDao.listByVmId(backup.getZoneId(), backup.getVmId());
        for (Backup b : allBackups) {
            if (b.getId() != backup.getId()) {
                backupDao.remove(b.getId());
            }
        }
        return result;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        final String restorePointId = backup.getExternalId();
        try {
            return getClient(vm.getDataCenterId()).restoreFullVM(vm.getInstanceName(), restorePointId);
        } catch (Exception ex) {
            logger.error(String.format("Failed to restore Full VM due to: %s. Retrying after some preparation", ex.getMessage()));
            prepareForBackupRestoration(vm);
            return getClient(vm.getDataCenterId()).restoreFullVM(vm.getInstanceName(), restorePointId);
        }
    }

    private void prepareForBackupRestoration(VirtualMachine vm) {
        if (!Hypervisor.HypervisorType.VMware.equals(vm.getHypervisorType())) {
            return;
        }
        logger.info("Preparing for restoring VM " + vm);
        PrepareForBackupRestorationCommand command = new PrepareForBackupRestorationCommand(vm.getInstanceName());
        Long hostId = virtualMachineManager.findClusterAndHostIdForVm(vm.getId()).second();
        if (hostId == null) {
            throw new CloudRuntimeException("Cannot find a host to prepare for restoring VM " + vm);
        }
        try {
            Answer answer = agentMgr.easySend(hostId, command);
            if (answer != null && answer.getResult()) {
                logger.info("Succeeded to prepare for restoring VM " + vm);
            } else {
                throw new CloudRuntimeException(String.format("Failed to prepare for restoring VM %s. details: %s", vm,
                        (answer != null ? answer.getDetails() : null)));
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to prepare for restoring VM %s due to exception %s", vm, e));
        }
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        final Long zoneId = backup.getZoneId();
        final String restorePointId = backup.getExternalId();
        return getClient(zoneId).restoreVMToDifferentLocation(restorePointId, hostIp, dataStoreUuid);
    }

    @Override
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(final Long zoneId, final List<VirtualMachine> vms) {
        final Map<VirtualMachine, Backup.Metric> metrics = new HashMap<>();
        if (CollectionUtils.isEmpty(vms)) {
            logger.warn("Unable to get VM Backup Metrics because the list of VMs is empty.");
            return metrics;
        }

        List<String> vmUuids = vms.stream().filter(Objects::nonNull).map(VirtualMachine::getUuid).collect(Collectors.toList());
        logger.debug(String.format("Get Backup Metrics for VMs: [%s].", String.join(", ", vmUuids)));

        final Map<String, Backup.Metric> backendMetrics = getClient(zoneId).getBackupMetrics();
        for (final VirtualMachine vm : vms) {
            if (vm == null || !backendMetrics.containsKey(vm.getUuid())) {
                continue;
            }

            Metric metric = backendMetrics.get(vm.getUuid());
            logger.debug("Metrics for VM [{}] is [backup size: {}, data size: {}].", vm,
                    metric.getBackupSize(), metric.getDataSize());
            metrics.put(vm, metric);
        }
        return metrics;
    }

    private List<Backup.RestorePoint> listRestorePoints(VirtualMachine vm) {
        String backupName = getGuestBackupName(vm.getInstanceName(), vm.getUuid());
        return getClient(vm.getDataCenterId()).listRestorePoints(backupName, vm.getInstanceName());
    }

    private Backup checkAndUpdateIfBackupEntryExistsForRestorePoint(List<Backup> backupsInDb, Backup.RestorePoint restorePoint, Backup.Metric metric) {
        for (final Backup backup : backupsInDb) {
            if (restorePoint.getId().equals(backup.getExternalId())) {
                if (metric != null) {
                    logger.debug("Update backup with [id: {}, uuid: {}, name: {}, external id: {}] from [size: {}, protected size: {}] to [size: {}, protected size: {}].",
                            backup.getId(), backup.getUuid(), backup.getName(), backup.getExternalId(), backup.getSize(), backup.getProtectedSize(), metric.getBackupSize(), metric.getDataSize());

                    ((BackupVO) backup).setSize(metric.getBackupSize());
                    ((BackupVO) backup).setProtectedSize(metric.getDataSize());
                    backupDao.update(backup.getId(), ((BackupVO) backup));
                }
                return backup;
            }
        }
        return null;
    }

    @Override
    public void syncBackups(VirtualMachine vm, Backup.Metric metric) {
        List<Backup.RestorePoint> restorePoints = listRestorePoints(vm);
        if (CollectionUtils.isEmpty(restorePoints)) {
            logger.debug("Can't find any restore point to VM: {}", vm);
            return;
        }
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                final List<Backup> backupsInDb = backupDao.listByVmId(null, vm.getId());
                final List<Long> removeList = backupsInDb.stream().map(InternalIdentity::getId).collect(Collectors.toList());
                for (final Backup.RestorePoint restorePoint : restorePoints) {
                    if (!(restorePoint.getId() == null || restorePoint.getType() == null || restorePoint.getCreated() == null)) {
                        Backup existingBackupEntry = checkAndUpdateIfBackupEntryExistsForRestorePoint(backupsInDb, restorePoint, metric);
                        if (existingBackupEntry != null) {
                            removeList.remove(existingBackupEntry.getId());
                            continue;
                        }

                        BackupVO backup = new BackupVO();
                        backup.setVmId(vm.getId());
                        backup.setExternalId(restorePoint.getId());
                        backup.setType(restorePoint.getType());
                        backup.setDate(restorePoint.getCreated());
                        backup.setStatus(Backup.Status.BackedUp);
                        if (metric != null) {
                            backup.setSize(metric.getBackupSize());
                            backup.setProtectedSize(metric.getDataSize());
                        }
                        backup.setBackupOfferingId(vm.getBackupOfferingId());
                        backup.setAccountId(vm.getAccountId());
                        backup.setDomainId(vm.getDomainId());
                        backup.setZoneId(vm.getDataCenterId());
                        backup.setBackedUpVolumes(BackupManagerImpl.createVolumeInfoFromVolumes(volumeDao.findByInstance(vm.getId())));

                        logger.debug("Creating a new entry in backups: [id: {}, uuid: {}, name: {}, vm_id: {}, external_id: {}, type: {}, date: {}, backup_offering_id: {}, account_id: {}, "
                                + "domain_id: {}, zone_id: {}].", backup.getId(), backup.getUuid(), backup.getName(), backup.getVmId(), backup.getExternalId(), backup.getType(), backup.getDate(), backup.getBackupOfferingId(), backup.getAccountId(), backup.getDomainId(), backup.getZoneId());
                        backupDao.persist(backup);

                        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_BACKUP_CREATE,
                                String.format("Created backup %s for VM ID: %s", backup.getUuid(), vm.getUuid()),
                                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(),0);
                    }
                }
                for (final Long backupIdToRemove : removeList) {
                    logger.warn(String.format("Removing backup with ID: [%s].", backupIdToRemove));
                    backupDao.remove(backupIdToRemove);
                }
            }
        });
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                VeeamUrl,
                VeeamVersion,
                VeeamUsername,
                VeeamPassword,
                VeeamValidateSSLSecurity,
                VeeamApiRequestTimeout,
                VeeamRestoreTimeout,
                VeeamTaskPollInterval,
                VeeamTaskPollMaxRetry
        };
    }

    @Override
    public String getName() {
        return "veeam";
    }

    @Override
    public String getDescription() {
        return "Veeam Backup Plugin";
    }
}
