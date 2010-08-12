package com.cloud.maid.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.maid.StackMaidVO;
import com.cloud.serializer.SerializerHelper;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value = { StackMaidDao.class })
public class StackMaidDaoImpl extends GenericDaoBase<StackMaidVO, Long> implements StackMaidDao {
    private static final Logger s_logger = Logger.getLogger(StackMaidDaoImpl.class.getName());
    
	private SearchBuilder<StackMaidVO> popSearch;
	private SearchBuilder<StackMaidVO> clearSearch;
	
	public StackMaidDaoImpl() {
		popSearch = createSearchBuilder();
		popSearch.and("msid", popSearch.entity().getMsid(), SearchCriteria.Op.EQ);
		popSearch.and("threadId", popSearch.entity().getThreadId(), SearchCriteria.Op.EQ);
		
		clearSearch = createSearchBuilder();
		clearSearch.and("msid", clearSearch.entity().getMsid(), SearchCriteria.Op.EQ);
	}

    @DB
	public void pushCleanupDelegate(long msid, int seq, String delegateClzName, Object context) {
		StackMaidVO delegateItem = new StackMaidVO();
		delegateItem.setMsid(msid);
		delegateItem.setThreadId(Thread.currentThread().getId());
		delegateItem.setSeq(seq);
		delegateItem.setDelegate(delegateClzName);
		delegateItem.setContext(SerializerHelper.toSerializedString(context));
		delegateItem.setCreated(DateUtil.currentGMTTime());
		
		super.persist(delegateItem);
	}

    @DB
	public StackMaidVO popCleanupDelegate(long msid) {
        SearchCriteria sc = popSearch.create();
        sc.setParameters("msid", msid);
        sc.setParameters("threadId", Thread.currentThread().getId());
        
		Filter filter = new Filter(StackMaidVO.class, "seq", false, 0L, (long)1);
		List<StackMaidVO> l = listBy(sc, filter);
		if(l != null && l.size() > 0) {
			delete(l.get(0).getId());
			return l.get(0);
		}
		
		return null;
	}
	
    @DB
	public void clearStack(long msid) {
        SearchCriteria sc = clearSearch.create();
        sc.setParameters("msid", msid);
        
        delete(sc);
	}
    
    @DB
	public List<StackMaidVO> listLeftoversByMsid(long msid) {
    	List<StackMaidVO> l = new ArrayList<StackMaidVO>();
    	String sql = "select * from stack_maid where msid=? order by msid asc, thread_id asc, seq desc";
    	
        Transaction txn = Transaction.currentTxn();;
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
        }
        return l;
    }
    
    @DB
	public List<StackMaidVO> listLeftoversByCutTime(Date cutTime) {
    	
    	List<StackMaidVO> l = new ArrayList<StackMaidVO>();
    	String sql = "select * from stack_maid where created < ? order by msid asc, thread_id asc, seq desc";
    	
        Transaction txn = Transaction.currentTxn();;
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
        }
        return l;
    }
}

