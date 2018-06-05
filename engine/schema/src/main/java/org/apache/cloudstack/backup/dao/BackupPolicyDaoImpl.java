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

import org.apache.cloudstack.api.response.BackupPolicyResponse;
import org.apache.cloudstack.backup.BackupPolicy;
import org.apache.cloudstack.backup.BackupPolicyVO;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class BackupPolicyDaoImpl extends GenericDaoBase<BackupPolicyVO, Long> implements BackupPolicyDao {

    @Inject
    DataCenterDao dataCenterDao;

    private SearchBuilder<BackupPolicyVO> backupPoliciesSearch;

    public BackupPolicyDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupPoliciesSearch = createSearchBuilder();
        backupPoliciesSearch.and("zone_id", backupPoliciesSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        backupPoliciesSearch.done();
    }

    @Override
    public BackupPolicyResponse newBackupPolicyResponse(BackupPolicy policy) {
        DataCenterVO zone = dataCenterDao.findById(policy.getZoneId());

        BackupPolicyResponse response = new BackupPolicyResponse();
        if (policy.isImported()) {
            response.setId(policy.getUuid());
            response.setZoneId(zone.getUuid());
        }
        response.setName(policy.getName());
        response.setDescription(policy.getDescription());
        response.setExternalId(policy.getExternalId());
        response.setObjectName("backuppolicy");
        return response;
    }

    @Override
    public List<BackupPolicy> listByZone(Long zoneId) {
        SearchCriteria<BackupPolicyVO> sc = backupPoliciesSearch.create();
        if (zoneId != null) {
            sc.setParameters("zone_id", zoneId);
        }
        return new ArrayList<>(listBy(sc));
    }
}
