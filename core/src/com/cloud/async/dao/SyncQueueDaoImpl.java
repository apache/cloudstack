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
import java.sql.SQLException;
import java.util.Date;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.async.SyncQueueVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value = { SyncQueueDao.class })
public class SyncQueueDaoImpl extends GenericDaoBase<SyncQueueVO, Long> implements SyncQueueDao {
    private static final Logger s_logger = Logger.getLogger(SyncQueueDaoImpl.class.getName());
	
	@Override
	public void ensureQueue(String syncObjType, long syncObjId) {
		Date dt = DateUtil.currentGMTTime();
		String sql = "INSERT IGNORE INTO sync_queue(sync_objtype, sync_objid, created, last_updated) values(?, ?, ?, ?)";
		
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setString(1, syncObjType);
            pstmt.setLong(2, syncObjId);
            pstmt.setString(3, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), dt));
            pstmt.setString(4, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), dt));
            pstmt.execute();
        } catch (SQLException e) {
        	s_logger.warn("Unable to create sync queue " + syncObjType + "-" + syncObjId + ":" + e.getMessage(), e);
        } catch (Throwable e) {
        	s_logger.warn("Unable to create sync queue " + syncObjType + "-" + syncObjId + ":" + e.getMessage(), e);
        }
	}
	
	@Override
	public SyncQueueVO find(String syncObjType, long syncObjId) {
		
		SearchBuilder<SyncQueueVO> sb = createSearchBuilder();
        sb.and("syncObjType", sb.entity().getSyncObjType(), SearchCriteria.Op.EQ);
        sb.and("syncObjId", sb.entity().getSyncObjId(), SearchCriteria.Op.EQ);
        sb.done();
        
    	SearchCriteria sc = sb.create();
    	sc.setParameters("syncObjType", syncObjType);
    	sc.setParameters("syncObjId", syncObjId);
        return findOneActiveBy(sc);
	}
}
