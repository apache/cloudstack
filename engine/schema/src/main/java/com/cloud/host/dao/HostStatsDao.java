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
package com.cloud.host.dao;

import java.util.Date;
import java.util.List;

import com.cloud.host.HostStatsVO;
import com.cloud.utils.db.GenericDao;

/** DAO for the host_stats table. */
public interface HostStatsDao extends GenericDao<HostStatsVO, Long> {

    List<HostStatsVO> findByHostId(long hostId);

    List<HostStatsVO> findByHostIdAndTimestampGreaterThanEqual(long hostId, Date time);

    List<HostStatsVO> findByHostIdAndTimestampLessThanEqual(long hostId, Date time);

    List<HostStatsVO> findByHostIdAndTimestampBetween(long hostId, Date startTime, Date endTime);

    /**
     * Expunges all host stats older than {@code limitDate}.
     * @param limitPerQuery max rows removed per query; 0 or negative means no limit.
     */
    void removeAllByTimestampLessThan(Date limitDate, long limitPerQuery);

}
