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
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.cluster.ClusterDrsPlan;
import org.apache.cloudstack.cluster.ClusterDrsPlanVO;

import java.util.Date;
import java.util.List;

public interface ClusterDrsPlanDao extends GenericDao<ClusterDrsPlanVO, Long> {
    List<ClusterDrsPlanVO> listByStatus(ClusterDrsPlan.Status status);

    List<ClusterDrsPlanVO> listByClusterIdAndStatus(Long clusterId, ClusterDrsPlan.Status status);

    ClusterDrsPlanVO listLatestPlanForClusterId(Long clusterId);

    Pair<List<ClusterDrsPlanVO>, Integer> searchAndCount(Long clusterId, Long planId, Long startIndex,
                                                         Long pageSizeVal);

    int expungeBeforeDate(Date date);
}
