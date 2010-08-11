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
package com.cloud.ha.dao;

import java.util.List;

import com.cloud.ha.WorkVO;
import com.cloud.ha.WorkVO.WorkType;
import com.cloud.utils.db.GenericDao;

public interface HighAvailabilityDao extends GenericDao<WorkVO, Long> {

    /**
     * Takes an available HA work item.
     * 
     * @param serverId server that is taking this.
     * @return WorkVO if there's one to work on; null if none.
     */
    WorkVO take(long serverId);

    /**
     * Finds all the work items related to this instance.
     * 
     * @param instanceId
     * @return list of WorkVO or empty list.
     */
    List<WorkVO> findPreviousHA(long instanceId);
    
    boolean delete(long instanceId, WorkType type);

    /**
     * Finds all the work items that were successful and is now ready to be purged.
     * 
     * @param time that the work item must be successful before.
     * @return list of WorkVO or empty list.
     */
    void cleanup(long time);
    
    void deleteMigrationWorkItems(final long hostId, final WorkType type, final long serverId);
    
    List<WorkVO> findTakenWorkItems(WorkType type);
    
    /**
     * finds out if a work item has been scheduled for this work type but has not been taken yet.
     * 
     * @param instanceId vm instance id
     * @param type type of work scheduled for it.
     * @return true if it has been scheduled and false if it hasn't.
     */
    boolean hasBeenScheduled(long instanceId, WorkType type);
}
