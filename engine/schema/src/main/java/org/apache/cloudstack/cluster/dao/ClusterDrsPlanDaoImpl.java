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

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.cluster.ClusterDrsPlan;
import org.apache.cloudstack.cluster.ClusterDrsPlanVO;

import java.util.Date;
import java.util.List;

public class ClusterDrsPlanDaoImpl extends GenericDaoBase<ClusterDrsPlanVO, Long> implements ClusterDrsPlanDao {
    public ClusterDrsPlanDaoImpl() {
    }

    @Override
    public List<ClusterDrsPlanVO> listByStatus(ClusterDrsPlan.Status status) {
        SearchBuilder<ClusterDrsPlanVO> sb;
        sb = createSearchBuilder();
        sb.and(ApiConstants.STATUS, sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<ClusterDrsPlanVO> sc = sb.create();
        sc.setParameters(ApiConstants.STATUS, status);
        return search(sc, null);
    }

    @Override
    public List<ClusterDrsPlanVO> listByClusterIdAndStatus(Long clusterId, ClusterDrsPlan.Status status) {
        SearchBuilder<ClusterDrsPlanVO> sb;
        sb = createSearchBuilder();
        sb.and(ApiConstants.CLUSTER_ID, sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and(ApiConstants.STATUS, sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<ClusterDrsPlanVO> sc = sb.create();
        sc.setParameters(ApiConstants.CLUSTER_ID, clusterId);
        sc.setParameters(ApiConstants.STATUS, status);
        return search(sc, null);
    }

    @Override
    public ClusterDrsPlanVO listLatestPlanForClusterId(Long clusterId) {
        SearchBuilder<ClusterDrsPlanVO> sb;
        sb = createSearchBuilder();
        sb.and(ApiConstants.CLUSTER_ID, sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<ClusterDrsPlanVO> sc = sb.create();
        sc.setParameters(ApiConstants.CLUSTER_ID, clusterId);
        Filter filter = new Filter(ClusterDrsPlanVO.class, "id", false, 0L, 1L);
        List<ClusterDrsPlanVO> plans = listBy(sc, filter);
        if (plans != null && !plans.isEmpty()) {
            return plans.get(0);
        }
        return null;
    }

    @Override
    public Pair<List<ClusterDrsPlanVO>, Integer> searchAndCount(Long clusterId, Long planId, Long startIndex,
                                                                Long pageSizeVal) {
        SearchBuilder<ClusterDrsPlanVO> sb;
        sb = createSearchBuilder();
        sb.and(ApiConstants.CLUSTER_ID, sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and(ApiConstants.ID, sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<ClusterDrsPlanVO> sc = sb.create();
        if (clusterId != null) {
            sc.setParameters(ApiConstants.CLUSTER_ID, clusterId);
        }
        if (planId != null) {
            sc.setParameters(ApiConstants.ID, planId);
        }
        Filter filter = new Filter(ClusterDrsPlanVO.class, "id", false, startIndex, pageSizeVal);
        return searchAndCount(sc, filter);
    }

    @Override
    public int expungeBeforeDate(Date date) {
        SearchBuilder<ClusterDrsPlanVO> sb;
        sb = createSearchBuilder();
        sb.and(ApiConstants.CREATED, sb.entity().getCreated(), SearchCriteria.Op.LT);
        sb.done();
        SearchCriteria<ClusterDrsPlanVO> sc = sb.create();
        sc.setParameters(ApiConstants.CREATED, date);
        return expunge(sc);
    }
}
