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

import com.cloud.async.AsyncJobVO;
import com.cloud.async.SyncQueueItemVO;

public interface AsyncJobExecutor {
	public AsyncJobManager getAsyncJobMgr();
	public void setAsyncJobMgr(AsyncJobManager asyncMgr);
	public SyncQueueItemVO getSyncSource();
	public void setSyncSource(SyncQueueItemVO syncSource);
	public AsyncJobVO getJob();
	public void setJob(AsyncJobVO job);
	public void setFromPreviousSession(boolean value);
	public boolean isFromPreviousSession();
	
	/**
	 * 
	 * @return if executor has a sync source, return true to indicate completion of using the sync-source
	 * 	otherwise return false and once the executor finally has completed with the sync source,
	 *  it needs to call AsyncJobManager.releaseSyncSource
	 *  
	 *  if executor does not have a sync source, always return true
	 */
	public boolean execute();
}

