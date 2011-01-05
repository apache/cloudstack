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

package com.cloud.async.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.async.SyncQueueItemVO;
import com.cloud.async.SyncQueueVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value = { SyncQueueItemDao.class })
public class SyncQueueItemDaoImpl extends GenericDaoBase<SyncQueueItemVO, Long> implements SyncQueueItemDao {
    private static final Logger s_logger = Logger.getLogger(SyncQueueItemDaoImpl.class);
    
    private final SyncQueueDao _syncQueueDao = new SyncQueueDaoImpl();

	@Override
	public SyncQueueItemVO getNextQueueItem(long queueId) {
		
		SearchBuilder<SyncQueueItemVO> sb = createSearchBuilder();
        sb.and("queueId", sb.entity().getQueueId(), SearchCriteria.Op.EQ);
        sb.and("lastProcessNumber", sb.entity().getLastProcessNumber(),	SearchCriteria.Op.NULL);
        sb.done();
        
    	SearchCriteria<SyncQueueItemVO> sc = sb.create();
    	sc.setParameters("queueId", queueId);
    	
    	Filter filter = new Filter(SyncQueueItemVO.class, "created", true, 0L, 1L);
        List<SyncQueueItemVO> l = listBy(sc, filter);
        if(l != null && l.size() > 0)
        	return l.get(0);
    	
		return null;
	}

	@Override
	public List<SyncQueueItemVO> getNextQueueItems(int maxItems) {
		List<SyncQueueItemVO> l = new ArrayList<SyncQueueItemVO>();
		
		String sql = "SELECT i.id, i.queue_id, i.content_type, i.content_id, i.created " +
					 " FROM sync_queue AS q JOIN sync_queue_item AS i ON q.id = i.queue_id " +
					 " WHERE q.queue_proc_time IS NULL AND i.queue_proc_number IS NULL " +
					 " GROUP BY q.id " +
					 " ORDER BY i.id " +
					 " LIMIT 0, ?";

        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setInt(1, maxItems);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	SyncQueueItemVO item = new SyncQueueItemVO();
            	item.setId(rs.getLong(1));
            	item.setQueueId(rs.getLong(2));
            	item.setContentType(rs.getString(3));
            	item.setContentId(rs.getLong(4));
            	item.setCreated(DateUtil.parseDateString(TimeZone.getTimeZone("GMT"), rs.getString(5)));
            	l.add(item);
            }
        } catch (SQLException e) {
        	s_logger.error("Unexpected sql excetpion, ", e);
        } catch (Throwable e) {
        	s_logger.error("Unexpected excetpion, ", e);
        }
		return l;
	}
	
	@Override
	public List<SyncQueueItemVO> getActiveQueueItems(Long msid, boolean exclusive) {
		SearchBuilder<SyncQueueItemVO> sb = createSearchBuilder();
        sb.and("lastProcessMsid", sb.entity().getLastProcessMsid(),
    		SearchCriteria.Op.EQ);
        sb.done();
        
    	SearchCriteria<SyncQueueItemVO> sc = sb.create();
    	sc.setParameters("lastProcessMsid", msid);
    	
    	Filter filter = new Filter(SyncQueueItemVO.class, "created", true, null, null);
    	
    	if(exclusive)
    		return lockRows(sc, filter, true);
        return listBy(sc, filter);
	}
	
    @Override
    public List<SyncQueueItemVO> getBlockedQueueItems(long thresholdMs, boolean exclusive) {
        Date cutTime = DateUtil.currentGMTTime();
        cutTime = new Date(cutTime.getTime() - thresholdMs);
        
        SearchBuilder<SyncQueueVO> sbQueue = _syncQueueDao.createSearchBuilder();
        sbQueue.and("lastProcessTime", sbQueue.entity().getLastProcessTime(), SearchCriteria.Op.NNULL);
        sbQueue.and("lastProcessTime2", sbQueue.entity().getLastProcessTime(), SearchCriteria.Op.LT);
        
        SearchBuilder<SyncQueueItemVO> sbItem = createSearchBuilder();
        sbItem.join("queueItemJoinQueue", sbQueue, sbQueue.entity().getId(), sbItem.entity().getQueueId(), JoinBuilder.JoinType.INNER);
        sbItem.and("lastProcessMsid", sbItem.entity().getLastProcessMsid(), SearchCriteria.Op.NNULL);
        sbItem.and("lastProcessNumber", sbItem.entity().getLastProcessNumber(), SearchCriteria.Op.NNULL);
        
        sbQueue.done();
        sbItem.done();
        
        SearchCriteria<SyncQueueItemVO> sc = sbItem.create();
        sc.setJoinParameters("queueItemJoinQueue", "lastProcessTime2", cutTime);
        
        if(exclusive)
            return lockRows(sc, null, true);
        return listBy(sc, null);
    }
}
