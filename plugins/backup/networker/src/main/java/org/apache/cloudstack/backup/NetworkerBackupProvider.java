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
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDaoImpl;
import org.apache.cloudstack.backup.networker.NetworkerClient;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.apache.xml.utils.URI;
import org.apache.cloudstack.backup.networker.api.NetworkerBackup;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.cloud.utils.script.Script;

public class NetworkerBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    public static final String BACKUP_IDENTIFIER = "-CSBKP-";
    private static final Logger LOG = Logger.getLogger(NetworkerBackupProvider.class);

    public ConfigKey<String> NetworkerUrl = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.networker.url", "https://localhost:9090/nwrestapi/v3",
            "The EMC Networker API URL.", true, ConfigKey.Scope.Zone);

    private final ConfigKey<String> NetworkerUsername = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.networker.username", "administrator",
            "The EMC Networker API username.", true, ConfigKey.Scope.Zone);

    private final ConfigKey<String> NetworkerPassword = new ConfigKey<>("Secure", String.class,
            "backup.plugin.networker.password", "password",
            "The EMC Networker API password.", true, ConfigKey.Scope.Zone);

    private final ConfigKey<String> NetworkerMediaPool = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.networker.pool", "Default",
            "The EMC Networker Media Pool", true, ConfigKey.Scope.Zone);

    private final ConfigKey<Boolean> NetworkerValidateSSLSecurity = new ConfigKey<>("Advanced", Boolean.class,
            "backup.plugin.networker.validate.ssl", "false",
            "Validate the SSL certificate when connecting to EMC Networker API service.", true, ConfigKey.Scope.Zone);

    private final ConfigKey<Integer> NetworkerApiRequestTimeout = new ConfigKey<>("Advanced", Integer.class,
            "backup.plugin.networker.request.timeout", "300",
            "The EMC Networker API request timeout in seconds.", true, ConfigKey.Scope.Zone);

    private final ConfigKey<Boolean> NetworkerClientVerboseLogs = new ConfigKey<>("Advanced", Boolean.class,
            "backup.plugin.networker.client.verbosity", "false",
            "Produce Verbose logs in Hypervisor", true, ConfigKey.Scope.Zone);

    @Inject
    private BackupDao backupDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private ClusterDao clusterDao;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private StoragePoolHostDao storagePoolHostDao;

    @Inject
    private VMInstanceDao vmInstanceDao;

    private static String getUrlDomain(String url) throws URISyntaxException {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URI.MalformedURIException e) {
            throw new CloudRuntimeException("Failed to cast URI");
        }

        return uri.getHost();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                NetworkerUrl,
                NetworkerUsername,
                NetworkerPassword,
                NetworkerValidateSSLSecurity,
                NetworkerApiRequestTimeout,
                NetworkerMediaPool,
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

        if ( host.getStatus() == Status.Up ) {
            return host;
        } else {
            // Try to find a host in the same cluster
            List<HostVO> altClusterHosts = hostDao.findHypervisorHostInCluster(host.getClusterId());
            for (final HostVO candidateClusterHost : altClusterHosts) {
                if ( candidateClusterHost.getStatus() == Status.Up ) {
                    LOG.debug("Found Host " + candidateClusterHost.getName());
                    return candidateClusterHost;
                }
            }
        }
        // Try to find a Host in the zone
        List<HostVO> altZoneHosts = hostDao.findByDataCenterId(host.getDataCenterId());
        for (final HostVO candidateZoneHost : altZoneHosts) {
            if ( candidateZoneHost.getStatus() == Status.Up && candidateZoneHost.getHypervisorType() == Hypervisor.HypervisorType.KVM ) {
                LOG.debug("Found Host " + candidateZoneHost.getName());
                return candidateZoneHost;
            }
        }
        return null;
    }

    protected HostVO getRunningVMHypervisorHost(VirtualMachine vm) {

        HostVO host;
        Long hostId = vm.getHostId();

        if (hostId == null) {
            throw new CloudRuntimeException("Unable to find the HYPERVISOR for " + vm.getName() + ". Make sure the virtual machine is running");
        }

        host = hostDao.findById(hostId);

        return host;
    }

    protected String getVMHypervisorCluster(HostVO host) {

        return clusterDao.findById(host.getClusterId()).getName();
    }

    protected Ternary<String, String, String> getKVMHyperisorCredentials(HostVO host) {

        String username = null;
        String password = null;

        if (host != null && host.getHypervisorType() == Hypervisor.HypervisorType.KVM) {
            hostDao.loadDetails(host);
            password = host.getDetail("password");
            username = host.getDetail("username");
        }
        if ( password == null  || username == null) {
            throw new CloudRuntimeException("Cannot find login credentials for HYPERVISOR " + Objects.requireNonNull(host).getUuid());
        }

        return new Ternary<>(username, password, null);
    }

    private String executeBackupCommand(HostVO host, String username, String password, String command) {
        String nstRegex = "\\bcompleted savetime=([0-9]{10})";
        Pattern saveTimePattern = Pattern.compile(nstRegex);

        try {
            Pair<Boolean, String> response = SshHelper.sshExecute(host.getPrivateIpAddress(), 22,
                    username, null, password, command, 120000, 120000, 3600000);
            if (!response.first()) {
                LOG.error(String.format("Backup Script failed on HYPERVISOR %s due to: %s", host, response.second()));
            } else {
                LOG.debug(String.format("Networker Backup Results: %s", response.second()));
            }
            Matcher saveTimeMatcher = saveTimePattern.matcher(response.second());
            if (saveTimeMatcher.find()) {
                LOG.debug(String.format("Got saveTimeMatcher: %s", saveTimeMatcher.group(1)));
                return saveTimeMatcher.group(1);
            }
        } catch (final Exception e) {
            throw new CloudRuntimeException(String.format("Failed to take backup on host %s due to: %s", host.getName(), e.getMessage()));
        }

        return null;
    }
    private boolean executeRestoreCommand(HostVO host, String username, String password, String command) {

        try {
            Pair<Boolean, String> response = SshHelper.sshExecute(host.getPrivateIpAddress(), 22,
                username, null, password, command, 120000, 120000, 3600000);

            if (!response.first()) {
                LOG.error(String.format("Restore Script failed on HYPERVISOR %s due to: %s", host, response.second()));
            } else {
                LOG.debug(String.format("Networker Restore Results: %s",response.second()));
                return true;
            }
        } catch (final Exception e) {
            throw new CloudRuntimeException(String.format("Failed to restore backup on host %s due to: %s", host.getName(), e.getMessage()));
        }
        return false;
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
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) { return true; }

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
        String networkerServer;
        HostVO hostVO;

        final Long zoneId = backup.getZoneId();
        final String externalBackupId = backup.getExternalId();
        final NetworkerBackup networkerBackup=getClient(zoneId).getNetworkerBackupInfo(externalBackupId);
        final String SSID = networkerBackup.getShortId();

        LOG.debug("Restoring vm " + vm.getUuid() + "from backup " + backup.getUuid() + " on the Networker Backup Provider");

        if ( SSID.isEmpty() ) {
            LOG.debug("There was an error retrieving the SSID for backup with id " + externalBackupId + " from EMC NEtworker");
            return false;
        }

        // Find where the VM was last running
        hostVO = getLastVMHypervisorHost(vm);
        // Get credentials for that host
        Ternary<String, String, String> credentials = getKVMHyperisorCredentials(hostVO);
        LOG.debug("The SSID was reported successfully " + externalBackupId);
        try {
            networkerServer = getUrlDomain(NetworkerUrl.value());
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(String.format("Failed to convert API to HOST : %s", e));
        }
        String networkerRestoreScr = "/usr/share/cloudstack-common/scripts/vm/hypervisor/kvm/nsrkvmrestore.sh";
        final Script script = new Script(networkerRestoreScr);
        script.add("-s");
        script.add(networkerServer);
        script.add("-S");
        script.add(SSID);

        if ( Boolean.TRUE.equals(NetworkerClientVerboseLogs.value()) )
            script.add("-v");

        Date restoreJobStart = new Date();
        LOG.debug("Starting Restore for VM ID " + vm.getUuid() + " and SSID" + SSID + " at " + restoreJobStart);

        if ( executeRestoreCommand(hostVO, credentials.first(), credentials.second(), script.toString()) ) {
            Date restoreJobEnd = new Date();
            LOG.debug("Restore Job for SSID " + SSID + " completed successfully at " + restoreJobEnd);
            return true;
        } else {
            LOG.debug("Restore Job for SSID " + SSID + " failed!");
            return false;
        }
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid) {
        String networkerServer;
        VolumeVO volume = volumeDao.findByUuid(volumeUuid);
        VMInstanceVO backupSourceVm = vmInstanceDao.findById(backup.getVmId());
        StoragePoolHostVO dataStore = storagePoolHostDao.findByUuid(dataStoreUuid);
        HostVO hostVO = hostDao.findByIp(hostIp);

        final Long zoneId = backup.getZoneId();
        final String externalBackupId = backup.getExternalId();
        final NetworkerBackup networkerBackup=getClient(zoneId).getNetworkerBackupInfo(externalBackupId);
        final String SSID = networkerBackup.getShortId();
        final String clusterName = networkerBackup.getClientHostname();
        final String destinationNetworkerClient = hostVO.getName().split("\\.")[0];
        Long restoredVolumeDiskSize = 0L;

        LOG.debug("Restoring volume " + volumeUuid + "from backup " + backup.getUuid() + " on the Networker Backup Provider");

        if ( SSID.isEmpty() ) {
            LOG.debug("There was an error retrieving the SSID for backup with id " + externalBackupId + " from EMC NEtworker");
            return null;
        }

        Ternary<String, String, String> credentials = getKVMHyperisorCredentials(hostVO);
        LOG.debug("The SSID was reported successfully " + externalBackupId);
        try {
            networkerServer = getUrlDomain(NetworkerUrl.value());
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(String.format("Failed to convert API to HOST : %s", e));
        }

        // Find volume size  from backup vols
        for ( Backup.VolumeInfo VMVolToRestore : backupSourceVm.getBackupVolumeList()) {
            if (VMVolToRestore.getUuid().equals(volumeUuid))
                restoredVolumeDiskSize = (VMVolToRestore.getSize());
        }

        VolumeVO restoredVolume = new VolumeVO(Volume.Type.DATADISK, null, backup.getZoneId(),
                backup.getDomainId(), backup.getAccountId(), 0, null,
                backup.getSize(), null, null, null);

        restoredVolume.setName("RV-"+volume.getName());
        restoredVolume.setProvisioningType(volume.getProvisioningType());
        restoredVolume.setUpdated(new Date());
        restoredVolume.setUuid(UUID.randomUUID().toString());
        restoredVolume.setRemoved(null);
        restoredVolume.setDisplayVolume(true);
        restoredVolume.setPoolId(volume.getPoolId());
        restoredVolume.setPath(restoredVolume.getUuid());
        restoredVolume.setState(Volume.State.Copying);
        restoredVolume.setSize(restoredVolumeDiskSize);
        restoredVolume.setDiskOfferingId(volume.getDiskOfferingId());

        try {
            volumeDao.persist(restoredVolume);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to craft restored volume due to: "+e);
        }
        String networkerRestoreScr = "/usr/share/cloudstack-common/scripts/vm/hypervisor/kvm/nsrkvmrestore.sh";
        final Script script = new Script(networkerRestoreScr);
        script.add("-s");
        script.add(networkerServer);
        script.add("-c");
        script.add(clusterName);
        script.add("-d");
        script.add(destinationNetworkerClient);
        script.add("-n");
        script.add(restoredVolume.getUuid());
        script.add("-p");
        script.add(dataStore.getLocalPath());
        script.add("-a");
        script.add(volume.getUuid());

        if ( Boolean.TRUE.equals(NetworkerClientVerboseLogs.value()) )
            script.add("-v");

        Date restoreJobStart = new Date();
        LOG.debug("Starting Restore for Volume UUID " + volume.getUuid() + " and SSID" + SSID + " at " + restoreJobStart);

        if ( executeRestoreCommand(hostVO, credentials.first(), credentials.second(), script.toString()) ) {
            Date restoreJobEnd = new Date();
            LOG.debug("Restore Job for SSID " + SSID + " completed successfully at " + restoreJobEnd);
            return new Pair<>(true,restoredVolume.getUuid());
        } else {
            volumeDao.expunge(restoredVolume.getId());
            LOG.debug("Restore Job for SSID " + SSID + " failed!");
            return null;
        }
    }

    @Override
    public boolean takeBackup(VirtualMachine vm) {
        String networkerServer;
        String clusterName;

        try {
            networkerServer = getUrlDomain(NetworkerUrl.value());
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(String.format("Failed to convert API to HOST : %s", e));
        }

        // Find where the VM is currently running
        HostVO hostVO = getRunningVMHypervisorHost(vm);
        // Get credentials for that host
        Ternary<String, String, String> credentials = getKVMHyperisorCredentials(hostVO);
        // Get retention Period for our Backup
        BackupOfferingVO vmBackupOffering = new BackupOfferingDaoImpl().findById(vm.getBackupOfferingId());
        final String backupProviderPolicyId = vmBackupOffering.getExternalId();
        String backupRentionPeriod = getClient(vm.getDataCenterId()).getBackupPolicyRetentionInterval(backupProviderPolicyId);

        if ( backupRentionPeriod == null ) {
            LOG.warn("There is no retention setting for Emc Networker Policy, setting default for 1 day");
            backupRentionPeriod = "1 Day";
        }

        // Get Cluster
        clusterName = getVMHypervisorCluster(hostVO);
        String networkerBackupScr = "/usr/share/cloudstack-common/scripts/vm/hypervisor/kvm/nsrkvmbackup.sh";
        final Script script = new Script(networkerBackupScr);
        script.add("-s");
        script.add(networkerServer);
        script.add("-R");
        script.add("'"+backupRentionPeriod+"'");
        script.add("-P");
        script.add(NetworkerMediaPool.valueIn(vm.getDataCenterId()));
        script.add("-c");
        script.add(clusterName);
        script.add("-u");
        script.add(vm.getUuid());
        script.add("-t");
        script.add(vm.getName());
        if ( Boolean.TRUE.equals(NetworkerClientVerboseLogs.value()) )
            script.add("-v");

        LOG.debug("Starting backup for VM ID " + vm.getUuid() + " on Networker provider");
        Date backupJobStart = new Date();

        String saveTime = executeBackupCommand(hostVO, credentials.first(), credentials.second(), script.toString());
        LOG.info ("EMC Networker finished backup job for vm " + vm.getName() + " with saveset Time: " + saveTime);
        BackupVO backup = getClient(vm.getDataCenterId()).registerBackupForVm(vm, backupJobStart, saveTime);
        if (backup != null) {
            backupDao.persist(backup);
            return true;
        } else {
            LOG.error("Could not register backup for vm " + vm.getName() + " with saveset Time: " + saveTime);
            // We need to handle this rare situation where backup is successful but can't be registered properly.
            return false;
        }
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {

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
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms) {
        final Map<VirtualMachine, Backup.Metric> metrics = new HashMap<>();
        Long vmBackupSize=0L;
        Long vmBackupProtectedSize=0L;

        if (CollectionUtils.isEmpty(vms)) {
            LOG.warn("Unable to get VM Backup Metrics because the list of VMs is empty.");
            return metrics;
        }

        for (final VirtualMachine vm : vms) {
            for ( Backup.VolumeInfo thisVMVol : vm.getBackupVolumeList()) {
                vmBackupSize += (thisVMVol.getSize() / 1024L / 1024L);
            }
            final ArrayList<String> vmBackups = getClient(zoneId).getBackupsForVm(vm);
            for ( String vmBackup : vmBackups ) {
                NetworkerBackup vmNwBackup = getClient(zoneId).getNetworkerBackupInfo(vmBackup);
                vmBackupProtectedSize+= vmNwBackup.getSize().getValue() / 1024L;
            }
            Backup.Metric vmBackupMetric = new Backup.Metric(vmBackupSize,vmBackupProtectedSize);
            LOG.debug(String.format("Metrics for VM [uuid: %s, name: %s] is [backup size: %s, data size: %s].", vm.getUuid(),
                    vm.getInstanceName(), vmBackupMetric.getBackupSize(), vmBackupMetric.getDataSize()));
            metrics.put(vm, vmBackupMetric);
        }
        return metrics;
    }

    @Override
    public void syncBackups(VirtualMachine vm, Backup.Metric metric) {
        final Long zoneId = vm.getDataCenterId();
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                final List<Backup> backupsInDb = backupDao.listByVmId(null, vm.getId());
                final ArrayList<String> backupsInNetworker = getClient(zoneId).getBackupsForVm(vm);
                final List<Long> removeList = backupsInDb.stream().map(InternalIdentity::getId).collect(Collectors.toList());
                for (final String networkerBackupId : backupsInNetworker ) {
                    Long vmBackupSize=0L;
                    boolean backupExists = false;
                    for (final Backup backupInDb : backupsInDb) {
                        LOG.debug("Checking if Backup with external ID " + backupInDb.getName() + " for VM " + backupInDb.getVmId() + "is valid");
                        if ( networkerBackupId.equals(backupInDb.getExternalId()) ) {
                            LOG.debug("Found Backup with id " + backupInDb.getId() + " in both Database and Networker");
                            backupExists = true;
                            removeList.remove(backupInDb.getId());
                            if (metric != null) {
                                LOG.debug(String.format("Update backup with [uuid: %s, external id: %s] from [size: %s, protected size: %s] to [size: %s, protected size: %s].",
                                        backupInDb.getUuid(), backupInDb.getExternalId(), backupInDb.getSize(), backupInDb.getProtectedSize(),
                                        metric.getBackupSize(), metric.getDataSize()));
                                ((BackupVO) backupInDb).setSize(metric.getBackupSize());
                                ((BackupVO) backupInDb).setProtectedSize(metric.getDataSize());
                                backupDao.update(backupInDb.getId(), ((BackupVO) backupInDb));
                            }
                            break;
                        }
                    }
                    if (backupExists) {
                        continue;
                    }
                    // Technically an administrator can manually create a backup for a VM by utilizing the KVM scripts
                    // with the proper parameters. So we will register any backups taken on the Networker side from
                    // outside Cloudstack. If ever Networker will support KVM out of the box this functionality also will
                    // ensure that SLA like backups will be found and registered.
                    NetworkerBackup strayNetworkerBackup = getClient(vm.getDataCenterId()).getNetworkerBackupInfo(networkerBackupId);
                    // Since running backups are already present in Networker Server but not completed
                    // make sure the backup is not in progress at this time.
                    if ( strayNetworkerBackup.getCompletionTime() != null) {
                        BackupVO strayBackup = new BackupVO();
                        strayBackup.setVmId(vm.getId());
                        strayBackup.setExternalId(strayNetworkerBackup.getId());
                        strayBackup.setType(strayNetworkerBackup.getType());
                        strayBackup.setDate(strayNetworkerBackup.getSaveTime());
                        strayBackup.setStatus(Backup.Status.BackedUp);
                        for ( Backup.VolumeInfo thisVMVol : vm.getBackupVolumeList()) {
                            vmBackupSize += (thisVMVol.getSize() / 1024L /1024L);
                        }
                        strayBackup.setSize(vmBackupSize);
                        strayBackup.setProtectedSize(strayNetworkerBackup.getSize().getValue() / 1024L );
                        strayBackup.setBackupOfferingId(vm.getBackupOfferingId());
                        strayBackup.setAccountId(vm.getAccountId());
                        strayBackup.setDomainId(vm.getDomainId());
                        strayBackup.setZoneId(vm.getDataCenterId());
                        LOG.debug(String.format("Creating a new entry in backups: [uuid: %s, vm_id: %s, external_id: %s, type: %s, date: %s, backup_offering_id: %s, account_id: %s, "
                                        + "domain_id: %s, zone_id: %s].", strayBackup.getUuid(), strayBackup.getVmId(), strayBackup.getExternalId(),
                                strayBackup.getType(), strayBackup.getDate(), strayBackup.getBackupOfferingId(), strayBackup.getAccountId(),
                                strayBackup.getDomainId(), strayBackup.getZoneId()));
                        backupDao.persist(strayBackup);
                        LOG.warn("Added backup found in provider with ID: [" + strayBackup.getId() + "]");
                    } else {
                        LOG.debug ("Backup is in progress, skipping addition for this run");
                    }
                }
                for (final Long backupIdToRemove : removeList) {
                    LOG.warn(String.format("Removing backup with ID: [%s].", backupIdToRemove));
                    backupDao.remove(backupIdToRemove);
                }
            }
        });
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() { return false; }
}
