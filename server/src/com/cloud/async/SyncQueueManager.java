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

package com.cloud.async;

import java.util.List;

import com.cloud.async.SyncQueueItemVO;
import com.cloud.async.SyncQueueVO;
import com.cloud.utils.component.Manager;

public interface SyncQueueManager extends Manager {
    public SyncQueueVO queue(String syncObjType, long syncObjId, String itemType, long itemId);
    public SyncQueueItemVO dequeueFromOne(long queueId, Long msid);
    public List<SyncQueueItemVO> dequeueFromAny(Long msid, int maxItems);
    public void purgeItem(long queueItemId);
    
	public List<SyncQueueItemVO> getActiveQueueItems(Long msid);
}
