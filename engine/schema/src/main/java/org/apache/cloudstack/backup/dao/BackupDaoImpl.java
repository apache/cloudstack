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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.backup.BackupTO;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.backup.Backup;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

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
        backupSearch.and("zone_id", backupSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        backupSearch.and("external_id", backupSearch.entity().getExternalId(), SearchCriteria.Op.EQ);
        backupSearch.done();
    }
    @Override
    public List<Backup> listByVmId(Long zoneId, Long vmId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("zone_id", zoneId);
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
        backupVO.setName(backup.getName());
        backupVO.setDescription(backup.getDescription());
        if (backup instanceof BackupTO) {
            String parentExternalId = ((BackupTO) backup).getParentExternalId();
            if (StringUtils.isNotBlank(parentExternalId)) {
                Backup parent = findByExternalId(backup.getZoneId(), parentExternalId);
                backupVO.setParentId(parent.getId());
            }
        }
        backupVO.setVmId(backup.getVmId());
        backupVO.setVolumeIds(backup.getVolumeIds());
        backupVO.setStatus(backup.getStatus());
        backupVO.setStartTime(backup.getStartTime());
        return backupVO;
    }

    public void removeExistingVMBackups(Long zoneId, Long vmId) {
        SearchCriteria<BackupVO> sc = backupSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("zone_id", zoneId);
        expunge(sc);
    }

    @Override
    public List<Backup> syncVMBackups(Long zoneId, Long vmId, List<Backup> externalBackups) {
        for (Backup backup : externalBackups) {
            BackupVO backupVO = getBackupVO(backup);
            persist(backupVO);
        }
        return listByVmId(zoneId, vmId);
    }

    @Override
    public BackupResponse newBackupResponse(Backup backup) {
        AccountVO account = accountDao.findById(backup.getAccountId());
        BackupVO parent = findById(backup.getParentId());
        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        DataCenterVO zone = dataCenterDao.findById(backup.getZoneId());

        BackupResponse backupResponse = new BackupResponse();
        backupResponse.setZoneId(zone.getUuid());
        backupResponse.setId(backup.getUuid());
        backupResponse.setAccountId(account.getUuid());
        backupResponse.setExternalId(backup.getExternalId());
        backupResponse.setName(backup.getName());
        backupResponse.setDescription(backup.getDescription());
        if (parent != null) {
            backupResponse.setParentId(parent.getUuid());
        }
        backupResponse.setVmId(vm.getUuid());
        if (CollectionUtils.isNotEmpty(backup.getVolumeIds())) {
            List<String> volIds = new ArrayList<>();
            for (Long volId : backup.getVolumeIds()) {
                VolumeVO volume = volumeDao.findById(volId);
                volIds.add(volume.getUuid());
            }
            backupResponse.setVolumeIds(volIds);
        }
        backupResponse.setStatus(backup.getStatus());
        backupResponse.setStartDate(backup.getStartTime());
        backupResponse.setObjectName("backup");
        return backupResponse;
    }
}
