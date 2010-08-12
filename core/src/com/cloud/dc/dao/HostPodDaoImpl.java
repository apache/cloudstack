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

package com.cloud.dc.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.dc.HostPodVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;

@Local(value={HostPodDao.class})
public class HostPodDaoImpl extends GenericDaoBase<HostPodVO, Long> implements HostPodDao {
    private static final Logger s_logger = Logger.getLogger(HostPodDaoImpl.class);
	
	protected SearchBuilder<HostPodVO> DataCenterAndNameSearch;
	protected SearchBuilder<HostPodVO> DataCenterIdSearch;
	
	protected HostPodDaoImpl() {
	    DataCenterAndNameSearch = createSearchBuilder();
	    DataCenterAndNameSearch.and("dc", DataCenterAndNameSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
	    DataCenterAndNameSearch.and("name", DataCenterAndNameSearch.entity().getName(), SearchCriteria.Op.EQ);
	    DataCenterAndNameSearch.done();
	    
	    DataCenterIdSearch = createSearchBuilder();
	    DataCenterIdSearch.and("dcId", DataCenterIdSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
	    DataCenterIdSearch.done();
	}
	
	public List<HostPodVO> listByDataCenterId(long id) {
		SearchCriteria sc = DataCenterIdSearch.create();
		sc.setParameters("dcId", id);
		
	    return listActiveBy(sc);
	}
	
	public HostPodVO findByName(String name, long dcId) {
	    SearchCriteria sc = DataCenterAndNameSearch.create();
	    sc.setParameters("dc", dcId);
	    sc.setParameters("name", name);
	    
	    return findOneActiveBy(sc);
	}
	
	@Override
	public HashMap<Long, List<Object>> getCurrentPodCidrSubnets(long zoneId, long podIdToSkip) {
		HashMap<Long, List<Object>> currentPodCidrSubnets = new HashMap<Long, List<Object>>();
		
		String selectSql = "SELECT id, cidr_address, cidr_size FROM host_pod_ref WHERE data_center_id=" + zoneId;
		Transaction txn = Transaction.currentTxn();
		try {
        	PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
        	ResultSet rs = stmt.executeQuery();
        	while (rs.next()) {
        		Long podId = rs.getLong("id");
        		if (podId.longValue() == podIdToSkip) continue;
        		String cidrAddress = rs.getString("cidr_address");
        		long cidrSize = rs.getLong("cidr_size");
        		List<Object> cidrPair = new ArrayList<Object>();
        		cidrPair.add(0, cidrAddress);
        		cidrPair.add(1, new Long(cidrSize));
        		currentPodCidrSubnets.put(podId, cidrPair);
        	}
        } catch (SQLException ex) {
        	s_logger.warn("DB exception " + ex.getMessage(), ex);
            return null;
        }
        
        return currentPodCidrSubnets;
	}
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		super.configure(name, params);
/*
		SearchBuilder<HostPodVO> builder = createSearchBuilder();
		builder.and("abc", builder.entity().getId(), SearchCriteria.Op.EQ);
		builder.op(Op.AND, "def", builder.entity().getId(), SearchCriteria.Op.GT);
		builder.or("fee", builder.entity().getId(), SearchCriteria.Op.LT);
		builder.cp();
		builder.and("jjj", builder.entity().getId(), SearchCriteria.Op.EQ);
		builder.done();
		SearchCriteria sc = builder.create();
		sc.setParameters("abc", 1);
		sc.setParameters("def", 1);
		sc.setParameters("fee", 1);
		sc.setParameters("jjj", 1);
		searchAll(sc, null, null, false);
		try {
		Transaction txn2 = Transaction.open("a1");
		txn2 = Transaction.open("a2");
        PreparedStatement pstmt2 = txn2.prepareAutoCloseStatement("SELECT id FROM host_pod_ref");
        pstmt2.executeQuery();
        txn2.close();
        txn2.setSavepoint();
        txn2.rollback();
		txn2.start();
		Transaction.open("a3");
		txn2.setSavepoint();
		txn2.start();
		Transaction.open("a4");
		Savepoint sp = txn2.setSavepoint();
		Transaction.open("a5");
		txn2.rollback(sp);
		pstmt2 = txn2.prepareAutoCloseStatement("SELECT id FROM host_pod_ref");
		pstmt2.executeQuery();
        txn2.close();
		txn2.rollback();
		txn2.close();
		txn2.close();
		txn2.close();
		// This tests multiple starts and one single rollback rolls them all back.
		txn2.open("a6");
		txn2.start();
        pstmt2 = txn2.prepareAutoCloseStatement("SELECT id FROM host_pod_ref");
        pstmt2.executeQuery();
		txn2.start();
        pstmt2 = txn2.prepareAutoCloseStatement("SELECT id FROM host_pod_ref");
        pstmt2.executeQuery();
		txn2.open("a7");
		txn2.start();
        pstmt2 = txn2.prepareAutoCloseStatement("SELECT id FROM host_pod_ref");
        pstmt2.executeQuery();
		txn2.rollback();
		txn2.close();
		txn2.close();
		
		// This tests multiple starts and need multiple commits.
        txn2.open("a8");
        txn2.start();
        pstmt2 = txn2.prepareAutoCloseStatement("SELECT id FROM host_pod_ref");
        pstmt2.executeQuery();
        txn2.start();
        pstmt2 = txn2.prepareAutoCloseStatement("SELECT id FROM host_pod_ref");
        pstmt2.executeQuery();
        txn2.open("a9");
        txn2.start();
        pstmt2 = txn2.prepareAutoCloseStatement("SELECT id FROM host_pod_ref");
        pstmt2.executeQuery();
        txn2.commit();
        txn2.close();
        txn2.commit();
        txn2.commit();
        txn2.close();
        
		
		} catch (Exception e) {
		    s_logger.warn("Exeception", e);
		}
		
            Merovingian m = new Merovingian(Transaction.VMOPS_DB);
            m.acquire("key1", 100);
            m.acquire("key1", 100);
            m.release("key1");
            m.acquire("key2", 100);
            m.acquire("key3", 1000);
            m.acquire("key1", 1000);
            m.release("key3");
            m.release("key1");
            m.release("key2");
            m.release("key1");
            
            
            acquire(1l);
            release(1l);
    */
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			ArrayList<Long> podIds = new ArrayList<Long>();
			PreparedStatement pstmt = txn.prepareAutoCloseStatement("SELECT id FROM host_pod_ref FOR UPDATE");
			ResultSet rs = pstmt.executeQuery();
			int i = 1;
			while (rs.next()) {
				podIds.add(rs.getLong(1));
			}
			PreparedStatement alter = txn.prepareAutoCloseStatement("ALTER TABLE host_pod_ref ADD COLUMN cidr_address VARCHAR(15) NOT NULL");
			try {
				int result = alter.executeUpdate();
				if (result == 0) {
					txn.rollback();
					return true;
				}
			} catch (SQLException e) {
				txn.rollback();
				
				if (e.getMessage().contains("Duplicate column name")) {
					s_logger.info("host_pod_ref table is already up to date");
					return true;
				}
				
				// assume this is because it's already been updated.
				s_logger.debug("Got this while updating", e);
				
				throw new ConfigurationException("Unable to update the host_pod_ref table ");
			}
			alter = txn.prepareStatement("ALTER TABLE host_pod_ref ADD COLUMN cidr_size bigint NOT NULL");
			try {
				int result = alter.executeUpdate();
				if (result == 0) {
					txn.rollback();
					throw new ConfigurationException("How can the first ALTER work but this doesn't?");
				}
			} catch (SQLException e) {
				s_logger.warn("Couldn't alter the table: ", e);
				txn.rollback();
				throw new ConfigurationException("How can the first ALTER work but this doesn't? " + e.getMessage());
			}
			
			PreparedStatement netmask = txn.prepareAutoCloseStatement("SELECT value FROM configuration WHERE name='private.net.mask'");
			String privateNetmask;
			try {
				rs = netmask.executeQuery();
				if (!rs.next()) {
					txn.rollback();
					throw new ConfigurationException("There's no private.netmask?");
				}
				privateNetmask = rs.getString(1);
			} catch (SQLException e) {
				s_logger.warn("Couldn't get private.netmask due to ", e);
				txn.rollback();
				throw new ConfigurationException("Unable to find the private.netmask");
			}
			
			for (Long podId : podIds) {
				PreparedStatement ip = txn.prepareAutoCloseStatement("SELECT ip_address from op_dc_ip_address_alloc where pod_id=? LIMIT 0,1");
				ip.setLong(1, podId);
				String addr = "192.168.1.1";
				try {
					rs = ip.executeQuery();
					if (rs.next()) {
						addr = rs.getString(1);
					} else {
						s_logger.debug("Default pod " + podId + " to 192.168.1.1 because it has no ip addresses allocated to it");
					}
				} catch(SQLException e) {
					s_logger.warn("Didn't work for " + podId + " due to " + e.getMessage(), e);
				}
				PreparedStatement update = txn.prepareAutoCloseStatement("UPDATE host_pod_ref set cidr_address=?, cidr_size=? WHERE id=?");
				update.setString(1, addr);
				update.setLong(2, NetUtils.getCidrSize(privateNetmask));
				update.setLong(3, podId);
				
				try {
					update.executeUpdate();
				} catch (SQLException e) {
					s_logger.debug("Unable to update host_pod_ref table due to " + e.getMessage(), e);
				}
			}
			
			txn.commit();
		} catch (SQLException e) {
			s_logger.error("Unable to upgrade the db due to " + e);
			txn.rollback();
			throw new ConfigurationException("Unable to upgrade the db due to " + e);
		}
	    
		return true;
	}
}
