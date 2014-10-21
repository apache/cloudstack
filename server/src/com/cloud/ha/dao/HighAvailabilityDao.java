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
package com.cloud.ha.dao;

import java.util.List;

import com.cloud.ha.HaWorkVO;
import com.cloud.ha.HighAvailabilityManager.WorkType;
import com.cloud.utils.db.GenericDao;

public interface HighAvailabilityDao extends GenericDao<HaWorkVO, Long> {

    /**
     * Takes an available HA work item.
     *
     * @param serverId server that is taking this.
     * @return WorkVO if there's one to work on; null if none.
     */
    HaWorkVO take(long serverId);

    /**
     * Finds all the work items related to this instance.
     *
     * @param instanceId
     * @return list of WorkVO or empty list.
     */
    List<HaWorkVO> findPreviousHA(long instanceId);

    boolean delete(long instanceId, WorkType type);

    /**
     * Finds all the work items that were successful and is now ready to be purged.
     *
     * @param time that the work item must be successful before.
     * @return list of WorkVO or empty list.
     */
    void cleanup(long time);

    void deleteMigrationWorkItems(final long hostId, final WorkType type, final long serverId);

    List<HaWorkVO> findTakenWorkItems(WorkType type);

    /**
     * finds out if a work item has been scheduled for this work type but has not been taken yet.
     *
     * @param instanceId vm instance id
     * @param type type of work scheduled for it.
     * @return true if it has been scheduled and false if it hasn't.
     */
    boolean hasBeenScheduled(long instanceId, WorkType type);

    int releaseWorkItems(long nodeId);

    /**
     * Look for HA work that has been scheduled for a vm since a certain work id.
     *
     * @param vmId virtual machine id.
     * @param workId work item id.
     * @return List of work items.
     */
    List<HaWorkVO> listFutureHaWorkForVm(long vmId, long workId);

    /**
     * Look for HA work that is being run right now for a VM.
     *
     * @param vmId virtual machine id
     * @return List of work items
     */
    List<HaWorkVO> listRunningHaWorkForVm(long vmId);

    List<HaWorkVO> listPendingHaWorkForVm(long vmId);
}
