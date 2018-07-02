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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.utils.Pair;
import org.apache.cloudstack.backup.veeam.VeeamClient;
import org.apache.cloudstack.backup.veeam.api.Job;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.vmware.VmwareDatacenter;
import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMap;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;

public class VeeamBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    private static final Logger LOG = Logger.getLogger(VeeamBackupProvider.class);
    public static final String BACKUP_IDENTIFIER = "-CSBKP-";

    public ConfigKey<String> VeeamUrl = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.url",
            "http://localhost:9399/api/",
            "The Veeam backup and recovery URL.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> VeeamUsername = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.host.username",
            "administrator",
            "The Veeam backup and recovery username.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> VeeamPassword = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.host.password",
            "P@ssword123",
            "The Veeam backup and recovery password.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Boolean> VeeamValidateSSLSecurity = new ConfigKey<>("Advanced", Boolean.class, "backup.plugin.veeam.validate.ssl", "true",
            "When set to true, this will validate the SSL certificate when connecting to https/ssl enabled Veeam API service.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Integer> VeeamApiRequestTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.veeam.request.timeout", "300",
            "The Veeam B&R API request timeout in seconds.", true, ConfigKey.Scope.Zone);

    @Inject
    private VmwareDatacenterZoneMapDao vmwareDatacenterZoneMapDao;
    @Inject
    private VmwareDatacenterDao vmwareDatacenterDao;

    private VeeamClient getClient(final Long zoneId) {
        try {
            return new VeeamClient(VeeamUrl.valueIn(zoneId), VeeamUsername.valueIn(zoneId), VeeamPassword.valueIn(zoneId),
                VeeamValidateSSLSecurity.valueIn(zoneId), VeeamApiRequestTimeout.valueIn(zoneId));
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Failed to parse Veeam API URL: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Failed to build Veeam API client due to: ", e);
        }
        throw new CloudRuntimeException("Failed to build Veeam API client");
    }

    public List<BackupPolicy> listBackupPolicies(final Long zoneId) {
        List<BackupPolicy> policies = new ArrayList<>();
        for (final BackupPolicy policy : getClient(zoneId).listJobs()) {
            if (!policy.getName().contains(BACKUP_IDENTIFIER)) {
                policies.add(policy);
            }
        }
        return policies;
    }

    @Override
    public boolean isBackupPolicy(final Long zoneId, final String uuid) {
        List<BackupPolicy> policies = listBackupPolicies(zoneId);
        if (CollectionUtils.isEmpty(policies)) {
            return false;
        }
        for (final BackupPolicy policy : policies) {
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

    public boolean addVMToBackupPolicy(final BackupPolicy policy, final VirtualMachine vm) {
        final VmwareDatacenter vmwareDatacenter = findVmwareDatacenterForVM(vm);
        return getClient(vm.getDataCenterId()).removeVMFromVeeamJob(policy.getExternalId(), vm.getInstanceName(), vmwareDatacenter.getVcenterHost());
    }

    private String getGuestBackupName(final String instanceName, final String uuid) {
        return String.format("%s%s%s", instanceName, BACKUP_IDENTIFIER, uuid);
    }

    @Override
    public VMBackup createVMBackup(final BackupPolicy policy, final VirtualMachine vm, final VMBackup backup) {
        final VeeamClient client = getClient(vm.getDataCenterId());
        final Job parentJob = client.listJob(policy.getExternalId());
        final String clonedJobName = getGuestBackupName(vm.getInstanceName(), backup.getUuid());
        if (client.cloneVeeamJob(parentJob, clonedJobName)) {
            for (BackupPolicy job : client.listJobs()) {
                if (job.getName().equals(clonedJobName)) {
                    final Job clonedJob = client.listJob(job.getExternalId());
                    if (clonedJob.getScheduleConfigured() && !clonedJob.getScheduleEnabled()) {
                        client.toggleJobSchedule(clonedJob.getId());
                    }
                    final VmwareDatacenter vmwareDC = findVmwareDatacenterForVM(vm);
                    if (client.addVMToVeeamJob(job.getExternalId(), vm.getInstanceName(), vmwareDC.getVcenterHost())) {
                        VMBackupVO vmBackup = ((VMBackupVO) backup);
                        vmBackup.setStatus(VMBackup.Status.Queued);
                        vmBackup.setExternalId(job.getExternalId());
                        vmBackup.setCreated(new Date());
                        if (!startBackup(vmBackup)) {
                            LOG.warn("Veeam provider failed to start backup job after creating a new backup for VM id: " + vm.getId());
                        }
                        return vmBackup;
                    }
                }
            }
        } else {
            LOG.error("Failed to clone pre-defined Veeam job (backup policy) for policy id: " + policy.getExternalId());
        }
        ((VMBackupVO) backup).setStatus(VMBackup.Status.Error);
        return backup;
    }

    @Override
    public boolean removeVMBackup(final VirtualMachine vm, final VMBackup backup) {
        final VeeamClient client = getClient(vm.getDataCenterId());
        final VmwareDatacenter vmwareDC = findVmwareDatacenterForVM(vm);
        if (!client.removeVMFromVeeamJob(backup.getExternalId(), vm.getInstanceName(), vmwareDC.getVcenterHost())) {
            LOG.warn("Failed to remove VM from Veeam Job id: " + backup.getExternalId());
        }
        final String clonedJobName = getGuestBackupName(vm.getInstanceName(), backup.getUuid());
        return client.deleteJobAndBackup(clonedJobName) && client.listJob(clonedJobName) == null;
    }

    @Override
    public boolean startBackup(final VMBackup vmBackup) {
        final VeeamClient client = getClient(vmBackup.getZoneId());
        return client.startBackupJob(vmBackup.getExternalId());
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, String backupUuid, String restorePointId) {
        return getClient(vm.getDataCenterId()).restoreFullVM(vm.getInstanceName(), restorePointId);
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(long zoneId, String backupUuid, String restorePointId,
                                                       String volumeUuid, String hostIp, String dataStoreUuid) {
        return getClient(zoneId).restoreVMToDifferentLocation(restorePointId, hostIp, dataStoreUuid);
    }

    @Override
    public List<VMBackup> listVMBackups(Long zoneId, VirtualMachine vm) {
        //return getClient(zoneId).listAllBackups();
        return null;
    }

    @Override
    public Map<VMBackup, VMBackup.Metric> getBackupMetrics(final Long zoneId, final List<VMBackup> backupList) {
        final Map<VMBackup, VMBackup.Metric> metrics = new HashMap<>();
        final Map<String, VMBackup.Metric> backendMetrics = getClient(zoneId).getBackupMetrics();
        for (final VMBackup backup : backupList) {
            if (!backendMetrics.containsKey(backup.getUuid())) {
                continue;
            }
            metrics.put(backup, backendMetrics.get(backup.getUuid()));
        }
        return metrics;
    }

    @Override
    public List<VMBackup.RestorePoint> listVMBackupRestorePoints(String backupUuid, VirtualMachine vm) {
        String backupName = getGuestBackupName(vm.getInstanceName(), backupUuid);
        return getClient(vm.getDataCenterId()).listRestorePoints(backupName, vm.getInstanceName());
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                VeeamUrl,
                VeeamUsername,
                VeeamPassword,
                VeeamValidateSSLSecurity,
                VeeamApiRequestTimeout
        };
    }

    @Override
    public String getName() {
        return "veeam";
    }

    @Override
    public String getDescription() {
        return "Veeam B&R Plugin";
    }
}
