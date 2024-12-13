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

import java.io.IOException;
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

import org.apache.cloudstack.backup.Backup.Metric;
import org.apache.cloudstack.backup.backroll.BackrollClient;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;
import org.apache.cloudstack.backup.backroll.utils.BackrollApiException;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClientProvider;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ParseException;
import org.joda.time.DateTime;

import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

public class BackrollBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    public static final String BACKUP_IDENTIFIER = "-CSBKP-";

    public ConfigKey<String> BackrollUrlConfigKey = new ConfigKey<>("Advanced", String.class,
    "backup.plugin.backroll.config.url",
    "http://api.backup.demo.ccc:5050/api/v1",
    "Url for backroll plugin.", true, ConfigKey.Scope.Zone);

    public ConfigKey<String> BackrollAppNameConfigKey = new ConfigKey<>("Advanced", String.class,
    "backup.plugin.backroll.config.appname",
    "backroll-api",
    "App Name for backroll plugin.", true, ConfigKey.Scope.Zone);

    public ConfigKey<String> BackrollPasswordConfigKey = new ConfigKey<>("Advanced", String.class,
    "backup.plugin.backroll.config.password",
    "VviX8dALauSyYJMqVYJqf3UyZOpO3joS",
    "Password for backroll plugin.", true, ConfigKey.Scope.Zone);

    private BackrollClient backrollClient;

    @Inject
    private BackupDao backupDao;
    @Inject
    private VMInstanceDao vmInstanceDao;

    @Override
    public String getName() {
        return "backroll";
    }

    @Override
    public String getDescription() {
        return "Backroll Backup Plugin";
    }

    @Override
    public List<BackupOffering> listBackupOfferings(Long zoneId) {
        logger.debug("Listing backup policies on backroll B&R Plugin");
        BackrollClient client = getClient(zoneId);
        try{
            String urlToRequest = client.getBackupOfferingUrl();
            logger.info("BackrollProvider: urlToRequest: " + urlToRequest);
            if (!StringUtils.isEmpty(urlToRequest)){
                List<BackupOffering> results = new ArrayList<BackupOffering>();
                // return client.getBackupOfferings(urlToRequest);
                results = client.getBackupOfferings(urlToRequest);
                if(results.size()>0) {
                    logger.info("BackrollProvider: results > 0");
                } else {
                    logger.info("BackrollProvider: results <= 0");
                }
                return results;
            }
        } catch (ParseException | BackrollApiException | IOException e) {
            logger.info("BackrollProvider: catch erreur: " + e);
            throw new CloudRuntimeException("Failed to load backup offerings");
        }
        return new ArrayList<BackupOffering>();
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        logger.info("Checking if backup offering exists on the Backroll Backup Provider");
        return true;
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        logger.info("Creating VM backup for VM {} from backup offering {}", vm.getInstanceName(), backupOffering.getName());
        if(vm instanceof VMInstanceVO) {
            ((VMInstanceVO) vm).setBackupExternalId(backupOffering.getUuid());
            return true;
        }
        return false;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        logger.debug("Restoring vm {} from backup {} on the backroll Backup Provider", vm.getUuid(), backup.getUuid());
        boolean isSuccess;
        try {
            isSuccess = getClient(vm.getDataCenterId()).restoreVMFromBackup(vm.getUuid(), getBackupName(backup));
        } catch (ParseException | BackrollApiException | IOException e) {
            throw new CloudRuntimeException("Failed to restore VM from Backrup");
        }
        return isSuccess;
    }

    @Override
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms) {
        final Map<VirtualMachine, Backup.Metric> metrics = new HashMap<>();
        if (CollectionUtils.isEmpty(vms)) {
            logger.warn("Unable to get VM Backup Metrics because the list of VMs is empty.");
            return metrics;
        }

        List<String> vmUuids = vms.stream().filter(Objects::nonNull).map(VirtualMachine::getUuid).collect(Collectors.toList());
        logger.debug("Get Backup Metrics for VMs: {}.", String.join(", ", vmUuids));

        BackrollClient client = getClient(zoneId);
        for (final VirtualMachine vm : vms) {
            if (vm == null) {
                continue;
            }

            Metric metric;
            try {
                metric = client.getVirtualMachineMetrics(vm.getUuid());
            } catch (BackrollApiException | IOException e) {
                throw new CloudRuntimeException("Failed to retrieve backup metrics");
            }
            logger.debug("Metrics for VM [uuid: {}, name: {}] is [backup size: {}, data size: {}].", vm.getUuid(),
                    vm.getInstanceName(), metric.getBackupSize(), metric.getDataSize());
            metrics.put(vm, metric);
        }
        return metrics;
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        return true;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public boolean takeBackup(VirtualMachine vm) {
        logger.info("Starting backup for VM ID {} on backroll provider", vm.getUuid());
        final BackrollClient client = getClient(vm.getDataCenterId());

        String idBackupTask;
        try {
            idBackupTask = client.startBackupJob(vm.getUuid());
        } catch (IOException | BackrollApiException e) {
            logger.error(e);
            throw new CloudRuntimeException("Failed to take backup");
        }
        if (!StringUtils.isEmpty(idBackupTask)) {
            BackupVO backup = new BackupVO();
            backup.setVmId(vm.getId());
            backup.setExternalId(idBackupTask);
            backup.setType("INCREMENTAL");
            backup.setDate(new DateTime().toDate());
            backup.setSize(0L);
            backup.setProtectedSize(0L);
            backup.setStatus(Backup.Status.BackingUp);
            backup.setBackupOfferingId(vm.getBackupOfferingId());
            backup.setAccountId(vm.getAccountId());
            backup.setDomainId(vm.getDomainId());
            backup.setZoneId(vm.getDataCenterId());
            return backupDao.persist(backup) != null;
        }
        return false;
    }

    @Override
    public void syncBackups(VirtualMachine vm, Backup.Metric metric) {
        logger.info("Starting sync backup for VM ID " + vm.getUuid() + " on backroll provider");

        final BackrollClient client = getClient(vm.getDataCenterId());
        List<Backup> backupsInDb = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());

        for (Backup backup : backupsInDb) {
            if (backup.getStatus().equals(Backup.Status.BackingUp)) {
                BackrollTaskStatus response;
                try {
                    response = client.checkBackupTaskStatus(backup.getExternalId());
                } catch (ParseException | BackrollApiException | IOException e) {
                    logger.error(e);
                    throw new CloudRuntimeException("Failed to sync backups");
                }

                if (response != null) {
                    logger.debug("backroll backup id: {}", backup.getExternalId());
                    logger.debug("backroll backup status: {}", response.getState());

                    BackupVO backupToUpdate = ((BackupVO) backup);

                    if (response.getState().equals("PENDING")) {
                        backupToUpdate.setStatus(Backup.Status.BackingUp);
                    } else if (response.getState().equals("FAILURE")) {
                        backupToUpdate.setStatus(Backup.Status.Failed);
                    } else if (response.getState().equals("SUCCESS")) {
                        backupToUpdate.setStatus(Backup.Status.BackedUp);
                        backupToUpdate.setExternalId(backup.getExternalId() + "," + response.getInfo());

                        BackrollBackupMetrics backupMetrics = null;
                        try {
                            backupMetrics = client.getBackupMetrics(vm.getUuid() , response.getInfo());
                        } catch (BackrollApiException | IOException e) {
                            logger.error(e);
                            throw new CloudRuntimeException("Failed to get backup metrics");
                        }

                        if (backupMetrics != null) {
                            backupToUpdate.setSize(backupMetrics.getDeduplicated()); // real size
                            backupToUpdate.setProtectedSize(backupMetrics.getSize()); // total size
                        }
                    } else {
                        backupToUpdate.setStatus(Backup.Status.BackingUp);
                    }

                    if (backupDao.persist(backupToUpdate) != null) {
                        logger.info("Backroll mise à jour enregistrée");
                    }
                }
            } else if (backup.getStatus().equals(Backup.Status.BackedUp) && backup.getSize().equals(0L)) {
                String backupId = backup.getExternalId().contains(",") ? backup.getExternalId().split(",")[1] : backup.getExternalId();
                BackrollBackupMetrics backupMetrics;
                try {
                    backupMetrics = client.getBackupMetrics(vm.getUuid() , backupId);
                } catch (BackrollApiException | IOException e) {
                    logger.error(e);
                    throw new CloudRuntimeException("Failed to get backup metrics");
                }

                if (backupMetrics != null) {
                    BackupVO backupToUpdate = ((BackupVO) backup);
                    backupToUpdate.setSize(backupMetrics.getDeduplicated()); // real size
                    backupToUpdate.setProtectedSize(backupMetrics.getSize()); // total size
                    backupDao.persist(backupToUpdate);
                }
            }
        }

        // Backups synchronisation between Backroll ad CS Db
        List<BackrollVmBackup> backupsFromBackroll;
        try {
            backupsFromBackroll = client.getAllBackupsfromVirtualMachine(vm.getUuid());

            backupsInDb = backupDao.listByVmId(null, vm.getId());

            // insert new backroll backup in CS
            for (BackrollVmBackup backupInBackroll : backupsFromBackroll) {
                Backup backupToFind = backupsInDb.stream()
                    .filter(backupInDb -> backupInDb.getExternalId().contains(backupInBackroll.getName()))
                    .findAny()
                    .orElse(null);

                if (backupToFind == null) {
                    BackupVO backupToInsert = new BackupVO();
                    backupToInsert.setVmId(vm.getId());
                    backupToInsert.setExternalId(backupInBackroll.getId() + "," + backupInBackroll.getName());
                    backupToInsert.setType("INCREMENTAL");
                    backupToInsert.setDate(backupInBackroll.getDate());
                    backupToInsert.setSize(0L);
                    backupToInsert.setProtectedSize(0L);
                    backupToInsert.setStatus(Backup.Status.BackedUp);
                    backupToInsert.setBackupOfferingId(vm.getBackupOfferingId());
                    backupToInsert.setAccountId(vm.getAccountId());
                    backupToInsert.setDomainId(vm.getDomainId());
                    backupToInsert.setZoneId(vm.getDataCenterId());
                    backupDao.persist(backupToInsert);
                }
                if (backupToFind != null && backupToFind.getStatus() == Backup.Status.Removed) {
                    BackupVO backupToUpdate = ((BackupVO) backupToFind);
                    backupToUpdate.setStatus(Backup.Status.BackedUp);
                    if (backupDao.persist(backupToUpdate) != null) {
                        logger.info("Backroll update saved");
                        backupDao.remove(backupToFind.getId());
                    }
                }
            }

            // delete deleted backroll backup in CS
            backupsInDb = backupDao.listByVmId(null, vm.getId());
            for (Backup backup : backupsInDb) {
                String backupName = backup.getExternalId().contains(",") ? backup.getExternalId().split(",")[1] : backup.getExternalId();
                BackrollVmBackup backupToFind = backupsFromBackroll.stream()
                    .filter(backupInBackroll -> backupInBackroll.getName().contains(backupName))
                    .findAny()
                    .orElse(null);

                if (backupToFind == null) {
                    BackupVO backupToUpdate = ((BackupVO) backup);
                    backupToUpdate.setStatus(Backup.Status.Removed);
                    if (backupDao.persist(backupToUpdate) != null) {
                        logger.debug("Backroll delete saved (sync)");
                    }
                }
            }
        } catch (BackrollApiException | IOException e) {
            logger.error(e);
        }
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
            BackrollUrlConfigKey,
            BackrollAppNameConfigKey,
            BackrollPasswordConfigKey
        };
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        logger.info("backroll delete backup id: {}", backup.getExternalId());
        if (backup.getStatus().equals(Backup.Status.BackingUp)) {
            throw new CloudRuntimeException("You can't delete a backup while it still BackingUp");
        } else {
            logger.debug("backroll - try delete backup");
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());

            if (backup.getStatus().equals(Backup.Status.Removed) || backup.getStatus().equals(Backup.Status.Failed)){
                return deleteBackupInDb(backup);
            } else {
                try {
                    if (getClient(backup.getZoneId()).deleteBackup(vm.getUuid(), getBackupName(backup))) {
                        logger.debug("Backup deletion for backup {} complete on backroll side.", backup.getUuid());
                        return deleteBackupInDb(backup);
                    }
                } catch (BackrollApiException | IOException e) {
                    logger.error(e);
                    throw new CloudRuntimeException("Failed to delete backup");
                }
            }
        }
        return false;
    }

    private boolean deleteBackupInDb(Backup backup) {
        BackupVO backupToUpdate = ((BackupVO) backup);
        backupToUpdate.setStatus(Backup.Status.Removed);
        if (backupDao.persist(backupToUpdate) != null) {
            logger.debug("Backroll backup {} deleted in database.", backup.getUuid());
            return true;
        }
        return false;
    }

    protected BackrollClient getClient(final Long zoneId) {
        logger.debug("Backroll Provider GetClient with zone id {}", zoneId);
        try {
            if (backrollClient == null) {
                logger.debug("backroll client null - instanciation of new one ");
                BackrollHttpClientProvider provider = new BackrollHttpClientProvider(BackrollUrlConfigKey.valueIn(zoneId), BackrollAppNameConfigKey.valueIn(zoneId), BackrollPasswordConfigKey.valueIn(zoneId), true, 300, 600);
                backrollClient = new BackrollClient(provider);
            }
            return backrollClient;
        } catch (URISyntaxException e) {
            logger.error(e);
            throw new CloudRuntimeException("Failed to parse Backroll API URL: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error(e);
            throw new CloudRuntimeException("Failed to build Backroll API client");
        }
    }

    private String getBackupName(Backup backup) {
        return backup.getExternalId().substring(backup.getExternalId().indexOf(",") + 1);
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        logger.debug("Restoring volume {} from backup {} on the Backroll Backup Provider", volumeUuid, backup.getUuid());
        throw new CloudRuntimeException("Backroll plugin does not support this feature");
    }
}
