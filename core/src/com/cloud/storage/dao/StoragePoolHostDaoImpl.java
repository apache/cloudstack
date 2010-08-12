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

package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.host.Status;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={StoragePoolHostDao.class})
public class StoragePoolHostDaoImpl extends GenericDaoBase<StoragePoolHostVO, Long> implements StoragePoolHostDao {
	public static final Logger s_logger = Logger.getLogger(StoragePoolHostDaoImpl.class.getName());
	
	protected final SearchBuilder<StoragePoolHostVO> PoolSearch;
	protected final SearchBuilder<StoragePoolHostVO> HostSearch;
	protected final SearchBuilder<StoragePoolHostVO> PoolHostSearch;
	
	protected static final String HOST_FOR_POOL_SEARCH=
		"SELECT * FROM storage_pool_host_ref ph,  host h where  ph.host_id = h.id and ph.pool_id=? and h.status=? ";
	
	protected static final String STORAGE_POOL_HOST_INFO =
    	"SELECT p.data_center_id,  count(ph.host_id) " +
    	" FROM storage_pool p, storage_pool_host_ref ph " +
    	" WHERE p.id = ph.pool_id AND p.data_center_id = ? " +
    	" GROUP by p.data_center_id";
	
	protected static final String SHARED_STORAGE_POOL_HOST_INFO =
    	"SELECT p.data_center_id,  count(ph.host_id) " +
    	" FROM storage_pool p, storage_pool_host_ref ph " +
    	" WHERE p.id = ph.pool_id AND p.data_center_id = ? " +
    	" AND p.pool_type NOT IN ('LVM', 'Filesystem')" +
    	" GROUP by p.data_center_id";
	
	protected static final String GET_POOL_IDS  =
		"SELECT pool_id "+
		"FROM storage_pool_host_ref "+
		"WHERE host_id = ?";
	
	protected static final String DELETE_PRIMARY_RECORDS  =
		"DELETE "+
		"FROM storage_pool_host_ref "+
		"WHERE host_id = ?";
	
	public StoragePoolHostDaoImpl () {
		PoolSearch = createSearchBuilder();
		PoolSearch.and("pool_id", PoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
		PoolSearch.done();
		
		HostSearch = createSearchBuilder();
		HostSearch.and("host_id", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		HostSearch.done();
		
		PoolHostSearch = createSearchBuilder();
		PoolHostSearch.and("pool_id", PoolHostSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
		PoolHostSearch.and("host_id", PoolHostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		PoolHostSearch.done();

	}

	@Override
	public List<StoragePoolHostVO> listByPoolId(long id) {
	    SearchCriteria sc = PoolSearch.create();
	    sc.setParameters("pool_id", id);
	    return listBy(sc);
	}

	@Override
	public List<StoragePoolHostVO> listByHostId(long hostId) {
	    SearchCriteria sc = HostSearch.create();
	    sc.setParameters("host_id", hostId);
	    return listBy(sc);
	}

	@Override
	public StoragePoolHostVO findByPoolHost(long poolId, long hostId) {
		SearchCriteria sc = PoolHostSearch.create();
	    sc.setParameters("pool_id", poolId);
	    sc.setParameters("host_id", hostId);
	    return findOneBy(sc);
	}
	
	@Override
	public List<StoragePoolHostVO> listByHostStatus(long poolId, Status hostStatus) {
        Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		List<StoragePoolHostVO> result = new ArrayList<StoragePoolHostVO>();
		ResultSet rs = null;
		try {
			String sql = HOST_FOR_POOL_SEARCH;
			pstmt = txn.prepareStatement(sql);
			
			pstmt.setLong(1, poolId);
			pstmt.setString(2, hostStatus.toString());
			rs = pstmt.executeQuery();
			while (rs.next()) {
                // result.add(toEntityBean(rs, false)); TODO: this is buggy in GenericDaoBase for hand constructed queries
				long id = rs.getLong(1); //ID column
				result.add(findById(id));
            }
		} catch (Exception e) {
			s_logger.warn("Exception: ", e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (SQLException e) {
			}
		}
		return result;

	}
	
	 @Override
	 public List<Pair<Long, Integer>> getDatacenterStoragePoolHostInfo(long dcId, boolean sharedOnly) {
		 ArrayList<Pair<Long, Integer>> l = new ArrayList<Pair<Long, Integer>>();
		 String sql = sharedOnly?SHARED_STORAGE_POOL_HOST_INFO:STORAGE_POOL_HOST_INFO;
		 Transaction txn = Transaction.currentTxn();;
		 PreparedStatement pstmt = null;
		 try {
			 pstmt = txn.prepareAutoCloseStatement(sql);
			 pstmt.setLong(1, dcId);

			 ResultSet rs = pstmt.executeQuery();
			 while(rs.next()) {
				 l.add(new Pair<Long, Integer>(rs.getLong(1), rs.getInt(2)));
			 }
		 } catch (SQLException e) {
		 } catch (Throwable e) {
		 }
		 return l;
	 }
	 
	 /**
	  * This method returns the pool_ids associated with the host
	  * @param hostId -- id for the host
	  * @return -- list of pool ids
	  */
	 @DB
	 public ArrayList<Long> getPoolIds(Long hostId)
	 {
		 ArrayList<Long> poolIdsList = new ArrayList<Long>();

	        Transaction txn = Transaction.currentTxn();
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				 String sql = GET_POOL_IDS;
				 pstmt = txn.prepareStatement(sql);
				
				 pstmt.setLong(1, hostId);
				 rs = pstmt.executeQuery();
				 while (rs.next())
				 {
					poolIdsList.add(rs.getLong(1));
	            }
			}
			catch (Exception e)
			{
				s_logger.warn("Exception getting pool ids: ", e);
			}
			finally
			{
				try
				{
					if (rs != null)
					{
						rs.close();
					}
					if (pstmt != null)
					{
						pstmt.close();
					}
				}
				catch (SQLException e)
				{
				}
			}
			
		 return poolIdsList;
	 }

	 /**
	  * This method deletes the primary records from the host
	  * @param hostId -- id of the host
	  */
	 public void deletePrimaryRecordsForHost(long hostId)
	 {
		 SearchCriteria sc = HostSearch.create();
		 sc.setParameters("host_id", hostId);
		 Transaction txn = Transaction.currentTxn();
		 txn.start();
		 remove(sc);
		 txn.commit();
	 }



	@Override
	public void deleteStoragePoolHostDetails(long hostId, long poolId) {
		SearchCriteria sc = PoolHostSearch.create();
		sc.setParameters("host_id", hostId);
		sc.setParameters("pool_id", poolId);
		 Transaction txn = Transaction.currentTxn();
		 txn.start();
		 remove(sc);
		 txn.commit();
	}
}
