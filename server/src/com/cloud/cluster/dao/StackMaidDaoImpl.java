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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.cluster.CheckPointVO;
import com.cloud.serializer.SerializerHelper;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;

@Local(value = { StackMaidDao.class }) @DB(txn=false)
public class StackMaidDaoImpl extends GenericDaoBase<CheckPointVO, Long> implements StackMaidDao {
    private static final Logger s_logger = Logger.getLogger(StackMaidDaoImpl.class);
    
	private SearchBuilder<CheckPointVO> popSearch;
	private SearchBuilder<CheckPointVO> clearSearch;
	private final SearchBuilder<CheckPointVO> AllFieldsSearch;
	
	public StackMaidDaoImpl() {
		popSearch = createSearchBuilder();
		popSearch.and("msid", popSearch.entity().getMsid(), SearchCriteria.Op.EQ);
		popSearch.and("threadId", popSearch.entity().getThreadId(), SearchCriteria.Op.EQ);
		
		clearSearch = createSearchBuilder();
		clearSearch.and("msid", clearSearch.entity().getMsid(), SearchCriteria.Op.EQ);
		
		AllFieldsSearch = createSearchBuilder();
		AllFieldsSearch.and("msid", AllFieldsSearch.entity().getMsid(), Op.EQ);
		AllFieldsSearch.and("thread", AllFieldsSearch.entity().getThreadId(), Op.EQ);
		AllFieldsSearch.done();
	}
	
	@Override
	public boolean takeover(long takeOverMsid, long selfId) {
	    CheckPointVO task = createForUpdate();
	    task.setMsid(selfId);
	    task.setThreadId(0);
	    
	    SearchCriteria<CheckPointVO> sc = AllFieldsSearch.create();
	    sc.setParameters("msid", takeOverMsid);
	    return update(task, sc) > 0;
	    
	}
	
	@Override
	public List<CheckPointVO> listCleanupTasks(long msId) {
	    SearchCriteria<CheckPointVO> sc = AllFieldsSearch.create();
        sc.setParameters("msid", msId);
        sc.setParameters("thread", 0);
	    
        return this.search(sc, null);
	}

    @Override
	public long pushCleanupDelegate(long msid, int seq, String delegateClzName, Object context) {
		CheckPointVO delegateItem = new CheckPointVO();
		delegateItem.setMsid(msid);
		delegateItem.setThreadId(Thread.currentThread().getId());
		delegateItem.setSeq(seq);
		delegateItem.setDelegate(delegateClzName);
		delegateItem.setContext(SerializerHelper.toSerializedStringOld(context));
		delegateItem.setCreated(DateUtil.currentGMTTime());
		
		super.persist(delegateItem);
		return delegateItem.getId();
	}

    @Override
	public CheckPointVO popCleanupDelegate(long msid) {
        SearchCriteria<CheckPointVO> sc = popSearch.create();
        sc.setParameters("msid", msid);
        sc.setParameters("threadId", Thread.currentThread().getId());
        
		Filter filter = new Filter(CheckPointVO.class, "seq", false, 0L, (long)1);
		List<CheckPointVO> l = listIncludingRemovedBy(sc, filter);
		if(l != null && l.size() > 0) {
			expunge(l.get(0).getId());
			return l.get(0);
		}
		
		return null;
	}
    
    @Override
	public void clearStack(long msid) {
        SearchCriteria<CheckPointVO> sc = clearSearch.create();
        sc.setParameters("msid", msid);
        
        expunge(sc);
	}
    
    @Override
    @DB
	public List<CheckPointVO> listLeftoversByMsid(long msid) {
    	List<CheckPointVO> l = new ArrayList<CheckPointVO>();
    	String sql = "select * from stack_maid where msid=? order by msid asc, thread_id asc, seq desc";
    	
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, msid);
            
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	l.add(toEntityBean(rs, false));
            }
        } catch (SQLException e) {
        	s_logger.error("unexcpected exception " + e.getMessage(), e);
        } catch (Throwable e) {
        	s_logger.error("unexcpected exception " + e.getMessage(), e);
        } finally {
            txn.close();
        }
        return l;
    }
    
    @Override
    @DB
	public List<CheckPointVO> listLeftoversByCutTime(Date cutTime) {
    	
    	List<CheckPointVO> l = new ArrayList<CheckPointVO>();
    	String sql = "select * from stack_maid where created < ? order by msid asc, thread_id asc, seq desc";
    	
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            String gmtCutTime = DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime);
            pstmt.setString(1, gmtCutTime);
            
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	l.add(toEntityBean(rs, false));
            }
        } catch (SQLException e) {
        	s_logger.error("unexcpected exception " + e.getMessage(), e);
        } catch (Throwable e) {
        	s_logger.error("unexcpected exception " + e.getMessage(), e);
        } finally {
            txn.close();
        }
        return l;
    }
    
    @Override
    @DB
	public List<CheckPointVO> listLeftoversByCutTime(Date cutTime, long msid) {
    	
    	List<CheckPointVO> l = new ArrayList<CheckPointVO>();
    	String sql = "select * from stack_maid where created < ? and msid = ? order by msid asc, thread_id asc, seq desc";
    	
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            String gmtCutTime = DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime);
            pstmt.setString(1, gmtCutTime);
            pstmt.setLong(2, msid);
            
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	l.add(toEntityBean(rs, false));
            }
        } catch (SQLException e) {
        	s_logger.error("unexcpected exception " + e.getMessage(), e);
        } catch (Throwable e) {
        	s_logger.error("unexcpected exception " + e.getMessage(), e);
        } finally {
            txn.close();
        }
        return l;
    }
}

