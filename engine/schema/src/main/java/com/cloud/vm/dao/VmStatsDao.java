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
package com.cloud.vm.dao;

import java.util.Date;
import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VmStatsVO;

/**
 * Data Access Object for vm_stats table.
 */
public interface VmStatsDao extends GenericDao<VmStatsVO, Long> {

    /**
     * Finds VM stats by VM ID.
     * @param vmId the VM ID.
     * @return list of stats for the specified VM.
     */
    List<VmStatsVO> findByVmId(long vmId);

    /**
     * Finds VM stats by VM ID. The result is sorted by timestamp in descending order.
     * @param vmId the VM ID.
     * @return ordered list of stats for the specified VM.
     */
    List<VmStatsVO> findByVmIdOrderByTimestampDesc(long vmId);

    /**
     * Finds stats by VM ID and timestamp >= a given time.
     * @param vmId the specific VM.
     * @param time the specific time.
     * @return list of stats for the specified VM, with timestamp >= the specified time.
     */
    List<VmStatsVO> findByVmIdAndTimestampGreaterThanEqual(long vmId, Date time);

    /**
     * Finds stats by VM ID and timestamp <= a given time.
     * @param vmId the specific VM.
     * @param time the specific time.
     * @return list of stats for the specified VM, with timestamp <= the specified time.
     */
    List<VmStatsVO> findByVmIdAndTimestampLessThanEqual(long vmId, Date time);

    /**
     * Finds stats by VM ID and timestamp between a given time range.
     * @param vmId the specific VM.
     * @param startTime the start time.
     * @param endTime the start time.
     * @return list of stats for the specified VM, between the specified start and end times.
     */
    List<VmStatsVO> findByVmIdAndTimestampBetween(long vmId, Date startTime, Date endTime);

    /**
     * Removes (expunges) all stats of the specified VM.
     * @param vmId the VM ID to remove stats.
     */
    void removeAllByVmId(long vmId);

    /**
     * Removes (expunges) all VM stats with {@code timestamp} less than
     * a given Date.
     * @param limit the maximum date to keep stored. Records that exceed this limit will be removed.
     */
    void removeAllByTimestampLessThan(Date limit);

}
