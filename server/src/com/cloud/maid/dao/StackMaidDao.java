package com.cloud.maid.dao;

import java.util.Date;
import java.util.List;

import com.cloud.maid.StackMaidVO;
import com.cloud.utils.db.GenericDao;

public interface StackMaidDao extends GenericDao<StackMaidVO, Long> {
	public void pushCleanupDelegate(long msid, int seq, String delegateClzName, Object context);
	public StackMaidVO popCleanupDelegate(long msid);
	public void clearStack(long msid);
	
	public List<StackMaidVO> listLeftoversByMsid(long msid); 
	public List<StackMaidVO> listLeftoversByCutTime(Date cutTime); 
}
