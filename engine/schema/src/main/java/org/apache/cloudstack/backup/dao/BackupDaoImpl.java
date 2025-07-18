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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.cloud.storage.Storage;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.db.GenericSearchBuilder;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupDetailVO;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.commons.collections.CollectionUtils;

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
import com.cloud.vm.VirtualMachine;
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
    private VMTemplateDao templateDao;

    @Inject
    BackupOfferingDao backupOfferingDao;

    @Inject
    BackupDetailsDao backupDetailsDao;

    private SearchBuilder<BackupVO> backupSearch;
    private GenericSearchBuilder<BackupVO, Long> CountBackupsByAccount;
    private GenericSearchBuilder<BackupVO, SumCount> CalculateBackupStorageByAccount;
    private SearchBuilder<BackupVO> ListBackupsByVMandIntervalType;
    private GenericSearchBuilder<BackupVO, Long> backupVmSearchInZone;

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

        backupVmSearchInZone = createSearchBuilder(Long.class);
        backupVmSearchInZone.select(null, SearchCriteria.Func.DISTINCT, backupVmSearchInZone.entity().getVmId());
        backupVmSearchInZone.and("zone_id", backupVmSearchInZone.entity().getZoneId(), SearchCriteria.Op.EQ);
        backupVmSearchInZone.done();

        CountBackupsByAccount = createSearchBuilder(Long.class);
        CountBackupsByAccount.select(null, SearchCriteria.Func.COUNT, null);
        CountBackupsByAccount.and("account", CountBackupsByAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        CountBackupsByAccount.and("status", CountBackupsByAccount.entity().getStatus(), SearchCriteria.Op.NIN);
        CountBackupsByAccount.and("removed", CountBackupsByAccount.entity().getRemoved(), SearchCriteria.Op.NULL);
        CountBackupsByAccount.done();

        CalculateBackupStorageByAccount = createSearchBuilder(SumCount.class);
        CalculateBackupStorageByAccount.select("sum", SearchCriteria.Func.SUM, CalculateBackupStorageByAccount.entity().getSize());
        CalculateBackupStorageByAccount.and("account", CalculateBackupStorageByAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        CalculateBackupStorageByAccount.and("status", CalculateBackupStorageByAccount.entity().getStatus(), SearchCriteria.Op.NIN);
        CalculateBackupStorageByAccount.and("removed", CalculateBackupStorageByAccount.entity().getRemoved(), SearchCriteria.Op.NULL);
        CalculateBackupStorageByAccount.done();

        ListBackupsByVMandIntervalType = createSearchBuilder();
        ListBackupsByVMandIntervalType.and("vmId", ListBackupsByVMandIntervalType.entity().getVmId(), SearchCriteria.Op.EQ);
        ListBackupsByVMandIntervalType.and("intervalType", ListBackupsByVMandIntervalType.entity().getBackupIntervalType(), SearchCriteria.Op.EQ);
        ListBackupsByVMandIntervalType.and("status", ListBackupsByVMandIntervalType.entity().getStatus(), SearchCriteria.Op.EQ);
        ListBackupsByVMandIntervalType.and("removed", ListBackupsByVMandIntervalType.entity().getRemoved(), SearchCriteria.Op.NULL);
        ListBackupsByVMandIntervalType.done();
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

    @Override
    public List<Backup> listByVmIdAndOffering(Long zoneId, Long vmId, Long offeringId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        if (zoneId != null) {
            sc.setParameters("zone_id", zoneId);
        }
        sc.setParameters("backup_offering_id", offeringId);
        return new ArrayList<>(listBy(sc));
    }

    private Backup findByExternalId(Long zoneId, String externalId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("external_id", externalId);
        sc.setParameters("zone_id", zoneId);
        return findOneBy(sc);
    }

    @Override
    public List<BackupVO> searchByVmIds(List<Long> vmIds) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return new ArrayList<>();
        }
        SearchBuilder<BackupVO> sb = createSearchBuilder();
        sb.and("vmIds", sb.entity().getVmId(), SearchCriteria.Op.IN);
        SearchCriteria<BackupVO> sc = sb.create();
        sc.setParameters("vmIds", vmIds.toArray());
        return search(sc, null);
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
            if (result) {
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
    public Long countBackupsForAccount(long accountId) {
        SearchCriteria<Long> sc = CountBackupsByAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("status", Backup.Status.Failed, Backup.Status.Removed, Backup.Status.Expunged);
        return customSearch(sc, null).get(0);
    }

    @Override
    public Long calculateBackupStorageForAccount(long accountId) {
        SearchCriteria<SumCount> sc = CalculateBackupStorageByAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("status", Backup.Status.Failed, Backup.Status.Removed, Backup.Status.Expunged);
        return customSearch(sc, null).get(0).sum;
    }

    @Override
    public List<BackupVO> listBackupsByVMandIntervalType(Long vmId, Backup.Type backupType) {
        SearchCriteria<BackupVO> sc = ListBackupsByVMandIntervalType.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("type", backupType.ordinal());
        sc.setParameters("status", Backup.Status.BackedUp);
        return listBy(sc, null);
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
    public List<Long> listVmIdsWithBackupsInZone(Long zoneId) {
        SearchCriteria<Long> sc = backupVmSearchInZone.create();
        sc.setParameters("zone_id", zoneId);
        return customSearchIncludingRemoved(sc, null);
    }

    @Override
    public BackupResponse newBackupResponse(Backup backup, Boolean listVmDetails) {
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        AccountVO account = accountDao.findByIdIncludingRemoved(backup.getAccountId());
        DomainVO domain = domainDao.findByIdIncludingRemoved(backup.getDomainId());
        DataCenterVO zone = dataCenterDao.findByIdIncludingRemoved(backup.getZoneId());
        Long offeringId = backup.getBackupOfferingId();
        BackupOffering offering = backupOfferingDao.findByIdIncludingRemoved(offeringId);

        BackupResponse response = new BackupResponse();
        response.setId(backup.getUuid());
        response.setName(backup.getName());
        response.setDescription(backup.getDescription());
        response.setVmName(vm.getHostName());
        if (vm.getState() != VirtualMachine.State.Expunging) {
            response.setVmId(vm.getUuid());
        }
        if (vm.getBackupOfferingId() == null || vm.getBackupOfferingId() != backup.getBackupOfferingId()) {
            response.setVmOfferingRemoved(true);
        }
        response.setExternalId(backup.getExternalId());
        response.setType(backup.getType());
        response.setDate(backup.getDate());
        response.setSize(backup.getSize());
        response.setProtectedSize(backup.getProtectedSize());
        response.setStatus(backup.getStatus());
        if (backup.getBackupIntervalType() != null) {
            response.setIntervalType(Backup.Type.values()[backup.getBackupIntervalType()].toString());
        }
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

        if (Boolean.TRUE.equals(listVmDetails)) {
            Map<String, String> details = backupDetailsDao.listDetailsKeyPairs(backup.getId(), true);
            details.put(ApiConstants.HYPERVISOR, vm.getHypervisorType().toString());
            VirtualMachineTemplate template = templateDao.findById(vm.getTemplateId());
            if (template != null) {
                details.put(ApiConstants.TEMPLATE_ID, template.getUuid());
                details.put(ApiConstants.IS_ISO, String.valueOf(template.getFormat().equals(Storage.ImageFormat.ISO)));
            }
            if (details != null) {
                response.setVmDetails(details);
            }
        }

        response.setObjectName("backup");
        return response;
    }
}
