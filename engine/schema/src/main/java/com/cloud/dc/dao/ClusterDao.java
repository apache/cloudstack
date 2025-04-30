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
import java.util.Map;

import com.cloud.cpu.CPU;
import com.cloud.dc.ClusterVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

public interface ClusterDao extends GenericDao<ClusterVO, Long> {
    List<ClusterVO> listByPodId(long podId);

    ClusterVO findBy(String name, long podId);

    List<ClusterVO> listByZoneId(long zoneId);

    List<HypervisorType> getAvailableHypervisorInZone(Long zoneId);

    List<Pair<HypervisorType, CPU.CPUArch>> listDistinctHypervisorsArchAcrossClusters(Long zoneId);

    List<ClusterVO> listByDcHyType(long dcId, String hyType);

    Map<Long, List<Long>> getPodClusterIdMap(List<Long> clusterIds);

    List<Long> listDisabledClusters(long zoneId, Long podId);

    List<Long> listClustersWithDisabledPods(long zoneId);

    Integer countAllByDcId(long zoneId);

    Integer countAllManagedAndEnabledByDcId(long zoneId);

    List<ClusterVO> listClustersByDcId(long zoneId);

    List<Long> listAllClusterIds(Long zoneId);

    boolean getSupportsResigning(long clusterId);

    List<CPU.CPUArch> getClustersArchsByZone(long zoneId);

    List<ClusterVO> listClustersByArchAndZoneId(long zoneId, CPU.CPUArch arch);
}
