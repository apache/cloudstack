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

package com.cloud.cluster.dao;

import java.util.Date;
import java.util.List;

import com.cloud.cluster.CheckPointVO;
import com.cloud.utils.db.GenericDao;

public interface StackMaidDao extends GenericDao<CheckPointVO, Long> {
	public long pushCleanupDelegate(long msid, int seq, String delegateClzName, Object context);
	public CheckPointVO popCleanupDelegate(long msid);
	public void clearStack(long msid);
	
	public List<CheckPointVO> listLeftoversByMsid(long msid); 
	public List<CheckPointVO> listLeftoversByCutTime(Date cutTime);
	
	/**
	 * Take over the task items of another management server and clean them up.
	 * This method changes the management server id of all of the tasks to
	 * this management server and mark the thread id as 0.  It then returns
	 * all of the tasks that needs to be reverted to be processed.
	 * 
	 * @param takeOverMsid management server id to take over.
	 * @param selfId the management server id of this node.
	 * @return list of tasks to take over.
	 */
	boolean takeover(long takeOverMsid, long selfId);
	
	List<CheckPointVO> listCleanupTasks(long selfId);
	List<CheckPointVO> listLeftoversByCutTime(Date cutTime, long msid);
}
