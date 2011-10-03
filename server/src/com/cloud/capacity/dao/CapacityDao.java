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

package com.cloud.capacity.dao;

import java.util.List;

import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.utils.db.GenericDao;

public interface CapacityDao extends GenericDao<CapacityVO, Long> {
	CapacityVO findByHostIdType(Long hostId, short capacityType);
	List<Long> listClustersInZoneOrPodByHostCapacities(long id, int requiredCpu, long requiredRam, short capacityTypeForOrdering, boolean isZone, float cpuOverprovisioningFactor);
	List<Long> listHostsWithEnoughCapacity(int requiredCpu, long requiredRam, Long clusterId, String hostType, float cpuOverprovisioningFactor);	
	 List<SummedCapacity> findCapacityByType(short capacityType, Long zoneId, Long podId,
			Long clusterId, Long startIndex, Long pageSize);
	boolean removeBy(Short capacityType, Long zoneId, Long podId, Long clusterId);
	List<SummedCapacity> findByClusterPodZone(Long zoneId, Long podId, Long clusterId);
	List<SummedCapacity> findNonSharedStorageForClusterPodZone(Long zoneId,Long podId, Long clusterId);
    List<Long> orderClustersByAggregateCapacity(long id, short capacityType, boolean isZone, float cpuOverprovisioningFactor);
}
