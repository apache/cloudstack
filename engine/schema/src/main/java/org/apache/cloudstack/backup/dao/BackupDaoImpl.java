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

package org.apache.cloudstack.backup.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupDetailVO;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.BackupVO;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;

public class BackupDaoImpl extends GenericDaoBase<BackupVO, Long> implements BackupDao {

    @Inject
    AccountDao accountDao;

    @Inject
    DomainDao domainDao;

    @Inject
    DataCenterDao dataCenterDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    BackupOfferingDao backupOfferingDao;

    @Inject
    BackupDetailsDao backupDetailsDao;

    private SearchBuilder<BackupVO> backupSearch;

    public BackupDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupSearch = createSearchBuilder();
        backupSearch.and("vm_id", backupSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        backupSearch.and("external_id", backupSearch.entity().getExternalId(), SearchCriteria.Op.EQ);
        backupSearch.and("backup_offering_id", backupSearch.entity().getBackupOfferingId(), SearchCriteria.Op.EQ);
        backupSearch.and("zone_id", backupSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        backupSearch.done();
    }

    @Override
    public List<Backup> listByAccountId(Long accountId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("account_id", accountId);
        return new ArrayList<>(listBy(sc));
    }

    @Override
    public Backup findByVmId(Long vmId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        return findOneBy(sc);
    }

    @Override
    public Backup findByVmIdIncludingRemoved(Long vmId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<Backup> listByVmId(Long zoneId, Long vmId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        if (zoneId != null) {
            sc.setParameters("zone_id", zoneId);
        }
        return new ArrayList<>(listBy(sc));
    }

    private Backup findByExternalId(Long zoneId, String externalId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("external_id", externalId);
        sc.setParameters("zone_id", zoneId);
        return findOneBy(sc);
    }

    public BackupVO getBackupVO(Backup backup) {
        BackupVO backupVO = new BackupVO();
        backupVO.setExternalId(backup.getExternalId());
        backupVO.setVmId(backup.getVmId());
        return backupVO;
    }

    @Override
    public List<Backup> listByOfferingId(Long backupOfferingId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("backup_offering_id", backupOfferingId);
        return new ArrayList<>(listBy(sc));
    }

    public void removeExistingBackups(Long zoneId, Long vmId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("zone_id", zoneId);
        expunge(sc);
    }

    @Override
    public BackupVO persist(BackupVO backup) {
        return Transaction.execute((TransactionCallback<BackupVO>) status -> {
            BackupVO backupDb = super.persist(backup);
            saveDetails(backup);
            loadDetails(backupDb);
            return backupDb;
        });
    }

    @Override
    public boolean update(Long id, BackupVO backup) {
        return Transaction.execute((TransactionCallback<Boolean>) status -> {
            boolean result = super.update(id, backup);
            if (result == true) {
                saveDetails(backup);
            }
            return result;
        });
    }

    @Override
    public List<Backup> syncBackups(Long zoneId, Long vmId, List<Backup> externalBackups) {
        for (Backup backup : externalBackups) {
            BackupVO backupVO = getBackupVO(backup);
            persist(backupVO);
        }
        return listByVmId(zoneId, vmId);
    }

    @Override
    public void loadDetails(BackupVO backup) {
        Map<String, String> details = backupDetailsDao.listDetailsKeyPairs(backup.getId());
        backup.setDetails(details);
    }

    @Override
    public void saveDetails(BackupVO backup) {
        Map<String, String> detailsStr = backup.getDetails();
        if (detailsStr == null) {
            return;
        }
        List<BackupDetailVO> details = new ArrayList<BackupDetailVO>();
        for (String key : detailsStr.keySet()) {
            BackupDetailVO detail = new BackupDetailVO(backup.getId(), key, detailsStr.get(key), true);
            details.add(detail);
        }
        backupDetailsDao.saveDetails(details);
    }

    @Override
    public BackupResponse newBackupResponse(Backup backup) {
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        AccountVO account = accountDao.findByIdIncludingRemoved(vm.getAccountId());
        DomainVO domain = domainDao.findByIdIncludingRemoved(vm.getDomainId());
        DataCenterVO zone = dataCenterDao.findByIdIncludingRemoved(vm.getDataCenterId());
        Long offeringId = backup.getBackupOfferingId();
        if (offeringId == null) {
            offeringId = vm.getBackupOfferingId();
        }
        BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(offeringId);

        BackupResponse response = new BackupResponse();
        response.setId(backup.getUuid());
        response.setVmId(vm.getUuid());
        response.setVmName(vm.getHostName());
        response.setExternalId(backup.getExternalId());
        response.setType(backup.getType());
        response.setDate(backup.getDate());
        response.setSize(backup.getSize());
        response.setProtectedSize(backup.getProtectedSize());
        response.setStatus(backup.getStatus());
        // ACS 4.20: For backups taken prior this release the backup.backed_volumes column would be empty hence use vm_instance.backup_volumes
        String backedUpVolumes;
        if (Objects.isNull(backup.getBackedUpVolumes())) {
            backedUpVolumes = new Gson().toJson(vm.getBackupVolumeList().toArray(), Backup.VolumeInfo[].class);
        } else {
            backedUpVolumes = new Gson().toJson(backup.getBackedUpVolumes().toArray(), Backup.VolumeInfo[].class);
        }
        response.setVolumes(backedUpVolumes);
        response.setBackupOfferingId(offering.getUuid());
        response.setBackupOffering(offering.getName());
        response.setAccountId(account.getUuid());
        response.setAccount(account.getAccountName());
        response.setDomainId(domain.getUuid());
        response.setDomain(domain.getName());
        response.setZoneId(zone.getUuid());
        response.setZone(zone.getName());

        Map<String, String> details = backupDetailsDao.listDetailsKeyPairs(backup.getId(), true);
        if (details != null) {
            HashMap<String, String> vmDetails = new HashMap<>();
            vmDetails.put(ApiConstants.HYPERVISOR, details.get(ApiConstants.HYPERVISOR));
            vmDetails.put(ApiConstants.TEMPLATE_ID, details.get(ApiConstants.TEMPLATE_ID));
            vmDetails.put(ApiConstants.SERVICE_OFFERING_ID, details.get(ApiConstants.SERVICE_OFFERING_ID));
            vmDetails.put(ApiConstants.NETWORK_IDS, details.get(ApiConstants.NETWORK_IDS));
            vmDetails.put(ApiConstants.DISK_OFFERING_IDS, details.get(ApiConstants.DISK_OFFERING_IDS));
            vmDetails.put(ApiConstants.DISK_SIZES, details.get(ApiConstants.DISK_SIZES));
            vmDetails.put(ApiConstants.MIN_IOPS, details.get(ApiConstants.MIN_IOPS));
            vmDetails.put(ApiConstants.MAX_IOPS, details.get(ApiConstants.MAX_IOPS));
            response.setVmDetails(vmDetails);
        }

        response.setObjectName("backup");
        return response;
    }
}
