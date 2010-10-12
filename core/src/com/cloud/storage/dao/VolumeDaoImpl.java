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
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.MirrorState;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value=VolumeDao.class) @DB(txn=false)
public class VolumeDaoImpl extends GenericDaoBase<VolumeVO, Long> implements VolumeDao {
    private static final Logger s_logger = Logger.getLogger(VolumeDaoImpl.class);
    protected final SearchBuilder<VolumeVO> DetachedAccountIdSearch;
    protected final SearchBuilder<VolumeVO> AccountIdSearch;
    protected final SearchBuilder<VolumeVO> AccountPodSearch;
    protected final SearchBuilder<VolumeVO> TemplateZoneSearch;
    protected final GenericSearchBuilder<VolumeVO, SumCount> TotalSizeByPoolSearch;
    protected final SearchBuilder<VolumeVO> InstanceIdSearch;
    protected final SearchBuilder<VolumeVO> InstanceAndTypeSearch;
    protected final SearchBuilder<VolumeVO> InstanceIdDestroyedSearch;
    protected final SearchBuilder<VolumeVO> InstanceIdCreatedSearch;
    protected final SearchBuilder<VolumeVO> DetachedDestroyedSearch;
    protected final SearchBuilder<VolumeVO> MirrorSearch;
    protected final GenericSearchBuilder<VolumeVO, Long> ActiveTemplateSearch;
    protected final SearchBuilder<VolumeVO> RemovedButNotDestroyedSearch;
    protected final SearchBuilder<VolumeVO> PoolIdSearch;
    protected final SearchBuilder<VolumeVO> InstanceAndDeviceIdSearch;
    protected final SearchBuilder<VolumeVO> InstanceStatesSearch;
    protected final SearchBuilder<VolumeVO> IdStateSearch;
    
    protected final Attribute _stateAttr;
    
    protected static final String SELECT_VM_SQL = "SELECT DISTINCT instance_id from volumes v where v.host_id = ? and v.mirror_state = ?";
    protected static final String SELECT_VM_ID_SQL = "SELECT DISTINCT instance_id from volumes v where v.host_id = ?";
    protected static final String SELECT_HYPERTYPE_FROM_VOLUME = "SELECT c.hypervisor_type from volumes v, storage_pool s, cluster c where v.pool_id = s.id and s.cluster_id = c.id and v.id = ?";

    @Override
    public List<VolumeVO> listRemovedButNotDestroyed() {
        SearchCriteria<VolumeVO> sc = RemovedButNotDestroyedSearch.create();
        sc.setParameters("destroyed", false);
        
        return searchIncludingRemoved(sc, null, null, false);
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
    	SearchCriteria<VolumeVO> sc = DetachedAccountIdSearch.create();
    	sc.setParameters("accountId", accountId);
    	sc.setParameters("destroyed", false);
    	return listBy(sc);
    }
    
    @Override
    public List<VolumeVO> findByAccount(long accountId) {
        SearchCriteria<VolumeVO> sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("destroyed", false);
        return listBy(sc);
    }
    
    @Override
    public List<VolumeVO> findByInstance(long id) {
        SearchCriteria<VolumeVO> sc = InstanceIdSearch.create();
        sc.setParameters("instanceId", id);
	    return listBy(sc);
	}
   
    @Override
    public List<VolumeVO> findByInstanceAndDeviceId(long instanceId, long deviceId){
    	SearchCriteria<VolumeVO> sc = InstanceAndDeviceIdSearch.create();
    	sc.setParameters("instanceId", instanceId);
    	sc.setParameters("deviceId", deviceId);
    	return listBy(sc);
    }
    
    @Override
    public List<VolumeVO> findByPoolId(long poolId) {
        SearchCriteria<VolumeVO> sc = PoolIdSearch.create();
        sc.setParameters("poolId", poolId);
	    return listBy(sc);
	}
    
    @Override 
    public List<VolumeVO> findCreatedByInstance(long id) {
        SearchCriteria<VolumeVO> sc = InstanceIdCreatedSearch.create();
        sc.setParameters("instanceId", id);
        sc.setParameters("status", AsyncInstanceCreateStatus.Created);
        sc.setParameters("destroyed", false);
        return listBy(sc);
    }
    
    @Override
    public List<VolumeVO> findUsableVolumesForInstance(long instanceId) {
        SearchCriteria<VolumeVO> sc = InstanceStatesSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("states", Volume.State.Creating, Volume.State.Ready, Volume.State.Allocated);
        
        return listBy(sc);
    }
    
	@Override
	public List<VolumeVO> findByInstanceAndType(long id, VolumeType vType) {
        SearchCriteria<VolumeVO> sc = InstanceAndTypeSearch.create();
        sc.setParameters("instanceId", id);
        sc.setParameters("vType", vType.toString());
	    return listBy(sc);
	}
	
	@Override
	public List<VolumeVO> findByInstanceIdDestroyed(long vmId) {
		SearchCriteria<VolumeVO> sc = InstanceIdDestroyedSearch.create();
		sc.setParameters("instanceId", vmId);
		sc.setParameters("destroyed", true);
		return listIncludingRemovedBy(sc);
	}
	
	@Override
	public List<VolumeVO> findByDetachedDestroyed() {
		SearchCriteria<VolumeVO> sc = DetachedDestroyedSearch.create();
		sc.setParameters("destroyed", true);
		return listBy(sc);
	}
	
	@Override
	public List<VolumeVO> findByAccountAndPod(long accountId, long podId) {
		SearchCriteria<VolumeVO> sc = AccountPodSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("pod", podId);
        sc.setParameters("destroyed", false);
        sc.setParameters("status", AsyncInstanceCreateStatus.Created);
        
        return listIncludingRemovedBy(sc);
	}
	
	@Override
	public List<VolumeVO> findByTemplateAndZone(long templateId, long zoneId) {
		SearchCriteria<VolumeVO> sc = TemplateZoneSearch.create();
		sc.setParameters("template", templateId);
		sc.setParameters("zone", zoneId);
		
		return listIncludingRemovedBy(sc);
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
		SearchCriteria<VolumeVO> sc = MirrorSearch.create();
        sc.setParameters("mirrorState", MirrorState.ACTIVE.toString());

	    return listIncludingRemovedBy(sc);
	}
	
	@Override
	public boolean isAnyVolumeActivelyUsingTemplateOnPool(long templateId, long poolId) {
	    SearchCriteria<Long> sc = ActiveTemplateSearch.create();
	    sc.setParameters("template", templateId);
	    sc.setParameters("pool", poolId);
	    
	    List<Long> results = searchIncludingRemoved(sc, null);
	    assert results.size() > 0 : "How can this return a size of " + results.size();
	    
	    return results.get(0) > 0;
	}
	
    @Override
    public void deleteVolumesByInstance(long instanceId) {
        SearchCriteria<VolumeVO> sc = InstanceIdSearch.create();
        sc.setParameters("instanceId", instanceId);
        expunge(sc);
    }
    
    @Override
    public void attachVolume(long volumeId, long vmId, long deviceId) {
    	VolumeVO volume = createForUpdate(volumeId);
    	volume.setInstanceId(vmId);
    	volume.setDeviceId(deviceId);
    	volume.setUpdated(new Date());
    	volume.setAttached(new Date());
    	update(volumeId, volume);
    }
    
    @Override
    public void detachVolume(long volumeId) {
    	VolumeVO volume = createForUpdate(volumeId);
    	volume.setInstanceId(null);
        volume.setDeviceId(null);
    	volume.setUpdated(new Date());
    	volume.setAttached(null);
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
    
    @Override
    public boolean update(VolumeVO vol, Volume.Event event) throws ConcurrentOperationException {
        Volume.State oldState = vol.getState();
        Volume.State newState = oldState.getNextState(event);
        
        assert newState != null : "Event "+  event + " cannot happen from " + oldState; 
        
        UpdateBuilder builder = getUpdateBuilder(vol);
        builder.set(vol, _stateAttr, newState);
        
        SearchCriteria<VolumeVO> sc = IdStateSearch.create();
        sc.setParameters("id", vol.getId());
        sc.setParameters("state", oldState);
        
        int rows = update(builder, sc, null);
        if (rows != 1) {
            VolumeVO dbVol = findById(vol.getId()); 
            throw new ConcurrentOperationException("Unable to update " + vol + ": Old State=" + oldState + "; New State = " + newState + "; DB State=" + dbVol.getState());
        }
        return rows == 1;
    }
    @DB
	public HypervisorType getHypervisorType(long volumeId) {
		/*lookup from cluster of pool*/
    	 Transaction txn = Transaction.currentTxn();
         PreparedStatement pstmt = null;

         try {
             String sql = SELECT_HYPERTYPE_FROM_VOLUME;
             pstmt = txn.prepareAutoCloseStatement(sql);
             pstmt.setLong(1, volumeId);
             ResultSet rs = pstmt.executeQuery();
             if (rs.next())
            	 return HypervisorType.getType(rs.getString(1));
             return HypervisorType.None;
         } catch (SQLException e) {
             throw new CloudRuntimeException("DB Exception on: " + SELECT_HYPERTYPE_FROM_VOLUME, e);
         } catch (Throwable e) {
             throw new CloudRuntimeException("Caught: " + SELECT_HYPERTYPE_FROM_VOLUME, e);
         }
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
        
        TotalSizeByPoolSearch = createSearchBuilder(SumCount.class);
        TotalSizeByPoolSearch.select("sum", Func.SUM, TotalSizeByPoolSearch.entity().getSize());
        TotalSizeByPoolSearch.select("count", Func.COUNT, (Object[])null);
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

        InstanceAndDeviceIdSearch = createSearchBuilder();
        InstanceAndDeviceIdSearch.and("instanceId", InstanceAndDeviceIdSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceAndDeviceIdSearch.and("deviceId", InstanceAndDeviceIdSearch.entity().getDeviceId(), SearchCriteria.Op.EQ);
        InstanceAndDeviceIdSearch.done();
        
        PoolIdSearch = createSearchBuilder();
        PoolIdSearch.and("poolId", PoolIdSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PoolIdSearch.done();

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
        
        ActiveTemplateSearch = createSearchBuilder(Long.class);
        ActiveTemplateSearch.and("pool", ActiveTemplateSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        ActiveTemplateSearch.and("template", ActiveTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        ActiveTemplateSearch.and("removed", ActiveTemplateSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        ActiveTemplateSearch.select(null, Func.COUNT, null);
        ActiveTemplateSearch.done();
        
        RemovedButNotDestroyedSearch = createSearchBuilder();
        RemovedButNotDestroyedSearch.and("destroyed", RemovedButNotDestroyedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        RemovedButNotDestroyedSearch.and("removed", RemovedButNotDestroyedSearch.entity().getRemoved(), SearchCriteria.Op.NNULL);
        RemovedButNotDestroyedSearch.done();
        
        InstanceStatesSearch = createSearchBuilder();
        InstanceStatesSearch.and("instance", InstanceStatesSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceStatesSearch.and("states", InstanceStatesSearch.entity().getState(), SearchCriteria.Op.IN);
        InstanceStatesSearch.done();
        
        IdStateSearch = createSearchBuilder();
        IdStateSearch.and("id", IdStateSearch.entity().getId(), SearchCriteria.Op.EQ);
        IdStateSearch.and("state", IdStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        IdStateSearch.done();
        
        _stateAttr = _allAttributes.get("state");
        assert _stateAttr != null : "Couldn't get the state attribute";
	}

	@Override @DB(txn=false)
	public Pair<Long, Long> getCountAndTotalByPool(long poolId) {
        SearchCriteria<SumCount> sc = TotalSizeByPoolSearch.create();
        sc.setParameters("poolId", poolId);
        List<SumCount> results = searchIncludingRemoved(sc, null);
        SumCount sumCount = results.get(0);
        return new Pair<Long, Long>(sumCount.count, sumCount.sum);
	}
	
	public static class SumCount {
	    public long sum;
	    public long count;
	    public SumCount() {
	    }
	}
}
