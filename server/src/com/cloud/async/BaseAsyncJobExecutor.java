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

public abstract class BaseAsyncJobExecutor implements AsyncJobExecutor {
	private SyncQueueItemVO _syncSource;
	private AsyncJobVO _job;
	private boolean _fromPreviousSession;
	private AsyncJobManager _asyncJobMgr;
	
	private static ThreadLocal<AsyncJobExecutor> s_currentExector = new ThreadLocal<AsyncJobExecutor>();
	
	public AsyncJobManager getAsyncJobMgr() {
		return _asyncJobMgr;
	}
	
	public void setAsyncJobMgr(AsyncJobManager asyncMgr) {
		_asyncJobMgr = asyncMgr;
	}
	
	public SyncQueueItemVO getSyncSource() {
		return _syncSource;
	}
	
	public void setSyncSource(SyncQueueItemVO syncSource) {
		_syncSource = syncSource;
	}
	
	public AsyncJobVO getJob() {
		return _job;
	}
	
	public void setJob(AsyncJobVO job) {
		_job = job;
	}
	
	public void setFromPreviousSession(boolean value) {
		_fromPreviousSession = value;
	}
	
	public boolean isFromPreviousSession() {
		return _fromPreviousSession;
	}
	
	public abstract boolean execute();
	
	public static AsyncJobExecutor getCurrentExecutor() {
		return s_currentExector.get();
	}
	
	public static void setCurrentExecutor(AsyncJobExecutor currentExecutor) {
		s_currentExector.set(currentExecutor);
	}
}
