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
package com.cloud.storage.dao;

import java.util.Date;
import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.storage.VolumeStatsVO;

/**
 * Data Access Object for volume_stats table.
 */
public interface VolumeStatsDao extends GenericDao<VolumeStatsVO, Long> {

    /**
     * Finds Volume stats by Volume ID.
     * @param volumeId the Volume ID.
     * @return list of stats for the specified Volume.
     */
    List<VolumeStatsVO> findByVolumeId(long volumeId);

    /**
     * Finds Volume stats by Volume ID. The result is sorted by timestamp in descending order.
     * @param volumeId the Volume ID.
     * @return ordered list of stats for the specified Volume.
     */
    List<VolumeStatsVO> findByVolumeIdOrderByTimestampDesc(long volumeId);

    /**
     * Finds stats by Volume ID and timestamp >= a given time.
     * @param volumeId the specific Volume.
     * @param time the specific time.
     * @return list of stats for the specified Volume, with timestamp >= the specified time.
     */
    List<VolumeStatsVO> findByVolumeIdAndTimestampGreaterThanEqual(long volumeId, Date time);

    /**
     * Finds stats by Volume ID and timestamp <= a given time.
     * @param volumeId the specific Volume.
     * @param time the specific time.
     * @return list of stats for the specified Volume, with timestamp <= the specified time.
     */
    List<VolumeStatsVO> findByVolumeIdAndTimestampLessThanEqual(long volumeId, Date time);

    /**
     * Finds stats by Volume ID and timestamp between a given time range.
     * @param volumeId the specific Volume.
     * @param startTime the start time.
     * @param endTime the start time.
     * @return list of stats for the specified Volume, between the specified start and end times.
     */
    List<VolumeStatsVO> findByVolumeIdAndTimestampBetween(long volumeId, Date startTime, Date endTime);

    /**
     * Removes (expunges) all stats of the specified Volume.
     * @param volumeId the Volume ID to remove stats.
     */
    void removeAllByVolumeId(long volumeId);

    /**
     * Removes (expunges) all Volume stats with {@code timestamp} less than
     * a given Date.
     * @param limit the maximum date to keep stored. Records that exceed this limit will be removed.
     */
    void removeAllByTimestampLessThan(Date limit);

}
