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
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={ManagementServerHostDao.class})
public class ManagementServerHostDaoImpl extends GenericDaoBase<ManagementServerHostVO, Long> implements ManagementServerHostDao {
    private static final Logger s_logger = Logger.getLogger(ManagementServerHostDaoImpl.class);
    
    private final SearchBuilder<ManagementServerHostVO> MsIdSearch;
    
	public ManagementServerHostVO findByMsid(long msid) {
        SearchCriteria sc = MsIdSearch.create();
        sc.setParameters("msid", msid);
		
		List<ManagementServerHostVO> l = listBy(sc);
		if(l != null && l.size() > 0)
			return l.get(0);
		 
		return null;
	}
	
	public void update(long id, String name, String version, String serviceIP, int servicePort, Date lastUpdate) {
		// Can't get removed column to work with null value, switch to raw JDBC that I can control
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            
            pstmt = txn.prepareAutoCloseStatement("update mshost set name=?, version=?, service_ip=?, service_port=?, last_update=?, removed=null, alert_count=0 where id=?");
            pstmt.setString(1, name);
            pstmt.setString(2, version);
            pstmt.setString(3, serviceIP);
            pstmt.setInt(4, servicePort);
            pstmt.setString(5, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), lastUpdate));
            pstmt.setLong(6, id);
            
            pstmt.executeUpdate();
            txn.commit();
        } catch(Exception e) {
            s_logger.warn("Unexpected exception, ", e);
            txn.rollback();
        }
	}
	
	public void update(long id, Date lastUpdate) {
		// Can't get removed column to work with null value, switch to raw JDBC that I can control
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            
            pstmt = txn.prepareAutoCloseStatement("update mshost set last_update=?, removed=null, alert_count=0 where id=?");
            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), lastUpdate));
            pstmt.setLong(2, id);
            
            pstmt.executeUpdate();
            txn.commit();
        } catch(Exception e) {
            s_logger.warn("Unexpected exception, ", e);
            txn.rollback();
        }
	}
	
	public List<ManagementServerHostVO> getActiveList(Date cutTime) {
	    SearchBuilder<ManagementServerHostVO> activeSearch = createSearchBuilder();
	    activeSearch.and("lastUpdateTime", activeSearch.entity().getLastUpdateTime(),  SearchCriteria.Op.GT);
	    activeSearch.and("removed", activeSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
	    
	    SearchCriteria sc = activeSearch.create();
	    sc.setParameters("lastUpdateTime", cutTime);
	    
	    return listBy(sc);
	}
	
	public void increaseAlertCount(long id) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            
            pstmt = txn.prepareAutoCloseStatement("update mshost set alert_count=alert_count+1 where id=?");
            pstmt.setLong(1, id);
            
            pstmt.executeUpdate();
            txn.commit();
        } catch(Exception e) {
            s_logger.warn("Unexpected exception, ", e);
            txn.rollback();
        }
	}
	
	protected ManagementServerHostDaoImpl() {
		MsIdSearch = createSearchBuilder();
		MsIdSearch.and("msid",  MsIdSearch.entity().getMsid(), SearchCriteria.Op.EQ);
		MsIdSearch.done();
	}
}
