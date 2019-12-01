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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.BackupScheduleResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupVO;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

public class BackupDaoImpl extends GenericDaoBase<BackupVO, Long> implements BackupDao {

    @Inject
    AccountDao accountDao;

    @Inject
    DataCenterDao dataCenterDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    VolumeDao volumeDao;

    private SearchBuilder<BackupVO> backupSearch;

    public BackupDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupSearch = createSearchBuilder();
        backupSearch.and("vm_id", backupSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        backupSearch.and("account_id", backupSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        backupSearch.and("zone_id", backupSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        backupSearch.and("external_id", backupSearch.entity().getExternalId(), SearchCriteria.Op.EQ);
        backupSearch.and("offering_id", backupSearch.entity().getOfferingId(), SearchCriteria.Op.EQ);
        backupSearch.and("status", backupSearch.entity().getStatus(), SearchCriteria.Op.EQ);
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
        sc.setParameters("zone_id", zoneId);
        return new ArrayList<>(listBy(sc));
    }

    @Override
    public List<Backup> listByOfferingId(Long offeringId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("offering_id", offeringId);
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
        backupVO.setZoneId(backup.getZoneId());
        backupVO.setAccountId(backup.getAccountId());
        backupVO.setExternalId(backup.getExternalId());
        backupVO.setVmId(backup.getVmId());
        backupVO.setStatus(backup.getStatus());
        backupVO.setCreated(backup.getCreated());
        return backupVO;
    }

    public void removeExistingBackups(Long zoneId, Long vmId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("zone_id", zoneId);
        expunge(sc);
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
    public List<Backup> listByZoneAndState(Long zoneId, Backup.Status state) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("zone_id", zoneId);
        if (state != null) {
            sc.setParameters("status", state);
            return new ArrayList<>(listIncludingRemovedBy(sc));
        }
        return new ArrayList<>(listBy(sc));
    }

    @Override
    public BackupResponse newBackupResponse(Backup backup) {
        AccountVO account = accountDao.findById(backup.getAccountId());
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        DataCenterVO zone = dataCenterDao.findById(backup.getZoneId());

        BackupResponse backupResponse = new BackupResponse();
        backupResponse.setZoneId(zone.getUuid());
        backupResponse.setId(backup.getUuid());
        backupResponse.setAccountId(account.getUuid());
        backupResponse.setExternalId(backup.getExternalId());
        backupResponse.setVmId(vm.getUuid());
        backupResponse.setVolumes(backup.getVolumes());
        backupResponse.setStatus(backup.getStatus());
        backupResponse.setSize(backup.getSize());
        backupResponse.setProtectedSize(backup.getProtectedSize());
        backupResponse.setCreatedDate(backup.getCreated());
        backupResponse.setObjectName("backup");
        return backupResponse;
    }

    @Override
    public BackupScheduleResponse newBackupScheduleResponse(Backup backup) {
        AccountVO account = accountDao.findById(backup.getAccountId());
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        DataCenterVO zone = dataCenterDao.findById(backup.getZoneId());

        BackupScheduleResponse response = new BackupScheduleResponse();
        response.setZoneId(zone.getUuid());
        response.setId(backup.getUuid());
        response.setAccountId(account.getUuid());
        response.setVmId(vm.getUuid());
        response.setVmName(vm.getHostName());
        response.setObjectName("backupschedule");
        return response;
    }
}
