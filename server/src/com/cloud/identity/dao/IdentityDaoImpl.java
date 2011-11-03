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
package com.cloud.Identity.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.IdentityMapper;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;

@Local(value={IdentityDao.class})
public class IdentityDaoImpl extends GenericDaoBase<IdentityVO, Long> implements IdentityDao {
    private static final Logger s_logger = Logger.getLogger(IdentityDaoImpl.class);
    
    public IdentityDaoImpl() {
    }
    
    @DB
	public Long getIdentityId(IdentityMapper mapper, String identityString) {
		assert(mapper.entityTableName() != null);
		assert(identityString != null);

        PreparedStatement pstmt = null;
		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
	        try {
	            pstmt = txn.prepareAutoCloseStatement(
	        		String.format("SELECT id FROM `%s` WHERE id=? OR uuid=?", mapper.entityTableName())
	        		
	        		// TODO : after graceful period, use following line turn on more secure check
	        		// String.format("SELECT id FROM %s WHERE (id=? AND uuid IS NULL) OR uuid=?", mapper.entityTableName())
	            );
	            
	            long id = 0;
	            try {
	            	// TODO : use regular expression to determine
	            	id = Long.parseLong(identityString);
	            } catch(NumberFormatException e) {
	            	// this could happen when it is a uuid string, so catch and ignore it
	            }
	            
	            pstmt.setLong(1, id);
	            pstmt.setString(2, identityString);
	            
	            ResultSet rs = pstmt.executeQuery();
	            if(rs.next()) {
	            	return rs.getLong(1);
	            }
	        } catch (SQLException e) {
	        	s_logger.error("Unexpected exception ", e);
	        }
		} finally {
			txn.close();
		}
		return null;
	}
	
    @DB
	public String getIdentityUuid(String tableName, String identityString) {
		assert(tableName != null);
		assert(identityString != null);
		
        PreparedStatement pstmt = null;
		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
	        try {
	            pstmt = txn.prepareAutoCloseStatement(
	            	String.format("SELECT uuid FROM %s WHERE id=? OR uuid=?", tableName)
	        		// String.format("SELECT uuid FROM %s WHERE (id=? AND uuid IS NULL) OR uuid=?", tableName)
	        	);
	            
	            long id = 0;
	            try {
	            	// TODO : use regular expression to determine
	            	id = Long.parseLong(identityString);
	            } catch(NumberFormatException e) {
	            	// this could happen when it is a uuid string, so catch and ignore it
	            }
	            
	            pstmt.setLong(1, id);
	            pstmt.setString(2, identityString);
	            
	            ResultSet rs = pstmt.executeQuery();
	            if(rs.next()) {
	            	String uuid = rs.getString(1);
	            	if(uuid != null && !uuid.isEmpty())
	            		return uuid;
	            	return identityString;
	            }
	        } catch (SQLException e) {
	        	s_logger.error("Unexpected exception ", e);
	        }
		} finally {
			txn.close();
		}
		return null;
	}
}
