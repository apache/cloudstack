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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.storage.StoragePoolDetailVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={StoragePoolDao.class}) @DB(txn=false)
public class StoragePoolDaoImpl extends GenericDaoBase<StoragePoolVO, Long>  implements StoragePoolDao {
    private static final Logger s_logger = Logger.getLogger(StoragePoolDaoImpl.class);
    protected final SearchBuilder<StoragePoolVO> NameSearch;
	protected final SearchBuilder<StoragePoolVO> UUIDSearch;
	protected final SearchBuilder<StoragePoolVO> DatacenterSearch;
	protected final SearchBuilder<StoragePoolVO> DcPodSearch;
    protected final SearchBuilder<StoragePoolVO> HostSearch;
    protected final SearchBuilder<StoragePoolVO> HostPathDcPodSearch;
    protected final SearchBuilder<StoragePoolVO> HostPathDcSearch;
    protected final SearchBuilder<StoragePoolVO> DcPodAnyClusterSearch;
    protected final SearchBuilder<StoragePoolVO> DeleteLvmSearch;
    
    protected final StoragePoolDetailsDao _detailsDao;
	
    private final String DetailsSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_details ON storage_pool.id = storage_pool_details.pool_id WHERE storage_pool.removed is null and storage_pool.data_center_id = ? and (storage_pool.pod_id = ? or storage_pool.pod_id is null) and (";
	private final String DetailsSqlSuffix = ") GROUP BY storage_pool_details.pool_id HAVING COUNT(storage_pool_details.name) >= ?";
	private final String FindPoolTagDetails = "SELECT storage_pool_details.name FROM storage_pool_details WHERE pool_id = ? and value = ?";
	
    protected StoragePoolDaoImpl() {
    	NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.done();
        
    	UUIDSearch = createSearchBuilder();
        UUIDSearch.and("uuid", UUIDSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        UUIDSearch.done();
        
    	DatacenterSearch = createSearchBuilder();
        DatacenterSearch.and("datacenterId", DatacenterSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DatacenterSearch.done();
        
    	DcPodSearch = createSearchBuilder();
    	DcPodSearch.and("datacenterId", DcPodSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
    	DcPodSearch.op(Op.AND, "nullpod", DcPodSearch.entity().getPodId(), SearchCriteria.Op.NULL);
    	DcPodSearch.or("podId", DcPodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
    	DcPodSearch.cp();
    	DcPodSearch.op(Op.AND, "nullcluster", DcPodSearch.entity().getClusterId(), SearchCriteria.Op.NULL);
    	DcPodSearch.or("cluster", DcPodSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
    	DcPodSearch.cp();
    	DcPodSearch.done();
    	
    	DcPodAnyClusterSearch = createSearchBuilder();
        DcPodAnyClusterSearch.and("datacenterId", DcPodAnyClusterSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.op(Op.AND, "nullpod", DcPodAnyClusterSearch.entity().getPodId(), SearchCriteria.Op.NULL);
        DcPodAnyClusterSearch.or("podId", DcPodAnyClusterSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.cp();
        DcPodAnyClusterSearch.done();
        
        DeleteLvmSearch = createSearchBuilder();
        DeleteLvmSearch.and("ids", DeleteLvmSearch.entity().getId(), SearchCriteria.Op.IN);
        DeleteLvmSearch.op(SearchCriteria.Op.AND,"LVM", DeleteLvmSearch.entity().getPoolType(), SearchCriteria.Op.EQ);
        DeleteLvmSearch.or("Filesystem", DeleteLvmSearch.entity().getPoolType(), SearchCriteria.Op.EQ);
        DeleteLvmSearch.cp();
        DeleteLvmSearch.done();
        
        HostSearch = createSearchBuilder();
        HostSearch.and("host", HostSearch.entity().getHostAddress(), SearchCriteria.Op.EQ);
        HostSearch.done();
        
        HostPathDcPodSearch = createSearchBuilder();
        HostPathDcPodSearch.and("hostAddress", HostPathDcPodSearch.entity().getHostAddress(), SearchCriteria.Op.EQ);
        HostPathDcPodSearch.and("path", HostPathDcPodSearch.entity().getPath(), SearchCriteria.Op.EQ);
        HostPathDcPodSearch.and("datacenterId", HostPathDcPodSearch.entity().getDataCenterId(), Op.EQ);
        HostPathDcPodSearch.and("podId", HostPathDcPodSearch.entity().getPodId(), Op.EQ);
        HostPathDcPodSearch.done();
        
        HostPathDcSearch = createSearchBuilder();
        HostPathDcSearch.and("hostAddress", HostPathDcSearch.entity().getHostAddress(), SearchCriteria.Op.EQ);
        HostPathDcSearch.and("path", HostPathDcSearch.entity().getPath(), SearchCriteria.Op.EQ);
        HostPathDcSearch.done();

        _detailsDao = ComponentLocator.inject(StoragePoolDetailsDaoImpl.class);
    }
    
	@Override
	public List<StoragePoolVO> findPoolByName(String name) {
		SearchCriteria sc = NameSearch.create();
        sc.setParameters("name", name);
        return listBy(sc);
	}


	@Override
	public StoragePoolVO findPoolByUUID(String uuid) {
		SearchCriteria sc = UUIDSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
	}


	@Override
	public List<StoragePoolVO> listByDataCenterId(long datacenterId) {
		SearchCriteria sc = DatacenterSearch.create();
        sc.setParameters("datacenterId", datacenterId);
        return listBy(sc);
	}


	@Override
	public void updateAvailable(long id, long available) {
		StoragePoolVO pool = createForUpdate(id);
		pool.setAvailableBytes(available);
		update(id, pool);
	}


	@Override
	public void updateCapacity(long id, long capacity) {
		StoragePoolVO pool = createForUpdate(id);
		pool.setCapacityBytes(capacity);
		update(id, pool);

	}
	
    @Override
    public List<StoragePoolVO> listByStorageHost(String hostFqdnOrIp) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("host", hostFqdnOrIp);
        return listBy(sc);
    }

    @Override
    public StoragePoolVO findPoolByHostPath(long datacenterId, Long podId, String host, String path) {
        SearchCriteria sc = HostPathDcPodSearch.create();
        sc.setParameters("hostAddress", host);
        sc.setParameters("path", path);
        sc.setParameters("datacenterId", datacenterId);
        sc.setParameters("podId", podId);
        
        return findOneBy(sc);
    }

	@Override
	public List<StoragePoolVO> listBy(long datacenterId, long podId, Long clusterId) {
	    if (clusterId != null) {
    		SearchCriteria sc = DcPodSearch.create();
            sc.setParameters("datacenterId", datacenterId);
            sc.setParameters("podId", podId);
           
            sc.setParameters("cluster", clusterId);
            return listActiveBy(sc);
	    } else {
	        SearchCriteria sc = DcPodAnyClusterSearch.create();
	        sc.setParameters("datacenterId", datacenterId);
	        sc.setParameters("podId", podId);
	        return listActiveBy(sc);
	    }
	}

	@Override
	public List<StoragePoolVO> listPoolByHostPath(String host, String path) {
        SearchCriteria sc = HostPathDcSearch.create();
        sc.setParameters("hostAddress", host);
        sc.setParameters("path", path);
        
        return listBy(sc);
	}
	
	public StoragePoolVO listById(Integer id)
	{
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("id", id);
        
        return findOneBy(sc);
	}
	
	@Override @DB
	public StoragePoolVO persist(StoragePoolVO pool, Map<String, String> details) {
	    Transaction txn = Transaction.currentTxn();
	    txn.start();
	    pool = super.persist(pool);
	    if (details != null) {
    	    for (Map.Entry<String, String> detail : details.entrySet()) {
    	        StoragePoolDetailVO vo = new StoragePoolDetailVO(pool.getId(), detail.getKey(), detail.getValue());
    	        _detailsDao.persist(vo);
    	    }
        }
	    txn.commit();
	    return pool;
	}
	
	@DB
	@Override
	public void deleteStoragePoolRecords(ArrayList<Long> ids)
	{
			SearchCriteria sc = DeleteLvmSearch.create();
			sc.setParameters("ids", ids.toArray());
			sc.setParameters("LVM", StoragePoolType.LVM);
			sc.setParameters("Filesystem", StoragePoolType.Filesystem);
			remove(sc);
	}
	
	@DB
	@Override
	public List<StoragePoolVO> findPoolsByDetails(long dcId, long podId, Long clusterId, Map<String, String> details) {
	    StringBuilder sql = new StringBuilder(DetailsSqlPrefix);
	    if (clusterId != null) {
	        sql.append("storage_pool.cluster_id = ? OR storage_pool.cluster_id IS NULL) AND (");
	    }
	    Set<Map.Entry<String, String>> entries = details.entrySet();
	    for (Map.Entry<String, String> detail : details.entrySet()) {
	        sql.append("((storage_pool_details.name='").append(detail.getKey()).append("') AND (storage_pool_details.value='").append(detail.getValue()).append("')) OR ");
	    }
	    sql.delete(sql.length() - 4, sql.length());
	    sql.append(DetailsSqlSuffix);
	    Transaction txn = Transaction.currentTxn();
	    PreparedStatement pstmt = s_initStmt;
	    try {
	        pstmt = txn.prepareAutoCloseStatement(sql.toString());
	        int i = 1;
	        pstmt.setLong(i++, dcId);
	        pstmt.setLong(i++, podId);
	        if (clusterId != null) {
	            pstmt.setLong(i++, clusterId);
	        }
	        pstmt.setInt(i++, details.size());
	        ResultSet rs = pstmt.executeQuery();
	        List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();
	        while (rs.next()) {
	            pools.add(toEntityBean(rs, false));
	        }
	        return pools;
	    } catch (SQLException e) {
	        throw new CloudRuntimeException("Unable to execute " + pstmt.toString(), e);
	    }
	}
	
	protected Map<String, String> tagsToDetails(String[] tags) {
	    Map<String, String> details = new HashMap<String, String>(tags.length);
	    for (String tag: tags) {
	        details.put(tag, "true");
	    }
	    return details;
	}
	
	@Override
	public List<StoragePoolVO> findPoolsByTags(long dcId, long podId, Long clusterId, String[] tags, Boolean shared) {
		List<StoragePoolVO> storagePools = null;
	    if (tags == null || tags.length == 0) {
	        storagePools = listBy(dcId, podId, clusterId);
	    } else {
	        Map<String, String> details = tagsToDetails(tags);
	        storagePools =  findPoolsByDetails(dcId, podId, clusterId, details);
	    }
	    
	    if (shared == null) {
	    	return storagePools;
	    } else {
	    	List<StoragePoolVO> filteredStoragePools = new ArrayList<StoragePoolVO>(storagePools);
	    	for (StoragePoolVO pool : storagePools) {
	    		if (shared != pool.isShared()) {
	    			filteredStoragePools.remove(pool);
	    		}
	    	}
	    	
	    	return filteredStoragePools;
	    }
	}
	
	@Override
	@DB
	public List<String> searchForStoragePoolDetails(long poolId, String value){
		
	    StringBuilder sql = new StringBuilder(FindPoolTagDetails);

	    Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
	    try {
	        pstmt = txn.prepareAutoCloseStatement(sql.toString());
	        pstmt.setLong(1, poolId);
	        pstmt.setString(2, value);

	        ResultSet rs = pstmt.executeQuery();
	        List<String> tags = new ArrayList<String>();

	        while (rs.next()) {
	            tags.add(rs.getString("name"));
	        }
	        
	        return tags;
	    } catch (SQLException e) {
	        throw new CloudRuntimeException("Unable to execute " + pstmt.toString(), e);
	    }

	}
	
	@Override
	public void updateDetails(long poolId, Map<String, String> details) {
	    if (details != null) {
	        _detailsDao.update(poolId, details);
	    }
    }
	
	@Override
	public Map<String, String> getDetails(long poolId) {
		return _detailsDao.getDetails(poolId);
	}
    
	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
	    super.configure(name, params);
	    _detailsDao.configure("DetailsDao", params);
	    return true;
	}
}
