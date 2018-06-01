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
import java.util.List;

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.backup.veeam.VeeamClient;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.log4j.Logger;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class VeeamBackupProvider extends AdapterBase implements BackupProvider, Configurable {
    private static final Logger LOG = Logger.getLogger(VeeamBackupProvider.class);

    private ConfigKey<String> VeeamUrl = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.url",
            "http://localhost:9399/api/",
            "The Veeam backup and recovery URL.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> VeeamUsername = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.username",
            "administrator",
            "The Veeam backup and recovery username.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> VeeamPassword = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.password",
            "P@ssword123",
            "The Veeam backup and recovery password.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Boolean> VeeamValidateSSLSecurity = new ConfigKey<>("Advanced", Boolean.class, "backup.plugin.veeam.validate.ssl", "true",
            "When set to true, this will validate the SSL certificate when connecting to https/ssl enabled Veeam API service.", true, ConfigKey.Scope.Zone);


    private ConfigKey<Integer> VeeamApiRequestTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.veeam.request.timeout", "300",
            "The Veeam B&R API request timeout in seconds.", true, ConfigKey.Scope.Zone);

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

    @Override
    public boolean addVMToBackupPolicy(Long zoneId, String policyId, VirtualMachine vm) {
        String instanceName = vm.getInstanceName();
        //TODO: Get vcenter ip
        return getClient(zoneId).assignBackupPolicyToVM(policyId, instanceName, "");
    }

    @Override
    public boolean removeVMFromBackupPolicy(Long zoneId, String policyId, VirtualMachine vm) {
        //TODO: Remove VM from backup policy on the client
        return false;
    }

    @Override
    public List<BackupPolicy> listBackupPolicies(Long zoneId) {
        return getClient(zoneId).listBackupPolicies();
    }

    @Override
    public boolean isBackupPolicy(String uuid) {
        //TODO
        return true;
    }

    @Override
    public boolean restoreVMFromBackup(String vmUuid, String backupUuid) {
        //TODO
        return false;
    }

    @Override
    public VolumeTO restoreVolumeFromBackup(String volumeUuid, String backupUuid) {
        //TODO
        return null;
    }

    @Override
    public List<Backup> listVMBackups(Long zoneId, VirtualMachine vm) {
        //TODO
        return null;
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
