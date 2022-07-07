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

import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.utils.ssh.SshException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.networker.NetworkerClient;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.apache.xml.utils.URI;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Objects;



public class NetworkerBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    public static final String BACKUP_IDENTIFIER = "-CSBKP-";
    private static final Logger LOG = Logger.getLogger(NetworkerBackupProvider.class);
    public ConfigKey<String> NetworkerUrl = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.networker.url", "https://localhost:9090/nwrestapi/v3",
            "The EMC Networker API URL.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> NetworkerUsername = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.networker.username", "administrator",
            "The EMC Networker API username.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> NetworkerPassword = new ConfigKey<>("Secure", String.class,
            "backup.plugin.networker.password", "password",
            "The EMC Networker API password.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Boolean> NetworkerValidateSSLSecurity = new ConfigKey<>("Advanced", Boolean.class, "backup.plugin.networker.validate.ssl", "false",
            "Validate the SSL certificate when connecting to EMC Networker API service.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Integer> NetworkerApiRequestTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.networker.request.timeout", "300",
            "The EMC Networker API request timeout in seconds.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Boolean> NetworkerClientVerboseLogs = new ConfigKey<>("Advanced", Boolean.class, "backup.plugin.networker.client.verbosity", "false",
            "Produce Verbose logs in Hypervisor", true, ConfigKey.Scope.Zone);

    @Inject
    private BackupDao backupDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private ConfigurationDao configDao;

    @Inject
    private ClusterDao clusterDao;

    private static String getUrlDomain(String url) throws URISyntaxException {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URI.MalformedURIException e) {
            throw new CloudRuntimeException("Failed to cast URI");
        }
        String domain = uri.getHost();

        return domain;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                NetworkerUrl,
                NetworkerUsername,
                NetworkerPassword,
                NetworkerValidateSSLSecurity,
                NetworkerApiRequestTimeout,
                NetworkerClientVerboseLogs
        };
    }

    @Override
    public String getName() {
        return "networker";
    }

    @Override
    public String getDescription() {
        return "EMC Networker Backup Plugin";
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    protected HostVO getLastVMHypervisorHost(VirtualMachine vm) {
        HostVO host;
        Long hostId = vm.getLastHostId();

        if (hostId == null) {
            LOG.debug("Cannot find last host for vm. This should never happen, please check your database.");
            return null;
        }

        host = hostDao.findById(hostId);
        return host;
    }

    protected HostVO getRunningVMHypervisorHost(VirtualMachine vm) {

        HostVO host;
        Long hostId = vm.getHostId();

        if (hostId == null) {
            throw new CloudRuntimeException("Unable to find the HYPERVISOR for " + vm.getName());
        }

        host = hostDao.findById(hostId);

        return host;
    }

    protected String getVMHypervisorCluster(HostVO host) {

        return clusterDao.findById(host.getClusterId()).getName();
    }

    protected Ternary<String, String, String> getRunningVMHyperisorCredentials(HostVO host) {

        String username = null;
        String password = null;
        String privateKey = null;

        if (host != null && host.getHypervisorType() == Hypervisor.HypervisorType.KVM) {
            hostDao.loadDetails(host);
            password = host.getDetail("password");
            username = host.getDetail("username");
            privateKey = configDao.getValue("ssh.privatekey");
        }
        if ((password == null && privateKey == null) || username == null) {
            throw new CloudRuntimeException("Cannot find login credentials for HYPERVISOR " + host.getName());
        }

        return new Ternary<>(username, password, privateKey);
    }

    private String executeBackupCommand(HostVO host, String username, String password, String privateKey, String command) {

        SSHCmdHelper.SSHCmdResult result = new SSHCmdHelper.SSHCmdResult(-1, null, null);


        final com.trilead.ssh2.Connection connection = SSHCmdHelper.acquireAuthorizedConnection(
                host.getPrivateIpAddress(), 22, username, password, privateKey);
        if (connection == null) {
            throw new CloudRuntimeException(String.format("Failed to connect to SSH at HYPERVISOR %s via IP address [%s].", host, host.getPrivateIpAddress()));
        }
        try {
            result = SSHCmdHelper.sshExecuteCmdOneShot(connection, command);
            if (result.getReturnCode() != 0) {
                throw new CloudRuntimeException(String.format("Backup Script failed on HYPERVISOR %s due to: %s", host, result.getStdErr()));
            }
            LOG.debug("BACKUP RESULT: " + result.toString());
        } catch (final SshException e) {
            throw new CloudRuntimeException("BACKUP SCRIPT FAILED", e);
        }

        return result.toString();
    }

    private NetworkerClient getClient(final Long zoneId) {
        try {
            return new NetworkerClient(NetworkerUrl.valueIn(zoneId), NetworkerUsername.valueIn(zoneId), NetworkerPassword.valueIn(zoneId),
                    NetworkerValidateSSLSecurity.valueIn(zoneId), NetworkerApiRequestTimeout.valueIn(zoneId));
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Failed to parse EMC Networker API URL: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Failed to build EMC Networker API client due to: ", e);
        }
        throw new CloudRuntimeException("Failed to build EMC Networker API client");
    }

    @Override
    public List<BackupOffering> listBackupOfferings(Long zoneId) {
        List<BackupOffering> policies = new ArrayList<>();
        for (final BackupOffering policy : getClient(zoneId).listPolicies()) {
            System.out.println(policy.getName());
            if (!policy.getName().contains(BACKUP_IDENTIFIER)) {
                policies.add(policy);
            }
        }

        return policies;
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        List<BackupOffering> policies = listBackupOfferings(zoneId);
        if (CollectionUtils.isEmpty(policies)) {
            return false;
        }
        for (final BackupOffering policy : policies) {
            if (Objects.equals(policy.getExternalId(), uuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        return true;
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        LOG.debug("Removing VirtualMachine from Backup offering and Deleting any existing backups");

        List<String> backupsTaken = getClient(vm.getDataCenterId()).getBackupsForVm(vm);

        for (String backupId : backupsTaken) {
            LOG.debug("Trying to remove backup with id" + backupId);
            getClient(vm.getDataCenterId()).deleteBackupForVM(backupId);
        }

        return true;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        LOG.debug("Restoring vm " + vm.getUuid() + "from backup " + backup.getUuid() + " on the Networker Backup Provider");

        return true;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid) {
        LOG.debug("Restoring volume " + volumeUuid + "from backup " + backup.getUuid() + " on the Networker Backup Provider");

        return null;
    }

    @Override
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms) {
        final Map<VirtualMachine, Backup.Metric> metrics = new HashMap<>();
        final Backup.Metric metric = new Backup.Metric(1000L, 100L);
        if (vms == null || vms.isEmpty()) {
            return metrics;
        }
        for (VirtualMachine vm : vms) {
            if (vm != null) {
                metrics.put(vm, metric);
            }
        }
        return metrics;
    }


    @Override
    public boolean takeBackup(VirtualMachine vm) {

        String networkerServer = null;
        String clusterName = null;

        try {
            networkerServer = getUrlDomain(NetworkerUrl.value());
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(String.format("Failed to convert API to HOST : %s", e));
        }

        // Find where the VM is currently running
        HostVO hostVO = getRunningVMHypervisorHost(vm);
        // Get credentials for that host
        Ternary<String, String, String> credentials = getRunningVMHyperisorCredentials(hostVO);

        // Get Cluster
        clusterName = getVMHypervisorCluster(hostVO);

        String command = "sudo /usr/share/cloudstack-common/scripts/vm/hypervisor/kvm/nsrkvmbackup.sh" +
                " -s " + networkerServer +
                " -c " + clusterName +
                " -u " + vm.getUuid() +
                " -t " + vm.getName();
        if (NetworkerClientVerboseLogs.value() == true)
            command = command + " -v ";

        LOG.debug("Starting backup for VM ID " + vm.getUuid() + " on Networker provider");
        Date backupJobStart = new Date();

        executeBackupCommand(hostVO, credentials.first(), credentials.second(), credentials.third(), command);

        BackupVO backup = new BackupVO();


        backup = getClient(vm.getDataCenterId()).registerBackupForVm(vm, backupJobStart);
        if (backup != null) {
            backupDao.persist(backup);
            return true;
        } else {
            // We need to handle this rare situation where backup is successful but can't be registered properly.
            return false;
        }
    }

    @Override
    public boolean deleteBackup(Backup backup) {

        final Long zoneId = backup.getZoneId();
        final String externalBackupId = backup.getExternalId();

        if (getClient(zoneId).deleteBackupForVM(externalBackupId)) {
            LOG.debug("EMC Networker successfully deleted backup with id " + externalBackupId);
            return true;
        } else {
            LOG.debug("There was an error removing the backup with id " + externalBackupId + " from EMC NEtworker");
        }
        return false;
    }

    @Override
    public void syncBackups(VirtualMachine vm, Backup.Metric metric) {
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return true;
    }
}

