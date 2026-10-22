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

import org.apache.cloudstack.backup.BackupUsageMetricVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class BackupUsageMetricDaoImpl extends GenericDaoBase<BackupUsageMetricVO, Long> implements BackupUsageMetricDao {
    private SearchBuilder<BackupUsageMetricVO> vmAndOfferingSearch;

    @PostConstruct
    protected void init() {
        vmAndOfferingSearch = createSearchBuilder();
        vmAndOfferingSearch.and("vmId", vmAndOfferingSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        vmAndOfferingSearch.and("backupOfferingId", vmAndOfferingSearch.entity().getBackupOfferingId(), SearchCriteria.Op.EQ);
        vmAndOfferingSearch.done();
    }

    private SearchCriteria<BackupUsageMetricVO> createVmAndOfferingCriteria(long vmId, long backupOfferingId) {
        SearchCriteria<BackupUsageMetricVO> sc = vmAndOfferingSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("backupOfferingId", backupOfferingId);
        return sc;
    }

    @Override
    public BackupUsageMetricVO findByVmAndOffering(long vmId, long backupOfferingId) {
        return findOneBy(createVmAndOfferingCriteria(vmId, backupOfferingId));
    }

    @Override
    public int removeByVmAndOffering(long vmId, long backupOfferingId) {
        return remove(createVmAndOfferingCriteria(vmId, backupOfferingId));
    }
}
