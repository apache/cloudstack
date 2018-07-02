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

import org.apache.cloudstack.api.response.VMBackupResponse;
import org.apache.cloudstack.backup.VMBackup;
import org.apache.cloudstack.backup.VMBackupVO;
import org.apache.commons.collections.CollectionUtils;

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

public class VMBackupDaoImpl extends GenericDaoBase<VMBackupVO, Long> implements VMBackupDao {

    @Inject
    AccountDao accountDao;

    @Inject
    DataCenterDao dataCenterDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    VolumeDao volumeDao;

    private SearchBuilder<VMBackupVO> backupSearch;

    public VMBackupDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupSearch = createSearchBuilder();
        backupSearch.and("vm_id", backupSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        backupSearch.and("zone_id", backupSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        backupSearch.and("external_id", backupSearch.entity().getExternalId(), SearchCriteria.Op.EQ);
        backupSearch.and("status", backupSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        backupSearch.done();
    }
    @Override
    public List<VMBackup> listByVmId(Long zoneId, Long vmId) {
        SearchCriteria<VMBackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("zone_id", zoneId);
        return new ArrayList<>(listBy(sc));
    }

    private VMBackup findByExternalId(Long zoneId, String externalId) {
        SearchCriteria<VMBackupVO> sc = backupSearch.create();
        sc.setParameters("external_id", externalId);
        sc.setParameters("zone_id", zoneId);
        return findOneBy(sc);
    }

    public VMBackupVO getBackupVO(VMBackup backup) {
        VMBackupVO backupVO = new VMBackupVO();
        backupVO.setZoneId(backup.getZoneId());
        backupVO.setAccountId(backup.getAccountId());
        backupVO.setExternalId(backup.getExternalId());
        backupVO.setName(backup.getName());
        backupVO.setDescription(backup.getDescription());
        backupVO.setVmId(backup.getVmId());
        backupVO.setStatus(backup.getStatus());
        backupVO.setCreated(backup.getCreated());
        return backupVO;
    }

    public void removeExistingVMBackups(Long zoneId, Long vmId) {
        SearchCriteria<VMBackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("zone_id", zoneId);
        expunge(sc);
    }

    @Override
    public List<VMBackup> syncVMBackups(Long zoneId, Long vmId, List<VMBackup> externalBackups) {
        for (VMBackup backup : externalBackups) {
            VMBackupVO backupVO = getBackupVO(backup);
            persist(backupVO);
        }
        return listByVmId(zoneId, vmId);
    }

    @Override
    public List<VMBackup> listByZoneAndState(Long zoneId, VMBackup.Status state) {
        SearchCriteria<VMBackupVO> sc = backupSearch.create();
        sc.setParameters("zone_id", zoneId);
        if (state != null) {
            sc.setParameters("status", state);
        }
        return new ArrayList<>(listIncludingRemovedBy(sc));
    }

    @Override
    public VMBackupResponse newBackupResponse(VMBackup backup) {
        AccountVO account = accountDao.findById(backup.getAccountId());
        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        DataCenterVO zone = dataCenterDao.findById(backup.getZoneId());

        VMBackupResponse backupResponse = new VMBackupResponse();
        backupResponse.setZoneId(zone.getUuid());
        backupResponse.setId(backup.getUuid());
        backupResponse.setAccountId(account.getUuid());
        backupResponse.setExternalId(backup.getExternalId());
        backupResponse.setName(backup.getName());
        backupResponse.setDescription(backup.getDescription());
        backupResponse.setVmId(vm.getUuid());
        if (CollectionUtils.isNotEmpty(backup.getBackedUpVolumes())) {
            List<String> volIds = new ArrayList<>();
            for (VMBackup.VolumeInfo volInfo : backup.getBackedUpVolumes()) {
                volIds.add(volInfo.toString());
            }
            backupResponse.setVolumeIds(volIds);
        }
        backupResponse.setStatus(backup.getStatus());
        backupResponse.setSize(backup.getSize());
        backupResponse.setProtectedSize(backup.getProtectedSize());
        backupResponse.setCreatedDate(backup.getCreated());
        backupResponse.setObjectName("backup");
        return backupResponse;
    }
}
