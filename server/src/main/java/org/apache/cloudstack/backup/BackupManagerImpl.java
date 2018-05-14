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

import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.backup.DeleteBackupPolicyCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProviderPoliciesCmd;
import org.apache.cloudstack.api.command.user.backup.AssignBackupPolicyCmd;
import org.apache.cloudstack.api.command.admin.backup.CreateBackupPolicyCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupPoliciesCmd;
import org.apache.cloudstack.api.command.admin.backup.ListBackupProvidersCmd;
import org.apache.cloudstack.api.response.BackupPolicyResponse;
import org.apache.cloudstack.backup.dao.BackupPolicyDao;
import org.apache.cloudstack.framework.backup.BackupPolicy;
import org.apache.cloudstack.framework.backup.BackupProvider;
import org.apache.cloudstack.framework.backup.BackupService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class BackupManagerImpl extends ManagerBase implements BackupManager {
    private static final Logger LOG = Logger.getLogger(BackupManagerImpl.class);

    @Inject
    BackupPolicyDao backupPolicyDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    DataCenterDao dataCenterDao;

    private static Map<String, BackupProvider> backupProvidersMap = new HashMap<>();
    private List<BackupProvider> backupProviders;

    @Override
    public BackupPolicy addBackupPolicy(String policyId, String policyName, Long zoneId) {
        BackupProvider provider = getBackupProvider(zoneId);
        if (!provider.isBackupPolicy(policyId)) {
            throw new CloudRuntimeException("Policy " + policyId + " does not exist on provider " + provider.getName());
        }

        BackupPolicyVO policy = new BackupPolicyVO(policyName, policyId);
        BackupPolicyVO vo = backupPolicyDao.persist(policy);
        if (vo == null) {
            throw new CloudRuntimeException("Unable to create backup policy: " + policyId + ", name: " + policyName);
        }
        LOG.debug("Successfully created backup policy " + policyName + " mapped to backup provider policy " + policyId);
        return vo;
    }

    @Override
    public boolean assignVMToBackupPolicy(String policyUuid, Long virtualMachineId, Long zoneId) {
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(virtualMachineId);
        if (vmInstanceVO == null) {
            throw new CloudRuntimeException("VM " + virtualMachineId + " does not exist");
        }
        String vmUuid = vmInstanceVO.getUuid();
        BackupProvider backupProvider = getBackupProvider(zoneId);
        if (backupProvider == null) {
            throw new CloudRuntimeException("Could not find a backup provider on zone " + zoneId);
        }
        return backupProvider.assignVMToBackupPolicy(vmUuid, policyUuid);
    }

    @Override
    public List<BackupPolicy> listBackupPolicies() {
        return new ArrayList<>(backupPolicyDao.listAll());
    }

    @Override
    public List<BackupPolicy> listBackupProviderPolicies(Long zoneId) {
        BackupProvider backupProvider = getBackupProvider(zoneId);
        return backupProvider.listBackupPolicies();
    }

    @Override
    public boolean deleteBackupPolicy(String policyId) {
        BackupPolicyVO policy = backupPolicyDao.findByUuid(policyId);
        if (policy == null) {
            throw new CloudRuntimeException("Could not find a backup policy with id: " + policyId);
        }
        return backupPolicyDao.expunge(policy.getId());
    }

    @Override
    public BackupPolicyResponse createBackupPolicyResponse(BackupPolicy policyVO) {
        return null;
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
        cmdList.add(CreateBackupPolicyCmd.class);
        cmdList.add(AssignBackupPolicyCmd.class);
        cmdList.add(ListBackupProviderPoliciesCmd.class);
        cmdList.add(DeleteBackupPolicyCmd.class);
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
