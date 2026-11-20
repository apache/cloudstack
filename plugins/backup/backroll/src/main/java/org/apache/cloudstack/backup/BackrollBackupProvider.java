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
import org.apache.cloudstack.backup.Backup.RestorePoint;
import org.apache.cloudstack.backup.backroll.BackrollClient;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.utils.BackrollApiException;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClientProvider;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ParseException;
import org.apache.logging.log4j.Logger;
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

    public BackrollBackupProvider(BackupDao backupDao, VMInstanceDao vmInstanceDao, BackrollClient client, Logger logger){
        this.backupDao = backupDao;
        this.vmInstanceDao = vmInstanceDao;
        this.backrollClient = client;
        this.logger = logger;
    }

    public BackrollBackupProvider(){}

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
            logger.info("BackrollProvider: catch erreur: {}", e);
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
            throw new CloudRuntimeException("Failed to restore VM from Backup");
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
            try {
                // get backups from database
                List<Backup> backupsInDb = backupDao.listByVmId(zoneId, vm.getId());

                // check backing up task
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
                                backupToUpdate.setExternalId(response.getInfo());

                                BackrollBackupMetrics backupMetrics = null;
                                try {
                                    backupMetrics = client.getBackupMetrics(vm.getUuid() , response.getInfo());
                                    if (backupMetrics != null) {
                                        backupToUpdate.setProtectedSize(backupMetrics.getDeduplicated());
                                        backupToUpdate.setSize(backupMetrics.getSize());
                                    }
                                } catch (BackrollApiException | IOException e) {
                                    logger.error(e);
                                    throw new CloudRuntimeException("Failed to get backup metrics");
                                }
                            } else {
                                backupToUpdate.setStatus(Backup.Status.BackingUp);
                            }

                            if (backupDao.persist(backupToUpdate) != null) {
                                logger.info("Backroll backup updated");
                            }
                        }
                    } else {
                        if(backup.getExternalId().contains(",")) {
                            String backupId = backup.getExternalId().split(",")[1];
                            BackupVO backupToUpdate = ((BackupVO) backup);
                            backupToUpdate.setExternalId(backupId);
                            try {
                                BackrollBackupMetrics backupMetrics = client.getBackupMetrics(vm.getUuid() , backupId);
                                if (backupMetrics != null) {
                                    backupToUpdate.setProtectedSize(backupMetrics.getDeduplicated());
                                    backupToUpdate.setSize(backupMetrics.getSize());
                                }
                            } catch (BackrollApiException | IOException e) {
                                logger.error(e);
                                throw new CloudRuntimeException("Failed to get backup metrics");
                            }
                            if (backupDao.persist(backupToUpdate) != null) {
                                logger.info("Backroll backup updated");
                            }
                        }
                    }
                }

                // refresh backup in database list
                backupsInDb = backupDao.listByVmId(zoneId, vm.getId());

                Long usedSize = 0L;
                Long dataSize = 0L;
                List<RestorePoint> backups = client.listRestorePoints(vm.getUuid());
                for (RestorePoint backup : backups) {

                    BackrollBackupMetrics backupMetrics = client.getBackupMetrics(vm.getUuid() , getBackupName(backup.getId()));
                    if (backupMetrics != null) {
                        usedSize += Long.valueOf(backupMetrics.getDeduplicated());
                        dataSize += Long.valueOf(backupMetrics.getSize());

                        // update backup metrics
                        Backup backupToFind = backupsInDb.stream()
                            .filter(backupInDb -> backupInDb.getExternalId().contains(backup.getId()))
                            .findAny()
                            .orElse(null);

                        if (backupToFind != null) {
                            BackupVO backupToUpdate = ((BackupVO) backupToFind);
                            backupToUpdate.setProtectedSize(usedSize);
                            backupToUpdate.setSize(dataSize);
                            backupDao.persist(backupToUpdate);
                        }

                    }
                }
                Metric metric = new Metric(dataSize, usedSize);
                logger.debug("Metrics for VM [uuid: {}, name: {}] is [backup size: {}, data size: {}].", vm.getUuid(),
                    vm.getInstanceName(), metric.getBackupSize(), metric.getDataSize());
                metrics.put(vm, metric);
            } catch (BackrollApiException | IOException e) {
                throw new CloudRuntimeException("Failed to retrieve backup metrics");
            }
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
    public Pair<Boolean, Backup> takeBackup(VirtualMachine vm) {
        logger.info("Starting backup for VM ID {} on backroll provider", vm.getUuid());
        final BackrollClient client = getClient(vm.getDataCenterId());

        try {
            String urlToRequest = client.startBackupJob(vm.getUuid());
            logger.info("BackrollProvider: urlToRequest: " + urlToRequest);
            String backupJob = urlToRequest.replace("/status/", "");
            if (!StringUtils.isEmpty(backupJob)) {
                BackupVO backup = new BackupVO();
                backup.setVmId(vm.getId());
                backup.setExternalId(backupJob);
                backup.setType("INCREMENTAL");
                backup.setDate(new DateTime().toDate());
                backup.setSize(0L);
                backup.setProtectedSize(0L);
                backup.setStatus(Backup.Status.BackingUp);
                backup.setBackupOfferingId(vm.getBackupOfferingId());
                backup.setAccountId(vm.getAccountId());
                backup.setDomainId(vm.getDomainId());
                backup.setZoneId(vm.getDataCenterId());
                Boolean result = backupDao.persist(backup) != null;
                return new Pair<Boolean,Backup>(result, backup);
            }
        } catch (ParseException | BackrollApiException | IOException e) {
            logger.debug(e.getMessage());
            throw new CloudRuntimeException("Failed to take backup");
        }
        return new Pair<Boolean,Backup>(false, null);
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
        logger.info("BACKROLL: delete backup id: {}", backup.getExternalId());
        if (backup.getStatus().equals(Backup.Status.BackingUp)) {
            throw new CloudRuntimeException("You can't delete a backup while it still BackingUp");
        } else {
            logger.debug("BACKROLL: try delete backup");

            if (backup.getStatus().equals(Backup.Status.Removed) || backup.getStatus().equals(Backup.Status.Failed)){
                return deleteBackupInDb(backup);
            } else {
                VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
                try {
                    if (getClient(backup.getZoneId()).deleteBackup(vm.getUuid(), getBackupName(backup))) {
                        logger.debug("BACKROLL: Backup deletion for backup {} complete on backroll side.", backup.getUuid());
                        return deleteBackupInDb(backup);
                    }
                } catch (BackrollApiException | IOException e) {
                    logger.error(e);
                    throw new CloudRuntimeException("BACKROLL: Failed to delete backup");
                }
            }
        }
        return false;
    }

    private boolean deleteBackupInDb(Backup backup) {
        BackupVO backupToUpdate = ((BackupVO) backup);
        backupToUpdate.setStatus(Backup.Status.Removed);
        if (backupDao.persist(backupToUpdate) != null) {
            logger.debug("BACKROLL: Backroll backup {} deleted in database.", backup.getUuid());
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
            return true;
        }
        return false;
    }

    protected BackrollClient getClient(final Long zoneId) {
        logger.debug("Backroll Provider GetClient with zone id {}", zoneId);
        try {
            if (backrollClient == null) {
                logger.debug("backroll client null - instantiation of new one ");
                BackrollHttpClientProvider provider = BackrollHttpClientProvider.createProvider(new BackrollHttpClientProvider(), BackrollUrlConfigKey.valueIn(zoneId), BackrollAppNameConfigKey.valueIn(zoneId), BackrollPasswordConfigKey.valueIn(zoneId), true, 300, 600);
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
        return getBackupName(backup.getExternalId());
    }

    private String getBackupName(String externalId) {
        return externalId.substring(externalId.indexOf(",") + 1);
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        logger.debug("Restoring volume {} from backup {} on the Backroll Backup Provider", volumeUuid, backup.getUuid());
        throw new CloudRuntimeException("Backroll plugin does not support this feature");
    }

    @Override
    public List<RestorePoint> listRestorePoints(VirtualMachine vm) {
        try {
            final BackrollClient client = getClient(vm.getDataCenterId());
            return client.listRestorePoints(vm.getUuid());
        } catch (BackrollApiException | IOException e) {
            logger.error(e);
            throw new CloudRuntimeException("Error while listing restore points");
        }
    }

    @Override
    public Backup createNewBackupEntryForRestorePoint(RestorePoint restorePoint, VirtualMachine vm, Metric metric) {
        final BackrollClient client = getClient(vm.getDataCenterId());
        BackupVO backupToInsert = new BackupVO();
        backupToInsert.setVmId(vm.getId());
        backupToInsert.setExternalId(restorePoint.getId());
        backupToInsert.setType("INCREMENTAL");
        backupToInsert.setDate(restorePoint.getCreated());
        backupToInsert.setStatus(Backup.Status.BackedUp);
        backupToInsert.setBackupOfferingId(vm.getBackupOfferingId());
        backupToInsert.setAccountId(vm.getAccountId());
        backupToInsert.setDomainId(vm.getDomainId());
        backupToInsert.setZoneId(vm.getDataCenterId());

        try {
            BackrollBackupMetrics backupMetrics = client.getBackupMetrics(vm.getUuid() , getBackupName(restorePoint.getId()));
            if (backupMetrics != null) {
                backupToInsert.setProtectedSize(backupMetrics.getDeduplicated());
                backupToInsert.setSize(backupMetrics.getSize());
            }
        } catch (IOException | BackrollApiException e) {
            logger.error(e);
        }

        backupDao.persist(backupToInsert);
        return backupToInsert;
    }
}
