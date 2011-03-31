package com.cloud.cluster.dao;

import java.util.Date;
import java.util.List;

import com.cloud.cluster.TaskVO;
import com.cloud.utils.db.GenericDao;

public interface StackMaidDao extends GenericDao<TaskVO, Long> {
	public long pushCleanupDelegate(long msid, int seq, String delegateClzName, Object context);
	public TaskVO popCleanupDelegate(long msid);
	public void clearStack(long msid);
	
	public List<TaskVO> listLeftoversByMsid(long msid); 
	public List<TaskVO> listLeftoversByCutTime(Date cutTime);
	
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
	
	List<TaskVO> listCleanupTasks(long selfId);
}
