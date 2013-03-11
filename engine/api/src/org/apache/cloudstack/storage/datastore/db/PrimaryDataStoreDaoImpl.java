// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.datastore.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.springframework.stereotype.Component;

import com.cloud.host.Status;

import com.cloud.storage.StoragePoolStatus;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value={PrimaryDataStoreDao.class}) @DB(txn=false)
public class PrimaryDataStoreDaoImpl extends GenericDaoBase<StoragePoolVO, Long>  implements PrimaryDataStoreDao {
    protected final SearchBuilder<StoragePoolVO> AllFieldSearch;
	protected final SearchBuilder<StoragePoolVO> DcPodSearch;
    protected final SearchBuilder<StoragePoolVO> DcPodAnyClusterSearch;
    protected final SearchBuilder<StoragePoolVO> DeleteLvmSearch;
    protected final GenericSearchBuilder<StoragePoolVO, Long> StatusCountSearch;
    
    @Inject protected StoragePoolDetailsDao _detailsDao;
	
    private final String DetailsSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_details ON storage_pool.id = storage_pool_details.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' and storage_pool.data_center_id = ? and (storage_pool.pod_id = ? or storage_pool.pod_id is null) and storage_pool.scope = ? and (";
	private final String DetailsSqlSuffix = ") GROUP BY storage_pool_details.pool_id HAVING COUNT(storage_pool_details.name) >= ?";
	private final String ZoneWideDetailsSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_details ON storage_pool.id = storage_pool_details.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' and storage_pool.data_center_id = ? and storage_pool.scope = ? and (";
	private final String ZoneWideDetailsSqlSuffix = ") GROUP BY storage_pool_details.pool_id HAVING COUNT(storage_pool_details.name) >= ?";
		
	private final String FindPoolTagDetails = "SELECT storage_pool_details.name FROM storage_pool_details WHERE pool_id = ? and value = ?";
	
    public PrimaryDataStoreDaoImpl() {
        AllFieldSearch = createSearchBuilder();
        AllFieldSearch.and("name", AllFieldSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("uuid", AllFieldSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("datacenterId", AllFieldSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("hostAddress", AllFieldSearch.entity().getHostAddress(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("status",AllFieldSearch.entity().getStatus(),SearchCriteria.Op.EQ);
        AllFieldSearch.and("path", AllFieldSearch.entity().getPath(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("podId", AllFieldSearch.entity().getPodId(), Op.EQ);
        AllFieldSearch.and("clusterId", AllFieldSearch.entity().getClusterId(), Op.EQ);
        AllFieldSearch.done();  
        
    	DcPodSearch = createSearchBuilder();
    	DcPodSearch.and("datacenterId", DcPodSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
    	DcPodSearch.and("status", DcPodSearch.entity().getStatus(), SearchCriteria.Op.EQ);
    	DcPodSearch.and("scope", DcPodSearch.entity().getScope(), SearchCriteria.Op.EQ);
    	DcPodSearch.and().op("nullpod", DcPodSearch.entity().getPodId(), SearchCriteria.Op.NULL);
    	DcPodSearch.or("podId", DcPodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
    	DcPodSearch.cp();
    	DcPodSearch.and().op("nullcluster", DcPodSearch.entity().getClusterId(), SearchCriteria.Op.NULL);
    	DcPodSearch.or("cluster", DcPodSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
    	DcPodSearch.cp();
    	DcPodSearch.done();
    	
    	DcPodAnyClusterSearch = createSearchBuilder();
        DcPodAnyClusterSearch.and("datacenterId", DcPodAnyClusterSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.and("status", DcPodAnyClusterSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.and("scope", DcPodAnyClusterSearch.entity().getScope(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.and().op("nullpod", DcPodAnyClusterSearch.entity().getPodId(), SearchCriteria.Op.NULL);
        DcPodAnyClusterSearch.or("podId", DcPodAnyClusterSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.cp();
        DcPodAnyClusterSearch.done();
        
        DeleteLvmSearch = createSearchBuilder();
        DeleteLvmSearch.and("ids", DeleteLvmSearch.entity().getId(), SearchCriteria.Op.IN);
        DeleteLvmSearch.and().op("LVM", DeleteLvmSearch.entity().getPoolType(), SearchCriteria.Op.EQ);
        DeleteLvmSearch.or("Filesystem", DeleteLvmSearch.entity().getPoolType(), SearchCriteria.Op.EQ);
        DeleteLvmSearch.cp();
        DeleteLvmSearch.done();        

        
        
        StatusCountSearch = createSearchBuilder(Long.class);
        StatusCountSearch.and("status", StatusCountSearch.entity().getStatus(), SearchCriteria.Op.IN);
        StatusCountSearch.select(null, Func.COUNT, null);
        StatusCountSearch.done();

    }
    
	@Override
	public List<StoragePoolVO> findPoolByName(String name) {
		SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("name", name);
        return listIncludingRemovedBy(sc);
	}


	@Override
	public StoragePoolVO findPoolByUUID(String uuid) {
		SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneIncludingRemovedBy(sc);
	}
	
	

	@Override
	public List<StoragePoolVO> findIfDuplicatePoolsExistByUUID(String uuid) {
		SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("uuid", uuid);
        return listBy(sc);
	}


	@Override
	public List<StoragePoolVO> listByDataCenterId(long datacenterId) {
		SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
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
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("hostAddress", hostFqdnOrIp);
        return listIncludingRemovedBy(sc);
    }
    
    @Override
    public List<StoragePoolVO> listByStatus(StoragePoolStatus status){
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
    	sc.setParameters("status", status);
    	return listBy(sc);
    }
    
    @Override
    public List<StoragePoolVO> listByStatusInZone(long dcId, StoragePoolStatus status){
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
    	sc.setParameters("status", status);
    	sc.setParameters("datacenterId", dcId);
    	return listBy(sc);
    }

    @Override
    public StoragePoolVO findPoolByHostPath(long datacenterId, Long podId, String host, String path, String uuid) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("hostAddress", host);
        sc.setParameters("path", path);
        sc.setParameters("datacenterId", datacenterId);
        sc.setParameters("podId", podId);
        sc.setParameters("uuid", uuid);
        
        return findOneBy(sc);
    }

	@Override
	public List<StoragePoolVO> listBy(long datacenterId, long podId, Long clusterId, ScopeType scope) {
	    if (clusterId != null) {
    		SearchCriteria<StoragePoolVO> sc = DcPodSearch.create();
            sc.setParameters("datacenterId", datacenterId);
            sc.setParameters("podId", podId);
            sc.setParameters("status", Status.Up);
            sc.setParameters("scope", scope);
           
            sc.setParameters("cluster", clusterId);
            return listBy(sc);
	    } else {
	        SearchCriteria<StoragePoolVO> sc = DcPodAnyClusterSearch.create();
	        sc.setParameters("datacenterId", datacenterId);
	        sc.setParameters("podId", podId);
	        sc.setParameters("status", Status.Up);
	        sc.setParameters("scope", scope);
	        return listBy(sc);
	    }
	}

	@Override
	public List<StoragePoolVO> listPoolByHostPath(String host, String path) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("hostAddress", host);
        sc.setParameters("path", path);
        
        return listBy(sc);
	}
	
	public StoragePoolVO listById(Integer id)
	{
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("id", id);
        
        return findOneIncludingRemovedBy(sc);
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
	public List<StoragePoolVO> findPoolsByDetails(long dcId, long podId, Long clusterId, Map<String, String> details, ScopeType scope) {
	    StringBuilder sql = new StringBuilder(DetailsSqlPrefix);
	    if (clusterId != null) {
	        sql.append("storage_pool.cluster_id = ? OR storage_pool.cluster_id IS NULL) AND (");
	    }
	    
	    for (Map.Entry<String, String> detail : details.entrySet()) {
	        sql.append("((storage_pool_details.name='").append(detail.getKey()).append("') AND (storage_pool_details.value='").append(detail.getValue()).append("')) OR ");
	    }
	    sql.delete(sql.length() - 4, sql.length());
	    sql.append(DetailsSqlSuffix);
	    Transaction txn = Transaction.currentTxn();
	    PreparedStatement pstmt = null;
	    try {
	        pstmt = txn.prepareAutoCloseStatement(sql.toString());
	        int i = 1;
	        pstmt.setLong(i++, dcId);
	        pstmt.setLong(i++, podId);
	        pstmt.setString(i++, scope.toString());
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
	        throw new CloudRuntimeException("Unable to execute " + pstmt, e);
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
	public List<StoragePoolVO> findPoolsByTags(long dcId, long podId, Long clusterId, String[] tags) {
		List<StoragePoolVO> storagePools = null;
	    if (tags == null || tags.length == 0) {
	        storagePools = listBy(dcId, podId, clusterId, ScopeType.CLUSTER);
	    } else {
	        Map<String, String> details = tagsToDetails(tags);
	        storagePools =  findPoolsByDetails(dcId, podId, clusterId, details, ScopeType.CLUSTER);
	    }

	    return storagePools;
	}
	
	@Override
	public List<StoragePoolVO> findLocalStoragePoolsByTags(long dcId, long podId, Long clusterId, String[] tags) {
		List<StoragePoolVO> storagePools = null;
	    if (tags == null || tags.length == 0) {
	        storagePools = listBy(dcId, podId, clusterId, ScopeType.HOST);
	    } else {
	        Map<String, String> details = tagsToDetails(tags);
	        storagePools =  findPoolsByDetails(dcId, podId, clusterId, details, ScopeType.HOST);
	    }

	    return storagePools;
	}
	
	@Override
	public List<StoragePoolVO> findZoneWideStoragePoolsByTags(long dcId, String[] tags) {
		List<StoragePoolVO> storagePools = null;
	    if (tags == null || tags.length == 0) {
	    	SearchCriteriaService<StoragePoolVO, StoragePoolVO> sc =  SearchCriteria2.create(StoragePoolVO.class);
	    	sc.addAnd(sc.getEntity().getDataCenterId(), Op.EQ, dcId);
	    	sc.addAnd(sc.getEntity().getStatus(), Op.EQ, Status.Up);
	    	sc.addAnd(sc.getEntity().getScope(), Op.EQ, ScopeType.ZONE);
	    	return sc.list();
	    } else {
	        Map<String, String> details = tagsToDetails(tags);
	        
	        StringBuilder sql = new StringBuilder(ZoneWideDetailsSqlPrefix);
		    
		    for (Map.Entry<String, String> detail : details.entrySet()) {
		        sql.append("((storage_pool_details.name='").append(detail.getKey()).append("') AND (storage_pool_details.value='").append(detail.getValue()).append("')) OR ");
		    }
		    sql.delete(sql.length() - 4, sql.length());
		    sql.append(ZoneWideDetailsSqlSuffix);
		    Transaction txn = Transaction.currentTxn();
		    PreparedStatement pstmt = null;
		    try {
		        pstmt = txn.prepareAutoCloseStatement(sql.toString());
		        int i = 1;
		        pstmt.setLong(i++, dcId);
		        pstmt.setString(i++, ScopeType.ZONE.toString());
		        pstmt.setInt(i++, details.size());
		        ResultSet rs = pstmt.executeQuery();
		        List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();
		        while (rs.next()) {
		            pools.add(toEntityBean(rs, false));
		        }
		        return pools;
		    } catch (SQLException e) {
		        throw new CloudRuntimeException("Unable to execute " + pstmt, e);
		    }
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
	
    
    
    @Override
    public long countPoolsByStatus( StoragePoolStatus... statuses) {
        SearchCriteria<Long> sc = StatusCountSearch.create();
        
        sc.setParameters("status", (Object[])statuses);
        
        List<Long> rs = customSearchIncludingRemoved(sc, null);
        if (rs.size() == 0) {
            return 0;
        }
        
        return rs.get(0);
    }
    
    @Override
    public List<StoragePoolVO> listPoolsByCluster(long clusterId) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("clusterId", clusterId);
        
        return listBy(sc);
    }
}
