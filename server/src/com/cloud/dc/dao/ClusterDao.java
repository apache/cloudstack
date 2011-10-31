/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.dc.dao;

import java.util.List;
import java.util.Map;

import com.cloud.dc.ClusterVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.GenericDao;

public interface ClusterDao extends GenericDao<ClusterVO, Long> {
    List<ClusterVO> listByPodId(long podId);
    ClusterVO findBy(String name, long podId);
    List<ClusterVO> listByHyTypeWithoutGuid(String hyType);
    List<ClusterVO> listByZoneId(long zoneId);

    List<HypervisorType> getAvailableHypervisorInZone(Long zoneId);
    List<ClusterVO> listByDcHyType(long dcId, String hyType);
    Map<Long, List<Long>> getPodClusterIdMap(List<Long> clusterIds);
    List<Long> listDisabledClusters(long zoneId, Long podId);
    List<Long> listClustersWithDisabledPods(long zoneId);
}
