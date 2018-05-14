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

import org.apache.cloudstack.api.command.user.backup.CreateBackupPolicyCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupPoliciesCmd;
import org.apache.cloudstack.api.command.user.backup.ListBackupProvidersCmd;
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
import com.google.common.base.Strings;

@Component
public class BackupManagerImpl extends ManagerBase implements BackupManager {
    private static final Logger LOG = Logger.getLogger(BackupManagerImpl.class);

    @Inject
    BackupPolicyDao backupPolicyDao;

    @Inject
    DataCenterDao dataCenterDao;

    private static Map<String, BackupProvider> backupProvidersMap = new HashMap<>();
    private List<BackupProvider> backupProviders;

    @Override
    public BackupPolicy addBackupPolicy(String policyId, String policyName, String providerId) {
        BackupProvider provider = getBackupProvider(null, null);
        boolean exists = false; //provider.policyExists(policyId, policyName);
        if (!exists) {
            throw new CloudRuntimeException("Policy " + policyId + " does not exist on provider " + provider.getName());
        }

        BackupPolicyVO policy = new BackupPolicyVO(policyName, policyId);
        return backupPolicyDao.persist(policy);
    }

    @Override
    public boolean assignVMToBackupPolicy() {
        return false;
    }

    @Override
    public List<BackupPolicy> listBackupPolicies() {
        return null;
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
    public BackupProvider getBackupProvider(final String providerName, final Long zoneId) {
        String name = providerName;
        if (Strings.isNullOrEmpty(name)) {
            name = BackupProviderPlugin.valueIn(zoneId);
        }
        return backupProvidersMap.getOrDefault(name, null);
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListBackupProvidersCmd.class);
        cmdList.add(ListBackupPoliciesCmd.class);
        cmdList.add(CreateBackupPolicyCmd.class);
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
        initializeBackupProviderMap();
    }

    private void initializeBackupProviderMap() {
        if (backupProviders != null && backupProviders.size() != backupProviders.size()) {
            for (final BackupProvider backupProvider : backupProviders) {
                backupProvidersMap.put(backupProvider.getName().toLowerCase(), backupProvider);
            }
        }
    }
}
