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
import java.text.MessageFormat;
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
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class BackrollBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    private static final Logger s_logger = Logger.getLogger(BackrollBackupProvider.class);
    public static final String BACKUP_IDENTIFIER = "-CSBKP-";

    public ConfigKey<String> BackrollUrlConfigKey = new ConfigKey<>("Advanced", String.class,
    "backup.plugin.backroll.config.url",
    "http://api.backup.demo.ccc:5050/api/v1",
    "Url for backroll plugin by DIMSI", true, ConfigKey.Scope.Zone);

    public ConfigKey<String> BackrollAppNameConfigKey = new ConfigKey<>("Advanced", String.class,
    "backup.plugin.backroll.config.appname",
    "backroll_api",
    "App Name for backroll plugin by DIMSI", true, ConfigKey.Scope.Zone);

    public ConfigKey<String> BackrollPasswordConfigKey = new ConfigKey<>("Advanced", String.class,
    "backup.plugin.backroll.config.password",
    "VviX8dALauSyYJMqVYJqf3UyZOpO3joS",
    "Password for backroll plugin by DIMSI", true, ConfigKey.Scope.Zone);

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
        s_logger.debug("Listing backup policies on backroll B&R Plugin");
        BackrollClient client = getClient(zoneId);
        String urlToRequest = client.getBackupOfferingUrl();
        if(!StringUtils.isEmpty(urlToRequest)){
            return client.getBackupOfferings(urlToRequest);
        }
        return new ArrayList<BackupOffering>();
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        s_logger.info("Checking if backup offering exists on the Backroll Backup Provider");
        return true;
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        s_logger.info("Creating VM backup for VM " + vm.getInstanceName() + " from backup offering " + backupOffering.getName());
        ((VMInstanceVO) vm).setBackupExternalId(backupOffering.getUuid());
        return true;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        s_logger.debug("Restoring vm " + vm.getUuid() + "from backup " + backup.getUuid() + " on the backroll Backup Provider");
        boolean isSuccess = getClient(vm.getDataCenterId()).restoreVMFromBackup(vm.getUuid(), getBackupName(backup));
        return isSuccess;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        s_logger.debug("Restoring volume " + volumeUuid + "from backup " + backup.getUuid() + " on the backroll Backup Provider");
        throw new CloudRuntimeException("Backroll plugin does not support this feature");
    }

    @Override
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms) {
        final Map<VirtualMachine, Backup.Metric> metrics = new HashMap<>();
        if (CollectionUtils.isEmpty(vms)) {
            s_logger.warn("Unable to get VM Backup Metrics because the list of VMs is empty.");
            return metrics;
        }

        List<String> vmUuids = vms.stream().filter(Objects::nonNull).map(VirtualMachine::getUuid).collect(Collectors.toList());
        s_logger.debug(String.format("Get Backup Metrics for VMs: [%s].", String.join(", ", vmUuids)));

        for (final VirtualMachine vm : vms) {
            if (vm == null) {
                continue;
            }

            Metric metric = getClient(zoneId).getVirtualMachineMetrics(vm.getUuid());
            s_logger.debug(String.format("Metrics for VM [uuid: %s, name: %s] is [backup size: %s, data size: %s].", vm.getUuid(),
                    vm.getInstanceName(), metric.getBackupSize(), metric.getDataSize()));
            metrics.put(vm, metric);
        }
        return metrics;
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        s_logger.info("Removing VM ID " + vm.getUuid() + " from Backrool backup offering ");

        boolean isAnyProblemWhileRemovingBackups = false;

        List<Backup> backupsInCs = backupDao.listByVmId(null, vm.getId());

        for (Backup backup : backupsInCs) {
            s_logger.debug("Trying to remove backup with id" + backup.getId());

            if (getClient(backup.getZoneId()).deleteBackup(vm.getUuid(), getBackupName(backup))) {
                var message = MessageFormat.format("Backup {0} deleted in Backroll for virtual machine {1}", backup.getId(), vm.getName());
                s_logger.info(message);
                if(!backupDao.remove(backup.getId())){
                    isAnyProblemWhileRemovingBackups = true;
                }
                message = MessageFormat.format("Backup {0} deleted in CS for virtual machine {1}", backup.getId(), vm.getName());
                s_logger.info(message);
            } else {
                isAnyProblemWhileRemovingBackups = false;
            }
        }

        if(isAnyProblemWhileRemovingBackups) {
            var message = MessageFormat.format("Problems occured while removing some backups for virtual machine {0}", vm.getName());
            s_logger.info(message);
        }
        return isAnyProblemWhileRemovingBackups;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public boolean takeBackup(VirtualMachine vm) {
        s_logger.info("Starting backup for VM ID " + vm.getUuid() + " on backroll provider");
        final BackrollClient client = getClient(vm.getDataCenterId());

        String idBackupTask = client.startBackupJob(vm.getUuid());
        if(!StringUtils.isEmpty(idBackupTask)) {
            BackupVO backup = new BackupVO();
            backup.setVmId(vm.getId());
            backup.setExternalId(idBackupTask);
            backup.setType("INCREMENTIAL");
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
        s_logger.info("Starting sync backup for VM ID " + vm.getUuid() + " on backroll provider");

        final BackrollClient client = getClient(vm.getDataCenterId());
        List<Backup> backupsInDb = backupDao.listByVmId(null, vm.getId());

        for (Backup backup : backupsInDb) {
            if(backup.getStatus().equals(Backup.Status.BackingUp)) {
                BackrollTaskStatus response = client.checkBackupTaskStatus(backup.getExternalId());
                if(response != null) {
                    s_logger.debug("backroll backup id: " + backup.getExternalId());
                    s_logger.debug("backroll backup status: " + response.getState());

                    BackupVO backupToUpdate = new BackupVO();
                    backupToUpdate.setVmId(backup.getVmId());
                    backupToUpdate.setExternalId(backup.getExternalId());
                    backupToUpdate.setType(backup.getType());
                    backupToUpdate.setDate(backup.getDate());
                    backupToUpdate.setSize(backup.getSize());
                    backupToUpdate.setProtectedSize(backup.getProtectedSize());
                    backupToUpdate.setBackupOfferingId(vm.getBackupOfferingId());
                    backupToUpdate.setAccountId(backup.getAccountId());
                    backupToUpdate.setDomainId(backup.getDomainId());
                    backupToUpdate.setZoneId(backup.getZoneId());

                    if(response.getState().equals("PENDING")) {
                        backupToUpdate.setStatus(Backup.Status.BackingUp);
                    } else if(response.getState().equals("FAILURE")) {
                        backupToUpdate.setStatus(Backup.Status.Failed);
                    } else if(response.getState().equals("SUCCESS")) {
                        backupToUpdate.setStatus(Backup.Status.BackedUp);
                        backupToUpdate.setExternalId(backup.getExternalId() + "," + response.getInfo());

                        BackrollBackupMetrics backupMetrics = client.getBackupMetrics(vm.getUuid() , response.getInfo());
                        if(backupMetrics != null) {
                            backupToUpdate.setSize(backupMetrics.getDeduplicated()); // real size
                            backupToUpdate.setProtectedSize(backupMetrics.getSize()); // total size
                        }
                    } else {
                        backupToUpdate.setStatus(Backup.Status.BackingUp);
                    }

                    if(backupDao.persist(backupToUpdate) != null) {
                        s_logger.info("Backroll mise à jour enregistrée");
                        backupDao.remove(backup.getId());
                    }
                }
            } else if(backup.getStatus().equals(Backup.Status.BackedUp) && backup.getSize().equals(0L)) {
                String backupId = backup.getExternalId().contains(",") ? backup.getExternalId().split(",")[1] : backup.getExternalId();
                BackrollBackupMetrics backupMetrics = client.getBackupMetrics(vm.getUuid() , backupId);
                if(backupMetrics != null) {
                    BackupVO backupToUpdate = ((BackupVO) backup);
                    backupToUpdate.setSize(backupMetrics.getDeduplicated()); // real size
                    backupToUpdate.setProtectedSize(backupMetrics.getSize()); // total size
                    backupDao.persist(backupToUpdate);
                }
            }
        }

        // Backups synchronisation between Backroll ad CS Db
        List<BackrollVmBackup> backupsFromBackroll = client.getAllBackupsfromVirtualMachine(vm.getUuid());
        backupsInDb = backupDao.listByVmId(null, vm.getId());

        // insert new backroll backup in CS
        for (BackrollVmBackup backupInBackroll : backupsFromBackroll) {
            Backup backupToFind = backupsInDb.stream()
                .filter(backupInDb -> backupInDb.getExternalId().contains(backupInBackroll.getName()))
                .findAny()
                .orElse(null);

            if(backupToFind == null) {
                BackupVO backupToInsert = new BackupVO();
                backupToInsert.setVmId(vm.getId());
                backupToInsert.setExternalId(backupInBackroll.getId() + "," + backupInBackroll.getName());
                backupToInsert.setType("INCREMENTIAL");
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
            if(backupToFind != null && backupToFind.getStatus() == Backup.Status.Removed) {
                BackupVO backupToUpdate = ((BackupVO) backupToFind);
                backupToUpdate.setStatus(Backup.Status.BackedUp);
                if(backupDao.persist(backupToUpdate) != null) {
                    s_logger.info("Backroll mise à jour enregistrée");
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

            if(backupToFind == null) {
                BackupVO backupToUpdate = ((BackupVO) backup);
                backupToUpdate.setStatus(Backup.Status.Removed);
                if(backupDao.persist(backupToUpdate) != null) {
                    s_logger.debug("Backroll suppression enregistrée (sync)");
                }
            }
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
        s_logger.info("backroll delete backup id: " + backup.getExternalId());
        if(backup.getStatus().equals(Backup.Status.BackingUp)) {
            throw new CloudRuntimeException("You can't delete a backup while it still BackingUp");
        } else {
            s_logger.debug("backroll - try delete backup");
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());

            if(backup.getStatus().equals(Backup.Status.Removed) || backup.getStatus().equals(Backup.Status.Failed)){
                return deleteBackupInDb(backup);
            } else {
                if (getClient(backup.getZoneId()).deleteBackup(vm.getUuid(), getBackupName(backup))) {
                    s_logger.debug("backroll delete backup ok on backroll side");
                    return deleteBackupInDb(backup);
                }
            }
        }
        return false;
    }

    private boolean deleteBackupInDb(Backup backup) {
        BackupVO backupToUpdate = ((BackupVO) backup);
        backupToUpdate.setStatus(Backup.Status.Removed);
        if(backupDao.persist(backupToUpdate) != null) {
            s_logger.debug("Backroll backup delete in database");
            return true;
        }
        return false;
    }

    protected BackrollClient getClient(final Long zoneId) {
        s_logger.debug("Backroll Provider GetClient");
        try {
            if(backrollClient == null){
                s_logger.debug("backroll client null - instanciation of new one ");
                backrollClient = new BackrollClient(BackrollUrlConfigKey.valueIn(zoneId), BackrollAppNameConfigKey.valueIn(zoneId), BackrollPasswordConfigKey.valueIn(zoneId), true, 300, 600);
            }
            return backrollClient;
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Failed to parse Backroll API URL: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            s_logger.info("Failed to build Backroll API client due to: ", e);
        }
        throw new CloudRuntimeException("Failed to build Backroll API client");
    }

    private String getBackupName(Backup backup) {
        return backup.getExternalId().substring(backup.getExternalId().indexOf(",") + 1);
    }
}
