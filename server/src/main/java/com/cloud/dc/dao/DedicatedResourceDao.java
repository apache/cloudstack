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
package com.cloud.dc.dao;

import java.util.List;

import com.cloud.dc.DedicatedResourceVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;

public interface DedicatedResourceDao extends GenericDao<DedicatedResourceVO, Long> {

    DedicatedResourceVO findByZoneId(Long zoneId);

    DedicatedResourceVO findByPodId(Long podId);

    DedicatedResourceVO findByClusterId(Long clusterId);

    DedicatedResourceVO findByHostId(Long hostId);

    Pair<List<DedicatedResourceVO>, Integer> searchDedicatedHosts(Long hostId, Long domainId, Long accountId, Long affinityGroupId, Filter filter);

    Pair<List<DedicatedResourceVO>, Integer> searchDedicatedClusters(Long clusterId, Long domainId, Long accountId, Long affinityGroupId, Filter filter);

    Pair<List<DedicatedResourceVO>, Integer> searchDedicatedPods(Long podId, Long domainId, Long accountId, Long affinityGroupId, Filter filter);

    Pair<List<DedicatedResourceVO>, Integer> searchDedicatedZones(Long dataCenterId, Long domainId, Long accountId, Long affinityGroupId, Filter filter);

    List<DedicatedResourceVO> listByAccountId(Long accountId);

    List<DedicatedResourceVO> listByDomainId(Long domainId);

    List<DedicatedResourceVO> listZonesNotInDomainIds(List<Long> domainIds);

    List<Long> listAllPods();

    List<Long> listAllClusters();

    List<Long> listAllHosts();

    List<DedicatedResourceVO> listByAffinityGroupId(Long affinityGroupId);

    List<Long> findHostsByCluster(Long clusterId);

    List<Long> findHostsByZone(Long zoneId);
}
