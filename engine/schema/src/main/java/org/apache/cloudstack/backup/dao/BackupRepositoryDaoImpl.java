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

import org.apache.cloudstack.backup.BackupOfferingVO;
import org.apache.cloudstack.backup.BackupRepository;
import org.apache.cloudstack.backup.BackupRepositoryVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class BackupRepositoryDaoImpl extends GenericDaoBase<BackupRepositoryVO, Long> implements BackupRepositoryDao {
    @Inject
    BackupOfferingDao backupOfferingDao;

    private SearchBuilder<BackupRepositoryVO> backupRepoSearch;

    public BackupRepositoryDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupRepoSearch = createSearchBuilder();
        backupRepoSearch.and("zone_id", backupRepoSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        backupRepoSearch.and("provider", backupRepoSearch.entity().getProvider(), SearchCriteria.Op.EQ);
        backupRepoSearch.done();
    }

    @Override
    public List<BackupRepository> listByZoneAndProvider(Long zoneId, String provider) {
        SearchCriteria<BackupRepositoryVO> sc = backupRepoSearch.create();
        sc.setParameters("zone_id", zoneId);
        sc.setParameters("provider", provider);
        return new ArrayList<>(listBy(sc));
    }

    @Override
    public BackupRepository findByBackupOfferingId(Long backupOfferingId) {
        BackupOfferingVO offering = backupOfferingDao.findByIdIncludingRemoved(backupOfferingId);
        if (offering == null) {
            return null;
        }
        return findByUuid(offering.getExternalId());
    }
}
