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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.backup.DeleteBackupPolicyCmd;
import org.apache.cloudstack.api.command.user.backup.AssignBackupPolicyCmd;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupPolicyCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupPoliciesCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProvidersCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupsCmd;
import org.apache.cloudstack.api.command.user.backup.RestoreBackupCmd;
import org.apache.cloudstack.api.command.user.backup.RestoreBackupVolumeCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupPolicyDao;
import org.apache.cloudstack.backup.dao.BackupPolicyVMMapDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

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
    private BackupPolicyVMMapDao backupPolicyVMMapDao;

    @Inject
    private BackupDao backupDao;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private VolumeApiService volumeApiService;

    private static Map<String, BackupProvider> backupProvidersMap = new HashMap<>();
    private List<BackupProvider> backupProviders;

    @Override
    public BackupPolicy addBackupPolicy(Long zoneId, String policyExternalId, String policyName, String policyDescription) {
        BackupProvider provider = getBackupProvider(zoneId);
        if (!provider.isBackupPolicy(policyExternalId)) {
            throw new CloudRuntimeException("Policy " + policyExternalId + " does not exist on provider " + provider.getName() + " on zone " + zoneId);
        }

        BackupPolicyVO policy = new BackupPolicyVO(zoneId, policyExternalId, policyName, policyDescription);
        BackupPolicyVO vo = backupPolicyDao.persist(policy);
        if (vo == null) {
            throw new CloudRuntimeException("Unable to create backup policy: " + policyExternalId + ", name: " + policyName);
        }
        LOG.debug("Successfully created backup policy " + policyName + " mapped to backup provider policy " + policyExternalId);
        return vo;
    }

    @Override
    public boolean assignVMToBackupPolicy(Long zoneId, Long policyId, Long virtualMachineId) {
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(virtualMachineId);
        if (vmInstanceVO == null) {
            throw new CloudRuntimeException("VM " + virtualMachineId + " does not exist");
        }
        BackupPolicyVO policy = backupPolicyDao.findById(policyId);
        if (policy == null) {
            throw new CloudRuntimeException("Policy " + policy + " does not exist");
        }
        String vmUuid = vmInstanceVO.getUuid();
        BackupProvider backupProvider = getBackupProvider(zoneId);
        boolean result = backupProvider.assignVMToBackupPolicy(vmUuid, policy.getUuid());
        if (result) {
            BackupPolicyVMMapVO map = backupPolicyVMMapDao.findByVMId(virtualMachineId);
            if (map != null) {
                backupPolicyVMMapDao.expunge(map.getId());
            }
            map = new BackupPolicyVMMapVO(policy.getId(), virtualMachineId);
            backupPolicyVMMapDao.persist(map);
            LOG.debug("Successfully assigned VM " + virtualMachineId + " to backup policy " + policy.getName());
        } else {
            LOG.debug("Could not assign VM " + virtualMachineId + " to backup policy " + policyId);
        }
        return result;
    }

    @Override
    public List<Backup> listBackups(Long vmId) {
        return backupDao.listByVmId(vmId);
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

    @Override
    public List<BackupPolicy> listBackupPolicies(Long zoneId, Boolean external) {
        return BooleanUtils.isTrue(external) ? listExternalPolicies(zoneId) : listInternalPolicies(zoneId);
    }

    @Override
    public boolean restoreBackup(Long zoneId, Long vmId, Long backupId) {
        BackupProvider backupProvider = getBackupProvider(zoneId);
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " does not exist");
        }
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        return backupProvider.restoreVMFromBackup(vm.getUuid(), backup.getUuid());
    }

    @Override
    public boolean restoreBackupVolumeAndAttachToVM(Long zoneId, Long volumeId, Long vmId, Long backupId) {
        BackupProvider backupProvider = getBackupProvider(zoneId);
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup " + backupId + " does not exist");
        }
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM " + vmId + " does not exist");
        }
        VolumeVO volume = volumeDao.findByIdIncludingRemoved(volumeId);
        if (volume == null) {
            throw new CloudRuntimeException("Volume " + volumeId + " could not be found");
        }
        LOG.debug("Asking provider to restore volume " + volumeId + " from backup " + backupId);
        VolumeTO restoredVolume = backupProvider.restoreVolumeFromBackup(backup.getUuid(), volume.getUuid());
        attachVolumeToVM(restoredVolume, vm);
        return false;
    }

    /**
     * Attach volume to VM
     */
    private void attachVolumeToVM(VolumeTO restoredVolume, VMInstanceVO vm) {
        LOG.debug("Attaching the restored volume to VM " + vm.getId());
        volumeDao.attachVolume(restoredVolume.getId(), vm.getId(), restoredVolume.getDeviceId());
    }

    @Override
    public boolean deleteBackupPolicy(Long policyId) {
        BackupPolicyVO policy = backupPolicyDao.findById(policyId);
        if (policy == null) {
            throw new CloudRuntimeException("Could not find a backup policy with id: " + policyId);
        }
        return backupPolicyDao.expunge(policy.getId());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
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
        String name = BackupProviderPlugin.valueIn(zoneId);
        if (!backupProvidersMap.containsKey(name)) {
            throw new CloudRuntimeException("Could not find a backup provider on zone " + zoneId);
        }
        return backupProvidersMap.get(name);
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListBackupProvidersCmd.class);
        cmdList.add(ListBackupPoliciesCmd.class);
        cmdList.add(ImportBackupPolicyCmd.class);
        cmdList.add(AssignBackupPolicyCmd.class);
        cmdList.add(DeleteBackupPolicyCmd.class);
        cmdList.add(ListBackupsCmd.class);
        cmdList.add(RestoreBackupCmd.class);
        cmdList.add(RestoreBackupVolumeCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{BackupFrameworkEnabled, BackupProviderPlugin};
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
}
