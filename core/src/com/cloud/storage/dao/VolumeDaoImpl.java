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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Volume.MirrorState;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value=VolumeDao.class) @DB(txn=false)
public class VolumeDaoImpl extends GenericDaoBase<VolumeVO, Long> implements VolumeDao {
    private static final Logger s_logger = Logger.getLogger(VolumeDaoImpl.class);
    protected final SearchBuilder<VolumeVO> DetachedAccountIdSearch;
    protected final SearchBuilder<VolumeVO> AccountIdSearch;
    protected final SearchBuilder<VolumeVO> AccountPodSearch;
    protected final SearchBuilder<VolumeVO> TemplateZoneSearch;
    protected final SearchBuilder<VolumeVO> TotalSizeByPoolSearch;
    protected final SearchBuilder<VolumeVO> InstanceIdSearch;
    protected final SearchBuilder<VolumeVO> InstanceAndTypeSearch;
    protected final SearchBuilder<VolumeVO> InstanceIdDestroyedSearch;
    protected final SearchBuilder<VolumeVO> InstanceIdCreatedSearch;
    protected final SearchBuilder<VolumeVO> DetachedDestroyedSearch;
    protected final SearchBuilder<VolumeVO> MirrorSearch;
    protected final SearchBuilder<VolumeVO> ActiveTemplateSearch;
    protected final SearchBuilder<VolumeVO> RemovedButNotDestroyedSearch;

    protected static final String SELECT_VM_SQL = "SELECT DISTINCT instance_id from volumes v where v.host_id = ? and v.mirror_state = ?";
    protected static final String SELECT_VM_ID_SQL = "SELECT DISTINCT instance_id from volumes v where v.host_id = ?";

    @Override
    public List<VolumeVO> listRemovedButNotDestroyed() {
        SearchCriteria sc = RemovedButNotDestroyedSearch.create();
        sc.setParameters("destroyed", false);
        
        return searchAll(sc, null, null, false);
    }
    
    @Override @DB
    public List<Long> findVmsStoredOnHost(long hostId) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        try {
            String sql = SELECT_VM_ID_SQL;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, hostId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + SELECT_VM_SQL, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + SELECT_VM_SQL, e);
        }
    }
    
    @Override
    public List<VolumeVO> findDetachedByAccount(long accountId) {
    	SearchCriteria sc = DetachedAccountIdSearch.create();
    	sc.setParameters("accountId", accountId);
    	sc.setParameters("destroyed", false);
    	return listActiveBy(sc);
    }
    
    @Override
    public List<VolumeVO> findByAccount(long accountId) {
        SearchCriteria sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("destroyed", false);
        return listActiveBy(sc);
    }
    
    @Override
    public List<VolumeVO> findByInstance(long id) {
        SearchCriteria sc = InstanceIdSearch.create();
        sc.setParameters("instanceId", id);
	    return listActiveBy(sc);
	}
    
    @Override 
    public List<VolumeVO> findCreatedByInstance(long id) {
        SearchCriteria sc = InstanceIdCreatedSearch.create();
        sc.setParameters("instanceId", id);
        sc.setParameters("status", AsyncInstanceCreateStatus.Created);
        sc.setParameters("destroyed", false);
        return listActiveBy(sc);
    }
    
	@Override
	public List<VolumeVO> findByInstanceAndType(long id, VolumeType vType) {
        SearchCriteria sc = InstanceAndTypeSearch.create();
        sc.setParameters("instanceId", id);
        sc.setParameters("vType", vType.toString());
	    return listActiveBy(sc);
	}
	
	@Override
	public List<VolumeVO> findByInstanceIdDestroyed(long vmId) {
		SearchCriteria sc = InstanceIdDestroyedSearch.create();
		sc.setParameters("instanceId", vmId);
		sc.setParameters("destroyed", true);
		return listBy(sc);
	}
	
	@Override
	public List<VolumeVO> findByDetachedDestroyed() {
		SearchCriteria sc = DetachedDestroyedSearch.create();
		sc.setParameters("destroyed", true);
		return listActiveBy(sc);
	}
	
	@Override
	public List<VolumeVO> findByAccountAndPod(long accountId, long podId) {
		SearchCriteria sc = AccountPodSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("pod", podId);
        sc.setParameters("destroyed", false);
        sc.setParameters("status", AsyncInstanceCreateStatus.Created);
        
        return listBy(sc);
	}
	
	@Override
	public List<VolumeVO> findByTemplateAndZone(long templateId, long zoneId) {
		SearchCriteria sc = TemplateZoneSearch.create();
		sc.setParameters("template", templateId);
		sc.setParameters("zone", zoneId);
		
		return listBy(sc);
	}

	@Override @DB
	public List<Long> findVMInstancesByStorageHost(long hostId, Volume.MirrorState mirrState) {
		
		Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

		try {
			String sql = SELECT_VM_SQL;
			pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setLong(1, hostId);
			pstmt.setString(2, mirrState.toString());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				result.add(rs.getLong(1));
			}
			return result;
		} catch (SQLException e) {
			throw new CloudRuntimeException("DB Exception on: " + SELECT_VM_SQL, e);
		} catch (Throwable e) {
			throw new CloudRuntimeException("Caught: " + SELECT_VM_SQL, e);
		}
	}

	@Override
	public List<VolumeVO> findStrandedMirrorVolumes() {
		SearchCriteria sc = MirrorSearch.create();
        sc.setParameters("mirrorState", MirrorState.ACTIVE.toString());

	    return listBy(sc);
	}
	
	@Override
	public boolean isAnyVolumeActivelyUsingTemplateOnPool(long templateId, long poolId) {
	    SearchCriteria sc = ActiveTemplateSearch.create();
	    sc.setParameters("template", templateId);
	    sc.setParameters("pool", poolId);
	    
	    List<Object[]> results = this.searchAll(sc, null);
	    if (results.size() == 0) {
	        return false;
	    }
	    
	    Object[] counts = results.get(0);
	    return ((BigInteger) counts[0]).longValue() > 0;
	}
	
    @Override
    public void deleteVolumesByInstance(long instanceId) {
        SearchCriteria sc = InstanceIdSearch.create();
        sc.setParameters("instanceId", instanceId);
        delete(sc);
    }
    
    @Override
    public void attachVolume(long volumeId, long vmId, long deviceId) {
    	VolumeVO volume = createForUpdate(volumeId);
    	volume.setInstanceId(vmId);
    	volume.setDeviceId(deviceId);
    	volume.setUpdated(new Date());
    	update(volumeId, volume);
    }
    
    @Override
    public void detachVolume(long volumeId) {
    	VolumeVO volume = createForUpdate(volumeId);
    	volume.setInstanceId(null);
        volume.setDeviceId(null);
    	volume.setUpdated(new Date());
    	update(volumeId, volume);
    }
    
    @Override
    public void destroyVolume(long volumeId) {
    	VolumeVO volume = createForUpdate(volumeId);
    	volume.setDestroyed(true);
    	update(volumeId, volume);
    }
    
    @Override
    public void recoverVolume(long volumeId) {
    	VolumeVO volume = createForUpdate(volumeId);
    	volume.setDestroyed(false);
    	update(volumeId, volume);
    }
    
	protected VolumeDaoImpl() {
        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("accountId", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.and("destroyed", AccountIdSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();
        
        DetachedAccountIdSearch = createSearchBuilder();
        DetachedAccountIdSearch.and("accountId", DetachedAccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        DetachedAccountIdSearch.and("destroyed", DetachedAccountIdSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        DetachedAccountIdSearch.and("instanceId", DetachedAccountIdSearch.entity().getInstanceId(), SearchCriteria.Op.NULL);
        DetachedAccountIdSearch.done();
        
        AccountPodSearch = createSearchBuilder();
        AccountPodSearch.and("account", AccountPodSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountPodSearch.and("pod", AccountPodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        AccountPodSearch.and("destroyed", AccountPodSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        AccountPodSearch.and("status", AccountPodSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        AccountPodSearch.done();
        
        TemplateZoneSearch = createSearchBuilder();
        TemplateZoneSearch.and("template", TemplateZoneSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateZoneSearch.and("zone", TemplateZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        TemplateZoneSearch.done();
        
        TotalSizeByPoolSearch = createSearchBuilder();
        TotalSizeByPoolSearch.select(Func.SUM, TotalSizeByPoolSearch.entity().getSize());
        TotalSizeByPoolSearch.select(Func.COUNT, (Object[])null);
        TotalSizeByPoolSearch.and("poolId", TotalSizeByPoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        TotalSizeByPoolSearch.and("removed", TotalSizeByPoolSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        TotalSizeByPoolSearch.done();
        
      
        InstanceIdCreatedSearch = createSearchBuilder();
        InstanceIdCreatedSearch.and("instanceId", InstanceIdCreatedSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceIdCreatedSearch.and("status", InstanceIdCreatedSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        InstanceIdCreatedSearch.and("destroyed", InstanceIdCreatedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        InstanceIdCreatedSearch.done();
        
        InstanceIdSearch = createSearchBuilder();
        InstanceIdSearch.and("instanceId", InstanceIdSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceIdSearch.done();
        
        InstanceAndTypeSearch= createSearchBuilder();
        InstanceAndTypeSearch.and("instanceId", InstanceAndTypeSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceAndTypeSearch.and("vType", InstanceAndTypeSearch.entity().getVolumeType(), SearchCriteria.Op.EQ);
        InstanceAndTypeSearch.done();
        
        InstanceIdDestroyedSearch = createSearchBuilder();
        InstanceIdDestroyedSearch.and("instanceId", InstanceIdDestroyedSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceIdDestroyedSearch.and("destroyed", InstanceIdDestroyedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        InstanceIdDestroyedSearch.done();
        
        DetachedDestroyedSearch = createSearchBuilder();
        DetachedDestroyedSearch.and("instanceId", DetachedDestroyedSearch.entity().getInstanceId(), SearchCriteria.Op.NULL);
        DetachedDestroyedSearch.and("destroyed", DetachedDestroyedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        DetachedDestroyedSearch.done();
               
        MirrorSearch = createSearchBuilder();
        MirrorSearch.and("mirrorVolume", MirrorSearch.entity().getMirrorVolume(), Op.NULL);
        MirrorSearch.and("mirrorState", MirrorSearch.entity().getMirrorState(), Op.EQ);
        MirrorSearch.done();
        
        ActiveTemplateSearch = createSearchBuilder();
        ActiveTemplateSearch.and("pool", ActiveTemplateSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        ActiveTemplateSearch.and("template", ActiveTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        ActiveTemplateSearch.and("removed", ActiveTemplateSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        ActiveTemplateSearch.selectField(Func.COUNT);
        ActiveTemplateSearch.done();
        
        RemovedButNotDestroyedSearch = createSearchBuilder();
        RemovedButNotDestroyedSearch.and("destroyed", RemovedButNotDestroyedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        RemovedButNotDestroyedSearch.and("removed", RemovedButNotDestroyedSearch.entity().getRemoved(), SearchCriteria.Op.NNULL);
        RemovedButNotDestroyedSearch.done();
	}

	@Override @DB(txn=false)
	public Pair<Long, Long> getCountAndTotalByPool(long poolId) {
        SearchCriteria sc = TotalSizeByPoolSearch.create();
        sc.setParameters("poolId", poolId);
        List<Object[]> results = searchAll(sc, null);
        Object[] objs = results.get(0);
        long size = (objs[0] == null) ? 0 : ((BigDecimal)objs[0]).longValue();
        long count = (Long)objs[1];
        return new Pair<Long, Long>(count, size);
	}
}
