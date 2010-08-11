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
package com.cloud.storage.preallocatedlun.dao;

import java.util.List;

import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.utils.db.GenericDao;

public interface PreallocatedLunDao extends GenericDao<PreallocatedLunVO, Long> {
    /**
     * Takes a LUN
     * @param instanceId vm instance that's taking this LUN
     * @param tags special tag to match to the LUN
     * @return PreallocatedLunVO if matches; NULL if not.
     */
    PreallocatedLunVO take(long volumeId, String targetIqn, long size1, long size2, String... tags);
    
    /**
     * Releases a LUN
     * @param id LUN to release
     * @param instanceId vm instance that this LUN used to belong to.
     * @return true if released; false if not.
     */
    boolean release(String targetIqn, int lunId, long volumeId);
    
    /**
     * Return the distinct preallocated luns.  Ignore the LUN as it is meaningless.
     * @return List of PreallocatedLunVO
     */
    List<PreallocatedLunVO> listDistinctTargets(long dataCenterId);
    
    List<String> findDistinctTagsForTarget(String targetIqn);
    
    PreallocatedLunVO persist(PreallocatedLunVO lun, String[] tags);
    long getTotalSize(String targetIqn);
    long getUsedSize(String targetIqn);
    boolean delete(long id);
}
