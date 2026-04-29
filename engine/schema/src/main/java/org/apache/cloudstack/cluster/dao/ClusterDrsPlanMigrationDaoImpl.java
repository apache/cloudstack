/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster.dao;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.cluster.ClusterDrsPlanMigrationVO;
import org.apache.cloudstack.jobs.JobInfo;

import java.util.List;

public class ClusterDrsPlanMigrationDaoImpl extends GenericDaoBase<ClusterDrsPlanMigrationVO, Long> implements ClusterDrsPlanMigrationDao {
    public ClusterDrsPlanMigrationDaoImpl() {
    }

    @Override
    public List<ClusterDrsPlanMigrationVO> listByPlanId(long planId) {
        SearchBuilder<ClusterDrsPlanMigrationVO> sb = createSearchBuilder();
        sb.and("planId", sb.entity().getPlanId(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<ClusterDrsPlanMigrationVO> sc = sb.create();
        sc.setParameters("planId", planId);
        Filter filter = new Filter(ClusterDrsPlanMigrationVO.class, "id", true, null, null);
        return search(sc, filter);
    }

    @Override
    public List<ClusterDrsPlanMigrationVO> listPlanMigrationsToExecute(Long id) {
        SearchBuilder<ClusterDrsPlanMigrationVO> sb = createSearchBuilder();
        sb.and("planId", sb.entity().getPlanId(), SearchCriteria.Op.EQ);
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.NULL);
        sb.done();
        SearchCriteria<ClusterDrsPlanMigrationVO> sc = sb.create();
        sc.setParameters("planId", id);
        Filter filter = new Filter(ClusterDrsPlanMigrationVO.class, "id", true, null, null);
        return search(sc, filter);
    }

    @Override
    public List<ClusterDrsPlanMigrationVO> listPlanMigrationsInProgress(Long id) {
        SearchBuilder<ClusterDrsPlanMigrationVO> sb = createSearchBuilder();
        sb.and("planId", sb.entity().getPlanId(), SearchCriteria.Op.EQ);
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<ClusterDrsPlanMigrationVO> sc = sb.create();
        sc.setParameters("planId", id);
        sc.setParameters("status", JobInfo.Status.IN_PROGRESS);
        Filter filter = new Filter(ClusterDrsPlanMigrationVO.class, "id", true, null, null);
        return search(sc, filter);
    }
}
