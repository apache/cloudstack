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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.api.response.BackupOfferingResponse;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.BackupOfferingVO;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class BackupOfferingDaoImpl extends GenericDaoBase<BackupOfferingVO, Long> implements BackupOfferingDao {

    @Inject
    DataCenterDao dataCenterDao;

    private SearchBuilder<BackupOfferingVO> backupPoliciesSearch;

    public BackupOfferingDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupPoliciesSearch = createSearchBuilder();
        backupPoliciesSearch.and("name", backupPoliciesSearch.entity().getName(), SearchCriteria.Op.EQ);
        backupPoliciesSearch.and("zone_id", backupPoliciesSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        backupPoliciesSearch.and("external_id", backupPoliciesSearch.entity().getExternalId(), SearchCriteria.Op.EQ);
        backupPoliciesSearch.done();
    }

    @Override
    public BackupOfferingResponse newBackupOfferingResponse(BackupOffering offering, Boolean crossZoneInstanceCreation) {
        DataCenterVO zone = dataCenterDao.findById(offering.getZoneId());

        BackupOfferingResponse response = new BackupOfferingResponse();
        response.setId(offering.getUuid());
        response.setName(offering.getName());
        response.setDescription(offering.getDescription());
        response.setExternalId(offering.getExternalId());
        response.setProvider(offering.getProvider());
        response.setUserDrivenBackups(offering.isUserDrivenBackupAllowed());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }
        if (crossZoneInstanceCreation) {
            response.setCrossZoneInstanceCreation(true);
        }
        response.setCreated(offering.getCreated());
        response.setObjectName("backupoffering");
        return response;
    }

    @Override
    public BackupOffering findByExternalId(String externalId, Long zoneId) {
        SearchCriteria<BackupOfferingVO> sc = backupPoliciesSearch.create();
        sc.setParameters("external_id", externalId);
        if (zoneId != null) {
            sc.setParameters("zone_id", zoneId);
        }
        return findOneBy(sc);
    }

    @Override
    public BackupOffering findByName(String name, Long zoneId) {
        SearchCriteria<BackupOfferingVO> sc = backupPoliciesSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("zone_id", zoneId);
        return findOneBy(sc);
    }
}
